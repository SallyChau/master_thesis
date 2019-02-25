package de.rwth.i2.attestor.recursiveStateMachine;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;


public class RecursiveStateMachine {

	private final Map<String, ComponentStateMachine> components = new HashMap<>();
	RSMNode currentNode;
	private final Method mainMethod;
	
	public RecursiveStateMachine(Method mainMethod) {
		this.mainMethod = mainMethod;
	}
	
	public ComponentStateMachine getOrCreateComponentStateMachine(Method method) {
		
		if(components.containsKey(method.getSignature())) {
            return components.get(method.getSignature());
        } else {
            ComponentStateMachine result = new ComponentStateMachine(method);
            components.put(method.getSignature(), result);
            return result;
        }
	}	
	
	public RSMNode getCurrentNode() {
		return currentNode;
	}
	
	public void setCurrentNode(RSMNode node) {
		this.currentNode = node;
	}
	
	public LinkedList<RSMNode> getNextNodes() {
		return getSuccessorNodes(currentNode);
	}
	
	public Collection<ComponentStateMachine> getComponentStateMachines() {
		
		return components.values();
	}
	
	public ComponentStateMachine getMainComponent() {
		
		return components.get(this.mainMethod.getSignature());
	}
	
	/**
	 * Returns number of CSM of RSM
	 * @return
	 */
	public int getSize() {
		return components.size();
	}	
	
	public LinkedList<RSMNode> getSuccessorNodes(RSMNode node) {
		
		LinkedList<RSMNode> successors = new LinkedList<>();
		ComponentStateMachine component = node.getComponent();
		
		// get statement
		SemanticsCommand currentStatement = node.getStatement();
		
		// handle call nodes
		RSMBox calledBox = node.getCallingBox();
		if (calledBox != null) {
			System.out.println("Calling " + calledBox.toString());			
			
			// enter called box
			// need to know which entry node is used (might be single entry due to construction)
			for (RSMNode entryNode : calledBox.getCalledComponent().getEntryNodes()) {
				if (entryNode != null && !entryNode.visited) { 
					// TODO might rerun if model checking requires it
					successors.add(entryNode);
				} else if (entryNode != null && entryNode.visited) {
					// only for completeness
					System.err.println(calledBox.toString() + " has been visited before!!");
				}
			}
			
		} else if (node.isExitNode()) {
			// TODO Handle return nodes
			System.out.println("Return");
			node.visited = true;
		} else {				
			// handle internal nodes: get next PCs
			// TODO change to computeSuccessors (successor state spaces) for model checking
			Set<Integer> successorPCs = currentStatement.getSuccessorPCs();
			
	        node.visited = true;
			for (int i : successorPCs) {
				RSMNode n = component.getNode(i);
				if(n != null) {
					successors.add(n);
				}
			}
		}
		
		return successors;
	}
	
	// for testing: get transition relation of RSM
	public void run() {
		System.out.println("DFS Traversing RSM ...");
	
		ComponentStateMachine mainComponent = getMainComponent();
		Set<RSMNode> entryNodes = mainComponent.getEntryNodes();
		
		for (RSMNode entryNode : entryNodes) {
			if(entryNode != null && !entryNode.visited) {
				dfs(entryNode, mainComponent);
			}
		}		
	}
	
	//TODO kinda works for static case without state space generation, 
	// need to adjust when state space is generated
	public void dfs(RSMNode node, ComponentStateMachine component) {
		System.out.print("Component " + component.getSignature() + " at PC " + node.getProgramCounter() + "\n");
		
		// get statement
		SemanticsCommand currentStatement = node.getStatement();
		
		// handle call nodes
		RSMBox calledBox = node.getCallingBox();
		if (calledBox != null) {
			System.out.println("Calling " + calledBox.toString());			
			
			// enter called box
			// need to know which entry node is used (might be single entry due to construction)
			for (RSMNode entryNode : calledBox.getCalledComponent().getEntryNodes()) {
				if (entryNode != null && !entryNode.visited) { 
					// TODO might rerun if model checking requires it
					dfs(entryNode, calledBox.getCalledComponent());
				} else if (entryNode != null && entryNode.visited) {
					// only for completeness
					System.err.println(calledBox.toString() + " has been visited before!!");
				}
			}
			
			// continue at successor statement after return from box
			Set<Integer> successors = currentStatement.getSuccessorPCs();
			
	        node.visited = true;
			for (int i : successors) {
				RSMNode n = component.getNode(i);
				if(n != null && !n.visited) {
					dfs(n, component);
				}
			}
		} else if (node.isExitNode()) {
			// Handle return nodes
			System.out.println("Return");
			node.visited = true;
		} else {				
			// handle internal nodes: get next PCs
			// TODO change to computeSuccessors (successor state spaces) for model checking
			Set<Integer> successors = currentStatement.getSuccessorPCs();
			
	        node.visited = true;
			for (int i : successors) {
				RSMNode n = component.getNode(i);
				if(n != null && !n.visited) {
					dfs(n, component);
				}
			}
		}
	}
	
	@Override
	public String toString() {
		
		String result = "Recursive State Machine \n";
		result += "# Components: " + components.size() + ", with \n";
		
		for (ComponentStateMachine csm : components.values()) {
			result += csm.toString();
			result += "\n";
		}
		
		return result;
	}
}
