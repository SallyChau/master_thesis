package de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis;

import java.util.Collections;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.onthefly.OnTheFlyProofStructure;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.InterproceduralAnalysis;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.PartialStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class ModelCheckingInterproceduralAnalysis extends InterproceduralAnalysis {

	@Override
	public void run() {

		while(!remainingProcedureCalls.isEmpty() || !remainingPartialStateSpaces.isEmpty()) {
			
			ProcedureCall call;
			Set<Node> formulae;
			boolean contractChanged;
			if(!remainingProcedureCalls.isEmpty()) {
				// creates new state space for recursive methods
				call = remainingProcedureCalls.pop();
				formulae = callToFormulae.get(call);
				
				System.out.println("ModelCheckingInterproceduralAnalysis: Handling remaining procedure calls");
				System.out.println("ModelCheckingInterproceduralAnalysis: Current call " + call.getMethod().getSignature());
				
				StateSpace stateSpace = call.executeOnTheFly(formulae);
				
				OnTheFlyProofStructure proofStructure = callToProofStructure.get(call);
				System.err.println("ModelCheckingInterproceduralAnalysis: Proofstructure was successful: " + proofStructure.isSuccessful());
				if (!proofStructure.isSuccessful()) {
					abortDependingProofStructures(call);
					FailureTrace ft = proofStructure.getFailureTrace(stateSpace);
					System.err.println("ModelCheckingInterproceduralAnalysis: FailureTrace: " + ft);
					addFailureTrace(ft);
				}				
				
				contractChanged = stateSpace.getFinalStateIds().size() > 0;
			} else {
				// continue partial state space 
				System.out.println("ModelCheckingInterproceduralAnalysis: Handling partial state spaces");
				PartialStateSpace partialStateSpace = remainingPartialStateSpaces.pop();
				
				int currentNumberOfFinalStates = partialStateSpace.unfinishedStateSpace().getFinalStateIds().size();
				call = stateSpaceToAnalyzedCall.get( partialStateSpace.unfinishedStateSpace() );

				System.out.println("ModelCheckingInterproceduralAnalysis: Current call " + call.getMethod().getSignature());
				
				OnTheFlyProofStructure proofStructure = callToProofStructure.get(call);
				System.err.println("ModelCheckingInterproceduralAnalysis: before: Proof Structure successful: " + proofStructure.isSuccessful());
				
				Set<Node> inputFormulae = partialStateSpaceToContinueFormulae.get(partialStateSpace);
				
				System.out.println("ModelCheckingInterproceduralAnalysis: continuing with formulae " + inputFormulae);
				
				if (!proofStructure.isSuccessful()) {
	            	// abort this structure and notify others
	            	System.err.println("ModelCheckingInterproceduralAnalysis: this proofstructure is already unsuccessful. aborting ...");
	            	abortDependingProofStructures(call);
	            	FailureTrace ft = proofStructure.getFailureTrace(partialStateSpace.unfinishedStateSpace());
					System.err.println("ModelCheckingInterproceduralAnalysis: FailureTrace: " + ft);
					addFailureTrace(ft);
	            } else {				
					partialStateSpace.continueExecutionOnTheFly(call, inputFormulae, proofStructure);
					
					// abort depending proof structures if current proofstructure was unsuccessful
					System.err.println("ModelCheckingInterproceduralAnalysis: after: Proof Structure successful: " + proofStructure.isSuccessful());
					if (!proofStructure.isSuccessful()) {
						abortDependingProofStructures(call);
						FailureTrace ft = proofStructure.getFailureTrace(partialStateSpace.unfinishedStateSpace());
						System.err.println("ModelCheckingInterproceduralAnalysis: FailureTrace: " + ft);
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

		Set<PartialStateSpace> dependencies = callingDependencies.getOrDefault(call, Collections.emptySet());
		remainingPartialStateSpaces.addAll(dependencies);
		
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
		
		Set<PartialStateSpace> dependencies = callingDependencies.getOrDefault(call, Collections.emptySet());
		for (PartialStateSpace partialStateSpace : dependencies) {

			ProcedureCall dependenedCall = stateSpaceToAnalyzedCall.get( partialStateSpace.unfinishedStateSpace() );

			System.err.println("ModelCheckingInterproceduralAnalysis: aaborting proofstructure for " + dependenedCall.getMethod().getSignature());
			OnTheFlyProofStructure proofStructure = callToProofStructure.get(dependenedCall);
			proofStructure.abort();			
		}
	}
}
