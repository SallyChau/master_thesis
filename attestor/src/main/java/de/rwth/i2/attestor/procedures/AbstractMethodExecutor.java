package de.rwth.i2.attestor.procedures;

import java.util.Collection;
import java.util.LinkedHashSet;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public abstract class AbstractMethodExecutor implements MethodExecutor {

    protected ScopeExtractor scopeExtractor;
    private ContractCollection contractCollection;
    
    

    public AbstractMethodExecutor(ScopeExtractor scopeExtractor, ContractCollection contractCollection) {

        this.scopeExtractor = scopeExtractor;
        this.contractCollection = contractCollection;
    }
    
    

    public ScopeExtractor getScopeExtractor() {
        return scopeExtractor;
    }

    public ContractCollection getContractCollection() {
        return contractCollection;
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
    
    protected Collection<ProgramState> createResultStates(ProgramState input, Collection<HeapConfiguration> postconditions) {

        Collection<ProgramState> result = new LinkedHashSet<>();
        
        // apply postconditions (resulting heap from method execution) to heap of input state
        for(HeapConfiguration outputHeap : postconditions) {
            ProgramState resultState = input.shallowCopyWithUpdateHeap(outputHeap);
            resultState.setProgramCounter(0);
            result.add(resultState);
        }
        return result;
    }

    protected abstract Collection<HeapConfiguration> getPostconditions(ProgramState callingState, ScopedHeap scopedHeap, ScopedHeapHierarchy scopedHierarchy);

    @Override
    public void addContract(Contract contract) {

        contractCollection.addContract(contract);
    }
    
    @Override
    public Collection<Contract> getContractsForExport() {
    	
    	return contractCollection.getContractsForExport();
    }
}
