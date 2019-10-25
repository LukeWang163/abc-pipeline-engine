package base.operators.operator.nlp.pyconvert.cn2py;

import java.util.*;


/**
 * Conversion of Chinese characters into pinyin
 */
public class ChineseConvertPinyin {

	public static final Map<String, String> CONVERSIONS = new HashMap<>();
	
	/**
	 * WITH_TONE_NUMBER--The number represents the tone
	 *  WITHOUT_TONE--Without tone
	 *  WITH_TONE_MARK--With tone
	 */
	public static final int WITH_TONE_MARK=0;
	public static final int WITH_TONE_NUMBER=1;
	public static final int WITHOUT_TONE=2;
	private static final String PINYIN_TYPE[] = new String[3];
	
	static {
		PINYIN_TYPE[WITH_TONE_MARK] = "WITH_TONE_MARK";
		PINYIN_TYPE[WITH_TONE_NUMBER] = "WITH_TONE_NUMBER";
		PINYIN_TYPE[WITHOUT_TONE] = "WITHOUT_TONE";
	}

	private static final int NUM_OF_PINYIN = 3;
	private static final ChineseConvertPinyin[] PINYINS = new ChineseConvertPinyin[NUM_OF_PINYIN];
	
    private static final String ALL_UNMARKED_VOWEL = "aeiouv";
    private static final String ALL_MARKED_VOWEL = "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜ";//All tones of the phonetic alphabet

	DictManagerChinese dictManager;
	/**
	 * pinyinFormat--Phonetic type
	 */
	private final int pinyinFormat;

	/**
	 *
	 * @param format
	 *            0 for WITH_TONE_MARK and 2 for WITHOUT_TONE
	 * @return
	 */
	public static ChineseConvertPinyin getInstance(int format) {
		if (format >= 0 && format < NUM_OF_PINYIN) {
			if (PINYINS[format] == null) {
				synchronized (ChineseConvertPinyin.class) {
					if (PINYINS[format] == null) {
						PINYINS[format] = new ChineseConvertPinyin(format);
					}
				}
			}			
			return PINYINS[format];				
		} else {
			return null;
		}
	}

	/**
	 * Convert API
	 * 
	 * @param text
	 * @param pinyinFormat
	 * @return
	 */
	public static String convert(String text, int pinyinFormat) {
		ChineseConvertPinyin instance = getInstance(pinyinFormat);
		return instance.convert(text);
	}

	private ChineseConvertPinyin(int format) {
		dictManager = new DictManagerChinese();
		this.pinyinFormat=format;
	}

	/**
	 *
	 * @return dict name
	 */
	public String getDictName() {
		return dictManager.getDictName();
	}


	/**
	 * Convert method
	 * 
	 * @param in
	 * @return
	 */
	public  String convert(String in) {
		// Check CodePoint
		int codePoint = in.codePointCount(0, in.length());															
		int strLen = in.length();
		// Only has normal chars	
		if (codePoint == strLen) {		
			return convertNormalString(in);
		} else {
			// Has complex chars
			return convertComplexString(in);
		}
	}
	
	/**
	 * convert complex chars
	 * 
	 * @param in
	 * @return
	 */
	private String convertComplexString(String in) {
		
		StringBuilder outString = new StringBuilder();
		String stack = new String();
		Set confSets = dictManager.getConfSets();
		TreeMap<String, String> allDictMap = dictManager.getAllDictMap();
		int codePointNumber = in.codePointCount(0, in.length());
		for (int i = 0; i < codePointNumber; i++) {
			int index = in.offsetByCodePoints(0, i);
			int codePoint = in.codePointAt(index);
			String c = new String(Character.toChars(codePoint));
			stack = stack + c;
			if (confSets.contains(stack)) {
			} else if (allDictMap.containsKey(stack)) {
				String[] pinyin = allDictMap.get(stack).trim().split("\t");				
				String dictPair[] = formatPinyin(pinyin[0]);				
				for (int ii =0;ii<dictPair.length;ii++) {
					if(ii==dictPair.length-1){						
						outString.append(dictPair[ii]+" ");	
					}else{
						outString.append(dictPair[ii]+" ");	
					}	
					stack = "";
				}
			} else {
				int stackCodePointNumber = stack.codePointCount(0, stack.length());
				String sequence = getSubStringByCodePoint(stack.toString(), 0, stackCodePointNumber - 1);
				stack = getSubStringByCodePoint(stack, stackCodePointNumber - 1, stackCodePointNumber);
				flushCodePointStack(outString, new StringBuilder(sequence));
			}
		}
		flushCodePointStack(outString, new StringBuilder(stack));
		return outString.toString();
	}

