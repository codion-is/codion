/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.Util;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

/**
 * A panel for presenting a InputProvider.
 */
public class InputProviderPanel extends JPanel implements InputProvider {

  private final Event evtButtonClicked = new Event();

  private final InputProvider inputProvider;

  private JButton okButton;
  private int buttonValue = -Integer.MAX_VALUE;

  /**
   * Instantiates a new InputProviderPanel
   * @param caption the input panel caption
   * @param inputProvider the InputProvider to use
   */
  public InputProviderPanel(final String caption, final InputProvider inputProvider) {
    Util.rejectNullValue(inputProvider);
    this.inputProvider = inputProvider;
    initUI(caption);
  }

  /**
   * @return true if the edit has been accepted
   */
  public boolean isEditAccepted() {
    return buttonValue == JOptionPane.OK_OPTION;
  }

  /**
   * @return the OK button
   */
  public JButton getOkButton() {
    return okButton;
  }

  /** {@inheritDoc} */
  public Object getValue() {
    return inputProvider.getValue();
  }

  /** {@inheritDoc} */
  public JComponent getInputComponent() {
    return inputProvider.getInputComponent();
  }

  public Event eventButtonClicked() {
    return evtButtonClicked;
  }

  protected void initUI(final String caption) {
    setLayout(new BorderLayout(5,5));
    setBorder(BorderFactory.createTitledBorder(caption));
    add(inputProvider.getInputComponent(), BorderLayout.CENTER);
    final JPanel btnBase = new JPanel(new FlowLayout(FlowLayout.CENTER));
    btnBase.add(createButtonPanel());
    add(btnBase, BorderLayout.SOUTH);
  }

  private JPanel createButtonPanel() {
    final JPanel panel = new JPanel(new GridLayout(1,2,5,5));
    okButton = createButton(Messages.get(Messages.OK), Messages.get(Messages.OK_MNEMONIC), JOptionPane.OK_OPTION);
    panel.add(okButton);
    panel.add(createButton(Messages.get(Messages.CANCEL), Messages.get(Messages.CANCEL_MNEMONIC), JOptionPane.CANCEL_OPTION));

    return panel;
  }

  private JButton createButton(final String caption, final String mnemonic, final int option) {
    final JButton button = new JButton(new AbstractAction(caption) {
      public void actionPerformed(final ActionEvent e) {
        buttonValue = option;
        evtButtonClicked.fire();
      }
    });
    button.setMnemonic(mnemonic.charAt(0));

    return button;
  }
}
