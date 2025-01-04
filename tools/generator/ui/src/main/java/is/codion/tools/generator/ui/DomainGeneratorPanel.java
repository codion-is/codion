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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.generator.ui;

import is.codion.common.db.database.Database;
import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.observable.Observable;
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.plugin.intellij.themes.materialtheme.MaterialTheme;
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
import is.codion.tools.generator.model.DomainGeneratorModel;
import is.codion.tools.generator.model.DomainGeneratorModel.EntityColumns;
import is.codion.tools.generator.model.DomainGeneratorModel.SchemaColumns;
import is.codion.tools.generator.model.EntityRow;
import is.codion.tools.generator.model.SchemaRow;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static is.codion.common.Configuration.stringValue;
import static is.codion.common.model.UserPreferences.setUserPreference;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.dialog.Dialogs.lookAndFeelSelectionDialog;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.defaultLookAndFeelName;
import static is.codion.swing.common.ui.laf.LookAndFeelProviders.findLookAndFeelProvider;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.JOptionPane.showMessageDialog;

public final class DomainGeneratorPanel extends JPanel {

	/**
	 * The default user on the form username:password or just username.
	 */
	public static final PropertyValue<String> DEFAULT_USER =
					stringValue("codion.domain.generator.defaultUser");

	private static final double RESIZE_WEIGHT = 0.2;

