package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureRegistry;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;

public class NonRecursiveModelCheckingMethodExecutorTest {

	private NonRecursiveModelCheckingMethodExecutor methodExecutor;
	private ContractCollection contractCollection;
	
	
	@Before
	public void init() {
		Method method = mock(Method.class);
		ScopeExtractor scopeExtractor = mock(ScopeExtractor.class);
		contractCollection = mock(ContractCollection.class);
		OnTheFlyProcedureRegistry procedureRegistry = mock(OnTheFlyProcedureRegistry.class);
		methodExecutor = new NonRecursiveModelCheckingMethodExecutor(method, scopeExtractor, 
				contractCollection, procedureRegistry);	
	}

	@Test
	public void testGenerateAndAddContract_ensureExecute() {
		//given
		OnTheFlyProcedureCall call = mock(OnTheFlyProcedureCall.class);
		
		//when
		methodExecutor.generateAndAddContract(call);
		
		//then
		verify(call).execute();
	}
}
