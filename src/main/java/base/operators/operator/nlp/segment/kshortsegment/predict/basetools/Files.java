package base.operators.operator.nlp.segment.kshortsegment.predict.basetools;

import java.io.*;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 针对于读写文件获取存储秘钥
 * 
 * @author limm,zhangxian
 * 
 */
public class Files {

	/**
	 * 把文件读入byte数组
	 * 
	 * @param filePath
	 *            文件完整存储路径
	 * @return byte数组
	 */
	public static byte[] readFileToByte(String filePath) {
		File file = new File(filePath);
		long len = file.length();
		byte data[] = new byte[(int) len];
		FileInputStream fin;
		try {
			fin = new FileInputStream(file);
			int r = fin.read(data);
			fin.close();
			if (r != len) {
				throw new IOException("Only read " + r + " of " + len + " for " + file);
			}
			return data;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return data;
	}
	
	/**
	 * 把工程中类路径下的文件读入byte数组
	 * 
	 * @param filePath
	 *            文件相对路径
	 * @return byte数组
	 * 
	 * @author zhangxian
	 */
	public byte[] readSystemFileToByte(String filePath) {
		InputStream inStream = this.getClass().getResourceAsStream(filePath);
		
		long len = 0;
		byte data[] = null;
		
		try {
			len = inStream.available();//文件长度，返回为int型，最大能支持1.99G大小的文件，超过1.99G不准,目前所有词典文件总共(3000W个词)大小不超过600M
			data = new byte[(int) len];
			int r = inStream.read(data);
			inStream.close();
			if (r != len) {
				throw new IOException("Only read " + r + " of " + len + " for " + filePath);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}
	
	
	

	/**
	 * 把byte数组写出到文件
	 * 
	 * @param filePath
	 *            文件完整存储路径
	 * @param data
	 *            数据数组
	 */
	public static void writeFile(String filePath, byte data[]) {
		FileOutputStream fout;
		try {
			fout = new FileOutputStream(filePath);
			fout.write(data);
			fout.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Properties getProperties(String filePath) {
		try {
			// InputStream in = Files.class.getClassLoader()
			// .getResourceAsStream(filePath);
			InputStream in = new FileInputStream(filePath);
			Properties p = new Properties();
			p.load(in);
			return p;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * 写内容到文件
	 * 
	 * @param
	 * 
	 */
	public static void writeContentToFile(String content, String filePath,boolean flag) {
		try {
			FileOutputStream out = new FileOutputStream(filePath,flag);//flag==true表示在文件的末尾续写；flag==false表示覆盖重写文件
			OutputStreamWriter logWriter = new OutputStreamWriter(out, "utf-8");
			BufferedWriter logBufferedWriter = new BufferedWriter(logWriter);
			logBufferedWriter.write(content);
			logBufferedWriter.write("\r\n");
//			logBufferedWriter.newLine();
			logBufferedWriter.flush();
			logBufferedWriter.close();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	
	
	
	/**
	 * 
	 * @param
	 * @param filePath
	 * @throws IOException
	 */
    public static List<String> readFileToBuffer(String filePath) throws IOException {
        InputStream in = new FileInputStream(filePath);
        String line; // 用来保存每行读取的内容
        List<String> list=new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        line = reader.readLine(); // 读取第一行
        while (line != null) {
        	list.add(line);
            line = reader.readLine(); // 读取下一行
        }
        reader.close();
        in.close();
        return list;
    }
    
    public List<String> readSystemFileToBuffer(String filePath) throws IOException {
    	InputStream inStream =  this.getClass().getResourceAsStream(filePath);
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



	/**
	 * 获取RSA公钥或私钥
	 * 
	 * @param keyFile
	 *            公钥或私钥路径＋文件名
	 * @return String (BASE64编码)
	 * @throws IOException
	 */
	public static String getRSAKeyFromFile(String keyFile) {
		ObjectInputStream inputStream = null;
		Key key = null;
		try {
			// // 从jar包中加载keyFile
			// InputStream file =
			// AsymPasswordEncoder.class.getClassLoader().getResourceAsStream(keyFile);

			InputStream file = new FileInputStream(keyFile);

			inputStream = new ObjectInputStream(file);
			key = (Key) inputStream.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return Coder.encryptBASE64(key.getEncoded());
	}

	/**
	 * 获取DES对称秘钥
	 * 
	 * @param keyFile
	 *            秘钥路径＋文件名
	 * @return Key 对象
	 * @throws IOException
	 */
	public static Key getDESKeyFromFile(String keyFile) {
		ObjectInputStream inputStream = null;
		Key key = null;
		try {
			// // 从jar包中加载keyFile
			// InputStream file =
			// AsymPasswordEncoder.class.getClassLoader().getResourceAsStream(keyFile);
			File newfile = new File(keyFile);
			InputStream file = new FileInputStream(newfile);

			inputStream = new ObjectInputStream(file);
			key = (Key) inputStream.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return key;
	}
	
	
	

	/**
	 * 获取DES对称秘钥
	 * 
	 * @param keyFile
	 *            秘钥路径＋文件名
	 * @return Key 字符串
	 * @throws IOException
	 */
	public static String getDESKeyStringFromFile(String keyFile) {
		ObjectInputStream inputStream = null;
		Key key = null;
		try {
			// // 从jar包中加载keyFile
			// InputStream file =
			// AsymPasswordEncoder.class.getClassLoader().getResourceAsStream(keyFile);
			File newfile = new File(keyFile);
			InputStream file = new FileInputStream(newfile);

			inputStream = new ObjectInputStream(file);
			key = (Key) inputStream.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return Coder.encryptBASE64(key.getEncoded());
	}

}
