package de.rwth.i2.attestor.markingGeneration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.rwth.i2.attestor.grammar.canonicalization.CanonicalizationStrategy;
import de.rwth.i2.attestor.grammar.materialization.strategies.MaterializationStrategy;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.symbolicExecution.stateSpaceGenerationImpl.InternalStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies.DepthFirstStateExplorationStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies.NoPostProcessingStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies.NoStateCounter;
import de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies.NoStateLabelingStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies.NoStateRefinementStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies.TerminalStatementFinalStateStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.AbortStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.Program;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateCanonicalizationStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.StateRectificationStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerator;

public abstract  class AbstractMarkingGenerator {

    protected final Collection<String> availableSelectorLabelNames;
    protected final AbortStrategy abortStrategy;
    protected final MaterializationStrategy materializationStrategy;
    protected final CanonicalizationStrategy canonicalizationStrategy;
    protected final CanonicalizationStrategy aggressiveCanonicalizationStrategy;
    protected final StateRectificationStrategy stateRectificationStrategy;

    public AbstractMarkingGenerator(Collection<String> availableSelectorLabelNames,
                                    AbortStrategy abortStrategy,
                                    MaterializationStrategy materializationStrategy,
                                    CanonicalizationStrategy canonicalizationStrategy,
                                    CanonicalizationStrategy aggressiveCanonicalizationStrategy,
                                    StateRectificationStrategy stateRectificationStrategy) {

        this.availableSelectorLabelNames = availableSelectorLabelNames;
        this.abortStrategy = abortStrategy;
        this.materializationStrategy = materializationStrategy;
        this.canonicalizationStrategy = canonicalizationStrategy;
        this.aggressiveCanonicalizationStrategy = aggressiveCanonicalizationStrategy;
        this.stateRectificationStrategy = stateRectificationStrategy;
    }

    protected abstract List<ProgramState> placeInitialMarkings(ProgramState initialState);
    protected abstract Program getProgram();
    protected abstract Collection<HeapConfiguration> getResultingHeaps(StateSpace stateSpace);

    protected Collection<String> getAvailableSelectorLabelNames() {

        return availableSelectorLabelNames;
    }

    public Collection<HeapConfiguration> marked(ProgramState initialState) {

        List<ProgramState> initialStates = placeInitialMarkings(initialState);

        if(initialStates.isEmpty()) {
            return new ArrayList<>();
        }

        Program program = getProgram();


        StateSpaceGenerator generator = StateSpaceGenerator.builder()
                .setProgram(program)
                .addInitialStates(initialStates)
                .setAbortStrategy(abortStrategy)
                .setCanonizationStrategy(
                        new StateCanonicalizationStrategy(
                                canonicalizationStrategy
                        )
                )
                .setStateExplorationStrategy(new DepthFirstStateExplorationStrategy())
                .setMaterializationStrategy(materializationStrategy)
                .setStateRectificationStrategy(stateRectificationStrategy)
                .setStateCounter(new NoStateCounter())
                .setStateSpaceSupplier(() -> new InternalStateSpace(100000))
                .setStateLabelingStrategy(new NoStateLabelingStrategy())
                .setStateRefinementStrategy(new NoStateRefinementStrategy())
                .setPostProcessingStrategy(new NoPostProcessingStrategy())
                .setFinalStateStrategy(new TerminalStatementFinalStateStrategy())
//                .setStateSpaceGenerationStrategy(new OfflineStateSpaceGenerationStrategy())
                .build();

        try {
            StateSpace stateSpace = generator.generate();
            return getResultingHeaps(stateSpace);
        } catch (StateSpaceGenerationAbortedException e) {
            throw new IllegalStateException("Marking generation aborted. This is most likely caused by non-termination.");
        }
    }
}
