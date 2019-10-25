package base.operators.operator.nlp.autosummary.core.stopwords;

import base.operators.operator.nlp.autosummary.core.summary.ConfConfig;
import idsw.nlp.read.ReadFileAsStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class StopWordDictionary {
	
//	static String ChStopWordDictionaryPath = "/nlp/stopfilter/dict/stopdict.txt";
//	static String EnStopWordDictionaryPath = "/nlp/stopfilter/dict/enstopwords.txt";
	//static StopWordDictionary dictionary;
	static LinkedList<String> chDict = new LinkedList<String>();
	static LinkedList<String> enDict = new LinkedList<String>();
	
	public StopWordDictionary(List<String> dicts) {
		loadData(dicts);
	}
    
    private void loadData(List<String> dicts) {
    	InputStream inStream = null;
		InputStream enStream = null;
		Scanner sc = null;
		int lang = ConfConfig.getInstance().getLang();
		int type = ConfConfig.getInstance().getType();
		try {
			if(type==0 || type==2) {
				inStream = ReadFileAsStream.readStopDict();
				enStream = ReadFileAsStream.readEnglishStopDict();
				BufferedReader in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
				BufferedReader en = new BufferedReader(new InputStreamReader(enStream, "UTF-8"));
				String row;
				while ((row = in.readLine()) != null) {
					if (!chDict.contains(row)) {
						chDict.add(row);
	                }
				}
				String enrow;
				while ((enrow = en.readLine()) != null) {
					if (!enDict.contains(enrow)) {
						enDict.add(enrow);
	                }
				}
			}
			if(type==1 || type==2) {
				if(0==lang && null != dicts) {
					for(int i=0; i<dicts.size(); i++) {
						if (!chDict.contains(dicts.get(i))) {
							chDict.add(dicts.get(i));
		                }
					}
				} else if(1 == lang && null != dicts){
					for(int i=0; i<dicts.size(); i++) {
						if (!enDict.contains(dicts.get(i))) {
							enDict.add(dicts.get(i));
		                }
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (sc != null) {
				sc.close();
			}
		}
    }

    public boolean contains(String key, int lang)
    {
    	if(0==lang) {
    		return chDict.contains(key);
    	} else if(1==lang) {
    		return enDict.contains(key);
    	}
    	return false;
    }

}
