package de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis;

import java.util.Collection;
import java.util.List;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
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
	    
	    if(!contractMatch.hasMatch()) {
	        
	        ContractCollection contractCollection = getContractCollection();
	        generateAndAddContract( call);
	        contractMatch = contractCollection.matchContract(heapInScope);
	    }
	    
	    return scopedHeap.merge(contractMatch);
	}
	
	@Override
	protected final Collection<HeapConfiguration> getPostconditions(ProgramState callingState, ScopedHeap scopedHeap, List<Node> formulae) {
		
	    HeapConfiguration heapInScope = scopedHeap.getHeapInScope();
	    ContractMatch contractMatch = getContractCollection().matchContract(heapInScope);
	    if( contractMatch.hasMatch() ) {
	    	heapInScope = contractMatch.getPrecondition();
	    }
	    
	    ProcedureCall call = procedureRegistry.getProcedureCall( method, heapInScope );
	    procedureRegistry.registerDependency( callingState, call );
	    
	    if(!contractMatch.hasMatch()) {
	        
	        ContractCollection contractCollection = getContractCollection();
	        generateAndAddContract(call, formulae);
	        contractMatch = contractCollection.matchContract(heapInScope);
	    }
	    
	    return scopedHeap.merge(contractMatch);
	}
	
	// TODO refactor so that contracts dont need to be matched twice
	@Override
	protected List<Node> getOutputFormulae(ProgramState callingState, ScopedHeap scopedHeap, List<Node> inputFormulae){
		HeapConfiguration heapInScope = scopedHeap.getHeapInScope();
	    ContractMatch contractMatch = getContractCollection().matchContract(heapInScope);
	    if( contractMatch.hasMatch() ) {
	    	heapInScope = contractMatch.getPrecondition();
	    }
	    
	    ProcedureCall call = procedureRegistry.getProcedureCall( method, heapInScope );
	    procedureRegistry.registerDependency( callingState, call );
	    
	    if(!contractMatch.hasMatch() && !contractMatch.hasInputFormulaeMatch(inputFormulae)) {
	        
	        ContractCollection contractCollection = getContractCollection();
	        generateAndAddContract(call, inputFormulae);
	        contractMatch = contractCollection.matchContract(heapInScope);
	    }    
	    
    	return contractMatch.getOutputFormulae(inputFormulae);
	}
	
	/**
	 * Is called when no contract for this call is found. 
     * @param call the procedureCall for this method and input
	 */
	abstract protected void generateAndAddContract(ProcedureCall call);
	
	/**
	 * For on-the-fly
	 */
	abstract protected void generateAndAddContract(ProcedureCall call, List<Node> formulae);

}