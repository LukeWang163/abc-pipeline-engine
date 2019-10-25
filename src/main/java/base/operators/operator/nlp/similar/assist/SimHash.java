package base.operators.operator.nlp.similar.assist;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class SimHash {

	public String tokens;//Hash内容
	
	public String separator = "";//Hash内容的分隔符，""代表以字符为单位。

	public BigInteger intSimHash;

	public String strSimHash;

	public int hashbits = 64;//Hash位数

	public SimHash(String tokens) {
		this.tokens = tokens;
		this.intSimHash = this.simHash();
	}

	public SimHash(String tokens, int hashbits) {
		this.tokens = tokens;
		this.hashbits = hashbits;
		this.intSimHash = this.simHash();
	}
	
	public SimHash(String tokens, int hashbits, String separator) {
		this.tokens = tokens;
		this.separator = separator;
		this.hashbits = hashbits;
		this.intSimHash = this.simHash();
	}

	public BigInteger simHash() {
		// 定义特征向量/数组
		int[] v = new int[this.hashbits];
		//1.根据对内容的分析模式不同，分析的单位分为字符、切割子串的方式。
		List<String> tokens_list = new ArrayList<String>();
		if ("".equals(separator)){
			for (int i = 0; i < this.tokens.length(); i++) {
				tokens_list.add(this.tokens.substring(i, i + 1));
			}
		}else{
			for (int j = 0; j < this.tokens.split(separator).length; j++) {
				tokens_list.add(this.tokens.split(separator)[j]);
			}
		}
		for (int u = 0; u < tokens_list.size(); u++){
			String temp = tokens_list.get(u);
			// 2.将每一个分词hash为一组固定长度的数列.比如 64bit 的一个整数.
			BigInteger t = this.hash(temp);
			for (int i = 0; i < this.hashbits; i++) {
				BigInteger bitmask = new BigInteger("1").shiftLeft(i);
				// 3.建立一个长度为64的整数数组(假设要生成64位的数字指纹,也可以是其它数字),
				// 对每一个分词hash后的数列进行判断,如果是1000...1,那么数组的第一位和末尾一位加1,
				// 中间的62位减一,也就是说,逢1加1,逢0减1.一直到把所有的分词hash数列全部判断完毕.
				if (t.and(bitmask).signum() != 0) {
					// 这里是计算整个文档的所有特征的向量和
					// 这里实际使用中需要 +- 权重，而不是简单的 +1/-1，
					v[i] += 1;
				} else {
					v[i] -= 1;
				}
			}
		}
		BigInteger fingerprint = new BigInteger("0");
		StringBuffer simHashBuffer = new StringBuffer();
		for (int i = 0; i < this.hashbits; i++) {
			// 4、最后对数组进行判断,大于0的记为1,小于等于0的记为0,得到一个 64bit 的数字指纹/签名.
			if (v[i] >= 0) {
				fingerprint = fingerprint.add(new BigInteger("1").shiftLeft(i));
				simHashBuffer.append("1");
			} else {
				simHashBuffer.append("0");
			}
		}
		this.strSimHash = simHashBuffer.toString();
//		System.out.println(this.strSimHash + " length " + this.strSimHash.length());
		return fingerprint;
	}

	public BigInteger hash(String source) {
		if (source == null || source.length() == 0) {
			return new BigInteger("0");
		} else {
			char[] sourceArray = source.toCharArray();
			BigInteger x = BigInteger.valueOf(((long) sourceArray[0]) << 7);
			BigInteger m = new BigInteger("1000003");
			BigInteger mask = new BigInteger("2").pow(this.hashbits).subtract(new BigInteger("1"));
			for (char item : sourceArray) {
				BigInteger temp = BigInteger.valueOf((long) item);
				x = x.multiply(m).xor(temp).and(mask);
			}
			x = x.xor(new BigInteger(String.valueOf(source.length())));
			if (x.equals(new BigInteger("-1"))) {
				x = new BigInteger("-2");
			}
			return x;
		}
	}

	public int hammingDistance(SimHash other) {

		BigInteger x = this.intSimHash.xor(other.intSimHash);
		int tot = 0;

		// 统计x中二进制位数为1的个数
		// 我们想想，一个二进制数减去1，那么，从最后那个1（包括那个1）后面的数字全都反了，对吧，然后，n&(n-1)就相当于把后面的数字清0，
		// 我们看n能做多少次这样的操作就行了。

		while (x.signum() != 0) {
			tot += 1;
			x = x.and(x.subtract(new BigInteger("1")));
		}
		return tot;
	}

	public int getDistance(String str1, String str2) {
		int distance;
		if (str1.length() != str2.length()) {
			distance = -1;
		} else {
			distance = 0;
			for (int i = 0; i < str1.length(); i++) {
				if (str1.charAt(i) != str2.charAt(i)) {
					distance++;
				}
			}
		}
		return distance;
	}

	public List<BigInteger> subByDistance(SimHash simHash, int distance) {
		// 分成几组来检查
		int numEach = this.hashbits / (distance + 1);
		List<BigInteger> characters = new ArrayList<BigInteger>();

		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < this.intSimHash.bitLength(); i++) {
			// 当且仅当设置了指定的位时，返回 true
			boolean sr = simHash.intSimHash.testBit(i);

			if (sr) {
				buffer.append("1");
			} else {
				buffer.append("0");
			}

			if ((i + 1) % numEach == 0) {
				// 将二进制转为BigInteger
				BigInteger eachValue = new BigInteger(buffer.toString(), 2);
				buffer.delete(0, buffer.length());
				characters.add(eachValue);
			}
		}

		return characters;
	}

//	public static void main(String[] args) {
//		String s = "我很开心";
//		SimHash hash1 = new SimHash(s, 64);
//		System.out.println(hash1.intSimHash + "  " + hash1.intSimHash.bitCount());
//		
//		s = "呀我开心";
//		SimHash hash2 = new SimHash(s, 64);
//		System.out.println(hash2.intSimHash + "  " + hash2.intSimHash.bitCount());
//
//		s = "我有点";
//		SimHash hash3 = new SimHash(s, 64);
//		System.out.println(hash3.intSimHash + "  " + hash3.intSimHash.bitCount());
//
//		s = "忧伤呀";
//		SimHash hash4 = new SimHash(s, 64);
//		System.out.println(hash4.intSimHash + "  " + hash4.intSimHash.bitCount());
//
//		System.out.println("============================");
//		
//		int dis = hash1.getDistance(hash1.strSimHash, hash2.strSimHash);
//		System.out.println(hash1.hammingDistance(hash2) + " " + dis);
//
//		int dis2 = hash1.getDistance(hash3.strSimHash, hash4.strSimHash);
//		System.out.println(hash3.hammingDistance(hash4) + " " + dis2);
//		
////		//通过Unicode编码来判断中文
////		String str = "中国chinese";
////		for (int i = 0; i < str.length(); i++) {
////			System.out.println(str.substring(i, i + 1).matches("[\\u4e00-\\u9fbb]+"));
////		}
//
//	}
}
