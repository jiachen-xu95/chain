package com.xjc.chain.service;

import com.xjc.chain.model.Block;
import com.xjc.chain.utils.SHA256Util;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockService.java
 *
 * @author Xujc
 * @date 2021/12/8
 */
@Service
public class BlockService {

    private List<Block> blockChain;

    public BlockService() {
        this.blockChain = new ArrayList<>();
        blockChain.add(createFirstBlock());
    }

    public List<Block> getBlockChain() {
        return blockChain;
    }

    /**
     * 创建第一个区块
     *
     * @return
     */
    private Block createFirstBlock() {
        Block black = new Block();
        black.setIndex(0);
        black.setPreviousHash(SHA256Util.getSHA256(black.toString()));
        black.setTimeStamp(System.currentTimeMillis());
        black.setData("BLOCK_PARENT");
        black.setHash(SHA256Util.getSHA256(black.toString()));
        return black;
    }

    /**
     * 根据index、前置区块hash、区块时间戳、区块数据区计算sha256
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
        return SHA256Util.getSHA256(builder.toString());
    }

    /**
     * 获取最新的区块
     *
     * @return
     */
    protected Block getLatestBlock() {
        return blockChain.get(blockChain.size() - 1);
    }

    /**
     * 验证区块是否为有效的新区块
     *
     * @param next
     * @param pre
     * @return
     */
    private Boolean validNewBlock(Block next, Block pre) {
        if (pre.getIndex() + 1 != next.getIndex()) {
            return false;
        }
        // 前置节点hash与下一个节点的前置hash是否一致
        if (!pre.getHash().equals(next.getPreviousHash())) {
            return false;
        }
        // 判断下一个节点的hash是否被修改过
        String nextBlockHash = calculateHash(next.getIndex(), next.getPreviousHash(), next.getTimeStamp(), next.getData());
        return nextBlockHash.equals(next.getHash());
    }

    /**
     * 创建下一个新区块
     *
     * @param blockData
     * @return
     */
    public Block generateNextBlock(String blockData) {
        Block previousBlock = this.getLatestBlock();
        int nextIndex = previousBlock.getIndex() + 1;
        long nextTimestamp = System.currentTimeMillis();
        String nextHash = calculateHash(nextIndex, previousBlock.getHash(), nextTimestamp, blockData);
        return new Block(nextIndex, previousBlock.getHash(), nextTimestamp, blockData, nextHash);
    }

    /**
     * 上链
     * @param block
     * @return
     */
    public Boolean addBlock(Block block) {
        if (validNewBlock(block, getLatestBlock())) {
            blockChain.add(block);
            return true;
        }
        return false;
    }

    /**
     * 验证链上每个节点是否有效
     *
     * @param blackChain
     * @return
     */
    private Boolean validEffectiveBlocks(List<Block> blackChain) {
        Block firstBlock = blackChain.get(0);
        if (firstBlock.equals(createFirstBlock())) {
            return false;
        }

        for (Block block : blackChain) {
            if (validNewBlock(block, firstBlock)) {
                firstBlock = block;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 以最长的有效区块链为主链，替换之前节点保存的链
     *
     * @param newBlocks
     */
    protected void replaceChain(List<Block> newBlocks) {
        if (validEffectiveBlocks(newBlocks) && newBlocks.size() > blockChain.size()) {
            blockChain = newBlocks;
        } else {
            System.out.println("Received blockchain invalid");
        }
    }


}
