package de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl;

import java.util.List;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.canonicalization.CanonicalizationStrategy;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.main.scene.Strategies;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.OnTheFlyProofStructure;
import de.rwth.i2.attestor.phases.symbolicExecution.stateSpaceGenerationImpl.InternalStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies.AggressivePostProcessingStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies.DepthFirstStateExplorationStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies.FinalStateSubsumptionPostProcessingStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies.NoPostProcessingStrategy;
import de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies.TerminalStatementFinalStateStrategy;
import de.rwth.i2.attestor.procedures.ScopedHeap;
import de.rwth.i2.attestor.stateSpaceGeneration.OnTheFlyStateSpaceGenerator;
import de.rwth.i2.attestor.stateSpaceGeneration.OnTheFlyStateSpaceGeneratorBuilder;
import de.rwth.i2.attestor.stateSpaceGeneration.PostProcessingStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.Program;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateCanonicalizationStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class OnTheFlyStateSpaceGeneratorFactory extends SceneObject {

    public OnTheFlyStateSpaceGeneratorFactory(Scene scene) {
        super(scene);
    }

    public OnTheFlyStateSpaceGenerator create(Program program, ProgramState initialState) {

        return createBuilder()
                .addInitialState(initialState)
                .setProgram(program)
                .build();
    }

    public OnTheFlyStateSpaceGenerator create(Program program, List<ProgramState> initialStates) {

        return createBuilder()
                .addInitialStates(initialStates)
                .setProgram(program)
                .build();
    }

    protected OnTheFlyStateSpaceGeneratorBuilder createBuilder() {

        Strategies strategies = scene().strategies();

        return OnTheFlyStateSpaceGenerator
                .builder()
                .setStateLabelingStrategy(
                        strategies.getStateLabelingStrategy()
                )
                .setMaterializationStrategy(
                        strategies.getMaterializationStrategy()
                )
                .setStateRectificationStrategy(
                        strategies.getStateRectificationStrategy()
                )
                .setAlwaysCanonicalize(
                        strategies.isAlwaysCanonicalize()
                )
                .setCanonizationStrategy(
                        new StateCanonicalizationStrategy(strategies.getCanonicalizationStrategy())
                )
                .setAbortStrategy(
                        strategies.getAbortStrategy()
                )
                .setStateRefinementStrategy(
                        strategies.getStateRefinementStrategy()
                )
                .setStateCounter(
                        scene()::addNumberOfGeneratedStates
                )
                .setStateExplorationStrategy(new DepthFirstStateExplorationStrategy())
                .setStateSpaceSupplier(() -> new InternalStateSpace(scene().options().getMaxStateSpace()))
                .setPostProcessingStrategy(getPostProcessingStrategy())
                .setFinalStateStrategy(new TerminalStatementFinalStateStrategy())
                ;
    }

    private PostProcessingStrategy getPostProcessingStrategy() {

        CanonicalizationStrategy aggressiveStrategy = scene().strategies().getAggressiveCanonicalizationStrategy();

        if (!scene().options().isPostprocessingEnabled() || !scene().options().isAdmissibleAbstractionEnabled()) {
            return new NoPostProcessingStrategy();
        }

        StateCanonicalizationStrategy strategy = new StateCanonicalizationStrategy(aggressiveStrategy);

        if (scene().options().isIndexedMode()) {
            return new AggressivePostProcessingStrategy(strategy, scene().options().isAdmissibleAbstractionEnabled());
        }

        return new FinalStateSubsumptionPostProcessingStrategy(
                strategy,
                scene().options().isAdmissibleAbstractionEnabled()
        );
    }


    public OnTheFlyStateSpaceGenerator create(Program program, ProgramState initialState, StateSpace stateSpace) {

        if(stateSpace == null) {
            throw new IllegalArgumentException("Attempt to continue state space generation with empty state space.");
        }

        return createBuilder()
                .addInitialState(initialState)
                .setProgram(program)
                .setInitialStateSpace(stateSpace)
                .build();
    }
    
    public OnTheFlyStateSpaceGenerator create(Program program, ProgramState initialState, StateSpace stateSpace, OnTheFlyProofStructure proofStructure) {

        if(stateSpace == null) {
            throw new IllegalArgumentException("Attempt to continue state space generation with empty state space.");
        }

        return createBuilder()
                .addInitialState(initialState)
                .setProgram(program)
                .setInitialStateSpace(stateSpace)
                .setProofStructure(proofStructure)
                .build();
    }
    
    public OnTheFlyStateSpaceGenerator create(Program program, 
    							      ProgramState initialState, 
    							      StateSpace stateSpace, 
    							      OnTheFlyProofStructure proofStructure, 
    							      Set<Node> modelCheckingFormulae) {

        if(stateSpace == null) {
            throw new IllegalArgumentException("Attempt to continue state space generation with empty state space.");
        }

        return createBuilder()
                .addInitialState(initialState)
                .setProgram(program)
                .setInitialStateSpace(stateSpace)
                .setProofStructure(proofStructure)
                .setModelCheckingFormulae(modelCheckingFormulae)
                .build();
    }
    
    public OnTheFlyStateSpaceGenerator create(Program program, 
								      ProgramState initialState, 
								      StateSpace stateSpace, 
								      Set<Node> modelCheckingFormulae) {

    	if(stateSpace == null) {
			throw new IllegalArgumentException("Attempt to continue state space generation with empty state space.");
		}
				
		return createBuilder()
				.addInitialState(initialState)
				.setProgram(program)
				.setInitialStateSpace(stateSpace)
				.setModelCheckingFormulae(modelCheckingFormulae)
				.build();
	}
    
    public OnTheFlyStateSpaceGenerator create(Program program, 
								      List<ProgramState> initialStates, 
								      Set<Node> modelCheckingFormulae) {
		
		return createBuilder()
			.addInitialStates(initialStates)
			.setProgram(program)
			.setModelCheckingFormulae(modelCheckingFormulae)
			.build();
	}
    
    public OnTheFlyStateSpaceGenerator create(Program program, 
								      ProgramState initialState, 
								      Set<Node> modelCheckingFormulae) {

		return createBuilder()
			.addInitialState(initialState)
			.setProgram(program)
			.setModelCheckingFormulae(modelCheckingFormulae)
			.build();
	}
    
    public OnTheFlyStateSpaceGenerator create(Program program, 
								      ProgramState initialState, 
								      ScopedHeap scopedHeap,
								      Set<Node> modelCheckingFormulae) {

		return createBuilder()
			.addInitialState(initialState)
			.setScopedHeap(scopedHeap)
			.setProgram(program)
			.setModelCheckingFormulae(modelCheckingFormulae)
			.build();
	}
}
