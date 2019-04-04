package de.rwth.i2.attestor.phases.modelChecking.hierarchical;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.main.AbstractPhase;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.communication.ModelCheckingSettings;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingTrace;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.phases.transformers.MCSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.ModelCheckingResultsTransformer;
import de.rwth.i2.attestor.phases.transformers.StateSpaceTransformer;
import de.rwth.i2.attestor.recursiveStateMachine.RecursiveStateMachine;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class HierarchicalModelCheckingPhase extends AbstractPhase implements ModelCheckingResultsTransformer {	
	
	private final Map<LTLFormula, ModelCheckingResult> formulaResults = new LinkedHashMap<>();
    private final Map<LTLFormula, HierarchicalFailureTrace> traces = new LinkedHashMap<>();
    private boolean allSatisfied = true;
    private int numberSatFormulae = 0;

    RecursiveStateMachine rsm;
	
	public HierarchicalModelCheckingPhase(Scene scene) {
		
		super(scene);
	}
	
	@Override
    public void executePhase() {
		
		// get model checking settings
		ModelCheckingSettings mcSettings = getPhase(MCSettingsTransformer.class).getMcSettings();
        Set<LTLFormula> formulae = mcSettings.getFormulae();
        if (formulae.isEmpty()) {
            logger.debug("No LTL formulae have been provided.");
            return;
        }

        buildRSM();
        	
        // start hierarchical model checking for one main procedure call 
        // other main procedure calls are implied by initial states that are already included in the state space
    	for (LTLFormula formula : formulae) {
    		
    		String formulaString = formula.getFormulaString();
    		logger.info("Checking formula: " + formulaString + "...");

    		ProcedureCall call = getPhase(StateSpaceTransformer.class).getMainProcedureCalls().get(0);
            boolean mcSuccessful = rsm.check(call, formula);
            
            System.err.println("Hierarchical Model Checking Phase successful: " + mcSuccessful);            
            if (rsm.getHierarchicalFailureTrace(call, formula) != null) {
            	System.err.println("Hierarchical Model Checking Phase: " + rsm.getHierarchicalFailureTrace(call, formula).getStateTrace());
            	System.err.println("Hierarchical Model Checking Phase: " + rsm.getHierarchicalFailureTrace(call, formula).getStateIdTrace());
            	System.err.println("Hierarchical Model Checking Phase: " + rsm.getHierarchicalFailureTrace(call, formula).getStateSpace());
            }
            
            if (mcSuccessful) {

                if(rsm.getOrCreateComponentStateMachine(call.getMethod()).getStateSpace(call.getInput(), null).containsAbortedStates()) {
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
                    HierarchicalFailureTrace failureTrace = rsm.getHierarchicalFailureTrace(call, formula);
                    traces.put(formula, failureTrace);
                }
            }
    	}        
    }
	
	private void buildRSM() {
		
		Map<StateSpace, ProcedureCall> stateSpaceToAnalyzedCall = getPhase(StateSpaceTransformer.class).getProcedureStateSpaces();
		Map<ProgramState, ProcedureCall> callingStatesToCall = getPhase(StateSpaceTransformer.class).getCallingStatesToCall();
        rsm = new RecursiveStateMachine(stateSpaceToAnalyzedCall, callingStatesToCall);		
	}

	@Override
	public String getName() {
		
		return "Hierarchical Model Checking";
	}

	@Override
	public void logSummary() {
		
		if (formulaResults.isEmpty()) {
            return;
        }

        if (allSatisfied) {
            logHighlight("Hierarchical model checking results: All provided LTL formulae are satisfied.");
        } else {
            logHighlight("Hierarchical model checking results: Some provided LTL formulae could not be verified.");
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
}
