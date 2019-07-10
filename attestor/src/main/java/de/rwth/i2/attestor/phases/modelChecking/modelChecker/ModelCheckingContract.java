package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import java.util.HashSet;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.phases.modelChecking.hierarchical.HierarchicalFailureTrace;
import de.rwth.i2.attestor.phases.modelChecking.hierarchical.HierarchicalProofStructure;

public class ModelCheckingContract {
	
	// Input parameter
	private HeapConfiguration inputHeap;
	private Set<Node> inputFormulae;
	
	private HierarchicalProofStructure proofStructure;
	
	// Result parameter
	private Set<Node> resultFormulae;
	private boolean isSuccessful;
	private FailureTrace failureTrace;
	
	
	/**
	 * Creates a model checking contract without result parameters.
	 * @param input
	 * @param inputFormulae
	 */
	public ModelCheckingContract(HeapConfiguration input, Set<Node> inputFormulae, HierarchicalProofStructure proofStructure) {
		this.inputHeap = input;
		this.inputFormulae = inputFormulae;
		this.proofStructure = proofStructure;
	}
	
	public ModelCheckingContract(HeapConfiguration input, Set<Node> inputFormulae, Set<Node> resultFormulae,
			boolean isSuccessful, FailureTrace failureTrace) {
		this.inputHeap = input;
		this.inputFormulae = inputFormulae;
		this.resultFormulae = resultFormulae;
		this.isSuccessful = isSuccessful;
		this.failureTrace = failureTrace;
	}

	public HierarchicalProofStructure getProofStructure() {
		
		return proofStructure;
	}
	
	public void setProofStructure(HierarchicalProofStructure proofStructure) {
		
		this.proofStructure = proofStructure;
	}
	
	public HeapConfiguration getInputHeap() {
		
		return inputHeap;
	}
	
	public Set<Node> getInputFormulae() {
		
		return inputFormulae;
	}
	
	public Set<Node> getResultFormulae() {
		
		return resultFormulae;
	}
	
	public void setResultFormulae(Set<Node> formulae) {
		
		this.resultFormulae = formulae;
	}
	
	public ModelCheckingResult getModelCheckingResult() {
	
		return isModelCheckingSuccessful() ? ModelCheckingResult.SATISFIED : ModelCheckingResult.UNSATISFIED;
	}
	
	public boolean isModelCheckingSuccessful() {
		
		return isSuccessful;
	}
	
	public void setModelCheckingSuccessful(boolean successful) {
		
		this.isSuccessful = successful;
	}
	
	public boolean matches(HeapConfiguration heapConf, Set<Node> formulae) {
		
		if (heapConf.equals(inputHeap)) {
			Set<String> formulaeStrings = new HashSet<>();
			formulae.forEach((Node node) -> formulaeStrings.add(node.toString()));
			
			Set<String> inputFormulaeStrings = new HashSet<>();
			inputFormulae.forEach((Node node) -> inputFormulaeStrings.add(node.toString()));
			
			return formulaeStrings.equals(inputFormulaeStrings);
		}
			
		return false;
	}
	
	public boolean matches(Set<Node> formulae) {
		
		Set<String> formulaeStrings = new HashSet<>();
		formulae.forEach((Node node) -> formulaeStrings.add(node.toString()));
		
		Set<String> inputFormulaeStrings = new HashSet<>();
		inputFormulae.forEach((Node node) -> inputFormulaeStrings.add(node.toString()));
		
		return formulaeStrings.equals(inputFormulaeStrings);
	}
	
	public boolean isComplete() {
		
		return (resultFormulae != null);
	}
	
	public HierarchicalFailureTrace getHierarchicalFailureTrace() {
		
		return proofStructure.getHierarchicalFailureTrace();
	}
	
	public FailureTrace getFailureTrace() {
		
		return failureTrace;
	}
	
	public void setFailureTrace(FailureTrace failureTrace) {
		
		this.failureTrace = failureTrace;
	}
	
	@Override
	public String toString() {
		
		return "Heap: " + inputHeap.toString() + "\n" + "Input: " + inputFormulae + "\n" + "Output: " + resultFormulae;
	}
}