/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.randomizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ItemRandomizerModelTest {

  @Test
  public void test() {
    final Object one = "one";
    final Object two = "two";
    final Object three = "three";

    final ItemRandomizerModel<Object> model = new ItemRandomizerModel<>();
    model.addItem(one);
    model.addItem(two);
    model.addItem(three);
    assertEquals(3, model.getItemCount());
    assertEquals(3, model.getItems().size());

    model.incrementWeight(three);
    assertEquals(three, model.getRandomItem());

    model.decrementWeight(three);

    model.incrementWeight(one);
    assertEquals(1, model.getWeight(one));
    model.incrementWeight(two);
    assertEquals(1, model.getWeight(two));
    model.incrementWeight(three);
    assertEquals(1, model.getWeight(three));

    assertEquals(Double.valueOf(1 / 3d), Double.valueOf(model.getWeightRatio(one)));

    model.incrementWeight(three);
    assertEquals(2, model.getWeight(three));
    model.incrementWeight(three);
    assertEquals(3, model.getWeight(three));
    model.incrementWeight(three);
    assertEquals(4, model.getWeight(three));

    assertEquals(Double.valueOf(4 / 6d), Double.valueOf(model.getWeightRatio(three)));

    model.incrementWeight(one);
    assertEquals(2, model.getWeight(one));

    assertEquals(Double.valueOf(2 / 7d), Double.valueOf(model.getWeightRatio(one)));

    model.incrementWeight(two);
    assertEquals(2, model.getWeight(two));

    assertEquals(Double.valueOf(2 / 8d), Double.valueOf(model.getWeightRatio(one)));
    assertEquals(Double.valueOf(2 / 8d), Double.valueOf(model.getWeightRatio(two)));
    assertEquals(Double.valueOf(4 / 8d), Double.valueOf(model.getWeightRatio(three)));

    model.decrementWeight(one);
    assertEquals(1, model.getWeight(one));
    model.decrementWeight(two);
    assertEquals(1, model.getWeight(two));

    model.decrementWeight(one);
    assertEquals(0, model.getWeight(one));
    model.decrementWeight(two);
    assertEquals(0, model.getWeight(two));

    model.setItemEnabled(one, false);
    assertFalse(model.isItemEnabled(one));

    model.setItemEnabled(one, true);
    assertTrue(model.isItemEnabled(one));

    try {
      model.decrementWeight(one);
      fail();
    }
    catch (final IllegalStateException ignored) {/*ignored*/}
  }
}
