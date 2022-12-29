package ca.l5.expandingdev.jsgf;

import java.util.List;

public class TemplateSubMatched {
    private String utterance;//intent名称
    private List<Slot> slots;//slot对应的id
    private Float priority = 1.0f;  //rule优先级
    private Integer index;  //Rule在模板文件中的顺序
    
    public TemplateSubMatched(Rule r, List<Slot> slot_list) {
    	this.utterance = r.name;
    	this.slots = slot_list;
	}

	public String getUtterance() {
		return this.utterance;
	}

	public List<Slot> getSlots() {
		return this.slots;
	}
}
