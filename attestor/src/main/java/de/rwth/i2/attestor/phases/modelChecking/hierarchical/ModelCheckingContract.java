package de.rwth.i2.attestor.phases.modelChecking.hierarchical;

import java.util.List;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ModelCheckingResult;

public class ModelCheckingContract {
	
	HeapConfiguration input;
	List<Node> inputFormulae;
	List<Node> outputFormulae;
	ModelCheckingResult mcResult;
	HierarchicalFailureTrace failureTrace;
	
	public ModelCheckingContract(HeapConfiguration input, List<Node> inputFormulae, List<Node> outputFormulae, ModelCheckingResult mcResult, HierarchicalFailureTrace failureTrace) {

		this.input = input;
		this.inputFormulae = inputFormulae;
		this.outputFormulae = outputFormulae;
		this.mcResult = mcResult;
		this.failureTrace = failureTrace;
	}
	
	public HeapConfiguration getInput() {
		
		return input;
	}
	
	public List<Node> getInputFormulae() {
		
		return inputFormulae;
	}
	
	public List<Node> getOutputFormulae() {
		
		return outputFormulae;
	}
	
	public ModelCheckingResult getModelCheckingResult() {
	
		return mcResult;
	}
	
	public boolean modelCheckingSuccessful() {
		
		return mcResult == ModelCheckingResult.SATISFIED;
	}
	
	public HierarchicalFailureTrace getHierarchicalFailureTrace() {
		
		return failureTrace;
	}
	
	@Override
	public String toString() {
		
		return "Heap: " + input.toString() + "\n" + "Input: " + inputFormulae + "\n" + "Output: " + outputFormulae;
	}
}
