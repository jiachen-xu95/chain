package com.xjc.chain.web;

import com.alibaba.fastjson.JSON;
import com.xjc.chain.model.Block;
import com.xjc.chain.service.BlockService;
import com.xjc.chain.service.P2PService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ChainController.java
 *
 * @author Xujc
 * @date 2021/12/8
 */
@RestController
@RequestMapping(value = "/chain")
public class ChainController {

    @Autowired
    private BlockService blockService;

    @Autowired
    private P2PService p2PService;

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String upload(HttpServletRequest request, HttpServletResponse response, String data) {
        Block newBlock = blockService.generateNextBlock(data);
        blockService.addBlock(newBlock);
        p2PService.broatcast(p2PService.responseLatestMsg());
        return JSON.toJSONString(newBlock);
    }

}
