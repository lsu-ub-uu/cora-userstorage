/*
 * Copyright 2022 Uppsala University Library
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
import java.util.Set;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.gatekeeper.user.User;

public class DataGroupToUserImp implements DataGroupToUser {

	private DataGroup userGroup;
	private User user;

	@Override
	public User groupToUser(DataGroup dataGroup) {
		this.userGroup = dataGroup;
		setUserId();
		setActiveStatus();
		setAppTokenLinkIds();
		setNames();
		setRoleIds();
		return user;
	}

	private void setUserId() {
		DataRecordGroup userRecordGroup = DataProvider.createRecordGroupFromDataGroup(userGroup);
		user = new User(userRecordGroup.getId());
	}

	private void setActiveStatus() {
		user.active = "active".equals(userGroup.getFirstAtomicValueWithNameInData("activeStatus"));
	}

	private void setAppTokenLinkIds() {
		List<DataGroup> appTokenGroups = userGroup.getAllGroupsWithNameInData("userAppTokenGroup");
		getAppTokensForAppTokenGroups(appTokenGroups);
	}

	private void getAppTokensForAppTokenGroups(List<DataGroup> appTokenGroups) {
		Set<String> userAppTokens = user.appTokenIds;
		for (DataGroup appTokenGroup : appTokenGroups) {
			userAppTokens.add(extractAppTokenId(appTokenGroup));
		}
	}

	private String extractAppTokenId(DataGroup appTokenGroup) {
		return ((DataRecordLink) appTokenGroup.getFirstChildWithNameInData("appTokenLink"))
				.getLinkedRecordId();
	}

	private void setNames() {
		if (userGroup.containsChildWithNameInData("userFirstname")) {
			user.firstName = userGroup.getFirstAtomicValueWithNameInData("userFirstname");
		}
		if (userGroup.containsChildWithNameInData("userLastname")) {
			user.lastName = userGroup.getFirstAtomicValueWithNameInData("userLastname");
		}
	}

	private void setRoleIds() {
		List<DataGroup> roleGroups = userGroup.getAllGroupsWithNameInData("userRole");
		getRolesForRolesGroups(roleGroups);
	}

	private void getRolesForRolesGroups(List<DataGroup> roleGroups) {
		Set<String> userRoles = user.roles;
		for (DataGroup roleGroup : roleGroups) {
			userRoles.add(extractRoleId(roleGroup));
		}
	}

	private String extractRoleId(DataGroup roleGroup) {
		return ((DataRecordLink) roleGroup.getFirstChildWithNameInData("userRole"))
				.getLinkedRecordId();
	}

}
