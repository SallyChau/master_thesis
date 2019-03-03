package de.rwth.i2.attestor.stateSpaceGeneration;


import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ProofStructure2;
import de.rwth.i2.attestor.recursiveStateMachine.RecursiveStateMachine;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * A StateSpaceGenerator takes an analysis and generates a
 * state space from it. <br>
 * Initialization of a StateSpaceGenerator has to be performed
 * by calling the static method StateSpaceGenerator.builder().
 * This yields a builder object to configure the analysis used during
 * state space generation.
 * <br>
 * The generation of a StateSpace itself is started by invoking generate().
 *
 * @author christoph
 */
public class StateSpaceGenerator {

    @FunctionalInterface
    public interface TotalStatesCounter {
        void addStates(int states);
    }

    /**
     * Stores the state space generated upon instantiation of
     * this generator.
     */
    StateSpace stateSpace;
    /**
     * The control flow of the program together with the
     * abstract semantics of each statement
     */
    Program program;
    /**
     * 
     */
    RecursiveStateMachine rsm;
    /**
     * Strategy guiding the materialization of states.
     * This strategy is invoked whenever an abstract transfer
     * function cannot be executed.
     */
    StateMaterializationStrategy materializationStrategy;
    /**
     * Strategy guiding the canonicalization of states.
     * This strategy is invoked after execution of abstract transfer
     * functions in order to generalize.
     */
    StateCanonicalizationStrategy canonicalizationStrategy;
    /**
     * strategy to rectify a state
      */
    StateRectificationStrategy stateRectificationStrategy;
    /**
     * Strategy determining when to give up on further state space
     * exploration.
     */
    AbortStrategy abortStrategy;
    /**
     * Strategy determining the labels of states in the state space
     */
    StateLabelingStrategy stateLabelingStrategy;
    /**
     * Strategy determining how states are refined prior to canonicalization
     */
    StateRefinementStrategy stateRefinementStrategy;
    /**
     * Strategy determining how states are explored before being added to the state space
     */
    StateExplorationStrategy stateExplorationStrategy;
    /**
     * Strategy determining post-processing after termination of state space generation
     */
    PostProcessingStrategy postProcessingStrategy;
    /**
     * Counter for the total number of states generated so far.
     */
    TotalStatesCounter totalStatesCounter;
    /**
     * Functional interface to obtain instances of state spaces.
     */
    StateSpaceSupplier stateSpaceSupplier;
    /**
     * Functional interface to check whether a state has to be marked as final
     */
    FinalStateStrategy finalStateStrategy;

    boolean alwaysCanonicalize = false;

    protected StateSpaceGenerator() {
    }

    /**
     * @return An StateSpaceGeneratorBuilder which is the only means to create a new
     * StateSpaceGenerator object.
     */
    public static StateSpaceGeneratorBuilder builder() {

        return new StateSpaceGeneratorBuilder();
    }

    public static StateSpaceGeneratorBuilder builder(StateSpaceGenerator stateSpaceGenerator) {

        return new StateSpaceGeneratorBuilder()
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
                .setPostProcessingStrategy(stateSpaceGenerator.getPostProcessingStrategy());
    }

    public boolean isAlwaysCanonicalize() {
        return alwaysCanonicalize;
    }

    /**
     * @return The strategy determining when state space generation is aborted.
     */
    public AbortStrategy getAbortStrategy() {

        return abortStrategy;
    }

    /**
     * @return The strategy determining how materialization is performed.
     */
    public StateMaterializationStrategy getMaterializationStrategy() {

        return materializationStrategy;
    }

    /**
     * @return The strategy determining how canonicalization is performed.
     */
    public StateCanonicalizationStrategy getCanonizationStrategy() {

        return canonicalizationStrategy;
    }

    /**
     * @return The strategy used to rectify states.
     */
    public StateRectificationStrategy getStateRectificationStrategy() {

        return stateRectificationStrategy;
    }

    /**
     * @return The strategy determining how states are labeled with atomic propositions.
     */
    public StateLabelingStrategy getStateLabelingStrategy() {

        return stateLabelingStrategy;
    }

    /**
     * @return The strategy determining how states are refined prior to canonicalization.
     */
    public StateRefinementStrategy getStateRefinementStrategy() {

        return stateRefinementStrategy;
    }

