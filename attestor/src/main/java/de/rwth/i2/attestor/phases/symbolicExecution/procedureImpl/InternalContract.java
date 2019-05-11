package de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl;

import java.util.ArrayList;
import java.util.Collection;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingContract;
import de.rwth.i2.attestor.procedures.Contract;

public class InternalContract implements Contract {

    private final HeapConfiguration precondition;
    private final Collection<HeapConfiguration> postconditions;
    private final Collection<ModelCheckingContract> modelCheckingContracts;

    public InternalContract(HeapConfiguration precondition, Collection<HeapConfiguration> postconditions) {

        this.precondition = precondition;
        this.postconditions = postconditions;
        this.modelCheckingContracts = new ArrayList<>();
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
	public void addModelCheckingContracts(Collection<ModelCheckingContract> contracts) {
		
		this.modelCheckingContracts.addAll(contracts);
	}

	@Override
	public Collection<ModelCheckingContract> getModelCheckingContracts() {

		return modelCheckingContracts;
	}
}
