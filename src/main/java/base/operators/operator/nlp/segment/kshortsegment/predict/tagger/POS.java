package base.operators.operator.nlp.segment.kshortsegment.predict.tagger;

/**
 * 词性
 *
 */
public enum POS {
	/** 名词-名词 */
	n,
	/** 名词-普通名词 */
	ng,
	/** 名词-方位名词 */
	nd,
	/** 名词-处所名词 */
	nl,
	/** 名词-人名 */
	nh,
	/** 名词-人名-汉族人名 */
	nhh,
	/** 名词-人名-汉族人名姓 */
	nhf,
	/** 名词-人名-汉族人名名 */
	nhg,
	/** 名词-人名-音译名或类音译名 */
	nhy,
	/** 名词-人名-日本人名 */
	nhr,
	/** 名词-人名-其他 */
	/**
	 * 职务职称
	 */
	np, nhw,
	/** 名词-地名 */
	ns,
	/** 名词-族名 */
	nn,
	/** 名词-机构名 */
	ni,
	/** 名词-时间名词 */
	nt,
	/** 名词-专有名词 */
	nz,
	/** 名词-专有名词-邮箱 */
	nze,
	/** 名词-专有名词-电话 */
	nzt,
	/** 名词-专有名词-传真 */
	nzf,
	/** 名词-专有名词-邮政编码 */
	nzp,
	/** 名词-专有名词-网址 */
	nzw,
	/** 名词-专有名词-QQ */
	nzq,
	/** 名词-专有名词-微信 */
	nzx,
	/** 名词-专有名词-网络ip地址 */
	nzn,
	/** 名词-专有名词-车牌号 */
	nzc,
	/** 名词-专有名词-身份证号 */
	nzi,
	/** 名词-专有名词-银行卡号 */
	nzb,
	/** 名词-专有名词-微博 */
	nzv,
	/** 动词 */
	v,
	/** 动词-及物动词 */
	vt,
	/** 动词-不及物动词 */
	vi,
	/** 动词-联系动词 */
	vl,
	/** 动词-能愿动词 */
	vu,
	/** 动词-趋向动词 */
	vd,
	/** 形容词 */
	a,
	/** 形容词-性质形容词 */
	aq,
	/** 形容词-状态形容词 */
	as,
	/** 区别词 */
	f,
	/** 代词 */
	r,
	/** 副词 */
	d,
	/** 介词 */
	p,
	/** 连词 */
	c,
	/** 助词 */
	u,
	/** 叹词 */
	e,
	/** 拟声词 */
	o,
	/** 习用语 */
	i,
	/** 习用语-名词性习用语 */
	in,
	/** 习用语-动词性习用语 */
	iv,
	/** 习用语-形容词性习用语 */
	ia,
	/** 习用语-连词性习用语 */
	ic,
	/** 缩略词 */
	j,
	/** 缩略词-名词性缩略词 */
	jn,
	/** 缩略词-动词性缩略词 */
	jv,
	/** 缩略词-形容词性缩略词 */
	ja,
	/** 前接成分 */
	h,
	/** 后接成分 */
	k,
	/** 语素字- */
	g,
	/** 语素字-名词性语素字 */
	gn,
	/** 语素字-动词性语素字 */
	gv,
	/** 语素字-形容词性语素字 */
	ga,
	/** 非语素字-非语素字 */
	x,
	/** 其他- */
	w,
	/** 其他-标点符号 */
	wp,
	/** 其他-非汉字字符串-合并成字符串 */
	ws,
	/** 其他-其他未知的符号 */
	wu,
	/** 数词- */
	m,
	/** 数词-百分数 */
	mp,
	/** 数词-整数 */
	mi,
	/** 数词-分数 */
	mf,
	/** 数词-小数 */
	md,
	/** 数词-序数 */
	mo,
	/** 数词-比率 */
	mr,
	/** 量词- */
	q,
	/** 数量词-数量词 */
	mq,
	/** 数量词-钱款 */
	mqm,
	/** 数量词-年龄 */
	mqa,
	/** 数量词-温度 */
	mqt,
	/** 数量词-角度 */
	mqn,
	/** 数量词-长度 */
	mql,
	/** 数量词-面积 */
	mqr,
	/** 数量词-容积 */
	mqc,
	/** 数量词-重量 */
	mqw,
	/** 数量词-速度 */
	mqs,
	/** 数量词-加速度 */
	mqv,

