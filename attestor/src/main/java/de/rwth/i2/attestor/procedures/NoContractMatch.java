package de.rwth.i2.attestor.procedures;

import java.util.Collection;
import java.util.List;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;

public final class NoContractMatch implements ContractMatch {

    public static final NoContractMatch NO_CONTRACT_MATCH = new NoContractMatch();
    
	private NoContractMatch() {}

    @Override
    public boolean hasMatch() {
        return false;
    }

    @Override
    public int[] getExternalReordering() {
        return null;
    }

    @Override
    public Collection<HeapConfiguration> getPostconditions() {
        return null;
    }

	@Override
	public HeapConfiguration getPrecondition() {
		return null;
	}
	@Override
	public List<Node> getOutputFormulae(List<Node> inputFormulae) {
		return null;
	}
}
