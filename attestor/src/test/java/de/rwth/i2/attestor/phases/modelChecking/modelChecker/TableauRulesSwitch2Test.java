package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.generated.node.AAndStateform;
import de.rwth.i2.attestor.generated.node.AAtomicpropTerm;
import de.rwth.i2.attestor.generated.node.ADllAtomicprop;
import de.rwth.i2.attestor.generated.node.AFalseTerm;
import de.rwth.i2.attestor.generated.node.ANegStateform;
import de.rwth.i2.attestor.generated.node.AReleaseLtlform;
import de.rwth.i2.attestor.generated.node.ASllAtomicprop;
import de.rwth.i2.attestor.generated.node.AStateformLtlform;
import de.rwth.i2.attestor.generated.node.ATermLtlform;
import de.rwth.i2.attestor.generated.node.ATreeAtomicprop;
import de.rwth.i2.attestor.generated.node.ATrueTerm;
import de.rwth.i2.attestor.generated.node.AUntilLtlform;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.generated.node.TAnd;
import de.rwth.i2.attestor.generated.node.TApdll;
import de.rwth.i2.attestor.generated.node.TApsll;
import de.rwth.i2.attestor.generated.node.TAptree;
import de.rwth.i2.attestor.generated.node.TFalse;
import de.rwth.i2.attestor.generated.node.TLcurlyparen;
import de.rwth.i2.attestor.generated.node.TLparen;
import de.rwth.i2.attestor.generated.node.TNeg;
import de.rwth.i2.attestor.generated.node.TRcurlyparen;
import de.rwth.i2.attestor.generated.node.TRelease;
import de.rwth.i2.attestor.generated.node.TRparen;
import de.rwth.i2.attestor.generated.node.TTrue;
import de.rwth.i2.attestor.generated.node.TUntil;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.symbolicExecution.stateSpaceGenerationImpl.InternalStateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class TableauRulesSwitch2Test {
	
	private SceneObject sceneObject;
    private HeapConfiguration hc;
    private ProgramState state;
    private StateSpace stateSpace;

    @Before
    public void setup() {

        sceneObject = new MockupSceneObject();
        hc = sceneObject.scene().createHeapConfiguration();
        state = sceneObject.scene().createProgramState(hc);
        state.addAP("{ sll }");
        stateSpace = new InternalStateSpace(0);
        stateSpace.addStateIfAbsent(state);
    }

    @Test
    public void caseAAtomicpropTerm() {

        Assertion2 currentVertex = new Assertion2(state, null);

        ASllAtomicprop ap = new ASllAtomicprop(new TLcurlyparen(), new TApsll(), new TRcurlyparen());
        AAtomicpropTerm term = new AAtomicpropTerm(ap);

        currentVertex.addFormula(term);

        // Check whether the generated assertion satisfies AP term
        TableauRulesSwitch2 rulesSwitch = new TableauRulesSwitch2(state);
        rulesSwitch.setIn(term, currentVertex);

        term.apply(rulesSwitch);

        assertTrue(currentVertex.isTrue());

        // Reset state and rule switch with new AP
        state = sceneObject.scene().createProgramState(hc);
        state.addAP("{ sll }");
        stateSpace.addStateIfAbsent(state);

        currentVertex = new Assertion2(state, currentVertex);

        ADllAtomicprop ap2 = new ADllAtomicprop(new TLcurlyparen(), new TApdll(), new TRcurlyparen());
        AAtomicpropTerm term2 = new AAtomicpropTerm(ap2);

        currentVertex.addFormula(term2);

        // Check whether the generated assertion satisfies AP term2
        rulesSwitch = new TableauRulesSwitch2(state);
        rulesSwitch.setIn(term2, currentVertex);

        term2.apply(rulesSwitch);

        assertFalse(currentVertex.isTrue());
        @SuppressWarnings("unchecked") HashSet<Assertion2> output = (HashSet<Assertion2>) rulesSwitch.getOut(term2);

        assertEquals(output.size(), 1);
        for (Assertion2 newAssertion : output) {
            assertTrue(newAssertion.getFormulae().isEmpty());
        }
    }

    @Test
    public void caseALtlTerm() {

        Assertion2 currentVertex = new Assertion2(state, null);

        ASllAtomicprop ap = new ASllAtomicprop(new TLcurlyparen(), new TApsll(), new TRcurlyparen());
        AAtomicpropTerm term = new AAtomicpropTerm(ap);
        ATermLtlform ltlTerm = new ATermLtlform(term);

        currentVertex.addFormula(ltlTerm);

        // Check whether the generated assertion satisfies AP term
        TableauRulesSwitch2 rulesSwitch = new TableauRulesSwitch2(state);
        rulesSwitch.setIn(ltlTerm, currentVertex);

        ltlTerm.apply(rulesSwitch);

        assertTrue(currentVertex.isTrue());
        assertEquals(rulesSwitch.getOut(ltlTerm), null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void caseAFalseTerm() {

        HeapConfiguration hc = sceneObject.scene().createHeapConfiguration();
        ProgramState state = sceneObject.scene().createProgramState(hc);
        state.addAP("{ sll }");

        Assertion2 currentVertex = new Assertion2(state, null);

        TFalse f = new TFalse();
        AFalseTerm term = new AFalseTerm(f);

        currentVertex.addFormula(term);
        // The current assertion holds one formulae
        assertEquals(currentVertex.getFormulae().size(), 1);

        // Check whether the generated assertion satisfies AP term "false"
        TableauRulesSwitch2 rulesSwitch = new TableauRulesSwitch2(state);
        rulesSwitch.setIn(term, currentVertex);

        term.apply(rulesSwitch);

        // The current assertion still holds one formulae
        assertEquals(currentVertex.getFormulae().size(), 1);

        assertFalse(currentVertex.isTrue());
        HashSet<Assertion2> output = (HashSet<Assertion2>) rulesSwitch.getOut(term);

        assertEquals(output.size(), 1);
        for (Assertion2 newAssertion : output) {
            assertTrue(newAssertion.getFormulae().isEmpty());
        }


    }

    @Test
    public void caseATrueTerm() {

        Assertion2 currentVertex = new Assertion2(state, null);

        TTrue t = new TTrue();
        ATrueTerm term = new ATrueTerm(t);

        currentVertex.addFormula(term);


        // Check whether the generated assertion satisfies AP term "true"
        TableauRulesSwitch2 rulesSwitch = new TableauRulesSwitch2(state);
        rulesSwitch.setIn(term, currentVertex);

        term.apply(rulesSwitch);

        assertTrue(currentVertex.isTrue());
        assertEquals(rulesSwitch.getOut(term), null);

    }

    @Test
    public void ANegStateform() {
        // Generate assertion

        HeapConfiguration hc = sceneObject.scene().createHeapConfiguration();
        ProgramState state = sceneObject.scene().createProgramState(hc);
        state.addAP("{ sll }");
        stateSpace.addStateIfAbsent(state);

        Assertion2 currentVertex = new Assertion2(state, null);

        ASllAtomicprop ap = new ASllAtomicprop(new TLcurlyparen(), new TApsll(), new TRcurlyparen());
        ATermLtlform apTerm = new ATermLtlform(new AAtomicpropTerm(ap));
        ANegStateform negStateForm = new ANegStateform(new TNeg(), apTerm);

        currentVertex.addFormula(negStateForm);

        // Check whether the generated assertion satisfies negated AP term "ap1"
        TableauRulesSwitch2 rulesSwitch = new TableauRulesSwitch2(state);
        rulesSwitch.setIn(negStateForm, currentVertex);

        negStateForm.apply(rulesSwitch);

        assertFalse(currentVertex.isTrue());

        HashSet<Assertion2> output = (HashSet<Assertion2>) rulesSwitch.getOut(negStateForm);

        assertEquals(output.size(), 1);
        for (Assertion2 outputAssertion : output) {
            assertTrue(outputAssertion.getFormulae().isEmpty());
        }
    }

    @Test
    public void AAndStateform() {

        Assertion2 currentVertex = new Assertion2(state, null);

        ASllAtomicprop ap1 = new ASllAtomicprop(new TLcurlyparen(), new TApsll(), new TRcurlyparen());
        AAtomicpropTerm term1 = new AAtomicpropTerm(ap1);
        ATermLtlform termLtl1 = new ATermLtlform(term1);

        ADllAtomicprop ap2 = new ADllAtomicprop(new TLcurlyparen(), new TApdll(), new TRcurlyparen());
        AAtomicpropTerm term2 = new AAtomicpropTerm(ap2);
        ATermLtlform termLtl2 = new ATermLtlform(term2);
        currentVertex.addFormula(termLtl2);

        AAndStateform andStateForm = new AAndStateform(new TLparen(), termLtl1, new TAnd(), termLtl2, new TRparen());
        currentVertex.addFormula(andStateForm);


        // Check whether the tableau rule application returns two new assertions with the same state but term1 (term2)
        // included instead of term1 and term2
        TableauRulesSwitch2 rulesSwitch = new TableauRulesSwitch2(state);
        rulesSwitch.setIn(andStateForm, currentVertex);

        andStateForm.apply(rulesSwitch);

        assertFalse(currentVertex.isTrue());
        @SuppressWarnings("unchecked") HashSet<Assertion2> output = (HashSet<Assertion2>) rulesSwitch.getOut(andStateForm);

        assertEquals(output.size(), 2);
        for (Assertion2 outputAssertion : output) {
            assertTrue(outputAssertion.getFormulae().contains(termLtl1) | outputAssertion.getFormulae().contains(termLtl2));
        }

    }

    @SuppressWarnings("unchecked")
    @Test
    public void caseAUntilLtlform() {

        Assertion2 currentVertex = new Assertion2(state, null);

        ASllAtomicprop ap1 = new ASllAtomicprop(new TLcurlyparen(), new TApsll(), new TRcurlyparen());
        AAtomicpropTerm term1 = new AAtomicpropTerm(ap1);
        ATermLtlform termLtl1 = new ATermLtlform(term1);

        ADllAtomicprop ap2 = new ADllAtomicprop(new TLcurlyparen(), new TApdll(), new TRcurlyparen());
        AAtomicpropTerm term2 = new AAtomicpropTerm(ap2);
        ATermLtlform termLtl2 = new ATermLtlform(term2);

        ATreeAtomicprop ap3 = new ATreeAtomicprop(new TLcurlyparen(), new TAptree(), new TRcurlyparen());
        AAtomicpropTerm term3 = new AAtomicpropTerm(ap3);
        ATermLtlform termLtl3 = new ATermLtlform(term3);

        AAndStateform andStateForm = new AAndStateform(new TLparen(), termLtl1, new TAnd(), termLtl2, new TRparen());
        AStateformLtlform andLTLForm = new AStateformLtlform(andStateForm);
        currentVertex.addFormula(andStateForm);

        AUntilLtlform untilForm = new AUntilLtlform(new TLparen(), termLtl3, new TUntil(), andLTLForm, new TRparen());
        currentVertex.addFormula(untilForm);

        // Check whether the tableau rule application returns two new assertions with the same state but term1 (term2)
        // included instead of term1 and term2
        TableauRulesSwitch2 rulesSwitch = new TableauRulesSwitch2(state);
        rulesSwitch.setIn(untilForm, currentVertex);

        untilForm.apply(rulesSwitch);
        assertFalse(currentVertex.isTrue());
        HashSet<Assertion2> output = (HashSet<Assertion2>) rulesSwitch.getOut(untilForm);

        assertEquals(output.size(), 2);

        boolean containsNextForm = false;
        for (Assertion2 generatedAssertion : output) {

            assertFalse(generatedAssertion.getFormulae().contains(untilForm));

            for (Node node : generatedAssertion.getNextFormulae()) {
                    
                assertEquals(node, generatedAssertion.getNextFormulae().getLast());
                containsNextForm = true;
            }
        }
        assertTrue(containsNextForm);
    }

    @Test
    public void caseAReleaseLtlform() {

        Assertion2 currentVertex = new Assertion2(state, null);

        ASllAtomicprop ap1 = new ASllAtomicprop(new TLcurlyparen(), new TApsll(), new TRcurlyparen());
        AAtomicpropTerm term1 = new AAtomicpropTerm(ap1);
        ATermLtlform termLtl1 = new ATermLtlform(term1);

        ADllAtomicprop ap2 = new ADllAtomicprop(new TLcurlyparen(), new TApdll(), new TRcurlyparen());
        AAtomicpropTerm term2 = new AAtomicpropTerm(ap2);
        ATermLtlform termLtl2 = new ATermLtlform(term2);
        currentVertex.addFormula(termLtl2);

        ATreeAtomicprop ap3 = new ATreeAtomicprop(new TLcurlyparen(), new TAptree(), new TRcurlyparen());
        AAtomicpropTerm term3 = new AAtomicpropTerm(ap3);
        ATermLtlform termLtl3 = new ATermLtlform(term3);

        AAndStateform andStateForm = new AAndStateform(new TLparen(), termLtl1, new TAnd(), termLtl2, new TRparen());
        AStateformLtlform andLTLForm = new AStateformLtlform(andStateForm);


        AReleaseLtlform releaseForm = new AReleaseLtlform(new TLparen(), termLtl3, new TRelease(), andLTLForm, new TRparen());
        currentVertex.addFormula(releaseForm);

        // Check whether the tableau rule application returns two new assertions with the same state but term1 (term2)
        // included instead of term1 and term2
        TableauRulesSwitch2 rulesSwitch = new TableauRulesSwitch2(state);
        rulesSwitch.setIn(releaseForm, currentVertex);

        releaseForm.apply(rulesSwitch);
        assertFalse(currentVertex.isTrue());
        HashSet<Assertion2> output = (HashSet<Assertion2>) rulesSwitch.getOut(releaseForm);

        assertEquals(output.size(), 2);

        boolean containsNextForm = false;
        for (Assertion2 generatedAssertion : output) {

            assertFalse(generatedAssertion.getFormulae().contains(releaseForm));

            for (Node node : generatedAssertion.getNextFormulae()) {
            	
                assertEquals(node, generatedAssertion.getNextFormulae().getLast());
                containsNextForm = true;                

                if (node.equals(andLTLForm)) {
                    assertEquals(generatedAssertion.getFormulae().size(), 2);
                }
            }
        }
        assertTrue(containsNextForm);
    }

}
