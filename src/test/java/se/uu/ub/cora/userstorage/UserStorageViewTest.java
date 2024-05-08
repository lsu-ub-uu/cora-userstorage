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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.spies.DataFactorySpy;
import se.uu.ub.cora.data.spies.DataGroupSpy;
import se.uu.ub.cora.gatekeeper.storage.UserStorageViewException;
import se.uu.ub.cora.gatekeeper.user.AppToken;
import se.uu.ub.cora.gatekeeper.user.User;
import se.uu.ub.cora.storage.Condition;
import se.uu.ub.cora.storage.Filter;
import se.uu.ub.cora.storage.Part;
import se.uu.ub.cora.storage.RecordNotFoundException;
import se.uu.ub.cora.storage.RelationalOperator;
import se.uu.ub.cora.storage.StorageReadResult;
import se.uu.ub.cora.storage.spies.RecordStorageSpy;
import se.uu.ub.cora.userstorage.spies.UserReaderSpy;

public class UserStorageViewTest {
	private static final String ID_FROM_LOGIN = "someIdFromLogin";
	private static final String APP_TOKEN_ID = "someAppTokenId";
	private static final String USER_ID = "someUserId";
	private RecordStorageSpy recordStorage;
	private UserStorageViewImp userStorageView;
	private UserReaderSpy userReader;
	private DataFactorySpy dataFactorySpy;

	@BeforeMethod
	public void beforeMethod() {
		dataFactorySpy = new DataFactorySpy();
		DataProvider.onlyForTestSetDataFactory(dataFactorySpy);

		recordStorage = new RecordStorageSpy();
		userReader = new UserReaderSpy();
		userStorageView = UserStorageViewImp
				.usingRecordStorageAndRecordTypeHandlerFactory(recordStorage, userReader);
	}

	@Test
	public void testInit() throws Exception {
		assertTrue(userStorageView instanceof UserStorageViewImp);
	}

	@Test
	public void testGetUserById_usingDependencies() throws Exception {
		userStorageView.getUserById(USER_ID);

		recordStorage.MCR.assertParameter("read", 0, "id", USER_ID);
		recordStorage.MCR.assertParameterAsEqual("read", 0, "types", List.of("user"));
	}

	@Test
	public void testGetUserById_userContainsInfo() throws Exception {
		DataGroupSpy userDataGroup = new DataGroupSpy();
		userDataGroup.MRV.setReturnValues("getAllGroupsWithNameInData", List.of(),
				"userAppTokenGroup");

		recordStorage.MRV.setReturnValues("read", List.of(userDataGroup), Collections.emptyList(),
				USER_ID);

		User user = userStorageView.getUserById(USER_ID);

		userReader.MCR.assertReturn("groupToUser", 0, user);
	}

