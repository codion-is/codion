/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;

/**
 * A default value map validator implementation, which performs basic null validation.
 */
public class DefaultValueMapValidator<K, V extends ValueMap<K, ?>> implements ValueMap.Validator<K, V> {

  private final Event evtRevalidate = Events.event();

  /** {@inheritDoc} */
  @Override
  public boolean isNullable(final V valueMap, final K key) {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValid(final V valueMap) {
    try {
      validate(valueMap);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  /** {@inheritDoc} */
  @Override
  public void validate(final V valueMap) throws ValidationException {
    Util.rejectNullValue(valueMap, "valueMap");
    for (final K key : valueMap.getValueKeys()) {
      validate(valueMap, key);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void validate(final V valueMap, final K key) throws ValidationException {
    Util.rejectNullValue(valueMap, "valueMap");
    if (valueMap.isValueNull(key) && !isNullable(valueMap, key)) {
      throw new NullValidationException(key, Messages.get(Messages.VALUE_MISSING) + ": " + key);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void revalidate() {
    evtRevalidate.fire();
  }

  /** {@inheritDoc} */
  @Override
  public final void addRevalidationListener(final EventListener listener) {
    evtRevalidate.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeRevalidationListener(final EventListener listener) {
    evtRevalidate.removeListener(listener);
  }
}
