package base.operators.operator.nlp.segment.kshortsegment.predict.segment;


import base.operators.operator.nlp.segment.kshortsegment.predict.tagger.POS;

public class TextTools {
	/**
	 * 判断字符的类别
	 * 
	 * @param codePoint
	 * @return
	 */
	public static POS testCharType(int codePoint) {
		if (isChineseCharacter(codePoint))
			return POS.x;
		if (isAsciiCharacter(codePoint))
			return POS.ws;
		if (isAsciiNumber(codePoint))
			return POS.m;
		if (isAsciiSymbols(codePoint))
			return POS.wp;
		if (isChineseSymbols(codePoint))
			return POS.wp;
		return POS.x;
	}

	/**
	 * 判断字符是否属于Ascii标点符号
	 * 
	 * @param
	 * @return
	 */
	public static boolean isAsciiSymbols(int codePoint) {
		if ((codePoint >= 33 && codePoint <= 47) || (codePoint >= 58 && codePoint <= 64)
				|| (codePoint >= 91 && codePoint <= 96) || (codePoint >= 123 && codePoint <= 126))
			return true;
		return false;
	}

	/**
	 * 判断是Ascaii数字0-9
	 * 
	 * @param codePoint
	 * @return
	 */
	public static boolean isAsciiNumber(int codePoint) {
		if (codePoint >= 48 && codePoint <= 57)
			return true;
		return false;
	}

	/**
	 * 判断是否Ascaii字母A-Z/a-z
	 * 
	 * @param codePoint
	 * @return
	 */
	public static boolean isAsciiCharacter(int codePoint) {
		if ((codePoint >= 65 && codePoint <= 90) || (codePoint >= 97 && codePoint <= 122))
			return true;
		return false;
	}

	// 使用UnicodeScript方法判断中文汉字
	public static boolean isChineseCharacter(int codePoint) {
		Character.UnicodeScript sc = Character.UnicodeScript.of(codePoint);
		if (sc == Character.UnicodeScript.HAN) {
			return true;
		}
		return false;
	}

	// 根据UnicodeBlock方法判断中文标点符号
	public static boolean isChineseSymbols(int codePoint) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(codePoint);
		if (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS
				|| ub == Character.UnicodeBlock.VERTICAL_FORMS) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isAllNum(String str) {

		if (str != null) {
			int i = 0;
			String temp = str + " ";
			// 判断开头是否是+-之类的符号
			if ("±+—-＋".indexOf(temp.substring(0, 1)) != -1)
				i++;
			/** 如果是全角的０１２３４５６７８９ 字符* */
			while (i < str.length() && "０１２３４５６７８９".indexOf(str.substring(i, i + 1)) != -1)
				i++;

			// Get middle delimiter such as .
			if (i < str.length()) {
				String s = str.substring(i, i + 1);
				if ("∶·．／".indexOf(s) != -1 || ".".equals(s) || "/".equals(s)) {// 98．1％
					i++;
					while (i + 1 < str.length() && "０１２３４５６７８９".indexOf(str.substring(i + 1, i + 2)) != -1)

						i++;
				}
			}

			if (i >= str.length())
				return true;

			while (i < str.length() && cint(str.substring(i, i + 1)) >= 0 && cint(str.substring(i, i + 1)) <= 9)
				i++;
			// Get middle delimiter such as .
			if (i < str.length()) {
				String s = str.substring(i, i + 1);
				if ("∶·．／".indexOf(s) != -1 || ".".equals(s) || "/".equals(s)) {// 98．1％
					i++;
					while (i + 1 < str.length() && "0123456789".indexOf(str.substring(i + 1, i + 2)) != -1)
						i++;
				}
			}

			if (i < str.length()) {

				if ("百千万亿佰仟％‰".indexOf(str.substring(i, i + 1)) == -1 && !"%".equals(str.substring(i, i + 1)))
					i--;
			}
			if (i >= str.length())
				return true;
		}
		return false;
	}

	/**
	 * 把表示数字含义的字符串转你成整形
	 *
	 * @param str
	 *            要转换的字符串
	 * @return 如果是有意义的整数，则返回此整数值。否则，返回-1。
	 */
	public static int cint(String str) {
		if (str != null)
			try {
				int i = new Integer(str).intValue();
				return i;
			} catch (NumberFormatException e) {

			}

		return -1;
	}

	/**
	 * 是否全为英文
	 *
	 * @param text
	 * @return
	 */
	public static boolean isAllLetter(String text) {
		for (int i = 0; i < text.length(); ++i) {
			char c = text.charAt(i);
			if ((((c < 'a' || c > 'z')) && ((c < 'A' || c > 'Z')))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 是否全为英文或字母
	 *
	 * @param text
	 * @return
	 */
	public static boolean isAllLetterOrNum(String text) {
		for (int i = 0; i < text.length(); ++i) {
			char c = text.charAt(i);
			if ((((c < 'a' || c > 'z')) && ((c < 'A' || c > 'Z')) && ((c < '0' || c > '9')))) {
				return false;
			}
		}

		return true;
	}

	public static boolean isAllChineseNum(String word) {// 百分之五点六的人早上八点十八分起床

		String chineseNum = "零○一二两三四五六七八九十廿百千万亿壹贰叁肆伍陆柒捌玖拾佰仟∶·．／点";//
		String prefix = "几数第上成";

		if (word != null) {
			String temp = word + " ";
			for (int i = 0; i < word.length(); i++) {

				if (temp.indexOf("分之", i) != -1)// 百分之五
				{
					i += 2;
					continue;
				}

				String tchar = temp.substring(i, i + 1);
				if (chineseNum.indexOf(tchar) == -1 && (i != 0 || prefix.indexOf(tchar) == -1))
					return false;
			}
			return true;
		}

		return false;
	}

	/**
	 * 判断字符串是否是年份
	 *
	 * @param snum
	 * @return
	 */
	public static boolean isYearTime(String snum) {
		if (snum != null) {
			int len = snum.length();
			String first = snum.substring(0, 1);

			// 1992年, 98年,06年
			if (isAllSingleByte(snum) && (len == 4 || len == 2 && (cint(first) > 4 || cint(first) == 0)))
				return true;
			if (isAllNum(snum) && (len >= 6 || len == 4 && "０５６７８９".indexOf(first) != -1))
				return true;
			if (getCharCount("零○一二三四五六七八九壹贰叁肆伍陆柒捌玖", snum) == len && len >= 2)
				return true;
			if (len == 4 && getCharCount("千仟零○", snum) == 2)// 二仟零二年
				return true;
			if (len == 1 && getCharCount("千仟", snum) == 1)
				return true;
			if (len == 2 && getCharCount("甲乙丙丁戊己庚辛壬癸", snum) == 1
					&& getCharCount("子丑寅卯辰巳午未申酉戌亥", snum.substring(1)) == 1)
				return true;
		}
		return false;
	}

	/**
	 * 是否全是单字节
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isAllSingleByte(String str) {
		if (str != null) {
			int len = str.length();
			int i = 0;
			while (i < len && str.charAt(i) < 128) {
				i++;
			}
			if (i < len)
				return false;
			return true;
		}
		return false;
	}

	/**
	 * 得到字符集的字符在字符串中出现的次数
	 *
	 * @param charSet
	 * @param word
	 * @return
	 */
	public static int getCharCount(String charSet, String word) {
		int nCount = 0;

		if (word != null) {
			String temp = word + " ";
			for (int i = 0; i < word.length(); i++) {
				String s = temp.substring(i, i + 1);
				if (charSet.indexOf(s) != -1)
					nCount++;
			}
		}

		return nCount;
	}

}
