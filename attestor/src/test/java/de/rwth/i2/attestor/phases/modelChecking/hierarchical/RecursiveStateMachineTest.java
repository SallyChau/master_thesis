package de.rwth.i2.attestor.phases.modelChecking.hierarchical;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class RecursiveStateMachineTest {
	
	@Test
	public void buildTest() {
		Map<StateSpace, ProcedureCall> stateSpaceToAnalyzedCall = new HashMap<>();
		
		// Method 1
		ProcedureCall call1 = mock(ProcedureCall.class);
		Method method1 = mock(Method.class);
		method1.setName("method1");
		when(call1.getMethod()).thenReturn(method1);
		
		StateSpace stateSpace1 = mock(StateSpace.class);		

		ProgramState callingState1 = mock(ProgramState.class);
		when(callingState1.getContainingStateSpace()).thenReturn(stateSpace1);
		
		// Method 2
		ProcedureCall call2 = mock(ProcedureCall.class);
		Method method2 = mock(Method.class);
		method1.setName("method2");
		when(call2.getMethod()).thenReturn(method2);
		
		StateSpace stateSpace2 = mock(StateSpace.class);	
		
		ProgramState callingState2 = mock(ProgramState.class);
		when(callingState2.getContainingStateSpace()).thenReturn(stateSpace2);
		
		stateSpaceToAnalyzedCall.put(stateSpace1, call1);
		stateSpaceToAnalyzedCall.put(stateSpace2, call2);
		
		Map<ProgramState, ProcedureCall> callingStatesToCall = new HashMap<>();
		callingStatesToCall.put(callingState1, call1);
		callingStatesToCall.put(callingState2, call2);
		
		List<String> methodsToSkip = new LinkedList<>();
		
		RecursiveStateMachine rsm = new RecursiveStateMachine(stateSpaceToAnalyzedCall, 
				callingStatesToCall, methodsToSkip);
		
		assertEquals(2, rsm.getSize());
		
		ComponentStateMachine csm1 = rsm.getComponentStateMachine(method1);
		assertNotNull(csm1);
		assertEquals(1, csm1.getBoxes().values().size());
		
		ComponentStateMachine csm2 = rsm.getComponentStateMachine(method2);
		assertNotNull(csm2);
		assertEquals(1, csm2.getBoxes().values().size());
		
		assertEquals(2, rsm.getSize());
	}

}
