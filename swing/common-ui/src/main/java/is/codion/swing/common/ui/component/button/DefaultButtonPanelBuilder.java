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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.GridLayout;

final class DefaultButtonPanelBuilder extends AbstractControlPanelBuilder<JPanel, ButtonPanelBuilder>
				implements ButtonPanelBuilder {

	private int buttonGap = Layouts.GAP.getOrThrow();

	DefaultButtonPanelBuilder(Action... actions) {
		this(Controls.controls(actions));
	}

	DefaultButtonPanelBuilder(Controls controls) {
		super(controls);
	}

	@Override
	public ButtonPanelBuilder buttonGap(int buttonGap) {
		this.buttonGap = buttonGap;
		return this;
	}

	@Override
	protected JPanel createComponent() {
		JPanel panel = createPanel();
		new ButtonControlHandler(panel, controls(), buttonBuilder(), toggleButtonBuilder());

		return panel;
	}

	private JPanel createPanel() {
		return new JPanel(orientation() == SwingConstants.HORIZONTAL ?
						new GridLayout(1, 0, buttonGap, 0) :
						new GridLayout(0, 1, 0, buttonGap));
	}

	private final class ButtonControlHandler extends ControlHandler {

		private final JPanel panel;
		private final ButtonBuilder<?, ?, ?> buttonBuilder;
		private final ToggleButtonBuilder<?, ?> toggleButtonBuilder;

		private ButtonControlHandler(JPanel panel, Controls controls,
																 ButtonBuilder<?, ?, ?> buttonBuilder,
																 ToggleButtonBuilder<?, ?> toggleButtonBuilder) {
			this.panel = panel;
			this.buttonBuilder = buttonBuilder;
			this.toggleButtonBuilder = toggleButtonBuilder;
			controls.actions().forEach(this);
		}

		@Override
		void onSeparator() {
			panel.add(new JLabel());
		}

		@Override
		void onControl(Control control) {
			onAction(control);
		}

		@Override
		void onToggleControl(ToggleControl toggleControl) {
			panel.add(toggleButtonBuilder.toggleControl(toggleControl).build());
		}

		@Override
		void onControls(Controls controls) {
			JPanel controlPanel = createPanel();
			new ButtonControlHandler(controlPanel, controls, buttonBuilder, toggleButtonBuilder);
			panel.add(controlPanel);
		}

		@Override
		void onAction(Action action) {
			panel.add(buttonBuilder.action(action).build());
		}
	}
}
