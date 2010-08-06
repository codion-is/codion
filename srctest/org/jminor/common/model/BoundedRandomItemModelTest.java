/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * User: Björn Darri
 * Date: 6.4.2010
 * Time: 21:50:22
 */
public class BoundedRandomItemModelTest {
  private final Object one = "one";
  private final Object two = "two";
  private final Object three = "three";

  @Test
  public void testConstructors() {
    BoundedRandomItemModel<Object> model = new BoundedRandomItemModel<Object>(10, one, two, three);
    assertTrue(model.getItemCount() == 3);
    assertTrue(model.getWeightBounds() == 10);
    try {
      model = new BoundedRandomItemModel<Object>(10);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      model = new BoundedRandomItemModel<Object>();
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      model = new BoundedRandomItemModel<Object>(-10);
      fail();
    }
    catch (IllegalArgumentException e) {}
  }

  @Test
  public void testExceptionals() {
    final BoundedRandomItemModel<Object> model = new BoundedRandomItemModel<Object>(10, one, two, three);
    try {
      model.setWeight(one, 10);
      fail();
    }
    catch (UnsupportedOperationException e) {}
    try {
      model.addItem("four");
      fail();
    }
    catch (UnsupportedOperationException e) {}
  }

  @Test
  public void test() {
    final BoundedRandomItemModel<Object> model = new BoundedRandomItemModel<Object>(10, one, two, three);

    assertEquals(3, model.getWeight(one));//last
    assertEquals(3, model.getWeight(two));
    assertEquals(4, model.getWeight(three));

    model.increment(one);

    assertEquals(4, model.getWeight(one));
    assertEquals(3, model.getWeight(two));
    assertEquals(3, model.getWeight(three));//last

    model.increment(three);

    assertEquals(4, model.getWeight(one));
    assertEquals(2, model.getWeight(two));//last
    assertEquals(4, model.getWeight(three));

    model.decrement(one);

    assertEquals(3, model.getWeight(one));
    assertEquals(2, model.getWeight(two));
    assertEquals(5, model.getWeight(three));//last

    model.decrement(two);

    assertEquals(4, model.getWeight(one));//last
    assertEquals(1, model.getWeight(two));
    assertEquals(5, model.getWeight(three));

    model.decrement(two);

    assertEquals(4, model.getWeight(one));
    assertEquals(0, model.getWeight(two));
    assertEquals(6, model.getWeight(three));//last

    try {
      model.decrement(two);
      fail();
    }
    catch (RuntimeException e) {}

    model.increment(three);

    assertEquals(3, model.getWeight(one));//last
    assertEquals(0, model.getWeight(two));
    assertEquals(7, model.getWeight(three));

    model.increment(three);

    assertEquals(2, model.getWeight(one));//last
    assertEquals(0, model.getWeight(two));
    assertEquals(8, model.getWeight(three));

    model.decrement(three);

    assertEquals(2, model.getWeight(one));
    assertEquals(1, model.getWeight(two));//last
    assertEquals(7, model.getWeight(three));

    model.decrement(three);

    assertEquals(3, model.getWeight(one));//last
    assertEquals(1, model.getWeight(two));
    assertEquals(6, model.getWeight(three));

    model.decrement(two);

    assertEquals(3, model.getWeight(one));
    assertEquals(0, model.getWeight(two));
    assertEquals(7, model.getWeight(three));//last

    model.increment(three);

    assertEquals(2, model.getWeight(one));//last
    assertEquals(0, model.getWeight(two));
    assertEquals(8, model.getWeight(three));

    model.increment(three);

    assertEquals(1, model.getWeight(one));//last
    assertEquals(0, model.getWeight(two));
    assertEquals(9, model.getWeight(three));

    model.increment(three);

    assertEquals(0, model.getWeight(one));//last
    assertEquals(0, model.getWeight(two));
    assertEquals(10, model.getWeight(three));

    try {
      model.increment(three);
      fail();
    }
    catch (RuntimeException e) {}
  }
}
