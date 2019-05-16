package attestor;

public class DLList {

    private DLList next;
    private DLList prev;
    
    public DLList(){
        this.next = null;
        this.prev = null;
    }
    
    public DLList(DLList list) {
    
        this.next = list;
        this.prev = null;
        list.prev = this;
    }
    
    public static DLList build() {
    	
	    DLList list = new DLList();
	    
	    for(int i = 0; i < 10; i++){
	        list = new DLList(list);
	    }
	    
	    return list;
	}

	public static void traverse(DLList head) {

		DLList current = head;
		
        while(current != null){
            current = current.next;
        }
    }

    public static DLList reverse(DLList head) {

        DLList current = head; 
        DLList tmp = null;
  
        // swap next and prev for all nodes
        while (current != null) { 
            tmp = current.prev; 
            current.prev = current.next; 
            current.next = tmp; 
            current = current.prev; 
        } 
        
        if (tmp != null) { 
            head = tmp.prev; 
        } 
        
        return head;
	}
    
    public static DLList concat(DLList head, DLList tail) {
    	
    	DLList result = head;
    	
    	DLList current = findLast(head);
    	
    	DLList tmp = current;
    	current.next = tail;
    	tail.prev = tmp;
    	
    	print(result);
    	
    	return result;
	}
    
    public static int find(DLList list, DLList object) {
    	
    	int counter = 0;
    	
    	DLList current = list;
		while (current.next != null) {
			if (current.equals(object)) {
				return counter;
			}
			current = current.next;
			counter = counter + 1;
		}
    	
    	return -1;
    }

	public static DLList findFirst(DLList head) {

		DLList current = head;

		while(current.prev != null){
			current = current.prev;
		}

		return current;
	}

	public static DLList findLast(DLList head) {

		DLList current = head;

		while(current.next != null){
			current = current.next;
		}

		return current;
	}
	
	public static DLList insert(DLList list, DLList object, int pos) {
		
		DLList current = list;
    	for (int i = 0; i < pos; i++) {
    		if (current.next != null) {
    			current = current.next;
    		}
		}
    	
    	DLList secondHalf = current.next;   
    	current.next = object;
    	object.prev = current;
    	
    	// add second half
    	current = findLast(object);
    	current.next = secondHalf;
    	secondHalf.prev = current;
		
		return list;
	}

	public static DLList delete(DLList list, int pos) {
		
		// Base case 
        if (list == null || pos < 0) { 
            return list; 
        } 
        
        // If node to be deleted is head node 
        // does not modify the original list
        if (pos == 0) { 
        
        	DLList tmp = list.next;
            list = list.next; 
            tmp.prev = null; 
            return list;
        }
        
        System.out.println("inside delete");
        print(list);
		
        // will change the original list
		DLList current = list;
    	DLList prev = null;
    	for (int i = 0; i < pos; i++) {
    		if (current.next != null) {
    			prev = current;
    			current = current.next;
    		} else {
    			return list;
    		}
		}
    	
    	DLList tmp = current.next;
		prev.next = current.next;
    	tmp.prev = prev;
        
        return list;
	}
	
	public static void print(DLList list) {
    	
    	int counter = 0;
    	DLList current = list;
    	
		System.out.println(counter + ": " + current);
		
    	while(current.next != null) {
    		current = current.next;
    		counter ++;
    		System.out.println(counter + ": " + current);
    	}
    }
	
	public static void printReversed(DLList list) {
    	
    	int counter = 0;
    	DLList current = findLast(list);
    	
		System.out.println(counter + ": " + current);
		
    	while(current.prev != null) {
    		current = current.prev;
    		counter++;
    		System.out.println(counter + ": " + current);
    	}
    }
	

	public static void main(String[] args) {
    
        DLList head = build();
        print(head);
        System.out.println("----");
        DLList deletedList = delete(head, 5);
        print(deletedList);
    }    
}
