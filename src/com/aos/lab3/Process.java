package com.aos.lab3;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Random;
import java.lang.reflect.Field;
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

			String hostname = InetAddress.getLocalHost().getHostName();
			nodeId = Integer.valueOf(System.getProperty("nodeId"));
			logger.info("Hostname:{} NodeId:{} Label value:{}", hostname, nodeId, labelValue);
			Node node = config.getNodes().get(nodeId);
			
			//starting servers
			Server server = new Server(nodeId, labelValue, node.getPort(), config, quroumRequestHandler, csHandler);
			Client client = new Client(hostname, labelValue, config, nodeId);
			server.setClientHandler(client);

			Thread clientThread = new Thread(client, "client-thread");
			Thread serverThread = new Thread(server, "server-thread");

			clientThread.start();
			serverThread.start();

			Thread.sleep(10000);
			
			//Iterating through the list of operations
			ICheckpointRequestHandler cRequestHandler = null;
			IRecoveryRequestHandler rRequestHandler = null;
			
			Iterator itr = config.operations.iterator();
			Operation opr= null;
			while(itr.hasNext()){
				opr = (Operation) itr.next();
				if (opr.getNodeId() == nodeId && opr.getType().equals(OperationType.CHECKPOINT)) {
					RequestingCandidate rc = new RequestingCandidate(config, nodeId, client, cRequestHandler);
					rc.requestCheckpoint();
				}
				else if(opr.getNodeId() == nodeId && opr.getType().equals(OperationType.RECOVERY)) {
					RequestingCandidate rc = new RequestingCandidate(config, nodeId, client, rRequestHandler);
					rc.requestRecovery();
				}
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
