package com.aos.lab3;

public interface ICheckpointRequestHandler {

	void broadcast(Config config, Integer src, Integer initiator, MessageType msgType, String operationId);

	void handleCheckpointMessage(Message msg, Integer[] fls, String operationId) throws InterruptedException;

	void handleAckChpMessage(Integer source, Integer destination);

	void handleCheckpointCompletionMsg(Integer initiator, Integer source, String operationId)
			throws InterruptedException;

	void requestCheckpoint(Integer counter, String operationId) throws InterruptedException;

	boolean isRunning();

	public void setRequestingCandidateHandler(RequestingCandidate rc);

}
