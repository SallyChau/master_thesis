package de.rwth.i2.attestor.phases.symbolicExecution.hierarchical;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.AbstractPhase;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.communication.ModelCheckingSettings;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ProofStructure2;
import de.rwth.i2.attestor.phases.preprocessing.RSMGenerationPhase;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.StateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.transformers.InputTransformer;
import de.rwth.i2.attestor.phases.transformers.MCSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.ModelCheckingResultsTransformer;
import de.rwth.i2.attestor.phases.transformers.StateSpaceTransformer;
import de.rwth.i2.attestor.procedures.Method;
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
		
		System.out.println("Hierarchical State Space Generation Phase");
    	
    	// RSM
    	this.rsm = getPhase(RSMGenerationPhase.class).getRSM(); 
    	ComponentStateMachine mainCSM = rsm.getMainComponent();
    	mainMethod = mainCSM.getMethod();
    	
    	loadInitialStates();
    	
    	// INIT MAIN STATE SPACE
        
    	
    	// EXECUTE STATEMENT
    	
    	
    	// MODEL CHECKING
    	ModelCheckingSettings mcSettings = getPhase(MCSettingsTransformer.class).getMcSettings();
        Set<LTLFormula> formulae = mcSettings.getFormulae();
        if (formulae.isEmpty()) {
            logger.debug("No LTL formulae have been provided.");
            return;
        }

        // TODO do not generate state space again for each formula
        for (LTLFormula formula : formulae) {

        	String formulaString = formula.getFormulaString();
            logger.info("Checking formula: " + formulaString + "...");
            
        	ProofStructure2 proofStructure = new ProofStructure2();
        	
        	try {
        		// model check formula while generating state space on the fly
    			mainStateSpace = stateSpaceGeneratorFactory.create(mainMethod.getBody(), initialStates).generateAndCheck(formula, proofStructure);
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
                    traces.put(formula, failureTrace);
                }
            }
        }
    }

	@Override
	public StateSpace getStateSpace() {

		return mainStateSpace;
	}

	@Override
	public String getName() {
		
		return "Hierarchical State Space Generation";
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

}
