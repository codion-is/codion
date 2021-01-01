/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventObserver;

import java.util.Optional;

/**
 * A read only value observer
 * @param <V> the type of the value
 */
public interface ValueObserver<V> extends EventObserver<V> {

  /**
   * @return the value
   */
  V get();

  /**
   * @return an {@link Optional} wrapping this value.
   */
  Optional<V> toOptional();

  /**
   * @return true if the underlying value is null.
   */
  boolean isNull();

  /**
   * If false then get() is guaranteed to never return null.
   * @return true if this value can be null
   */
  boolean isNullable();
}
