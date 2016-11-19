package com.aos.lab2;

import java.net.InetAddress;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Process {

	private static Logger logger = LogManager.getLogger(Process.class);
	private static Random rand = new Random();
	private static int labelValue = rand.nextInt(9) + 1;
	private static Integer nodeId;

	public static void main(String[] args) {
		try {
			ConfigParser parser = new ConfigParser();
			Config config = parser.getConfig();

			String version = System.getProperty("version");

			if (version.equals("holdandwait")) {
				config.setVersion(DeadlockResolverType.HOLD_AND_WAIT);
			} else if (version.equals("preemptive")) {
				config.setVersion(DeadlockResolverType.PREEMPTIVE);
			} else {
				logger.error("Unsupported version: {}", version);
			}

			String hostname = InetAddress.getLocalHost().getHostName();
			nodeId = Integer.valueOf(System.getProperty("nodeId"));
			logger.info("Hostname:{} NodeId:{} Label value:{}", hostname, nodeId, labelValue);
			Node node = config.getNodeById(nodeId);

			IQuorumRequestHandler quroumRequestHandler = null;
			ICriticalSectionHandler csHandler = null;
			if (config.getVersion().equals(DeadlockResolverType.HOLD_AND_WAIT)) {
				quroumRequestHandler = new HoldAndWaitQuorumRequestHandler(node, config);
				csHandler = new HoldAndWaitCSHandler(config, node, config.getNodeQuorumById(node.getNodeId()));
			} else if (config.getVersion().equals(DeadlockResolverType.PREEMPTIVE)) {
				quroumRequestHandler = new PreemptiveQuorumRequestHandler(node, config);
				csHandler = new PreemptiveCSHandler(config, node, config.getNodeQuorumById(nodeId));
			} else {
				logger.error("Unsupported config version: {}", config.getVersion().toString());
			}

			Server server = new Server(nodeId, labelValue, node.getPort(), config, quroumRequestHandler, csHandler);
			Client client = new Client(hostname, labelValue, config, nodeId);
			server.setClientHandler(client);

			Thread clientThread = new Thread(client, "client-thread");
			Thread serverThread = new Thread(server, "server-thread");

			clientThread.start();
			serverThread.start();

			Thread.sleep(10000);

			if (config.getNodeQuorumById(nodeId).size() != 0) {
				RequestingCandidate rc = new RequestingCandidate(config, nodeId, client, csHandler, quroumRequestHandler);
				rc.requestCS();
			}

		} catch (Exception e) {
			logger.error("Exception in Process", e);
		}

	}

	// private static void writeOutputToFile(String hostname) throws IOException
	// {
	// BufferedWriter writer = new BufferedWriter(new FileWriter(hostname + "_"
	// + nodeId + ".txt"));
	// writer.write("Label value: " + labelValue);
	// writer.write("\nOutput value: " + ServerWorker.getResult());
	// writer.close();
	// }

	private static Integer getNodeId(String hostname, Config config) {
		for (Node node : config.getNodes()) {
			if (node.getHostname().equals(hostname))
				return node.getNodeId();
		}
		logger.error("Unable to find nodeId for hostname: {} in the list", hostname);
		return null;
	}

}
