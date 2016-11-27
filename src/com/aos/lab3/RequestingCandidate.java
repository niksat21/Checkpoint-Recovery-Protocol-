package com.aos.lab3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Set;

public class RequestingCandidate {

	private static final Logger logger = LogManager.getLogger(RequestingCandidate.class);

	private Config config;
	private Integer nodeId;
	private Client client;
	private IRecoveryRequestHandler iRecovereHandler;
	private ICheckpointRequestHandler iCheckpointHandler;
	private Integer initiator;
	
	RequestingCandidate(Config config, Integer nodeId, Client client, ICheckpointRequestHandler iCheckpointHandler, Integer initiator) {
		this.config = config;
		this.nodeId = nodeId;
		this.client = client;
		this.iCheckpointHandler = iCheckpointHandler;
		this.initiator = initiator;
	}

	RequestingCandidate(Config config2, Integer nodeId2, Client client2, IRecoveryRequestHandler iRecovereHandler, Integer initiator) {
		// TODO Auto-generated constructor stub
		this.config = config2;
		this.nodeId = nodeId2;
		this.client = client2;
		this.iRecovereHandler = iRecovereHandler;
		this.initiator = initiator;
	}

	public void requestCheckpoint() throws InterruptedException {

		

		//commenting Nikhil's code for now
		/*int appCount=0,ctrlCount=0;
		long timeToSend=0;


		while(ctrlCount<=config.getNoOfOperations() && ctrlCount<=config.getNoOfMsgs()){
			timeToSend = System.currentTimeMillis();
			if(!isOpearationRunning){									//if no operation is currently running in system then only send app msgs
				while(timeToSend<=(timeToSend+getExpoRandom(config.getMinInstanceDelay()))){
					appCount++;
					Thread.sleep(config.getMinSendDelay());				//sleep before sending application msg

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
		}*/
		
		//lets send checkpoint request to all and setting myself as true		
		
		//setting this only if its initiator
		if(this.nodeId == this.initiator)	
			client.tentativeCheckpoint = true;
		
		iCheckpointHandler.spreadTheWord(config, nodeId, MessageType.CHECKPOINT);
		

	}
	
	public void requestRecovery(){
		
		//reverting to old state
		iRecovereHandler.revert();
		iRecovereHandler.askOthersForRollback(config, nodeId);
		
	}

//Commenting Nikhil's code for now
/*	private static int getExpoRandom(int mean) {

		double temp = Math.random();
		double exp = -(Math.log(temp) * mean);

		return (int) exp;

	}*/
	

}