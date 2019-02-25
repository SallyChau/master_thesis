package de.rwth.i2.attestor.recursiveStateMachine;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;

public class ComponentStateMachine {
	
	// program states, maps program locations of component to nodes of statement
	private final Map<Integer, RSMNode> nodes = new HashMap<>();
	
	// one box for each procedure call, maps box with signature of called method
	private final Map<String, RSMBox> boxes = new HashMap<>();
	
	private Method method;
	private String signature;
	
	public ComponentStateMachine(Method method) {

		this.method = method;
		this.signature = method.getSignature();
	}
	
	public RSMNode getOrCreateNode(int programCounter, SemanticsCommand statement, RSMBox callingBox, boolean isEntryNode, boolean isExitNode) {

		if (nodes.containsKey(programCounter)) {
            return nodes.get(programCounter);
        } else {
            RSMNode result = new RSMNode(this, programCounter, statement, callingBox, isEntryNode, isExitNode);
            nodes.put(programCounter, result);            
            return result;
        }		
	}
	
	public RSMNode getNode(int programCounter) {
		if (nodes.containsKey(programCounter)) {
            return nodes.get(programCounter);
        } else {
        	return null;
        }
	}
	
	public RSMBox getOrCreateBox(ComponentStateMachine calledComponent, int pc, Set<Integer> successorPCs) {
		
		if (boxes.containsKey(calledComponent.signature)) {
            return boxes.get(calledComponent.signature);
        } else {
            RSMBox result = new RSMBox(this, calledComponent, pc, successorPCs);
            boxes.put(calledComponent.signature, result);
            return result;
        }
	}
	
	public Collection<RSMNode> getNodes() {
		return nodes.values();
	}
	
	public Collection<RSMBox> getBoxes() {
		return boxes.values();
	}
	
	public Set<RSMNode> getEntryNodes() {

		Set<RSMNode> entryNodes = new HashSet<>();
		
		for (RSMNode node : nodes.values()) {
			if (node.isEntryNode()) {
				entryNodes.add(node);
			}
		}
		
		return entryNodes;
	}
	
	public Set<RSMNode> getExitNodes() {

		Set<RSMNode> exitNodes = new HashSet<>();
		
		for (RSMNode node : nodes.values()) {
			if (node.isExitNode()) {
				exitNodes.add(node);
			}
		}
		
		return exitNodes;
	}
	
	public String getSignature() {
		return signature;
	}
	
	public Method getMethod() {
		return this.method;
	}
	
	@Override
	public String toString() {
		
		String result = "CSM for method " + signature + "\n";
		result += "==============================================================================\n";
		result += "Boxes to components: \n";
		for (RSMBox box : boxes.values()) {
			result += box.getCalledComponent().getSignature() + "\n";
		}
		
		return result;
	}
}
