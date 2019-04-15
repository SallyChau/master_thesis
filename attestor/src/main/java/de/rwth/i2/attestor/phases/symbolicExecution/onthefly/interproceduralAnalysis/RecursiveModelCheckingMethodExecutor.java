package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import java.util.Collection;
import java.util.LinkedHashSet;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ModelCheckingContractCollection;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalContract;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureRegistry;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;

public class RecursiveModelCheckingMethodExecutor extends AbstractModelCheckingMethodExecutor {

	
	public RecursiveModelCheckingMethodExecutor( Method method, 
			ScopeExtractor scopeExtractor, 
			ContractCollection contractCollection,
			ModelCheckingContractCollection mcContractCollection,
            ProcedureRegistry procedureRegistry ) {

		super(method, scopeExtractor, contractCollection, mcContractCollection, procedureRegistry);
	}

	@Override
	protected void generateAndAddContract(ProcedureCall call) {
		
	    System.err.println("RecursiveModelCheckingMethodExecutor: called for method " + call.getMethod().getSignature());
	
		Collection<HeapConfiguration> postconditions = new LinkedHashSet<>();
		getContractCollection().addContract(new InternalContract(call.getInput().getHeap(), postconditions));
		
		System.err.println("RecursiveModelCheckingMethodExecutor: Register procedure call " + call.getMethod().getSignature());
		
		procedureRegistry.registerProcedure(call);	
		procedureRegistry.registerFormulae(call, inputFormulae);
	}
}
