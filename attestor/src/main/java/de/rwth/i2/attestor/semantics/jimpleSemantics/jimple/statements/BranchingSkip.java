package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;

import java.util.Collection;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.util.SingleElementUtil;

/**
 * Branching Skip has no effect on the heap and two successors. It models
 * statements with two successors which we do not actually translate
 *
 * @author Hannah Arndt
 */
public class BranchingSkip extends Statement {

    /**
     * program counter for the first successor
     */
    private final int leftSuccessor;
    /**
     * program counter for the second successor
     */
    private final int rightSuccessor;

    public BranchingSkip(SceneObject sceneObject, int leftSuccessor, int rightSuccessor) {

        super(sceneObject);
        this.leftSuccessor = leftSuccessor;
        this.rightSuccessor = rightSuccessor;
    }


    @Override
	public String toString() {

        return "Skip;";
    }

    /**
     * copies the input heap to both successor states
     */
    @Override
    public Set<ProgramState> computeSuccessors(ProgramState programState) {

        ProgramState leftResult = programState.shallowCopy();
        leftResult.setProgramCounter(leftSuccessor);

        ProgramState rightResult = programState.shallowCopy();
        rightResult.setProgramCounter(rightSuccessor);

        Set<ProgramState> res = SingleElementUtil.createSet(leftResult);
        res.add(rightResult);
        return res;
    }
    
    @Override
	public Collection<ProgramState> computeSuccessorsOnTheFly(ProgramState programState, Set<Node> formulae) {
    	return computeSuccessors(programState);
    }
    
    @Override
	public Set<Node> getResultFormulaeOnTheFly(ProgramState programState, Set<Node> formulae) {

    	return null;
    }
    
    @Override
	public boolean satisfiesFormulae(ProgramState programState, Set<Node> formulae) {

    	return true;
	}

    @Override
    public ViolationPoints getPotentialViolationPoints() {

        return ViolationPoints.getEmptyViolationPoints();
    }

    @Override
    public Set<Integer> getSuccessorPCs() {

        Set<Integer> res = SingleElementUtil.createSet(leftSuccessor);
        res.add(rightSuccessor);
        return res;
    }

    @Override
    public boolean needsCanonicalization() {
        return false;
    }


	@Override
	public ProgramState prepareHeap(ProgramState programState) {
		// TODO Auto-generated method stub
		return null;
	}
}
