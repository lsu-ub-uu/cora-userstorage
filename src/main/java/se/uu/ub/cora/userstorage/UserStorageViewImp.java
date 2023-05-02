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
package se.uu.ub.cora.userstorage;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.gatekeeper.storage.UserStorageView;
import se.uu.ub.cora.gatekeeper.storage.UserStorageViewException;
import se.uu.ub.cora.gatekeeper.user.AppToken;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.storage.Condition;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.Part;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.RelationalOperator;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.userstorage.convert.DataGroupToUser;

public class UserStorageViewImp implements UserStorageView {
	private static final String USER = "user";
	private static final String APP_TOKEN = "appToken";
	private static final String ERROR_MESSAGE = "Error reading {0} with id: {1} from storage.";
	private static final String ERROR_MESSAGE_LOGIN_ID = "Error reading user with login id: {0} from storage.";

	private RecordStorage recordStorage;
	protected List<String> userRecordTypeNames = new ArrayList<>();
	private DataGroupToUser dataGroupToUser;

	public static UserStorageViewImp usingRecordStorageAndRecordTypeHandlerFactory(
			RecordStorage recordStorage, DataGroupToUser dataGroupToUser) {
		return new UserStorageViewImp(recordStorage, dataGroupToUser);
	}

	private UserStorageViewImp(RecordStorage recordStorage, DataGroupToUser dataGroupToUser) {
		this.recordStorage = recordStorage;
		this.dataGroupToUser = dataGroupToUser;
	}

	@Override
	public User getUserById(String userId) {
		try {
			return tryToGetUserById(userId);
		} catch (Exception e) {
			String formatErrorMessage = MessageFormat.format(ERROR_MESSAGE, USER, userId);
			throw UserStorageViewException.usingMessageAndException(formatErrorMessage, e);
		}
	}

	private User tryToGetUserById(String userId) {
		var userDataGroup = recordStorage.read(List.of("user"), userId);
		return dataGroupToUser.groupToUser(userDataGroup);
	}

	@Override
	public User getUserByIdFromLogin(String idFromLogin) {
		try {
			return tryToGetUserByIdFromLogin(idFromLogin);
		} catch (UserStorageViewException e) {
			throw e;
		} catch (Exception e) {
			String formatErrorMessage = MessageFormat.format(ERROR_MESSAGE_LOGIN_ID, idFromLogin);
			throw UserStorageViewException.usingMessageAndException(formatErrorMessage, e);
		}
	}

	private User tryToGetUserByIdFromLogin(String idFromLogin) {
		Filter filter = createFilter(idFromLogin);
		StorageReadResult usersList = recordStorage.readList(List.of("user"), filter);
		assertOnlyOneUserFound(usersList, idFromLogin);
		return dataGroupToUser.groupToUser(usersList.listOfDataGroups.get(0));
	}

	private void assertOnlyOneUserFound(StorageReadResult userReadResult, String idFromLogin) {
		if (foundNoneOrMultipleUsers(userReadResult)) {
			String formatErrorMessage = MessageFormat.format(ERROR_MESSAGE_LOGIN_ID, idFromLogin);
			throw UserStorageViewException.usingMessage(formatErrorMessage);
		}
	}

	private boolean foundNoneOrMultipleUsers(StorageReadResult userReadResult) {
		return userReadResult.totalNumberOfMatches != 1;
	}

	private Filter createFilter(String idFromLogin) {
		Filter filter = new Filter();
		Part part = new Part();
		filter.include.add(part);
		Condition userIdCondition = new Condition("userId", RelationalOperator.EQUAL_TO,
				idFromLogin);
		part.conditions.add(userIdCondition);
		return filter;
	}

	@Override
	public AppToken getAppTokenById(String appTokenId) {
		try {
			return tryToGetAppTokenById(appTokenId);
		} catch (Exception e) {
			String formatErrorMessage = MessageFormat.format(ERROR_MESSAGE, APP_TOKEN, appTokenId);
			throw UserStorageViewException.usingMessageAndException(formatErrorMessage, e);
		}
	}

	private AppToken tryToGetAppTokenById(String appTokenId) {
		DataGroup appTokenGroup = recordStorage.read(List.of(APP_TOKEN), appTokenId);
		String tokenValue = appTokenGroup.getFirstAtomicValueWithNameInData("token");
		return new AppToken(appTokenId, tokenValue);
	}

	public RecordStorage onlyForTestGetRecordStorage() {
		return recordStorage;
	}

	public DataGroupToUser onlyForTestGetDataGroupToUser() {
		return dataGroupToUser;
	}

}
