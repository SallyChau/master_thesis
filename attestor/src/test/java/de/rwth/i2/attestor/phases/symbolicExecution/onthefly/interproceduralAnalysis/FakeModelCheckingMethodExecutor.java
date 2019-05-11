package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureRegistry;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;

public class FakeModelCheckingMethodExecutor extends AbstractModelCheckingMethodExecutor {
	
	public FakeModelCheckingMethodExecutor(Method method, ScopeExtractor scopeExtractor,
			ContractCollection contractCollection, OnTheFlyProcedureRegistry procedureRegistry) {
		super(method, scopeExtractor, contractCollection, procedureRegistry);
	}

	@Override
	protected void generateAndAddContract(OnTheFlyProcedureCall call) {

	}

}
