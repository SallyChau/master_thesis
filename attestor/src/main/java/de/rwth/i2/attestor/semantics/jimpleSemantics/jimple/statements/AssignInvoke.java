package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis.AbstractModelCheckingMethodExecutor;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeCleanup;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeHelper;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.ConcreteValue;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.NullPointerDereferenceException;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.SettableValue;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.util.SingleElementUtil;

/**
 * AssignInvoke models statements of the form x = foo(); or x = bar(3, name);
 *
 * @author Hannah Arndt
 */
public class AssignInvoke extends Statement implements InvokeCleanup {

    private static final Logger logger = LogManager.getLogger("AssignInvoke");

    /**
     * the value to which the result will be assigned
     */
    private final SettableValue lhs;
    /**
     * the abstract translation of the method that is called
     */
    private final Method method;
    /**
     * handles arguments, and if applicable the this-reference.
     */
    private final InvokeHelper invokePrepare;
    /**
     * the program counter of the successor statement
     */
    private final int nextPC;

    public AssignInvoke(SceneObject sceneObject, SettableValue lhs,
                        Method method, InvokeHelper invokePrepare,
                        int nextPC) {

        super(sceneObject);
        this.lhs = lhs;
        this.method = method;
        this.invokePrepare = invokePrepare;
        this.nextPC = nextPC;
    }

    /**
     * gets the fixpoint of the abstract method for the given input.
     * For each of the resulting heaps, retrieves the return argument and
     * creates a new heap where it is assigned correctly.
     * If a result is lacking a return, it is ignored.<br>
     * <p>
     * If any variable appearing in the arguments is not live at this point,
     * it will be removed from the heap to enable abstraction. Furthermore,
     * if lhs is a variable it will be removed before invoking the function,
     * as it is clearly not live at this point.
     */
    @Override
    public Collection<ProgramState> computeSuccessors(ProgramState programState, ScopedHeapHierarchy scopeHierarchy) {

        // programState is callingState, prepared state is new input
        ProgramState preparedState = programState.clone();
        invokePrepare.prepareHeap(preparedState);

        Collection<ProgramState> methodResult = method
                .getMethodExecutor()
                .getResultStates(programState, preparedState, new ScopedHeapHierarchy(scopeHierarchy));
        return getCleanedResultStates(methodResult);
    }
    
    @Override
	public Collection<ProgramState> computeSuccessorsAndCheck(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy) {

        // programState is callingState, prepared state is new input
    	ProgramState preparedState = prepareHeap(programState);

        AbstractModelCheckingMethodExecutor methodExecutor = (AbstractModelCheckingMethodExecutor) method.getMethodExecutor();
        methodExecutor.setModelCheckingFormulae(formulae);
        Collection<ProgramState> methodResult = methodExecutor.getResultStates(programState, preparedState, new ScopedHeapHierarchy(scopeHierarchy));
        
        return getCleanedResultStates(methodResult);
    }
    
    @Override
	public Set<Node> getResultFormulae(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy) {
    	// programState is callingState, prepared state is new input
    	ProgramState preparedState = prepareHeap(programState);
    	
        AbstractModelCheckingMethodExecutor methodExecutor = (AbstractModelCheckingMethodExecutor) method.getMethodExecutor();
        return methodExecutor.getModelCheckingResultFormulae(programState, preparedState, formulae, new ScopedHeapHierarchy(scopeHierarchy));
    }
    
    @Override
	public boolean satisfiesFormulae(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy) {
		
    	ProgramState preparedState = prepareHeap(programState);
    	
        AbstractModelCheckingMethodExecutor methodExecutor = (AbstractModelCheckingMethodExecutor) method.getMethodExecutor();
        return methodExecutor.satisfiesFormulae(programState, preparedState, formulae, new ScopedHeapHierarchy(scopeHierarchy));
	}

    protected Collection<ProgramState> getCleanedResultStates(Collection<ProgramState> resultStates) {

        Set<ProgramState> assignResult = new LinkedHashSet<>();
        for (ProgramState resState : resultStates) {

            resState = getCleanedResultState(resState);
            ProgramState freshState = resState.clone();
            freshState.setProgramCounter(nextPC);
            assignResult.add(freshState);
        }
        return assignResult;
    }

    @Override
	public ProgramState getCleanedResultState(ProgramState state) {

        ConcreteValue concreteRHS = state.removeIntermediate("@return");
        invokePrepare.cleanHeap(state);

        try {
            lhs.setValue(state, concreteRHS);
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
        }

        return state;

    }

    public boolean needsMaterialization(ProgramState programState) {

        return invokePrepare.needsMaterialization(programState);
    }
    
    @Override
    public ProgramState prepareHeap(ProgramState programState) {
    	ProgramState preparedState = programState.clone();
        invokePrepare.prepareHeap(preparedState);
        
        return preparedState;
    }

    @Override
	public String toString() {

        String res = lhs.toString() + " = ";
        res += invokePrepare.baseValueString() + method.toString() + "(";
        res += invokePrepare.argumentString();
        res += ");";
        return res;
    }

    @Override
    public ViolationPoints getPotentialViolationPoints() {

        return invokePrepare.getPotentialViolationPoints();
    }

    @Override
    public Set<Integer> getSuccessorPCs() {

        return SingleElementUtil.createSet(nextPC);
    }

    @Override
    public boolean needsCanonicalization() {
        return true;
    }
}
