package de.rwth.i2.attestor.recursiveStateMachine;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

// TODO maybe do some generator stuff pattern thingy
public class RecursiveStateMachine {

	private Map<Method, ComponentStateMachine> components;
	private Map<StateSpace, ProcedureCall> stateSpaceToAnalyzedCall;
	private Map<ProgramState, ProcedureCall> callingStatesToCall;
	
	public RecursiveStateMachine(Map<StateSpace, ProcedureCall> stateSpaceToAnalyzedCall, Map<ProgramState, ProcedureCall> callingStatesToCall) {
		
		this.stateSpaceToAnalyzedCall = stateSpaceToAnalyzedCall;	
		this.callingStatesToCall = callingStatesToCall;
		this.components = new LinkedHashMap<>();
		build();
	}
	
	private void build() {
		
		// create Component State Machines
		Collection<ProcedureCall> procedureCalls = stateSpaceToAnalyzedCall.values();
		for (ProcedureCall call : procedureCalls) {
			
			Method method = call.getMethod();
			StateSpace stateSpace = getStateSpace(call);
			ProgramState callingState = call.getInput();
			
			ComponentStateMachine csm = getOrCreateComponentStateMachine(method);
			csm.addStateSpace(callingState, stateSpace); 
			
			components.put(method, csm);			
		}
		
		// translate procedure calls to CSM boxes
		Set<ProgramState> callingStates = callingStatesToCall.keySet();
		for (ProgramState state : callingStates) {
			StateSpace stateSpace = state.getContainingStateSpace();
			ComponentStateMachine caller = getComponentStateMachine(stateSpace);
			ComponentStateMachine callee = getOrCreateComponentStateMachine(callingStatesToCall.get(state).getMethod());
			caller.addCalledCSM(state, callee);
		}		
	}
	
	public ComponentStateMachine getComponentStateMachine(String signature) {
		
		for (ComponentStateMachine csm : components.values()) {
			if (csm.getMethod().getSignature().equals(signature)) {
				return csm;
			}
		}
		
		return null;
	}
	
	public ComponentStateMachine getComponentStateMachine(StateSpace stateSpace) {
		
		for (ComponentStateMachine csm : components.values()) {
			if (csm.getStateSpaces().contains(stateSpace)) {
				return csm;
			}
		}
		
		return null;
	}

	public ComponentStateMachine getOrCreateComponentStateMachine(Method method) {
		
		if(components.containsKey(method)) {
            return components.get(method);
        } else {
            ComponentStateMachine result = new ComponentStateMachine(method);
            components.put(method, result);
            return result;
        }
	}	
	
	public Collection<ComponentStateMachine> getComponentStateMachines() {
		
		return components.values();
	}
	
	private StateSpace getStateSpace(ProcedureCall call) {
        for (Entry<StateSpace, ProcedureCall> entry : stateSpaceToAnalyzedCall.entrySet()) {
            if (Objects.equals(call, entry.getValue())) {
                return entry.getKey();
            }
        }
        
        return null;
    }
	
	@Override
	public String toString() {
		
		return "RSM  with " + components.size();
	}
}
