package com.xjc.chain.service;

import com.alibaba.fastjson.JSON;
import com.xjc.chain.model.Block;
import com.xjc.chain.model.Message;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * P2PService.java
 *
 * @author Xujc
 * @date 2021/12/8
 */
@Service
public class P2PService {

    @Autowired
    private BlockService blockService;
    private List<WebSocket> sockets;
    private static final int QUERY_LATEST = 0;
    private static final int QUERY_ALL = 1;
    private static final int RESPONSE_BLOCKCHAIN = 2;

    public P2PService() {
        this.sockets = new ArrayList<>();
    }

    public void initP2PServer(int port) {
        final WebSocketServer socket = new WebSocketServer() {
            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                write(webSocket, queryChainLengthMsg());
                // /将启动的节点加入区块链 websocket list中
                sockets.add(webSocket);
            }

            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
                //websocket长链接断后触发onClose回调，将webSocket实例从list中移除
                sockets.remove(webSocket);
            }

            @Override
            public void onMessage(WebSocket webSocket, String s) {
                handleMessage(webSocket, s);
            }

            @Override
            public void onError(WebSocket webSocket, Exception e) {
                sockets.remove(webSocket);
            }

            @Override
            public void onStart() {

            }
        };
        socket.start();
        System.out.println("listening websocket p2p port on: " + port);
    }

    private void handleMessage(WebSocket webSocket, String s) {
        try {
            Message message = JSON.parseObject(s, Message.class);
            System.out.println("Received message" + JSON.toJSONString(message));
            switch (message.getType()) {
                case QUERY_LATEST:
                    write(webSocket, responseLatestMsg());
                    break;
                case QUERY_ALL:
                    write(webSocket, responseChainMsg());
                    break;
                case RESPONSE_BLOCKCHAIN:
                    handleBlockChainResponse(message.getData());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.out.println("hanle message is error:" + e.getMessage());
        }
    }

    private String responseChainMsg() {
        return JSON.toJSONString(new Message(RESPONSE_BLOCKCHAIN, JSON.toJSONString(blockService)));
    }

    private void write(WebSocket ws, String message) {
        ws.send(message);
    }

    private String queryChainLengthMsg() {
        return JSON.toJSONString(new Message(QUERY_LATEST));
    }

    /**
     * websocketclient连接到server节点
     * @param peer
     */
    public void connectToPeer(String peer) {
        try {
            final WebSocketClient socket = new WebSocketClient(new URI(peer)) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    write(this, queryChainLengthMsg());
                    sockets.add(this);
                }

                @Override
                public void onMessage(String s) {
                    handleMessage(this, s);
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    System.out.println("connection failed");
                    sockets.remove(this);
                }

                @Override
                public void onError(Exception e) {
                    System.out.println("connection failed");
                    sockets.remove(this);
                }
            };
            socket.connect();
        } catch (URISyntaxException e) {
            System.out.println("p2p connect is error:" + e.getMessage());
        }
    }

    private void handleBlockChainResponse(String message) {
        List<Block> receiveBlocks = JSON.parseArray(message, Block.class);
        //将从消息中获取到的区块链中所有的区块节点，按照index顺序进行排序，进行排序是为了防止节点传递的区块链是无序的
        Collections.sort(receiveBlocks, (o1, o2) -> o1.getIndex() - o1.getIndex());

        Block latestBlockReceived = receiveBlocks.get(receiveBlocks.size() - 1);
        Block latestBlock = blockService.getLatestBlock();

        //将从其他节点获取到的区块链最新节点和本节点最新的节点进行比较
        if (latestBlockReceived.getIndex() > latestBlock.getIndex()) {
            //本地最新节点的hash等于传递主链中的最新区块的上一个区块的hash则将数据验证后上链
            if (latestBlock.getHash().equals(latestBlockReceived.getPreviousHash())) {
                blockService.addBlock(latestBlockReceived);
                //新区块上链后广播通知所有节点最新区块
                broatcast(responseLatestMsg());
            } else if (receiveBlocks.size() == 1) {
                //节点为新节点则去要广播所有节点去查询区块
                broatcast(queryAllMsg());
            } else {
                //区块hash不匹配则进行区块链替换
                blockService.replaceChain(receiveBlocks);
            }
        } else {
            System.out.println("received blockchain is not longer than received blockchain. Do nothing");
        }
    }

    public void broatcast(String message) {
        for (WebSocket socket : sockets) {
            this.write(socket, message);
        }
    }

    public String responseLatestMsg() {
        Block[] blocks = {blockService.getLatestBlock()};
        return JSON.toJSONString(new Message(RESPONSE_BLOCKCHAIN, JSON.toJSONString(blocks)));
    }

    private String queryAllMsg() {
        return JSON.toJSONString(new Message(QUERY_ALL));
    }

}
