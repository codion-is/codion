/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixColumnWidths;
import is.codion.swing.common.ui.layout.FlexibleGridLayout.FixRowHeights;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.textfield.TextFields;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

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
    initializeUI(defaultUser);
  }

  /**
   * @return a User object based on the values found in this LoginPanel
   */
  public User getUser() {
    return Users.user(usernameField.getText(), passwordField.getPassword());
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
    final JOptionPane pane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, icon);
    Window parentWindow = Windows.getParentWindow(parent);
    JFrame dummyFrame = null;
    if (parentWindow == null && isWindows()) {
      dummyFrame = createDummyFrame(title, icon);
      parentWindow = dummyFrame;
    }
    final JDialog dialog = pane.createDialog(parentWindow == null ? parentWindow : null,
            title == null ? Messages.get(Messages.LOGIN) : title);
    if (icon != null) {
      dialog.setIconImage(icon.getImage());
    }
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.pack();
    Windows.centerWindow(dialog);
    dialog.setResizable(false);
    dialog.setVisible(true);
    if (dummyFrame != null) {
      dummyFrame.dispose();
    }

    if (pane.getValue() != null && pane.getValue().equals(0)) {
      return getUser();
    }

    throw new CancelException();
  }

  private void initializeUI(final User defaultUser) {
    usernameField.setText(defaultUser == null ? "" : defaultUser.getUsername());
    usernameField.setColumns(DEFAULT_FIELD_COLUMNS);
    TextFields.selectAllOnFocusGained(usernameField);
    passwordField.setText(defaultUser == null ? "" : String.valueOf(defaultUser.getPassword()));
    passwordField.setColumns(DEFAULT_FIELD_COLUMNS);
    TextFields.selectAllOnFocusGained(passwordField);
    KeyEvents.addKeyEvent(passwordField, KeyEvent.VK_BACK_SPACE, InputEvent.CTRL_DOWN_MASK,
            Controls.control(() -> passwordField.getDocument().remove(0, passwordField.getCaretPosition())));

    final JPanel basePanel = new JPanel(Layouts.flexibleGridLayout(GRID_SIZE, GRID_SIZE, FixRowHeights.YES, FixColumnWidths.NO));
    basePanel.add(new JLabel(Messages.get(Messages.USERNAME), JLabel.RIGHT));
    basePanel.add(usernameField);
    basePanel.add(new JLabel(Messages.get(Messages.PASSWORD), JLabel.RIGHT));
    basePanel.add(passwordField);

    final JPanel centerPanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    centerPanel.add(basePanel);
    setLayout(Layouts.borderLayout());
    add(centerPanel, BorderLayout.CENTER);
    if (usernameField.getText().length() == 0) {
      Components.addInitialFocusHack(usernameField, Controls.control(() -> usernameField.setCaretPosition(usernameField.getText().length())));
    }
    else {
      Components.addInitialFocusHack(passwordField, Controls.control(() -> passwordField.setCaretPosition(passwordField.getPassword().length)));
    }
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