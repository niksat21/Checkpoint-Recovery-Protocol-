package com.aos.lab3;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
	private Set<String> operationIds = new HashSet<String>();
	private List<Message> operationQueue;
	private RequestingCandidate rc;

	CheckpointRequestHandler(Client client, Config config, Integer src, IApplicationStateHandler appStateHandler,
			List<Message> operationQueue) {
		this.client = client;
		this.config = config;
		this.cohorts = config.getNodeIdVsNeighbors().get(src);
		this.initiator = src;
		this.appStateHandler = appStateHandler;
		this.operationQueue = operationQueue;
	}

	@Override
	public void broadcast(Config config, Integer nodeId, MessageType msgType, String operationId) {
		int dest;
		Iterator<Integer> itr = cohorts.iterator();
		while (itr.hasNext()) {
			dest = itr.next();
			Message msg = new Message(initiator, nodeId, dest, client.getLlr()[dest], msgType, operationId);
			logger.debug("Sending {} message to nodeId:{} from nodeId:{}", msgType.toString(), dest, initiator);
			client.sendMsg(msg);
			if (msgType.equals(MessageType.CHECKPOINT)) {
				synchronized (waitingSet) {
					waitingSet.add(dest);
					logger.debug("Waiting set in nodeId:{} is {}", initiator, waitingSet);
				}
			}
		}
	}

	@Override
	public void handleCheckpointMessage(Message msg, Integer[] fls, String operationId) throws InterruptedException {
		Integer src = msg.getSource();
		Integer dest = msg.getDestination();
		Integer llr = msg.getValue();
		logger.info("HANDLECHECKPOINT! at:{} from:{} value: {} and fls:{} inside take Checkpoint with operationId:{}",
				dest, src, llr, fls, operationId);
		if (!client.tentativeCheckpoint && !operationIds.contains(operationId)) {
			synchronized (isRunning) {
				isRunning = Boolean.TRUE;
				logger.info("isRunning TRUE HANDLECHECKPOINT! at:{} inside takeCheckpoint with operationId:{}", src,
						operationId);
			}
			client.tentativeCheckpoint = canITakeCheckpoint(src, dest, llr, fls);
			logger.info("Node :{} can take CHECKPOINT??:{}", src, client.tentativeCheckpoint);
			if (client.tentativeCheckpoint) {
				logger.info("Node :{} took tentative checkpoint with operationId:{}", src, operationId);
				takeCheckpoint(src, operationId);
			} else {
				logger.debug("NodeId:{} is already in checkpointing process", initiator);
				sendAck(dest, src, MessageType.ACKCHECKPOINT, operationId);
			}
			logger.info("HANDLECHECKPOINT! at:{} inside takeCheckpoint with operationId:{}", src, operationId);
		} else if (client.tentativeCheckpoint && !operationIds.contains(operationId)) {
			synchronized (operationQueue) {
				operationQueue.add(msg);
			}
			logger.debug("Queued msg type: {} from nodeId:{} at nodeId:{} by initiator:{}", msg.getMsgType(),
					msg.getSource(), msg.getDestination(), msg.getInitiator());
		} else if (operationId.contains(operationId)) {
			logger.debug(
					"Operation set in nodeId:{} already contains operationId:{}. Sending ACK to nodeId:{} initiator:{}",
					initiator, operationId, src, msg.getInitiator());
			sendAck(dest, src, MessageType.ACKCHECKPOINT, operationId);
		}
	}

	private void takeCheckpoint(Integer src, String operationId) throws InterruptedException {
		if (client.tentativeCheckpoint) {
			operationIds.add(operationId);
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
			logger.info("Checkpointing completed at nodeId:{} initiated by nodeId:{}", initiator, src);
			synchronized (isRunning) {
				isRunning = Boolean.FALSE;
				client.tentativeCheckpoint = Boolean.FALSE;
			}
			sendAck(initiator, src, MessageType.ACKCHECKPOINT, operationId);
		} else {
			logger.debug("Something wrong in nodeId:{} ", initiator);
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

	private void sendAck(int src, int dest, MessageType msgType, String operationId) {
		logger.info("SENDACK! at:{} to:{}", src, dest);
		this.client.sendMsg(new Message(initiator, src, dest, msgType));
		rc.moveToNextOpr(operationId, initiator);
	}

	public boolean canITakeCheckpoint(int src, int dest, Integer llr, Integer[] fls) {
		// checkpoint condition
		logger.info("checkpoint condition!!!!!!! llr:{} >= fls[{}]:{} >min:{}", llr, src, fls[src], Integer.MIN_VALUE);
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
		if (!client.tentativeCheckpoint && !operationIds.contains(operationId)) {
			synchronized (isRunning) {
				isRunning = Boolean.TRUE;
			}
			client.tentativeCheckpoint = Boolean.TRUE;
			takeCheckpoint(initiator, operationId);
		} else {
			logger.debug("NodeId:{} has already taken a tentative checkpoint", initiator);
		}
	}

	@Override
	public void setRequestingCandidateHandler(RequestingCandidate rc) {
		this.rc = rc;
	}
}
