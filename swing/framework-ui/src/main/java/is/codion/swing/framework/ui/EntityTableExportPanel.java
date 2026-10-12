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
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.list.FilterList;
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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
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
import static java.util.Collections.singletonList;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.joining;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.SwingUtilities.isLeftMouseButton;

final class EntityTableExportPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTableExportPanel.class, getBundle(EntityTableExportPanel.class.getName()));

	private static final String TSV = "tsv";
	private static final String JSON = "json";
	private static final String PATH_SEPARATOR = " \u203A ";
	private static final FileNameExtensionFilter CONFIGURATION_FILE =
					new FileNameExtensionFilter(MESSAGES.getString("configuration_file") + " (" + JSON + ")", JSON);
	private static final DataFlavor PATHS = new DataFlavor(TransferablePaths.class, "Attribute paths");

	private final EntityTableExportModel model;
	private final AtomicReference<Dimension> dialogSize;
	private final AttributeRenderer attributeRenderer = new AttributeRenderer();
	private final FilterTree<AttributeDefinition<?>> attributeTree;
	private final FilterList<NodePath<AttributeDefinition<?>>> columnList;
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
	private final Control moveUp;
	private final Control moveDown;
	private final Control removeColumns;
	private final Control tableColumns;
	private final Control clearColumns;
	private final ToggleControl showHidden;
	private final ToggleControl allRows;
	private final ToggleControl selectedRows;

	EntityTableExportPanel(EntityTableExportModel model, AtomicReference<Dimension> dialogSize) {
		super(borderLayout());
		this.model = model;
		this.dialogSize = dialogSize;
		this.moveUp = Control.builder()
						.command(() -> model.columns().move(selectedColumns(), true))
						.enabled(model.columns().list().selection().present())
						.icon(FrameworkIcons.instance().up())
						.description(MESSAGES.getString("move_up"))
						.build();
		this.moveDown = Control.builder()
						.command(() -> model.columns().move(selectedColumns(), false))
						.enabled(model.columns().list().selection().present())
						.icon(FrameworkIcons.instance().down())
						.description(MESSAGES.getString("move_down"))
						.build();
		this.removeColumns = Control.builder()
						.command(() -> model.columns().remove(selectedColumns()))
						.enabled(model.columns().list().selection().present())
						.icon(FrameworkIcons.instance().delete())
						.description(MESSAGES.getString("remove"))
						.build();
		this.tableColumns = Control.builder()
						.command(model.columns()::reset)
						.caption(MESSAGES.getString("table_columns"))
						.mnemonic(MESSAGES.getString("table_columns_mnemonic").charAt(0))
						.build();
		this.clearColumns = Control.builder()
						.command(model.columns()::clear)
						.caption(MESSAGES.getString("clear"))
						.mnemonic(MESSAGES.getString("clear_mnemonic").charAt(0))
						.build();
		this.showHidden = Control.builder()
						.toggle(model.columns().showHidden())
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
		this.attributeTree = createAttributeTree();
		this.columnList = createColumnList();
		if (model.configurationFiles().selection().item().isNull()) {
			//the table columns as they are when the export is first shown, with the column preferences applied
			model.columns().reset();
		}
		initializeUI();
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

	private FilterTree<AttributeDefinition<?>> createAttributeTree() {
		return FilterTree.builder()
						.model(model.columns().tree())
						.cellRenderer(attributeRenderer)
						.mouseListener(new ToggleOnCheckBoxClick())
						.dragEnabled(true)
						.transferHandler(new AttributeTransferHandler())
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_SPACE)
										.action(command(() -> model.columns().toggle(selectedAttributes()))))
						.keyEvent(KeyEvents.builder()
										.modifiers(SHIFT_DOWN_MASK)
										.keyCode(VK_SPACE)
										.action(command(() -> model.columns().toggleChildren(selectedAttributes()))))
						.build();
	}

	private FilterList<NodePath<AttributeDefinition<?>>> createColumnList() {
		return FilterList.builder()
						.model(model.columns().list())
						.items()
						.formatter(EntityTableExportPanel::caption)
						.dragEnabled(true)
						.dropMode(DropMode.INSERT)
						.transferHandler(new ColumnTransferHandler())
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_DELETE)
										.action(removeColumns))
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

	private List<NodePath<AttributeDefinition<?>>> selectedAttributes() {
		return attributeTree.model().selection().paths().get();
	}

	private List<NodePath<AttributeDefinition<?>>> selectedColumns() {
		return columnList.model().selection().items().get();
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
		add(borderLayoutPanel()
						.border(emptyBorder())
						.center(gridLayoutPanel(1, 2)
										.add(createAttributesPanel())
										.add(createColumnsPanel()))
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

	private JPanel createAttributesPanel() {
		return borderLayoutPanel()
						.border(createTitledBorder(MESSAGES.getString("available")))
						.north(stringField()
										.link(model.columns().search())
										.hint(Messages.search() + "...")
										.selectAllOnFocusGained(true))
						.center(scrollPane()
										.view(attributeTree))
						.south(checkBox()
										.toggle(showHidden))
						.build();
	}

	private JPanel createColumnsPanel() {
		return borderLayoutPanel()
						.border(createTitledBorder(MESSAGES.getString("columns")))
						.center(scrollPane()
										.view(columnList))
						.east(borderLayoutPanel()
										.north(flexibleGridLayoutPanel(0, 1)
														.add(button()
																		.control(moveUp))
														.add(button()
																		.control(moveDown))
														.add(button()
																		.control(removeColumns))))
						.south(buttonPanel()
										.controls(Controls.builder()
														.control(tableColumns)
														.control(clearColumns))
										.fixedButtonSize(false))
						.build();
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

	/**
	 * @return the captions on the given path, separated
	 */
	private static String caption(NodePath<AttributeDefinition<?>> path) {
		return path.items().stream()
						.map(AttributeDefinition::caption)
						.collect(joining(PATH_SEPARATOR));
	}

	private final class ToggleOnCheckBoxClick extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent event) {
			if (isLeftMouseButton(event)) {
				TreePath treePath = attributeTree.getPathForLocation(event.getX(), event.getY());
				Rectangle bounds = treePath == null ? null : attributeTree.getPathBounds(treePath);
				if (bounds != null && event.getX() < bounds.x + attributeRenderer.checkBoxWidth()) {
					model.columns().toggle(singletonList((NodePath<AttributeDefinition<?>>) treePath.getLastPathComponent()));
				}
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

	private static final class TransferablePaths implements Transferable {

		private final List<NodePath<AttributeDefinition<?>>> paths;

		private TransferablePaths(List<NodePath<AttributeDefinition<?>>> paths) {
			this.paths = paths;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] {PATHS};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return PATHS.equals(flavor);
		}

		@Override
		public TransferablePaths getTransferData(DataFlavor flavor) {
			return this;
		}

		private static List<NodePath<AttributeDefinition<?>>> paths(TransferHandler.TransferSupport support) {
			try {
				return ((TransferablePaths) support.getTransferable().getTransferData(PATHS)).paths;
			}
			catch (Exception e) {
				return emptyList();
			}
		}
	}

	/**
	 * Drags the selected attributes, to be dropped onto the columns.
	 */
	private final class AttributeTransferHandler extends TransferHandler {

		@Override
		protected Transferable createTransferable(JComponent component) {
			return new TransferablePaths(selectedAttributes());
		}

		@Override
		public int getSourceActions(JComponent component) {
			return TransferHandler.COPY;
		}
	}

	/**
	 * Drags the selected columns, to move them, and receives attributes dropped from the tree, to insert them.
	 */
	private final class ColumnTransferHandler extends TransferHandler {

		@Override
		protected Transferable createTransferable(JComponent component) {
			return new TransferablePaths(selectedColumns());
		}

		@Override
		public int getSourceActions(JComponent component) {
			return TransferHandler.MOVE;
		}

		@Override
		public boolean canImport(TransferSupport support) {
			return support.isDrop() && support.isDataFlavorSupported(PATHS);
		}

		@Override
		public boolean importData(TransferSupport support) {
			if (!canImport(support)) {
				return false;
			}
			List<NodePath<AttributeDefinition<?>>> paths = TransferablePaths.paths(support);
			if (paths.isEmpty()) {
				return false;
			}
			model.columns().insert(paths, ((JList.DropLocation) support.getDropLocation()).getIndex());

			return true;
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

	/**
	 * Renders each attribute with a check box, checked when it is a column, a foreign key followed by the number of
	 * columns below it. The check box and the label are painted side by side, rather than the check box as the icon
	 * of the label, since some look and feels, such as Nimbus, measure and paint a component wrongly while measuring
	 * or painting another one.
	 */
	private final class AttributeRenderer extends JPanel implements TreeCellRenderer {

		private final DefaultTreeCellRenderer label = new DefaultTreeCellRenderer();
		private final JCheckBox checkBox = new JCheckBox();

		private AttributeRenderer() {
			super(new BorderLayout());
			configure();
			add(checkBox, BorderLayout.WEST);
			add(label, BorderLayout.CENTER);
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
																									boolean leaf, int row, boolean hasFocus) {
			label.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			label.setIcon(null);
			Color inactiveForeground = UIManager.getColor("Tree.selectionInactiveForeground");
			if (selected && inactiveForeground != null &&
							KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner() != tree) {
				//look and feels, such as FlatLaf, adjusting only a DefaultTreeCellRenderer to the inactive selection
				label.setForeground(inactiveForeground);
			}
			NodePath<AttributeDefinition<?>> path = (NodePath<AttributeDefinition<?>>) value;
			checkBox.setVisible(!path.root());
			if (!path.root()) {
				checkBox.setSelected(model.columns().included(path));
				String caption = path.item().caption();
				int includedCount = path.item() instanceof ForeignKeyDefinition ? model.columns().includedCount(path) : 0;
				label.setText(includedCount > 0 ? caption + " (" + includedCount + ")" : caption);
			}
			//the label, a DefaultTreeCellRenderer, does not invalidate this panel when its text changes,
			//which would otherwise keep the preferred size it had when last validated, truncating the text
			invalidate();

			return this;
		}

		@Override
		public void updateUI() {
			super.updateUI();
			//null when called during construction
			if (label != null) {
				label.updateUI();
				checkBox.updateUI();
				configure();
			}
		}

		private int checkBoxWidth() {
			return checkBox.getPreferredSize().width;
		}

		private void configure() {
			setOpaque(false);
			checkBox.setOpaque(false);
			checkBox.setBorder(createEmptyBorder());
		}
	}
}
