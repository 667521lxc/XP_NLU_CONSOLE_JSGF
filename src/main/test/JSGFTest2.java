import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import ca.l5.expandingdev.jsgf.AlternativeSet;
import ca.l5.expandingdev.jsgf.Expansion;
import ca.l5.expandingdev.jsgf.Grammar;
import ca.l5.expandingdev.jsgf.KleeneStar;
import ca.l5.expandingdev.jsgf.OptionalGrouping;
import ca.l5.expandingdev.jsgf.PlusOperator;
import ca.l5.expandingdev.jsgf.RequiredGrouping;
import ca.l5.expandingdev.jsgf.Rule;
import ca.l5.expandingdev.jsgf.Sequence;
import ca.l5.expandingdev.jsgf.Tag;
import ca.l5.expandingdev.jsgf.Token;
import ca.l5.expandingdev.jsgf.Wildcard;
//这个是单句调试用的

public class JSGFTest2 {
    public static HashMap<String,Byte> ch_dict = new HashMap<String,Byte>();
    public static HashMap<Byte,String> ch_dict2 = new HashMap<Byte,String>();
    
	public static void read_dict(String file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
        String str;
        byte b = Byte.MIN_VALUE;
        String[] str_list = new String[2];
		while ((str = in.readLine()) != null) {
			str_list = str.split(" ");
			ch_dict.put(str_list[0], b);
			ch_dict2.put(b , str_list[0]);
			b += 1;
        }
		in.close();		
	}	
	public static String readfile(String file) throws IOException {//读文件，所有内容拼成一个字符串
		BufferedReader in = new BufferedReader(new FileReader(file));
        String str;
        StringBuilder re = new StringBuilder();
		while ((str = in.readLine()) != null) {
			re.append(str).append("\n");
        }
		in.close();
        return re.toString();	
	}
	
    public static String traversePreOrder(Expansion root) throws UnsupportedEncodingException {//root必不为空
    	StringBuilder sb = new StringBuilder();
        String pointerRight = "└──";
        if (root instanceof Token) {
        	traverseNodes(sb, "", pointerRight, root, false);
        }if (root instanceof Wildcard) {
        	traverseNodes(sb, "", pointerRight, root, false);
        }else if(root instanceof AlternativeSet) {
            List<Expansion> expansionA=((AlternativeSet)root).getChildExpansions();
    		for(int i=0; i<expansionA.size()-1; i++) {
    			traverseNodes(sb, "", pointerRight, expansionA.get(i), true);
    		}
            traverseNodes(sb, "", pointerRight, expansionA.get(expansionA.size()-1), false);
        }else if(root instanceof Sequence) {
            List<Expansion> expansionA=((Sequence)root).getChildExpansions();
    		for(int i=0; i<expansionA.size()-1; i++) {
    			traverseNodes(sb, "", pointerRight, expansionA.get(i), true);
    		}
            traverseNodes(sb, "", pointerRight, expansionA.get(expansionA.size()-1), false);
        }else if(root instanceof Tag) {//Tag只有一个exp
            traverseNodes(sb, "", pointerRight, ((Tag)root).getChildExpansion(), false);
        }else if(root instanceof OptionalGrouping) {
            traverseNodes(sb, "", pointerRight, ((OptionalGrouping)root).getChildExpansion(), false);
        }else if(root instanceof RequiredGrouping) { 
            traverseNodes(sb, "", pointerRight, ((RequiredGrouping)root).getChildExpansion(), false);
        }else if(root instanceof PlusOperator) { 
            traverseNodes(sb, "", pointerRight, ((PlusOperator)root).getChildExpansion(), false);
        }else if(root instanceof KleeneStar) { 
            traverseNodes(sb, "", pointerRight, ((KleeneStar)root).getChildExpansion(), false);
        }    
        sb.append("\n");
        return sb.toString();
    }

