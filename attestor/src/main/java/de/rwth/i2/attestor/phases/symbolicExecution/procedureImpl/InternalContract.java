package de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.procedures.Contract;

public class InternalContract implements Contract {

    private final HeapConfiguration precondition;
    private final Collection<HeapConfiguration> postconditions;
    
    private Map<List<Node>, List<Node>> inputToOutputFormulae;    

    public InternalContract(HeapConfiguration precondition, Collection<HeapConfiguration> postconditions) {

        this.precondition = precondition;
        this.postconditions = postconditions;
        
        this.inputToOutputFormulae = new LinkedHashMap<>();
    }

    public InternalContract(HeapConfiguration precondition) {

        this(precondition, new ArrayList<>());
    }

    @Override
    public void addPostconditions(Collection<HeapConfiguration> postconditions) {

        this.postconditions.addAll(postconditions);
    }

    @Override
    public HeapConfiguration getPrecondition() {

        return precondition;
    }

    @Override
    public Collection<HeapConfiguration> getPostconditions() {

        return postconditions;
    }
    
    @Override
	public List<Node> getOutputFormulae(List<Node> inputFormulae) {
    	
    	return inputToOutputFormulae.get(inputFormulae);
    }
    
    @Override
	public Map<List<Node>, List<Node>> getFormulaeMap() {

    	return this.inputToOutputFormulae;
    }
    
    @Override
	public void addFormulaPair(List<Node> inputFormulae, List<Node> outputFormulae) {

    	this.inputToOutputFormulae.put(inputFormulae, outputFormulae);
    }
}
