package base.operators.operator.nlp.segment.kshortsegment.predict.basetools;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * 基于RSA算法的加密解密
 * 
 * @author limm
 * 
 */
public class RSACoder {
	
	/**加密算法*/
	public static final String KEY_ALGORTHM="RSA";
	
	/** RSA最大加密明文大小*/
    private static final int MAX_ENCRYPT_BLOCK = 117;
    
    /**RSA最大解密密文大小*/
    private static final int MAX_DECRYPT_BLOCK = 128;
	
	
	/**
	 * 用私钥加密
	 * 
	 * @param data
	 *            加密数据
	 * @param key
	 *            密钥(BASE64编码)
	 * @return
	 * @throws Exception
	 */
	public static byte[] encryptByPrivateKey(byte[] data, String key) {
		byte[] result = null;
		try {
			// 加密密钥：私钥
			byte[] keyBytes = Coder.decryptBASE64(key); 
			
			
			// 取私钥
			PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
			Key privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);

			// 对数据加密
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, privateKey);
			
			int inputLen = data.length;
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        int offSet = 0;
	        byte[] cache;
	        int i = 0;
	        // 对数据分段加密
	        while (inputLen - offSet > 0) {
	            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
	                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
	            } else {
	                cache = cipher.doFinal(data, offSet, inputLen - offSet);
	            }
	            out.write(cache, 0, cache.length);
	            i++;
	            offSet = i * MAX_ENCRYPT_BLOCK;
	        }
	        result = out.toByteArray();
	        out.close();
	        return result;
//			result = cipher.doFinal(data);
//			return result;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 用公钥解密
	 * 
	 * @param data
	 *            加密之后的数据
	 * @param key
	 *            密钥(BASE64编码)
	 * @return
	 * @throws Exception
	 */
	public static byte[] decryptByPublicKey(byte[] data, String key) {
		byte[] result = null;
		try {
			// 对私钥解密
			byte[] keyBytes = Coder.decryptBASE64(key);
			X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
			Key publicKey = keyFactory.generatePublic(x509EncodedKeySpec);

			// 对数据解密
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, publicKey);
			int inputLen = data.length;
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        int offSet = 0;
	        byte[] cache;
	        int i = 0;
	        // 对数据分段解密
	        while (inputLen - offSet > 0) {
	            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
	                cache = cipher.doFinal(data, offSet, MAX_DECRYPT_BLOCK);
	            } else {
	                cache = cipher.doFinal(data, offSet, inputLen - offSet);
	            }
	            out.write(cache, 0, cache.length);
	            i++;
	            offSet = i * MAX_DECRYPT_BLOCK;
	        }
	        result = out.toByteArray();
	        out.close();
	        return result;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 用公钥加密
	 * 
	 * @param data
	 *            待加密数据
	 * @param key
	 *            密钥(BASE64编码)
	 * @return
	 * @throws Exception
	 */
	public static byte[] encryptByPublicKey(byte[] data, String key) {
		byte[] result = null;
		try {
			// 对公钥解密
			byte[] keyBytes = Coder.decryptBASE64(key);
			// 取公钥
			X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
			Key publicKey = keyFactory.generatePublic(x509EncodedKeySpec);

			// 对数据解密
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			int inputLen = data.length;
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        int offSet = 0;
	        byte[] cache;
	        int i = 0;
	        // 对数据分段加密
	        while (inputLen - offSet > 0) {
	            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
	                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
	            } else {
	                cache = cipher.doFinal(data, offSet, inputLen - offSet);
	            }
	            out.write(cache, 0, cache.length);
	            i++;
	            offSet = i * MAX_ENCRYPT_BLOCK;
	        }
	        result = out.toByteArray();
	        out.close();
	        return result;
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 用私钥解密 * @param data 加密数据
	 * 
	 * @param key
	 *            密钥(BASE64编码)
	 * @return
	 * @throws Exception
	 */
	public static byte[] decryptByPrivateKey(byte[] data, String key) {
		byte[] result = null;
		try {
			// 对私钥解密
			byte[] keyBytes = Coder.decryptBASE64(key);

			PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
			Key privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
			// 对数据解密
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			int inputLen = data.length;
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	        int offSet = 0;
	        byte[] cache;
	        int i = 0;
	        // 对数据分段解密
	        while (inputLen - offSet > 0) {
	            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
	                cache = cipher.doFinal(data, offSet, MAX_DECRYPT_BLOCK);
	            } else {
	                cache = cipher.doFinal(data, offSet, inputLen - offSet);
	            }
	            out.write(cache, 0, cache.length);
	            i++;
	            offSet = i * MAX_DECRYPT_BLOCK;
	        }
	        result = out.toByteArray();
	        out.close();
	        return result;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
}
