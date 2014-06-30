/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.tools;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * User: Björn Darri
 * Date: 6.4.2010
 * Time: 21:50:22
 */
public class BoundedItemRandomizerModelTest {
  private final String one = "one";
  private final String two = "two";
  private final String three = "three";

  @Test(expected = IllegalArgumentException.class)
  public void constructWithoutObjects() {
    new BoundedItemRandomizerModel<>(10, Collections.<String>emptyList());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructWithoutParemeters() {
    new BoundedItemRandomizerModel<>(Collections.emptyList());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructNegativeWeight() {
    new BoundedItemRandomizerModel<>(-10, Collections.<String>emptyList());
  }

  @Test
  public void construct() {
    final BoundedItemRandomizerModel<String> model = new BoundedItemRandomizerModel<>(10, Arrays.asList(one, two, three));
    assertTrue(model.getItemCount() == 3);
    assertTrue(model.getWeightBounds() == 10);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void setWeight() {
    final BoundedItemRandomizerModel<String> model = new BoundedItemRandomizerModel<>(10, Arrays.asList(one, two, three));
    model.setWeight(one, 10);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void addItem() {
    final BoundedItemRandomizerModel<String> model = new BoundedItemRandomizerModel<>(10, Arrays.asList(one, two, three));
    model.addItem("four");
  }

  @Test
  public void test() {
    final BoundedItemRandomizerModel<String> model = new BoundedItemRandomizerModel<>(10, Arrays.asList(one, two, three));

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
    catch (IllegalStateException e) {}

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
    catch (IllegalStateException e) {}
  }
}
