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
package is.codion.swing.framework.ui;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Map;

import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.ControlKeys.NAVIGATE_LEFT;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.ControlKeys.NAVIGATE_RIGHT;
import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.*;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;

/**
 * Displays the given dependencies in a tabbed pane.
 * @see #displayDependenciesDialog(Collection, EntityConnectionProvider, JComponent)
 */
final class EntityDependenciesPanel extends JPanel {

	/**
	 * The dependencies panel controls.
	 */
	static final class ControlKeys {

		/**
		 * Navigates to the dependencies panel on the left (with wrap-around).<br>
		 * Default key stroke: CTRL-ALT-LEFT ARROW
		 */
		public static final ControlKey<CommandControl> NAVIGATE_LEFT = CommandControl.key("navigateLeft", keyStroke(VK_LEFT, CTRL_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * Navigates to the dependencies panel on the right (with wrap-around).<br>
		 * Default key stroke: CTRL-ALT-RIGHT ARROW
		 */
		public static final ControlKey<CommandControl> NAVIGATE_RIGHT = CommandControl.key("navigateRight", keyStroke(VK_RIGHT, CTRL_DOWN_MASK | ALT_DOWN_MASK));

		private ControlKeys() {}
	}

	private final JTabbedPane tabPane = new JTabbedPane(SwingConstants.TOP);

	EntityDependenciesPanel(Map<EntityType, Collection<Entity>> dependencies, EntityConnectionProvider connectionProvider) {
		super(new BorderLayout());
		for (Map.Entry<EntityType, Collection<Entity>> entry : dependencies.entrySet()) {
			tabPane.addTab(connectionProvider.entities().definition(entry.getKey()).caption(),
							createTablePanel(entry.getValue(), connectionProvider));
		}
		add(tabPane, BorderLayout.CENTER);
		NAVIGATE_RIGHT.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(command(new NavigateRightCommand()))
										.enable(tabPane));
		NAVIGATE_LEFT.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder(keyStroke)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.action(command(new NavigateLeftCommand()))
										.enable(tabPane));
	}

	private static EntityTablePanel createTablePanel(Collection<Entity> entities, EntityConnectionProvider connectionProvider) {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(entities, connectionProvider);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeConditions(false));
		tablePanel.configurePopupMenu(config -> config.clear()
						.control(EDIT_ATTRIBUTE_CONTROLS)
						.control(DELETE)
						.separator()
						.control(VIEW_DEPENDENCIES));

		return tablePanel.initialize();
	}

	void requestSelectedTableFocus() {
		((EntityTablePanel) tabPane.getSelectedComponent()).table().requestFocusInWindow();
	}

	private final class NavigateRightCommand implements Control.Command {

		@Override
		public void execute() {
			int selectedIndex = tabPane.getSelectedIndex();
			tabPane.setSelectedIndex(selectedIndex == tabPane.getTabCount() - 1 ? 0 : selectedIndex + 1);
			requestSelectedTableFocus();
		}
	}

	private final class NavigateLeftCommand implements Control.Command {

		@Override
		public void execute() {
			int selectedIndex = tabPane.getSelectedIndex();
			tabPane.setSelectedIndex(selectedIndex == 0 ? tabPane.getTabCount() - 1 : selectedIndex - 1);
			requestSelectedTableFocus();
		}
	}
}
