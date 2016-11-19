package com.aos.lab2;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HoldAndWaitCSHandler implements ICriticalSectionHandler {

	private static final Logger logger = LogManager.getLogger(HoldAndWaitCSHandler.class);
	private static final int DEFAULT_SLEEP_TIME = 100;
	private static final int RELEASE_WAIT = 2000;

	private Config config;
	private Node sourceNode;
	private Client client;
	private Set<Integer> quorumSet;
	private Integer requestedNodeId = null;
	private volatile boolean waitingForReply = false;

	public HoldAndWaitCSHandler(Config config, Node sourceNode, Set<Integer> quorumSet) {
		super();
		this.config = config;
		this.sourceNode = sourceNode;
		this.quorumSet = quorumSet;
	}

	@Override
	public void csEnter(Long timestamp) throws InterruptedException {
		// Send request message to all the nodes in the quorum set
		for (Integer nodeId : quorumSet) {
			Message msg = new Message(sourceNode.getNodeId(), nodeId, MessageType.REQUEST);
			logger.debug("Sending request message to nodeId:{} from nodeId:{}. Timestamp: {}", nodeId,
					sourceNode.getNodeId(), timestamp);
			client.sendMsg(msg);
			requestedNodeId = nodeId;
			waitingForReply = true;
			while (true) {
				synchronized (this) {
					if (!waitingForReply) {
						break;
					}
				}
				Thread.sleep(DEFAULT_SLEEP_TIME);
			}
		}
		Thread.sleep(RELEASE_WAIT);
	}

	@Override
	public void csLeave() {
		Integer[] nodes = new Integer[quorumSet.size()];
		quorumSet.toArray(nodes);
		for (int i = nodes.length - 1; i >= 0; i--) {
			logger.debug("Sending release message to nodeId:{} from nodeId:{}", nodes[i],
					sourceNode.getNodeId());
			client.sendMsg(new Message(sourceNode.getNodeId(), nodes[i], MessageType.RELEASE));
		}
	}

	public synchronized void handleGrantMessage(Integer nodeId) {
		logger.debug("Received grant message by nodeId:{} from quorum nodeId:{}", sourceNode.getNodeId(), nodeId);
		waitingForReply = false;
		requestedNodeId = null;
	}

	@Override
	public void handleFailedMessage(Integer nodeId) {
		logger.error("Unimplemented method handleFailedMessage in HoldAndWaitCS Handler");

	}

	@Override
	public void handleInquireMessage(Integer nodeId) {
		logger.error("Unimplemented method handleInquireMessage in HoldAndWaitCS Handler");
	}

	@Override
	public void setClientHandler(Client client) {
		this.client = client;
	}
	
	
	public boolean checkSets() {
		return true;
	}

}
