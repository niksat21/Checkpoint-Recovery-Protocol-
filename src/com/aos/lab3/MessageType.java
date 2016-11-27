package com.aos.lab3;

import java.io.Serializable;

public enum MessageType implements Serializable {

	COMPLETED, DATA, INQUIRE, REQUEST, RELEASE, GRANT, YIELD, FAILED, CHECKPOINT, RECOVERY, ACKCHECKPOINT, 
	ACKRECOVERY, APPLICATION, PERMCHECKPOINT
}
