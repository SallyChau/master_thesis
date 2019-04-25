package de.rwth.i2.attestor.phases.symbolicExecution.onthefly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.ModelCheckingContract;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.OnTheFlyProofStructure;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalContract;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.OnTheFlyStateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.PartialStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.OnTheFlyStateSpaceGenerator;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class OnTheFlyPartialStateSpace implements PartialStateSpace {
	
	private ProgramState stateToContinue;
    private OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory;
    StateSpace partialStateSpace;
    
    private Set<Node> modelCheckingFormulae;
    private OnTheFlyProofStructure proofStructure;
    private boolean modelCheck;
    
    

    public OnTheFlyPartialStateSpace(ProgramState callingState, OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory) {

        this.stateToContinue = callingState;
        this.partialStateSpace = callingState.getContainingStateSpace();
        this.stateSpaceGeneratorFactory = stateSpaceGeneratorFactory;
        
        this.modelCheckingFormulae = new HashSet<>();
        this.proofStructure = new OnTheFlyProofStructure();
        this.modelCheck = false;
    }
    
    

    public void setModelCheckingFormulae(Set<Node> formulae) {
    	
    	this.modelCheckingFormulae = formulae;
    }
    
	public void setProofStructure(OnTheFlyProofStructure proofStructure) {
	    	
		this.proofStructure = proofStructure;
    }
	
	public void modelCheck(boolean modelCheck) {
		
		this.modelCheck = modelCheck;
	}
    
    @Override
    public void continueExecution(ProcedureCall call) {

        try {

            Method method = call.getMethod();
            ProgramState preconditionState = call.getInput();
            
            System.out.println("InternalPartialStateSpace: Continuing execution for " + call.getMethod().getSignature() + " (" + call + ")");

            if (partialStateSpace.containsAbortedStates()) {
                return;
            }
            
            stateToContinue.flagAsContinueState();
            
            OnTheFlyStateSpaceGenerator stateSpaceGenerator = stateSpaceGeneratorFactory.create(
                    call.getMethod().getBody(),
                    stateToContinue,
                    partialStateSpace,
                    proofStructure,
                    modelCheckingFormulae
            );

            StateSpace stateSpace = null;
            Set<Node> modelCheckingResultFormulae;
            ModelCheckingResult modelCheckingResult = null;
            
            if (modelCheck) {
            	stateSpace = stateSpaceGenerator.generateAndCheck();
            	
            	modelCheckingResultFormulae = stateSpaceGenerator.getResultFormulae();
                
                if (proofStructure.isSuccessful() && modelCheckingResultFormulae == null) {

                    System.err.println("InternalPartialStateSpace: Proofstructure for method: " + method.getName() + " was successful");
                    LTLFormula ltlFormula = new LTLFormula("true");
                    ltlFormula.toPNF();
                    modelCheckingResultFormulae = new HashSet<>();
                    modelCheckingResultFormulae.add(ltlFormula.getASTRoot().getPLtlform());
                } else if (!proofStructure.isSuccessful()) {
                	
                	System.out.println("InternalPartialStateSpace: Proofstructure for method: " + method.getName() + " was unsuccessful");  
                }
                
                modelCheckingResult = proofStructure.isSuccessful() ? ModelCheckingResult.SATISFIED : ModelCheckingResult.UNSATISFIED;
            } else {
            	System.err.println("InternalPartialStateSpace: SKIPPING MC for method: " + method.getName());
            	stateSpace = stateSpaceGenerator.generate();
            	
            	modelCheckingResultFormulae = modelCheckingFormulae;
            	modelCheckingResult = ModelCheckingResult.SATISFIED;
            }
            
            stateToContinue.unflagContinueState();

            List<HeapConfiguration> finalHeaps = new ArrayList<>();
            stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()) );            
            
            Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);

            
            ModelCheckingContract modelCheckingContract = new ModelCheckingContract(
            		preconditionState.getHeap(), 
            		modelCheckingFormulae, 
					modelCheckingResultFormulae, 
					modelCheckingResult,
					proofStructure.getFailureTrace());
            Collection<ModelCheckingContract> modelCheckingContracts = new ArrayList<>();
            modelCheckingContracts.add(modelCheckingContract);
            contract.addModelCheckingContracts(modelCheckingContracts);            
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
		OnTheFlyPartialStateSpace other = (OnTheFlyPartialStateSpace) obj;
		if (partialStateSpace == null) {
			if (other.partialStateSpace != null)
				return false;
		} else if (!partialStateSpace.equals(other.partialStateSpace)) {
			return false;
		}
		if (stateToContinue == null) {
			if (other.stateToContinue != null)
				return false;
		} else if (!stateToContinue.equals(other.stateToContinue)) {
			return false;
		}
		return true;
	}

	@Override
	public StateSpace unfinishedStateSpace() {
		return this.partialStateSpace;
	}
}
