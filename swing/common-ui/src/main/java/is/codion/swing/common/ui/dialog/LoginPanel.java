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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.component.progressbar.ProgressBarBuilder;
import is.codion.swing.common.ui.component.text.PasswordFieldBuilder;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.LoginDialogBuilder.LoginValidator;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import static java.util.Objects.requireNonNull;

/**
 * A JPanel for retrieving login information.
 */
final class LoginPanel extends JPanel {

	static {
		//initialize button captions
		UiManagerDefaults.initialize();
	}

	private static final String PASSWORD_CARD = "password";
	private static final String PROGRESS_CARD = "progress";

	private final JTextField usernameField;
	private final JPasswordField passwordField;
	private final Value<User> user = Value.nullable();
	private final LoginValidator loginValidator;
	private final ImageIcon icon;
	private final Control okControl;
	private final Control cancelControl;
	private final State validating = State.state();

	LoginPanel(User defaultUser, LoginValidator loginValidator, ImageIcon icon, JComponent southComponent, int inputFieldColumns) {
		Value<String> usernameValue = Value.nullable(defaultUser == null ? null : defaultUser.username());
		this.usernameField = TextFieldBuilder.builder(String.class, usernameValue)
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
						.name(Messages.ok())
						.mnemonic(Messages.okMnemonic())
						.enabled(State.and(usernameSpecifiedState(usernameValue), validating.not()))
						.build();
		this.cancelControl = Control.builder()
						.command(this::closeDialog)
						.name(Messages.cancel())
						.mnemonic(Messages.cancelMnemonic())
						.enabled(validating.not())
						.build();
		this.loginValidator = requireNonNull(loginValidator);
		initializeUI(southComponent);
	}

	User user() {
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

	private void initializeUI(JComponent southComponent) {
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

		GridBagConstraints constraints = createGridBagConstraints();
		if (icon != null) {
			add(new JLabel(icon, SwingConstants.CENTER), constraints);
		}
		add(createCredentialsBasePanel(southComponent), constraints);
	}

	private JPanel createCredentialsBasePanel(JComponent southComponent) {
		JPanel credentialsBasePanel = new JPanel(Layouts.borderLayout());
		credentialsBasePanel.add(createCredentialsPanel(), BorderLayout.CENTER);
		if (southComponent != null) {
			credentialsBasePanel.add(southComponent, BorderLayout.SOUTH);
		}

		return credentialsBasePanel;
	}

	private JPanel createCredentialsPanel() {
		return PanelBuilder.builder(Layouts.flexibleGridLayout(2, 2))
						.add(new JLabel(Messages.username(), SwingConstants.RIGHT))
						.add(usernameField)
						.add(new JLabel(Messages.password(), SwingConstants.RIGHT))
						.add(createPasswordProgressPanel())
						.build();
	}

	private JPanel createPasswordProgressPanel() {
		CardLayout passwordProgressLayout = new CardLayout();
		JPanel passwordProgressPanel = PanelBuilder.builder(passwordProgressLayout)
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
		ProgressWorker.builder(this::validateLogin)
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
						.owner(Utilities.parentWindow(this))
						.show(exception);
		requestInitialFocus();
	}

	private void closeDialog() {
		Utilities.disposeParentWindow(this);
	}

	private static State usernameSpecifiedState(Value<String> usernameValue) {
		State usernameSpecified = State.state(usernameValue.isNotNull());
		usernameValue.addConsumer(username -> usernameSpecified.set(username != null));

		return usernameSpecified;
	}

	private static GridBagConstraints createGridBagConstraints() {
		GridBagConstraints constraints = new GridBagConstraints();
		int insets = Layouts.GAP.getOrThrow();
		constraints.insets = new Insets(insets, insets, insets, insets);

		return constraints;
	}
}