package ca.l5.expandingdev.trie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import ca.l5.expandingdev.trie.PayloadTrie.PayloadTrieBuilder;

public class Test {

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
    
	public static void main(String[] args) throws IOException {	
        //dict = loadFileToMap("D:/CODE/aho-corasick-master/jsgf/char.txt");
        //dict2 = reverse(dict);
        PayloadTrieBuilder<AcSlot> trie_builder = PayloadTrie.<AcSlot>builder();
        File file = new File("D:/CODE/aho-corasick-master/jsgf");
        HashMap<String, ArrayList<String>> slots_list = new HashMap<String, ArrayList<String>>();
        if(file.isDirectory()) {
            String[] list = file.list();
            for(int i=0;i<list.length;i++) {
            	String slot=list[i].substring(0,list[i].length()-4);
                String file2 = "D:/CODE/aho-corasick-master/jsgf/"+list[i];
                String[] slots=readfile(file2).split("\n");
                for(int j=0;j<slots.length;j++) {
                    //slots[j]是词语,slot是keyword.txt名字
                    if(slots_list.containsKey(slots[j])) {
                    	ArrayList<String> slot_tmp = slots_list.get(slots[j]);
                    	slot_tmp.add(slot);
                    	slots_list.put(slots[j], slot_tmp);
                    } else {
                    	ArrayList<String> slot_tmp=new ArrayList<String>();
                    	slot_tmp.add(slot);
                    	slots_list.put(slots[j], slot_tmp);
                    }
                }
            }
        }       
        
        System.out.println("读完文件，开始加载");
        long time0 = System.currentTimeMillis();
        
        for (Entry<String, ArrayList<String>> entry : slots_list.entrySet()) {
        	String vacab = entry.getKey();
        	ArrayList<String> slot = entry.getValue();
        	if (slot.size()==1) {
        		trie_builder.addKeyword(vacab, new AcSlot(slot.get(0)));
      		
        	} else {
        		trie_builder.addKeyword(vacab, new AcSlot(slot));
	
        	}
        }	 
        PayloadTrie<AcSlot> trie = trie_builder.build();
		long time1 = System.currentTimeMillis();
		System.out.println("加载完成："+(int) ((time1 - time0))+"毫秒！");
		
		/*try {
			Thread.sleep(7000);//为了等待VisualVM的连接，对于0.1数据，需要等68秒。0.25数据，需要等65秒。0.5数据，需要等67秒,1数据需要等待67秒
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        for(int j=0;j<100000000;j++) {
        	trie.parseText("打开左右车窗");
        }//*/
		
		Collection<PayloadEmit<AcSlot>> emits = trie.parseText("打开车窗");
		long time2 = System.currentTimeMillis();
		System.out.println("匹配完成："+(int) ((time2 - time1))+"毫秒！");
		
		Iterator<PayloadEmit<AcSlot>> iterator = emits.iterator();
        while(iterator.hasNext()) {
            System.out.println(iterator.next().toString());
        }
	}

}
