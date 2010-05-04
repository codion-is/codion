package org.jminor.common.ui.input;

import org.jminor.common.ui.TextInputPanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class TextInputProviderTest {

  @Test
  public void test() {
    final String value = "hello";
    TextInputProvider provider = new TextInputProvider("none", null, value);
    assertEquals(value, provider.getValue());

    provider = new TextInputProvider("none", null, null);
    assertNull(provider.getValue());

    ((TextInputPanel) provider.getInputComponent()).setText("tester");
    assertEquals("tester", provider.getValue());

    ((TextInputPanel) provider.getInputComponent()).setText("");
    assertNull(provider.getValue());
  }
}