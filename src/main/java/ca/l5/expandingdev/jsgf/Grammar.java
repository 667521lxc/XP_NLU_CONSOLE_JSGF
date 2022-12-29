package ca.l5.expandingdev.jsgf;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

import ca.l5.expandingdev.trie.AcSlot;
import ca.l5.expandingdev.trie.PayloadEmit;
import ca.l5.expandingdev.trie.PayloadTrie;
import ca.l5.expandingdev.trie.PayloadTrie.PayloadTrieBuilder;

public class Grammar {
    public static final String specialCharacterRegex = "[;=<>*+\\[\\]()|{} ]";
    public static Map<String, Short> dict = new HashMap<>();
    public static Map<Short, String> dict2 = new HashMap<>();
    public static Set<String> set = new HashSet<>();
    public static PayloadTrie<AcSlot> trie;
    public static Map<String, Set<Integer>> key2IndexMap;

    public String name;
    private List<Rule> rules; //返回规则
    private List<Import> imports; //这个用处不大
    private GrammarHeader header;

    public Grammar() {
        header = new GrammarHeader();
        rules = new ArrayList<Rule>();
        imports = new ArrayList<Import>();
        name = "default";
    }

    public Grammar(String n) {
        header = new GrammarHeader();
        rules = new ArrayList<Rule>();
        imports = new ArrayList<Import>();
        name = n;
    }

    /**
     * 从这里开始到下面是匹配部分，把query分割成子词，拿这个子词词组来跟模板匹配，只要最后每一个query子词都能命中模板，即整个query命中模板。那么就匹配成功
     * @throws UnsupportedEncodingException
     */
    public List<MatchInfo> getMatchingExpansions(Expansion e, String[] words, int wordPosition) throws UnsupportedEncodingException {//递归
        //System.out.println(e.getClass());
        List<MatchInfo> matchList = new ArrayList<>();
        if (e instanceof Token) {
            Token t = (Token) e;
            short textIdx = (short) t.getText();
            String ts = "";

            if(dict2.containsKey(textIdx)){
                ts = dict2.get(textIdx);
            }else{
                ts = "####";
                System.out.println("无法从dict2中获取对应的字符，textIdx："+textIdx);
            }

            //System.out.println(ts);
            if (ts.equals(words[wordPosition])) {
                matchList.add(new MatchInfo(t, words[wordPosition]));
            } else {
                // No match
            }
        } else if (e instanceof Wildcard) {//通配符W,这里作废，W通配符不会走到这里来
            Wildcard t = (Wildcard) e;
            int minl=t.getMin();
            int maxl=t.getMax();
            int tmpWP=wordPosition;
            
            while(words[wordPosition].equals("W")) {
            //while(isWildChar(words[wordPosition].charAt(0))) {//发现一个W字符，说明有匹配的可能，能否匹配还要继续往后看一共出现了多少次
                wordPosition+=1;
                if (wordPosition > words.length - 1) {
                    break;
                }
            }
            //System.out.println(minl+" "+maxl+" "+tmpWP+" "+wordPosition+" "+words[wordPosition]);//此时wordposition指向第一个不是W的字符
            if ((wordPosition-tmpWP)>maxl || (wordPosition-tmpWP)<minl) {
                wordPosition=tmpWP;//匹配错了，回溯
            }else if(wordPosition==tmpWP && minl==0){
                matchList.add(new MatchInfo(t, ""));
            }else {
                for (int j=tmpWP;j<wordPosition;j+=1) {
                    matchList.add(new MatchInfo(t, words[j]));
                }
            }
        } else if (e instanceof OptionalGrouping) {
            OptionalGrouping og = (OptionalGrouping) e;
            List<MatchInfo> m1 = this.getMatchingExpansions(og.getChildExpansion(), words, wordPosition);
            if (m1.size() == 0) {
                // Optional, so it can match. Used for sequences
                matchList.add(new MatchInfo(e, ""));
            } else {
                //Matches
                matchList.add(new MatchInfo(e, ""));
                //matchList.addAll(this.getMatchingExpansions(og.getChildExpansion(), words, wordPosition));
                matchList.addAll(m1);
            }
        } else if (e instanceof RequiredGrouping) {
            RequiredGrouping rg = (RequiredGrouping) e;
            List<MatchInfo> m1 = (this.getMatchingExpansions(rg.getChildExpansion(), words, wordPosition));

            if (m1.size() != 0) {
                matchList.add(new MatchInfo(e, ""));
                matchList.addAll(m1);
            }
        } else if (e instanceof Tag) {//这里进行修改
            Tag t = (Tag) e;
            int wp_end = wordPosition;
            List<MatchInfo> m1 = this.getMatchingExpansions(t.getChildExpansion(), words, wordPosition);
            for (MatchInfo localMatch : m1) {
                if (!localMatch.getMatchingStringSection().equals("")) {
                    wp_end += localMatch.getMatchingStringSection().length();
                }
            }
            if (m1.size() != 0) {
                t.setWp(wordPosition, wp_end);//给Tag增加一个wordPosition分量
                matchList.add(new MatchInfo(t, ""));//
                matchList.addAll(m1); // Found a match! Add it to the list
            }
        } else if (e instanceof AlternativeSet) {
            AlternativeSet as = (AlternativeSet) e;
            for (Expansion x : as.getChildExpansions()) {
                List<MatchInfo> m1 = this.getMatchingExpansions(x, words, wordPosition);

                if ((x instanceof KleeneStar || x instanceof OptionalGrouping) && m1.size() == 0) { // Stupid OptionalGrouping
                    continue;  //这一行似乎没啥用
                }

                if (m1.size() != 0) {
                    matchList.add(new MatchInfo(e, ""));
                    matchList.addAll(m1); // Found a match! Add it to the list
                    break;//只要有一个命中就行了
                }
            }
        } else if (e instanceof Sequence) {
            Sequence seq = (Sequence) e;
            /*for (Object o : seq) {
            	System.out.println("lk  "+o.getClass());
            }*/
            List<MatchInfo> localMatchList = new ArrayList<>();
            int matchedCount = 0;
            for (Object o : seq) {
                Expansion x = (Expansion) o;
                if ((wordPosition > words.length - 1) && ((x instanceof KleeneStar) || (x instanceof OptionalGrouping))) { // Sequence is longer than provided words! Abort!
                    matchedCount++;
                    continue;
                } else if((wordPosition > words.length - 1) && (x instanceof Wildcard) && (((Wildcard)x).getMin()==0)) {
                    matchedCount++;
                    continue;
                }else if (wordPosition > words.length - 1) { // Sequence is longer than provided words! Abort!
                    //System.out.println("wordPosition:"+wordPosition+" "+(words.length - 1));
                    break;
                }
                List<MatchInfo> m1 = this.getMatchingExpansions(x, words, wordPosition);
                if (m1.size() == 0 && (x instanceof KleeneStar || x instanceof OptionalGrouping)) {
                    matchedCount++; // Still counts a match
                    continue;
                }

                if (m1.size() != 0) {
                    matchedCount++;
                    //System.out.println("m1.size:"+m1.size()+" "+matchedCount);
                    for (MatchInfo localMatch : m1) {
                        if (!localMatch.getMatchingStringSection().equals("")) {
                            wordPosition += localMatch.getMatchingStringSection().split(" ").length;
                        }
                    }
                    localMatchList.addAll(m1); // Found a match! Add it to the list
                } else { // Doesn't match! Sequence aborted.
                    localMatchList.clear();
                    break;
                }
            }
            //System.out.println(matchedCount+":"+seq.size());
            if (matchedCount != seq.size()) { // Not all of the required matches were met! 未满足所有要求的匹配！
                localMatchList.clear();
            }

            if (localMatchList.size() != 0) { //上面如果不匹配，就给清空了
                matchList.add(new MatchInfo(e, ""));
                matchList.addAll(localMatchList);
            }
        } else if (e instanceof KleeneStar) {//*号
            KleeneStar ks = (KleeneStar) e;
            boolean done = false;
            List<MatchInfo> m1;
            matchList.add(new MatchInfo(e, ""));
            while (!done) {
                if (wordPosition > words.length - 1) {
                    break;
                }
                m1 = this.getMatchingExpansions(ks.getChildExpansion(), words, wordPosition);
                if (m1.size() == 0) {
                    // No matches
                    done = true;
                } else {
                    //Matches
                    for (MatchInfo mi2 : m1) {
                        if(!mi2.getMatchingStringSection().equals("")) {
                            //System.out.println("* "+mi2.getMatchingStringSection());
                            wordPosition += mi2.getMatchingStringSection().split(" ").length;
                        }
                    }
                    matchList.addAll(m1);
                    matchList.add(new MatchInfo(e, ""));
                }
            }
        } else if (e instanceof PlusOperator) {//+号
            PlusOperator po = (PlusOperator) e;
            boolean done = false;
            List<MatchInfo> m1;
            while (!done) {
                if (wordPosition > words.length - 1) {
                    break;
                }
                m1 = this.getMatchingExpansions(po.getChildExpansion(), words, wordPosition);
                if (m1.size() == 0) {
                    // No matches
                    done = true;
                } else {
                    //Matches
                    matchList.add(new MatchInfo(e, ""));
                    for (MatchInfo mi2 : m1) {
                        if (!mi2.getMatchingStringSection().equals("")) {
                            //System.out.println("+ "+mi2.getMatchingStringSection());
                            wordPosition += mi2.getMatchingStringSection().split(" ").length;
                        }
                    }
                    matchList.addAll(m1);
                }
            }
        }
        /*for (MatchInfo mi2 : matchList) {
        	System.out.println(matchList+":"+mi2.getMatchingStringSection());
        }*/
        return matchList;
    }

