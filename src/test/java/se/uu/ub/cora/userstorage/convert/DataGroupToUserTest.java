/*
 * Copyright 2022, 2024, 2025 Uppsala University Library
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
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordGroupSpy;
import se.uu.ub.cora.data.spies.DataRecordLinkSpy;
import se.uu.ub.cora.gatekeeper.user.User;

public class DataGroupToUserTest {
	private static final String USER_ID = "someId";
	private DataGroupToUser dataGroupToUser;
	private DataRecordGroupSpy userDataRecordGroup;

	@BeforeMethod
	public void beforeMethod() {
		DataProvider.onlyForTestSetDataFactory(null);
		dataGroupToUser = new DataGroupToUserImp();
		userDataRecordGroup = createDataRecordGroup();
	}

	@Test
	public void testId() {
		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.id, USER_ID);
		assertEquals(user.appTokenIds, Collections.emptyList());
		assertFalse(user.active);
	}

	private DataRecordGroupSpy createDataRecordGroup() {
		DataRecordGroupSpy dataRecordGroup = new DataRecordGroupSpy();
		dataRecordGroup.MRV.setDefaultReturnValuesSupplier("getId", () -> USER_ID);
		dataRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "inactive", "activeStatus");
		return dataRecordGroup;
	}

	@Test
	public void testAppTokenIds_NoAppTokenGroup() {

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.appTokenIds.size(), 0);
	}

	@Test
	public void testAppTokenIds() {
		DataGroup appTokensGroup = setAppTokenWithTokenIds("someAppTokenId1", "someAppTokenId2");

		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstGroupWithNameInData",
				() -> appTokensGroup, "appTokens");
		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, "appTokens");

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.appTokenIds.size(), 2);
		assertTrue(user.appTokenIds.contains("someAppTokenId1"));
		assertTrue(user.appTokenIds.contains("someAppTokenId2"));
	}

	private DataGroup setAppTokenWithTokenIds(String... tokenIds) {
		DataGroupSpy appTokensGroup = new DataGroupSpy();
		List<DataGroup> appTokenGroups = createApptokenGroupsForTokenIds(tokenIds);
		appTokensGroup.MRV.setSpecificReturnValuesSupplier("getAllGroupsWithNameInData",
				() -> appTokenGroups, "appToken");
		return appTokensGroup;
	}

	private List<DataGroup> createApptokenGroupsForTokenIds(String... tokenIds) {
		List<DataGroup> appTokenGroups = new ArrayList<>();
		for (String tokenId : tokenIds) {
			DataGroupSpy appTokenGroup = createAppTokenGroup(tokenId);
			appTokenGroups.add(appTokenGroup);
		}
		return appTokenGroups;
	}

	private DataGroupSpy createAppTokenGroup(String tokenId) {
		DataGroupSpy appTokenGroup = new DataGroupSpy();
		appTokenGroup.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
				() -> createLinkToAppToken(tokenId), DataRecordLink.class, "appTokenLink");
		return appTokenGroup;
	}

	private DataRecordLinkSpy createLinkToAppToken(String tokenId) {
		DataRecordLinkSpy tokenLink = new DataRecordLinkSpy();
		tokenLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> tokenId);
		return tokenLink;
	}

	@Test
	public void testActive() {
		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "active", "activeStatus");

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertTrue(user.active);
	}

	@Test
	public void testloginId() {
		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someLoginId", "loginId");

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.loginId, "someLoginId");
	}

	@Test
	public void testFirstName() {
		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, "userFirstname");
		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someFirstName", "userFirstname");

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.firstName, "someFirstName");
	}

	@Test
	public void testLastName() {
		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, "userLastname");
		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("getFirstAtomicValueWithNameInData",
				() -> "someLastName", "userLastname");

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.lastName, "someLastName");
	}

	@Test
	public void testNameNotInData() {
		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertNull(user.firstName);
		assertNull(user.lastName);
	}

	@Test
	public void testRoleIds() {
		setUpUserWithRolesIds("someRoleId1", "someRoleId2");

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertEquals(user.roles.size(), 2);
		assertTrue(user.roles.contains("someRoleId1"));
		assertTrue(user.roles.contains("someRoleId2"));
	}

	private void setUpUserWithRolesIds(String... roleIds) {
		List<DataGroup> roleGroups = new ArrayList<>();
		for (String roleId : roleIds) {
			DataGroupSpy roleGroup = new DataGroupSpy();
			roleGroup.MRV.setSpecificReturnValuesSupplier("getFirstChildOfTypeAndName",
					() -> createRoleLink(roleId), DataRecordLink.class, "userRole");
			roleGroups.add(roleGroup);
		}

		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("getAllGroupsWithNameInData",
				() -> roleGroups, "userRole");
	}

	private DataRecordLinkSpy createRoleLink(String roleId) {
		DataRecordLinkSpy roleLink = new DataRecordLinkSpy();
		roleLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> roleId);
		return roleLink;
	}

	@Test
	public void testPasswordLinkDoesNotExist() {
		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertTrue(user.passwordId.isEmpty());
	}

	@Test
	public void testPasswordLinkExists() {
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

	@Test
	public void testPermissionUnitDoesNotExists() {
		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> false, "permissionUnit");

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		assertTrue(user.permissionUnitIds.isEmpty());
	}

	@Test
	public void testPermissionUnitExists() {
		setTwoPermissionUnits();

		User user = dataGroupToUser.groupToUser(userDataRecordGroup);

		Set<String> permissionUnitIds = user.permissionUnitIds;
		assertEquals(permissionUnitIds.size(), 2);
		assertTrue(permissionUnitIds.contains("someId"));
		assertTrue(permissionUnitIds.contains("someId2"));
	}

	private void setTwoPermissionUnits() {
		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("containsChildWithNameInData",
				() -> true, "permissionUnit");

		DataRecordLinkSpy permissionUnitLink = new DataRecordLinkSpy();
		permissionUnitLink.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId", () -> "someId");
		DataRecordLinkSpy permissionUnitLink2 = new DataRecordLinkSpy();
		permissionUnitLink2.MRV.setDefaultReturnValuesSupplier("getLinkedRecordId",
				() -> "someId2");

		userDataRecordGroup.MRV.setSpecificReturnValuesSupplier("getChildrenOfTypeAndName",
				() -> List.of(permissionUnitLink, permissionUnitLink2), DataRecordLink.class,
				"permissionUnit");
	}
}
