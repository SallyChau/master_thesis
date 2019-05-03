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
                programState.addAP(ap);
                System.err.println("AutomationStateLabelingStrategy: Added AP " + ap + "to state " + programState);
            }
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
				System.out.println("AutomationStateLabelingStrategy: Merging scopedHeap #" + i);
				ScopedHeap scopedHeap = scopes.get(i);
	
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
}