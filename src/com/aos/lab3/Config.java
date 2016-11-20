package com.aos.lab3;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Config {

	private Integer noOfNodes;
	private Integer noOfOperations;
	private Integer minInstanceDelay;
	private Integer minSendDelay;
	private Integer noOfMsgs;

	private List<Node> nodes = new LinkedList<Node>();
	private Map<Integer, Set<Integer>> nodeIdVsNeighbors;
	private List<Operation> operations;

	public Config(Integer noOfNodes, Integer noOfOperations, Integer minInstanceDelay, Integer minSendDelay,
			Integer noOfMsgs, List<Node> nodes, Map<Integer, Set<Integer>> nodeIdVsNeighbors,
			List<Operation> operations) {
		super();
		this.noOfNodes = noOfNodes;
		this.noOfOperations = noOfOperations;
		this.minInstanceDelay = minInstanceDelay;
		this.minSendDelay = minSendDelay;
		this.noOfMsgs = noOfMsgs;
		this.nodes = nodes;
		this.nodeIdVsNeighbors = nodeIdVsNeighbors;
		this.operations = operations;
	}

	public Integer getNoOfNodes() {
		return noOfNodes;
	}

	public Integer getNoOfOperations() {
		return noOfOperations;
	}

	public Integer getMinInstanceDelay() {
		return minInstanceDelay;
	}

	public Integer getMinSendDelay() {
		return minSendDelay;
	}

	public Integer getNoOfMsgs() {
		return noOfMsgs;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public Map<Integer, Set<Integer>> getNodeIdVsQuorum() {
		return nodeIdVsNeighbors;
	}

}