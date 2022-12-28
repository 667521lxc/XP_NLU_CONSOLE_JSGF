package ca.l5.expandingdev.jsgf;

import java.util.List;

public class WordKeyBO {
    private String value;//query中的汉字词语
    private List<String> labelList;//value对应的keyword列表
	
	public WordKeyBO(String s , List<String> s_lable) {
		this.value = s;
		this.labelList = s_lable;
	}
	
    @Override
    public String toString() {
        return value + "->" + labelList;
    }
    
	public String getValue() {
		return value;
	}
	
	public List<String> getLabelList() {
		return labelList;
	}
}