	private void flushCodePointStack(StringBuilder outString, StringBuilder stackString) {
		TreeMap<String, String> allDictMap = dictManager.getAllDictMap();
		while (stackString.length() > 0) {
			if (allDictMap.containsKey(stackString.toString())) {	
				String[] pinyin = allDictMap.get(stackString.toString()).trim().split("\t");		
				String[] dictPair = formatPinyin(pinyin[0]);
				outString.append(dictPair[0]+" ");
				stackString.setLength(0);
			} else {
				int codePoint = stackString.codePointAt(0);
				String firtsChar = new String(Character.toChars(codePoint));
				outString.append(firtsChar+" ");
				if (isExtendCodePoint(stackString.codePointAt(0))) {
					stackString.delete(0, 2);
				} else {
					stackString.delete(0, 1);
				}
			}
		}
	}

	/**
	 * Check a char
	 * 
	 * @param codePoint
	 * @return
	 */
	private boolean isExtendCodePoint(int codePoint) {
		char c[] = Character.toChars(codePoint);
		if (c.length == 1)
			return false;
			return true;
	}

	/**
	 * getSubStringByCodePoint
	 * 
	 * @param s
	 * @param begin
	 * @param end
	 * @return
	 */
	private String getSubStringByCodePoint(String s, int begin, int end) {
		int codePointNumber = s.codePointCount(0, s.length());
		int startLoc = begin;
		if (startLoc < 0)
			startLoc = 0;
		int endLoc = end;
		if (endLoc > codePointNumber)
			endLoc = codePointNumber;
		StringBuilder sb = new StringBuilder();
		for (int i = begin; i < endLoc; i++) {
			int index = s.offsetByCodePoints(0, i);
			int codePoint = s.codePointAt(index);
			sb.appendCodePoint(codePoint);
		}
		return sb.toString();
	}

	/**
	 * convert normal chars
	 * 
	 * @param in
	 * @return
	 */
	private String convertNormalString(String in) {
		StringBuilder outString = new StringBuilder();
		StringBuilder stackString = new StringBuilder();
		Set confSets = dictManager.getConfSets();
		TreeMap<String, String> allDictMap = dictManager.getAllDictMap();
		for (int i = 0; i < in.length(); i++) {		
			char c = in.charAt(i);
			String key = "" + c;
			stackString.append(key);	
			System.out.println(stackString.toString());
			if (confSets.contains(stackString.toString())) {
			} else if (allDictMap.containsKey(stackString.toString())) {
				String[] pinyin = allDictMap.get(stackString.toString()).trim().split("\t");
				String dictPair[] = formatPinyin(pinyin[0]);	
				if(stackString.length() == 1) {
					outString.append(dictPair[0]);
					stackString.setLength(0);
				} else {
					for (int ii =0;ii<dictPair.length;ii++) {					
						if(ii==dictPair.length-1){
							outString.append(dictPair[ii]+" ");							
						}else{						
							outString.append(dictPair[ii]+" ");	
						}	
						if(ii<1){
						stackString.setLength(ii);
						}
					}
				}
			} else {	
				CharSequence sequence = stackString.subSequence(0, stackString.length() - 1);	
				stackString.delete(0, stackString.length() - 1);
				flushStack(outString, new StringBuilder(sequence));
			}
		}

		flushStack(outString, stackString);		
		return outString.toString();		
	}

