package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;

import java.util.Collection;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis.AbstractModelCheckingMethodExecutor;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeCleanup;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeHelper;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.util.SingleElementUtil;

/**
 * InvokeStmt models statements like foo(); or bar(1,2);
 *
 * @author Hannah Arndt
 */
public class InvokeStmt extends Statement implements InvokeCleanup {

    /**
     * the abstract representation of the called method
     */
    private final Method method;
    /**
     * handles arguments, and if applicable the this-reference.
     */
    private final InvokeHelper invokePrepare;
    /**
     * the program location of the successor state
     */
    private final int nextPC;

    public InvokeStmt(SceneObject sceneObject, Method method, InvokeHelper invokePrepare, int nextPC) {

        super(sceneObject);
        this.method = method;
        this.invokePrepare = invokePrepare;
        this.nextPC = nextPC;
    }

    /**
     * gets the fixpoint from the method
     * for the input heap and returns it for the successor
     * location.<br>
     * <p>
     * If any variable appearing in the arguments is not live at this point,
     * it will be removed from the heap to enable abstraction.
     */
    @Override
    public Collection<ProgramState> computeSuccessors(ProgramState programState, ScopedHeapHierarchy scopeHierarchy) {

    	ProgramState preparedState = prepareHeap(programState);

        Collection<ProgramState> methodResult = method
                .getMethodExecutor()
                .getResultStates(programState, preparedState, new ScopedHeapHierarchy(scopeHierarchy));

        methodResult.forEach(invokePrepare::cleanHeap);
        methodResult.forEach(ProgramState::clone);
        methodResult.forEach(x -> x.setProgramCounter(nextPC));

        return methodResult;
    }
    
    @Override
	public Collection<ProgramState> computeSuccessorsAndCheck(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy) {

    	ProgramState preparedState = prepareHeap(programState);
        
        AbstractModelCheckingMethodExecutor methodExecutor = (AbstractModelCheckingMethodExecutor) method.getMethodExecutor();
        methodExecutor.setModelCheckingFormulae(formulae);
        Collection<ProgramState> methodResult = methodExecutor.getResultStates(programState, preparedState, new ScopedHeapHierarchy(scopeHierarchy)); 

        methodResult.forEach(invokePrepare::cleanHeap);
        methodResult.forEach(ProgramState::clone);
        methodResult.forEach(x -> x.setProgramCounter(nextPC));

        return methodResult;
    }
    
    @Override
	public Set<Node> getResultFormulae(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy) {
  
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

    @Override
	public ProgramState getCleanedResultState(ProgramState state) {

        invokePrepare.cleanHeap(state);
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

        return invokePrepare.baseValueString() + method.toString() + "(" + invokePrepare.argumentString() + ");";
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
