package com.dizsun.timechain.service;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
//import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.alibaba.fastjson.JSON;
import com.dizsun.timechain.component.ACK;
import com.dizsun.timechain.component.Message;
import com.dizsun.timechain.constant.Config;
import com.dizsun.timechain.constant.R;
import com.dizsun.timechain.constant.ViewState;
import com.dizsun.timechain.interfaces.ISubscriber;
import com.dizsun.timechain.util.DateUtil;
import com.dizsun.timechain.util.RSAUtil;

/**
 * 包含了和其他节点之间通信的协议和对消息处理的业务逻辑
 */
public class P2PService implements ISubscriber {
	private BlockService blockService;
	private ThreadPoolExecutor pool; // 线程池
	private RSAUtil rsaUtil;
	private PeerService peerService;
	private MessageHelper messageHelper;
	private Logger logger = Logger.getLogger(P2PService.class);
	private Config config = Config.getInstance();
	private int N = 1;
	private int stabilityValue = 128;
	private ViewState viewState = ViewState.Running;
	private List<ACK> acks;
	private DateUtil dateUtil = DateUtil.getInstance();

	private static class Holder {
		private static P2PService p2pService = new P2PService();
	}

	public static P2PService getInstance() {
		return Holder.p2pService;
	}

	private P2PService() {
	}