    public List<MatchInfo> getMatchingExpansions_w(Expansion e, int wl, String[] words, int wordPosition) throws UnsupportedEncodingException {//单通配符
        List<MatchInfo> matchList = new ArrayList<>();
        if (e instanceof Token) {
            Token t = (Token) e;
            short textIdx = (short) t.getText();
            String ts = "";

            if(dict2.containsKey(textIdx)){
                ts = dict2.get(textIdx);
            }else{
                ts = "####";
                System.out.println("无法从dict2中获取对应的字符，textIdx："+textIdx);
            }

            //System.out.println(ts);
            if (ts.equals(words[wordPosition])) {
                matchList.add(new MatchInfo(t, words[wordPosition]));
            } else {
                // No match
            }
        } else if (e instanceof Wildcard) {//通配符W
            Wildcard t = (Wildcard) e;
            if (wordPosition + wl <= words.length) {
                matchList.add(new MatchInfo(t, ""));
                for (int j = 0; j < wl; j += 1) {
                    matchList.add(new MatchInfo(t, words[wordPosition+j]));
                }
            }
        } else if (e instanceof OptionalGrouping) {
            OptionalGrouping og = (OptionalGrouping) e;
            List<MatchInfo> m1 = this.getMatchingExpansions_w(og.getChildExpansion(), wl, words, wordPosition);
            if (m1.size() == 0) {
                // Optional, so it can match. Used for sequences
                matchList.add(new MatchInfo(e, ""));
            } else {
                //Matches
                matchList.add(new MatchInfo(e, ""));
                //matchList.addAll(this.getMatchingExpansions(og.getChildExpansion(), words, wordPosition));
                matchList.addAll(m1);
            }
        } else if (e instanceof RequiredGrouping) {
            RequiredGrouping rg = (RequiredGrouping) e;
            List<MatchInfo> m1 = (this.getMatchingExpansions_w(rg.getChildExpansion(), wl, words, wordPosition));

            if (m1.size() != 0) {
                matchList.add(new MatchInfo(e, ""));
                matchList.addAll(m1);
            }
        } else if (e instanceof Tag) {//这里进行修改
            Tag t = (Tag) e;
            int wp_end = wordPosition;
            List<MatchInfo> m1 = this.getMatchingExpansions_w(t.getChildExpansion(), wl, words, wordPosition);
            for (MatchInfo localMatch : m1) {
                if (!localMatch.getMatchingStringSection().equals("")) {
                    wp_end += localMatch.getMatchingStringSection().length();
                }
            }
            if (m1.size() != 0) {
                t.setWp(wordPosition, wp_end);//给Tag增加一个wordPosition分量
                matchList.add(new MatchInfo(t, ""));//
                matchList.addAll(m1); // Found a match! Add it to the list
            }
        } else if (e instanceof AlternativeSet) {
            AlternativeSet as = (AlternativeSet) e;
            for (Expansion x : as.getChildExpansions()) {
                List<MatchInfo> m1 = this.getMatchingExpansions_w(x, wl, words, wordPosition);

                if ((x instanceof KleeneStar || x instanceof OptionalGrouping) && m1.size() == 0) { // Stupid OptionalGrouping
                    continue;  //这一行似乎没啥用
                }

                if (m1.size() != 0) {
                    matchList.add(new MatchInfo(e, ""));
                    matchList.addAll(m1); // Found a match! Add it to the list
                    break;//只要有一个命中就行了
                }
            }
        } else if (e instanceof Sequence) {
            Sequence seq = (Sequence) e;
            /*for (Object o : seq) {
            	System.out.println(wl+"lk  "+o.getClass());
            }*/
            List<MatchInfo> localMatchList = new ArrayList<>();
            int matchedCount = 0;
            for (Object o : seq) {//检查是否要提前终止
                Expansion x = (Expansion) o;                
                if ((wordPosition > words.length - 1) && ((x instanceof KleeneStar) || (x instanceof OptionalGrouping))) { // Sequence is longer than provided words! Abort!
                    matchedCount++;
                    continue;
                } else if((wordPosition > words.length - 1) && (x instanceof Tag)) {
                	Expansion xx = ((Tag) x).getChildExpansion();  
                	if ((xx instanceof Wildcard) && (((Wildcard)xx).getMin()==0)) {
                        matchedCount++;
                        continue;	
                	}
                }else if (wordPosition > words.length - 1) { // Sequence is longer than provided words! Abort!
                    //System.out.println("wordPosition:"+wordPosition+" "+(words.length - 1));
                    break;
                }
                List<MatchInfo> m1 = this.getMatchingExpansions_w(x, wl, words, wordPosition);
                if (m1.size() == 0 && (x instanceof KleeneStar || x instanceof OptionalGrouping)) {
                    matchedCount++; // Still counts a match
                    continue;
                }

                if (m1.size() != 0) {
                    matchedCount++;
                    //System.out.println("m1.size:"+m1.size()+" "+matchedCount);
                    for (MatchInfo localMatch : m1) {
                        if (!localMatch.getMatchingStringSection().equals("")) {
                            wordPosition += localMatch.getMatchingStringSection().split(" ").length;
                        }
                    }
                    localMatchList.addAll(m1); // Found a match! Add it to the list
                } else { // Doesn't match! Sequence aborted.
                    localMatchList.clear();
                    break;
                }
            }
            //System.out.println(matchedCount+":"+seq.size());
            if (matchedCount != seq.size()) { // Not all of the required matches were met! 未满足所有要求的匹配！
                localMatchList.clear();
            }

            if (localMatchList.size() != 0) { //上面如果不匹配，就给清空了
                matchList.add(new MatchInfo(e, ""));
                matchList.addAll(localMatchList);
            }
        } else if (e instanceof KleeneStar) {//*号
            KleeneStar ks = (KleeneStar) e;
            boolean done = false;
            List<MatchInfo> m1;
            matchList.add(new MatchInfo(e, ""));
            while (!done) {
                if (wordPosition > words.length - 1) {
                    break;
                }
                m1 = this.getMatchingExpansions_w(ks.getChildExpansion(), wl, words, wordPosition);
                if (m1.size() == 0) {
                    // No matches
                    done = true;
                } else {
                    //Matches
                    for (MatchInfo mi2 : m1) {
                        if(!mi2.getMatchingStringSection().equals("")) {
                            //System.out.println("* "+mi2.getMatchingStringSection());
                            wordPosition += mi2.getMatchingStringSection().split(" ").length;
                        }
                    }
                    matchList.addAll(m1);
                    matchList.add(new MatchInfo(e, ""));
                }
            }
        } else if (e instanceof PlusOperator) {//+号
            PlusOperator po = (PlusOperator) e;
            boolean done = false;
            List<MatchInfo> m1;
            while (!done) {
                if (wordPosition > words.length - 1) {
                    break;
                }
                m1 = this.getMatchingExpansions_w(po.getChildExpansion(), wl, words, wordPosition);
                if (m1.size() == 0) {
                    // No matches
                    done = true;
                } else {
                    //Matches
                    matchList.add(new MatchInfo(e, ""));
                    for (MatchInfo mi2 : m1) {
                        if (!mi2.getMatchingStringSection().equals("")) {
                            //System.out.println("+ "+mi2.getMatchingStringSection());
                            wordPosition += mi2.getMatchingStringSection().split(" ").length;
                        }
                    }
                    matchList.addAll(m1);
                }
            }
        }
        /*for (MatchInfo mi2 : matchList) {
        	System.out.println(matchList+":"+mi2.getMatchingStringSection());
        }*/
        return matchList;
    }

