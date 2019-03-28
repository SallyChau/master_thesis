package de.rwth.i2.attestor.recursiveStateMachine;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.HierarchicalProofStructure;
import de.rwth.i2.attestor.procedures.AbstractMethodExecutor;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;
import de.rwth.i2.attestor.procedures.ScopedHeap;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class ComponentStateMachine {
	
	private Method method;
	private String signature;
	
	private Map<ProgramState, StateSpace> inputStateToStateSpace;
	
	private Map<StateSpace, Set<HierarchicalProofStructure>> stateSpaceToProofStructures;
	
	public ComponentStateMachine(Method method) {

		this.method = method;
		this.signature = method.getSignature();
		
		this.inputStateToStateSpace = new LinkedHashMap<>();
		this.stateSpaceToProofStructures = new LinkedHashMap<>();
	}
	
	/**
	 * Get state space of CSM whose input heap matches the calling state.
	 * @param callingState
	 * @param statement
	 * @return the state space
	 */
	public final StateSpace getCalledStateSpace(ProgramState callingState, SemanticsCommand statement) {

		StateSpace result = null;
		
		AbstractMethodExecutor executor = (AbstractMethodExecutor) method.getMethodExecutor();
		ScopeExtractor scopeExtractor = executor.getScopeExtractor();
		
        HeapConfiguration inputHeap = statement.prepareHeap(callingState).getHeap();
        ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
	    HeapConfiguration heapInScope = scopedHeap.getHeapInScope();
	    
	    for (ProgramState inputState : inputStateToStateSpace.keySet()) {
			if (inputState.getHeap().equals(heapInScope)) {
				result = inputStateToStateSpace.get(inputState);
			}
		}
	    
	    return result;
	}
	
	public void addStateSpace(ProgramState inputState, StateSpace stateSpace) {
		
		inputStateToStateSpace.put(inputState, stateSpace);
	}
	
	public StateSpace getStateSpace(ProgramState inputState) {		
		
		return inputStateToStateSpace.get(inputState);
	}
	
	public Collection<StateSpace> getStateSpaces() {
		
		return inputStateToStateSpace.values();
	}
	
	public SemanticsCommand getSemanticsCommand(ProgramState programState) {
		
		return method.getBody().getStatement(programState.getProgramCounter());
	}
	
	public String getSignature() {
		return signature;
	}
	
	public Method getMethod() {
		return this.method;
	}
	
	@Override
	public String toString() {
		
		return getSignature();
	}
}
