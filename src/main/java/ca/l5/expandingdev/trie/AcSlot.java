package ca.l5.expandingdev.trie;
import java.util.ArrayList;

public class AcSlot {
	//这个类是自己加的，表示提取的slot名字
	private String slot = null;
    private ArrayList<String> slots = null;
    public AcSlot(String slot) {
        this.slot = slot;
    }
    public AcSlot(ArrayList<String> slots) {
        this.slots = slots;
    }
    
    @Override
    public String toString() {
    	if (this.slots != null) {
    		return String.join("!", this.slots);
    	} else {
    		return this.slot;
    	}
    }
}
