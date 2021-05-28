package convex.cli.peer;

import java.io.IOException;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Map;


import convex.api.Shutdown;
import convex.cli.Helpers;
import convex.core.data.Keyword;
import convex.core.data.Keywords;
import convex.core.crypto.AKeyPair;
import convex.core.store.AStore;
import convex.core.Init;
import convex.core.store.Stores;
import convex.core.Order;
import convex.core.State;
import convex.peer.API;
import convex.peer.Server;
import etch.EtchStore;


/**
*
* Convex PeerManager
*
*/

public class PeerManager {

	private static final Logger log = Logger.getLogger(PeerManager.class.getName());

	static public List<Server> peerServerList = new ArrayList<Server>();

	protected Session session = new Session();

	/**
	 * Launch a set of peers.
	 *
	 * @param count Number of peers to launch.
	 *
	 * @param keyPairs Array of keyPairs for each peer. The length of the array must be >= the count of peers to launch.
	 *
	 */
	public void launchPeers(int count, AKeyPair[] keyPairs) {
		peerServerList.clear();

		for (int i = 0; i < count; i++) {
			AKeyPair keyPair = keyPairs[i];
			Server peerServer = launchPeer(keyPair);
		}
	}

	/**
	 * Connect a peer server to a list of peer addresses.
	 *
	 * @param peerSerever The peer server that you wish to connect too.
	 *
	 * @param addressList The array list of addresses that you wish to have the peer server too connect too.
	 *
	 */
	public void connectToPeers(Server peerServer, InetSocketAddress[] addressList) {
		InetSocketAddress peerAddress = peerServer.getHostAddress();
		for (int index = 0; index < addressList.length; index++) {
			InetSocketAddress address = addressList[index];
			if (peerAddress != address) {
				try {
					peerServer.connectToPeer(address);
				} catch (IOException e) {
					System.out.println("Connect failed to: "+address);
				}
			}
		}
	}

	/**
	 * Load in a session from a session file.
	 *
	 * @param sessionFilename Filename to load.
	 *
	 */
	protected void loadSession(String sessionFilename) {
		File sessionFile = new File(sessionFilename);
		try {
			session.load(sessionFile);
		} catch (IOException e) {
			log.severe("Cannot load the session control file");
		}
	}

	/**
	 * Add a peer to the session list of peers.
	 *
	 * @param peerServer Add the peerServer to the list of peers for this session.
	 *
	 */
	protected void addToSession(Server peerServer) {
		InetSocketAddress peerHostAddress = peerServer.getHostAddress();
		EtchStore store = (EtchStore) peerServer.getStore();

		session.addPeer(
			peerServer.getAddress().toHexString(),
			peerHostAddress.getHostName(),
			peerHostAddress.getPort(),
			store.getFileName()
		);
	}

	/**
	 * Add all peers started in this session to the session list.
	 *
	 */
	protected void addAllToSession() {
		for (Server peerServer: peerServerList) {
			addToSession(peerServer);
		}
	}

	/**
	 * Remove all peers added by this manager from the session list of peers.
	 *
	 */
	protected void removeAllFromSession() {
		for (Server peerServer: peerServerList) {
			session.removePeer(peerServer.getAddress().toHexString());
		}
	}

	/**
	 * Store the session details to file.
	 *
	 * @param sessionFilename Fileneame to save the session.
	 *
	 */
	protected void storeSession(String sessionFilename) {
		File sessionFile = new File(sessionFilename);
		try {
			Helpers.createPath(sessionFile);
			if (session.getPeerCount() > 0) {
				session.store(sessionFile);
			}
			else {
				sessionFile.delete();
			}
		} catch (IOException e) {
			log.severe("Cannot store the session control data");
		}
	}

	/**
	 * Launch a local peer on a random port number.
	 *
	 * @param keyPair KeyPair to use for the lauched peer.
	 *
	 * @return Server object of the launched peer.
	 *
	 */
	public Server launchPeer(AKeyPair keyPair) {
		return launchPeer(keyPair, 0);
	}

	/**
	 * Launch a pear for a given port number.
	 *
	 * @param keyPair KeyPair to use for the lauched peer.
	 *
	 * @param port Port number to use for the peer.
	 *
	 * @return Server object of the launched peer.
	 *
	 */
	public Server launchPeer(AKeyPair keyPair, int port) {
		return launchPeer(keyPair, 0, null);

	}

	/**
	 * Launch a peer.
	 *
	 * @param keyPair KeyPair to use for the lauched peer.
	 *
	 * @param port Port number to use for the peer.
	 *
	 * @param store Store to use for the peer.
	 *
	 * @return Server object of the launched peer.
	 *
	*/
	public Server launchPeer(AKeyPair keyPair, int port, AStore store) {
		Map<Keyword, Object> config = new HashMap<>();

		config.put(Keywords.PORT, null);
		if (port>0) {
			config.put(Keywords.PORT, port);
		}
		config.put(Keywords.KEYPAIR, keyPair);
		config.put(Keywords.STATE, Init.createState());

		// Use a different fresh store for each peer
		// config.put(Keywords.STORE, EtchStore.createTemp());

		if (store==null) {
			// Use a shared store
			store = Stores.getGlobalStore();
		}
		config.put(Keywords.STORE, store);

		log.info("launch peer: "+keyPair.getAccountKey().toHexString());

		Server peerServer = API.launchPeer(config);

		peerServerList.add(peerServer);

		return peerServer;
	}

	/**
	 * Once the manager has launched 1 or more peers. The manager now needs too loop and wait for the peer(s)
	 * to exit.
	 *
	 * @param sessionFilename filename of the session file to save the peer session details.
	 *
	 */
	public void waitForPeers(String sessionFilename) {
		long consensusPoint = 0;
		long maxBlock = 0;

		// write the launched peer details to a session file
		loadSession(sessionFilename);
		addAllToSession();
		storeSession(sessionFilename);

		/*
			Go through each started peer server connection and make sure
			that each peer is connected to the other peer.
		*/
		for (Server peerServer: peerServerList) {
			connectToPeers(peerServer, session.getPeerAddressList());
		}

		// shutdown hook to remove/update the session file
		convex.api.Shutdown.addHook(Shutdown.CLI,new Runnable() {
		    public void run() {
				// System.out.println("peers stopping");
				// remove session file
				loadSession(sessionFilename);
				removeAllFromSession();
				storeSession(sessionFilename);
		    }
		});

		Server firstServer = peerServerList.get(0);
		State lastState = firstServer.getPeer().getConsensusState();
		log.info("state hash: "+lastState.getHash());

		while (true) {
			try {
				Thread.sleep(30);
				for (Server peerServer: peerServerList) {
					convex.core.Peer peer = peerServer.getPeer();
					if (peer==null) continue;

					State state = peer.getConsensusState();
					// System.out.println("state " + state);
					Order order=peer.getPeerOrder();
					if (order==null) continue; // not an active peer?
					maxBlock = Math.max(maxBlock, order.getBlockCount());

					long peerConsensusPoint = peer.getConsensusPoint();
					if (peerConsensusPoint > consensusPoint) {
						consensusPoint = peerConsensusPoint;
						System.err.printf("Consenus State update detected at depth %d\n", consensusPoint);
					}
				}
			} catch (InterruptedException e) {
				System.out.println("Peer manager interrupted!");
				return;
			}
		}
	}
}