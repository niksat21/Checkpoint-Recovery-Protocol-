package com.aos.lab3;

import java.io.Serializable;

public enum MessageType implements Serializable{

	COMPLETED, FAILED, CHECKPOINT, RECOVERY, ACKCHECKPOINT,
	ACKRECOVERY

}
