package org.jminor.common.ui.input;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

public class InputProviderPanelTest {

  @Test
  public void test() {
    final JTextField txt = new JTextField();
    final InputProviderPanel panel = new InputProviderPanel("Test", new InputProvider() {
      public JComponent getInputComponent() {
        return txt;
      }
      public Object getValue() {
        return txt.getText();
      }
    });
    assertEquals(txt, panel.getInputComponent());
    txt.setText("hello");
    assertEquals("hello", panel.getValue());
    assertFalse(panel.isEditAccepted());
    final Collection<Object> event = new ArrayList<Object>();
    panel.eventButtonClicked().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        event.add(new Object());
      }
    });
    panel.getOkButton().doClick();
    assertTrue(panel.isEditAccepted());
    assertEquals(1, event.size());
  }
}
