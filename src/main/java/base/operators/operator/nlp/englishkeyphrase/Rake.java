package base.operators.operator.nlp.englishkeyphrase;

import idsw.nlp.read.ReadFileAsStream;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Rapid Automatic Keyword Extraction (RAKE)
 * =========================================
 *
 * Rose, Stuart & Engel, Dave & Cramer, Nick & Cowley, Wendy. (2010).
 * Automatic Keyword Extraction from Individual Documents.
 * Text Mining: Applications and Theory. 1 - 20. 10.1002/9780470689646.ch1.
 *
 * Implementation based on https://github.com/aneesha/RAKE
 */
public class Rake {
    String language;//语言
    String stopWordsPattern;//停用词表转成的正则表达式
    int min_char_length = 1;//限定所抽取的短语应具有的长度下限
    int max_word_length = 4;//限定所抽取的短语应包含的单词数的上限

    /**
     * 初始化：根据语言获取相应的停用词表，并将停用词转换成正则表达式
     * @param language 语言
     */
    Rake(String language) {
        this.language = language;

        // Read the stop words file for the given language
        //InputStream stream = this.getClass().getResourceAsStream("/stopwords/" + language + ".txt");
        InputStream stream = ReadFileAsStream.readEnglishKeyPhraseDict(language + ".txt");
        String line;

        if (stream != null) {
            try {
                ArrayList<String> stopWords = new ArrayList<>();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));

                // Loop through each stop word and add it to the list
                while ((line = bufferedReader.readLine()) != null){
                	if(!"".equals(line.trim())){
                		stopWords.add(line.trim());
                	} 
                }
                	
                ArrayList<String> regexList = new ArrayList<>();

                // Turn the stop words into an array of regex
                for (String word : stopWords) {
                    String regex = "\\b" + word + "(?![\\w-])";
                    regexList.add(regex);
                }

                // Join all regexes into global pattern
                this.stopWordsPattern = String.join("|", regexList);
            } catch (Exception e) {
                throw new Error("An error occurred reading stop words for language " + language);
            }
        } else throw new Error("Could not find stop words required for language " + language);

    }
    

    /**
     * 初始化：根据语言获取相应的停用词表，并将停用词转换成正则表达式；设定短语的最小字符数和短语中包含的最大单词数
     * @param language 语言
     * @param min_char_length 所抽取的短语具有的长度下限，用户设定，可为null，null时取默认
     * @param max_word_length 所抽取的短语包含的单词数的上限，用户设定，可为null，null时取默认
     */
    Rake(String language, Integer min_char_length, Integer max_word_length) {
        this.language = language;//语言
        
        if(min_char_length!=null){
        	this.min_char_length = min_char_length;//短语的最小字符数
        }
        if(max_word_length!=null){
        	this.max_word_length = max_word_length;//短语中包含的最大单词数
        }
                
        // Read the stop words file for the given language
        //InputStream stream = this.getClass().getResourceAsStream("/nlp/englishkeyphrase/" + language + ".txt");
        InputStream stream = ReadFileAsStream.readEnglishKeyPhraseDict(language + ".txt");
        String line;

        if (stream != null) {
            try {
                ArrayList<String> stopWords = new ArrayList<>();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));

                // Loop through each stop word and add it to the list
                while ((line = bufferedReader.readLine()) != null){
                	if(!"".equals(line.trim())){
                		stopWords.add(line.trim());
                	} 
                }

                ArrayList<String> regexList = new ArrayList<>();

                // Turn the stop words into an array of regex
                for (String word : stopWords) {
                    String regex = "\\b" + word + "(?![\\w-])";
                    regexList.add(regex);
                }

                // Join all regexes into global pattern
                this.stopWordsPattern = String.join("|", regexList);
            } catch (Exception e) {
                throw new Error("An error occurred reading stop words for language " + language);
            }
        } else throw new Error("Could not find stop words required for language " + language);

    }
    

    /**
     * Returns a list of all sentences in a given string of text
     *
     * @param text
     * @return String[]
     */
    private String[] getSentences(String text) {
        return text.split("[\\[\\]\n.!?,;:\\t\\\\\\\"\\(\\)\\'\\u2019\\u2013]|\\\\s\\\\s");
    }

    /**
     * Returns a list of all words that are have a length greater than a specified number of characters
     * 对phrase进行切分，按照规则进行切分(切分规则只适合英文，另外切分后的word如果是数字或者长度小于设定的值，那么就不保留。)（这里切分是为了便于计算phrase的score,）
     * @param text given text
     * @param size minimum size
     */
    private String[] separateWords(String text, int size) {
        String[] split = text.split("[^a-zA-Z0-9_\\+\\-/]");
        ArrayList<String> words = new ArrayList<>();

        for (String word : split) {
            String current = word.trim().toLowerCase();
            int len = current.length();

            if (len > size && len > 0 && !StringUtils.isNumeric(current))//
                words.add(current);
             
        }

        return words.toArray(new String[words.size()]);
    }
   

    /**
     * Generates a list of keywords by splitting sentences by their stop words
     *
     * @param sentences
     * @return
     */
    private String[] getKeywords(String[] sentences) {
        ArrayList<String> phraseList = new ArrayList<>();
        
        for (String sentence : sentences) {
            String temp = sentence.trim().replaceAll(this.stopWordsPattern, "|");
            String[] phrases = temp.split("\\|");

            for (String phrase : phrases) {
                phrase = phrase.trim().toLowerCase();

                if (phrase.length() > 0)
                    phraseList.add(phrase);
            }
            
            
/*            for (String phrase : phrases) {
                phrase = phrase.trim().toLowerCase();
                String words[] = phrase.split(" ");
                int num_words = words.length;

                if (phrase.length() >= this.min_char_length  &&  num_words<= this.max_word_length)//当短语长度大于等于设定的短语最小字符长度，包含的单词数小于等于设定的短语最大单词数时，添加到队列中
                    phraseList.add(phrase);
            }*/
            
        }

        return phraseList.toArray(new String[phraseList.size()]);
    }
    

    /**
     * Calculates word scores for each word in a collection of phrases
     * <p>
     * Scores is calculated by dividing the word degree (collective length of phrases the word appears in)
     * by the number of times the word appears
     *
     * @param phrases
     * @return
     */
    private LinkedHashMap<String, Double> calculateWordScores(String[] phrases) {
        LinkedHashMap<String, Integer> wordFrequencies = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> wordDegrees = new LinkedHashMap<>();
        LinkedHashMap<String, Double> wordScores = new LinkedHashMap<>();

        for (String phrase : phrases) {
        	
        	//Returns a list of all words that are have a length greater than a specified number of characters
            String[] words = this.separateWords(phrase, 0);//对通过停用词过滤得到的短语逐一按照规则进行切分，并排除数字
            
            int length = words.length;
            int degree = length - 1;

            for (String word : words) {
                wordFrequencies.put(word, wordFrequencies.getOrDefault(word, 0) + 1);
                wordDegrees.put(word, wordDegrees.getOrDefault(word, 0) + degree);
            }
        }

        for (String item : wordFrequencies.keySet()) {
            wordDegrees.put(item, wordDegrees.get(item) + wordFrequencies.get(item));
            wordScores.put(item, wordDegrees.get(item) / (wordFrequencies.get(item) * 1.0));
        }

        return wordScores;
    }

    /**
     * Returns a list of keyword candidates and their respective word scores
     *
     * @param phrases
     * @param wordScores
     * @return
     */
    private LinkedHashMap<String, Double> getCandidateKeywordScores(String[] phrases, LinkedHashMap<String, Double> wordScores) {
        LinkedHashMap<String, Double> keywordCandidates = new LinkedHashMap<>();

        for (String phrase : phrases) {
            double score = 0.0;

            String[] words = this.separateWords(phrase, 0);
            
            int num_words = words.length;

            //加上对短语长度，以及短语中单词数量的判断
            //当短语长度大于等于设定的短语最小字符长度，包含的单词数小于等于设定的短语最大单词数时，添加到候选队列中                                     
            if (phrase.length() >= this.min_char_length  &&  num_words<= this.max_word_length){
            	int digits = 0;//短语中包含的数字单词的数目
        		int alpha = 0;//短语中包含的字母单词的数目
            	for (String word : words) {           		            		
            		if(StringUtils.isAlpha(word)){
            			alpha+=1;
            		}
            		if(StringUtils.isNumeric(word)){
            			digits+=1;
            		}
                    score += wordScores.get(word);                    
                }
            	
            	//score为1说明degree为0，过滤出去;全是字母的单词数不能小于全是数字的单词数
            	/*if(alpha>0 && alpha>=digits && score>1){
            		keywordCandidates.put(phrase, score);
            	}*/
            	if(alpha>0 && alpha>=digits){
            		keywordCandidates.put(phrase, score);
            	}
            }
        }

        return keywordCandidates;
    }

    /**
     * Sorts a LinkedHashMap by value from lowest to highest
     *
     * @param map
     * @return
     */
    private LinkedHashMap<String, Double> sortHashMap(LinkedHashMap<String, Double> map) {
        LinkedHashMap<String, Double> result = new LinkedHashMap<>();
        List<Map.Entry<String, Double>> list = new LinkedList<>(map.entrySet());       

        Collections.sort(list, Comparator.comparing(Map.Entry<String, Double>::getValue));
        Collections.reverse(list);

        for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext(); ) {
            Map.Entry<String, Double> entry = it.next();
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Extracts keywords from the given text body using the RAKE algorithm
     * 1-分句（标点符号来分割）
     * 2-提取关键短语，逐句提取（1：切分句子为短语（通过停用词来切割）；2-根据短语字符长度、短语中单词数等来过滤）
     * 3-计算每个单词的度（(D+F)/F）
     * 4-计算候选短语的score（包含的词的度累加，map去重）
     * 5-排序输出
     * @param text
     */
    public LinkedHashMap<String, Double> getKeywordsFromText(String text) {
    	text = text.trim().toLowerCase();//转换成小写
        String[] sentences = this.getSentences(text);//用标点符号来分割文本形成句子
        String[] keywords = this.getKeywords(sentences);//用停用词来分割句子形成phrase,phrase没有去重

        LinkedHashMap<String, Double> wordScores = this.calculateWordScores(keywords);
        LinkedHashMap<String, Double> keywordCandidates = this.getCandidateKeywordScores(keywords, wordScores);

        return this.sortHashMap(keywordCandidates);
    }

    public static void main(String[] args) {
        String languageCode = RakeLanguages.ENGLISH;
        Rake rake = new Rake(languageCode,6,4);
        String text1 = "Compatibility of systems of linear constraints over the set of natural numbers. Criteria of compatibility "+
        "of a system of linear Diophantine equations, strict inequations, and nonstrict inequations are considered. "+
        "Upper bounds for components of a minimal set of solutions and algorithms of construction of minimal generating"+
        " sets of solutions for all types of systems are given. These criteria and the corresponding algorithms "+
        "for constructing a minimal supporting set of solutions can be used in solving all the considered types of "+
        "systems and systems of mixed types.";
        
        String text2 ="The history of NLP generally starts in the 1950s, although work can be found from earlier periods. In 1950, Alan Turing published an article titled 'Computing Machinery and Intelligencec' which proposed what is now called the Turing test as a criterion of intelligence."+
"The Georgetown experiment in 1954 involved fully automatic translation of more than sixty Russian sentences into English. The authors claimed that within three or five years, machine translation would be a solved problem.[2] However, real progress was much slower, and after the ALPAC report in 1966, which found that ten-year-long research had failed to fulfill the expectations, funding for machine translation was dramatically reduced."+
" Little further research in machine translation was conducted until the late 1980s, when the first statistical machine translation systems were developed.Some notably successful NLP systems developed in the 1960s were SHRDLU, a natural language system working in restricted 'blocks worlds' with restricted vocabularies, and ELIZA, a simulation of a Rogerian psychotherapist, written by Joseph Weizenbaum between 1964 and 1966."+
" Using almost no information about human thought or emotion, ELIZA sometimes provided a startlingly human-like interaction. When the 'patient' exceeded the very small knowledge base, ELIZA might provide a generic response, for example, responding to 'My head hurts' with 'Why do you say your head hurts?'."+
"During the 1970s many programmers began to write 'conceptual ontologies', which structured real-world information into computer-understandable data. Examples are MARGIE (Schank, 1975), SAM (Cullingford, 1978), PAM (Wilensky, 1978), TaleSpin (Meehan, 1976), QUALM (Lehnert, 1977), Politics (Carbonell, 1979), and Plot Units (Lehnert 1981). "+
"During this time, many chatterbots were written including PARRY, Racter, and Jabberwacky.Up to the 1980s, most NLP systems were based on complex sets of hand-written rules. Starting in the late 1980s, however, there was a revolution in NLP with the introduction of machine learning algorithms for language processing. This was due to both the steady increase in computational power (see Moore's Law) and the gradual lessening of the dominance of Chomskyan theories of linguistics (e.g. transformational grammar), whose theoretical underpinnings discouraged the sort of corpus linguistics that underlies the machine-learning approach to language processing.[3]"+
"Some of the earliest-used machine learning algorithms, such as decision trees, produced systems of hard if-then rules similar to existing hand-written rules. However, Part-of-speech tagging introduced the use of Hidden Markov Models to NLP, and increasingly, research has focused on statistical models, which make soft, probabilistic decisions based on attaching real-valued weights to the features making up the input data. The cache language models upon which many speech recognition systems now rely are examples of such statistical models. Such models are generally more robust when given unfamiliar input, especially input that contains errors (as is very common for real-world data), and produce more reliable results when integrated into a larger system comprising multiple subtasks.";
        
        LinkedHashMap<String, Double> results = rake.getKeywordsFromText(text2);
        System.out.println(results);
        
        
/*    	//String text = "The Georgetown experiment in 1954 involved fully_automatic translation of more-than+sixty/Russian\\sentences into English. The authors claimed that within three or five years, machine translation would be a solved problem.(2)3";
    	String text = "The Georgetown experiment in 1954";
    	String[] words = rake.separateWords(text,0);
    	for(String word:words){
    		System.out.println(word);
    	}

    	String num = "1954";
    	System.out.println(StringUtils.isNumeric(num));*/
               
    }

}