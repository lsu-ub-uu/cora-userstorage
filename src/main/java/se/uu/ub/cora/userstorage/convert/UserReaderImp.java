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
import java.util.Optional;
import java.util.Set;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.storage.RecordStorage;

public class UserReaderImp implements UserReader {

	private DataRecordGroup userRecordGroup;
	private RecordStorage recordStorage;

	public static UserReader usingRecordStorageA(RecordStorage recordStorage) {
		return new UserReaderImp(recordStorage);
	}

	private UserReaderImp(RecordStorage recordStorage) {
		this.recordStorage = recordStorage;
	}

	@Override
	public User readUser(String userId) {
		userRecordGroup = recordStorage.read("user", userId);

		User user = createAndSetUserId();
		setAppTokenLinkIds(user);
		setActiveStatus(user);
		setNames(user);
		setRoleIds(user);
		setPassword(user);

		return user;
	}

	private void setPassword(User user) {
		if (hasPasswordLink()) {
			String textHashed = readPassword();
			user.passwordId = Optional.of(textHashed);
		}
	}

	private String readPassword() {
		DataRecordLink passwordLink = userRecordGroup
				.getFirstChildOfTypeAndName(DataRecordLink.class, "passwordLink");
		passwordLink.getLinkedRecordId();

		DataRecordGroup systemSecret = recordStorage.read(passwordLink.getLinkedRecordType(),
				passwordLink.getLinkedRecordId());
		return systemSecret.getFirstAtomicValueWithNameInData("secret");
	}

	private boolean hasPasswordLink() {
		return userRecordGroup.containsChildOfTypeAndName(DataRecordLink.class, "passwordLink");
	}

	private User createAndSetUserId() {
		return new User(userRecordGroup.getId());
	}

	private void setActiveStatus(User user) {
		user.active = "active"
				.equals(userRecordGroup.getFirstAtomicValueWithNameInData("activeStatus"));
	}

	private void setAppTokenLinkIds(User user) {
		List<DataGroup> appTokenGroups = userRecordGroup
				.getAllGroupsWithNameInData("userAppTokenGroup");
		getAppTokensForAppTokenGroups(user, appTokenGroups);
	}

	private void getAppTokensForAppTokenGroups(User user, List<DataGroup> appTokenGroups) {
		for (DataGroup appTokenGroup : appTokenGroups) {
			user.appTokenIds.add(extractAppTokenId(appTokenGroup));
		}
	}

	private String extractAppTokenId(DataGroup appTokenGroup) {
		return appTokenGroup.getFirstChildOfTypeAndName(DataRecordLink.class, "appTokenLink")
				.getLinkedRecordId();
	}

	private void setNames(User user) {
		if (userRecordGroup.containsChildWithNameInData("userFirstname")) {
			user.firstName = userRecordGroup.getFirstAtomicValueWithNameInData("userFirstname");
		}
		if (userRecordGroup.containsChildWithNameInData("userLastname")) {
			user.lastName = userRecordGroup.getFirstAtomicValueWithNameInData("userLastname");
		}
	}

	private void setRoleIds(User user) {
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

	public Object onlyForTestGetRecordStorage() {
		return recordStorage;
	}
}
