/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

/**
 * A utility class for adding focus traversal events based on the Enter key.
 */
public final class TransferFocusOnEnter {

  private TransferFocusOnEnter() {}

  /**
   * Adds a key event to the component which transfers focus
   * on enter, and backwards if shift is down
   * @param component the component
   * @param <T> the component type
   * @see #disable(JComponent)
   * @return the component
   */
  public static <T extends JComponent> T enable(T component) {
    forwardBuilder(component).enable(component);
    backwardsBuilder(component).enable(component);

    return component;
  }

  /**
   * Disables the transfer focus action added via {@link #enable(JComponent)}
   * @param component the component
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JComponent> T disable(T component) {
    forwardBuilder(component).disable(component);
    backwardsBuilder(component).disable(component);

    return component;
  }

  /**
   * Instantiates an Action for transferring keyboard focus forward.
   * @param component the component
   * @return an Action for transferring focus
   */
  public static Action forwardAction(JComponent component) {
    return new TransferFocusAction(component);
  }

  /**
   * Instantiates an Action for transferring keyboard focus backward.
   * @param component the component
   * @return an Action for transferring focus
   */
  public static Action backwardAction(JComponent component) {
    return new TransferFocusAction(component, true);
  }

  private static <T extends JComponent> KeyEvents.Builder backwardsBuilder(T component) {
    return KeyEvents.builder(KeyEvent.VK_ENTER)
            .modifiers(InputEvent.SHIFT_DOWN_MASK)
            .condition(JComponent.WHEN_FOCUSED)
            .action(backwardAction(component));
  }

  private static <T extends JComponent> KeyEvents.Builder forwardBuilder(T component) {
    return KeyEvents.builder(KeyEvent.VK_ENTER)
            .modifiers(component instanceof JTextArea ? InputEvent.CTRL_DOWN_MASK : 0)
            .condition(JComponent.WHEN_FOCUSED)
            .action(forwardAction(component));
  }

  /**
   * An action which transfers focus either forward or backward for a given component
   */
  private static final class TransferFocusAction extends AbstractAction {

    private final JComponent component;
    private final boolean backward;

    /**
     * Instantiates an Action for transferring keyboard focus.
     * @param component the component
     */
    private TransferFocusAction(JComponent component) {
      this(component, false);
    }

    /**
     * @param component the component
     * @param backward if true the focus is transferred backward
     */
    private TransferFocusAction(JComponent component, boolean backward) {
      super(backward ? "TransferFocusOnEnter.transferFocusBackward" : "KeyTransferFocusOnEnter.transferFocusForward");
      this.component = Objects.requireNonNull(component, "component");
      this.backward = backward;
    }

    /**
     * Transfers focus according the value of {@code backward}
     * @param e the action event
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      if (backward) {
        component.transferFocusBackward();
      }
      else {
        component.transferFocus();
      }
    }
  }
}
