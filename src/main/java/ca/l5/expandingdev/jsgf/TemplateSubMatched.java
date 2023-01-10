package ca.l5.expandingdev.jsgf;

import java.util.List;

public class TemplateSubMatched {
    private Rule rule;//intent名称
    private List<Slot> slots;//slot对应的id
    private Float priority = 1.0f;  //rule优先级
    private Integer index;  //Rule在模板文件中的顺序
    
    public TemplateSubMatched(Rule r, List<Slot> slot_list) {
    	this.rule = r;
    	this.slots = slot_list; 
	}

	public Rule getUtterance() {
		return this.rule;
	}

	public List<Slot> getSlots() {
    	//处理name（utterance）中的slot信息
    	String name = this.rule.utterance;
    	String[] name1 = name.split("!");//这样分割出来，第一个是utterance，后面是每一个槽位
    	for (String slot: name1){
    		if (slot.contains(":")) {
    			String[] name2 = slot.split(":");
    			Slot sl = new Slot(name2[0],name2[1]); 
    			this.slots.add(sl);
    		}  
    	}  
		return this.slots;
	}
}
