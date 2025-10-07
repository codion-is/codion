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

import is.codion.common.i18n.Messages;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableExportModel.AttributeNode;
import is.codion.swing.framework.ui.EntityTableExportModel.ExportTask;

import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.button.ToggleButtonType.RADIO_BUTTON;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.SwingConstants.CENTER;

final class EntityTableExportPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTableExportPanel.class, getBundle(EntityTableExportPanel.class.getName()));

	private static final String JSON = "json";

	private final EntityTableExportModel model;
	private final JTree exportTree;

	private final CommandControl selectDefaults = Control.builder()
					.command(this::selectDefaults)
					.caption(MESSAGES.getString("default_columns"))
					.mnemonic(MESSAGES.getString("default_columns_mnemonic").charAt(0))
					.build();
	private final CommandControl selectAll = Control.builder()
					.command(this::selectAll)
					.caption(MESSAGES.getString("columns_all"))
					.mnemonic(MESSAGES.getString("columns_all_mnemonic").charAt(0))
					.build();
	private final CommandControl selectNone = Control.builder()
					.command(this::selectNone)
					.caption(MESSAGES.getString("columns_none"))
					.mnemonic(MESSAGES.getString("columns_none_mnemonic").charAt(0))
					.build();
	private final CommandControl saveConfiguration = Control.builder()
					.command(this::saveConfiguration)
					.caption(Messages.save() + "...")
					.mnemonic(Messages.saveMnemonic())
					.build();
	private final CommandControl openConfiguration = Control.builder()
					.command(this::openConfiguration)
					.caption(Messages.open())
					.mnemonic(Messages.openMnemonic())
					.build();
	private final ToggleControl allRows;
	private final ToggleControl selectedRows;

	EntityTableExportPanel(SwingEntityTableModel tableModel, EntityTableExportModel model) {
		super(borderLayout());
		this.model = model;
		this.exportTree = createTree();
		this.selectedRows = Control.builder()
						.toggle(model.selected())
						.caption(MESSAGES.getString("rows_selected"))
						.mnemonic(MESSAGES.getString("rows_selected_mnemonic").charAt(0))
						.enabled(tableModel.selection().empty().not())
						.build();
		this.allRows = Control.builder()
						.toggle(State.state(!model.selected().is()))
						.caption(MESSAGES.getString("rows_all"))
						.mnemonic(MESSAGES.getString("rows_all_mnemonic").charAt(0))
						.build();
		add(borderLayoutPanel()
						.border(emptyBorder())
						.center(borderLayoutPanel()
										.border(createTitledBorder(MESSAGES.getString("columns")))
										.center(scrollPane()
														.view(exportTree))
										.south(borderLayoutPanel()
														.center(stringField()
																		.horizontalAlignment(CENTER)
																		.value(MESSAGES.getString("help"))
																		.enabled(false))
														.south(borderLayoutPanel()
																		.east(buttonPanel()
																						.controls(Controls.builder()
																										.actions(selectDefaults, selectAll, selectNone,
																														openConfiguration, saveConfiguration))
																						.transferFocusOnEnter(true)))))
						.south(borderLayoutPanel()
										.border(createTitledBorder(MESSAGES.getString("rows")))
										.center(borderLayoutPanel()
														.east(buttonPanel()
																		.controls(Controls.builder()
																						.actions(allRows, selectedRows))
																		.toggleButtonType(RADIO_BUTTON)
																		.buttonGroup(new ButtonGroup())
																		.fixedButtonSize(false)
																		.transferFocusOnEnter(true))))
						.build(), BorderLayout.CENTER);
	}

	void export(JComponent dialogOwner) {
		Dialogs.action()
						.component(this)
						.owner(dialogOwner)
						.title(MESSAGES.getString("export"))
						.escapeAction(Control.builder()
										.command(() -> parentWindow(this).dispose())
										.caption(MESSAGES.getString("close"))
										.mnemonic(MESSAGES.getString("close_mnemonic").charAt(0))
										.build())
						.action(Control.builder()
										.command(this::exportToClipboard)
										.caption(MESSAGES.getString("to_clipboard"))
										.mnemonic(MESSAGES.getString("to_clipboard_mnemonic").charAt(0))
										.build())
						.action(Control.builder()
										.command(this::exportToFile)
										.caption(MESSAGES.getString("to_file") + "...")
										.mnemonic(MESSAGES.getString("to_file_mnemonic").charAt(0))
										.build())
						.show();
	}

	EntityTableExportModel model() {
		return model;
	}

	private void openConfiguration() throws IOException {
		File file = Dialogs.select()
						.files()
						.filter(new FileNameExtensionFilter(MESSAGES.getString("configuration_file") + " (" + JSON + ")", JSON))
						.owner(this)
						.selectFile();
		applyPreferences(new JSONObject(new String(Files.readAllBytes(file.toPath()), UTF_8)));
	}

	private void saveConfiguration() throws IOException {
		File file = Dialogs.select()
						.files()
						.owner(this)
						.selectFileToSave(model.defaultConfigFileName());
		Files.write(file.toPath(), new ExportPreferences(this).preferences().toString().getBytes(UTF_8));
	}

	private void exportToFile() {
		ExportTask task = model.exportToFile(Dialogs.select()
						.files()
						.owner(this)
						.selectFileToSave(model.defaultExportFileName())
						.toPath());
		Dialogs.progressWorker()
						.task(task)
						.owner(this)
						.title(MESSAGES.getString("exporting_data"))
						.control(createCancelControl(task.cancelled()))
						.onResult(MESSAGES.getString("data_exported"), task.successMessage())
						.execute();
	}

	private void exportToClipboard() {
		ExportTask task = model.exportToClipboard();
		Dialogs.progressWorker()
						.task(task)
						.owner(this)
						.title(MESSAGES.getString("exporting_data"))
						.control(createCancelControl(task.cancelled()))
						.onResult(MESSAGES.getString("data_exported"), task.successMessage())
						.execute();
	}

	private static Control createCancelControl(State cancelled) {
		return Control.builder()
						.toggle(cancelled)
						.caption(Messages.cancel())
						.mnemonic(Messages.cancelMnemonic())
						.enabled(cancelled.not())
						.build();
	}

	private JTree createTree() {
		JTree tree = new JTree(model.treeModel());
		tree.setShowsRootHandles(true);
		tree.setRootVisible(false);
		tree.addTreeWillExpandListener(new ExpandListener());
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.isAltDown()) {
					toggleSelected();
				}
			}
		});
		KeyEvents.builder()
						.keyCode(VK_SPACE)
						.action(command(this::toggleSelected))
						.enable(tree);

		return tree;
	}

	private void selectDefaults() {
		model.selectDefaults();
		exportTree.repaint();
	}

	private void selectAll() {
		model.selectAll();
		exportTree.repaint();
	}

	private void selectNone() {
		model.selectNone();
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

	private void expandToShowSelections() {
		expandNodeIfHasSelectedChildren(model.treeModel().getRoot());
	}

	private boolean expandNodeIfHasSelectedChildren(TreeNode node) {
		boolean hasSelectedDescendants = false;
		for (int i = 0; i < node.getChildCount(); i++) {
			TreeNode child = node.getChildAt(i);
			if (child instanceof AttributeNode) {
				AttributeNode attrNode = (AttributeNode) child;
				if (attrNode.selected().is()) {
					hasSelectedDescendants = true;
				}
				if (expandNodeIfHasSelectedChildren(child)) {
					hasSelectedDescendants = true;
					exportTree.expandPath(new TreePath(attrNode.getPath()));
				}
			}
		}

		return hasSelectedDescendants;
	}

	private void applyPreferences(JSONObject preferences) {
		model.applyPreferences(preferences);
		expandToShowSelections();
		exportTree.repaint();
	}

	private final class ExpandListener implements TreeWillExpandListener {
		@Override
		public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
			Object node = event.getPath().getLastPathComponent();
			if (node instanceof AttributeNode) {
				AttributeNode attributeNode = (AttributeNode) node;
				if (attributeNode.isCyclicalStub() && attributeNode.getChildCount() == 0) {
					attributeNode.expand();
					model.treeModel().nodeStructureChanged(attributeNode);
				}
			}
		}

		@Override
		public void treeWillCollapse(TreeExpansionEvent event) {}
	}

	static final class ExportPreferences {

		private final JSONObject preferences;

		ExportPreferences(String preferencesString) {
			this(new JSONObject(preferencesString));
		}

		private ExportPreferences(JSONObject preferences) {
			this.preferences = preferences;
		}

		ExportPreferences(@Nullable EntityTableExportPanel exportPanel) {
			if (exportPanel == null) {
				this.preferences = new JSONObject("{}");
			}
			else {
				this.preferences = exportPanel.model.createPreferences();
			}
		}

		void apply(@Nullable EntityTableExportPanel exportPanel) {
			if (exportPanel != null) {
				exportPanel.applyPreferences(preferences);
			}
		}

		JSONObject preferences() {
			return preferences;
		}
	}
}
