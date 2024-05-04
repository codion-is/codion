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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.tools.generator;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.tools.generator.DefinitionRow;
import is.codion.swing.framework.model.tools.generator.DomainGeneratorModel;
import is.codion.swing.framework.model.tools.generator.DomainGeneratorModel.DefinitionColumns;
import is.codion.swing.framework.model.tools.generator.DomainGeneratorModel.SchemaColumns;
import is.codion.swing.framework.model.tools.generator.SchemaRow;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.dialog.Dialogs.lookAndFeelSelectionDialog;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.defaultLookAndFeelName;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.findLookAndFeelProvider;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public final class DomainGeneratorPanel extends JPanel {

	private static final double RESIZE_WEIGHT = 0.2;

	private final DomainGeneratorModel model;

	/**
	 * Instantiates a new DomainGeneratorPanel.
	 * @param model the domain generator model to base this panel on
	 */
	DomainGeneratorPanel(DomainGeneratorModel model) {
		this.model = requireNonNull(model);
		Control populateSchemaControl = Control.builder(this::populateSchema)
						.name("Populate")
						.enabled(model.schemaModel().selectionModel().selectionNotEmpty())
						.build();
		FilterTable<SchemaRow, SchemaColumns.Id> schemaTable =
						FilterTable.builder(model.schemaModel(), createSchemaColumns())
										.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
										.doubleClickAction(populateSchemaControl)
										.keyEvent(KeyEvents.builder(VK_ENTER)
														.modifiers(InputEvent.CTRL_DOWN_MASK)
														.action(populateSchemaControl))
										.popupMenuControls(table -> Controls.builder()
														.control(populateSchemaControl)
														.controls(Controls.builder()
																		.name("Columns")
																		.control(table.createToggleColumnsControls())
																		.controls(table.createAutoResizeModeControl()))
														.build())
										.build();
		schemaTable.sortModel().setSortOrder(SchemaColumns.Id.SCHEMA, SortOrder.ASCENDING);

		FilterTable<DefinitionRow, DefinitionColumns.Id> definitionTable =
						FilterTable.builder(model.definitionModel(), createDefinitionColumns())
										.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
										.popupMenuControl(FilterTable::createAutoResizeModeControl)
										.build();

		JSplitPane schemaTableSplitPane = splitPane()
						.orientation(JSplitPane.VERTICAL_SPLIT)
						.resizeWeight(RESIZE_WEIGHT)
						.topComponent(new JScrollPane(schemaTable))
						.bottomComponent(new JScrollPane(definitionTable))
						.build();

		JLabel packageLabel = label("Package")
						.displayedMnemonic('P')
						.build();
		JPanel packagePanel = borderLayoutPanel()
						.westComponent(packageLabel)
						.centerComponent(stringField(model.domainPackage())
										.columns(42)
										.build(packageLabel::setLabelFor))
						.build();

		JPanel schemaTablePanel = borderLayoutPanel()
						.northComponent(label()
										.preferredHeight(packagePanel.getPreferredSize().height)
										.build())
						.centerComponent(schemaTableSplitPane)
						.build();

		Font font = UIManager.getFont("TextArea.font");
		Font monospace = new Font(Font.MONOSPACED, font.getStyle(), font.getSize());
		JTextArea apiTextArea = textArea()
						.rowsColumns(40, 60)
						.editable(false)
						.font(monospace)
						.build();
		JTextArea implementationTextArea = textArea()
						.rowsColumns(40, 60)
						.editable(false)
						.font(monospace)
						.build();
		JTextArea combinedTextArea = textArea()
						.rowsColumns(40, 60)
						.editable(false)
						.font(monospace)
						.build();

		JSplitPane apiImplPanel = splitPane()
						.orientation(JSplitPane.VERTICAL_SPLIT)
						.resizeWeight(0.5)
						.topComponent(borderLayoutPanel()
										.centerComponent(new JScrollPane(apiTextArea))
										.southComponent(flowLayoutPanel(FlowLayout.RIGHT)
														.add(button(createCopyControl(apiTextArea))
																		.build())
														.build())
										.build())
						.bottomComponent(borderLayoutPanel()
										.centerComponent(new JScrollPane(implementationTextArea))
										.southComponent(flowLayoutPanel(FlowLayout.RIGHT)
														.add(button(createCopyControl(implementationTextArea))
																		.build())
														.build())
										.build())
						.continuousLayout(true)
						.oneTouchExpandable(true)
						.build();

		JPanel sourcePanel = borderLayoutPanel()
						.centerComponent(tabbedPane()
										.tabBuilder("API/Impl", apiImplPanel)
										.mnemonic('A')
										.add()
										.tabBuilder("Combined", borderLayoutPanel()
														.centerComponent(new JScrollPane(combinedTextArea))
														.southComponent(flowLayoutPanel(FlowLayout.RIGHT)
																		.add(button(createCopyControl(combinedTextArea))
																						.build())
																		.build())
														.build())
										.mnemonic('C')
										.add()
										.build())
						.build();

		JPanel sourcePackagePanel = borderLayoutPanel()
						.northComponent(packagePanel)
						.centerComponent(sourcePanel)
						.build();

		JSplitPane splitPane = splitPane()
						.resizeWeight(RESIZE_WEIGHT)
						.leftComponent(schemaTablePanel)
						.rightComponent(sourcePackagePanel)
						.build();

		setLayout(borderLayout());
		add(splitPane, BorderLayout.CENTER);

		model.domainApi().addConsumer(sourceText -> {
			apiTextArea.setText(sourceText);
			apiTextArea.setCaretPosition(0);
		});
		model.domainImpl().addConsumer(sourceText -> {
			implementationTextArea.setText(sourceText);
			implementationTextArea.setCaretPosition(0);
		});
		model.domainCombined().addConsumer(sourceText -> {
			combinedTextArea.setText(sourceText);
			combinedTextArea.setCaretPosition(0);
		});
		KeyEvents.builder()
						.modifiers(InputEvent.ALT_DOWN_MASK)
						.condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.keyCode(KeyEvent.VK_1)
						.action(Control.control(schemaTable::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_2)
						.action(Control.control(definitionTable::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_3)
						.action(Control.control(apiTextArea::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_4)
						.action(Control.control(implementationTextArea::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_5)
						.action(Control.control(combinedTextArea::requestFocusInWindow))
						.enable(this);
	}

	private static Control createCopyControl(JTextArea textArea) {
		return Control.builder(() -> Utilities.setClipboard(textArea.getText()))
						.name(Messages.copy())
						.mnemonic(Messages.copy().charAt(0))
						.build();
	}

	public void showFrame() {
		Windows.frame(this)
						.title("Codion Domain Generator")
						.icon(Logos.logoTransparent())
						.menuBar(menu(createMainMenuControls()).createMenuBar())
						.defaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
						.onClosing(windowEvent -> model.close())
						.centerFrame(true)
						.show();
	}

	private void populateSchema() {
		JLabel schemaLabel = new JLabel("Testing", SwingConstants.CENTER);
		JPanel northPanel = borderLayoutPanel()
						.centerComponent(schemaLabel)
						.build();
		Consumer<String> schemaNotifier = schema -> SwingUtilities.invokeLater(() -> schemaLabel.setText(schema));
		Dialogs.progressWorkerDialog(() -> model.populateSelected(schemaNotifier))
						.owner(this)
						.title("Populating")
						.northPanel(northPanel)
						.onResult(model.definitionModel()::refresh)
						.execute();
	}

	private Controls createMainMenuControls() {
		return Controls.builder()
						.controls(Controls.builder()
										.name("File")
										.mnemonic('F')
										.control(Control.builder(() -> System.exit(0))
														.name("Exit")
														.mnemonic('X')))
						.controls(Controls.builder()
										.name("View")
										.mnemonic('V')
										.control(lookAndFeelSelectionDialog()
														.owner(this)
														.userPreferencePropertyName(DomainGeneratorPanel.class.getName())
														.createControl()))
						.build();
	}

	private static List<FilterTableColumn<SchemaColumns.Id>> createSchemaColumns() {
		FilterTableColumn<SchemaColumns.Id> catalogColumn = FilterTableColumn.builder(SchemaColumns.Id.CATALOG)
						.headerValue("Catalog")
						.build();
		FilterTableColumn<SchemaColumns.Id> schemaColumn = FilterTableColumn.builder(SchemaColumns.Id.SCHEMA)
						.headerValue("Schema")
						.build();
		FilterTableColumn<SchemaColumns.Id> populatedColumn = FilterTableColumn.builder(SchemaColumns.Id.POPULATED)
						.headerValue("Populated")
						.build();

		return asList(catalogColumn, schemaColumn, populatedColumn);
	}

	private static List<FilterTableColumn<DefinitionColumns.Id>> createDefinitionColumns() {
		FilterTableColumn<DefinitionColumns.Id> domainColumn = FilterTableColumn.builder(DefinitionColumns.Id.DOMAIN)
						.headerValue("Domain")
						.build();
		FilterTableColumn<DefinitionColumns.Id> entityTypeColumn = FilterTableColumn.builder(DefinitionColumns.Id.ENTITY)
						.headerValue("Entity")
						.build();
		FilterTableColumn<DefinitionColumns.Id> typeColumn = FilterTableColumn.builder(DefinitionColumns.Id.TABLE_TYPE)
						.headerValue("Type")
						.preferredWidth(120)
						.build();

		return asList(domainColumn, entityTypeColumn, typeColumn);
	}

	/**
	 * Runs a DomainGeneratorPanel instance in a frame
	 * @param arguments no arguments required
	 */
	public static void main(String[] arguments) {
		Arrays.stream(FlatAllIJThemes.INFOS)
						.forEach(LookAndFeelProvider::addLookAndFeel);
		findLookAndFeelProvider(defaultLookAndFeelName(DomainGeneratorPanel.class.getName()))
						.ifPresent(LookAndFeelProvider::enable);
		try {
			SwingUtilities.invokeLater(DomainGeneratorPanel::start);
		}
		catch (CancelException ignored) {
			System.exit(0);
		}
		catch (Exception e) {
			Dialogs.displayExceptionDialog(e, null);
			System.exit(1);
		}
	}

	private static void start() {
		Database database = Database.instance();
		try {
			new DomainGeneratorPanel(DomainGeneratorModel.domainGeneratorModel(database,
							Dialogs.loginDialog()
											.icon(Logos.logoTransparent())
											.validator(user -> database.createConnection(user).close())
											.show()))
							.showFrame();
		}
		catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
	}
}
