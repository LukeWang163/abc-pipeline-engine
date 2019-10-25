package base.operators.operator.nlp.segment.kshortsegment.predict.dictionary;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import base.operators.operator.nlp.segment.kshortsegment.predict.basetools.*;
import base.operators.operator.nlp.segment.kshortsegment.predict.segment.Define;
import base.operators.operator.nlp.segment.kshortsegment.predict.tagger.POS;
import base.operators.operator.nlp.segment.kshortsegment.predict.utils.BCConvert;
import idsw.nlp.read.ReadFileAsStream;

import java.io.*;
import java.security.Key;
import java.util.*;

public class DictManager {

	public static int MAX_FRQUENCY = 0;
	final TreeMap<String, TreeMap<POS, Double>> dictMap = new TreeMap<String, TreeMap<POS, Double>>();
	final TreeMap<String, Double> wordFrequencyMap = new TreeMap<String, Double>();
	static String TOTAL_FREQUENCY = "TOTAL_FREQUENCY";
	protected DoubleArrayTrie trie = new DoubleArrayTrie();
	protected List<String> words = new ArrayList<String>();

	/**
	 * Dict Name
	 */
	protected String name;
    //键为“tf”的值为用户模型中的词典，键为“custom”的值为用户额外的自定义词典
	Map<String, List<String>> dataList;

	private DictManager(Map<String, List<String>> dataList){
		this.dataList = dataList;
		loadDict();
	}



	// 定义一个静态私有变量(不初始化，不使用final关键字，使用volatile保证了多线程访问时instance变量的可见性，避免了instance初始化时其他变量属性还没赋值完时，被另外线程调用)

	private static volatile DictManager customInstance;

	// 定义一个共有的静态方法，返回该类型实例
	public static DictManager getInstance(Map<String, List<String>> dataList) {
		// 对象实例化时与否判断（不使用同步代码块，instance不等于null时，直接返回对象，提高运行效率）
		if (customInstance == null) {
			// 同步代码块（对象未初始化时，使用同步代码块，保证多线程访问时对象在第一次创建后，不再重复被创建）
			synchronized (DictManager.class) {
				// 未初始化，则初始instance变量
				if (customInstance == null) {
					customInstance = new DictManager(dataList);
				}
			}
		}
		return customInstance;
	}

	//+++++++++++++++实例集合(根据文件的HashCode创建的)++++++++++++++++++++++++++++++++
	private static volatile Map<String,DictManager> dictMangerInstanceMap = new HashMap<String,DictManager>();
	public static DictManager getInstance(String hashDict, Map<String, List<String>> dictDataMap){
		if(dictMangerInstanceMap.get(hashDict)==null){
			dictMangerInstanceMap.put(hashDict,new DictManager(dictDataMap));
		}
		return dictMangerInstanceMap.get(hashDict);
	}
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 *
	 * @return dict name
	 */
	public String getDictName() {
		return name;
	}

	/**
	 * get Dict Name
	 *
	 * @return
	 */
	public String getName() {
		return name;
	}

	public static int getMAX_FRQUENCY() {
		return MAX_FRQUENCY;
	}

	public DoubleArrayTrie getTrie() {
		return trie;
	}

	public List<String> getWords() {
		return words;
	}

	public TreeMap<String, Double> getWordFrequencyMap() {
		return wordFrequencyMap;
	}



	public TreeMap<String, TreeMap<POS, Double>> getDictMap() {
		return dictMap;
	}

	/**
	 * build dictionary words into an Double-Array-Trie
	 */

	private void loadDict(){
		trie.clear();
		dictMap.clear();
		loadSysDict();
		loadExtraDict();

		words = new ArrayList<String>(dictMap.keySet());
		// 必须先排序
		Collections.sort(words);
		System.out.println("构建trie树...");
		int load_error = trie.build(words);
		Define.MAX_FREQUENCY = MAX_FRQUENCY;
		System.out.println("词典加载是否错误: " + load_error);
		System.out.println("词总数: " + words.size());
	}

