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
package is.codion.swing.framework.ui;

import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableExport.AttributeNode;
import is.codion.swing.framework.ui.EntityTableExport.ExportTask;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.util.Enumeration;
import java.util.stream.Stream;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.button.ToggleButtonType.RADIO_BUTTON;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createTitledBorder;

final class EntityTableExportPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTableExportPanel.class, getBundle(EntityTableExportPanel.class.getName()));

	private final EntityTableExport export;
	private final FilterTableColumnModel<Attribute<?>> columnModel;
	private final JTree exportTree;

	private final CommandControl selectDefaultsControl = Control.builder()
					.command(this::selectDefaults)
					.caption(MESSAGES.getString("default_columns"))
					.mnemonic(MESSAGES.getString("default_columns_mnemonic").charAt(0))
					.build();
	private final CommandControl selectAllControl = Control.builder()
					.command(() -> select(true))
					.caption(MESSAGES.getString("columns_all"))
					.mnemonic(MESSAGES.getString("columns_all_mnemonic").charAt(0))
					.build();
	private final CommandControl selectNoneControl = Control.builder()
					.command(() -> select(false))
					.caption(MESSAGES.getString("columns_none"))
					.mnemonic(MESSAGES.getString("columns_none_mnemonic").charAt(0))
					.build();
	private final ToggleControl allRowsControl = Control.builder()
					.toggle(State.state())
					.caption(MESSAGES.getString("rows_all"))
					.mnemonic(MESSAGES.getString("rows_all_mnemonic").charAt(0))
					.build();
	private final ToggleControl selectedRowsControl;

	EntityTableExportPanel(SwingEntityTableModel tableModel, FilterTableColumnModel<Attribute<?>> columnModel) {
		super(borderLayout());
		this.export = new EntityTableExport(tableModel);
		this.columnModel = columnModel;
		this.exportTree = createTree();
		this.selectedRowsControl = Control.builder()
						.toggle(export.selected())
						.caption(MESSAGES.getString("rows_selected"))
						.mnemonic(MESSAGES.getString("rows_selected_mnemonic").charAt(0))
						.enabled(tableModel.selection().empty().not())
						.build();
		add(borderLayoutPanel()
						.border(emptyBorder())
						.center(borderLayoutPanel()
										.border(createTitledBorder(MESSAGES.getString("columns")))
										.center(scrollPane()
														.view(exportTree))
										.south(borderLayoutPanel()
														.east(buttonPanel()
																		.controls(Controls.builder()
																						.actions(selectDefaultsControl, selectAllControl, selectNoneControl))
																		.transferFocusOnEnter(true))))
						.south(borderLayoutPanel()
										.border(createTitledBorder(MESSAGES.getString("rows")))
										.center(borderLayoutPanel()
														.east(buttonPanel()
																		.controls(Controls.builder()
																						.actions(allRowsControl, selectedRowsControl))
																		.toggleButtonType(RADIO_BUTTON)
																		.buttonGroup(new ButtonGroup())
																		.fixedButtonSize(false)
																		.transferFocusOnEnter(true))))
						.build(), BorderLayout.CENTER);
	}

	void exportToClipboard(JComponent dialogOwner) {
		selectDefaults();
		Dialogs.okCancel()
						.component(this)
						.owner(dialogOwner)
						.title(MESSAGES.getString("export"))
						.onOk(() -> export(dialogOwner))
						.show();
	}

	private void export(JComponent dialogOwner) {
		ExportTask task = export.task();
		Dialogs.progressWorker()
						.task(task)
						.owner(dialogOwner)
						.title(MESSAGES.getString("exporting_rows"))
						.control(Control.builder()
										.toggle(task.cancelled())
										.caption("Cancel")
										.enabled(task.cancelled().not()))
						.onResult(Utilities::setClipboard)
						.execute();
	}

	private JTree createTree() {
		JTree tree = new JTree(export.entityNode());
		tree.setShowsRootHandles(true);
		tree.setRootVisible(false);
		KeyEvents.builder()
						.keyCode(VK_SPACE)
						.action(command(this::toggleSelected))
						.enable(tree);

		return tree;
	}

	private void selectDefaults() {
		select(false);
		Enumeration<TreeNode> children = export.entityNode().children();
		while (children.hasMoreElements()) {
			TreeNode child = children.nextElement();
			if (child instanceof AttributeNode) {
				Attribute<?> attribute = ((AttributeNode) child).definition().attribute();
				if (columnModel.contains(attribute) && columnModel.visible(attribute).is()) {
					((AttributeNode) child).selected().set(true);
				}
			}
		}
	}

	private void select(boolean select) {
		Enumeration<TreeNode> enumeration = export.entityNode().breadthFirstEnumeration();
		enumeration.nextElement();// root
		while (enumeration.hasMoreElements()) {
			((AttributeNode) enumeration.nextElement()).selected().set(select);
		}
		exportTree.repaint();
	}

	private void toggleSelected() {
		TreePath[] selectionPaths = exportTree.getSelectionPaths();
		if (selectionPaths != null) {
			Stream.of(selectionPaths)
							.filter(exportTree::isPathSelected)
							.map(TreePath::getLastPathComponent)
							.map(AttributeNode.class::cast)
							.forEach(node -> node.selected().toggle());
			exportTree.repaint();
		}
	}
}
