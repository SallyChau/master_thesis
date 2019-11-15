package de.rwth.i2.attestor.phases.symbolicExecution.onthefly;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import de.rwth.i2.attestor.phases.modelChecking.hierarchical.HierarchicalFailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingTrace;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis.ModelCheckingInterproceduralAnalysis;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis.NonRecursiveModelCheckingMethodExecutor;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis.RecursiveModelCheckingMethodExecutor;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalContractCollection;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalPreconditionMatchingStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.OnTheFlyStateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.scopes.DefaultScopeExtractor;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.phases.transformers.InputSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.InputTransformer;
import de.rwth.i2.attestor.phases.transformers.MCSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.OnTheFlyModelCheckingResultsTransformer;
import de.rwth.i2.attestor.phases.transformers.OnTheFlyStateSpaceTransformer;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.MethodExecutor;
import de.rwth.i2.attestor.procedures.PreconditionMatchingStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.OnTheFlyStateSpaceGenerator;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;

public class OnTheFlyModelCheckingPhase extends AbstractPhase implements OnTheFlyStateSpaceTransformer, OnTheFlyModelCheckingResultsTransformer {
	
	private final OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory;	

	private ModelCheckingInterproceduralAnalysis interproceduralAnalysis;	
	private List<ProgramState> initialStates;
	private Method mainMethod;
	
	private StateSpace mainStateSpace = null;	
	
	private Set<LTLFormula> modelCheckingFormulae;
	private OnTheFlyProofStructure proofStructure;
	
	private final Map<LTLFormula, ModelCheckingResult> formulaResults = new LinkedHashMap<>();
    private final Map<LTLFormula, ModelCheckingTrace> traces = new LinkedHashMap<>();
    
    private boolean allSatisfied = true;
    private int numberSatFormulae = 0;
    
    
	
	public OnTheFlyModelCheckingPhase(Scene scene) {
		
		super(scene);
		stateSpaceGeneratorFactory = new OnTheFlyStateSpaceGeneratorFactory(scene);
	}
	
	
    
    @Override
	public String getName() {
		
		return "On-the-fly Model Checking";
	}
	
	@Override
    public void executePhase() {	    	
    	
		loadModelCheckingFormulae();
    	for (LTLFormula formula : modelCheckingFormulae) {
    		interproceduralAnalysis = new ModelCheckingInterproceduralAnalysis();
        	loadMethodsToSkip();
        	loadInitialStates();
        	loadMainMethod();
        	initializeMethodExecutors(); 
    		startOnTheFlyModelChecking(formula); 
    		registerMainProcedureCalls();
	        interproceduralAnalysis.run();
	        processModelCheckingResults(formula);
    	}
    }
    
    private void loadModelCheckingFormulae() {

    	ModelCheckingSettings settings = getPhase(MCSettingsTransformer.class).getMcSettings();
    	
    	modelCheckingFormulae = settings.getFormulae();        
        if (modelCheckingFormulae.isEmpty()) {
            logger.debug("No LTL formulae have been provided.");
        }
    }
    
    private void loadMethodsToSkip() {

    	ModelCheckingSettings settings = getPhase(MCSettingsTransformer.class).getMcSettings();
        
        List<String> methodsToSkip = settings.getMethodsToSkip(); 
        
        if (!methodsToSkip.isEmpty()) {
	    	for (Method method : scene().getRegisteredMethods()) {
	    		if (methodsToSkip.contains(method.getName())) {
	    			interproceduralAnalysis.registerMethodToSkip(method);
	    		}
	    	}
        }  
    }
    