	private void loadSysDict() {
		// Begin Load Dicts
		System.out.println("系统词典名称:" + ReadFileAsStream.SegmentDict);
		boolean isEncrypt = true;
		// 读取加密文件
		if (true==isEncrypt) {
			Key desKey = getDesKey();
			DESCoder dcoder = new DESCoder();
			byte[] decryptDictBytes = dcoder.decryptSystemToByte(ReadFileAsStream.readSegmentDict(), desKey);

			if (decryptDictBytes == null) {
				System.out.println("加密词典文件解析失败");
			}
			try {
				String decryptDictString = new String(decryptDictBytes, "UTF-8");
				String[] decryptDictTerms = decryptDictString.split("\r\n");
				int termsSize = decryptDictTerms.length;
				System.out.println("系统词典中的词条数：" + termsSize);
				int index = 0;
				for (String term : decryptDictTerms) {
					if (index == 0) {
						if (term.length() > 0) {
							if (((int) term.charAt(0)) == 65279) {// 处理有bom格式的utf-8编码文档
								term = term.substring(1);
							}
						}
					}
					loadWord(term, 0);
					index++;
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		} else {
			InputStream inStream = null;
			try {
				//inStream = this.getClass().getResourceAsStream(fileName);
				inStream = ReadFileAsStream.readSegmentDict();
				BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
				String row;
				int rowNumber = 0;
				while ((row = reader.readLine()) != null) {
					if (rowNumber == 0) {
						if (row.length() > 0) {
							if (((int) row.charAt(0)) == 65279) {// 处理有bom格式的utf-8编码文档
								row = row.substring(1);
							}
						}
					}
					loadWord(row, 0);
					rowNumber++;
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();

			} finally {
				if (inStream != null) {
					try {
						inStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void loadExtraDict(){
		if(dataList == null){
			return;
		}
	    for(String key : dataList.keySet()){
			List<String> data  =  dataList.get(key);
            for (String row : data) {
                if (row.length() > 0) {
                    if (((int) row.charAt(0)) == 65279) {// 处理有bom格式的utf-8编码文档
                        row = row.substring(1);
                    }
                }
                try {
                	if("custom".equals(key)){
						loadWord(row, 2000);
					}else{
						loadWord(row, 0);
					}
				}catch (Exception e){
                	//跳过当前行
					System.err.println(row + "加载失败");
				}

            }
        }

	}

	/**
	 * 读取一行文本中的单词、词频及词性
	 * 
	 * @param row
	 */
	private void loadWord(String row, int userFrequency) {
		String[] params = row.split("[ \\t]");

		// ++++++++++++++正常词典格式：word nature1 freq1 nature2
		// freq2++++++++++++++++++++++++++
		 if ((params.length%2) != 1){
		 	return;
		 }
		int natureCount = (params.length - 1) / 2;
		double totalFrequency = 0;
		TreeMap<POS, Double> posMap = new TreeMap();
		try {
			for (int i = 0; i < natureCount; ++i) {
				String pos = params[1 + 2 * i];

				Double posFrq = Double.parseDouble(params[2 + 2 * i]) + userFrequency;

				totalFrequency += posFrq;
				if (pos != null && (!"".equals(pos))) {
					POS posTag = POS.fromString(pos);
					if (posTag != null) {
						Double frequency = posMap.get(posTag);
						if (frequency == null)
							frequency = 5d;
						frequency = frequency + posFrq;
						posMap.put(posTag, frequency);
					}

				}

				// posMap.put(TOTAL_FREQUENCY, totalFrequency);
			}
		}catch (NumberFormatException e){
			System.err.println("词典错误:" + row);
			return;
		}


		String word = params[0].trim();// 去首尾空格
		// ================转半角、转大写==========================
		word = BCConvert.qj2bj(word).toUpperCase();
		// if (word != null && (!"".equals(word)) && posMap.size()
		// != 0) {
		if (word != null && (!"".equals(word))) {
			// 惰性词及其词频比较添加
			TreeMap<POS, Double> oldPosMap = dictMap.get(word);
			if (oldPosMap == null) {
				dictMap.put(word, posMap);
			} else {
				Iterator it = posMap.keySet().iterator();
				while (it.hasNext()) {
					POS pos = (POS) it.next();
					Double oldFrq = oldPosMap.get(pos);
					if (oldFrq == null) {
						oldPosMap.put(pos, posMap.get(pos));
					} else {
						Double newFrq = posMap.get(pos);
						if (newFrq > oldFrq)
							oldPosMap.put(pos, newFrq);
					}
				}
				dictMap.put(word, oldPosMap);
			}
			MAX_FRQUENCY += totalFrequency;
			wordFrequencyMap.put(word, totalFrequency);
		}
	}

	private List<Map<String, String>> getDictFileNames(JSONObject dictObject) {
		List<Map<String, String>> fileNames = new ArrayList<>();

		String type = (String) dictObject.get("type");
		// System.out.println("type:" + type);
		if (type.equals("txt")) {
			Map<String, String> obj = new HashMap<String, String>();
			String isEncrypt = (String) dictObject.get("is_encrypt");
			obj.put("isEncrypt", isEncrypt);
			obj.put("name", (String) dictObject.get("file"));
			fileNames.add(obj);
			// System.out.println((String) dictObject.get("file"));
		} else if (type.equals("group")) {
			JSONArray dictGroup = (JSONArray) dictObject.get("dicts");
			for (Object obj : dictGroup) {
				fileNames.addAll(getDictFileNames((JSONObject) obj));
			}
		}

		return fileNames;
	}

	/**
	 * 验证license签名并获取解密后的密钥（zhangxian）
	 * 
	 * licenseTXTPath:license.txt文件路径(用以存放用户信息（明文）以及对license.key签名)
	 * licenseKeyPath:license.key文件路径(存放DES密钥信息、RSA公钥信息、用户签名信息)
	 * @return 解密后的词典文件内容，按行逐条存入List中
	 */
	private Key getDesKey() {
		String desKey = "";
		Key key_decrypt = null;
		try {
			List<String> list = readLicenseKeyFileToBuffer();
			desKey = list.get(0);// DESkey
			String rsaPublicKey = list.get(1);// RSA公钥
			String sign = list.get(2);// 数字签名
			byte[] licensebyte = readLicenseFileToByte();

			// 校验数字签名:即校验license.txt中的用户信息是否和license.key中的相关信息一致
			boolean sucesses = SignCoder.verify(licensebyte, rsaPublicKey, sign);// 校验数字签名:即校验license.txt中的用户信息是否和license.key中的相关信息一致
			System.out.println(sucesses);

			// 如果用户信息与license.key中的一致，则用从license.key中解析出的des对文件进行解密
			if (sucesses) {
				// 从license.key中将deskeyRSA读出
				String deskeyRSA_l = desKey;
				byte[] deskeyRSAbyte = null;
				try {
					deskeyRSAbyte = Coder.decryptBASE64(deskeyRSA_l);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// 用RSA公钥对deskey进行解密
				byte[] deskeyRSA_decrypt_byte = RSACoder.decryptByPublicKey(deskeyRSAbyte, rsaPublicKey);
				key_decrypt = DESCoder.toKey(deskeyRSA_decrypt_byte);
			} else {
				System.out.println("注册文件被破坏");
			}
			System.out.println("end");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return key_decrypt;
	}

//	// 解析加密词典文件
//	public static void readEncryptDictFromSystem(String encryptDictPathSystem, Key key) {
//		DESCoder dcoder = new DESCoder();
//		byte[] decryptDictBytes = dcoder.decryptSystemToByte(encryptDictPathSystem, key);
//
//		if (decryptDictBytes == null) {
//			System.out.println("加密词典文件解析失败");
//		}
//		try {
//			String decryptDictString = new String(decryptDictBytes, "UTF-8");
//			String[] decryptDictTerms = decryptDictString.split("\r\n");
//			int termsSize = decryptDictTerms.length;
//			System.out.println("词典中的词条数：" + termsSize);
//			int index = 0;
//			for (String term : decryptDictTerms) {
//				if (index > 100) {
//					break;
//				}
//				System.out.println(term);
//				index++;
//			}
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	public List<String> readLicenseKeyFileToBuffer() throws IOException {
		InputStream inStream =  ReadFileAsStream.readSegmentDictLicenseKey();
		//InputStream in = new FileInputStream(filePath);
		String line; // 用来保存每行读取的内容
		List<String> list=new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
		line = reader.readLine(); // 读取第一行
		while (line != null) {
			list.add(line);
			line = reader.readLine(); // 读取下一行
		}
		reader.close();
		inStream.close();
		return list;
	}

	public byte[] readLicenseFileToByte() {
		InputStream inStream = ReadFileAsStream.readSegmentDictLicense();

		long len = 0;
		byte data[] = null;

		try {
			len = inStream.available();//文件长度，返回为int型，最大能支持1.99G大小的文件，超过1.99G不准,目前所有词典文件总共(3000W个词)大小不超过600M
			data = new byte[(int) len];
			int r = inStream.read(data);
			inStream.close();
			if (r != len) {
				throw new IOException("Only read " + r + " of " + len + " for " + ReadFileAsStream.SegmentDictLicense);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

}
