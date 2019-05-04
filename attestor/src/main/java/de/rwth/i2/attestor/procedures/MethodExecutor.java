package de.rwth.i2.attestor.procedures;

import java.util.Collection;

import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;

public interface MethodExecutor {

    void addContract(Contract contract);

    Collection<ProgramState> getResultStates(ProgramState callingState, ProgramState input, ScopedHeapHierarchy scopedHierarchy);

	Collection<Contract> getContractsForExport();
}
