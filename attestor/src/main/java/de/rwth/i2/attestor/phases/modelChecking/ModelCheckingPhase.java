package de.rwth.i2.attestor.phases.modelChecking;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.main.AbstractPhase;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.communication.ModelCheckingSettings;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.SimpleProofStructure;
import de.rwth.i2.attestor.phases.transformers.MCSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.ModelCheckingResultsTransformer;
import de.rwth.i2.attestor.phases.transformers.StateSpaceTransformer;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import gnu.trove.iterator.TIntIterator;

public class ModelCheckingPhase extends AbstractPhase implements ModelCheckingResultsTransformer {

    private final Map<LTLFormula, ModelCheckingResult> formulaResults = new LinkedHashMap<>();
    private final Map<LTLFormula, ModelCheckingTrace> traces = new LinkedHashMap<>();
    private boolean allSatisfied = true;

    private int numberSatFormulae = 0;

    public ModelCheckingPhase(Scene scene) {

        super(scene);
    }

    @Override
    public String getName() {

        return "Model checking";
    }

    @Override
    public void executePhase() {

        ModelCheckingSettings mcSettings = getPhase(MCSettingsTransformer.class).getMcSettings();
        Set<LTLFormula> formulae = mcSettings.getFormulae();
        if (formulae.isEmpty()) {
            logger.debug("No LTL formulae have been provided.");
            return;
        }

        StateSpace stateSpace = getPhase(StateSpaceTransformer.class).getStateSpace();        
        
        // get list of initial program states
        LinkedList<ProgramState> initialProgramStates = new LinkedList<>();
        TIntIterator initialStatesIterator = stateSpace.getInitialStateIds().iterator();
        while (initialStatesIterator.hasNext()) {
        	initialProgramStates.add(stateSpace.getState(initialStatesIterator.next()));
        } 
        
        // build proof structure for each formula
        for (LTLFormula formula : formulae) {
        	
            String formulaString = formula.getFormulaString();
            logger.info("Checking formula: " + formulaString + "...");
            
            SimpleProofStructure proofStructure = new SimpleProofStructure(stateSpace);
            proofStructure.build(initialProgramStates, formula);
            
            System.out.println("Model Checking: Proof Structure checked " + proofStructure.getNumberOfCheckedAssertions() + " assertions.");
            if (proofStructure.isSuccessful()) {

                if(stateSpace.containsAbortedStates()) {
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
                    FailureTrace failureTrace = proofStructure.getFailureTrace();
                    traces.put(formula, failureTrace);
                }
            }
        }
    }

    @Override
    public void logSummary() {

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
}
