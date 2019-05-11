package de.rwth.i2.attestor.stateSpaceGeneration;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.OnTheFlyProofStructure;
import de.rwth.i2.attestor.refinement.AutomatonStateLabelingStrategy;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * 
 * @author chau
 *
 */
public class OnTheFlyStateSpaceGenerator extends StateSpaceGenerator {
 
    /**
     * The proof structure for the program resulting from LTL model checking.
     */
    OnTheFlyProofStructure proofStructure;
    /**
     * The set of formulae that are demanded to hold for successors of final states 
     * (relevant for state spaces using this state space as a procedure).
     */
    Set<Node> resultFormulae;   
    /**
     * Contains heap out of scope. Important for AP labeling of states of method calls.
     */
    ScopedHeapHierarchy scopeHierarchy;
   
    

    protected OnTheFlyStateSpaceGenerator() {
    }

    /**
     * @return An OnTheFlyStateSpaceGeneratorBuilder which is the only means to create a new
     * OnTheFlyStateSpaceGenerator object.
     */
    public static OnTheFlyStateSpaceGeneratorBuilder ontheflyBuilder() {

        return new OnTheFlyStateSpaceGeneratorBuilder();
    }

    public static OnTheFlyStateSpaceGeneratorBuilder builder(OnTheFlyStateSpaceGenerator stateSpaceGenerator) {

        return new OnTheFlyStateSpaceGeneratorBuilder()
                .setAbortStrategy(stateSpaceGenerator.getAbortStrategy())
                .setCanonizationStrategy(stateSpaceGenerator.getCanonizationStrategy())
                .setStateRectificationStrategy(stateSpaceGenerator.getStateRectificationStrategy())
                .setMaterializationStrategy(stateSpaceGenerator.getMaterializationStrategy().getHeapStrategy())
                .setStateLabelingStrategy(stateSpaceGenerator.getStateLabelingStrategy())
                .setStateRefinementStrategy(stateSpaceGenerator.getStateRefinementStrategy())
                .setStateExplorationStrategy(stateSpaceGenerator.getStateExplorationStrategy())
                .setStateSpaceSupplier(stateSpaceGenerator.getStateSpaceSupplier())
                .setStateCounter(stateSpaceGenerator.getTotalStatesCounter())
                .setFinalStateStrategy(stateSpaceGenerator.getFinalStateStrategy())
                .setAlwaysCanonicalize(stateSpaceGenerator.isAlwaysCanonicalize())
                .setPostProcessingStrategy(stateSpaceGenerator.getPostProcessingStrategy())
                .setProofStructure(stateSpaceGenerator.getProofStructure())
                .setScopeHierarchy(stateSpaceGenerator.getScopeHierarchy());
    }

    

	public OnTheFlyProofStructure getProofStructure() {

        return proofStructure;
    }
    
    public Set<Node> getResultFormulae() {

        return resultFormulae;
    }
    
    public ScopedHeapHierarchy getScopeHierarchy() {
    	
    	return scopeHierarchy;
    }
    
 
    
    /**
     * Attempts to model check LTL-formulae while generating the underlying state space on-the-fly.
     * @return The (incomplete) state space generated upon termination of model checking.
     * @throws StateSpaceGenerationAbortedException
     */
    public StateSpace generateAndCheck() throws StateSpaceGenerationAbortedException {
    	
    	System.out.println("StateSpaceGenerator: Start state space generation...");
    	System.out.println("StateSpaceGenerator: state space " + stateSpace);
    	
    	while (proofStructure.isContinuable()) {
    		
    		Set<Node> formulae = proofStructure.buildAndGetNextFormulaeToCheck();    		
    		System.out.println("StateSpaceGenerator: Received next formulae to check: " + formulae);
    		
    		// compute next assertions in order to continue the proof structure
    		if (formulae != null) {
    			
    			ProgramState programState = proofStructure.getLastCheckedState();    			
    			System.out.println("StateSpaceGenerator: Current state: " + programState.getStateSpaceId());
    			   			
    			if (isFinalState(programState)) {
    				System.out.println("StateSpaceGenerator: Setting result formulae for this state space: " + formulae);
    				setResultFormulae(formulae);
    			}    			
    			
    			Collection<ProgramState> successors = computeControlFlowSuccessors(programState, formulae);
    			
    			// if procedure model checking was executed during successor computation, check for results
    			formulae = postprocessProcedureCall(programState, formulae);  			

    			addSuccessorAssertionsToProofStructure(programState, successors, formulae);    				
    		} 
		}  	
    	
    	postProcessingStrategy.process(stateSpace);
        totalStatesCounter.addStates(stateSpace.size());
    	return stateSpace;
    }

