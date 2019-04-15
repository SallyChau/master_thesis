package de.rwth.i2.attestor.phases.counterexamples.counterexampleGeneration;

import static fj.data.Validation.fail;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ModelCheckingContract;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.ContractCollection;
import de.rwth.i2.attestor.procedures.ContractMatch;

public class CounterexampleContractCollectionTest {

    @Test
    public void testSimple() {

        ContractCollection mockup = new ContractCollection() {

            @Override
            public void addContract(Contract contract) {
                fail("Should not be called");
            }

            @Override
            public ContractMatch matchContract(HeapConfiguration precondition) {
                return new ContractMatch() {
                    @Override
                    public boolean hasMatch() {
                        return true;
                    }

                    @Override
                    public int[] getExternalReordering() {
                        int[] result = new int[1];
                        result[0] = 23; // marks that this has been called
                        return result;
                    }

                    @Override
                    public Collection<HeapConfiguration> getPostconditions() {
                        return null;
                    }

					@Override
					public HeapConfiguration getPrecondition() {
						return null;
					}

					@Override
					public ModelCheckingContract getModelCheckingContract(List<Node> inputFormulae) {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public boolean hasModelCheckingContractMatch(List<Node> inputFormulae) {
						// TODO Auto-generated method stub
						return false;
					}
                };
            }

			@Override
			public Collection<Contract> getContractsForExport() {
				fail("Should not be called");
				return null;
			}
        };

        CounterexampleContractCollection collection = new CounterexampleContractCollection(mockup);
        collection.addContract(new Contract() {
            @Override
            public void addPostconditions(Collection<HeapConfiguration> postconditions) {

            }

            @Override
            public HeapConfiguration getPrecondition() {
                return null;
            }

            @Override
            public Collection<HeapConfiguration> getPostconditions() {
                return null;
            }

			@Override
			public void addModelCheckingContract(ModelCheckingContract contract) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public List<ModelCheckingContract> getModelCheckingContracts() {
				// TODO Auto-generated method stub
				return null;
			}
        });
        // should not fail

        ContractMatch match = collection.matchContract(null);
        assertEquals(23, match.getExternalReordering()[0]); // checks delegation to mockup
    }
}