    public List<MatchInfo> getMatchingExpansions_ww(Expansion e, List<Integer> wl, String[] words, int wordPosition) throws UnsupportedEncodingException {//单通配符
        //System.out.println(e.getClass());
        List<MatchInfo> matchList = new ArrayList<>();
        if (e instanceof Token) {
            Token t = (Token) e;
            short textIdx = (short) t.getText();
            String ts = "";

            if(dict2.containsKey(textIdx)){
                ts = dict2.get(textIdx);
            }else{
                ts = "####";
                System.out.println("无法从dict2中获取对应的字符，textIdx："+textIdx);
            }

            //System.out.println(ts);
            if (ts.equals(words[wordPosition])) {
                matchList.add(new MatchInfo(t, words[wordPosition]));
            } else {
                // No match
            }
        } else if (e instanceof Wildcard) {//通配符W
            Wildcard t = (Wildcard) e;
            int wll = wl.get(0);
            wl.remove(0);
            if (wordPosition + wll <= words.length) {
                matchList.add(new MatchInfo(t, ""));
                for (int j = 0; j < wll; j += 1) {
                    matchList.add(new MatchInfo(t, words[wordPosition+j]));
                }
            } 
        } else if (e instanceof OptionalGrouping) {
            OptionalGrouping og = (OptionalGrouping) e;
            List<MatchInfo> m1 = this.getMatchingExpansions_ww(og.getChildExpansion(), wl, words, wordPosition);
            if (m1.size() == 0) {
                // Optional, so it can match. Used for sequences
                matchList.add(new MatchInfo(e, ""));
            } else {
                //Matches
                matchList.add(new MatchInfo(e, ""));
                //matchList.addAll(this.getMatchingExpansions(og.getChildExpansion(), words, wordPosition));
                matchList.addAll(m1);
            }
        } else if (e instanceof RequiredGrouping) {
            RequiredGrouping rg = (RequiredGrouping) e;
            List<MatchInfo> m1 = (this.getMatchingExpansions_ww(rg.getChildExpansion(), wl, words, wordPosition));

            if (m1.size() != 0) {
                matchList.add(new MatchInfo(e, ""));
                matchList.addAll(m1);
            }
        } else if (e instanceof Tag) {//这里进行修改
            Tag t = (Tag) e;
            int wp_end = wordPosition;
            List<MatchInfo> m1 = this.getMatchingExpansions_ww(t.getChildExpansion(), wl, words, wordPosition);
            for (MatchInfo localMatch : m1) {
                if (!localMatch.getMatchingStringSection().equals("")) {
                    wp_end += localMatch.getMatchingStringSection().length();
                }
            }
            if (m1.size() != 0) {
                t.setWp(wordPosition, wp_end);//给Tag增加一个wordPosition分量
                matchList.add(new MatchInfo(t, ""));//
                matchList.addAll(m1); // Found a match! Add it to the list
            }
        } else if (e instanceof AlternativeSet) {
            AlternativeSet as = (AlternativeSet) e;
            for (Expansion x : as.getChildExpansions()) {
                List<MatchInfo> m1 = this.getMatchingExpansions_ww(x, wl, words, wordPosition);

                if ((x instanceof KleeneStar || x instanceof OptionalGrouping) && m1.size() == 0) { // Stupid OptionalGrouping
                    continue;  //这一行似乎没啥用
                }

                if (m1.size() != 0) {
                    matchList.add(new MatchInfo(e, ""));
                    matchList.addAll(m1); // Found a match! Add it to the list
                    break;//只要有一个命中就行了
                }
            }
        } else if (e instanceof Sequence) {
            Sequence seq = (Sequence) e;
            /*for (Object o : seq) {
            	System.out.println("lk  "+o.getClass());
            }*/
            List<MatchInfo> localMatchList = new ArrayList<>();
            int matchedCount = 0;
            for (Object o : seq) {
                Expansion x = (Expansion) o;
                if ((wordPosition > words.length - 1) && ((x instanceof KleeneStar) || (x instanceof OptionalGrouping))) { // Sequence is longer than provided words! Abort!
                    matchedCount++;
                    continue;
                } else if((wordPosition > words.length - 1) && (x instanceof Tag)) {
                	Expansion xx = ((Tag) x).getChildExpansion();  
                	if ((xx instanceof Wildcard) && (((Wildcard)xx).getMin()==0)) {
                        matchedCount++;
                        continue;	
                	}
                }else if (wordPosition > words.length - 1) { // Sequence is longer than provided words! Abort!
                    //System.out.println("wordPosition:"+wordPosition+" "+(words.length - 1));
                    break;
                }
                List<MatchInfo> m1 = this.getMatchingExpansions_ww(x, wl, words, wordPosition);
                if (m1.size() == 0 && (x instanceof KleeneStar || x instanceof OptionalGrouping)) {
                    matchedCount++; // Still counts a match
                    continue;
                }

                if (m1.size() != 0) {
                    matchedCount++;
                    //System.out.println("m1.size:"+m1.size()+" "+matchedCount);
                    for (MatchInfo localMatch : m1) {
                        if (!localMatch.getMatchingStringSection().equals("")) {
                            wordPosition += localMatch.getMatchingStringSection().split(" ").length;
                        }
                    }
                    localMatchList.addAll(m1); // Found a match! Add it to the list
                } else { // Doesn't match! Sequence aborted.
                    localMatchList.clear();
                    break;
                }
            }
            //System.out.println(matchedCount+":"+seq.size());
            if (matchedCount != seq.size()) { // Not all of the required matches were met! 未满足所有要求的匹配！
                localMatchList.clear();
            }

            if (localMatchList.size() != 0) { //上面如果不匹配，就给清空了
                matchList.add(new MatchInfo(e, ""));
                matchList.addAll(localMatchList);
            }
        } else if (e instanceof KleeneStar) {//*号
            KleeneStar ks = (KleeneStar) e;
            boolean done = false;
            List<MatchInfo> m1;
            matchList.add(new MatchInfo(e, ""));
            while (!done) {
                if (wordPosition > words.length - 1) {
                    break;
                }
                m1 = this.getMatchingExpansions_ww(ks.getChildExpansion(), wl, words, wordPosition);
                if (m1.size() == 0) {
                    // No matches
                    done = true;
                } else {
                    //Matches
                    for (MatchInfo mi2 : m1) {
                        if(!mi2.getMatchingStringSection().equals("")) {
                            //System.out.println("* "+mi2.getMatchingStringSection());
                            wordPosition += mi2.getMatchingStringSection().split(" ").length;
                        }
                    }
                    matchList.addAll(m1);
                    matchList.add(new MatchInfo(e, ""));
                }
            }
        } else if (e instanceof PlusOperator) {//+号
            PlusOperator po = (PlusOperator) e;
            boolean done = false;
            List<MatchInfo> m1;
            while (!done) {
                if (wordPosition > words.length - 1) {
                    break;
                }
                m1 = this.getMatchingExpansions_ww(po.getChildExpansion(), wl, words, wordPosition);
                if (m1.size() == 0) {
                    // No matches
                    done = true;
                } else {
                    //Matches
                    matchList.add(new MatchInfo(e, ""));
                    for (MatchInfo mi2 : m1) {
                        if (!mi2.getMatchingStringSection().equals("")) {
                            //System.out.println("+ "+mi2.getMatchingStringSection());
                            wordPosition += mi2.getMatchingStringSection().split(" ").length;
                        }
                    }
                    matchList.addAll(m1);
                }
            }
        }
        /*for (MatchInfo mi2 : matchList) {
        	System.out.println(matchList+":"+mi2.getMatchingStringSection());
        }*/
        return matchList;
    }

