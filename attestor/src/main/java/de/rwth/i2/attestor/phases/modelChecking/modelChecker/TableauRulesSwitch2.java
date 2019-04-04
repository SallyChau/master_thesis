package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import java.util.HashSet;
import java.util.LinkedHashSet;

import de.rwth.i2.attestor.generated.analysis.AnalysisAdapter;
import de.rwth.i2.attestor.generated.node.AAndStateform;
import de.rwth.i2.attestor.generated.node.AAtomicpropTerm;
import de.rwth.i2.attestor.generated.node.AFalseTerm;
import de.rwth.i2.attestor.generated.node.ANegStateform;
import de.rwth.i2.attestor.generated.node.ANextLtlform;
import de.rwth.i2.attestor.generated.node.AOrStateform;
import de.rwth.i2.attestor.generated.node.AReleaseLtlform;
import de.rwth.i2.attestor.generated.node.AStateformLtlform;
import de.rwth.i2.attestor.generated.node.ATermLtlform;
import de.rwth.i2.attestor.generated.node.ATrueTerm;
import de.rwth.i2.attestor.generated.node.AUntilLtlform;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.generated.node.PStateform;
import de.rwth.i2.attestor.generated.node.PTerm;
import de.rwth.i2.attestor.generated.node.Start;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

/**
 * This class implements the tableau rules for LTL model checking. 
 * The syntax of formulae accepted by Attestor is given by the folloging CFG:
 * <ltlform> ::= <stateform> 
 *					| "X" <ltlform>           				# the formula holds in the next state
 *               	| "G" <ltlform>           				# the formula globally holds
 *               	| "F" <ltlform>           				# the formula eventually holds
 *					| <term>
 *					| "(" <ltlform> "U" <ltlform> ")"      	# the left formula holds until eventually the right formula holds
 *					| "(" <ltlform> "R" <ltlform> ")"      	# the right formula holds until the left formula becomes true
 *               	| "(" <ltlform> "->" <ltlform> ")"     	# the left formula implies the right formula
 *	
 * <stateform> ::= "!" <ltlform>                          	# negation 
 *					| "(" <ltlform> "&" <ltlform> ")"      	# conjunction
 *					| "(" <ltlform> "|" <ltlform> ")"      	# disjunction
 *
 * <term> ::= "true"
 *	        		| "false"	
 *					| <atomicprop>
 * <atomicprop> ::= "{" "L(" <NT> ")" "}"
 *	    			| "{" "visited" "}"
 *	    			| "{" "visited(" <variable> ")" "}"
 *	    			| "{" "identicNeighbours" "}"
 *	    			| "{" "isReachable(" <variable> "," <variable> ")" "}"
 *	    			| "{" "terminated" "}"
 *	    			| "{" <variable> " == " <variable> "}"
 *	    			| "{" <variable> " != " <variable> "}"
 * @author chau
 *
 */
public class TableauRulesSwitch2 extends AnalysisAdapter {
	
	ProgramState programState; // the program state of the current assertion
	 
	public TableauRulesSwitch2(ProgramState programState) {

        this.programState = programState;
	}

	/**
	 * 
	 */
	@Override
    public void caseStart(Start node) {
		
		throw (new RuntimeException());
    }

    /**
     * Case: Formula is a term formula in LTL form.
     * Forward the underlying PTerm of the formula as the out-map.
     */
	@Override
    public void caseATermLtlform(ATermLtlform node) {
    	
    	PTerm termform = node.getTerm();

        this.setIn(termform, this.getIn(node));
        termform.apply(this);
        this.setOut(node, this.getOut(termform));
    }

    /**
     * Case: Formula is state formula in LTL form. 
     * Forward the underlying state form of the formula as the out-map.
     */
	@Override
    public void caseAStateformLtlform(AStateformLtlform node) {
    	
    	PStateform stateform = node.getStateform();

        this.setIn(stateform, this.getIn(node));
        stateform.apply(this);
        this.setOut(node, this.getOut(stateform));
    }

	/**
	 * Case: Formula is an atomic proposition.
	 * Evaluate whether the program state satisfies the atomic proposition.
	 */
	@Override
    public void caseAAtomicpropTerm(AAtomicpropTerm node) {
    	
    	Assertion2 currentNode = (Assertion2) this.getIn(node);	
	    String expectedAP = node.toString().trim();

    	if (programState.satisfiesAP(expectedAP)) {
    		currentNode.setTrue();
            this.setOut(node, null); // successful
        } else {
        	removeFormulaAndSetOut(node);
        }
    }

    /**
     * Case: Formula is false.
     * Remove the formula from the node's formula list, as it can never be fulfilled.
     */
	@Override
    public void caseAFalseTerm(AFalseTerm node) {
    	
    	removeFormulaAndSetOut(node);
    }

	/**
	 * Case: Formula is true.
	 * Set the current node to true and return no successors. The current node is a leaf.
	 */
	@Override
    public void caseATrueTerm(ATrueTerm node) {
    	
    	Assertion2 currentNode = (Assertion2) this.getIn(node);
        currentNode.setTrue();

        this.setOut(node, null);
    }

