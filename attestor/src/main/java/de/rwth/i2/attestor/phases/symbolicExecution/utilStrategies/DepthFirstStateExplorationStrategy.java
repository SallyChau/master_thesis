package de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies;

import java.util.LinkedList;

import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateExplorationStrategy;

public class DepthFirstStateExplorationStrategy implements StateExplorationStrategy {

    private LinkedList<ProgramState> unexploredStates = new LinkedList<>();

    @Override
    public boolean hasUnexploredStates() {

        return !unexploredStates.isEmpty();
    }

    @Override
    public ProgramState getNextUnexploredState() {

        return unexploredStates.removeLast();
    }

    @Override
    public void addUnexploredState(ProgramState state, boolean isMaterializedState) {

        unexploredStates.addLast(state);
    }
}
