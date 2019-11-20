/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.EventDataListener;
import org.jminor.common.db.valuemap.DefaultValueMap;
import org.jminor.common.db.valuemap.DefaultValueMapValidator;
import org.jminor.common.db.valuemap.ValueChange;
import org.jminor.common.db.valuemap.ValueMap;
import org.jminor.common.db.valuemap.exception.ValidationException;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultValueMapEditModelTest {

  @Test
  public void test() throws Exception {
    final AtomicInteger anyValueChangeCounter = new AtomicInteger();
    final AtomicInteger valueChangeCounter = new AtomicInteger();
    final AtomicInteger valueSetCounter = new AtomicInteger();

    final EventDataListener<ValueChange<String, ?>> anyValueChangeListener = data -> anyValueChangeCounter.incrementAndGet();
    final EventDataListener<ValueChange<String, ?>> valueChangeListener = data -> valueChangeCounter.incrementAndGet();
    final EventDataListener<ValueChange<String, ?>> valueSetListener = data -> valueSetCounter.incrementAndGet();

    final String testAttribute = "test";

    final ValueMapEditModel model = new DefaultValueMapEditModel<>(new DefaultValueMap<>(),
            new DefaultValueMapValidator<String, ValueMap<String, ?>>() {
      @Override
      public boolean isNullable(final ValueMap valueMap, final String key) {
        return !key.equals(testAttribute);
      }
    });

    assertNotNull(model.getValidator());

    model.getValueObserver().addDataListener(anyValueChangeListener);
    model.addValueListener(testAttribute, valueChangeListener);
    model.addValueSetListener(testAttribute, valueSetListener);

    model.put(testAttribute, 1);
    model.validate();
    model.validate(testAttribute);
    assertTrue(model.isValid());
    assertTrue(model.isValid(testAttribute));
    assertTrue(model.getValidObserver().get());
    assertEquals(1, valueSetCounter.get());
    assertEquals(1, valueChangeCounter.get());
    assertEquals(1, anyValueChangeCounter.get());

    model.put(testAttribute, 1);
    assertEquals(1, valueSetCounter.get());
    assertEquals(1, valueChangeCounter.get());
    assertEquals(1, anyValueChangeCounter.get());

    assertFalse(model.isNullable(testAttribute));
    assertFalse(model.isNull(testAttribute));
    assertTrue(model.isNotNull(testAttribute));
    assertEquals(1, model.get(testAttribute));

    model.put(testAttribute, null);
    assertFalse(model.isValid());
    assertFalse(model.isValid(testAttribute));
    assertThrows(ValidationException.class, model::validate);
    assertThrows(ValidationException.class, () -> model.validate(testAttribute));

    assertEquals(2, valueSetCounter.get());
    assertEquals(2, valueChangeCounter.get());
    assertEquals(2, anyValueChangeCounter.get());
    assertTrue(model.isNull(testAttribute));
    assertFalse(model.isNotNull(testAttribute));

    final String nameAttribute = "name";

    model.put(nameAttribute, "Name");
    assertEquals(2, valueSetCounter.get());
    assertEquals(2, valueChangeCounter.get());
    assertEquals(3, anyValueChangeCounter.get());

    assertNotNull(model.get(nameAttribute));
    assertNotNull(model.remove(nameAttribute));
    assertNull(model.get(nameAttribute));
    assertNull(model.remove(nameAttribute));

    model.removeValueListener(testAttribute, valueChangeListener);
    model.removeValueSetListener(testAttribute, valueSetListener);
  }
}
