package com.aos.lab3;

public interface IQuorumRequestHandler {

	public void handleYieldMessage(Integer sourceNodeId);

	public void handleReleaseMessage(Integer sourceNodeId);

	public void handleRequestMessage(CSRequest request);

	public void setClientHandler(Client client);

	public boolean checkRequestingQueue();

}
