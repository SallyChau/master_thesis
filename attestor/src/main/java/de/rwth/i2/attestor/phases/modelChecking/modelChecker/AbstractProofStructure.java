package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.AReleaseLtlform;
import de.rwth.i2.attestor.generated.node.Node;

public abstract class AbstractProofStructure {	

	protected LinkedList<Assertion2> queue;
	protected HashMap<Assertion2, HashSet<Assertion2>> edges;		
	protected Assertion2 originOfFailure = null;  
	protected boolean buildFullStructure = false;
	protected boolean successful = true;
	protected int checkedAssertions = 0;
	
	public AbstractProofStructure() {

		this.edges = new LinkedHashMap<>();
		this.queue = new LinkedList<>();
	}

	protected void setOriginOfFailure(Assertion2 assertion) {

        if (this.originOfFailure == null) this.originOfFailure = assertion;
	}

	protected boolean containsReleaseOperator(Assertion2 assertion) {

        for (Node formula : assertion.getFormulae()) {
            if (formula instanceof AReleaseLtlform) return true;
        }
        
        return false;		
	}

	protected boolean isRealCycle(Assertion2 assertion) {
        
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
    
    protected Set<Assertion2> getSuccessorAssertions(Assertion2 assertion) {

        HashSet<Assertion2> successorAssertions = new LinkedHashSet<>();
        if (this.edges.get(assertion) != null) {
            for (Assertion2 successorAssertion : this.edges.get(assertion)) {
                successorAssertions.add(successorAssertion);
            }
        }
        
        return successorAssertions;
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
	protected List<Assertion2> expand(Assertion2 node, Node formula) {

		TableauRulesSwitch2 rulesSwitch = new TableauRulesSwitch2(node.getProgramState());
		rulesSwitch.setIn(formula, node);
		formula.apply(rulesSwitch);
		
		return (LinkedList<Assertion2>) rulesSwitch.getOut(formula);
	}

    protected void addEdge(Assertion2 assertion, Assertion2 successorAssertion) {

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
    
    public boolean isSuccessful() {

        return this.successful;
    }
    
    public int getNumberOfCheckedAssertions() {
		return checkedAssertions;
	}
    
    public abstract Set<Assertion2> getLeaves();

    public abstract Integer size();

    public abstract Set<Assertion2> getVertices();
    
    public abstract FailureTrace getFailureTrace();
}