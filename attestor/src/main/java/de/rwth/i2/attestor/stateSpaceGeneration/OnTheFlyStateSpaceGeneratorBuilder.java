package de.rwth.i2.attestor.stateSpaceGeneration;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.materialization.strategies.MaterializationStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.OnTheFlyProofStructure;
import de.rwth.i2.attestor.refinement.AutomatonStateLabelingStrategy;

/**
 * 
 * @author chau
 *
 */
public class OnTheFlyStateSpaceGeneratorBuilder {

    /**
     * The initial state passed to the state space generation
     */
    protected final List<ProgramState> initialStates;
    /**
     * Internal instance of the StateSpaceGenerator under
     * construction by this builder
     */
    protected final OnTheFlyStateSpaceGenerator generator;


    private StateSpace initialStateSpace = null;   
    
    
    private OnTheFlyProofStructure proofStructure = null;
    
    
    private Set<Node> modelCheckingFormulae = new HashSet<>();


    /**
     * Creates a new builder representing an everywhere
     * uninitialized StateSpaceGenerator.
     */
    OnTheFlyStateSpaceGeneratorBuilder() {

        initialStates = new ArrayList<>();
        generator = new OnTheFlyStateSpaceGenerator();
    }


    /**
     * Attempts to construct a new StateSpaceGenerator.
     * If the initialization is incomplete or invalid
     * calling this method causes an IllegalStateException.
     *
     * @return StateSpaceGenerator initialized by the previously called
     * methodExecution of this builder
     */
    public OnTheFlyStateSpaceGenerator build() {

        if (initialStates.isEmpty()) {
            throw new IllegalStateException("StateSpaceGenerator: No initial states.");
        }

        if (generator.program == null) {
            throw new IllegalStateException("StateSpaceGenerator: No program.");
        }

        if (generator.materializationStrategy == null) {
            throw new IllegalStateException("StateSpaceGenerator: No materialization strategy.");
        }

        if (generator.canonicalizationStrategy == null) {
            throw new IllegalStateException("StateSpaceGenerator: No canonicalization strategy.");
        }

        if (generator.abortStrategy == null) {
            throw new IllegalStateException("StateSpaceGenerator: No abort strategy.");
        }

        if (generator.stateLabelingStrategy == null) {
            throw new IllegalStateException("StateSpaceGenerator: No state labeling strategy.");
        }

        if (generator.stateRefinementStrategy == null) {
            throw new IllegalStateException("StateSpaceGenerator: No state refinement strategy.");
        }

        if (generator.totalStatesCounter == null) {
            throw new IllegalStateException("StateSpaceGenerator: No state counter.");
        }

        if (generator.stateExplorationStrategy == null) {
            throw new IllegalStateException("StateSpaceGenerator: No state exploration strategy.");
        }

        if (generator.stateSpaceSupplier == null) {
            throw new IllegalStateException("StateSpaceGenerator: No supplier for state spaces.");
        }

        if (generator.postProcessingStrategy == null) {
            throw new IllegalStateException("StateSpaceGenerator: No post-processing strategy.");
        }

        if(generator.finalStateStrategy == null) {
            throw new IllegalStateException("StateSpaceGenerator: No final state strategy.");
        }

        if(generator.stateRectificationStrategy == null) {
            throw new IllegalStateException("StateSpaceGenerator: No admissibility strategy.");
        }

        if(initialStateSpace == null) {
            generator.stateSpace = generator.stateSpaceSupplier.get();
        } else {
            generator.stateSpace = initialStateSpace;
        }
        
        if(proofStructure == null) {
            generator.proofStructure = new OnTheFlyProofStructure();
        } else {
            generator.proofStructure = proofStructure;
        }
        
        if(generator.resultFormulae == null) {
            generator.resultFormulae = new HashSet<>();
        }
        
        if(generator.scopeHierarchy == null) {
            generator.scopeHierarchy = new ScopedHeapHierarchy();
        }

        for (ProgramState state : initialStates) {

            if(initialStateSpace == null) {
                state.setProgramCounter(0);
                generator.stateSpace.addInitialState(state);
            }
            if (state.isFromTopLevelStateSpace()) {
            	generator.stateLabelingStrategy.computeAtomicPropositions(state);
            } 
            else {
            	if (generator.stateLabelingStrategy instanceof AutomatonStateLabelingStrategy) {
            
		        	AutomatonStateLabelingStrategy stateLabelingStrategy = (AutomatonStateLabelingStrategy) generator.stateLabelingStrategy;
		        	stateLabelingStrategy.computeAtomicPropositionsFromGlobalHeap(state, generator.scopeHierarchy);   
            	}
            }
          
            generator.stateExplorationStrategy.addUnexploredState(state, false);            
            
            // for model checking 
            if(state.isContinueState()) {            	
            	try {
                	Collection<ProgramState> successors = generator.computeControlFlowSuccessors(state, modelCheckingFormulae); 
                	for (ProgramState successorState : successors) {                 		
                		generator.proofStructure.addAssertion(successorState, modelCheckingFormulae);
        			}	
				} catch (StateSpaceGenerationAbortedException e) {
					e.printStackTrace();
				}
            } else {                
            	System.out.println("StateSpaceGeneratorBuilder: adding initial assertions to proof structure");                
            	generator.proofStructure.addAssertion(state, modelCheckingFormulae);
            }
        }        
        
        return generator;
    }

