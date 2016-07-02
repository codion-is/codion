/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap;

import org.jminor.common.EventListener;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultValueMapValidatorTest {

  @Test(expected = NullValidationException.class)
  public void nullValidation() throws ValidationException {
    final ValueMap.Validator<String, ValueMap<String, Integer>> validator = new DefaultValueMapValidator<String, ValueMap<String, Integer>>() {
      @Override
      public boolean isNullable(final ValueMap<String, Integer> valueMap, final String key) {
        return super.isNullable(valueMap, key) && !key.equals("1");
      }
    };
    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.put("1", null);
    validator.validate(map);
    map.put("1", 1);
    validator.validate(map);
    map.put("2", null);
    validator.validate(map);
  }

  @Test
  public void isValid() {
    final DefaultValueMapValidator<String, ValueMap<String, Integer>> validator = new DefaultValueMapValidator<String, ValueMap<String, Integer>>() {
      @Override
      public void validate(final ValueMap<String, Integer> valueMap, final String key) throws ValidationException {
        final Integer value = valueMap.get("1");
        if (value.equals(1)) {
          throw new ValidationException("1", 1, "Invalid");
        }
      }
    };
    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.put("1", 0);
    assertTrue(validator.isValid(map));
    map.put("1", 1);
    assertFalse(validator.isValid(map));
  }

  @Test(expected = ValidationException.class)
  public void validate() throws ValidationException {
    final DefaultValueMapValidator<String, ValueMap<String, Integer>> validator = new DefaultValueMapValidator<String, ValueMap<String, Integer>>() {
      @Override
      public void validate(final ValueMap<String, Integer> valueMap, final String key) throws ValidationException {
        super.validate(valueMap, key);
        throw new ValidationException("1", valueMap.get("1"), "Invalid");
      }
    };
    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.put("1", 1);

    validator.validate(map);
  }

  @Test
  public void revalidate() {
    final AtomicInteger counter = new AtomicInteger();
    final DefaultValueMapValidator<String, ValueMap<String, Integer>> validator = new DefaultValueMapValidator<>();
    final EventListener listener = counter::incrementAndGet;
    validator.addRevalidationListener(listener);
    validator.revalidate();
    assertEquals(1, counter.get());
    validator.removeRevalidationListener(listener);
    validator.revalidate();
    assertEquals(1, counter.get());
  }
}
