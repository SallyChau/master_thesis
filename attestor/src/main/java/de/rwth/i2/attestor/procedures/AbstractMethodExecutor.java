package de.rwth.i2.attestor.procedures;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public abstract class AbstractMethodExecutor implements MethodExecutor {

    private ScopeExtractor scopeExtractor;
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
    public Collection<ProgramState> getResultStates(ProgramState callingState, ProgramState input) {

        HeapConfiguration inputHeap = input.getHeap();
        ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
        Collection<HeapConfiguration> postconditions = getPostconditions(callingState, scopedHeap);
        return createResultStates(input, postconditions);
    }
    
    @Override
	public Collection<ProgramState> getResultStates(ProgramState callingState, ProgramState input, List<Node> formulae) {

        HeapConfiguration inputHeap = input.getHeap();
        ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
        Collection<HeapConfiguration> postconditions = getPostconditions(callingState, scopedHeap, formulae);
        return createResultStates(input, postconditions);
    }
    
    @Override
	public List<Node> getResultFormulae(ProgramState callingState, ProgramState input, List<Node> formulae) {

        HeapConfiguration inputHeap = input.getHeap();
        ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
        return getOutputFormulae(callingState, scopedHeap, formulae);
    }
    
    protected Collection<ProgramState> createResultStates(ProgramState input,
                                                        Collection<HeapConfiguration> postconditions) {

        Collection<ProgramState> result = new LinkedHashSet<>();
        
        // apply postconditions (resulting heap from method execution) to heap of input state
        for(HeapConfiguration outputHeap : postconditions) {
            ProgramState resultState = input.shallowCopyWithUpdateHeap(outputHeap);
            resultState.setProgramCounter(0);
            result.add(resultState);
        }
        return result;
    }

    protected abstract Collection<HeapConfiguration> getPostconditions(ProgramState callingState,
                                                                       ScopedHeap scopedHeap);

    @Override
    public void addContract(Contract contract) {

        contractCollection.addContract(contract);
    }
    
    @Override
    public Collection<Contract> getContractsForExport() {
    	return contractCollection.getContractsForExport();
    }

	protected abstract Collection<HeapConfiguration> getPostconditions(ProgramState callingState, ScopedHeap scopedHeap,
			List<Node> formulae);
	
	protected abstract List<Node> getOutputFormulae(ProgramState callingState, ScopedHeap scopedHeap,
			List<Node> formulae);

}
