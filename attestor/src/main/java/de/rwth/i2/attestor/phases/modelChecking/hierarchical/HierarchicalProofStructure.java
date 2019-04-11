package de.rwth.i2.attestor.phases.modelChecking.hierarchical;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.AbstractProofStructure;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.Assertion2;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.recursiveStateMachine.ComponentStateMachine;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.AssignInvoke;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.InvokeStmt;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class HierarchicalProofStructure extends AbstractProofStructure {
	
	private static final Logger logger = LogManager.getLogger("hierarchicalProofStructure.java");
	
	private ComponentStateMachine csm;
	private StateSpace stateSpace;
	private Map<ProgramState, Set<Assertion2>> stateToAssertions = new LinkedHashMap<>();		
	private List<Node> outputFormulae = new LinkedList<>();

	private HierarchicalFailureTrace hierarchicalFailureTrace = new HierarchicalFailureTrace();
	
	public HierarchicalProofStructure(ComponentStateMachine csm) {
		
		this.csm = csm;
	}
	
	public void build(StateSpace stateSpace, LTLFormula formula) {
		
		List<Node> formulae = new LinkedList<>();
		formulae.add(formula.getASTRoot().getPLtlform());
		
		build(stateSpace, formulae);
	}
	
	public void build(StateSpace stateSpace, List<Node> formulae) {
		
		logger.debug("Building hierarchical proof structure for formula " + formulae.toString());
		
		this.stateSpace = stateSpace;

		Set<ProgramState> initialStates = this.stateSpace.getInitialStates();		
		for (ProgramState state : initialStates) {
			Assertion2 assertion = new Assertion2(state, null);
			assertion.addFormulae(formulae);
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
				if (successorAssertions != null && this.successful) {
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
                            hierarchicalFailureTrace.addFailureTrace(new FailureTrace(this.originOfFailure, stateSpace));

                            // abort proof structure generation, as we already know that it is not successful!
                            if (!buildFullStructure) return;                                                      
                        } 
					}
				} else {
					if (!buildFullStructure) return; 
				}
			} 
			// assertion does not contain any formulae to check (unsuccessful)
			else {
				this.successful = false;				
				setOriginOfFailure(currentAssertion);
				hierarchicalFailureTrace.addFailureTrace(new FailureTrace(this.originOfFailure, stateSpace));
				
                // abort proof structure generation, as we already know that it is not successful!
                if (!buildFullStructure) return;                
			}
		}
	}		

	/**
	 * Creates assertions for Next Formulae of the given assertion. 
	 * Might trigger model checking of a procedure state space in case the next state's statement is a procedure call.
	 * @param assertion
	 * @return a list of successor assertions
	 */
	private List<Assertion2> expandNextAssertion(Assertion2 assertion) {
		
		ProgramState state = assertion.getProgramState();
		List<Assertion2> successorAssertions = new LinkedList<>();
		
		List<Node> nextFormulae = assertion.getNextFormulae();
		
		// Check for method call
		SemanticsCommand statement = csm.getSemanticsCommand(state);
		
		if (getCalledMethodSignature(statement) != null) {
			ComponentStateMachine calledCSM = csm.getBox(state);	
			if (calledCSM != null) {
				// skip model checking of procedure call if called csm does not exist (can be configured by input settings: mc-skip)
				List<Node> inputFormulae = nextFormulae;
				nextFormulae = calledCSM.check(state, statement, inputFormulae); // call new proof structure
				
				if (!calledCSM.modelCheckingSuccessful(state, statement, inputFormulae)) {
					
					this.successful = false;				
					setOriginOfFailure(assertion);
					hierarchicalFailureTrace.addFailureTrace(new FailureTrace(this.originOfFailure, stateSpace));
					hierarchicalFailureTrace.addHierarchicalFailureTrace(calledCSM.getHierarchicalFailureTrace(state, statement, inputFormulae));
					
	                // abort proof structure generation, as we already know that it is not successful!
	                if (!buildFullStructure) return Collections.emptyList();
				}
			}
		} 		
		
		// Create Assertions
		for (ProgramState successorState : getSuccessorStates(assertion.getProgramState())) {
			
			// set return formulae in case this proof structure is successful in order to continue model checking in above CSM
			if (stateSpace.getFinalStates().contains(successorState)) {
				outputFormulae.addAll(assertion.getNextFormulae());
			}
			
			// successor nodes of the current node have to satisfy the Next formulae of the current node
			Assertion2 successorAssertion = new Assertion2(successorState, assertion, true);
			successorAssertion.addFormulae(nextFormulae);
			successorAssertions.add(successorAssertion);
		}

		return successorAssertions;
	}
	
	/**
	 * Gets successor states from state space.
	 * @param state
	 * @return a list of successor states of the current state
	 */
	private List<ProgramState> getSuccessorStates(ProgramState state) {	
		
		List<ProgramState> successorStates = new LinkedList<>();
		TIntSet successors = new TIntHashSet(100);		

		int stateId = state.getStateSpaceId();	    			
			
		// Collect the "real" successor states (i.e. skipping materialisation steps)
		TIntArrayList materializationSuccessorIds = stateSpace.getMaterializationSuccessorsIdsOf(stateId);
		if (!materializationSuccessorIds.isEmpty()) {
			TIntIterator matStateIterator = materializationSuccessorIds.iterator();
			while (matStateIterator.hasNext()) {
				// Every materialisation state is followed by a control flow state
				int matState = matStateIterator.next();
				TIntArrayList controlFlowSuccessorIds = stateSpace.getControlFlowSuccessorsIdsOf(matState);
				assert (!controlFlowSuccessorIds.isEmpty());
				successors.addAll(controlFlowSuccessorIds);
			}
		} else {
			successors.addAll(stateSpace.getControlFlowSuccessorsIdsOf(stateId));
			// In case the state is final
			successors.addAll(stateSpace.getArtificialInfPathsSuccessorsIdsOf(stateId));
		}	

		TIntIterator successorsIterator = successors.iterator();
		while (successorsIterator.hasNext()) {
			successorStates.add(stateSpace.getState(successorsIterator.next()));
		}
		
		return successorStates;
	}
	
	/**
	 * Links assertions to according state.
	 * @param assertion to be added to the map between assertions and a state
	 */
	private void addAssertion(Assertion2 assertion) {
			
		ProgramState state = assertion.getProgramState();
        Set<Assertion2> assertionsOfState = stateToAssertions.get(state);
        if (assertionsOfState == null) {
            assertionsOfState = new LinkedHashSet<>();
            assertionsOfState.add(assertion);
            stateToAssertions.put(state, assertionsOfState);
        } else {
            assertionsOfState.add(assertion);
        }
	}
	
	private Assertion2 getPresentAssertion(Assertion2 assertion) {

		Set<Assertion2> presentAssertions = getAssertionsForState(assertion.getProgramState());
        if (!presentAssertions.isEmpty()) {
            for (Assertion2 presentAssertion : presentAssertions) {            	
                if (assertion.equals(presentAssertion)) return presentAssertion;
            }
        }
        
        return null;
	}
	
	/**
     * This method collects all vertices, whose program state component is equal to
     * the input program state.
     *
     * @param programState, the state that the vertices are checked for
     * @return a set containing all assertions with program state component 'state',
     * if none exist an empty set is returned.
     */
    private Set<Assertion2> getAssertionsForState(ProgramState programState) {

        Set<Assertion2> result = stateToAssertions.get(programState);        
        if (result == null) return Collections.emptySet();
        
        return result;
    }
    
    /**
     * Gets the signature of the method called in the statement if the statement invokes a method call.
     * @param statement
     * @return signature of the called method if the statement invokes a method call, else null
     */
    private String getCalledMethodSignature(SemanticsCommand statement) {

		String signature = null;
		
		if (statement.getClass().equals(AssignInvoke.class) || statement.getClass().equals(InvokeStmt.class)) {
			
			String methodCall = statement.toString();
			signature = methodCall.substring(methodCall.indexOf("<"), methodCall.lastIndexOf(">") + 1);
		}

		return signature;
	}
    
	/**
	 * Returns formulae that are to be checked at the final states of the state space.
	 * Output formulae can be used to continue model checking in other states spaces that called this state space.
	 * @return list of formulae that need to be checked at the successor states of the final states of a state space
	 */
	public List<Node> getOutputFormulae() {
		
		return outputFormulae;
	}
    
    @Override
	public Set<Assertion2> getLeaves() {

        HashSet<Assertion2> leaves = new LinkedHashSet<>();

        Iterator<Set<Assertion2>> iter = stateToAssertions.values().iterator();
        while (iter.hasNext()) {
            for (Assertion2 assertion : iter.next()) {
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
        
        Iterator<Set<Assertion2>> iter = stateToAssertions.values().iterator();
        while (iter.hasNext()) {
            vertices.addAll(iter.next());
        }
        
        return vertices;
    }    

	@Override
	public FailureTrace getFailureTrace() {
		// proof was successful, no counterexample exists
        if (isSuccessful()) return null;
        
        return new FailureTrace(this.originOfFailure, stateSpace);
	}
	
	public HierarchicalFailureTrace getHierarchicalFailureTrace() {
		if (isSuccessful()) return null;
        
        return this.hierarchicalFailureTrace;		
	}
}
