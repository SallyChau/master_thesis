package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.util.SingleElementUtil;

/**
 * Skip models Statements which we do not translate and who have a single
 * successor
 *
 * @author Hannah Arndt
 */
public class Skip extends Statement {

    /**
     * the program counter of the successor state
     */
    private final int nextPC;

    public Skip(SceneObject sceneObject, int nextPC) {

        super(sceneObject);
        this.nextPC = nextPC;
    }


    @Override
	public String toString() {

        return "Skip;";
    }

    @Override
    public Collection<ProgramState> computeSuccessors(ProgramState programState) {

        return Collections.singleton(programState.shallowCopyUpdatePC(nextPC));
    }

    @Override
	public Collection<ProgramState> computeSuccessorsAndCheck(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopedHeap) {
    	return computeSuccessors(programState);
    }
    
    @Override
	public Set<Node> getResultFormulae(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopedHeap) {

    	return Collections.emptySet();
    }
    
    @Override
	public boolean satisfiesFormulae(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopedHeap) {

    	return true;
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


	@Override
	public ProgramState prepareHeap(ProgramState programState) {

		return programState;
	}

}
