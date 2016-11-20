package com.aos.lab3;

public class Operation {

	private OperationType type;
	private Integer nodeId;

	public Operation(OperationType type, Integer nodeId) {
		super();
		this.type = type;
		this.nodeId = nodeId;
	}

	public OperationType getType() {
		return type;
	}

	public Integer getNodeId() {
		return nodeId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		return builder.append("Operation Type:").append(type.toString()).append(" NodeId:").append(nodeId).toString();
	}

}
