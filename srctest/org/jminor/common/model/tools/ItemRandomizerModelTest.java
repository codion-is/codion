/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.tools;

import org.jminor.common.model.EventListener;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class ItemRandomizerModelTest {

  @Test
  public void test() {
    final Object one = "one";
    final Object two = "two";
    final Object three = "three";

    final Collection<Object> weightChangeCounter = new ArrayList<Object>();
    final EventListener weightListener = new EventListener() {
      @Override
      public void eventOccurred() {
        weightChangeCounter.add(new Object());
      }
    };

    final ItemRandomizerModel<Object> model = new ItemRandomizerModel<Object>(0, one, two, three);
    model.getWeightsObserver().addListener(weightListener);
    assertNotNull(model.getRandom());
    assertEquals(3, model.getItemCount());
    assertEquals(3, model.getItems().size());

    model.incrementWeight(three);
    assertEquals(three, model.getRandomItem());
    assertEquals(1, weightChangeCounter.size());

    model.decrementWeight(three);
    assertEquals(2, weightChangeCounter.size());

    model.incrementWeight(one);
    assertEquals(1, model.getWeight(one));
    assertEquals(3, weightChangeCounter.size());
    model.incrementWeight(two);
    assertEquals(1, model.getWeight(two));
    assertEquals(4, weightChangeCounter.size());
    model.incrementWeight(three);
    assertEquals(1, model.getWeight(three));
    assertEquals(5, weightChangeCounter.size());

    assertEquals(Double.valueOf(1/3d), Double.valueOf(model.getWeightRatio(one)));

    model.incrementWeight(three);
    assertEquals(2, model.getWeight(three));
    assertEquals(6, weightChangeCounter.size());
    model.incrementWeight(three);
    assertEquals(3, model.getWeight(three));
    assertEquals(7, weightChangeCounter.size());
    model.incrementWeight(three);
    assertEquals(4, model.getWeight(three));
    assertEquals(8, weightChangeCounter.size());

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
    catch (IllegalStateException e) {}
  }
}
