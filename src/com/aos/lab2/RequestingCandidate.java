package com.aos.lab2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RequestingCandidate {

	private static final Logger logger = LogManager.getLogger(RequestingCandidate.class);

	private Config config;
	private Integer nodeId;
	private Client client;
	private ICriticalSectionHandler csHandler;
	private IQuorumRequestHandler quroumRequestHandler;
	
	public RequestingCandidate(Config config, Integer nodeId, Client client, ICriticalSectionHandler csHandler, IQuorumRequestHandler quroumRequestHandler) {
		this.config = config;
		this.nodeId = nodeId;
		this.client = client;
		this.csHandler = csHandler;
		this.quroumRequestHandler = quroumRequestHandler;
	}

	public void requestCS() throws InterruptedException {
		int count = 0;
		int noOfRequests = config.getNoOfAttempts();
		Node node = config.getNodeById(nodeId);
		// sleep for some random time before making request for CS
		Thread.sleep(getExpoRandom(config.getWaitTime()));

		while (count < noOfRequests) {
			logger.debug("noOfRequests :{} count:{} ", noOfRequests, count);
			long timestamp = System.currentTimeMillis();
			csHandler.csEnter(timestamp);
			logger.info("Critical Section: Enter NodeId:{} Request TS:{}", node.getNodeId(), timestamp);

			// sleep till CS is executed
			Thread.sleep(getExpoRandom(config.getCsExecTime()));
			csHandler.csLeave();
			logger.info("Critical Section: Leave NodeId:{}", node.getNodeId());
			count++;
		}
		while(true){
			Thread.sleep(500);
			//checking if all the queues are empty
			if(quroumRequestHandler.checkRequestingQueue() && csHandler.checkSets() ){
				//start broadcasting complete message
				ServerWorker.isCompleted = Boolean.TRUE;
				client.broadcastCompletionMsg();
				break;
			}
		}
	}

	private static int getExpoRandom(int mean) {

		double temp = Math.random();
		double exp = -(Math.log(temp) * mean);

		return (int) exp;

	}

}