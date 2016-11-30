package com.aos.lab3;

import java.io.PrintStream;
import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.ShutdownNotification;

public class Server implements Runnable {

	private Client client;
	private Logger logger = LogManager.getLogger(Server.class);
	private int port;
	private Integer nodeId;
	private Integer labelValue;
	private Config config;
	private SctpChannel sc;
	private IRecoveryRequestHandler iRecovereHandler;
	private ICheckpointRequestHandler iCheckpointHandler;
	private RequestingCandidate reqCandidate;

	public Server(Integer nodeId, Integer labelValue, Integer port, Config config) {
		this.nodeId = nodeId;
		this.labelValue = labelValue;
		this.port = port;
		this.config = config;
	}

	@Override
	public void run() {
		try {
			listenForConnections();
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public RequestingCandidate getReqCandidate() {
		return reqCandidate;
	}

	public void setReqCandidate(RequestingCandidate reqCandidate) {
		this.reqCandidate = reqCandidate;
	}

	public void setClientHandler(Client client) {
		this.client = client;
	}

	public void setRecovereHandler(IRecoveryRequestHandler iRecovereHandler) {
		this.iRecovereHandler = iRecovereHandler;
	}

	public void setCheckpointHandler(ICheckpointRequestHandler iCheckpointHandler) {
		this.iCheckpointHandler = iCheckpointHandler;
	}

	private void listenForConnections() throws Exception {
		SctpServerChannel ssc = SctpServerChannel.open();
		InetSocketAddress serverAddr = new InetSocketAddress(port);
		ssc.bind(serverAddr);
		AssociationHandler assocHandler = new AssociationHandler();
		try {
			while (true) {

				sc = ssc.accept();
				ServerWorker worker = new ServerWorker(nodeId, sc, client, labelValue, config, assocHandler, ssc,
						iCheckpointHandler, iRecovereHandler, reqCandidate);
				logger.debug("Created server worker");
				Thread workerThread = new Thread(worker);
				logger.debug("Created server worker thread");
				workerThread.start();
			}
		} finally {
			sc.close();
			ssc.close();
		}
	}

	static class AssociationHandler extends AbstractNotificationHandler<PrintStream> {
		public HandlerResult handleNotification(AssociationChangeNotification not, PrintStream stream) {
			if (not.event().equals(AssociationChangeNotification.AssocChangeEvent.COMM_UP)) {
				int outbound = not.association().maxOutboundStreams();
				int inbound = not.association().maxInboundStreams();
			}

			return HandlerResult.CONTINUE;
		}

		public HandlerResult handleNotification(ShutdownNotification not, PrintStream stream) {
			return HandlerResult.RETURN;
		}
	}

}