package com.aos.lab3;

import java.util.Iterator;
import java.util.Set;

public class RecoveryRequestHandler implements IRecoveryRequestHandler {

	private Client client;
	private Boolean isRunning;

	public RecoveryRequestHandler(Client client) {
		// TODO Auto-generated constructor stub
		this.client = client;
	}

	@Override
	public void handleRecoveryMessage(int src, int dest, Integer lls, Integer[] llr) {
		// TODO Auto-generated method stub
		if (!client.recover) {
			client.recover = shouldIRollback(src, dest, lls, llr);

			if (client.recover) {
				// revert to old state

			} else {
				// no change in state

			}
			sendAckRecovery(src, dest);
		} else {
			// already have recovered
			// discard this

		}
	}

	private void sendAckRecovery(int src, int dest) {
		// TODO Auto-generated method stub
		client.sendMsg(new Message(dest, src, MessageType.ACKRECOVERY));
	}

	private boolean shouldIRollback(int src, int dest, Integer lls, Integer[] llr) {
		// TODO Auto-generated method stub
		if (llr[src] > lls)
			return true;
		else
			return false;
	}

	@Override
	public void askOthersForRollback(Config config, Integer nodeId) {
		// TODO Auto-generated method stub

		// resetting llr vector on crash
		for (int i = 0; i < client.llr.length; i++)
			client.llr[i] = Integer.MIN_VALUE;

		Set<Integer> myBuddies = config.getNodeIdVsNeighbors().get(nodeId);
		int dest;
		Iterator<Integer> itr = myBuddies.iterator();

		dest = itr.next();
		while (itr.hasNext()) {
			Message msg = new Message(nodeId, dest, client.lls[dest], MessageType.RECOVERY);
			client.sendMsg(msg);
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
	public void requestRecovery(Integer src) {
		// TODO Auto-generated method stub

	}

}
