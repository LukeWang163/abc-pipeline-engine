package base.operators.operator.nlp.segment.kshortsegment.predict.basetools;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * 签名验证器
 * 
 * @author limm
 * 
 */
public class SignCoder {
	
	/**加密算法*/
	public static final String KEY_ALGORTHM="RSA";
	
	/**签名算法*/
	public static final String SIGNATURE_ALGORITHM="MD5withRSA";
	
	/**
	 * 用私钥对信息生成数字签名
	 * @param data
	 *            //加密数据
	 * @param privateKey
	 *            //私钥
	 * @return
	 */
	public static String sign(byte[] data, String privateKey) {
		Signature signature = null;
		try {
			// 解密私钥
			byte[] keyBytes = Coder.decryptBASE64(privateKey);
			// 构造PKCS8EncodedKeySpec对象
			PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
			// 指定加密算法
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
			// 取私钥匙对象
			PrivateKey privateKey2 = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
			// 用私钥对信息生成数字签名
			signature = Signature.getInstance(SIGNATURE_ALGORITHM);
			signature.initSign(privateKey2);
			signature.update(data);
			return Coder.encryptBASE64(signature.sign());
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * 校验数字签名
	 * @param data
	 *            加密数据
	 * @param publicKey
	 *            公钥
	 * @param sign
	 *            数字签名
	 * @return
	 * @throws Exception
	 */
	public static boolean verify(byte[] data, String publicKey, String sign){
		
		try {
			// 解密公钥
			byte[] keyBytes = Coder.decryptBASE64(publicKey);
			// 构造X509EncodedKeySpec对象
			X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
			// 指定加密算法
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
			// 取公钥匙对象
			PublicKey publicKey2 = keyFactory.generatePublic(x509EncodedKeySpec);

			Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
			signature.initVerify(publicKey2);
			signature.update(data);
			// 验证签名是否正常
			return signature.verify(Coder.decryptBASE64(sign));
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
}
