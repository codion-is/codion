/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.layout.Layouts;
import org.jminor.swing.common.ui.textfield.TextFields;

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
   * @param defaultUser the default user
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
    final Window parentWindow = Windows.getParentWindow(parent);
    final JFrame dummyFrame = parentWindow == null ? createDummyFrame(title, icon) : null;
    final JDialog dialog = pane.createDialog(dummyFrame == null ? parent : dummyFrame, title == null ? Messages.get(Messages.LOGIN) : title);
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
    else {
      throw new CancelException();
    }
  }

  private void initializeUI(final User defaultUser) {
    usernameField.setText(defaultUser == null ? "" : defaultUser.getUsername());
    usernameField.setColumns(DEFAULT_FIELD_COLUMNS);
    TextFields.selectAllOnFocusGained(usernameField);
    passwordField.setText(defaultUser == null ? "" : String.valueOf(defaultUser.getPassword()));
    passwordField.setColumns(DEFAULT_FIELD_COLUMNS);
    TextFields.selectAllOnFocusGained(passwordField);

    final JPanel basePanel = new JPanel(Layouts.createFlexibleGridLayout(GRID_SIZE, GRID_SIZE, true, false));
    basePanel.add(new JLabel(Messages.get(Messages.USERNAME), JLabel.RIGHT));
    basePanel.add(usernameField);
    basePanel.add(new JLabel(Messages.get(Messages.PASSWORD), JLabel.RIGHT));
    basePanel.add(passwordField);

    final JPanel centerPanel = new JPanel(Layouts.createFlowLayout(FlowLayout.CENTER));
    centerPanel.add(basePanel);
    setLayout(Layouts.createBorderLayout());
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
}