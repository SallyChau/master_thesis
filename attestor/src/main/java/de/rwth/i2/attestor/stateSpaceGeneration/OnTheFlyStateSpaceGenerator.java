package de.rwth.i2.attestor.stateSpaceGeneration;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProofStructure;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * An OnTheFlyStateSpaceGenerator takes an analysis and generates a
 * state space from it (analogously to a StateSpaceGenerator.
 * During state space generation model checking of an LTL formula
 * is performed on-the-fly. <br>
 * Initialization of an OnTheStateSpaceGenerator has to be performed
 * by calling the static method OnTheFlyStateSpaceGenerator.builder().
 * This yields a builder object to configure the analysis used during
 * state space generation.
 * <br>
 * The generation of a StateSpace and model checking are
 * started by invoking generateAndCheck().
 * 
 * @author chau
 *
 */
public class OnTheFlyStateSpaceGenerator extends StateSpaceGenerator {
 
    /**
     * The proof structure for LTL model checking of the generated StateSpace.
     */
    OnTheFlyProofStructure proofStructure;
    /**
     * The set of formulae that are demanded to hold for successors of final states 
     * (relevant for state spaces using this state space as a procedure).
     */
    Set<Node> resultFormulae;   
   
    

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
    
 
    
    /**
     * Attempts to model check LTL-formulae while generating the underlying state space on-the-fly.
     * @return The (incomplete) state space generated upon termination of model checking.
     * @throws StateSpaceGenerationAbortedException
     */
    public StateSpace generateAndCheck() throws StateSpaceGenerationAbortedException {
    	
    	while (proofStructure.isContinuable()) {
    		
    		Set<Node> formulae = proofStructure.buildAndGetNextFormulaeToCheck();    		
    		
    		// compute next states in order to continue the proof structure
    		if (!formulae.isEmpty()) {
    			
    			ProgramState programState = proofStructure.getLastCheckedState();    			
    			   			
    			if (isFinalState(programState)) {
    				setResultFormulae(formulae);
    			}    			
    			
    			Collection<ProgramState> successors = computeControlFlowSuccessors(programState, formulae);
    			
    			// if procedure model checking was executed during successor computation, check for results
    			Set<Node> resultFormulaeFromModelChecking = getResultFormulaeFromProcedureModelChecking(programState, formulae);
    			
    			if (!semanticsOf(programState).satisfiesFormulae(programState, formulae, scopeHierarchy)) {
    				
    				proofStructure.abort();
    				setResultFormulae(resultFormulaeFromModelChecking);
    			} else {

    				addSuccessorAssertionsToProofStructure(programState, successors, resultFormulaeFromModelChecking); 
    			}
    		} else {
    			if (proofStructure.isSuccessful()) setResultFormulaeToTrue();	
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
						
	    	state.setContainingStateSpace(this.stateSpace);
		}
			
		if (!checkAbortCriteria(state)) {
			return;
		} 
			
		SemanticsCommand stateSemanticsCommand = semanticsOf(state);
	    boolean isMaterialized = materializationPhase(stateSemanticsCommand, state) ;
	    if (isMaterialized) {
	    	Collection<ProgramState> successorStates = stateSemanticsCommand.computeSuccessorsAndCheck(state, formulae, scopeHierarchy);        
	        if (finalStateStrategy.isFinalState(state, successorStates, stateSemanticsCommand)) {
	            stateSpace.setFinal(state);
	            stateSpace.addArtificialInfPathsTransition(state); // Add self-loop to each final state
	        } else {
	            for (ProgramState nextState : successorStates) {
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
	private Set<Node> getResultFormulaeFromProcedureModelChecking(ProgramState state, Set<Node> formulae) {
		
		SemanticsCommand statement = semanticsOf(state);		
		return statement.getResultFormulae(state, formulae, scopeHierarchy);
	}

	private void addSuccessorAssertionsToProofStructure(ProgramState state, Collection<ProgramState> successors, Set<Node> formulae) {

		if (proofStructure.isSuccessful() && !formulae.isEmpty()) {
			for (ProgramState successorState : successors) { 				
				if (successorState.isFromTopLevelStateSpace()) {
					proofStructure.addAssertion(successorState, formulae);
				} else {
					// procedure calls: don't add assertions another time for final states of procedure state spaces
					if (!(isFinalState(successorState) && state.equals(successorState))) {
						proofStructure.addAssertion(successorState, formulae);
					}
				}
			}
		}		
	}

	private void setResultFormulae(Set<Node> formulae) {

		this.resultFormulae = formulae;		
	}

	private void setResultFormulaeToTrue() {
	
		LTLFormula ltlFormula = null;
		
		try {
			ltlFormula = new LTLFormula("true");
			ltlFormula.toPNF();
		} catch (Exception e) {
			e.printStackTrace();
		}        
	    
	    Set<Node> result = new HashSet<>();
	    result.add(ltlFormula.getASTRoot().getPLtlform());
	    
		setResultFormulae(result);		
	}

	private boolean isFinalState(ProgramState state) {
		
		return stateSpace.getFinalStates().contains(state);
	}
}
