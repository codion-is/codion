/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.randomizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ItemRandomizerModelTest {

  @Test
  void test() {
    final Object one = "one";
    final Object two = "two";
    final Object three = "three";

    ItemRandomizerModel<Object> model = new ItemRandomizerModel<>();
    model.addItem(one);
    model.addItem(two);
    model.addItem(three);
    assertEquals(3, model.itemCount());
    assertEquals(3, model.items().size());

    model.incrementWeight(three);
    assertEquals(three, model.randomItem());

    model.decrementWeight(three);

    model.incrementWeight(one);
    assertEquals(1, model.weight(one));
    model.incrementWeight(two);
    assertEquals(1, model.weight(two));
    model.incrementWeight(three);
    assertEquals(1, model.weight(three));

    assertEquals(Double.valueOf(1 / 3d), Double.valueOf(model.weightRatio(one)));

    model.incrementWeight(three);
    assertEquals(2, model.weight(three));
    model.incrementWeight(three);
    assertEquals(3, model.weight(three));
    model.incrementWeight(three);
    assertEquals(4, model.weight(three));

    assertEquals(Double.valueOf(4 / 6d), Double.valueOf(model.weightRatio(three)));

    model.incrementWeight(one);
    assertEquals(2, model.weight(one));

    assertEquals(Double.valueOf(2 / 7d), Double.valueOf(model.weightRatio(one)));

    model.incrementWeight(two);
    assertEquals(2, model.weight(two));

    assertEquals(Double.valueOf(2 / 8d), Double.valueOf(model.weightRatio(one)));
    assertEquals(Double.valueOf(2 / 8d), Double.valueOf(model.weightRatio(two)));
    assertEquals(Double.valueOf(4 / 8d), Double.valueOf(model.weightRatio(three)));

    model.decrementWeight(one);
    assertEquals(1, model.weight(one));
    model.decrementWeight(two);
    assertEquals(1, model.weight(two));

    model.decrementWeight(one);
    assertEquals(0, model.weight(one));
    model.decrementWeight(two);
    assertEquals(0, model.weight(two));

    model.setItemEnabled(one, false);
    assertFalse(model.isItemEnabled(one));

    model.setItemEnabled(one, true);
    assertTrue(model.isItemEnabled(one));

    try {
      model.decrementWeight(one);
      fail();
    }
    catch (IllegalStateException ignored) {/*ignored*/}
  }
}
