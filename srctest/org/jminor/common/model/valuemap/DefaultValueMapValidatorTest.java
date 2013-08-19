/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class DefaultValueMapValidatorTest {

  @Test(expected = NullValidationException.class)
  public void nullValidation() throws ValidationException {
    final ValueMap.Validator<String, ValueMap<String, Integer>> validator = new DefaultValueMapValidator<String, ValueMap<String, Integer>>() {
      @Override
      public boolean isNullable(final ValueMap<String, Integer> valueMap, final String key) {
        return super.isNullable(valueMap, key) && !key.equals("1");
      }
    };
    final ValueMap<String, Integer> map = new DefaultValueMap<String, Integer>();
    map.setValue("1", null);
    validator.validate(map);
    map.setValue("1", 1);
    validator.validate(map);
    map.setValue("2", null);
    validator.validate(map);
  }

  @Test(expected = ValidationException.class)
  public void validate() throws ValidationException {
    final DefaultValueMapValidator<String, ValueMap<String, Integer>> validator = new DefaultValueMapValidator<String, ValueMap<String, Integer>>() {
      @Override
      public void validate(final ValueMap<String, Integer> valueMap, final String key) throws ValidationException {
        super.validate(valueMap, key);
        throw new ValidationException("1", valueMap.getValue("1"), "Invalid");
      }
    };
    final ValueMap<String, Integer> map = new DefaultValueMap<String, Integer>();
    map.setValue("1", 1);

    validator.validate(map);
  }

  @Test
  public void revalidate() {
    final Collection<Object> counter = new ArrayList<Object>();
    final DefaultValueMapValidator<String, ValueMap<String, Integer>> validator = new DefaultValueMapValidator<String, ValueMap<String, Integer>>();
    final EventListener listener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        counter.add(new Object());
      }
    };
    validator.addRevalidationListener(listener);
    validator.revalidate();
    assertEquals(1, counter.size());
    validator.removeRevalidationListener(listener);
    validator.revalidate();
    assertEquals(1, counter.size());
  }
}
