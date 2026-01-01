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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;
import is.codion.swing.common.ui.layout.Layouts;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

final class DefaultButtonPanelBuilder extends AbstractControlPanelBuilder<JPanel, ButtonPanelBuilder>
				implements ButtonPanelBuilder {

	static final ControlsStep<JPanel, ButtonPanelBuilder> CONTROLS = new ButtonPanelControlsStep();

	private final Controls controls;

	private int buttonGap = Layouts.GAP.getOrThrow();
	private boolean fixedButtonSize = true;
	private @Nullable ButtonGroup buttonGroup;

	DefaultButtonPanelBuilder(Controls controls) {
		this.controls = controls;
	}

	@Override
	public ButtonPanelBuilder buttonGap(int buttonGap) {
		this.buttonGap = buttonGap;
		return this;
	}

	@Override
	public ButtonPanelBuilder fixedButtonSize(boolean fixedButtonSize) {
		this.fixedButtonSize = fixedButtonSize;
		return this;
	}

	@Override
	public ButtonPanelBuilder buttonGroup(@Nullable ButtonGroup buttonGroup) {
		this.buttonGroup = buttonGroup;
		return this;
	}

	@Override
	protected JPanel createComponent() {
		JPanel panel = createPanel();
		new ButtonControlHandler(panel, controls);

		return panel;
	}

	@Override
	protected void enableTransferFocusOnEnter(JPanel panel, TransferFocusOnEnter transferFocusOnEnter) {
		Stream.of(panel.getComponents())
						.filter(AbstractButton.class::isInstance)
						.map(AbstractButton.class::cast)
						.forEach(transferFocusOnEnter::enable);
	}

	private JPanel createPanel() {
		FlexibleGridLayout.Builder layout = FlexibleGridLayout.builder()
						.fixColumnWidths(fixedButtonSize)
						.fixRowHeights(fixedButtonSize);

		return new JPanel(orientation() == SwingConstants.HORIZONTAL ?
						layout.rowsColumns(1, 0)
										.horizontalGap(buttonGap)
										.verticalGap(0)
										.build() :
						layout.rowsColumns(0, 1)
										.horizontalGap(0)
										.verticalGap(buttonGap)
										.build());
	}

	private final class ButtonControlHandler extends ControlHandler {

		private final JPanel panel;

		private ButtonControlHandler(JPanel panel, Controls controls) {
			this.panel = panel;
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
			JToggleButton button = toggleButtonBuilder().toggle(toggleControl).build();
			if (buttonGroup != null) {
				buttonGroup.add(button);
			}
			panel.add(button);
		}

		@Override
		void onControls(Controls controls) {
			JPanel controlPanel = createPanel();
			new ButtonControlHandler(controlPanel, controls);
			panel.add(controlPanel);
		}

		@Override
		void onAction(Action action) {
			AbstractButton button = buttonBuilder().action(action).build();
			if (buttonGroup != null) {
				buttonGroup.add(button);
			}
			panel.add(button);
		}
	}

	private static final class ButtonPanelControlsStep implements ControlsStep<JPanel, ButtonPanelBuilder> {

		@Override
		public ButtonPanelBuilder action(Action action) {
			return controls(Controls.builder()
							.action(requireNonNull(action))
							.build());
		}

		@Override
		public ButtonPanelBuilder control(Control control) {
			return controls(Controls.builder()
							.control(requireNonNull(control))
							.build());
		}

		@Override
		public ButtonPanelBuilder control(Supplier<? extends Control> control) {
			return controls(Controls.builder()
							.control(requireNonNull(control))
							.build());
		}

		@Override
		public ButtonPanelBuilder controls(Controls controls) {
			return new DefaultButtonPanelBuilder(requireNonNull(controls));
		}

		@Override
		public ButtonPanelBuilder controls(Supplier<Controls> controls) {
			return new DefaultButtonPanelBuilder(requireNonNull(controls).get());
		}
	}
}
