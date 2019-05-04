package de.rwth.i2.attestor.semantics;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;

/**
 * Terminal Statements are used to model the exit point of a method. They return
 * an empty result set.
 *
 * @author Hannah Arndt, Christoph
 */
public class TerminalStatement implements SemanticsCommand {


    @Override
    public Collection<ProgramState> computeSuccessors(ProgramState executable, ScopedHeapHierarchy scopeHierarchy) {

        return new LinkedHashSet<>();
    }
    
    @Override
	public Collection<ProgramState> computeSuccessorsAndCheck(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy) {

		return computeSuccessors(programState, scopeHierarchy);
	}
    
    @Override
	public Set<Node> getResultFormulae(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy) {

    	return Collections.emptySet();
    }
    
    @Override
	public boolean satisfiesFormulae(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy) {

    	return true;
	}

    @Override
    public ViolationPoints getPotentialViolationPoints() {

        return ViolationPoints.getEmptyViolationPoints();
    }

    @Override
    public Set<Integer> getSuccessorPCs() {

        return new LinkedHashSet<>();
    }

    @Override
    public boolean needsCanonicalization() {
        return true;
    }

    @Override
    public String toString() {

        return "program terminated";
    }

	@Override
	public ProgramState prepareHeap(ProgramState programState) {

		return programState;
	}

	

}
