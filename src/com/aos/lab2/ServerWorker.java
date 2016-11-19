package com.aos.lab2;

import java.io.ByteArrayInputStream;
import com.sun.nio.sctp.SctpServerChannel;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.aos.lab2.Server.AssociationHandler;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class ServerWorker implements Runnable {

	private volatile static Set<Integer> completedSet = new HashSet<Integer>();
	public volatile static Boolean isCompleted = Boolean.FALSE;
	private volatile static int result = -1;
	private volatile static int completeMessageCount=0;
	private volatile static Integer noOfNodes;

	private Logger logger = LogManager.getLogger(ServerWorker.class);
	private SctpChannel sc;
	private Integer nodeId;
	private Client client;
	private Integer labelValue;
	private Config config;
	private AssociationHandler assocHandler;
	private IQuorumRequestHandler quorumRequestHandler;
	private ICriticalSectionHandler csHandler;
	private SctpServerChannel ssc;

	public ServerWorker(Integer nodeId, SctpChannel sc, Client client, Integer labelValue, Config config,
			AssociationHandler assocHandler, IQuorumRequestHandler quorumRequestHandler,
			ICriticalSectionHandler csHandler, SctpServerChannel ssc) {
		this.sc = sc;
		this.nodeId = nodeId;
		this.client = client;
		this.labelValue = labelValue;
		this.config = config;
		this.assocHandler = assocHandler;
		this.quorumRequestHandler = quorumRequestHandler;
		this.csHandler = csHandler;
		this.ssc = ssc;
		noOfNodes= config.getNoOfNodes();
	}

	@Override
	public void run() {
		try {
			// Sleep for sometime so that the other nodes come up.
			Thread.sleep(8000);

			while (true /* && !isCompleted */) {
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

				if (msg.getMsgType().equals(MessageType.REQUEST)) {
					quorumRequestHandler.handleRequestMessage(new CSRequest(msg.getSource(), msg.getRequestTS()));
				} else if (msg.getMsgType().equals(MessageType.RELEASE)) {
					quorumRequestHandler.handleReleaseMessage(msg.getSource());
				} else if (msg.getMsgType().equals(MessageType.YIELD)) {
					quorumRequestHandler.handleYieldMessage(msg.getSource());
				} else if (msg.getMsgType().equals(MessageType.GRANT)) {
					csHandler.handleGrantMessage(msg.getSource());
				} else if (msg.getMsgType().equals(MessageType.FAILED)) {
					csHandler.handleFailedMessage(msg.getSource());
				} else if (msg.getMsgType().equals(MessageType.INQUIRE)) {
					csHandler.handleInquireMessage(msg.getSource());
				} else if (msg.getMsgType().equals(MessageType.COMPLETED)) {
						handleCompleteMessage(msg.getSource());
						logger.error("Received COMPLETED at:{} from:{} ", msg.getSource(), msg.getDestination());
				} else {
					logger.error("Unsupported message type : {} by the quorum handler", msg.getMsgType().toString());
				}
			}
		} catch (Exception e) {
			//logger.error("Exception in Server Worker thread", e);
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
			synchronized(ServerWorker.class) {
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
