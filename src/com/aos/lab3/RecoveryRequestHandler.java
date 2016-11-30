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
	private Integer initiator;

	public RecoveryRequestHandler(Client client, Config config, Integer src, IApplicationStateHandler appStateHandler) {
		this.client = client;
		this.appStateHandler = appStateHandler;
		this.cohorts = config.getNodeIdVsNeighbors().get(src);
		this.config = config;
		this.initiator = src;
	}

	@Override
	public void handleRecoveryMessage(int src, int dest, Integer lls, Integer[] llr, String operationId)
			throws InterruptedException {
		logger.debug("Received recovery message from nodeId:{} in nodeId:{} with LLS:{} current LLR:{} operationId:{}",
				src, dest, lls, llr, operationId);
		if (!client.recover) {
			client.recover = shouldIRollback(src, dest, lls, llr);

			if (client.recover) {
				// revert to old state
				resetClientVectorsToLastCheckpointedVal();
				doRollback(src, operationId);
			} else {
				logger.debug("NodeId:{} is already in recovery mode", initiator);
			}
		}
		sendAckRecovery(src, dest);
		synchronized (isRunning) {
			isRunning = Boolean.FALSE;
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
		logger.debug("Resetting vector values in nodeId:{} to LLR:{} LLS:{} FLS:{} AppCounter:{}", initiator, llr, lls,
				fls, appCounter);
	}

	private void initLLR() {
		List<Integer[]> LLR = appStateHandler.getLLR();
		Integer[] array = LLR.get(LLR.size() - 1);
		logger.debug("LLR values in nodeId:{} before reset {}", initiator, LLR);
		for (int i = 0; i < LLR.size(); i++) {
			array[i] = Integer.MIN_VALUE;
		}
		client.setLlr(array);
		logger.debug("Initialized LLR in nodeId:{} to {}", initiator, array);
	}

	private void sendAckRecovery(int src, int dest) {
		client.sendMsg(new Message(dest, src, MessageType.ACKRECOVERY));
	}

	private boolean shouldIRollback(int src, int dest, Integer lls, Integer[] llr) {

		if (llr[src] > lls)
			return true;
		else
			return false;
	}

	@Override
	public void broadcastRollback(Config config, Integer nodeId, MessageType msgType, String operationId) {
		int dest;
		Iterator<Integer> itr = cohorts.iterator();
		logger.debug("Broadcasting rollback message from nodeId:{}", initiator);
		while (itr.hasNext()) {
			dest = itr.next();
			List<Integer[]> llsList = appStateHandler.getLLS();
			Message msg = new Message(initiator, nodeId, dest, llsList.get(llsList.size() - 1)[dest], msgType,
					operationId);
			logger.debug("Sending rollback message to nodeId:{} from nodeId:{}", dest, initiator);
			client.sendMsg(msg);
			synchronized (waitingSet) {
				waitingSet.add(dest);
				logger.debug("Waiting set of nodeId:{} is {}", initiator, waitingSet);
			}
		}
	}

	private void doRollback(Integer src, String operationId) throws InterruptedException {
		if (client.recover) {
			initLLR();
			broadcastRollback(config, initiator, MessageType.RECOVERY, operationId);
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
		if (!client.recover) {
			synchronized (isRunning) {
				isRunning = Boolean.TRUE;
			}
			client.recover = Boolean.TRUE;
			resetClientVectorsToLastCheckpointedVal();
			doRollback(initiator, operationId);
		}
	}

	@Override
	public void handleAckRcvMessage(Integer source, Integer destination) {
		logger.debug("Received ACK for recovery msg in nodeId:{} from nodeId:{}", destination, source);
		synchronized (waitingSet) {
			waitingSet.remove(source);
			logger.debug("Waiting set in nodeId:{} is {}", initiator, waitingSet);
		}
	}

}
