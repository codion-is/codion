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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.user.User;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;

import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.Configuration.integerValue;

/**
 * A login dialog builder.
 */
public interface LoginDialogBuilder extends DialogBuilder<LoginDialogBuilder> {

	/**
	 * <p>Specifies the default input field columns for the username and password fields.
	 * <p>Note that this only affects the minimum field size, the actual field sizes may be larger
	 * than the specified columns value dictates due to other login panel size constraints.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 8
	 * </ul>
	 * @see javax.swing.JTextField#setColumns(int)
	 * @see #inputFieldColumns(int)
	 */
	PropertyValue<Integer> INPUT_FIELD_COLUMNS =
					integerValue(LoginDialogBuilder.class.getName() + ".inputFieldColumns", 8);

	/**
	 * <p>Specifies whether login dialogs are resizable by default.
	 * <p>When resizable, the dialog can not be made smaller than its packed size.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see javax.swing.JDialog#setResizable(boolean)
	 * @see #resizable(boolean)
	 */
	PropertyValue<Boolean> RESIZABLE =
					booleanValue(LoginDialogBuilder.class.getName() + ".resizable", true);

	/**
	 * @param defaultUser the default user credentials to display
	 * @return this builder instance
	 */
	LoginDialogBuilder defaultUser(@Nullable User defaultUser);

	/**
	 * @param validator the login validator to use
	 * @return this builder instance
	 */
	LoginDialogBuilder validator(LoginValidator validator);

	/**
	 * @param southComponent a component to add to the south of the credentials input fields
	 * @return this builder instance
	 */
	LoginDialogBuilder southComponent(@Nullable JComponent southComponent);

	/**
	 * <p>Note that this only affects the minimum field size, the actual field sizes may be larger
	 * than the specified columns value dictates due to other login panel size constraints.
	 * @param inputFieldColumns the number of columns to display in the input fields (username/password)
	 * @return this builder instance
	 * @see #INPUT_FIELD_COLUMNS
	 */
	LoginDialogBuilder inputFieldColumns(int inputFieldColumns);

	/**
	 * When resizable, the dialog can not be made smaller than its packed size.
	 * @param resizable specifies whether the login dialog is resizable
	 * @return this builder instance
	 * @see #RESIZABLE
	 */
	LoginDialogBuilder resizable(boolean resizable);

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
		 * Validates a login with the given user
		 * @param user the user
		 * @throws Exception in case validation fails
		 */
		void validate(User user) throws Exception;
	}
}
