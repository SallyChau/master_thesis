package de.rwth.i2.attestor.procedures;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ModelCheckingContract;
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
    public Collection<ProgramState> getResultStates(ProgramState callingState, ProgramState input) {

        HeapConfiguration inputHeap = input.getHeap();
        ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
        Collection<HeapConfiguration> postconditions = getPostconditions(callingState, scopedHeap);
        return createResultStates(input, postconditions);
    }
    
    @Override
	public Set<Node> getModelCheckingResultFormulae(ProgramState callingState, ProgramState input, Set<Node> formulae) {

        HeapConfiguration inputHeap = input.getHeap();
        ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
        ModelCheckingContract mcContract = getModelCheckingContract(callingState, scopedHeap, formulae);
        if (mcContract != null) {
        	return mcContract.getResultFormulae();
        }
        return Collections.emptySet();
    }
    
    @Override
	public boolean satisfiesFormulae(ProgramState callingState, ProgramState input, Set<Node> formulae) {

        HeapConfiguration inputHeap = input.getHeap();
        ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
        ModelCheckingContract mcContract = getModelCheckingContract(callingState, scopedHeap, formulae);
        if (mcContract != null) {
        	return mcContract.modelCheckingIsSuccessful();
        }
        return true;
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
	
	protected abstract ModelCheckingContract getModelCheckingContract(ProgramState callingState, ScopedHeap scopedHeap,
			Set<Node> formulae);

    @Override
    public void addContract(Contract contract) {

        contractCollection.addContract(contract);
    }
    
    @Override
    public Collection<Contract> getContractsForExport() {
    	return contractCollection.getContractsForExport();
    }
}
