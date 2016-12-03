package com.aos.lab3;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RequestingCandidate {

	private final Logger logger = LogManager.getLogger(RequestingCandidate.class);

	private Config config;
	private Integer nodeId;
	private Client client;
	private IRecoveryRequestHandler recoveryHandler;
	private ICheckpointRequestHandler checkpointHandler;
	private Integer counter = 0;
	private Integer opCount = 0;
	private Iterator<Operation> oprIterator;
	private Operation opr = null;
	private List<Message> operationQueue;
	private Integer ctrlCount = new Integer(1);

	private volatile Boolean flag = Boolean.FALSE;

	RequestingCandidate(Config config, Integer nodeId, Client client, ICheckpointRequestHandler checkpointHandler,
			IRecoveryRequestHandler recoveryHandler, List<Message> operationQueue) {
		this.config = config;
		this.nodeId = nodeId;
		this.client = client;
		this.checkpointHandler = checkpointHandler;
		this.recoveryHandler = recoveryHandler;
		this.operationQueue = operationQueue;
	}

	public void start() throws InterruptedException {
		int appCount = 1;
		long timeToSend = 0;
		int dest;
		oprIterator = config.getOperationsList().iterator();
		opr = oprIterator.next();
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
					dest = it.next();
					Message msg = new Message(nodeId, nodeId, new Integer(dest), new Integer(appCount),
							MessageType.APPLICATION, null);
					client.sendMsg(msg);

					// updating fls of current node
					if (client.getFls()[dest] == Integer.MIN_VALUE)
						client.getFls()[dest] = appCount;

					// updating LLS of current node
					client.getLls()[dest] = appCount;

					counter++;
					appCount++;
					timeToSend = System.currentTimeMillis();
					logger.debug("Operation:{} at nodeId:{} with counter:{} and appCount:{}", i, nodeId, counter,
							appCount);
				}

				synchronized (operationQueue) {
					if (!operationQueue.isEmpty()) {
						Message msg = operationQueue.remove(0);
						logger.debug("Removed operation type:{} from the operation queue in nodeId:{} initiator:{}",
								msg.getMsgType(), nodeId, msg.getInitiator());
						if (!msg.getInitiator().equals(opr.getNodeId())) {
							logger.error(
									"Something wrong. Next operation is from nodeId:{} but operation queue initiator nodeId is {}",
									opr.getNodeId(), msg.getInitiator());
						} else {
							if (msg.getMsgType().equals(MessageType.CHECKPOINT)) {
								checkpointHandler.handleCheckpointMessage(msg, client.getFls(), msg.getOperationId());
								continue;
							} else if (msg.getMsgType().equals(MessageType.RECOVERY)) {
								if (!msg.getInitiator().equals(opr.getNodeId())) {
									logger.error(
											"Something wrong. Next operation is from nodeId:{} but operation queue initiator nodeId is {}",
											opr.getNodeId(), msg.getInitiator());
								} else {
									recoveryHandler.handleRecoveryMessage(msg, client.getLls(), msg.getOperationId());
									continue;
								}
							} else {
								logger.error("Unsupported message type {} in the operation queue in nodeId:{}",
										msg.getMsgType(), nodeId);
							}
						}
					}
				}

				logger.debug("Operation Type:{} nodeId:{} in nodeId:{}", opr.toString(), opr.getNodeId(), nodeId);
				if (nodeId.equals(opr.getNodeId())) {
					if (opr.getType().equals(OperationType.CHECKPOINT)) {
						logger.debug("NodeId:{} initiated CHECKPOINT req with ctrlCount{}", nodeId, ctrlCount);
						checkpointHandler.requestCheckpoint(counter, "C-" + nodeId + "-" + ctrlCount);
					} else if (opr.getType().equals(OperationType.RECOVERY)) {
						logger.debug("NodeId:{} initiated RECOVERY req with ctrlCount{}", nodeId, ctrlCount);
						recoveryHandler.requestRecovery("R-" + nodeId + "-" + opCount);
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

		// termination detection initiation
		logger.info("Done with all operations at nodeId:{}!", nodeId);

	}

	public synchronized void moveToNextOpr(String operationId, Integer initiator) {
		String oprId = getOperationId(opr);
		if (oprId.equals(operationId)) {
			logger.debug("Moving to the next operation in nodeId:{}", nodeId);
			if (oprIterator.hasNext()) {
				opr = oprIterator.next();
				opCount++;
				ctrlCount++;
				flag = Boolean.TRUE;
			}
		} else {
			logger.debug("OperationId in the msg:{} did not match the current operationId:{} in nodeId:{}", operationId,
					oprId, nodeId);
		}
	}

	private String getOperationId(Operation opr) {
		StringBuilder builder = new StringBuilder();
		if (opr.getType().equals(OperationType.CHECKPOINT)) {
			builder.append("C-");
		} else if (opr.getType().equals(OperationType.RECOVERY)) {
			builder.append("R-");
		}
		return builder.append(opr.getNodeId()).append("-").append(opCount).toString();
	}

	private static int getExpoRandom(int mean) {

		double temp = Math.random();
		double exp = -(Math.log(temp) * mean);

		return (int) exp;
	}
}