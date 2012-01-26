package org.jminor.common.ui.input;

import org.junit.Test;

import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class InputProviderPanelTest {

  @Test
  public void test() {
    final JTextField txt = new JTextField();
    final InputProviderPanel panel = new InputProviderPanel("Test", new InputProvider() {
      @Override
      public JComponent getInputComponent() {
        return txt;
      }
      @Override
      public Object getValue() {
        return txt.getText();
      }
    });
    assertEquals(txt, panel.getInputComponent());
    txt.setText("hello");
    assertEquals("hello", panel.getValue());
    assertFalse(panel.isInputAccepted());
    final Collection<Object> event = new ArrayList<Object>();
    final ActionListener listener = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        event.add(new Object());
      }
    };
    panel.addButtonClickListener(listener);
    panel.getOkButton().doClick();
    assertTrue(panel.isInputAccepted());
    assertEquals(1, event.size());
    panel.removeButtonClickListener(listener);
  }
}
