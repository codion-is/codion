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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.swing.common.ui.control.DefaultControls.DefaultControlsBuilder;
import is.codion.swing.common.ui.control.DefaultControls.DefaultLayout;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A collection of controls and separators, note that these can be nested controls.
 */
public interface Controls extends Control {

	/**
	 * A placeholder representing a separator in a {@link Controls} instance.
	 * All methods throw {@link UnsupportedOperationException}.
	 */
	Action SEPARATOR = new DefaultControls.DefaultSeparator();

	/**
	 * @return an unmodifiable view of the actions in this {@link Controls} instance, including separators
	 */
	List<Action> actions();

	/**
	 * @return the number of actions in this {@link Controls} instance, ignoring separators
	 */
	int size();

	@Override
	ControlsBuilder copy();

	/**
	 * Constructs a new Controls instance.
	 * @param actions the actions
	 * @return a new Controls instance.
	 */
	static Controls controls(Action... actions) {
		return builder().actions(actions).build();
	}

	/**
	 * Constructs a new Controls instance.
	 * @param controls the controls
	 * @return a new Controls instance.
	 */
	static Controls controls(Control... controls) {
		return builder().controls(controls).build();
	}

	/**
	 * @return a new {@link ControlsBuilder} instance
	 */
	static ControlsBuilder builder() {
		return new DefaultControlsBuilder();
	}

	/**
	 * @param defaults the default controls layout
	 * @return a new Config instance
	 */
	static Layout layout(List<ControlKey<?>> defaults) {
		return new DefaultLayout(defaults);
	}

	/**
	 * @param name the controls name
	 * @return a new {@link ControlKey} for identifying a {@link Controls} instance
	 */
	static ControlsKey key(String name) {
		return key(name, null);
	}

	/**
	 * @param name the controls name
	 * @param defaultLayout the default controls layout
	 * @return a new {@link ControlsKey} for identifying a {@link Controls} instance
	 */
	static ControlsKey key(String name, @Nullable Layout defaultLayout) {
		return new DefaultControlsKey(name, null, defaultLayout);
	}

	/**
	 * A {@link ControlKey} for {@link Controls} instances
	 */
	interface ControlsKey extends ControlKey<Controls> {

		/**
		 * @return the default layout config, if available
		 */
		Optional<Layout> defaultLayout();
	}

	/**
	 * A builder for Controls
	 */
	interface ControlsBuilder extends ControlBuilder<Controls, ControlsBuilder> {

		/**
		 * @param control the control to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder control(Control control);

		/**
		 * @param control the control to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder control(Supplier<? extends Control> control);

		/**
		 * @param index the index at which to add the control
		 * @param control the control to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder controlAt(int index, Control control);

		/**
		 * @param index the index at which to add the control
		 * @param control the control to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder controlAt(int index, Supplier<? extends Control> control);

		/**
		 * @param controls the controls to add
		 * @return this Builder instance
		 */
		ControlsBuilder controls(Control... controls);

		/**
		 * @param controls the controls to add
		 * @return this Builder instance
		 */
		ControlsBuilder controls(Collection<? extends Control> controls);

		/**
		 * @param controls the controls to add
		 * @return this Builder instance
		 */
		ControlsBuilder controls(Supplier<? extends Control>... controls);

		/**
		 * @param action the Action to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder action(Action action);

		/**
		 * @param index the index at which to add the action
		 * @param action the Action to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder actionAt(int index, Action action);

		/**
		 * @param actions the Actions to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder actions(Action... actions);

		/**
		 * @param actions the Actions to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder actions(Collection<Action> actions);

		/**
		 * Adds a separator to the Controls
		 * @return this Builder instance
		 */
		ControlsBuilder separator();

		/**
		 * Adds a separator to the Controls
		 * @param index the index at which to insert the separator
		 * @return this Builder instance
		 */
		ControlsBuilder separatorAt(int index);

		/**
		 * @param action the action to remove
		 * @return this Builder instance
		 */
		ControlsBuilder remove(Action action);

		/**
		 * Removes all actions from this controls builder instance
		 * @return this Builder instance
		 */
		ControlsBuilder removeAll();
	}

	/**
	 * Specifies a layout for a {@link Controls} instance.
	 */
	interface Layout {

		/**
		 * Adds a separator
		 * @return this layout instance
		 */
		Layout separator();

		/**
		 * Adds a standard control
		 * @param controlKey the key identifying the standard control to add
		 * @return this layout instance
		 */
		Layout control(ControlKey<?> controlKey);

		/**
		 * @param control the control to add
		 * @return this layout instance
		 */
		Layout control(Control control);

		/**
		 * @param control the control to add
		 * @return this layout instance
		 */
		Layout control(Supplier<? extends Control> control);

		/**
		 * Adds standard controls
		 * @param controlsKey the key identifying the standard controls to add
		 * @return this layout instance
		 */
		Layout controls(ControlKey<Controls> controlsKey);

		/**
		 * Adds standard configurable controls
		 * @param controlsKey the controls key
		 * @param layout provides a way to configure the layout
		 * @return this layout instance
		 * @throws IllegalArgumentException in case the {@link Controls} associated with {@code controlsKey} does not provide a default layout
		 */
		Layout controls(ControlsKey controlsKey, Consumer<Layout> layout);

		/**
		 * @param action the Action to add
		 * @return this layout instance
		 */
		Layout action(Action action);

		/**
		 * Adds all remaining default controls
		 * @return this layout instance
		 */
		Layout defaults();

		/**
		 * Adds all remaining default controls, up until and including {@code stopAt}
		 * @param stopAt the standard control to stop at (inclusive)
		 * @return this layout instance
		 */
		Layout defaults(ControlKey<?> stopAt);

		/**
		 * Clears all controls from this layout
		 * @return this layout instance
		 */
		Layout clear();

		/**
		 * @return a copy of this {@link Layout} instance
		 */
		Layout copy();

		/**
		 * @param controlMap provides the standard controls
		 * @return a {@link Controls} instance based on this layout
		 */
		Controls create(ControlMap controlMap);
	}
}
