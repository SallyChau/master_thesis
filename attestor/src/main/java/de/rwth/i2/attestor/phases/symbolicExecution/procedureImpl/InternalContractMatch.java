package de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.procedures.ContractMatch;

class InternalContractMatch implements ContractMatch {

    private int[] externalReordering;
    private Collection<HeapConfiguration> postconditions;
	private HeapConfiguration matchedPrecondition;
	
	private Map<List<Node>, List<Node>> inputToOutputFormulae;

    public InternalContractMatch( int[] externalReordering,
    							  HeapConfiguration matchedPrecondition,
    							  Collection<HeapConfiguration> postconditions ) {

        this.externalReordering = externalReordering;
        this.matchedPrecondition = matchedPrecondition;
        
        this.postconditions = postconditions;
    }
    
    public InternalContractMatch( int[] externalReordering,
			  HeapConfiguration matchedPrecondition,
			  Collection<HeapConfiguration> postconditions,
			  Map<List<Node>, List<Node>> inputToOutputFormulae) {

		this(externalReordering, matchedPrecondition, postconditions);
		
		this.inputToOutputFormulae = inputToOutputFormulae;
	}

    @Override
    public boolean hasMatch() {
        return true;
    }

    @Override
    public int[] getExternalReordering() {

        return externalReordering;
    }

    @Override
    public Collection<HeapConfiguration> getPostconditions() {

        return postconditions;
    }

	@Override
	public HeapConfiguration getPrecondition() {
		return this.matchedPrecondition;
	}
	
	@Override
	public boolean hasInputFormulaeMatch(List<Node> inputFormulae) {
		return this.inputToOutputFormulae.containsKey(inputFormulae);
	}
	
	@Override
	public List<Node> getOutputFormulae(List<Node> inputFormulae) {
		return this.inputToOutputFormulae.get(inputFormulae);
	}
}
