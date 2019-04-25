package de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.ModelCheckingContract;
import de.rwth.i2.attestor.procedures.ContractMatch;

public class InternalContractMatch implements ContractMatch {

    private int[] externalReordering;
    private Collection<HeapConfiguration> postconditions;
	private HeapConfiguration matchedPrecondition;
	
	private Collection<ModelCheckingContract> modelCheckingContracts;

    public InternalContractMatch( int[] externalReordering,
    							  HeapConfiguration matchedPrecondition,
    							  Collection<HeapConfiguration> postconditions ) {

        this.externalReordering = externalReordering;
        this.matchedPrecondition = matchedPrecondition;
        
        this.postconditions = postconditions;
        this.modelCheckingContracts = new ArrayList<>();
    }
    
    public InternalContractMatch( int[] externalReordering,
			  					  HeapConfiguration matchedPrecondition,
		  					  	  Collection<HeapConfiguration> postconditions,
		  					  	  Collection<ModelCheckingContract> mcContracts) {

		this(externalReordering, matchedPrecondition, postconditions);		
		this.modelCheckingContracts = mcContracts;
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
	public ModelCheckingContract getModelCheckingContract(Set<Node> inputFormulae) {

		for (ModelCheckingContract contract : modelCheckingContracts) {
			if (contract.getInputFormulae().equals(inputFormulae)) {
				return contract;
			}
		}
		
		return null;
	}

	@Override
	public boolean hasModelCheckingContractMatch(Set<Node> inputFormulae) {

		for (ModelCheckingContract contract : modelCheckingContracts) {
			if (contract.getInputFormulae().equals(inputFormulae)) {
				return true;
			}
		}
		
		return false;
	}
}
