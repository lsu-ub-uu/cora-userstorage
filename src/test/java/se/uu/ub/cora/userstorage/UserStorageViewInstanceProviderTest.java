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
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.bookkeeper.recordtype.RecordTypeHandlerFactoryImp;
import se.uu.ub.cora.gatekeeper.storage.UserStorageViewInstanceProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.logger.spies.LoggerFactorySpy;
import se.uu.ub.cora.storage.RecordStorageProvider;
import se.uu.ub.cora.storage.spies.RecordStorageInstanceProviderSpy;
import se.uu.ub.cora.userstorage.convert.DataGroupToUser;
import se.uu.ub.cora.userstorage.convert.DataGroupToUserImp;

public class UserStorageViewInstanceProviderTest {
	LoggerFactorySpy loggerFactorySpy = new LoggerFactorySpy();
	RecordStorageInstanceProviderSpy recordStorageInstanceProvider;
	private UserStorageViewInstanceProvider instanceProvider;

	@BeforeMethod
	public void beforeMethod() {
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		recordStorageInstanceProvider = new RecordStorageInstanceProviderSpy();
		RecordStorageProvider
				.onlyForTestSetRecordStorageInstanceProvider(recordStorageInstanceProvider);
		instanceProvider = new UserStorageViewInstanceProviderImp();
	}

	@Test
	public void testGetStorageView() throws Exception {
		UserStorageViewImp appTokenStorageView = (UserStorageViewImp) instanceProvider
				.getStorageView();

		assertTrue(appTokenStorageView instanceof UserStorageViewImp);
		recordStorageInstanceProvider.MCR.assertReturn("getRecordStorage", 0,
				appTokenStorageView.onlyForTestGetRecordStorage());
	}

	@Test
	public void testCreatedRecordTypeHandlerFactory() throws Exception {
		UserStorageViewImp appTokenStorageView = (UserStorageViewImp) instanceProvider
				.getStorageView();

		RecordTypeHandlerFactoryImp recordTypeHandlerFactory = (RecordTypeHandlerFactoryImp) appTokenStorageView
				.onlyForTestGetRecordTypeHandlerFactory();
		assertTrue(recordTypeHandlerFactory instanceof RecordTypeHandlerFactoryImp);
	}

	@Test
	public void testCreatedDataGroupToUser() throws Exception {
		UserStorageViewImp appTokenStorageView = (UserStorageViewImp) instanceProvider
				.getStorageView();
		DataGroupToUser dataGroupToUser = appTokenStorageView.onlyForTestGetDataGroupToUser();
		assertTrue(dataGroupToUser instanceof DataGroupToUserImp);
	}

	@Test
	public void testGetOrderToSelectImplemtationsBy() throws Exception {
		assertEquals(instanceProvider.getOrderToSelectImplementionsBy(), 0);
	}
}
