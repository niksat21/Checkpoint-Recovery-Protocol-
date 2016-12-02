package com.aos.lab3;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RecoveryRequestHandler implements IRecoveryRequestHandler {

	private static final Logger logger = LogManager.getLogger(RecoveryRequestHandler.class);

	private Client client;
	private Boolean isRunning = Boolean.FALSE;
	private IApplicationStateHandler appStateHandler;
	private Set<Integer> waitingSet = new HashSet<Integer>();
	private Set<Integer> cohorts;
	private Config config;
	private Integer nodeId;
	private Set<String> operationIds = new HashSet<String>();
	private List<Message> operationQueue;
	private RequestingCandidate rc;
	private Set<String> completedId = new HashSet<String>();

	public RecoveryRequestHandler(Client client, Config config, Integer src, IApplicationStateHandler appStateHandler,
			List<Message> operationQueue) {
		this.client = client;
		this.appStateHandler = appStateHandler;
		this.cohorts = config.getNodeIdVsNeighbors().get(src);
		this.config = config;
		this.nodeId = src;
		this.operationQueue = operationQueue;
	}

	@Override
	public void handleRecoveryMessage(Message msg, Integer[] llr, String operationId) throws InterruptedException {
		Integer src = msg.getSource();
		Integer dest = msg.getDestination();
		Integer lls = msg.getValue();

		logger.debug("Received recovery message from nodeId:{} in nodeId:{} with LLS:{} current LLR:{} operationId:{}",
				src, dest, lls, llr, operationId);
		synchronized (client) {
			if (!client.recover && !operationIds.contains(operationId)) {
				client.recover = shouldIRollback(src, dest, lls, llr);

				if (client.recover) {
					// revert to old state
					resetClientVectorsToLastCheckpointedVal();
					doRollback(msg.getInitiator(), src, operationId);
				} else {
					logger.debug("NodeId:{} is already in recovery mode", nodeId);
					sendAckRecovery(msg.getInitiator(), nodeId, src, operationId);
				}

			} else if (client.recover && !operationIds.contains(operationId)) {
				synchronized (operationQueue) {
					operationQueue.add(msg);
				}
				logger.debug("Queued msg type: {} from nodeId:{} at nodeId:{} by initiator:{}", msg.getMsgType(),
						msg.getSource(), msg.getDestination(), msg.getInitiator());

			} else if (operationId.contains(operationId)) {
				logger.debug(
						"Operation set in nodeId:{} already contains operationId:{}. Sending ACK to nodeId:{} initiator:{}",
						nodeId, operationId, src, msg.getInitiator());
				sendAckRecovery(msg.getInitiator(), nodeId, src, operationId);
			}
		}
	}

	private void resetClientVectorsToLastCheckpointedVal() {
		List<Integer[]> llrList = appStateHandler.getLLR();
		List<Integer[]> llsList = appStateHandler.getLLS();
		List<Integer[]> flsList = appStateHandler.getFLS();
		List<Integer> appValues = appStateHandler.getAppValues();
		Integer[] llr = llrList.get(llrList.size() - 1);
		Integer[] lls = llsList.get(llsList.size() - 1);
		Integer[] fls = flsList.get(flsList.size() - 1);
		Integer appCounter = appValues.get(appValues.size() - 1);
		client.setLlr(llr);
		client.setLls(lls);
		client.setFls(fls);
		client.setAppCounter(appCounter);
		logger.debug("Resetting vector values in nodeId:{} to LLR:{} LLS:{} FLS:{} AppCounter:{}", nodeId, llr, lls,
				fls, appCounter);
	}

	private void initLLR() {
		List<Integer[]> LLR = appStateHandler.getLLR();
		Integer[] array = LLR.get(LLR.size() - 1);
		logger.debug("LLR values in nodeId:{} before reset {}", nodeId, LLR);
		for (int i = 0; i < LLR.size(); i++) {
			array[i] = Integer.MIN_VALUE;
		}
		client.setLlr(array);
		logger.debug("Initialized LLR in nodeId:{} to {}", nodeId, array);
	}

	private void sendAckRecovery(Integer initiator, Integer src, Integer dest, String operationId) {
		logger.debug("Sending recovery ACK message to nodeId:{} from nodeId:{}", src, dest);
		client.sendMsg(new Message(initiator, src, dest, MessageType.ACKRECOVERY));
		rc.moveToNextOpr(operationId, dest);
	}

	private boolean shouldIRollback(int src, int dest, Integer lls, Integer[] llr) {
		logger.info("RECOVERY condition!!!!!!! llr[{}]:{} > lls:{} ", src, llr[src], lls);
		if (llr[src] > lls)
			return true;
		else
			return false;
	}

	@Override
	public void broadcast(Config config, Integer initiator, Integer src, MessageType msgType, String operationId) {
		Integer dest;
		Iterator<Integer> itr = cohorts.iterator();
		logger.debug("Broadcasting {} message from nodeId:{}", msgType.toString(), nodeId);
		while (itr.hasNext()) {
			dest = itr.next();
			List<Integer[]> llsList = appStateHandler.getLLS();
			Message msg = new Message(nodeId, src, dest, llsList.get(llsList.size() - 1)[dest], msgType, operationId);
			logger.debug("Sending {} message to nodeId:{} from nodeId:{}", msgType.toString(), dest, nodeId);
			client.sendMsg(msg);
			if (msgType.equals(MessageType.RECOVERY)) {
				synchronized (waitingSet) {
					waitingSet.add(dest);
					logger.debug("Waiting set of nodeId:{} is {}", nodeId, waitingSet);
				}
			}
		}
	}

	private void doRollback(Integer initiator, Integer src, String operationId) throws InterruptedException {
		if (client.recover) {
			operationIds.add(operationId);
			initLLR();
			broadcast(config, initiator, src, MessageType.RECOVERY, operationId);
			while (true) {
				synchronized (waitingSet) {
					if (waitingSet.isEmpty())
						break;
				}
				Thread.sleep(200);
			}
		}
		sendAckRecovery(initiator, nodeId, src, operationId);
		synchronized (isRunning) {
			isRunning = Boolean.FALSE;
			client.recover = Boolean.FALSE;
		}
		logger.info("Recovery completed at nodeId:{} initiated by nodeId:{} ", nodeId, initiator);
	}

	@Override
	public void revert() {
		// revert vectors to old state

	}

	@Override
	public boolean isRunning() {
		synchronized (isRunning) {
			return isRunning;
		}
	}

	@Override
	public void requestRecovery(String operationId) throws InterruptedException {
		synchronized (client) {
			if (!client.recover && !operationIds.contains(operationId)) {
				synchronized (isRunning) {
					isRunning = Boolean.TRUE;
				}
				client.recover = Boolean.TRUE;
				resetClientVectorsToLastCheckpointedVal();
				doRollback(nodeId, nodeId, operationId);
			} else {
				logger.debug("Something wrong in nodeId:{}", nodeId);
			}
		}
	}

	@Override
	public void handleAckRcvMessage(Integer source, Integer destination) {
		logger.debug("Received ACK for recovery msg in nodeId:{} from nodeId:{}", destination, source);
		synchronized (waitingSet) {
			waitingSet.remove(source);
			logger.debug("Waiting set in nodeId:{} is {}", nodeId, waitingSet);
		}
	}

	@Override
	public void setRequestingCandidateHandler(RequestingCandidate rc) {
		this.rc = rc;
	}

	private void broadcastOpCompleteMsg(Integer initiator, String operationId) {
		for (Integer dest : cohorts) {
			logger.debug("Sending Operation completed msg for operationId:{} from nodeId:{} to nodeId:{}", operationId,
					nodeId, dest);
			Message msg = new Message(initiator, nodeId, dest, MessageType.RECOVERY_COMPLETED);
			msg.setOperationId(operationId);
			client.sendMsg(msg);
		}
		rc.moveToNextOpr(operationId, nodeId);
	}

	@Override
	public synchronized void handleRecoveryCompletionMsg(Integer initiator, Integer source, String operationId)
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
}
