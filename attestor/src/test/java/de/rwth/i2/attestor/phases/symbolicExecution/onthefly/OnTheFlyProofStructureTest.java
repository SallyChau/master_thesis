package de.rwth.i2.attestor.phases.symbolicExecution.onthefly;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.Assertion2;
import de.rwth.i2.attestor.phases.symbolicExecution.stateSpaceGenerationImpl.InternalStateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class OnTheFlyProofStructureTest {

	private SceneObject sceneObject;
    private HeapConfiguration hc;
    private StateSpace stateSpace;
    
	@Before
    public void setup() {

        sceneObject = new MockupSceneObject();
        hc = sceneObject.scene().createHeapConfiguration();
        stateSpace = new InternalStateSpace(0);
    }
	
	@Test
    public void buildProofStructureTestAndStateform() {

		Set<Node> formulae = new HashSet<>();
        try {
            formulae.add(new LTLFormula("({dll} & {tree})").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.addAP("{ dll }");
        initialState.setProgramCounter(0);
        ProgramState state1 = sceneObject.scene().createProgramState(hc);
        state1.addAP("{ tree }");
        state1.setProgramCounter(1);

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, state1);
        stateSpace.addArtificialInfPathsTransition(state1);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        Set<Node> nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();

        // Expected output
        assertTrue(nextFormulaeToCheck.isEmpty());
        assertEquals(2, proofStruct.getLeaves().size());
        boolean successful = true;
        for (Assertion2 assertion : proofStruct.getLeaves()) {
            // Make sure one leaf is not successful
            if (!assertion.isTrue()) {
                successful = false;
            }
        }
        assertFalse(successful);

        assertTrue(proofStruct.size() == 4);
        assertFalse(proofStruct.isSuccessful());
    }

	
    @Test
    public void buildProofStructureTestOrStateform() {

    	Set<Node> formulae = new HashSet<>();
        try {
            formulae.add(new LTLFormula("({dll} | {tree})").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.addAP("{ dll }");
        initialState.setProgramCounter(0);
        ProgramState state1 = sceneObject.scene().createProgramState(hc);
        state1.addAP("{ tree }");
        state1.setProgramCounter(1);

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, state1);
        stateSpace.addArtificialInfPathsTransition(state1);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        Set<Node> nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();

        // Expected output
        assertTrue(nextFormulaeToCheck.isEmpty());
        assertEquals(1, proofStruct.getLeaves().size());
        for (Assertion2 assertion : proofStruct.getLeaves()) {
            // Make sure the one leaf is successful
            assertTrue(assertion.isTrue());
        }

        assertTrue(proofStruct.size() <= 3 && proofStruct.size() >= 2);
        assertTrue(proofStruct.isSuccessful());
    }


    @Test
    public void buildProofStructureTestNextLtlform() {

    	Set<Node> formulae = new HashSet<>();
        try {
            formulae.add(new LTLFormula("X {dll}").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.setProgramCounter(0);
        ProgramState state1 = sceneObject.scene().createProgramState(hc);
        state1.addAP("{ dll }");
        state1.setProgramCounter(1);

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, state1);
        stateSpace.addArtificialInfPathsTransition(state1);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        
        Set<Node> nextFormulaeToCheck = new HashSet<>();
        while(proofStruct.isContinuable()) {        
	        nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();
	        if (!nextFormulaeToCheck.isEmpty()) proofStruct.addAssertion(state1, nextFormulaeToCheck);
        }
        
        assertTrue(proofStruct.isSuccessful());
    }


    @Test
    public void buildProofStructureTestNextNegLtlform() {

    	Set<Node> formulae = new HashSet<>();
        try {
            formulae.add(new LTLFormula("X ! {dll}").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.addAP("{ dll }");
        initialState.setProgramCounter(0);
        ProgramState state1 = sceneObject.scene().createProgramState(hc);
        state1.addAP("{ tree }");
        state1.setProgramCounter(1);

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, state1);
        stateSpace.addArtificialInfPathsTransition(state1);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        
        Set<Node> nextFormulaeToCheck = new HashSet<>();
        while(proofStruct.isContinuable()) {        
	        nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();
	        if (!nextFormulaeToCheck.isEmpty()) proofStruct.addAssertion(state1, nextFormulaeToCheck);
        }
        
        // proof structure is done
        assertTrue(nextFormulaeToCheck.isEmpty());
        
        assertEquals(1, proofStruct.getLeaves().size());
        for (Assertion2 assertion : proofStruct.getLeaves()) {
            // Make sure all leaves are successful
            assertTrue(assertion.isTrue());
        }
        
        assertTrue(proofStruct.size() == 3);
        assertTrue(proofStruct.isSuccessful());
    }

    
    @Test
    public void buildProofStructureTestUntilLtlform() {

    	Set<Node> formulae = new HashSet<>();
        try {
            formulae.add(new LTLFormula("({dll} U {tree})").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.addAP("{ dll }");
        initialState.setProgramCounter(0);
        ProgramState state1 = sceneObject.scene().createProgramState(hc);
        state1.addAP("{ tree }");
        state1.setProgramCounter(1);

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, state1);
        stateSpace.addArtificialInfPathsTransition(state1);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        
        Set<Node> nextFormulaeToCheck = new HashSet<>();
        while(proofStruct.isContinuable()) {        
	        nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();
	        if (!nextFormulaeToCheck.isEmpty()) proofStruct.addAssertion(state1, nextFormulaeToCheck);
        }
        
        // proof structure is done
        assertTrue(nextFormulaeToCheck.isEmpty());

        // Expected output
        assertEquals(proofStruct.getLeaves().size(), 3);
        for (Assertion2 assertion : proofStruct.getLeaves()) {
            // Make sure all leaves are successful
            assertTrue(assertion.isTrue());

        }
        assertTrue(proofStruct.size() <= 9 && proofStruct.size() >= 7);
        assertTrue(proofStruct.isSuccessful());
    }

    
    @Test
    public void buildProofStructureTestTrueUntil() {

    	Set<Node> formulae = new HashSet<>();
        try {
            formulae.add(new LTLFormula("(true U {tree})").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.setProgramCounter(0);
        ProgramState state1 = sceneObject.scene().createProgramState(hc);
        state1.setProgramCounter(1);

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, state1);
        stateSpace.addArtificialInfPathsTransition(state1);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        
        Set<Node> nextFormulaeToCheck = new HashSet<>();
        while(proofStruct.isContinuable()) {
	        nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();
	        
	        assertEquals(1, nextFormulaeToCheck.size());
	        
	        // add successor assertion to proof structure (normally done by statespaceGenerator)
	        proofStruct.addAssertion(state1, nextFormulaeToCheck);
        }

        // Make sure that verification fails
        assertFalse(proofStruct.isSuccessful());
    }

    
    @Test
    public void buildProofStructureTestNegFinally() {

    	Set<Node> formulae = new HashSet<>();
        try {
        	LTLFormula formula = new LTLFormula("! F { tree }");
            formula.toPNF();
            formulae.add(formula.getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.setProgramCounter(0);
        ProgramState state1 = sceneObject.scene().createProgramState(hc);
        state1.setProgramCounter(1);
        initialState.addAP("{ dll }");
        state1.addAP("{ tree }");

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, state1);
        stateSpace.addArtificialInfPathsTransition(state1);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        
        Set<Node> nextFormulaeToCheck = new HashSet<>();
        while(proofStruct.isContinuable()) {        
	        nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();
	        if (!nextFormulaeToCheck.isEmpty()) proofStruct.addAssertion(state1, nextFormulaeToCheck);
        }

        // Make sure that verification fails
        assertFalse(proofStruct.isSuccessful());
    }

    
    @Test
    public void buildProofStructureTestGloballyFinally() {

    	Set<Node> formulae = new HashSet<>();
        try {
        	LTLFormula formula = new LTLFormula("G F { tree }");
            formula.toPNF();
            formulae.add(formula.getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.setProgramCounter(0);
        ProgramState state1 = sceneObject.scene().createProgramState(hc);
        state1.setProgramCounter(1);
        initialState.addAP("{ dll }");
        state1.addAP("{ tree }");

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, state1);
        stateSpace.addArtificialInfPathsTransition(state1);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        Set<Node> nextFormulaeToCheck = new HashSet<>();
        while(proofStruct.isContinuable()) {        
	        nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();
	        if (!nextFormulaeToCheck.isEmpty()) proofStruct.addAssertion(state1, nextFormulaeToCheck);
        }

        // Make sure that verification succeeds
        assertTrue(proofStruct.isSuccessful());
    }


    @Test
    public void buildProofStructureTestImpliesFalse() {

    	Set<Node> formulae = new HashSet<>();
        try {
        	LTLFormula formula = new LTLFormula("(F {tree} -> false)");
            formula.toPNF();
            formulae.add(formula.getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.setProgramCounter(0);
        ProgramState state1 = sceneObject.scene().createProgramState(hc);
        state1.setProgramCounter(1);
        initialState.addAP("{ dll }");
        state1.addAP("{ tree }");

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, state1);
        stateSpace.addArtificialInfPathsTransition(state1);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        Set<Node> nextFormulaeToCheck = new HashSet<>();
        while(proofStruct.isContinuable()) {        
	        nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();
	        if (!nextFormulaeToCheck.isEmpty()) proofStruct.addAssertion(state1, nextFormulaeToCheck);
        }

        // Make sure that verification fails
        assertFalse(proofStruct.isSuccessful());
    }

    
    @Test
    public void buildProofStructureTestImpliesFalseLoop() {

    	Set<Node> formulae = new HashSet<>(); 
        try {
        	LTLFormula formula = new LTLFormula("(F {tree} -> false)");
            formula.toPNF();
            formulae.add(formula.getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.setProgramCounter(0);
        ProgramState state1 = sceneObject.scene().createProgramState(hc);
        state1.setProgramCounter(1);
        initialState.addAP("{ dll }");
        state1.addAP("{ tree }");

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, initialState);
        stateSpace.addControlFlowTransition(initialState, state1);
        stateSpace.addArtificialInfPathsTransition(state1);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        Set<Node> nextFormulaeToCheck = new HashSet<>();
        while(proofStruct.isContinuable()) {        
	        nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();
	        if (!nextFormulaeToCheck.isEmpty()) proofStruct.addAssertion(state1, nextFormulaeToCheck);
        }        

        // Make sure that verification fails
        assertFalse(proofStruct.isSuccessful());
    }

    
    @Test
    public void buildProofStructureTestUntilWithCycle() {

    	Set<Node> formulae = new HashSet<>(); 
        try {
            formulae.add(new LTLFormula("({dll} U {tree})").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.addAP("{ dll }");

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        //this.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, initialState);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        Set<Node> nextFormulaeToCheck = new HashSet<>();
        while(proofStruct.isContinuable()) {        
	        nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();
	        if (!nextFormulaeToCheck.isEmpty()) proofStruct.addAssertion(initialState, nextFormulaeToCheck);
        }               
        
        // Expected output
        assertEquals(proofStruct.getLeaves().size(), 1);
        for (Assertion2 assertion : proofStruct.getLeaves()) {
            // Make sure all leaves are successful
            assertTrue(assertion.isTrue());

        }
        assertTrue(proofStruct.size() >= 4 && proofStruct.size() <= 5);
        assertFalse(proofStruct.isSuccessful());
    }

    
    @Test
    public void buildProofStructureTestUntilOrRelease() {

    	Set<Node> formulae = new HashSet<>(); 
        try {
            formulae.add(new LTLFormula("(({sll} U {dll}) | ({dll} R {sll}))").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.addAP("{ sll }");

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addControlFlowTransition(initialState, initialState);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        Set<Node> nextFormulaeToCheck = new HashSet<>();
        while(proofStruct.isContinuable()) {        
	        nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();
	        if (!nextFormulaeToCheck.isEmpty()) proofStruct.addAssertion(initialState, nextFormulaeToCheck);
        }

        assertEquals(proofStruct.getLeaves().size(), 2);
        for (Assertion2 assertion : proofStruct.getLeaves()) {
            // Make sure all leaves are successful
            assertTrue(assertion.isTrue());

        }
        
        assertTrue(proofStruct.isSuccessful());
    }

    
    @Test
    public void buildProofStructureTestUntilAndRelease() {

    	Set<Node> formulae = new HashSet<>(); 
        try {
            formulae.add(new LTLFormula("(({sll} U {dll}) & ({dll} R {sll}))").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.addAP("{ sll }");

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        //this.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, initialState);

        OnTheFlyProofStructure proofStruct = new OnTheFlyProofStructure();
        proofStruct.addAssertion(initialState, formulae);
        Set<Node> nextFormulaeToCheck = new HashSet<>();
        while(proofStruct.isContinuable()) {        
	        nextFormulaeToCheck = proofStruct.buildAndGetNextFormulaeToCheck();
	        if (!nextFormulaeToCheck.isEmpty()) proofStruct.addAssertion(initialState, nextFormulaeToCheck);
        }                
        assertFalse(proofStruct.isSuccessful());
    }
}
