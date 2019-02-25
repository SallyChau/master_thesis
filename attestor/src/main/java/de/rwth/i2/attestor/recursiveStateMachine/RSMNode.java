package de.rwth.i2.attestor.recursiveStateMachine;

import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;

public class RSMNode {
	
	private SemanticsCommand statement;
	
	// program location of node within the component
	private int programCounter;

	private boolean isEntryNode;
	
	private boolean isExitNode;
	
	private RSMBox callingBox = null;
	
	public boolean visited = false;
	
	private ComponentStateMachine component;
	
	public RSMNode(ComponentStateMachine component, int programCounter, SemanticsCommand statement, RSMBox callingBox, boolean isEntryNode, boolean isExitNode) {

		this.component = component;
		this.programCounter = programCounter;
		this.statement = statement;
		this.callingBox = callingBox;
		this.isEntryNode = isEntryNode;
		this.isExitNode = isExitNode;
	}
	
	public boolean isEntryNode() {
		return isEntryNode;
	}
	
	public boolean isExitNode() {
		return isExitNode;
	}
	
	public int getProgramCounter() {
		return programCounter;
	}
	
	public SemanticsCommand getStatement() {
		return statement;
	}
	
	public RSMBox getCallingBox() {
		return callingBox;
	}
	
	public ComponentStateMachine getComponent() {
		return component;
	}
}