	private void flushStack(StringBuilder outString, StringBuilder stackString) {
		TreeMap<String, String> allDictMap = dictManager.getAllDictMap();
		while (stackString.length() > 0) {
			if (allDictMap.containsKey(stackString.toString())) {	
				if(stackString.length() > 1){
					String[] pinyin = allDictMap.get(stackString.toString()).trim().split("\t");		
					if(!pinyin[0].contains(" ") ){	
						String[] dictPair = formatPinyin(pinyin[0]);
						outString.append(dictPair[0]+" ");
						stackString.setLength(0);
					}
					else{
						String[] dictPair = formatPinyin(pinyin[0]);	
						if(dictPair.length > 1) {
							outString.append(dictPair[0]+" "+dictPair[1]+" ");
						} else {
							outString.append(dictPair[0] + " ");
						}
						stackString.setLength(0);
					}
				}else{
					String[] pinyin = allDictMap.get(stackString.toString()).trim().split(" ");			
					String[] dictPair = formatPinyin(pinyin[0]);
					outString.append(dictPair[0]+" ");
					stackString.setLength(0);
				}
			} else {
				if(stackString.length()>1){
					char c = stackString.charAt(0);
					String string = String.valueOf(c);
					String[] pinyin = allDictMap.get(string).split(" ");			
					String[] dictPair = formatPinyin(pinyin[0]);	
					outString.append(dictPair[0]+" ");
					stackString.delete(0, 1);					
				}else{
				outString.append("" + stackString.charAt(0) + " ");
				stackString.delete(0, 1);
				}
			}
		}
	}
	
	 /**
     * Pinyin with tone format conversion to digital representation of tone format
     * 
     * @param pinyinArrayString
     *            pinyinarraystring
     * @return Pinyin for digital representation of tone format
     */
    private static  String[] convertToneNumber(String pinyinArrayString) {
        String[] pinyinArray = pinyinArrayString.split(" ");
        for (int i = pinyinArray.length - 1; i >= 0; i--) {
            boolean hasMarkedChar = false;
            // Will replace the Pinyin in ü v
            String originalPinyin = pinyinArray[i].replace("ü", "v"); 
            for (int j = originalPinyin.length() - 1; j >= 0; j--) {
                char originalChar = originalPinyin.charAt(j);
                // Search with the phonetic alphabet, if there is replaced by the corresponding without the tone of the English alphabet
                if (originalChar < 'a' || originalChar > 'z') {
                    int indexInAllMarked = ALL_MARKED_VOWEL.indexOf(originalChar);                   
                    int toneNumber = indexInAllMarked % 4 + 1; 
                    // Tone number
                    char replaceChar = ALL_UNMARKED_VOWEL.charAt((indexInAllMarked - indexInAllMarked % 4) / 4);
                    pinyinArray[i] = originalPinyin.replace(String.valueOf(originalChar), String.valueOf(replaceChar)) + toneNumber;
                    hasMarkedChar = true;
                }             
            }            
            if (!hasMarkedChar) {
                // Can not find the tone of the phonetic alphabet is soft, with the number 5
                pinyinArray[i] = originalPinyin + "5";
            }
        }
       
		return pinyinArray;
    }

    /**
     * Converts Pinyin with tone format to Pinyin without tone format
     * 
     * @param pinyinArrayString
     *            Pinyin with tone format
     * @return Pinyin without tone
     */
    private static  String[] convertOutTone(String pinyinArrayString) {
        String[] pinyinArray;
        for (int i = ALL_MARKED_VOWEL.length() - 1; i >= 0; i--) {
            char originalChar = ALL_MARKED_VOWEL.charAt(i);
            char replaceChar = ALL_UNMARKED_VOWEL.charAt((i - i % 4) / 4);
            pinyinArrayString = pinyinArrayString.replace(String.valueOf(originalChar), String.valueOf(replaceChar));
        }   
        // Replace the Pinyin in ü v
        pinyinArray = pinyinArrayString.replace("ü", "v").split(" ");
        // After the tone is removed, there may be a repeat of the phonetic alphabet.
        LinkedHashSet<String> pinyinSet = new LinkedHashSet<String>();
        for (String pinyin : pinyinArray) {
            pinyinSet.add(pinyin);
        }
        return pinyinArray;
    }
  
    /**
     * The phonetic alphabet is formatted into the corresponding format
     * 
     * @param pinyinString
     *            Pinyin with tone
     *            Phonetic format：WITH_TONE_NUMBER--The number represents the tone，WITHOUT_TONE--Without tone，WITH_TONE_MARK--With tone
     * @return Pinyin after format conversion
     */
    private   String[] formatPinyin(String pinyinString) {   	
        if (pinyinFormat == WITH_TONE_MARK) {       
            return pinyinString.split(" ");
        } else if (pinyinFormat == WITH_TONE_NUMBER) {    	
            return convertToneNumber(pinyinString);
        } else if (pinyinFormat == WITHOUT_TONE) {
            return convertOutTone(pinyinString);
        }     
        return new String[0];
    }
    	
}
