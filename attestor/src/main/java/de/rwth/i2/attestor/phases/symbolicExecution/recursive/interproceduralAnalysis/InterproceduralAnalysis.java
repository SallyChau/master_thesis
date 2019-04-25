package de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis;


import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

/**
 * This class is responsible of computing the fixpoint of the interprocedural analysis
 * in case there are recursive functions.
 * It keeps track of any procedure calls to recursive methods that have not yet been analysed.
 * Furthermore it stores the dependencies between partialStateSpaces and procedureCalls so that
 * it can continue those stateSpaces whenever it has found new contracts for a procedureCall.
 * 
 * see {@link ProcedureRegistry} for the interaction between the semantics and this class.
 * @author Hannah
 *
 */
public class InterproceduralAnalysis {

	protected Deque<ProcedureCall> remainingProcedureCalls = new ArrayDeque<>();
	protected Deque<PartialStateSpace> remainingPartialStateSpaces = new ArrayDeque<>();

	protected Map<ProcedureCall, Set<PartialStateSpace>> callingDependencies = new LinkedHashMap<>();
	protected Map<StateSpace, ProcedureCall> stateSpaceToAnalyzedCall = new LinkedHashMap<>();
	
	Map<ProgramState, ProcedureCall> callingStateToCall = new LinkedHashMap<>();	

	

	public void registerStateSpace(ProcedureCall call, StateSpace stateSpace) {

		stateSpaceToAnalyzedCall.put(stateSpace, call);
	}

	public void registerDependency(ProcedureCall procedureCall, PartialStateSpace dependentPartialStateSpace) {

		if(!callingDependencies.containsKey(procedureCall)) {
			Set<PartialStateSpace> dependencies = new LinkedHashSet<>();
			dependencies.add(dependentPartialStateSpace);
			callingDependencies.put(procedureCall, dependencies);
		} else {
			callingDependencies.get(procedureCall).add(dependentPartialStateSpace);
		}
	}
	
	public void registerCallingStates(ProgramState callingState, ProcedureCall call) {
		callingStateToCall.put(callingState, call);
	}

	public void registerProcedureCall(ProcedureCall procedureCall) {

		if(!remainingProcedureCalls.contains(procedureCall)) {
			remainingProcedureCalls.push(procedureCall);
		} 
	}
	
	public Map<StateSpace, ProcedureCall> getStateSpaceToCallMap() {
		
		return stateSpaceToAnalyzedCall;
	}
	
	public Map<ProgramState, ProcedureCall> getCallingStateToCall() {
		
		return callingStateToCall;
	}
	
	/**
	 * the fixpoint iteration
	 * (note that this routine may need to be stopped if it is not able to find
	 * a fixpoint. 
	 */
	public void run() {

		while(!remainingProcedureCalls.isEmpty() || !remainingPartialStateSpaces.isEmpty()) {
			ProcedureCall call;
			boolean contractChanged;
			if(!remainingProcedureCalls.isEmpty()) {
				// creates new state space for recursive methods
				call = remainingProcedureCalls.pop();
				StateSpace stateSpace = call.execute(); 
				contractChanged = stateSpace.getFinalStateIds().size() > 0;
			} else {
				// continue partial state space 
				PartialStateSpace partialStateSpace = remainingPartialStateSpaces.pop();
				
				int currentNumberOfFinalStates = partialStateSpace.unfinishedStateSpace().getFinalStateIds().size();
				call = stateSpaceToAnalyzedCall.get( partialStateSpace.unfinishedStateSpace() );
				partialStateSpace.continueExecution(call);
				int newNumberOfFinalsStates = partialStateSpace.unfinishedStateSpace().getFinalStateIds().size();
				contractChanged = newNumberOfFinalsStates > currentNumberOfFinalStates;
			}
			if( contractChanged ) {
				notifyDependencies(call);
			}
		}
	}

	/**
	 * enqueues the partial stateSpace depending on the given call for continued analysis.
	 * @param call the procedure call for which the contract has changed
	 * (i.e. for which more postconditions have been discovered)
	 */ 
	private void notifyDependencies(ProcedureCall call) {

		Set<PartialStateSpace> dependencies = callingDependencies.getOrDefault(call, Collections.emptySet());
		remainingPartialStateSpaces.addAll(dependencies);
	}
}
