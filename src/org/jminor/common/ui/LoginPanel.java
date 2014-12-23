/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

/**
 * A JPanel for retrieving login information.
 */
public final class LoginPanel extends JPanel {

  private static final int DEFAULT_FIELD_COLUMNS = 8;
  private static final int GRID_SIZE = 2;

  private final JTextField usernameField = new JTextField(DEFAULT_FIELD_COLUMNS);
  private final JPasswordField passwordField = new JPasswordField(DEFAULT_FIELD_COLUMNS);

  /**
   * Instantiates a new LoginPanel
   * @param defaultUser the default user
   */
  public LoginPanel(final User defaultUser) {
    initUI(defaultUser);
  }

  /**
   * @return a User object based on the values found in this LoginPanel
   */
  public User getUser() {
    return new User(usernameField.getText(), new String(passwordField.getPassword()));
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
  public User showLoginPanel(final JComponent parent, final String title, final Icon icon) {
    final JOptionPane pane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, icon);
    final JDialog dialog = pane.createDialog(parent, title == null ? Messages.get(Messages.LOGIN) : title);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.pack();
    UiUtil.centerWindow(dialog);
    dialog.setResizable(false);
    dialog.setVisible(true);

    if (pane.getValue() != null && pane.getValue().equals(0)) {
      return getUser();
    }
    else {
      throw new CancelException();
    }
  }

  private void initUI(final User defaultUser) {
    usernameField.setText(defaultUser == null ? "" : defaultUser.getUsername());
    usernameField.setColumns(DEFAULT_FIELD_COLUMNS);
    UiUtil.selectAllOnFocusGained(usernameField);
    passwordField.setText(defaultUser == null ? "" : defaultUser.getPassword());
    passwordField.setColumns(DEFAULT_FIELD_COLUMNS);
    UiUtil.selectAllOnFocusGained(passwordField);

    final JPanel basePanel = new JPanel(UiUtil.createFlexibleGridLayout(GRID_SIZE, GRID_SIZE, true, false));
    basePanel.add(new JLabel(Messages.get(Messages.USERNAME), JLabel.RIGHT));
    basePanel.add(usernameField);
    basePanel.add(new JLabel(Messages.get(Messages.PASSWORD), JLabel.RIGHT));
    basePanel.add(passwordField);

    final JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    centerPanel.add(basePanel);
    setLayout(UiUtil.createBorderLayout());
    add(centerPanel, BorderLayout.CENTER);
    if (usernameField.getText().length() == 0) {
      UiUtil.addInitialFocusHack(usernameField, new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          usernameField.setCaretPosition(usernameField.getText().length());
        }
      });
    }
    else {
      UiUtil.addInitialFocusHack(passwordField, new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          passwordField.setCaretPosition(passwordField.getPassword().length);
        }
      });
    }
  }
}