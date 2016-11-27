package com.aos.lab3;

public interface ICheckpointRequestHandler {

	void spreadTheWord(Config config, Integer nodeId, MessageType msgType);

	void handleCheckpointMessage(int src, int dest, Integer llr, Integer[] fls);

	void handleAckChpMessage(Integer source, Integer destination);

	void requestCheckpoint(Integer source);

	boolean isRunning();

}
