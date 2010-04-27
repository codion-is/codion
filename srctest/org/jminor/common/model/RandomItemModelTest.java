/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

public class RandomItemModelTest {

  @Test
  public void test() {
    final Object one = "one";
    final Object two = "two";
    final Object three = "three";

    final RandomItemModel<Object> model = new RandomItemModel<Object>(0, one, two, three);

    model.increment(three);
    assertEquals(three, model.getRandomItem());

    model.decrement(three);

    model.increment(one);
    assertEquals(1, model.getWeight(one));
    model.increment(two);
    assertEquals(1, model.getWeight(two));
    model.increment(three);
    assertEquals(1, model.getWeight(three));

    assertEquals(Double.valueOf(1/3d), Double.valueOf(model.getWeightRatio(one)));

    model.increment(three);
    assertEquals(2, model.getWeight(three));
    model.increment(three);
    assertEquals(3, model.getWeight(three));
    model.increment(three);
    assertEquals(4, model.getWeight(three));

    assertEquals(Double.valueOf(4/6d), Double.valueOf(model.getWeightRatio(three)));

    model.increment(one);
    assertEquals(2, model.getWeight(one));

    assertEquals(Double.valueOf(2/7d), Double.valueOf(model.getWeightRatio(one)));

    model.increment(two);
    assertEquals(2, model.getWeight(two));

    assertEquals(Double.valueOf(2/8d), Double.valueOf(model.getWeightRatio(one)));
    assertEquals(Double.valueOf(2/8d), Double.valueOf(model.getWeightRatio(two)));
    assertEquals(Double.valueOf(4/8d), Double.valueOf(model.getWeightRatio(three)));

    model.decrement(one);
    assertEquals(1, model.getWeight(one));
    model.decrement(two);
    assertEquals(1, model.getWeight(two));

    model.decrement(one);
    assertEquals(0, model.getWeight(one));
    model.decrement(two);
    assertEquals(0, model.getWeight(two));

    try {
      model.decrement(one);
      fail();
    }
    catch (Exception e) {}
  }
}
