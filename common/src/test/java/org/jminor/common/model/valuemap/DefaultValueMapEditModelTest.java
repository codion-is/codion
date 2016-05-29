/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.EventInfoListener;
import org.jminor.common.model.valuemap.exception.ValidationException;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultValueMapEditModelTest {

  @Test
  public void test() throws Exception {
    final AtomicInteger anyValueChangeCounter = new AtomicInteger();
    final AtomicInteger valueChangeCounter = new AtomicInteger();
    final AtomicInteger valueSetCounter = new AtomicInteger();

    final EventInfoListener<ValueChange<String, ?>> anyValueChangeListener = info -> anyValueChangeCounter.incrementAndGet();
    final EventInfoListener<ValueChange<String, ?>> valueChangeListener = info -> valueChangeCounter.incrementAndGet();
    final EventInfoListener<ValueChange<String, ?>> valueSetListener = info -> valueSetCounter.incrementAndGet();

    final ValueMapEditModel model = new DefaultValueMapEditModel<>(new DefaultValueMap<>(), new DefaultValueMapValidator<String, ValueMap<String, ?>>() {
      @Override
      public boolean isNullable(final ValueMap valueMap, final String key) {
        return !key.equals("id");
      }
    });

    assertNotNull(model.getValidator());

    model.getValueObserver().addInfoListener(anyValueChangeListener);
    model.addValueListener("id", valueChangeListener);
    model.addValueSetListener("id", valueSetListener);

    model.setValue("id", 1);
    model.validate();
    model.validate("id");
    assertTrue(model.isValid());
    assertTrue(model.isValid("id"));
    assertTrue(model.getValidObserver().isActive());
    assertTrue(valueSetCounter.get() == 1);
    assertTrue(valueChangeCounter.get() == 1);
    assertTrue(anyValueChangeCounter.get() == 1);

    model.setValue("id", 1);
    assertTrue(valueSetCounter.get() == 1);
    assertTrue(valueChangeCounter.get() == 1);
    assertTrue(anyValueChangeCounter.get() == 1);

    assertFalse(model.isNullable("id"));
    assertTrue(!model.isValueNull("id"));
    assertEquals(1, model.getValue("id"));

    model.setValue("id", null);
    assertFalse(model.isValid());
    assertFalse(model.isValid("id"));
    try {
      model.validate();
      fail();
    }
    catch (final ValidationException e) {}
    try {
      model.validate("id");
      fail();
    }
    catch (final ValidationException e) {}

    assertTrue(valueSetCounter.get() == 2);
    assertTrue(valueChangeCounter.get() == 2);
    assertTrue(anyValueChangeCounter.get() == 2);
    assertTrue(model.isValueNull("id"));

    model.setValue("name", "Name");
    assertTrue(valueSetCounter.get() == 2);
    assertTrue(valueChangeCounter.get() == 2);
    assertTrue(anyValueChangeCounter.get() == 3);

    model.removeValueListener("id", valueChangeListener);
    model.removeValueSetListener("id", valueSetListener);
  }
}
