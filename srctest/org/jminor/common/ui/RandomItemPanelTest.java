package org.jminor.common.ui;

import org.jminor.common.model.RandomItemModel;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RandomItemPanelTest {

  @Test
  public void test() {
    final RandomItemModel<String> model = new RandomItemModel<String>(5, "one", "two", "three");
    final RandomItemPanel<String> panel = new RandomItemPanel<String>(model);
    assertEquals(model, panel.getModel());
  }
}
