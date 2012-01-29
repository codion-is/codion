package org.jminor.common.ui.input;

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

    provider.getInputComponent().setText("tester");
    assertEquals("tester", provider.getValue());

    provider.getInputComponent().setText("");
    assertNull(provider.getValue());
  }
}