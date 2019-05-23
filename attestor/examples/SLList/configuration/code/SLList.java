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
    
    public static SLList buildRecursive(int argLength, SLList list) {

        if(argLength < 42) { // nondeterminism
        	SLList nextElement = new SLList(list);
            return buildRecursive(argLength, nextElement);
        }
        
        return list;
    }
    
    public static void traverse(SLList head){

		SLList current = head;
		
		while (current.next != null) {
			current = current.next;
		}
	}
    
    public static void traverseFaulty(SLList head){

		SLList current = head;
		
		while (current.next != null) {
			current = current.next;
			swap(head);
		}
	}
    
    public static void traverseFaulty2(SLList head){

		SLList current = head;
		SLList second = build();
		
		while (current.next != null) {
			current = current.next;
			foo(head);
		}
	}
    
    public static void foo(SLList head) {
    	SLList second = build();
    }
    
    public static void swap(SLList head){
    	
    	SLList dummy = head.next;
    	head.next = null;
    	head.next = dummy;
	}
    
    public static SLList traverseRecursive(SLList head) {
    	
    	SLList current = head;

        if(current == null) {
            return null;
        } else if (current.next == null) {
            return current;
        } else {
            return traverseRecursive(current.next);
        }
    }

    // list needs to be named "head" because of variable reference in LTL formula
    public static SLList reverse(SLList head) {
    	
    	SLList reversedList = null;
		SLList current = head;
		
		while (current != null) {
			SLList next = current.next;
			current.next = reversedList;
			reversedList = current;
			current = next;
		}
		
    	return reversedList;
    }
    
    public static SLList reverseRecursive(SLList head) {

    	SLList first;
    	SLList rest;

		if(head == null) {
			return head;
		}

		first = head;
		rest = first.next;

		if(rest == null) {
			return first;
		}

		rest = reverseRecursive(rest);

		first.next.next = first;
		first.next = null;

		head = rest;
		return head;
	}
    
    // returns the position of the object
    public static SLList find(SLList list) {
    	
    	SLList current = list;
    	int length = 5;
    	int i = 0;
		while (current.next != null) {
			if (i == length) { // nondeterminism
				return current;
			}
			i++;
			current = current.next;
		}
    	
    	return current;
    }
    
    public static SLList findMiddle(SLList head){

    	SLList slow = head;
    	SLList fast = head;
		while(fast != null){

			fast = fast.next;
			if(fast != null) {
				fast = fast.next;
			}

			slow = slow.next;
		}

		return slow;
	}

	public static SLList findMiddleFaulty(SLList head){

		SLList slow = head;
		SLList fast = head;
		while(fast != null){

			fast = fast.next;
			slow = slow.next;
			if(fast != null) {
				slow = fast.next;
			}

		}

		return slow;
	}
    
    // Concatenates first list with second list. First list will be modified.
	public static void buildAndConcat() {
		
		SLList first = build();
		SLList second = build();
		
		SLList result = first;
	
		// find last element in first list
		while (result.next != null) {
			result = result.next;
		}
		
		result.next = second;
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
    
    public static void zipDummy() {
    	
//    	SLList left = new SLList(null);
//    	SLList right = new SLList(null);
    	SLList left = build();
    	SLList right = build();
    	// using nondeterminism causes infinite computation time --> require thresholds
    	SLList zipped = zip(left, right);
    }
    
    public static SLList zip(SLList left, SLList right) {
    	
    	if (left == null)  {
            return right;
        }
    	
    	SLList first = left;
    	
    	return zipRight(first, first.next, right);
    }
    
	public static SLList zipLeft(SLList current, SLList left, SLList right) {
	    	
		if(left == null) {
            current.next = right;
            return current;
        } else {
            current.next = left;
            return zipRight(left, left.next, right);
        }
    }
	
	public static SLList zipRight(SLList current, SLList left, SLList right) {
		
		if(right == null) {
            current.next = left;
            return current;
        } else {
            current.next = right;
            return zipLeft(right, left, right.next);
        }
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
