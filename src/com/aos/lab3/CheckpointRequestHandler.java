package com.aos.lab3;

import java.util.Iterator;
import java.util.Set;

public class CheckpointRequestHandler implements ICheckpointRequestHandler {

	private Client client;
	private Boolean isRunning;

	CheckpointRequestHandler(Client client) {
		this.client = client;

		Set<Integer> myBuddies = config.getNodeIdVsNeighbors().get(nodeId);
	}

	@Override
	public void spreadTheWord(Config config, Integer nodeId, MessageType msgType) {
		// TODO Auto-generated method stub
		int dest;
		Iterator<Integer> itr = myBuddies.iterator();

		dest = itr.next();
		while (itr.hasNext()) {
			Message msg = new Message(nodeId, dest, client.llr[dest], msgType);
			client.sendMsg(msg);
		}
	}

	@Override
	public void handleCheckpointMessage(int src, int dest, Integer llr, Integer[] fls) {
		// TODO Auto-generated method stub
		if (!client.tentativeCheckpoint) {
			client.tentativeCheckpoint = canITakeCheckpoint(src, dest, llr, fls);

			if (client.tentativeCheckpoint) {
				// save state
				client.initVectors();
			} else {
				// dont save

			}
			spreadTheWord(src, dest, MessageType.ACKCHECKPOINT);
		} else {
			// already have taken a checkpoint

		}
	}

	public boolean canITakeCheckpoint(int src, int dest, Integer llr, Integer[] fls) {

		// checkpoint condition
		if ((llr >= fls[dest]) && (fls[dest] > Integer.MIN_VALUE))
			return true;
		else
			return false;
	}

	@Override
	public void handleAckChpMessage(Integer source, Integer destination) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isRunning() {
		synchronized (isRunning) {
			return isRunning;
		}
	}

	@Override
	public void requestCheckpoint(Integer source, Integer counter) {
		// TODO Auto-generated method stub

	}

}
