package de.rwth.i2.attestor.phases.symbolicExecution.onthefly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingContract;
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
    
    private Set<Node> modelCheckingResultFormulae;
    
    

    public OnTheFlyPartialStateSpace(ProgramState callingState, OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory) {

        this.stateToContinue = callingState;
        this.partialStateSpace = callingState.getContainingStateSpace();
        this.stateSpaceGeneratorFactory = stateSpaceGeneratorFactory;
        
        this.modelCheckingFormulae = new HashSet<>();
        this.modelCheckingResultFormulae = new HashSet<>();
        this.proofStructure = new OnTheFlyProofStructure();
        this.modelCheck = false;
    }
    
    

    @Override
	public void continueExecution(ProcedureCall call) {
	
	    try {
	
	    	OnTheFlyProcedureCall onTheFlyCall = (OnTheFlyProcedureCall) call;
	        Method method = onTheFlyCall.getMethod();
	        ProgramState preconditionState = onTheFlyCall.getInput();
	        	
	        if (partialStateSpace.containsAbortedStates()) {
	            return;
	        }
	        
	        stateToContinue.flagAsContinueState(); 
	        
	        OnTheFlyStateSpaceGenerator stateSpaceGenerator = stateSpaceGeneratorFactory.create(
	        		onTheFlyCall.getMethod().getBody(),
	                stateToContinue,
	                partialStateSpace,
	                onTheFlyCall.getScopeHierarchy(),
	                proofStructure,
	                modelCheckingFormulae
	        );
	
	        StateSpace stateSpace = null;
	        boolean modelCheckingResult;
	        
	        if (modelCheck) {
	        	stateSpace = stateSpaceGenerator.generateAndCheck();	        	
	        	modelCheckingResultFormulae = stateSpaceGenerator.getResultFormulae();
	            modelCheckingResult = proofStructure.isSuccessful();
	        } else {
	        	stateSpace = stateSpaceGenerator.generate();	        	
	        	modelCheckingResultFormulae = modelCheckingFormulae;
	        	modelCheckingResult = true;
	        }
	        
	        stateToContinue.unflagContinueState();
	
	        // set contract
	        List<HeapConfiguration> finalHeaps = new ArrayList<>();
	        stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()) );            
	        Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);
	        
	        // add model checking contract
	        Collection<ModelCheckingContract> modelCheckingContracts = new ArrayList<>();
	        ModelCheckingContract modelCheckingContract = 
	        		new ModelCheckingContract(preconditionState.getHeap(), 
	        				modelCheckingFormulae, modelCheckingResultFormulae, 
	        				modelCheckingResult, proofStructure.getFailureTrace(stateSpace));
	      
	        modelCheckingContracts.add(modelCheckingContract);	    
	        
	        contract.addModelCheckingContracts(modelCheckingContracts);            
	        method.addContract(contract);
	    } catch (Exception e) {
	    	
	        throw new IllegalStateException("Failed to continue state space execution.");
	    }
	}

	public void modelCheck(boolean modelCheck) {
		
		this.modelCheck = modelCheck;
	}

	public void setModelCheckingFormulae(Set<Node> formulae) {
    	
    	this.modelCheckingFormulae = formulae;
    }
	
	public Set<Node> getModelCheckingResultFormulae() {
    	
    	return this.modelCheckingResultFormulae;
    }
    
	public void setProofStructure(OnTheFlyProofStructure proofStructure) {
	    	
		this.proofStructure = proofStructure;
    }
	
	@Override
	public StateSpace unfinishedStateSpace() {
		
		return this.partialStateSpace;
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
}
