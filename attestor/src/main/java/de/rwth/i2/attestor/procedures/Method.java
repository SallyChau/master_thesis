package de.rwth.i2.attestor.procedures;

import java.util.Collection;

import de.rwth.i2.attestor.stateSpaceGeneration.Program;

public interface Method {

    String getSignature();

    String getName();
    void setName(String name);

    void setBody(Program body);
    Program getBody();

    boolean isRecursive();
    void setRecursive(boolean isRecursive);

    void addContract(Contract contract);
    Collection<Contract> getContractsForExport();

    void setMethodExecution(MethodExecutor methodExecution);
    MethodExecutor getMethodExecutor();

}
