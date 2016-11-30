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
	private Integer opCount = 0;
	private Iterator<Operation> oprIterator;
	private volatile Boolean flag = Boolean.FALSE;

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

		oprIterator = config.getOperationsList().iterator();
		Operation opr = oprIterator.next();
		opCount++;
		while (ctrlCount <= config.getNoOfOperations() && appCount <= config.getNoOfMsgs()) {
			timeToSend = System.currentTimeMillis();

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
					logger.trace("Operation:{} at nodeId:{} with counter:{} and appCount:{}", i, nodeId, counter,
							appCount);
				}

				logger.debug("Operation Type:{} nodeId:{} in nodeId:{}", opr.toString(), opr.getNodeId(), nodeId);
				if (nodeId.equals(opr.getNodeId())) {
					if (opr.getType().equals(OperationType.CHECKPOINT)) {
						logger.debug("NodeId:{} initiated CHECKPOINT req with ctrlCount{}", nodeId, ctrlCount);
						checkpointHandler.requestCheckpoint(counter, "C-" + nodeId + "-" + opCount);
						ctrlCount++;
					} else if (opr.getType().equals(OperationType.RECOVERY)) {
						logger.debug("NodeId:{} initiated RECOVERY req with ctrlCount{}", nodeId, ctrlCount);
						recoveryHandler.requestRecovery("R-" + nodeId + "-" + opCount);
						ctrlCount++;
					} else {
						logger.error("Unsupported operation type: {}", opr.toString());
					}
				}
				while (true) {
					Thread.sleep(200);
					if (flag.booleanValue() == true) {
						flag = Boolean.FALSE;
						break;
					}
				}

			} else {
				Thread.sleep(200);
			}
		}
	}

	public synchronized void moveToNextOpr() {
		logger.debug("Moving to the next operation in nodeId:{}", nodeId);
		oprIterator.next();
		opCount++;
		flag = Boolean.TRUE;
	}

	private static int getExpoRandom(int mean) {

		double temp = Math.random();
		double exp = -(Math.log(temp) * mean);

		return (int) exp;
	}
}