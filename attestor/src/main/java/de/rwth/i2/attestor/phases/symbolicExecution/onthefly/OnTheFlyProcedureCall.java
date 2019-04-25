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
import de.rwth.i2.attestor.procedures.ScopedHeap;
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
	
	private OnTheFlyStateSpaceGeneratorFactory factory;
	private OnTheFlyProcedureRegistry registry;
	
    private Method method;
    private ProgramState preconditionState;
    private ScopedHeap scopedHeap;
    
    private Set<Node> modelCheckingFormulae;
	
    
    
    public OnTheFlyProcedureCall(Method method, HeapConfiguration precondition, 
								 OnTheFlyStateSpaceGeneratorFactory factory, 
								 OnTheFlyProcedureRegistry registry) {

		super(factory); //as SceneObject
		this.registry = registry; 
		
		this.method = method;
		this.preconditionState = scene().createProgramState(precondition);
		this.scopedHeap = null;
		this.factory = factory;
		
		this.modelCheckingFormulae = new HashSet<>();
	}
    
	public OnTheFlyProcedureCall(Method method, HeapConfiguration precondition, 
								 ScopedHeap scopedHeap,
			  					 OnTheFlyStateSpaceGeneratorFactory factory, 
			  					 OnTheFlyProcedureRegistry registry) {

		super(factory); //as SceneObject
		this.registry = registry; 
		
		this.method = method;
		this.preconditionState = scene().createProgramState(precondition);
		this.scopedHeap = scopedHeap;
		this.factory = factory;
		
		this.modelCheckingFormulae = new HashSet<>();
	}
	
	
	
	public void setModelCheckingFormulae(Set<Node> formulae) {
		this.modelCheckingFormulae = formulae;
	}
	
	@Override
    public StateSpace execute() {
    	
    	ProgramState initialState = preconditionState.clone();
    	
    	
    	Set<Method> methodsToSkip = registry.getMethodsToSkip();

    	// if method is not contained in list of methods to be skipped, else generate state space, but do not model check
    	// output formulae = input formulae for registry
        try {        	
        	OnTheFlyStateSpaceGenerator stateSpaceGenerator = factory.create( method.getBody(), initialState, scopedHeap, modelCheckingFormulae );
        	
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
        stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()) );
        Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);
        
        ModelCheckingResult mcResult = proofStructure.isSuccessful() ? ModelCheckingResult.SATISFIED : ModelCheckingResult.UNSATISFIED;
        ModelCheckingContract mcContract = new ModelCheckingContract(preconditionState.getHeap(), formulae, resultFormulae, 
        															 mcResult, proofStructure.getFailureTrace());

        Collection<ModelCheckingContract> mcContracts = new ArrayList<>();
        mcContracts.add(mcContract);
        contract.addModelCheckingContracts(mcContracts);
        method.addContract(contract);
        
        registry.registerStateSpace(this, stateSpace);
        registry.registerProofStructure(this, proofStructure);
        registry.registerReturnFormulae(this, resultFormulae);
        
        return stateSpace;
    }
    
    private StateSpace executeWithoutCheck(OnTheFlyStateSpaceGenerator stateSpaceGenerator, Set<Node> formulae) throws StateSpaceGenerationAbortedException {
    	
    	StateSpace stateSpace = stateSpaceGenerator.generate();
        OnTheFlyProofStructure proofStructure = stateSpaceGenerator.getProofStructure(); // proofstructure should be empty
        Set<Node> resultFormulae = formulae;
        System.err.println("InternalProcedureCall: Received result formulae from " + method.getSignature() + ": " + resultFormulae);
        
        List<HeapConfiguration> finalHeaps = new ArrayList<>();
        stateSpace.getFinalStates().forEach( finalState -> finalHeaps.add(finalState.getHeap()) );
        Contract contract = new InternalContract(preconditionState.getHeap(), finalHeaps);
        
        ModelCheckingResult mcResult = ModelCheckingResult.SATISFIED; // actually unknown, but need to be set to satisfied to return true
        ModelCheckingContract mcContract = new ModelCheckingContract(preconditionState.getHeap(), formulae, resultFormulae, 
        															 mcResult, proofStructure.getFailureTrace());

        Collection<ModelCheckingContract> mcContracts = new ArrayList<>();
        mcContracts.add(mcContract);
        contract.addModelCheckingContracts(mcContracts);
        method.addContract(contract);
        
        registry.registerStateSpace( this, stateSpace );
        registry.registerProofStructure(this, proofStructure);
        registry.registerReturnFormulae(this, resultFormulae);
        
        return stateSpace;
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
        if(otherOject.getClass() != OnTheFlyProcedureCall.class) {
            return false;
        }
        OnTheFlyProcedureCall call = (OnTheFlyProcedureCall) otherOject;
        return method.equals(call.method) &&
                preconditionState.equals(call.preconditionState);
    }
}
