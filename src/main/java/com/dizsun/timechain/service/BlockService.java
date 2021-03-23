package com.dizsun.timechain.service;

import com.alibaba.fastjson.JSON;
import com.dizsun.timechain.component.ACK;
import com.dizsun.timechain.component.Block;
import com.dizsun.timechain.constant.Config;
import com.dizsun.timechain.util.CryptoUtil;
import com.dizsun.timechain.constant.R;

import java.util.ArrayList;
import java.util.List;

//import com.dizsun.util.SQLUtil;

/**
 * 本类整合了对本地区块链的基本数据操作
 * 其中getLatestBlock()、addBlock()、replaceChain()、rollback()
 * 等函数涉及到对本地链的读写，在多线程用途中要注意使用锁同步以避免数据出错
 */
public class BlockService {
    //TODO 恢复数据库的作用
    //    private SQLUtil sqlUtil;
    private List<Block> blockChain;
    private Config config = Config.getInstance();
    private PersistenceService persistenceService = PersistenceService.getInstance();

    private BlockService() {
    }

    private static class Holder {
        private static final BlockService blockService = new BlockService();
    }

    public static BlockService getInstance() {
        return Holder.blockService;
    }

    public void init() {
//        this.sqlUtil=new SQLUtil();
        blockChain = persistenceService.blockchainUpload(config.getLocalHost() + "." + config.getP2pPort());
        if (blockChain == null) {
            this.blockChain = new ArrayList<>();
            blockChain.add(this.getFirstBlock());
        }

//        List<Block> dbBlocks = sqlUtil.queryBlocks();
//        List<Block> dbBlocks = new ArrayList<>();
//        if(dbBlocks==null){
//            blockChain.add(this.getFirstBlock());
//            sqlUtil.initBlocks(blockChain);
//        }else{
//            blockChain=dbBlocks;
//        }
    }

    /**
     * 计算区块hash
     * 将(索引+前一个区块hash+时间戳+数据)进行hash
     *
     * @param index
     * @param previousHash
     * @param timestamp
     * @param data
     * @return
     */
    private String calculateHash(int index, String previousHash, long timestamp, String data) {
        StringBuilder builder = new StringBuilder(index);
        builder.append(previousHash).append(timestamp).append(data);
        return CryptoUtil.getSHA256(builder.toString());
    }

    /**
     * 获取创世区块
     * @return
     */
    private Block getFirstBlock() {
        return new Block(1, "0", 0, "Hello Block", "1db6aa3c81dc4b05a49eaed6feba99ed4ef07aa418d10bfbbc12af68cab6fb2a", 0, "0.0.0.0");
    }

    /**
     * 生成新区块并添加到链上
     *
     * @param blockData
     * @return
     */
    public void generateNextBlock(String blockData, String localHost) {
        Block previousBlock = this.getLatestBlock();
        int nextIndex = previousBlock.getIndex() + 1;
        long nextTimestamp = System.currentTimeMillis();
        String nextHash = calculateHash(nextIndex, previousBlock.getHash(), nextTimestamp, blockData);
        //int proof=createProofOfWork(previousBlock.getProof(),previousBlock.getHash());
        Block newBlock = new Block(nextIndex, previousBlock.getHash(), nextTimestamp, blockData, nextHash, R.getViewNumber(), localHost);
        blockChain.add(newBlock);
        R.endConsensus();
    }

    /**
     * 获取最新的区块
     * @return
     */
    public Block getLatestBlock() {
        return blockChain.get(blockChain.size() - 1);
    }

    /**
     * 向链上添加新区块
     * 并设置时间中心 ip
     * @param newBlock
     */
    public void addBlock(Block newBlock) {
        if (isValidNewBlock(newBlock, getLatestBlock())) {
//            sqlUtil.addBlock(newBlock);
            blockChain.add(newBlock);
            R.setViewNumber(newBlock.getVN());
            R.endConsensus();
            if (config.getLocalHost().equals(newBlock.getCreater())) { // 如果当前节点为代表节点，则ntp请求ip设为授时中心地址
                config.setTimeCenterIp(R.TIME_CENTER_IP);
            } else {
                config.setTimeCenterIp(newBlock.getCreater()); // 否则设置ntp请求ip为代表节点地址
            }
        }
    }

    /**
     * 用新链替换旧链
     *
     * @param newBlocks
     */
    public void replaceChain(List<Block> newBlocks) {
        if (isValidBlocks(newBlocks) && newBlocks.size() > blockChain.size()) {
//            sqlUtil.replaceChain(newBlocks);
            blockChain = newBlocks;
            R.setViewNumber(newBlocks.get(newBlocks.size() - 1).getVN());
            if (config.getLocalHost().equals(newBlocks.get(newBlocks.size() - 1).getCreater())) {
                config.setTimeCenterIp(R.TIME_CENTER_IP);
            } else {
                config.setTimeCenterIp(newBlocks.get(newBlocks.size() - 1).getCreater());
            }
        } else {
            System.out.println("收到的区块链为无效链");
        }
    }

    /**
     * 回滚上一次写的区块
     */
    public void rollback() {
        if (this.blockChain.size() > 0) {
            this.blockChain.remove(this.blockChain.size() - 1);
            R.setViewNumber(blockChain.get(blockChain.size() - 1).getVN());
        }
    }

    /**
     * 验证新区块是否合法
     *
     * @param newBlock
     * @param previousBlock
     * @return
     */
    private boolean isValidNewBlock(Block newBlock, Block previousBlock) {
        if (previousBlock.getIndex() + 1 != newBlock.getIndex()) {
            System.out.println("无效的 index");
            return false;
        } else if (!previousBlock.getHash().equals(newBlock.getPreviousHash())) {
            System.out.println("无效的 previoushash");
            return false;
        } else {
            String hash = calculateHash(newBlock.getIndex(), newBlock.getPreviousHash(), newBlock.getTimestamp(),
                    newBlock.getData());
            if (!hash.equals(newBlock.getHash())) {
                System.out.println("无效的 hash: " + hash + " " + newBlock.getHash());
                return false;
            }
        }
        return true;
    }

    /**
     * 验证区块链是否合法
     *
     * @param newBlocks
     * @return
     */
    private boolean isValidBlocks(List<Block> newBlocks) {
        Block firstBlock = newBlocks.get(0);
        if (!firstBlock.equals(getFirstBlock())) {
            return false;
        }

        for (int i = 1; i < newBlocks.size(); i++) {
            if (!isValidNewBlock(newBlocks.get(i), newBlocks.get(i - 1))) {
                return false;
            }
        }
        return true;
    }


    /**
     * 将所有ACK转换成json
     * @param acks
     * @return
     */
    public String getJSONData(List<ACK> acks) {
        return JSON.toJSONString(acks);
    }

    public List<Block> getBlockChain() {
        return blockChain;
    }

}
