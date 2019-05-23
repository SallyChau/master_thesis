package de.rwth.i2.attestor.refinement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.SimpleContractMatch;
import de.rwth.i2.attestor.procedures.ContractMatch;
import de.rwth.i2.attestor.procedures.ScopedHeap;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateLabelingStrategy;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;

public class AutomatonStateLabelingStrategy implements StateLabelingStrategy {

    private final HeapAutomaton heapAutomaton;

    private List<StatelessHeapAutomaton> statelessHeapAutomata;

    public AutomatonStateLabelingStrategy(HeapAutomaton heapAutomaton) {

        this.heapAutomaton = heapAutomaton;
    }

    public AutomatonStateLabelingStrategy(HeapAutomaton heapAutomaton,
                                          List<StatelessHeapAutomaton> statelessHeapAutomata) {

        this.heapAutomaton = heapAutomaton;
        this.statelessHeapAutomata = statelessHeapAutomata;
    }

    public static AutomatonStateLabelingStrategyBuilder builder() {

        return new AutomatonStateLabelingStrategyBuilder();
    }

    private HeapAutomatonState transition(HeapConfiguration heapConfiguration) {

        return heapAutomaton.transition(heapConfiguration, extractStatesOfNonterminals(heapConfiguration));
    }

    private List<HeapAutomatonState> extractStatesOfNonterminals(HeapConfiguration heapConfiguration) {

        List<HeapAutomatonState> result = new ArrayList<>(heapConfiguration.countNonterminalEdges());
        TIntIterator iter = heapConfiguration.nonterminalEdges().iterator();
        while (iter.hasNext()) {
            int edge = iter.next();
            // If this cast fails the whole configuration is broken and we cannot recover from this here
            RefinedNonterminal nt = (RefinedNonterminal) heapConfiguration.labelOf(edge);
            result.add(nt.getState());
        }

        return result;
    }

    @Override
    public void computeAtomicPropositions(ProgramState programState) {

        HeapConfiguration heapConf = programState.getHeap();
        if (heapAutomaton != null) {
            for (String ap : transition(heapConf).toAtomicPropositions()) {
                programState.addAP(ap);
            }
        }
        for (StatelessHeapAutomaton automaton : statelessHeapAutomata) {
            for (String ap : automaton.transition(heapConf)) {
                programState.addAP(ap);
            }
        }
    }
    
    /**
     * Computes a global heap configuration for a scoped heap configuration for prodecure states.
     * Then determines the atomic propositions assigned to the given program state.
     * These propositions are directly attached to the given program state.
     * @param programState The program state whose atomic propositions should be determined.
     * @param scopeHierarchy The global heap to be scoped to.
     */
    public void computeAtomicPropositionsFromGlobalHeap(ProgramState programState, ScopedHeapHierarchy scopeHierarchy) {
    	
    	HeapConfiguration heapConf = programState.getHeap().clone();
    	if (!scopeHierarchy.getScopedHeaps().isEmpty()) {    		
    		heapConf = scopeHeapToTopLevelStateSpace(programState, scopeHierarchy);
		}
	
        if (heapAutomaton != null) {
            for (String ap : transition(heapConf).toAtomicPropositions()) {
                programState.addAP(ap);
            }
        }
        for (StatelessHeapAutomaton automaton : statelessHeapAutomata) {
            for (String ap : automaton.transition(heapConf)) {
                programState.addAP(ap);            }
        }
    }
    
    /**
	 * Scopes programState to scope of top level state space.
	 * @param programState The state to be scoped.
	 * @param scopeHierarchy Contains scopedHeaps until top level scope is reached.
	 * @return HeapConfiguration of programState scoped to top level state space.
	 */
	private HeapConfiguration scopeHeapToTopLevelStateSpace(ProgramState programState, ScopedHeapHierarchy scopeHierarchy) {
		
		HeapConfiguration heapConf = programState.getHeap().clone();

		List<ScopedHeap> scopes = scopeHierarchy.getScopedHeaps();
			
		for (int i = 0; i < scopes.size(); i++) {
			if (!heapConf.externalNodes().isEmpty()) {
				ScopedHeap scopedHeap = scopes.get(i);
				
				// if variables of heapConf are contained in heapsOutsideScope, rename variables in heapOutsideScope
				renameExistingVariables(heapConf, scopedHeap.getHeapOutsideScope(), i);
	
		    	HeapConfiguration precondition = scopedHeap.getHeapInScope(); // initial heap
		    	
				List<HeapConfiguration> postconditions = new ArrayList<>();
				postconditions.add(cleanHeap(programState.shallowCopyWithUpdateHeap(heapConf)).getHeap());
			
				ContractMatch match = new SimpleContractMatch(precondition, postconditions, scopeHierarchy.getExternalReordering(scopedHeap));	
				
				// merge heap with heapOutsideScope to get configuration for AP computation
				Collection<HeapConfiguration> mergedHeaps = scopedHeap.merge(match);	
				heapConf = mergedHeaps.iterator().next();
			} else {
	
				return heapConf;
			}
		}
		
		return heapConf;
	}

	/**
     * Remove local variables from the heap of programState
     * @param programState The program state to clean.
     * @return cleaned program state.
     */
    private ProgramState cleanHeap(ProgramState programState) {

    	ProgramState cleanedState = programState.clone();
    	
    	cleanedState.removeIntermediate("@this:");
    	
		TIntArrayList list = cleanedState.getHeap().variableEdges();
		TIntIterator iter = list.iterator();
		while (iter.hasNext()) {
			String name = cleanedState.getHeap().nameOf(iter.next());
			if (name.contains("@parameter")) {
				cleanedState.removeIntermediate(name);
			}
		}		
		
		cleanedState.removeIntermediate("@return");
		
		return cleanedState;
    }
    
    /**
     * Adds an index to variables of heapToMutate that also occur in heap.
     * @param heap
     * @param heapToMutate
     * @param index
     */
    private void renameExistingVariables(HeapConfiguration heap, HeapConfiguration heapToMutate, int index) {
    	
    	TIntArrayList variableEdges = heap.variableEdges();
		for (int j = 0; j < variableEdges.size(); j++) {
			int varEdge = variableEdges.get(j);
			String variable = heap.nameOf(varEdge);

			if (heapToMutate.variableWith(variable) != HeapConfiguration.INVALID_ELEMENT) {
				// variable exists in outer scope
				TIntArrayList heapOutsideScopeVariableEdges = heapToMutate.variableEdges();
				TIntIterator varIter = heapOutsideScopeVariableEdges.iterator();
				while (varIter.hasNext()) {
					int heapOutsideScopeVarEdge = varIter.next();
					if (heapToMutate.nameOf(heapOutsideScopeVarEdge).equals(variable)) {
						int target = heapToMutate.variableTargetOf(variable);
						heapToMutate.builder()
							.removeVariableEdge(heapOutsideScopeVarEdge)
							.addVariableEdge(variable + index, target)
							.build();
					}
				}
			}
		}
    }
}