package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingContract;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureRegistry;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;
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
	
	

	public AbstractModelCheckingMethodExecutor(Method method, ScopeExtractor scopeExtractor, ContractCollection contractCollection, 
			OnTheFlyProcedureRegistry procedureRegistry) {
		super(scopeExtractor, contractCollection);
		this.method = method;
		this.procedureRegistry = procedureRegistry;
		this.modelCheckingFormulae = new HashSet<>();
	}

	
	
	@Override
	public Collection<ProgramState> getResultStates(ProgramState callingState, ProgramState input, ScopedHeapHierarchy scopeHierarchy) {
	
	    HeapConfiguration inputHeap = input.getHeap();
	    ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
	    
	    // add new scoped heap to the scope hierarchy
	    if (scopeHierarchy != null) {
	    	scopeHierarchy.addScopedHeap(scopedHeap);
	    }
	    
	    Collection<HeapConfiguration> postconditions = getPostconditions(callingState, scopedHeap, scopeHierarchy);
	    return createResultStates(input, postconditions);
	}

	@Override	
	protected Collection<HeapConfiguration> getPostconditions(ProgramState callingState, ScopedHeap scopedHeap, ScopedHeapHierarchy scopeHierarchy) {
		
		HeapConfiguration heapInScope = scopedHeap.getHeapInScope();		
		
	    ContractMatch contractMatch = getContractCollection().matchContract(heapInScope);
	    if( contractMatch.hasMatch() ) {
		    System.out.println("AbstractModelCheckingMethodExecutor: getPostconditions(): found contractMatch for method " + method.getSignature());
	    	heapInScope = contractMatch.getPrecondition();
	    }
	    
	    OnTheFlyProcedureCall call = (OnTheFlyProcedureCall) procedureRegistry.getProcedureCall( method, heapInScope, scopeHierarchy);
	    call.setModelCheckingFormulae(modelCheckingFormulae);
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
	
	public void setModelCheckingFormulae(Set<Node> formulae) {
		
		modelCheckingFormulae = formulae;
	}

	public Set<Node> getModelCheckingResultFormulae(ProgramState callingState, ProgramState input, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy) {

        HeapConfiguration inputHeap = input.getHeap();
        ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
        
        // add new scoped heap to the scope hierarchy
        if (scopeHierarchy != null) {
        	scopeHierarchy.addScopedHeap(scopedHeap);
        }
    	
        ModelCheckingContract modelCheckingContract = getModelCheckingContract(callingState, scopedHeap, formulae, scopeHierarchy);
        if (modelCheckingContract != null) {
        	return modelCheckingContract.getResultFormulae();
        }
        
        return Collections.emptySet();
    }
    
	public boolean satisfiesFormulae(ProgramState callingState, ProgramState input, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy) {

        HeapConfiguration inputHeap = input.getHeap();
        ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
        
        // add new scoped heap to the scope hierarchy
        if (scopeHierarchy != null) {
        	scopeHierarchy.addScopedHeap(scopedHeap);
        }
    	
        ModelCheckingContract modelCheckingContract = getModelCheckingContract(callingState, scopedHeap, formulae, scopeHierarchy);
        if (modelCheckingContract != null) {
        	return modelCheckingContract.modelCheckingIsSuccessful();
        }
        
        return true;
    }

	private ModelCheckingContract getModelCheckingContract(ProgramState callingState, ScopedHeap scopedHeap, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy) {
		
		System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: Checking MC contracts for " + method.getSignature());
		
		HeapConfiguration heapInScope = scopedHeap.getHeapInScope(); 
	    ContractMatch contractMatch = getContractCollection().matchContract(heapInScope);
	    
	    if(contractMatch.hasMatch()) {
	    	
	    	System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: found matching contract for " + method.getSignature());
	    	heapInScope = contractMatch.getPrecondition();
	    	scopeHierarchy.addExternalReordering(scopedHeap, contractMatch.getExternalReordering());
	    }
	    
	    OnTheFlyProcedureCall call = (OnTheFlyProcedureCall) procedureRegistry.getProcedureCall(method, heapInScope, scopeHierarchy);	    
	    call.setModelCheckingFormulae(formulae);
	    procedureRegistry.registerDependency(callingState, call);
	    
	    if(!contractMatch.hasModelCheckingContractMatch(formulae)) {
	    	System.out.println("AbstractModelCheckingMethodExecutor: getModelCheckingContract: no matching model checking contract for " + method.getSignature());
	        ContractCollection contractCollection = getContractCollection();
	        generateAndAddContract(call);
	        contractMatch = contractCollection.matchContract(heapInScope);
	    } 
	    
	    return contractMatch.getModelCheckingContract(formulae);
	}
	
	/**
	 * Generate contracts for call and add to contractCollection.
	 * @param call
	 */
	protected abstract void generateAndAddContract(OnTheFlyProcedureCall call);
}
