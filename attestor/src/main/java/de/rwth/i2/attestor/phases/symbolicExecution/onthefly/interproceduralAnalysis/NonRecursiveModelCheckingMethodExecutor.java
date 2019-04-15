package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ModelCheckingContractCollection;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureRegistry;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;

public class NonRecursiveModelCheckingMethodExecutor extends AbstractModelCheckingMethodExecutor {

	public NonRecursiveModelCheckingMethodExecutor( Method method, 
			ScopeExtractor scopeExtractor, 
			ContractCollection contractCollection,
			ModelCheckingContractCollection mcContractCollection,
            ProcedureRegistry procedureRegistry ) {

		super(method, scopeExtractor, contractCollection, mcContractCollection, procedureRegistry);
	}
	
	@Override
	protected void generateAndAddContract(ProcedureCall call) {
		
		System.out.println("NonRecursiveModelCheckingMethodExecutor: Executing procedure call " + call.getMethod().getSignature());
		call.executeOnTheFly(inputFormulae);
	} 

}
