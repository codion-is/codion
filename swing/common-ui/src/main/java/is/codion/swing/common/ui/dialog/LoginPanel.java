/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.LoginDialogBuilder.LoginValidator;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
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

  private static final int DEFAULT_FIELD_COLUMNS = 8;
  private static final String PASSWORD_CARD = "password";
  private static final String PROGRESS_CARD = "progress";

  private final JTextField usernameField;
  private final JPasswordField passwordField;
  private final Value<User> userValue = Value.value();
  private final LoginValidator loginValidator;
  private final ImageIcon icon;
  private final Control okControl;
  private final Control cancelControl;
  private final State validatingState = State.state();

  LoginPanel(User defaultUser, LoginValidator loginValidator, ImageIcon icon, JComponent southComponent) {
    this.usernameField = Components.textField()
            .initialValue(defaultUser == null ? "" : defaultUser.getUsername())
            .columns(DEFAULT_FIELD_COLUMNS)
            .selectAllOnFocusGained(true)
            .enabledState(validatingState.getReversedObserver())
            .build();
    this.passwordField = Components.passwordField()
            .initialValue(defaultUser == null ? "" : String.valueOf(defaultUser.getPassword()))
            .columns(DEFAULT_FIELD_COLUMNS)
            .selectAllOnFocusGained(true)
            .build();
    this.icon = icon;
    this.okControl = Control.builder(this::onOkPressed)
            .caption(Messages.get(Messages.OK))
            .mnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0))
            .enabledState(validatingState.getReversedObserver())
            .build();
    this.cancelControl = Control.builder(this::closeDialog)
            .caption(Messages.get(Messages.CANCEL))
            .mnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0))
            .enabledState(validatingState.getReversedObserver())
            .build();
    this.loginValidator = requireNonNull(loginValidator);
    initializeUI(southComponent);
  }

  User getUser() {
    return userValue.get();
  }

  Control getOkControl() {
    return okControl;
  }

  Control getCancelControl() {
    return cancelControl;
  }

  private void initializeUI(JComponent southComponent) {
    JProgressBar progressBar = new JProgressBar();
    progressBar.setPreferredSize(passwordField.getPreferredSize());
    progressBar.setIndeterminate(true);
    CardLayout passwordProgressLayout = new CardLayout();
    JPanel passwordProgressPanel = Components.panel(passwordProgressLayout)
            .add(passwordField, PASSWORD_CARD)
            .add(progressBar, PROGRESS_CARD)
            .build();
    validatingState.addDataListener(validating ->
            passwordProgressLayout.show(passwordProgressPanel, validating ? PROGRESS_CARD : PASSWORD_CARD));

    JPanel credentialsPanel = Components.panel(Layouts.flexibleGridLayout(2, 2))
            .add(new JLabel(Messages.get(Messages.USERNAME), SwingConstants.RIGHT))
            .add(usernameField)
            .add(new JLabel(Messages.get(Messages.PASSWORD), SwingConstants.RIGHT))
            .add(passwordProgressPanel)
            .build();
    JPanel credentialsBasePanel = new JPanel(Layouts.borderLayout());
    credentialsBasePanel.add(credentialsPanel, BorderLayout.CENTER);
    if (southComponent != null) {
      credentialsBasePanel.add(southComponent, BorderLayout.SOUTH);
    }
    if (usernameField.getText().isEmpty()) {
      Utilities.addInitialFocusHack(usernameField, Control.control(() -> usernameField.setCaretPosition(usernameField.getText().length())));
    }
    else {
      Utilities.addInitialFocusHack(passwordField, Control.control(() -> passwordField.setCaretPosition(passwordField.getPassword().length)));
    }
    GridBagConstraints constraints = new GridBagConstraints();
    int insets = Layouts.HORIZONTAL_VERTICAL_GAP.get();
    constraints.insets = new Insets(insets, insets, insets, insets);
    setLayout(new GridBagLayout());
    if (icon != null) {
      add(new JLabel(icon, SwingConstants.CENTER), constraints);
    }
    add(credentialsBasePanel, constraints);
    setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
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
    DefaultDialogExceptionHandler.getInstance().displayException(exception, Windows.getParentWindow(this).orElse(null));
  }

  private void closeDialog() {
    Windows.getParentDialog(this).ifPresent(JDialog::dispose);
  }
}