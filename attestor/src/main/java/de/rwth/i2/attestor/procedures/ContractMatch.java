package de.rwth.i2.attestor.procedures;

import java.util.Collection;
import java.util.List;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;

public interface ContractMatch {

    ContractMatch NO_CONTRACT_MATCH = NoContractMatch.NO_CONTRACT_MATCH;

    boolean hasMatch();
    int[] getExternalReordering();
    HeapConfiguration getPrecondition();
    Collection<HeapConfiguration> getPostconditions();

	List<Node> getOutputFormulae(List<Node> inputFormulae);
	boolean hasInputFormulaeMatch(List<Node> inputFormulae);
}
