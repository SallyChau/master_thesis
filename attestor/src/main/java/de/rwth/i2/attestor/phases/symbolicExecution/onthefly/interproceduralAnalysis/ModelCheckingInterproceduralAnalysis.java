package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.modelChecking.hierarchical.HierarchicalFailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyPartialStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.OnTheFlyProofStructure;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.PartialStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class ModelCheckingInterproceduralAnalysis {
	
	protected Deque<ProcedureCall> remainingProcedureCalls = new ArrayDeque<>();
	protected Deque<OnTheFlyPartialStateSpace> remainingPartialStateSpaces = new ArrayDeque<>();

	protected Map<ProcedureCall, Set<OnTheFlyPartialStateSpace>> callingDependencies = new LinkedHashMap<>();
	protected Map<StateSpace, ProcedureCall> stateSpaceToAnalyzedCall = new LinkedHashMap<>();
	
	protected Map<ProcedureCall, OnTheFlyProofStructure> callToProofStructure = new LinkedHashMap<>();
	protected Map<ProcedureCall, Set<Node>> callToFormulae = new LinkedHashMap<>();
	protected Map<ProcedureCall, Set<Node>> callToReturnFormulae = new LinkedHashMap<>();	
	protected Map<PartialStateSpace, Set<Node>> partialStateSpaceToContinueFormulae = new LinkedHashMap<>();
	
	protected HierarchicalFailureTrace hierarchicalFailureTrace = new HierarchicalFailureTrace();
	
	protected Set<Method> methodsToSkip = new HashSet<>();


	
	public void registerStateSpace(ProcedureCall call, StateSpace stateSpace) {

		stateSpaceToAnalyzedCall.put(stateSpace, call);
	}

	public void registerDependency(ProcedureCall procedureCall, OnTheFlyPartialStateSpace dependentPartialStateSpace) {

		if(!callingDependencies.containsKey(procedureCall)) {
			Set<OnTheFlyPartialStateSpace> dependencies = new LinkedHashSet<>();
			dependencies.add(dependentPartialStateSpace);
			callingDependencies.put(procedureCall, dependencies);
		} else {
			callingDependencies.get(procedureCall).add(dependentPartialStateSpace);
		}
	}

	public void registerProcedureCall(ProcedureCall procedureCall) {

		if(!remainingProcedureCalls.contains(procedureCall)) {
			remainingProcedureCalls.push(procedureCall);
		} 
	}

	public void registerProofStructure(ProcedureCall procedureCall, OnTheFlyProofStructure proofStructure) {
		
		callToProofStructure.put(procedureCall, proofStructure);
	}
	
	public void registerFormulae(ProcedureCall procedureCall, Set<Node> formulae) {
		
		if(!callToFormulae.containsKey(procedureCall)) {
			callToFormulae.put(procedureCall, formulae);
		} else {
			callToFormulae.get(procedureCall).addAll(formulae);
		}
	}
	
	public void registerResultFormulae(ProcedureCall procedureCall, Set<Node> returnFormulae) {
		
		if(!callToReturnFormulae.containsKey(procedureCall)) {
			callToReturnFormulae.put(procedureCall, returnFormulae);
		} else {
			callToReturnFormulae.get(procedureCall).addAll(returnFormulae);
		}
	}
	
	public void registerMethodToSkip(Method method) {
		
		methodsToSkip.add(method);
	}
	
	public void addFailureTrace(FailureTrace failureTrace) {
		
		hierarchicalFailureTrace.addFailureTrace(failureTrace);
	}
	
	public HierarchicalFailureTrace getHierarchicalFailureTrace() {
		
		return hierarchicalFailureTrace;
	}

	public Set<Method> getMethodsToSkip() {
		
		return methodsToSkip;		
	}
	
	

	public void run() {

		while (!remainingProcedureCalls.isEmpty() || !remainingPartialStateSpaces.isEmpty()) {
			
			OnTheFlyProcedureCall call;
			Set<Node> formulae;
			boolean contractChanged;
			
			if (!remainingProcedureCalls.isEmpty()) {
				
				// creates new state space for recursive methods
				call = (OnTheFlyProcedureCall) remainingProcedureCalls.pop();
				formulae = callToFormulae.get(call);
				call.setModelCheckingFormulae(formulae);

				StateSpace stateSpace = call.execute();
				
				OnTheFlyProofStructure proofStructure = callToProofStructure.get(call);
				if (!proofStructure.isSuccessful()) {
					abortDependingProofStructures(call);
					FailureTrace ft = proofStructure.getFailureTrace(stateSpace);
					addFailureTrace(ft);
				}				
				
				contractChanged = stateSpace.getFinalStateIds().size() > 0;
			} else {
				
				// continue partial state space 
				OnTheFlyPartialStateSpace partialStateSpace = remainingPartialStateSpaces.pop();				
				int currentNumberOfFinalStates = partialStateSpace.unfinishedStateSpace().getFinalStateIds().size();
				
				call = (OnTheFlyProcedureCall) stateSpaceToAnalyzedCall.get(partialStateSpace.unfinishedStateSpace());
				
				OnTheFlyProofStructure proofStructure = callToProofStructure.get(call);				
				Set<Node> continueFormulae = partialStateSpaceToContinueFormulae.get(partialStateSpace);	
				
				if (!proofStructure.isSuccessful()) {
	            	// abort this structure and notify others
	            	abortDependingProofStructures(call);
					addFailureTrace(proofStructure.getFailureTrace(partialStateSpace.unfinishedStateSpace()));
	            } else {	
	            	// prepare execution
	            	partialStateSpace.setModelCheckingFormulae(continueFormulae);
	            	partialStateSpace.setProofStructure(proofStructure);
	            	partialStateSpace.modelCheck(!getMethodsToSkip().contains(call.getMethod()));
					
	            	partialStateSpace.continueExecution(call);
					
					// abort depending proof structures if current proof structure was unsuccessful
					if (!proofStructure.isSuccessful()) {
						abortDependingProofStructures(call);
						addFailureTrace(proofStructure.getFailureTrace(partialStateSpace.unfinishedStateSpace()));
					} else {
						registerResultFormulae(call, partialStateSpace.getModelCheckingResultFormulae());
					}
	            }				

				int newNumberOfFinalsStates = partialStateSpace.unfinishedStateSpace().getFinalStateIds().size();	
				contractChanged = newNumberOfFinalsStates > currentNumberOfFinalStates;
			}
			
			if (contractChanged) {
				notifyDependencies(call);
			}
		}
	}

	protected void notifyDependencies(ProcedureCall call) {

		Set<OnTheFlyPartialStateSpace> dependencies = callingDependencies.getOrDefault(call, Collections.emptySet());
		remainingPartialStateSpaces.addAll(dependencies);
		
		Set<Node> continueFormulae = callToReturnFormulae.get(call);
		
		// map return formulae and partial state spaces
		for (PartialStateSpace dependency : dependencies) {
			if (!partialStateSpaceToContinueFormulae.containsKey(dependency)) {
				partialStateSpaceToContinueFormulae.put(dependency, continueFormulae);
			} else {				
				partialStateSpaceToContinueFormulae.get(dependency).addAll(continueFormulae);
			}
		}
	}
	
	protected void abortDependingProofStructures(ProcedureCall call) {
		
		Set<OnTheFlyPartialStateSpace> dependencies = callingDependencies.getOrDefault(call, Collections.emptySet());
		for (PartialStateSpace partialStateSpace : dependencies) {

			ProcedureCall dependenedCall = stateSpaceToAnalyzedCall.get( partialStateSpace.unfinishedStateSpace() );

			OnTheFlyProofStructure proofStructure = callToProofStructure.get(dependenedCall);
			proofStructure.abort();			
		}
	}
}
