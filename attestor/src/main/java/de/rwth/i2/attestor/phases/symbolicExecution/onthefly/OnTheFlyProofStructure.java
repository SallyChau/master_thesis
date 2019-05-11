package de.rwth.i2.attestor.phases.symbolicExecution.onthefly;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.AbstractProofStructure;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.Assertion2;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class OnTheFlyProofStructure extends AbstractProofStructure {
	
	private Assertion2 currentParent = null; // last checked assertion
	protected TIntObjectMap<Set<Assertion2>> stateIdToAssertions = new TIntObjectHashMap<>();
	
	private Assertion2 currentAssertion = null;
	
	public Set<Node> buildAndGetNextFormulaeToCheck() {
		
		System.out.println("ProofStructure: Building proofstructure...");
		
        while (!queue.isEmpty()) {
			currentAssertion = queue.poll();
			checkedAssertions++;			

			System.out.println("ProofStructure: Checking assertion: " + currentAssertion.toString());
			
			// tableau step for formulae without X operator
			if (!currentAssertion.getFormulae().isEmpty()) {
				
				Node currentFormula = currentAssertion.getFirstFormula();
				
				System.out.println("ProofStructure: Checking formula: " + currentFormula);
				
				HashSet<Assertion2> successorAssertions = expand(currentAssertion, currentFormula);
				
				if (successorAssertions != null) {

					System.out.println("ProofStructure: Successor assertions: " + successorAssertions.size());
					for (Assertion2 successorAssertion : successorAssertions) {						
						addEdge(currentAssertion, successorAssertion);
						addAssertionToState(successorAssertion);
						queue.add(successorAssertion);
						
						System.out.println("ProofStructure: added assertion to queue: " + successorAssertion);
					}
				} else {
					System.out.println("ProofStructure: no successor assertions");
				}
			} 
			// return formulae with X operator
			else if (!currentAssertion.getNextFormulae().isEmpty()) {
				// set current parent 
				currentParent = currentAssertion;
				
				List<Node> nextFormulae = currentAssertion.getNextFormulae();
				Set<Node> result = new HashSet<>();
				for (Node formula : nextFormulae) {
					result.add(formula);
				}
				
				System.out.println("ProofStructure: returning formulae for assertion " + currentAssertion);
				System.out.println("ProofStructure: returning formulae " + result);
				
				return result;
			} 
			// assertion does not contain any formulae to check (unsuccessful)
			else {
				this.successful = false;				
				setOriginOfFailure(currentAssertion);
				
                // abort proof structure generation, as we already know that it is not successful!
                if (!buildFullStructure) {
                	System.out.println("Proofstructure: abort since assertion does not contain formulae to check");
                	return null;                
                }
			}
		}
        
        System.out.println("Proofstructure: queue is completely done.");
		return null;		// TODO how to handle successful proofstructures?
	}
	
	/**
	 * Links assertions to according state.
	 * @param assertion to be added to the map between assertions and a state
	 */
	private void addAssertionToState(Assertion2 assertion) {
			
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
    private Set<Assertion2> getAssertionsForState(int stateId) {

        Set<Assertion2> result = stateIdToAssertions.get(stateId);  // need to store complete state object      
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
	 * Adds an assertion to the queue of the proof structure if it does not exist yet.
	 * 
	 * @param state to be checked
	 * @param formulae to be checked
	 * @return true if the assertion has successfully been added to the proof structure, false otherwise
	 */
	public void addAssertion(ProgramState state, Set<Node> formulae) {
				
		// create assertion from state and formulae
		Assertion2 assertion = new Assertion2(state, currentParent, true);
		for (Node formula : formulae) {

			assertion.addFormula(formula);
		}
		
		// check if assertion already exists
        Assertion2 presentAssertion = getPresentAssertion(assertion);
        boolean assertionPresent = (presentAssertion != null);
        if (assertionPresent) assertion = presentAssertion;

        if (currentParent != null) addEdge(currentParent, assertion);
        addAssertionToState(assertion);
        
        // Process the assertion further only in case it is not one, that was already processed.                       
        if (!assertionPresent) {
        	System.out.println("ProofStructure: Added assertion " + assertion.toString());
            queue.add(assertion);
        } // Otherwise, check whether the assertion is part of a real and harmful cycle (if it does not contain an R operator)
        else if (isRealCycle(assertion) && !containsReleaseOperator(assertion)) {                         
            this.successful = false;                                    
            setOriginOfFailure(assertion);
        }        
        
        System.out.println("ProofStructure: Queue contains " + queue.size() + " elements.");
	}
    
	/**
	 * States whether the proofstructure can be continued, i.e. there exists a next assertion in the model checking queue
	 * and the proofstructure is (still) successful.
	 * @return true if there is at least one assertion in the model checking queue and the proofstructure is (still) successful
	 */
    public boolean isContinuable() {
    	
		return ((queue.peekFirst() != null) && isSuccessful());
	}
	
    /**
     * Gets the state of the last checked assertion in the model checking queue.
     * @return state of the last checked assertion in the model checking queue
     */
	public ProgramState getLastCheckedState() {

		return currentParent.getProgramState();
	}
	
	/**
	 * Gets the last checked assertion with X-formulae which is also the current parent for new assertions.
	 * @return last checked assertion with X-formulae which is also the current parent for new assertions
	 */
	public Assertion2 getCurrentParent() {

		return currentParent;
	}
	
	public void abort() {
		setOriginOfFailure(currentAssertion);
		this.successful = false;
	}
    
    @Override
	public Set<Assertion2> getLeaves() {

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
	
	/**
     * For on-the-fly model checking.
     * @param stateSpace
     * @return
     */
    public FailureTrace getFailureTrace(StateSpace stateSpace) {

    	// proof was successful, no counterexample exists
        if (isSuccessful()) return null;
        
        return new FailureTrace(this.originOfFailure, stateSpace);
    }

	@Override
	public FailureTrace getFailureTrace() {
		// TODO Auto-generated method stub
		return null;
	}
}
