/*
 * Copyright 2022, 2024 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.uu.ub.cora.userstorage.convert;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.Collections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.gatekeeper.user.User;

public class DataGroupToUserTest {

	// private static final String PASSWORD_GROUP_NAME_IN_DATA = "password";
	private static final String USER_ID = "someId";
	private DataGroupToUser dataGroupToUser;
	private DataRecordGroup userDataRecordGroup;

	@BeforeMethod
	public void beforeMethod() {
		DataProvider.onlyForTestSetDataFactory(null);
		dataGroupToUser = new DataGroupToUserImp();
		userDataRecordGroup = createUserDataGroup();
	}

	@Test
	public void testId() throws Exception {
		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.id, USER_ID);
		assertEquals(user.appTokenIds, Collections.emptyList());
		assertFalse(user.active);
	}

	private DataRecordGroup createUserDataGroup() {
		DataRecordGroup recordDataGroup = DataProvider.createRecordGroupUsingNameInData("user");
		recordDataGroup.setId(USER_ID);
		recordDataGroup.addChild(
				DataProvider.createAtomicUsingNameInDataAndValue("activeStatus", "inactive"));
		recordDataGroup.addChild(
				DataProvider.createAtomicUsingNameInDataAndValue("userFirstname", "someFirstName"));
		recordDataGroup.addChild(
				DataProvider.createAtomicUsingNameInDataAndValue("userLastname", "someLastName"));

		return recordDataGroup;
	}

	@Test
	public void testAppTokenIds_NoAppTokenGroup() throws Exception {
		setUpAppTokensGroup();

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.appTokenIds.size(), 0);
	}

	@Test
	public void testAppTokenIds() throws Exception {
		setUpAppTokensGroup("someAppTokenId1", "someAppTokenId2");

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.appTokenIds.size(), 2);
		assertTrue(user.appTokenIds.contains("someAppTokenId1"));
		assertTrue(user.appTokenIds.contains("someAppTokenId2"));

	}

	private void setUpAppTokensGroup(String... appTokenIds) {
		if (appTokenIds.length > 0) {
			userDataRecordGroup.addChild(createAppTokensGroupUsingAppTokens(appTokenIds));
		}
	}

	private DataGroup createAppTokensGroupUsingAppTokens(String... appTokenIds) {
		DataGroup appTokensGroup = DataProvider.createGroupUsingNameInData("appTokens");
		for (String appTokenId : appTokenIds) {
			DataGroup appTokenGroup = DataProvider.createGroupUsingNameInData("appToken");

			appTokenGroup.addChild(createLinkToAppToken(appTokenId));
			appTokensGroup.addChild(appTokenGroup);
		}
		return appTokensGroup;
	}

	private DataRecordLink createLinkToAppToken(String appTokenId) {
		return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId("appTokenLink", "appToken",
				appTokenId);
	}

	@Test
	public void testActive() throws Exception {
		userDataRecordGroup.removeFirstChildWithNameInData("activeStatus");
		userDataRecordGroup.addChild(
				DataProvider.createAtomicUsingNameInDataAndValue("activeStatus", "active"));

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertTrue(user.active);
	}

	@Test
	public void testName() throws Exception {
		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.firstName, "someFirstName");
		assertEquals(user.lastName, "someLastName");
	}

	@Test
	public void testNameNotInData() throws Exception {
		userDataRecordGroup.removeFirstChildWithNameInData("userFirstname");
		userDataRecordGroup.removeFirstChildWithNameInData("userLastname");
		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertNull(user.firstName);
		assertNull(user.lastName);
	}

	@Test
	public void testRoleIds() throws Exception {
		userDataRecordGroup.addChild(createUserRoleGroup("someRoleId1"));
		userDataRecordGroup.addChild(createUserRoleGroup("someRoleId2"));

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.roles.size(), 2);
		assertTrue(user.roles.contains("someRoleId1"));
		assertTrue(user.roles.contains("someRoleId2"));
	}

	private DataGroup createUserRoleGroup(String roleId) {
		DataGroup appTokenGroup = DataProvider.createGroupUsingNameInData("userRole");
		appTokenGroup.addChild(createLinkToRole(roleId));
		return appTokenGroup;
	}

	private DataRecordLink createLinkToRole(String roleId) {
		return DataProvider.createRecordLinkUsingNameInDataAndTypeAndId("userRole", "appToken",
				roleId);
	}

	@Test
	public void testPasswordLinkDoesNotExist() throws Exception {
		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertTrue(user.passwordId.isEmpty());
	}

	@Test
	public void testPasswordLinkExists() throws Exception {
		DataRecordLinkSpy passwordLink = createAndConfigurePasswordLink();
		DataRecordGroupSpy userRecordGroup = createAndConfigureUserRecordGroup(passwordLink);

		User user = dataGroupToUser.groupToUser(userRecordGroup);

		userRecordGroup.MCR.assertParameters("containsChildOfTypeAndName", 0, DataRecordLink.class,
				"passwordLink");
		userRecordGroup.MCR.assertParameters("getFirstChildOfTypeAndName", 0, DataRecordLink.class,
				"passwordLink");

		assertTrue(user.passwordId.isPresent());
		passwordLink.MCR.assertReturn("getLinkedRecordId", 0, user.passwordId.get());
	}

	private DataRecordGroupSpy createAndConfigureUserRecordGroup(DataRecordLinkSpy passwordLink) {
		DataRecordGroupSpy userRecordGroup = new DataRecordGroupSpy();
		userRecordGroup.MRV.setSpecificReturnValuesSupplier("containsChildOfTypeAndName",
				() -> true, DataRecordLink.class, "passwordLink");
		userRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> passwordLink, DataRecordLink.class, "passwordLink");
		return userRecordGroup;
	}

	private DataRecordLinkSpy createAndConfigurePasswordLink() {
		DataRecordLinkSpy passwordLink = new DataRecordLinkSpy();
		passwordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> "someSystemSecretId");
		return passwordLink;
	}
}
