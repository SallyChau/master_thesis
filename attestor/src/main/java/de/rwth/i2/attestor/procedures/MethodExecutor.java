package de.rwth.i2.attestor.procedures;

import java.util.Collection;
import java.util.List;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public interface MethodExecutor {

    void addContract(Contract contract);

    Collection<ProgramState> getResultStates(ProgramState callingState, ProgramState input);
    
    Collection<ProgramState> getResultStatesOnTheFly(ProgramState callingState, ProgramState input, List<Node> formulae);

	Collection<Contract> getContractsForExport();

	List<Node> getResultFormulaeOnTheFly(ProgramState programState, ProgramState preparedState, List<Node> formulae);
}
