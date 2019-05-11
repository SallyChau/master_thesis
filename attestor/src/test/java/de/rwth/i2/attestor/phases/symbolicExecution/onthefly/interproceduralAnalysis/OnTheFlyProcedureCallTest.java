package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureRegistry;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProofStructure;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.OnTheFlyStateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.HeapConfigurationDummy;
import de.rwth.i2.attestor.phases.symbolicExecution.stateSpaceGenerationImpl.InternalStateSpace;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.OnTheFlyStateSpaceGenerator;
import de.rwth.i2.attestor.stateSpaceGeneration.Program;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;

public class OnTheFlyProcedureCallTest {

	SceneObject sceneObject = new MockupSceneObject();
	
	@Test
	public void testExecute() {
		OnTheFlyProcedureRegistry registry = mock(OnTheFlyProcedureRegistry.class);		
		
		Method methodMock = mock(Method.class);
		Set<Method> methodsToSkip = new HashSet<>();
		methodsToSkip.add(methodMock);
		
		when(registry.getMethodsToSkip()).thenReturn(methodsToSkip);
	
		HeapConfiguration preconditionDummy = new HeapConfigurationDummy("precondition");
		
		HeapConfigurationDummy finalHeap1 = new HeapConfigurationDummy("f1");
		ProgramState finalStateDummy1 = sceneObject.scene().createProgramState(finalHeap1);
		HeapConfigurationDummy finalHeap2 = new HeapConfigurationDummy("f2");
		ProgramState finalStateDummy2 = sceneObject.scene().createProgramState(finalHeap2);
		StateSpace fakeResult = new InternalStateSpace(3);
		fakeResult.addState(finalStateDummy1);
		fakeResult.setFinal(finalStateDummy1);
		fakeResult.addState(finalStateDummy2);
		fakeResult.setFinal(finalStateDummy2);

		OnTheFlyStateSpaceGenerator fakeGenerator = mock(OnTheFlyStateSpaceGenerator.class);

		OnTheFlyProofStructure proofStructure = mock(OnTheFlyProofStructure.class);
		
		OnTheFlyStateSpaceGeneratorFactory fakeFactory = createFakeFactory(fakeGenerator, fakeResult, proofStructure);
		
		OnTheFlyProcedureCall call = new OnTheFlyProcedureCall(methodMock, preconditionDummy, 
				fakeFactory, registry);
		
		call.execute();		
		
		try {
			verify(fakeGenerator).generate();
			verify(fakeGenerator, never()).generateAndCheck();
		} catch (StateSpaceGenerationAbortedException e) {
			fail("Exception invoked during state space generation.");
		}		
		
		verify(registry).registerStateSpace(call, fakeResult);
		verify(registry).registerProofStructure(call, proofStructure);
		verify(registry).registerReturnFormulae(any(OnTheFlyProcedureCall.class), any(Set.class));
		verify(registry).getMethodsToSkip();
		verifyNoMoreInteractions(registry);
		
		final ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
		verify(methodMock).addContract(captor.capture());
		assertEquals(preconditionDummy, captor.getValue().getPrecondition());
		assertThat(captor.getValue().getPostconditions(), containsInAnyOrder(finalHeap1, finalHeap2));
	}

	@Test
	public void testExecuteAndCheck() {
		OnTheFlyProcedureRegistry registry = mock(OnTheFlyProcedureRegistry.class);		
		
		Method methodMock = mock(Method.class);
	
		HeapConfiguration preconditionDummy = new HeapConfigurationDummy("precondition");
		
		HeapConfigurationDummy finalHeap1 = new HeapConfigurationDummy("f1");
		ProgramState finalStateDummy1 = sceneObject.scene().createProgramState(finalHeap1);
		HeapConfigurationDummy finalHeap2 = new HeapConfigurationDummy("f2");
		ProgramState finalStateDummy2 = sceneObject.scene().createProgramState(finalHeap2);
		
		StateSpace fakeResult = new InternalStateSpace(3);
		fakeResult.addState(finalStateDummy1);
		fakeResult.setFinal(finalStateDummy1);
		fakeResult.addState(finalStateDummy2);
		fakeResult.setFinal(finalStateDummy2);

		OnTheFlyStateSpaceGenerator fakeGenerator = mock(OnTheFlyStateSpaceGenerator.class);
		
		OnTheFlyProofStructure proofStructure = mock(OnTheFlyProofStructure.class);
		
		OnTheFlyStateSpaceGeneratorFactory fakeFactory = createFakeFactory(fakeGenerator, fakeResult, proofStructure);
		
		OnTheFlyProcedureCall call = new OnTheFlyProcedureCall(methodMock, preconditionDummy, 
				fakeFactory, registry);
		
		call.execute();		
		
		try {
			verify(fakeGenerator, never()).generate();
			verify(fakeGenerator).generateAndCheck();
		} catch (StateSpaceGenerationAbortedException e) {
			fail("Exception invoked during state space generation.");
		}	
		
		verify(registry).registerStateSpace(call, fakeResult);
		verify(registry).registerProofStructure(call, proofStructure);
		verify(registry).registerReturnFormulae(any(OnTheFlyProcedureCall.class), any(Set.class));
		verify(registry).getMethodsToSkip();
		verifyNoMoreInteractions(registry);
		
		final ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
		verify(methodMock).addContract(captor.capture());
		assertEquals(preconditionDummy, captor.getValue().getPrecondition());
		assertThat(captor.getValue().getPostconditions(), containsInAnyOrder(finalHeap1, finalHeap2));
	}

	/**
	 * creates a factory which directly returns a fake stateSpaceGenerator
	 * which in turn returns only fakeResult when invoked.
	 * Furthermore, a fakeScene is used which creates ProgramStatedummies
	 * (just to avoid nullPointers)
	 * @param fakeResult
	 * @return
	 */
	private OnTheFlyStateSpaceGeneratorFactory createFakeFactory(OnTheFlyStateSpaceGenerator fakeGenerator,
			StateSpace fakeResult, OnTheFlyProofStructure proofStructure) {
		
		try {
			when(fakeGenerator.generate()).thenReturn(fakeResult);
			when(fakeGenerator.generateAndCheck()).thenReturn(fakeResult);
		} catch (StateSpaceGenerationAbortedException e) {
			fail("As the method is mocked, it should not invoke an exception");
		}		
		
		when(proofStructure.isSuccessful()).thenReturn(true);
		when(fakeGenerator.getProofStructure()).thenReturn(proofStructure);
		
		OnTheFlyStateSpaceGeneratorFactory fakeFaktory = mock(OnTheFlyStateSpaceGeneratorFactory.class);
		when(fakeFaktory.create(any(Program.class), any(List.class), any(Set.class))).thenReturn(fakeGenerator);
		when(fakeFaktory.create(any(Program.class), any(ProgramState.class), any(ScopedHeapHierarchy.class), 
				any(Set.class))).thenReturn(fakeGenerator);
		when(fakeFaktory.scene()).thenReturn(sceneObject.scene());
		return fakeFaktory;
	}
}
