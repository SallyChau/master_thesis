package de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis;

import java.util.Collection;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ModelCheckingContract;
import de.rwth.i2.attestor.procedures.AbstractMethodExecutor;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.ContractMatch;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;
import de.rwth.i2.attestor.procedures.ScopedHeap;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public abstract class AbstractInterproceduralMethodExecutor extends AbstractMethodExecutor {

	protected final Method method;
	protected ProcedureRegistry procedureRegistry;

	public AbstractInterproceduralMethodExecutor( Method method, 
												  ScopeExtractor scopeExtractor, 
												  ContractCollection contractCollection, 
												  ProcedureRegistry procedureRegistry ) {
		super(scopeExtractor, contractCollection);
		this.method = method;
		this.procedureRegistry = procedureRegistry;
	}

	// template method. can be configured by overriding generateAndAddContract.
	@Override
	protected final Collection<HeapConfiguration> getPostconditions(ProgramState callingState, ScopedHeap scopedHeap) {
	
	    HeapConfiguration heapInScope = scopedHeap.getHeapInScope();
	    ContractMatch contractMatch = getContractCollection().matchContract(heapInScope);
	    if( contractMatch.hasMatch() ) {
	    	heapInScope = contractMatch.getPrecondition();
	    }	    
	    
	    ProcedureCall call = procedureRegistry.getProcedureCall( method, heapInScope );
	    procedureRegistry.registerDependency( callingState, call );
	    System.out.println("Postconditions: Created procedure call " + call);
	    System.out.println("Heap: " + heapInScope);
	    
	    if(!contractMatch.hasMatch()) {
	        
	        ContractCollection contractCollection = getContractCollection();
	        generateAndAddContract( call);
	        contractMatch = contractCollection.matchContract(heapInScope);
	    }
	    
	    return scopedHeap.merge(contractMatch);
	}
	
	@Override
	public void setModelCheckingFormulae(Set<Node> formulae) {
		// NOP		
	}
	@Override
	protected ModelCheckingContract getModelCheckingContract(ProgramState callingState, ScopedHeap scopedHeap,
			Set<Node> formulae) {
		
		return null;
	}
	
	/**
	 * Is called when no contract for this call is found. 
     * @param call the procedureCall for this method and input
	 */
	protected abstract void generateAndAddContract(ProcedureCall call);

}