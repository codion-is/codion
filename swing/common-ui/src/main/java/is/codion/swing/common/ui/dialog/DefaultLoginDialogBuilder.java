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

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.user.User;

import org.jspecify.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;

import static java.util.Objects.requireNonNull;

final class DefaultLoginDialogBuilder extends AbstractDialogBuilder<LoginDialogBuilder> implements LoginDialogBuilder {

	private static final int DEFAULT_FIELD_COLUMNS = 8;

	private @Nullable User defaultUser;
	private LoginValidator validator = new NoLoginValidation();
	private @Nullable JComponent southComponent;
	private int inputFieldColumns = DEFAULT_FIELD_COLUMNS;

	DefaultLoginDialogBuilder() {
		title(Value.nullable(Messages.login()));
	}

	@Override
	public LoginDialogBuilder defaultUser(@Nullable User defaultUser) {
		this.defaultUser = defaultUser;
		return this;
	}

	@Override
	public LoginDialogBuilder validator(LoginValidator validator) {
		this.validator = requireNonNull(validator);
		return this;
	}

	@Override
	public LoginDialogBuilder southComponent(@Nullable JComponent southComponent) {
		this.southComponent = southComponent;
		return this;
	}

	@Override
	public LoginDialogBuilder inputFieldColumns(int inputFieldColumns) {
		this.inputFieldColumns = inputFieldColumns;
		return this;
	}

	@Override
	public User show() {
		JFrame dummyFrame = null;
		if (owner == null && isWindows()) {
			owner = dummyFrame = createDummyFrame(title == null ? null : title.get(), icon);
		}
		LoginPanel loginPanel = new LoginPanel(defaultUser, validator, icon, southComponent, inputFieldColumns);
		OkCancelDialogBuilder dialogBuilder = new DefaultOkCancelDialogBuilder()
						.component(loginPanel)
						.owner(owner)
						.resizable(false)
						.title(title)
						.icon(icon)
						.okAction(loginPanel.okControl())
						.cancelAction(loginPanel.cancelControl())
						.onShown(dialog -> loginPanel.requestInitialFocus());
		onBuildConsumers.forEach(dialogBuilder::onBuild);

		dialogBuilder.show();
		if (dummyFrame != null) {
			dummyFrame.dispose();
		}

		User user = loginPanel.user();
		if (user == null) {
			throw new CancelException();
		}

		return user;
	}

	private static JFrame createDummyFrame(@Nullable String title, @Nullable ImageIcon icon) {
		JFrame frame = new JFrame(title);
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);
		if (icon != null) {
			frame.setIconImage(icon.getImage());
		}

		return frame;
	}

	private static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	private static final class NoLoginValidation implements LoginValidator {

		@Override
		public void validate(User user) {}
	}
}
