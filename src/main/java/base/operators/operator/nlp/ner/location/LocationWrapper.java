package base.operators.operator.nlp.ner.location;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;

public class LocationWrapper {

	private String word;

	private String initWord;

	private String topCode;

	private Location location;

	private int wordLength;

	private int startIndex;

	private int endIndex;

	private int topLevel;

	private boolean hasExtendedCharset = false;

	private ArrayList<Location> locationPath = null;

	private boolean mergeFlag = false;
	/**
	 * word在词典中是否只存在唯一性
	 */
	private boolean isUnique;

	static NSDictionary DICT;;

	public LocationWrapper(String word, Location location, int startIndex, int type, InputStream stream) {
		DICT = NSDictionary.getIstance(type, stream);
		this.word = word;
		this.initWord = word;
		this.location = location;
		this.topLevel = location.getLevel();
		this.topCode = location.getCode();
		wordLength = (word == null ? 0: word.codePointCount(0, word.length()));
		if (word == null)
			hasExtendedCharset = false;
		if (word != null && word.length() != wordLength) {
			hasExtendedCharset = true;
		}
		this.startIndex = startIndex;
		this.endIndex = startIndex + wordLength - 1;
		mergeFlag = false;
		// word在词典中是否唯一
		Set<Location> wordLocSet = DICT.getLocationWordMap().get(word);
		if (wordLocSet == null || wordLocSet.size() == 1) {
			isUnique = true;
		} else {
			isUnique = false;
		}
	}

	private LocationWrapper(String word, Location location, int startIndex,
			boolean flag, String topCode, int topLevel) {
		this.word = word;
		this.initWord = word;
		this.location = location;
		this.topLevel = topLevel;
		this.topCode = topCode;
		wordLength = (word == null ? 0: word.codePointCount(0, word.length()));
		if (word == null)
			hasExtendedCharset = false;
		if (word!= null && word.length() != wordLength) {
			hasExtendedCharset = true;
		}
		this.startIndex = startIndex;
		this.endIndex = startIndex + wordLength - 1;
		this.mergeFlag = flag;
		// word在词典中是否唯一
		Set<Location> wordLocSet = DICT.getLocationWordMap().get(word);
		if (wordLocSet == null || wordLocSet.size() == 1) {
			isUnique = true;
		} else {
			isUnique = false;
		}

	}

	public LocationWrapper mergeLocation(LocationWrapper locWord) {
		String newWord = word + locWord.getWord();
		LocationWrapper location = new LocationWrapper(newWord,
				locWord.getLocation(), startIndex, true, topCode, topLevel);
		return location;
	}

