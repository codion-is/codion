package org.jminor.common.ui.input;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BooleanInputProviderTest {

  @Test
  public void test() {
    final BooleanInputProvider provider = new BooleanInputProvider(false);
    assertEquals(false, provider.getValue());
    provider.getInputComponent().getModel().setSelectedItem(true);
    assertEquals(true, provider.getValue());
    provider.getInputComponent().getModel().setSelectedItem(null);
    assertNull(provider.getValue());
  }
}