    public StateSpaceSupplier getStateSpaceSupplier() {

        return stateSpaceSupplier;
    }

    public StateExplorationStrategy getStateExplorationStrategy() {

        return stateExplorationStrategy;
    }

    public PostProcessingStrategy getPostProcessingStrategy() {

        return postProcessingStrategy;
    }

    public StateSpace getStateSpace() {

        return stateSpace;
    }

    public TotalStatesCounter getTotalStatesCounter() {

        return totalStatesCounter;
    }

    public FinalStateStrategy getFinalStateStrategy() {
        return finalStateStrategy;
    }
    
    public RecursiveStateMachine getRecursiveStateMachine() {
    	return rsm;
    }

    /**
     * Attempts to generate a StateSpace according to the
     * underlying analysis.
     *
     * @return The generated StateSpace.
     */
    public StateSpace generate() throws StateSpaceGenerationAbortedException {

        while (stateExplorationStrategy.hasUnexploredStates()) {

            ProgramState state = stateExplorationStrategy.getNextUnexploredState();
            state.setContainingStateSpace( this.stateSpace );

            if(!checkAbortCriteria(state)) {
                totalStatesCounter.addStates(stateSpace.size());
                return stateSpace;
            }

            SemanticsCommand stateSemanticsCommand = semanticsOf(state);

            boolean isMaterialized = materializationPhase(stateSemanticsCommand, state) ;
            if(isMaterialized) {
                Collection<ProgramState> successorStates = stateSemanticsCommand.computeSuccessors(state);
                if(finalStateStrategy.isFinalState(state, successorStates, stateSemanticsCommand)) {
                    stateSpace.setFinal(state);
                    stateSpace.addArtificialInfPathsTransition(state); // Add self-loop to each final state
                } else {
                    for(ProgramState nextState : successorStates) {
                        handleSuccessorState(state, nextState);
                    }
                }
            }
        }

        postProcessingStrategy.process(stateSpace);
        totalStatesCounter.addStates(stateSpace.size());
        return stateSpace;
    }
    
    public StateSpace generateAndCheck(LTLFormula formula, ProofStructure2 proofStructure) throws StateSpaceGenerationAbortedException {
    	
    	// Initialize proof structure with assertions for initial states
    	Set<ProgramState> initialStates = stateSpace.getInitialStates();
    	LinkedList<Node> formulae = new LinkedList<>();
    	formulae.add(formula.getASTRoot().getPLtlform());
    	for (ProgramState initialState : initialStates) {
            proofStructure.addAssertion(initialState, formulae);
    	}    	    
    	
    	while (proofStructure.getNextStateToCheck() != null) {
    		
    		// start model checking and get next formulae to check
    		LinkedList<Node> nextFormulae = proofStructure.build();
    		
    		// get successor states of last checked program state in MC
    		if (nextFormulae != null) {
    			ProgramState lastCheckedState = proofStructure.getLastCheckedState();
    			
    			// explore state if it hasn't been explored yet
    			exploreState(lastCheckedState);
    			
    			LinkedList<ProgramState> successorStates = computeSuccessorStates(lastCheckedState);
    			
    			// add next assertions to proof structure queue
    			for (ProgramState succState : successorStates) {
    	    		exploreState(succState);
    	    		Boolean assertionAdded = proofStructure.addAssertion(succState, nextFormulae);

    	    		if (!assertionAdded) {
    	    			System.err.println("Proof Structure found cycle while adding state " + succState.getStateSpaceId());
    	    			
    	    			if (proofStructure.isSuccessful()) {

    	                    if(stateSpace.containsAbortedStates()) {
//    	                        allSatisfied = false;
//    	                        formulaResults.put(formula, ModelCheckingResult.UNKNOWN);
    	                        System.err.println("done. It is unknown whether the formula is satisfied.");
    	                    } else {
//    	                        formulaResults.put(formula, ModelCheckingResult.SATISFIED);
    	                        System.err.println("done. Formula is satisfied.");
//    	                        numberSatFormulae++;
    	                    }

    	                } else {
    	                	System.err.println("Formula is violated: " + formula.getFormulaString());
//    	                    allSatisfied = false;
//    	                    formulaResults.put(formula, ModelCheckingResult.UNSATISFIED);

//    	                    if (scene().options().isIndexedMode()) {
//    	                        logger.warn("Counterexample generation for indexed grammars is not supported yet.");
//    	                    } else {
    	                        FailureTrace failureTrace = proofStructure.getFailureTrace(stateSpace);
    	                        System.out.println(failureTrace.toString());
//    	                        traces.put(formula, failureTrace);
//    	                    }
    	                }
//    	    			
    	    			postProcessingStrategy.process(stateSpace);
    	    	        totalStatesCounter.addStates(stateSpace.size());
    	    			return stateSpace;
    	    		}
    			}	
    		} else {
    			// abort model checking
    			if (!proofStructure.isSuccessful()) {
    				postProcessingStrategy.process(stateSpace);
	    	        totalStatesCounter.addStates(stateSpace.size());
	    			return stateSpace;
    			}
    		}
		}  	
    	
    	postProcessingStrategy.process(stateSpace);
        totalStatesCounter.addStates(stateSpace.size());
    	return stateSpace;
    }
    