	private final DomainGeneratorModel model;
	private final FilterTable<SchemaRow, SchemaColumns.Id> schemaTable;
	private final FilterTable<EntityRow, EntityColumns.Id> entityTable;
	private final JTextArea apiTextArea;
	private final JTextArea implementationTextArea;
	private final JTextArea combinedTextArea;
	private final JTabbedPane sourceTabbedPane;
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
		entityTable = createEntityTable();
		apiTextArea = createSourceTextArea(model.domainApi());
		implementationTextArea = createSourceTextArea(model.domainImpl());
		combinedTextArea = createSourceTextArea(model.domainCombined());
		sourceTabbedPane = createSourceTabbedPane();
		apiHighlighter = searchHighlighter(apiTextArea);
		implementationHighlighter = searchHighlighter(implementationTextArea);
		combinedHighlighter = searchHighlighter(combinedTextArea);
		initializeUI();
		bindEvents();
		setupKeyEvents();
	}

	private void initializeUI() {
		JPanel schemaSourceDirPanel = borderLayoutPanel()
						.northComponent(createSourceDirectoryPanel())
						.centerComponent(borderLayoutPanel()
										.centerComponent(splitPane()
														.orientation(JSplitPane.VERTICAL_SPLIT)
														.resizeWeight(RESIZE_WEIGHT)
														.topComponent(createScrollablePanel(schemaTable, "Schemas (Alt-1)"))
														.bottomComponent(createScrollablePanel(entityTable, "Entities (Alt-2)"))
														.build())
										.build())
						.build();

		JPanel sourcePackagePanel = borderLayoutPanel()
						.northComponent(createPackageSavePanel())
						.centerComponent(sourceTabbedPane)
						.build();

		JSplitPane splitPane = splitPane()
						.resizeWeight(RESIZE_WEIGHT)
						.leftComponent(schemaSourceDirPanel)
						.rightComponent(sourcePackagePanel)
						.build();

		setLayout(borderLayout());
		add(splitPane, BorderLayout.CENTER);
	}

	private FilterTable<SchemaRow, SchemaColumns.Id> createSchemaTable() {
		Control populateSchemaControl = Control.builder()
						.command(this::populateSchema)
						.name("Populate")
						.enabled(model.schemaModel().selection().empty().not())
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
										.control(Controls.builder()
														.name("Columns")
														.control(table.createToggleColumnsControls())
														.control(table.createToggleAutoResizeModeControls()))
										.build())
						.build();
	}

	private FilterTable<EntityRow, EntityColumns.Id> createEntityTable() {
		return FilterTable.builder(model.entityModel(), createEntityColumns())
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.popupMenuControl(FilterTable::createToggleAutoResizeModeControls)
						.build();
	}

	private JSplitPane createApiImplPanel() {
		return splitPane()
						.orientation(JSplitPane.VERTICAL_SPLIT)
						.resizeWeight(0.5)
						.topComponent(borderLayoutPanel()
										.centerComponent(createScrollablePanel(apiTextArea, "API (Alt-3)"))
										.southComponent(createSearchCopyPanel(apiTextArea))
										.build())
						.bottomComponent(borderLayoutPanel()
										.centerComponent(createScrollablePanel(implementationTextArea, "Implementation (Alt-4)"))
										.southComponent(createSearchCopyPanel(implementationTextArea))
										.build())
						.continuousLayout(true)
						.oneTouchExpandable(true)
						.build();
	}

	private JPanel createCombinedPanel() {
		return borderLayoutPanel()
						.centerComponent(createScrollablePanel(combinedTextArea, "Combined (Alt-5)"))
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
		return Control.builder()
						.command(() -> Utilities.setClipboard(textArea.getText()))
						.name(Messages.copy())
						.mnemonic(Messages.copy().charAt(0))
						.build();
	}

	private static JTextArea createSourceTextArea(Observable<String> observable) {
		return textArea()
						.link(observable)
						.rowsColumns(40, 60)
						.editable(false)
						.font(monospaceFont())
						.caretUpdatePolicy(DefaultCaret.NEVER_UPDATE)
						.build();
	}

	private JTabbedPane createSourceTabbedPane() {
		return tabbedPane()
						.tabBuilder("API/Impl", createApiImplPanel())
						.mnemonic('A')
						.add()
						.tabBuilder("Combined", createCombinedPanel())
						.mnemonic('C')
						.add()
						.build();
	}

	private static Font monospaceFont() {
		Font font = UIManager.getFont("TextArea.font");

		return new Font(Font.MONOSPACED, font.getStyle(), font.getSize());
	}

	private JPanel createPackageSavePanel() {
		JLabel packageLabel = label("Package")
						.displayedMnemonic('P')
						.build();

		return borderLayoutPanel()
						.centerComponent(borderLayoutPanel()
										.westComponent(gridLayoutPanel(2, 1)
														.add(new JLabel(" "))
														.add(createDtoCheckBox())
														.build())
										.centerComponent(gridLayoutPanel(2, 1)
														.add(packageLabel)
														.add(createPackageField(packageLabel))
														.build())
										.build())
						.eastComponent(gridLayoutPanel(2, 1)
										.add(label(" ").build())
										.add(button(Control.builder()
														.command(this::save)
														.name("Save")
														.mnemonic('S')
														.enabled(model.saveEnabled()))
														.build())
										.build())
						.build();
	}

	private JPanel createSourceDirectoryPanel() {
		JLabel sourceDirectoryLabel = label("Source directory")
						.displayedMnemonic('D')
						.build();
		Control selectSourceDirectoryControl = Control.builder()
						.command(this::selectSourceDirectory)
						.name("...")
						.build();

		return borderLayoutPanel()
						.centerComponent(gridLayoutPanel(2, 1)
										.add(sourceDirectoryLabel)
										.add(createSourceDirectoryField(sourceDirectoryLabel, selectSourceDirectoryControl))
										.build())
						.eastComponent(gridLayoutPanel(2, 1)
										.add(label(" ").build())
										.add(button(selectSourceDirectoryControl).build())
										.build())
						.build();
	}

	private JCheckBox createDtoCheckBox() {
		return checkBox(model.includeDto())
						.text("Include DTOs")
						.mnemonic('D')
						.onBuild(checkBox -> KeyEvents.builder(KeyEvent.VK_D)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.modifiers(InputEvent.ALT_DOWN_MASK)
										.action(command(() -> checkBox.setSelected(!checkBox.isSelected())))
										.enable(this))
						.build();
	}

	private JTextField createPackageField(JLabel packageLabel) {
		return stringField(model.domainPackage())
						.hint("(Alt-P)")
						.onBuild(field -> KeyEvents.builder(KeyEvent.VK_P)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.modifiers(InputEvent.ALT_DOWN_MASK)
										.action(command(field::requestFocusInWindow))
										.enable(this))
						.onBuild(packageLabel::setLabelFor)
						.build();
	}

	private JTextField createSourceDirectoryField(JLabel sourceDirectoryLabel,
																								Control selectSourceDirectoryControl) {
		return stringField(model.sourceDirectory())
						.hint("(Alt-D / INSERT)")
						.editable(false)
						.keyEvent(KeyEvents.builder(KeyEvent.VK_INSERT)
										.action(selectSourceDirectoryControl))
						.onBuild(field -> KeyEvents.builder(KeyEvent.VK_D)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.modifiers(InputEvent.ALT_DOWN_MASK)
										.action(command(field::requestFocusInWindow))
										.enable(this))
						.onBuild(sourceDirectoryLabel::setLabelFor)
						.build();
	}

	private void selectSourceDirectory() {
		model.sourceDirectory().set(Dialogs.fileSelectionDialog()
						.startDirectory(DomainGeneratorModel.DEFAULT_SOURCE_DIRECTORY.get())
						.selectDirectory()
						.getAbsolutePath());
	}

	private void save() throws IOException {
		if (sourceTabbedPane.getSelectedIndex() == 0) {
			model.saveApiImpl();
		}
		else {
			model.saveCombined();
		}

		showMessageDialog(this, "File(s) saved");
	}

	private static JPanel createScrollablePanel(JComponent component, String title) {
		return borderLayoutPanel()
						.centerComponent(scrollPane(component).build())
						.border(createCompoundBorder(createTitledBorder(title), createEmptyBorder()))
						.build();
	}

	private static Border createEmptyBorder() {
		return BorderFactory.createEmptyBorder(Layouts.GAP.getOrThrow(), Layouts.GAP.getOrThrow(),
						Layouts.GAP.getOrThrow(), Layouts.GAP.getOrThrow());
	}

	public void showFrame() {
		Windows.frame(borderLayoutPanel()
										.centerComponent(this)
										.border(createEmptyBorder())
										.build())
						.title("Codion Domain Generator")
						.icon(Logos.logoTransparent())
						.menuBar(menu(createMainMenuControls()).buildMenuBar())
						.defaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
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
						.onResult(() -> model.entityModel().items().refresh())
						.execute();
	}

	private Controls createMainMenuControls() {
		return Controls.builder()
						.control(Controls.builder()
										.name("File")
										.mnemonic('F')
										.control(Control.builder()
														.command(() -> System.exit(0))
														.name("Exit")
														.mnemonic('X')))
						.control(Controls.builder()
										.name("View")
										.mnemonic('V')
										.control(lookAndFeelSelectionDialog()
														.owner(this)
														.createControl(DomainGeneratorPanel::lookAndFeelSelected)))
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
						.action(command(schemaTable::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_2)
						.action(command(entityTable::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_3)
						.action(command(apiTextArea::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_4)
						.action(command(implementationTextArea::requestFocusInWindow))
						.enable(this)
						.keyCode(KeyEvent.VK_5)
						.action(command(combinedTextArea::requestFocusInWindow))
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

	private static List<FilterTableColumn<EntityColumns.Id>> createEntityColumns() {
		FilterTableColumn<EntityColumns.Id> entityTypeColumn =
						FilterTableColumn.builder(EntityColumns.Id.ENTITY)
										.headerValue("Entity")
										.build();
		FilterTableColumn<EntityColumns.Id> typeColumn =
						FilterTableColumn.builder(EntityColumns.Id.TABLE_TYPE)
										.headerValue("Type")
										.preferredWidth(120)
										.build();

		return asList(entityTypeColumn, typeColumn);
	}

	private static SearchHighlighter searchHighlighter(JTextArea textArea) {
		return SearchHighlighter.builder(textArea)
						.scrollYRatio(0.2)
						.scrollXRatio(0.5)
						.build();
	}

	private static void lookAndFeelSelected(LookAndFeelProvider lookAndFeelProvider) {
		setUserPreference(DomainGeneratorPanel.class.getName(),
						lookAndFeelProvider.lookAndFeelInfo().getClassName());
	}

	/**
	 * Runs a DomainGeneratorPanel instance in a frame
	 * @param arguments no arguments required
	 */
	public static void main(String[] arguments) {
		LookAndFeelProvider.SYSTEM.set(false);
		LookAndFeelProvider.CROSS_PLATFORM.set(false);
		findLookAndFeelProvider(defaultLookAndFeelName(DomainGeneratorPanel.class.getName(),
						MaterialTheme.class.getName())).ifPresent(LookAndFeelProvider::enable);
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
		new DomainGeneratorPanel(DomainGeneratorModel.domainGeneratorModel(database,
						Dialogs.loginDialog()
										.icon(Logos.logoTransparent())
										.defaultUser(DEFAULT_USER.optional()
														.map(User::parse)
														.orElse(null))
										.validator(user -> database.createConnection(user).close())
										.show()))
						.showFrame();
	}
}
