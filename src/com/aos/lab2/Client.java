package com.aos.lab2;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class Client implements Runnable {

	private Map<Integer, SocketAddress> nodeVsSocket = new HashMap<Integer, SocketAddress>();
	private Logger logger = LogManager.getLogger(Client.class);
	private String nodeHostname = null;
	private int labelValue;
	private Config config;
	private Integer nodeId;

	public Client(String nodeHostname, int labelValue, Config config, Integer nodeId) {
		this.nodeHostname = nodeHostname;
		this.labelValue = labelValue;
		this.config = config;
		this.nodeId = nodeId;

	}

	// Open connections with every other node
	private void createSockets(List<Node> nodes) {
		for (Node node : nodes) {
			try {
				logger.debug("Trying to create connection with host:{} port:{}", node.getHostname(), node.getPort());
				SocketAddress socketAddress = new InetSocketAddress(node.getHostname(), node.getPort());
				nodeVsSocket.put(node.getNodeId(), socketAddress);
				logger.debug("Successfully created socket connection to host:{} from:{} ", node.getHostname(),
						nodeHostname);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		logger.debug("NodeVsSocketMap:{}", nodeVsSocket);
	}

	public void closeSockets() {
		try {
			for (Entry<Integer, SocketAddress> entry : nodeVsSocket.entrySet()) {
				SocketAddress socketAddress = entry.getValue();
				logger.debug("Closing socket between local host:{} and remote host:{}");
				// socketAddress.close();
			}
		} catch (Exception e) {
			logger.error("Problem with closing the socket.", e);
		}
	}

	@Override
	public void run() {
		try {
			// Sleep for sometime so that the other nodes come up.
			logger.debug("Sleeping for 5 seconds until other nodes come up");
			Thread.sleep(5000);
			createSockets(config.getNodes());
		} catch (Exception e) {
			logger.error("Problem in client thread.", e);
		}
	}

	public void sendMsg(Message msg) {
		SocketAddress socketAddress = nodeVsSocket.get(msg.getDestination());
		
		logger.debug("Destination socket here is:{} ::{}",msg.getDestination(),socketAddress);
		while (true) {
			try {
				SctpChannel sctpChannel = SctpChannel.open();
				sctpChannel.connect(socketAddress);

				MessageInfo messageInfo = MessageInfo.createOutgoing(socketAddress, 0);
				ByteBuffer buf = ByteBuffer.allocateDirect(500000);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos);
				oos.writeObject(msg);
				oos.flush();
				buf.put(bos.toByteArray());
				buf.flip();
				sctpChannel.send(buf, messageInfo);

				bos.close();
				buf.clear();
				return;
			} catch (Exception e) {
				logger.warn("Exception in Send()" + e);
				e.printStackTrace();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
	
		}
	}

	public void broadcastCompletionMsg() {
		List<Node> nodeList = config.getNodes();
		for (Node node : nodeList) {
			// Ignore sending the completion message to itself
			if (node.getNodeId().equals(nodeId))
				continue;
			Message msg = new Message(nodeId, node.getNodeId(), null, 0, MessageType.COMPLETED);
			sendMsg(msg);
		}

	}

}
