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

    final RandomItemModel model = new RandomItemModel(0, one, two, three);

    model.increment(three);
    assertEquals(three, model.getRandomItem());

    model.decrement(three);

    model.increment(one);
    assertEquals(1, model.getWeight(one));
    model.increment(two);
    assertEquals(1, model.getWeight(two));
    model.increment(three);
    assertEquals(1, model.getWeight(three));

    model.increment(three);
    assertEquals(2, model.getWeight(three));
    model.increment(three);
    assertEquals(3, model.getWeight(three));
    model.increment(three);
    assertEquals(4, model.getWeight(three));

    model.increment(one);
    assertEquals(2, model.getWeight(one));
    model.increment(two);
    assertEquals(2, model.getWeight(two));

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
