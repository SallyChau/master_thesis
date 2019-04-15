package de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis;

import java.util.List;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;

public class FakeInterproceduralMethodExecutor extends AbstractInterproceduralMethodExecutor {

	public FakeInterproceduralMethodExecutor(Method method, ScopeExtractor scopeExtractor,
			ContractCollection contractCollection, ProcedureRegistry procedureRegistry) {
		super(method, scopeExtractor, contractCollection, procedureRegistry);
	}

	@Override
	protected void generateAndAddContract(ProcedureCall call) {

	}

	@Override
	protected void generateAndAddContractOnTheFly(ProcedureCall call, List<Node> formulae) {
		// TODO Auto-generated method stub
		
	}

}
