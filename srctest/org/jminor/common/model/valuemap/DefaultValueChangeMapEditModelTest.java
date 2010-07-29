/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.valuemap.exception.ValidationException;

import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

public class DefaultValueChangeMapEditModelTest {

  private final ValueChangeMap<String, Integer> valueMap = new ValueChangeMapImpl<String, Integer>();
  private final DefaultValueChangeMapEditModel<String, Integer> model =
          new DefaultValueChangeMapEditModel<String, Integer>(valueMap, new DefaultValueMapValidator<String, Integer>() {
    @Override
    public boolean isNullable(final ValueMap<String, Integer> valueMap, final String key) {
      return super.isNullable(valueMap, key) && false;
    }

    @Override
    public void validate(final ValueMap<String, Integer> valueMap, final String key, final int action) throws ValidationException {
      super.validate(valueMap, key, action);
      final Object value = valueMap.getValue(key);
      if (value.equals(-1)) {
        throw new ValidationException(key, -1, "nono");
      }
    }
  });

  private final Collection<Object> valueChangeCounter = new ArrayList<Object>();
  private final Collection<Object> valueSetCounter = new ArrayList<Object>();
  private final Collection<Object> valueMapSetCounter = new ArrayList<Object>();

  private final ValueChangeListener valueChangeListener = new ValueChangeListener() {
    @Override
    protected void valueChanged(final ValueChangeEvent event) {
      valueChangeCounter.add(new Object());
    }
  };
  private final ActionListener valueSetListener = new ActionListener() {
    public void actionPerformed(final ActionEvent e) {
      valueSetCounter.add(new Object());
    }
  };
  private final ActionListener valueMapSetListener = new ActionListener() {
    public void actionPerformed(final ActionEvent e) {
      valueMapSetCounter.add(new Object());
    }
  };

  @Test
  public void test() throws Exception {
    final String key = "key";
    model.addValueListener(key, valueChangeListener);
    model.addValueSetListener(key, valueSetListener);
    model.addValueMapSetListener(valueMapSetListener);

    model.setValue(key, 1);
    assertTrue(valueSetCounter.size() == 1);
    assertTrue(valueChangeCounter.size() == 1);

    model.setValue(key, 1);
    assertTrue(valueSetCounter.size() == 1);
    assertTrue(valueChangeCounter.size() == 1);

    assertFalse(model.isNullable(key));
    assertTrue(!model.isValueNull(key));
    assertEquals(Integer.valueOf(1), valueMap.getValue(key));
    assertEquals(Integer.valueOf(1), model.getValue(key));

    model.setValue(key, null);
    assertTrue(valueSetCounter.size() == 2);
    assertTrue(valueChangeCounter.size() == 2);
    assertTrue(model.isValueNull(key));

    assertTrue(model.stateModified().isActive());

    model.getValueMap();
    model.clear();
    model.refresh();

    assertNotNull(model.getDefaultValueMap());

    final ValueChangeMap<String, Integer> newMap = new ValueChangeMapImpl<String, Integer>();
    newMap.setValue(key, 1);

    model.setValueMap(newMap);
    assertEquals(Integer.valueOf(1), model.getValue(key));
    assertTrue(valueMapSetCounter.size() == 1);

    assertNotNull(model.getValidator());

    model.setValue(key, -1);
    try {
      model.validate(key, ValueMapValidator.UNKNOWN);
      fail();
    }
    catch (ValidationException e) {}

    assertFalse(model.isValid(key, -1));
    assertFalse(model.isValid());

    model.setValue(key, null);
    assertFalse(model.isValid(key, -1));
    assertFalse(model.isValid());

    model.setValue(key, 2);
    assertTrue(model.isValid(key, -1));
    assertTrue(model.isValid());

    model.removeValueListener(key, valueChangeListener);
    model.removeValueSetListener(key, valueSetListener);
    model.removeValueMapSetListener(valueMapSetListener);
  }
}
