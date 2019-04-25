package de.rwth.i2.attestor.procedures;

import java.util.Collection;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.ModelCheckingContract;

public interface ContractMatch {

    ContractMatch NO_CONTRACT_MATCH = NoContractMatch.NO_CONTRACT_MATCH;

    boolean hasMatch();
    int[] getExternalReordering();
    HeapConfiguration getPrecondition();
    Collection<HeapConfiguration> getPostconditions();
    
    ModelCheckingContract getModelCheckingContract(Set<Node> inputFormulae);
    boolean hasModelCheckingContractMatch(Set<Node> inputFormulae);
}
