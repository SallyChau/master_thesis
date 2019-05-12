package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.rwth.i2.attestor.phases.symbolicExecution.stateSpaceGenerationImpl.InternalStateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class FailureTrace implements ModelCheckingTrace {

    private final LinkedList<Integer> stateIdTrace = new LinkedList<>();
    private final LinkedList<ProgramState> stateTrace = new LinkedList<>();
    private Iterator<ProgramState> iterator;

    public FailureTrace(Assertion failureAssertion, StateSpace stateSpace) {

        Assertion current = failureAssertion;

        do {
            int stateId = current.getProgramState();
            stateIdTrace.addFirst(stateId);
            ProgramState state = stateSpace.getState(stateId);
            stateTrace.addFirst(state);
            current = current.getParent();
        } while (current != null);

        iterator = stateTrace.iterator();
    }
    
    public FailureTrace(Assertion2 failureAssertion, StateSpace stateSpace) {

        Assertion2 current = failureAssertion;

        do {
            int stateId = current.getProgramState().getStateSpaceId();
            stateIdTrace.addFirst(stateId);
            ProgramState state = stateSpace.getState(stateId);
            stateTrace.addFirst(state);
            current = current.getParent();
        } while (current != null);

        iterator = stateTrace.iterator();
    }

    @Override
    public List<Integer> getStateIdTrace() {

        return new LinkedList<>(stateIdTrace);
    }


    @Override
    public ProgramState getInitialState() {

        return stateTrace.getFirst();
    }

    @Override
    public ProgramState getFinalState() {

        return stateTrace.getLast();
    }

    public boolean isEmpty() {

        return stateIdTrace.isEmpty();
    }

    @Override
	public String toString() {

        return stateIdTrace.toString();
    }


    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public ProgramState next() {
        return iterator.next();
    }

    @Override
    public StateSpace getStateSpace() {


        StateSpace stateSpace = new InternalStateSpace(stateTrace.size());

        if(stateTrace.isEmpty()) {
            return stateSpace;
        }

        Iterator<ProgramState> iterator = stateTrace.iterator();

        ProgramState current = iterator.next();
        stateSpace.addInitialState(current);

        while(iterator.hasNext()) {
            ProgramState next = iterator.next();
            stateSpace.addState(next);
            stateSpace.addControlFlowTransition(current, next);
            current = next;
        }

        stateSpace.setFinal(stateTrace.getLast());
        return stateSpace;
    }
    
    @Override
    public boolean equals(Object trace) {
    	
    	if (this == trace) return true;
		if (trace == null) return false;
		if (!(trace instanceof FailureTrace)) return false;
		
		// check content
		FailureTrace traceTest = (FailureTrace) trace;
		LinkedList<ProgramState> states = traceTest.stateTrace;
		for (int i = 0; i < states.size(); i++) {
			if (!this.stateTrace.get(i).equals(states.get(i))) {
				return false;
			}
		}
		
		return true;
    }
}
