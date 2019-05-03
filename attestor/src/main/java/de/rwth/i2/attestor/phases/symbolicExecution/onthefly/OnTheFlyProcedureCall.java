package de.rwth.i2.attestor.phases.symbolicExecution.onthefly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.lexer.LexerException;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.generated.parser.ParserException;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.ModelCheckingContract;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.modelChecking.OnTheFlyProofStructure;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.InternalContract;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.OnTheFlyStateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.OnTheFlyStateSpaceGenerator;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;

/**
 * 
 * @author chau
 *
 */
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

    	// if method is not contained in list of methods to be skipped, else generate state space, but do not model check
    	// output formulae = input formulae for registry
        try {        	
        	OnTheFlyStateSpaceGenerator stateSpaceGenerator = factory.create(method.getBody(), initialState, scopeHierarchy, modelCheckingFormulae);
        	
        	System.out.println("InternalProcedureCall: Generate state space for method " + method.getSignature() + " and formulae " + modelCheckingFormulae);
        	
        	if (methodsToSkip.contains(method)) {
        		System.err.println("InternalProcedureCall: SKIPPING MC of method " + method.getSignature() + " and formulae " + modelCheckingFormulae);
        		return executeWithoutCheck(stateSpaceGenerator, modelCheckingFormulae);
        	} else {
        		System.err.println("InternalProcedureCall: Generate state space for method " + method.getSignature() + " and formulae " + modelCheckingFormulae);
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
        System.err.println("InternalProcedureCall: Received result formulae from " + method.getSignature() + ": " + resultFormulae);
        
        if (proofStructure.isSuccessful() && resultFormulae == null) {

            System.out.println("InternalProcedureCall: Proofstructure for method: " + method.getSignature() + " was successful");
            LTLFormula ltlFormula = null;
			try {
				ltlFormula = new LTLFormula("true");
			} catch (ParserException | LexerException | IOException e) {
				e.printStackTrace();
			}
            ltlFormula.toPNF();
            resultFormulae = new HashSet<>();
            resultFormulae.add(ltlFormula.getASTRoot().getPLtlform());
        } else if (!proofStructure.isSuccessful()) {
        	
        	System.out.println("InternalProcedureCall: Proofstructure for method: " + method.getSignature() + " was unsuccessful");
        	System.out.println("InternalProcedureCall: FailureTrace: " + proofStructure.getFailureTrace(stateSpace));
        	registry.addFailureTrace(proofStructure.getFailureTrace(stateSpace));
        }
        
        List<HeapConfiguration> finalHeaps = new ArrayList<>();
        stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()));
        Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);
        
        ModelCheckingResult modelCheckingResult = proofStructure.isSuccessful() ? ModelCheckingResult.SATISFIED : ModelCheckingResult.UNSATISFIED;
        ModelCheckingContract modelCheckingContract = new ModelCheckingContract(preconditionState.getHeap(), formulae, resultFormulae, 
        		modelCheckingResult, proofStructure.getFailureTrace());
        Collection<ModelCheckingContract> modelCheckingContracts = new ArrayList<>();
        modelCheckingContracts.add(modelCheckingContract);
        contract.addModelCheckingContracts(modelCheckingContracts);
        method.addContract(contract);
        
        registry.registerStateSpace(this, stateSpace);
        registry.registerProofStructure(this, proofStructure);
        registry.registerReturnFormulae(this, resultFormulae);
        
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
        Set<Node> resultFormulae = formulae;
        System.err.println("InternalProcedureCall: Received result formulae from " + method.getSignature() + ": " + resultFormulae);
        
        List<HeapConfiguration> finalHeaps = new ArrayList<>();
        stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()) );
        Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);
        
        ModelCheckingResult modelCheckingResult = ModelCheckingResult.SATISFIED; // actually unknown, but need to be set to satisfied to return true
        ModelCheckingContract modelCheckingContract = new ModelCheckingContract(preconditionState.getHeap(), formulae, resultFormulae, 
        		modelCheckingResult, proofStructure.getFailureTrace());
        Collection<ModelCheckingContract> modelCheckingContracts = new ArrayList<>();
        modelCheckingContracts.add(modelCheckingContract);
        contract.addModelCheckingContracts(modelCheckingContracts);
        method.addContract(contract);
        
        registry.registerStateSpace(this, stateSpace);
        registry.registerProofStructure(this, proofStructure);
        registry.registerReturnFormulae(this, resultFormulae);
        
        return stateSpace;
    }

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
