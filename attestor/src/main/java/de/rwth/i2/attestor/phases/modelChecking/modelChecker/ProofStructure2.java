package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.AReleaseLtlform;
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

public class ProofStructure2 {
	
	private static final Logger logger = LogManager.getLogger("proofStructure.java");

	/**
	 * State space instance not needed for on-the-fly model checking.
	 */
	private StateSpace stateSpace;
	private TIntObjectMap<Set<Assertion2>> stateIdToAssertions;
	private HashMap<Assertion2, HashSet<Assertion2>> edges;
	
	private LinkedList<Assertion2> queue = new LinkedList<>();
	private Assertion2 currentParentAssertion = null;
	
	private boolean buildFullStructure = false;
	private boolean successful = true;
	private Assertion2 originOfFailure = null;    

	public ProofStructure2(StateSpace stateSpace) {
		this.stateSpace = stateSpace;
		this.stateIdToAssertions = new TIntObjectHashMap<>();
		this.edges = new LinkedHashMap<>();
	}
	
	public ProofStructure2() {
		this.stateSpace = null;
		this.stateIdToAssertions = new TIntObjectHashMap<>();
		this.edges = new LinkedHashMap<>();
	}
	
	public ProgramState getNextStateToCheck() {
		Assertion2 nextAssertion = queue.peekFirst();
		return nextAssertion != null ? nextAssertion.getProgramState() : null;
	}
	
	public ProgramState getLastCheckedState() {

		return currentParentAssertion.getProgramState();
	}
	
