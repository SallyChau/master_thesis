package de.rwth.i2.attestor.phases.modelChecking.modelChecker;

import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.ANextLtlform;
import de.rwth.i2.attestor.generated.node.AReleaseLtlform;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.generated.node.Start;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * The proof structure is generated by the tableau method for model checking
 * (see Another Look at LTL Model Checking, Clarke, Grumberg, Hamaguchi, 1994).
 *
 * @author christina
 */

public class ProofStructure {

    private static final Logger logger = LogManager.getLogger("proofStructure.java");

    final TIntObjectMap<Set<Assertion>> stateIdToVertices;

    final HashMap<Assertion, HashSet<SuccState>> edges;
    boolean successful = true;

    Assertion originOfFailure = null;
    boolean buildFullStructure = false;
    private StateSpace stateSpace;

    public ProofStructure() {

        this.stateIdToVertices = new TIntObjectHashMap<>();
        this.edges = new LinkedHashMap<>();
    }

    void setBuildFullStructure() {

        buildFullStructure = true;
    }

    private void addAssertion(Assertion assertion) {

        int stateId = assertion.getProgramState();
        Set<Assertion> assertionsOfId = stateIdToVertices.get(stateId);
        if (assertionsOfId == null) {
            assertionsOfId = new LinkedHashSet<>();
            assertionsOfId.add(assertion);
            stateIdToVertices.put(stateId, assertionsOfId);
        } else {
            assertionsOfId.add(assertion);
        }
    }