	public void initP2PServer(int port) {
		this.blockService = BlockService.getInstance();
		blockService.init();
		this.pool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		this.acks = new ArrayList<>();
		this.rsaUtil = RSAUtil.getInstance();
		logger.info("initing rsaUtil -> pubprikey............");
		rsaUtil.init(config.getLocalHost() + "." + config.getP2pPort());
		this.peerService = PeerService.getInstance();
		peerService.init(config.getLocalHost());
		this.messageHelper = MessageHelper.getInstance();
		messageHelper.init();
		final WebSocketServer socket = new WebSocketServer(new InetSocketAddress(port)) {
			public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
				peerService.write(webSocket, messageHelper.queryLatestBlock());
				String host = webSocket.getRemoteSocketAddress().getHostString();
				if (peerService.contains(host)) {
					peerService.removePeer(host);
				}
				peerService.addPeer(webSocket);
				logger.info("[initP2PServer.onOpen]当前连接节点列表：" + JSON.toJSONString(peerService.getPeerArray()));
			}

			public void onClose(WebSocket webSocket, int i, String s, boolean b) {
				logger.error(
						"[initP2PServer.onClose]服务端连接至" + webSocket.getRemoteSocketAddress().getHostString() + "节点关闭！");
				logger.error("[initP2PServer.onClose]关闭代码：" + i + "，额外信息：" + s + "，是否被远端关闭：" + b);
				peerService.removePeer(webSocket);
				logger.error("[initP2PServer.onClose]移除该连接后节点列表：" + JSON.toJSONString(peerService.getPeerArray()));
			}

			public void onMessage(WebSocket webSocket, String s) {
				Thread thread = new HandleMsgThread(webSocket, s);
				pool.execute(thread);
			}

			public void onError(WebSocket webSocket, Exception e) {
				logger.error(
						"[initP2PServer.onError]服务端连接至" + webSocket.getRemoteSocketAddress().getHostString() + "节点错误！");
				peerService.removePeer(webSocket);
				logger.error("[initP2PServer.onError]移除该连接后节点列表：" + JSON.toJSONString(peerService.getPeerArray()));
			}

			public void onStart() {

			}
		};
		socket.start();
		logger.info("在端口" + port + "监听P2P连接......");
	}

	/**
	 * 相应peer的信息请求
	 *
	 * @param webSocket
	 * @param s
	 */
	private void handleMessage(WebSocket webSocket, String s) {
		logger.info("[handleMessage]线程开始运行......");
		try {
			Message message = JSON.parseObject(s, Message.class);
			if (message.getSourceIp().equals(config.getLocalHost()))
				return;
			logger.info("[messageType]......" + message.getType());
			switch (message.getType()) {
			case R.QUERY_LATEST_BLOCK:
				logger.info("[handleMessage]请求新区块......");
				peerService.write(webSocket, messageHelper.responseLatestBlock());
				break;
			case R.QUERY_ALL_BLOCKS:
				logger.info("[handleMessage]请求所有区块......");
				peerService.write(webSocket, messageHelper.responseAllBlocks());
				break;
			case R.QUERY_ALL_PEERS:
				logger.info("[handleMessage]请求所有的节点列表......");
				peerService.write(webSocket, messageHelper.responseAllPeers());
				messageHelper.handlePeersResponse(message.getData());
				break;
			case R.RESPONSE_ALL_BLOCKS:
				logger.info("[handleMessage]收到新的区块链......");
				messageHelper.handleBlockChain(webSocket, message.getData());
				break;
			case R.RESPONSE_ALL_PEERS:
				logger.info("[handleMessage]收到新的节点列表......");
				messageHelper.handlePeersResponse(message.getData());
				break;
			case R.REQUEST_NEGOTIATION:
				/**
				 * 若对方同意开始共识,则回复ACK
				 */
				logger.info("[handleMessage]收到共识请求......");
				N = (peerService.length() + 1) / 3;
				logger.info("[handleMessage]当前连接节点数目的1/3为：" + N);
				if (viewState == ViewState.WaitingNegotiation) {
					R.beginConsensus();
					logger.info("[handleMessage]回复ACK......");
//					peerService.broadcast(messageHelper.responseACK());
					peerService.write(webSocket, messageHelper.responseACK());
					logger.info("[handleMessage]回复ACK完成......");
					viewState = ViewState.WaitingACK;
				}
				break;
			case R.RESPONSE_ACK:
				/**
				 * 收到对方的ACK后进行判断是否满足写区块条件
				 */
				logger.info("[handleMessage]收到一个ACK......");
				ACK tempACK = new ACK(message.getData());
				logger.info("[checkACK]测试ack成分:sign: " + tempACK.getSign());
				logger.info("[handleMessage]收到的ACK的正确性：" + checkACK(tempACK));
				if (viewState == ViewState.WaitingACK && checkACK(tempACK)) {
					if (stabilityValue == 1) {
						peerService.updateSI(webSocket, 1);
					} else {
						peerService.updateSI(webSocket, stabilityValue - 2);
						stabilityValue -= 2;
					}
					acks.add(tempACK);
					logger.info("[handleMessage]当前收到的ACK总数：" + acks.size());
					logger.info("[handleMessage]是否达到写区块条件：" + (acks.size() >= 2 && acks.size() >= 2 * N));
					if (acks.size() >= 2 && acks.size() >= 2 * N) {
						logger.info("[handleMessage]开始写区块并广播......");
						R.getBlockWriteLock().lock();
						writeBlock(config.getLocalHost());
						// 区块生成权限获取，写最新区块并且设置时间中心IP为授时中心
						config.setTimeCenterIp(R.TIME_CENTER_IP);
						peerService.broadcast(messageHelper.responseLatestBlock());
						dateUtil.getTimeFromRC();
						viewState = ViewState.Running;
						R.getBlockWriteLock().unlock();
						logger.info("[handleMessage]广播新区块完成！");
					}
				}
				break;
			case R.RESPONSE_BLOCK:
				/**
				 * 收到对方新区块
				 */
				switch (viewState) {
				case Running:
				case WritingBlock:
				case WaitingACK:
				case Negotiation:
				case WaitingBlock:
				case WaitingNegotiation:
					R.getBlockWriteLock().lock();
					try {
						stopWriteBlock();
						// handleBlock方法中加入了将授时中心变为新增区块创建者的语句
						messageHelper.handleBlock(webSocket, message.getData());
						viewState = ViewState.Running;
					} finally {
						R.getBlockWriteLock().unlock();
					}
					break;
				}
				break;
			}
		} catch (Exception e) {
			logger.info("[handleMessage]处理信息时发生错误：" + e.getMessage());
			e.printStackTrace();
		}
		logger.info("[handleMessage]线程结束运行......");
	}

	/**
	 * 停止写虚区块,若是已经计算完毕则回滚
	 */
	private void stopWriteBlock() {
		if (viewState == ViewState.WritingBlock) {
			viewState = ViewState.WaitingBlock;
		}
		if (blockService.getLatestBlock().getVN() == R.getViewNumber() + 1) {
			blockService.rollback();
		}
	}

	/**
	 * 生成新区块
	 */
	private void writeBlock(String localHost) {
		if (viewState == ViewState.Running) {
			return;
		}
		viewState = ViewState.WritingBlock;
		blockService.generateNextBlock(blockService.getJSONData(acks), localHost);
		logger.info("[writeBlock]生成新区块成功，创造者为" + localHost);
	}

	/**
	 * 处理消息的方法,开启处理线程
	 * 
	 * @param webSocket
	 * @param msg
	 */
	public void handleMsgThread(WebSocket webSocket, String msg) {
		Thread thread = new HandleMsgThread(webSocket, msg);
		pool.execute(thread);
	}

	/**
	 * 检查ACK是否合法
	 * 
	 * @param ack
	 * @return
	 */
	private boolean checkACK(ACK ack) {
		if (ack.getVN() != R.getViewNumber()) {
			return false;
		}
		String sign = rsaUtil.getPubPriKey().decrypt(ack.getPublicKey(), ack.getSign());
		if (!sign.equals(ack.getPublicKey() + ack.getVN()))
			return false;
		return true;
	}

	/**
	 * 当进入时间点tc时,开始共识
	 */
	@Override
	public void doPerTC() {
		logger.info("[doPerTC]进入时间点tc，视图编号：" + R.getViewNumber());
		switch (this.viewState) {
		case WaitingNegotiation:
			R.beginConsensus();
			N = (peerService.length() + 1) / 3;
			this.viewState = ViewState.WaitingACK;
			logger.info("[doPerTC]开始广播共识请求......");
			peerService.broadcast(messageHelper.requestNegotiation());
			logger.info("[doPerTC]广播共识请求完成！");
			break;
		default:
			break;
		}
	}

	/**
	 * running期间进行同步节点列表
	 */
	@Override
	public void doPerRunning() {
		logger.info("[doPerRunning]进入running状态，视图编号：" + R.getViewNumber());
		logger.info("[doPerRunning]开始广播节点列表和最新块......");
		peerService.broadcast(messageHelper.queryAllPeers());
		peerService.broadcast(messageHelper.responseLatestBlock());
		logger.info("[doPerRunning]广播节点列表和最新块完成！");
	}

	/**
	 * 进入时间点tp时,准备共识
	 */
	@Override
	public void doPerTP() {
		N = (peerService.length() + 1) / 3;
		logger.info("[doPerTP]进入时间点tp，视图编号：" + R.getViewNumber() + "，当前连接节点数目：" + peerService.length());
		this.viewState = ViewState.WaitingNegotiation;
	}

	/**
	 * 进入时间点te时,结束共识
	 */
	@Override
	public void doPerTE() {
		logger.info("[doPerTE]进入时间点te，视图编号：" + R.getViewNumber());
		// 此处加锁是因为handleMessage中的RESPONSE_ACK中需要判定acks，因此和这里会相互影响
		logger.info("[doPerTE]尝试获取锁");
		R.getBlockWriteLock().lock();
		logger.info("[doPerTE]已经获取锁");
		this.viewState = ViewState.Running;
		acks.clear();
		R.getBlockWriteLock().unlock();
		R.getAndIncrementViewNumber();
		stabilityValue = 128;
		peerService.regularizeSI();
		logger.info("[doPerTE]共识结束，查看当前最新块创造者：" + blockService.getLatestBlock().getCreater());
		logger.info("[doPerTE]共识结束，查看当前最新授时中心：" + config.getTimeCenterIp());
		// 本节点不是当前代表节点，需要向代表节点请求时间
		if (!config.getTimeCenterIp().equals(R.TIME_CENTER_IP))
			dateUtil.getTimeFromRC();
		// 追加方式写入最新区块日志
		CloseHook.persistenceAppend();
	}

	class HandleMsgThread extends Thread {
		private WebSocket ws;
		private String s;

		public HandleMsgThread(WebSocket ws, String s) {
			this.ws = ws;
			this.s = s;
		}

		@Override
		public void run() {
			logger.info("[HandleMsgThread]正在执行线程" + this.getName() + "......");
			handleMessage(ws, s);
		}

	}

	public void connect(String ip) {
		logger.info("[connect]准备连接至" + ip + "......");
		this.peerService.connectToPeer(ip);
		logger.info("[connect]已经连接至" + ip + "......");
	}
}
