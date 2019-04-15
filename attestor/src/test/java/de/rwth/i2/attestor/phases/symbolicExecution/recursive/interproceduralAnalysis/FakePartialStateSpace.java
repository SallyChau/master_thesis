package de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis;

import java.util.List;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.modelChecking.onthefly.OnTheFlyProofStructure;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class FakePartialStateSpace implements PartialStateSpace {
	
	StateSpace stateSpaceBeforeContinuation;
	StateSpace stateSpaceAfterContinuation;
	
	boolean didContinue;

	public FakePartialStateSpace( StateSpace stateSpaceBeforeContinuation, 
								  StateSpace stateSpaceAfterContinuation) {
		this.stateSpaceBeforeContinuation = stateSpaceBeforeContinuation;
		this.stateSpaceAfterContinuation = stateSpaceAfterContinuation;
		
		this.didContinue = false;
	}

	@Override
	public void continueExecution(ProcedureCall call) {
		this.didContinue = true;

	}

	@Override
	public StateSpace unfinishedStateSpace() {
		if( didContinue ) {
			return stateSpaceAfterContinuation;
		}else {
			return stateSpaceBeforeContinuation;
		}
	}

	@Override
	public void continueExecutionOnTheFly(ProcedureCall call, List<Node> formulae, OnTheFlyProofStructure proofStructure) {
		// TODO Auto-generated method stub
		
	}
}
