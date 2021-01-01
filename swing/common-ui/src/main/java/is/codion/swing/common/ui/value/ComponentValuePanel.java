/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventObserver;
import is.codion.common.event.Events;
import is.codion.common.i18n.Messages;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.AbstractAction;
import javax.swing.Action;
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

import static is.codion.swing.common.ui.KeyEvents.KeyTrigger.ON_KEY_PRESSED;
import static java.util.Objects.requireNonNull;

/**
 * A panel for presenting a ComponentValue. Provides OK and Cancel buttons
 * for use when displayed in a Dialog.
 * @param <V> the input value type
 * @param <C> the type of the input component
 */
public final class ComponentValuePanel<V, C extends JComponent> extends JPanel {

  private static final int COLUMNS = 2;

  private final Event<Integer> buttonClickedEvent = Events.event();
  private final ComponentValue<V, C> componentValue;
  private final Action okAction;
  private final Action cancelAction;

  private int buttonValue = -Integer.MAX_VALUE;

  /**
   * Instantiates a new ComponentValuePanel
   * @param caption the input panel caption
   * @param componentValue the ComponentValue to display
   */
  public ComponentValuePanel(final String caption, final ComponentValue<V, C> componentValue) {
    this.componentValue = requireNonNull(componentValue, "componentValue");
    this.okAction = createAction(Messages.get(Messages.OK), Messages.get(Messages.OK_MNEMONIC), JOptionPane.OK_OPTION);
    this.cancelAction = createAction(Messages.get(Messages.CANCEL), Messages.get(Messages.CANCEL_MNEMONIC), JOptionPane.CANCEL_OPTION);
    initializeUI(caption);
  }

  /**
   * @return the value from the underlying {@link ComponentValue}
   */
  public V getValue() {
    return componentValue.get();
  }

  /**
   * @param value the value to set in the underlying {@link ComponentValue}
   */
  public void setValue(final V value) {
    componentValue.set(value);
  }

  /**
   * @return the input component from the underlying {@link ComponentValue}
   */
  public C getInputComponent() {
    return componentValue.getComponent();
  }

  /**
   * @return true if the input has been accepted, that is, the OK button has been clicked
   */
  public boolean isInputAccepted() {
    return buttonValue == JOptionPane.OK_OPTION;
  }

  /**
   * @return the OK action
   */
  public Action getOkAction() {
    return okAction;
  }

  /**
   * @return the Cancel action
   */
  public Action getCancelAction() {
    return cancelAction;
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
  public void removeButtonClickListener(final EventDataListener<Integer> listener) {
    buttonClickedEvent.removeDataListener(listener);
  }

  private void initializeUI(final String caption) {
    setLayout(Layouts.borderLayout());
    if (caption != null) {
      setBorder(BorderFactory.createTitledBorder(caption));
    }
    add(componentValue.getComponent(), BorderLayout.CENTER);
    final JPanel panel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    panel.add(createButtonPanel());
    add(panel, BorderLayout.SOUTH);
  }

  private JPanel createButtonPanel() {
    final JButton okButton = new JButton(okAction);
    final JButton cancelButton = new JButton(cancelAction);
    final JPanel panel = new JPanel(new GridLayout(1, COLUMNS));
    panel.add(okButton);
    KeyEvents.addKeyEvent(this, KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_IN_FOCUSED_WINDOW, ON_KEY_PRESSED,
            new AbstractAction("cancelInput") {
              @Override
              public void actionPerformed(final ActionEvent e) {
                cancelButton.doClick();
              }
            });
    panel.add(cancelButton);

    return panel;
  }

  private Action createAction(final String caption, final String mnemonic, final int option) {
    final AbstractAction action = new AbstractAction(caption) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        buttonValue = option;
        buttonClickedEvent.onEvent(option);
      }
    };
    action.putValue(Action.MNEMONIC_KEY, (int) mnemonic.charAt(0));

    return action;
  }
}
