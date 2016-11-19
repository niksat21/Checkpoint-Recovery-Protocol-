package com.aos.lab2;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PreemptiveCSHandler implements ICriticalSectionHandler {

	private static final Logger logger = LogManager.getLogger(PreemptiveCSHandler.class);
	private static final int DEFAULT_SLEEP_TIME = 100;

	private Config config;
	private Set<Integer> quorumSet;
	private Set<Integer> grantSet;
	private Set<Integer> failedSet;
	private Node sourceNode;
	private Client client;
	private boolean isInsideCS = false;
	private boolean wasInquired = false;
	private Integer inquiredBy = null;

	public PreemptiveCSHandler(Config config, Node sourceNode, Set<Integer> quorumSet) {
		super();
		this.config = config;
		this.sourceNode = sourceNode;
		this.quorumSet = quorumSet;
		this.grantSet = new HashSet<Integer>();
		this.failedSet = new HashSet<Integer>();
	}
	
	public PreemptiveCSHandler(Node sourceNode){
		this.sourceNode = sourceNode;
	}

	@Override
	public void csEnter(Long timestamp) throws InterruptedException {
		// Send request message to all the nodes in the quorum set
		for (Integer nodeId : quorumSet) {
			Message msg = new Message(sourceNode.getNodeId(), nodeId, MessageType.REQUEST, timestamp);
			logger.debug("Sending request message to nodeId:{} from nodeId:{}", nodeId, sourceNode.getNodeId(),
					timestamp);
			client.sendMsg(msg);
		}
		while (true) {
			synchronized (this) {
				// TODO: Check if the requesting node is also in quorum set. If
				// so, then check with size - 1
				if (failedSet.size() == 0 && grantSet.size() == quorumSet.size())
					break;
			}
			Thread.sleep(DEFAULT_SLEEP_TIME);
		}
		isInsideCS = true;
		logger.debug("Critical Section request granted for NodeId:{}", sourceNode.getNodeId());
		logger.debug("NodeId:{} GrantSet:{} FailedSet:{}", sourceNode.getNodeId(), grantSet, failedSet);
	}

	@Override
	public void csLeave() {
		// Send release message to all the nodes in the quorum set
		for (Integer nodeId : quorumSet) {
			Message msg = new Message(sourceNode.getNodeId(), nodeId, MessageType.RELEASE);
			logger.debug("Sending release message to nodeId:{} from nodeId:{}", nodeId, sourceNode.getNodeId());
			client.sendMsg(msg);
		}
		isInsideCS = false;
		grantSet.clear();
		failedSet.clear();
		logger.debug("NodeId:{} GrantSet:{} FailedSet:{}", sourceNode.getNodeId(), grantSet, failedSet);
	}

	public synchronized void handleFailedMessage(Integer nodeId) {
		logger.debug("Received failed message for request by nodeId:{} from quorum nodeId:{}", sourceNode.getNodeId(),
				nodeId);
		if (failedSet.contains(nodeId)) {
			logger.error("Something wrong in the nodeId:{} . Failed set already contains the nodeId:{}",
					sourceNode.getNodeId(), nodeId);
		}
		failedSet.add(nodeId);

		if (wasInquired && inquiredBy != null) {
			sendYieldMessage(inquiredBy);
			wasInquired = false;
			inquiredBy = null;
		}
		logger.debug("NodeId:{} GrantSet:{} FailedSet:{}", sourceNode.getNodeId(), grantSet, failedSet);
	}

	public synchronized void handleInquireMessage(Integer nodeId) {
		logger.debug("Received inquire message by nodeId:{} from quorum nodeId:{}", sourceNode.getNodeId(), nodeId);
		if (isInsideCS) {
			logger.info("The nodeId:{} is already in the critical section. Not reliquishing", sourceNode.getNodeId());
		} else {
			if (failedSet.size() >= 1) {
				logger.debug(
						"Received failed message from at least 1 quorum member in nodeId:{} . So, sending yield message to quorum nodeId:{}",
						sourceNode.getNodeId(), nodeId);
				sendYieldMessage(nodeId);
			} else {
				wasInquired = true;
				inquiredBy = nodeId;
			}
		}
		logger.debug("NodeId:{} GrantSet:{} FailedSet:{}", sourceNode.getNodeId(), grantSet, failedSet);
	}

	private void sendYieldMessage(Integer nodeId) {
		logger.debug("Sending yield message from nodeId:{} to quorum nodeId:{}", sourceNode.getNodeId(), nodeId);
		client.sendMsg(new Message(sourceNode.getNodeId(), nodeId, MessageType.YIELD));
		grantSet.remove(nodeId);
		failedSet.add(nodeId);
	}

	public synchronized void handleGrantMessage(Integer nodeId) {
		logger.debug("Received grant message by nodeId:{} from quorum nodeId:{}", sourceNode.getNodeId(), nodeId);
		if (grantSet.contains(nodeId)) {
			logger.error("Something wrong in the nodeId:{} . Grant set already contains the nodeId:{}",
					sourceNode.getNodeId(), nodeId);
		}
		grantSet.add(nodeId);
		failedSet.remove(nodeId);
		logger.debug("NodeId:{} GrantSet:{} FailedSet:{}", sourceNode.getNodeId(), grantSet, failedSet);
	}

	@Override
	public void setClientHandler(Client client) {
		this.client = client;
	}

	
	public boolean checkSets() {
		if(this.grantSet.isEmpty() && this.failedSet.isEmpty())
			return true;
		else
			return false;
	}

}