    public boolean matchesRule(Rule rule, String test) throws UnsupportedEncodingException {//没有通配符
        String[] words = test.split(" ");//要分词
        List<MatchInfo> m1 = this.getMatchingExpansions(rule.getChildExpansion(), words, 0);

        int matchCount = 0;
        for (MatchInfo mi2 : m1) {
            if (!mi2.getMatchingStringSection().equals("")) {
                matchCount++;
            }
        }
        //System.out.println(matchCount);
        if (matchCount == words.length) { //这里匹配成功，我们要找出里面的Tag
            for (MatchInfo mi2 : m1) {
                Expansion tmpt = mi2.getMatchedExpansion();
                if (tmpt instanceof Tag) {
                    String tag = ((Tag) tmpt).getTags()[0];
                    int wp = ((Tag) tmpt).getWp();
                    int wpe = ((Tag) tmpt).getWpE();
                    ReTags retag = new ReTags(tag, wp, wpe);
                    rule.re_tags.add(retag);
                }
            }
        }
        return matchCount == words.length; // Must match all the words!
    }

    public boolean matchesRule_w(Rule rule, int wl, String test) throws UnsupportedEncodingException {//一个通配符
        String[] words = test.split(" ");//要分词
        List<MatchInfo> m1 = this.getMatchingExpansions_w(rule.getChildExpansion(), wl, words, 0);

        int matchCount = 0;
        for (MatchInfo mi2 : m1) {
            if (!mi2.getMatchingStringSection().equals("")) {
                matchCount++;
            }
        }
        //System.out.println(matchCount);
        if (matchCount == words.length) { //这里匹配成功，我们要找出里面的Tag
            for (MatchInfo mi2 : m1) {
                Expansion tmpt = mi2.getMatchedExpansion();
                if (tmpt instanceof Tag) {
                    String tag = ((Tag) tmpt).getTags()[0];
                    int wp = ((Tag) tmpt).getWp();
                    int wpe = ((Tag) tmpt).getWpE();
                    ReTags retag = new ReTags(tag, wp, wpe);
                    rule.re_tags.add(retag);
                }
            }
        }
        return matchCount == words.length; // Must match all the words!
    }
  
    public boolean matchesRule_ww(Rule rule, List<Integer> wl, String test) throws UnsupportedEncodingException {//两个通配符
        String[] words = test.split(" ");//要分词
        List<MatchInfo> m1 = this.getMatchingExpansions_ww(rule.getChildExpansion(), wl, words, 0);

        int matchCount = 0;
        for (MatchInfo mi2 : m1) {
            if (!mi2.getMatchingStringSection().equals("")) {
                matchCount++;
            }
        }
        //System.out.println(matchCount);
        if (matchCount == words.length) { //这里匹配成功，我们要找出里面的Tag
            for (MatchInfo mi2 : m1) {
                Expansion tmpt = mi2.getMatchedExpansion();
                if (tmpt instanceof Tag) {
                    String tag = ((Tag) tmpt).getTags()[0];
                    int wp = ((Tag) tmpt).getWp();
                    int wpe = ((Tag) tmpt).getWpE();
                    ReTags retag = new ReTags(tag, wp, wpe);
                    rule.re_tags.add(retag);
                }
            }
        }
        return matchCount == words.length; // Must match all the words!
    }
    
    public List<TemplateSubMatched> getMatchingRule(String test) throws IOException {	
    	List<TemplateSubMatched> re= new ArrayList<TemplateSubMatched>();
    	// 1、执行ac自动机获取词表矩阵
        LinkedHashMap<Integer, List<ACResult>> wordListMatrix = ahoCor(test);
        /*
        System.out.println("------------执行ac自动机获取词表矩阵------------");
        Iterator<Entry<Integer, List<ACResult>>> entries = wordListMatrix.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<Integer, List<ACResult>> entry = entries.next();
            Integer key = entry.getKey();
            List<ACResult> value = entry.getValue();
            System.out.println(key + ":" + value);
        }   
        System.out.println("----------------------------------------------");
        */
        //2、获取rule对应的index，这个写到测试文件里面了Map<String, Set<Integer>> key2IndexMap = getKey2IndexMap();

        //3、遍历矩阵获取word list
        List<List<WordKeyBO>> wordList = getWordList(wordListMatrix);
        /*
        System.out.println("------------遍历矩阵获取word list--------------");
        for (List<WordKeyBO> wl:wordList) {
        	System.out.println(wl);
        }
        System.out.println("----------------------------------------------");
        */
        //4、调用模板初筛方法findUnionSetForWordList
        Set<Integer> ruleIndexFiltered = findUnionSetForWordList(wordList);
        /*
        System.out.println("------------调用模板初筛方法--------------");
        if(ruleIndexFiltered!=null){
            System.out.println("【TemplateQueryMatcherComponent】初筛后需要匹配的模板"+ruleIndexFiltered.size()+"条");
            System.out.println(ruleIndexFiltered);
        }else{
            System.out.println("【TemplateQueryMatcherComponent】初筛后没有需要匹配的模板");
        }
    	*/
        //List<Rule> tmpList = new ArrayList<>();//返回Rule
        //List<List<Slot>> tmp_slot_List = new ArrayList<>();//返回槽位，List<Rule>里面每一个规则对应一个List<Slot>
        String tmp1=new String();
        String tmp2=new String();
        //System.out.println(test);
        for (Rule r : rules) {
        	/*模板初筛if(ruleIndexFiltered == null || !ruleIndexFiltered.contains(r.index)) {
        		continue; 
        	}*/
            //System.out.println(r.getRuleString());
        	r.re_tags.clear();
        	r.results.clear();
        	if (!r.isWild) {
                if (matchesRule(r,test)) {
                	for (int i=0;i<r.re_tags.size();i+=1) {//打印抽槽 **************
                        //System.out.println(r.re_tags.get(i).wp+" "+r.re_tags.get(i).wpe);
                        tmp1=r.re_tags.get(i).getString();
                        tmp2=test.substring(2*r.re_tags.get(i).wp,2*r.re_tags.get(i).wpe-1).replaceAll(" ","");
                        List<Integer> ll = new ArrayList<Integer>();
                        ll.add(r.re_tags.get(i).wp);
                        ll.add(r.re_tags.get(i).wpe);
                        //System.out.println(tmp1+"  "+tmp2);
                        Slot tmpr=new Slot(tmp1, tmp2, ll);
                        r.results.add(tmpr);
                    }
                	List<Slot> slot_list = new ArrayList<Slot>();
                	slot_list.addAll(r.results);
                	TemplateSubMatched ts = new TemplateSubMatched(r,slot_list);
                    re.add(ts);
                }	
        	} else { //这里处理通配符，目前一句支持两个通配符
        		if (r.wl.size() == 2) {//一个通配符
            		for (int i = r.wl.get(0); i <= r.wl.get(1); i += 1) {                  		
                    	if (matchesRule_w(r, i, test)) {
                    		for (int j = 0 ; j < r.re_tags.size(); j += 1) {//打印抽槽 **************
                                //System.out.println(re_tags.get(i).wp+" "+re_tags.get(i).wpe);
                                int tstart = 2*r.re_tags.get(j).wp;
                                int tend = 2*r.re_tags.get(j).wpe-1;
                                if (tstart < tend) {//防止空的出现
                                    tmp1=r.re_tags.get(j).getString();
                                    tmp2=test.substring(tstart,tend).replaceAll(" ","");
                                    List<Integer> ll = new ArrayList<Integer>();
                                    ll.add(r.re_tags.get(i).wp);
                                    ll.add(r.re_tags.get(i).wpe);
                                    Slot tmpr=new Slot(tmp1, tmp2, ll);
                                    r.results.add(tmpr);
                                }
                            }
                        	List<Slot> slot_list = new ArrayList<Slot>();
                        	slot_list.addAll(r.results);
                        	TemplateSubMatched ts = new TemplateSubMatched(r,slot_list);
                            re.add(ts);
                    		break;
                    	}
            		}	
        		} else if (r.wl.size() == 4) {//两个通配符
        			List<Integer> wl = new ArrayList<Integer>(); //wl里面有两个元素，分别代表两个W的节点大小
        			boolean iii = false;
            		for (int i = r.wl.get(0); i <= r.wl.get(1); i+=1) {//这种循环逻辑下，优先第一个W的贪婪匹配，但第二个W就不一定贪婪了
            			for (int j = r.wl.get(2); j <= r.wl.get(3); j+=1) {
            				wl.clear();
                    		wl.add(i);
                    		wl.add(j);
                    		if (matchesRule_ww(r, wl, test)) {
                    			for (int k = 0 ; k < r.re_tags.size(); k += 1) {//打印抽槽 **************
                                    //System.out.println(re_tags.get(i).wp+" "+re_tags.get(i).wpe);
                                    int tstart = 2*r.re_tags.get(k).wp;
                                    int tend = 2*r.re_tags.get(k).wpe-1;
                                    if (tstart < tend) {//防止空的出现
                                        tmp1=r.re_tags.get(k).getString();
                                        tmp2=test.substring(tstart,tend).replaceAll(" ","");
                                        List<Integer> ll = new ArrayList<Integer>();
                                        ll.add(r.re_tags.get(i).wp);
                                        ll.add(r.re_tags.get(i).wpe);
                                        Slot tmpr=new Slot(tmp1, tmp2, ll);
                                        r.results.add(tmpr);	
                                    }
                                }
                            	List<Slot> slot_list = new ArrayList<Slot>();
                            	slot_list.addAll(r.results);
                            	TemplateSubMatched ts = new TemplateSubMatched(r,slot_list);
                                re.add(ts);
                    			iii = true;
                    			break;
                    		}		
            			}
            			if (iii) {
            				break;
            			}
            		}
        		}
        	}
        }
        if (re.size()!=0) {
            return re;
        }else {
            return null;
        }
    }

