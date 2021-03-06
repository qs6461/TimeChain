package com.dizsun.timechain.component;

import java.io.Serializable;

import com.alibaba.fastjson.JSON;

/**
 * ACK组件类,包括字段view number:VN,公钥:publicKey和签名sign
 */
public class ACK implements Serializable {
	public int VN;
	public String publicKey;

	public String sign;

	public ACK() {
	}

	public ACK(int VN, String sign) {
		this.VN = VN;
		this.sign = sign;
	}

	public ACK(String jsonStr) {
		ACK ack = JSON.parseObject(jsonStr, ACK.class);
		this.VN = ack.getVN();
		this.publicKey = ack.getPublicKey();
		this.sign = ack.getSign();
	}

	public int getVN() {
		return VN;
	}

	public void setVN(int VN) {
		this.VN = VN;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
}
