/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.ui.input.AbstractInputProvider;
import org.jminor.framework.domain.Property;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

/**
 * A class for editing a property value for one or more entities at a time.
 */
public class PropertyEditPanel extends JPanel {

  private final Event evtButtonClicked = new Event();

  private final AbstractInputProvider inputProvider;

  private JButton okButton;
  private int buttonValue = -Integer.MAX_VALUE;

  /**
   * Instantiates a new PropertyEditPanel
   * @param property the property to edit
   * @param inputProvider the InputManager to use
   */
  public PropertyEditPanel(final Property property, final AbstractInputProvider inputProvider) {
    if (property == null)
      throw new IllegalArgumentException("Property must be specified");
    if (inputProvider == null)
      throw new IllegalArgumentException("InputProvider must be specified");

    this.inputProvider = inputProvider;
    initUI(property.getCaption());
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

  /**
   * @return the value specified by the input component of this PropertyEditPanel
   */
  public Object getValue() {
    return inputProvider.getValue();
  }

  public Event eventButtonClicked() {
    return evtButtonClicked;
  }

  protected void initUI(final String propertyID) {
    setLayout(new BorderLayout(5,5));
    setBorder(BorderFactory.createTitledBorder(propertyID));
    add(inputProvider.getInputComponent(), BorderLayout.CENTER);
    final JPanel btnBase = new JPanel(new FlowLayout(FlowLayout.CENTER));
    btnBase.add(createButtonPanel());
    add(btnBase, BorderLayout.SOUTH);
  }

  private JPanel createButtonPanel() {
    final JPanel panel = new JPanel(new GridLayout(1,2,5,5));
    panel.add(okButton = createButton(Messages.get(Messages.OK), Messages.get(Messages.OK_MNEMONIC), JOptionPane.OK_OPTION));
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
