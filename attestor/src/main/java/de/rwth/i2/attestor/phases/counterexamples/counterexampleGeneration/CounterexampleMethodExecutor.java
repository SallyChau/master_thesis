package de.rwth.i2.attestor.phases.counterexamples.counterexampleGeneration;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.canonicalization.CanonicalizationStrategy;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.ModelCheckingContract;
import de.rwth.i2.attestor.procedures.AbstractMethodExecutor;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.ContractMatch;
import de.rwth.i2.attestor.procedures.ScopeExtractor;
import de.rwth.i2.attestor.procedures.ScopedHeap;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public class CounterexampleMethodExecutor extends AbstractMethodExecutor {

    private final CounterexampleContractGenerator counterexampleContractGenerator;
    private final CanonicalizationStrategy canonicalizationStrategy;

    CounterexampleMethodExecutor(ScopeExtractor scopeExtractor, ContractCollection contractCollection,
                                        CounterexampleContractGenerator contractGenerator,
                                        CanonicalizationStrategy canonicalizationStrategy) {

        super(scopeExtractor, contractCollection);
        this.counterexampleContractGenerator = contractGenerator;
        this.canonicalizationStrategy = canonicalizationStrategy;
    }

    @Override
    protected Collection<HeapConfiguration> getPostconditions(ProgramState callingState,
                                                              ScopedHeap scopedHeap) {

        HeapConfiguration abstractedHeapInScope = canonicalizationStrategy.canonicalize(scopedHeap.getHeapInScope());
        ContractMatch abstractMatch = getContractCollection().matchContract(abstractedHeapInScope);
        if(!abstractMatch.hasMatch()) {
            throw new IllegalStateException("Could not match contract during counterexample generation.");
        }
        counterexampleContractGenerator.setRequiredFinalHeaps(abstractMatch.getPostconditions());

        ContractMatch contractMatch = computeNewContract(callingState, scopedHeap);
        if(contractMatch.hasMatch()) {
            return scopedHeap.merge(contractMatch);
        }
        return Collections.emptySet();
    }

    private ContractMatch computeNewContract(ProgramState someState, ScopedHeap scopedHeap) {

        HeapConfiguration heapInScope = scopedHeap.getHeapInScope();
        ProgramState initialState = someState.shallowCopyWithUpdateHeap(heapInScope);
        Contract generatedContract = counterexampleContractGenerator.generateContract(initialState);
        ContractCollection contractCollection = getContractCollection();
        contractCollection.addContract(generatedContract);
        return contractCollection.matchContract(heapInScope);
    }

	@Override
	protected ModelCheckingContract getModelCheckingContract(ProgramState callingState, ScopedHeap scopedHeap,
			Set<Node> formulae) {
		// TODO Auto-generated method stub
		return null;
	}
}
