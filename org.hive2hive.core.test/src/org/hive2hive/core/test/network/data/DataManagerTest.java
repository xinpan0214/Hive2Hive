package org.hive2hive.core.test.network.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.List;
import java.util.Random;

import net.tomp2p.futures.FutureGet;
import net.tomp2p.futures.FuturePut;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

import org.hive2hive.core.H2HConstants;
import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.network.NetworkManager;
import org.hive2hive.core.security.EncryptionUtil;
import org.hive2hive.core.security.H2HSignatureFactory;
import org.hive2hive.core.test.H2HJUnitTest;
import org.hive2hive.core.test.H2HSharableTestData;
import org.hive2hive.core.test.H2HTestData;
import org.hive2hive.core.test.network.NetworkTestUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Seppi
 */
public class DataManagerTest extends H2HJUnitTest {

	private static List<NetworkManager> network;
	private static final int networkSize = 3;
	private static Random random = new Random();

	@BeforeClass
	public static void initTest() throws Exception {
		testClass = DataManagerTest.class;
		beforeClass();
		network = NetworkTestUtil.createNetwork(networkSize);
	}

	@Test
	public void testPutGet() throws Exception {
		Number160 locationKey = Number160.createHash(NetworkTestUtil.randomString());
		Number160 domainKey = H2HConstants.TOMP2P_DEFAULT_KEY;
		Number160 contentKey = Number160.createHash(NetworkTestUtil.randomString());

		NetworkManager node = network.get(random.nextInt(networkSize));

		String data = NetworkTestUtil.randomString();
		FuturePut future = node.getDataManager().put(locationKey, domainKey, contentKey,
				new H2HTestData(data), null);
		future.awaitUninterruptibly();

		FutureGet futureGet = node.getDataManager().get(locationKey, domainKey, contentKey);
		futureGet.awaitUninterruptibly();

		String result = (String) ((H2HTestData) futureGet.getData().object()).getTestString();
		assertEquals(data, result);
	}

	@Test
	public void testPutGetFromOtherNode() throws Exception {
		Number160 locationKey = Number160.createHash(NetworkTestUtil.randomString());
		Number160 domainKey = H2HConstants.TOMP2P_DEFAULT_KEY;
		Number160 contentKey = Number160.createHash(NetworkTestUtil.randomString());

		NetworkManager nodeA = network.get(random.nextInt(networkSize / 2));
		NetworkManager nodeB = network.get(random.nextInt(networkSize / 2) + networkSize / 2);

		String data = NetworkTestUtil.randomString();
		FuturePut future = nodeA.getDataManager().put(locationKey, domainKey, contentKey,
				new H2HTestData(data), null);
		future.awaitUninterruptibly();

		FutureGet futureGet = nodeB.getDataManager().get(locationKey, domainKey, contentKey);
		futureGet.awaitUninterruptibly();

		String result = ((H2HTestData) futureGet.getData().object()).getTestString();
		assertEquals(data, result);
	}

	@Test
	public void testPutOneLocationKeyMultipleContentKeys() throws Exception {
		Number160 locationKey = Number160.createHash(NetworkTestUtil.randomString());
		Number160 domainKey = H2HConstants.TOMP2P_DEFAULT_KEY;
		Number160 contentKey1 = Number160.createHash(NetworkTestUtil.randomString());
		Number160 contentKey2 = Number160.createHash(NetworkTestUtil.randomString());
		Number160 contentKey3 = Number160.createHash(NetworkTestUtil.randomString());

		NetworkManager node = network.get(random.nextInt(networkSize));

		String data1 = NetworkTestUtil.randomString();
		FuturePut future1 = node.getDataManager().put(locationKey, domainKey, contentKey1,
				new H2HTestData(data1), null);
		future1.awaitUninterruptibly();

		String data2 = NetworkTestUtil.randomString();
		FuturePut future2 = node.getDataManager().put(locationKey, domainKey, contentKey2,
				new H2HTestData(data2), null);
		future2.awaitUninterruptibly();

		String data3 = NetworkTestUtil.randomString();
		FuturePut future3 = node.getDataManager().put(locationKey, domainKey, contentKey3,
				new H2HTestData(data3), null);
		future3.awaitUninterruptibly();

		FutureGet get1 = node.getDataManager().get(locationKey, domainKey, contentKey1);
		get1.awaitUninterruptibly();
		String result1 = (String) ((H2HTestData) get1.getData().object()).getTestString();
		assertEquals(data1, result1);

		FutureGet get2 = node.getDataManager().get(locationKey, domainKey, contentKey2);
		get2.awaitUninterruptibly();
		String result2 = (String) ((H2HTestData) get2.getData().object()).getTestString();
		assertEquals(data2, result2);

		FutureGet get3 = node.getDataManager().get(locationKey, domainKey, contentKey3);
		get3.awaitUninterruptibly();
		String result3 = (String) ((H2HTestData) get3.getData().object()).getTestString();
		assertEquals(data3, result3);
	}

