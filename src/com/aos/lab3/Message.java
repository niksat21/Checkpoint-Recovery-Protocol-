package com.aos.lab3;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Message implements Serializable {

	private Integer source;
	private Integer destination;
	private List<Integer> path = new LinkedList<Integer>();
	private Integer value;
	private MessageType msgType;
	private Long requestTS;
	private Integer initiator;
	private String operationId;

	public Message(Integer initiator, Integer source, Integer destination, Integer value, MessageType msgType,
			String operationId) {
		this.source = source;
		this.destination = destination;
		this.value = value;
		this.msgType = msgType;
		this.initiator = initiator;
		this.operationId = operationId;
	}

	public Message(Integer initiator, Integer source, Integer destination, MessageType msgType) {
		this.source = source;
		this.destination = destination;
		this.msgType = msgType;
		this.initiator = initiator;
	}

	public Message(Integer source, Integer destination, MessageType msgType) {
		super();
		this.source = source;
		this.destination = destination;
		this.msgType = msgType;
	}

	public Message(Integer source, Integer destination, MessageType msgType, Long requestTS) {
		super();
		this.source = source;
		this.destination = destination;
		this.msgType = msgType;
		this.requestTS = requestTS;
	}

	public Long getRequestTS() {
		return requestTS;
	}

	public Integer getSource() {
		return source;
	}

	public Integer getDestination() {
		return destination;
	}

	public List<Integer> getPath() {
		return path;
	}

	public Integer getValue() {
		return value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	public MessageType getMsgType() {
		return msgType;
	}

	public void setSource(Integer source) {
		this.source = source;
	}

	public void setDestination(Integer destination) {
		this.destination = destination;
	}

	public void setPath(List<Integer> path) {
		this.path = path;
	}

	public Integer getInitiator() {
		return initiator;
	}

	public String getOperationId() {
		return operationId;
	}

}