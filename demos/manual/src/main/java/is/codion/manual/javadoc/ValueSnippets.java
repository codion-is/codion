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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.javadoc;

import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Notify;

import org.jspecify.annotations.Nullable;

/**
 * The {@link Value} javadoc snippets, each the region of the same name.
 */
final class ValueSnippets {

  void nullable() {
    Value<Integer> value = Value.nullable(); // @start region=nullable
    value.set(42);
    value.addConsumer(this::onValueChange);
    value.isNullable(); // true // @end
  }

  void nonNull() {
    Value<Boolean> value = Value.nonNull(false); // @start region=nonNull
    value.set(true);
    value.set(null);
    value.get(); // false
    value.isNullable(); // false // @end
  }

  void builder() {
    Value<String> value = Value.builder() // @start region=builder
            .nonNull("none")
            .value("hello")                  // the initial value
            .notify(Notify.SET)              // notifies listeners when set
            .validator(this::validateString) // using a validator
            .listener(this::onStringSet)     // and a listener
            .build();
    value.isNullable(); // false
    value.set("hey");
    value.set(null); // reverts to the null substitute: "none" // @end
  }

  void update() {
    Value<Integer> value = Value.nonNull(0); // @start region=update

    // increment the value by one
    value.update(currentValue -> currentValue + 1); // @end
  }

  private void onValueChange(@Nullable Integer value) {}

  private void validateString(@Nullable String value) {}

  private void onStringSet() {}
}
