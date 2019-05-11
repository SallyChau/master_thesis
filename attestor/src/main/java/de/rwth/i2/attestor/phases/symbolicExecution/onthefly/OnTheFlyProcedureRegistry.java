package de.rwth.i2.attestor.phases.symbolicExecution.onthefly;

import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.FailureTrace;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.interproceduralAnalysis.ModelCheckingInterproceduralAnalysis;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.OnTheFlyStateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureRegistry;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class OnTheFlyProcedureRegistry implements ProcedureRegistry {
	
	private final ModelCheckingInterproceduralAnalysis analysis;
    private final OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory;

    public OnTheFlyProcedureRegistry(ModelCheckingInterproceduralAnalysis analysis,
                                     OnTheFlyStateSpaceGeneratorFactory stateSpaceGeneratorFactory) {

        this.analysis = analysis;
        this.stateSpaceGeneratorFactory = stateSpaceGeneratorFactory;
    }

	@Override	
	public ProcedureCall getProcedureCall(Method method, HeapConfiguration initialHeap, ScopedHeapHierarchy scopeHierarchy) {

		return new OnTheFlyProcedureCall(method, initialHeap, scopeHierarchy, stateSpaceGeneratorFactory, this);
	}

	@Override
	public void registerProcedure(ProcedureCall call) {
		
		analysis.registerProcedureCall(call);		
	}

	@Override
	public void registerDependency(ProgramState callingState, ProcedureCall call) {
		
		OnTheFlyPartialStateSpace partialStateSpace = new OnTheFlyPartialStateSpace(callingState, stateSpaceGeneratorFactory);
        analysis.registerDependency(call, partialStateSpace);
	}

	@Override
	public void registerStateSpace(ProcedureCall call, StateSpace generatedStateSpace) {
		
		analysis.registerStateSpace(call, generatedStateSpace);				
	}

	public void registerProofStructure(ProcedureCall call, OnTheFlyProofStructure proofStructure) {
		
		analysis.registerProofStructure(call, proofStructure);				
	}

	public void registerFormulae(ProcedureCall call, Set<Node> formulae) {
		
		analysis.registerFormulae(call, formulae);				
	}

	public void registerReturnFormulae(ProcedureCall call, Set<Node> returnFormulae) {
		
		analysis.registerReturnFormulae(call, returnFormulae);				
	}

	public void addFailureTrace(FailureTrace failureTrace) {
		
		analysis.addFailureTrace(failureTrace);				
	}

	public void registerMethodToSkip(Method method) {
		
		analysis.registerMethodToSkip(method);		
	}

	public Set<Method> getMethodsToSkip() {
		
		return analysis.getMethodsToSkip();
	}
}