    private void loadInitialStates() {

        List<HeapConfiguration> inputs = getPhase(InputTransformer.class).getInputs();
        initialStates = new ArrayList<>(inputs.size());
        for (HeapConfiguration hc : inputs) {
        	initialStates.add(scene().createProgramState(hc));
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

    private void initializeMethodExecutors() {

        OnTheFlyProcedureRegistry procedureRegistry = new OnTheFlyProcedureRegistry(
                interproceduralAnalysis,
                stateSpaceGeneratorFactory
        );

        PreconditionMatchingStrategy preconditionMatchingStrategy = new InternalPreconditionMatchingStrategy();

        for(Method method : scene().getRegisteredMethods()) {
            MethodExecutor executor;
            ContractCollection contractCollection = new InternalContractCollection(preconditionMatchingStrategy);
            if(method.isRecursive()) {
                executor = new RecursiveModelCheckingMethodExecutor(
                        method,
                        new DefaultScopeExtractor(this, method.getName()),
                        contractCollection,
                        procedureRegistry
                );
            } else {
                executor = new NonRecursiveModelCheckingMethodExecutor(
                		method,
                        new DefaultScopeExtractor(this, method.getName()),
                        contractCollection,
                        procedureRegistry 
                );
            }
            method.setMethodExecution(executor);
        }
    }
    
    private void startOnTheFlyModelChecking(LTLFormula formula) {   	    	
    		
        logger.info("Checking formula: " + formula.getFormulaString() + "...");
        
        Set<Node> formulae = new HashSet<>();
        formulae.add(formula.getASTRoot().getPLtlform());        
        
        OnTheFlyStateSpaceGenerator stateSpaceGenerator = stateSpaceGeneratorFactory.create(mainMethod.getBody(), initialStates, formulae);
    	
    	try {
			mainStateSpace = stateSpaceGenerator.generateAndCheck();
		} catch (StateSpaceGenerationAbortedException e) {	
			e.printStackTrace();
		}
    	
    	proofStructure = stateSpaceGenerator.getProofStructure();			
    }
    
    /**
     * Register (current) main state space (incl proofStructure and result formulae) 
     * in interprocedural analysis to continue model checking for procedure calls
     */
    private void registerMainProcedureCalls() {

        for(ProgramState iState : initialStates) {
            StateSpace stateSpace = iState.getContainingStateSpace();
            OnTheFlyProcedureCall mainCall = new OnTheFlyProcedureCall(mainMethod, iState.getHeap(), stateSpaceGeneratorFactory, null);
            interproceduralAnalysis.registerStateSpace(mainCall, stateSpace);
            interproceduralAnalysis.registerProofStructure(mainCall, proofStructure);
        }
    }    
    
    private void processModelCheckingResults(LTLFormula formula) {
    	
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
            	HierarchicalFailureTrace failureTrace = interproceduralAnalysis.getHierarchicalFailureTrace();
            	FailureTrace mainStateSpaceFailureTrace = proofStructure.getFailureTrace(mainStateSpace);
            	if (failureTrace.getStateTrace().isEmpty() || 
            			!failureTrace.getStateTrace().get(0).equals(mainStateSpaceFailureTrace)) {
            	
            		interproceduralAnalysis.addFailureTrace(mainStateSpaceFailureTrace);
            		failureTrace = interproceduralAnalysis.getHierarchicalFailureTrace();
            	}            	
            	traces.put(formula, failureTrace); 
            	
            	logger.info("Hierarchical FailureTrace: " + interproceduralAnalysis.getHierarchicalFailureTrace().getStateTrace());
            }
        }    	
    }

	@Override
	public void logSummary() {
		
		logSum("+-------------------------+------------------+");
		logSum("+---- On-the-fly Model Checking Results -----+");
		logSum("+-------------------------+------------------+");
        logHighlight("| Generated states        | Number of states |");
        logSum("+-------------------------+------------------+");
        logSum(String.format("| w/ procedure calls      | %16d |",
                scene().getNumberOfOnTheFlyGeneratedStates()));
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

		// do not count phase into verification phase
		return false;
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



	@Override
	public StateSpace getStateSpace() {
		
		return mainStateSpace;
	}



	@Override
	public Map<StateSpace, ProcedureCall> getProcedureStateSpaces() {

		return null;
	}



	@Override
	public List<ProcedureCall> getMainProcedureCalls() {
		
		return null;
	}



	@Override
	public Map<ProgramState, ProcedureCall> getCallingStatesToCall() {

		return null;
	}
}
