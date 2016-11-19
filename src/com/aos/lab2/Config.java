package com.aos.lab2;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Config {

	private int noOfNodes;
	private int csExecTime;
	private int waitTime;
	private int noOfAttempts;
	private List<Node> nodes = new LinkedList<Node>();
	private Map<Integer, Set<Integer>> nodeIdVsQuorum = new HashMap<Integer, Set<Integer>>();
	private Map<Integer, Node> nodeIdVsNodes = new HashMap<Integer, Node>();

	private DeadlockResolverType version = null;

	public Config(int noOfNodes, int csExecTime, int waitTime, int noOfAttempts, List<Node> nodes,
			Map<Integer, Set<Integer>> nodeIdVsQuorum) {
		super();
		this.noOfNodes = noOfNodes;
		this.csExecTime = csExecTime;
		this.waitTime = waitTime;
		this.noOfAttempts = noOfAttempts;
		this.nodes = nodes;
		this.nodeIdVsQuorum = nodeIdVsQuorum;
		populateNodeIdVsNodesMap();
	}

	private void populateNodeIdVsNodesMap() {
		for (Node node : nodes) {
			nodeIdVsNodes.put(node.getNodeId(), node);
		}
	}

	public int getNoOfNodes() {
		return noOfNodes;
	}

	public int getCsExecTime() {
		return csExecTime;
	}

	public int getWaitTime() {
		return waitTime;
	}

	public int getNoOfAttempts() {
		return noOfAttempts;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public Set<Integer> getNodeQuorumById(Integer id) {
		return nodeIdVsQuorum.get(id);
	}

	public Node getNodeById(Integer id) {
		return nodeIdVsNodes.get(id);
	}

	public DeadlockResolverType getVersion() {
		return version;
	}

	public void setVersion(DeadlockResolverType version) {
		this.version = version;
	}

	// public Boolean checkIntersection() {
	// int counter = 0;
	// for (Integer i : nodeIdVsQuorum.keySet()) {
	// Set<Integer> first = new ArrayList<>();
	// first = nodeIdVsQuorum.get(i);
	// int tempc = 1;
	// for (Integer j : nodeIdVsQuorum.keySet()) {
	// List<Integer> temp = new ArrayList<>();
	// if (i != j) {
	// for (int m = 0; m < nodeIdVsQuorum.get(j).size(); m++)
	// temp.add(nodeIdVsQuorum.get(j).get(m));
	// temp.retainAll(first);
	// if (temp.size() > 0)
	// tempc++;
	// }
	// }
	// if (tempc == noOfNodes) {
	// counter++;
	// }
	// }
	// if (counter == noOfNodes)
	// return true;
	// else
	// return false;
	// }

}