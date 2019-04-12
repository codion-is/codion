/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.common.Event;
import org.jminor.common.EventDataListener;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.i18n.Messages;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Controls;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.Objects;

/**
 * A panel for presenting a InputProvider.
 * @param <T> the input value type
 * @param <K> the type of the input component
 */
public final class InputProviderPanel<T, K extends JComponent> extends JPanel implements InputProvider<T, K> {

  private static final int COLUMNS = 2;

  private final Event<Integer> buttonClickedEvent = Events.event();
  private final InputProvider<T, K> inputProvider;
  private final JButton okButton;
  private final JButton cancelButton;

  private int buttonValue = -Integer.MAX_VALUE;

  /**
   * Instantiates a new InputProviderPanel
   * @param caption the input panel caption
   * @param inputProvider the InputProvider to use
   */
  public InputProviderPanel(final String caption, final InputProvider<T, K> inputProvider) {
    Objects.requireNonNull(inputProvider, "inputProvider");
    this.inputProvider = inputProvider;
    this.okButton = createButton(Messages.get(Messages.OK), Messages.get(Messages.OK_MNEMONIC), JOptionPane.OK_OPTION);
    this.cancelButton = createButton(Messages.get(Messages.CANCEL), Messages.get(Messages.CANCEL_MNEMONIC), JOptionPane.CANCEL_OPTION);
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

  /**
   * @return the Cancel button
   */
  public JButton getCancelButton() {
    return cancelButton;
  }

  /** {@inheritDoc} */
  @Override
  public T getValue() {
    return inputProvider.getValue();
  }

  /** {@inheritDoc} */
  @Override
  public K getInputComponent() {
    return inputProvider.getInputComponent();
  }

  /**
   * @return an EventObserver notified when a button is clicked,
   * the event info is either {@link JOptionPane#CANCEL_OPTION}
   * or {@link JOptionPane#OK_OPTION} depending on the button clicked
   */
  public EventObserver<Integer> getButtonClickObserver() {
    return buttonClickedEvent.getObserver();
  }

  /**
   * @param listener a listener notified each time a button is clicked,
   * the event info is either {@link JOptionPane#CANCEL_OPTION}
   * or {@link JOptionPane#OK_OPTION} depending on the button clicked
   */
  public void addButtonClickListener(final EventDataListener<Integer> listener) {
    buttonClickedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeButtonClickListener(final EventDataListener listener) {
    buttonClickedEvent.removeDataListener(listener);
  }

  private void initUI(final String caption) {
    setLayout(UiUtil.createBorderLayout());
    if (caption != null) {
      setBorder(BorderFactory.createTitledBorder(caption));
    }
    add(inputProvider.getInputComponent(), BorderLayout.CENTER);
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    panel.add(createButtonPanel());
    add(panel, BorderLayout.SOUTH);
  }

  private JPanel createButtonPanel() {
    final JPanel panel = new JPanel(UiUtil.createGridLayout(1, COLUMNS));
    panel.add(okButton);
    UiUtil.addKeyEvent(this, KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_IN_FOCUSED_WINDOW, true,
            Controls.control(cancelButton::doClick, "cancelInput"));
    panel.add(cancelButton);

    return panel;
  }

  private JButton createButton(final String caption, final String mnemonic, final int option) {
    final JButton button = new JButton(Controls.control(() -> {
      buttonValue = option;
      buttonClickedEvent.fire(option);
    }, caption));
    button.setMnemonic(mnemonic.charAt(0));

    return button;
  }
}
