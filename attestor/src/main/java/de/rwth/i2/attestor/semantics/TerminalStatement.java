package de.rwth.i2.attestor.semantics;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
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
    public Collection<ProgramState> computeSuccessors(ProgramState executable) {

        return new LinkedHashSet<>();
    }
    
    @Override
	public Collection<ProgramState> computeSuccessors(ProgramState programState, List<Node> formulae) {

		return computeSuccessors(programState);
	}
    
    @Override
	public List<Node> getResultFormulae(ProgramState programState, List<Node> formulae) {

    	return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	

}
