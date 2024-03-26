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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.Collection;
import java.util.Set;

final class DefaultValueSetObserver<T> extends DefaultValueObserver<Set<T>> implements ValueSetObserver<T> {

  DefaultValueSetObserver(ValueSet<T> valueSet) {
    super(valueSet);
  }

  @Override
  public boolean contains(T value) {
    ValueSet<T> valueSet = value();

    return valueSet.contains(value);
  }

  @Override
  public boolean containsAll(Collection<T> values) {
    ValueSet<T> valueSet = value();

    return valueSet.containsAll(values);
  }

  @Override
  public boolean empty() {
    ValueSet<T> valueSet = value();

    return valueSet.empty();
  }

  @Override
  public boolean notEmpty() {
    ValueSet<T> valueSet = value();

    return valueSet.notEmpty();
  }
}
