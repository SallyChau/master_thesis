package de.rwth.i2.attestor.phases.symbolicExecution.onthefly;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.PreconditionMatchingStrategy;

public class ModelCheckingContractCollection {
	
	private final PreconditionMatchingStrategy preconditionMatchingStrategy;
	private final Map<Integer, Collection<ModelCheckingContract>> contracts;
	
	public ModelCheckingContractCollection(PreconditionMatchingStrategy preconditionMatchingStrategy) {

        this.preconditionMatchingStrategy = preconditionMatchingStrategy;
        this.contracts = new HashMap<>();
    }

	public void addContract(ModelCheckingContract contract) {
		
	}

	public ModelCheckingContract matchContract(HeapConfiguration precondition) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<Contract> getContractsForExport() {
		// TODO Auto-generated method stub
		return null;
	}
}
