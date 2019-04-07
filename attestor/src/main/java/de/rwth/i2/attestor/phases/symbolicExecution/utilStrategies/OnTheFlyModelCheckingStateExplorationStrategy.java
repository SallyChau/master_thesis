package de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.modelChecking.onthefly.OnTheFlyProofStructure;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;
import de.rwth.i2.attestor.stateSpaceGeneration.StateExplorationStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class OnTheFlyModelCheckingStateExplorationStrategy implements StateExplorationStrategy {
	
	private LinkedList<ProgramState> unexploredStates = new LinkedList<>();
	
	private OnTheFlyProofStructure proofStructure = new OnTheFlyProofStructure();
	private List<Node> inputFormulae;
	private List<Node> currentFormulae;
	private Map<ProgramState, List<Node>> stateToFormulae = new LinkedHashMap<>();

	@Override
	public boolean hasUnexploredStates() {

		return !unexploredStates.isEmpty();
	}

	@Override
	public ProgramState getNextUnexploredState() {

		return unexploredStates.removeLast();
	}

	@Override
	public void addUnexploredState(ProgramState state, boolean isMaterializedState) {
		
		unexploredStates.addLast(state);		
	}

	@Override
	public void removeUnexploredState(ProgramState state) {
		
		if (unexploredStates.contains(state)) {
			unexploredStates.remove(state);
		}			
	}

	@Override
	public boolean containsState(ProgramState state) {
		
		return unexploredStates.contains(state);
	}
	
	/**
     * Get successor states of a state relevant for model checking (skipping materialization steps).
     * @param state
     * @return
     * @throws StateSpaceGenerationAbortedException
     */
    private List<ProgramState> computeModelCheckingSuccessorStates(ProgramState state, List<Node> formulae) throws StateSpaceGenerationAbortedException {      	
	   
    	StateSpace containingStateSpace = state.getContainingStateSpace();
		int stateId = state.getStateSpaceId();
		
		LinkedList<ProgramState> result = new LinkedList<>();
		TIntSet successors = new TIntHashSet(100);
		
		// Collect the "real" successor states (i.e. skipping materialization steps)
		TIntArrayList materializationSuccessorIds = containingStateSpace.getMaterializationSuccessorsIdsOf(stateId);
		if (!materializationSuccessorIds.isEmpty()) {
			TIntIterator materializationStateIterator = materializationSuccessorIds.iterator();
			while (materializationStateIterator.hasNext()) {
				// Every materialization state is followed by a control flow state
				int materializationState = materializationStateIterator.next();
				// generate materialization state in order to get control flow successors
				//generateState(containingStateSpace.getState(materializationState), formulae);
				// TODO mat states might not have been generated yet
				TIntArrayList controlFlowSuccessorIds = containingStateSpace.getControlFlowSuccessorsIdsOf(materializationState);
				assert (!controlFlowSuccessorIds.isEmpty());
				successors.addAll(controlFlowSuccessorIds);
			}
		} else {
			successors.addAll(containingStateSpace.getControlFlowSuccessorsIdsOf(stateId));			
			successors.addAll(containingStateSpace.getArtificialInfPathsSuccessorsIdsOf(stateId)); // In case the state is final
		}

		TIntIterator successorsIterator = successors.iterator();
		while (successorsIterator.hasNext()) {
			result.add(containingStateSpace.getState(successorsIterator.next()));
		}

		// generate successor states in state space (if they have not been added to state space yet)
//		for (ProgramState successorState : result) {			
//			generateState(successorState, formulae);
//		}
		
		return result;
    }
    
    @Override
    public void setFormulae(List<Node> formulae) {

		this.currentFormulae = formulae;
    }

	@Override
	public void check(ProgramState state) {
		
//		if (state.isContinueState()) {
//        	List<ProgramState> successors = computeModelCheckingSuccessorStates(state, formulae);
//        	for (ProgramState successorState : successors) {    	    		
//	    		proofStructure.addAssertion(successorState, formulae);
//			}	
//        } else {
//            proofStructure.addAssertion(state, formulae);
//        }	
		
		
		proofStructure.addAssertion(state, currentFormulae);
		
		// start model checking and get next formulae to check
		currentFormulae = proofStructure.build();
		
		ProgramState lastCheckedState = proofStructure.getLastCheckedState();
		
		// compute next states for model checking
		List<ProgramState> successors = new LinkedList<>();
		try {
			successors = computeModelCheckingSuccessorStates(lastCheckedState, currentFormulae);
		} catch (StateSpaceGenerationAbortedException e) {
			e.printStackTrace();
		}
		
		// get resulting LTL formulae from state space generation of procedure call (used nextFormulae to check procedure call)
		// null if command was not a procedure call
//		List<Node> outputFormulae = semanticsOf(lastCheckedState).getResultFormulae(lastCheckedState, currentFormulae);
//		if (outputFormulae != null) {
//			currentFormulae = outputFormulae;
//		}
		
		
		
		// add next assertions to proof structure
		for (ProgramState successorState : successors) {    	
			// add successors to queue
			addUnexploredState(successorState, false);
//    		proofStructure.addAssertion(successorState, currentFormulae);
		}	
		
	}
	
	private SemanticsCommand semanticsOf(ProgramState state) {

//        return program.getStatement(state.getProgramCounter());
		return null;
    }
}
