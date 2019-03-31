package de.rwth.i2.attestor.phases.modelChecking.onthefly;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.AbstractPhase;
import de.rwth.i2.attestor.main.scene.ElementNotPresentException;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.communication.InputSettings;
import de.rwth.i2.attestor.phases.communication.ModelCheckingSettings;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingTrace;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalContractCollection;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalPreconditionMatchingStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.StateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.scopes.DefaultScopeExtractor;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.InternalProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.InternalProcedureRegistry;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.InterproceduralAnalysis;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.NonRecursiveMethodExecutor;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.RecursiveMethodExecutor;
import de.rwth.i2.attestor.phases.transformers.InputSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.InputTransformer;
import de.rwth.i2.attestor.phases.transformers.MCSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.ModelCheckingResultsTransformer;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.MethodExecutor;
import de.rwth.i2.attestor.procedures.PreconditionMatchingStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerator;

public class OnTheFlyModelCheckingPhase extends AbstractPhase implements ModelCheckingResultsTransformer {
	
	private final StateSpaceGeneratorFactory stateSpaceGeneratorFactory;
	
	Method mainMethod;
	private List<ProgramState> initialStates; // states before any statement has been executed (independent from code)
	private StateSpace mainStateSpace;
	private OnTheFlyProofStructure proofStructure;
	private InterproceduralAnalysis interproceduralAnalysis;
	
	private Set<LTLFormula> modelCheckingFormulae;
	
	private final Map<LTLFormula, ModelCheckingResult> formulaResults = new LinkedHashMap<>();
    private final Map<LTLFormula, ModelCheckingTrace> traces = new LinkedHashMap<>();
    private boolean allSatisfied = true;

    private int numberSatFormulae = 0;
	
	public OnTheFlyModelCheckingPhase(Scene scene) {
		
		super(scene);
		stateSpaceGeneratorFactory = new StateSpaceGeneratorFactory(scene);
	}
	
	@Override
    public void executePhase() {
		
		System.out.println("--- On-the-fly Model Checking ---");
		
    	interproceduralAnalysis = new InterproceduralAnalysis();
    	loadInitialStates();
    	loadMainMethod();
    	initializeMethodExecutors();    	
    	loadModelCheckingFormulae();  
    	
    	if (modelCheckingFormulae != null) {
	        for (LTLFormula formula : modelCheckingFormulae) { // TODO do not generate state space again for each formula
	        	modelCheckOnTheFly(formula); 
	        }
    	}
        
        System.out.println("--- END On-the-fly Model Checking ---");
    }

	@Override
	public String getName() {
		
		return "On-the-fly Model Checking";
	}

	@Override
	public void logSummary() {
		
		logSum("+-------------------------+------------------+");
		logSum("+---- On-the-fly Model Checking Results ---+");
		logSum("+-------------------------+------------------+");
        logHighlight("| Generated states        | Number of states |");
        logSum("+-------------------------+------------------+");
        logSum(String.format("| w/ procedure calls      | %16d |",
                scene().getNumberOfGeneratedStates()));
        if (mainStateSpace != null) {
		    logSum(String.format("| w/o procedure calls     | %16d |",
		            mainStateSpace.getStates().size()));
		    logSum(String.format("| final states            | %16d |",
		            mainStateSpace.getFinalStateIds().size()));
        }
        logSum("+-------------------------+------------------+");
		
		if (formulaResults.isEmpty()) {
            return;
        }

        if (allSatisfied) {
            logHighlight("On-the-fly Model checking results: All provided LTL formulae are satisfied.");
        } else {
            logHighlight("On-the-fly Model checking results: Some provided LTL formulae could not be verified.");
        }

        for (Map.Entry<LTLFormula, ModelCheckingResult> result : formulaResults.entrySet()) {
            Level level = Level.getLevel(ModelCheckingResult.getString(result.getValue()));
            String formulaString = result.getKey().getFormulaString();
            logger.log(level, formulaString);
        }
	}

	@Override
	public boolean isVerificationPhase() {

		return true;
	}
	
	@Override
    public Map<LTLFormula, ModelCheckingResult> getLTLResults() {

        return formulaResults;
    }

    @Override
    public ModelCheckingTrace getTraceOf(LTLFormula formula) {

        if (traces.containsKey(formula)) {
            return traces.get(formula);
        }
        return null;
    }

    @Override
    public boolean hasAllLTLSatisfied() {

        return allSatisfied;
    }

    @Override
    public int getNumberSatFormulae() {
        return numberSatFormulae;
    }
    
    private void loadInitialStates() {

        List<HeapConfiguration> inputs = getPhase(InputTransformer.class).getInputs();
        initialStates = new ArrayList<>(inputs.size());
        for(HeapConfiguration hc : inputs) {
        	initialStates.add(scene().createProgramState(hc));
        }
    }

