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
package is.codion.common.model.selection;

import is.codion.common.reactive.sensor.Sensor;
import is.codion.common.reactive.signal.Signal;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.Value;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

final class DefaultSingleSelection<T> implements SingleSelection<T> {

	private final SelectedItem selected;

	DefaultSingleSelection() {
		selected = new SelectedItem();
	}

	@Override
	public ObservableState present() {
		return selected.present.observable();
	}

	@Override
	public Sensor changing() {
		return selected.changing;
	}

	@Override
	public Value<T> item() {
		return selected;
	}

	@Override
	public void clear() {
		selected.clear();
	}

	private final class SelectedItem extends AbstractValue<T> {

		private final Signal<T> changing = Signal.signal();
		private final State present = State.state();

		private @Nullable T item = null;

		private SelectedItem() {}

		@Override
		protected @Nullable T getValue() {
			return item;
		}

		@Override
		protected void setValue(@Nullable T value) {
			setSelectedItem(value);
		}

		private void setSelectedItem(@Nullable T item) {
			if (!Objects.equals(this.item, item)) {
				changing.accept(item);
				this.item = item;
				present.set(item != null);
				notifyObserver();
			}
		}
	}
}
