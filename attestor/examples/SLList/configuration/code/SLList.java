public class SLList {
		
    private SLList next;
    
    // constructor
    public SLList(SLList next) {
		this.next = next;
	}
    
    public static SLList build() {
    	
		SLList list = new SLList(null);		
		
		for (int i = 0; i < 10; i++) {
			list = new SLList(list);
		}
		
		return list;
	}
    
    public static void traverse(SLList head){

		SLList current = head;
		
		while (current.next != null) {
			current = current.next;
		}
	}
    
    public static SLList reverse(SLList list) {
    	
    	SLList reversedList = null;
		SLList current = list;
		
		while (current != null) {
			SLList next = current.next;
			current.next = reversedList;
			reversedList = current;
			current = next;
		}
		
    	return reversedList;
    }
    
    // Concatenates first list with second list. First list will be modified.
    public static SLList concat(SLList first, SLList second) {
    	
    	SLList result = first;

    	// find last element in first list
    	while (result.next != null) {
    		result = result.next;
		}
    	
    	result.next = second;
    	result = first;
    	
    	return result;
    }
    
    // returns the position of the object
    public static int find(SLList list, SLList object) {
    	
    	int counter = 0;
    	
    	SLList current = list;
		while (current.next != null) {
			if (current.equals(object)) {
				return counter;
			}
			current = current.next;
			counter = counter + 1;
		}
    	
    	return -1;
    }
    
    public static SLList insert(SLList list, SLList object, int pos) {
    	
    	SLList current = list;
    	for (int i = 0; i < pos; i++) {
    		if (current.next != null) {
    			current = current.next;
    		}
		}
    	
    	SLList secondHalf = current.next;    	
    	current.next = object;
    	
    	// add second half
    	current = object;
    	while (current.next != null) {
    		current = current.next;
    	}
    	current.next = secondHalf;
		
		return list;
    }
    
    public static SLList delete(SLList list, int pos) {
    	
    	if (pos == 0) {
    		list = list.next;
    		return list;
    	}
    	SLList current = list;
    	SLList previous = null;
    	for (int i = 0; i < pos; i++) {
    		if (current.next != null) {
    			previous = current;
    			current = current.next;
    		} else {
    			return list;
    		}
		}
    	
		previous.next = current.next;
    	return list;
    }
    
    /**
     * Add ten entries to the head of the list tail.
     * @param tail
     * @return
     */
    public static SLList prepend(SLList tail){

        SLList first = new SLList(tail);
        SLList curr = first;
        for(int i = 0; i < 10; i++){ //for Attestor this is non-deterministic
              SLList tmp = new SLList(null); //for demonstration: setting next to null first
              curr.next = tmp;
              curr = tmp;
        }
        
        return first;
   }

	public static void print(SLList list) {
    	
    	int counter = 0;
    	SLList current = list;
    	
		//System.out.println(counter + ": " + current);
		
    	while(current.next != null) {
    		current = current.next;
    		counter ++;
    		//System.out.println(counter + ": " + current);
    	}
    }
    
    // dummy method so that the file compiles
	public static void main(String[] args) {        
    	
    	SLList list1 = build();
    }
}
