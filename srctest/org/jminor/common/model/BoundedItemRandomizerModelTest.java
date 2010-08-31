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
public class BoundedItemRandomizerModelTest {
  private final Object one = "one";
  private final Object two = "two";
  private final Object three = "three";

  @Test
  public void testConstructors() {
    BoundedItemRandomizerModel<Object> model = new BoundedItemRandomizerModel<Object>(10, one, two, three);
    assertTrue(model.getItemCount() == 3);
    assertTrue(model.getWeightBounds() == 10);
    try {
      new BoundedItemRandomizerModel<Object>(10);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      new BoundedItemRandomizerModel<Object>();
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      new BoundedItemRandomizerModel<Object>(-10);
      fail();
    }
    catch (IllegalArgumentException e) {}
  }

  @Test
  public void testExceptionals() {
    final BoundedItemRandomizerModel<Object> model = new BoundedItemRandomizerModel<Object>(10, one, two, three);
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
    final BoundedItemRandomizerModel<Object> model = new BoundedItemRandomizerModel<Object>(10, one, two, three);

    assertEquals(3, model.getWeight(one));//last
    assertEquals(3, model.getWeight(two));
    assertEquals(4, model.getWeight(three));

    model.incrementWeight(one);

    assertEquals(4, model.getWeight(one));
    assertEquals(3, model.getWeight(two));
    assertEquals(3, model.getWeight(three));//last

    model.incrementWeight(three);

    assertEquals(4, model.getWeight(one));
    assertEquals(2, model.getWeight(two));//last
    assertEquals(4, model.getWeight(three));

    model.decrementWeight(one);

    assertEquals(3, model.getWeight(one));
    assertEquals(2, model.getWeight(two));
    assertEquals(5, model.getWeight(three));//last

    model.decrementWeight(two);

    assertEquals(4, model.getWeight(one));//last
    assertEquals(1, model.getWeight(two));
    assertEquals(5, model.getWeight(three));

    model.decrementWeight(two);

    assertEquals(4, model.getWeight(one));
    assertEquals(0, model.getWeight(two));
    assertEquals(6, model.getWeight(three));//last

    try {
      model.decrementWeight(two);
      fail();
    }
    catch (RuntimeException e) {}

    model.incrementWeight(three);

    assertEquals(3, model.getWeight(one));//last
    assertEquals(0, model.getWeight(two));
    assertEquals(7, model.getWeight(three));

    model.incrementWeight(three);

    assertEquals(2, model.getWeight(one));//last
    assertEquals(0, model.getWeight(two));
    assertEquals(8, model.getWeight(three));

    model.decrementWeight(three);

    assertEquals(2, model.getWeight(one));
    assertEquals(1, model.getWeight(two));//last
    assertEquals(7, model.getWeight(three));

    model.decrementWeight(three);

    assertEquals(3, model.getWeight(one));//last
    assertEquals(1, model.getWeight(two));
    assertEquals(6, model.getWeight(three));

    model.decrementWeight(two);

    assertEquals(3, model.getWeight(one));
    assertEquals(0, model.getWeight(two));
    assertEquals(7, model.getWeight(three));//last

    model.incrementWeight(three);

    assertEquals(2, model.getWeight(one));//last
    assertEquals(0, model.getWeight(two));
    assertEquals(8, model.getWeight(three));

    model.incrementWeight(three);

    assertEquals(1, model.getWeight(one));//last
    assertEquals(0, model.getWeight(two));
    assertEquals(9, model.getWeight(three));

    model.incrementWeight(three);

    assertEquals(0, model.getWeight(one));//last
    assertEquals(0, model.getWeight(two));
    assertEquals(10, model.getWeight(three));

    try {
      model.incrementWeight(three);
      fail();
    }
    catch (RuntimeException e) {}
  }
}
