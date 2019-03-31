package de.rwth.i2.attestor.phases.transformers;

import java.util.List;
import java.util.Map;

import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public interface StateSpaceTransformer {

    StateSpace getStateSpace();
	Map<StateSpace, ProcedureCall> getProcedureStateSpaces();
	List<ProcedureCall> getMainProcedureCalls();
	Map<ProgramState, ProcedureCall> getCallingStatesToCall();
}
