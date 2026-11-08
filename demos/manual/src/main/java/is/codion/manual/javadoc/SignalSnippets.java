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

import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.signal.Signal;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The {@link Signal} javadoc snippets, each the region of the same name.
 */
final class SignalSnippets {

  // Strongly referenced by this instance, for the weak region
  private final Runnable listener = this::doSomethingElse;
  private final Consumer<Boolean> consumer = this::onBoolean;

  void usage() {
    Signal<Boolean> signal = Signal.signal(); // @start region=usage

    signal.addListener(this::doSomething);

    signal.addConsumer(this::onBoolean);

    signal.accept(true);

    Observer<Boolean> observer = signal.observer();

    observer.addConsumer(this::onBooleanElsewhere); // @end
  }

  void weak(Observer<Boolean> observer) {
    // listener and consumer are fields // @start region=weak
    observer.addWeakListener(listener);
    observer.addWeakConsumer(consumer); // @end
  }

  private void doSomething() {}

  private void doSomethingElse() {}

  private void onBoolean(@Nullable Boolean value) {}

  private void onBooleanElsewhere(@Nullable Boolean value) {}
}
