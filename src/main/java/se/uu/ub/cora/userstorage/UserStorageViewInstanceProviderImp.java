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

import se.uu.ub.cora.gatekeeper.storage.UserStorageView;
import se.uu.ub.cora.gatekeeper.storage.UserStorageViewInstanceProvider;
import se.uu.ub.cora.spider.recordtype.internal.RecordTypeHandlerFactoryImp;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.RecordStorageProvider;
import se.uu.ub.cora.userstorage.convert.DataGroupToUser;
import se.uu.ub.cora.userstorage.convert.DataGroupToUserImp;

public class UserStorageViewInstanceProviderImp implements UserStorageViewInstanceProvider {

	@Override
	public UserStorageView getStorageView() {
		DataGroupToUser dataGroupToUser = new DataGroupToUserImp();
		RecordStorage recordStorage = RecordStorageProvider.getRecordStorage();
		RecordStorage recordStorage2 = RecordStorageProvider.getRecordStorage();
		RecordTypeHandlerFactoryImp recordTypeHandlerFactory = new RecordTypeHandlerFactoryImp(
				recordStorage2);
		return UserStorageViewImp.usingRecordStorageAndRecordTypeHandlerFactory(recordStorage,
				recordTypeHandlerFactory, dataGroupToUser);
	}

	@Override
	public int getOrderToSelectImplementionsBy() {
		return 0;
	}

}