	@Test
	public void testGetUserById_throwsError() throws Exception {
		RecordNotFoundException error = RecordNotFoundException.withMessage("error from spy");
		recordStorage.MRV.setAlwaysThrowException("read", error);

		try {
			userStorageView.getUserById(USER_ID);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof UserStorageViewException);
			assertEquals(e.getMessage(),
					"Error reading user with id: " + USER_ID + " from storage.");
			assertSame(e.getCause(), error);
		}
	}

	@Test
	public void testGetUserByIdFromLogin_usingDependencies() throws Exception {
		setupRecordStorageToReturnUserForReadListUsingFilter();

		userStorageView.getUserByIdFromLogin(ID_FROM_LOGIN);

		recordStorage.MCR.assertParameterAsEqual("readList", 0, "types", List.of("user"));
		var filter = recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("readList", 0, "filter");
		assertTrue(filter instanceof Filter);

	}

	private void setupRecordStorageToReturnUserForReadListUsingFilter() {
		DataGroupSpy userDataGroup = new DataGroupSpy();
		userDataGroup.MRV.setReturnValues("getAllGroupsWithNameInData", List.of(),
				"userAppTokenGroup");
		StorageReadResult readResult = new StorageReadResult();
		readResult.listOfDataGroups = List.of(userDataGroup);
		readResult.totalNumberOfMatches = 1;
		recordStorage.MRV.setDefaultReturnValuesSupplier("readList",
				(Supplier<StorageReadResult>) () -> readResult);
	}

	@Test
	public void testGetUserByIdFromLogin_userContainsInfo() throws Exception {
		setupRecordStorageToReturnUserForReadListUsingFilter();

		User user = userStorageView.getUserByIdFromLogin(ID_FROM_LOGIN);

		userReader.MCR.assertReturn("groupToUser", 0, user);
	}

	@Test
	public void testGetUserByIdFromLogin_filterContainsCorrectInfo() throws Exception {
		setupRecordStorageToReturnUserForReadListUsingFilter();

		userStorageView.getUserByIdFromLogin(ID_FROM_LOGIN);

		Filter filter = (Filter) recordStorage.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("readList", 0, "filter");
		assertTrue(filter.fromNoIsDefault());
		assertTrue(filter.toNoIsDefault());
		assertTrue(filter.exclude.isEmpty());

		List<Part> include = filter.include;
		assertEquals(include.size(), 1);

		Part part = include.get(0);
		Condition userIdCondition = part.conditions.get(0);
		assertEquals(userIdCondition.key(), "userId");
		assertEquals(userIdCondition.operator(), RelationalOperator.EQUAL_TO);
		assertEquals(userIdCondition.value(), ID_FROM_LOGIN);
	}

	@Test
	public void testGetUserByIdFromLogin_moreThanOneUserFoundInStorage() throws Exception {
		setupRecordStorageToReturnUserForReadListUsingFilterNumberOfResults(2);

		try {
			userStorageView.getUserByIdFromLogin(ID_FROM_LOGIN);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof UserStorageViewException);
			assertEquals(e.getMessage(),
					"Error reading user with login id: " + ID_FROM_LOGIN + " from storage.");
		}
	}

	@Test
	public void testGetUserByIdFromLogin_noUserFoundInStorage() throws Exception {
		setupRecordStorageToReturnUserForReadListUsingFilterNumberOfResults(0);

		try {
			userStorageView.getUserByIdFromLogin(ID_FROM_LOGIN);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof UserStorageViewException);
			assertEquals(e.getMessage(),
					"Error reading user with login id: " + ID_FROM_LOGIN + " from storage.");
		}
	}

	private void setupRecordStorageToReturnUserForReadListUsingFilterNumberOfResults(
			int totalNumberOfMatches) {
		DataGroupSpy userDataGroup = new DataGroupSpy();
		userDataGroup.MRV.setReturnValues("getAllGroupsWithNameInData", List.of(),
				"userAppTokenGroup");
		StorageReadResult readResult = new StorageReadResult();
		readResult.listOfDataGroups = List.of(userDataGroup, userDataGroup);
		readResult.totalNumberOfMatches = totalNumberOfMatches;
		recordStorage.MRV.setDefaultReturnValuesSupplier("readList",
				(Supplier<StorageReadResult>) () -> readResult);
	}

	@Test
	public void testGetUserByIdFromLogin_throwsError() throws Exception {
		RecordNotFoundException error = RecordNotFoundException.withMessage("error from spy");
		recordStorage.MRV.setAlwaysThrowException("readList", error);

		try {
			userStorageView.getUserByIdFromLogin(ID_FROM_LOGIN);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof UserStorageViewException);
			assertEquals(e.getMessage(),
					"Error reading user with login id: " + ID_FROM_LOGIN + " from storage.");
			assertSame(e.getCause(), error);
		}
	}

	@Test
	public void testGetAppTokenById() throws Exception {
		AppToken appToken = userStorageView.getAppTokenById(APP_TOKEN_ID);

		recordStorage.MCR.assertParameterAsEqual("read", 0, "types", List.of("appToken"));
		recordStorage.MCR.assertParameterAsEqual("read", 0, "id", APP_TOKEN_ID);

		DataGroupSpy appTokenData = (DataGroupSpy) recordStorage.MCR.getReturnValue("read", 0);

		assertEquals(appToken.id, APP_TOKEN_ID);

		appTokenData.MCR.assertParameters("getFirstAtomicValueWithNameInData", 0, "token");
		appTokenData.MCR.assertReturn("getFirstAtomicValueWithNameInData", 0, appToken.tokenString);
	}

	@Test
	public void testGetAppTokenById_throwsError() throws Exception {
		RecordNotFoundException error = RecordNotFoundException.withMessage("error from spy");
		recordStorage.MRV.setThrowException("read", error, List.of("appToken"), APP_TOKEN_ID);

		try {
			userStorageView.getAppTokenById(APP_TOKEN_ID);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof UserStorageViewException);
			assertEquals(e.getMessage(),
					"Error reading appToken with id: " + APP_TOKEN_ID + " from storage.");
			assertSame(e.getCause(), error);
		}
	}
}
