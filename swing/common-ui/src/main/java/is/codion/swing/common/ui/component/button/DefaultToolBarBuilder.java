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

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.Action;
import javax.swing.JToolBar;
import java.util.ArrayList;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultToolBarBuilder extends AbstractControlPanelBuilder<JToolBar, ToolBarBuilder> implements ToolBarBuilder {

	static final ControlsStep<JToolBar, ToolBarBuilder> CONTROLS = new ButtonPanelControlsStep();

	private final Controls controls;

	private boolean floatable = true;
	private boolean rollover = false;
	private boolean borderPainted = true;

	DefaultToolBarBuilder(Controls controls) {
		this.controls = controls;
		includeButtonText(false);
	}

	@Override
	public ToolBarBuilder floatable(boolean floatable) {
		this.floatable = floatable;
		return this;
	}

	@Override
	public ToolBarBuilder rollover(boolean rollover) {
		this.rollover = rollover;
		return this;
	}

	@Override
	public ToolBarBuilder borderPainted(boolean borderPainted) {
		this.borderPainted = borderPainted;
		return this;
	}

	@Override
	protected JToolBar createComponent() {
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(floatable);
		toolBar.setOrientation(orientation());
		toolBar.setRollover(rollover);
		toolBar.setBorderPainted(borderPainted);

		new ToolBarControlHandler(toolBar, controls);

		return toolBar;
	}

	private final class ToolBarControlHandler extends ControlHandler {

		private final JToolBar toolBar;

		private ToolBarControlHandler(JToolBar toolBar, Controls controls) {
			this.toolBar = toolBar;
			cleanupSeparators(new ArrayList<>(controls.actions())).forEach(this);
		}

		@Override
		void onSeparator() {
			toolBar.addSeparator();
		}

		@Override
		void onControl(Control control) {
			onAction(control);
		}

		@Override
		void onToggleControl(ToggleControl toggleControl) {
			toolBar.add(toggleButtonBuilder()
							.toggle(toggleControl)
							.build());
		}

		@Override
		void onControls(Controls controls) {
			new ToolBarControlHandler(toolBar, controls);
		}

		@Override
		void onAction(Action action) {
			toolBar.add(buttonBuilder()
							.action(action)
							.build());
		}
	}

	private static final class ButtonPanelControlsStep implements ControlsStep<JToolBar, ToolBarBuilder> {

		@Override
		public ToolBarBuilder action(Action action) {
			return controls(Controls.builder()
							.action(requireNonNull(action))
							.build());
		}

		@Override
		public ToolBarBuilder control(Control control) {
			return controls(Controls.builder()
							.control(requireNonNull(control))
							.build());
		}

		@Override
		public ToolBarBuilder control(Supplier<? extends Control> control) {
			return controls(Controls.builder()
							.control(requireNonNull(control))
							.build());
		}

		@Override
		public ToolBarBuilder controls(Controls controls) {
			return new DefaultToolBarBuilder(requireNonNull(controls));
		}

		@Override
		public ToolBarBuilder controls(Supplier<Controls> controls) {
			return new DefaultToolBarBuilder(requireNonNull(controls).get());
		}
	}
}
