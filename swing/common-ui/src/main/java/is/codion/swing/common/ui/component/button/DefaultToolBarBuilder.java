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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

final class DefaultToolBarBuilder extends AbstractControlPanelBuilder<JToolBar, ToolBarBuilder> implements ToolBarBuilder {

	private @Nullable Boolean floatable;
	private @Nullable Boolean rollover;
	private @Nullable Boolean borderPainted;

	DefaultToolBarBuilder() {
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
	public ToolBarBuilder glue() {
		return add(this::createGlue);
	}

	@Override
	protected JToolBar createComponent() {
		JToolBar toolBar = new JToolBar();
		if (floatable != null) {
			toolBar.setFloatable(floatable);
		}
		toolBar.setOrientation(orientation());
		if (rollover != null) {
			toolBar.setRollover(rollover);
		}
		if (borderPainted != null) {
			toolBar.setBorderPainted(borderPainted);
		}
		addContent(new ToolBarControlHandler(toolBar), toolBar::add);

		return toolBar;
	}

	// the glue follows the orientation, which may be specified after the glue is added
	private JComponent createGlue() {
		return (JComponent) (orientation() == SwingConstants.HORIZONTAL ?
						Box.createHorizontalGlue() :
						Box.createVerticalGlue());
	}

	private final class ToolBarControlHandler extends ControlHandler {

		private final JToolBar toolBar;

		private ToolBarControlHandler(JToolBar toolBar) {
			this.toolBar = toolBar;
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
			cleanupSeparators(controls.actions()).forEach(this);
		}

		@Override
		void onAction(Action action) {
			toolBar.add(buttonBuilder()
							.action(action)
							.build());
		}
	}
}
