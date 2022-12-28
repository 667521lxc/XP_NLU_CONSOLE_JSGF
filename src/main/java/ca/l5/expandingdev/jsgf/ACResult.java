package ca.l5.expandingdev.jsgf;

import java.util.List;

public class ACResult {
	WordKeyBO wordKeyBO;
	Integer index;//value的结束坐标
	
	public ACResult(String s , List<String> lable , int index) {
		WordKeyBO w = new WordKeyBO(s, lable);
		this.wordKeyBO = w;
		this.index = index;
	}
	
    @Override
    public String toString() {
        return wordKeyBO.toString() + "->" + index;
    }

	public WordKeyBO getWordKeyBO() {
		// TODO Auto-generated method stub
		return wordKeyBO;
	}

	public Integer getIndex() {
		// TODO Auto-generated method stub
		return index;
	}

}