    public void exploreState(ProgramState state) throws StateSpaceGenerationAbortedException {
    	
    	if (state.getContainingStateSpace() == null) {
	    	state.setContainingStateSpace(this.stateSpace);
			
			if(!checkAbortCriteria(state)) {
				totalStatesCounter.addStates(stateSpace.size());
				// return stateSpace;
			} 
			
	    	// State space generation
	    	SemanticsCommand stateSemanticsCommand = semanticsOf(state);
	
	        boolean isMaterialized = materializationPhase(stateSemanticsCommand, state) ;
	        if(isMaterialized) {
	            Collection<ProgramState> successorStates = stateSemanticsCommand.computeSuccessors(state);
	            if(finalStateStrategy.isFinalState(state, successorStates, stateSemanticsCommand)) {
	                stateSpace.setFinal(state);
	                stateSpace.addArtificialInfPathsTransition(state); // Add self-loop to each final state
	            } else {
	                for(ProgramState nextState : successorStates) {
	                    handleSuccessorState(state, nextState); // Add unexplored states to exploration strategy
	                }
	            }
	        }
	        
		    postProcessingStrategy.process(stateSpace);
		    totalStatesCounter.addStates(stateSpace.size()); 
    	}
    }
    
    public LinkedList<ProgramState> computeSuccessorStates(ProgramState state) throws StateSpaceGenerationAbortedException {      	
	    
	    // compute next states for model checking
	    // from proofstructure2: only get relevant successors for model checking
	    System.out.println("Computing successors of state " + state.getStateSpaceId() + " (PC: " + state.getProgramCounter() + ")");
	    int stateID = state.getStateSpaceId();
		LinkedList<ProgramState> mcSuccessorStates = new LinkedList<>();
		TIntSet successors = new TIntHashSet(100);

		// Collect the "real" successor states (i.e. skipping materialisation steps)
		TIntArrayList materializationSuccessorIds = this.stateSpace.getMaterializationSuccessorsIdsOf(stateID);
		if (!materializationSuccessorIds.isEmpty()) {
			TIntIterator matStateIterator = materializationSuccessorIds.iterator();
			while (matStateIterator.hasNext()) {
				// Every materialisation state is followed by a control flow state
				int matState = matStateIterator.next();
				// expand materialisation state in order to get control flow successors
				exploreState(stateSpace.getState(matState));
				TIntArrayList controlFlowSuccessorIds = this.stateSpace.getControlFlowSuccessorsIdsOf(matState);
				assert (!controlFlowSuccessorIds.isEmpty());
				successors.addAll(controlFlowSuccessorIds);
			}
		} else {
			successors.addAll(this.stateSpace.getControlFlowSuccessorsIdsOf(stateID));
			// In case the state is final
			successors.addAll(this.stateSpace.getArtificialInfPathsSuccessorsIdsOf(stateID));
		}

		TIntIterator successorsIterator = successors.iterator();
		while (successorsIterator.hasNext()) {
			mcSuccessorStates.add(this.stateSpace.getState(successorsIterator.next()));
		}

		for (ProgramState s : mcSuccessorStates) {

			System.out.println(s.getStateSpaceId() + " (PC: " + s.getProgramCounter() +")");
		}
		return mcSuccessorStates;
    }

