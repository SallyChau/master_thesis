package de.rwth.i2.attestor.phases.symbolicExecution.onthefly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rwth.i2.attestor.procedures.ScopedHeap;

/**
 * Wraps scopedHeaps and according externalReorderings for AP computation of states for
 * procedure state spaces.
 * 
 * @author chau
 *
 */
public class ScopedHeapHierarchy {
	
	private List<ScopedHeap> scopedHeaps = new ArrayList<>();
	// maps scoped heap to external reordering for the actual heap in scope in procedure call
	private Map<ScopedHeap, int[]> scopedHeapToExternalReordering = new HashMap<>();
	
	
	
	public ScopedHeapHierarchy() {
		
	}
	
	public ScopedHeapHierarchy(ScopedHeapHierarchy hierarchicalScopedHeap) {
	
		if (hierarchicalScopedHeap != null) {
			this.scopedHeapToExternalReordering = hierarchicalScopedHeap.scopedHeapToExternalReordering;
			this.scopedHeaps = hierarchicalScopedHeap.getScopedHeaps();	
		}
	}

	
	
	public void addScopedHeap(ScopedHeap scopedHeap) {
		
		if (!scopedHeaps.contains(scopedHeap)) scopedHeaps.add(0, scopedHeap);
	}
	
	public List<ScopedHeap> getScopedHeaps() {
		
		return new ArrayList<>(scopedHeaps);
	}
	
	public void addExternalReordering(ScopedHeap scopedHeap, int[] externalReordering) {
		
		addScopedHeap(scopedHeap);
		scopedHeapToExternalReordering.put(scopedHeap, externalReordering);
	}
	
	public int[] getExternalReordering(ScopedHeap scopedHeap) {
		
		return scopedHeapToExternalReordering.get(scopedHeap);
	}
}
