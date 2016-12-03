package com.aos.lab3;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.aos.lab3.Server.AssociationHandler;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class ServerWorker implements Runnable {

	private volatile static Set<Integer> completedSet = new HashSet<Integer>();
	public volatile static Boolean isCompleted = Boolean.FALSE;
	private volatile static int result = -1;
	private volatile static int completeMessageCount = 0;
	private volatile static Integer noOfNodes;

	private Logger logger = LogManager.getLogger(ServerWorker.class);
	private SctpChannel sc;
	private Integer nodeId;
	private Client client;
	private Integer labelValue;
	private Config config;
	private AssociationHandler assocHandler;
	private SctpServerChannel ssc;
	private ICheckpointRequestHandler iCheckpointHandler;
	private IRecoveryRequestHandler iRecoveryHandler;
	private RequestingCandidate reqCandidate;

	public ServerWorker(Integer nodeId, SctpChannel sc, Client client, Integer labelValue, Config config,
			AssociationHandler assocHandler, SctpServerChannel ssc, ICheckpointRequestHandler iCheckpointHandler,
			IRecoveryRequestHandler iRecoveryHandler, RequestingCandidate reqCandidate) {
		this.sc = sc;
		this.nodeId = nodeId;
		this.client = client;
		this.labelValue = labelValue;
		this.config = config;
		this.assocHandler = assocHandler;
		this.ssc = ssc;
		this.iCheckpointHandler = iCheckpointHandler;
		this.iRecoveryHandler = iRecoveryHandler;
		this.reqCandidate = reqCandidate;
		noOfNodes = config.getNoOfNodes();
	}

	@Override
	public void run() {
		try {
			// Sleep for sometime so that the other nodes come up.
			Thread.sleep(8000);

			while (true) {
				ByteBuffer buf = ByteBuffer.allocateDirect(500000);
				MessageInfo messageInfo = sc.receive(buf, System.out, assocHandler);
				buf.flip();
				byte[] data = new byte[buf.remaining()];
				buf.get(data, 0, data.length);
				ByteArrayInputStream bis = new ByteArrayInputStream(data);
				ObjectInputStream ois = new ObjectInputStream(bis);
				Message msg = (Message) ois.readObject();

				// clearing buffer
				buf.clear();
				ois.close();
				bis.close();

				if (msg.getMsgType().equals(MessageType.CHECKPOINT)) {
					logger.debug("RECEIVED CHECKPOINT MSG at:{} from:{} with fls:{}", msg.getDestination(),
							msg.getSource(), client.getFls());
					iCheckpointHandler.handleCheckpointMessage(msg, client.getFls(), msg.getOperationId());
				} else if (msg.getMsgType().equals(MessageType.RECOVERY)) {
					logger.debug("RECEIVED RECIVERY MSG at:{} from:{} with LLR:{}", msg.getDestination(),
							msg.getSource(), client.getLlr());
					iRecoveryHandler.handleRecoveryMessage(msg, client.getLlr(), msg.getOperationId());
				} else if (msg.getMsgType().equals(MessageType.COMPLETED)) {
					logger.debug("Received COMPLETED at:{} from:{} ", msg.getSource(), msg.getDestination());
					handleCompleteMessage(msg.getSource());
				} else if (msg.getMsgType().equals(MessageType.ACKCHECKPOINT)) {
					logger.debug("RECEIVED ACKCHECKPOINT MSG at:{} from:{}", msg.getDestination(), msg.getSource());
					iCheckpointHandler.handleAckChpMessage(msg.getSource(), msg.getDestination());
				} else if (msg.getMsgType().equals(MessageType.ACKRECOVERY)) {
					logger.debug("RECEIVED ACKRECOVERY MSG at:{} from:{}", msg.getDestination(), msg.getSource());
					iRecoveryHandler.handleAckRcvMessage(msg.getSource(), msg.getDestination());
				} else if (msg.getMsgType().equals(MessageType.APPLICATION)) {
					client.getLlr()[msg.getSource()] = msg.getValue();
					logger.debug("RECEIVED APPLICATION MSG at:{} from:{} with LLR:{}", msg.getDestination(),
							msg.getSource(), client.getLlr());
				} else if (msg.getMsgType().equals(MessageType.CHECKPOINT_COMPLETED)) {
					iCheckpointHandler.handleCheckpointCompletionMsg(msg.getInitiator(), msg.getSource(),
							msg.getOperationId());
				} else if (msg.getMsgType().equals(MessageType.RECOVERY_COMPLETED)) {
					iRecoveryHandler.handleRecoveryCompletionMsg(msg.getInitiator(), msg.getSource(),
							msg.getOperationId());
				} else {
					logger.error("Unsupported message type : {} by the quorum handler", msg.getMsgType().toString());
				}
			}
		} catch (Exception e) {
			logger.error("Exception in Server Worker thread", e);
		}
	}

	public static boolean isCompleted() {
		return isCompleted;
	}

	public static int getResult() {
		return result;
	}

	public static void main(String[] args) {
		String json = "O{\"source\":1,\"destination\":0,\"path\":[1],\"value\":0,\"msgType\":\"DATA\",\"port\":1252}";
		json = json.substring(1);
		System.out.println(json);
	}

	public void handleCompleteMessage(Integer src) throws InterruptedException {
		synchronized (ServerWorker.class) {
			completeMessageCount++;
			if ((completeMessageCount + 1 == noOfNodes) && isCompleted) {
				shutdown();
				logger.error("EXIT!!!!!!!!!!!!!!");
				Thread.sleep(1000);
				System.exit(0);
			}
		}
	}

	public void shutdown() {
		try {
			sc.close();
			ssc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
