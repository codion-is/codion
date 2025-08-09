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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import javax.swing.Action;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * A builder for Controls
 */
public interface ControlsBuilder extends ControlBuilder<Controls, ControlsBuilder> {

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
