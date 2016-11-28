package com.aos.lab3;

public interface IRecoveryRequestHandler {

	public void handleRecoveryMessage(int src, int dest, Integer llr, Integer[] lls, String operationId) throws InterruptedException;

	public void broadcastRollback(Config config, Integer nodeId, MessageType msgType, String operationId);

	public void revert();

	public void requestRecovery(String operationId) throws InterruptedException;

	boolean isRunning();

	public void handleAckRcvMessage(Integer source, Integer destination);

}
