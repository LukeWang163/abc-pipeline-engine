package base.operators.operator.nlp.segment.mechanicalsegment;

public class IsCharType {

	// 根据Unicode编码完美的判断中文汉字和中文符号
	public static boolean isChinese(char c) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		//Character.UnicodeBlock.of(4567);
		if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
				|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
				|| ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
				|| ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
			return true;
		}
		return false;
	}
	
	// 根据Unicode编码完美的判断中文汉字和中文符号
	public static boolean isChinese(int unicode) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(unicode);
		if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
				|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
				|| ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
				|| ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
			return true;
		}
		return false;
	}

	// 完整的判断字符串中是否都为中文汉字和中文符号
	public static boolean isChinese(String strName) {
		char[] ch = strName.toCharArray();
		for (int i = 0; i < ch.length; i++) {
			char c = ch[i];
			if (!isChinese(c)) {
				return false;
			}
		}
		return true;
	}
	
	//判断是否数字
	public static boolean isNum(char c){
		return Character.isDigit(c);
	}
	public static boolean isNum(int codePoint){
		return Character.isDigit(codePoint);
	}

	public static boolean isNum(String str){
		String regex_num = "(\\+|\\-)?\\d+(\\.\\d+)?";
		return str.matches(regex_num);
	}
	
	//判断是否字母(asc码)
	public static boolean isLetter(char c){
		//return Character.isLetter(c);
		int i =(int)c;
		return ((i>=65&&i<=90)||(i>=97&&i<=122)) ? true : false;
	}
	
/*	public static boolean isLetter(int codePoint){
		return Character.isLetter(codePoint);
	}*/
	
	//判断字符串中是否都是字母(asc码)
	public static boolean isLetter(String str){
		char[] ch = str.toCharArray();
		for (int i = 0; i < ch.length; i++) {
			char c = ch[i];
			if (!isLetter(c)) {
				return false;
			}
		}
		return true;
	}
	//判断单个符号是不是中英文符号
	public static boolean isPunctuation(String str){
		String reg = "\\pP||\\pS||\\pC";
		return str.matches(reg);
	}

}

