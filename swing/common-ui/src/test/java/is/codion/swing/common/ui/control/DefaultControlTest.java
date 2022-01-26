/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.model.CancelException;
import is.codion.common.state.State;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;

import static org.junit.jupiter.api.Assertions.*;

public final class DefaultControlTest {

  private int callCount = 0;

  void method() {
    callCount++;
  }

  void errorMethod() throws Exception {
    throw new Exception("test");
  }

  void runtimeErrorMethod() {
    throw new RuntimeException("test");
  }

  void cancelMethod() {
    throw new CancelException();
  }

  @Test
  void test() throws Exception {
    final State enabledState = State.state();
    final Control control = Control.builder(this::method).enabledState(enabledState).build();
    final JButton button = control.createButton();
    assertFalse(button.isEnabled());
    enabledState.set(true);
    assertTrue(button.isEnabled());
    button.doClick();
    assertEquals(1, callCount);
    control.setForeground(Color.RED);
    assertEquals(button.getForeground(), Color.RED);
    control.setBackground(Color.BLACK);
    assertEquals(button.getBackground(), Color.BLACK);
    final Font font = button.getFont().deriveFont(Font.ITALIC);
    control.setFont(font);
    assertEquals(button.getFont(), font);
  }

  @Test
  void eventControl() {
    final State state = State.state();
    final Event<ActionEvent> event = Event.event();
    event.addListener(() -> state.set(true));
    Control.eventControl(event).actionPerformed(null);
    assertTrue(state.get());
  }

  @Test
  void basics() throws Exception {
    final Control test = Control.control(this::doNothing);
    test.setCaption("test");
    assertEquals("test", test.toString());
    assertEquals("test", test.getCaption());
    assertEquals(0, test.getMnemonic());
    test.setMnemonic(10);
    assertEquals(10, test.getMnemonic());
    assertNull(test.getSmallIcon());
    test.setKeyStroke(null);
    test.setDescription("description");
    assertEquals("description", test.getDescription());
    test.actionPerformed(null);
  }

  @Test
  void actionCommand() {
    final ActionEvent event = new ActionEvent(this, -1, "test");
    final Control test = Control.actionControl(actionEvent -> {
      assertSame(this, actionEvent.getSource());
      assertEquals(actionEvent.getActionCommand(), "test");
      assertEquals(actionEvent.getID(), -1);
    });
    assertTrue(test instanceof DefaultActionControl);
    test.actionPerformed(event);
  }

  @Test
  void setEnabled() {
    final State enabledState = State.state();
    final Control control = Control.builder(this::doNothing).caption("control").enabledState(enabledState.getObserver()).build();
    assertEquals("control", control.getCaption());
    assertEquals(enabledState.getObserver(), control.getEnabledObserver());
    assertFalse(control.isEnabled());
    enabledState.set(true);
    assertTrue(control.isEnabled());
    enabledState.set(false);
    assertFalse(control.isEnabled());
  }

  @Test
  void setEnabledViaMethod() {
    final Control test = Control.control(this::doNothing);
    assertThrows(UnsupportedOperationException.class, () -> test.setEnabled(true));
  }

  @Test
  void exceptionOnExecute() {
    final Control control = Control.control(this::errorMethod);
    assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
  }

  @Test
  void runtimeExceptionOnExecute() {
    final Control control = Control.control(this::runtimeErrorMethod);
    assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
  }

  @Test
  void cancelOnExecute() {
    final Control control = Control.control(this::cancelMethod);
    control.actionPerformed(null);
  }

  private void doNothing() {}
}