    //从这里开始到下面是解析部分,最终在716行parseGrammarFromString完成解析功能，具体细节没有看，大概是些比较繁琐的逻辑
    private static Expansion parseAlternativeSets(List<Expansion> exp) {
        //Remove all leftover UnparsedSections
        Iterator<Expansion> expansionIterator;
        Expansion e;
        for (expansionIterator = exp.iterator(); (expansionIterator.hasNext() && ((e = expansionIterator.next()) != null)); ) {
            if (e instanceof UnparsedSection) {
                UnparsedSection up = ((UnparsedSection) e);
                if (up.text.equals("") || up.text.equals(" ")) {
                    expansionIterator.remove();
                } else {
                    up.text = up.text.trim();
                }
            }
        }

        Sequence currentSequence = new Sequence();
        AlternativeSet set = new AlternativeSet();
        for (expansionIterator = exp.iterator(); (expansionIterator.hasNext() && ((e = expansionIterator.next()) != null)); ) {
            if (e instanceof UnparsedSection) {
                UnparsedSection up = (UnparsedSection) e;
                if (up.text.contains("|")) {
                    set.addExpansion(currentSequence.simplestForm());
                    currentSequence = new Sequence();
                } else {
                    currentSequence.addExpansion(up);
                }
            } else {
                currentSequence.addExpansion(e);
            }
        }
        //将最终返回的根节点视为一个Alternative，如果有多个，就是真Alternative；否则就是一个Sequence
        if (set.getChildExpansions().size() > 0) {//如果有多个，那么就是Alternative
            set.addExpansion(currentSequence.simplestForm());
            return set;
        } else {//否则就是Sequence
            return currentSequence.simplestForm();
        }
    }

    private static List<Expansion> parseUnaryOperators(List<Expansion> exp) {
        List<Expansion> tempExp = new ArrayList<Expansion>();
        //Parse Plus and Unary operators
        //Note: Each symbol cannot be nested
        boolean expansionFound = false; //We found an expansion that a Unary operator can be applied to, check to see if the next char is a unary operator
        Expansion selectedExpansion = null;
        Iterator<Expansion> expansionIterator;
        Expansion e;
        for (expansionIterator = exp.iterator(); (expansionIterator.hasNext() && ((e = expansionIterator.next()) != null)); ) {
            if (e instanceof UnparsedSection) {
                if (expansionFound) {
                    UnparsedSection up = (UnparsedSection) e;
                    if (up.text.startsWith("*")) { // Kleene star operator
                        tempExp.add(new KleeneStar(selectedExpansion));
                        String newUnprocessedText = up.text.replaceFirst("\\*", ""); // Remove the ) token from the  UnparsedSection
                        tempExp.add(new UnparsedSection(newUnprocessedText.trim()));
                        expansionFound = false;
                    } else if (up.text.startsWith("+")) { // Plus operator
                        tempExp.add(new PlusOperator(selectedExpansion));
                        String newUnprocessedText = up.text.replaceFirst("\\+", ""); // Remove the ) token from the  UnparsedSection
                        tempExp.add(new UnparsedSection(newUnprocessedText.trim()));
                        expansionFound = false;
                    } else {
                        tempExp.add(selectedExpansion);
                        tempExp.add(e);
                        expansionFound = false;
                    }
                } else {
                    tempExp.add(e);
                }
            } else {
                if (expansionFound) { // If we already had found an expansion before this and we didnt find a unary operator after it, add it to the list of processed expansions
                    tempExp.add(selectedExpansion);
                }
                expansionFound = true;
                selectedExpansion = e;
            }
        }

        if (expansionFound) { // If we reached the end of the loop with a taggable expansion selected, but didnt add it to the list of expressions
            tempExp.add(selectedExpansion);
        }

        exp = tempExp;

        tempExp = new ArrayList<Expansion>();
        boolean foundLegalExpansion = false;
        boolean foundStart = false;
        String currentTag = "";
        Tag tagExpansion = null;
        //NOTE: A single expansion is allowed to have multiple tags!
        for (expansionIterator = exp.iterator(); (expansionIterator.hasNext() && ((e = expansionIterator.next()) != null)); ) {
            if (foundLegalExpansion) {
                if (foundStart) {
                    if (e instanceof UnparsedSection) { // Could contain the ending }
                        UnparsedSection up = (UnparsedSection) e;
                        if (up.text.startsWith("}") || up.text.equals("}")) { // Found the end of the tag!
                            tagExpansion.addTag(currentTag);
                            currentTag = ""; // Reset the tag string contents
                            String upText = up.text.replaceFirst("\\}", ""); // Remove the } token from the  UnparsedSection
                            upText = upText.trim();

                            if (upText.endsWith("{")) { // Test to see if another tag follows this one
                                foundStart = true;
                                upText = upText.substring(0, upText.length() - 1); // Remove the last char, which should be the { symbol
                            } else {
                                foundStart = false;
                                tempExp.add(tagExpansion);
                                foundLegalExpansion = false;
                            }

                            tempExp.add(new UnparsedSection(upText.trim()));
                        } else {
                            tempExp.add(e);
                        }
                    } else {
                        currentTag += e.getString(); // Add the expansions text contents to the tag string
                    }
                } else {
                    // Looking for a starting bracket {
                    if (e instanceof UnparsedSection) { // May contain the { we're looking for
                        UnparsedSection up = (UnparsedSection) e;
                        if (up.text.endsWith("{")) { // Found the start of the tag!
                            foundStart = true;
                            String newUnprocessedText = up.text.substring(0, up.text.length() - 1); // Remove the last char, which should be the { symbol
                            tempExp.add(new UnparsedSection(newUnprocessedText.trim())); // Add the updated UnprocessedText section to the list
                        } else {
                            tempExp.add(tagExpansion.getChildExpansion());
                            tempExp.add(e);
                            foundLegalExpansion = false;
                        }
                    } else { // No tag possible for the selected expansion, but check to see if this expansion can be tagged
                        tempExp.add(tagExpansion.getChildExpansion());
                        if (!(e instanceof PlusOperator || e instanceof KleeneStar)) {
                            tagExpansion = new Tag(e);
                        } else { // current expansion cannot be tagged, begin search for taggable expansion over again
                            foundLegalExpansion = false;
                            tempExp.add(e);
                        }
                    }
                }
            } else { // Looking for a expansion that is taggable
                if (!(e instanceof PlusOperator || e instanceof KleeneStar || e instanceof UnparsedSection)) {
                    foundLegalExpansion = true; // Found a taggable expansion, select it and start searching for tags
                    tagExpansion = new Tag(e);
                } else { // Unary operators and UnparsedSections cannot be tagged, pass over them
                    tempExp.add(e);
                }
            }
        }

        if (foundLegalExpansion) { // Reached end of loop and had selected a taggable expansion, but no tags found
            tempExp.add(tagExpansion.getChildExpansion());
        }

        exp = tempExp;
        return exp;
    }

