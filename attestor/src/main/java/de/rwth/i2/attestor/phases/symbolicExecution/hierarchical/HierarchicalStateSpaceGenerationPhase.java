package de.rwth.i2.attestor.phases.symbolicExecution.hierarchical;

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
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.communication.ModelCheckingSettings;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ProofStructure2;
import de.rwth.i2.attestor.phases.preprocessing.RSMGenerationPhase;
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
import de.rwth.i2.attestor.phases.transformers.InputTransformer;
import de.rwth.i2.attestor.phases.transformers.MCSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.ModelCheckingResultsTransformer;
import de.rwth.i2.attestor.phases.transformers.StateSpaceTransformer;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.MethodExecutor;
import de.rwth.i2.attestor.procedures.PreconditionMatchingStrategy;
import de.rwth.i2.attestor.recursiveStateMachine.ComponentStateMachine;
import de.rwth.i2.attestor.recursiveStateMachine.RecursiveStateMachine;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;

public class HierarchicalStateSpaceGenerationPhase extends AbstractPhase implements StateSpaceTransformer, ModelCheckingResultsTransformer {
	
	private final StateSpaceGeneratorFactory stateSpaceGeneratorFactory;
	
	Method mainMethod;
	private List<ProgramState> initialStates; // states before any statement has been executed (independent from code)
	private StateSpace mainStateSpace;
	private InterproceduralAnalysis interproceduralAnalysis;
	
	private RecursiveStateMachine rsm;
	
	private final Map<LTLFormula, ModelCheckingResult> formulaResults = new LinkedHashMap<>();
    private final Map<LTLFormula, ModelCheckingTrace> traces = new LinkedHashMap<>();
    private boolean allSatisfied = true;

    private int numberSatFormulae = 0;
	
	public HierarchicalStateSpaceGenerationPhase(Scene scene) {
		
		super(scene);
		stateSpaceGeneratorFactory = new StateSpaceGeneratorFactory(scene);
	}
	
	@Override
    public void executePhase() {
    	
    	// RSM
    	this.rsm = getPhase(RSMGenerationPhase.class).getRSM(); 
    	ComponentStateMachine mainCSM = rsm.getMainComponent();
    	mainMethod = mainCSM.getMethod();
    	interproceduralAnalysis = new InterproceduralAnalysis();
    	loadInitialStates();
    	
    	initializeMethodExecutors();
    	
    	// MODEL CHECKING
    	ModelCheckingSettings mcSettings = getPhase(MCSettingsTransformer.class).getMcSettings();
        Set<LTLFormula> mcFormulae = mcSettings.getFormulae();
        if (mcFormulae.isEmpty()) {
            logger.debug("No LTL formulae have been provided.");
            return;
        }

        // TODO do not generate state space again for each formula
        for (LTLFormula formula : mcFormulae) {

        	String formulaString = formula.getFormulaString();
            logger.info("Checking formula: " + formulaString + "...");
            
            LinkedList<Node> formulae = new LinkedList<>();
            formulae.add(formula.getASTRoot().getPLtlform());
        	ProofStructure2 proofStructure = new ProofStructure2();
        	
        	try {
        		// model check formula while generating state space on the fly
    			mainStateSpace = stateSpaceGeneratorFactory.create(mainMethod.getBody(), initialStates).generateAndCheck(formulae, proofStructure);
    		} catch (StateSpaceGenerationAbortedException e) {

    			e.printStackTrace();
    		}
        	
        	// process model checking result
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
                logger.info("Formula is violated: " + formulaString);
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
        
        registerMainProcedureCalls();
        interproceduralAnalysis.run();

        if(mainStateSpace.getFinalStateIds().isEmpty()) {
            logger.error("Computed state space contains no final states.");
        }
    }

	@Override
	public StateSpace getStateSpace() {

		return mainStateSpace;
	}

	@Override
	public String getName() {
		
		return "Hierarchical Model Checking";
	}

	@Override
	public void logSummary() {
		
		logSum("+-------------------------+------------------+");
		logSum("+---- Hierarchical Model Checking Results ---+");
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
            logHighlight("Model checking results: All provided LTL formulae are satisfied.");
        } else {
            logHighlight("Model checking results: Some provided LTL formulae could not be verified.");
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
            StateSpace mainStateSpace = iState.getContainingStateSpace();
            ProcedureCall mainCall = new InternalProcedureCall(mainMethod, iState.getHeap(), stateSpaceGeneratorFactory, null);
            interproceduralAnalysis.registerStateSpace(mainCall, mainStateSpace);
        }
    }
}
