package com.aos.lab3;

public interface IRecoveryRequestHandler {

	public void handleRecoveryMessage(Message msg, Integer[] lls, String operationId) throws InterruptedException;

	public void broadcast(Config config, Integer initiator, Integer src, MessageType msgType, String operationId);

	public void revert();

	public void requestRecovery(String operationId) throws InterruptedException;

	boolean isRunning();

	public void handleAckRcvMessage(Integer source, Integer destination);

	public void setRequestingCandidateHandler(RequestingCandidate rc);

}