	/**
	 * 仅用于终##终，不会出现在分词结果中
	 */
	end,

	/**
	 * 仅用于始##始，不会出现在分词结果中
	 */
	begin,

	;

	/**
	 * 词性是否以该前缀开头<br>
	 * 词性根据开头的几个字母可以判断大的类别
	 * 
	 * @param prefix
	 *            前缀
	 * @return 是否以该前缀开头
	 */
	public boolean startsWith(String prefix) {
		return toString().startsWith(prefix);
	}

	/**
	 * 词性是否以该前缀开头<br>
	 * 词性根据开头的几个字母可以判断大的类别
	 * 
	 * @param prefix
	 *            前缀
	 * @return 是否以该前缀开头
	 */
	public boolean startsWith(char prefix) {
		return toString().charAt(0) == prefix;
	}

	/**
	 * 词性的首字母<br>
	 * 词性根据开头的几个字母可以判断大的类别
	 * 
	 * @return
	 */
	public char firstChar() {
		return toString().charAt(0);
	}

	/**
	 * 安全地将字符串类型的词性转为Enum类型，如果未定义该词性，则返回null
	 * 
	 * @param name
	 *            字符串词性
	 * @return Enum词性
	 */
	public static POS fromString(String name) {
		try {
			return POS.valueOf(name);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 人名判断
	 * 
	 * @param pos
	 * @return
	 */
	public static boolean isPerson(String pos) {
		if (pos == null || "".equals(pos))
			return false;
		if (pos.startsWith(nh.toString()))
			return true;
		return false;
	}

	/**
	 * 地名判断
	 * 
	 * @param pos
	 * @return
	 */
	public static boolean isPlace(String pos) {
		if (pos == null || "".equals(pos))
			return false;
		if (pos.startsWith(ns.toString()))
			return true;
		return false;
	}

	/**
	 * 机构名判断
	 */
	public static boolean isOrg(String pos) {
		if (pos == null || "".equals(pos))
			return false;
		if (pos.startsWith(ni.toString()))
			return true;
		return false;
	}

	/**
	 * 时间判断
	 * 
	 * @param pos
	 * @return
	 */
	public static boolean isTime(String pos) {
		if (pos == null || "".equals(pos))
			return false;
		if (pos.equals(nt.toString()))
			return true;
		return false;
	}
	/**
	 * 数量词判断
	 * @param pos
	 * @return
	 */
	public static boolean isNumber(String pos) {
		if (pos == null || "".equals(pos))
			return false;
		if (pos.startsWith(m.toString()))
			return true;
		return false;
	}
	
	/**
	 * 字符串判断
	 * @param pos
	 * @return
	 */
	public static boolean isLetter(String pos) {
		if (pos == null || "".equals(pos))
			return false;
		if (pos.equals(ws.toString()))
			return true;
		return false;
	}
	
	/**
	 * 专有名词判断
	 * @param pos
	 * @return
	 */
	public static boolean isSpecial(String pos) {
		if (pos == null || "".equals(pos))
			return false;
		if (pos.startsWith("nz"))
			return true;
		return false;
	}
	
	/**
	 * 其他标志判断
	 * @param pos
	 * @return
	 */
	public static boolean isOther(String pos) {
		if (pos == null || "".equals(pos))
			return false;
		if (pos.startsWith(w.toString()))
			return true;
		return false;
	}
	
	/**
	 * 开始标志判断
	 * @param pos
	 * @return
	 */
	public static boolean isBegin(String pos) {
		if (pos == null || "".equals(pos))
			return false;
		if (pos.equals(begin.toString()))
			return true;
		return false;
	}
	/**
	 * 结束标志判断
	 * @param pos
	 * @return
	 */
	public static boolean isEnd(String pos) {
		if (pos == null || "".equals(pos))
			return false;
		if (pos.equals(end.toString()))
			return true;
		return false;
	}
	
	

}