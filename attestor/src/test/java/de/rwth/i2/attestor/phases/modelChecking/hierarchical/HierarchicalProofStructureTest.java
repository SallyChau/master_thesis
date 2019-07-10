package de.rwth.i2.attestor.phases.modelChecking.hierarchical;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.Assertion2;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.ExampleRecursiveProgram;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureRegistry;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.RecursiveMethodExecutor;
import de.rwth.i2.attestor.phases.symbolicExecution.stateSpaceGenerationImpl.InternalStateSpace;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.ContractMatch;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;
import de.rwth.i2.attestor.procedures.ScopedHeap;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.types.Type;

public class HierarchicalProofStructureTest {

	private SceneObject sceneObject;
    private HeapConfiguration hc;
    private StateSpace stateSpace;
    private ComponentStateMachine csm;
    private Method method;
    private Map<ProgramState, ComponentStateMachine> boxes;
    
	@Before
    public void setup() {

		sceneObject = new MockupSceneObject();
        hc = sceneObject.scene().createHeapConfiguration();
        stateSpace = new InternalStateSpace(0);
        
        Type type = sceneObject.scene().getType("List");
        String paramName = "@this";
        SelectorLabel next = sceneObject.scene().getSelectorLabel("next");
        ExampleRecursiveProgram examplePrograms = new ExampleRecursiveProgram(sceneObject, type, paramName, next);

        method = sceneObject.scene().getOrCreateMethod("callNext");
        method.setBody(examplePrograms.getRecursiveProgram(method));
        
        ScopeExtractor scopeExtractor = mock(ScopeExtractor.class);
        when(scopeExtractor.extractScope(any())).thenReturn(mock(ScopedHeap.class));
        
    	ContractCollection contractCollection = mock(ContractCollection.class);
    	ProcedureRegistry procedureRegistry = mock(ProcedureRegistry.class);
    	
    	RecursiveMethodExecutor executor = spy(new RecursiveMethodExecutor(method, scopeExtractor, 
    			contractCollection, procedureRegistry));
    	when(executor.getContractCollection().matchContract(any())).thenReturn(mock(ContractMatch.class));
        method.setMethodExecution(executor);
        
		csm = spy(new ComponentStateMachine(method));
		boxes = new HashMap<>();
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
        proofStruct.build(stateSpace, formula);

        // Expected output
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Expected output
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Expected output
        assertEquals(1, proofStruct.getLeaves().size());
        for (Assertion2 assertion : proofStruct.getLeaves()) {
            // Make sure all leaves are successful
            assertTrue(assertion.isTrue());

        }
        
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Expected output
        assertEquals(3, proofStruct.getLeaves().size());
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
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
        //stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, initialState);

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Expected output
        assertEquals(1, proofStruct.getLeaves().size());
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        assertEquals(2, proofStruct.getLeaves().size());
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
        //stateSpace.addStateIfAbsent(state1);
        stateSpace.addControlFlowTransition(initialState, initialState);

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        Set<Assertion2> leaves = proofStruct.getLeaves();
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

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
        proofStruct.setBuildFullStructure();
        proofStruct.build(stateSpace, formula);

        // Expected output
        assertFalse(proofStruct.isSuccessful());
    }
    
    
    @Test
    public void buildProofStructureNestedPositiveTest() {
		
		LTLFormula formula = null;
		try {
		    formula = new LTLFormula("X { dll }");
		    formula.toPNF();
		} catch (Exception e) {
		    fail("Formula should parse correctly. No Parser and Lexer exception expected!");
		}
		
		ProgramState initialState = sceneObject.scene().createProgramState(hc);
		initialState.setProgramCounter(0);
		ProgramState state1 = sceneObject.scene().createProgramState(hc);
		state1.setProgramCounter(1);
		initialState.addAP("{ dll }");
		state1.addAP("{ dll }");
		
		stateSpace.addStateIfAbsent(initialState);
		stateSpace.addInitialState(initialState);
		stateSpace.addStateIfAbsent(state1);
		stateSpace.addControlFlowTransition(initialState, state1);
		stateSpace.addArtificialInfPathsTransition(state1);
        
		// invoke method call (start 2nd level of model checking)
		doReturn(method.getBody().getStatement(1)).when(csm).getSemanticsCommand(any());		
        when(csm.getStateSpace(any(), any())).thenReturn(stateSpace);
		when(boxes.get(any())).thenReturn(csm);
		
		// assume model checking of box was successful
		doReturn(true).when(csm).modelCheckingSuccessful(any(), any(), any());

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
        proofStruct.setBuildFullStructure();
        
        //when
        proofStruct.build(stateSpace, formula);     		
      		
  		//then
  		verify(csm).check(any(), any(), any());

        // Expected output
  		// result of top level CSM is relevant since model checking of box was successful
        assertTrue(proofStruct.isSuccessful());
    }
    
    
    @Test
    public void buildProofStructureNestedNegativeTest() {
		
		LTLFormula formula = null;
		try {
		    formula = new LTLFormula("X { dll }");
		    formula.toPNF();
		} catch (Exception e) {
		    fail("Formula should parse correctly. No Parser and Lexer exception expected!");
		}
		
		ProgramState initialState = sceneObject.scene().createProgramState(hc);
		initialState.setProgramCounter(0);
		ProgramState state1 = sceneObject.scene().createProgramState(hc);
		state1.setProgramCounter(1);
		initialState.addAP("{ dll }");
		state1.addAP("{ dll }");
		
		stateSpace.addStateIfAbsent(initialState);
		stateSpace.addInitialState(initialState);
		stateSpace.addStateIfAbsent(state1);
		stateSpace.addControlFlowTransition(initialState, state1);
		stateSpace.addArtificialInfPathsTransition(state1);
        
		// invoke method call (start 2nd level of model checking)
		doReturn(method.getBody().getStatement(1)).when(csm).getSemanticsCommand(any());		
        when(csm.getStateSpace(any(), any())).thenReturn(stateSpace);
		when(boxes.get(any())).thenReturn(csm);
		
		// assume model checking of box was not successful
		doReturn(false).when(csm).modelCheckingSuccessful(any(), any(), any());
		doReturn(mock(HierarchicalFailureTrace.class)).when(csm).getHierarchicalFailureTrace(any(), any(), any());

        HierarchicalProofStructure proofStruct = new HierarchicalProofStructure(method, boxes);
        proofStruct.setBuildFullStructure();
        
        //when
        proofStruct.build(stateSpace, formula);     		
      		
  		//then
  		verify(csm).check(any(), any(), any());

        // Expected output
  		// result of top level CSM is not relevant since model checking of box was successful
        assertFalse(proofStruct.isSuccessful());
    }
}
