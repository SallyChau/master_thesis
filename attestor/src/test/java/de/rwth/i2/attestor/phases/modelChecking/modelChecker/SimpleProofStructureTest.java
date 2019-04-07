package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.symbolicExecution.stateSpaceGenerationImpl.InternalStateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class SimpleProofStructureTest {
	
	private SceneObject sceneObject;
    private HeapConfiguration hc;
    private StateSpace stateSpace;

    public SimpleProofStructureTest() {

        stateSpace = new InternalStateSpace(0);
    }

    @Before
    public void setup() {

        sceneObject = new MockupSceneObject();
        hc = sceneObject.scene().createHeapConfiguration();
    }

    @Test
    public void buildProofStructureTestAndStateform() {

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("({dll} & {tree})");
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

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.build(stateSpace, formula);

        // Expected output
        assertEquals(proofStruct.getLeaves().size(), 2);
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

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("({dll} | {tree})");
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

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Expected output
        assertEquals(proofStruct.getLeaves().size(), 1);
        for (Assertion2 assertion : proofStruct.getLeaves()) {
            // Make sure the one leaf is successful
            assertTrue(assertion.isTrue());
        }

        assertTrue(proofStruct.size() <= 3 && proofStruct.size() >= 2);
        assertTrue(proofStruct.isSuccessful());
    }

    @Test
    public void buildProofStructureTestNextLtlform() {

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("X {dll}");
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

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        assertTrue(proofStruct.isSuccessful());

    }


    @Test
    public void buildProofStructureTestNextNegLtlform() {

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("X ! {dll}");
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

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Expected output
        assertEquals(proofStruct.getLeaves().size(), 1);
        for (Assertion2 assertion : proofStruct.getLeaves()) {
            // Make sure all leaves are successful
            assertTrue(assertion.isTrue());

        }
        assertTrue(proofStruct.size() == 3);
        assertTrue(proofStruct.isSuccessful());

    }

    @Test
    public void buildProofStructureTestUntilLtlform() {

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("({dll} U {tree})");
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

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

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

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("(true U {tree})");
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

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Make sure that verification fails
        assertFalse(proofStruct.isSuccessful());
    }

    @Test
    public void buildProofStructureTestNegFinally() {

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("! F { tree }");
            formula.toPNF();
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

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Make sure that verification fails
        assertFalse(proofStruct.isSuccessful());
    }

    @Test
    public void buildProofStructureTestGloballyFinally() {

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("G F { tree }");
            formula.toPNF();
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

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Make sure that verification succeeds
        assertTrue(proofStruct.isSuccessful());
    }


    @Test
    public void buildProofStructureTestImpliesFalse() {

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("(F {tree} -> false)");
            formula.toPNF();
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

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Make sure that verification fails
        assertFalse(proofStruct.isSuccessful());
    }

    @Test
    public void buildProofStructureTestImpliesFalseLoop() {

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("(F {tree} -> false)");
            formula.toPNF();
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

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Make sure that verification fails
        assertFalse(proofStruct.isSuccessful());
    }

    @Test
    public void buildProofStructureTestUntilWithCycle() {

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("({dll} U {tree})");
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.addAP("{ dll }");

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        //this.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, initialState);

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

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

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("(({sll} U {dll}) | ({dll} R {sll}))");
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.addAP("{ sll }");

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        stateSpace.addControlFlowTransition(initialState, initialState);

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        assertEquals(proofStruct.getLeaves().size(), 2);
        for (Assertion2 assertion : proofStruct.getLeaves()) {
            // Make sure all leaves are successful
            assertTrue(assertion.isTrue());

        }
        assertTrue(proofStruct.isSuccessful());


    }

    @Test
    public void buildProofStructureTestUntilAndRelease() {

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("(({sll} U {dll}) & ({dll} R {sll}))");
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.addAP("{ sll }");

        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        //this.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, initialState);

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        HashSet<Assertion2> leaves = proofStruct.getLeaves();
        assertEquals(2, leaves.size());
        for (Assertion2 assertion : leaves) {
            // Make sure all leaves are successful
            assertTrue(assertion.isTrue());

        }
        assertFalse(proofStruct.isSuccessful());
    }

    @Test
    public void buildProofStructureComplexTest() {

        LTLFormula formula = null;
        try {
            formula = new LTLFormula("X({sll} U ({tree} R X {dll}))");
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        }

        ProgramState initialState = sceneObject.scene().createProgramState(hc);
        initialState.addAP("{ dll }");
        initialState.setProgramCounter(0);
        ProgramState state1 = sceneObject.scene().createProgramState(hc);
        state1.addAP("{ sll }");
        state1.setProgramCounter(1);
        ProgramState state2 = sceneObject.scene().createProgramState(hc);
        state2.addAP("{ tree }");
        state2.setProgramCounter(2);


        stateSpace.addStateIfAbsent(initialState);

        initialState.setProgramCounter(0);
        stateSpace.addStateIfAbsent(initialState);
        stateSpace.addInitialState(initialState);
        state1.setProgramCounter(1);
        stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, state1);
        stateSpace.addControlFlowTransition(state1, state1);
        state2.setProgramCounter(2);
        stateSpace.addStateIfAbsent(state2);
        stateSpace.addControlFlowTransition(state1, state2);
        stateSpace.addControlFlowTransition(state2, initialState);

        SimpleProofStructure proofStruct = new SimpleProofStructure();
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Expected output
        assertFalse(proofStruct.isSuccessful());
    }

}
