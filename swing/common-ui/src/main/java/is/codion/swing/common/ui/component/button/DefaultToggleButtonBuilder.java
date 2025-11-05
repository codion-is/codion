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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JToggleButton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class DefaultToggleButtonBuilder<C extends JToggleButton, B extends ToggleButtonBuilder<C, B>>
				extends AbstractButtonBuilder<C, Boolean, B> implements ToggleButtonBuilder<C, B> {

	final List<ObservableState> linkedObservableStates = new ArrayList<>(1);

	DefaultToggleButtonBuilder() {}

	@Override
	public B toggle(ToggleControl toggleControl) {
		if (requireNonNull(toggleControl).value().isNullable() && !supportsNull()) {
			throw new IllegalArgumentException("This toggle button builder does not support a nullable value");
		}
		link(toggleControl.value());
		action(toggleControl);
		return self();
	}

	@Override
	public final B toggle(Supplier<ToggleControl> toggleControl) {
		return toggle(requireNonNull(toggleControl).get());
	}

	@Override
	public final B link(State linkedState) {
		link(requireNonNull(linkedState).value());
		return (B) this;
	}

	@Override
	public final B link(ObservableState linkedState) {
		linkedObservableStates.add(requireNonNull(linkedState));
		return (B) this;
	}

	@Override
	protected C createButton() {
		return (C) new JToggleButton();
	}

	@Override
	protected ComponentValue<C, Boolean> createComponentValue(JToggleButton component) {
		ComponentValue<?, Boolean> componentValue = new ToggleButtonValue<>(component);
		linkedObservableStates.forEach(state -> state.addConsumer(new SetComponentValue(componentValue)));

		return (ComponentValue<C, Boolean>) componentValue;
	}

	@Override
	protected boolean supportsNull() {
		return false;
	}

	static final class SetComponentValue implements Consumer<Boolean> {

		private final ComponentValue<?, Boolean> componentValue;

		SetComponentValue(ComponentValue<?, Boolean> componentValue) {
			this.componentValue = componentValue;
		}

		@Override
		public void accept(Boolean value) {
			componentValue.set(value);
		}
	}
}
