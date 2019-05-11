package de.rwth.i2.attestor.procedures;

import java.util.Collection;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingContract;

public interface Contract {

    void addPostconditions(Collection<HeapConfiguration> postconditions);

    HeapConfiguration getPrecondition();
    Collection<HeapConfiguration> getPostconditions();
    
    void addModelCheckingContracts(Collection<ModelCheckingContract> contract);
    Collection<ModelCheckingContract> getModelCheckingContracts();
}
