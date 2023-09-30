/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2012 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import is.codion.common.event.EventObserver;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A read only value observer
 * @param <T> the type of the value
 */
public interface ValueObserver<T> extends EventObserver<T>, Supplier<T> {

  /**
   * @return an {@link Optional} wrapping this value.
   */
  default Optional<T> optional() {
    if (nullable()) {
      return Optional.ofNullable(get());
    }

    return Optional.of(get());
  }

  /**
   * @return true if the underlying value is null.
   */
  default boolean isNull() {
    return get() == null;
  }

  /**
   * @return true if the underlying value is not null.
   */
  default boolean isNotNull() {
    return !isNull();
  }

  /**
   * If false then get() is guaranteed to never return null.
   * @return true if this value can be null
   */
  boolean nullable();

  /**
   * Returns true if the underlying value is equal to the given one. Note that null == null.
   * @param value the value
   * @return true if the underlying value is equal to the given one
   */
  default boolean equalTo(T value) {
    return Objects.equals(get(), value);
  }

  /**
   * Returns true if the underlying value is NOT equal to the given one. Note that null == null.
   * @param value the value
   * @return true if the underlying value is NOT equal to the given one
   */
  default boolean notEqualTo(T value) {
    return !equalTo(value);
  }
}