	public int getWordLength() {
		return wordLength;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	/**
	 * 获得地址路径信息
	 * 
	 * @return
	 */
	public ArrayList<Location> getLocationPath() {
		if (locationPath == null)
			locationPath = new ArrayList();
		if (locationPath.size() != 0)
			return locationPath;
		locationPath.add(location);
		String parentCode = location.getParentCode();
		boolean loopFlag = true;

		while (loopFlag) {
			Location parentLoc = DICT.getLocMap().get(parentCode);

			if (parentLoc == null) {
				loopFlag = false;
				break;
			}

			locationPath.add(parentLoc);
			parentCode = parentLoc.getParentCode();

			if (parentCode == null || parentCode.equals(DICT.TOPCODE)
					|| parentCode.equals("-1") || parentCode.equals("")) {
				loopFlag = false;
				break;
			}
		}

		return locationPath;
	}

	/**
	 * 根据既有地址得到上级路径
	 * 
	 * @param loc
	 * @return
	 */
	public ArrayList<Location> getLocationPath(Location another) {
		ArrayList<Location> cLocationPath = new ArrayList();
		cLocationPath.add(another);
		String parentCode = another.getParentCode();
		boolean loopFlag = true;
		String thisCode = another.getCode();
		while (loopFlag) {
			Location parentLoc = DICT.getLocMap().get(parentCode);
			if (parentLoc == null) {
				loopFlag = false;
				break;
			}
			thisCode = parentLoc.getCode();
			cLocationPath.add(parentLoc);
			parentCode = parentLoc.getParentCode();
			if (parentCode == null || parentCode.equals(DICT.TOPCODE)
					|| parentCode.equals("-1") || parentCode.equals(""))
				loopFlag = false;
			break;
		}

		return cLocationPath;
	}

	/**
	 * 返回地址路径编码列表（从本级到上级从前到后，包含本级编码）
	 * 
	 * @return
	 */
	public ArrayList<String> getLocationStringPath(Location another) {
		ArrayList<Location> path = getLocationPath(another);
		ArrayList pathList = new ArrayList();
		for (int i = 0; i < path.size(); i++) {
			Location loc = path.get(i);
			pathList.add(loc.getCode());
		}
		return pathList;
	}

	/**
	 * 返回地址路径编码列表（从本级到上级从前到后，包含本级编码）
	 * 
	 * @return
	 */
	public ArrayList<String> getLocationStringPath() {
		ArrayList<Location> path = getLocationPath();
		ArrayList pathList = new ArrayList();
		for (int i = 0; i < path.size(); i++) {
			Location loc = path.get(i);
			pathList.add(loc.getCode());
		}
		return pathList;
	}

	/**
	 * 返回低端地址到高端地址的路径编码列表（从本级到上级从前到后，包含本级编码）
	 * 
	 * @return
	 */
	public ArrayList<String> getLocationStringPathToTopCode() {
		ArrayList<Location> path = getLocationPath();
		ArrayList pathList = new ArrayList();
		for (int i = 0; i < path.size(); i++) {
			Location loc = path.get(i);
			pathList.add(loc.getCode());
			if (loc.getCode().equals(topCode))
				break;
		}
		return pathList;
	}

	/**
	 * 判断本地址是不是对方地址的上级（包含本节点相同视为上级）
	 * 
	 * @param child
	 * @return
	 */
	public boolean isFather(LocationWrapper child) {
		if (location.getLevel() > child.getLocation().getLevel())
			return false;
		ArrayList<String> myParentList = getLocationStringPathToTopCode();

		for (int i = 0; i < myParentList.size(); i++) {
			String pCode = myParentList.get(i);

			ArrayList<Location> locationPath = child.getLocationPath();

			for (int j = 0; j < locationPath.size(); j++) {
				String thisPCode = locationPath.get(j).getCode();
				if (pCode.equals(thisPCode)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 判断两个地址是否有共同的直接上级编码
	 * 
	 * @param child
	 * @return
	 */
	public boolean isSameFather(LocationWrapper brother) {
		return location.getParentCode().equals(
				brother.getLocation().getParentCode());
	}

	/**
	 * 计算两个地址间的逻辑距离（以共同的根节点为基础）
	 * 
	 * @param otherLoc
	 * @return
	 */
	public int getDistance(LocationWrapper otherLoc) {
		ArrayList<String> thisPath = getLocationStringPath();
		ArrayList<String> otherPath = otherLoc.getLocationStringPath();
		String thisPathArray[] = convertListToArray(thisPath);
		String otherPathArray[] = convertListToArray(otherPath);
		int thisSize = thisPathArray.length;
		int otherSize = otherPathArray.length;
		int minSize = thisSize < otherSize ? thisSize : otherSize;
		int sameFatherLevel = -1;

		for (int i = 0; i < minSize; i++) {
			if (thisPathArray[i].equals(otherPathArray[i])) {
				sameFatherLevel = i;
			} else {
				break;
			}
		}
		if (sameFatherLevel == -1) {// 不存在任何直接上级
			return thisSize + otherSize;
		} else {// 存在共同上级
			return (thisSize - sameFatherLevel) + (otherSize - sameFatherLevel);
		}
	}

	/**
	 * 计算两个地址间的逻辑距离（以共同的根节点为基础）
	 * 
	 * @param otherLoc
	 * @return
	 */
	public int getDistance(Location otherLoc) {

		ArrayList<String> thisPath = getLocationStringPath();
		ArrayList<String> otherPath = getLocationStringPath(otherLoc);

		String thisPathArray[] = convertListToArray(thisPath);
		String otherPathArray[] = convertListToArray(otherPath);
		int thisSize = thisPathArray.length;
		int otherSize = otherPathArray.length;
		int minSize = thisSize < otherSize ? thisSize : otherSize;
		int sameFatherLevel = -1;

		for (int i = 0; i < minSize; i++) {
			if (thisPathArray[i].equals(otherPathArray[i])) {
				sameFatherLevel = i;
			} else {
				break;
			}
		}
		if (sameFatherLevel == -1) {// 不存在任何直接上级
			return thisSize + otherSize;
		} else {// 存在共同上级
			return (thisSize - sameFatherLevel) + (otherSize - sameFatherLevel);
		}
	}

	private String[] convertListToArray(ArrayList<String> list) {
		if (list == null || list.size() == 0)
			return null;
		String[] array = new String[list.size()];
		int index = 0;
		for (int i = list.size() - 1; i >= 0; i--) {
			array[index] = list.get(i);
			index++;
		}
		return array;
	}

	public boolean isMergeFlag() {
		return mergeFlag;
	}

	public String getWord() {
		return word;
	}

	public Location getLocation() {
		return location;
	}

	public int hashCode() {
		return (topCode + word + startIndex).hashCode();
	}

	public String getTopCode() {
		return topCode;
	}

	public int getTopLevel() {
		return topLevel;
	}

	/**
	 * 取得高低位地址级别差
	 * 
	 * @return
	 */
	public int getLevelDistance() {
		return location.getLevel() - topLevel;
	}

	public boolean equals(LocationWrapper loc) {
		if (this == loc)
			return true;
		if (this.word.equals(loc.getWord())
				&& topCode.equals(loc.getLocation().getCode()))
			return true;
		return false;
	}

	public boolean isUnique() {
		return isUnique;
	}

	public String toString() {
		// return "[{word:" + word + ",code:" + location.getCode()
		// + ",parentCode:" + location.getParentCode() + ",start:"
		// + startIndex + ",endIndex:" + endIndex + ",mergeFlag："
		// + mergeFlag + "}{"+location+"}]\n";
		return "[{word:" + word + ",code:" + location.getCode()
				+ ",parentCode:" + location.getParentCode() + ",start:"
				+ startIndex + ",endIndex:" + endIndex + ",mergeFlag："
				+ mergeFlag + ",level:" + location.getLevel() + "]\n";
	}
}
