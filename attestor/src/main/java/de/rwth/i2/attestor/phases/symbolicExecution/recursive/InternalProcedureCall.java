package de.rwth.i2.attestor.phases.symbolicExecution.recursive;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.modelChecking.onthefly.OnTheFlyProofStructure;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalContract;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.StateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureRegistry;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerator;

public class InternalProcedureCall extends SceneObject implements ProcedureCall {

	private StateSpaceGeneratorFactory factory;
	private ProcedureRegistry registry;
	
    private Method method;
    private ProgramState preconditionState;

    
    public InternalProcedureCall( Method method, HeapConfiguration precondition, 
    							  StateSpaceGeneratorFactory factory, ProcedureRegistry registry ) {

    	super( factory );//as SceneObject
    	this.registry = registry; 
    	
        this.method = method;
        this.preconditionState = scene().createProgramState(precondition);
        this.factory = factory;
    }



    @Override
    public StateSpace execute() {
    	
    	ProgramState initialState = preconditionState.clone();

        try {
            StateSpace stateSpace = factory.create( method.getBody(), initialState ).generate();

            List<HeapConfiguration> finalHeaps = new ArrayList<>();
            stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()) );
            Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);
            method.addContract(contract);
            
            if (finalHeaps.isEmpty()) {
            	System.err.println("Internal Partial State Space: Final heap is empty ");
            }
            
            registry.registerStateSpace( this, stateSpace );
            
            return stateSpace;
        } catch (StateSpaceGenerationAbortedException e) {
            throw new IllegalStateException("Procedure call execution failed.");
        }
    }
    
    @Override
	public StateSpace executeOnTheFly(List<Node> formulae) {
    	
    	ProgramState initialState = preconditionState.clone();

        try {        	
        	StateSpaceGenerator stateSpaceGenerator = factory.create( method.getBody(), initialState, formulae );
        	
        	System.out.println("InternalProcedureCall: Generate state space for method " + method.getName() + " and formulae " + formulae);
            StateSpace stateSpace = stateSpaceGenerator.generateOnTheFly();
            OnTheFlyProofStructure proofStructure = stateSpaceGenerator.getProofStructure();    
            List<Node> resultFormulae = stateSpaceGenerator.getResultFormulae();
            
            if (proofStructure.isSuccessful() && resultFormulae == null) {

                System.out.println("Proofstructure for method: " + method.getName() + " was successful");
                LTLFormula ltlFormula = new LTLFormula("true");
                ltlFormula.toPNF();
                resultFormulae = new LinkedList<>();
                resultFormulae.add(ltlFormula.getASTRoot().getPLtlform());
            } else if (!proofStructure.isSuccessful()) {
            	
            	System.out.println("Proofstructure for method: " + method.getName() + " was unsuccessful");
                // TODO abort model checking here
            }
            
            List<HeapConfiguration> finalHeaps = new ArrayList<>();
            stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()) );
            Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);
            contract.addFormulaPair(formulae, resultFormulae);
            method.addContract(contract);
            
            registry.registerStateSpace( this, stateSpace );
            registry.registerProofStructure(this, proofStructure);
            registry.registerReturnFormulae(this, resultFormulae);
            
            return stateSpace;
        } catch (Exception e) {
            throw new IllegalStateException("Procedure call execution failed.");
        } 
    }

    @Override
    public Method getMethod() {

        return method;
    }

    @Override
    public ProgramState getInput() {

        return preconditionState;
    }

    @Override
    public int hashCode() {

        if(preconditionState == null) {
            return Objects.hashCode(method);
        } else {
            return Objects.hash(method, preconditionState.getHeap());
        }
    }

    @Override
    public boolean equals(Object otherOject) {

        if(this == otherOject) {
            return true;
        }
        if(otherOject == null) {
            return false;
        }
        if(otherOject.getClass() != InternalProcedureCall.class) {
            return false;
        }
        InternalProcedureCall call = (InternalProcedureCall) otherOject;
        return method.equals(call.method) &&
                preconditionState.equals(call.preconditionState);
    }
    
    @Override
    public String toString() {
    	return method.getName();
    }
}
