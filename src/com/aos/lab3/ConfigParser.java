package com.aos.lab3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigParser {

	private static Logger logger = LogManager.getLogger(ConfigParser.class);

	private String fileLocation = System.getProperty("config", "conf/config.txt");
	private Config config;

	public ConfigParser() throws IOException {
		parseConfig();
	}

	private void parseConfig() throws IOException {
		List<String> fileContent = Files.readAllLines(Paths.get(fileLocation));
		Iterator<String> iterator = fileContent.iterator();
		List<Node> nodes = new LinkedList<Node>();
		Map<Integer, Set<Integer>> nodeIdVsNeighbors = new HashMap<Integer, Set<Integer>>();
		Integer noOfNodes;
		Integer noOfOperations;
		Integer minInstanceDelay;
		Integer minSendDelay;
		Integer noOfMsgs;

		String line = getNextLine(iterator);

		// Ignore comments
		while (line.startsWith("#") || line.isEmpty()) {
			line = getNextLine(iterator);
		}

		String[] val = line.split(" ");
		noOfNodes = Integer.valueOf(val[0]);
		noOfOperations = Integer.valueOf(val[1]);
		minInstanceDelay = Integer.valueOf(val[2]);
		minSendDelay = Integer.valueOf(val[3]);
		noOfMsgs = Integer.valueOf(val[4]);

		line = getNextLine(iterator);

		//
		for (int i = 0; i < noOfNodes && iterator.hasNext(); line = getNextLine(iterator)) {
			// Ignore comments
			if (line.startsWith("#") || line.isEmpty())
				continue;

			String[] split = line.split("\\s+");

			Node node = new Node(Integer.valueOf(split[0]), split[1], Integer.valueOf(split[2]));

			nodes.add(node);
			i++;
		}

		for (int i = 0; i < noOfNodes;) {
			// Ignore comments
			if (line.startsWith("#") || line.isEmpty())
				continue;
			line = line.trim();

			String[] split = line.split(" ");
			int j = 1;
			Integer nodeId = Integer.valueOf(split[0]);
			Set<Integer> neighbhors = new TreeSet<Integer>();

			for (; j < split.length; j++) {
				neighbhors.add(Integer.valueOf(split[j].trim()));
			}

			nodeIdVsNeighbors.put(nodeId, neighbhors);
			i++;
			if (i < noOfNodes)
				line = getNextLine(iterator);
		}

		List<Operation> operations = new LinkedList<Operation>();

		while (iterator.hasNext()) {
			line = getNextLine(iterator);
			if (line == null)
				break;
			String[] split = line.split(",");
			Character opr = split[0].charAt(1);
			Integer nodeId = Integer.valueOf(split[1].substring(0, 1));
			OperationType oprType = null;
			if (opr.charValue() == 'C' || opr.charValue() == 'c') {
				oprType = OperationType.CHECKPOINT;
			} else if (opr.charValue() == 'R' || opr.charValue() == 'r') {
				oprType = OperationType.RECOVERY;
			} else {
				logger.error("Unsupported operation type: {}", opr);
			}
			operations.add(new Operation(oprType, nodeId));
		}
		config = new Config(noOfNodes, noOfOperations, minInstanceDelay, minSendDelay, noOfMsgs, nodes,
				nodeIdVsNeighbors, operations);
	}

	private String getNextLine(Iterator<String> iterator) {
		String line = null;
		while (iterator.hasNext()) {
			line = iterator.next();
			line = line.trim();
			if (line.startsWith("#") || line.isEmpty())
				continue;
			else
				break;
		}
		return line;
	}

	public Config getConfig() {
		return config;
	}

	public static void main(String[] args) {
		try {
			ConfigParser parser = new ConfigParser();
			Config config2 = parser.getConfig();

			printSampleMsg();

			System.out.println("Done");
		} catch (IOException e) {
			logger.error(e);
		}
	}

	private static void printSampleMsg() {
		Integer[] fls = new Integer[5];
		Integer[] llr = new Integer[5];
		Integer[] lls = new Integer[5];
		Set<Integer> neighbors = new HashSet<Integer>();
		neighbors.add(5);
		neighbors.add(1);
		neighbors.add(2);
		neighbors.add(4);
		neighbors.add(8);

		String reqId = "R-1";
		Integer nodeId = new Integer(3);
		for (int k = 0, i = 3, j = 7; k < 5; i++, j++, k++) {
			fls[k] = i;
			llr[k] = j;
			lls[k] = j * i;
		}
		logger.error("Operation completed in NodeId:{} OperationId:{} FLS:{} LLR:{} LLS:{} Neighbors:{}", nodeId, reqId,
				fls, llr, lls, neighbors);
	}

}