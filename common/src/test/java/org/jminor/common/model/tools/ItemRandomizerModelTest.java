/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.tools;

import org.jminor.common.EventListener;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ItemRandomizerModelTest {

  @Test
  public void test() {
    final Object one = "one";
    final Object two = "two";
    final Object three = "three";

    final AtomicInteger weightChangeCounter = new AtomicInteger();
    final EventListener weightListener = weightChangeCounter::incrementAndGet;

    final ItemRandomizerModel<Object> model = new ItemRandomizerModel<>(0, one, two, three);
    model.getWeightsObserver().addListener(weightListener);
    assertNotNull(model.getRandom());
    assertEquals(3, model.getItemCount());
    assertEquals(3, model.getItems().size());

    model.incrementWeight(three);
    assertEquals(three, model.getRandomItem());
    assertEquals(1, weightChangeCounter.get());

    model.decrementWeight(three);
    assertEquals(2, weightChangeCounter.get());

    model.incrementWeight(one);
    assertEquals(1, model.getWeight(one));
    assertEquals(3, weightChangeCounter.get());
    model.incrementWeight(two);
    assertEquals(1, model.getWeight(two));
    assertEquals(4, weightChangeCounter.get());
    model.incrementWeight(three);
    assertEquals(1, model.getWeight(three));
    assertEquals(5, weightChangeCounter.get());

    assertEquals(Double.valueOf(1/3d), Double.valueOf(model.getWeightRatio(one)));

    model.incrementWeight(three);
    assertEquals(2, model.getWeight(three));
    assertEquals(6, weightChangeCounter.get());
    model.incrementWeight(three);
    assertEquals(3, model.getWeight(three));
    assertEquals(7, weightChangeCounter.get());
    model.incrementWeight(three);
    assertEquals(4, model.getWeight(three));
    assertEquals(8, weightChangeCounter.get());

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
