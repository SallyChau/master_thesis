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
     * If true, unreachable parts of heap are regularly eliminated.
     */
    private boolean hierarchicalModelCheckingEnabled = false;
    
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

    public void setHierarchicalModelCheckingEnabled(boolean enabled) {
    	
    	this.hierarchicalModelCheckingEnabled = enabled;
    }
    
    public boolean isHierarchicalModelCheckingEnabled() {
    	
    	return this.hierarchicalModelCheckingEnabled;
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
