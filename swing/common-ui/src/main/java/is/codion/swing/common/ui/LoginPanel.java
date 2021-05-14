/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixColumnWidths;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixRowHeights;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.textfield.TextFields;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;

/**
 * A JPanel for retrieving login information.
 */
public final class LoginPanel extends JPanel {

  static {
    //initialize button captions
    UiManagerDefaults.initialize();
  }

  private static final int DEFAULT_FIELD_COLUMNS = 8;
  private static final int GRID_SIZE = 2;

  private final JTextField usernameField = new JTextField(DEFAULT_FIELD_COLUMNS);
  private final JPasswordField passwordField = new JPasswordField(DEFAULT_FIELD_COLUMNS);
  private final Value<User> userValue = Value.value();
  private final UserValidator userValidator;
  private final Control okControl;
  private final Control cancelControl;
  private final State validatingState = State.state();

  /**
   * Instantiates a new LoginPanel
   */
  public LoginPanel() {
    this(null);
  }

  /**
   * Instantiates a new LoginPanel
   * @param defaultUser the default user credentials to display
   */
  public LoginPanel(final User defaultUser) {
    this(defaultUser, user -> {});
  }

  /**
   * Instantiates a new LoginPanel
   * @param defaultUser the default user credentials to display
   * @param userValidator the user validator to use
   */
  public LoginPanel(final User defaultUser, final UserValidator userValidator) {
    this(defaultUser, userValidator, null);
  }