    private static List<Expansion> parseRequiredGroupings(List<Expansion> exp) {
        List<Expansion> tempExp = new ArrayList<>();
        List<Expansion> children = new ArrayList<>();
        int nestCount = 0;
        final char startChar = '(';
        final char endChar = ')';
        for (Expansion e : exp) {
            if (e instanceof UnparsedSection) {
                UnparsedSection up = (UnparsedSection) e;
                StringBuilder childString = new StringBuilder();
                StringBuilder outsideString = new StringBuilder();
                for (char c : up.text.toCharArray()) {
                    if (c == startChar) {
                        nestCount++;
                        if (nestCount == 1) {
                            if (outsideString.toString().length() > 0) {
                                tempExp.add(new UnparsedSection(outsideString.toString()));
                            }
                            outsideString = new StringBuilder();
                            continue;
                        }
                    } else if (c == endChar) {
                        nestCount--;
                        if (nestCount == 0) { // Transition to the end of the string
                            children.add(new UnparsedSection(childString.toString()));
                            children = parseRequiredGroupings(children);
                            children = parseOptionalGroupings(children);
                            children = parseUnaryOperators(children);
                            tempExp.add(new RequiredGrouping(parseAlternativeSets(children)));
                            childString = new StringBuilder();
                            children = new ArrayList<>();
                        }
                    }

                    if (nestCount >= 1) {
                        childString.append(c);
                    } else if (c != endChar) {
                        outsideString.append(c);
                    }
                }

                if (outsideString.toString().length() > 0) {
                    tempExp.add(new UnparsedSection(outsideString.toString()));
                }

                if (childString.toString().length() > 0) {
                    if (nestCount > 0) {
                        children.add(new UnparsedSection(childString.toString()));
                    } else {
                        tempExp.add(new UnparsedSection(childString.toString()));
                    }
                }
            } else {
                if (nestCount >= 1) { // Element is part of this grouping's children
                    children.add(e);
                } else {
                    tempExp.add(e);
                }
            }
        }
        exp = tempExp;
        return exp;
    }

    private static List<Expansion> parseOptionalGroupings(List<Expansion> exp) {
        List<Expansion> tempExp = new ArrayList<>();
        List<Expansion> children = new ArrayList<>();
        int nestCount = 0;
        final char startChar = '[';
        final char endChar = ']';
        for (Expansion e : exp) {
            if (e instanceof UnparsedSection) {
                UnparsedSection up = (UnparsedSection) e;
                StringBuilder childString = new StringBuilder();
                StringBuilder outsideString = new StringBuilder();
                for (char c : up.text.toCharArray()) {
                    if (c == startChar) {
                        nestCount++;
                        if (nestCount == 1) {
                            if (outsideString.toString().length() > 0) {
                                tempExp.add(new UnparsedSection(outsideString.toString()));
                            }
                            outsideString = new StringBuilder();
                            continue;
                        }
                    } else if (c == endChar) {
                        nestCount--;
                        if (nestCount == 0) { // Transition to the end of the string
                            children.add(new UnparsedSection(childString.toString()));
                            children = parseOptionalGroupings(children);
                            children = parseUnaryOperators(children);
                            tempExp.add(new OptionalGrouping(parseAlternativeSets(children)));
                            childString = new StringBuilder();
                            children = new ArrayList<>();
                        }
                    }

                    if (nestCount >= 1) {
                        childString.append(c);
                    } else if (c != endChar) {
                        outsideString.append(c);
                    }
                }

                if (outsideString.toString().length() > 0) {
                    tempExp.add(new UnparsedSection(outsideString.toString()));
                }

                if (childString.toString().length() > 0) {
                    if (nestCount > 0) {
                        children.add(new UnparsedSection(childString.toString()));
                    } else {
                        tempExp.add(new UnparsedSection(childString.toString()));
                    }
                }
            } else {
                if (nestCount >= 1) { // Element is part of this grouping's children
                    children.add(e);
                } else {
                    tempExp.add(e);
                }
            }
        }
        exp = tempExp;
        return exp;
    }

    public static List<Expansion> parseWildcards(List<Expansion> exp) throws UnsupportedEncodingException{
        //Parse Rule References(借用之前Rule规则名的解析) because they have next highest precedence
        //Pattern: "<"+TOKEN+">"
        ArrayList<Expansion> tempExp = new ArrayList<Expansion>(); // Temporary list that will be copied into exp
        boolean startSearch; // True = looking for a <
        boolean endSearch; // True = looking for a >
        boolean tokenSearch; // True = hoping that the next Expansion is a Token object containing the name of the rule that is being referenced
        Token selectedToken; // Only set to null to avoid compiler warnings.
        Iterator<Expansion> expansionIterator;
        Expansion e;
        boolean iterationNeeded = true;
        while (iterationNeeded) {
            iterationNeeded = false;
            tempExp = new ArrayList<>();
            startSearch = true;
            endSearch = false;
            tokenSearch = false;
            selectedToken = null;
            for (expansionIterator = exp.iterator(); (expansionIterator.hasNext() && ((e = expansionIterator.next()) != null)); ) {
                if (startSearch) {
                    if (e instanceof UnparsedSection) {//这里的逻辑更多的是预处理,找到一个<，然后把他删掉
                        UnparsedSection up = (UnparsedSection) e;
                        if (up.text.endsWith("<")) {
                            startSearch = false;
                            tokenSearch = true;
                            //Found the < that starts the rule reference, so we need to remove it from the old UnparsedSection and add the new  UnparsedSection to the list
                            String newUnprocessedText = up.text.substring(0, up.text.length() - 1); // Remove the last char, which should be the < symbol
                            tempExp.add(new UnparsedSection(newUnprocessedText.trim())); // Add the updated UnprocessedText section to the list
                        } else {
                            tempExp.add(up); // UnparsedSection does not end in a < so it must not contain the start to a rule reference
                        }
                    } else {
                        tempExp.add(e); // Not an UnparsedSection with text, continue on
                    }
                } else if (endSearch) {//这里要开始解析了
                    if (e instanceof UnparsedSection) {
                        UnparsedSection up = (UnparsedSection) e;
                        if (up.text.startsWith(">")) {
                            endSearch = false;
                            startSearch = true;

                            short b_arr = selectedToken.getText();
                            String ts = "";
                            if(dict2.containsKey(b_arr)){
                                ts = dict2.get(b_arr);
                            }else{
                                ts = "#";
                                System.out.println("dict2中没有对应的字符");
                            }

                            int maohao=ts.indexOf(":");//找到冒号和横杠的位置
                            if (maohao != -1) {
                                int henggang=ts.indexOf("-");
                                int mao=Integer.parseInt(ts.substring(maohao+1,henggang));
                                int heng=Integer.parseInt(ts.substring(henggang+1));
                                tempExp.add(new Wildcard(mao,heng));//找到了Wildcard，加入exp列表	
                            }
                            //Found the > that ends the rule reference, so we need to remove it from the old  UnparsedSection and add the new  UnparsedSection to the list
                            String newUnprocessedText = up.text.replaceFirst(">", ""); // Remove the > token from the  UnparsedSection
                            tempExp.add(new UnparsedSection(newUnprocessedText.trim()));
                            iterationNeeded = true;
                        }
                    }
                } else if (tokenSearch) {
                    if (e instanceof Token) {
                        endSearch = true;
                        tokenSearch = false;
                        selectedToken = (Token) e;
                    }
                } else {
                    tempExp.add(e);
                }
            }
            exp = tempExp;
        }
        return exp;
    }

