package se.uu.ub.cora.userstorage.convert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Collections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.gatekeeper.user.User;

public class DataGroupToUserTest {

	private static final String USER_ID = "someId";
	private DataGroupToUser dataGroupToUser;
	private DataGroup userDataGroup;

	@BeforeMethod
	public void beforeMethod() {
		DataProvider.onlyForTestSetDataFactory(null);
		dataGroupToUser = new DataGroupToUserImp();
		userDataGroup = createUserDataGroup();
	}

	@Test
	public void testId() throws Exception {
		User user = dataGroupToUser.groupToUser(userDataGroup);

		assertEquals(user.id, USER_ID);
		assertEquals(user.appTokenIds, Collections.emptyList());
		assertFalse(user.active);
	}

	private DataGroup createUserDataGroup() {
		DataGroup userDataGroup = DataProvider.createGroupUsingNameInData("user");
		DataRecordGroup recordDataGroup = DataProvider
				.createRecordGroupFromDataGroup(userDataGroup);
		recordDataGroup.setId(USER_ID);
		DataGroup userGroup = DataProvider.createGroupFromRecordGroup(recordDataGroup);
		userGroup.addChild(
				DataProvider.createAtomicUsingNameInDataAndValue("activeStatus", "inactive"));
		return userGroup;
	}

	@Test
	public void testAppTokenIds() throws Exception {
		userDataGroup.addChild(createAppTokenGroupWithLinkId("someAppTokenId1"));
		userDataGroup.addChild(createAppTokenGroupWithLinkId("someAppTokenId2"));

		User user = dataGroupToUser.groupToUser(userDataGroup);

		assertEquals(user.appTokenIds.size(), 2);
	}

	private DataGroup createAppTokenGroupWithLinkId(String appTokenId) {
		DataGroup appTokenGroup = DataProvider.createGroupUsingNameInData("userAppTokenGroup");
		appTokenGroup.addChild(createLinkToAppToken(appTokenId));
		return appTokenGroup;
	}

	private DataRecordLink createLinkToAppToken(String appTokenId) {
		return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId("appTokenLink", "appToken",
				appTokenId);
	}

	@Test
	public void testActive() throws Exception {
		userDataGroup.removeFirstChildWithNameInData("activeStatus");
		userDataGroup.addChild(
				DataProvider.createAtomicUsingNameInDataAndValue("activeStatus", "active"));

		User user = dataGroupToUser.groupToUser(userDataGroup);

		assertTrue(user.active);
	}
}
