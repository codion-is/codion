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
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.component.text.SearchHighlighter;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.tools.generator.DefinitionRow;
import is.codion.swing.framework.model.tools.generator.DomainGeneratorModel;
import is.codion.swing.framework.model.tools.generator.DomainGeneratorModel.DefinitionColumns;
import is.codion.swing.framework.model.tools.generator.DomainGeneratorModel.SchemaColumns;
import is.codion.swing.framework.model.tools.generator.SchemaRow;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static is.codion.common.Configuration.stringValue;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.text.SearchHighlighter.searchHighlighter;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.dialog.Dialogs.lookAndFeelSelectionDialog;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.defaultLookAndFeelName;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.findLookAndFeelProvider;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createTitledBorder;

public final class DomainGeneratorPanel extends JPanel {

	/**
	 * The default username.
	 */
	public static final PropertyValue<String> DEFAULT_USERNAME =
					stringValue("codion.domain.generator.defaultUsername");

	private static final String DEFAULT_FLAT_LOOK_AND_FEEL =
					"com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme";

	private static final double RESIZE_WEIGHT = 0.2;

	private final DomainGeneratorModel model;
	private final FilterTable<SchemaRow, SchemaColumns.Id> schemaTable;
	private final FilterTable<DefinitionRow, DefinitionColumns.Id> definitionTable;
	private final JTextArea apiTextArea;
	private final JTextArea implementationTextArea;
	private final JTextArea combinedTextArea;
	private final SearchHighlighter apiHighlighter;
	private final SearchHighlighter implementationHighlighter;
	private final SearchHighlighter combinedHighlighter;

	/**
	 * Instantiates a new DomainGeneratorPanel.
	 * @param model the domain generator model to base this panel on
	 */
	DomainGeneratorPanel(DomainGeneratorModel model) {
		this.model = requireNonNull(model);
		schemaTable = createSchemaTable();
		definitionTable = createDefinitionTable();
		apiTextArea = createSourceTextArea(model.domainApi());
		implementationTextArea = createSourceTextArea(model.domainImpl());
		combinedTextArea = createSourceTextArea(model.domainCombined());
		apiHighlighter = searchHighlighter(apiTextArea);
		implementationHighlighter = searchHighlighter(implementationTextArea);
		combinedHighlighter = searchHighlighter(combinedTextArea);
		initializeUI();
		bindEvents();
		setupKeyEvents();
	}

