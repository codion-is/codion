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

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.ValueSet;

/**
 * The {@link State} javadoc snippets, each the region of the same name.
 */
final class StateSnippets {

  void usage() {
    State state = State.state(); // @start region=usage

    ObservableState observable = state.observable();

    observable.addConsumer(this::onStateChange);

    state.set(true);
    state.set(false);

    boolean value = state.is();

    ObservableState opposite = state.not(); // @end
  }

  void contains() {
    ValueSet<String> tags = ValueSet.valueSet(); // @start region=contains
    State containsImportant = State.contains(tags, "important");

    // State → Set
    containsImportant.set(true);
    tags.contains("important"); // true

    // Set → State
    tags.remove("important");
    containsImportant.is(); // false // @end
  }

  private void onStateChange(boolean state) {}
}
