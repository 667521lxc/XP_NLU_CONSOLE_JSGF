package ca.l5.expandingdev.jsgf;

import java.util.ArrayList;
import java.util.List;

public class Rule {
	public static List<ReTags> re_tags = new ArrayList<ReTags>();//提槽的位置信息，存储在这
	public static List<Slot> results = new ArrayList<Slot>(); //提的槽位都在这里
    
    public String utterance;
    public Expansion expansion;
    public int priority;
    public int index;
    public boolean isWild;//表明这条规则里面是否有W: 通配符
    public List<Integer> wl = null;

    public Rule(String n, boolean visible, List<Integer> WL, int pri, int ind, Expansion... exp) {//有通配符的构造函数
    	utterance = n;
        if (exp.length > 1) {
            expansion = new Sequence(exp);
        } else {
            expansion = exp[0];
        }
        priority = pri;
        index = ind;
        isWild = visible;
        wl = WL;
    }
    
    public Rule(String n, boolean visible, int pri, int ind, Expansion... exp) {//没有通配符的构造函数
    	utterance = n;
        if (exp.length > 1) {
            expansion = new Sequence(exp);
        } else {
            expansion = exp[0];
        }
        priority = pri;
        index = ind;
        isWild = visible;
    }

    public String getRuleString() {
        String s = "";
        s = expansion.getString();

        return index + "  <" + utterance + "> = " + s + ";";
    }

    public Expansion getChildExpansion() {
        return expansion;
    }

    public void setFalse() {
    	isWild = false;
    }

    public void setTrue() {
    	isWild = true;
    }
}
