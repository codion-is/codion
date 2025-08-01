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
import is.codion.common.model.preferences.UserPreferences;
import is.codion.common.observer.Observable;
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.component.text.SearchHighlighter;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.frame.Frames;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.tools.generator.model.DomainGeneratorModel;
import is.codion.tools.generator.model.DomainGeneratorModel.EntityColumns;
import is.codion.tools.generator.model.DomainGeneratorModel.SchemaColumns;
import is.codion.tools.generator.model.EntityRow;
import is.codion.tools.generator.model.SchemaRow;

import com.formdev.flatlaf.FlatDarculaLaf;

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

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.stringValue;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.enableLookAndFeel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.tools.generator.model.DomainGeneratorModel.domainGeneratorModel;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.JOptionPane.showMessageDialog;

public final class DomainGeneratorPanel extends JPanel {

	/**
	 * The default user on the form username:password or just username.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 */
	public static final PropertyValue<String> DEFAULT_USER =
					stringValue("codion.domain.generator.defaultUser");

	/**
	 * Specifies whether a user is required for connecting to the database.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> USER_REQUIRED =
					booleanValue("codion.domain.generator.userRequired", true);

	private static final double RESIZE_WEIGHT = 0.2;
	private static final String LOOK_AND_FEEL_PROPERTY = ".lookAndFeel";

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
						.caption("Populate")
						.enabled(model.schemaModel().selection().empty().not())
						.build();

		return FilterTable.builder()
						.model(model.schemaModel())
						.columns(createSchemaColumns())
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.doubleClick(populateSchemaControl)
						.selectionMode(ListSelectionModel.SINGLE_SELECTION)
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_ENTER)
										.modifiers(InputEvent.CTRL_DOWN_MASK)
										.action(populateSchemaControl))
						.popupMenuControls(table -> Controls.builder()
										.control(populateSchemaControl)
										.control(Controls.builder()
														.caption("Columns")
														.control(table.createToggleColumnsControls())
														.control(table.createToggleAutoResizeModeControls()))
										.build())
						.build();
	}

	private FilterTable<EntityRow, EntityColumns.Id> createEntityTable() {
		return FilterTable.builder()
						.model(model.entityModel())
						.columns(createEntityColumns())
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
						.eastComponent(button()
										.control(createCopyControl(textArea))
										.build())
						.build();
	}

	private static Control createCopyControl(JTextArea textArea) {
		return Control.builder()
						.command(() -> Utilities.setClipboard(textArea.getText()))
						.caption(Messages.copy())
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
		JLabel packageLabel = label()
						.text("Package")
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
										.add(label()
														.text(" ")
														.build())
										.add(button()
														.control(Control.builder()
																		.command(this::save)
																		.caption("Save")
																		.mnemonic('S')
																		.enabled(model.saveEnabled()))
														.build())
										.build())
						.build();
	}

	private JPanel createSourceDirectoryPanel() {
		JLabel sourceDirectoryLabel = label()
						.text("Source directory")
						.displayedMnemonic('D')
						.build();
		Control selectSourceDirectoryControl = Control.builder()
						.command(this::selectSourceDirectory)
						.caption("...")
						.build();

		return borderLayoutPanel()
						.centerComponent(gridLayoutPanel(2, 1)
										.add(sourceDirectoryLabel)
										.add(createSourceDirectoryField(sourceDirectoryLabel, selectSourceDirectoryControl))
										.build())
						.eastComponent(gridLayoutPanel(2, 1)
										.add(label()
														.text(" ")
														.build())
										.add(button()
														.control(selectSourceDirectoryControl)
														.build())
										.build())
						.build();
	}

	private JCheckBox createDtoCheckBox() {
		return checkBox()
						.link(model.includeDto())
						.text("Include DTOs")
						.mnemonic('D')
						.onBuild(checkBox -> KeyEvents.builder()
										.keyCode(KeyEvent.VK_D)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.modifiers(InputEvent.ALT_DOWN_MASK)
										.action(command(() -> checkBox.setSelected(!checkBox.isSelected())))
										.enable(this))
						.build();
	}

	private JTextField createPackageField(JLabel packageLabel) {
		return stringField()
						.link(model.domainPackage())
						.hint("(Alt-P)")
						.onBuild(field -> KeyEvents.builder()
										.keyCode(KeyEvent.VK_P)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.modifiers(InputEvent.ALT_DOWN_MASK)
										.action(command(field::requestFocusInWindow))
										.enable(this))
						.onBuild(packageLabel::setLabelFor)
						.build();
	}

	private JTextField createSourceDirectoryField(JLabel sourceDirectoryLabel,
																								Control selectSourceDirectoryControl) {
		return stringField()
						.link(model.sourceDirectory())
						.hint("(Alt-D / INSERT)")
						.editable(false)
						.keyEvent(KeyEvents.builder()
										.keyCode(KeyEvent.VK_INSERT)
										.action(selectSourceDirectoryControl))
						.onBuild(field -> KeyEvents.builder()
										.keyCode(KeyEvent.VK_D)
										.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.modifiers(InputEvent.ALT_DOWN_MASK)
										.action(command(field::requestFocusInWindow))
										.enable(this))
						.onBuild(sourceDirectoryLabel::setLabelFor)
						.build();
	}

	private void selectSourceDirectory() {
		model.sourceDirectory().set(Dialogs.select()
						.files()
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
						.centerComponent(scrollPane()
										.view(component)
										.build())
						.border(createCompoundBorder(createTitledBorder(title), createEmptyBorder()))
						.build();
	}

	private static Border createEmptyBorder() {
		return BorderFactory.createEmptyBorder(Layouts.GAP.getOrThrow(), Layouts.GAP.getOrThrow(),
						Layouts.GAP.getOrThrow(), Layouts.GAP.getOrThrow());
	}

	public void showFrame() {
		Frames.builder()
						.component(borderLayoutPanel()
										.centerComponent(this)
										.border(createEmptyBorder())
										.build())
						.title("Codion Domain Generator")
						.icon(Logos.logoTransparent())
						.menuBar(menu()
										.controls(createMainMenuControls())
										.buildMenuBar())
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
		Dialogs.progressWorker()
						.task(() -> model.populateSelected(schemaNotifier))
						.owner(this)
						.title("Populating")
						.northPanel(northPanel)
						.onResult(() -> model.entityModel().items().refresh())
						.execute();
	}

	private Controls createMainMenuControls() {
		return Controls.builder()
						.control(Controls.builder()
										.caption("File")
										.mnemonic('F')
										.control(Control.builder()
														.command(() -> System.exit(0))
														.caption("Exit")
														.mnemonic('X')))
						.control(Controls.builder()
										.caption("View")
										.mnemonic('V')
										.control(Dialogs.select()
														.lookAndFeel()
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
		model.schemaModel().items().refresher().exception().addConsumer(this::displayException);
		model.entityModel().items().refresher().exception().addConsumer(this::displayException);
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

	private void displayException(Exception e) {
		Dialogs.displayException(e, parentWindow(this));
	}

	private static List<FilterTableColumn<SchemaColumns.Id>> createSchemaColumns() {
		FilterTableColumn<SchemaColumns.Id> catalogColumn =
						FilterTableColumn.builder()
										.identifier(SchemaColumns.Id.CATALOG)
										.headerValue("Catalog")
										.build();
		FilterTableColumn<SchemaColumns.Id> schemaColumn =
						FilterTableColumn.builder()
										.identifier(SchemaColumns.Id.SCHEMA)
										.headerValue("Schema")
										.build();
		FilterTableColumn<SchemaColumns.Id> populatedColumn =
						FilterTableColumn.builder()
										.identifier(SchemaColumns.Id.POPULATED)
										.headerValue("Populated")
										.build();

		return asList(catalogColumn, schemaColumn, populatedColumn);
	}

	private static List<FilterTableColumn<EntityColumns.Id>> createEntityColumns() {
		FilterTableColumn<EntityColumns.Id> entityTypeColumn =
						FilterTableColumn.builder()
										.identifier(EntityColumns.Id.ENTITY)
										.headerValue("Entity")
										.build();
		FilterTableColumn<EntityColumns.Id> typeColumn =
						FilterTableColumn.builder()
										.identifier(EntityColumns.Id.TABLE_TYPE)
										.headerValue("Type")
										.preferredWidth(120)
										.build();

		return asList(entityTypeColumn, typeColumn);
	}

	private static SearchHighlighter searchHighlighter(JTextArea textArea) {
		return SearchHighlighter.builder()
						.component(textArea)
						.scrollYRatio(0.2)
						.scrollXRatio(0.5)
						.build();
	}

	private static void lookAndFeelSelected(LookAndFeelEnabler lookAndFeelEnabler) {
		UserPreferences.set(DomainGeneratorPanel.class.getName() + LOOK_AND_FEEL_PROPERTY,
						lookAndFeelEnabler.lookAndFeelInfo().getClassName());
	}

	/**
	 * Runs a DomainGeneratorPanel instance in a frame
	 * @param arguments no arguments required
	 */
	public static void main(String[] arguments) {
		enableLookAndFeel(DomainGeneratorPanel.class.getName() + LOOK_AND_FEEL_PROPERTY, FlatDarculaLaf.class);
		try {
			SwingUtilities.invokeLater(DomainGeneratorPanel::start);
		}
		catch (CancelException ignored) {
			System.exit(0);
		}
		catch (Exception e) {
			Dialogs.displayException(e, null);
			System.exit(1);
		}
	}

	private static void start() {
		Database database = Database.instance();
		if (USER_REQUIRED.getOrThrow()) {
			new DomainGeneratorPanel(domainGeneratorModel(database, Dialogs.login()
							.icon(Logos.logoTransparent())
							.defaultUser(DEFAULT_USER.optional()
											.map(User::parse)
											.orElse(null))
							.validator(user -> database.createConnection(user).close())
							.show())).showFrame();
		}
		else {
			new DomainGeneratorPanel(domainGeneratorModel(database)).showFrame();
		}
	}
}
