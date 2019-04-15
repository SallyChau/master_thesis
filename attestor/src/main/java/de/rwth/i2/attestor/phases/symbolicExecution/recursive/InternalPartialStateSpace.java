package de.rwth.i2.attestor.phases.symbolicExecution.recursive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.phases.modelChecking.onthefly.OnTheFlyProofStructure;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ModelCheckingContract;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalContract;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.StateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.PartialStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerator;

public class InternalPartialStateSpace implements PartialStateSpace {

    private ProgramState stateToContinue;
    private StateSpaceGeneratorFactory stateSpaceGeneratorFactory;
    StateSpace partialStateSpace;

    public InternalPartialStateSpace(ProgramState callingState,
                                     StateSpaceGeneratorFactory stateSpaceGeneratorFactory) {

        this.stateToContinue = callingState;
        this.partialStateSpace = callingState.getContainingStateSpace();
        this.stateSpaceGeneratorFactory = stateSpaceGeneratorFactory;
    }

    @Override
    public void continueExecution(ProcedureCall call) {

        try {
        	
        	System.out.println("Continuing execution for " + call.getMethod().getSignature() + " (" + call + ")");

            Method method = call.getMethod();
            
            ProgramState preconditionState = call.getInput();

            if(partialStateSpace.containsAbortedStates()) {
                return;
            }
            
            stateToContinue.flagAsContinueState();

            StateSpace stateSpace = stateSpaceGeneratorFactory.create(
                    call.getMethod().getBody(),
                    stateToContinue,
                    partialStateSpace
            ).generate();
            
            stateToContinue.unflagContinueState();

            List<HeapConfiguration> finalHeaps = new ArrayList<>();
            stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()) );
            Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);
            method.addContract(contract);
        } catch (StateSpaceGenerationAbortedException e) {
            throw new IllegalStateException("Failed to continue state space execution.");
        }
    }
    
    @Override
    public void continueExecutionOnTheFly(ProcedureCall call, Set<Node> formulae, OnTheFlyProofStructure proofStructure) {

        try {

            Method method = call.getMethod();
            ProgramState preconditionState = call.getInput();
            
            System.out.println("InternalPartialStateSpace: Continuing execution for " + call.getMethod().getSignature() + " (" + call + ")");

            if (partialStateSpace.containsAbortedStates()) {
                return;
            }
            
            stateToContinue.flagAsContinueState();
            
            StateSpaceGenerator stateSpaceGenerator = stateSpaceGeneratorFactory.create(
                    call.getMethod().getBody(),
                    stateToContinue,
                    partialStateSpace,
                    proofStructure,
                    formulae
            );

            StateSpace stateSpace = stateSpaceGenerator.generateOnTheFly();
            
            stateToContinue.unflagContinueState();

            List<HeapConfiguration> finalHeaps = new ArrayList<>();
            stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()) );
            Set<Node> resultFormulae = stateSpaceGenerator.getResultFormulae();
            
            if (proofStructure.isSuccessful() && resultFormulae == null) {

                System.err.println("InternalPartialStateSpace: Proofstructure for method: " + method.getName() + " was successful");
                LTLFormula ltlFormula = new LTLFormula("true");
                ltlFormula.toPNF();
                resultFormulae = new HashSet<>();
                resultFormulae.add(ltlFormula.getASTRoot().getPLtlform());
            } else if (!proofStructure.isSuccessful()) {
            	
            	System.out.println("InternalPartialStateSpace: Proofstructure for method: " + method.getName() + " was unsuccessful");  
            }
            
            Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);

            ModelCheckingResult mcResult = proofStructure.isSuccessful() ? ModelCheckingResult.SATISFIED : ModelCheckingResult.UNSATISFIED;
            ModelCheckingContract mcContract = new ModelCheckingContract(preconditionState.getHeap(), 
            															 formulae, 
            															 resultFormulae, 
            															 mcResult,
            															 proofStructure.getFailureTrace());
            Collection<ModelCheckingContract> mcContracts = new ArrayList<>();
            mcContracts.add(mcContract);
            contract.addModelCheckingContracts(mcContracts);            
            method.addContract(contract);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to continue state space execution.");
        }
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((partialStateSpace == null) ? 0 : partialStateSpace.hashCode());
		result = prime * result + ((stateToContinue == null) ? 0 : stateToContinue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InternalPartialStateSpace other = (InternalPartialStateSpace) obj;
		if (partialStateSpace == null) {
			if (other.partialStateSpace != null)
				return false;
		} else if (!partialStateSpace.equals(other.partialStateSpace))
			return false;
		if (stateToContinue == null) {
			if (other.stateToContinue != null)
				return false;
		} else if (!stateToContinue.equals(other.stateToContinue))
			return false;
		return true;
	}

	@Override
	public StateSpace unfinishedStateSpace() {
		return this.partialStateSpace;
	}
}