	@Test
	public void testPutOneLocationKeyMultipleContentKeysGlobalGetFromOtherNodes() throws Exception {
		Number160 locationKey = Number160.createHash(NetworkTestUtil.randomString());
		Number160 domainKey = H2HConstants.TOMP2P_DEFAULT_KEY;
		Number160 contentKey1 = Number160.createHash(NetworkTestUtil.randomString());
		Number160 contentKey2 = Number160.createHash(NetworkTestUtil.randomString());
		Number160 contentKey3 = Number160.createHash(NetworkTestUtil.randomString());

		String data1 = NetworkTestUtil.randomString();
		FuturePut future1 = network.get(random.nextInt(networkSize)).getDataManager()
				.put(locationKey, domainKey, contentKey1, new H2HTestData(data1), null);
		future1.awaitUninterruptibly();

		String data2 = NetworkTestUtil.randomString();
		FuturePut future2 = network.get(random.nextInt(networkSize)).getDataManager()
				.put(locationKey, domainKey, contentKey2, new H2HTestData(data2), null);
		future2.awaitUninterruptibly();

		String data3 = NetworkTestUtil.randomString();
		FuturePut future3 = network.get(random.nextInt(networkSize)).getDataManager()
				.put(locationKey, domainKey, contentKey3, new H2HTestData(data3), null);
		future3.awaitUninterruptibly();

		FutureGet get1 = network.get(random.nextInt(networkSize)).getDataManager()
				.get(locationKey, domainKey, contentKey1);
		get1.awaitUninterruptibly();
		String result1 = (String) ((H2HTestData) get1.getData().object()).getTestString();
		assertEquals(data1, result1);

		FutureGet get2 = network.get(random.nextInt(networkSize)).getDataManager()
				.get(locationKey, domainKey, contentKey2);
		get2.awaitUninterruptibly();
		String result2 = (String) ((H2HTestData) get2.getData().object()).getTestString();
		assertEquals(data2, result2);

		FutureGet get3 = network.get(random.nextInt(networkSize)).getDataManager()
				.get(locationKey, domainKey, contentKey3);
		get3.awaitUninterruptibly();
		String result3 = (String) ((H2HTestData) get3.getData().object()).getTestString();
		assertEquals(data3, result3);
	}

	@Test
	public void testRemovalOneContentKey() throws NoPeerConnectionException {
		NetworkManager nodeA = network.get(random.nextInt(networkSize / 2));
		NetworkManager nodeB = network.get(random.nextInt(networkSize / 2) + networkSize / 2);
		String locationKey = nodeB.getNodeId();
		Number160 lKey = Number160.createHash(locationKey);
		Number160 domainKey = Number160.createHash("a domain key");
		String contentKey = NetworkTestUtil.randomString();
		Number160 cKey = Number160.createHash(contentKey);

		// put a content
		nodeA.getDataManager()
				.put(lKey, domainKey, cKey, new H2HTestData(NetworkTestUtil.randomString()), null)
				.awaitUninterruptibly();

		// test that it is there
		FutureGet futureGet = nodeB.getDataManager().get(lKey, domainKey, cKey);
		futureGet.awaitUninterruptibly();
		assertNotNull(futureGet.getData());

		// delete it
		nodeA.getDataManager().remove(lKey, domainKey, cKey, null).awaitUninterruptibly();

		// check that it is gone
		futureGet = nodeB.getDataManager().get(lKey, domainKey, cKey);
		futureGet.awaitUninterruptibly();
		assertNull(futureGet.getData());
	}

