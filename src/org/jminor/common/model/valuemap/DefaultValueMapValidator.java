/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;

/**
 * User: Björn Darri
 * Date: 26.7.2010
 * Time: 23:12:16
 */
public class DefaultValueMapValidator<K, V> implements ValueMapValidator<K, V> {

  public boolean isNullable(final ValueMap<K, V> valueMap, final K key) {
    return true;
  }

  public boolean isValid(final ValueMap<K, V> valueMap, final int action) {
    try {
      validate(valueMap, action);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  public void validate(final ValueMap<K, V> valueMap, final int action) throws ValidationException {
    Util.rejectNullValue(valueMap, "valueMap");
    for (final K key : valueMap.getValueKeys()) {
      validate(valueMap, key, action);
    }
  }

  public void validate(final ValueMap<K, V> valueMap, final K key, final int action) throws ValidationException {
    Util.rejectNullValue(valueMap, "valueMap");
    if (valueMap.isValueNull(key) && !isNullable(valueMap, key)) {
      throw new NullValidationException(key, Messages.get(Messages.VALUE_MISSING) + ": " + key);
    }
  }
}