    private void initializeMethodExecutors() {

        InternalProcedureRegistry procedureRegistry = new InternalProcedureRegistry(
                interproceduralAnalysis,
                stateSpaceGeneratorFactory
        );

        PreconditionMatchingStrategy preconditionMatchingStrategy = new InternalPreconditionMatchingStrategy();

        for(Method method : scene ().getRegisteredMethods()) {
            MethodExecutor executor;
            ContractCollection contractCollection = new InternalContractCollection(preconditionMatchingStrategy);
            if(method.isRecursive()) {
                executor = new RecursiveMethodExecutor(
                        method,
                        new DefaultScopeExtractor(this, method.getName()),
                        contractCollection,
                        procedureRegistry
                );
            } else {
                executor = new NonRecursiveMethodExecutor(
                		method,
                        new DefaultScopeExtractor(this, method.getName()),
                        contractCollection,
                        procedureRegistry 
                );
            }
            method.setMethodExecution(executor);
        }
    }
    
    private void registerMainProcedureCalls() {

        for(ProgramState iState : initialStates) {
            StateSpace stateSpace = iState.getContainingStateSpace();
            ProcedureCall mainCall = new InternalProcedureCall(mainMethod, iState.getHeap(), stateSpaceGeneratorFactory, null);
            interproceduralAnalysis.registerStateSpace(mainCall, stateSpace);
            interproceduralAnalysis.registerProofStructure(mainCall, proofStructure); // TODO adapt proof structure to initial states
        }
    }
    
    private void loadMainMethod() {

        InputSettings inputSettings = getPhase(InputSettingsTransformer.class).getInputSettings();
        String methodName = inputSettings.getMethodName();
        try {
			mainMethod = scene().getMethodIfPresent(methodName);
		} catch (ElementNotPresentException e) {
			mainMethod = findMatchingMethod(methodName);
		}
        if(mainMethod.getBody() == null) {
            mainMethod = findMatchingMethod(methodName);
        }
    }
    
    private Method findMatchingMethod(String methodName) { 

        for(Method method : scene().getRegisteredMethods()) {
            if(methodName.equals(method.getName())) {
                logger.info("Found matching top-level method with signature: " + method.getSignature());
                return method;
            }
        }

        throw new IllegalArgumentException("Could not find top-level method '" + methodName + "'.");
    }
    
    private void loadModelCheckingFormulae() {
    	
    	ModelCheckingSettings settings = getPhase(MCSettingsTransformer.class).getMcSettings();
        modelCheckingFormulae = settings.getFormulae();
        
        if (modelCheckingFormulae.isEmpty()) {
            logger.debug("No LTL formulae have been provided.");
        }
    }
    
    private void modelCheckOnTheFly(LTLFormula formula) {    	
    	
        logger.info("Checking formula: " + formula.getFormulaString() + "...");
        
        LinkedList<Node> formulae = new LinkedList<>();
        formulae.add(formula.getASTRoot().getPLtlform());        
        
        StateSpaceGenerator stateSpaceGenerator = stateSpaceGeneratorFactory.create(mainMethod.getBody(), initialStates);
    	
    	try {
    		System.out.println("Generate state space for method " + mainMethod.getName());
			mainStateSpace = stateSpaceGenerator.generateAndCheck(initialStates, formulae);
		} catch (StateSpaceGenerationAbortedException e) {	
			e.printStackTrace();
		}
    	
    	proofStructure = stateSpaceGenerator.getProofStructure();
    
		registerMainProcedureCalls();
        interproceduralAnalysis.run2();
    	
    	// process model checking result
    	System.out.println("Model Checking: Proof Structure checked " + proofStructure.getNumberOfCheckedAssertions() + " assertions.");
    	if (proofStructure.isSuccessful()) {
            if(mainStateSpace.containsAbortedStates()) {
                allSatisfied = false;
                formulaResults.put(formula, ModelCheckingResult.UNKNOWN);
                logger.info("done. It is unknown whether the formula is satisfied.");
            } else {
                formulaResults.put(formula, ModelCheckingResult.SATISFIED);
                logger.info("done. Formula is satisfied.");
                numberSatFormulae++;
            }
        } else {
            logger.info("Formula is violated: " + formula.getFormulaString());
            allSatisfied = false;
            formulaResults.put(formula, ModelCheckingResult.UNSATISFIED);

            if (scene().options().isIndexedMode()) {
                logger.warn("Counterexample generation for indexed grammars is not supported yet.");
            } else {
                FailureTrace failureTrace = proofStructure.getFailureTrace(mainStateSpace);
                System.out.println(failureTrace.toString());
                traces.put(formula, failureTrace);
            }
        }
    }
}
