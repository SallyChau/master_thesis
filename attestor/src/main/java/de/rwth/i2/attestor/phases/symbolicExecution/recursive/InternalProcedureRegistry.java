package de.rwth.i2.attestor.phases.symbolicExecution.recursive;

import java.util.List;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.OnTheFlyProofStructure;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.StateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.InterproceduralAnalysis;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.PartialStateSpace;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureCall;
import de.rwth.i2.attestor.phases.symbolicExecution.recursive.interproceduralAnalysis.ProcedureRegistry;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

public class InternalProcedureRegistry implements ProcedureRegistry {

    private final InterproceduralAnalysis analysis;
    private final StateSpaceGeneratorFactory stateSpaceGeneratorFactory;

    public InternalProcedureRegistry(InterproceduralAnalysis analysis,
                                     StateSpaceGeneratorFactory stateSpaceGeneratorFactory) {

        this.analysis = analysis;
        this.stateSpaceGeneratorFactory = stateSpaceGeneratorFactory;
    }
    
    @Override
	public InternalProcedureCall getProcedureCall(Method method, HeapConfiguration initialHeap ) {
		return new InternalProcedureCall(method, initialHeap, stateSpaceGeneratorFactory, this);
	}

    @Override
    public void registerProcedure( ProcedureCall call ) {

        analysis.registerProcedureCall(call);
    }

    @Override
    public void registerDependency(ProgramState callingState, ProcedureCall call) {

        PartialStateSpace partialStateSpace = new InternalPartialStateSpace(callingState, stateSpaceGeneratorFactory);
        analysis.registerDependency(call, partialStateSpace);
    }

	@Override
	public void registerStateSpace(ProcedureCall call, StateSpace generatedStateSpace) {
		analysis.registerStateSpace(call, generatedStateSpace);		
	}

	@Override
	public void registerProofStructure(ProcedureCall call, OnTheFlyProofStructure proofStructure) {
		analysis.registerProofStructure(call, proofStructure);		
	}
	
	@Override
	public void registerFormulae(ProcedureCall call, List<Node> formulae) {
		analysis.registerFormulae(call, formulae);		
	}
	
	@Override
	public void registerReturnFormulae(ProcedureCall call, List<Node> returnFormulae) {
		analysis.registerReturnFormulae(call, returnFormulae);		
	}
}