	/**
	 * Compute the control flow successor states of state skipping materialization steps (relevant for model checking).
	 * @param state The state for which the control flow successor states shall be computed.
	 * @return The control flow successor states of state (skipping materialization steps).
	 * @throws StateSpaceGenerationAbortedException
	 */
	public Collection<ProgramState> computeControlFlowSuccessors(ProgramState state, Set<Node> formulae) throws StateSpaceGenerationAbortedException {      	
		
		System.out.println("StateSpaceGenerator: generate state in order to get control flow successors");
		generateState(state, formulae);
		
		StateSpace containingStateSpace = state.getContainingStateSpace();
		int stateId = state.getStateSpaceId();
		
		Set<ProgramState> result = new HashSet<>();
		TIntSet successors = new TIntHashSet(100);
		
		// Collect the "real" successor states (i.e. skipping materialization steps)
		TIntArrayList materializationSuccessorIds = containingStateSpace.getMaterializationSuccessorsIdsOf(stateId);
		if (!materializationSuccessorIds.isEmpty()) {
			TIntIterator materializationStateIterator = materializationSuccessorIds.iterator();
			while (materializationStateIterator.hasNext()) {
				// Every materialization state is followed by a control flow state
				int materializationState = materializationStateIterator.next();
				// generate materialization state in order to get control flow successors
				System.out.println("StateSpaceGenerator: generate MATERIALIZATION state in order to get control flow successors");
				generateState(containingStateSpace.getState(materializationState), formulae);
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
		for (ProgramState successorState : result) {			
			System.out.println("StateSpaceGenerator: generate SUCCESSOR state in order to get control flow successors");
			generateState(successorState, formulae);
		}
		
		return result;
	}

	/**
	 * Generates a single state (adds it to the state space if it has not been added yet).
	 * In case state invokes a procedure call, formulae is set as LTL formulae to the method executor for future model checking.
	 * @param state The state to be generated.
	 * @param formulae The LTL formulae to be model checked for state.
	 * @throws StateSpaceGenerationAbortedException
	 */
	private void generateState(ProgramState state, Set<Node> formulae) throws StateSpaceGenerationAbortedException {

		if (state.getContainingStateSpace() == null || state.isContinueState()) {
			
			System.out.println("StateSpaceGenerator: Generating state " + state.getStateSpaceId());
			
	    	state.setContainingStateSpace(this.stateSpace);
			
			if(!checkAbortCriteria(state)) {
				totalStatesCounter.addStates(stateSpace.size());
				return;
			} 
		}
			
		SemanticsCommand stateSemanticsCommand = semanticsOf(state);
	    boolean isMaterialized = materializationPhase(stateSemanticsCommand, state) ;
	    if(isMaterialized) {
	    	System.out.println("StateSpaceGenerator: Computing successors for " + state.getStateSpaceId() + " and formulae " + formulae);
	    	System.out.println("StateSpaceGenerator: Current statement: " + stateSemanticsCommand);
	    	Collection<ProgramState> successorStates = stateSemanticsCommand.computeSuccessorsAndCheck(state, formulae, scopeHierarchy);        
	        if(finalStateStrategy.isFinalState(state, successorStates, stateSemanticsCommand)) {
	            stateSpace.setFinal(state);
	            stateSpace.addArtificialInfPathsTransition(state); // Add self-loop to each final state
	        } else {
	            for(ProgramState nextState : successorStates) {
	                handleSuccessorState(state, nextState); // Add unexplored states to exploration strategy
	            }
	        }
	    }
	}

	/**
	 * After executing a procedure call, check for updates for this state space resulting from
	 * model checking a procedure.
	 * @param state The program state that invoked a procedure call.
	 * @param formulae The formulae that have been checked during execution of the procedure.
	 * @return the formulae resulting from model checking the called procedure.
	 */
	private Set<Node> postprocessProcedureCall(ProgramState state, Set<Node> formulae) {
		
		SemanticsCommand statement = semanticsOf(state);
		System.out.println("StateSpaceGenerator: Current statement: " + statement);
		if (!statement.satisfiesFormulae(state, formulae, scopeHierarchy)) {
			// mainly for non-recursive calls
			System.err.println("StateSpaceGenerator: Aborting proof structure for statespace " + stateSpace);
			proofStructure.abort();
		} else {
	
			// if procedure model checking was executed, get resulting LTL formulae
			Set<Node> resultFormulaeFromCall = statement.getResultFormulae(state, formulae, scopeHierarchy);
			System.out.println("StateSpaceGenerator: Result formulae: statement: " + statement);
			System.out.println("StateSpaceGenerator: Result formulae from procedure call? " + resultFormulaeFromCall);
			if (resultFormulaeFromCall != null && !resultFormulaeFromCall.isEmpty()) {
	
				System.out.println("StateSpaceGenerator: current next formulae updated: " + resultFormulaeFromCall);  
				return resultFormulaeFromCall;
			}	
		}		
	
		System.out.println("StateSpaceGenerator: current next formulae stayed the same: " + formulae);  
		return formulae;
	}

	private void addSuccessorAssertionsToProofStructure(ProgramState state, Collection<ProgramState> successors, Set<Node> formulae) {
		
		if (proofStructure.isSuccessful()) {
			for (ProgramState successorState : successors) { 
				if (!(isFinalState(successorState) && state.equals(successorState) && !successorState.isFromTopLevelStateSpace())) {
					proofStructure.addAssertion(successorState, formulae);
				}
			}
		}		
	}

	@Override
	protected void labelWithAtomicPropositions(ProgramState state) {
	
	    if(state.isFromTopLevelStateSpace()) {
	        stateLabelingStrategy.computeAtomicPropositions(state);
	    } else {
	    	if (stateLabelingStrategy instanceof AutomatonStateLabelingStrategy) {        
	        	System.out.println("StateSpaceGenerator: adding APs for state in statespace " + stateSpace);
	        	AutomatonStateLabelingStrategy labelingStrategy = (AutomatonStateLabelingStrategy) stateLabelingStrategy;
	        	labelingStrategy.computeAtomicPropositionsFromGlobalHeap(state, scopeHierarchy);
	    	}
	    }
	}

	private void setResultFormulae(Set<Node> formulae) {
		
		this.resultFormulae = formulae;		
	}

	private boolean isFinalState(ProgramState state) {
		
		return stateSpace.getFinalStates().contains(state);
	}
}
