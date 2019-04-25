package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureRegistry;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.ModelCheckingContract;
import de.rwth.i2.attestor.procedures.AbstractMethodExecutor;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.ContractMatch;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;
import de.rwth.i2.attestor.procedures.ScopedHeap;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public abstract class AbstractModelCheckingMethodExecutor extends AbstractMethodExecutor {
	
	protected final Method method;
	protected OnTheFlyProcedureRegistry procedureRegistry;

	protected Set<Node> modelCheckingFormulae;
	
	

	public AbstractModelCheckingMethodExecutor(Method method, 
			  								   ScopeExtractor scopeExtractor, 
											   ContractCollection contractCollection, 
											   OnTheFlyProcedureRegistry procedureRegistry) {
		super(scopeExtractor, contractCollection);
		this.method = method;
		this.procedureRegistry = procedureRegistry;
		this.modelCheckingFormulae = new HashSet<>();
	}
	
	

	@Override
	protected Collection<HeapConfiguration> getPostconditions(ProgramState callingState, ScopedHeap scopedHeap) {
		
		HeapConfiguration heapInScope = scopedHeap.getHeapInScope();		
		
	    ContractMatch contractMatch = getContractCollection().matchContract(heapInScope);
	    if( contractMatch.hasMatch() ) {
		    System.out.println("AbstractModelCheckingMethodExecutor: getPostconditions(): found contractMatch for method " + method.getSignature());
	    	heapInScope = contractMatch.getPrecondition();
	    }
	    
	    OnTheFlyProcedureCall call = (OnTheFlyProcedureCall) procedureRegistry.getProcedureCall( method, heapInScope, scopedHeap );
	    procedureRegistry.registerDependency( callingState, call );
	    System.out.println("AbstractModelCheckingMethodExecutor: getPostconditions(): Created procedure call " + call + " for method " + method.getSignature());
	    
	    if(!contractMatch.hasMatch()) {
	    	
		    System.out.println("AbstractModelCheckingMethodExecutor: getPostconditions(): no contract match for method " + method.getSignature());
	        
	        ContractCollection contractCollection = getContractCollection();
	        generateAndAddContract(call);
	        contractMatch = contractCollection.matchContract(heapInScope);
	    }
	    
	    return scopedHeap.merge(contractMatch);
	}

	@Override
	protected ModelCheckingContract getModelCheckingContract(ProgramState callingState, ScopedHeap scopedHeap, Set<Node> formulae) {
		
		System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: Checking MC contracts for " + method.getSignature());
		
		HeapConfiguration heapInScope = scopedHeap.getHeapInScope();
	    ContractMatch contractMatch = getContractCollection().matchContract(heapInScope);
	    if( contractMatch.hasMatch() ) {
	    	heapInScope = contractMatch.getPrecondition();
	    	System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: found matching contract for " + method.getSignature());
	    }
	    
	    OnTheFlyProcedureCall call = (OnTheFlyProcedureCall) procedureRegistry.getProcedureCall( method, heapInScope, scopedHeap );
	    procedureRegistry.registerDependency( callingState, call );
	    System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: Created procedure call " + call);
	    
	    if(!contractMatch.hasMatch() || !contractMatch.hasModelCheckingContractMatch(modelCheckingFormulae)) {
	    	System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: no matching contract for " + method.getSignature());
	    	System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: Generating new contracts");
	        ContractCollection contractCollection = getContractCollection();
	        generateAndAddContract(call);
	        contractMatch = contractCollection.matchContract(heapInScope);
	    } 
	    System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: result formulae: " + contractMatch.getModelCheckingContract(modelCheckingFormulae));
    	return contractMatch.getModelCheckingContract(modelCheckingFormulae);
	}
	
	public void setModelCheckingFormulae(Set<Node> formulae) {
		
		modelCheckingFormulae = formulae;
	}
	
	/**
	 * 
	 * @param call
	 */
	protected abstract void generateAndAddContract(OnTheFlyProcedureCall call);
}
