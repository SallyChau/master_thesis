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
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.OnTheFlyProofStructure;
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
			System.out.println("InterproceduralAnalysis: Call added: " + procedureCall.getMethod().getSignature() + " (" + procedureCall + ")");
		} else {
			System.out.println("InterproceduralAnalysis: Call already added: " + procedureCall.getMethod().getSignature() + " (" + procedureCall + ")");
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
	
	public void registerReturnFormulae(ProcedureCall procedureCall, Set<Node> returnFormulae) {
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

		while(!remainingProcedureCalls.isEmpty() || !remainingPartialStateSpaces.isEmpty()) {
			
			OnTheFlyProcedureCall call;
			Set<Node> formulae;
			boolean contractChanged;
			if(!remainingProcedureCalls.isEmpty()) {
				// creates new state space for recursive methods
				call = (OnTheFlyProcedureCall) remainingProcedureCalls.pop();
				formulae = callToFormulae.get(call);
				call.setModelCheckingFormulae(formulae);
				
				System.out.println("ModelCheckingInterproceduralAnalysis: Handling remaining procedure calls");
				System.out.println("ModelCheckingInterproceduralAnalysis: Current call " + call.getMethod().getSignature());
				
				StateSpace stateSpace = call.execute();
				
				OnTheFlyProofStructure proofStructure = callToProofStructure.get(call);
				System.out.println("ModelCheckingInterproceduralAnalysis: Proofstructure was successful: " + proofStructure.isSuccessful());
				if (!proofStructure.isSuccessful()) {
					abortDependingProofStructures(call);
					FailureTrace ft = proofStructure.getFailureTrace(stateSpace);
					System.out.println("ModelCheckingInterproceduralAnalysis: FailureTrace (remaining PC): " + ft);
					addFailureTrace(ft);
				}				
				
				contractChanged = stateSpace.getFinalStateIds().size() > 0;
			} else {
				// continue partial state space 
				System.out.println("ModelCheckingInterproceduralAnalysis: Handling partial state spaces");
				OnTheFlyPartialStateSpace partialStateSpace = remainingPartialStateSpaces.pop();
				
				int currentNumberOfFinalStates = partialStateSpace.unfinishedStateSpace().getFinalStateIds().size();
				call = (OnTheFlyProcedureCall) stateSpaceToAnalyzedCall.get( partialStateSpace.unfinishedStateSpace() );

				System.out.println("ModelCheckingInterproceduralAnalysis: Current call " + call.getMethod().getSignature());
				
				OnTheFlyProofStructure proofStructure = callToProofStructure.get(call);
				System.out.println("ModelCheckingInterproceduralAnalysis: before: Proof Structure successful: " + proofStructure.isSuccessful());
				
				Set<Node> inputFormulae = partialStateSpaceToContinueFormulae.get(partialStateSpace);
				
				System.out.println("ModelCheckingInterproceduralAnalysis: continuing with formulae " + inputFormulae);
				
				if (!proofStructure.isSuccessful()) {
	            	// abort this structure and notify others
	            	System.out.println("ModelCheckingInterproceduralAnalysis: this proofstructure is already unsuccessful. aborting ...");
	            	abortDependingProofStructures(call);
	            	FailureTrace ft = proofStructure.getFailureTrace(partialStateSpace.unfinishedStateSpace());
					System.out.println("ModelCheckingInterproceduralAnalysis: FailureTrace (PartialSS): " + ft);
					addFailureTrace(ft);
	            } else {				
	            	Method method = call.getMethod();
	            	boolean modelCheck = !getMethodsToSkip().contains(method);
	            	partialStateSpace.setModelCheckingFormulae(inputFormulae);
	            	partialStateSpace.setProofStructure(proofStructure);
	            	partialStateSpace.modelCheck(modelCheck);
					partialStateSpace.continueExecution(call);
					
					// abort depending proof structures if current proofstructure was unsuccessful
					System.out.println("ModelCheckingInterproceduralAnalysis: after: Proof Structure successful: " + proofStructure.isSuccessful());
					if (!proofStructure.isSuccessful()) {
						abortDependingProofStructures(call);
						FailureTrace ft = proofStructure.getFailureTrace(partialStateSpace.unfinishedStateSpace());
						System.out.println("ModelCheckingInterproceduralAnalysis: FailureTrace (PartialSS2): " + ft);
						addFailureTrace(ft);
					}
	            }				

				int newNumberOfFinalsStates = partialStateSpace.unfinishedStateSpace().getFinalStateIds().size();	
				contractChanged = newNumberOfFinalsStates > currentNumberOfFinalStates;
			}
			
			if( contractChanged ) {
				notifyDependencies(call);
			}
		}
	}

	private void notifyDependencies(ProcedureCall call) {

		Set<OnTheFlyPartialStateSpace> dependencies = callingDependencies.getOrDefault(call, Collections.emptySet());
		remainingPartialStateSpaces.addAll(dependencies);
		for (OnTheFlyPartialStateSpace dependency : dependencies) {
			System.err.println("ModelCheckingInterproceduralAnalysis: Added dependency " + dependency);
		}
		
		// map return formulae and partial state spaces
		for (PartialStateSpace dependency : dependencies) {
			if (!partialStateSpaceToContinueFormulae.containsKey(dependency)) {
				partialStateSpaceToContinueFormulae.put(dependency, callToReturnFormulae.get(call));
			} else {
				partialStateSpaceToContinueFormulae.get(dependency).addAll(callToReturnFormulae.get(call));
			}
		}
	}
	
	private void abortDependingProofStructures(ProcedureCall call) {
		
		Set<OnTheFlyPartialStateSpace> dependencies = callingDependencies.getOrDefault(call, Collections.emptySet());
		for (PartialStateSpace partialStateSpace : dependencies) {

			ProcedureCall dependenedCall = stateSpaceToAnalyzedCall.get( partialStateSpace.unfinishedStateSpace() );

			System.out.println("ModelCheckingInterproceduralAnalysis: aaborting proofstructure for " + dependenedCall.getMethod().getSignature());
			OnTheFlyProofStructure proofStructure = callToProofStructure.get(dependenedCall);
			proofStructure.abort();			
		}
	}
}
