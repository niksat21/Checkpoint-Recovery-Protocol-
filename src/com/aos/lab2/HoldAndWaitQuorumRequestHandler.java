package com.aos.lab2;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HoldAndWaitQuorumRequestHandler implements IQuorumRequestHandler {

	private static Logger logger = LogManager.getLogger(HoldAndWaitCSHandler.class);

	private List<CSRequest> requestQueue = new LinkedList<CSRequest>();
	private Node quorumNode;
	private Client client;
	private Config config;

	public HoldAndWaitQuorumRequestHandler(Node quorumNode, Config config) {
		super();
		this.quorumNode = quorumNode;
		this.config = config;
	}

	@Override
	public synchronized void handleReleaseMessage(Integer source) {
		logger.debug("Received release message from nodeId:{} in quorum nodeId:{}", source, quorumNode.getNodeId());
		requestQueue.remove(0);
		if (requestQueue.size() != 0) {
			CSRequest request = requestQueue.get(0);
			sendGrantMessage(request.getNodeId());
		} else {
			logger.debug("No pending request in the quorum nodeId:{}", quorumNode.getNodeId());
		}
	}

	private void sendGrantMessage(Integer destinationId) {
		logger.debug("Sending grant message to nodeId:{} from  quorum nodeId:{}", destinationId,
				quorumNode.getNodeId());
		Message msg = new Message(quorumNode.getNodeId(), destinationId, MessageType.GRANT);
		client.sendMsg(msg);
	}

	@Override
	public void handleYieldMessage(Integer sourceNodeId) {
		logger.error("Calling unimplemented method: handleYieldMessage in HoldAndWait Quorum Request handler");
	}

	@Override
	public void handleRequestMessage(CSRequest request) {
		logger.debug("Received request message from nodeId:{} in quorum nodeId:{}", request.getNodeId(),
				quorumNode.getNodeId());
		requestQueue.add(request);

		if (requestQueue.size() == 1) {
			sendGrantMessage(request.getNodeId());
		}
	}

	@Override
	public void setClientHandler(Client client) {
		this.client = client;
	}
	
	
	public boolean checkRequestingQueue(){
		if(this.requestQueue.isEmpty())
			return true;
		else
			return false;
		
	}

}
