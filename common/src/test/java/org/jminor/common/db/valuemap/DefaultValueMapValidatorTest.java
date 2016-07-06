/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap;

import org.jminor.common.EventListener;
import org.jminor.common.db.Attribute;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultValueMapValidatorTest {

  @Test(expected = NullValidationException.class)
  public void nullValidation() throws ValidationException {
    final TestAttribute testAttribute = new TestAttribute();
    final ValueMap.Validator<TestAttribute, ValueMap<TestAttribute, Integer>> validator =
            new DefaultValueMapValidator<TestAttribute, ValueMap<TestAttribute, Integer>>() {
      @Override
      public boolean isNullable(final ValueMap<TestAttribute, Integer> valueMap, final TestAttribute key) {
        return super.isNullable(valueMap, key) && !key.equals(testAttribute);
      }
    };
    final ValueMap<TestAttribute, Integer> map = new DefaultValueMap<>();
    map.put(testAttribute, null);
    validator.validate(map);
    map.put(testAttribute, 1);
    validator.validate(map);
    map.put(testAttribute, null);
    validator.validate(map);
  }

  @Test
  public void isValid() {
    final TestAttribute testAttribute = new TestAttribute();
    final DefaultValueMapValidator<TestAttribute, ValueMap<TestAttribute, Integer>> validator =
            new DefaultValueMapValidator<TestAttribute, ValueMap<TestAttribute, Integer>>() {
      @Override
      public void validate(final ValueMap<TestAttribute, Integer> valueMap, final TestAttribute key) throws ValidationException {
        final Integer value = valueMap.get(testAttribute);
        if (value.equals(1)) {
          throw new ValidationException(testAttribute, 1, "Invalid");
        }
      }
    };
    final ValueMap<TestAttribute, Integer> map = new DefaultValueMap<>();
    map.put(testAttribute, 0);
    assertTrue(validator.isValid(map));
    map.put(testAttribute, 1);
    assertFalse(validator.isValid(map));
  }

  @Test(expected = ValidationException.class)
  public void validate() throws ValidationException {
    final TestAttribute testAttribute = new TestAttribute();
    final DefaultValueMapValidator<TestAttribute, ValueMap<TestAttribute, Integer>> validator =
            new DefaultValueMapValidator<TestAttribute, ValueMap<TestAttribute, Integer>>() {
      @Override
      public void validate(final ValueMap<TestAttribute, Integer> valueMap, final TestAttribute key) throws ValidationException {
        super.validate(valueMap, key);
        throw new ValidationException(testAttribute, valueMap.get(testAttribute), "Invalid");
      }
    };
    final ValueMap<TestAttribute, Integer> map = new DefaultValueMap<>();
    map.put(testAttribute, 1);

    validator.validate(map);
  }

  @Test
  public void revalidate() {
    final AtomicInteger counter = new AtomicInteger();
    final DefaultValueMapValidator<TestAttribute, ValueMap<TestAttribute, Integer>> validator = new DefaultValueMapValidator<>();
    final EventListener listener = counter::incrementAndGet;
    validator.addRevalidationListener(listener);
    validator.revalidate();
    assertEquals(1, counter.get());
    validator.removeRevalidationListener(listener);
    validator.revalidate();
    assertEquals(1, counter.get());
  }

  private static final class TestAttribute implements Attribute {
    @Override
    public String getCaption() {return null;}
    @Override
    public String getDescription() {return null;}
    @Override
    public Class<?> getTypeClass() {return null;}
  }
}