    public static void traverseNodes(StringBuilder sb, String padding, String pointer, Expansion node,
                                      boolean hasRightSibling) throws UnsupportedEncodingException {
        sb.append("\n");
        sb.append(padding);
        sb.append(pointer);
        if (node instanceof Token) {
            Token t = (Token) node;
            short textIdx = (short) t.getText();
            String ts = "";
            /*if (b_arr.length==1) {
                if (ch_dict2.containsKey((b_arr[0]))) {
                    ts=ch_dict2.get(b_arr[0]);
                } else {
                    ts=new String(b_arr,"UTF-8");
                }
            } else {
                ts=new String(b_arr,"UTF-8");
            }*/

            if(Grammar.dict2.containsKey(textIdx)){
                ts = Grammar.dict2.get(textIdx);
            }else{
                ts = "####";
                System.out.println("无法从dict2中获取对应的字符，textIdx："+textIdx);
            }
            
        	sb.append(ts);
        	return;
        }
        if (node instanceof Wildcard) {
        	sb.append(((Wildcard)node).getString());
        	return;
        }
        
        StringBuilder paddingBuilder = new StringBuilder(padding);
        if (hasRightSibling) {
            paddingBuilder.append("│  ");
        } else {
            paddingBuilder.append("   ");
        }

        String paddingForBoth = paddingBuilder.toString();
        String pointerRight = "└──";
        if(node instanceof AlternativeSet) {
        	sb.append("AlternativeSet");
            List<Expansion> expansionA=((AlternativeSet)node).getChildExpansions();
    		for(int i=0; i<expansionA.size()-1; i++) {
    			traverseNodes(sb, paddingForBoth, pointerRight, expansionA.get(i), true);
    		}
            traverseNodes(sb, paddingForBoth, pointerRight, expansionA.get(expansionA.size()-1), false);
        }else if(node instanceof Sequence) {
            sb.append("Sequence");
            List<Expansion> expansionA=((Sequence)node).getChildExpansions();
    		for(int i=0; i<expansionA.size()-1; i++) {
    			traverseNodes(sb, paddingForBoth, pointerRight, expansionA.get(i), true);
    		}
            traverseNodes(sb, paddingForBoth, pointerRight, expansionA.get(expansionA.size()-1), false);
        }else if(node instanceof Tag) {
            sb.append("Tag"+((Tag)node).getContent());
            traverseNodes(sb, paddingForBoth, pointerRight, ((Tag)node).getChildExpansion(), false);
        }else if(node instanceof OptionalGrouping) {
            sb.append("OptionalGrouping");
            traverseNodes(sb, paddingForBoth, pointerRight, ((OptionalGrouping)node).getChildExpansion(), false);
        }else if(node instanceof RequiredGrouping) {
            sb.append("RequiredGrouping");
            traverseNodes(sb, paddingForBoth, pointerRight, ((RequiredGrouping)node).getChildExpansion(), false);
        }else if(node instanceof PlusOperator) {
            sb.append("PlusOperator");
            traverseNodes(sb, paddingForBoth, pointerRight, ((PlusOperator)node).getChildExpansion(), false);
        }else if(node instanceof KleeneStar) {
            sb.append("KleeneStar");
            traverseNodes(sb, paddingForBoth, pointerRight, ((KleeneStar)node).getChildExpansion(), false);
        }    
    }

    public static void printTreeHorizontal(Expansion root) throws UnsupportedEncodingException {
        System.out.print(traversePreOrder(root));
    }
	
	
	public static void ceshi() throws IOException {		
		int num=0;//统计命中个数
		long time = System.currentTimeMillis();
		System.out.println("开始加载");
		//Grammar testGram = Grammar.parseGrammarFromString(readfile("D:\\CODE\\jsgf_new\\data\\1\\G1Reg.jsgf"));//加载、解析语法
		Grammar testGram = Grammar.parseGrammarFromString(readfile("src/main/test/G1副本.jsgf"));//加载、解析语法
		long time0 = System.currentTimeMillis();
		System.out.println("加载完成："+(int) ((time0 - time))+"毫秒！");
		
		//这里绘制解析出来的语法树
		List<Rule> rules=testGram.getRules();
		for (int i=0;i<rules.size();i+=1) {
			//printTreeHorizontal(rules.get(i).getChildExpansion());
		}
		
		System.out.println("开始匹配");
		long time1 = System.currentTimeMillis();
		
		List<Rule> tmp=testGram.getMatchingRule("收 起 来 尾 气 防 护 功 能");//单句测试
		if (tmp != null){
			num+=1;
			for(int i=0;i<tmp.size();i++) {
				System.out.println(tmp.get(i).name);//打印domain和intent	
			}
		}

		System.out.println("匹配结束");
		long time2 = System.currentTimeMillis();
        System.out.println("执行了："+(int) ((time2 - time1))+"毫秒！");
        System.out.println("命中："+num);
	}
}
