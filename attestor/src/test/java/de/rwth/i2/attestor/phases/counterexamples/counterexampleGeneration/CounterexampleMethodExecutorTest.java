package de.rwth.i2.attestor.phases.counterexamples.counterexampleGeneration;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public class CounterexampleMethodExecutorTest {

    private SceneObject sceneObject;
    private MockupHeaps mockupHeaps;

    private MockupScopeExtractor scopeExtractor;
    private MockupContractCollection contractCollection;
    private CounterexampleContractGenerator contractGenerator;

    @Before
    public void setUp() {

        sceneObject = new MockupSceneObject();
        mockupHeaps = new MockupHeaps(sceneObject);

        scopeExtractor = new MockupScopeExtractor(mockupHeaps);
        contractCollection = new MockupContractCollection(mockupHeaps);
        contractGenerator = new CounterexampleContractGenerator(
                (programStatePredicate, programState) -> {
                    Collection<ProgramState> result = new ArrayList<>();
                    result.add(sceneObject.scene().createProgramState(mockupHeaps.getPostcondition()));
                    return result;
                }
        );
    }

    @Test
    public void testSimplePositive() {

        CounterexampleMethodExecutor executor = new CounterexampleMethodExecutor(
                scopeExtractor,
                contractCollection,
                contractGenerator,
                heapConfiguration -> heapConfiguration
        );

        ProgramState input = sceneObject.scene().createProgramState(mockupHeaps.getHeap());
        Collection<ProgramState> resultStates = executor.getResultStates(input, input, null);
        ProgramState validFinalState = sceneObject.scene().createProgramState(mockupHeaps.getPostcondition());

        Collection<HeapConfiguration> predicate = contractGenerator.getRequiredFinalHeaps();
        assertNotNull(predicate);
        assertTrue(predicate.contains(validFinalState.getHeap()));

        assertEquals(1, resultStates.size());

        ProgramState obtainedState = resultStates.iterator().next();
        ProgramState expectedState = sceneObject.scene().createProgramState(mockupHeaps.getHeapOutsideScope()
                .builder()
                .replaceNonterminalEdge(
                        mockupHeaps.getPlaceholderEdge(),
                        mockupHeaps.getPostcondition()
                )
                .build()
        );
        assertEquals(expectedState, obtainedState);
    }

    @Test
    public void testNoMatch() {

        CounterexampleMethodExecutor executor = new CounterexampleMethodExecutor(
                scopeExtractor,
                contractCollection,
                contractGenerator,
                heapConfiguration -> sceneObject.scene().createHeapConfiguration()
        );

        ProgramState input = sceneObject.scene().createProgramState(mockupHeaps.getHeap());

        try {
            executor.getResultStates(null, input, null);
            fail("Should not be able to match contract.");
        } catch(IllegalStateException e) {
            // expected
        }

    }

}
