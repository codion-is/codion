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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.swing.common.ui.control.DefaultControls.DefaultConfig;
import is.codion.swing.common.ui.control.DefaultControls.DefaultControlsBuilder;

import javax.swing.Action;
import java.util.Collection;
import java.util.List;

/**
 * A collection of controls and separators, note that these can be nested controls.
 */
public interface Controls extends Control {

	/**
	 * A placeholder representing a separator in a Controls instance.
	 * All methods throw {@link UnsupportedOperationException}.
	 */
	Action SEPARATOR = new DefaultControls.DefaultSeparator();

	/**
	 * @return an unmodifiable view of the actions in this set
	 */
	List<Action> actions();

	/**
	 * @return the number of controls in this controls instance
	 */
	int size();

	/**
	 * @return true if this controls instance contains no controls (ignoring separators)
	 */
	boolean empty();

	/**
	 * @return true if this controls instance contains controls (ignoring separators)
	 */
	boolean notEmpty();

	/**
	 * @param index the index
	 * @return the action at the given index
	 */
	Action get(int index);

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
	 * @param controls the available controls
	 * @param defaults the default controls layout
	 * @return a new Config instance
	 */
	static Config config(ControlMap controls, List<ControlKey<?>> defaults) {
		return new DefaultConfig(controls, defaults);
	}

	/**
	 * A builder for Controls
	 */
	interface ControlsBuilder extends Control.Builder<Controls, ControlsBuilder> {

		/**
		 * @param control the control to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder control(Control control);

		/**
		 * @param controlBuilder the control builder to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder control(Control.Builder<?, ?> controlBuilder);

		/**
		 * @param index the index at which to add the control
		 * @param control the control to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder controlAt(int index, Control control);

		/**
		 * @param index the index at which to add the control
		 * @param controlBuilder the control builder to add to this controls instance
		 * @return this Builder instance
		 */
		ControlsBuilder controlAt(int index, Control.Builder<?, ?> controlBuilder);

		/**
		 * @param controls the controls to add
		 * @return this Builder instance
		 */
		ControlsBuilder controls(Control... controls);

		/**
		 * @param controlBuilders the control builder to add
		 * @return this Builder instance
		 */
		ControlsBuilder controls(Control.Builder<?, ?>... controlBuilders);

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
	 * Configures controls.
	 */
	interface Config {

		/**
		 * Adds a separator
		 * @return this config instance
		 */
		Config separator();

		/**
		 * Adds a standard control
		 * @param controlKey the control key
		 * @return this config instance
		 */
		Config standard(ControlKey<?> controlKey);

		/**
		 * @param control the control to add
		 * @return this config instance
		 */
		Config control(Control control);

		/**
		 * @param controlBuilder the builder for the control to add
		 * @return this config instance
		 */
		Config control(Control.Builder<?, ?> controlBuilder);

		/**
		 * @param action the Action to add
		 * @return this config instance
		 */
		Config action(Action action);

		/**
		 * Adds all remaining default controls
		 * @return this config instance
		 */
		Config defaults();

		/**
		 * Adds all remaining default controls, up until and including {@code stopAt}
		 * @param stopAt the table control to stop at (inclusive)
		 * @return this config instance
		 */
		Config defaults(ControlKey<?> stopAt);

		/**
		 * Clears all controls from this config
		 * @return this config instance
		 */
		Config clear();

		/**
		 * @return a {@link Controls} instance based on this config
		 */
		Controls create();
	}
}
