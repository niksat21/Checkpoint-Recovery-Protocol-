package com.aos.lab2;

public interface ICriticalSectionHandler {

	public void csEnter(Long timestamp) throws InterruptedException;

	public void csLeave();

	public void handleFailedMessage(Integer nodeId);

	public void handleInquireMessage(Integer nodeId);

	public void handleGrantMessage(Integer nodeId);

	public void setClientHandler(Client client);

	public boolean checkSets();

}
