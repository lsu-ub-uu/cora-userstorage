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

import java.util.ArrayList;
import java.util.Collections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;

public class UserReaderTest {

	private static final String SOME_USER_ID = "someUserId";
	private UserReader userReader;
	private RecordStorageSpy recordStorage;
	private DataRecordGroupSpy userRecordGroup;

	@BeforeMethod
	public void beforeMethod() {
		DataProvider.onlyForTestSetDataFactory(null);
		createAndSetUpRecordStorage();
		userReader = UserReaderImp.usingRecordStorageA(recordStorage);
	}

	private void createAndSetUpRecordStorage() {
		recordStorage = new RecordStorageSpy();
		userRecordGroup = new DataRecordGroupSpy();
		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> userRecordGroup, "user",
				SOME_USER_ID);

		userRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> SOME_USER_ID);

		setGroupUsingAmountAppTokens(0, "userAppTokenGroup", "appTokenLink", "someAppTokenId");
	}

	private void setGroupUsingAmountAppTokens(int amountOfRecordLinksInGroup, String nameOfGroup,
			String nameOfLink, String prefixLinkedId) {
		ArrayList<DataGroupSpy> apptokens = new ArrayList<DataGroupSpy>();
		for (int recordLinkId = 1; recordLinkId <= amountOfRecordLinksInGroup; recordLinkId++) {
			DataGroupSpy appTokenGroup = createGroupWithOneRecordLinkInIt(nameOfLink,
					prefixLinkedId + recordLinkId);
			apptokens.add(appTokenGroup);
		}
		userRecordGroup.MRV.setSpecificReturnValuesSupplier("getAllGroupsWithNameInData",
				() -> apptokens, nameOfGroup);
	}

	private DataGroupSpy createGroupWithOneRecordLinkInIt(String nameOfLink,
			String prefixLinkedId) {
		DataRecordLink appTokenLink = createRecordLink(prefixLinkedId);

		DataGroupSpy appTokenGroup = new DataGroupSpy();
		appTokenGroup.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> appTokenLink, DataRecordLink.class, nameOfLink);
		return appTokenGroup;
	}

	private DataRecordLink createRecordLink(String linkedIdName) {
		DataRecordLinkSpy recordLink = new DataRecordLinkSpy();
		recordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> linkedIdName);
		return recordLink;
	}

	@Test
	public void testReadUserWithIdInit() throws Exception {

		userReader.readUser(SOME_USER_ID);

		recordStorage.MCR.assertParameters("read", 0, "user", SOME_USER_ID);

	}

	@Test
	public void testId() throws Exception {
		User user = userReader.readUser(SOME_USER_ID);

		assertEquals(user.id, SOME_USER_ID);
		assertEquals(user.appTokenIds, Collections.emptyList());
		assertFalse(user.active);
		assertTrue(user.password.isEmpty());
	}

	@Test
	public void testAppTokenIds() throws Exception {
		setGroupUsingAmountAppTokens(2, "userAppTokenGroup", "appTokenLink", "someAppTokenId");

		User user = userReader.readUser(SOME_USER_ID);

		assertEquals(user.appTokenIds.size(), 2);
		assertTrue(user.appTokenIds.contains("someAppTokenId1"));
		assertTrue(user.appTokenIds.contains("someAppTokenId2"));

	}

	@Test
	public void testActive() throws Exception {
		userRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "active", "activeStatus");

		User user = userReader.readUser(SOME_USER_ID);

		assertTrue(user.active);
	}

	@Test
	public void testName() throws Exception {
		userRecordGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, "userFirstname");
		userRecordGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, "userLastname");
		userRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someFirstName", "userFirstname");
		userRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someLastName", "userLastname");

		User user = userReader.readUser(SOME_USER_ID);

		assertEquals(user.firstName, "someFirstName");
		assertEquals(user.lastName, "someLastName");
	}

	@Test
	public void testNameNotInData() throws Exception {
		User user = userReader.readUser(SOME_USER_ID);

		assertNull(user.firstName);
		assertNull(user.lastName);
	}

	@Test
	public void testRoleIds() throws Exception {
		setGroupUsingAmountAppTokens(2, "userRole", "userRole", "someRoleId");

		User user = userReader.readUser(SOME_USER_ID);

		assertEquals(user.roles.size(), 2);
		assertTrue(user.roles.contains("someRoleId1"));
		assertTrue(user.roles.contains("someRoleId2"));
	}

	@Test
	public void testPassword() throws Exception {
		String passwordLinkLinkedRecordId = "someSystemSecretId";
		DataRecordLinkSpy passwordLink = new DataRecordLinkSpy();
		passwordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> passwordLinkLinkedRecordId);
		passwordLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordType",
				() -> "systemSecret");

		userRecordGroup.MRV.setSpecificReturnValuesSupplier("containsChildOfTypeAndName",
				() -> true, DataRecordLink.class, "passwordLink");
		userRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> passwordLink, DataRecordLink.class, "passwordLink");

		DataRecordGroupSpy systemSecrestRecordGroup = new DataRecordGroupSpy();
		systemSecrestRecordGroup.MRV.setSpecificReturnValuesSupplier(
				"getFirstAtomicValueWithNameInData", () -> "someTextHashed", "secret");

		recordStorage.MRV.setSpecificReturnValuesSupplier("read", () -> systemSecrestRecordGroup,
				"systemSecret", passwordLinkLinkedRecordId);

		User user = userReader.readUser(SOME_USER_ID);

		userRecordGroup.MCR.assertParameters("getFirstChildOfTypeAndName", 0, DataRecordLink.class,
				"passwordLink");
		passwordLink.MCR.assertParameters("getLinkedRecordId", 0);
		recordStorage.MCR.assertParameters("read", 1, "systemSecret", passwordLinkLinkedRecordId);
		recordStorage.MCR.assertNumberOfCallsToMethod("read", 2);
		systemSecrestRecordGroup.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0,
				"secret");

		assertTrue(user.password.isPresent());
		assertEquals(user.password.get(), "someTextHashed");
	}
}
