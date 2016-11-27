package com.aos.lab3;

public interface IRecoveryRequestHandler {

	public void handleRecoveryMessage(int src, int dest, Integer llr, Integer[] lls);

	public void askOthersForRollback(Config config, Integer nodeId);

	public void revert();

}
