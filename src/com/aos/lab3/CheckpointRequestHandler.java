package com.aos.lab3;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CheckpointRequestHandler implements ICheckpointRequestHandler {

	private Client client;
	private Boolean isRunning;
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
			client.sendMsg(msg);
			synchronized (waitingSet) {
				waitingSet.add(dest);
			}
		}
	}

	@Override
	public void handleCheckpointMessage(int src, int dest, Integer llr, Integer[] fls, String operationId)
			throws InterruptedException {
		if (!client.tentativeCheckpoint) {
			synchronized (isRunning) {
				isRunning = Boolean.TRUE;
			}
			client.tentativeCheckpoint = canITakeCheckpoint(src, dest, llr, fls);
			if (client.tentativeCheckpoint)
				takeCheckpoint(src, operationId);
		}
		sendAck(dest, src, MessageType.ACKCHECKPOINT);
	}

	private void takeCheckpoint(Integer src, String operationId) throws InterruptedException {
		if (client.tentativeCheckpoint) {
			saveState();
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
		appStateHandler.getAppValues().add(client.getAppCounter());
		appStateHandler.getLLR().add(client.getLlr());
		appStateHandler.getLLS().add(client.getLls());
		appStateHandler.getFLS().add(client.getFls());
	}

	private void sendAck(int src, int dest, MessageType msgType) {
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
		synchronized (waitingSet) {
			waitingSet.remove(source);
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
		if (!client.tentativeCheckpoint) {
			synchronized (isRunning) {
				isRunning = Boolean.TRUE;
			}
			client.tentativeCheckpoint = Boolean.TRUE;
			takeCheckpoint(initiator, operationId);
		}
	}
}
