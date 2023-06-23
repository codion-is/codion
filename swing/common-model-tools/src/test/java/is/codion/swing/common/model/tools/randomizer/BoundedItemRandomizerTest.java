/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.tools.randomizer;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 6.4.2010
 * Time: 21:50:22
 */
public class BoundedItemRandomizerTest {
  private final String one = "one";
  private final String two = "two";
  private final String three = "three";

  @Test
  void constructWithoutObjects() {
    assertThrows(IllegalArgumentException.class, () -> new BoundedItemRandomizer<>(emptyList(), 10));
  }

  @Test
  void constructWithoutParemeters() {
    assertThrows(IllegalArgumentException.class, () -> new BoundedItemRandomizer<>(emptyList(), 100));
  }

  @Test
  void constructNegativeWeight() {
    assertThrows(IllegalArgumentException.class, () -> new BoundedItemRandomizer<>(emptyList(), -10));
  }

  @Test
  void construct() {
    BoundedItemRandomizer<String> model = new BoundedItemRandomizer<>(asList(one, two, three), 10);
    assertEquals(3, model.itemCount());
  }

  @Test
  void setWeight() {
    BoundedItemRandomizer<String> model = new BoundedItemRandomizer<>(asList(one, two, three), 10);
    assertThrows(UnsupportedOperationException.class, () -> model.setWeight(one, 10));
  }

  @Test
  void test() {
    BoundedItemRandomizer<String> model = new BoundedItemRandomizer<>(asList(one, two, three), 10);

    assertEquals(3, model.weight(one));//last
    assertEquals(3, model.weight(two));
    assertEquals(4, model.weight(three));

    model.incrementWeight(one);

    assertEquals(4, model.weight(one));
    assertEquals(3, model.weight(two));
    assertEquals(3, model.weight(three));//last

    model.incrementWeight(three);

    assertEquals(4, model.weight(one));
    assertEquals(2, model.weight(two));//last
    assertEquals(4, model.weight(three));

    model.decrementWeight(one);

    assertEquals(3, model.weight(one));
    assertEquals(2, model.weight(two));
    assertEquals(5, model.weight(three));//last

    model.decrementWeight(two);

    assertEquals(4, model.weight(one));//last
    assertEquals(1, model.weight(two));
    assertEquals(5, model.weight(three));

    model.decrementWeight(two);

    assertEquals(4, model.weight(one));
    assertEquals(0, model.weight(two));
    assertEquals(6, model.weight(three));//last

    try {
      model.decrementWeight(two);
      fail();
    }
    catch (IllegalStateException ignored) {/*ignored*/}

    model.incrementWeight(three);

    assertEquals(3, model.weight(one));//last
    assertEquals(0, model.weight(two));
    assertEquals(7, model.weight(three));

    model.incrementWeight(three);

    assertEquals(2, model.weight(one));//last
    assertEquals(0, model.weight(two));
    assertEquals(8, model.weight(three));

    model.decrementWeight(three);

    assertEquals(2, model.weight(one));
    assertEquals(1, model.weight(two));//last
    assertEquals(7, model.weight(three));

    model.decrementWeight(three);

    assertEquals(3, model.weight(one));//last
    assertEquals(1, model.weight(two));
    assertEquals(6, model.weight(three));

    model.decrementWeight(two);

    assertEquals(3, model.weight(one));
    assertEquals(0, model.weight(two));
    assertEquals(7, model.weight(three));//last

    model.incrementWeight(three);

    assertEquals(2, model.weight(one));//last
    assertEquals(0, model.weight(two));
    assertEquals(8, model.weight(three));

    model.incrementWeight(three);

    assertEquals(1, model.weight(one));//last
    assertEquals(0, model.weight(two));
    assertEquals(9, model.weight(three));

    model.incrementWeight(three);

    assertEquals(0, model.weight(one));//last
    assertEquals(0, model.weight(two));
    assertEquals(10, model.weight(three));

    try {
      model.incrementWeight(three);
      fail();
    }
    catch (IllegalStateException ignored) {/*ignored*/}
  }
}
