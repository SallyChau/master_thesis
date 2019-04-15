package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.ConcreteValue;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.NullPointerDereferenceException;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.SettableValue;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.util.SingleElementUtil;

/**
 * IdentityStmt models statements like x = {@literal @}this or x = {@literal @}param_1
 *
 * @author Hannah Arndt
 */
public class IdentityStmt extends Statement {

    private static final Logger logger = LogManager.getLogger("IdentityStmt");

    /**
     * the program counter of the successor statement
     */
    private final int nextPC;
    /**
     * the value to which something will be assigned
     */
    private final SettableValue lhs;
    /**
     * the string representation of the argument that will be assigned,
     * {@literal @}this, {@literal @}parameter_n
     */
    private final String rhs;

    public IdentityStmt(SceneObject sceneObject, int nextPC, SettableValue lhs, String rhs) {

        super(sceneObject);
        this.nextPC = nextPC;
        this.lhs = lhs;
        this.rhs = rhs.split(" ")[0];
    }

    /**
     * gets the value for the intermediate specified in {@link #rhs} from the heap
     * and assigns {@link #lhs} to it. Upon this the intermediate is deleted from the heap
     * (i.e. this statement can only be called once per intermediate)
     */
    @Override
    public Set<ProgramState> computeSuccessors(ProgramState programState) {

        programState = programState.clone();
        ConcreteValue concreteRHS = programState.removeIntermediate(rhs);

        try {
            lhs.setValue(programState, concreteRHS);
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
        }

        return Collections.singleton(programState.shallowCopyUpdatePC(nextPC));
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

    public boolean needsMaterialization(ProgramState programState) {

        return lhs.needsMaterialization(programState);
    }

    @Override
	public String toString() {

        return lhs + " = " + rhs + ";";
    }

    @Override
    public ViolationPoints getPotentialViolationPoints() {

        return lhs.getPotentialViolationPoints();
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
		// TODO Auto-generated method stub
		return null;
	}

}
