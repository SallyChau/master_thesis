package de.rwth.i2.attestor.phases.modelChecking.hierarchical;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.ModelCheckingContract;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.procedures.AbstractMethodExecutor;
import de.rwth.i2.attestor.procedures.ContractMatch;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.procedures.ScopeExtractor;
import de.rwth.i2.attestor.procedures.ScopedHeap;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class ComponentStateMachine {
	
	private static final Logger logger = LogManager.getLogger("componentStateMachine.java");
	
	private Method method;
	private HierarchicalProofStructure proofStructure;	
	private Map<ProgramState, StateSpace> stateSpaces = new LinkedHashMap<>();
	private Map<ProgramState, ComponentStateMachine> boxes = new LinkedHashMap<>();	
	private List<ModelCheckingContract> modelCheckingContracts = new LinkedList<>();
	
	public ComponentStateMachine(Method method) {

		this.method = method;
		this.proofStructure = new HierarchicalProofStructure(this);
	}
	
	public void addBox(ProgramState callingState, ComponentStateMachine calledCSM) {
		
		boxes.put(callingState, calledCSM);
	}

	public ComponentStateMachine getBox(ProgramState programState) {

		return boxes.get(programState);
	}
	
	public void addStateSpace(ProgramState inputState, StateSpace stateSpace) {
		
		stateSpaces.put(inputState, stateSpace);
	}
	
	/**
	 * Get state space whose input heap matches the calling state.
	 * @param callingState
	 * @param statement
	 * @return state space whose input heap matches the calling state
	 */
	public StateSpace getStateSpace(ProgramState callingState, SemanticsCommand statement) {

		StateSpace result = null;
		
		if (statement != null) {	    
		    for (ProgramState inputState : stateSpaces.keySet()) {
				if (inputState.getHeap().equals(getHeapInScope(callingState, statement))) {
					result = stateSpaces.get(inputState);
				}
			}
		} else {
			for (StateSpace stateSpace : stateSpaces.values()) {
				if (stateSpace.getInitialStates().contains(callingState)) {
					result = stateSpace;
				}
			}
		}
	    
	    return result;
	}
	
	public Collection<StateSpace> getStateSpaces() {
		
		return stateSpaces.values();
	}
	
	public SemanticsCommand getSemanticsCommand(ProgramState programState) {
		
		return method.getBody().getStatement(programState.getProgramCounter());
	}
	
	/**
	 * Model checks the underlying state spaces of the (main) procedure call for the given formula using the tableau method. 
	 * 
	 * @param call
	 * @param formula
	 * @return list of formulae to be checked in above lying state space
	 */
	public Set<Node> check(ProcedureCall call, LTLFormula formula) {

		Set<Node> formulae = new HashSet<>();
		formulae.add(formula.getASTRoot().getPLtlform());
		
		return check(call.getInput(), null, formulae);
	}
	
	/**
	 * Model checks the underlying state spaces of the procedure call for the given formula using the tableau method. 
	 * 
	 * @param inputState
	 * @param statement
	 * @param formulae
	 * @return
	 */
	public Set<Node> check(ProgramState inputState, SemanticsCommand statement, Set<Node> formulae) {
		
		logger.debug("Model checking method " + method.getSignature() + " for formulae " + formulae);
		
		StateSpace stateSpace = getStateSpace(inputState, statement);
		HeapConfiguration heapInScope = getHeapInScope(inputState, statement);
		Set<Node> returnFormulae = getOutputFormulae(heapInScope, formulae);
		if (returnFormulae.isEmpty()) {		
			proofStructure.build(stateSpace, formulae);		 
			returnFormulae = proofStructure.getOutputFormulae();
			addModelCheckingContract(heapInScope, formulae, returnFormulae, proofStructure.isSuccessful(), proofStructure.getHierarchicalFailureTrace());
			
			logger.debug("Proofstructure was successful for " + method.getSignature() + "? " + proofStructure.isSuccessful());
		} 
		
		return returnFormulae;		
	}
	
	private ModelCheckingContract matchModelCheckingContract(HeapConfiguration heap, Set<Node> formulae) {
	    
		for (ModelCheckingContract mc : modelCheckingContracts) {
			if (mc.getInputHeap().equals(heap) && mc.getInputFormulae().equals(formulae)) {
				return mc;
			}
		}
		
		return null;
	}
	
	/**
	 * Made for main procedure calls 
	 * @param call
	 * @param formula
	 * @return
	 */
	public ModelCheckingContract getModelCheckingContract(ProcedureCall call, LTLFormula formula) {
		
		Set<Node> formulae = new HashSet<>();
		formulae.add(formula.getASTRoot().getPLtlform());
	   
		return matchModelCheckingContract(call.getInput().getHeap(), formulae);
	}
	
	private void addModelCheckingContract(HeapConfiguration heapInScope, Set<Node> formulae, Set<Node> returnFormulae, boolean successful, HierarchicalFailureTrace failureTrace) {
		
		ModelCheckingResult mcResult = successful ? ModelCheckingResult.SATISFIED : ModelCheckingResult.UNSATISFIED;		
		modelCheckingContracts.add(new ModelCheckingContract(heapInScope, formulae, returnFormulae, mcResult, failureTrace));
	}
	
	private Set<Node> getOutputFormulae(HeapConfiguration heap, Set<Node> formulae) {
		
		ModelCheckingContract mc = matchModelCheckingContract(heap, formulae);
		
		if (mc != null) {
			return mc.getResultFormulae();
		} 
		
		return Collections.emptySet();
	}
	
	public HeapConfiguration getHeapInScope(ProgramState state, SemanticsCommand statement) {
		
		HeapConfiguration heapInScope = null;
		if (statement != null) {

			AbstractMethodExecutor executor = (AbstractMethodExecutor) method.getMethodExecutor();		
			ScopeExtractor scopeExtractor = executor.getScopeExtractor();			
			HeapConfiguration inputHeap = statement.prepareHeap(state).getHeap();
			ScopedHeap scopedHeap = scopeExtractor.extractScope(inputHeap);
			heapInScope = scopedHeap.getHeapInScope();
			ContractMatch contractMatch = executor.getContractCollection().matchContract(heapInScope);
		    if(contractMatch.hasMatch()) heapInScope = contractMatch.getPrecondition();
		} else {
			heapInScope = state.getHeap();
		}      	    
	    
	    return heapInScope;
	}
		
	
	
	/**
	 * Gets the model checking result for a heap and a formula.
	 * @param heap 
	 * @param formula
	 * @return model checking result for a heap and a formula
	 */
	private ModelCheckingResult getModelCheckingResult(HeapConfiguration heap, LTLFormula formula) {
		
		Set<Node> formulae = new HashSet<>();
		formulae.add(formula.getASTRoot().getPLtlform());
		
		return getModelCheckingResult(heap, formulae);
	}
	
	/**
	 * Gets the model checking result for a heap and a set of formulae.
	 * @param heap
	 * @param formulae
	 * @return model checking result for a heap and a set of formulae
	 */
	private ModelCheckingResult getModelCheckingResult(HeapConfiguration heap, Set<Node> formulae) {
		
		ModelCheckingContract mc = matchModelCheckingContract(heap, formulae);
		
		if (mc != null) {
			return mc.getModelCheckingResult();
		}
		
		return ModelCheckingResult.UNKNOWN;
	}
	
	public boolean modelCheckingSuccessful(HeapConfiguration heap, Set<Node> formulae) {
		
		return (getModelCheckingResult(heap, formulae) == ModelCheckingResult.SATISFIED);
	}
	
	public boolean modelCheckingSuccessful(HeapConfiguration heap, LTLFormula formula) {
		
		return (getModelCheckingResult(heap, formula) == ModelCheckingResult.SATISFIED);
	}
	
	public boolean modelCheckingSuccessful(ProcedureCall call, LTLFormula formula) {
		
		return (getModelCheckingResult(call.getInput().getHeap(), formula) == ModelCheckingResult.SATISFIED);
	}
	
	public boolean modelCheckingSuccessful(ProgramState state, SemanticsCommand statement, Set<Node> formulae) {
		
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
		
		Set<Node> formulae = new HashSet<>();
		formulae.add(formula.getASTRoot().getPLtlform());
		
		return getHierarchicalFailureTrace(state, statement, formulae);
	}
	
	/**
	 * Gets the failure trace resulting from model checking with the input heap and the formulae.
	 * @param heap input heap for model checking
	 * @param formulae LTLformulae to be checked
	 * @return failure trace resulting from model checking
	 */
	public HierarchicalFailureTrace getHierarchicalFailureTrace(ProgramState state, SemanticsCommand statement, Set<Node> formulae) {

		HeapConfiguration heap = getHeapInScope(state, statement);
		return getHierarchicalFailureTrace(heap, formulae);
	}
	
	public HierarchicalFailureTrace getHierarchicalFailureTrace(HeapConfiguration heap, LTLFormula formula) {
		
		Set<Node> formulae = new HashSet<>();
		formulae.add(formula.getASTRoot().getPLtlform());
		
		return getHierarchicalFailureTrace(heap, formulae);
	}
	
	public HierarchicalFailureTrace getHierarchicalFailureTrace(HeapConfiguration heap, Set<Node> formulae) {
		
		ModelCheckingContract mc = matchModelCheckingContract(heap, formulae);
		
		if (mc != null) {
			return mc.getHierarchicalFailureTrace();
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		
		return method.getSignature();
	}
}