/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class ValuesTest {

  private final Event integerValueChange = Events.event();
  private Integer integerValue = 42;

  public Integer getIntegerValue() {
    return integerValue;
  }

  public void setIntegerValue(final Integer integerValue) {
    if (!Util.equal(this.integerValue, integerValue)) {
      this.integerValue = integerValue;
      integerValueChange.fire();
    }
  }

  public Integer getIntValue() {
    return integerValue;
  }

  @Test
  public void value() {
    final Collection<Object> eventCounter = new ArrayList<Object>();
    final Value<Integer> intValue = Values.value(42);
    intValue.getChangeEvent().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        eventCounter.add(new Object());
      }
    });
    intValue.set(42);
    assertTrue(eventCounter.isEmpty());
    intValue.set(20);
    assertEquals(1, eventCounter.size());
    intValue.set(null);
    assertEquals(2, eventCounter.size());
    intValue.set(null);
    assertEquals(2, eventCounter.size());
    intValue.set(42);
    assertEquals(3, eventCounter.size());
  }

  @Test
  public void linkValues() {
    final Collection<Object> modelValueEventCounter = new ArrayList<Object>();
    final Value<Integer> modelValue = Values.beanValue(this, "integerValue", Integer.class, integerValueChange.getObserver());
    final Value<Integer> uiValue = Values.value();
    Values.link(modelValue, uiValue, false);
    modelValue.getChangeEvent().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        modelValueEventCounter.add(new Object());
      }
    });
    final Collection<Object> uiValueEventCounter = new ArrayList<Object>();
    uiValue.getChangeEvent().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        uiValueEventCounter.add(new Object());
      }
    });
    assertEquals(Integer.valueOf(42), uiValue.get());
    assertTrue(modelValueEventCounter.isEmpty());
    assertTrue(uiValueEventCounter.isEmpty());

    uiValue.set(20);
    assertEquals(Integer.valueOf(20), modelValue.get());
    assertEquals(1, modelValueEventCounter.size());
    assertEquals(1, uiValueEventCounter.size());

    modelValue.set(22);
    assertEquals(Integer.valueOf(22), uiValue.get());
    assertEquals(2, modelValueEventCounter.size());
    assertEquals(2, uiValueEventCounter.size());

    uiValue.set(22);
    assertEquals(2, modelValueEventCounter.size());
    assertEquals(2, uiValueEventCounter.size());

    uiValue.set(null);
    assertNull(modelValue.get());
    assertNull(uiValue.get());
    assertEquals(3, modelValueEventCounter.size());
    assertEquals(3, uiValueEventCounter.size());
  }

  @Test
  public void linkValuesReadOnly() {
    final Collection<Object> modelValueEventCounter = new ArrayList<Object>();
    final Value<Integer> modelValue = Values.beanValue(this, "intValue", Integer.class, integerValueChange.getObserver());
    final Value<Integer> uiValue = Values.value();
    Values.link(modelValue, uiValue, true);
    modelValue.getChangeEvent().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        modelValueEventCounter.add(new Object());
      }
    });
    final Collection<Object> uiValueEventCounter = new ArrayList<Object>();
    uiValue.getChangeEvent().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        uiValueEventCounter.add(new Object());
      }
    });
    assertEquals(Integer.valueOf(42), uiValue.get());
    assertTrue(modelValueEventCounter.isEmpty());
    assertTrue(uiValueEventCounter.isEmpty());

    uiValue.set(20);
    assertEquals(Integer.valueOf(42), modelValue.get());//read only, no change
    assertTrue(modelValueEventCounter.isEmpty());
    assertEquals(1, uiValueEventCounter.size());

    setIntegerValue(22);
    assertEquals(Integer.valueOf(22), uiValue.get());
    assertEquals(1, modelValueEventCounter.size());
    assertEquals(2, uiValueEventCounter.size());

    uiValue.set(22);
    assertEquals(1, modelValueEventCounter.size());
    assertEquals(2, uiValueEventCounter.size());

    uiValue.set(null);
    assertNotNull(modelValue.get());
    assertNull(uiValue.get());
    assertEquals(1, modelValueEventCounter.size());
    assertEquals(3, uiValueEventCounter.size());
  }

  @Test
  public void stateValue() {
    final State state = States.state(true);
    final Value<Boolean> stateValue = Values.stateValue(state);
    assertNotNull(stateValue.getChangeEvent());
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
