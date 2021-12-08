package com.xjc.chain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Block.java
 *
 * @author Xujc
 * @date 2021/12/8
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Block implements Serializable {

    private Integer index;
    private String previousHash;
    private Long timeStamp;
    private String data;
    private String hash;
}
