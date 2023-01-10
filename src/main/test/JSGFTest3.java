import ca.l5.expandingdev.jsgf.AlternativeSet;
import ca.l5.expandingdev.jsgf.Expansion;
import ca.l5.expandingdev.jsgf.Grammar;
import ca.l5.expandingdev.jsgf.KleeneStar;
import ca.l5.expandingdev.jsgf.OptionalGrouping;
import ca.l5.expandingdev.jsgf.PlusOperator;
import ca.l5.expandingdev.jsgf.RequiredGrouping;
import ca.l5.expandingdev.jsgf.Rule;
import ca.l5.expandingdev.jsgf.Sequence;
import ca.l5.expandingdev.jsgf.Slot;
import ca.l5.expandingdev.jsgf.Tag;
import ca.l5.expandingdev.jsgf.TemplateSubMatched;
import ca.l5.expandingdev.jsgf.Token;
import ca.l5.expandingdev.jsgf.Wildcard;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


public class JSGFTest3 {	
    public static void main(String[] args) throws IOException {
        long time1 = System.currentTimeMillis();
        Grammar testGram = Grammar.parseGrammarFromString(readfile("src/main/test/G1新模板.jsgf"));
        long time2 = System.currentTimeMillis();
        System.out.println("加载模板文件耗时："+(time2-time1)+"ms");
        System.out.println("grammar中共有"+testGram.getRules().size()+"条rule");
        Grammar.ahoCor_pre();
        Grammar.key2IndexMap = loadKeyWord2IndexFile();

        /*
		//这里绘制解析出来的语法树
		List<Rule> rules=testGram.getRules();
		for (int i=0;i<rules.size();i+=1) {
			//printTreeHorizontal(rules.get(i).getChildExpansion());
		}*/
		
        String testQuery = "冷 死 我 了";
        List<TemplateSubMatched> return_results = testGram.getMatchingRule(testQuery);
        if(return_results != null && return_results.size()>0){
            for(TemplateSubMatched ts:return_results){
            	Rule rule = ts.getUtterance();
                List<Slot> slots = ts.getSlots();
                System.out.println(rule.getRuleString()+" "+slots);
            }
        }else{
        	System.out.println(testQuery);//打印匹配失败的原句
        }

        //Rule rule1 = testGram.getRules().get(0);
        //System.out.println(rule1.getRuleString());
        //System.out.println(rule1.getChildExpansion().getString());
		

        /*try {
			Thread.sleep(67000);//为了等待VisualVM的连接，对于0.1数据，需要等68秒。0.25数据，需要等65秒。0.5数据，需要等67秒,1数据需要等待67秒
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
        
        /*
        long[] timearr=new long[11];
        String[] sentences=readfile("src/main/test/sentence_test0排序.txt").split("\n");
        String[] t= new String[2];
        String[] s= new String[sentences.length];
        for(int i=0;i<sentences.length;i++) {
            t=sentences[i].split(":");
            s[i]=t[0];
        }
        System.out.println("开始匹配");
        long time11 = System.currentTimeMillis();
        int num = 0;
        //循环测试文件里的句子
        for(int i=0;i<sentences.length;i++) {
        	List<TemplateSubMatched> return_results = testGram.getMatchingRule(s[i]);
        	if(return_results != null && return_results.size()>0){
                for(TemplateSubMatched ts:return_results){
            		Rule rule = ts.getUtterance();
                	List<Slot> slots = ts.getSlots();
                	System.out.println(rule.getRuleString()+" "+slots);
                }
                num+=1;
            }else{
            	System.out.println(s[i]);//打印匹配失败的原句
            }
            if (i%100==0){
                long timet = System.currentTimeMillis();
                timearr[i/100]=timet;
            }
        }
		
        System.out.println("匹配结束");
        long time22 = System.currentTimeMillis();
        System.out.println("执行了："+(int) ((time22 - time11))+"毫秒！");
        System.out.println("命中："+num);

        for (int i=0;i<timearr.length;i+=1) {
            System.out.println("第"+(i*100+1)+"条时执行了："+((int)(timearr[i]-time11))+"毫秒！");
        }

        /*try {
			Thread.sleep(67000);//为了等待VisualVM的连接，对于0.1数据，需要等68秒。0.25数据，需要等65秒。0.5数据，需要等67秒,1数据需要等待67秒
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/       
    }
    
    public static String readfile(String file) throws IOException {//读文件，所有内容拼成一个字符串
        BufferedReader in = new BufferedReader(new FileReader(file));
        String str;
        //String re = "";
        StringBuilder stringBuilder = new StringBuilder();
        while ((str = in.readLine()) != null) {

           /* re += str;
            re += "\n";*/
            stringBuilder.append(str);
            stringBuilder.append("\n");
        }
        in.close();
        return stringBuilder.toString();
    }

    
    public static Map<String, Set<Integer>> loadKeyWord2IndexFile(){
        String ahoCorFilePath = "src/main/test/G1副本_key2index2.txt";
        SortedMap<String, Set<Integer>> key2IndexMap = new TreeMap();
        String line;
        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(ahoCorFilePath), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                Set<Integer> indexSet = new HashSet<>();
                // 用英文冒号隔开，每个index用英文逗号隔开
                String[] keywordAndIndex = line.split(":");
                String key = keywordAndIndex[0];
                String indexListStr = keywordAndIndex[1];
                String[] indexArray = indexListStr.split(",");
                for(String s:indexArray){
                    if(s!=null&&isNumeric(s)){
                        int index = Integer.parseInt(s);
                        indexSet.add(index);
                    }
                }
                key2IndexMap.put(key, indexSet);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return key2IndexMap;
    }

    /**
     * 判断字符串是否为数字
     * @param str
     * @return
     */
    public static boolean isNumeric(String str) {
        for (int i = str.length(); --i >= 0; ) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
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
    
}
