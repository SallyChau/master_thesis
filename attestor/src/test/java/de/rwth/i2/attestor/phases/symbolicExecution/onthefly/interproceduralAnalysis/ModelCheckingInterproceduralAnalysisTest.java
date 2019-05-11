package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyPartialStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProofStructure;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.OnTheFlyStateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.stateSpaceGenerationImpl.InternalStateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class ModelCheckingInterproceduralAnalysisTest {

	static final Scene scene = new MockupSceneObject().scene();

	private ModelCheckingInterproceduralAnalysis analysis;
	private StateSpace ssWithoutFinalStates;
	private InternalStateSpace ssWithFinalStates;
	

	@Before
	public void setUp() throws Exception {
		analysis = spy(new ModelCheckingInterproceduralAnalysis());
		ssWithoutFinalStates = new InternalStateSpace(5);
		ssWithFinalStates = new InternalStateSpace(2);
		ProgramState state = scene.createProgramState();
		ssWithFinalStates.addState(state );
		ssWithFinalStates.setFinal(state);
	}

	@Test
	public void testRun_WhenCallGeneratesNoFinalStates_DependenciesNotNotified() {
		//given
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);
		when(call.execute()).thenReturn(ssWithoutFinalStates);
		
		analysis.registerProcedureCall(call);
		
		OnTheFlyProofStructure proofStructure = mock(OnTheFlyProofStructure.class);
		analysis.callToProofStructure.put(call, proofStructure);
		when(proofStructure.isSuccessful()).thenReturn(true);
		
		//when
		analysis.run();
		
		//then
		verify(analysis, never()).notifyDependencies(any());
		assertTrue(proofStructure.isSuccessful());
	}
	
	@Test
	public void testRun_WhenCallGeneratesFinalStates_DependenciesAreNotified() {
		//given
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);
		when( call.execute() ).thenReturn( ssWithFinalStates );
		
		analysis.registerProcedureCall(call);
		
		OnTheFlyProofStructure proofStructure = mock(OnTheFlyProofStructure.class);
		analysis.callToProofStructure.put(call, proofStructure);
		when(proofStructure.isSuccessful()).thenReturn(true);
		
		//when
		analysis.run();
		
		//then
		verify(analysis).notifyDependencies(any());
		assertTrue(proofStructure.isSuccessful());
	}
	
	@Test
	public void testRun_WhenCallProofStructureUnsuccessful_ProofStructureIsAborted() {
		//given
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);
		when(call.execute()).thenReturn( ssWithFinalStates );
		
		analysis.registerProcedureCall(call);
		
		OnTheFlyProofStructure proofStructure = mock(OnTheFlyProofStructure.class);
		analysis.callToProofStructure.put(call, proofStructure);
		when(proofStructure.isSuccessful()).thenReturn(false);
		
		FailureTrace failureTrace = mock(FailureTrace.class);
		when(proofStructure.getFailureTrace(any())).thenReturn(failureTrace);
		
		//when
		analysis.run();
		
		//then
		verify(analysis).notifyDependencies(any());
		verify(analysis).abortDependingProofStructures(any());
		assertFalse(proofStructure.isSuccessful());
	}
	
	@Test
	public void testRun_WhenCallProofStructureUnsuccessful_DependenedProofStructureIsAborted() {
		//given
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);
		when(call.execute()).thenReturn(ssWithFinalStates);
		
		analysis.registerProcedureCall(call);
		
		OnTheFlyProofStructure proofStructure = mock(OnTheFlyProofStructure.class);
		analysis.callToProofStructure.put(call, proofStructure);
		when(proofStructure.isSuccessful()).thenReturn(false);
		
		FailureTrace failureTrace = mock(FailureTrace.class);
		when(proofStructure.getFailureTrace(any())).thenReturn(failureTrace);
		
		// dependent partial state space
		OnTheFlyProcedureCall dependentCall = mock(OnTheFlyProcedureCall.class);
		ProgramState callingState = mock(ProgramState.class);
		OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory = mock(OnTheFlyStateSpaceGeneratorFactory.class);
		OnTheFlyPartialStateSpace toContinue = new FakeOnTheFlyPartialStateSpace(callingState, stateSpaceGeneratorFactory,
				ssWithoutFinalStates, ssWithFinalStates);
		analysis.stateSpaceToAnalyzedCall.put(ssWithoutFinalStates, dependentCall);
		analysis.registerDependency(call, toContinue);
		OnTheFlyProofStructure dependentProofStructure = mock(OnTheFlyProofStructure.class);
		analysis.callToProofStructure.put(dependentCall, dependentProofStructure);
		FailureTrace dependentFailureTrace = mock(FailureTrace.class);
		when(dependentProofStructure.getFailureTrace(any())).thenReturn(dependentFailureTrace);
		
		//when
		analysis.run();
		
		//then
		verify(analysis).notifyDependencies(any());		
		verify(analysis, times(2)).abortDependingProofStructures(any());
		assertFalse(proofStructure.isSuccessful());
		assertFalse(dependentProofStructure.isSuccessful());
	}

	@Test
	public void testRun_WhenCallProofStructureSuccessful_ContinuationFormulaeIsCorrect() {
		//given
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);
		when(call.execute()).thenReturn(ssWithFinalStates);
		
		analysis.registerProcedureCall(call);
		
		Set<Node> formulae = new HashSet<>();
        try {
            formulae.add(new LTLFormula("({dll} & {tree})").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        } 
		analysis.registerReturnFormulae(call, formulae);
		
		OnTheFlyProofStructure proofStructure = mock(OnTheFlyProofStructure.class);
		analysis.callToProofStructure.put(call, proofStructure);
		when(proofStructure.isSuccessful()).thenReturn(true);
		
		FailureTrace failureTrace = mock(FailureTrace.class);
		when(proofStructure.getFailureTrace(any())).thenReturn(failureTrace);
		
		// dependent partial state space
		OnTheFlyProcedureCall dependentCall = mock(OnTheFlyProcedureCall.class);
		ProgramState callingState = mock(ProgramState.class);
		OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory = mock(OnTheFlyStateSpaceGeneratorFactory.class);
		OnTheFlyPartialStateSpace toContinue = new FakeOnTheFlyPartialStateSpace(callingState, stateSpaceGeneratorFactory,
				ssWithoutFinalStates, ssWithoutFinalStates);
		analysis.stateSpaceToAnalyzedCall.put(ssWithoutFinalStates, dependentCall);
		analysis.registerDependency(call, toContinue);
		OnTheFlyProofStructure dependentProofStructure = mock(OnTheFlyProofStructure.class);
		when(dependentProofStructure.isSuccessful()).thenReturn(true);
		analysis.callToProofStructure.put(dependentCall, dependentProofStructure);
		FailureTrace dependentFailureTrace = mock(FailureTrace.class);
		when(dependentProofStructure.getFailureTrace(any())).thenReturn(dependentFailureTrace);
		
		//when
		analysis.run();
		
		//then
		assertEquals(formulae, analysis.partialStateSpaceToContinueFormulae.get(toContinue));
		verify(analysis).notifyDependencies(any());		
		verify(analysis, never()).abortDependingProofStructures(any());
		assertTrue(proofStructure.isSuccessful());
		assertTrue(dependentProofStructure.isSuccessful());
	}

	@Test
	public void testRun_WhenContinuationGeneratesNoFinalStates_DependenciesNotNotified() {
		//given
		ProgramState callingState = mock(ProgramState.class);
		OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory = mock(OnTheFlyStateSpaceGeneratorFactory.class);
		OnTheFlyPartialStateSpace toContinue = new FakeOnTheFlyPartialStateSpace(callingState, stateSpaceGeneratorFactory,
				ssWithoutFinalStates, ssWithoutFinalStates);
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);
		
		analysis.stateSpaceToAnalyzedCall.put(ssWithoutFinalStates, call);
		analysis.remainingPartialStateSpaces.push(toContinue);
		
		OnTheFlyProofStructure proofStructure = mock(OnTheFlyProofStructure.class);
		analysis.callToProofStructure.put(call, proofStructure);
		when(proofStructure.isSuccessful()).thenReturn(true);
		
		//when
		analysis.run();
		
		//then
		verify(analysis, never()).notifyDependencies(any());
		assertTrue(proofStructure.isSuccessful());
	}
	
	@Test
	public void testRun_WhenContinuationGeneratesFinalStates_DependenciesAreNotified() {
		//given
		ProgramState callingState = mock(ProgramState.class);
		OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory = mock(OnTheFlyStateSpaceGeneratorFactory.class);
		OnTheFlyPartialStateSpace toContinue = new FakeOnTheFlyPartialStateSpace(callingState, stateSpaceGeneratorFactory,
				ssWithoutFinalStates, ssWithFinalStates);		
		
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);		
		analysis.stateSpaceToAnalyzedCall.put(ssWithoutFinalStates, call);
		analysis.remainingPartialStateSpaces.push(toContinue);
		
		OnTheFlyProofStructure proofStructure = mock(OnTheFlyProofStructure.class);
		analysis.callToProofStructure.put(call, proofStructure);
		when(proofStructure.isSuccessful()).thenReturn(true);
		
		//when
		analysis.run();
		
		//then
		verify(analysis).notifyDependencies(any());
		assertTrue(proofStructure.isSuccessful());
	}
	
	@Test
	public void testRun_WhenContinuationProofStructureUnsuccessful_ProofStructureIsAborted() {
		//given
		ProgramState callingState = mock(ProgramState.class);
		OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory = mock(OnTheFlyStateSpaceGeneratorFactory.class);
		OnTheFlyPartialStateSpace toContinue = new FakeOnTheFlyPartialStateSpace(callingState, stateSpaceGeneratorFactory,
				ssWithoutFinalStates, ssWithFinalStates);		
		
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);		
		analysis.stateSpaceToAnalyzedCall.put(ssWithoutFinalStates, call);
		analysis.remainingPartialStateSpaces.push(toContinue);
		
		OnTheFlyProofStructure proofStructure = mock(OnTheFlyProofStructure.class);
		analysis.callToProofStructure.put(call, proofStructure);
		when(proofStructure.isSuccessful()).thenReturn(false);
		
		FailureTrace failureTrace = mock(FailureTrace.class);
		when(proofStructure.getFailureTrace(any())).thenReturn(failureTrace);
		
		//when
		analysis.run();
		
		//then
		verify(analysis, never()).notifyDependencies(any());
		verify(analysis).abortDependingProofStructures(any());
		assertFalse(proofStructure.isSuccessful());
	}
}