    /**
     * This method builds the proof structure according to the tableau method (as depicted in
     * Jonathan's PhD thesis).
     * It sets the successful variable to false, if a failing leaf or cycle is detected.
     *
     * @param statespace, the (labelled) state space we want to check the formula for
     * @param formula,    the ltl formula to check
     */
    public void build(StateSpace statespace, LTLFormula formula) {

        this.stateSpace = statespace;

        logger.trace("Building proof structure for formula " + formula.toString());

        // The queue holding the vertices that have still to be processed
        LinkedList<Assertion> vertexQueue = new LinkedList<>();

        // Initialise the switch
        TableauRulesSwitch rulesSwitch = new TableauRulesSwitch(statespace);

        TIntIterator initialStatesIterator = statespace.getInitialStateIds().iterator();
        while (initialStatesIterator.hasNext()) {
            //for(ProgramState initial : statespace.getInitialStates()){
            int stateId = initialStatesIterator.next();
            Assertion initialAssertion = new Assertion(stateId, null, formula);
            this.stateIdToVertices.putIfAbsent(stateId, new LinkedHashSet<>());

            addAssertion(initialAssertion);
            vertexQueue.add(initialAssertion);
        }

        // Process vertices until no unprocessed ones remain
        while (!vertexQueue.isEmpty()) {

            Assertion currentVertex = vertexQueue.poll();
            System.out.println(currentVertex.progState);

            // Do a tableau step
            if (!currentVertex.getFormulae().isEmpty()) {
                Node currentSubformula = currentVertex.getFirstFormula();

                if (isNextForm(currentSubformula)) {
                    // Apply next tableau rule to all remaining formula in the current vertice's formula set
                    // Note that due to the insertion order we know that all contained formulae are next formulae

                    // First collect the successor formula of each contained next formula
                    HashSet<Node> nextSuccessors = new LinkedHashSet<>();
                    for (Node nextFormula : currentVertex.getFormulae()) {
                        nextFormula.apply(rulesSwitch);

                        assert (rulesSwitch.getOut(nextFormula) instanceof Node);
                        Node successorNode = (Node) rulesSwitch.getOut(nextFormula);
                        nextSuccessors.add(successorNode);
                    }

                    // Generate an assertion for each successor state of the current state in the state space
                    // with formula set equal to the next successor formulae generated before
                    int currentState = currentVertex.getProgramState();
                    TIntSet successors = new TIntHashSet(100);
                    // Collect the "real" successor states (i.e. skipping materialisation steps)
                    TIntArrayList materializationSuccessorIds = statespace.getMaterializationSuccessorsIdsOf(currentState);
                    if (!materializationSuccessorIds.isEmpty()) {
                        TIntIterator matStateIterator = materializationSuccessorIds.iterator();
                        while (matStateIterator.hasNext()) {
                            //for(ProgramState matState : statespace.getMaterializationSuccessorsIdsOf(currentState)){
                            // Every materialisation state is followed by a control flow state
                            int matState = matStateIterator.next();
                            TIntArrayList controlFlowSuccessorIds = statespace.getControlFlowSuccessorsIdsOf(matState);
                            assert (!controlFlowSuccessorIds.isEmpty());
                            successors.addAll(controlFlowSuccessorIds);
                        }
                    } else {
                        successors.addAll(statespace.getControlFlowSuccessorsIdsOf(currentState));
                        // In case the state is final
                        successors.addAll(statespace.getArtificialInfPathsSuccessorsIdsOf(currentState));
                    }
                    TIntIterator successorIterator = successors.iterator();
                    while (successorIterator.hasNext()) {
                        int succState = successorIterator.next();
                        Assertion newAssertion = new Assertion(succState, currentVertex, true);

                        for (Node succFormula : nextSuccessors) {
                            newAssertion.addFormula(succFormula);
                        }

                        // Check if we have already seen an equal assertion before
                        boolean formulaePresent = true;
                        Set<Assertion> presentAssertions = getVerticesForState(newAssertion.getProgramState());
                        // Note that formulaePresent is finally true, iff we found an equal assertion
                        if (presentAssertions.isEmpty()) {
                            formulaePresent = false;
                        } else {
                            for (Assertion presentAssertion : presentAssertions) {
                                // Initialise for current iteration
                                formulaePresent = true;
                                if (newAssertion.getFormulae().size() == presentAssertion.getFormulae().size()) {
                                    for (Node ASTnode : newAssertion.getFormulae()) {
                                        if (!presentAssertion.getFormulae().contains(ASTnode)) {
                                            formulaePresent = false;
                                            break;
                                        }
                                    }
                                } else {
                                    formulaePresent = false;
                                }
                                if (formulaePresent) {
                                    newAssertion = presentAssertion;
                                    break;
                                }
                            }
                        }

                        this.addEdge(currentVertex, new SuccState(newAssertion, currentSubformula));

                        addAssertion(newAssertion);
                        // Process the assertion further only in case it is not one, that was already processed
                        if (!formulaePresent) {
                            vertexQueue.add(newAssertion);
                        } else {
                            // we detected a potential cycle, check if it is a real and in that case an unharmful one (containing a release
                            // operator)

                            // Real cycle?
                            boolean isReal = false;
                            // First collect all successors
                            HashSet<Assertion> seen = new LinkedHashSet<>();
                            LinkedList<Assertion> queue = new LinkedList<>();
                            queue.add(newAssertion);
                            seen.add(newAssertion);
                            while (!queue.isEmpty() && !isReal) {
                                Assertion curAssertion = queue.pop();
                                for (Assertion succAssertion : this.getSuccessors(curAssertion)) {
                                    if (succAssertion.equals(newAssertion)) {
                                        isReal = true;
                                        break;
                                    }

                                    if (!seen.contains(succAssertion)) {
                                        queue.add(succAssertion);
                                    }
                                    seen.add(succAssertion);
                                }

                            }

                            if (isReal) {
                                // Contains a release operator?
                                boolean containsReleaseOp = false;
                                for (Node current : newAssertion.getFormulae()) {
                                    if (current instanceof AReleaseLtlform) {
                                        containsReleaseOp = true;
                                    }
                                }
                                if (!containsReleaseOp) {
                                    this.successful = false;

                                    if (this.originOfFailure == null) {
                                        this.originOfFailure = newAssertion;
                                    }
                                    // Optimisation: abort proof structure generation, as we already know that it is not successful!
                                    if (!buildFullStructure) {
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    rulesSwitch.setIn(currentSubformula, currentVertex);
                    currentSubformula.apply(rulesSwitch);

                    // Retrieve the generated assertions
                    @SuppressWarnings("unchecked") Set<Assertion> successors = (Set<Assertion>) rulesSwitch.getOut(currentSubformula);
                    // This means that the current vertex is not (yet) successful
                    if (successors != null) {
                        HashSet<SuccState> successorStates = new LinkedHashSet<>();
                        for (Assertion assertion : successors) {
                            successorStates.add(new SuccState(assertion, currentSubformula));
                        }

                        for (Assertion succ : successors) {
                            addAssertion(succ);
                        }
                        vertexQueue.addAll(successors);

                        this.addEdges(currentVertex, successorStates);


                    }


                }
            } else {
                this.successful = false;
                if (this.originOfFailure == null) {
                    this.originOfFailure = currentVertex;
                }
                // Optimisation: abort proof structure generation, as we already know that it is not successful!
                if (!buildFullStructure) {
                    return;
                }
            }


        }
    }

    /**
     * This method collects all vertices, whose program state component is equal to
     * the input program state.
     *
     * @param programState, the state that the vertices are checked for
     * @return a set containing all assertions with program state component 'state',
     * if none exist an empty set is returned.
     */
    private Set<Assertion> getVerticesForState(int programState) {

        Set<Assertion> result = stateIdToVertices.get(programState);
        if (result == null) {
            return Collections.emptySet();
        }
        return result;
    }

    private void addEdges(Assertion currentVertex, HashSet<SuccState> successorStates) {

        if (!edges.containsKey(currentVertex)) {
            edges.put(currentVertex, successorStates);
        } else {
            edges.get(currentVertex).addAll(successorStates);
        }

    }

    private void addEdge(Assertion currentVertex, SuccState successorState) {

        if (!edges.containsKey(currentVertex)) {
            HashSet<SuccState> newSuccStatesSet = new LinkedHashSet<>();
            newSuccStatesSet.add(successorState);
            edges.put(currentVertex, newSuccStatesSet);
        } else {
            edges.get(currentVertex).add(successorState);
        }

    }

    /**
     * This method checks whether the outermost operator of the formula is a next-operator.
     *
     * @param formula, the formula the outermost operator should be checked for
     * @return true, in case the outermost operator is a next-operator
     * false, in all other cases
     */
    private boolean isNextForm(Node formula) {

        if (formula instanceof ANextLtlform) {
            return true;
        } else if (formula instanceof Start) {
            Start helper = (Start) formula;
            return helper.getPLtlform() instanceof ANextLtlform;
        }

        return false;
    }

    public boolean isSuccessful() {

        return this.successful;
    }

    public HashSet<Assertion> getLeaves() {

        HashSet<Assertion> leaves = new LinkedHashSet<>();

        TIntObjectIterator<Set<Assertion>> iter = stateIdToVertices.iterator();
        while (iter.hasNext()) {
            iter.advance();
            for (Assertion vertex : iter.value()) {
                if (!edges.containsKey(vertex)) {
                    leaves.add(vertex);
                }
            }
        }
        return leaves;
    }

    public Integer size() {

        return getVertices().size();
    }

    public HashSet<Assertion> getVertices() {

        HashSet<Assertion> vertices = new LinkedHashSet<>();

        TIntObjectIterator<Set<Assertion>> iter = stateIdToVertices.iterator();
        while (iter.hasNext()) {
            iter.advance();
            vertices.addAll(iter.value());
        }
        return vertices;
    }

    public HashSet<Assertion> getSuccessors(Assertion current) {

        HashSet<Assertion> successors = new LinkedHashSet<>();
        if (this.edges.get(current) != null) {
            for (SuccState successor : this.edges.get(current)) {
                successors.add(successor.assertion);
            }
        }
        return successors;
    }

    public FailureTrace getFailureTrace() {

        if (isSuccessful()) { // proof was successful, no counterexample exists
            return null;
        }

        assert (this.originOfFailure != null);

        return new FailureTrace(this.originOfFailure, stateSpace);
    }

    /**
     * This class models the edges of the proof structure. I.e. it holds a successor assertion
     * together with the edge label, that carries the type of applied tableau rule.
     *
     * @author christina
     */
    class SuccState {

        final Assertion assertion;
        final Node type;

        // TODO: check if we need succstates instead of assertions only
        private SuccState(Assertion assertion, Node type) {

            this.assertion = assertion;
            this.type = type;
        }

    }

}
