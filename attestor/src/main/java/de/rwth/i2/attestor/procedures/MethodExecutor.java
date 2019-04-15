package de.rwth.i2.attestor.procedures;

import java.util.Collection;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public interface MethodExecutor {

    void addContract(Contract contract);

    Collection<ProgramState> getResultStates(ProgramState callingState, ProgramState input);

	Collection<Contract> getContractsForExport();

	void setModelCheckingFormulae(Set<Node> formulae);
	Set<Node> getModelCheckingResultFormulae(ProgramState programState, ProgramState preparedState, Set<Node> inputFormulae);
	boolean satisfiesFormulae(ProgramState callingState, ProgramState input, Set<Node> inputFormulae);
}
