package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ModelCheckingContract;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ModelCheckingContractCollection;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureRegistry;
import de.rwth.i2.attestor.procedures.AbstractMethodExecutor;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.ContractMatch;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;
import de.rwth.i2.attestor.procedures.ScopedHeap;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public abstract class AbstractModelCheckingMethodExecutor extends AbstractMethodExecutor {
	
	protected final Method method;
	protected ProcedureRegistry procedureRegistry;

	ModelCheckingContractCollection mcContractCollection;
	protected Set<Node> inputFormulae;

	public AbstractModelCheckingMethodExecutor(Method method, 
			  								   ScopeExtractor scopeExtractor, 
											   ContractCollection contractCollection, 
											   ModelCheckingContractCollection mcContractCollection, 
											   ProcedureRegistry procedureRegistry) {
		super(scopeExtractor, contractCollection);
		this.method = method;
		this.mcContractCollection = mcContractCollection;
		this.procedureRegistry = procedureRegistry;
		this.inputFormulae = new HashSet<>();
	}

	@Override
	protected Collection<HeapConfiguration> getPostconditions(ProgramState callingState, ScopedHeap scopedHeap) {
		
		HeapConfiguration heapInScope = scopedHeap.getHeapInScope();
	    ContractMatch contractMatch = getContractCollection().matchContract(heapInScope);
	    if( contractMatch.hasMatch() ) {
		    System.out.println("AbstractModelCheckingMethodExecutor: getPostconditions(): found contractMatch for method " + method.getSignature());
	    	heapInScope = contractMatch.getPrecondition();
	    }
	    
	    ProcedureCall call = procedureRegistry.getProcedureCall( method, heapInScope );
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
	    
	    ProcedureCall call = procedureRegistry.getProcedureCall( method, heapInScope );
	    procedureRegistry.registerDependency( callingState, call );
	    System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: Created procedure call " + call);
	    
	    if(!contractMatch.hasMatch() || !contractMatch.hasModelCheckingContractMatch(inputFormulae)) {
	    	System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: no matching contract for " + method.getSignature());
	    	System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: Generating new contracts");
	        ContractCollection contractCollection = getContractCollection();
	        generateAndAddContract(call);
	        contractMatch = contractCollection.matchContract(heapInScope);
	    } 
	    System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: result formulae: " + contractMatch.getModelCheckingContract(inputFormulae));
    	return contractMatch.getModelCheckingContract(inputFormulae);
	}
	
	@Override
	public void setModelCheckingFormulae(Set<Node> formulae) {
		
		inputFormulae = formulae;
	}
	
	/**
	 * 
	 * @param call
	 */
	protected abstract void generateAndAddContract(ProcedureCall call);
}