	@Test
	public void testRemovalMultipleContentKey() throws ClassNotFoundException, IOException,
			NoPeerConnectionException {
		NetworkManager nodeA = network.get(random.nextInt(networkSize / 2));
		NetworkManager nodeB = network.get(random.nextInt(networkSize / 2) + networkSize / 2);

		String locationKey = nodeB.getNodeId();
		Number160 domainKey = H2HConstants.TOMP2P_DEFAULT_KEY;
		Number160 lKey = Number160.createHash(locationKey);
		String contentKey1 = NetworkTestUtil.randomString();
		Number160 cKey1 = Number160.createHash(contentKey1);
		String contentKey2 = NetworkTestUtil.randomString();
		Number160 cKey2 = Number160.createHash(contentKey2);
		String contentKey3 = NetworkTestUtil.randomString();
		Number160 cKey3 = Number160.createHash(contentKey3);

		String testString1 = NetworkTestUtil.randomString();
		String testString2 = NetworkTestUtil.randomString();
		String testString3 = NetworkTestUtil.randomString();

		// insert them
		FuturePut put1 = nodeA.getDataManager().put(lKey, domainKey, cKey1, new H2HTestData(testString1),
				null);
		put1.awaitUninterruptibly();

		FuturePut put2 = nodeA.getDataManager().put(lKey, domainKey, cKey2, new H2HTestData(testString2),
				null);
		put2.awaitUninterruptibly();

		FuturePut put3 = nodeA.getDataManager().put(lKey, domainKey, cKey3, new H2HTestData(testString3),
				null);
		put3.awaitUninterruptibly();

		// check that they are all stored
		FutureGet futureGet = nodeB.getDataManager().get(lKey, domainKey, cKey1);
		futureGet.awaitUninterruptibly();
		assertEquals(testString1, ((H2HTestData) futureGet.getData().object()).getTestString());
		futureGet = nodeB.getDataManager().get(lKey, domainKey, cKey2);
		futureGet.awaitUninterruptibly();
		assertEquals(testString2, ((H2HTestData) futureGet.getData().object()).getTestString());
		futureGet = nodeB.getDataManager().get(lKey, domainKey, cKey3);
		futureGet.awaitUninterruptibly();
		assertEquals(testString3, ((H2HTestData) futureGet.getData().object()).getTestString());

		// remove 2nd one and check that 1st and 3rd are still there
		nodeA.getDataManager().remove(lKey, domainKey, cKey2, null).awaitUninterruptibly();
		futureGet = nodeB.getDataManager().get(lKey, domainKey, cKey1);
		futureGet.awaitUninterruptibly();
		assertEquals(testString1, ((H2HTestData) futureGet.getData().object()).getTestString());
		futureGet = nodeB.getDataManager().get(lKey, domainKey, cKey2);
		futureGet.awaitUninterruptibly();
		assertNull(futureGet.getData());
		futureGet = nodeB.getDataManager().get(lKey, domainKey, cKey3);
		futureGet.awaitUninterruptibly();
		assertEquals(testString3, ((H2HTestData) futureGet.getData().object()).getTestString());

		// remove 3rd one as well and check that they are gone as well
		nodeA.getDataManager().remove(lKey, domainKey, cKey1, null).awaitUninterruptibly();
		nodeA.getDataManager().remove(lKey, domainKey, cKey3, null).awaitUninterruptibly();
		futureGet = nodeB.getDataManager().get(lKey, domainKey, cKey1);
		futureGet.awaitUninterruptibly();
		assertNull(futureGet.getData());
		futureGet = nodeB.getDataManager().get(lKey, domainKey, cKey2);
		futureGet.awaitUninterruptibly();
		assertNull(futureGet.getData());
		futureGet = nodeB.getDataManager().get(lKey, domainKey, cKey3);
		futureGet.awaitUninterruptibly();
		assertNull(futureGet.getData());
	}

	@Test
	public void testChangeProtectionKey() throws NoPeerConnectionException, IOException, InvalidKeyException,
			SignatureException {
		KeyPair keypairOld = EncryptionUtil.generateRSAKeyPair();
		KeyPair keypairNew = EncryptionUtil.generateRSAKeyPair();

		Number160 locationKey = Number160.createHash(NetworkTestUtil.randomString());
		Number160 domainKey = H2HConstants.TOMP2P_DEFAULT_KEY;
		Number160 contentKey = Number160.createHash(NetworkTestUtil.randomString());

		NetworkManager node = network.get(random.nextInt(networkSize));

		// put some initial data
		H2HSharableTestData data = new H2HSharableTestData(NetworkTestUtil.randomString());
		data.generateVersionKey();
		data.setBasedOnKey(Number160.ZERO);
		FuturePut putFuture1 = node.getDataManager()
				.put(locationKey, domainKey, contentKey, data, keypairOld);
		putFuture1.awaitUninterruptibly();
		Assert.assertTrue(putFuture1.isSuccess());

		// change content protection key
		FuturePut changeFuture = node.getDataManager().changeProtectionKey(locationKey, domainKey,
				contentKey, data.getVersionKey(), data.getBasedOnKey(), data.getTimeToLive(), keypairOld,
				keypairNew, data.getHash());
		changeFuture.awaitUninterruptibly();
		Assert.assertTrue(changeFuture.isSuccess());

		// verify if content protection key has been changed
		Data resData = node.getDataManager().get(locationKey, domainKey, contentKey, data.getVersionKey())
				.awaitUninterruptibly().getData();
		Assert.assertTrue(resData.verify(keypairNew.getPublic(), new H2HSignatureFactory()));
	}

	@AfterClass
	public static void cleanAfterClass() {
		NetworkTestUtil.shutdownNetwork(network);
		afterClass();
	}
}
