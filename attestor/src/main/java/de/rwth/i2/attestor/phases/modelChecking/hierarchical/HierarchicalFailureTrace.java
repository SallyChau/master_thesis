package de.rwth.i2.attestor.phases.modelChecking.hierarchical;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingTrace;
import de.rwth.i2.attestor.phases.symbolicExecution.stateSpaceGenerationImpl.InternalStateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class HierarchicalFailureTrace implements ModelCheckingTrace {
	
	private LinkedList<FailureTrace> globalStateTrace = new LinkedList<>();
	private LinkedList<List<Integer>> globalStateIdTrace = new LinkedList<>();
	private Iterator<FailureTrace> failureTraceIterator;
	
	private FailureTrace currentFailureTrace;
	
	public void addHierarchicalFailureTrace(HierarchicalFailureTrace trace) {
		
		globalStateTrace.addAll(trace.getStateTrace());
		globalStateIdTrace.add(trace.getStateIdTrace());
		failureTraceIterator = globalStateTrace.iterator();
	}
	
	public void addFailureTrace(FailureTrace trace) {

		globalStateTrace.addFirst(trace);
		globalStateIdTrace.addFirst(trace.getStateIdTrace());
		currentFailureTrace = trace;
		failureTraceIterator = globalStateTrace.iterator();
	}

	@Override
	public ProgramState getInitialState() {

		return globalStateTrace.getFirst().getInitialState();
	}

	@Override
	public ProgramState getFinalState() {

		return getTopLevelFailureTrace().getFinalState();
	}

	@Override
	public boolean hasNext() {
		
		if (!currentFailureTrace.hasNext()) {
			currentFailureTrace = failureTraceIterator.next();
		}

		return currentFailureTrace.hasNext();
	}

	@Override
	public ProgramState next() {
		
		if (!currentFailureTrace.hasNext()) {
			currentFailureTrace = failureTraceIterator.next();
		}

		return currentFailureTrace.next();
	}

	@Override
	public List<Integer> getStateIdTrace() {

		LinkedList<Integer> stateIdTrace = new LinkedList<>();
		for (List<Integer> idTrace : globalStateIdTrace) {
			for (int id : idTrace) {
				stateIdTrace.add(id);
			}
		}
		
		return stateIdTrace;
	}
	
	public List<FailureTrace> getStateTrace() {
		
		return new LinkedList<>(globalStateTrace);
	}
	
	public FailureTrace getTopLevelFailureTrace() {
		
		// find final state of trace that is a state in top-level state space
		// therefore, iterate list in reverse
		
		ListIterator<FailureTrace> traceIterator = globalStateTrace.listIterator(globalStateTrace.size());

		// Iterate in reverse.
		while(traceIterator.hasPrevious()) {
			FailureTrace current = traceIterator.previous();
			if (current.getFinalState().isFromTopLevelStateSpace()) return current;
		}
		
		return null;
	}
	
	private int size() {
		
		int size = 0;
		for (FailureTrace trace : globalStateTrace) {
			size += trace.getStateIdTrace().size();
		}
		
		return size;
	}

	@Override
	public StateSpace getStateSpace() {	
		
		StateSpace stateSpace = new InternalStateSpace(size());

        if(globalStateTrace.isEmpty()) {
            return stateSpace;
        }
        
        Iterator<FailureTrace> traceIterator = this.failureTraceIterator;
        
        while(traceIterator.hasNext()) {
        	
            FailureTrace currentTrace = traceIterator.next();

            ProgramState current = currentTrace.next();
            stateSpace.addInitialState(current);

	        while(currentTrace.hasNext()) {
	            ProgramState next = currentTrace.next();
	            stateSpace.addState(next);
	            stateSpace.addControlFlowTransition(current, next);
	            current = next;
	        }
        }

        stateSpace.setFinal(globalStateTrace.getLast().getFinalState());
        return stateSpace;
	}
}
