package de.rwth.i2.attestor.phases.preprocessing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.AbstractPhase;
import de.rwth.i2.attestor.main.scene.ElementNotPresentException;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.communication.InputSettings;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.StateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.transformers.InputSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.InputTransformer;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.recursiveStateMachine.ComponentStateMachine;
import de.rwth.i2.attestor.recursiveStateMachine.RSMBox;
import de.rwth.i2.attestor.recursiveStateMachine.RecursiveStateMachine;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.AssignInvoke;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.InvokeStmt;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.ReturnValueStmt;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.ReturnVoidStmt;
import de.rwth.i2.attestor.stateSpaceGeneration.Program;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;

public class RSMGenerationPhase extends AbstractPhase {
	
	private RecursiveStateMachine rsm;
    private Method mainMethod;
    private LinkedList<Method> translatedMethods;
    private StateSpaceGeneratorFactory stateSpaceGeneratorFactory;
    private List<ProgramState> initialStates;

	public RSMGenerationPhase(Scene scene) {
		super(scene);
		translatedMethods = new LinkedList<>();
		
		stateSpaceGeneratorFactory = new StateSpaceGeneratorFactory(scene);
	}
	
	@Override
    public String getName() {

        return "RSM Generation";
    }

    @Override
    public void executePhase() {
    	
    	loadMainMethod();
    
    	rsm = new RecursiveStateMachine(mainMethod);
    	
    	translateMethod(mainMethod);
    	System.out.println(rsm.toString());
    	// translate each method to a component state machine
//    	for (Method method : scene().getRegisteredMethods()) {
//    		translateMethod(method);   		
//    	}	
    	
    	loadInitialStates();
    	
    	// State space generation (separate into other phase later)
//    	try {
//			StateSpace mainStateSpace = stateSpaceGeneratorFactory.create(mainMethod.getBody(), initialStates).generate();
//		} catch (StateSpaceGenerationAbortedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
    }
    
    /**
     * Translate method to a component state machine by translating each statement.
     * 
     * @param method
     */
    private void translateMethod(Method method) {
    	
    	if (!translatedMethods.contains(method)) {
	    	System.out.println("Translating method " + method.getName());
	    	
	    	// get or create CSM to method
			ComponentStateMachine csm = rsm.getOrCreateComponentStateMachine(method);
			
			translatedMethods.add(method);
			
			// translate method statements to nodes and boxes
			Program methodBody = method.getBody();		
			for (int i = 0; i < methodBody.getSize(); i++) {		
				translateStatement(csm, methodBody.getStatement(i), i);		
			}
    	}		
    }
    
    /**
     * Translate statement to nodes or boxes of the according component state machine.
     * 
     * @param csm
     * @param statement
     * @param programCounter
     */
    private void translateStatement(ComponentStateMachine csm, SemanticsCommand statement, int programCounter) {
    			
    	System.out.println(programCounter + ": " + statement);
    	System.out.println(statement.getSuccessorPCs());
    	
		if (statement.getClass().equals(AssignInvoke.class) 
				|| statement.getClass().equals(InvokeStmt.class)) {
			// invoke statements
			translateCallStatement(csm, statement, programCounter);    				
		} else if (statement.getClass().equals(ReturnValueStmt.class) 
				|| statement.getClass().equals(ReturnVoidStmt.class)) {
			// return statements
			translateReturnStatement(csm, statement, programCounter);
		} else {
			// statements with normal transitions to next PC
			translateInternalStatement(csm, statement, programCounter);
		} 
    }
    
    /**
     * Create a node for an internal statement and set its successor program locations.
     * 
     * @param csm
     * @param statement
     * @param programCounter
     */
    private void translateInternalStatement(ComponentStateMachine csm, SemanticsCommand statement, int programCounter) {

    	boolean isEntryNode = false;
    	boolean isExitNode = false; 
    	if (programCounter == 0) {
    		isEntryNode = true;
    	} 
    	
    	csm.getOrCreateNode(programCounter, statement, null, isEntryNode, isExitNode);
	}

	/**
	 * Create a box in the calling CSM for invoke statements. 
     * The called component refers to the component of the method being called by the invoke statement.
     * 
	 * @param csm
	 * @param statement
	 * @param programCounter
	 */
    private void translateCallStatement(ComponentStateMachine csm, SemanticsCommand statement, int programCounter) {
        
		// get called method
		String methodName = statement.toString();
		methodName = methodName.substring(methodName.lastIndexOf(" ") + 1, methodName.indexOf("("));

		Method calledMethod = findMatchingMethod(methodName);
				
		// get CSM to called method
		ComponentStateMachine calledCSM = rsm.getOrCreateComponentStateMachine(calledMethod);

		// create box in CSM according to method call (Successor PCs of statement are return points of methods)
		RSMBox box = csm.getOrCreateBox(calledCSM, programCounter, statement.getSuccessorPCs());   		
		
    	// create node for call statement
		boolean isEntryNode = false;
    	boolean isExitNode = false; 
    	if (programCounter == 0) {
    		isEntryNode = true;
    	} 
    	
    	csm.getOrCreateNode(programCounter, statement, box, isEntryNode, isExitNode);
    	
    	// translate called method
    	translateMethod(calledMethod);
    }
    
    /**
     * Return statements are exit nodes of component state machines.
     * 
     * @param csm
     * @param statement
     * @param programCounter
     */
    private void translateReturnStatement(ComponentStateMachine csm, SemanticsCommand statement, int programCounter) {
    	// return statements = exit node
    	// have no successor PCs
    	// jump back to calling context
    	csm.getOrCreateNode(programCounter, statement, null, false, true);
    }
    
    private void loadMainMethod() {

        InputSettings inputSettings = getPhase(InputSettingsTransformer.class).getInputSettings();
        String methodName = inputSettings.getMethodName();
        try {
			mainMethod = scene().getMethodIfPresent(methodName);
		} catch (ElementNotPresentException e) {
			mainMethod = findMatchingMethod(methodName);
		}
        if(mainMethod.getBody() == null) {
            mainMethod = findMatchingMethod(methodName);
        }
    }
    
    private Method findMatchingMethod(String methodName) { 

        for(Method method : scene().getRegisteredMethods()) {
            if(methodName.equals(method.getName())) {
                logger.info("Found matching top-level method with signature: " + method.getSignature());
                return method;
            }
        }

        throw new IllegalArgumentException("Could not find top-level method '" + methodName + "'.");
    }
    
    private void loadInitialStates() {

        List<HeapConfiguration> inputs = getPhase(InputTransformer.class).getInputs();
        initialStates = new ArrayList<>(inputs.size());
        for(HeapConfiguration hc : inputs) {
        	initialStates.add(scene().createProgramState(hc));
        }
    }

	@Override
	public void logSummary() {
		
	}

	@Override
	public boolean isVerificationPhase() {

		return false;
	}

	public RecursiveStateMachine getRSM() {
		
		return rsm;
	}

}
