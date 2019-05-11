package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyPartialStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.OnTheFlyStateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class FakeOnTheFlyPartialStateSpace extends OnTheFlyPartialStateSpace {

	StateSpace stateSpaceBeforeContinuation;
	StateSpace stateSpaceAfterContinuation;
	
	boolean didContinue;

	public FakeOnTheFlyPartialStateSpace(ProgramState callingState, OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory,
			StateSpace stateSpaceBeforeContinuation, StateSpace stateSpaceAfterContinuation) {
		
		super(callingState, stateSpaceGeneratorFactory);
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
}
