package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ProofStructure2;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.util.SingleElementUtil;

/**
 * GotoStmt models the statement goto pc
 *
 * @author Hannah Arndt
 */
public class GotoStmt extends Statement {

    /**
     * the program counter of the successor state
     */
    private final int nextPC;

    public GotoStmt(SceneObject sceneObject, int nextPC) {

        super(sceneObject);
        this.nextPC = nextPC;
    }


    @Override
	public String toString() {

        return "goto " + nextPC + ";";
    }

    @Override
    public Collection<ProgramState> computeSuccessors(ProgramState state) {

        return Collections.singleton(state.shallowCopyUpdatePC(nextPC));
    }

    @Override
	public Collection<ProgramState> computeSuccessors(ProgramState programState, LinkedList<Node> formulae, ProofStructure2 proofStructure) {
    	return computeSuccessors(programState);
    }
    
    @Override
    public ViolationPoints getPotentialViolationPoints() {

        return ViolationPoints.getEmptyViolationPoints();
    }

    @Override
    public Set<Integer> getSuccessorPCs() {

        return SingleElementUtil.createSet(nextPC);
    }

    @Override
    public boolean needsCanonicalization() {
        return false;
    }

}
