package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureRegistry;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.HeapConfigurationDummy;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public class RecursiveModelCheckingMethodExecutorTest {

	private static Scene scene;

	private RecursiveModelCheckingMethodExecutor methodExecutor;
	private ContractCollection contractCollection;

	private OnTheFlyProcedureRegistry procedureRegistry;
	
	@BeforeClass
	public static void setUp() {
		scene = new MockupSceneObject().scene();
	}
	
	@Before
	public void init() {
		Method method = mock(Method.class);
		ScopeExtractor scopeExtractor = mock(ScopeExtractor.class);
		contractCollection = mock(ContractCollection.class);
		procedureRegistry = mock(OnTheFlyProcedureRegistry.class);
		methodExecutor = new RecursiveModelCheckingMethodExecutor(method, scopeExtractor, 
				contractCollection, procedureRegistry);	
	}

	@Test
	public void testGenerateAndAddContract_ensureExecute() {
		//given
		HeapConfigurationDummy precondition = new HeapConfigurationDummy("precondition");
		
		ProgramState inputState = scene.createProgramState(precondition);
		
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);
		when(call.getInput()).thenReturn(inputState);
		
		Set<Node> formulae = new HashSet<>();
        try {
            formulae.add(new LTLFormula("({dll} & {tree})").getASTRoot().getPLtlform());
        } catch (Exception e) {
            fail("Formula should parse correctly. No Parser and Lexer exception expected!");
        } 
        
        methodExecutor.setModelCheckingFormulae(formulae);
		
		//when
		methodExecutor.generateAndAddContract(call);
		
		//then
		//ensure empty contract with correct preconditon is added
		ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
		verify(contractCollection).addContract(contractCaptor.capture());
		Contract res = contractCaptor.getValue();
		assertEquals(precondition, res.getPrecondition());
		assertThat(res.getPostconditions(), empty());
		assertThat(res.getModelCheckingContracts(), empty());
		
		//ensure call is registered for later analysis
		verify(procedureRegistry).registerProcedure(call);
		verify(procedureRegistry).registerFormulae(call, formulae);
	}
}
