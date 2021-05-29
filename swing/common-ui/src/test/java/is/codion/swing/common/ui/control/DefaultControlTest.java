/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.model.CancelException;
import is.codion.common.state.State;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.event.ActionEvent;

import static org.junit.jupiter.api.Assertions.*;

public final class DefaultControlTest {

  private int callCount = 0;

  public void method() {
    callCount++;
  }

  public void errorMethod() throws Exception {
    throw new Exception("test");
  }

  public void runtimeErrorMethod() {
    throw new RuntimeException("test");
  }

  public void cancelMethod() {
    throw new CancelException();
  }

  @Test
  public void test() throws Exception {
    final State enabledState = State.state();
    final Control control = Control.builder(this::method).enabledState(enabledState).build();
    final JButton button = control.createButton();
    assertFalse(button.isEnabled());
    enabledState.set(true);
    assertTrue(button.isEnabled());
    button.doClick();
    assertEquals(1, callCount);
  }

  @Test
  public void eventControl() {
    final State state = State.state();
    final Event<ActionEvent> event = Event.event();
    event.addListener(() -> state.set(true));
    Control.eventControl(event).actionPerformed(null);
    assertTrue(state.get());
  }

  @Test
  public void basics() throws Exception {
    final Control test = Control.control(this::doNothing);
    test.setName("test");
    assertEquals("test", test.toString());
    assertEquals("test", test.getName());
    assertEquals(0, test.getMnemonic());
    test.setMnemonic(10);
    assertEquals(10, test.getMnemonic());
    assertNull(test.getIcon());
    test.setKeyStroke(null);
    test.setDescription("description");
    assertEquals("description", test.getDescription());
    test.actionPerformed(null);
  }

  @Test
  public void actionCommand() {
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
  public void setEnabled() {
    final State enabledState = State.state();
    final Control control = Control.builder(this::doNothing).caption("control").enabledState(enabledState.getObserver()).build();
    assertEquals("control", control.getName());
    assertEquals(enabledState.getObserver(), control.getEnabledObserver());
    assertFalse(control.isEnabled());
    enabledState.set(true);
    assertTrue(control.isEnabled());
    enabledState.set(false);
    assertFalse(control.isEnabled());
  }

  @Test
  public void setEnabledViaMethod() {
    final Control test = Control.control(this::doNothing);
    assertThrows(UnsupportedOperationException.class, () -> test.setEnabled(true));
  }

  @Test
  public void exceptionOnExecute() {
    final Control control = Control.control(this::errorMethod);
    assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
  }

  @Test
  public void runtimeExceptionOnExecute() {
    final Control control = Control.control(this::runtimeErrorMethod);
    assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
  }

  @Test
  public void cancelOnExecute() {
    final Control control = Control.control(this::cancelMethod);
    control.actionPerformed(null);
  }

  private void doNothing() {}
}