    public static List<Expansion> parseTokensFromString(String part) throws UnsupportedEncodingException {
        List<Expansion> exp = new ArrayList<Expansion>();
        //Parse Tokens because they have the highest precedence
        String passed = ""; // All characters that are not part of a token
        int position = 0;
        boolean tokenMode = false; // If True, test and add the following characters to the currentToken string
        String currentToken;

        while (position < part.length()) {
            tokenMode = false;
            currentToken = ""; // This holds the string of characters that are being scanned into one Token
            String a; // This holds the current character that is being scanned

            while (!tokenMode && position < part.length()) {
                a = "" + part.charAt(position);
                if (!a.matches(specialCharacterRegex)) {
                    exp.add(new UnparsedSection(passed.trim()));
                    passed = "";
                    tokenMode = true;
                    //DO NOT INCREMENT THE POSITION COUNTER, WE ARE LETTING THE NEXT LOOP EVALUATE THIS CHARACTER
                } else {
                    passed += a;
                    position++;
                }
            }

            if (!tokenMode) { // We reached the end of the string without finding a token, so add what we passed over
                exp.add(new UnparsedSection(passed.trim()));
                passed = "";
            }

            //tokenMode=TRUE, 从这里开始就是token了，记录字符准备创建Token对象
            while (tokenMode && position < part.length()) {
                a = "" + part.charAt(position); // Retrieve the current char we are using
                if (a.matches(specialCharacterRegex)) { // Check to see if char matches special characters
                    tokenMode = false;
                    //Entire token has now been scanned into currentToken
                    Token ttt = null;
                    if(dict.containsKey(currentToken)){
                        ttt = new Token(dict.get(currentToken));
                    }else{
                        ttt = new Token((short) -1);
                        System.out.println("001-string:"+currentToken);
                        set.add(currentToken);
                        //if(currentToken.contains(":")){
                        //   set.add(currentToken);
                        //}
                    }
                    exp.add(ttt);
                    currentToken = "";
                    passed = a;
                    position++;
                } else {
                    currentToken += a;
                    position++;
                }
            }
            if (tokenMode) { // Reached end of string before end of token
                /*Token ttt ;
                byte[] btmp=new byte[1];
                if(ch_dict.containsKey(currentToken)) {
                    btmp[0]=ch_dict.get(currentToken);
                    ttt = new Token(btmp);
                }else {
                    ttt = new Token(currentToken.getBytes("UTF-8"));
                }*/
                Token ttt = null;
                if(dict.containsKey(currentToken)){
                    ttt = new Token(dict.get(currentToken));
                }else{
                    ttt = new Token((short) -1);
                    System.out.println("该Token没有在字典中找到："+currentToken);
                }
                exp.add(ttt);
                currentToken = "";
            }
        }

        if (!tokenMode) { // We reached the end of the string without finding a token, so add what we passed over
            exp.add(new UnparsedSection(passed.trim()));
        }

        //Everything that can be Tokenized has been tokenized, so remove all of the whitespace as it serves no function now
        //后处理。去除空格
        Iterator<Expansion> expansionIterator;
        List<Expansion> tempExp = new ArrayList<>();
        Expansion e;
        for (expansionIterator = exp.iterator(); expansionIterator.hasNext() && ((e = expansionIterator.next()) != null); ) {
            if (e instanceof UnparsedSection) {
                UnparsedSection up = (UnparsedSection) e;
                String test = up.text.replaceAll(" ", "");
                if (!test.equals("")) {
                    tempExp.add(new UnparsedSection(test));
                }
            } else {
                tempExp.add(e);
            }
        }
        exp = tempExp;
        return exp;
    }

    public static Expansion parseExpansionsFromString(String part) throws IOException {//在下面parseGrammarFromString被调用
        //System.out.println(part);
        List<Expansion> exp = parseTokensFromString(part);
        exp = parseWildcards(exp);
        exp = parseRequiredGroupings(exp);
        exp = parseOptionalGroupings(exp);
        exp = parseUnaryOperators(exp);
        Expansion rootExpansion = parseAlternativeSets(exp);//一层一层递进解析
        return rootExpansion;
    }

    public static Grammar parseGrammarFromString(String s) throws IOException {//将字符串解析成语法对象
        Grammar grammar = new Grammar();
        int ind = 0;
        int pri = 10;
        // TODO 切换为vocab.txt
        dict = loadFileToMap("src/main/test//vocab-jsgf.txt");
        dict2 = reverse(dict);
        String[] statements = s.split(";");
        try {
            for (String statement : statements) {
                statement = statement.trim();

                if (statement.startsWith("grammar ")) {
                    String[] parts = statement.split(" ");
                    grammar.name = parts[1];
                } else if (statement.startsWith("public <")) {
                    statement = statement.replaceFirst("public ", "");
                    String[] parts = statement.split("=");
                    String ruleName = parts[0].trim();
                    Expansion exp = Grammar.parseExpansionsFromString(parts[1]);//调用parseExpansionsFromString                   
                    
                    if (ruleName.contains("#")) {//提取优先级，简化rule的名字，用对象属性index加以区分
                		int indexJing = ruleName.indexOf('#');
                		int indexAnd = ruleName.indexOf('&');
                		String prio = ruleName.substring(indexJing+1 , indexAnd);
                		pri  =  Short.parseShort(prio);
                		ruleName = ruleName.substring(1,indexJing);
                	} else {
                		int indexAnd = ruleName.indexOf('&');
                		ruleName = ruleName.substring(1,indexAnd);             		
                	}
                    
                    if (parts[1].contains("W:")) {//这里要把W多长这个信息提取出来
                    	List<Integer> wl = new ArrayList<Integer>();
                    	String[] sl = parts[1].split(">");
                		for (int ii = 0; ii < sl.length-1; ii++) {//形如 <W:0-5>
                			int jj = sl[ii].indexOf('<');
                			if (jj == -1) {
                				continue;
                			}
                			int kk = sl[ii].indexOf(':', jj);
                			if (kk == -1) { //sl[ii]出现过这种情况：{W:0-4} (移 动 地 图 ) [(<request_prefix
                				continue;
                			}                			
                			int ll = sl[ii].indexOf('-', kk);
                			//System.out.println(sl[ii]);
                			wl.add(Integer.parseInt(sl[ii].substring(kk+1,ll)));//第一个数字
                			wl.add(Integer.parseInt(sl[ii].substring(ll+1)));//第二个数字
                		}//不管怎样，这个wl列表内数据是成对出现的，也就是一定是偶数个。一般是2个或4个。
                    	grammar.addRule(new Rule(ruleName, true, wl, pri, ind, exp));
                    } else {
                    	grammar.addRule(new Rule(ruleName, false, pri, ind, exp));
                    }
                    ind += 1;
                }
            }          
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("EXCEPTION: " + e.getMessage());
        }
        return grammar;
    }

    public String compileGrammar() {
        String f = header.getHeader();
        f = f.concat("grammar " + name + ";\n");

        for (Import i : imports) {
            f = f.concat(i.getString() + "\n");
        }
        for (Rule r : rules) {
            f = f.concat(r.getRuleString() + "\n");
        }
        return f;
    }

    @Override
    public String toString() {
        return compileGrammar();
    }

    public void addRule(Rule r) {
        rules.add(r);
    }

    public Rule getRule(String ruleName) {
        for (Rule r : rules) {
            if (r.name.equals(ruleName)) {
                return r;
            }
        }
        throw new RuntimeException("Could not find rule by name: " + ruleName);
    }

    public void addImport(Import i) {
        imports.add(i);
    }

    public void addImport(String i) {
        imports.add(new Import(i));
    }

