package org.hive2hive.core.network.data;

import java.io.IOException;

import net.tomp2p.futures.FutureGet;
import net.tomp2p.futures.FuturePut;
import net.tomp2p.futures.FutureRemove;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.storage.Data;

import org.hive2hive.core.log.H2HLogger;
import org.hive2hive.core.log.H2HLoggerFactory;
import org.hive2hive.core.network.NetworkManager;
import org.hive2hive.core.network.data.futures.FutureGetListener;
import org.hive2hive.core.network.data.futures.FuturePutListener;
import org.hive2hive.core.network.data.futures.FutureRemoveListener;

/**
 * This class offers an interface for storing into and loading from the network.
 * Data can be stored or loaded globally or locally.
 * 
 * @author Seppi
 */
public class DataManager {

	private static final H2HLogger logger = H2HLoggerFactory.getLogger(DataManager.class);

	private final Number160 TOMP2P_DEFAULT_KEY = Number160.ZERO;

	private final NetworkManager networkManager;

	public DataManager(NetworkManager networkManager) {
		this.networkManager = networkManager;
	}

	private Peer getPeer() {
		return networkManager.getConnection().getPeer();
	}

	public void putGlobal(String locationKey, String contentKey, NetworkContent content, IPutListener listener) {
		FuturePut putFuture = putGlobal(locationKey, contentKey, content);
		if (putFuture == null && listener != null) {
			listener.onFailure();
			return;
		}
		putFuture.addListener(new FuturePutListener(locationKey, contentKey, content, listener, this));
	}

	public FuturePut putGlobal(String locationKey, String contentKey, NetworkContent content) {
		logger.debug(String.format("global put key = '%s' content key = '%s' version key = '%s'",
				locationKey, contentKey, content.getVersionKey()));
		try {
			Data data = new Data(content);
			data.ttlSeconds(content.getTimeToLive()).basedOn(content.getBasedOnKey());
			return getPeer().put(Number160.createHash(locationKey))
					.setData(Number160.createHash(contentKey), data).setVersionKey(content.getVersionKey())
					.start();
		} catch (IOException e) {
			logger.error(String
					.format("Global put failed. content = '%s' in the location = '%s' under the contentKey = '%s' exception = '%s'",
							content.toString(), locationKey, contentKey, e.getMessage()));
			return null;
		}
	}

	public void putLocal(String locationKey, String contentKey, NetworkContent content) {
		logger.debug(String.format("local put key = '%s' content key = '%s'", locationKey, contentKey));
		try {
			Number640 key = new Number640(Number160.createHash(locationKey), TOMP2P_DEFAULT_KEY,
					Number160.createHash(contentKey), content.getVersionKey());
			Data data = new Data(content);
			data.ttlSeconds(content.getTimeToLive()).basedOn(content.getBasedOnKey());
			// TODO add public key for content protection
			// TODO for what is this putIfAbsent flag?
			// TODO what is the domainProtaction flag?
			getPeer().getPeerBean().storage().put(key, data, null, false, false);
		} catch (IOException e) {
			logger.error(String
					.format("Local put failed. content = '%s' in the location = '%s' under the contentKey = '%s' exception = '%s'",
							content.toString(), locationKey, contentKey, e.getMessage()));
		}
	}

	public void getGlobal(String locationKey, String contentKey, IGetListener listener) {
		getGlobal(locationKey, contentKey, TOMP2P_DEFAULT_KEY, listener);
	}

	public void getGlobal(String locationKey, String contentKey, Number160 versionKey, IGetListener listener) {
		FutureGet futureGet = getGlobal(locationKey, contentKey, versionKey);
		futureGet.addListener(new FutureGetListener(locationKey, contentKey, versionKey, listener));
	}

	public FutureGet getGlobal(String locationKey, String contentKey) {
		return getGlobal(locationKey, contentKey, TOMP2P_DEFAULT_KEY);
	}

	public FutureGet getGlobal(String locationKey, String contentKey, Number160 versionKey) {
		logger.debug(String.format("global get key = '%s' content key = '%s' version key = '%s'",
				locationKey, contentKey, versionKey));
		return getPeer().get(Number160.createHash(locationKey))
				.setContentKey(Number160.createHash(contentKey)).setVersionKey(versionKey).start();
	}

	public NetworkContent getLocal(String locationKey, String contentKey) {
		return getLocal(locationKey, contentKey, TOMP2P_DEFAULT_KEY);
	}

	public NetworkContent getLocal(String locationKey, String contentKey, Number160 versionKey) {
		logger.debug(String.format("local get key = '%s' content key = '%s' version key = '%s'", locationKey,
				contentKey, versionKey));
		Number640 key = new Number640(Number160.createHash(locationKey), TOMP2P_DEFAULT_KEY,
				Number160.createHash(contentKey), versionKey);
		Data data = getPeer().getPeerBean().storage().get(key);
		if (data != null) {
			try {
				return (NetworkContent) data.object();
			} catch (ClassNotFoundException | IOException e) {
				logger.error(String.format("local get failed exception = '%s'", e.getMessage()));
			}
		} else {
			logger.warn("futureDHT.getData() is null");
		}
		return null;
	}

	public void remove(String locationKey, String contentKey, Number160 versionKey, IRemoveListener listener) {
		FutureRemove futureRemove = remove(locationKey, contentKey, versionKey);
		futureRemove
				.addListener(new FutureRemoveListener(locationKey, contentKey, versionKey, listener, this));
	}

	public FutureRemove remove(String locationKey, String contentKey, Number160 versionKey) {
		logger.debug(String.format("remove key = '%s' content key = '%s' version key = '%s", locationKey,
				contentKey, versionKey));
		return getPeer().remove(Number160.createHash(locationKey))
				.setContentKey(Number160.createHash(contentKey)).setVersionKey(versionKey).start();
	}

}
