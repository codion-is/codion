/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ValuesTest {

  private final Event<Integer> integerValueChange = Events.event();
  private Integer integerValue = 42;

  public Integer getIntegerValue() {
    return integerValue;
  }

  public void setIntegerValue(final Integer integerValue) {
    if (!Util.equal(this.integerValue, integerValue)) {
      this.integerValue = integerValue;
      integerValueChange.fire(integerValue);
    }
  }

  public Integer getIntValue() {
    return integerValue;
  }

  @Test
  public void value() {
    final AtomicInteger eventCounter = new AtomicInteger();
    final Value<Integer> intValue = Values.value(42);
    intValue.getObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        eventCounter.incrementAndGet();
      }
    });
    intValue.getObserver().addInfoListener(new EventInfoListener<Integer>() {
      @Override
      public void eventOccurred(final Integer info) {
        if (eventCounter.get() != 2) {
          assertNotNull(info);
        }
      }
    });
    intValue.set(42);
    assertEquals(0, eventCounter.get());
    intValue.set(20);
    assertEquals(1, eventCounter.get());
    intValue.set(null);
    assertEquals(2, eventCounter.get());
    intValue.set(null);
    assertEquals(2, eventCounter.get());
    intValue.set(42);
    assertEquals(3, eventCounter.get());
  }

  @Test
  public void linkValues() {
    final AtomicInteger modelValueEventCounter = new AtomicInteger();
    final Value<Integer> modelValue = Values.beanValue(this, "integerValue", Integer.class, integerValueChange.getObserver());
    final Value<Integer> uiValue = Values.value();
    Values.link(modelValue, uiValue, false);
    modelValue.getObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        modelValueEventCounter.incrementAndGet();
      }
    });
    final AtomicInteger uiValueEventCounter = new AtomicInteger();
    uiValue.getObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        uiValueEventCounter.incrementAndGet();
      }
    });
    assertEquals(Integer.valueOf(42), uiValue.get());
    assertEquals(0, modelValueEventCounter.get());
    assertEquals(0, uiValueEventCounter.get());

    uiValue.set(20);
    assertEquals(Integer.valueOf(20), modelValue.get());
    assertEquals(1, modelValueEventCounter.get());
    assertEquals(1, uiValueEventCounter.get());

    modelValue.set(22);
    assertEquals(Integer.valueOf(22), uiValue.get());
    assertEquals(2, modelValueEventCounter.get());
    assertEquals(2, uiValueEventCounter.get());

    uiValue.set(22);
    assertEquals(2, modelValueEventCounter.get());
    assertEquals(2, uiValueEventCounter.get());

    uiValue.set(null);
    assertNull(modelValue.get());
    assertNull(uiValue.get());
    assertEquals(3, modelValueEventCounter.get());
    assertEquals(3, uiValueEventCounter.get());
  }

  @Test
  public void linkValuesReadOnly() {
    final AtomicInteger modelValueEventCounter = new AtomicInteger();
    final Value<Integer> modelValue = Values.beanValue(this, "intValue", Integer.class, integerValueChange.getObserver());
    final Value<Integer> uiValue = Values.value();
    Values.link(modelValue, uiValue, true);
    modelValue.getObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        modelValueEventCounter.incrementAndGet();
      }
    });
    final AtomicInteger uiValueEventCounter = new AtomicInteger();
    uiValue.getObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        uiValueEventCounter.incrementAndGet();
      }
    });
    assertEquals(Integer.valueOf(42), uiValue.get());
    assertEquals(0, modelValueEventCounter.get());
    assertEquals(0, uiValueEventCounter.get());

    uiValue.set(20);
    assertEquals(Integer.valueOf(42), modelValue.get());//read only, no change
    assertEquals(0, modelValueEventCounter.get());
    assertEquals(1, uiValueEventCounter.get());

    setIntegerValue(22);
    assertEquals(Integer.valueOf(22), uiValue.get());
    assertEquals(1, modelValueEventCounter.get());
    assertEquals(2, uiValueEventCounter.get());

    uiValue.set(22);
    assertEquals(1, modelValueEventCounter.get());
    assertEquals(2, uiValueEventCounter.get());

    uiValue.set(null);
    assertNotNull(modelValue.get());
    assertNull(uiValue.get());
    assertEquals(1, modelValueEventCounter.get());
    assertEquals(3, uiValueEventCounter.get());
  }

  @Test
  public void stateValue() {
    final State state = States.state(true);
    final Value<Boolean> stateValue = Values.stateValue(state);
    assertNotNull(stateValue.getObserver());
    assertTrue(stateValue.get());
    stateValue.set(false);
    assertFalse(state.isActive());
    stateValue.set(true);
    assertTrue(state.isActive());
    state.setActive(false);
    assertFalse(stateValue.get());
  }

  @Test(expected = IllegalArgumentException.class)
  public void beanValueNoGetter() {
    Values.beanValue(this, "nonexistent", Integer.class, integerValueChange.getObserver());
  }

  @Test(expected = IllegalArgumentException.class)
  public void beanValueNoOwner() {
    Values.beanValue(null, "integerValue", Integer.class, integerValueChange.getObserver());
  }

  @Test(expected = IllegalArgumentException.class)
  public void beanValueNoPropertyName() {
    Values.beanValue(this, null, Integer.class, integerValueChange.getObserver());
  }

  @Test(expected = IllegalArgumentException.class)
  public void beanValueNoValueClass() {
    Values.beanValue(this, "integerValue", null, integerValueChange.getObserver());
  }

  @Test(expected = IllegalStateException.class)
  public void setReadOnly() {
    final Value<Integer> modelValue = Values.beanValue(this, "intValue", Integer.class, integerValueChange.getObserver());
    modelValue.set(42);
  }
}
