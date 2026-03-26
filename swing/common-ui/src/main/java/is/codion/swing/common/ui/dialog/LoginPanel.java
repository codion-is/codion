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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.user.User;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.UIManagerDefaults;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.panel.BorderLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.component.progressbar.ProgressBarBuilder;
import is.codion.swing.common.ui.component.text.PasswordFieldBuilder;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.LoginDialogBuilder.LoginValidator;

import org.jspecify.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import static is.codion.common.reactive.state.State.present;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.BorderLayout.WEST;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.SwingConstants.CENTER;
import static javax.swing.SwingConstants.LEADING;

/**
 * A JPanel for retrieving login information.
 */
final class LoginPanel extends JPanel {

	static {
		//initialize button captions
		UIManagerDefaults.initialize();
	}

	private static final String PASSWORD_CARD = "password";
	private static final String PROGRESS_CARD = "progress";

	private final JTextField usernameField;
	private final JPasswordField passwordField;
	private final Value<User> user = Value.nullable();
	private final LoginValidator loginValidator;
	private final @Nullable ImageIcon icon;
	private final Control okControl;
	private final Control cancelControl;
	private final State validating = State.state();

	LoginPanel(@Nullable User defaultUser, LoginValidator loginValidator, @Nullable ImageIcon icon, @Nullable JComponent southComponent, int inputFieldColumns) {
		Value<String> username = Value.nullable(defaultUser == null ? null : defaultUser.username());
		this.usernameField = TextFieldBuilder.builder()
						.valueClass(String.class)
						.link(username)
						.columns(inputFieldColumns)
						.selectAllOnFocusGained(true)
						.enabled(validating.not())
						.build();
		this.passwordField = PasswordFieldBuilder.builder()
						.value(defaultUser == null ? "" : String.valueOf(defaultUser.password()))
						.columns(inputFieldColumns)
						.selectAllOnFocusGained(true)
						.build();
		this.icon = icon;
		this.okControl = Control.builder()
						.command(this::onOkPressed)
						.caption(Messages.ok())
						.mnemonic(Messages.okMnemonic())
						.enabled(State.and(present(username), validating.not()))
						.build();
		this.cancelControl = Control.builder()
						.command(this::closeDialog)
						.caption(Messages.cancel())
						.mnemonic(Messages.cancelMnemonic())
						.enabled(validating.not())
						.build();
		this.loginValidator = requireNonNull(loginValidator);
		initializeUI(southComponent);
	}

	@Nullable User user() {
		return user.get();
	}

	Control okControl() {
		return okControl;
	}

	Control cancelControl() {
		return cancelControl;
	}

	void requestInitialFocus() {
		if (usernameField.getText().isEmpty()) {
			usernameField.requestFocusInWindow();
		}
		else {
			passwordField.requestFocusInWindow();
		}
	}

	private void initializeUI(@Nullable JComponent southComponent) {
		BorderLayoutPanelBuilder credentialsPanel = borderLayoutPanel()
						.west(gridLayoutPanel(2, 1)
										.add(new JLabel(Messages.username(), LEADING))
										.add(new JLabel(Messages.password(), LEADING)))
						.center(gridLayoutPanel(2, 1)
										.add(usernameField)
										.add(createPasswordProgressPanel()));
		if (southComponent != null) {
			credentialsPanel.south(southComponent);
		}
		setLayout(borderLayout());
		setBorder(createEmptyBorder(10, 10, 0, 10));
		if (icon != null) {
			add(new JLabel(icon, CENTER), WEST);
		}
		add(panel()
						.layout(new GridBagLayout())
						.add(credentialsPanel.build(), credentialsPanelConstraints())
						.build(), CENTER);
	}

	private JPanel createPasswordProgressPanel() {
		CardLayout passwordProgressLayout = new CardLayout();
		JPanel passwordProgressPanel = PanelBuilder.builder()
						.layout(passwordProgressLayout)
						.add(passwordField, PASSWORD_CARD)
						.add(ProgressBarBuilder.builder()
										.preferredSize(passwordField.getPreferredSize())
										.build(), PROGRESS_CARD)
						.build();
		validating.addConsumer(isValidating ->
						passwordProgressLayout.show(passwordProgressPanel, isValidating ? PROGRESS_CARD : PASSWORD_CARD));

		return passwordProgressPanel;
	}

	private void onOkPressed() {
		ProgressWorker.builder()
						.task(this::validateLogin)
						.onStarted(this::onValidationStarted)
						.onResult(this::onValidationSuccess)
						.onException(this::onValidationFailure)
						.execute();
	}

	private User validateLogin() throws Exception {
		User currentUser = User.user(usernameField.getText(), passwordField.getPassword());
		loginValidator.validate(currentUser);

		return currentUser;
	}

	private void onValidationStarted() {
		validating.set(true);
	}

	private void onValidationSuccess(User user) {
		this.user.set(user);
		validating.set(false);
		closeDialog();
	}

	private void onValidationFailure(Exception exception) {
		user.clear();
		validating.set(false);
		new DefaultExceptionDialogBuilder()
						.owner(Ancestor.window().of(this).get())
						.show(exception);
		requestInitialFocus();
	}

	private void closeDialog() {
		Ancestor.window().of(this).dispose();
	}

	private static GridBagConstraints credentialsPanelConstraints() {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;

		return constraints;
	}
}