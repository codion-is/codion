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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.swing.common.ui.control;

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.event.ActionEvent;

public final class ControlsDemo {

	static void control() {
		// tag::control[]
		State somethingEnabledState = State.state(true);

		CommandControl control = Control.builder()
						.command(() -> System.out.println("Doing something"))
						.caption("Do something")
						.mnemonic('D')
						.enabled(somethingEnabledState)
						.build();

		JButton somethingButton = new JButton(control);

		Control.ActionCommand actionCommand = actionEvent -> {
			if ((actionEvent.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
				System.out.println("Doing something else");
			}
		};
		CommandControl actionControl = Control.builder()
						.action(actionCommand)
						.caption("Do something else")
						.mnemonic('S')
						.build();

		JButton somethingElseButton = new JButton(actionControl);
		// end::control[]

		// tag::toggleControl[]
		State state = State.state();

		ToggleControl toggleStateControl = Control.builder()
						.toggle(state)
						.build();

		JToggleButton toggleButton = Components.toggleButton()
						.toggle(toggleStateControl)
						.text("Change state")
						.mnemonic('C')
						.build();

		Value<Boolean> booleanValue = Value.nonNull(false);

		ToggleControl toggleValueControl = Control.builder()
						.toggle(booleanValue)
						.build();

		JCheckBox checkBox = Components.checkBox()
						.toggle(toggleValueControl)
						.text("Change value")
						.mnemonic('V')
						.build();

		Value<Boolean> nullableBooleanValue = Value.nullable();

		ToggleControl nullableToggleControl = Control.builder()
						.toggle(nullableBooleanValue)
						.build();

		NullableCheckBox nullableCheckBox = Components.nullableCheckBox()
						.toggle(nullableToggleControl)
						.build();
		// end::toggleControl[]
	}

	void controls() {
		// tag::controls[]
		Controls controls = Controls.builder()
						.control(Control.builder()
										.command(this::doFirst)
										.caption("First")
										.mnemonic('F'))
						.control(Control.builder()
										.command(this::doSecond)
										.caption("Second")
										.mnemonic('S'))
						.control(Controls.builder()
										.caption("Submenu")
										.control(Control.builder()
														.command(this::doSubFirst)
														.caption("Sub-first")
														.mnemonic('b'))
										.control(Control.builder()
														.command(this::doSubSecond)
														.caption("Sub-second")
														.mnemonic('u')))
						.build();

		JMenu menu = Components.menu()
						.controls(controls)
						.build();

		Control firstControl = Control.builder()
						.command(this::doFirst)
						.caption("First")
						.mnemonic('F')
						.build();
		Control secondControl = Control.builder()
						.command(this::doSecond)
						.caption("Second")
						.mnemonic('S')
						.build();

		Controls twoControls = Controls.builder()
						.controls(firstControl, secondControl)
						.build();

		JPanel buttonPanel = Components.buttonPanel()
						.controls(twoControls)
						.build();
		// end::controls[]
	}

	void doFirst() {}

	void doSecond() {}

	void doSubFirst() {}

	void doSubSecond() {}
}
