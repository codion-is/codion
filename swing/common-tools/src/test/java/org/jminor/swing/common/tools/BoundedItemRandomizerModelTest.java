/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.tools;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 6.4.2010
 * Time: 21:50:22
 */
public class BoundedItemRandomizerModelTest {
  private final String one = "one";
  private final String two = "two";
  private final String three = "three";

  @Test
  public void constructWithoutObjects() {
    assertThrows(IllegalArgumentException.class, () -> new BoundedItemRandomizerModel<>(10, Collections.emptyList()));
  }

  @Test
  public void constructWithoutParemeters() {
    assertThrows(IllegalArgumentException.class, () -> new BoundedItemRandomizerModel<>(Collections.emptyList()));
  }

  @Test
  public void constructNegativeWeight() {
    assertThrows(IllegalArgumentException.class, () -> new BoundedItemRandomizerModel<>(-10, Collections.emptyList()));
  }

  @Test
  public void construct() {
    final BoundedItemRandomizerModel<String> model = new BoundedItemRandomizerModel<>(10, Arrays.asList(one, two, three));
    assertEquals(3, model.getItemCount());
    assertEquals(10, model.getWeightBounds());
  }

  @Test
  public void setWeight() {
    final BoundedItemRandomizerModel<String> model = new BoundedItemRandomizerModel<>(10, Arrays.asList(one, two, three));
    assertThrows(UnsupportedOperationException.class, () -> model.setWeight(one, 10));
  }

  @Test
  public void addItem() {
    final BoundedItemRandomizerModel<String> model = new BoundedItemRandomizerModel<>(10, Arrays.asList(one, two, three));
    assertThrows(UnsupportedOperationException.class, () -> model.addItem("four"));
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
    catch (final IllegalStateException ignored) {/*ignored*/}

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
    catch (final IllegalStateException ignored) {/*ignored*/}
  }
}
