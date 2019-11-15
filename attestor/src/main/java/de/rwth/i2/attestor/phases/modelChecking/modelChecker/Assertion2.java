package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import java.util.LinkedList;
import java.util.List;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

/**
 * This class implements the states of the tableau method proof structure. Each state consists
 * of a program state and a list of (sub)formulae, which together form an assertion, that has
 * to be discharged. In contrast to {@link Assertion} next-formulae are managed in an own list.
 * 
 * @author sally 
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
        this.isContainedInTrace = isContainedInTrace;
    }
	
	/**
     * This constructor returns a new assertion as a copy of the provided one.
     * Note that the new assertion receives a shallow copy of the formulae list.
     *
     * @param assertion, the assertion to be copied
     */
	public Assertion2(Assertion2 assertion) {
		
		this.programState = assertion.getProgramState();
        this.formulae = new LinkedList<>(assertion.getFormulae());
        this.nextFormulae = new LinkedList<>(assertion.getNextFormulae());
        this.isTrue = assertion.isTrue();
        this.isContainedInTrace = assertion.isContainedInTrace;
        this.parent = assertion.parent;		
	}
	
	public void addNextFormula(Node formula) {

		if (!this.nextFormulae.contains(formula)) {
			this.nextFormulae.addFirst(formula);
		}
	}
	
	public void addNextFormulae(List<Node> formulae) {

		if (formulae != null) {
			for (Node formula : formulae) {
				addNextFormula(formula);
			}
		}
	}
	
	public void addFormula(Node formula) {
		
		if (!this.formulae.contains(formula)) {
			this.formulae.addFirst(formula);
		}
	}
	
	public void addFormulae(List<Node> formulae) {
		
		if (formulae != null) {
			for (Node formula : formulae) {
				addFormula(formula);
			}
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
	
	public void removeFirstFormula() {
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
	
	public boolean isTrue() {
		return this.isTrue;
	}
	
	public void setContainedInTrace(boolean isContainedInTrace) {
		this.isContainedInTrace = isContainedInTrace;
	}
	
	public boolean isContainedInTrace() {
		return this.isContainedInTrace;
	}
	
	public boolean containsFormula(Node formula) {
		return this.formulae.contains(formula);
	}
	
	public boolean containsNextFormula(Node formula) {
		return this.nextFormulae.contains(formula);
	}
	
	public boolean containsAllFormulae(List<Node> formulae) {
		
		if (formulae.size() == this.formulae.size()) {
			for (Node formula : formulae) {
				if (!this.formulae.contains(formula)) return false;
			}
			
			return true;
		}		
		
		return false;
	}
	
	public boolean containsAllNextFormulae(List<Node> formulae) {

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
	
	@Override
	public String toString() {
		return "(state: " + programState.getStateSpaceId() + ", PC: " + programState.getProgramCounter() 
		+ ", formulae: " + formulae + ", nextFormulae: " + nextFormulae + ")";
	}
}
