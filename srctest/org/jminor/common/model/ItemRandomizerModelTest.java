/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

public class ItemRandomizerModelTest {

  @Test
  public void test() {
    final Object one = "one";
    final Object two = "two";
    final Object three = "three";

    final ItemRandomizerModel<Object> model = new ItemRandomizerModel<Object>(0, one, two, three);
    assertNotNull(model.getRandom());
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

    assertEquals(Double.valueOf(1/3d), Double.valueOf(model.getWeightRatio(one)));

    model.incrementWeight(three);
    assertEquals(2, model.getWeight(three));
    model.incrementWeight(three);
    assertEquals(3, model.getWeight(three));
    model.incrementWeight(three);
    assertEquals(4, model.getWeight(three));

    assertEquals(Double.valueOf(4/6d), Double.valueOf(model.getWeightRatio(three)));

    model.incrementWeight(one);
    assertEquals(2, model.getWeight(one));

    assertEquals(Double.valueOf(2/7d), Double.valueOf(model.getWeightRatio(one)));

    model.incrementWeight(two);
    assertEquals(2, model.getWeight(two));

    assertEquals(Double.valueOf(2/8d), Double.valueOf(model.getWeightRatio(one)));
    assertEquals(Double.valueOf(2/8d), Double.valueOf(model.getWeightRatio(two)));
    assertEquals(Double.valueOf(4/8d), Double.valueOf(model.getWeightRatio(three)));

    model.decrementWeight(one);
    assertEquals(1, model.getWeight(one));
    model.decrementWeight(two);
    assertEquals(1, model.getWeight(two));

    model.decrementWeight(one);
    assertEquals(0, model.getWeight(one));
    model.decrementWeight(two);
    assertEquals(0, model.getWeight(two));

    try {
      model.decrementWeight(one);
      fail();
    }
    catch (Exception e) {}
  }
}