    /**
     * Case: Formula is a negated state formula.
     * Treat negated state formulae similar to atomic propositions, i.e. evaluate directly. 
     * This is possible as formulae are required to be in PNF.
     */
	@Override
    public void caseANegStateform(ANegStateform node) {
    	
    	Assertion2 currentNode = (Assertion2) this.getIn(node);

        // Because of PNF we know that the negated LTL formula is a term
        assert node.getLtlform() instanceof ATermLtlform;

        ATermLtlform term = (ATermLtlform) node.getLtlform();
        String negExpectedAP = node.getLtlform().toString().trim();
        
    	if (term.getTerm() instanceof ATrueTerm) {
            removeFormulaAndSetOut(node);
        } else if (term.getTerm() instanceof AFalseTerm || !programState.satisfiesAP(negExpectedAP)) {
            currentNode.setTrue();
            this.setOut(node, null);
        } else {
            removeFormulaAndSetOut(node);
        }        
    }    

	/**
	 * Case: Formula is a conjunction.
	 * Split current node into two successor nodes, each containing either side of the conjunction.
	 */
	@Override
	public void caseAAndStateform(AAndStateform node) {
    	
    	Assertion2 successorNode1 = removeFormula(node);
    	successorNode1.addFormula(node.getLeftform());
    		
    	Assertion2 successorNode2 = removeFormula(node);
    	successorNode2.addFormula(node.getRightform()); 
    	
        HashSet<Assertion2> successors = new LinkedHashSet<>();
        successors.add(successorNode1);
        successors.add(successorNode2);
        this.setOut(node, successors);
    }

	/**
	 * Case: Formula is a disjunction.
	 * List of formulae of the successor node contains both sides of the disjunction.
	 */
	@Override
    public void caseAOrStateform(AOrStateform node) {
 
		Assertion2 successor = removeFormula(node);
		successor.addFormula(node.getLeftform());
		successor.addFormula(node.getRightform());

		HashSet<Assertion2> successors = new LinkedHashSet<>();
        successors.add(successor);
        this.setOut(node, successors);
    }

	/**
	 * Case: Formula is an Until LTL form.
	 * Split current node according to the Until expansion rule.
	 */
	@Override
    public void caseAUntilLtlform(AUntilLtlform node) {

		Assertion2 successorNode1 = removeFormula(node);
    	successorNode1.addFormula(node.getLeftform());
    	successorNode1.addFormula(node.getRightform()); 

    	Assertion2 successorNode2 = removeFormula(node);
    	successorNode2.addFormula(node.getRightform()); 
    	successorNode2.addNextFormula(node);
    	
    	HashSet<Assertion2> successors = new LinkedHashSet<>();
	    successors.add(successorNode1);
	    successors.add(successorNode2);
	    this.setOut(node, successors);
    }

	/**
	 * Case: Formula is a Release LTL form.
	 * Split current node according to the Release expansion rule.
	 */
	@Override
    public void caseAReleaseLtlform(AReleaseLtlform node) {

		Assertion2 successorNode1 = removeFormula(node);
    	successorNode1.addFormula(node.getRightform()); 

    	Assertion2 successorNode2 = removeFormula(node);
    	successorNode2.addFormula(node.getLeftform()); 
    	successorNode2.addNextFormula(node);
    	
    	HashSet<Assertion2> successors = new LinkedHashSet<>();
	    successors.add(successorNode1);
	    successors.add(successorNode2);
	    this.setOut(node, successors);
    }

    /**
     * Case: Formula is a Next LTL form.
     * Creates a copy of the current node, removes the current Next formula from the copy's formula list, and
     * adds the Next formula to the copy's list of Next formulae. 
     */
	@Override
    public void caseANextLtlform(ANextLtlform node) {

		Assertion2 successorNode = removeFormula(node);
		successorNode.addNextFormula(node.getLtlform());
		
		HashSet<Assertion2> successors = new LinkedHashSet<>();
	    successors.add(successorNode);
        this.setOut(node, successors);
    }    
    
    // ------------------------------------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------------------------------------
    
    /**
     * Creates a copy of the current node, removes the specified formula, and returns the new node.
     * 
     * @param node
     * 		formula to be removed from the list of formulae
     * @return
     * 		a node with the set of formulae of the current node without the passed formula
     */
	private Assertion2 removeFormula(Node node) {
		
		Assertion2 current = (Assertion2) this.getIn(node);

        Assertion2 currentCopy = new Assertion2(current);
        currentCopy.removeFirstFormula();

        return currentCopy;
	}
    
    /**
     * Creates a copy of the current node, removes the specified formula, and set the out-map to the new node.
     * 
     * @param node
     * 			formula to be removed from the list of formulae
     */
    private void removeFormulaAndSetOut(Node node) {
    	
    	Assertion2 resultNode = removeFormula(node);

        HashSet<Assertion2> successors = new LinkedHashSet<>();
        successors.add(resultNode);
        this.setOut(node, successors);
	}
}
