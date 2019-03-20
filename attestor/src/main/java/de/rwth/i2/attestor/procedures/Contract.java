package de.rwth.i2.attestor.procedures;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;

public interface Contract {

    void addPostconditions(Collection<HeapConfiguration> postconditions);

    HeapConfiguration getPrecondition();
    Collection<HeapConfiguration> getPostconditions();
    
	List<Node> getOutputFormulae(List<Node> inputFormulae);
	void addFormulaPair(List<Node> inputFormulae, List<Node> outputFormulae);
	Map<List<Node>, List<Node>> getFormulaeMap();
}