	private void initializeUI() {
		JPanel packagePanel = createPackagePanel(model);
		JPanel schemaTablePanel = borderLayoutPanel()
						.northComponent(label()
										.preferredHeight(packagePanel.getPreferredSize().height)
										.build())
						.centerComponent(splitPane()
										.orientation(JSplitPane.VERTICAL_SPLIT)
										.resizeWeight(RESIZE_WEIGHT)
										.topComponent(createScrollablePanel(schemaTable, "Schemas (ALT-1)"))
										.bottomComponent(createScrollablePanel(definitionTable, "Entities (ALT-2)"))
										.build())
						.build();

		JPanel sourcePanel = borderLayoutPanel()
						.centerComponent(tabbedPane()
										.tabBuilder("API/Impl", createApiImplPanel())
										.mnemonic('A')
										.add()
										.tabBuilder("Combined", createCombinedPanel())
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
	}

	private FilterTable<SchemaRow, SchemaColumns.Id> createSchemaTable() {
		Control populateSchemaControl = Control.builder(this::populateSchema)
						.name("Populate")
						.enabled(model.schemaModel().selectionModel().selectionNotEmpty())
						.build();

		return FilterTable.builder(model.schemaModel(), createSchemaColumns())
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.doubleClickAction(populateSchemaControl)
						.selectionMode(ListSelectionModel.SINGLE_SELECTION)
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
						.onBuild(table -> table.sortModel().setSortOrder(SchemaColumns.Id.SCHEMA, SortOrder.ASCENDING))
						.build();
	}

	private FilterTable<DefinitionRow, DefinitionColumns.Id> createDefinitionTable() {
		return FilterTable.builder(model.definitionModel(), createDefinitionColumns())
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.popupMenuControl(FilterTable::createAutoResizeModeControl)
						.onBuild(table -> table.sortModel().setSortOrder(DefinitionColumns.Id.ENTITY, SortOrder.ASCENDING))
						.build();
	}

	private JSplitPane createApiImplPanel() {
		return splitPane()
						.orientation(JSplitPane.VERTICAL_SPLIT)
						.resizeWeight(0.5)
						.topComponent(borderLayoutPanel()
										.centerComponent(createScrollablePanel(apiTextArea, "API (ALT-3)"))
										.southComponent(createSearchCopyPanel(apiTextArea))
										.build())
						.bottomComponent(borderLayoutPanel()
										.centerComponent(createScrollablePanel(implementationTextArea, "Implementation (ALT-4)"))
										.southComponent(createSearchCopyPanel(implementationTextArea))
										.build())
						.continuousLayout(true)
						.oneTouchExpandable(true)
						.build();
	}

	private JPanel createCombinedPanel() {
		return borderLayoutPanel()
						.centerComponent(createScrollablePanel(combinedTextArea, "Combined (ALT-5)"))
						.southComponent(createSearchCopyPanel(combinedTextArea))
						.build();
	}

	private static JPanel createSearchCopyPanel(JTextArea textArea) {
		return borderLayoutPanel()
						.eastComponent(button(createCopyControl(textArea))
										.build())
						.build();
	}

	private static Control createCopyControl(JTextArea textArea) {
		return Control.builder(() -> Utilities.setClipboard(textArea.getText()))
						.name(Messages.copy())
						.mnemonic(Messages.copy().charAt(0))
						.build();
	}

	private static JTextArea createSourceTextArea(ValueObserver<String> sourceValue) {
		return textArea()
						.link(sourceValue)
						.rowsColumns(40, 60)
						.editable(false)
						.font(monospaceFont())
						.build();
	}

	private static Font monospaceFont() {
		Font font = UIManager.getFont("TextArea.font");

		return new Font(Font.MONOSPACED, font.getStyle(), font.getSize());
	}

	private static JPanel createPackagePanel(DomainGeneratorModel model) {
		JLabel packageLabel = label("Package")
						.displayedMnemonic('P')
						.build();

		return borderLayoutPanel()
						.westComponent(packageLabel)
						.centerComponent(stringField(model.domainPackage())
										.columns(42)
										.build(packageLabel::setLabelFor))
						.build();
	}

	private static JPanel createScrollablePanel(JComponent component, String title) {
		return borderLayoutPanel()
						.centerComponent(scrollPane(component).build())
						.border(BorderFactory.createCompoundBorder(createTitledBorder(title),
										BorderFactory.createEmptyBorder(Layouts.GAP.get(), Layouts.GAP.get(),
														Layouts.GAP.get(), Layouts.GAP.get())))
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

	private void bindEvents() {
		model.domainApi().addListener(() -> apiTextArea.setCaretPosition(0));
		model.domainImpl().addListener(() -> implementationTextArea.setCaretPosition(0));
		model.domainCombined().addListener(() -> combinedTextArea.setCaretPosition(0));
		model.apiSearchValue().addConsumer(apiHighlighter.searchString()::set);
		model.implSearchValue().addConsumer(implementationHighlighter.searchString()::set);
		model.implSearchValue().addConsumer(combinedHighlighter.searchString()::set);
	}

	private void setupKeyEvents() {
		KeyEvents.builder()
						.modifiers(InputEvent.ALT_DOWN_MASK)
						.condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.keyCode(KeyEvent.VK_1)
						.action(control(schemaTable::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_2)
						.action(control(definitionTable::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_3)
						.action(control(apiTextArea::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_4)
						.action(control(implementationTextArea::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_5)
						.action(control(combinedTextArea::requestFocusInWindow))
						.enable(this);
	}

	private static List<FilterTableColumn<SchemaColumns.Id>> createSchemaColumns() {
		FilterTableColumn<SchemaColumns.Id> catalogColumn =
						FilterTableColumn.builder(SchemaColumns.Id.CATALOG)
										.headerValue("Catalog")
										.build();
		FilterTableColumn<SchemaColumns.Id> schemaColumn =
						FilterTableColumn.builder(SchemaColumns.Id.SCHEMA)
										.headerValue("Schema")
										.build();
		FilterTableColumn<SchemaColumns.Id> populatedColumn =
						FilterTableColumn.builder(SchemaColumns.Id.POPULATED)
										.headerValue("Populated")
										.build();

		return asList(catalogColumn, schemaColumn, populatedColumn);
	}

	private static List<FilterTableColumn<DefinitionColumns.Id>> createDefinitionColumns() {
		FilterTableColumn<DefinitionColumns.Id> entityTypeColumn =
						FilterTableColumn.builder(DefinitionColumns.Id.ENTITY)
										.headerValue("Entity")
										.build();
		FilterTableColumn<DefinitionColumns.Id> typeColumn =
						FilterTableColumn.builder(DefinitionColumns.Id.TABLE_TYPE)
										.headerValue("Type")
										.preferredWidth(120)
										.build();

		return asList(entityTypeColumn, typeColumn);
	}

	/**
	 * Runs a DomainGeneratorPanel instance in a frame
	 * @param arguments no arguments required
	 */
	public static void main(String[] arguments) {
		Arrays.stream(FlatAllIJThemes.INFOS)
						.forEach(LookAndFeelProvider::addLookAndFeel);
		LookAndFeelProvider.SYSTEM.set(false);
		LookAndFeelProvider.CROSS_PLATFORM.set(false);
		findLookAndFeelProvider(defaultLookAndFeelName(DomainGeneratorPanel.class.getName(),
						DEFAULT_FLAT_LOOK_AND_FEEL)).ifPresent(LookAndFeelProvider::enable);
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
											.defaultUser(DEFAULT_USERNAME.optional()
															.map(User::user)
															.orElse(null))
											.validator(user -> database.createConnection(user).close())
											.show()))
							.showFrame();
		}
		catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
	}
}
