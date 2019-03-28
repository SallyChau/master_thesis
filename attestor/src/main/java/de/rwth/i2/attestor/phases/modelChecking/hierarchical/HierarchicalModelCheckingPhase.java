package de.rwth.i2.attestor.phases.modelChecking.hierarchical;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.main.AbstractPhase;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.communication.ModelCheckingSettings;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.HierarchicalProofStructure;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingTrace;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.phases.transformers.MCSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.ModelCheckingResultsTransformer;
import de.rwth.i2.attestor.phases.transformers.StateSpaceTransformer;
import de.rwth.i2.attestor.recursiveStateMachine.RecursiveStateMachine;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class HierarchicalModelCheckingPhase extends AbstractPhase implements ModelCheckingResultsTransformer {	
	
	Map<StateSpace, ProcedureCall> stateSpaceToAnalyzedCall;
	
	public HierarchicalModelCheckingPhase(Scene scene) {
		
		super(scene);
	}
	
	@Override
    public void executePhase() {

		// get state spaces from RecursiveStateSpaceGenerationPhase
		stateSpaceToAnalyzedCall = getPhase(StateSpaceTransformer.class).getProcedureStateSpaces();

        List<ProcedureCall> mainProcedureCalls = getPhase(StateSpaceTransformer.class).getMainProcedureCalls();
        
        // build RSM from state spaces
        RecursiveStateMachine rsm = new RecursiveStateMachine(stateSpaceToAnalyzedCall);
		
		// get model checking settings
		ModelCheckingSettings mcSettings = getPhase(MCSettingsTransformer.class).getMcSettings();
        Set<LTLFormula> formulae = mcSettings.getFormulae();
        if (formulae.isEmpty()) {
            logger.debug("No LTL formulae have been provided.");
            return;
        }        
        
        // TODO maybe pass whole list of calls to proof structure
        for (ProcedureCall call : mainProcedureCalls) {
        	
        	for (LTLFormula formula : formulae) {

	            HierarchicalProofStructure proofStructure = new HierarchicalProofStructure(rsm);
	            proofStructure.build(call, formula);
        	}
        }
        
    }

	@Override
	public String getName() {
		
		return "Hierarchical Model Checking";
	}

	@Override
	public void logSummary() {
		
	}

	@Override
	public boolean isVerificationPhase() {

		return true;
	}

	@Override
	public Map<LTLFormula, ModelCheckingResult> getLTLResults() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModelCheckingTrace getTraceOf(LTLFormula formula) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAllLTLSatisfied() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getNumberSatFormulae() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private StateSpace getStateSpace(ProcedureCall call) {
        for (Entry<StateSpace, ProcedureCall> entry : stateSpaceToAnalyzedCall.entrySet()) {
            if (Objects.equals(call, entry.getValue())) {
                return entry.getKey();
            }
        }
        
        return null;
    }
	
	
}
