package base.operators.operator.nlp.pyconvert.py2cn;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pinyin to Chinese characters
 */
public class PinyinConvertChinese {
    DictManagerPinyin dictManager;
    public static final Map<String, String> CONVERSIONS = new HashMap<>();

    /**
     * Simplified Chinese to Traditional Chinese
     */
    public static final int pth = 1;
    private static final String CONVERTER_TYPE[] = new String[10];

    static {
        CONVERTER_TYPE[pth] = "pth";
    }

    private static final int NUM_OF_CONVERTERS = 10;
    private static final PinyinConvertChinese[] CONVERTERS = new PinyinConvertChinese[NUM_OF_CONVERTERS];

    /**
     * @param converterType 0 for tw2s and 4 for s2tw
     * @return
     */
    public static PinyinConvertChinese getInstance(int converterType) {

        if (converterType >= 0 && converterType < NUM_OF_CONVERTERS) {

            if (CONVERTERS[converterType] == null) {
                synchronized (PinyinConvertChinese.class) {
                    if (CONVERTERS[converterType] == null) {
                        CONVERTERS[converterType] = new PinyinConvertChinese(CONVERTER_TYPE[converterType]);
                    }
                }
            }
            return CONVERTERS[converterType];

        } else {
            return null;
        }
    }

    private PinyinConvertChinese(String config) {
        dictManager = new DictManagerPinyin();
    }

    /**
     * @return dict name
     */
    public String getDictName() {
        return dictManager.getDictName();
    }

    /**
     * convert normal chars
     *
     * @param in
     * @param isSpilt 拼音是否被拆分
     * @return
     */
    static IdentityHashMap<String, String> allDictMap;
    static List<Map<String, Object>> resultList; // 拼音数组

    public String convert(String in, Boolean isSpilt) {
        JSONObject result = new JSONObject();
        resultList = new ArrayList<>();
        String pinyin = in.trim() + " ";
        String regx = "ā|á|ǎ|à|ē|é|ě|è|ī|í|ǐ|ì|ō|ó|ǒ|ò|ū|ú|ǔ|ù|ǖ|ǘ|ǚ|ǜ";
        Pattern pattern = Pattern.compile(regx);
        Matcher matcher = pattern.matcher(pinyin);
        if(matcher.find()) {
        	pinyin = SpellTool.convertOutTone(pinyin);
        }
        if (!isSpilt) {
        	pinyin = pinyin.replace(" ","");
            pinyin = SpellTool.trimSpell(pinyin);
        }
        allDictMap = dictManager.getAllDictMap();
        List<String> dat_dict = dictManager.getDict();
        DoubleArrayTrie DOUBLE_ARRAY_TRIE = dictManager.getDat();
        int i = 0;
        int strLen = pinyin.length();
        Boolean flag = false;
        int flagIndexStart = 0;
        while (i < strLen) {
            String substr = pinyin.substring(i);
            List<Integer> commonPrefixList = DOUBLE_ARRAY_TRIE.commonPrefixSearch(substr);
            if (commonPrefixList.size() == 0) {
                if (flag) {
                    flagIndexStart = i;
                }
                flag = false;
                i++;
            } else {
                if (!flag) {
                    String unkownWord = pinyin.substring(flagIndexStart, i);
                    HandelUnknownWords(flagIndexStart,unkownWord);
                }
                flag = true;
                String py = "";
                String words = dat_dict.get(commonPrefixList.get(commonPrefixList.size() - 1));
                String string = "";
                for (Entry<String, String> entry : allDictMap.entrySet()) {
                    if (entry.getKey().equals(words)) {
                        string = string + entry.getValue();
                        py = words;
                    }
                }
                int blankCount = string.replaceAll("[^ ]", "").length();

                String[] array = string.split(" ");
                String arr;
                Map<String, Double> hashMap = new TreeMap<String, Double>();
                double sum = 0;
                for (int n = 0; n < blankCount; n++) {
                    arr = array[n];
                    String[] map_array = arr.split("\t");
                    sum += Integer.parseInt(map_array[1]);
                }
                for (int n = 0; n < blankCount; n++) {
                    arr = array[n];
                    String[] map_array = arr.split("\t");
                    if (sum != 0) {
                        DecimalFormat df = new DecimalFormat("#.######");
                        hashMap.put(map_array[0], Double.valueOf(df.format((Integer.parseInt(map_array[1]) / sum))));
                    } else {
                        hashMap.put(map_array[0], (double) 0);
                    }
                }
                ArrayList<Entry<String, Double>> arrayList = new ArrayList<Entry<String, Double>>(hashMap.entrySet());
                //sort
                Collections.sort(arrayList, new Comparator<Entry<String, Double>>() {
                    public int compare(Entry<String, Double> map1,
                                       Entry<String, Double> map2) {
                        return map1.getValue() > map2.getValue() ? -1 : (map1.getValue() < map2.getValue()) ? 1 : 0;
                    }
                });
                List<Map<String, Object>> cnWordList = new ArrayList<>();
                //out
                for (Entry<String, Double> entry : arrayList) {
                    BigDecimal bd = new BigDecimal(entry.getValue());
                    JSONObject cnWord = new JSONObject();
                    cnWord.put("words", entry.getKey());
                    cnWord.put("frequency", bd.setScale(6, BigDecimal.ROUND_HALF_UP).toPlainString());
                    cnWordList.add(cnWord);
                }
                JSONObject pyResult = new JSONObject();
                pyResult.put("pinyin", py.trim());
                pyResult.put("offset", i);
                pyResult.put("hanzi", cnWordList.subList(0, cnWordList.size() > 10 ? 10 : cnWordList.size()));
                resultList.add(pyResult);
                i += words.length();
            }
        }

        result.put("pinyin", pinyin.trim());
        result.put("status", "success");
        result.put("result", resultList);
        return JSON.toJSONString(result);
    }