    public List<Import> getImports() {
        return imports;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public GrammarHeader getGrammarHeader() {
        return header;
    }

    public void setGrammarHeader(GrammarHeader h) {
        header = h;
    }

    static class UnparsedSection implements Expansion { //未解析的部分，这里为社么只考虑到了字符串
        public String text;

        public UnparsedSection(String section) {
            text = section;
        }

        @Override
        public String getString() {
            return "UnparsedSection:" + text;
        }

        @Override
        public String toString() {
            return "UnparsedSection:" + text;
        }

        @Override
        public boolean hasChildren() {
            return false;
        }

        @Override
        public boolean hasUnparsedChildren() {
            return false;
        }
    }

    /**
     * 加载文件为map类型（key：每行数据，value：下标）
     * @param filePath
     * @return map映射表
     */
    public static Map<String, Short> loadFileToMap(String filePath) {
        SortedMap<String, Short> sortedVocab = new TreeMap();
        short index = 0;
        String line;
        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                sortedVocab.put(line.trim(), index);
                index++;
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sortedVocab;
    }

    //map must be a bijection in order for this to work properly
    public static <K,V> HashMap<V,K> reverse(Map<K,V> map) {
        HashMap<V,K> rev = new HashMap<V, K>();
        for(Map.Entry<K,V> entry : map.entrySet()){
            rev.put(entry.getValue(), entry.getKey());
        }
        return rev;
    }
    
    public LinkedHashMap<Integer,List<ACResult>> ahoCor(String s) throws IOException {
    	String ss=s.replace(" ", "");
		Collection<PayloadEmit<AcSlot>> emits = trie.parseText(ss);
		Iterator<PayloadEmit<AcSlot>> iterator = emits.iterator();
		LinkedHashMap<Integer,List<ACResult>> re = new LinkedHashMap<Integer,List<ACResult>>();
		List<ACResult> actmp;
        while(iterator.hasNext()) {
        	PayloadEmit<AcSlot> it = iterator.next();
        	int start = it.getStart();
        	int end = it.getEnd();
        	String keyword = it.getKeyword();
        	AcSlot payload = it.getPayload();
        	String[] ss1=payload.toString().split("!");       	
        	List<String> lable = new ArrayList<String>();
        	for(int i = 0; i <ss1.length; i++) {
        		if (!ss1[i].equals("")) {
        			lable.add(ss1[i]);
        		}
        	}
        	ACResult acr = new ACResult(keyword,lable,end+1);
        	if(re == null) {
        		actmp = new ArrayList<ACResult>();
        		actmp.add(acr);
            	re.put(start, actmp);
            	continue;
        	}
        	actmp = re.get(start);
        	if (actmp == null) {
        		actmp = new ArrayList<ACResult>();
        	}
        	actmp.add(acr);
        	re.put(start, actmp);
        }

        List<Map.Entry<Integer,List<ACResult>>> infoIds =new ArrayList<Map.Entry<Integer,List<ACResult>>>(re.entrySet());
        //排序
        Collections.sort(infoIds, new Comparator<Map.Entry<Integer,List<ACResult>>>() {
        public int compare(Map.Entry<Integer,List<ACResult>> o1, Map.Entry<Integer,List<ACResult>> o2) {
        	int p1 =  o1.getKey();
            int p2 = o2.getKey();;
            return p1-p2;//升序;
            }
        });
        //转换成新map输出
        LinkedHashMap<Integer,List<ACResult>> newMap = new LinkedHashMap <Integer,List<ACResult>>();

        for(Map.Entry<Integer,List<ACResult>> entity : infoIds){
            newMap.put(entity.getKey(), entity.getValue());
            //System.out.println(entity.getKey()+"  "+entity.getValue());
        }
		return newMap;
    }
    
	public static HashMap<String, ArrayList<String>> ahoCor_readfile() throws IOException {
		File file = new File("D:/CODE/AhoCor.txt");
		BufferedReader in = new BufferedReader(new FileReader(file));
		String str;
		String keyword = null;
		StringBuilder re = new StringBuilder();
	    HashMap<String, ArrayList<String>> slots_list = new HashMap<String, ArrayList<String>>();

	    while ((str = in.readLine()) != null) {
	    	re.append(str.trim()).append("\n");
	    }
	    String slots_pre = re.toString();
	    String[] slots = slots_pre.split("\n");
	    for(int j=0;j<slots.length;j++) {
	    	if(slots[j].charAt(0)=='!') {//这是个keyword
	    		keyword = slots[j];
	    	} else {//这是个keyword下面的词语
		        if(slots_list.containsKey(slots[j])) {
		        	ArrayList<String> slot_tmp = slots_list.get(slots[j]);
		        	slot_tmp.add(keyword);
		        	slots_list.put(slots[j], slot_tmp);
		        } else {
		        	ArrayList<String> slot_tmp=new ArrayList<String>();
		            slot_tmp.add(keyword);
		            slots_list.put(slots[j], slot_tmp);
		        } 	
	    	}    
	    } 
	    in.close();	
	    return slots_list;
	}
	
    public static void ahoCor_pre() throws IOException {
    	HashMap<String, ArrayList<String>> slots_list = ahoCor_readfile();
        PayloadTrieBuilder<AcSlot> trie_builder = PayloadTrie.<AcSlot>builder();        
        for (Entry<String, ArrayList<String>> entry : slots_list.entrySet()) {
        	String vacab = entry.getKey();
        	ArrayList<String> slot = entry.getValue();
        	if (slot.size()==1) {
        		trie_builder.addKeyword(vacab, new AcSlot(slot.get(0)));
        	} else {
        		trie_builder.addKeyword(vacab, new AcSlot(slot));
        	}
        }	 
        trie = trie_builder.build();	
    }
 
    /**
     * 根据词表矩阵，获取word list
     * @param wordListMatrix
     * @return
     */
    public List<List<WordKeyBO>> getWordList(LinkedHashMap<Integer, List<ACResult>> wordListMatrix) {
        if (wordListMatrix == null || wordListMatrix.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<WordKeyBO>> resultList = new ArrayList<>();
        List<WordKeyBO> path = new ArrayList<>();
        Iterator<Integer> iterator = wordListMatrix.keySet().iterator();
        //卡死从头开始
        Integer nextKey = iterator.next();
        findWordListPath(resultList, wordListMatrix, nextKey, path);
        
        /*//不卡死从头开始
        while(iterator.hasNext()){
            Integer nextKey = iterator.next();
            findWordListPath(resultList, wordListMatrix, nextKey, path);
        }*/
        return resultList;
    }
    
    /**
     * 深度优先遍历，获取所有可能的词表路径
     * @param resultList     所有路径结果
     * @param wordListMatrix 词表
     * @param key            下一个可能的路径的key
     * @param path           存储单条路径
     */
    private void findWordListPath(List<List<WordKeyBO>> resultList, Map<Integer, List<ACResult>> wordListMatrix,
                                         Integer key, List<WordKeyBO> path) {
        List<ACResult> acResultList = wordListMatrix.get(key);
        if (acResultList == null) {
            resultList.add(path);
            return;
        }
        for (ACResult acResult : acResultList) {
            List<WordKeyBO> cPath = new ArrayList<>(path);
            cPath.add(acResult.getWordKeyBO());
            findWordListPath(resultList, wordListMatrix, acResult.getIndex(), cPath);
        }
    }
 
    
    /**
    * 对于word list做初筛，缩小匹配范围
    * @param wordList
    * @param key2IndexMap
    * @return
    */
   public static Set<Integer> findUnionSetForWordList(List<List<WordKeyBO>> wordList) {
       if (wordList == null || wordList.isEmpty()) {
           return new HashSet<>();
       }
       Set<Integer> indexSetForWordList = new HashSet<>();
      // 存储所有的词
       Set<String> wordSet = new HashSet<>();
       // 每句话之间求并集
       for(List<WordKeyBO> sentence: wordList){
           // 直接取最小数目的词
           if(sentence!=null&&!sentence.isEmpty()){
               //long start = System.currentTimeMillis();
               Set<Integer> sentenceIndexSet = null;
               // 假设第一个词对应的模板数量最少
               WordKeyBO firstWord = sentence.get(0);
               //存储第一个word
               wordSet.add(firstWord.getValue());               
               Set<Integer> firstWordIndexSet = getWordIndexUnionSet(firstWord.getLabelList());
               sentenceIndexSet = firstWordIndexSet;
               // 遍历、获取词对应模板数量最少的
               for(int i=1;i<sentence.size();i++){
                   WordKeyBO nextWord = sentence.get(i);
                // 判断接下来的word是否已经出现
                   if(nextWord.getValue()!=null&&!wordSet.contains(nextWord.getValue())){
                	   wordSet.add(nextWord.getValue());
                       Set<Integer> nextWordIndexSet = getWordIndexUnionSet(nextWord.getLabelList());
                       if(nextWordIndexSet.size()<firstWordIndexSet.size()){
                           sentenceIndexSet = nextWordIndexSet;
                       }
                   }
               }
               //long end = System.currentTimeMillis();
               //System.out.println("[findUnionSetForWordList]里单句中词取候选模板数量最小耗时"+(end-start)+"ms");
               // 每句的候选集利用set.addAll方法，求句与句之间的并集
               //long start1 = System.currentTimeMillis();
               indexSetForWordList.addAll(sentenceIndexSet);
               //long end1 = System.currentTimeMillis();
               //System.out.println("[findUnionSetForWordList]里句与句之间求并集耗时"+(end1-start1)+"ms");
           }
       }
       return indexSetForWordList;
   }
   /**
    * 对单个word，分别去对应的rule index，并求并集
    * @param labelList
    * @param key2IndexMap
    * @return
    */
   private static Set<Integer> getWordIndexUnionSet(List<String> labelList){
       if(labelList == null || labelList.isEmpty()){
           return Collections.emptySet();
       }
       Set<Integer> set = new HashSet<>();
       for(int i=0;i<labelList.size();i++){
           String label = labelList.get(i);
           // 加<>拼接为keyword
           String keyword = "<"+label+">";
           Set<Integer> indexSet = key2IndexMap.get(keyword);
           if(indexSet!=null&&!indexSet.isEmpty()){
               set.addAll(indexSet);
           }
       }
       return set;
   }
}
