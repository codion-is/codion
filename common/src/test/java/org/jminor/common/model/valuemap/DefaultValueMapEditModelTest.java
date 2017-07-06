/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.EventInfoListener;
import org.jminor.common.db.Attribute;
import org.jminor.common.db.valuemap.DefaultValueMap;
import org.jminor.common.db.valuemap.ValueChange;
import org.jminor.common.db.valuemap.ValueMap;
import org.jminor.common.db.valuemap.exception.ValidationException;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultValueMapEditModelTest {

  @Test
  public void test() throws Exception {
    final AtomicInteger anyValueChangeCounter = new AtomicInteger();
    final AtomicInteger valueChangeCounter = new AtomicInteger();
    final AtomicInteger valueSetCounter = new AtomicInteger();

    final EventInfoListener<ValueChange<TestAttribute, ?>> anyValueChangeListener = info -> anyValueChangeCounter.incrementAndGet();
    final EventInfoListener<ValueChange<TestAttribute, ?>> valueChangeListener = info -> valueChangeCounter.incrementAndGet();
    final EventInfoListener<ValueChange<TestAttribute, ?>> valueSetListener = info -> valueSetCounter.incrementAndGet();

    final TestAttribute testAttribute = new TestAttribute();

    final ValueMapEditModel model = new DefaultValueMapEditModel<>(new DefaultValueMap<>(), new DefaultValueMap.DefaultValidator<TestAttribute, ValueMap<TestAttribute, ?>>() {
      @Override
      public boolean isNullable(final ValueMap valueMap, final TestAttribute key) {
        return !key.equals(testAttribute);
      }
    });

    assertNotNull(model.getValidator());

    model.getValueObserver().addInfoListener(anyValueChangeListener);
    model.addValueListener(testAttribute, valueChangeListener);
    model.addValueSetListener(testAttribute, valueSetListener);

    model.setValue(testAttribute, 1);
    model.validate();
    model.validate(testAttribute);
    assertTrue(model.isValid());
    assertTrue(model.isValid(testAttribute));
    assertTrue(model.getValidObserver().isActive());
    assertTrue(valueSetCounter.get() == 1);
    assertTrue(valueChangeCounter.get() == 1);
    assertTrue(anyValueChangeCounter.get() == 1);

    model.setValue(testAttribute, 1);
    assertTrue(valueSetCounter.get() == 1);
    assertTrue(valueChangeCounter.get() == 1);
    assertTrue(anyValueChangeCounter.get() == 1);

    assertFalse(model.isNullable(testAttribute));
    assertTrue(!model.isValueNull(testAttribute));
    assertEquals(1, model.getValue(testAttribute));

    model.setValue(testAttribute, null);
    assertFalse(model.isValid());
    assertFalse(model.isValid(testAttribute));
    try {
      model.validate();
      fail();
    }
    catch (final ValidationException e) {}
    try {
      model.validate(testAttribute);
      fail();
    }
    catch (final ValidationException e) {}

    assertTrue(valueSetCounter.get() == 2);
    assertTrue(valueChangeCounter.get() == 2);
    assertTrue(anyValueChangeCounter.get() == 2);
    assertTrue(model.isValueNull(testAttribute));

    final TestAttribute nameAttribute = new TestAttribute();

    model.setValue(nameAttribute, "Name");
    assertTrue(valueSetCounter.get() == 2);
    assertTrue(valueChangeCounter.get() == 2);
    assertTrue(anyValueChangeCounter.get() == 3);

    assertNotNull(model.getValue(nameAttribute));
    assertNotNull(model.removeValue(nameAttribute));
    assertNull(model.getValue(nameAttribute));
    assertNull(model.removeValue(nameAttribute));

    model.removeValueListener(testAttribute, valueChangeListener);
    model.removeValueSetListener(testAttribute, valueSetListener);
  }

  private static final class TestAttribute implements Attribute {
    @Override
    public String getCaption() {return null;}
    @Override
    public String getDescription() {return null;}
    @Override
    public Class<?> getTypeClass() {return null;}
    @Override
    public void validateType(final Object value) {}
  }
}
