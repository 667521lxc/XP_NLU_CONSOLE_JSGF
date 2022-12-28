import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import ca.l5.expandingdev.jsgf.Grammar;
import ca.l5.expandingdev.jsgf.Rule;

public class JSGFTest {	
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
	
	public static void ceshi() throws IOException {
		int num=0;//统计命中个数
		long[] timearr=new long[11];
		long time = System.currentTimeMillis();
		System.out.println("开始加载");
		//Grammar testGram = Grammar.parseGrammarFromString(readfile("D:\\CODE\\jsgf_new\\data\\0.25\\G1Reg.jsgf"));//加载、解析语法
		Grammar testGram = Grammar.parseGrammarFromString(readfile("src/main/test/G1_L.jsgf"));//加载、解析语法
		long time0 = System.currentTimeMillis();
		System.out.println("加载完成："+(int) ((time0 - time))+"毫秒！");
		
		//读取并预处理测试文件
		//String[] sentences=readfile("D:\\CODE\\jsgf_new\\data\\1\\sentence_test2 - 副本.txt").split("\n");
		String[] sentences=readfile("src/main/test/sentence_test2.txt").split("\n");//干掉全部阿拉伯数字，变成汉字
		String[] t= new String[2]; 
		String[] s= new String[sentences.length]; 
		for(int i=0;i<sentences.length;i++) {
			t=sentences[i].split(":");
			s[i]=t[0];
		}
		System.out.println("开始匹配");
		long time1 = System.currentTimeMillis();
		
		//循环测试文件里的句子
		for(int i=0;i<sentences.length;i++) {
			testGram.clear_result();
			List<Rule> tmp=testGram.getMatchingRule(s[i]);
			if (i%100==0){
			 	long timet = System.currentTimeMillis();
				timearr[i/100]=timet;	
			}
			if (tmp != null){
				//for (int j=0;j<tmp.size();j+=1) {
					//System.out.println(s[i]);//打印匹配成功的原句
					//System.out.println(tmp.name);//打印domain和intent
				//}
				num+=1;
			}else {
				System.out.println(s[i]);//打印匹配失败的原句
			}
			tmp=null;//临时变量
		}

		System.out.println("匹配结束");		
		long time2 = System.currentTimeMillis();
        System.out.println("执行了："+(int) ((time2 - time1))+"毫秒！");
        System.out.println("命中："+num);
        
        for (int i=0;i<timearr.length;i+=1) {
        	System.out.println("第"+(i*100+1)+"条时执行了："+((int)(timearr[i]-time1))+"毫秒！");
        } 	
        	
	}

	
	public static void main(String[] args) throws IOException {
		JSGFTest2.ceshi();
		/*try {
			Thread.sleep(67000);//为了等待VisualVM的连接，对于0.1数据，需要等68秒。0.25数据，需要等65秒。0.5数据，需要等67秒,1数据需要等待67秒
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
}
