/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

/**
 * A panel for presenting a InputProvider.
 */
public final class InputProviderPanel extends JPanel implements InputProvider {

  private static final int COLUMNS = 2;

  private final Event buttonClickedEvent = Events.event();

  private final InputProvider inputProvider;

  private JButton okButton;
  private int buttonValue = -Integer.MAX_VALUE;

  /**
   * Instantiates a new InputProviderPanel
   * @param caption the input panel caption
   * @param inputProvider the InputProvider to use
   */
  public InputProviderPanel(final String caption, final InputProvider inputProvider) {
    Util.rejectNullValue(inputProvider, "inputProvider");
    this.inputProvider = inputProvider;
    initUI(caption);
  }

  /**
   * @return true if the input has been accepted, that is, the OK button has been clicked
   */
  public boolean isInputAccepted() {
    return buttonValue == JOptionPane.OK_OPTION;
  }

  /**
   * @return the OK button
   */
  public JButton getOkButton() {
    return okButton;
  }

  /** {@inheritDoc} */
  @Override
  public Object getValue() {
    return inputProvider.getValue();
  }

  /** {@inheritDoc} */
  @Override
  public JComponent getInputComponent() {
    return inputProvider.getInputComponent();
  }

  /**
   * @return an EventObserver notified each time the OK button is clicked
   */
  public EventObserver getButtonClickObserver() {
    return buttonClickedEvent.getObserver();
  }

  /**
   * @param listener a listener notified each time the OK button is clicked
   */
  public void addButtonClickListener(final EventListener listener) {
    getButtonClickObserver().addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeButtonClickListener(final EventListener listener) {
    buttonClickedEvent.removeListener(listener);
  }

  private void initUI(final String caption) {
    setLayout(UiUtil.createBorderLayout());
    if (caption != null) {
      setBorder(BorderFactory.createTitledBorder(caption));
    }
    add(inputProvider.getInputComponent(), BorderLayout.CENTER);
    final JPanel btnBase = new JPanel(new FlowLayout(FlowLayout.CENTER));
    btnBase.add(createButtonPanel());
    add(btnBase, BorderLayout.SOUTH);
  }

  private JPanel createButtonPanel() {
    final JPanel panel = new JPanel(UiUtil.createGridLayout(1, COLUMNS));
    okButton = createButton(Messages.get(Messages.OK), Messages.get(Messages.OK_MNEMONIC), JOptionPane.OK_OPTION);
    panel.add(okButton);
    panel.add(createButton(Messages.get(Messages.CANCEL), Messages.get(Messages.CANCEL_MNEMONIC), JOptionPane.CANCEL_OPTION));

    return panel;
  }

  private JButton createButton(final String caption, final String mnemonic, final int option) {
    final JButton button = new JButton(new AbstractAction(caption) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        buttonValue = option;
        buttonClickedEvent.fire();
      }
    });
    button.setMnemonic(mnemonic.charAt(0));

    return button;
  }
}
