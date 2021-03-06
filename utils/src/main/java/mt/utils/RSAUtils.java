/*
 * Copyright 2005-2017 shopxx.net. All rights reserved.
 * Support: http://www.shopxx.net
 * License: http://www.shopxx.net/license
 */
package mt.utils;

import mt.utils.common.Assert;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Utils - RSA加密/解密
 *
 * @author OIG Team
 * @version 5.0
 */
public final class RSAUtils {
	
	/**
	 * 密钥算法
	 */
	private static final String KEY_ALGORITHM = "RSA";
	
	/**
	 * 加密/解密算法
	 */
	private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
	
	/**
	 * 安全服务提供者
	 */
	private static final Provider PROVIDER = new BouncyCastleProvider();
	
	/**
	 * 不可实例化
	 */
	private RSAUtils() {
	}
	
	/**
	 * 生成密钥对
	 *
	 * @param keySize 密钥大小
	 * @return 密钥对
	 */
	public static KeyPair generateKeyPair(int keySize) {
		Assert.state(keySize > 0, "keySize must > 0");
		
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, PROVIDER);
			keyPairGenerator.initialize(keySize);
			return keyPairGenerator.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 生成私钥
	 *
	 * @param encodedKey 密钥编码
	 * @return 私钥
	 */
	public static PrivateKey generatePrivateKey(byte[] encodedKey) {
		Assert.notNull(encodedKey, "encodedKey must not null");
		
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM, PROVIDER);
			return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 生成私钥
	 *
	 * @param keyString 密钥字符串(BASE64编码)
	 * @return 私钥
	 */
	public static PrivateKey generatePrivateKey(String keyString) {
		Assert.state(StringUtils.isNotBlank(keyString), "keyString must not blank");
		
		return generatePrivateKey(Base64.decodeBase64(keyString));
	}
	
	/**
	 * 生成公钥
	 *
	 * @param encodedKey 密钥编码
	 * @return 公钥
	 */
	public static PublicKey generatePublicKey(byte[] encodedKey) {
		Assert.notNull(encodedKey, "encodeKey must not null");
		
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM, PROVIDER);
			return keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 生成公钥
	 *
	 * @param keyString 密钥字符串(BASE64编码)
	 * @return 公钥
	 */
	public static PublicKey generatePublicKey(String keyString) {
		Assert.state(StringUtils.isNotBlank(keyString), "keyStream must not null");
		
		return generatePublicKey(Base64.decodeBase64(keyString));
	}
	
	/**
	 * 获取密钥字符串
	 *
	 * @param key 密钥
	 * @return 密钥字符串(BASE64编码)
	 */
	public static String getKeyString(Key key) {
		Assert.notNull(key, "key must not null");
		return Base64.encodeBase64String(key.getEncoded());
	}
	
	/**
	 * 获取密钥
	 *
	 * @param type        类型
	 * @param inputStream 输入流
	 * @param password    密码
	 * @return 密钥
	 */
	public static Key getKey(String type, InputStream inputStream, String password) {
		Assert.state(StringUtils.isNotBlank(type), "type must not blank");
		Assert.notNull(inputStream, "inputStream must not null");
		
		try {
			KeyStore keyStore = KeyStore.getInstance(type, PROVIDER);
			keyStore.load(inputStream, password != null ? password.toCharArray() : null);
			String alias = keyStore.aliases().hasMoreElements() ? keyStore.aliases().nextElement() : null;
			return keyStore.getKey(alias, password != null ? password.toCharArray() : null);
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 获取证书
	 *
	 * @param type        类型
	 * @param inputStream 输入流
	 * @return 证书
	 */
	public static Certificate getCertificate(String type, InputStream inputStream) {
		Assert.state(StringUtils.isNotBlank(type), "type must not blank");
		Assert.notNull(inputStream, "inputStream must not null");
		
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance(type, PROVIDER);
			return certificateFactory.generateCertificate(inputStream);
		} catch (CertificateException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 生成签名
	 *
	 * @param algorithm  签名算法
	 * @param privateKey 私钥
	 * @param data       数据
	 * @return 签名
	 */
	public static byte[] sign(String algorithm, PrivateKey privateKey, byte[] data) {
		Assert.state(StringUtils.isNotBlank(algorithm), "algorithm must not blank");
		Assert.notNull(privateKey, "privateKey must not null");
		Assert.notNull(data, "data must not null");
		
		try {
			Signature signature = Signature.getInstance(algorithm, PROVIDER);
			signature.initSign(privateKey);
			signature.update(data);
			return signature.sign();
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 验证签名
	 *
	 * @param algorithm 签名算法
	 * @param publicKey 公钥
	 * @param sign      签名
	 * @param data      数据
	 * @return 是否验证通过
	 */
	public static boolean verify(String algorithm, PublicKey publicKey, byte[] sign, byte[] data) {
		Assert.state(StringUtils.isNotBlank(algorithm), "algorithm must not blank");
		Assert.notNull(publicKey, "publicKey must not null");
		Assert.notNull(data, "data must not null");
		Assert.notNull(sign, "sign must not null");
		
		try {
			Signature signature = Signature.getInstance(algorithm, PROVIDER);
			signature.initVerify(publicKey);
			signature.update(data);
			return signature.verify(sign);
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 验证签名
	 *
	 * @param algorithm   签名算法
	 * @param certificate 证书
	 * @param sign        签名
	 * @param data        数据
	 * @return 是否验证通过
	 */
	public static boolean verify(String algorithm, Certificate certificate, byte[] sign, byte[] data) {
		Assert.state(StringUtils.isNotBlank(algorithm), "algorithm must not blank");
		Assert.notNull(certificate, "certificate must not null");
		Assert.notNull(data, "data must not null");
		Assert.notNull(sign, "sign must not null");
		
		try {
			Signature signature = Signature.getInstance(algorithm, PROVIDER);
			signature.initVerify(certificate);
			signature.update(data);
			return signature.verify(sign);
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 加密
	 *
	 * @param publicKey 公钥
	 * @param data      数据
	 * @return 密文
	 */
	public static byte[] encrypt(PublicKey publicKey, byte[] data) {
		Assert.notNull(publicKey, "publicKey must not null");
		Assert.notNull(data, "data must not null");
		
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION, PROVIDER);
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	/**
	 * 解密
	 *
	 * @param privateKey 私钥
	 * @param data       数据
	 * @return 明文
	 */
	public static byte[] decrypt(PrivateKey privateKey, byte[] data) {
		Assert.notNull(privateKey, "privateKey must not null");
		Assert.notNull(data, "data must not null");
		
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION, PROVIDER);
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
}