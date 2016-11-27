package com.aos.lab3;

import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RequestingCandidate {

	private static final Logger logger = LogManager.getLogger(RequestingCandidate.class);

	private Config config;
	private Integer nodeId;
	private Client client;
	private IRecoveryRequestHandler recoveryHandler;
	private ICheckpointRequestHandler checkpointHandler;
	private Integer counter = 0;

	RequestingCandidate(Config config, Integer nodeId, Client client, ICheckpointRequestHandler checkpointHandler,
			IRecoveryRequestHandler recoveryHandler) {
		this.config = config;
		this.nodeId = nodeId;
		this.client = client;
		this.checkpointHandler = checkpointHandler;
		this.recoveryHandler = recoveryHandler;
	}

	public void start() throws InterruptedException {
		int appCount = 1, ctrlCount = 1;
		long timeToSend = 0;

		while (ctrlCount <= config.getNoOfOperations() && appCount <= config.getNoOfMsgs()) {
			timeToSend = System.currentTimeMillis();

			// TODO: set the flag in server worker to notify operation is not
			// running in system
			if (!checkpointHandler.isRunning() && !recoveryHandler.isRunning()) {
				// if no operation is currently running in
				// system then only send app msgs

				Set<Integer> cohorts = config.getNodeIdVsNeighbors().get(nodeId);
				Iterator<Integer> it = cohorts.iterator();
				for (int i = 0; i < cohorts.size()
						&& timeToSend <= (timeToSend + getExpoRandom(config.getMinInstanceDelay())); i++) {
					Thread.sleep(config.getMinSendDelay()); // sleep before
															// sending
					Message msg = new Message(nodeId, it.next(), MessageType.APPLICATION);
					client.sendMsg(msg);
					counter++;
					appCount++;
					timeToSend = System.currentTimeMillis();
				}

				Iterator<Operation> it1 = config.getOperationsList().iterator();
				Operation opr = it1.next();
				if (nodeId.equals(opr.getNodeId())) {
					if (opr.getType().equals(OperationType.CHECKPOINT)) {
						checkpointHandler.requestCheckpoint(counter);
						ctrlCount++;
					} else if (opr.getType().equals(OperationType.RECOVERY)) {
						recoveryHandler.requestRecovery();
						ctrlCount++;
					} else {
						logger.error("Unsupported operation type: {}", opr.toString());
					}

				}

			} else {
				Thread.sleep(200);
			}
		}
	}

	private static int getExpoRandom(int mean) {

		double temp = Math.random();
		double exp = -(Math.log(temp) * mean);

		return (int) exp;
	}
}