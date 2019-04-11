package de.rwth.i2.attestor.phases.symbolicExecution.recursive;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.AbstractPhase;
import de.rwth.i2.attestor.main.scene.ElementNotPresentException;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.communication.InputSettings;
import de.rwth.i2.attestor.phases.communication.ModelCheckingSettings;
import de.rwth.i2.attestor.phases.modelChecking.onthefly.OnTheFlyProofStructure;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalContractCollection;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalPreconditionMatchingStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.StateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.scopes.DefaultScopeExtractor;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.InterproceduralAnalysis;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.NonRecursiveMethodExecutor;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.RecursiveMethodExecutor;
import de.rwth.i2.attestor.phases.transformers.InputSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.InputTransformer;
import de.rwth.i2.attestor.phases.transformers.MCSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.StateSpaceTransformer;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.MethodExecutor;
import de.rwth.i2.attestor.procedures.PreconditionMatchingStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerator;


public class RecursiveStateSpaceGenerationPhase extends AbstractPhase implements StateSpaceTransformer {

    private final StateSpaceGeneratorFactory stateSpaceGeneratorFactory;

    private InterproceduralAnalysis interproceduralAnalysis;
    private List<ProgramState> initialStates;
    private Method mainMethod;
    private StateSpace mainStateSpace = null;
    
    private List<ProcedureCall> mainProcedureCalls;

    public RecursiveStateSpaceGenerationPhase(Scene scene) {

        super(scene);
        stateSpaceGeneratorFactory = new StateSpaceGeneratorFactory(scene);
        mainProcedureCalls = new LinkedList<>();
    }

    @Override
    public String getName() {

        return "Interprocedural Analysis";
    }

    @Override
    public void executePhase() {

        interproceduralAnalysis = new InterproceduralAnalysis();
        loadInitialStates();
        loadMainMethod();
        initializeMethodExecutors();
        
        ModelCheckingSettings mcSettings = getPhase(MCSettingsTransformer.class).getMcSettings();
    	if (mcSettings.getModelCheckingMode().equals("onthefly")) {
    		modelCheckOnTheFly();
    	} else {
	        startPartialStateSpaceGeneration();
	        registerMainProcedureCalls();
	        interproceduralAnalysis.run();
    	}

        if(mainStateSpace.getFinalStateIds().isEmpty()) {
            logger.error("Computed state space contains no final states.");
        }
    }

    private void loadInitialStates() {

        List<HeapConfiguration> inputs = getPhase(InputTransformer.class).getInputs();
        initialStates = new ArrayList<>(inputs.size());
        for(HeapConfiguration hc : inputs) {
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

    private void startPartialStateSpaceGeneration() {

        try {
            mainStateSpace = stateSpaceGeneratorFactory.create(mainMethod.getBody(), initialStates).generate();
        } catch (StateSpaceGenerationAbortedException e) {
            e.printStackTrace();
        }
    }

    private void registerMainProcedureCalls() {

        for(ProgramState iState : initialStates) {
            StateSpace mainStateSpace = iState.getContainingStateSpace();
            ProcedureCall mainCall = new InternalProcedureCall(mainMethod, iState.getHeap(), stateSpaceGeneratorFactory, null);
            mainProcedureCalls.add(mainCall);
            interproceduralAnalysis.registerStateSpace(mainCall, mainStateSpace);
        }
    }
    
    private void modelCheckOnTheFly() {
    	
    	ModelCheckingSettings settings = getPhase(MCSettingsTransformer.class).getMcSettings();
        Set<LTLFormula> modelCheckingFormulae = settings.getFormulae();
        
        if (modelCheckingFormulae.isEmpty()) {
            logger.debug("No LTL formulae have been provided.");
        } 	
    	
        for (LTLFormula formula : modelCheckingFormulae) {
	        logger.info("Checking formula on-the-fly: " + formula.getFormulaString() + "...");
	        
	        LinkedList<Node> formulae = new LinkedList<>();
	        formulae.add(formula.getASTRoot().getPLtlform());        
	        
	        StateSpaceGenerator stateSpaceGenerator = stateSpaceGeneratorFactory.create(mainMethod.getBody(), initialStates, formulae);
	    	
	    	try {
				mainStateSpace = stateSpaceGenerator.generateOnTheFly();
			} catch (StateSpaceGenerationAbortedException e) {	
				e.printStackTrace();
			}
	    	
	    	OnTheFlyProofStructure proofStructure = stateSpaceGenerator.getProofStructure();
	    
			//registerMainProcedureCalls
            for(ProgramState iState : initialStates) {
                StateSpace mainStateSpace = iState.getContainingStateSpace();
                ProcedureCall mainCall = new InternalProcedureCall(mainMethod, iState.getHeap(), stateSpaceGeneratorFactory, null);
                mainProcedureCalls.add(mainCall);
                interproceduralAnalysis.registerStateSpace(mainCall, mainStateSpace);
                interproceduralAnalysis.registerProofStructure(mainCall, proofStructure); 
            }
            
	        interproceduralAnalysis.runOnTheFly();
	    	
	    	// process model checking result
//	    	if (proofStructure.isSuccessful()) {
//	            if(mainStateSpace.containsAbortedStates()) {
//	                allSatisfied = false;
//	                formulaResults.put(formula, ModelCheckingResult.UNKNOWN);
//	                logger.info("done. It is unknown whether the formula is satisfied.");
//	            } else {
//	                formulaResults.put(formula, ModelCheckingResult.SATISFIED);
//	                logger.info("done. Formula is satisfied.");
//	                numberSatFormulae++;
//	            }
//	        } else {
//	            logger.info("Formula is violated: " + formula.getFormulaString());
//	            allSatisfied = false;
//	            formulaResults.put(formula, ModelCheckingResult.UNSATISFIED);
//	
//	            if (scene().options().isIndexedMode()) {
//	                logger.warn("Counterexample generation for indexed grammars is not supported yet.");
//	            } else {
//	                FailureTrace failureTrace = proofStructure.getFailureTrace(mainStateSpace);
//	                System.out.println(failureTrace.toString());
//	                traces.put(formula, failureTrace);
//	            }
//	        }
        }
    }
    
    @Override
    public List<ProcedureCall> getMainProcedureCalls() {
    	return mainProcedureCalls;
    }

    @Override
    public void logSummary() {

        logSum("+-------------------------+------------------+");
        logHighlight("| Generated states        | Number of states |");
        logSum("+-------------------------+------------------+");
        logSum(String.format("| w/ procedure calls      | %16d |",
                scene().getNumberOfGeneratedStates()));
        logSum(String.format("| w/o procedure calls     | %16d |",
                mainStateSpace.getStates().size()));
        logSum(String.format("| final states            | %16d |",
                mainStateSpace.getFinalStateIds().size()));
        logSum("+-------------------------+------------------+");
    }

    @Override
    public boolean isVerificationPhase() {

        return true;
    }

    @Override
    public StateSpace getStateSpace() {

        return mainStateSpace;
    }

	@Override
	public Map<StateSpace, ProcedureCall> getProcedureStateSpaces() {

		return interproceduralAnalysis.getStateSpaceToCallMap();
	}
	
	@Override
	public Map<ProgramState, ProcedureCall> getCallingStatesToCall() {

		return interproceduralAnalysis.getCallingStateToCall();
	}
}
