package base.operators.operator.nlp.ner.location;

import java.util.HashSet;

public class Location {
	/**
	 * 地址编码
	 */
	private String code;
	/**
	 * 上级地址编码
	 */
	private String parentCode;
	/**
	 * 名称
	 */
	private String name;
	/**
	 * 地址简称
	 */
	private String shortName = "";
	/**
	 * 地址别名
	 */
	private String alias = "";
	/**
	 * 地址级别(从小到大地址级别从高到低)
	 */
	private int level = -1;
	/**
	 * 地址类别
	 */
	private String type = "";

	/**
	 * GCJ坐标系经度
	 */
	private double longtitudeGCJ = 0d;
	/**
	 * GCJ坐标系纬度
	 */
	private double latitudeGCJ = 0d;
	/**
	 * WGS坐标系经度
	 */
	private double longtitudeWGS = 0d;
	/**
	 * WGS坐标系纬度
	 */
	private double latitudeWGS = 0d;

	private HashSet<String> nameSet;

	public Location(String code, String parentCode, String name,
			String shortName, String alias, String level, String type) {
		if (code != null && !code.equals(""))
			this.code = code.trim();
		if (parentCode != null && !parentCode.equals(""))
			this.parentCode = parentCode.trim();
		if (name != null && !name.equals(""))
			this.name = name.trim();
		if (shortName != null && !shortName.equals(""))
			this.shortName = shortName.trim();
		if (alias != null && !alias.equals(""))
			this.alias = alias.trim();
		if (level != null && !level.equals(""))
			this.level = Integer.parseInt(level);
		if (type != null && !type.equals(""))
			this.type = type.trim();
		nameSet = new HashSet();
		nameSet.add(name);
		if (shortName != null && !shortName.equals("")) {
			String shortNames[] = shortName.split(",");
			for (int i = 0; i < shortNames.length; i++) {
				nameSet.add(shortNames[i]);
			}
		}
		if (alias != null && !alias.equals("")) {
			String aliasNames[] = alias.split(",");
			for (int i = 0; i < aliasNames.length; i++) {
				nameSet.add(aliasNames[i]);
			}
		}
	}

	public Location(String code, String parentCode, String name,
			String shortName, String alias, String level, String type,
			String longtitudeGCJ, String latitudeGCJ, String longtitudeWGS,
			String latitudeWGS) {
		this(code, parentCode, name, shortName, alias, level, type);
		if (longtitudeGCJ != null && !longtitudeGCJ.equals(""))
			this.longtitudeGCJ = Double.parseDouble(longtitudeGCJ.trim());
		if (latitudeGCJ != null && !latitudeGCJ.equals(""))
			this.latitudeGCJ = Double.parseDouble(latitudeGCJ.trim());
		if (longtitudeWGS != null && !longtitudeWGS.equals(""))
			this.longtitudeWGS = Double.parseDouble(longtitudeWGS.trim());
		if (latitudeWGS != null && !latitudeWGS.equals(""))
			this.latitudeWGS = Double.parseDouble(latitudeWGS.trim());

	}

	public String getCode() {
		return code;
	}

	public String getParentCode() {
		return parentCode;
	}

	public String getName() {
		return name;
	}

	public int getLevel() {
		return level;
	}

	public String getType() {
		return type;
	}

	public String getShortName() {
		return shortName;
	}

	public String getAlias() {
		return alias;
	}

	public double getLongtitudeGCJ() {
		return longtitudeGCJ;
	}

	public double getLatitudeGCJ() {
		return latitudeGCJ;
	}

	public double getLongtitudeWGS() {
		return longtitudeWGS;
	}

	public double getLatitudeWGS() {
		return latitudeWGS;
	}

	public HashSet<String> getNameSet() {
		return nameSet;
	}

	public int hashCode() {
		return code.hashCode();
	}

	public String toString() {
		return "[code:" + code + ",parentCode:" + parentCode + ",name:" + name
				+ ",shortName:" + shortName + ",alias:" + alias + ",level:"
				+ level + ",type:" + type +",longtitudeGCJ:"+longtitudeGCJ+",latitudeGCJ:"+latitudeGCJ+ "]";
		
	//	return "[code:" + code + ",name:" + name+ "]";
	}

}
