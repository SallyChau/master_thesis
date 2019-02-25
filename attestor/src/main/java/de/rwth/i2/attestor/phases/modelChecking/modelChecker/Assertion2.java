package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import java.util.LinkedList;
import java.util.List;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

/**
 * Represents node in tableau graph
 * @author chau 
 *
 */
public class Assertion2 {

	private ProgramState programState;
	private LinkedList<Node> formulae = new LinkedList<>();
	private LinkedList<Node> nextFormulae = new LinkedList<>(); 
	private Assertion2 parent;
	private boolean isTrue;
	private boolean isContainedInTrace;
	
	public Assertion2(ProgramState programState, Assertion2 parent) {
		this.programState = programState;
		this.parent = parent;
	}
	
	public Assertion2(ProgramState programState, Assertion2 parent, LTLFormula formula) {
		this.programState = programState;
		this.parent = parent;
		addFormula(formula.getASTRoot().getPLtlform());
	}
	
	public Assertion2(ProgramState programState, Assertion2 parent, boolean isContainedInTrace) {

        this(programState, parent);
        this.isContainedInTrace = true;
    }
	
	public void addNextFormula(Node formula) {

		if (!this.nextFormulae.contains(formula)) {
			this.nextFormulae.add(formula);
		}
	}
	
	public void addNextFormulae(List<Node> formulae) {

		for (Node formula : formulae) {
			addNextFormula(formula);
		}
	}
	
	public void addFormula(Node formula) {
		
		if (!this.formulae.contains(formula)) {
			this.formulae.add(formula);
		}
	}
	
	public void addFormulae(List<Node> formulae) {
		
		for (Node formula : formulae) {
			addFormula(formula);
		}
	}
	
	public LinkedList<Node> getFormulae() {
		return this.formulae;
	}
	
	public Node getFirstFormula() {
        return this.formulae.getFirst();
    }
	
	public LinkedList<Node> getNextFormulae() {
		return this.nextFormulae;
	}
	
	public void removeFormula(Node formula) {
		this.formulae.removeFirst();
	}
	
	public ProgramState getProgramState() {
		return this.programState;
	}
	
	public Assertion2 getParent() {
		return this.parent;
	}
	
	public void setTrue() {
		this.isTrue = true;
	}
	
	public boolean containsFormula(Node formula) {
		return this.formulae.contains(formula);
	}
	
	public boolean containsNextFormula(Node formula) {
		return this.nextFormulae.contains(formula);
	}
	
	public boolean containsAllFormulae(LinkedList<Node> formulae) {
		
		if (formulae.size() == this.formulae.size()) {
			for (Node formula : formulae) {
				if (!this.formulae.contains(formula)) return false;
			}
			
			return true;
		}		
		
		return false;
	}
	
	public boolean containsAllNextFormulae(LinkedList<Node> formulae) {

		if (formulae.size() == this.nextFormulae.size()) {
			for (Node formula : formulae) {
				if (!this.nextFormulae.contains(formula)) return false;
			}
			
			return true;
		}		
		
		return false;
	}
	
	@Override
	public boolean equals(Object node) {
		
		if (this == node) return true;
		if (node == null) return false;
		if (!(node instanceof Assertion2)) return false;
		
		// check content
		Assertion2 nodeTest = (Assertion2) node;
		return containsAllFormulae(nodeTest.getFormulae()) && 
				containsAllNextFormulae(nodeTest.getNextFormulae()) && 
				this.programState.getStateSpaceId() == nodeTest.programState.getStateSpaceId();
	}
}