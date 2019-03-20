package de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.ContractMatch;
import de.rwth.i2.attestor.procedures.PreconditionMatchingStrategy;

public class InternalContractCollection implements ContractCollection {

    private final PreconditionMatchingStrategy preconditionMatchingStrategy;

    private final Map<Integer, Collection<Contract>> contracts;

    public InternalContractCollection(PreconditionMatchingStrategy preconditionMatchingStrategy) {

        this.preconditionMatchingStrategy = preconditionMatchingStrategy;
        this.contracts = new HashMap<>();
    }

    @Override
    public void addContract(Contract contract) {

        int preconditionHash = contract.getPrecondition().hashCode();
        if(!contracts.containsKey(preconditionHash)) {
            Collection<Contract> value = new ArrayList<>();
            value.add(contract);
            contracts.put(preconditionHash, value);
        } else {
            Collection<Contract> hashedContracts = contracts.get(preconditionHash);
            for (Contract c : hashedContracts) {
                ContractMatch match = preconditionMatchingStrategy.match(c, contract.getPrecondition());
                if (match.hasMatch()) {
                    c.addPostconditions(contract.getPostconditions());
                    return;
                }
            }
            hashedContracts.add(contract);
        }
    }

    @Override
    public ContractMatch matchContract(HeapConfiguration precondition) {

        int preconditionHash = precondition.hashCode();
        if(!contracts.containsKey(preconditionHash)) {
            contracts.put(preconditionHash, new ArrayList<>());
        }
        for(Contract contract : contracts.get(preconditionHash)) {
            ContractMatch match = preconditionMatchingStrategy.match(contract, precondition);
            if(match.hasMatch()) {
                return match;
            }
        }

        return ContractMatch.NO_CONTRACT_MATCH;
    }

	@Override
	public Collection<Contract> getContractsForExport() {
		Collection<Contract> contractsForExport = new ArrayList<>();
		for( Entry<Integer, Collection<Contract>> entry : contracts.entrySet() ){
			contractsForExport.addAll(entry.getValue());
		}
		return contractsForExport;
	}
}
