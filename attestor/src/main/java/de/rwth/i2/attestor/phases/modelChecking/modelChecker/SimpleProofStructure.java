package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class SimpleProofStructure extends AbstractProofStructure {
	
	private static final Logger logger = LogManager.getLogger("proofStructure.java");

	private StateSpace stateSpace;	
	protected TIntObjectMap<Set<Assertion2>> stateIdToAssertions;

	public SimpleProofStructure() {
		
		super();
		this.stateIdToAssertions = new TIntObjectHashMap<>();
	}	
	
	public void build(StateSpace stateSpace, LTLFormula formula) {		

		this.stateSpace = stateSpace;
		
		for (ProgramState state : this.stateSpace.getInitialStates()) {
			Assertion2 assertion = new Assertion2(state, null, formula);
			addAssertion(assertion);
			queue.add(assertion);
		}		

		while (!queue.isEmpty()) {
			Assertion2 currentAssertion = queue.poll();
			checkedAssertions++;
			
			// tableau step for formulae without X operator
			if (!currentAssertion.getFormulae().isEmpty()) {
				Node currentFormula = currentAssertion.getFirstFormula();
				HashSet<Assertion2> successorAssertions = expand(currentAssertion, currentFormula);
				if (successorAssertions != null) {
					for (Assertion2 successorAssertion : successorAssertions) {						
						addEdge(currentAssertion, successorAssertion);
						addAssertion(successorAssertion);
						queue.add(successorAssertion);
					}
				}
			} 
			// tableau step for formulae with X operator
			else if (!currentAssertion.getNextFormulae().isEmpty()) {
				List<Assertion2> successorAssertions = expandNextAssertion(currentAssertion);
				if (successorAssertions != null) {
					for (Assertion2 successorAssertion : successorAssertions) {	
						
						// Check if we have already seen an equal assertion before
                        Assertion2 presentAssertion = getPresentAssertion(successorAssertion);
                        boolean assertionPresent = (presentAssertion != null);
                        if (assertionPresent) successorAssertion = presentAssertion;

                        addEdge(currentAssertion, successorAssertion);
                        addAssertion(successorAssertion);
                        
                        // Process the assertion further only in case it is not one, that was already processed.                       
                        if (!assertionPresent) {
                            queue.add(successorAssertion);
                        } 
                        // Otherwise, check whether the assertion is part of a real and harmful cycle (if it does not contain an R operator)
                        else if (isRealCycle(successorAssertion) && !containsReleaseOperator(successorAssertion)) {                         
                            this.successful = false;                                    
                            setOriginOfFailure(successorAssertion);

                            // abort proof structure generation, as we already know that it is not successful!
                            if (!buildFullStructure) return;                                                      
                        } 
					}
				}
			} 
			// assertion does not contain any formulae to check (unsuccessful)
			else {
				this.successful = false;				
				setOriginOfFailure(currentAssertion);
				
                // abort proof structure generation, as we already know that it is not successful!
                if (!buildFullStructure) return;                
			}
		}
	}	
	
	/**
	 * The Next formulae of a node have to be model checked for the successor program states of the current node. 
	 * This method creates the successor assertions for nodes with Next formulae only.
	 * 
	 * @param assertion
	 * 		the node only holding Next formulae
	 * @return
	 * 		a list of successor assertions with the list of formulae being the Next formulae of the current node
	 */
	private List<Assertion2> expandNextAssertion(Assertion2 assertion) {

		// list that holds successor nodes for the current node
		List<Assertion2> successorAssertions = new LinkedList<>();

		for (ProgramState successorProgramState : getSuccessorStates(assertion.getProgramState())) {
			// successor nodes of the current node have to satisfy the Next formulae of the current node
			Assertion2 successorAssertion = new Assertion2(successorProgramState, assertion, true);
			successorAssertion.addFormulae(assertion.getNextFormulae());
			successorAssertions.add(successorAssertion);
		}

		return successorAssertions;
	}
	
	/**
	 * Links assertions to according state.
	 * @param assertion to be added to the map between assertions and a state
	 */
	private void addAssertion(Assertion2 assertion) {
			
		int stateId = assertion.getProgramState().getStateSpaceId();
        Set<Assertion2> assertionsOfId = stateIdToAssertions.get(stateId);
        if (assertionsOfId == null) {
            assertionsOfId = new LinkedHashSet<>();
            assertionsOfId.add(assertion);
            stateIdToAssertions.put(stateId, assertionsOfId);
        } else {
            assertionsOfId.add(assertion);
        }
	}
	
	/**
     * This method collects all vertices, whose program state component is equal to
     * the input program state.
     *
     * @param programState, the state that the vertices are checked for
     * @return a set containing all assertions with program state component 'state',
     * if none exist an empty set is returned.
     */
    private Set<Assertion2> getAssertionsForState(int programStateID) {

        Set<Assertion2> result = stateIdToAssertions.get(programStateID);  // need to store complete state object      
        if (result == null) return Collections.emptySet();
        
        return result;
    }
    
    private Assertion2 getPresentAssertion(Assertion2 assertion) {

		Set<Assertion2> presentAssertions = getAssertionsForState(assertion.getProgramState().getStateSpaceId());
        if (!presentAssertions.isEmpty()) {
            for (Assertion2 presentAssertion : presentAssertions) {            	
                if (assertion.equals(presentAssertion)) return presentAssertion;
            }
        }
        
        return null;
	}

    /**
	 * Gets successor states from state space.
	 * @param state
	 * @return a list of successor states of the current state
	 */
	private List<ProgramState> getSuccessorStates(ProgramState programState) {		
		
		int stateID = programState.getStateSpaceId();
		LinkedList<ProgramState> successorStates = new LinkedList<>();
		TIntSet successors = new TIntHashSet(100);

		// Collect the "real" successor states (i.e. skipping materialisation steps)
		TIntArrayList materializationSuccessorIds = this.stateSpace.getMaterializationSuccessorsIdsOf(stateID);
		if (!materializationSuccessorIds.isEmpty()) {
			TIntIterator matStateIterator = materializationSuccessorIds.iterator();
			while (matStateIterator.hasNext()) {
				// Every materialisation state is followed by a control flow state
				int matState = matStateIterator.next();
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
			successorStates.add(this.stateSpace.getState(successorsIterator.next()));
		}
		
		return successorStates;
	}
	
	@Override
	public HashSet<Assertion2> getLeaves() {

        HashSet<Assertion2> leaves = new LinkedHashSet<>();

        TIntObjectIterator<Set<Assertion2>> iter = stateIdToAssertions.iterator();
        while (iter.hasNext()) {
            iter.advance();
            for (Assertion2 assertion : iter.value()) {
                if (!edges.containsKey(assertion)) leaves.add(assertion);
            }
        }
        
        return leaves;
    }

	@Override
    public Integer size() {

        return getVertices().size();
    }
	
	@Override
	public Set<Assertion2> getVertices() {

        HashSet<Assertion2> vertices = new LinkedHashSet<>();

        TIntObjectIterator<Set<Assertion2>> iter = stateIdToAssertions.iterator();
        while (iter.hasNext()) {
            iter.advance();
            vertices.addAll(iter.value());
        }
        
        return vertices;
    }
    
    @Override
    public FailureTrace getFailureTrace() {

    	// proof was successful, no counterexample exists
        if (isSuccessful()) return null;
        
        return new FailureTrace(this.originOfFailure, stateSpace);
    }
}
