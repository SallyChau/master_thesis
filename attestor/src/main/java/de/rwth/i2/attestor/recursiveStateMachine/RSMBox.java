package de.rwth.i2.attestor.recursiveStateMachine;

import java.util.Set;

public class RSMBox {
	
	// component to which the box belongs to
	private ComponentStateMachine component;
	
	// component that is called/ entered by this box
	private ComponentStateMachine calledComponent;
	
	// program location of box (PC of invoke statement)
	private int programCounter;
	
	// return program location of box
	private Set<Integer> successors;
	
	public RSMBox(ComponentStateMachine component, ComponentStateMachine calledComponent, int programCounter, Set<Integer> successors) {
		this.component = component;
		this.calledComponent = calledComponent;
		this.programCounter = programCounter;
		this.successors = successors;
	}
	
	public ComponentStateMachine getComponent() {
		return component;
	}
	
	public int getProgramCounter() {
		return programCounter;
	}
	
	public ComponentStateMachine getCalledComponent() {
		return calledComponent;
	}
	
	public String toString() {
		return "Box from " + component.getSignature() + " calls component " + calledComponent.getSignature();
	}
}