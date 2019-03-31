package de.rwth.i2.attestor.phases.modelChecking.hierarchical;

import java.util.Set;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.AbstractProofStructure;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.Assertion2;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.recursiveStateMachine.ComponentStateMachine;
import de.rwth.i2.attestor.recursiveStateMachine.RecursiveStateMachine;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class HierarchicalProofStructure extends AbstractProofStructure {	

	private RecursiveStateMachine rsm;
	private ComponentStateMachine mainCSM;
	private ProgramState inputState;
	private LTLFormula formula;
	
	public HierarchicalProofStructure(RecursiveStateMachine rsm) {
		
		super();
		this.rsm = rsm;		
	}
	
	public void build(ProcedureCall mainCall, LTLFormula formula) {
		
		mainCSM = rsm.getOrCreateComponentStateMachine(mainCall.getMethod());
		inputState = mainCall.getInput();
		this.formula = formula;
		StateSpace stateSpace = mainCSM.getStateSpace(inputState);
		
		mainCSM.check(stateSpace, inputState, formula);
		successful = mainCSM.modelCheckingSuccessful(inputState.getHeap(), formula); 
	}    

	@Override
	public Set<Assertion2> getLeaves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer size() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Assertion2> getVertices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FailureTrace getFailureTrace() {

		return null;
	}
	
	public HierarchicalFailureTrace getHierarchicalFailureTrace() {

		return mainCSM.getHierarchicalFailureTrace(inputState.getHeap(), formula);
	}
	
//	public FailureTrace getFullFailureTrace() {
//		List<Node> formulae = new LinkedList<>();
//		formulae.add(formula.getASTRoot().getPLtlform());
//		return mainCSM.getFullFailureTrace(inputState.getHeap(), formulae);
//	}
}
