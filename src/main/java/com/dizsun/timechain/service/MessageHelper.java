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

	private static class Holder {
		private static final MessageHelper messageHelper = new MessageHelper();
	}

	public static MessageHelper getInstance() {
		return Holder.messageHelper;
	}

	private MessageHelper() {
	}

	public void init() {
		blockService = BlockService.getInstance();
		peerService = PeerService.getInstance();
		rsaUtil = RSAUtil.getInstance();
		config = Config.getInstance();
		filterMap = new ConcurrentHashMap<>();
	}

	public String queryAllBlock() {
		return JSON.toJSONString(new Message(R.QUERY_ALL_BLOCKS, JSON.toJSONString(blockService.getBlockChain()),
				config.getLocalHost(), R.getAndIncrementMessageId()));
	}

	public String queryLatestBlock() {
		return JSON
				.toJSONString(new Message(R.QUERY_LATEST_BLOCK, config.getLocalHost(), R.getAndIncrementMessageId()));
	}

	public String queryAllPeers() {
		return JSON.toJSONString(new Message(R.QUERY_ALL_PEERS, JSON.toJSONString(peerService.getPeerArray()),
				config.getLocalHost(), R.getAndIncrementMessageId()));
	}

	public String requestNegotiation() {
		return JSON
				.toJSONString(new Message(R.REQUEST_NEGOTIATION, config.getLocalHost(), R.getAndIncrementMessageId()));
	}

	public String responseAllBlocks() {
		return JSON.toJSONString(new Message(R.RESPONSE_ALL_BLOCKS, JSON.toJSONString(blockService.getBlockChain()),
				config.getLocalHost(), R.getAndIncrementMessageId()));
	}

	public String responseLatestBlock() {
		Block block = blockService.getLatestBlock();
		return JSON.toJSONString(new Message(R.RESPONSE_BLOCK, JSON.toJSONString(block), config.getLocalHost(),
				R.getAndIncrementMessageId()));
	}

//    public String syncBlock() {
//        Block block = blockService.getLatestBlock();
//        return JSON.toJSONString(new Message(R.SYNC, JSON.toJSONString(block), config.getLocalHost(), R.getAndIncrementMessageId()));
//    }

//    public String responseBlock() {
//        return JSON.toJSONString(new Message(R.RESPONSE_BLOCK, JSON.toJSONString(blockService.getBlockChain()), config.getLocalHost(), R.getAndIncrementMessageId()));
//    }

	public String responseAllPeers() {
		return JSON.toJSONString(new Message(R.RESPONSE_ALL_PEERS, JSON.toJSONString(peerService.getPeerArray()),
				config.getLocalHost(), R.getAndIncrementMessageId()));
	}

	public String responseACK() {
		int vn = R.getViewNumber();
		ACK ack = new ACK();
		ack.setVN(vn);
		ack.setPublicKey(rsaUtil.getPubPriKey().getPublicKeyBase64());
		ack.setSign(rsaUtil.getPubPriKey().encrypt(rsaUtil.getPubPriKey().getPublicKeyBase64() + vn));
		return JSON.toJSONString(new Message(R.RESPONSE_ACK, JSON.toJSONString(ack), config.getLocalHost(),
				R.getAndIncrementMessageId()));
	}

	/**
	 * 处理接收到的单个区块
	 * 
	 * @param ws
	 * @param message
	 */
	public void handleBlock(WebSocket ws, String message) {
		Block receivedBlock = JSON.parseObject(message, Block.class);
//        R.getBlockReadLock().lock();
		Block latestBlock = blockService.getLatestBlock();
		logger.info("[receive a new block]from: " + receivedBlock.getCreater());
		logger.info("[check latest block]from: " + latestBlock.getCreater());
		logger.info("[compare index: ]latestIndex: " + latestBlock.getIndex() + "receivedIndex: "
				+ receivedBlock.getIndex());
		if (receivedBlock.getIndex() <= latestBlock.getIndex())
			return;
//        R.getBlockReadLock().unlock();

		if (latestBlock.getIndex() + 1 == receivedBlock.getIndex()) {
//            R.getBlockWriteLock().lock();
			config.setTimeCenterIp(receivedBlock.getCreater());
			logger.info(
					"[RESPONSE_BLOCK -> MessageHelper] new generated block's creator " + receivedBlock.getCreater());
			blockService.addBlock(receivedBlock);
//            R.getBlockWriteLock().unlock();
		} else {
			peerService.write(ws, queryAllBlock());
		}
	}

	/**
	 * 处理接收到的区块链
	 * 
	 * @param ws
	 * @param message
	 */
	public void handleBlockChain(WebSocket ws, String message) {
		List<Block> receivedBlocks = JSON.parseArray(message, Block.class);
		R.getBlockReadLock().lock();
		Block latestBlockReceived = receivedBlocks.get(receivedBlocks.size() - 1);
		Block latestBlock = blockService.getLatestBlock();
		if (latestBlockReceived.getIndex() <= latestBlock.getIndex()) {
			R.getBlockReadLock().unlock();
			return;
		}
		R.getBlockReadLock().unlock();

		R.getBlockWriteLock().lock();
		blockService.replaceChain(receivedBlocks);
		R.getBlockWriteLock().unlock();
	}

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
