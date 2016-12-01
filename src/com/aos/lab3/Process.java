package com.aos.lab3;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Process {

	private static final int INITIAL_SLEEP_TIME = 8000;
	private static Logger logger = LogManager.getLogger(Process.class);
	private static Random rand = new Random();
	private static int labelValue = rand.nextInt(9) + 1;
	private static Integer nodeId;

	public static void main(String[] args) {
		try {
			ConfigParser parser = new ConfigParser();
			Config config = parser.getConfig();

			String hostname = InetAddress.getLocalHost().getHostName();
			nodeId = Integer.valueOf(System.getProperty("nodeId"));
			logger.info("Hostname:{} NodeId:{} Label value:{}", hostname, nodeId, labelValue);
			Node node = config.getNodes().get(nodeId);

			// starting servers
			Server server = new Server(nodeId, labelValue, node.getPort(), config);
			Client client = new Client(hostname, labelValue, config, nodeId);
			server.setClientHandler(client);
			Thread clientThread = new Thread(client, "client-thread");
			Thread serverThread = new Thread(server, "server-thread");

			List<Message> operationQueue = new LinkedList<Message>();
			IApplicationStateHandler appStateHandler = new ApplicationStateHandler();
			ICheckpointRequestHandler checkpointHandler = new CheckpointRequestHandler(client, config, nodeId,
					appStateHandler, operationQueue);
			IRecoveryRequestHandler recoveryHandler = new RecoveryRequestHandler(client, config, nodeId,
					appStateHandler, operationQueue);
			RequestingCandidate rc = new RequestingCandidate(config, nodeId, client, checkpointHandler, recoveryHandler,
					operationQueue);
			server.setCheckpointHandler(checkpointHandler);
			server.setRecovereHandler(recoveryHandler);
			server.setReqCandidate(rc);
			checkpointHandler.setRequestingCandidateHandler(rc);
			recoveryHandler.setRequestingCandidateHandler(rc);
			clientThread.start();
			serverThread.start();
			// Wait for other nodes to come up
			logger.info("Sleeping for {}", INITIAL_SLEEP_TIME);
			Thread.sleep(INITIAL_SLEEP_TIME);
			rc.start();
		} catch (Exception e) {
			logger.error("Exception in Process", e);
		}

	}
}