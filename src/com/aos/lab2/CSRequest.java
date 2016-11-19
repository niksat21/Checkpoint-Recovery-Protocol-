package com.aos.lab2;

public class CSRequest implements Comparable<CSRequest> {

	private Integer nodeId;
	private Long timestamp;

	public CSRequest(Integer node, Long timestamp) {
		super();
		this.nodeId = node;
		this.timestamp = timestamp;
	}

	public CSRequest(Integer node) {
		super();
		this.nodeId = node;
	}

	public Integer getNodeId() {
		return nodeId;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	@Override
	public int compareTo(CSRequest o) {
		if (this.timestamp < o.getTimestamp())
			return -1;
		else if (this.timestamp > o.getTimestamp())
			return 1;
		else {
			if (this.nodeId < o.getNodeId())
				return -1;
			else
				return 1;
		}
	}
	
	@Override
	public String toString() {
		return "NodeId:" + nodeId + " TS:" + timestamp;
 	}

}
