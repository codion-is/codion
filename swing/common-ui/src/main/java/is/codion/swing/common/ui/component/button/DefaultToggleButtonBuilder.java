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

import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;

import org.jspecify.annotations.Nullable;

import javax.swing.JToggleButton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

class DefaultToggleButtonBuilder<C extends JToggleButton, B extends ToggleButtonBuilder<C, B>>
				extends AbstractButtonBuilder<Boolean, C, B> implements ToggleButtonBuilder<C, B> {

	private final List<State> linkedStates = new ArrayList<>(1);
	private final List<ObservableState> linkedObservableStates = new ArrayList<>(1);

	private @Nullable ToggleControl toggleControl;

	DefaultToggleButtonBuilder() {
		value(false);
	}

	@Override
	public final B toggle(ToggleControl toggleControl) {
		if (requireNonNull(toggleControl).value().isNullable() && !supportsNull()) {
			throw new IllegalArgumentException("This toggle button builder does not support a nullable value");
		}
		this.toggleControl = toggleControl;
		value(toggleControl.value().get());
		action(toggleControl);
		return self();
	}

	@Override
	public final B toggle(Control.Builder<ToggleControl, ?> toggleControlBuilder) {
		return toggle(requireNonNull(toggleControlBuilder).build());
	}

	@Override
	public final B link(State linkedState) {
		linkedStates.add(requireNonNull(linkedState));
		return (B) this;
	}

	@Override
	public final B link(ObservableState linkedState) {
		linkedObservableStates.add(requireNonNull(linkedState));
		return (B) this;
	}

	protected JToggleButton createToggleButton() {
		return new JToggleButton();
	}

	@Override
	protected final C createButton() {
		JToggleButton toggleButton = createToggleButton();
		if (toggleControl != null) {
			toggleButton.setModel(createButtonModel(toggleControl));
		}

		return (C) toggleButton;
	}

	@Override
	protected final ComponentValue<Boolean, C> createComponentValue(JToggleButton component) {
		ComponentValue<Boolean, ?> componentValue = component instanceof NullableCheckBox ?
						new BooleanNullableCheckBoxValue((NullableCheckBox) component) :
						new BooleanToggleButtonValue<>(component);
		linkedStates.forEach(state -> componentValue.link(state.value()));
		linkedObservableStates.forEach(state -> state.addConsumer(new SetComponentValue(componentValue)));

		return (ComponentValue<Boolean, C>) componentValue;
	}

	private static final class SetComponentValue implements Consumer<Boolean> {

		private final ComponentValue<Boolean, ?> componentValue;

		private SetComponentValue(ComponentValue<Boolean, ?> componentValue) {
			this.componentValue = componentValue;
		}

		@Override
		public void accept(Boolean value) {
			componentValue.set(value);
		}
	}
}
