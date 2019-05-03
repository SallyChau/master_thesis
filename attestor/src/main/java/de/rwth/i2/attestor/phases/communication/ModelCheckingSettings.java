package de.rwth.i2.attestor.phases.communication;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.rwth.i2.attestor.LTLFormula;


/**
 * The collection of the model checking related communication including the
 * formulae.
 *
 * @author christina
 */
public class ModelCheckingSettings {

    private final Set<String> requiredAtomicPropositions = new LinkedHashSet<>();
    // Contains all LTL formulae model checking should be performed for.
    private final Set<LTLFormula> formulae;
    // Indicates whether model checking is conducted.
    private boolean modelCheckingEnabled = false;
    /**
     * Describes mode of model checking: 
     * - default (only checks main state space), 
     * - hierarchical (also checks procedure state spaces using RSM), 
     * - onthefly (checks procedure state spaces on the fly) 
     */
    private String modelCheckingMode = "default";
    
    private List<String> methodsToSkip = new LinkedList<>();

    public ModelCheckingSettings() {

        this.formulae = new LinkedHashSet<>();
    }

    public boolean isModelCheckingEnabled() {

        return this.modelCheckingEnabled;
    }

    public void setModelCheckingEnabled(boolean enabled) {

        this.modelCheckingEnabled = enabled;
    }

    public Set<LTLFormula> getFormulae() {

        return this.formulae;
    }

    public void addFormula(LTLFormula formula) {

        this.formulae.add(formula);
        for (String ap : formula.getApList()) {
            requiredAtomicPropositions.add(extractAP(ap));
        }
    }

    private String extractAP(String apString) {

        String[] apContents = apString.split("[\\{\\}]");
        if (apContents.length < 2) {
            return null;
        }
        return apContents[1].trim();
    }

    public Set<String> getRequiredAtomicPropositions() {

        return requiredAtomicPropositions;
    }

    public void setModelCheckingMode(String mode) {
    	
    	switch(mode) {
    	case "default":
    	case "hierarchical":
    	case "onthefly":
    		modelCheckingMode = mode;
    		break;
    	default:
            throw new IllegalArgumentException("Unknown model checking mode: " + mode);
    	}
    }
    
    public String getModelCheckingMode() {
    	
    	return modelCheckingMode;
    }
    
    public void addMethodToSkip(String method) {
    	
    	if (!methodsToSkip.contains(method)) {
    		methodsToSkip.add(method);
    	}
    }
    
    public List<String> getMethodsToSkip() {
    	
    	return methodsToSkip;
    }
}
