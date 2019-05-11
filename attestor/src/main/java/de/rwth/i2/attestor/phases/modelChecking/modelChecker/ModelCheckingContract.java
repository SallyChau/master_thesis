package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.hierarchical.HierarchicalFailureTrace;

public class ModelCheckingContract {
	
	private HeapConfiguration input;
	private Set<Node> inputFormulae;
	private Set<Node> resultFormulae;
	private ModelCheckingResult mcResult;
	private HierarchicalFailureTrace hierarchicalFailureTrace;
	private FailureTrace failureTrace;
	
	public ModelCheckingContract(HeapConfiguration input, Set<Node> inputFormulae, Set<Node> outputFormulae, ModelCheckingResult mcResult, HierarchicalFailureTrace failureTrace) {

		this.input = input;
		this.inputFormulae = inputFormulae;
		this.resultFormulae = outputFormulae;
		this.mcResult = mcResult;
		this.hierarchicalFailureTrace = failureTrace;
		this.failureTrace = null;
	}
	
	// TODO adapt to normal failure trace
	public ModelCheckingContract(HeapConfiguration input, Set<Node> inputFormulae, Set<Node> outputFormulae, ModelCheckingResult mcResult, FailureTrace failureTrace) {

		this.input = input;
		this.inputFormulae = inputFormulae;
		this.resultFormulae = outputFormulae;
		this.mcResult = mcResult;
		this.hierarchicalFailureTrace = null;
		this.failureTrace = failureTrace;
	}
	
	public HeapConfiguration getInputHeap() {
		
		return input;
	}
	
	public Set<Node> getInputFormulae() {
		
		return inputFormulae;
	}
	
	public Set<Node> getResultFormulae() {
		
		return resultFormulae;
	}
	
	public ModelCheckingResult getModelCheckingResult() {
	
		return mcResult;
	}
	
	public boolean modelCheckingIsSuccessful() {
		
		return mcResult == ModelCheckingResult.SATISFIED;
	}
	
	public HierarchicalFailureTrace getHierarchicalFailureTrace() {
		
		return hierarchicalFailureTrace;
	}
	
	public FailureTrace getFailureTrace() {
		
		return failureTrace;
	}
	
	@Override
	public String toString() {
		
		return "Heap: " + input.toString() + "\n" + "Input: " + inputFormulae + "\n" + "Output: " + resultFormulae;
	}
}
