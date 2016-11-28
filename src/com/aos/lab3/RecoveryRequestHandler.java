package com.aos.lab3;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RecoveryRequestHandler implements IRecoveryRequestHandler {

	private Client client;
	private Boolean isRunning;
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
		// TODO Auto-generated method stub
		if (!client.recover) {
			client.recover = shouldIRollback(src, dest, lls, llr);

			if (client.recover) {
				// revert to old state
				resetClientVectorsToLastCheckpointedVal();
				doRollback(src, operationId);
			} else {
				// no change in state
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
		client.setLlr(llrList.get(llrList.size() - 1));
		client.setLls(llsList.get(llsList.size() - 1));
		client.setFls(flsList.get(flsList.size() - 1));
		client.setAppCounter(appValues.get(appValues.size() - 1));
	}

	private void initLLR() {
		List<Integer[]> LLR = appStateHandler.getLLR();
		Integer[] array = LLR.get(LLR.size() - 1);
		for (int i = 0; i < LLR.size(); i++) {
			array[i] = Integer.MIN_VALUE;
		}
		client.setLlr(array);
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
		while (itr.hasNext()) {
			dest = itr.next();
			List<Integer[]> llsList = appStateHandler.getLLS();
			Message msg = new Message(initiator, nodeId, dest, llsList.get(llsList.size() - 1)[dest], msgType,
					operationId);
			client.sendMsg(msg);
			synchronized (waitingSet) {
				waitingSet.add(dest);
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
		synchronized (waitingSet) {
			waitingSet.remove(source);
		}
	}

}
