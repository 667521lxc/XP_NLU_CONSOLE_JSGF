package ca.l5.expandingdev.jsgf;


public class Wildcard implements Expansion{//是根Sequence的一个子结构，与普通Expansion同级，下级就是Token
    private int minl;
    private int maxl;

    public Wildcard(int minlen,int maxlen) {
        minl = minlen;
        maxl = maxlen;
    }
    
    public int getMin() {
        return minl;
    }
    
    public int getMax() {
        return maxl;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public boolean hasUnparsedChildren() {
        return false;
    }

	@Override
	public String getString() {
		// TODO Auto-generated method stub
		return "<W:"+minl+"-"+maxl+">";
	}

}

