package com.aos.lab3;

import java.util.Iterator;
import java.util.Set;

public class CheckpointRequestHandler implements ICheckpointRequestHandler {

	private Client client;
	private Boolean isRunning;

	CheckpointRequestHandler(Client client) {
		this.client = client;
	}

	@Override
	public void spreadTheWord(Config config, Integer nodeId) {
		// TODO Auto-generated method stub
		Set<Integer> myBuddies = config.getNodeIdVsNeighbors().get(nodeId);
		int dest;
		Iterator<Integer> itr = myBuddies.iterator();

		dest = itr.next();
		while (itr.hasNext()) {
			Message msg = new Message(nodeId, dest, client.llr[dest], MessageType.CHECKPOINT);
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
			sendCheckpointAck(src, dest);
		} else {
			// already have taken a checkpoint

		}
	}

	private void sendCheckpointAck(int src, int dest) {
		// TODO Auto-generated method stub
		client.sendMsg(new Message(dest, src, MessageType.ACKCHECKPOINT));
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

}
