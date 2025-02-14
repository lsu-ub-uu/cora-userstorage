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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.gatekeeper.user.User;

public class DataGroupToUserImp implements DataGroupToUser {

	private static final String PASSWORD_LINK_NAME_IN_DATA = "passwordLink";
	private DataRecordGroup userRecordGroup;
	private User user;

	@Override
	public User groupToUser(DataRecordGroup dataGroup) {
		userRecordGroup = dataGroup;
		user = createUserAndSetUserId();
		setActiveStatus();
		setAppTokenLinkIds();
		addLoginId();
		setNames();
		setRoleIds();
		setPassword();
		setPermissionUnitIds();
		return user;
	}

	private User createUserAndSetUserId() {
		return new User(userRecordGroup.getId());
	}

	private void setActiveStatus() {
		user.active = "active"
				.equals(userRecordGroup.getFirstAtomicValueWithNameInData("activeStatus"));
	}

	private void setAppTokenLinkIds() {
		if (userRecordGroup.containsChildWithNameInData("appTokens")) {
			DataGroup appTokensGroup = userRecordGroup.getFirstGroupWithNameInData("appTokens");
			addAllSystemSecretsIdToUserAppTokens(user, appTokensGroup);
		}
	}

	private void addAllSystemSecretsIdToUserAppTokens(User user, DataGroup appTokensGroup) {
		List<DataGroup> appTokenGroups = appTokensGroup.getAllGroupsWithNameInData("appToken");
		for (DataGroup appTokenGroup : appTokenGroups) {
			String linkedRecordId = getSystemSecretIdFromAppTokenGroup(appTokenGroup);
			user.appTokenIds.add(linkedRecordId);
		}
	}

	private String getSystemSecretIdFromAppTokenGroup(DataGroup appTokenGroup) {
		DataRecordLink systemSecretLink = appTokenGroup
				.getFirstChildOfTypeAndName(DataRecordLink.class, "appTokenLink");
		return systemSecretLink.getLinkedRecordId();
	}

	private void setNames() {
		possiblySetFirstname(user);
		possiblySetLastname(user);
	}

	private void addLoginId() {
		user.loginId = userRecordGroup.getFirstAtomicValueWithNameInData("loginId");
	}

	private void possiblySetFirstname(User user) {
		if (userRecordGroup.containsChildWithNameInData("userFirstname")) {
			user.firstName = userRecordGroup.getFirstAtomicValueWithNameInData("userFirstname");
		}
	}

	private void possiblySetLastname(User user) {
		if (userRecordGroup.containsChildWithNameInData("userLastname")) {
			user.lastName = userRecordGroup.getFirstAtomicValueWithNameInData("userLastname");
		}
	}

	private void setRoleIds() {
		List<DataGroup> roleGroups = userRecordGroup.getAllGroupsWithNameInData("userRole");
		getRolesForRolesGroups(user, roleGroups);
	}

	private void getRolesForRolesGroups(User user, List<DataGroup> roleGroups) {
		Set<String> userRoles = user.roles;
		for (DataGroup roleGroup : roleGroups) {
			userRoles.add(extractRoleId(roleGroup));
		}
	}

	private String extractRoleId(DataGroup roleGroup) {
		return roleGroup.getFirstChildOfTypeAndName(DataRecordLink.class, "userRole")
				.getLinkedRecordId();
	}

	private void setPassword() {
		if (hasPassword()) {
			String systemSecretId = getPasswordRecordLinkId();
			user.passwordId = Optional.of(systemSecretId);
		}
	}

	private boolean hasPassword() {
		return userRecordGroup.containsChildOfTypeAndName(DataRecordLink.class,
				PASSWORD_LINK_NAME_IN_DATA);
	}

	private String getPasswordRecordLinkId() {
		DataRecordLink passwordLink = userRecordGroup
				.getFirstChildOfTypeAndName(DataRecordLink.class, PASSWORD_LINK_NAME_IN_DATA);
		return passwordLink.getLinkedRecordId();
	}

	private void setPermissionUnitIds() {
		if (hasPermissionUnits()) {
			addPermissionsUnitsToUserFromRecord(user);
		}
	}

	private boolean hasPermissionUnits() {
		return userRecordGroup.containsChildWithNameInData("permissionUnit");
	}

	private void addPermissionsUnitsToUserFromRecord(User user) {
		var permissionUnitLinks = readPermissionUnitsFromRecord();
		addPermissionUnitsToUser(user, permissionUnitLinks);
	}

	private List<DataRecordLink> readPermissionUnitsFromRecord() {
		return userRecordGroup.getChildrenOfTypeAndName(DataRecordLink.class, "permissionUnit");
	}

	private void addPermissionUnitsToUser(User user, List<DataRecordLink> permissionUnitLinks) {
		for (DataRecordLink permissionUnitLink : permissionUnitLinks) {
			user.permissionUnitIds.add(permissionUnitLink.getLinkedRecordId());
		}
	}
}
