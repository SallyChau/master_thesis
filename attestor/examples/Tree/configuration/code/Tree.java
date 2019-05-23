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
	
	public static Tree getLeft(Tree current) {
        
		current = current.left;
        return current;
    }
    
    public static Tree buildTree() {
    	Tree root = new Tree();
        
    	Tree left = new Tree();
    	Tree right = new Tree();
        root.left = left;
        root.right = right;
        
        return root;
    }

    public static Tree getLeftmostChild(Tree tree){
        while(tree.left != null){
            tree = tree.left;
        }
        return tree;
    }

    private static Tree traverseRecursive(Tree tree) {

    	Tree current = tree;
        if(tree == null) {
            return null;
        } else {
        	traverseRecursive(tree.left);
        	traverseRecursive(tree.right);
            return tree;
        }
    }
	
	public static void main(String[] args) {
		
		Tree tree = build();
		tree = traverseRecursive(tree);
	}
}