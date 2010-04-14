package org.jminor.common.ui.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import javax.swing.JTextField;

public class DoubleInputProviderTest {

  @Test
  public void test() {
    final Double value = 10.4;
    DoubleInputProvider provider = new DoubleInputProvider(value);
    assertEquals(value, provider.getValue());

    provider = new DoubleInputProvider(null);
    assertNull(provider.getValue());

    ((JTextField) provider.getInputComponent()).setText("15.5");
    assertEquals(Double.valueOf(15.5), provider.getValue());
  }
}