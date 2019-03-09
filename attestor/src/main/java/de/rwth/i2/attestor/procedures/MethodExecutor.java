package de.rwth.i2.attestor.procedures;

import java.util.Collection;
import java.util.LinkedList;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ProofStructure2;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public interface MethodExecutor {

    void addContract(Contract contract);

    Collection<ProgramState> getResultStates(ProgramState callingState, ProgramState input);
    
    Collection<ProgramState> getResultStates(ProgramState callingState, ProgramState input, LinkedList<Node> formulae, ProofStructure2 proofStructure);

	Collection<Contract> getContractsForExport();
}