    /**
     * @param initialState The initial state from which all reachable states are computed by
     *                     the state space generation.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder addInitialState(ProgramState initialState) {

        initialStates.add(initialState);
        return this;
    }

    /**
     * @param initialStates The initial states from which all reachable states are computed by
     *                      the state space generation.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder addInitialStates(List<ProgramState> initialStates) {

        this.initialStates.addAll(initialStates);
        return this;
    }
    
    public OnTheFlyStateSpaceGeneratorBuilder setScopeHierarchy(ScopedHeapHierarchy scopeHierarchy) {

    	generator.scopeHierarchy = scopeHierarchy;
    	return this;
    }

    /**
     * @param program The program that is executed to generate the state space.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder setProgram(Program program) {

        generator.program = program;
        return this;
    }

    /**
     * @param materializationStrategy The strategy used for materialization.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder setMaterializationStrategy(MaterializationStrategy materializationStrategy) {

        generator.materializationStrategy = new StateMaterializationStrategy(materializationStrategy);
        return this;
    }

    /**
     * @param canonicalizationStrategy The strategy used for canonicalization.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder setCanonizationStrategy(StateCanonicalizationStrategy canonicalizationStrategy) {

        generator.canonicalizationStrategy = canonicalizationStrategy;
        return this;
    }

    public OnTheFlyStateSpaceGeneratorBuilder setStateRectificationStrategy(StateRectificationStrategy stateRectificationStrategy) {

        generator.stateRectificationStrategy = stateRectificationStrategy;
        return this;
    }

    /**
     * @param abortStrategy The strategy used for aborting the state space generation.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder setAbortStrategy(AbortStrategy abortStrategy) {

        generator.abortStrategy = abortStrategy;
        return this;
    }

    /**
     * @param stateLabelingStrategy The strategy used to label states with atomic propositions.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder setStateLabelingStrategy(StateLabelingStrategy stateLabelingStrategy) {
    	
        generator.stateLabelingStrategy = stateLabelingStrategy;
        return this;
    }

    /**
     * @param stateRefinementStrategy The strategy to refine states before continuing the symbolic execution.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder setStateRefinementStrategy(StateRefinementStrategy stateRefinementStrategy) {

        generator.stateRefinementStrategy = stateRefinementStrategy;
        return this;
    }

    /**
     * @param stateCounter The global counter for the total number of states generated so far.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder setStateCounter(OnTheFlyStateSpaceGenerator.TotalStatesCounter stateCounter) {

        generator.totalStatesCounter = stateCounter;
        return this;
    }

    /**
     * @param strategy A strategy that determines how successors of a given state are explored.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder setStateExplorationStrategy(StateExplorationStrategy strategy) {

        generator.stateExplorationStrategy = strategy;
        return this;
    }

    /**
     * @param stateSpaceSupplier The function determining which instances of state spaces are generated
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder setStateSpaceSupplier(StateSpaceSupplier stateSpaceSupplier) {

        generator.stateSpaceSupplier = stateSpaceSupplier;
        return this;
    }

    /**
     * @param postProcessingStrategy A strategy to optimize the state space after state space generation terminated.
     *                               This strategy is applied for each generated state space, including procedure
     *                               calls.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder setPostProcessingStrategy(PostProcessingStrategy postProcessingStrategy) {

        generator.postProcessingStrategy = postProcessingStrategy;
        return this;
    }

    /**
     * Optional method to determine a (possibly non-empty) initial state space used for state space generation.
     * @param initialStateSpace The state space to use instead of a fresh one.
     * @return The builder.
     */
    public OnTheFlyStateSpaceGeneratorBuilder setInitialStateSpace(StateSpace initialStateSpace) {

        this.initialStateSpace = initialStateSpace;
        return this;
    }

    public OnTheFlyStateSpaceGeneratorBuilder setFinalStateStrategy(FinalStateStrategy finalStateStrategy) {

        generator.finalStateStrategy = finalStateStrategy;
        return this;
    }

    public OnTheFlyStateSpaceGeneratorBuilder setAlwaysCanonicalize(boolean alwaysCanonicalize) {

        generator.alwaysCanonicalize = alwaysCanonicalize;
        return this;
    }
    
    public OnTheFlyStateSpaceGeneratorBuilder setProofStructure(OnTheFlyProofStructure proofStructure) {
    	
    	this.proofStructure = proofStructure;
    	return this;
    }
    
    public OnTheFlyStateSpaceGeneratorBuilder setModelCheckingFormulae(Set<Node> modelCheckingFormulae) {
    	
    	this.modelCheckingFormulae = modelCheckingFormulae;
    	return this;
    }
}
