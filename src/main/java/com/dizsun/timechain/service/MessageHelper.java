
package com.dizsun.timechain.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

import com.alibaba.fastjson.JSON;
import com.dizsun.timechain.component.ACK;
import com.dizsun.timechain.component.Block;
import com.dizsun.timechain.component.Message;
import com.dizsun.timechain.constant.Config;
import com.dizsun.timechain.constant.R;
import com.dizsun.timechain.util.RSAUtil;

/**
 * 帮助处理节点间传输的消息的辅助类 query,request和response开头的方法是将需要传输的数据打包成json字符串格式
 */
public class MessageHelper {
	private BlockService blockService;
	private PeerService peerService;
	private RSAUtil rsaUtil;
	private Config config;
	private ConcurrentHashMap<String, Long> filterMap;
	private Logger logger = Logger.getLogger(MessageHelper.class);
	/**
	 * 处理接收到的节点
	 *
	 * @param message
	 */
	public void handlePeersResponse(String message) {
		List<String> peers = JSON.parseArray(message, String.class);
		for (String peer : peers) {
			peerService.connectToPeer(peer);
		}
	}

	/**
	 * 过滤器,过滤掉本节点发送的消息和消息id相同的重复消息
	 * 
	 * @param message
	 * @return
	 */
	public boolean filter(Message message) {
		String sourceIp = message.getSourceIp();
		if (sourceIp.equals(config.getLocalHost())) {
			return false;
		}
		if (filterMap.containsKey(sourceIp) && message.getMessageId() <= filterMap.get(sourceIp)) {
			return false;
		}
		filterMap.put(sourceIp, message.getMessageId());
		return true;
	}
}

