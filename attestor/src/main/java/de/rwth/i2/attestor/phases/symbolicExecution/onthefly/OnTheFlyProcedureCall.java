package de.rwth.i2.attestor.phases.symbolicExecution.onthefly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingContract;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalContract;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.OnTheFlyStateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.OnTheFlyStateSpaceGenerator;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;

public class OnTheFlyProcedureCall extends SceneObject implements ProcedureCall {
	
	private OnTheFlyProcedureRegistry registry;
	private OnTheFlyStateSpaceGeneratorFactory factory;	
	
    private Method method;
    private ProgramState preconditionState;
    
    private Set<Node> modelCheckingFormulae;
    private ScopedHeapHierarchy scopeHierarchy;
	
    
    
    public OnTheFlyProcedureCall(Method method, HeapConfiguration precondition, ScopedHeapHierarchy hierarchicalScopedHeap, 
    		OnTheFlyStateSpaceGeneratorFactory factory, OnTheFlyProcedureRegistry registry) {

		super(factory); //as SceneObject
		this.registry = registry; 
		this.factory = factory;
		
		this.method = method;
		this.preconditionState = scene().createProgramState(precondition);
		
		this.modelCheckingFormulae = new HashSet<>();
		this.scopeHierarchy = hierarchicalScopedHeap;
	}
	 
	public OnTheFlyProcedureCall(Method method, HeapConfiguration precondition, OnTheFlyStateSpaceGeneratorFactory factory, 
    		OnTheFlyProcedureRegistry registry) {

		this(method, precondition, new ScopedHeapHierarchy(), factory, registry);
	}
	
	
	
	@Override
    public StateSpace execute() {
    	
    	ProgramState initialState = preconditionState.clone();    	
    	Set<Method> methodsToSkip = registry.getMethodsToSkip();
    	
        try {        	
        	OnTheFlyStateSpaceGenerator stateSpaceGenerator = factory.create(method.getBody(), initialState, scopeHierarchy, modelCheckingFormulae);
    
        	if (methodsToSkip.contains(method)) {
        		return executeWithoutCheck(stateSpaceGenerator, modelCheckingFormulae);
        	} else {
        		return executeAndCheck(stateSpaceGenerator, modelCheckingFormulae);
        	}
        } catch (Exception e) {
        	
            throw new IllegalStateException("Procedure call execution failed.");
        } 
    }
    
	/**
	 * Executes the procedure call and model check formulae.
	 * @param stateSpaceGenerator
	 * @param formulae The formulae to be model checked.
	 * @return The stateSpace resulting from executing the procedure call.
	 * @throws StateSpaceGenerationAbortedException
	 */
    private StateSpace executeAndCheck(OnTheFlyStateSpaceGenerator stateSpaceGenerator, Set<Node> formulae) throws StateSpaceGenerationAbortedException {
    	
    	StateSpace stateSpace = stateSpaceGenerator.generateAndCheck();
    	
        OnTheFlyProofStructure proofStructure = stateSpaceGenerator.getProofStructure();    
        Set<Node> resultFormulae = stateSpaceGenerator.getResultFormulae();        
                
        if (!proofStructure.isSuccessful()) {
        	
        	registry.addFailureTrace(proofStructure.getFailureTrace(stateSpace));
        }        
        
        // set contract
        List<HeapConfiguration> finalHeaps = new ArrayList<>();
        stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()));
        Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);
        
        // add model checking contract
        Collection<ModelCheckingContract> modelCheckingContracts = new ArrayList<>();
        ModelCheckingContract modelCheckingContract = 
        		new ModelCheckingContract(preconditionState.getHeap(), 
        				formulae, resultFormulae, proofStructure.isSuccessful(), 
        				proofStructure.getFailureTrace(stateSpace));        
        modelCheckingContracts.add(modelCheckingContract);
        
        contract.addModelCheckingContracts(modelCheckingContracts);
        method.addContract(contract);
        
        registry.registerStateSpace(this, stateSpace);
        registry.registerProofStructure(this, proofStructure);
        registry.registerResultFormulae(this, resultFormulae);
        
        return stateSpace;
    }
    
    /**
     * Executes the procedure call without model checking.
     * @param stateSpaceGenerator
     * @param formulae The formulae to be model checked, but is skipped for this procedure. Formulae is transferred to return state.
     * @return The stateSpace resulting from executing the procedure call.
     * @throws StateSpaceGenerationAbortedException
     */
    private StateSpace executeWithoutCheck(OnTheFlyStateSpaceGenerator stateSpaceGenerator, Set<Node> formulae) throws StateSpaceGenerationAbortedException {
    	
    	StateSpace stateSpace = stateSpaceGenerator.generate();
    	
        OnTheFlyProofStructure proofStructure = stateSpaceGenerator.getProofStructure(); // proofstructure is empty
        
        // set contract
        List<HeapConfiguration> finalHeaps = new ArrayList<>();
        stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()) );
        Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);
        
        // add model checking contract
        Collection<ModelCheckingContract> modelCheckingContracts = new ArrayList<>();
        ModelCheckingContract modelCheckingContract = 
        		new ModelCheckingContract(preconditionState.getHeap(), 
        				formulae, formulae, true, null);        
        modelCheckingContracts.add(modelCheckingContract);
        
        contract.addModelCheckingContracts(modelCheckingContracts);
        method.addContract(contract);
        
        registry.registerStateSpace(this, stateSpace);
        registry.registerProofStructure(this, proofStructure);
        registry.registerResultFormulae(this, formulae);
        
        return stateSpace;
    }

    @Override
    public ScopedHeapHierarchy getScopeHierarchy() {
		
		return scopeHierarchy;
	}

	@Override
    public Method getMethod() {

        return method;
    }

    @Override
    public ProgramState getInput() {

        return preconditionState;
    }
    
    /**
	 * Set formulae to be checked during Contract computation for this call.
	 * @param formulae
	 */
	public void setModelCheckingFormulae(Set<Node> formulae) {
		
		this.modelCheckingFormulae = formulae;
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
        if(otherOject.getClass() != OnTheFlyProcedureCall.class) {
            return false;
        }
        OnTheFlyProcedureCall call = (OnTheFlyProcedureCall) otherOject;
        return method.equals(call.method) &&
                preconditionState.equals(call.preconditionState);
    }
}
