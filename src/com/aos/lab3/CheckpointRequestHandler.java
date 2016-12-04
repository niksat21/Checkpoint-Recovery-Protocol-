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
	private Integer nodeId;
	private IApplicationStateHandler appStateHandler;

	private Set<Integer> waitingSet = new HashSet<Integer>();
	private Set<String> operationIds = new HashSet<String>();
	private List<Message> operationQueue;
	private RequestingCandidate rc;
	private Set<String> completedId = new HashSet<String>();

	CheckpointRequestHandler(Client client, Config config, Integer nodeId, IApplicationStateHandler appStateHandler,
			List<Message> operationQueue) {
		this.client = client;
		this.config = config;
		this.cohorts = config.getNodeIdVsNeighbors().get(nodeId);
		this.nodeId = nodeId;
		this.appStateHandler = appStateHandler;
		this.operationQueue = operationQueue;
	}

	@Override
	public void broadcast(Config config, Integer initiator, Integer src, MessageType msgType, String operationId) {
		Integer dest;
		Iterator<Integer> itr = cohorts.iterator();
		while (itr.hasNext()) {
			dest = itr.next();
			Message msg = new Message(initiator, nodeId, dest, client.getLlr()[dest], msgType, operationId);
			logger.debug("Sending {} message to nodeId:{} from nodeId:{}", msgType.toString(), dest, nodeId);
			client.sendMsg(msg);
			if (msgType.equals(MessageType.CHECKPOINT)) {
				synchronized (waitingSet) {
					waitingSet.add(dest);
					logger.debug("Waiting set in nodeId:{} is {}", nodeId, waitingSet);
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
			Boolean checkpointNeeded = canITakeCheckpoint(src, dest, llr, fls);
			logger.info("Node :{} can take CHECKPOINT??:{}", nodeId, checkpointNeeded);
			if (checkpointNeeded) {
				synchronized (isRunning) {
					synchronized (client) {
						isRunning = Boolean.TRUE;
						logger.info("isRunning TRUE HANDLECHECKPOINT! at:{} inside takeCheckpoint with operationId:{}",
								nodeId, operationId);
						client.tentativeCheckpoint = Boolean.TRUE;
					}
				}
				takeCheckpoint(msg.getInitiator(), msg.getSource(), operationId);
			} else {
				logger.debug("NodeId:{} does not need to checkpoint because of initator {}", nodeId,
						msg.getInitiator());
				sendAck(msg.getInitiator(), nodeId, src, MessageType.ACKCHECKPOINT, operationId);
			}
		} else if (client.tentativeCheckpoint && !operationIds.contains(operationId)) {
			synchronized (operationQueue) {
				operationQueue.add(msg);
			}
			logger.debug("Queued msg type: {} from nodeId:{} at nodeId:{} by initiator:{}", msg.getMsgType(),
					msg.getSource(), msg.getDestination(), msg.getInitiator());
		} else if (operationId.contains(operationId)) {
			logger.debug(
					"Operation set in nodeId:{} already contains operationId:{}. Sending ACK to nodeId:{} initiator:{}",
					nodeId, operationId, src, msg.getInitiator());
			sendAck(msg.getInitiator(), nodeId, src, MessageType.ACKCHECKPOINT, operationId);
		}
	}

	private void takeCheckpoint(Integer initiator, Integer src, String operationId) throws InterruptedException {
		if (client.tentativeCheckpoint) {
			operationIds.add(operationId);
			saveState(operationId);
			logger.info("SAVED STATED! at:{} inside takeCheckpoint with operationId:{}", nodeId, operationId);
			logger.debug("Operation completed in NodeId:{} OperationId:{} FLS:{} LLR:{} LLS:{} Neighbors:{}", nodeId,
					operationId, appStateHandler.getFLS(), appStateHandler.getLLR(), appStateHandler.getLLS(), cohorts);
			client.initVectors();
			broadcast(config, initiator, src, MessageType.CHECKPOINT, operationId);
			while (true) {
				synchronized (waitingSet) {
					if (waitingSet.isEmpty())
						break;
				}
				Thread.sleep(200);
			}
			logger.info("Checkpointing completed at nodeId:{} initiated by nodeId:{}", nodeId, initiator);
			synchronized (isRunning) {
				synchronized (client) {
					isRunning = Boolean.FALSE;
					client.tentativeCheckpoint = Boolean.FALSE;
				}
			}
			sendAck(initiator, nodeId, src, MessageType.ACKCHECKPOINT, operationId);
		} else {
			logger.debug("Something wrong in nodeId:{} ", initiator);
		}
	}

	private void saveState(String operationId) {
		synchronized (appStateHandler) {
			logger.debug("Saving vector values in nodeId:{}. LLR:{} LLS:{} FLS:{}", nodeId, client.getLlr(),
					client.getLls(), client.getFls());
			appStateHandler.getAppValues().add(client.getAppCounter());
			appStateHandler.getLLR().add(client.getLlr());
			appStateHandler.getLLS().add(client.getLls());
			appStateHandler.getFLS().add(client.getFls());
			logger.info("SAVED STATE! at:{}", client.getNodeId());
			logger.debug("Operation completed in NodeId:{} OperationId:{} FLS:{} LLR:{} LLS:{} Neighbors:{}", nodeId,
					operationId, appStateHandler.getFLS(), appStateHandler.getLLR(), appStateHandler.getLLS(), cohorts);
		}
	}

	private void sendAck(Integer initiator, Integer src, Integer dest, MessageType msgType, String operationId) {
		logger.info("SENDACK! at:{} to:{}", nodeId, dest);
		this.client.sendMsg(new Message(initiator, src, dest, msgType));
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
			logger.debug("Waiting set in nodeId:{} is {}", nodeId, waitingSet);
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
				synchronized (client) {
					isRunning = Boolean.TRUE;
					client.tentativeCheckpoint = Boolean.TRUE;
				}
			}
			takeCheckpoint(nodeId, nodeId, operationId);
			broadcastOpCompleteMsg(nodeId, operationId);
		} else {
			logger.debug("NodeId:{} has already taken a tentative checkpoint", nodeId);
		}
	}

	private void broadcastOpCompleteMsg(Integer initiator, String operationId) {
		for (Integer dest : cohorts) {
			logger.debug("Sending Operation completed msg for operationId:{} from nodeId:{} to nodeId:{}", operationId,
					nodeId, dest);
			Message msg = new Message(initiator, nodeId, dest, MessageType.CHECKPOINT_COMPLETED);
			msg.setOperationId(operationId);
			client.sendMsg(msg);
		}
		rc.moveToNextOpr(operationId, nodeId);
	}

	@Override
	public synchronized void handleCheckpointCompletionMsg(Integer initiator, Integer source, String operationId)
			throws InterruptedException {
		logger.debug("Received completed msg in nodeId:{} from nodeId:{} for operationId:{}", nodeId, source,
				operationId);
		while (true) {
			if (!client.tentativeCheckpoint)
				break;
			else
				Thread.sleep(200);
		}
		if (!completedId.contains(operationId)) {
			broadcastOpCompleteMsg(initiator, operationId);
			completedId.add(operationId);
		} else {
			logger.debug("NodeId:{} already broadcasted completed msg for operationId:{} to its cohorts", nodeId,
					operationId);
		}
	}

	@Override
	public void setRequestingCandidateHandler(RequestingCandidate rc) {
		this.rc = rc;
	}
}
