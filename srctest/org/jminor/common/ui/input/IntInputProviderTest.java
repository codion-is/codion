package org.jminor.common.ui.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import javax.swing.JTextField;

public class IntInputProviderTest {

  @Test
  public void test() {
    final Integer value = 10;
    IntInputProvider provider = new IntInputProvider(value);
    assertEquals(value, provider.getValue());

    provider = new IntInputProvider(null);
    assertNull(provider.getValue());

    ((JTextField) provider.getInputComponent()).setText("15");
    assertEquals(Integer.valueOf(15), provider.getValue());
  }
}