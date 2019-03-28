package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class OnTheFlyProofStructure extends AbstractProofStructure {
	
	private Assertion2 currentParent; // last checked assertion
	protected TIntObjectMap<Set<Assertion2>> stateIdToAssertions;
	
	public OnTheFlyProofStructure() {
		
		super();
		this.currentParent = null;
		this.stateIdToAssertions = new TIntObjectHashMap<>();
	}
	
	public List<Node> build() {
		
		// start model checking until we meet X-formula
        while (!queue.isEmpty()) {
			Assertion2 currentAssertion = queue.poll();
			checkedAssertions++;
			
			// tableau step for formulae without X operator
			if (!currentAssertion.getFormulae().isEmpty()) {
				Node currentFormula = currentAssertion.getFirstFormula();
				List<Assertion2> successorAssertions = expand(currentAssertion, currentFormula);
				if (successorAssertions != null) {
					for (Assertion2 successorAssertion : successorAssertions) {						
						addEdge(currentAssertion, successorAssertion);
						addAssertion(successorAssertion);
						queue.add(successorAssertion);
					}
				}
			} 
			// return formulae with X operator
			else if (!currentAssertion.getNextFormulae().isEmpty()) {
				// set current parent 
				currentParent = currentAssertion;
				return currentAssertion.getNextFormulae();
			} 
			// assertion does not contain any formulae to check (unsuccessful)
			else {
				this.successful = false;				
				setOriginOfFailure(currentAssertion);
				
                // abort proof structure generation, as we already know that it is not successful!
                if (!buildFullStructure) return null;                
			}
		}
        
		return null;		
	}
	
	public ProgramState getNextStateToCheck() {
		Assertion2 nextAssertion = queue.peekFirst();
		return nextAssertion != null ? nextAssertion.getProgramState() : null;
	}
	
	public ProgramState getLastCheckedState() {

		return currentParent.getProgramState();
	}
	
	public Assertion2 getCurrentParent() {

		return currentParent;
	}
	
	/**
	 * Add an assertion to the proof structure.
	 * 
	 * @param state
	 * @param formulae
	 * @return true if the assertion has successfully been added to the proof structure, false otherwise
	 */
	public void addAssertion(ProgramState state, List<Node> formulae) {
				
		// create assertion from state and formulae
		Assertion2 assertion = new Assertion2(state, currentParent, true);
		assertion.addFormulae(formulae);
		
		// check if assertion already exists
        Assertion2 presentAssertion = getPresentAssertion(assertion);
        boolean assertionPresent = (presentAssertion != null);
        if (assertionPresent) assertion = presentAssertion;

        if (currentParent != null) addEdge(currentParent, assertion);
        addAssertion(assertion);
        
        // Process the assertion further only in case it is not one, that was already processed.                       
        if (!assertionPresent) {
            queue.add(assertion);
        } // Otherwise, check whether the assertion is part of a real and harmful cycle (if it does not contain an R operator)
        else if (isRealCycle(assertion) && !containsReleaseOperator(assertion)) {                         
            this.successful = false;                                    
            setOriginOfFailure(assertion);
        } 
	}
	
	/**
	 * 
	 * @param assertion
	 */
	protected void addAssertion(Assertion2 assertion) {
			
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

    public Integer size() {

        return getVertices().size();
    }
    
    protected Assertion2 getPresentAssertion(Assertion2 assertion) {

		Set<Assertion2> presentAssertions = getAssertionsForState(assertion.getProgramState().getStateSpaceId());
        if (!presentAssertions.isEmpty()) {
            for (Assertion2 presentAssertion : presentAssertions) {            	
                if (assertion.equals(presentAssertion)) return presentAssertion;
            }
        }
        
        return null;
	}

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
