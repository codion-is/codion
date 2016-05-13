/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.Util;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;

/**
 * A default value map validator implementation, which performs basic null validation.
 * @param <K> the type identifying the keys in the value map
 * @param <V> the value map type
 */
public class DefaultValueMapValidator<K, V extends ValueMap<K, ?>> implements ValueMap.Validator<K, V> {

  private final Event revalidateEvent = Events.event();

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
    catch (final ValidationException e) {
      return false;
    }
  }

  /** {@inheritDoc} */
  @Override
  public void validate(final V valueMap) throws ValidationException {
    Util.rejectNullValue(valueMap, "valueMap");
    for (final K key : valueMap.keySet()) {
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
    revalidateEvent.fire();
  }

  /** {@inheritDoc} */
  @Override
  public final void addRevalidationListener(final EventListener listener) {
    revalidateEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeRevalidationListener(final EventListener listener) {
    revalidateEvent.removeListener(listener);
  }
}
