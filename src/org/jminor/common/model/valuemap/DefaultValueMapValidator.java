/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;

/**
 * A default value map validator implementation, which performs basic null validation.
 */
public class DefaultValueMapValidator<K, V> implements ValueMapValidator<K, V> {

  /** {@inheritDoc} */
  @Override
  public boolean isNullable(final ValueMap<K, V> valueMap, final K key) {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValid(final ValueMap<K, V> valueMap, final int action) {
    try {
      validate(valueMap, action);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  /** {@inheritDoc} */
  @Override
  public void validate(final ValueMap<K, V> valueMap, final int action) throws ValidationException {
    Util.rejectNullValue(valueMap, "valueMap");
    for (final K key : valueMap.getValueKeys()) {
      validate(valueMap, key, action);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void validate(final ValueMap<K, V> valueMap, final K key, final int action) throws ValidationException {
    Util.rejectNullValue(valueMap, "valueMap");
    if (valueMap.isValueNull(key) && !isNullable(valueMap, key)) {
      throw new NullValidationException(key, Messages.get(Messages.VALUE_MISSING) + ": " + key);
    }
  }
}
