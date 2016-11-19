package com.aos.lab2;

import java.util.Comparator;

class RequestTSComparator implements Comparator<CSRequest> {

	@Override
	public int compare(CSRequest o1, CSRequest o2) {
		if (o1.getTimestamp() < o2.getTimestamp()) {
			return -1;
		} else if (o1.getTimestamp() > o2.getTimestamp()) {
			return 1;
		} else {
			// If the Timestamps are equal, then order it by nodeId
			if (o1.getNodeId() < o2.getNodeId())
				return -1;
			else
				return 1;
		}
	}

}
