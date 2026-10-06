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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.model.component.tree.NodePath;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.Text;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.tree.FilterTree;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.ui.EntityTableExportModel.ConfigurationFile;
import is.codion.swing.framework.ui.EntityTableExportModel.ExportTask;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
import static java.util.Collections.emptyList;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createTitledBorder;

final class EntityTableExportPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTableExportPanel.class, getBundle(EntityTableExportPanel.class.getName()));

	private static final String TSV = "tsv";
	private static final String JSON = "json";
	private static final FileNameExtensionFilter CONFIGURATION_FILE =
					new FileNameExtensionFilter(MESSAGES.getString("configuration_file") + " (" + JSON + ")", JSON);

	private final EntityTableExportModel model;
	private final AtomicReference<Dimension> dialogSize;
	private final FilterTree<AttributeDefinition<?>> exportTree;
	private final State singleSelection = State.state();
	private final State singleParentSelection = State.state();
	private final State exporting = State.state();

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
					.enabled(singleParentSelection)
					.icon(FrameworkIcons.instance().up())
					.build();
	private final Control moveDown = Control.builder()
					.command(this::moveSelectionDown)
					.enabled(singleParentSelection)
					.icon(FrameworkIcons.instance().down())
					.build();
	private final Control includeAll;
	private final Control includeNone;
	private final Control hideExcluded;
	private final Control showExcluded;
	private final ToggleControl showHidden;
	private final ToggleControl allRows;
	private final ToggleControl selectedRows;

	EntityTableExportPanel(EntityTableExportModel model, AtomicReference<Dimension> dialogSize) {
		super(borderLayout());
		this.model = model;
		this.dialogSize = dialogSize;
		this.exportTree = createTree();
		this.exportTree.model().selection().paths().addConsumer(this::selectionChanged);
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
						.toggle(model.all())
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
							.size(dialogSize.get())
							.onClosed(event -> dialogSize.set(event.getWindow().getSize()))
							.show();
		}
	}

	EntityTableExportModel model() {
		return model;
	}

	private void exportToFile() {
		ExportTask<Path> task = model.exportToFile(withExtension(Dialogs.select()
						.files()
						.owner(this)
						.filter(new FileNameExtensionFilter(TSV, TSV))
						.selectFileToSave(model.defaultExportFileName())
						.toPath(), TSV));
		Dialogs.progressWorker()
						.task(task)
						.owner(this)
						.title(MESSAGES.getString("exporting_data"))
						.control(cancelControl(task.cancel()))
						.onWorking(exporting::set)
						.onSuccess(MESSAGES.getString("data_exported"), MESSAGES.getString("exported_to_file"))
						.execute();
	}

	private void exportToClipboard() {
		ExportTask<String> task = model.exportToString();
		Dialogs.progressWorker()
						.task(task)
						.owner(this)
						.title(MESSAGES.getString("exporting_data"))
						.control(cancelControl(task.cancel()))
						.onWorking(exporting::set)
						.onResult(Utilities::setClipboard)
						.onSuccess(MESSAGES.getString("data_exported"), MESSAGES.getString("exported_to_clipboard"))
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

	private FilterTree<AttributeDefinition<?>> createTree() {
		return FilterTree.builder()
						.model(model.treeModel().treeModel())
						.cellRenderer(new AttributeRenderer(model.treeModel()))
						.mouseListener(new ExportTreeMouseListener())
						.dragEnabled(true)
						.dropMode(DropMode.INSERT)
						.transferHandler(new AttributeTransferHandler())
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
		model.treeModel().move(selectedPaths(), true);
	}

	private void moveSelectionDown() {
		model.treeModel().move(selectedPaths(), false);
	}

	private List<NodePath<AttributeDefinition<?>>> selectedPaths() {
		return exportTree.model().selection().paths().get();
	}

	private void toggleSelected() {
		model.treeModel().toggle(selectedPaths());
	}

	private void toggleChildren() {
		List<NodePath<AttributeDefinition<?>>> selected = selectedPaths();
		if (selected.size() == 1) {
			model.treeModel().toggleAll(exportTree.model().nodes().children(selected.get(0)));
		}
	}

	private void selectionChanged(List<NodePath<AttributeDefinition<?>>> selected) {
		//require a shared parent, the move operations reordering the children of a single parent
		singleParentSelection.set(!selected.isEmpty() && selected.stream()
						.map(NodePath::parent)
						.distinct()
						.count() == 1);
		singleSelection.set(selected.size() == 1);
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
		model.writeConfig(withExtension(Dialogs.select()
						.files()
						.owner(this)
						.startDirectory(destinationDirectory(configurationFile))
						.filter(CONFIGURATION_FILE)
						.selectFileToSave(defaultFileName(configurationFile))
						.toPath(), JSON).toFile());
	}

	//the file chooser filter suggests but does not enforce the extension, append it when the user typed none
	private static Path withExtension(Path path, String extension) {
		String fileName = path.getFileName().toString();
		if (fileName.indexOf('.') == -1) {
			return path.resolveSibling(fileName + "." + extension);
		}

		return path;
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
														.model(SwingFilterComboBoxModel.model(model.configurationFiles()))
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
														.enabled(exporting.not())
														.caption(MESSAGES.getString("to_clipboard"))
														.mnemonic(MESSAGES.getString("to_clipboard_mnemonic").charAt(0)))
										.control(Control.builder()
														.command(this::exportToFile)
														.enabled(exporting.not())
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
		exportTree.model().expansion().set(model.treeModel().includedParents());
	}

	private final class ExportTreeMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.isAltDown()) {
				toggleSelected();
			}
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

		void restore(EntityTableExportModel exportModel) {
			exportModel.restore(preferences);
		}

		JSONObject preferences() {
			return preferences;
		}
	}

	private static final class TransferableAttributeNodes implements Transferable {

		private static final DataFlavor FLAVOR = new DataFlavor(NodePath.class, "AttributeNodeDataFlavor");

		private final List<NodePath<AttributeDefinition<?>>> paths;

		private TransferableAttributeNodes(List<NodePath<AttributeDefinition<?>>> paths) {
			this.paths = paths;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] {FLAVOR};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor == FLAVOR;
		}

		@Override
		public List<NodePath<AttributeDefinition<?>>> getTransferData(DataFlavor flavor) {
			return paths;
		}
	}

	private final class AttributeTransferHandler extends TransferHandler {

		@Override
		protected Transferable createTransferable(JComponent component) {
			return new TransferableAttributeNodes(singleParentSelection.is() ? selectedPaths() : emptyList());
		}

		@Override
		public int getSourceActions(JComponent component) {
			return TransferHandler.MOVE;
		}

		@Override
		public boolean canImport(TransferSupport support) {
			Point dropPoint = support.getDropLocation().getDropPoint();
			TreePath treePath = exportTree.getPathForRow(exportTree.getRowForLocation(dropPoint.x, dropPoint.y));
			if (treePath == null) {
				return false;
			}
			List<NodePath<AttributeDefinition<?>>> paths = paths(support);
			if (paths.isEmpty()) {
				return false;
			}
			NodePath<AttributeDefinition<?>> path = (NodePath<AttributeDefinition<?>>) treePath.getLastPathComponent();
			// Not allow dropping on any of the selected nodes, and only allow nodes under the same parent to be moved
			return !selectedPaths().contains(path) && paths.get(0).parent().equals(path.parent());
		}

		@Override
		public boolean importData(TransferSupport support) {
			int dropIndex = ((JTree.DropLocation) support.getDropLocation()).getChildIndex();
			List<NodePath<AttributeDefinition<?>>> paths = paths(support);
			if (dropIndex == -1 || paths.isEmpty()) {
				return false;
			}
			model.treeModel().move(paths, dropIndex);

			return true;
		}

		private List<NodePath<AttributeDefinition<?>>> paths(TransferSupport support) {
			try {
				return (List<NodePath<AttributeDefinition<?>>>) support.getTransferable()
								.getTransferData(TransferableAttributeNodes.FLAVOR);
			}
			catch (Exception e) {
				return emptyList();
			}
		}
	}

	private static final class ConfigurationFileRenderer extends JPanel implements ListCellRenderer<ConfigurationFile> {

		private final DefaultListCellRenderer filename = new DefaultListCellRenderer();
		private final DefaultListCellRenderer path = new DefaultListCellRenderer();

		private ConfigurationFileRenderer() {
			super(new BorderLayout());
			filename.setHorizontalAlignment(SwingConstants.LEFT);
			path.setHorizontalAlignment(SwingConstants.RIGHT);
			add(filename, BorderLayout.WEST);
			add(path, BorderLayout.CENTER);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends ConfigurationFile> list, ConfigurationFile value,
																									int index, boolean isSelected, boolean cellHasFocus) {
			filename.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			path.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value == NULL_CONFIGURATION_FILE) {
				filename.setText("-");
				path.setText("");
			}
			else {
				filename.setText(value.filename());
				path.setText(value.file().getParentFile().getAbsolutePath());
			}

			return this;
		}
	}

	private static final class AttributeRenderer extends DefaultTreeCellRenderer {

		/**
		 * To make drag'n drop easier, otherwise the renderer can be
		 * too slim, making it difficult to hit as a drag target.
		 */
		private static final int MIN_LENGTH = 25;

		private final EntityTableExportTreeModel treeModel;

		private AttributeRenderer(EntityTableExportTreeModel treeModel) {
			this.treeModel = treeModel;
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			NodePath<AttributeDefinition<?>> path = (NodePath<AttributeDefinition<?>>) value;
			if (!path.root()) {
				StringBuilder builder = new StringBuilder(path.item().caption());
				if (treeModel.included(path)) {
					builder.insert(0, "+");
				}
				if (path.item() instanceof ForeignKeyDefinition) {
					int includedCount = treeModel.includedCount(path);
					if (includedCount > 0) {
						builder.append(" (").append(includedCount).append(")");
					}
				}
				setText(Text.rightPad(builder.toString(), MIN_LENGTH, ' '));
			}

			return component;
		}
	}
}
