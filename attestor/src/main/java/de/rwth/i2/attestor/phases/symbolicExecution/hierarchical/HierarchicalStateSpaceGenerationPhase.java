package de.rwth.i2.attestor.phases.symbolicExecution.hierarchical;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.AbstractPhase;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.communication.ModelCheckingSettings;
import de.rwth.i2.attestor.phases.modelChecking.modelChecker.ProofStructure2;
import de.rwth.i2.attestor.phases.preprocessing.RSMGenerationPhase;
import de.rwth.i2.attestor.phases.symbolicExecution.procedureImpl.StateSpaceGeneratorFactory;
import de.rwth.i2.attestor.phases.transformers.InputTransformer;
import de.rwth.i2.attestor.phases.transformers.MCSettingsTransformer;
import de.rwth.i2.attestor.phases.transformers.StateSpaceTransformer;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.recursiveStateMachine.ComponentStateMachine;
import de.rwth.i2.attestor.recursiveStateMachine.RecursiveStateMachine;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpaceGenerationAbortedException;

public class HierarchicalStateSpaceGenerationPhase extends AbstractPhase implements StateSpaceTransformer {
	
	private final StateSpaceGeneratorFactory stateSpaceGeneratorFactory;
	
	Method mainMethod;
	private List<ProgramState> initialStates; // states before any statement has been executed (independent from code)
	private StateSpace mainStateSpace;
	
	private RecursiveStateMachine rsm;
	
	public HierarchicalStateSpaceGenerationPhase(Scene scene) {
		
		super(scene);
		stateSpaceGeneratorFactory = new StateSpaceGeneratorFactory(scene);
	}
	
	@Override
    public void executePhase() {
		
		System.out.println("Hierarchical State Space Generation Phase");
    	
    	// RSM
    	this.rsm = getPhase(RSMGenerationPhase.class).getRSM(); 
    	ComponentStateMachine mainCSM = rsm.getMainComponent();
    	mainMethod = mainCSM.getMethod();
    	rsm.setCurrentNode(mainCSM.getEntryNodes().iterator().next());   	
    	SemanticsCommand currentStatement = rsm.getCurrentNode().getStatement();
    	
    	loadInitialStates();
    	ProgramState testState = initialStates.get(10);
    	
    	// INIT MAIN STATE SPACE
        
    	
    	// EXECUTE STATEMENT
    	
    	
    	// MODEL CHECKING
    	ModelCheckingSettings mcSettings = getPhase(MCSettingsTransformer.class).getMcSettings();
        Set<LTLFormula> formulae = mcSettings.getFormulae();

        for (LTLFormula formula : formulae) {

        	ProofStructure2 proofStructure = new ProofStructure2();
        	try {
    			mainStateSpace = stateSpaceGeneratorFactory.create(mainMethod.getBody(), initialStates).generateAndCheck(formula, proofStructure);
    		} catch (StateSpaceGenerationAbortedException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        }
    }

	@Override
	public StateSpace getStateSpace() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		
		return "Hierarchical State Space Generation";
	}

	@Override
	public void logSummary() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isVerificationPhase() {
		// TODO Auto-generated method stub
		return false;
	}
    
    private void loadInitialStates() {

        List<HeapConfiguration> inputs = getPhase(InputTransformer.class).getInputs();
        initialStates = new ArrayList<>(inputs.size());
        for(HeapConfiguration hc : inputs) {
            initialStates.add(scene().createProgramState(hc));
        }
    }

}
