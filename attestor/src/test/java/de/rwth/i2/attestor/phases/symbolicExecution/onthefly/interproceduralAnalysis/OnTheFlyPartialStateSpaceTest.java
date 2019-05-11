package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.rwth.i2.attestor.graph.heap.internal.InternalHeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyPartialStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.HeapConfigurationDummy;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.PartialStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.stateSpaceGenerationImpl.InternalStateSpace;
import de.rwth.i2.attestor.programState.defaultState.DefaultProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class OnTheFlyPartialStateSpaceTest {

	@Test
	public void testEqualsSimple() {
		StateSpace stateSpace = new InternalStateSpace(0);
		
		ProgramState programState1 = new DefaultProgramState( new HeapConfigurationDummy("a heap") );
		programState1.setContainingStateSpace(stateSpace);
		OnTheFlyPartialStateSpace p1 = new OnTheFlyPartialStateSpace(programState1, null);
		
		ProgramState programState2 = new DefaultProgramState( new HeapConfigurationDummy("a heap"));
		programState2.setContainingStateSpace(stateSpace);
		OnTheFlyPartialStateSpace p2 = new OnTheFlyPartialStateSpace(programState2, null);
		
		assertEquals( p1, p2 );
	}
	
	@Test
	public void testEqualsWithChangeInStateSpace() {
		StateSpace stateSpace = new InternalStateSpace(0);
		
		ProgramState programState1 = new DefaultProgramState(new InternalHeapConfiguration());
		programState1.setContainingStateSpace(stateSpace);
		PartialStateSpace p1 = new OnTheFlyPartialStateSpace(programState1, null);
		
		ProgramState programState2 = new DefaultProgramState(new InternalHeapConfiguration());
		stateSpace.addInitialState(programState1);
		programState2.setContainingStateSpace(stateSpace);
		PartialStateSpace p2 = new OnTheFlyPartialStateSpace(programState2, null);
		
		assertEquals( p1, p2 );
	}

	@Test
	public void testHashCodeSimple() {
		StateSpace stateSpace = new InternalStateSpace(0);
		
		ProgramState programState1 = new DefaultProgramState(new HeapConfigurationDummy("a heap"));
		programState1.setContainingStateSpace(stateSpace);
		PartialStateSpace p1 = new OnTheFlyPartialStateSpace(programState1, null);
		
		ProgramState programState2 = new DefaultProgramState(new HeapConfigurationDummy("a heap"));
		programState2.setContainingStateSpace(stateSpace);
		PartialStateSpace p2 = new OnTheFlyPartialStateSpace(programState2, null);
		
		assertEquals( p1.hashCode(), p2.hashCode() );
	}
}
