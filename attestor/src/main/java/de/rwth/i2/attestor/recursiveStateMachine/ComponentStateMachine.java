package de.rwth.i2.attestor.recursiveStateMachine;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.hierarchical.CSMProofStructure;
import de.rwth.i2.attestor.phases.modelChecking.hierarchical.HierarchicalFailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.hierarchical.ModelCheckingContract;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.procedures.AbstractMethodExecutor;
import de.rwth.i2.attestor.procedures.ContractMatch;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;
import de.rwth.i2.attestor.procedures.ScopedHeap;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class ComponentStateMachine {
	
	private Method method;
	private String signature;
	private CSMProofStructure proofStructure;	
	private Map<ProgramState, StateSpace> inputStateToStateSpace;
	private Map<ProgramState, ComponentStateMachine> callingStateToCSM;	
	private List<ModelCheckingContract> modelCheckingContracts;
	
	public ComponentStateMachine(Method method) {

		this.method = method;
		this.signature = method.getSignature();
		this.proofStructure = new CSMProofStructure(this);
		this.inputStateToStateSpace = new LinkedHashMap<>();
		this.callingStateToCSM = new LinkedHashMap<>();
		this.modelCheckingContracts = new LinkedList<>();
	}
	
	public List<Node> check(StateSpace stateSpace, ProgramState inputState, LTLFormula formula) {
		
		System.out.println("Checking CSM for " + method.getSignature());
		System.out.println("Formulae " + formula);
		
		List<Node> formulae = new LinkedList<>();
		formulae.add(formula.getASTRoot().getPLtlform());
		
		proofStructure.build(stateSpace, formula);		
		List<Node> returnFormulae = proofStructure.getOutputFormulae();

		System.err.println("Proof structure successful: " + proofStructure.isSuccessful());
		System.err.println("Failure trace: " + proofStructure.getFailureTrace());
		
		addModelCheckingContract(inputState.getHeap(), formulae, returnFormulae, proofStructure.isSuccessful(), proofStructure.getHierarchicalFailureTrace());
		
		return returnFormulae;
	}
	
	public List<Node> check(ProgramState inputState, SemanticsCommand statement, List<Node> formulae) {
		
		System.out.println("Checking CSM for " + method.getSignature());
		System.out.println("Formulae " + formulae);
		
		StateSpace stateSpace = getCalledStateSpace(inputState, statement);
		HeapConfiguration heapInScope = getHeapInScope(inputState, statement);
		List<Node> returnFormulae = getOutputFormulae(heapInScope, formulae);
		if (returnFormulae.isEmpty()) {
		
			proofStructure.build(stateSpace, formulae);
			
			System.err.println("CSM: Proof structure successful: " + proofStructure.isSuccessful());
			System.err.println("CSM: Failure trace: " + proofStructure.getFailureTrace());
			
			returnFormulae = proofStructure.getOutputFormulae();
			addModelCheckingContract(heapInScope, formulae, returnFormulae, proofStructure.isSuccessful(), proofStructure.getHierarchicalFailureTrace());
		} else {
			System.err.println("Using existing MC contract");
		}
		
		return returnFormulae;
	}
	
	private ModelCheckingContract matchModelCheckingContract(HeapConfiguration heap, List<Node> formulae) {
		
		System.out.println("Checking model checking contracts for " + method.getSignature());
	    
		for (ModelCheckingContract mc : modelCheckingContracts) {
			if (mc.getInput().equals(heap) && mc.getInputFormulae().equals(formulae)) {
				return mc;
			}
		}
		
		return null;
	}
	
	private void addModelCheckingContract(HeapConfiguration heapInScope, List<Node> formulae, List<Node> returnFormulae, boolean successful, HierarchicalFailureTrace failureTrace) {
		
		ModelCheckingResult mcResult = successful ? ModelCheckingResult.SATISFIED : ModelCheckingResult.UNSATISFIED;		
		modelCheckingContracts.add(new ModelCheckingContract(heapInScope, formulae, returnFormulae, mcResult, failureTrace));
	}
	
	private List<Node> getOutputFormulae(HeapConfiguration heap, List<Node> formulae) {
		
		ModelCheckingContract mc = matchModelCheckingContract(heap, formulae);
		
		if (mc != null) {
			return mc.getOutputFormulae();
		} 
		
		return Collections.emptyList();
	}
	
	public HeapConfiguration getHeapInScope(ProgramState state, SemanticsCommand statement) {
		
		AbstractMethodExecutor executor = (AbstractMethodExecutor) method.getMethodExecutor();		
		ScopeExtractor scopeExtractor = executor.getScopeExtractor();
		
        HeapConfiguration inputHeap = statement.prepareHeap(state).getHeap();
        ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
	    HeapConfiguration heapInScope = scopedHeap.getHeapInScope();
	    
	    ContractMatch contractMatch = executor.getContractCollection().matchContract(heapInScope);
	    if( contractMatch.hasMatch() ) {
	    	heapInScope = contractMatch.getPrecondition();
	    }
	    
	    return heapInScope;
	}
		
	public void addCalledCSM(ProgramState callingState, ComponentStateMachine calledCSM) {
		
		callingStateToCSM.put(callingState, calledCSM);
	}

	public ComponentStateMachine getCalledCSM(ProgramState programState) {

		return callingStateToCSM.get(programState);
	}
	
	public void addStateSpace(ProgramState inputState, StateSpace stateSpace) {
		
		inputStateToStateSpace.put(inputState, stateSpace);
	}
	
	/**
	 * Gets state space whose list of initial states contains the input state.
	 * @param inputState
	 * @return state space whose list of initial states contains the input state
	 */
	public StateSpace getStateSpace(ProgramState inputState) {
		
		for (StateSpace stateSpace : inputStateToStateSpace.values()) {
			if (stateSpace.getInitialStates().contains(inputState)) {
				return stateSpace;
			}
		}
		
		return null;
	}
	
	/**
	 * Get state space whose input heap matches the calling state.
	 * @param callingState
	 * @param statement
	 * @return state space whose input heap matches the calling state
	 */
	public StateSpace getCalledStateSpace(ProgramState callingState, SemanticsCommand statement) {

		StateSpace result = null;

	    HeapConfiguration heapInScope = getHeapInScope(callingState, statement);
	    
	    for (ProgramState inputState : inputStateToStateSpace.keySet()) {
			if (inputState.getHeap().equals(heapInScope)) {
				result = inputStateToStateSpace.get(inputState);
			}
		}
	    
	    return result;
	}
	
	public Collection<StateSpace> getStateSpaces() {
		
		return inputStateToStateSpace.values();
	}
	
	/**
	 * Gets the model checking result for a heap and a formula.
	 * @param heap 
	 * @param formula
	 * @return model checking result for a heap and a formula
	 */
	private ModelCheckingResult getModelCheckingResult(HeapConfiguration heap, LTLFormula formula) {
		
		List<Node> formulae = new LinkedList<>();
		formulae.add(formula.getASTRoot().getPLtlform());
		
		return getModelCheckingResult(heap, formulae);
	}
	
	/**
	 * Gets the model checking result for a heap and a list of formulae.
	 * @param heap
	 * @param formulae
	 * @return model checking result for a heap and a list of formulae
	 */
	private ModelCheckingResult getModelCheckingResult(HeapConfiguration heap, List<Node> formulae) {
		
		ModelCheckingContract mc = matchModelCheckingContract(heap, formulae);
		
		if (mc != null) {
			return mc.getModelCheckingResult();
		}
		
		return ModelCheckingResult.UNKNOWN;
	}
	
	public boolean modelCheckingSuccessful(HeapConfiguration heap, List<Node> formulae) {
		
		return (getModelCheckingResult(heap, formulae) == ModelCheckingResult.SATISFIED);
	}
	
	public boolean modelCheckingSuccessful(HeapConfiguration heap, LTLFormula formula) {
		
		return (getModelCheckingResult(heap, formula) == ModelCheckingResult.SATISFIED);
	}
	
	public boolean modelCheckingSuccessful(ProgramState state, SemanticsCommand statement, List<Node> formulae) {
		
		HeapConfiguration heap = getHeapInScope(state, statement);
		return (getModelCheckingResult(heap, formulae) == ModelCheckingResult.SATISFIED);
	}
	
	/**
	 * Gets the failure trace resulting from model checking with the input heap and the formulae.
	 * @param heap input heap for model checking
	 * @param formulae LTLformulae to be checked
	 * @return failure trace resulting from model checking
	 */
	public HierarchicalFailureTrace getHierarchicalFailureTrace(ProgramState state, SemanticsCommand statement, LTLFormula formula) {
		
		List<Node> formulae = new LinkedList<>();
		formulae.add(formula.getASTRoot().getPLtlform());
		
		return getHierarchicalFailureTrace(state, statement, formulae);
	}
	
	/**
	 * Gets the failure trace resulting from model checking with the input heap and the formulae.
	 * @param heap input heap for model checking
	 * @param formulae LTLformulae to be checked
	 * @return failure trace resulting from model checking
	 */
	public HierarchicalFailureTrace getHierarchicalFailureTrace(ProgramState state, SemanticsCommand statement, List<Node> formulae) {

		HeapConfiguration heap = getHeapInScope(state, statement);
		return getHierarchicalFailureTrace(heap, formulae);
	}
	
	public HierarchicalFailureTrace getHierarchicalFailureTrace(HeapConfiguration heap, LTLFormula formula) {
		
		List<Node> formulae = new LinkedList<>();
		formulae.add(formula.getASTRoot().getPLtlform());
		
		return getHierarchicalFailureTrace(heap, formulae);
	}
	
	public HierarchicalFailureTrace getHierarchicalFailureTrace(HeapConfiguration heap, List<Node> formulae) {
		
		ModelCheckingContract mc = matchModelCheckingContract(heap, formulae);
		
		if (mc != null) {
			return mc.getHierarchicalFailureTrace();
		}
		
		return null;
	}
	
//	public FailureTrace getFullFailureTrace(HeapConfiguration heap, List<Node> formulae) {
//		
//		FailureTrace currentTrace = getFailureTrace(heap, formulae);
//		while (currentTrace.hasNext()) {
//			ProgramState state = currentTrace.next();
//			HeapConfiguration heapInScope = getHeapInScope(state, method.getBody().getStatement(state.getProgramCounter()));
//			ComponentStateMachine calledCSM = getCalledCSM(state);
//			ModelCheckingContract mc = matchModelCheckingContract(heapInScope, formulae);
//			if (calledCSM != null) {
//				System.out.println("calling state");
//				calledCSM.getFullFailureTrace(getHeapInScope(state, method.getBody().getStatement(state.getProgramCounter())), formulae);
//				
//			}
//			System.out.println(state);
//		}
//		
//		return null;
//	}
	
	public SemanticsCommand getSemanticsCommand(ProgramState programState) {
		
		return method.getBody().getStatement(programState.getProgramCounter());
	}
	
	public String getSignature() {
		return signature;
	}
	
	public Method getMethod() {
		return this.method;
	}
	
	@Override
	public String toString() {
		
		return getSignature();
	}
}
