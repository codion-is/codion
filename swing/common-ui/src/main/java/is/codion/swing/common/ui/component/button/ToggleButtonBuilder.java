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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JToggleButton;
import java.util.function.Supplier;

/**
 * Builds a JToggleButton.
 * @param <C> the component type
 * @param <B> the builder type
 */
public interface ToggleButtonBuilder<C extends JToggleButton, B extends ToggleButtonBuilder<C, B>> extends ButtonBuilder<C, Boolean, B> {

	/**
	 * @param toggleControl the toggle control to base this toggle button on
	 * @return this builder instance
	 */
	B toggle(ToggleControl toggleControl);

	/**
	 * @param toggleControl the toggle control to base this toggle button on
	 * @return this builder instance
	 */
	B toggle(Supplier<ToggleControl> toggleControl);

	/**
	 * Creates a bidirectional link to the given state. Overrides any initial value set.
	 * @param linkedState a state to link to the component value
	 * @return this builder instance
	 */
	B link(State linkedState);

	/**
	 * Creates a read-only link to the given {@link ObservableState}.
	 * @param linkedState a state to link to the component value
	 * @return this builder instance
	 */
	B link(ObservableState linkedState);

	/**
	 * @param <C> the component type
	 * @param <B> the builder type
	 * @return a builder for a component
	 */
	static <C extends JToggleButton, B extends ToggleButtonBuilder<C, B>> ToggleButtonBuilder<C, B> builder() {
		return new DefaultToggleButtonBuilder<>();
	}
}
