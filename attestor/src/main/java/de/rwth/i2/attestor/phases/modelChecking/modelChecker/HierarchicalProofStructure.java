package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.recursiveStateMachine.ComponentStateMachine;
import de.rwth.i2.attestor.recursiveStateMachine.RecursiveStateMachine;
import de.rwth.i2.attestor.semantics.TerminalStatement;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.AssignInvoke;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.InvokeStmt;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.ReturnValueStmt;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.ReturnVoidStmt;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class HierarchicalProofStructure extends AbstractProofStructure {	

	private RecursiveStateMachine rsm;
	private ComponentStateMachine currentCSM;
	private StateSpace currentStateSpace;
	private Map<ProgramState, Set<Assertion2>> stateToAssertions;
	
	private List<HierarchicalProofStructure> proofStructureQueue;
	
	public HierarchicalProofStructure(RecursiveStateMachine rsm) {
		
		super();
		this.stateToAssertions = new LinkedHashMap<>();
		this.rsm = rsm;
		this.proofStructureQueue = new LinkedList<>();
	}
	
	public void build(ProcedureCall mainCall, LTLFormula formula) {
		
		setCurrentEnvironment(mainCall);
		
		Set<ProgramState> initialStates = currentStateSpace.getInitialStates();
		for (ProgramState state : initialStates) {
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
				List<Assertion2> successorAssertions = expand(currentAssertion, currentFormula);
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

	private void setCurrentEnvironment(ProcedureCall mainCall) {

		currentCSM = rsm.getOrCreateComponentStateMachine(mainCall.getMethod());
		currentStateSpace = currentCSM.getStateSpace(mainCall.getInput());
	}

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
     * 
     * @param programState
     * @return
     */
	private List<ProgramState> getSuccessorStates(ProgramState programState) {	
		
		System.out.println(programState.getStateSpaceId() + " " + programState.getContainingStateSpace());
		
		List<ProgramState> successorStates = new LinkedList<>();
		TIntSet successors = new TIntHashSet(100);		

		int stateID = programState.getStateSpaceId();
		
		// get state space and csm to program state
		StateSpace stateSpace = programState.getContainingStateSpace();
		ComponentStateMachine csm = rsm.getComponentStateMachine(stateSpace);
		
		// get semantics command to program state in according (!) csm
		SemanticsCommand statement = csm.getSemanticsCommand(programState);
		String calledMethod = getCalledMethodSignature(statement); 
		
		// Method call
		if (calledMethod != null) {
			
			System.out.println("Called method: " + calledMethod);
			stateSpace = rsm.getComponentStateMachine(calledMethod).getCalledStateSpace(programState, statement);
			System.out.println(stateSpace.toString());
			successors.addAll(stateSpace.getInitialStateIds());
		} else if (isReturnStatement(statement)) {
			System.err.println("return"); // TODO
			System.err.println(statement.toString());
		} else {			
			
			// Collect the "real" successor states (i.e. skipping materialisation steps)
			TIntArrayList materializationSuccessorIds = stateSpace.getMaterializationSuccessorsIdsOf(stateID);
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
				successors.addAll(stateSpace.getControlFlowSuccessorsIdsOf(stateID));
				// In case the state is final
				successors.addAll(stateSpace.getArtificialInfPathsSuccessorsIdsOf(stateID));
			}
		}

		TIntIterator successorsIterator = successors.iterator();
		while (successorsIterator.hasNext()) {
			successorStates.add(stateSpace.getState(successorsIterator.next()));
		}
		
		return successorStates;
	}	
	
	private String getCalledMethodSignature(SemanticsCommand statement) {

		String signature = null;
		
		if (statement.getClass().equals(AssignInvoke.class) || statement.getClass().equals(InvokeStmt.class)) {
			
			String methodCall = statement.toString();
			signature = methodCall.substring(methodCall.indexOf("<"), methodCall.lastIndexOf(">") + 1);
		}

		return signature;
	}
	
	// TODO
	private boolean isReturnStatement(SemanticsCommand statement) {

		return (statement.getClass().equals(ReturnValueStmt.class) 
				|| statement.getClass().equals(ReturnVoidStmt.class)
				|| statement.getClass().equals(TerminalStatement.class));		
	}

	/**
	 * 
	 * @param assertion
	 */
	protected void addAssertion(Assertion2 assertion) {
			
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
    
    public Set<Assertion2> getLeaves() {

        HashSet<Assertion2> leaves = new LinkedHashSet<>();

        Iterator<Set<Assertion2>> iter = stateToAssertions.values().iterator();
        while (iter.hasNext()) {
            iter.next();
            // TODO
//            for (Assertion2 assertion : iter.value()) {
//                if (!edges.containsKey(assertion)) leaves.add(assertion);
//            }
        }
        
        return leaves;
    }

    public Integer size() {

        return getVertices().size();
    }
    
    protected Assertion2 getPresentAssertion(Assertion2 assertion) {

		Set<Assertion2> presentAssertions = getAssertionsForState(assertion.getProgramState());
        if (!presentAssertions.isEmpty()) {
            for (Assertion2 presentAssertion : presentAssertions) {            	
                if (assertion.equals(presentAssertion)) return presentAssertion;
            }
        }
        
        return null;
	}

    public Set<Assertion2> getVertices() {

        HashSet<Assertion2> vertices = new LinkedHashSet<>();
// TODO
//        TIntObjectIterator<Set<Assertion2>> iter = stateIdToAssertions.iterator();
//        while (iter.hasNext()) {
//            iter.advance();
//            vertices.addAll(iter.value());
//        }
//        
        return vertices;
    }
    

	@Override
	public FailureTrace getFailureTrace() {
		// TODO Auto-generated method stub
		return null;
	}

}
