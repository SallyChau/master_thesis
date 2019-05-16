package attestor;

public class Tree {
	
	private Tree right;
	private Tree left;
	
	public Tree() {
		this.right = null;
		this.left = null;
	}
	
	public static Tree build() {
		
		Tree root = new Tree();
		root.left = new Tree();
		root.right = new Tree();

		Tree current = root;
		for(int i = 0; i < 10; i++) { // enforce nondeterminism
			while(current.left != null && current.right != null) {
				
				if(i < 4) { // enforce nondeterminism
	                current = current.left;
	            } else {
	                current = current.right;
	            }				 
	            
	            if(current.left == null && i > 0) { // enforce nondeterminism
	            	current.left = new Tree();
	            }
	            
	            if(current.right == null && i <= 9) { // enforce nondeterminism
	            	current.right = new Tree();
	            }
			}
        }
		
		return root;
	}
	
	// TODO not working
	public static Tree addRecursive(Tree tree, Tree object, int i) {
	    if (object == null) {
	        return tree;
	    }
	 
	    if (i < 2) { // nondeterminism
	        tree.left = addRecursive(tree.left, object, i-1);
	    } else if (i > 3) {
	        tree.right = addRecursive(tree.right, object, i-1);
	    } else {
	        // value already exists
	        return tree;
	    }
	 
	    return tree;
	}
	
	public static void main(String[] args) {
		
		Tree tree = build();
		Tree object = build();
		Tree added = addRecursive(tree, object, 4);
		System.out.println(added);
	}
	
}
