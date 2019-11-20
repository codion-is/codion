package org.jminor.common.db.valuemap;

import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;

import java.util.Locale;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * A default value map validator implementation, which performs basic null validation.
 * @param <K> the type identifying the keys in the value map
 * @param <V> the value map type
 */
public class DefaultValueMapValidator<K, V extends ValueMap<K, ?>> implements ValueMap.Validator<K, V> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultValueMapValidator.class.getName(), Locale.getDefault());

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
    requireNonNull(valueMap, "valueMap");
    for (final K key : valueMap.keySet()) {
      validate(valueMap, key);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void validate(final V valueMap, final K key) throws ValidationException {
    requireNonNull(valueMap, "valueMap");
    if (valueMap.isNull(key) && !isNullable(valueMap, key)) {
      throw new NullValidationException(key, MESSAGES.getString("value_missing") + ": " + key);
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
