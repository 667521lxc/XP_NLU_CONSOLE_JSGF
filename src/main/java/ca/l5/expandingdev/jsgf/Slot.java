package ca.l5.expandingdev.jsgf;

import java.util.List;

public class Slot {
	private short slotIndex; //序号
	private String name; //标签
	private String rawValue;//原始值
	private Object value;//解析后的值
	private String type;
	private List<Integer> pos;//槽位对应的位置信息
	
	public Slot(String n, String r, List<Integer> p) {
		this.name = n;
		this.rawValue = r;
		this.pos = p;
	}
	
	public Slot(String n, String r, Object v, String t, List<Integer> p) {
		this.name = n;
		this.rawValue = r;
		this.value = v;
		this.type = t;
		this.pos = p;
	}
	
	public String toString() {
		return this.name + "//" + this.rawValue+ "//" + this.pos.get(0)+ "//" + this.pos.get(1);
	}
}
