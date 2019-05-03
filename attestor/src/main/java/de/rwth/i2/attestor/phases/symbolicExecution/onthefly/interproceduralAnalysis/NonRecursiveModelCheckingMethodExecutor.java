package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureRegistry;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;

public class NonRecursiveModelCheckingMethodExecutor extends AbstractModelCheckingMethodExecutor {

	public NonRecursiveModelCheckingMethodExecutor(Method method, ScopeExtractor scopeExtractor, ContractCollection contractCollection,
			OnTheFlyProcedureRegistry procedureRegistry) {

		super(method, scopeExtractor, contractCollection, procedureRegistry);
	}
	
	
	
	/**
	 * generates the Contract and model checks formulae by executing the call
	 */
	@Override
	protected void generateAndAddContract(OnTheFlyProcedureCall call) {
		
		System.out.println("NonRecursiveModelCheckingMethodExecutor: Executing procedure call " + call.getMethod().getSignature());
		call.execute();
	}
}
