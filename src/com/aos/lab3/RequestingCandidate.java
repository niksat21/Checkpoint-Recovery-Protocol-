package com.aos.lab3;

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

	public void start() {

		 int appCount=1,ctrlCount=1;
		 long timeToSend=0;
		
		
		 while(ctrlCount<=config.getNoOfOperations() &&
		 appCount<=config.getNoOfMsgs()){
		 timeToSend = System.currentTimeMillis();
		 if(!checkpointHandler.isRunning() && !recoveryHandler.isRunning()){ 
			 //if no operation is currently running in
		 //system then only send app msgs
		 while(timeToSend<=(timeToSend+getExpoRandom(config.getMinInstanceDelay()))){
		 appCount++;
		 Thread.sleep(config.getMinSendDelay()); //sleep before sending
		 Set<Integer> cohorts = config.getNodeIdVsNeighbors().get(nodeId)
		 for(int i=0;i<.size();i++){
			 Message msg = new Message(nodeId, , msgType)
			 client.sendMsg(msg);
		 }
		 application msg
		
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
		 while(appCount<=config.getNoOfMsgs()){
		 //just send application msgs
		 }
		 while(ctrlCount<=config.getNoOfOperations()){
		 //perform operations with some instance delay
		 }
		
		 //lets send checkpoint request to all and setting myself as true
		 client.tentativeCheckpoint = true;
		 iCheckpointHandler.spreadTheWord(config, nodeId);
		

		
	}

	public void requestCheckpoint() throws InterruptedException {

	}

	public void requestRecovery() {
		// reverting to old state
		recoveryHandler.revert();
		recoveryHandler.askOthersForRollback(config, nodeId);

	}

	private static int getExpoRandom(int mean) {
		double temp = Math.random();
		double exp = -(Math.log(temp) * mean);
		return (int) exp;
	}
}