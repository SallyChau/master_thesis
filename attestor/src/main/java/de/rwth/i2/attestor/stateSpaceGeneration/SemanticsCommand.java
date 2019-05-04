package de.rwth.i2.attestor.stateSpaceGeneration;

import java.util.Collection;
import java.util.Set;

import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.phases.symbolicExecution.onthefly.ScopedHeapHierarchy;

/**
 * An abstraction of abstract program semantics that is executed on objects of type {@link ProgramState}.
 *
 * @author Christoph
 */
public interface SemanticsCommand {

    /**
     * Executes a single step of the abstract program semantics on the given program state.
     * Since the abstract program semantics may be non-deterministic (for example if a conditional statement cannot
     * be evaluated), this results in a set of successor program states in general.
     *
     * @param programState The state on which the abstract program semantics shall be executed.
     * @return All states resulting from executing the program semantics on programState.
     */
    Collection<ProgramState> computeSuccessors(ProgramState programState, ScopedHeapHierarchy scopeHierarchy);
    
    /**
     * Works similar as {@SemanticsCommand.computeSuccessors(ProgramState programState)} but also model checks formulae for programState
     * while computing successor states. The model checking results can be retrieved by 
     * {@SemanticsCommand.getResultFormulae(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy)} and
     * {@SemanticsCommand.satisfiesFormulae(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy)}.
     * @param programState The state on which the abstract program semantics shall be executed.
     * @param formula The formula to be checked for programState.
     * @param scopeHierarchy The hierarchy of scoped heaps to be considered when computing APs for procedure program states.
     * @return All states resulting from executing the program semantics on programState.
     */
    Collection<ProgramState> computeSuccessorsAndCheck(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy);
    
    /**
     * Returns the formulae to be checked for successor states of programState after the model checking for programState is done
     * if programState invoked a method call. Else an empty set is returned.
     * @param programState The state on which the abstract program semantics shall be executed.
     * @param formulae The formula to be checked for programState.
     * @param scopeHierarchy The hierarchy of scoped heaps to be considered when computing APs for procedure program states.
     * @return All formulae resulting from model checking the program semantics on programState.
     */
    Set<Node> getResultFormulae(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy);
    
    /**
     * Returns whether programState satisfies formulae.
     * @param programState The state for which the formulae should be checked.
     * @param formulae The formula to be checked for programState.
     * @param scopeHierarchy The hierarchy of scoped heaps to be considered when computing APs for procedure program states.
     * @return true if programState satisfies formulae.
     */
	boolean satisfiesFormulae(ProgramState programState, Set<Node> formulae, ScopedHeapHierarchy scopeHierarchy);

    /**
     * @return All potential violation points that may prevent execution of this statement.
     */
    ViolationPoints getPotentialViolationPoints();

    /**
     * @return The set of all program locations that are direct successors of this program statement in
     * the underlying control flow graph.
     */
    Set<Integer> getSuccessorPCs();

    /**
     * @return true, if the statement always requires canonicalization
     */
    boolean needsCanonicalization();
    
    /**
     * Prepares the heap of programState for the execution of the statement.
     * @param programState
     * @return The prepared heap.
     */
    ProgramState prepareHeap(ProgramState programState);
}
