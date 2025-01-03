/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;

import javax.swing.JComponent;

/**
 * A login dialog builder.
 */
public interface LoginDialogBuilder extends DialogBuilder<LoginDialogBuilder> {

	/**
	 * @param defaultUser the default user credentials to display
	 * @return this LoginDialogBuilder instance
	 */
	LoginDialogBuilder defaultUser(User defaultUser);

	/**
	 * @param validator the login validator to use
	 * @return this LoginDialogBuilder instance
	 */
	LoginDialogBuilder validator(LoginValidator validator);

	/**
	 * @param southComponent a component to add to the south of the credentials input fields
	 * @return this LoginDialogBuilder instance
	 */
	LoginDialogBuilder southComponent(JComponent southComponent);

	/**
	 * @param inputFieldColumns the number of columns to display in the input fields (username/password), 8 by default
	 * @return this LoginDialogBuilder instance
	 */
	LoginDialogBuilder inputFieldColumns(int inputFieldColumns);

	/**
	 * @return the logged-in user
	 * @throws CancelException in case the login is cancelled
	 */
	User show();

	/**
	 * Validates a login attempt.
	 */
	interface LoginValidator {

		/**
		 * Valdates a login with the given user
		 * @param user the user
		 * @throws Exception in case validation fails
		 */
		void validate(User user) throws Exception;
	}
}
