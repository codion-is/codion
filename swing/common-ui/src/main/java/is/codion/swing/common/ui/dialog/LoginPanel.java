/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.LoginDialogBuilder.LoginValidator;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

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

  private final JTextField usernameField = new JTextField(DEFAULT_FIELD_COLUMNS);
  private final JPasswordField passwordField = new JPasswordField(DEFAULT_FIELD_COLUMNS);
  private final Value<User> userValue = Value.value();
  private final LoginValidator loginValidator;
  private final ImageIcon icon;
  private final Control okControl;
  private final Control cancelControl;
  private final State validatingState = State.state();

  LoginPanel(final User defaultUser, final LoginValidator loginValidator,
             final ImageIcon icon, final JComponent southComponent) {
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
    initializeUI(defaultUser, southComponent);
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

  private void initializeUI(final User defaultUser, final JComponent southComponent) {
    usernameField.setText(defaultUser == null ? "" : defaultUser.getUsername());
    usernameField.setColumns(DEFAULT_FIELD_COLUMNS);
    usernameField.addFocusListener(new SelectAllListener(usernameField));
    Utilities.linkToEnabledState(validatingState.getReversedObserver(), usernameField);
    passwordField.setText(defaultUser == null ? "" : String.valueOf(defaultUser.getPassword()));
    passwordField.setColumns(DEFAULT_FIELD_COLUMNS);
    passwordField.addFocusListener(new SelectAllListener(passwordField));
    KeyEvents.builder(KeyEvent.VK_BACK_SPACE)
            .modifiers(InputEvent.CTRL_DOWN_MASK)
            .action(Control.control(() -> passwordField.getDocument().remove(0, passwordField.getCaretPosition())))
            .enable(passwordField);

    final JProgressBar progressBar = new JProgressBar();
    progressBar.setPreferredSize(passwordField.getPreferredSize());
    progressBar.setIndeterminate(true);
    final CardLayout passwordProgressLayout = new CardLayout();
    final JPanel passwordProgressPanel = new JPanel(passwordProgressLayout);
    passwordProgressPanel.add(passwordField, PASSWORD_CARD);
    passwordProgressPanel.add(progressBar, PROGRESS_CARD);
    validatingState.addDataListener(validating ->
            passwordProgressLayout.show(passwordProgressPanel, validating ? PROGRESS_CARD : PASSWORD_CARD));

    final JPanel credentialsPanel = new JPanel(Layouts.flexibleGridLayout()
            .rowsColumns(2, 2)
            .fixRowHeights(true)
            .build());
    credentialsPanel.add(new JLabel(Messages.get(Messages.USERNAME), SwingConstants.RIGHT));
    credentialsPanel.add(usernameField);
    credentialsPanel.add(new JLabel(Messages.get(Messages.PASSWORD), SwingConstants.RIGHT));
    credentialsPanel.add(passwordProgressPanel);
    final JPanel credentialsBasePanel = new JPanel(Layouts.borderLayout());
    credentialsBasePanel.add(credentialsPanel, BorderLayout.CENTER);
    if (southComponent != null) {
      credentialsBasePanel.add(southComponent, BorderLayout.SOUTH);
    }

    final JPanel centerPanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 20));
    centerPanel.add(credentialsBasePanel);
    setLayout(new BorderLayout(0, 0));
    add(centerPanel, BorderLayout.CENTER);
    if (usernameField.getText().isEmpty()) {
      Utilities.addInitialFocusHack(usernameField, Control.control(() -> usernameField.setCaretPosition(usernameField.getText().length())));
    }
    else {
      Utilities.addInitialFocusHack(passwordField, Control.control(() -> passwordField.setCaretPosition(passwordField.getPassword().length)));
    }
    if (icon != null) {
      final JLabel label = new JLabel(icon, SwingConstants.CENTER);
      label.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));
      add(label, BorderLayout.WEST);
    }
  }

  private void onOkPressed() {
    ProgressWorker.builder(this::validateLogin)
            .onStarted(this::onValidationStarted)
            .onResult(this::onValidationSuccess)
            .onException(this::onValidationFailure)
            .execute();
  }

  private User validateLogin() throws Exception {
    final User user = User.user(usernameField.getText(), passwordField.getPassword());
    loginValidator.validate(user);

    return user;
  }

  private void onValidationStarted() {
    validatingState.set(true);
  }

  private void onValidationSuccess(final User user) {
    userValue.set(user);
    validatingState.set(false);
    closeDialog();
  }

  private void onValidationFailure(final Throwable exception) {
    userValue.set(null);
    validatingState.set(false);
    DefaultDialogExceptionHandler.getInstance().displayException(exception, Windows.getParentWindow(this));
  }

  private void closeDialog() {
    Windows.getParentDialog(this).dispose();
  }

  private static final class SelectAllListener extends FocusAdapter {

    private final JTextComponent textComponent;

    private SelectAllListener(final JTextComponent textComponent) {
      this.textComponent = textComponent;
    }

    @Override
    public void focusGained(final FocusEvent e) {
      textComponent.selectAll();
    }

    @Override
    public void focusLost(final FocusEvent e) {
      textComponent.select(0, 0);
    }
  }
}