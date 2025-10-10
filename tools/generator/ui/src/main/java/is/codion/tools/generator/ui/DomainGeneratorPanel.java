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
import is.codion.framework.domain.db.SchemaDomain.SchemaSettings;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.common.ui.component.text.SearchHighlighter;
import is.codion.swing.common.ui.component.value.ComponentValue;
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
import is.codion.tools.generator.model.DomainGeneratorModel.PopulateTask;
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
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.stringValue;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.enableLookAndFeel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static is.codion.tools.generator.model.DomainGeneratorModel.domainGeneratorModel;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.JOptionPane.*;

public final class DomainGeneratorPanel extends JPanel {

	/**
	 * The user on the form username:password or just username in case no password is required.
	 * If a user is specified no login dialog is presented.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 */
	public static final PropertyValue<String> USER =
					stringValue("codion.domain.generator.user");

	/**
	 * The default user credentials to present in the login dialog, on the form username:password or just username.
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
	private final FilterTable<SchemaRow, String> schemaTable;
	private final FilterTable<EntityRow, String> entityTable;
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
						.north(createSourceDirectoryPanel())
						.center(borderLayoutPanel()
										.center(splitPane()
														.orientation(JSplitPane.VERTICAL_SPLIT)
														.resizeWeight(RESIZE_WEIGHT)
														.topComponent(createScrollablePanel(schemaTable, "Schemas (Alt-1)"))
														.bottomComponent(createScrollablePanel(entityTable, "Entities (Alt-2)"))))
						.build();

		JPanel sourcePackagePanel = borderLayoutPanel()
						.north(createPackageSavePanel())
						.center(sourceTabbedPane)
						.build();

		JSplitPane splitPane = splitPane()
						.resizeWeight(RESIZE_WEIGHT)
						.leftComponent(schemaSourceDirPanel)
						.rightComponent(sourcePackagePanel)
						.build();

		setLayout(borderLayout());
		add(splitPane, BorderLayout.CENTER);
	}

	private FilterTable<SchemaRow, String> createSchemaTable() {
		Control populateSchemaControl = Control.builder()
						.command(this::populateSchema)
						.caption("Populate")
						.enabled(model.schemaModel().selection().empty().not())
						.build();
		Control schemaSettingsControl = Control.builder()
						.command(this::schemaSettings)
						.caption("Settings...")
						.enabled(model.schemaModel().selection().empty().not())
						.build();

		return FilterTable.builder()
						.model(model.schemaModel())
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.doubleClick(populateSchemaControl)
						.columnReordering(false)
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_ENTER)
										.modifiers(MENU_SHORTCUT_MASK)
										.action(populateSchemaControl))
						.popupMenuControls(table -> Controls.builder()
										.control(populateSchemaControl)
										.control(schemaSettingsControl)
										.build())
						.build();
	}

	private FilterTable<EntityRow, String> createEntityTable() {
		return FilterTable.builder()
						.model(model.entityModel())
						.columns(column -> {
							switch (column.identifier()) {
								case EntityColumns.TABLE_TYPE:
									column.preferredWidth(120);
									break;
								case EntityColumns.DTO:
									column.fixedWidth(80);
									break;
							}
						})
						.columnReordering(false)
						.cellEditor(EntityColumns.DTO, FilterTableCellEditor.builder()
										.component(checkBox()::buildValue)
										.build())
						.hiddenColumns(EntityColumns.DTO)
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.popupMenuControl(FilterTable::createToggleAutoResizeModeControls)
						.build();
	}

	private JSplitPane createApiImplPanel() {
		return splitPane()
						.orientation(JSplitPane.VERTICAL_SPLIT)
						.resizeWeight(0.5)
						.topComponent(borderLayoutPanel()
										.center(createScrollablePanel(apiTextArea, "API (Alt-3)"))
										.south(createSearchCopyPanel(apiTextArea)))
						.bottomComponent(borderLayoutPanel()
										.center(createScrollablePanel(implementationTextArea, "Implementation (Alt-4)"))
										.south(createSearchCopyPanel(implementationTextArea)))
						.continuousLayout(true)
						.oneTouchExpandable(true)
						.build();
	}

	private JPanel createCombinedPanel() {
		return borderLayoutPanel()
						.center(createScrollablePanel(combinedTextArea, "Combined (Alt-5)"))
						.south(createSearchCopyPanel(combinedTextArea))
						.build();
	}

	private static JPanel createSearchCopyPanel(JTextArea textArea) {
		return borderLayoutPanel()
						.east(button()
										.control(createCopyControl(textArea)))
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
						.font(font -> new Font(Font.MONOSPACED, font.getStyle(), font.getSize()))
						.caretUpdatePolicy(DefaultCaret.NEVER_UPDATE)
						.build();
	}

	private JTabbedPane createSourceTabbedPane() {
		return tabbedPane()
						.tab("API/Impl")
						.component(createApiImplPanel())
						.mnemonic('A')
						.add()
						.tab("Combined")
						.component(createCombinedPanel())
						.mnemonic('C')
						.add()
						.build();
	}

	private JPanel createPackageSavePanel() {
		JLabel packageLabel = label("Package")
						.displayedMnemonic('P')
						.build();

		return borderLayoutPanel()
						.center(borderLayoutPanel()
										.center(gridLayoutPanel(2, 1)
														.add(packageLabel)
														.add(createPackageField(packageLabel)))
										.east(gridLayoutPanel(2, 2)
														.add(label(" "))
														.add(label(" "))
														.add(createDtoCheckBox())
														.add(createI18nCheckBox())))
						.east(gridLayoutPanel(2, 1)
										.add(label(" "))
										.add(button()
														.control(Control.builder()
																		.command(this::save)
																		.caption("Save")
																		.mnemonic('S')
																		.enabled(model.saveEnabled()))))
						.build();
	}

	private JPanel createSourceDirectoryPanel() {
		JLabel sourceDirectoryLabel = label("Source directory")
						.displayedMnemonic('D')
						.build();
		Control selectSourceDirectoryControl = Control.builder()
						.command(this::selectSourceDirectory)
						.caption("...")
						.build();

		return borderLayoutPanel()
						.center(gridLayoutPanel(2, 1)
										.add(sourceDirectoryLabel)
										.add(createSourceDirectoryField(sourceDirectoryLabel, selectSourceDirectoryControl)))
						.east(gridLayoutPanel(2, 1)
										.add(label(" "))
										.add(button()
														.control(selectSourceDirectoryControl)))
						.build();
	}

	private JCheckBox createDtoCheckBox() {
		return checkBox()
						.link(model.dtos())
						.text("DTOs")
						.mnemonic('T')
						.build();
	}

	private JCheckBox createI18nCheckBox() {
		return checkBox()
						.link(model.i18n())
						.text("i18n")
						.mnemonic('I')
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
						.startDirectory(DomainGeneratorModel.SOURCE_DIRECTORY.get())
						.selectDirectory()
						.getAbsolutePath());
	}

	private void save() throws IOException {
		if (sourceTabbedPane.getSelectedIndex() == 0) {
			saveApiImpl();
		}
		else {
			saveCombined();
		}
	}

	private void saveApiImpl() throws IOException {
		if (showConfirmDialog(this, "Save API and Impl files to source directory?",
						"Confirm save", YES_NO_OPTION) == YES_OPTION &&
						model.saveApiImpl(this::confirmOverwrite)) {
			showMessageDialog(this, "Files saved");
		}
	}

	private void saveCombined() throws IOException {
		if (showConfirmDialog(this, "Save combined API and Impl file to source directory?",
						"Confirm save", YES_NO_OPTION) == YES_OPTION &&
						model.saveCombined(this::confirmOverwrite)) {
			showMessageDialog(this, "File saved");
		}
	}

	private boolean confirmOverwrite() {
		return showConfirmDialog(DomainGeneratorPanel.this, "Overwrite existing file(s)?", "Confirm overwrite", YES_NO_OPTION) == YES_OPTION;
	}

	private static JPanel createScrollablePanel(JComponent component, String title) {
		return borderLayoutPanel()
						.center(scrollPane()
										.view(component))
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
										.center(this)
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
		PopulateTask task = model.populate();
		Dialogs.progressWorker()
						.task(task)
						.owner(this)
						.title("Populating")
						.stringPainted(true)
						.control(Control.builder()
										.toggle(task.cancelled())
										.caption("Cancel")
										.enabled(task.cancelled().not()))
						.maximum(task.maximum())
						.onResult(task::finish)
						.execute();
	}

	private void schemaSettings() {
		model.schemaModel().selection().item().optional().ifPresent(schema -> {
			SchemaSettingsPanel settingsPanel = new SchemaSettingsPanel(schema.schemaSettings());
			Dialogs.okCancel()
							.component(settingsPanel)
							.owner(schemaTable)
							.title("Schema Settings")
							.onOk(() -> model.setSchemaSettings(settingsPanel.settings()))
							.show();
		});
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
		model.dtos().addConsumer(entityTable.columnModel().visible(EntityColumns.DTO)::set);
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
			new DomainGeneratorPanel(domainGeneratorModel(database, user(database))).showFrame();
		}
		else {
			new DomainGeneratorPanel(domainGeneratorModel(database)).showFrame();
		}
	}

	private static User user(Database database) {
		return USER.optional()
						.map(User::parse)
						.orElseGet(Dialogs.login()
										.icon(Logos.logoTransparent())
										.defaultUser(DEFAULT_USER.optional()
														.map(User::parse)
														.orElse(null))
										.validator(user -> database.createConnection(user).close())
										::show);
	}

	private static final class SchemaSettingsPanel extends JPanel {

		private final ComponentValue<JTextField, String> primaryKeySuffix;
		private final ComponentValue<JTextField, String> viewSuffix;
		private final ComponentValue<JCheckBox, Boolean> hideAuditColumns;
		private final ComponentValue<JTextField, String> auditColumnNames;

		private SchemaSettingsPanel(SchemaSettings schemaSettings) {
			super(flexibleGridLayout(0, 2));
			setBorder(emptyBorder());
			primaryKeySuffix = stringField()
							.value(schemaSettings.primaryKeyColumnSuffix())
							.columns(5)
							.buildValue();
			viewSuffix = stringField()
							.value(schemaSettings.viewSuffix())
							.columns(5)
							.buildValue();
			hideAuditColumns = checkBox()
							.value(schemaSettings.hideAuditColumns())
							.buildValue();
			auditColumnNames = stringField()
							.value(schemaSettings.auditColumnNames().stream()
											.collect(joining(", ")))
							.buildValue();
			add(label("Primary key suffix").build());
			add(primaryKeySuffix.component());
			add(label("View suffix").build());
			add(viewSuffix.component());
			add(label("Hide audit columns").build());
			add(hideAuditColumns.component());
			add(label("Audit column names").build());
			add(auditColumnNames.component());
		}

		private SchemaSettings settings() {
			return SchemaSettings.builder()
							.primaryKeyColumnSuffix(primaryKeySuffix.optional().orElse(""))
							.viewSuffix(viewSuffix.optional().orElse(""))
							.hideAuditColumns(hideAuditColumns.getOrThrow())
							.auditColumnNames(auditColumnNames.optional().orElse(""))
							.build();
		}
	}
}
