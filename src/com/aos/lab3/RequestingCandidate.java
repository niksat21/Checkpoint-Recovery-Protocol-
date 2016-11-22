package com.aos.lab3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;

public class RequestingCandidate {

	private static final Logger logger = LogManager.getLogger(RequestingCandidate.class);

	private Config config;
	private Integer nodeId;
	private Client client;
	private IRecoveryRequestHandler iRecovereHandler;
	private ICheckpointRequestHandler iCheckpointHandler;
	
	public RequestingCandidate(Config config, Integer nodeId, Client client, ICheckpointRequestHandler iCheckpointHandler) {
		this.config = config;
		this.nodeId = nodeId;
		this.client = client;
		this.iCheckpointHandler = iCheckpointHandler;
	}

	public RequestingCandidate(Config config2, Integer nodeId2, Client client2, IRecoveryRequestHandler iRecovereHandler) {
		// TODO Auto-generated constructor stub
		this.config = config;
		this.nodeId = nodeId;
		this.client = client;
		this.iRecovereHandler = iRecovereHandler;
	}

	public void requestCheckpoint() throws InterruptedException {
//
//		while(true){
//			Thread.sleep(500);
//			//checking if all the queues are empty
//			if(quroumRequestHandler.checkRequestingQueue() && csHandler.checkSets() ){
//				//start broadcasting complete message
//				ServerWorker.isCompleted = Boolean.TRUE;
//				client.broadcastCompletionMsg();
//				break;
//			}
//		}

		int appCount=0,ctrlCount=0;
		long timeToSend=0;


		while(ctrlCount<=config.getNoOfOperations() && ctrlCount<=config.getNoOfMsgs()){
			timeToSend = System.currentTimeMillis();
			if(!isOpearationRunning){									//if no operation is currently running in system then only send app msgs
				while(timeToSend<=(timeToSend+getExpoRandom(config.getMinInstanceDelay()))){
					appCount++;
					Thread.sleep(config.getMinSendDelay());				//sleep before seding application msg

					//send application message

					timeToSend=System.currentTimeMillis();
				}
			}
			Iterator<Operation> it = config.getOperationsList().iterator();
			if(nodeId==it.node){
				//send checkpoint / rec req and carry out whole check/recov procedure

 			}
 			else{
				while (true){
					Thread.sleep(10);
					if(//get opComplete msg from initiator)
						break;
				}
				it.next();
			}

		}
		while(ctrlCount<=config.getNoOfMsgs()){
				//just send application msgs
		}
		while(ctrlCount<=config.getNoOfOperations()){
			//perform operations with some instance delay
		}

	}

	private static int getExpoRandom(int mean) {

		double temp = Math.random();
		double exp = -(Math.log(temp) * mean);

		return (int) exp;

	}

}