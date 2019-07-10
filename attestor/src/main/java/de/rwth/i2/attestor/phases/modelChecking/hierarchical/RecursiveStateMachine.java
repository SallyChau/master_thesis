package de.rwth.i2.attestor.phases.modelChecking.hierarchical;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingContract;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class RecursiveStateMachine {
	
	private static final Logger logger = LogManager.getLogger("componentStateMachine.java");

	private Map<Method, ComponentStateMachine> components = new LinkedHashMap<>();
	private Map<ProcedureCall, List<ModelCheckingContract>> modelCheckingResults = new LinkedHashMap<>();
	
	public RecursiveStateMachine(Map<StateSpace, ProcedureCall> stateSpaceToAnalyzedCall, 
			Map<ProgramState, ProcedureCall> callingStatesToCall, List<String> methodsToSkip) {

		// build Component State Machines
		Collection<ProcedureCall> procedureCalls = stateSpaceToAnalyzedCall.values();
		for (ProcedureCall call : procedureCalls) {
			
			Method method = call.getMethod();
			
			if (!methodsToSkip.contains(method.getName())) {			
			
				StateSpace stateSpace = null;
				for (Entry<StateSpace, ProcedureCall> entry : stateSpaceToAnalyzedCall.entrySet()) {
		            if (Objects.equals(call, entry.getValue())) {
		                stateSpace = entry.getKey();
		            }
		        }
		        
				ProgramState callingState = call.getInput();
				
				ComponentStateMachine csm = getOrCreateComponentStateMachine(method);
				csm.addStateSpace(callingState, stateSpace); 
				
				components.put(method, csm);	
				
				logger.debug("Created CSM for method " + method.getName());
			} else {
				logger.debug("Skipping method " + method.getName());
			}
		}
		
		// translate procedure calls to CSM boxes
		Set<ProgramState> callingStates = callingStatesToCall.keySet();
		for (ProgramState state : callingStates) {
			StateSpace stateSpace = state.getContainingStateSpace();
			ComponentStateMachine caller = getComponentStateMachine(stateSpace);
			ComponentStateMachine callee = getComponentStateMachine(callingStatesToCall.get(state).getMethod());
			if (caller != null && callee != null) caller.addBox(state, callee);
		}		
	}
	
	private ComponentStateMachine getComponentStateMachine(StateSpace stateSpace) {
		
		for (ComponentStateMachine csm : components.values()) {
			if (csm.getStateSpaces().contains(stateSpace)) {
				return csm;
			}
		}
		
		return null;
	}

	public ComponentStateMachine getOrCreateComponentStateMachine(Method method) {
		
		if(components.containsKey(method)) {
            return components.get(method);
        } else {
            ComponentStateMachine result = new ComponentStateMachine(method);
            components.put(method, result);
            return result;
        }
	}	
	
	public ComponentStateMachine getComponentStateMachine(Method method) {
		
        return components.get(method);
	}	
	
	/**
	 * Model checks the underlying state spaces of the procedure call for the given formula using the tableau method.
	 * @param call
	 * @param formula
	 * @return true if the proof structure is successful, false otherwise
	 */
	public boolean check(ProcedureCall call, LTLFormula formula) {
        
        ComponentStateMachine csm = getOrCreateComponentStateMachine(call.getMethod());
		
		csm.check(call, formula);
		
		ModelCheckingContract contract = csm.getModelCheckingContract(call, formula);		
		addModelCheckingContract(call, contract);
		
		return contract.isModelCheckingSuccessful();
	}
	
	public boolean modelCheckingSuccessful(ProcedureCall call, LTLFormula formula) {
		
		ModelCheckingContract contract = getModelCheckingContract(call, formula);
		return (contract != null) ? contract.isModelCheckingSuccessful() : null;
	}
	
	public HierarchicalFailureTrace getHierarchicalFailureTrace(ProcedureCall call, LTLFormula formula) {
		
		ModelCheckingContract contract = getModelCheckingContract(call, formula);
		return (contract != null) ? contract.getHierarchicalFailureTrace() : null;
	}
	
	private void addModelCheckingContract(ProcedureCall call, ModelCheckingContract contract) {
		
		List<ModelCheckingContract> contracts = modelCheckingResults.get(call);
		
		if (contracts == null) {
			contracts = new LinkedList<>();
			contracts.add(contract);
			modelCheckingResults.put(call, contracts);
        } else {
        	contracts.add(contract);
        }
	}
	
	private ModelCheckingContract getModelCheckingContract(ProcedureCall call, LTLFormula formula) {
		
		List<ModelCheckingContract> contracts =  modelCheckingResults.get(call);
		List<Node> formulae = new LinkedList<>();
		formulae.add(formula.getASTRoot().getPLtlform());
		
		for (ModelCheckingContract contract : contracts) {
			
			if (contract.getInputFormulae().equals(formulae)) return contract;
		}
		
		return null;
	}
	
	public int getSize() {
		return components.size();
	}
	
	@Override
	public String toString() {
		
		return "RSM  with " + components.size() + " components";
	}
}
