package com.xjc.chain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Message.java
 *
 * @author Xujc
 * @date 2021/12/8
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message implements Serializable {

    private Integer type;
    private String data;

    public Message(Integer type) {
        this.type = type;
    }
}
