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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.reactive.state.State;
import is.codion.demos.chinook.domain.api.Chinook.Employee;
import is.codion.swing.common.ui.component.tree.FilterTree;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.model.component.SwingEntityTableTreeModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import static is.codion.swing.common.ui.component.Components.splitPane;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.framework.ui.EntityDialogs.editEntityDialog;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_H;

public final class EmployeeTablePanel extends EntityTablePanel {

	// tag::tableTree[]
	private final JScrollPane treeScrollPane;
	private final JSplitPane splitPane = splitPane()
					.continuousLayout(true)
					.build();
	private final State treeVisible = State.builder()
					.consumer(this::treeVisible)
					.build();

	public EmployeeTablePanel(SwingEntityTableModel tableModel) {
		// We provide a EmployeeEditPanel instance, which is then accessible
		// via double click, popup menu (Add/Edit) or keyboard shortcuts:
		// INSERT to add a new employee or CTRL-INSERT to edit the selected one.
		super(tableModel, new EmployeeEditPanel(tableModel.editModel()));
		// The employees in the table, arranged by who they report to
		this.treeScrollPane = FilterTree.builder()
						.model(SwingEntityTableTreeModel.builder()
										.tableModel(tableModel)
										.parent(Employee.REPORTSTO_FK)
										.build())
						.scrollPane()
						.preferredWidth(200)
						.build();
		ToggleControl toggleTree = Control.builder()
						.toggle(treeVisible)
						.icon(FrameworkIcons.instance().get("tree"))
						.build();
		configureToolBar(layout -> layout.clear()
						.control(toggleTree)
						.separator()
						.defaults());
		// CTRL-ALT-H toggles the tree, wherever the focus is within the panel
		KeyEvents.builder()
						.keyCode(VK_H)
						.modifiers(MENU_SHORTCUT_MASK | ALT_DOWN_MASK)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(toggleTree)
						.enable(this);
	}

	@Override
	protected void layoutPanel(JComponent tableComponent, JPanel southPanel) {
		splitPane.setRightComponent(tableComponent);
		super.layoutPanel(splitPane, southPanel);
	}

	private void treeVisible(boolean visible) {
		splitPane.setLeftComponent(visible ? treeScrollPane : null);
	}
	// end::tableTree[]

	@Override
	protected void setupControls() {
		// Replace the default EDIT control command with one that sets
		// the initial focus according the selected table column
		control(ControlKeys.EDIT).update(control -> control.copy(this::edit).build());
	}

	private void edit() {
		editEntityDialog(editPanel())
						.owner(this)
						.onShown(this::requestEditFocus)
						.show();
	}

	private void requestEditFocus(EntityEditPanel editPanel) {
		table().columns().selection().lead().optional().ifPresent(index ->
						editPanel.focus().request(table().columns().columnAt(index).identifier()));
	}
}
