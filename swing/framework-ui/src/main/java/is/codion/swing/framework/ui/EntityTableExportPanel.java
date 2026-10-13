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
import is.codion.common.model.CancelException;
import is.codion.common.model.component.tree.NodePath;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.list.FilterList;
import is.codion.swing.common.ui.component.tree.FilterTree;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.ui.EntityTableExportModel.ConfigurationFile;
import is.codion.swing.framework.ui.EntityTableExportModel.ExportTask;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.button.ToggleButtonType.RADIO_BUTTON;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.EntityTableExportModel.NULL_CONFIGURATION_FILE;
import static is.codion.swing.framework.ui.EntityTableExportPanel.ControlKeys.*;
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
	private static final String PATH_SEPARATOR = " › ";
	private static final FileNameExtensionFilter CONFIGURATION_FILE =
					new FileNameExtensionFilter(MESSAGES.getString("configuration_file") + " (" + JSON + ")", JSON);
	private static final DataFlavor PATHS = new DataFlavor(TransferablePaths.class, "Attribute paths");

	/**
	 * The controls, their key strokes listed among the keyboard shortcuts.
	 */
	static final class ControlKeys {

		/**
		 * Checks or unchecks the selected attributes.<br>
		 * Default key stroke: SPACE
		 */
		static final ControlKey<CommandControl> TOGGLE = CommandControl.key("toggle", keyStroke(VK_SPACE));
		/**
		 * Checks or unchecks the attributes below the selected foreign keys.<br>
		 * Default key stroke: SHIFT-SPACE
		 */
		static final ControlKey<CommandControl> TOGGLE_BELOW = CommandControl.key("toggleBelow", keyStroke(VK_SPACE, SHIFT_DOWN_MASK));
		/**
		 * Moves the selected columns up.<br>
		 * Default key stroke: ALT-UP ARROW
		 */
		static final ControlKey<CommandControl> MOVE_UP = CommandControl.key("moveUp", keyStroke(VK_UP, ALT_DOWN_MASK));
		/**
		 * Moves the selected columns down.<br>
		 * Default key stroke: ALT-DOWN ARROW
		 */
		static final ControlKey<CommandControl> MOVE_DOWN = CommandControl.key("moveDown", keyStroke(VK_DOWN, ALT_DOWN_MASK));
		/**
		 * Removes the selected columns.<br>
		 * Default key stroke: DELETE
		 */
		static final ControlKey<CommandControl> REMOVE = CommandControl.key("remove", keyStroke(VK_DELETE));
		/**
		 * Moves the focus to the search field.<br>
		 * Default key stroke: CTRL-F
		 */
		static final ControlKey<CommandControl> FOCUS_SEARCH_FIELD = CommandControl.key("focusSearchField", keyStroke(VK_F, MENU_SHORTCUT_MASK));
		/**
		 * Moves the focus to the attributes.<br>
		 * Default key stroke: ALT-1
		 */
		static final ControlKey<CommandControl> FOCUS_ATTRIBUTES = CommandControl.key("focusAttributes", keyStroke(VK_1, ALT_DOWN_MASK));
		/**
		 * Moves the focus to the columns.<br>
		 * Default key stroke: ALT-2
		 */
		static final ControlKey<CommandControl> FOCUS_COLUMNS = CommandControl.key("focusColumns", keyStroke(VK_2, ALT_DOWN_MASK));
		/**
		 * Moves the focus to the configurations.<br>
		 * Default key stroke: ALT-3
		 */
		static final ControlKey<CommandControl> FOCUS_CONFIGURATIONS = CommandControl.key("focusConfigurations", keyStroke(VK_3, ALT_DOWN_MASK));

		private ControlKeys() {}
	}

	private final EntityTableExportModel model;
	private final AtomicReference<Dimension> dialogSize;
	private final AttributeRenderer attributeRenderer = new AttributeRenderer();
	private final Value<String> allRowsCaption = Value.nonNull("");
	private final Value<String> selectedRowsCaption = Value.nonNull("");
	private final State exporting = State.state();
	private final Control toggle;
	private final Control toggleBelow;
	private final Control moveUp;
	private final Control moveDown;
	private final Control removeColumns;
	private final Control tableColumns;
	private final Control clearColumns;
	private final Control openConfiguration;
	private final Control saveConfiguration;
	private final Control saveConfigurationAs;
	private final ToggleControl showHidden;
	private final ToggleControl allRows;
	private final ToggleControl selectedRows;
	private final FilterTree<AttributeDefinition<?>> attributeTree;
	private final FilterList<NodePath<AttributeDefinition<?>>> columnList;
	private final JComboBox<ConfigurationFile> configurationFilesComboBox;
	private final JTextField searchField;

	EntityTableExportPanel(EntityTableExportModel model, AtomicReference<Dimension> dialogSize) {
		super(borderLayout());
		this.model = model;
		this.dialogSize = dialogSize;
		ObservableState columnsSelected = model.columns().list().selection().present();
		this.toggle = Control.builder()
						.command(() -> model.columns().toggle(selectedAttributes()))
						.caption(MESSAGES.getString("toggle"))
						.keyStroke(TOGGLE.defaultKeystroke().get())
						.build();
		this.toggleBelow = Control.builder()
						.command(() -> model.columns().toggleChildren(selectedAttributes()))
						.caption(MESSAGES.getString("toggle_below"))
						.enabled(model.columns().foreignKeySelected())
						.keyStroke(TOGGLE_BELOW.defaultKeystroke().get())
						.build();
		this.moveUp = Control.builder()
						.command(() -> model.columns().move(selectedColumns(), true))
						.caption(MESSAGES.getString("move_up"))
						.enabled(columnsSelected)
						.icon(FrameworkIcons.instance().up())
						.description(MESSAGES.getString("move_up"))
						.keyStroke(MOVE_UP.defaultKeystroke().get())
						.build();
		this.moveDown = Control.builder()
						.command(() -> model.columns().move(selectedColumns(), false))
						.caption(MESSAGES.getString("move_down"))
						.enabled(columnsSelected)
						.icon(FrameworkIcons.instance().down())
						.description(MESSAGES.getString("move_down"))
						.keyStroke(MOVE_DOWN.defaultKeystroke().get())
						.build();
		this.removeColumns = Control.builder()
						.command(() -> model.columns().remove(selectedColumns()))
						.caption(MESSAGES.getString("remove"))
						.enabled(columnsSelected)
						.icon(FrameworkIcons.instance().delete())
						.description(MESSAGES.getString("remove"))
						.keyStroke(REMOVE.defaultKeystroke().get())
						.build();
		this.tableColumns = Control.builder()
						.command(model.columns()::reset)
						.caption(MESSAGES.getString("table_columns"))
						.icon(FrameworkIcons.instance().columns())
						.description(MESSAGES.getString("table_columns_description"))
						.build();
		this.clearColumns = Control.builder()
						.command(model.columns()::clear)
						.caption(MESSAGES.getString("clear"))
						.icon(FrameworkIcons.instance().clear())
						.description(MESSAGES.getString("clear_description"))
						.build();
		this.openConfiguration = Control.builder()
						.command(this::openConfigurationFiles)
						.caption(Messages.open())
						.mnemonic(Messages.openMnemonic())
						.description(MESSAGES.getString("open_description"))
						.build();
		this.saveConfiguration = Control.builder()
						.command(this::saveConfiguration)
						.enabled(State.and(model.configurationFiles().selection().present(), model.modified()))
						.caption(Messages.save())
						.mnemonic(Messages.saveMnemonic())
						.description(MESSAGES.getString("save_description"))
						.build();
		this.saveConfigurationAs = Control.builder()
						.command(this::saveConfigurationAs)
						.caption(MESSAGES.getString("save_as"))
						.mnemonic(MESSAGES.getString("save_as_mnemonic").charAt(0))
						.description(MESSAGES.getString("save_as_description"))
						.build();
		this.showHidden = Control.builder()
						.toggle(model.columns().showHidden())
						.caption(MESSAGES.getString("show_hidden"))
						.mnemonic(MESSAGES.getString("show_hidden_mnemonic").charAt(0))
						.description(MESSAGES.getString("show_hidden_description"))
						.build();
		this.selectedRows = Control.builder()
						.toggle(model.selected())
						.caption(selectedRowsCaption)
						.mnemonic(MESSAGES.getString("rows_selected_mnemonic").charAt(0))
						.build();
		this.allRows = Control.builder()
						.toggle(model.all())
						.caption(allRowsCaption)
						.mnemonic(MESSAGES.getString("rows_all_mnemonic").charAt(0))
						.build();
		this.attributeTree = createAttributeTree();
		this.columnList = createColumnList();
		this.searchField = createSearchField();
		this.configurationFilesComboBox = createConfigurationFilesComboBox();
		//the table columns as they are when the export is first shown, with the column preferences applied
		model.initialize();
		model.rowCounts().addListener(this::updateRowCounts);
		model.configurationFiles().selection().changing().addListener(this::confirmDiscard);
		updateRowCounts();
		initializeUI();
		configureKeyEvents();
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
							.disposeOnEscape(false)
							.title(MESSAGES.getString("export") + " - " + model.entityDefinition().caption())
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
						.onResult(this::exportedToFile)
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

	/**
	 * Reports the export, offering to open the file or its folder, where the desktop supports it.
	 */
	private void exportedToFile(Path path) {
		String message = MESSAGES.getString("exported_to_file") + "\n" + path;
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
			JOptionPane.showMessageDialog(this, message, MESSAGES.getString("data_exported"), JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		String[] options = {MESSAGES.getString("open_file"), MESSAGES.getString("open_folder"), Messages.ok()};
		int option = JOptionPane.showOptionDialog(this, message, MESSAGES.getString("data_exported"),
						JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[2]);
		if (option == 0 || option == 1) {
			File file = path.toFile();
			try {
				Desktop.getDesktop().open(option == 0 ? file : file.getAbsoluteFile().getParentFile());
			}
			catch (IOException e) {
				Dialogs.exception()
								.owner(this)
								.show(e);
			}
		}
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
						.doubleClick(new ToggleOnDoubleClick())
						.dragEnabled(true)
						.transferHandler(new AttributeTransferHandler())
						.popupControls(tree -> Controls.builder()
										.control(toggle)
										.control(toggleBelow)
										.separator()
										.control(showHidden)
										.build())
						.onBuild(this::configureAttributeTreeKeyEvents)
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
						.popupControls(list -> Controls.builder()
										.control(moveUp)
										.control(moveDown)
										.control(removeColumns)
										.separator()
										.control(tableColumns)
										.control(clearColumns)
										.build())
						.onBuild(this::configureColumnListKeyEvents)
						.build();
	}

	private JTextField createSearchField() {
		return stringField()
						.link(model.columns().search())
						.hint(Messages.search() + "...")
						.toolTipText(withKeyStroke(MESSAGES.getString("search_description"), FOCUS_SEARCH_FIELD))
						.selectAllOnFocusGained(true)
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_ESCAPE)
										.action(command(this::focusAttributes)))
						.build();
	}

	private JComboBox<ConfigurationFile> createConfigurationFilesComboBox() {
		return comboBox()
						.model(SwingFilterComboBoxModel.model(model.configurationFiles()))
						.toolTipText(MESSAGES.getString("configurations_description"))
						.popupControls(comboBox -> Controls.builder()
										.control(Control.builder()
														.command(model::removeConfigurationFile)
														.caption(MESSAGES.getString("remove_configuration"))
														.enabled(model.configurationFiles().selection().present()))
										.control(Control.builder()
														.command(model::clearConfigurationFiles)
														.caption(Messages.clear()))
										.build())
						.renderer(new ConfigurationFileRenderer())
						.build();
	}

	private void configureKeyEvents() {
		focus(FOCUS_SEARCH_FIELD, searchField);
		focus(FOCUS_ATTRIBUTES, attributeTree);
		focus(FOCUS_COLUMNS, columnList);
		focus(FOCUS_CONFIGURATIONS, configurationFilesComboBox);
	}

	private void focus(ControlKey<CommandControl> controlKey, JComponent component) {
		controlKey.defaultKeystroke().optional().ifPresent(keyStroke -> KeyEvents.builder()
						.keyStroke(keyStroke)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(command(component::requestFocusInWindow))
						.enable(this));
	}

	private void configureAttributeTreeKeyEvents(FilterTree<AttributeDefinition<?>> attributes) {
		TOGGLE.defaultKeystroke().optional().ifPresent(keyStroke -> KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(toggle)
						.enable(attributes));
		TOGGLE_BELOW.defaultKeystroke().optional().ifPresent(keyStroke -> KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(toggleBelow)
						.enable(attributes));
	}

	private void configureColumnListKeyEvents(FilterList<NodePath<AttributeDefinition<?>>> columns) {
		REMOVE.defaultKeystroke().optional().ifPresent(keyStroke -> KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(removeColumns)
						.enable(columns));
		MOVE_UP.defaultKeystroke().optional().ifPresent(keyStroke -> KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(moveUp)
						.enable(columns));
		MOVE_DOWN.defaultKeystroke().optional().ifPresent(keyStroke -> KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(moveDown)
						.enable(columns));
	}

	private void focusAttributes() {
		if (attributeTree.isSelectionEmpty() && attributeTree.getRowCount() > 0) {
			attributeTree.setSelectionRow(0);
		}
		attributeTree.requestFocusInWindow();
	}

	private void updateRowCounts() {
		NumberFormat format = NumberFormat.getIntegerInstance();
		allRowsCaption.set(MESSAGES.getString("rows_all") + " (" + format.format(model.allCount()) + ")");
		selectedRowsCaption.set(MESSAGES.getString("rows_selected") + " (" + format.format(model.selectedCount()) + ")");
	}

	/**
	 * Throws a {@link CancelException}, cancelling the configuration change, in case the columns have been modified
	 * and the user does not confirm the change.
	 */
	private void confirmDiscard() {
		if (model.modified().is() && isShowing() && JOptionPane.showConfirmDialog(this, FrameworkMessages.modifiedWarning(),
						FrameworkMessages.modifiedWarningTitle(), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
			throw new CancelException();
		}
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
		model.writeConfig(model.configurationFiles().selection().item().getOrThrow().file());
	}

	private void saveConfigurationAs() {
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
						.center(splitPane()
										.leftComponent(createAttributesPanel())
										.rightComponent(createColumnsPanel())
										.resizeWeight(0.5)
										.continuousLayout(true))
						.south(createConfigurationsPanel())
						.build(), BorderLayout.CENTER);
		add(createSouthPanel(), BorderLayout.SOUTH);
	}

	private JPanel createAttributesPanel() {
		return borderLayoutPanel()
						.border(createTitledBorder(withKeyStroke(MESSAGES.getString("available"), FOCUS_ATTRIBUTES)))
						.center(scrollPane()
										.view(attributeTree))
						.south(searchField)
						.build();
	}

	private JPanel createColumnsPanel() {
		return borderLayoutPanel()
						.border(createTitledBorder(withKeyStroke(MESSAGES.getString("columns"), FOCUS_COLUMNS)))
						.center(scrollPane()
										.view(columnList))
						.east(borderLayoutPanel()
										.north(gridLayoutPanel(0, 1)
														.add(button()
																		.control(moveUp)
																		.includeText(false))
														.add(button()
																		.control(moveDown)
																		.includeText(false))
														.add(button()
																		.control(removeColumns)
																		.includeText(false)))
										.south(gridLayoutPanel(0, 1)
														.add(button()
																		.control(clearColumns)
																		.includeText(false))
														.add(button()
																		.control(tableColumns)
																		.includeText(false))))
						.build();
	}

	private JPanel createConfigurationsPanel() {
		return borderLayoutPanel()
						.border(createTitledBorder(withKeyStroke(MESSAGES.getString("configurations"), FOCUS_CONFIGURATIONS)))
						.center(configurationFilesComboBox)
						.east(buttonPanel()
										.controls(Controls.builder()
														.control(openConfiguration)
														.control(saveConfiguration)
														.control(saveConfigurationAs))
										.transferFocusOnEnter(true))
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
														.mnemonic(MESSAGES.getString("close_mnemonic").charAt(0))))
						.fixedButtonSize(false)
						.build();
	}

	private void displayHelp() {
		Dialogs.builder()
						.owner(this)
						.title(MESSAGES.getString("help"))
						.component(borderLayoutPanel()
										.north(textArea()
														.value(MESSAGES.getString("help_text"))
														.editable(false)
														.focusable(false)
														.opaque(false)
														.border(emptyBorder()))
										.center(KeyboardShortcutsPanel.tableExportPanel())
										.border(emptyBorder()))
						.show();
	}

	/**
	 * @return the given text followed by the key stroke of the given control key, if any
	 */
	private static String withKeyStroke(String text, ControlKey<?> controlKey) {
		return controlKey.defaultKeystroke().optional()
						.map(keyStroke -> text + " (" + KeyboardShortcutsPanel.text(keyStroke) + ")")
						.orElse(text);
	}

	/**
	 * @return the captions on the given path, separated
	 */
	private static String caption(NodePath<AttributeDefinition<?>> path) {
		return path.items().stream()
						.map(AttributeDefinition::caption)
						.collect(joining(PATH_SEPARATOR));
	}

	/**
	 * @return true if the given mouse event is on the check box of a node
	 */
	private boolean onCheckBox(MouseEvent event) {
		TreePath treePath = attributeTree.getPathForLocation(event.getX(), event.getY());
		Rectangle bounds = treePath == null ? null : attributeTree.getPathBounds(treePath);

		return bounds != null && event.getX() < bounds.x + attributeRenderer.checkBoxWidth();
	}

	private final class ToggleOnCheckBoxClick extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent event) {
			if (isLeftMouseButton(event) && onCheckBox(event)) {
				TreePath treePath = attributeTree.getPathForLocation(event.getX(), event.getY());
				model.columns().toggle(singletonList((NodePath<AttributeDefinition<?>>) treePath.getLastPathComponent()));
			}
		}
	}

	/**
	 * Toggles an attribute double-clicked, other than on its check box, foreign keys expanding instead.
	 */
	private final class ToggleOnDoubleClick extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent event) {
			List<NodePath<AttributeDefinition<?>>> selected = selectedAttributes();
			if (!onCheckBox((MouseEvent) event.getSource()) && selected.size() == 1 &&
							!(selected.get(0).item() instanceof ForeignKeyDefinition)) {
				model.columns().toggle(selected);
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

	/**
	 * Renders the file name and its folder.
	 */
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