  /**
   * Instantiates a new LoginPanel
   * @param defaultUser the default user credentials to display
   * @param userValidator the user validator to use
   * @param southComponent a component to add to the south of the credentials input fields
   */
  public LoginPanel(final User defaultUser, final UserValidator userValidator,
                    final JComponent southComponent) {
    this.okControl = Control.builder()
            .name(Messages.get(Messages.OK))
            .mnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0))
            .command(this::onOkPressed)
            .enabledState(validatingState.getReversedObserver())
            .build();
    this.cancelControl = Control.builder()
            .name(Messages.get(Messages.CANCEL))
            .mnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0))
            .command(() -> Windows.getParentDialog(this).dispose())
            .enabledState(validatingState.getReversedObserver())
            .build();
    this.userValidator = requireNonNull(userValidator);
    initializeUI(defaultUser, southComponent);
  }

  /**
   * @return an Optional containing a User instance, if the OK button was pressed and the user was valid
   */
  public Optional<User> getUser() {
    return userValue.toOptional();
  }

  /**
   * @return the username field
   */
  public JTextField getUsernameField() {
    return usernameField;
  }

  /**
   * @return the password field
   */
  public JPasswordField getPasswordField() {
    return passwordField;
  }

  /**
   * Displays a LoginPanel
   * @param parent the dialog parent component
   * @return a User object based on the values found in this LoginPanel
   * @throws CancelException in case the user cancels
   */
  public User showLoginPanel(final JComponent parent) {
    return showLoginPanel(parent, null, null);
  }

  /**
   * Displays a LoginPanel
   * @param parent the dialog parent component
   * @param title the dialog title
   * @param icon the dialog icon
   * @return a User object based on the values found in this LoginPanel
   * @throws CancelException in case the user cancels
   */
  public User showLoginPanel(final JComponent parent, final String title, final ImageIcon icon) {
    Window parentWindow = Windows.getParentWindow(parent);
    JFrame dummyFrame = null;
    if (parentWindow == null && isWindows()) {
      dummyFrame = createDummyFrame(title, icon);
      parentWindow = dummyFrame;
    }
    if (icon != null) {
      add(new JLabel(icon), BorderLayout.WEST);
    }
    final JDialog dialog = Dialogs.builder()
            .component(this)
            .owner(parentWindow)
            .title(title == null ? Messages.get(Messages.LOGIN) : title)
            .icon(icon)
            .enterAction(okControl)
            .build();
    dialog.setResizable(false);
    dialog.setVisible(true);
    if (dummyFrame != null) {
      dummyFrame.dispose();
    }
    dialog.dispose();

    final Optional<User> user = getUser();
    if (!user.isPresent()) {
      throw new CancelException();
    }

    return user.get();
  }

  /**
   * Validates a login attempt.
   */
  public interface UserValidator {

    /**
     * Valdates a login with the given user
     * @param user the user
     * @throws Exception in case validation fails
     */
    void validate(User user) throws Exception;
  }

  private void initializeUI(final User defaultUser, final JComponent southComponent) {
    usernameField.setText(defaultUser == null ? "" : defaultUser.getUsername());
    usernameField.setColumns(DEFAULT_FIELD_COLUMNS);
    TextFields.selectAllOnFocusGained(usernameField);
    passwordField.setText(defaultUser == null ? "" : String.valueOf(defaultUser.getPassword()));
    passwordField.setColumns(DEFAULT_FIELD_COLUMNS);
    TextFields.selectAllOnFocusGained(passwordField);
    KeyEvents.builder()
            .keyEvent(KeyEvent.VK_BACK_SPACE)
            .modifiers(InputEvent.CTRL_DOWN_MASK)
            .action(Control.control(() -> passwordField.getDocument().remove(0, passwordField.getCaretPosition())))
            .enable(passwordField);

    final JPanel credentialsPanel = new JPanel(Layouts.flexibleGridLayout(GRID_SIZE, GRID_SIZE, FixRowHeights.YES, FixColumnWidths.NO));
    credentialsPanel.add(new JLabel(Messages.get(Messages.USERNAME), JLabel.RIGHT));
    credentialsPanel.add(usernameField);
    credentialsPanel.add(new JLabel(Messages.get(Messages.PASSWORD), JLabel.RIGHT));
    credentialsPanel.add(passwordField);
    final JPanel credentialsBasePanel = new JPanel(Layouts.borderLayout());
    credentialsBasePanel.add(credentialsPanel, BorderLayout.CENTER);
    if (southComponent != null) {
      credentialsBasePanel.add(southComponent, BorderLayout.SOUTH);
    }

    final JPanel centerPanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    centerPanel.add(credentialsPanel);
    setLayout(Layouts.borderLayout());
    add(centerPanel, BorderLayout.CENTER);
    centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    if (usernameField.getText().isEmpty()) {
      Components.addInitialFocusHack(usernameField, Control.control(() -> usernameField.setCaretPosition(usernameField.getText().length())));
    }
    else {
      Components.addInitialFocusHack(passwordField, Control.control(() -> passwordField.setCaretPosition(passwordField.getPassword().length)));
    }
    final JPanel buttonBasePanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    buttonBasePanel.add(Controls.builder()
            .control(okControl)
            .control(cancelControl)
            .build()
            .createHorizontalButtonPanel());
    add(buttonBasePanel, BorderLayout.SOUTH);
  }

  private void onOkPressed() {
    final JDialog parentDialog = Windows.getParentDialog(this);
    final User user = User.user(usernameField.getText(), passwordField.getPassword());
    final SwingWorker<User, Object> worker = new SwingWorker<User, Object>() {
      @Override
      protected User doInBackground() throws Exception {
        userValidator.validate(user);

        return user;
      }
    };
    usernameField.setEnabled(false);
    passwordField.setEnabled(false);
    validatingState.set(true);
    SwingUtilities.invokeLater(() -> {
      worker.execute();
      try {
        userValue.set(worker.get());
        validatingState.set(false);
        parentDialog.dispose();
      }
      catch (final ExecutionException exception) {
        userValue.set(null);
        validatingState.set(false);
        usernameField.setEnabled(true);
        passwordField.setEnabled(true);
        DefaultDialogExceptionHandler.getInstance().displayException(exception.getCause(), parentDialog);
      }
      catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
  }

  private static JFrame createDummyFrame(final String title, final ImageIcon icon) {
    final JFrame frame = new JFrame(title);
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
}