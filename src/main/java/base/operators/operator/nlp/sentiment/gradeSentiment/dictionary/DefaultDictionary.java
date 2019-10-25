package base.operators.operator.nlp.sentiment.gradeSentiment.dictionary;

import idsw.nlp.read.ReadFileAsStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class DefaultDictionary {
    //使用大连理工情感词典进行等级
//    public static final String chineseDaLianDegreeDicPath = "/nlp/sentiment/gradeSentiment-degree.txt";
//    public static final String chineseDaLianNotDicPath = "/nlp/sentiment/gradeSentiment-notDic.txt";
//    public static final String chineseDaLianSentimentDicPath = "/nlp/sentiment/DaLianSentimentDicNew.txt";
//    public static final String chineseDaLianSymbolDicPath = "/nlp/sentiment/symbol.txt";

    public static List<String> dalian_not = new ArrayList<>();
    public static Map<String, Double> dalian_senti = new HashMap<>();
    public static Map<String, Double> dalian_degree = new HashMap<>();
    public static List<String> symbol = new ArrayList<>();
    static
    {
        InputStream notDicStream = null;
        InputStream sentiStream = null;
        InputStream degreeStream = null;
        InputStream symbolStream = null;
        Scanner sc = null;
        try {
            notDicStream = ReadFileAsStream.readSentimentNegatorDict();
            sentiStream = ReadFileAsStream.readSentimentDict();
            degreeStream = ReadFileAsStream.readSentimentDegreeDict();
            symbolStream = ReadFileAsStream.readSentimentSymbolDict();

            BufferedReader notDicIn = new BufferedReader(new InputStreamReader(notDicStream, "UTF-8"));
            BufferedReader sentiIn = new BufferedReader(new InputStreamReader(sentiStream, "UTF-8"));
            BufferedReader degreeIn = new BufferedReader(new InputStreamReader(degreeStream, "UTF-8"));
            BufferedReader symbolIn = new BufferedReader(new InputStreamReader(symbolStream, "UTF-8"));

            String notDicrow;
            while ((notDicrow = notDicIn.readLine()) != null) {
                if (!dalian_not.contains(notDicrow.trim())) {
                    dalian_not.add(notDicrow.trim());
                }
            }
            //列名为：词语,词性种类,词义数,词义序号,情感分类,强度,新极性,极性,辅助情感分类,强度,极性,其他,
            String sentirow;
            while ((sentirow = sentiIn.readLine()) != null) {
                String[] rowList = sentirow.trim().split(",");
                if (rowList.length>1) {
                    dalian_senti.put(rowList[0], Double.parseDouble(rowList[5]) * Double.parseDouble(rowList[6]));
                }
            }
            String degreerow;
            while ((degreerow = degreeIn.readLine()) != null) {
                String[] rowList = degreerow.trim().split(" ");
                if (rowList.length>1) {
                    dalian_degree.put(rowList[0],Double.parseDouble(rowList[1]));
                }
            }
            String symbolDicrow;
            while ((symbolDicrow = symbolIn.readLine()) != null) {
                if (!symbol.contains(symbolDicrow.trim())) {
                    symbol.add(symbolDicrow.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (notDicStream != null) {
                try {
                    notDicStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (sentiStream != null) {
                try {
                    sentiStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (degreeStream != null) {
                try {
                    degreeStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (symbolStream != null) {
                try {
                    symbolStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (sc != null) {
                sc.close();
            }
        }
    }
}
