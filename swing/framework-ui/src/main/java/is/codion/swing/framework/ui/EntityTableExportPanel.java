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
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.ui.EntityTableExportModel.ConfigurationFile;
import is.codion.swing.framework.ui.EntityTableExportModel.ExportTask;
import is.codion.swing.framework.ui.EntityTableExportTreeModel.AttributeNode;
import is.codion.swing.framework.ui.EntityTableExportTreeModel.EntityNode;
import is.codion.swing.framework.ui.EntityTableExportTreeModel.MutableForeignKeyNode;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.button.ToggleButtonType.RADIO_BUTTON;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.EntityTableExportModel.NULL_CONFIGURATION_FILE;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createTitledBorder;

final class EntityTableExportPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTableExportPanel.class, getBundle(EntityTableExportPanel.class.getName()));

	private static final String TSV = "tsv";
	private static final String JSON = "json";
	private static final AttributeRenderer ATTRIBUTE_RENDERER = new AttributeRenderer();
	private static final FileNameExtensionFilter CONFIGURATION_FILE =
					new FileNameExtensionFilter(MESSAGES.getString("configuration_file") + " (" + JSON + ")", JSON);

	private final EntityTableExportModel model;
	private final JTree exportTree;
	private final State refreshingNodes = State.state();
	private final State singleSelection = State.state();
	private final State singleLevelSelection = State.state();
	private final State movableNodesSelected = State.state();
	private final ObservableState moveEnabled = State.or(refreshingNodes,
					State.and(singleLevelSelection, movableNodesSelected));

	private final Control saveConfiguration = Control.builder()
					.command(this::saveConfiguration)
					.caption(Messages.save())
					.mnemonic(Messages.saveMnemonic())
					.build();
	private final Control openConfiguration = Control.builder()
					.command(this::openConfigurationFiles)
					.caption(Messages.open())
					.mnemonic(Messages.openMnemonic())
					.build();
	private final Control moveUp = Control.builder()
					.command(this::moveSelectionUp)
					.enabled(moveEnabled)
					.icon(FrameworkIcons.instance().up())
					.build();
	private final Control moveDown = Control.builder()
					.command(this::moveSelectionDown)
					.enabled(moveEnabled)
					.icon(FrameworkIcons.instance().down())
					.build();
	private final Control includeAll;
	private final Control includeNone;
	private final Control hideExcluded;
	private final Control showExcluded;
	private final ToggleControl showHidden;
	private final ToggleControl allRows;
	private final ToggleControl selectedRows;

	EntityTableExportPanel(EntityTableExportModel model) {
		super(borderLayout());
		this.model = model;
		this.exportTree = createTree();
		this.includeAll = Control.builder()
						.command(model.treeModel()::includeAll)
						.caption(MESSAGES.getString("columns_all"))
						.mnemonic(MESSAGES.getString("columns_all_mnemonic").charAt(0))
						.build();
		this.includeNone = Control.builder()
						.command(model.treeModel()::includeNone)
						.caption(MESSAGES.getString("columns_none"))
						.mnemonic(MESSAGES.getString("columns_none_mnemonic").charAt(0))
						.build();
		this.hideExcluded = Control.builder()
						.command(model.treeModel()::hideExcluded)
						.caption(MESSAGES.getString("hide_excluded"))
						.mnemonic(MESSAGES.getString("hide_excluded_mnemonic").charAt(0))
						.build();
		this.showExcluded = Control.builder()
						.command(model.treeModel()::showExcluded)
						.caption(MESSAGES.getString("show_excluded"))
						.mnemonic(MESSAGES.getString("show_excluded_mnemonic").charAt(0))
						.build();
		this.showHidden = Control.builder()
						.toggle(model.treeModel().showHidden())
						.caption(MESSAGES.getString("show_hidden"))
						.mnemonic(MESSAGES.getString("show_hidden_mnemonic").charAt(0))
						.build();
		this.selectedRows = Control.builder()
						.toggle(model.selected())
						.caption(MESSAGES.getString("rows_selected"))
						.mnemonic(MESSAGES.getString("rows_selected_mnemonic").charAt(0))
						.build();
		this.allRows = Control.builder()
						.toggle(State.state(!model.selected().is()))
						.caption(MESSAGES.getString("rows_all"))
						.mnemonic(MESSAGES.getString("rows_all_mnemonic").charAt(0))
						.build();
		model.treeModel().configuration().addListener(this::expandIncludedNodes);
		initializeUI();
		expandIncludedNodes();
	}

	void show(JComponent dialogOwner) {
		if (isShowing()) {
			Ancestor.window().of(this).toFront();
		}
		else {
			Dialogs.builder()
							.component(this)
							.owner(dialogOwner)
							.modal(false)
							.title(MESSAGES.getString("export"))
							.icon(FrameworkIcons.instance().export().small())
							.size(model.getDialogSize())
							.onClosed(event ->
											model.setDialogSize(event.getWindow().getSize()))
							.show();
		}
	}

	EntityTableExportModel model() {
		return model;
	}

	private void exportToFile() {
		ExportTask task = model.exportToFile(Dialogs.select()
						.files()
						.owner(this)
						.filter(new FileNameExtensionFilter(TSV, TSV))
						.selectFileToSave(model.defaultExportFileName())
						.toPath());
		Dialogs.progressWorker()
						.task(task)
						.owner(this)
						.title(MESSAGES.getString("exporting_data"))
						.control(cancelControl(task.cancel()))
						.onResult(MESSAGES.getString("data_exported"), MESSAGES.getString("exported_to_file"))
						.execute();
	}

	private void exportToClipboard() {
		ExportTask task = model.exportToClipboard();
		Dialogs.progressWorker()
						.task(task)
						.owner(this)
						.title(MESSAGES.getString("exporting_data"))
						.control(cancelControl(task.cancel()))
						.onResult(MESSAGES.getString("data_exported"), MESSAGES.getString("exported_to_clipboard"))
						.execute();
	}

	private static Control cancelControl(State cancel) {
		return Control.builder()
						.toggle(cancel)
						.caption(Messages.cancel())
						.mnemonic(Messages.cancelMnemonic())
						.enabled(cancel.not())
						.build();
	}

	private JTree createTree() {
		return Components.tree()
						.model(model.treeModel())
						.cellRenderer(ATTRIBUTE_RENDERER)
						.showsRootHandles(true)
						.rootVisible(false)
						.treeWillExpandListener(new ExpandListener())
						.treeSelectionListener(new SingleLevelSelectionListener())
						.mouseListener(new ExportTreeMouseListener())
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_SPACE)
										.action(command(this::toggleSelected)))
						.keyEvent(KeyEvents.builder()
										.modifiers(SHIFT_DOWN_MASK)
										.keyCode(VK_SPACE)
										.action(Control.builder()
														.command(this::toggleChildren)
														.enabled(singleSelection)
														.build()))
						.keyEvent(KeyEvents.builder()
										.modifiers(ALT_DOWN_MASK)
										.keyCode(VK_UP)
										.action(moveUp))
						.keyEvent(KeyEvents.builder()
										.modifiers(ALT_DOWN_MASK)
										.keyCode(VK_DOWN)
										.action(moveDown))
						.build();
	}

	private void moveSelectionUp() {
		moveSelection(true);
	}

	private void moveSelectionDown() {
		moveSelection(false);
	}

	private void moveSelection(boolean up) {
		TreePath[] selectionPaths = exportTree.getSelectionPaths();
		List<TreeNode> selected = selectedNodes(selectionPaths);
		if (!selected.isEmpty()) {
			refreshingNodes.set(true);
			List<TreePath> expandedPaths = Collections.list(exportTree.getExpandedDescendants(new TreePath(exportTree.getModel().getRoot())));
			((EntityNode) selected.get(0).getParent()).move(selected, up);
			expandedPaths.forEach(exportTree::expandPath);
			exportTree.setSelectionPaths(selectionPaths);
			refreshingNodes.set(false);
			exportTree.scrollPathToVisible(selectionPaths[0]);
		}
	}

	private List<TreeNode> selectedNodes(TreePath[] selectionPaths) {
		return Stream.of(selectionPaths)
						.filter(exportTree::isPathSelected)
						.map(TreePath::getLastPathComponent)
						.map(TreeNode.class::cast)
						.collect(toList());
	}

	private void toggleSelected() {
		TreePath[] selectionPaths = exportTree.getSelectionPaths();
		if (selectionPaths != null) {
			Stream.of(selectionPaths)
							.filter(exportTree::isPathSelected)
							.map(TreePath::getLastPathComponent)
							.map(AttributeNode.class::cast)
							.forEach(node -> node.include().toggle());
			updateMovableNodesSelected();
			exportTree.repaint();
		}
	}

	private void toggleChildren() {
		TreePath[] selectionPaths = exportTree.getSelectionPaths();
		if (selectionPaths != null) {
			List<DefaultMutableTreeNode> children = Stream.of(selectionPaths)
							.filter(exportTree::isPathSelected)
							.map(TreePath::getLastPathComponent)
							.map(DefaultMutableTreeNode.class::cast)
							.flatMap(node -> Collections.list(node.children()).stream())
							.map(DefaultMutableTreeNode.class::cast)
							.collect(toList());
			if (!children.isEmpty()) {
				boolean allSelected = children.stream()
								.allMatch(child -> ((AttributeNode) child).include().is());
				children.forEach(node -> ((AttributeNode) node).include().set(!allSelected));
				updateMovableNodesSelected();
				exportTree.repaint();
			}
		}
	}

	private void openConfigurationFiles() {
		model.addConfigurationFiles(Dialogs.select()
						.files()
						.owner(this)
						.filter(CONFIGURATION_FILE)
						.selectFiles());
	}

	private void saveConfiguration() {
		ConfigurationFile configurationFile = model.configurationFiles().selection().item().get();
		model.writeConfig(Dialogs.select()
						.files()
						.owner(this)
						.startDirectory(destinationDirectory(configurationFile))
						.filter(CONFIGURATION_FILE)
						.selectFileToSave(defaultFileName(configurationFile)));
	}

	private static @Nullable String destinationDirectory(@Nullable ConfigurationFile configurationFile) {
		return configurationFile != null ? configurationFile.file().getParentFile().getAbsolutePath() : null;
	}

	private String defaultFileName(@Nullable ConfigurationFile configurationFile) {
		if (configurationFile == null) {
			return model.entityDefinition().caption();
		}

		return configurationFile.filename();
	}

	private void initializeUI() {
		GridLayout buttonLayout = new GridLayout(1, 0, 0, 0);
		add(borderLayoutPanel()
						.border(emptyBorder())
						.center(borderLayoutPanel()
										.border(createTitledBorder(MESSAGES.getString("columns")))
										.center(scrollPane()
														.view(exportTree))
										.east(borderLayoutPanel()
														.north(flexibleGridLayoutPanel(0, 1)
																		.add(panel()
																						.layout(buttonLayout)
																						.border(createTitledBorder(MESSAGES.getString("include")))
																						.add(button()
																										.control(includeAll))
																						.add(button()
																										.control(includeNone)))
																		.add(panel()
																						.layout(buttonLayout)
																						.border(createTitledBorder(MESSAGES.getString("excluded")))
																						.add(button()
																										.control(showExcluded))
																						.add(button()
																										.control(hideExcluded)))
																		.add(panel()
																						.layout(buttonLayout)
																						.border(createTitledBorder(MESSAGES.getString("move")))
																						.add(button()
																										.control(moveUp))
																						.add(button()
																										.control(moveDown)))))
										.south(checkBox()
														.toggle(showHidden)))
						.south(borderLayoutPanel()
										.border(createTitledBorder(MESSAGES.getString("configurations")))
										.center(comboBox()
														.model(model.configurationFiles())
														.popupControl(comboBox -> Control.builder()
																		.command(model::clearConfigurationFiles)
																		.caption(Messages.clear())
																		.build())
														.renderer(new ConfigurationFileRenderer()))
										.east(buttonPanel()
														.controls(Controls.builder()
																		.control(openConfiguration)
																		.control(saveConfiguration))
														.transferFocusOnEnter(true)))
						.build(), BorderLayout.CENTER);
		add(createSouthPanel(), BorderLayout.SOUTH);
	}

	private JPanel createSouthPanel() {
		return borderLayoutPanel()
						.west(button()
										.control(Control.builder()
														.command(this::displayHelp)
														.caption("?")))
						.east(borderLayoutPanel()
										.west(borderLayoutPanel()
														.center(borderLayoutPanel()
																		.east(buttonPanel()
																						.controls(Controls.builder()
																										.actions(allRows, selectedRows))
																						.toggleButtonType(RADIO_BUTTON)
																						.buttonGroup(new ButtonGroup())
																						.fixedButtonSize(false)
																						.transferFocusOnEnter(true))))
										.center(createActionButtonPanel()))
						.border(createEmptyBorder(10, 10, 5, 10))
						.build();
	}

	private JPanel createActionButtonPanel() {
		return buttonPanel()
						.controls(Controls.builder()
										.control(Control.builder()
														.command(this::exportToClipboard)
														.caption(MESSAGES.getString("to_clipboard"))
														.mnemonic(MESSAGES.getString("to_clipboard_mnemonic").charAt(0)))
										.control(Control.builder()
														.command(this::exportToFile)
														.caption(MESSAGES.getString("to_file"))
														.mnemonic(MESSAGES.getString("to_file_mnemonic").charAt(0)))
										.control(Control.builder()
														.command(() -> Ancestor.window().of(this).dispose())
														.caption(MESSAGES.getString("close"))
														.mnemonic(MESSAGES.getString("close_mnemonic").charAt(0))
														.keyStroke(keyStroke(VK_ESCAPE))))
						.fixedButtonSize(false)
						.build();
	}

	private void displayHelp() {
		Dialogs.builder()
						.owner(this)
						.title(MESSAGES.getString("help"))
						.component(textArea()
										.value(MESSAGES.getString("help_text"))
										.editable(false)
										.focusable(false)
										.border(emptyBorder()))
						.show();
	}

	private void expandIncludedNodes() {
		collapseAll();
		expandNodeIfHasSelectedChildren(model.treeModel().getRoot());
		exportTree.repaint();
	}

	private void collapseAll() {
		TreePath rootPath = new TreePath(model.treeModel().getRoot());
		Enumeration<? extends TreeNode> children = model.treeModel().getRoot().children();
		while (children.hasMoreElements()) {
			collapseAll(rootPath.pathByAddingChild(children.nextElement()));
		}
	}

	private void collapseAll(TreePath parent) {
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		Enumeration<? extends TreeNode> children = node.children();
		while (children.hasMoreElements()) {
			collapseAll(parent.pathByAddingChild(children.nextElement()));
		}
		exportTree.collapsePath(parent);
	}

	private boolean expandNodeIfHasSelectedChildren(TreeNode node) {
		boolean hasSelectedDescendants = false;
		for (int i = 0; i < node.getChildCount(); i++) {
			TreeNode child = node.getChildAt(i);
			if (child instanceof AttributeNode) {
				AttributeNode attrNode = (AttributeNode) child;
				if (attrNode.include().is()) {
					hasSelectedDescendants = true;
				}
				if (expandNodeIfHasSelectedChildren(child)) {
					hasSelectedDescendants = true;
					exportTree.expandPath(new TreePath(((DefaultMutableTreeNode) attrNode).getPath()));
				}
			}
		}

		return hasSelectedDescendants;
	}

	private void updateMovableNodesSelected() {
		movableNodesSelected.set(!exportTree.isSelectionEmpty() && Stream.of(exportTree.getSelectionPaths())
						.map(TreePath::getLastPathComponent)
						.map(DefaultMutableTreeNode.class::cast)
						.allMatch(EntityTableExportPanel::movableNode));
	}

	private static boolean movableNode(DefaultMutableTreeNode node) {
		Enumeration<? extends TreeNode> children = node.children();
		while (children.hasMoreElements()) {
			if (movableNode((DefaultMutableTreeNode) children.nextElement())) {
				return true;
			}
		}

		return ((AttributeNode) node).include().is();
	}

	private final class ExportTreeMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.isAltDown()) {
				toggleSelected();
			}
		}
	}

	private static final class ExpandListener implements TreeWillExpandListener {
		@Override
		public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
			Object node = event.getPath().getLastPathComponent();
			if (node instanceof MutableForeignKeyNode) {
				((MutableForeignKeyNode) node).populate();
			}
		}

		@Override
		public void treeWillCollapse(TreeExpansionEvent event) {}
	}

	private final class SingleLevelSelectionListener implements TreeSelectionListener {
		@Override
		public void valueChanged(TreeSelectionEvent e) {
			singleLevelSelection.set(!exportTree.isSelectionEmpty() && Stream.of(exportTree.getSelectionPaths())
							.filter(exportTree::isPathSelected)
							.map(TreePath::getPathCount)
							.distinct()
							.count() == 1);
			singleSelection.set(!exportTree.isSelectionEmpty() && exportTree.getSelectionPaths().length == 1);
			updateMovableNodesSelected();
		}
	}

	static final class ExportPreferences {

		private final JSONObject preferences;

		ExportPreferences(String preferencesString) {
			preferences = new JSONObject(preferencesString);
		}

		ExportPreferences(EntityTableExportModel exportModel) {
			preferences = exportModel.createPreferences();
		}

		void apply(EntityTableExportModel exportModel) {
			exportModel.applyPreferences(preferences);
		}

		JSONObject preferences() {
			return preferences;
		}
	}

	private static final class ConfigurationFileRenderer extends JPanel implements ListCellRenderer<ConfigurationFile> {

		private static final DefaultListCellRenderer FILENAME = new DefaultListCellRenderer();
		private static final DefaultListCellRenderer PATH = new DefaultListCellRenderer();

		static {
			FILENAME.setHorizontalAlignment(SwingConstants.LEFT);
			PATH.setHorizontalAlignment(SwingConstants.RIGHT);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends ConfigurationFile> list, ConfigurationFile value,
																									int index, boolean isSelected, boolean cellHasFocus) {
			JLabel filename = (JLabel) FILENAME.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			JLabel path = (JLabel) PATH.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value == NULL_CONFIGURATION_FILE) {
				filename.setText("-");
				path.setText("");
			}
			else {
				filename.setText(value.filename());
				path.setText(value.file().getParentFile().getAbsolutePath());
			}

			return borderLayoutPanel()
							.layout(new BorderLayout())
							.west(filename)
							.center(path)
							.build();
		}
	}

	private static class AttributeRenderer extends DefaultTreeCellRenderer {

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			if (value instanceof AttributeNode) {
				AttributeNode treeNode = (AttributeNode) value;
				StringBuilder builder = new StringBuilder(treeNode.definition().caption());
				if (treeNode.include().is()) {
					builder.insert(0, "+");
				}
				if (treeNode instanceof MutableForeignKeyNode) {
					int includedChildrenCount = ((MutableForeignKeyNode) treeNode).includedCount();
					if (includedChildrenCount > 0) {
						builder.append(" (").append(includedChildrenCount).append(")");
					}
				}
				setText(builder.toString());
			}

			return component;
		}
	}
}
