package com.aos.lab3;

public interface ICheckpointRequestHandler {

	void broadcast(Config config, Integer nodeId, MessageType msgType, String operationId);

	void handleCheckpointMessage(int src, int dest, Integer llr, Integer[] fls, String operationId)
			throws InterruptedException;

	void handleAckChpMessage(Integer source, Integer destination);

	void requestCheckpoint(Integer counter, String operationId) throws InterruptedException;

	boolean isRunning();

}
