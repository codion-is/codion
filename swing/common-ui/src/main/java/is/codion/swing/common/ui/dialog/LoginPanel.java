/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
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
  private final Value<User> userValue = Value.value();
  private final LoginValidator loginValidator;
  private final ImageIcon icon;
  private final Control okControl;
  private final Control cancelControl;
  private final Value<String> usernameValue = Value.value();
  private final State usernameSpecifiedState;
  private final State validatingState = State.state();

  LoginPanel(User defaultUser, LoginValidator loginValidator, ImageIcon icon, JComponent southComponent, int inputFieldColumns) {
    this.usernameValue.set(defaultUser == null ? null : defaultUser.username());
    this.usernameSpecifiedState = State.state(usernameValue.isNotNull());
    this.usernameValue.addDataListener(username -> usernameSpecifiedState.set(username != null));
    this.usernameField = Components.textField(usernameValue)
            .columns(inputFieldColumns)
            .selectAllOnFocusGained(true)
            .enabledState(validatingState.reversedObserver())
            .build();
    this.passwordField = Components.passwordField()
            .initialValue(defaultUser == null ? "" : String.valueOf(defaultUser.getPassword()))
            .columns(inputFieldColumns)
            .selectAllOnFocusGained(true)
            .build();
    this.icon = icon;
    this.okControl = Control.builder(this::onOkPressed)
            .caption(Messages.ok())
            .mnemonic(Messages.okMnemonic())
            .enabledState(State.and(usernameSpecifiedState, validatingState.reversedObserver()))
            .build();
    this.cancelControl = Control.builder(this::closeDialog)
            .caption(Messages.cancel())
            .mnemonic(Messages.cancelMnemonic())
            .enabledState(validatingState.reversedObserver())
            .build();
    this.loginValidator = requireNonNull(loginValidator);
    initializeUI(southComponent);
  }

  User user() {
    return userValue.get();
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
    return Components.panel(Layouts.flexibleGridLayout(2, 2))
            .add(new JLabel(Messages.username(), SwingConstants.RIGHT))
            .add(usernameField)
            .add(new JLabel(Messages.password(), SwingConstants.RIGHT))
            .add(createPasswordProgressPanel())
            .build();
  }

  private JPanel createPasswordProgressPanel() {
    CardLayout passwordProgressLayout = new CardLayout();
    JPanel passwordProgressPanel = Components.panel(passwordProgressLayout)
            .add(passwordField, PASSWORD_CARD)
            .add(Components.progressBar()
                    .preferredSize(passwordField.getPreferredSize())
                    .indeterminate(true)
                    .build(), PROGRESS_CARD)
            .build();
    validatingState.addDataListener(validating ->
            passwordProgressLayout.show(passwordProgressPanel, validating ? PROGRESS_CARD : PASSWORD_CARD));

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
    User user = User.user(usernameField.getText(), passwordField.getPassword());
    loginValidator.validate(user);

    return user;
  }

  private void onValidationStarted() {
    validatingState.set(true);
  }

  private void onValidationSuccess(User user) {
    userValue.set(user);
    validatingState.set(false);
    closeDialog();
  }

  private void onValidationFailure(Throwable exception) {
    userValue.set(null);
    validatingState.set(false);
    DefaultDialogExceptionHandler.displayException(exception, Utilities.parentWindow(this));
    requestInitialFocus();
  }

  private void closeDialog() {
    Utilities.disposeParentWindow(this);
  }

  private static GridBagConstraints createGridBagConstraints() {
    GridBagConstraints constraints = new GridBagConstraints();
    int insets = Layouts.HORIZONTAL_VERTICAL_GAP.get();
    constraints.insets = new Insets(insets, insets, insets, insets);

    return constraints;
  }
}