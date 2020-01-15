/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;
import org.jminor.common.i18n.Messages;
import org.jminor.swing.common.ui.KeyEvents;

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
import java.awt.event.KeyEvent;

import static java.util.Objects.requireNonNull;

/**
 * A panel for presenting a ComponentValue, along with OK and Cancel buttons.
 * @param <V> the input value type
 * @param <C> the type of the input component
 */
public final class ComponentValuePanel<V, C extends JComponent> extends JPanel {

  private static final int COLUMNS = 2;

  private final Event<Integer> buttonClickedEvent = Events.event();
  private final ComponentValue<V, C> componentValue;
  private final JButton okButton;
  private final JButton cancelButton;

  private int buttonValue = -Integer.MAX_VALUE;

  /**
   * Instantiates a new ComponentValuePanel
   * @param caption the input panel caption
   * @param componentValue the ComponentValue to display
   */
  public ComponentValuePanel(final String caption, final ComponentValue<V, C> componentValue) {
    requireNonNull(componentValue, "componentValue");
    this.componentValue = componentValue;
    this.okButton = createButton(Messages.get(Messages.OK), Messages.get(Messages.OK_MNEMONIC), JOptionPane.OK_OPTION);
    this.cancelButton = createButton(Messages.get(Messages.CANCEL), Messages.get(Messages.CANCEL_MNEMONIC), JOptionPane.CANCEL_OPTION);
    initializeUI(caption);
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

  public V getValue() {
    return componentValue.get();
  }

  public void setValue(final V value) {
    componentValue.set(value);
  }

  public C getInputComponent() {
    return componentValue.getComponent();
  }

  /**
   * @return an EventObserver notified when a button is clicked,
   * the event data is either {@link JOptionPane#CANCEL_OPTION}
   * or {@link JOptionPane#OK_OPTION} depending on the button clicked
   */
  public EventObserver<Integer> getButtonClickObserver() {
    return buttonClickedEvent.getObserver();
  }

  /**
   * @param listener a listener notified each time a button is clicked,
   * the event data is either {@link JOptionPane#CANCEL_OPTION}
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

  private void initializeUI(final String caption) {
    setLayout(new BorderLayout(5, 5));
    if (caption != null) {
      setBorder(BorderFactory.createTitledBorder(caption));
    }
    add(componentValue.getComponent(), BorderLayout.CENTER);
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    panel.add(createButtonPanel());
    add(panel, BorderLayout.SOUTH);
  }

  private JPanel createButtonPanel() {
    final JPanel panel = new JPanel(new GridLayout(1, COLUMNS));
    panel.add(okButton);
    KeyEvents.addKeyEvent(this, KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_IN_FOCUSED_WINDOW, true,
            new AbstractAction("cancelInput") {
              @Override
              public void actionPerformed(final ActionEvent e) {
                cancelButton.doClick();
              }
            });
    panel.add(cancelButton);

    return panel;
  }

  private JButton createButton(final String caption, final String mnemonic, final int option) {
    final JButton button = new JButton(new AbstractAction(caption) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        buttonValue = option;
        buttonClickedEvent.onEvent(option);
      }
    });
    button.setMnemonic(mnemonic.charAt(0));

    return button;
  }
}
