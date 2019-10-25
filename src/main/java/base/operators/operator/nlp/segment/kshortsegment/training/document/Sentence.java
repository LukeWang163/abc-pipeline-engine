package base.operators.operator.nlp.segment.kshortsegment.training.document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 句子，指的是以。，：！结尾的句子
 */
public class Sentence implements Serializable {
	public List<IWord> wordList;

	public Sentence(List<IWord> wordList) {
		this.wordList = wordList;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int i = 1;
		for (IWord word : wordList) {
			sb.append(word);
			if (i != wordList.size())
				sb.append(' ');
			++i;
		}
		return sb.toString();
	}

	static String POSSET[] = { "n", "np", "ng", "nd", "nl", "nh", "nhh", "nhf", "nhg", "nhy", "nhr", "nhw", "ns", "nn",
			"ni", "nt", "nz", "nze", "nzt", "nzf", "nzp", "nzw", "nzq", "nzc", "nzn", "nzc", "nzi", "nzb", "nzt", "v",
			"vt", "vi", "vl", "vu", "vd", "a", "aq", "as", "f", "r", "d", "p", "c", "u", "e", "o", "i", "in", "iv",
			"ia", "ic", "j", "jn", "jv", "ja", "h", "k", "g", "gn", "gv", "ga", "x", "w", "wp", "ws", "wu", "m", "mp",
			"mi", "mr", "md", "mo", "mr", "q", "mq", "mqm", "mqa", "mqt", "mqn", "mql", "mqr", "mqc", "mqw", "mqs",
			"mqaa" };

	public static List<IWord> splitSentence(String sentence) {
		List<IWord> wordList = new LinkedList<IWord>();
		String row = BCConvert.qj2bj(trim(sentence));
		if (!isValid(row)) {
			return wordList;
		}

		if (row.indexOf("[") == -1 && row.indexOf("]") == -1) {
			String words[] = split(row);
			if (words.length == 0)
				return wordList;
			for (int i = 0; i < words.length; i++) {
				String word = words[i];
				splitWordTag(word, wordList);
			}

		} else {
			String words[] = split(row);
			if (words.length == 0)
				return wordList;
			List<Word> comList = new ArrayList();
			boolean inComBox = false;
			for (int i = 0; i < words.length; i++) {
				String word = words[i];
				if (word.startsWith("[") && (!word.startsWith("[/w"))) {
					inComBox = true;
					splitInnerTag(word.substring(1), comList);
				} else {
					if (inComBox) {
						if (word.indexOf("]") != -1 && (!word.endsWith("]/w"))) {
							inComBox = false;
							String wordTags[] = splitComboxTailTag(word);
							if (wordTags != null) {
								String item = wordTags[0];
								splitInnerTag(item, comList);
								CompoundWord comWord = new CompoundWord(comList, wordTags[1]);
								wordList.add(comWord);
								comList = new ArrayList();
							} else {
								return wordList;
							}
						} else {
							splitInnerTag(word, comList);
						}

					} else {

						splitWordTag(word, wordList);
					}
				}

			}

		}
		return wordList;
	}

	static void splitWordTag(String wordTag, List<IWord> list) {
		for (int i = 0; i < POSSET.length; i++) {
			String endTag = "/" + POSSET[i];
			if (wordTag.endsWith(endTag)) {
				String item = wordTag.substring(0, wordTag.length() - endTag.length());
				String pos = POSSET[i];
				list.add(new Word(item, pos));
				return;
			}
		}
	}

	static void splitInnerTag(String wordTag, List<Word> list) {
		for (int i = 0; i < POSSET.length; i++) {
			String endTag = "/" + POSSET[i];
			if (wordTag.endsWith(endTag)) {
				String item = wordTag.substring(0, wordTag.length() - endTag.length());
				String pos = POSSET[i];
				list.add(new Word(item, pos));
				return;
			}
		}
	}

	static String[] splitComboxTailTag(String wordTag) {
		for (int i = 0; i < POSSET.length; i++) {
			String endTag = "]/" + POSSET[i];
			String item = null;
			if (wordTag.endsWith(endTag)) {
				item = wordTag.substring(0, wordTag.length() - endTag.length());
				return new String[] { item, POSSET[i] };
			}
			endTag = "]" + POSSET[i];
			if (wordTag.endsWith(endTag)) {
				item = wordTag.substring(0, wordTag.length() - endTag.length());
				return new String[] { item, POSSET[i] };
			}
		}
		return null;
	}

	public static boolean isValid(String row) {
		String words[] = split(row);
		if (words.length == 0)
			return true;
		int comCount = 0;
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			if (!isValidWordTag(word)) {
				System.out.println("Error word:" + word + "\t" + row);
				return false;
			}

			if (word.startsWith("[") && (!word.startsWith("[/w"))) {
				comCount++;
			}
			if (word.indexOf("]") != -1 && (!word.endsWith("]/w")&&!word.endsWith("]/wp"))) {
				comCount--;
			}
		}
		if (comCount != 0) {
			System.out.println("Error row:" + comCount + "\t" + row);
			return false;
		}

		return true;
	}

	public static boolean isValidWordTag(String word) {
		int validCount = 0;
		for (int i = 0; i < POSSET.length; i++) {
			String tailL = "/" + POSSET[i];
			String tailR = "]" + POSSET[i];
			if (word.endsWith(tailL) || word.endsWith(tailR)) {
				validCount++;
			}
		}
		if (validCount == 0)
			return false;
		return true;
	}

	public static String trim(String strArg) {
		char[] cVal = strArg.toCharArray();
		int p1 = 0;
		int len = cVal.length;

		// 从首到尾进行遍历，如果发现了第一个不是 ' ' 就break:表示终止了遍历，找到了首部到尾部第一个不为 ' ' 的位置
		while (p1 < len) {
			char c = cVal[p1];
			if (c == ' ' || c == '\t' || c == '	' || c == '　') {
				p1 += 1;
			} else {
				break;
			}
		}

		// 这说明 strArg 压根就是由空格字符组成的字符串
		if (p1 == len) {
			return "";
		}

		// 从尾部到首部进行遍历，如果发现了第一个不是 ' ' 就break:表示终止了遍历，找到了尾部到首部第一个不为 ' ' 的位置
		int p2 = len - 1;
		while (p2 >= 0) {
			char c = cVal[p2];
			if (c == ' ' || c == '\t' || c == '	' || c == '　') {
				p2 -= 1;
			} else {
				break;
			}
		}

		String subStr = strArg.substring(p1, p2 + 1);
		return subStr;
	}

	public static String[] split(String row) {
		ArrayList<String> list = new ArrayList();
		if (row == null || "".equals(row))
			return new String[0];
		String lString = "";
		for (int i = 0; i < row.length(); i++) {
			char c = row.charAt(i);
			if (c == ' ' || c == '\t' || c == '	') {
				if (!"".equals(lString)) {
					list.add(lString);
					lString = "";
				}
			} else {
				lString = lString + c;
			}
		}
		if (!"".equals(lString))
			list.add(lString);
		int size = list.size();
		String array[] = new String[size];
		for (int i = 0; i < list.size(); i++)
			array[i] = list.get(i);
		return array;
	}

	public static Sentence create(String sentence) {
		return new Sentence(splitSentence(sentence));
	}
}
