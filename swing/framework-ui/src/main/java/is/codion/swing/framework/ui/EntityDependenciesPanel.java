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

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.resource.MessageBundle;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.ui.Cursors;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.ControlKeys.NAVIGATE_LEFT;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.ControlKeys.NAVIGATE_RIGHT;
import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.*;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * Displays the given dependencies in a tabbed pane.
 * @see #displayDependenciesDialog(Collection, EntityConnectionProvider, JComponent)
 */
final class EntityDependenciesPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityDependenciesPanel.class, getBundle(EntityDependenciesPanel.class.getName()));

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

	/**
	 * Displays a dialog containing the entities depending on the given entities.
	 * @param entities the entities for which to display dependencies
	 * @param connectionProvider the connection provider
	 * @param dialogParent the dialog parent
	 */
	static void displayDependenciesDialog(Collection<Entity> entities, EntityConnectionProvider connectionProvider,
																				JComponent dialogParent) {
		displayDependenciesDialog(entities, connectionProvider, dialogParent, MESSAGES.getString("no_dependencies"));
	}

	/**
	 * Shows a dialog containing the entities depending on the given entities.
	 * @param entities the entities for which to display dependencies
	 * @param connectionProvider the connection provider
	 * @param dialogParent the dialog parent
	 * @param noDependenciesMessage the message to show in case of no dependencies
	 */
	static void displayDependenciesDialog(Collection<Entity> entities, EntityConnectionProvider connectionProvider,
																				JComponent dialogParent, String noDependenciesMessage) {
		requireNonNull(entities);
		requireNonNull(connectionProvider);
		requireNonNull(dialogParent);
		Map<EntityType, Collection<Entity>> dependencies = dependencies(entities, connectionProvider, dialogParent);
		displayDependenciesDialog(dependencies, connectionProvider, dialogParent, noDependenciesMessage);
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

	private static Map<EntityType, Collection<Entity>> dependencies(Collection<Entity> entities,
																																	EntityConnectionProvider connectionProvider,
																																	JComponent dialogParent) {
		dialogParent.setCursor(Cursors.WAIT);
		try {
			return connectionProvider.connection().dependencies(entities);
		}
		catch (DatabaseException e) {
			Dialogs.displayExceptionDialog(e, parentWindow(dialogParent));

			return Collections.emptyMap();
		}
		finally {
			dialogParent.setCursor(Cursors.DEFAULT);
		}
	}

	private static void displayDependenciesDialog(Map<EntityType, Collection<Entity>> dependencies,
																								EntityConnectionProvider connectionProvider,
																								JComponent dialogParent, String noDependenciesMessage) {
		if (dependencies.isEmpty()) {
			JOptionPane.showMessageDialog(dialogParent, noDependenciesMessage,
							MESSAGES.getString("no_dependencies_title"), JOptionPane.INFORMATION_MESSAGE);
		}
		else {
			EntityDependenciesPanel dependenciesPanel = new EntityDependenciesPanel(dependencies, connectionProvider);
			int gap = Layouts.GAP.get();
			dependenciesPanel.setBorder(createEmptyBorder(0, gap, 0, gap));
			Dialogs.componentDialog(dependenciesPanel)
							.owner(dialogParent)
							.title(FrameworkMessages.dependencies())
							.onShown(dialog -> dependenciesPanel.requestSelectedTableFocus())
							.show();
		}
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