    private boolean checkAbortCriteria(ProgramState state) throws StateSpaceGenerationAbortedException {

        try {
            abortStrategy.checkAbort(stateSpace);
        } catch (StateSpaceGenerationAbortedException e) {

            stateSpace.setAborted(state);
            abortRemainingStates();
            if (!state.isFromTopLevelStateSpace()) {
                throw e;
            }
            return false;
        }
        return true;
    }

    private void abortRemainingStates() {

        while (stateExplorationStrategy.hasUnexploredStates()) {
            ProgramState unexploredState = stateExplorationStrategy.getNextUnexploredState();
            stateSpace.setAborted(unexploredState);
        }
        assert !stateExplorationStrategy.hasUnexploredStates();
    }


    private SemanticsCommand semanticsOf(ProgramState state) {

        return program.getStatement(state.getProgramCounter());
    }

    /**
     * In the materialization phase violation points of the given state are removed until the current statement
     * can be executed. The materialized states are immediately added to the state space as successors of the
     * given state.
     *
     * @param semanticsCommand The statement that should be executed next
     *                         and thus determines the necessary materialization.
     * @param state     The program state that should be materialized.
     * @return True if and only if no materialization is needed.
     */
    private boolean materializationPhase(SemanticsCommand semanticsCommand, ProgramState state) {

        Collection<ProgramState> materialized = materializationStrategy.materialize(
                state,
                semanticsCommand.getPotentialViolationPoints()
        );

        for (ProgramState m : materialized) {
            // performance optimization that prevents isomorphism checks against states in the state space.
            stateSpace.addState(m);
            stateExplorationStrategy.addUnexploredState(m, true);
            stateSpace.addMaterializationTransition(state, m);
        }
        return materialized.isEmpty();
    }

    private void labelWithAtomicPropositions(ProgramState state) {

        if(state.isFromTopLevelStateSpace()) {
            stateLabelingStrategy.computeAtomicPropositions(state);
        }
    }

    private void handleSuccessorState(ProgramState state, ProgramState nextState) {

        SemanticsCommand semanticsCommand = semanticsOf(nextState);
        nextState = stateRefinementStrategy.refine(semanticsCommand, nextState);

        if(needsCanonicalization(semanticsCommand, nextState)) {
            ProgramState abstractedState = canonicalizationStrategy.canonicalize(nextState);
            for(ProgramState rectifiedState : stateRectificationStrategy.rectify(abstractedState)) {
                addOrMergeState(state, rectifiedState);
            }
        } else if(state.isContinueState()) {
            // if the previous state is a procedure invocation continued during fixpoint iteration, 
        	//we check whether the next state already exists; even if no canonicalization is performed.
            for(ProgramState rectifiedState : stateRectificationStrategy.rectify(nextState)) {
                addOrMergeState(state, rectifiedState);
            }
        } else {
            for(ProgramState rectifiedState : stateRectificationStrategy.rectify(nextState)) {
                addState(state, rectifiedState);
            }
        }
    }

    private boolean needsCanonicalization(SemanticsCommand semanticsCommand, ProgramState state) {
        return alwaysCanonicalize || semanticsCommand.needsCanonicalization()
                || program.countPredecessors(state.getProgramCounter()) > 1;
    }

    /**
     * If a isomorphic state is already present, a transition to this isomorphic state is added and the
     * state is discareded.
     * Otherwise the state is added to the stateSpace and marked as unexplored. A transition is then added to this state.
     * @param predecessorState the state from which a transition is drawn
     * @param state the state to which a transition is drawn (or to a isomorphic state)
     */
    private void addOrMergeState(ProgramState predecessorState, ProgramState state) {

        labelWithAtomicPropositions(state);
        if (stateSpace.addStateIfAbsent(state)) {
            stateExplorationStrategy.addUnexploredState(state, false);
        }
        stateSpace.addControlFlowTransition(predecessorState, state);
    }

    /**
     * The state is added to the stateSpace and marked as unexplored. Also, a transition
     * form the predecessorState to the state is a added.
     * In particular, there is no check whether a isomorphic state already exists
     * @param predecessorState the state from which a transition is drawn
     * @param state the state to which a transition is drawn
     */
    private void addState(ProgramState predecessorState, ProgramState state) {

        labelWithAtomicPropositions(state);
        stateSpace.addState(state);
        stateExplorationStrategy.addUnexploredState(state, false);
        stateSpace.addControlFlowTransition(predecessorState, state);
    }
}
