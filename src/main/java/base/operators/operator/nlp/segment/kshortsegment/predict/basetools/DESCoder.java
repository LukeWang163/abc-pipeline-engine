package base.operators.operator.nlp.segment.kshortsegment.predict.basetools;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;


/**
 * 基于DES加密解密
 * 
 * @author limm,zhangxian
 * 
 */
public class DESCoder {
	
	/**加密算法*/
	public static final String KEY_ALGORTHM="DES";
	
	/**
	 * 文件file进行加密并保存目标文件destFile中
	 * 
	 * @param file
	 *            要加密的文件 如c:/test/srcFile.txt
	 * @param destFile
	 *            加密后存放的文件名 如c:/加密后文件.txt
	 * @param key 加密生成key
	 */
	public void encrypt(String file, String destFile,Key key) {
		try {
			Cipher cipher = Cipher.getInstance(KEY_ALGORTHM);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			InputStream is = new FileInputStream(file);
			OutputStream out = new FileOutputStream(destFile);
			CipherInputStream cis = new CipherInputStream(is, cipher);
			byte[] buffer = new byte[1024];
			int r;
			while ((r = cis.read(buffer)) > 0) {
				out.write(buffer, 0, r);
			}
			cis.close();
			is.close();
			out.close();
		}catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}catch (InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 文件采用DES算法解密文件
	 * 
	 * @param file
	 *            已加密的文件 如c:/加密后文件.txt * 
	 * @param dest 解密后存放的文件名 如c:/
	 *            test/解密后文件.txt
	 * @param key 加密生成key
	 *            
	 */
	public void decrypt(String file, String dest,Key key){
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(KEY_ALGORTHM);
			cipher.init(Cipher.DECRYPT_MODE, key);
			InputStream is;
			is = new FileInputStream(file);
			OutputStream out = new FileOutputStream(dest);
			CipherOutputStream cos = new CipherOutputStream(out, cipher);
			byte[] buffer = new byte[1024];
			int r;
			while ((r = is.read(buffer)) >= 0) {
				//System.out.println();
				cos.write(buffer, 0, r);
			}
			cos.close();
			out.close();
			is.close();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}catch (InvalidKeyException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 文件采用DES算法解密文件
	 * 
	 * @param file
	 *            已加密的文件 如c:/加密后文件.txt * 

	 * @param key 加密生成key
	 * @return byte[]
	 *            
	 */
	public byte[] decryptToByte(String file,Key key){
		Cipher cipher;
		byte[] result=null;
		try {
			cipher = Cipher.getInstance(KEY_ALGORTHM);
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] fileByte=Files.readFileToByte(file);
			result = cipher.doFinal(fileByte);
			return result;
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * 解密工程内部类根路径下的文件（zhangxian）
	 * 
	 * @param file
	 *            工程类根路径下已加密的文件路径
	 * @param key 加密生成key
	 * @return byte[]
	 *            
	 */
	public byte[] decryptSystemToByte( InputStream inputStream, Key key){
		Cipher cipher;
		byte[] result=null;
		try {
			cipher = Cipher.getInstance(KEY_ALGORTHM);
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] fileByte=readSystemFileToByte(inputStream);
			if(fileByte==null){
				return null;
			}
			result = cipher.doFinal(fileByte);			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * 读取工程内部类根路径下的文件转换为byte[],便于解密（zhangxian）
	 * 
	 * @param filePath
	 *            工程类根路径下已加密的文件路径
	 * @return byte[]
	 *            
	 */
	public byte[] readSystemFileToByte(InputStream inputStream) {
		//long len = 0;
		byte data[] = null;
		
		try {
			/*
			//这种写法，在生成jar包后放入工程中，工程中再使用时会报错
			len = inStream.available();//文件长度，返回为int型，最大能支持1.99G大小的文件，超过1.99G不准,目前所有词典文件总共(3000W个词)大小不超过600M
			data = new byte[(int) len];
			int r = inStream.read(data);
			inStream.close();
			if (r != len) {
				throw new IOException("Only read " + r + " of " + len + " for " + filePath);
			}*/
			
			ByteArrayOutputStream swapStream = new ByteArrayOutputStream();  
			byte[] buff = new byte[100];  
			int rc = 0;  
			while ((rc = inputStream.read(buff, 0, 100)) > 0) {
			  swapStream.write(buff, 0, rc);  
			}  
			data = swapStream.toByteArray();			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}
	
	
	/**
	 * 文件file进行加密并保存目标文件destFile中
	 * 
	 * @param file
	 *            要加密的文件 如c:/test/srcFile.txt
	 * @param destFile
	 *            加密后存放的文件名 如c:/加密后文件.txt
	 * @param key key(Base64编码）
	 */
	public void encryptByString(String file, String destFile,String key) {
		try {
			byte[] keyBytes = Coder.decryptBASE64(key);
			Key keyObj=toKey(keyBytes);
			encrypt(file, destFile, keyObj);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 文件采用DES算法解密文件
	 * 
	 * @param file
	 *            已加密的文件 如c:/加密后文件.txt * 
	 * @param dest 解密后存放的文件名 如c:/
	 *            test/解密后文件.txt
	 * @param key key(Base64编码）
	 *            
	 */
	public void decryptByString(String file, String dest,String key){
		try {
			byte[] keyBytes = Coder.decryptBASE64(key);
			Key keyObj=toKey(keyBytes);
			decrypt(file, dest, keyObj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	 public static Key toKey(byte[] key) throws Exception {
	        DESKeySpec dks = new DESKeySpec(key);
	        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KEY_ALGORTHM);
	        SecretKey secretKey = keyFactory.generateSecret(dks);
	        return secretKey;
	    }
	    
}