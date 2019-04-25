package de.rwth.i2.attestor.phases.counterexamples.counterexampleGeneration;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.ModelCheckingContract;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.ContractMatch;

public class MockupContractCollection implements ContractCollection {

    private HeapConfiguration precondition;
    private Collection<HeapConfiguration> postconditions = new ArrayList<>();

    public  MockupContractCollection(MockupHeaps mockupHeaps) {

        this.precondition = mockupHeaps.getHeapInScope();
        this.postconditions.add(mockupHeaps.getPostcondition());
    }

    @Override
    public void addContract(Contract contract) {
    }

    @Override
    public ContractMatch matchContract(HeapConfiguration precondition) {

        boolean matched = this.precondition.equals(precondition);

            return new ContractMatch() {
                @Override
                public boolean hasMatch() {
                    return matched;
                }

                @Override
                public int[] getExternalReordering() {
                    if(matched) {
                        int[] result = new int[precondition.countExternalNodes()];
                        for (int i = 0; i < result.length; i++) {
                            result[i] = i;
                        }
                        return result;
                    } else {
                        throw new IllegalStateException();
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
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public boolean hasModelCheckingContractMatch(Set<Node> inputFormulae) {
					// TODO Auto-generated method stub
					return false;
				}
            };
    }

	@Override
	public Collection<Contract> getContractsForExport() {
		fail("should not be called");
		return Collections.emptyList();
	}
}