    public String getFirstLetters(String text) {
        String firstLetters = "";
        text = text.replaceAll("[.,]", ""); // Replace dots, etc (optional)
        for (String s : text.split(" ")) {
            firstLetters += s.charAt(0);
        }
        return firstLetters;
    }

    public void HandelUnknownWords(Integer flagIndexStart,String sb) {
        if ((sb.toString().trim()).equals("")) {
            System.out.print("");
        } else {
            String string = "";
            for (Entry<String, String> entry : allDictMap.entrySet()) {
                if ((entry.getKey().substring(0, 1)).equals((sb.toString().trim()).substring(0, 1))) {
                    String array[] = (entry.getKey().trim()).split(" ");
                    if((sb.toString().trim()).length() == array.length) {
                        if(getFirstLetters(entry.getKey()).equals(sb.toString().trim())){
                            string = string + entry.getValue();
                        }
                    }
                }
            }
            int blankCount = string.replaceAll("[^ ]", "").length();
            String[] array = string.split(" ");
            String arr;
            Map<String, Double> hashMap = new TreeMap<String, Double>();
            double sum = 0;
            for (int n = 0; n < blankCount; n++) {
                arr = array[n];
                String[] map_array = arr.split("\t");
                sum += Integer.parseInt(map_array[1]);
            }
            for (int n = 0; n < blankCount; n++) {
                arr = array[n];
                String[] map_array = arr.split("\t");
                if (sum != 0) {
                    DecimalFormat df = new DecimalFormat("#.######");
                    hashMap.put(map_array[0], Double.valueOf(df.format((Integer.parseInt(map_array[1]) / sum))));
                } else {
                    hashMap.put(map_array[0], (double) 0);
                }
            }
            ArrayList<Entry<String, Double>> arrayList = new ArrayList<Entry<String, Double>>(
                    hashMap.entrySet());
            Collections.sort(arrayList, new Comparator<Entry<String, Double>>() {
                public int compare(Entry<String, Double> map1,
                                   Entry<String, Double> map2) {
                    return map1.getValue() > map2.getValue() ? -1 : (map1.getValue() < map2.getValue()) ? 1 : 0;
                }
            });
            List<Map<String, Object>> cnWordsList = new ArrayList<>();
            //out
            for (Entry<String, Double> entry : arrayList) {
                BigDecimal bd = new BigDecimal(entry.getValue());
                JSONObject cnWord = new JSONObject();
                cnWord.put("words", entry.getKey());
                cnWord.put("frequency", bd.setScale(6, BigDecimal.ROUND_HALF_UP).toPlainString());
                cnWordsList.add(cnWord);
            }
            JSONObject pyResult = new JSONObject();
            pyResult.put("pinyin", sb.toString().trim());
            pyResult.put("offset", flagIndexStart);
            pyResult.put("hanzi", cnWordsList.subList(0, cnWordsList.size() > 10 ? 10 : cnWordsList.size()));
            resultList.add(pyResult);
        }
    }
}
