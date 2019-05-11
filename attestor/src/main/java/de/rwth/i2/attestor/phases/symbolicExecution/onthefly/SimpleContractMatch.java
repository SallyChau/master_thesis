package de.rwth.i2.attestor.phases.symbolicExecution.onthefly;

import java.util.Collection;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingContract;
import de.rwth.i2.attestor.procedures.ContractMatch;

public class SimpleContractMatch implements ContractMatch {
	
	boolean matched = true;
	HeapConfiguration precondition;
	Collection<HeapConfiguration> postconditions;
	int[] externalReordering;
	
	public SimpleContractMatch(HeapConfiguration precondition, Collection<HeapConfiguration> postconditions) {
		
		this.precondition = precondition;
		this.postconditions = postconditions;
		setDefaultExternalReordering();
	}
	
	public SimpleContractMatch(HeapConfiguration precondition, Collection<HeapConfiguration> postconditions, int[] externalReordering) {
		
		this.precondition = precondition;
		this.postconditions = postconditions;
		if (externalReordering != null) {
			this.externalReordering = externalReordering;
		} else {
			setDefaultExternalReordering();
		}
	}
	
    @Override
    public boolean hasMatch() {
        return matched;
    }

    @Override
    public int[] getExternalReordering() {
        return externalReordering;
    }
    
    private void setDefaultExternalReordering() {
    	externalReordering = new int[precondition.countExternalNodes()];
        for (int i = 0; i < externalReordering.length; i++) {
        	externalReordering[i] = i;
        }
	}

    @Override
    public Collection<HeapConfiguration> getPostconditions() {
        if(matched) {
            return postconditions;
        } else {
            throw new IllegalStateException();
        }
    }

	@Override
	public HeapConfiguration getPrecondition() {
		if( matched ) {
			return precondition;
		}else {
			throw new IllegalStateException();
		}
	}

	@Override
	public ModelCheckingContract getModelCheckingContract(Set<Node> inputFormulae) {

		return null;
	}

	@Override
	public boolean hasModelCheckingContractMatch(Set<Node> inputFormulae) {

		return false;
	}
}