	public LinkedList<Node> build() {
		
		// start model checking until we meet X-formula
        while (!queue.isEmpty()) {
			Assertion2 currentAssertion = queue.poll();
            System.out.println(currentAssertion.getProgramState().getStateSpaceId());
			
			// tableau step for formulae without X operator
			if (!currentAssertion.getFormulae().isEmpty()) {
				Node currentFormula = currentAssertion.getFirstFormula();
				LinkedList<Assertion2> successorAssertions = expand(currentAssertion, currentFormula);
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
				// set current parent and return X-Formula
				currentParentAssertion = currentAssertion;
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
	
	public void build(List<ProgramState> initialStates, LTLFormula formula) {
		
		for (ProgramState state : initialStates) {
			Assertion2 assertion = new Assertion2(state, null, formula);
			addAssertion(assertion);
			queue.add(assertion);
		}		

		while (!queue.isEmpty()) {
			Assertion2 currentAssertion = queue.poll();
            System.out.println(currentAssertion.getProgramState().getStateSpaceId());
			
			// tableau step for formulae without X operator
			if (!currentAssertion.getFormulae().isEmpty()) {
				Node currentFormula = currentAssertion.getFirstFormula();
				LinkedList<Assertion2> successorAssertions = expand(currentAssertion, currentFormula);
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
				LinkedList<Assertion2> successorAssertions = expandNextAssertion(currentAssertion);
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
	
	private Assertion2 getPresentAssertion(Assertion2 assertion) {

		Set<Assertion2> presentAssertions = getAssertionsForState(assertion.getProgramState().getStateSpaceId());
        if (!presentAssertions.isEmpty()) {
            for (Assertion2 presentAssertion : presentAssertions) {            	
                if (assertion.equals(presentAssertion)) return presentAssertion;
            }
        }
        
        return null;
	}

	private void setOriginOfFailure(Assertion2 assertion) {

        if (this.originOfFailure == null) this.originOfFailure = assertion;
	}

	private boolean containsReleaseOperator(Assertion2 assertion) {

        for (Node formula : assertion.getFormulae()) {
            if (formula instanceof AReleaseLtlform) return true;
        }
        
        return false;		
	}

	private boolean isRealCycle(Assertion2 assertion) {
		
		System.err.println("cycle check");
        
        LinkedList<Assertion2> cycleQueue = new LinkedList<>();
        cycleQueue.add(assertion);        

        HashSet<Assertion2> seen = new LinkedHashSet<>();
        seen.add(assertion);        
        
        while (!cycleQueue.isEmpty()) {
            Assertion2 currentAssertion = cycleQueue.pop();
            for (Assertion2 successorAssertion : this.getSuccessorAssertions(currentAssertion)) {            	
                if (successorAssertion.equals(assertion)) return true;   
                if (!seen.contains(successorAssertion)) cycleQueue.add(successorAssertion);
                seen.add(successorAssertion);
            }
        }
        
		return false;
	}

	/**
	 * Do one step in the tableau according to the tableau rules.
	 * 
	 * @param node
	 * 		assertion which holds the program state and the formula to be checked
	 * @param formula
	 * 		the formula to be checked
	 * @return
	 * 		list of successor nodes, possibly empty (successful path)
	 */
	@SuppressWarnings("unchecked")
	private LinkedList<Assertion2> expand(Assertion2 node, Node formula) {

		TableauRulesSwitch2 rulesSwitch = new TableauRulesSwitch2(node.getProgramState());
		rulesSwitch.setIn(formula, node);
		formula.apply(rulesSwitch);
		
		return (LinkedList<Assertion2>) rulesSwitch.getOut(formula);
	}

	/**
	 * 
	 * @param assertion
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
	 * The Next formulae of a node have to be model checked for the successor program states of the current node. 
	 * This method creates the successor assertions for nodes with Next formulae only.
	 * 
	 * @param assertion
	 * 		the node only holding Next formulae
	 * @return
	 * 		a list of successor assertions with the list of formulae being the Next formulae of the current node
	 */
	private LinkedList<Assertion2> expandNextAssertion(Assertion2 assertion) {

		// list that holds successor nodes for the current node
		LinkedList<Assertion2> successorAssertions = new LinkedList<>();

		for (ProgramState successorProgramState : this.getSuccessorProgramStates(assertion.getProgramState())) {
			// successor nodes of the current node have to satisfy the Next formulae of the current node
			Assertion2 successorAssertion = new Assertion2(successorProgramState, assertion, true);
			successorAssertion.addFormulae(assertion.getNextFormulae());
			successorAssertions.add(successorAssertion);
		}

		return successorAssertions;
	}

	private void addEdge(Assertion2 assertion, Assertion2 successorAssertion) {

        if (!edges.containsKey(assertion)) {
            HashSet<Assertion2> successorAssertions = new LinkedHashSet<>();
            successorAssertions.add(successorAssertion);
            edges.put(assertion, successorAssertions);
        } else {
            edges.get(assertion).add(successorAssertion);
        }
    }
	
	public void setBuildFullStructure() {

        buildFullStructure = true;
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

        Set<Assertion2> result = stateIdToAssertions.get(programStateID);        
        if (result == null) return Collections.emptySet();
        
        return result;
    }
    
    public boolean isSuccessful() {

        return this.successful;
    }

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


    public HashSet<Assertion2> getVertices() {

        HashSet<Assertion2> vertices = new LinkedHashSet<>();

        TIntObjectIterator<Set<Assertion2>> iter = stateIdToAssertions.iterator();
        while (iter.hasNext()) {
            iter.advance();
            vertices.addAll(iter.value());
        }
        
        return vertices;
    }

    public Integer size() {

        return getVertices().size();
    }
    
    public FailureTrace getFailureTrace(StateSpace stateSpace) {

    	// proof was successful, no counterexample exists
        if (isSuccessful()) return null;
        
        return new FailureTrace(this.originOfFailure, stateSpace);
    }
    
    public FailureTrace getFailureTrace() {

    	// proof was successful, no counterexample exists
        if (isSuccessful()) return null;
        
        return new FailureTrace(this.originOfFailure, stateSpace);
    }
    
    private HashSet<Assertion2> getSuccessorAssertions(Assertion2 assertion) {

        HashSet<Assertion2> successorAssertions = new LinkedHashSet<>();
        if (this.edges.get(assertion) != null) {
            for (Assertion2 successorAssertion : this.edges.get(assertion)) {
                successorAssertions.add(successorAssertion);
            }
        }
        
        return successorAssertions;
    }

    /**
     * (not needed for on-the-fly model checking)
     * 
     * @param programState
     * @return
     */
	private LinkedList<ProgramState> getSuccessorProgramStates(ProgramState programState) {		
		
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
	
	/**
	 * Add an assertion to the proof structure.
	 * 
	 * @param state
	 * @param formulae
	 * @return true if the assertion has successfully been added to the proof structure, false otherwise
	 */
	public boolean addAssertion(ProgramState state, List<Node> formulae) {
				
		// create assertion from state and formulae
		Assertion2 assertion = new Assertion2(state, currentParentAssertion, true);
		assertion.addFormulae(formulae);
		
		// check if assertion already exists
        Assertion2 presentAssertion = getPresentAssertion(assertion);
        boolean assertionPresent = (presentAssertion != null);
        if (assertionPresent) assertion = presentAssertion;

        if (currentParentAssertion != null) addEdge(currentParentAssertion, assertion);
        addAssertion(assertion);
        
        // Process the assertion further only in case it is not one, that was already processed.                       
        if (!assertionPresent) {
            queue.add(assertion);
        } // Otherwise, check whether the assertion is part of a real and harmful cycle (if it does not contain an R operator)
        else if (isRealCycle(assertion) && !containsReleaseOperator(assertion)) {                         
            this.successful = false;                                    
            setOriginOfFailure(assertion);

            // abort proof structure generation, as we already know that it is not successful!
            // if (!buildFullStructure) return false;   
            return false;
        } 
		
        // TODO check this return statement
        return true;
	}
}
