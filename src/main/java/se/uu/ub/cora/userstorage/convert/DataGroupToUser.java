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

import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.gatekeeper.user.User;

public interface DataGroupToUser {

	/**
	 * groupToUser Converts a {@link DataRecordGroup} to {@link User} object.
	 * 
	 * @param userDataRecordGroup
	 *            is the {@link DataRecordGroup} to convert to {@link User}
	 * @return the represenetnation of userDataRecordGroup as {@link User}
	 */
	User groupToUser(DataRecordGroup userDataRecordGroup);

}
