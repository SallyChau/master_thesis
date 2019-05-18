public class SLList {
	
    public SLList next;
    
    public SLList(SLList next) {
		this.next = next;
	}
    
    public static SLList buildList() {
    	
		SLList list = new SLList(null);
		
		for (int i = 0; i < 10; i++) {
			list = new SLList(list);
		}
		
		return list;
	}
    
    public static void traverseList(SLList head){

		SLList current = head;
		int counter = 0;
		while (current.next != null) {
			System.out.println(counter);
			counter++;
			current = current.next;
		}
	}   
    
    public static void breakList() {
    	
    	SLList list = buildList();
    	
    	traverseList(list);
    	
    	// assume we have at least 3 elements
    	SLList secondElement = list.next;
    	
    	SLList thirdElement = secondElement.next;
    	System.out.println(thirdElement.toString());
    	
    	// delete second element of list
    	secondElement = null;
    	
    	// concatenate two lists again
    	list.next = thirdElement;    	
    }
    
    public static void main(String[] args) {
        
    	SLList list = buildList();
    	breakList();
    }
} 
