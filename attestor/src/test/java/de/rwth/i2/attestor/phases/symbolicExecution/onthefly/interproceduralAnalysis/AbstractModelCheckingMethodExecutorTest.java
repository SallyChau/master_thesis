package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureRegistry;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.HeapConfigurationDummy;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.ContractMatch;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.NoContractMatch;
import de.rwth.i2.attestor.procedures.ScopedHeap;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public class AbstractModelCheckingMethodExecutorTest {

	static Scene scene;
	private Method method;
	private ContractCollection contractCollection;
	private OnTheFlyProcedureRegistry procedureRegistry;
	private AbstractModelCheckingMethodExecutor methodExecutor;
	private ProgramState callingState;
	private HeapConfigurationDummy heapInScope;
	private ScopedHeap scopedHeap;
	
	
	@BeforeClass
	public static void SetUp() {
		scene = new MockupSceneObject().scene();
	}
	
	@Before
	public void init() {
		method = mock(Method.class);
		contractCollection = mock(ContractCollection.class);
		procedureRegistry = mock(OnTheFlyProcedureRegistry.class);
		methodExecutor = spy(new FakeModelCheckingMethodExecutor(method, null, contractCollection, 
				procedureRegistry));
		
		Set<Node> formulae = new HashSet<>();
        try {
            formulae.add(new LTLFormula("({dll} & {tree})").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        } 
        
        methodExecutor.setModelCheckingFormulae(formulae);
		
		HeapConfigurationDummy callingHeap = new HeapConfigurationDummy("callingState");
		callingState = scene.createProgramState( callingHeap );
		
		heapInScope = new HeapConfigurationDummy("heapInScope");
		scopedHeap = mock(ScopedHeap.class);
		when(scopedHeap.getHeapInScope()).thenReturn(heapInScope);		
	}

	@Test
	public void testGetPostconditions_WhenContractIsPresent() {
		
	//given
		//ensure it does not match the given heap
		ContractMatch match = mock(ContractMatch.class);
		when(match.hasMatch()).thenReturn(true);
		when(contractCollection.matchContract(heapInScope)).thenReturn(match);
		
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);
		when(procedureRegistry.getProcedureCall(any(), any(), any())).thenReturn(call);
		
	//when
		methodExecutor.getPostconditions(callingState, scopedHeap, null);
	//then
		//ensure the dependency is registered
		verify(procedureRegistry).registerDependency(eq(callingState), any());
		
		//ensure the contractCollection is not altered
		verify(contractCollection).matchContract(heapInScope);
		verifyNoMoreInteractions(contractCollection);
		
		verify(methodExecutor, never()).generateAndAddContract( any() );
	}
	
	@Test
	public void testGetPrecondition_WhenContractIsNotPresent() {
	//given
		//ensure it does not match the given heap		
		ContractMatch noMatch = NoContractMatch.NO_CONTRACT_MATCH;
		when(contractCollection.matchContract(heapInScope)).thenReturn(noMatch);
		
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);
		when(procedureRegistry.getProcedureCall(any(), any(), any())).thenReturn(call);
	//when
		methodExecutor.getPostconditions(callingState, scopedHeap, null);
	//then
		//ensure the dependency is registered
		verify(procedureRegistry).registerDependency(eq(callingState), any());
		//ensure a new contract is generated
		verify(methodExecutor).generateAndAddContract(any());
		
	}

}
