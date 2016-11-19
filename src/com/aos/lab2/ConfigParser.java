package com.aos.lab2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ConfigParser {

	// private String fileLocation = System.getProperty("config",
	// "C://Fall16/AOS/Project/Project2/config.txt");
	private String fileLocation = System.getProperty("config", "conf/config.txt");

	private Config config;

	public ConfigParser() throws IOException {
		parseConfig();
	}

	private void parseConfig() throws IOException {
		List<String> fileContent = Files.readAllLines(Paths.get(fileLocation));
		Iterator<String> iterator = fileContent.iterator();
		List<Node> nodes = new LinkedList<Node>();
		Map<Integer, Set<Integer>> nodeIdVsQuorum = new HashMap<Integer, Set<Integer>>();
		int noOfNodes;
		int csExecTime;
		int waitTime;
		int noOfAttempts;

		String line = getNextLine(iterator);

		// Ignore comments
		while (line.startsWith("#") || line.isEmpty()) {
			line = getNextLine(iterator);
		}

		String[] val = line.split(" ");
		noOfNodes = Integer.valueOf(val[0]);
		csExecTime = Integer.valueOf(val[2]);
		waitTime = Integer.valueOf(val[1]);
		noOfAttempts = Integer.valueOf(val[3]);

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

		for (int i = 0; i < noOfNodes; line = getNextLine(iterator)) {
			// Ignore comments

			if (line.startsWith("#") || line.isEmpty())
				continue;
			line = line.trim();

			String[] split = line.split(" ");
			int j = 1;
			Integer nodeId = Integer.valueOf(split[0]);
			Set<Integer> quorum = new TreeSet<Integer>();

			for (; j < split.length; j++) {
				quorum.add(Integer.valueOf(split[j].trim()));
			}

			nodeIdVsQuorum.put(nodeId, quorum);
			i++;
		}
		config = new Config(noOfNodes, csExecTime, waitTime, noOfAttempts, nodes, nodeIdVsQuorum);
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

	private List<Integer> parsePath(String string) {
		List<Integer> path = new LinkedList<Integer>();
		String[] split = string.split(",");

		path.add(Integer.valueOf(split[0].split("\\(")[1].trim()));
		for (int i = 1; i < split.length - 1; i++) {
			path.add(Integer.valueOf(split[i].trim()));
		}

		path.add(Integer.valueOf(split[split.length - 1].split("\\)")[0].trim()));
		return path;
	}

	public Config getConfig() {
		return config;
	}

	public static void main(String[] args) {
		try {
			ConfigParser parser = new ConfigParser();
			Config config2 = parser.getConfig();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}