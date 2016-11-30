package com.aos.lab3;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CheckpointRequestHandler implements ICheckpointRequestHandler {
	private static Logger logger = LogManager.getLogger(CheckpointRequestHandler.class);
	private Client client;
	private Boolean isRunning = Boolean.FALSE;
	private Config config;
	private Set<Integer> cohorts;
	private Integer initiator;
	private IApplicationStateHandler appStateHandler;

	private Set<Integer> waitingSet = new HashSet<Integer>();

	CheckpointRequestHandler(Client client, Config config, Integer src, IApplicationStateHandler appStateHandler) {
		this.client = client;
		this.config = config;
		this.cohorts = config.getNodeIdVsNeighbors().get(src);
		this.initiator = src;
		this.appStateHandler = appStateHandler;
	}

	@Override
	public void broadcast(Config config, Integer nodeId, MessageType msgType, String operationId) {
		int dest;
		Iterator<Integer> itr = cohorts.iterator();
		while (itr.hasNext()) {
			dest = itr.next();
			Message msg = new Message(initiator, nodeId, dest, client.getLlr()[dest], msgType, operationId);
			logger.debug("Sending checkpoint message to nodeId:{} from nodeId:{}", dest, initiator);
			client.sendMsg(msg);
			synchronized (waitingSet) {
				waitingSet.add(dest);
				logger.debug("Waiting set in nodeId:{} is {}", initiator, waitingSet);
			}
		}
		logger.info("BROADCAST! at:{} inside takeCheckpoint with operationId:{}", nodeId, operationId);
	}

	@Override
	public void handleCheckpointMessage(int src, int dest, Integer llr, Integer[] fls, String operationId)
			throws InterruptedException {
		logger.info("HANDLECHECKPOINT! at:{} inside takeCheckpoint with operationId:{}", src, operationId);
		if (!client.tentativeCheckpoint) {
			synchronized (isRunning) {
				isRunning = Boolean.TRUE;
				logger.info("isRunning TRUE HANDLECHECKPOINT! at:{} inside takeCheckpoint with operationId:{}", src,
						operationId);
			}
			client.tentativeCheckpoint = canITakeCheckpoint(src, dest, llr, fls);
			if (client.tentativeCheckpoint) {
				logger.info("Node :{} took tentative checkpoint with operationId:{}", src, operationId);
				takeCheckpoint(src, operationId);
			} else {
				logger.debug("NodeId:{} is already in checkpointing process", initiator);
			}
			logger.info("HANDLECHECKPOINT! at:{} inside takeCheckpoint with operationId:{}", src, operationId);
		}
		sendAck(dest, src, MessageType.ACKCHECKPOINT);
	}

	private void takeCheckpoint(Integer src, String operationId) throws InterruptedException {
		if (client.tentativeCheckpoint) {
			saveState();
			logger.info("SAVED STATED! at:{} inside takeCheckpoint with operationId:{}", src, operationId);
			client.initVectors();
			broadcast(config, initiator, MessageType.CHECKPOINT, operationId);
			while (true) {
				synchronized (waitingSet) {
					if (waitingSet.isEmpty())
						break;
				}
				Thread.sleep(200);
			}
		}
		synchronized (isRunning) {
			isRunning = Boolean.FALSE;
		}
	}

	private void saveState() {
		logger.debug("Saving vector values in nodeId:{}. LLR:{} LLS:{} FLS:{}", initiator, client.getLlr(),
				client.getLls(), client.getFls());
		appStateHandler.getAppValues().add(client.getAppCounter());
		appStateHandler.getLLR().add(client.getLlr());
		appStateHandler.getLLS().add(client.getLls());
		appStateHandler.getFLS().add(client.getFls());
		logger.info("SAVED STATE! at:{}", client.getNodeId());
	}

	private void sendAck(int src, int dest, MessageType msgType) {
		logger.info("SENDACK! at:{} to:{}", src, dest);
		this.client.sendMsg(new Message(initiator, src, dest, msgType));
	}

	public boolean canITakeCheckpoint(int src, int dest, Integer llr, Integer[] fls) {
		// checkpoint condition
		if ((llr >= fls[src]) && (fls[src] > Integer.MIN_VALUE))
			return true;
		else
			return false;
	}

	@Override
	public void handleAckChpMessage(Integer source, Integer destination) {
		logger.debug("Received checkpoint ACK message from nodeId:{} in nodeId:{}", source, destination);
		synchronized (waitingSet) {
			waitingSet.remove(source);
			logger.debug("Waiting set in nodeId:{} is {}", initiator, waitingSet);
		}
	}

	@Override
	public boolean isRunning() {
		synchronized (isRunning) {
			return isRunning;
		}
	}

	@Override
	public void requestCheckpoint(Integer counter, String operationId) throws InterruptedException {
		logger.info("Came inside requestCheckpoint with counter:{} and operationId:{}", counter, operationId);
		if (!client.tentativeCheckpoint) {
			synchronized (isRunning) {
				isRunning = Boolean.TRUE;
			}
			client.tentativeCheckpoint = Boolean.TRUE;
			takeCheckpoint(initiator, operationId);
		} else {
			logger.debug("NodeId:{} has already taken a tentative checkpoint", initiator);
		}
	}
}
