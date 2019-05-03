package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import java.util.Collection;
import java.util.LinkedHashSet;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureRegistry;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalContract;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;

public class RecursiveModelCheckingMethodExecutor extends AbstractModelCheckingMethodExecutor {

	public RecursiveModelCheckingMethodExecutor(Method method, 
												ScopeExtractor scopeExtractor, 
												ContractCollection contractCollection,
												OnTheFlyProcedureRegistry procedureRegistry) {

		super(method, scopeExtractor, contractCollection, procedureRegistry);
	}
	
	

	/**
     * Adds an empty contract and registers the call as for recursive Methods the contract is 
     * generated in a later phase in order to detect fixpoints.
     * Registers the formulae to be checked during model checking.
     */
	@Override
	protected void generateAndAddContract(OnTheFlyProcedureCall call) {
		
	    System.err.println("RecursiveModelCheckingMethodExecutor: register method " + call.getMethod().getSignature());
	
		Collection<HeapConfiguration> postconditions = new LinkedHashSet<>();
		getContractCollection().addContract(new InternalContract(call.getInput().getHeap(), postconditions));
		
		procedureRegistry.registerProcedure(call);	
		procedureRegistry.registerFormulae(call, modelCheckingFormulae);
	}
}
