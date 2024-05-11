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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.i18n.Messages;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnSummaryModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.property.PropertyValue;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityEditModel.Delete;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableSelectionModel;
import is.codion.swing.common.ui.Cursors;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState;
import is.codion.swing.common.ui.component.table.FilterColumnConditionPanel;
import is.codion.swing.common.ui.component.table.FilterColumnConditionPanel.FieldFactory;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.component.table.FilterTableConditionPanel;
import is.codion.swing.common.ui.component.table.TableConditionPanel;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.KeyboardShortcuts;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityEditPanel.Confirmer;
import is.codion.swing.framework.ui.component.DefaultEntityComponentFactory;
import is.codion.swing.framework.ui.component.EntityComponentFactory;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static is.codion.common.Text.nullOrEmpty;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.common.value.ValueSet.valueSet;
import static is.codion.swing.common.ui.Utilities.*;
import static is.codion.swing.common.ui.component.Components.menu;
import static is.codion.swing.common.ui.component.Components.toolBar;
import static is.codion.swing.common.ui.component.table.ColumnSummaryPanel.columnSummaryPanel;
import static is.codion.swing.common.ui.component.table.FilterColumnConditionPanel.filterColumnConditionPanel;
import static is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel.filterTableColumnComponentPanel;
import static is.codion.swing.common.ui.component.table.FilterTableConditionPanel.filterTableConditionPanel;
import static is.codion.swing.common.ui.dialog.Dialogs.progressWorkerDialog;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static is.codion.swing.framework.ui.ColumnPreferences.columnPreferences;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.displayDependenciesDialog;
import static is.codion.swing.framework.ui.EntityDialogs.addEntityDialog;
import static is.codion.swing.framework.ui.EntityDialogs.editEntityDialog;
import static is.codion.swing.framework.ui.EntityTableColumns.entityTableColumns;
import static is.codion.swing.framework.ui.EntityTablePanel.EntityTablePanelControl.*;
import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.*;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

/**
 * The EntityTablePanel is a UI class based on the EntityTableModel class.
 * It consists of a JTable as well as filtering/searching and summary panels.
 * The default layout is as follows
 * <pre>
 *  ____________________________________________________
 * |                conditionPanel                      |
 * |____________________________________________________|
 * |                                                    |
 * |                                                    |
 * |                                                    |
 * |                                                    |
 * |                entityTable (FilterTable)         |
 * |                                                    |
 * |                                                    |
 * |                                                    |
 * |                                                    |
 * |____________________________________________________|
 * |                summaryPanel                        |
 * |____________________________________________________|
 * |                southPanel                          |
 * |____________________________________________________|
 * </pre>
 * The condition and summary panels can be hidden
 * Note that {@link #initialize()} must be called to initialize this panel before displaying it.
 * @see EntityTableModel
 */
public class EntityTablePanel extends JPanel {

	private static final Logger LOG = LoggerFactory.getLogger(EntityTablePanel.class);

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTablePanel.class, getBundle(EntityTablePanel.class.getName()));
	private static final MessageBundle EDIT_PANEL_MESSAGES =
					messageBundle(EntityEditPanel.class, getBundle(EntityEditPanel.class.getName()));
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	private static final String COLUMN_PREFERENCES = "-columns";
	private static final String CONDITIONS_PREFERENCES = "-conditions";

	/**
	 * The standard controls available
	 */
	public enum EntityTablePanelControl implements KeyboardShortcuts.Shortcut {
		/**
		 * Add a new entity instance.<br>
		 * Default key stroke: INSERT
		 * @see EntityTablePanel#EntityTablePanel(SwingEntityTableModel, EntityEditPanel)
		 * @see EntityTablePanel#EntityTablePanel(SwingEntityTableModel, EntityEditPanel, Consumer)
		 */
		ADD(keyStroke(VK_INSERT)),
		/**
		 * Edit the selected entity instance.<br>
		 * Default key stroke: CTRL-INSERT
		 * @see EntityTablePanel#EntityTablePanel(SwingEntityTableModel, EntityEditPanel)
		 * @see EntityTablePanel#EntityTablePanel(SwingEntityTableModel, EntityEditPanel, Consumer)
		 */
		EDIT(keyStroke(VK_INSERT, CTRL_DOWN_MASK)),
		/**
		 * Select and edit a single attribute value for the selected entity instances.<br>
		 * Default key stroke: SHIFT-INSERT
		 * @see Config#editAttributeSelection(EditAttributeSelection)
		 */
		EDIT_SELECTED_ATTRIBUTE(keyStroke(VK_INSERT, SHIFT_DOWN_MASK)),
		/**
		 * Requests focus for the table.<br>
		 * Default key stroke: CTRL-T
		 */
		REQUEST_TABLE_FOCUS(keyStroke(VK_T, CTRL_DOWN_MASK)),
		/**
		 * Toggles the condition panel between hidden, visible and advanced.<br>
		 * Default key stroke: CTRL-ALT-S
		 * @see TableConditionPanel#state()
		 */
		TOGGLE_CONDITION_PANEL(keyStroke(VK_S, CTRL_DOWN_MASK | ALT_DOWN_MASK)),
		/**
		 * Displays a dialog for selecting a column condition panel.<br>
		 * Default key stroke: CTRL-S
		 */
		SELECT_CONDITION_PANEL(keyStroke(VK_S, CTRL_DOWN_MASK)),
		/**
		 * Toggles the filter panel between hidden, visible and advanced.<br>
		 * Default key stroke: CTRL-ALT-F
		 * @see TableConditionPanel#state()
		 */
		TOGGLE_FILTER_PANEL(keyStroke(VK_F, CTRL_DOWN_MASK | ALT_DOWN_MASK)),
		/**
		 * Displays a dialog for selecting a column filter panel.<br>
		 * Default key stroke: CTRL-SHIFT-F
		 */
		SELECT_FILTER_PANEL(keyStroke(VK_F, CTRL_DOWN_MASK | SHIFT_DOWN_MASK)),
		/**
		 * Moves the selection up.<br>
		 * Default key stroke: ALT-SHIFT-UP
		 */
		MOVE_SELECTION_UP(keyStroke(VK_UP, ALT_DOWN_MASK | SHIFT_DOWN_MASK)),
		/**
		 * Moves the selection down.<br>
		 * Default key stroke: ALT-SHIFT-DOWN
		 */
		MOVE_SELECTION_DOWN(keyStroke(VK_DOWN, ALT_DOWN_MASK | SHIFT_DOWN_MASK)),
		/**
		 * The main print action<br>
		 * Default key stroke: CTRL-P
		 */
		PRINT(keyStroke(VK_P, CTRL_DOWN_MASK)),
		/**
		 * Triggers the {@link EntityTablePanelControl#DELETE} control.<br>
		 * Default key stroke: DELETE
		 */
		DELETE(keyStroke(VK_DELETE)),
		/**
		 * Displays the table popup menu, if one is available.<br>
		 * Default key stroke: CTRL-G
		 */
		DISPLAY_POPUP_MENU(keyStroke(VK_G, CTRL_DOWN_MASK)),
		/**
		 * Displays the entity menu, if one is available.<br>
		 * Default key stroke: CTRL-ALT-V
		 */
		DISPLAY_ENTITY_MENU(keyStroke(VK_V, CTRL_DOWN_MASK | ALT_DOWN_MASK)),
		/**
		 * A {@link Controls} instance containing controls for printing.
		 */
		PRINT_CONTROLS,
		/**
		 * A {@link Controls} instance containing any additional popup menu controls.
		 * @see #addPopupMenuControls(Controls)
		 */
		ADDITIONAL_POPUP_MENU_CONTROLS,
		/**
		 * A {@link Controls} instance containing any additional toolbar controls.
		 * @see #addToolBarControls(Controls)
		 */
		ADDITIONAL_TOOLBAR_CONTROLS,
		/**
		 * A {@link Control} for viewing the dependencies of the selected entities.
		 */
		VIEW_DEPENDENCIES,
		/**
		 * A {@link Controls} instance containing edit controls for all editable attributes.
		 * @see Config#editAttributeSelection(EditAttributeSelection)
		 */
		EDIT_ATTRIBUTE_CONTROLS,
		/**
		 * Either a {@link Control} for displaying a dialog for selecting the visible table columns
		 * or a {@link Controls} instance containing a {@link ToggleControl} for each columns visibility.
		 * @see Config#columnSelection(ColumnSelection)
		 */
		SELECT_COLUMNS,
		/**
		 * A {@link Control} for resetting the columns to their original visibility and location.
		 */
		RESET_COLUMNS,
		/**
		 * A {@link Control} for displaying a dialog for configuring the column auto-resize-mode.
		 * @see JTable#setAutoResizeMode(int)
		 */
		COLUMN_AUTO_RESIZE_MODE,
		/**
		 * A {@link Control} for toggling between single and multi selection mode.
		 */
		SELECTION_MODE,
		/**
		 * A {@link Control} for clearing the data from the table.
		 * @see SwingEntityTableModel#clear()
		 */
		CLEAR,
		/**
		 * A {@link Control} for refreshing the table data.<br>
		 * Default key stroke: ALT-R
		 * @see SwingEntityTableModel#refresh()
		 */
		REFRESH(keyStroke(VK_R, ALT_DOWN_MASK)),
		/**
		 * A {@link ToggleControl} for showing/hiding the summary panel.
		 */
		TOGGLE_SUMMARY_PANEL,
		/**
		 * A {@link Controls} instance containing the condition panel controls.
		 */
		CONDITION_CONTROLS,
		/**
		 * A {@link Controls} instance containing the filter panel controls.
		 */
		FILTER_CONTROLS,
		/**
		 * A {@link Control} for clearing the table selection.
		 */
		CLEAR_SELECTION,
		/**
		 * A {@link Controls} instance containing controls for copying either cell or table data.
		 * @see #COPY_CELL
		 * @see #COPY_ROWS
		 */
		COPY_CONTROLS,
		/**
		 * A {@link Control} for copying the selected cell data.<br>
		 * Default key stroke: CTRL-ALT-C
		 */
		COPY_CELL(keyStroke(VK_C, CTRL_DOWN_MASK | ALT_DOWN_MASK)),
		/**
		 * A {@link Control} for copying the table rows with header.
		 */
		COPY_ROWS,
		/**
		 * A {@link Controls} instance containing controls for configuring columns.
		 * @see #SELECT_COLUMNS
		 * @see #RESET_COLUMNS
		 * @see #COLUMN_AUTO_RESIZE_MODE
		 */
		COLUMN_CONTROLS,
		/**
		 * Requests focus for the table search field.<br>
		 * Default key stroke: CTRL-F
		 */
		REQUEST_SEARCH_FIELD_FOCUS(keyStroke(VK_F, CTRL_DOWN_MASK));

		private final KeyStroke defaultKeystroke;

		EntityTablePanelControl() {
			this(null);
		}

		EntityTablePanelControl(KeyStroke defaultKeystroke) {
			this.defaultKeystroke = defaultKeystroke;
		}

		@Override
		public Optional<KeyStroke> defaultKeystroke() {
			return Optional.ofNullable(defaultKeystroke);
		}
	}

	/**
	 * Specifies the refresh button visibility.
	 */
	public enum RefreshButtonVisible {
		/**
		 * Refresh button should always be visible
		 */
		ALWAYS,
		/**
		 * Refresh button should only be visible when the table condition panel is visible
		 */
		WHEN_CONDITION_PANEL_IS_VISIBLE
	}

	/**
	 * Specifies how column selection is presented.
	 */
	public enum ColumnSelection {
		/**
		 * Display a dialog.
		 */
		DIALOG,
		/**
		 * Display toggle controls directly in the menu.
		 */
		MENU
	}

	/**
	 * Specifies how attribute selection is presented for editing the selected records.
	 */
	public enum EditAttributeSelection {
		/**
		 * Display a dialog.
		 */
		DIALOG,
		/**
		 * Display an item for each attribute in a submenu.
		 */
		MENU
	}

	private static final int FONT_SIZE_TO_ROW_HEIGHT = 4;
	private static final Consumer<Config> NO_CONFIGURATION = c -> {};

	private final State summaryPanelVisibleState = State.state(Config.SUMMARY_PANEL_VISIBLE.get());

	private final State orderQueryBySortOrder = State.state(Config.ORDER_QUERY_BY_SORT_ORDER.get());
	private final State queryHiddenColumns = State.state(Config.QUERY_HIDDEN_COLUMNS.get());

	private final FilterTable<Entity, Attribute<?>> table;
	private final JScrollPane tableScrollPane = new JScrollPane();
	private final EntityEditPanel editPanel;
	private final Map<EntityTablePanelControl, Value<Control>> controls;
	private final Controls.Config<EntityTablePanelControl> popupMenuConfiguration;
	private final Controls.Config<EntityTablePanelControl> toolBarConfiguration;
	private final SwingEntityTableModel tableModel;
	private final Control conditionRefreshControl;
	private final JToolBar refreshButtonToolBar;
	private final List<Controls> additionalPopupControls = new ArrayList<>();
	private final List<Controls> additionalToolBarControls = new ArrayList<>();

	private StatusPanel statusPanel;
	private TableConditionPanel<Attribute<?>> conditionPanel;
	private JScrollPane conditionPanelScrollPane;
	private JScrollPane filterPanelScrollPane;
	private FilterTableColumnComponentPanel<Attribute<?>> summaryPanel;
	private JScrollPane summaryPanelScrollPane;
	private TablePanel tablePanel;

	final Config configuration;

	private boolean initialized = false;

	/**
	 * Initializes a new EntityTablePanel instance
	 * @param tableModel the SwingEntityTableModel instance
	 */
	public EntityTablePanel(SwingEntityTableModel tableModel) {
		this(tableModel, NO_CONFIGURATION);
	}

	/**
	 * Initializes a new EntityTablePanel instance
	 * @param tableModel the SwingEntityTableModel instance
	 * @param config provides access to the table panel configuration
	 */
	public EntityTablePanel(SwingEntityTableModel tableModel, Consumer<Config> config) {
		this.tableModel = requireNonNull(tableModel, "tableModel");
		this.editPanel = null;
		this.conditionRefreshControl = createConditionRefreshControl();
		this.configuration = configure(config);
		this.table = configuration.tableBuilder.build();
		this.controls = createControls();
		this.refreshButtonToolBar = createRefreshButtonToolBar();
		this.popupMenuConfiguration = createPopupMenuConfiguration();
		this.toolBarConfiguration = createToolBarConfiguration();
		bindTableEvents();
		applyPreferences();
	}

	/**
	 * Initializes a new EntityTablePanel instance
	 * @param tableModel the SwingEntityTableModel instance
	 * @param editPanel the edit panel
	 */
	public EntityTablePanel(SwingEntityTableModel tableModel, EntityEditPanel editPanel) {
		this(tableModel, editPanel, NO_CONFIGURATION);
	}

	/**
	 * Initializes a new EntityTablePanel instance
	 * @param tableModel the SwingEntityTableModel instance
	 * @param editPanel the edit panel
	 * @param config provides access to the table panel configuration
	 */
	public EntityTablePanel(SwingEntityTableModel tableModel, EntityEditPanel editPanel, Consumer<Config> config) {
		this.tableModel = requireNonNull(tableModel, "tableModel");
		this.editPanel = validateEditModel(requireNonNull(editPanel, "editPanel"));
		this.conditionRefreshControl = createConditionRefreshControl();
		this.configuration = configure(config);
		this.table = configuration.tableBuilder.build();
		this.controls = createControls();
		this.refreshButtonToolBar = createRefreshButtonToolBar();
		this.popupMenuConfiguration = createPopupMenuConfiguration();
		this.toolBarConfiguration = createToolBarConfiguration();
		bindTableEvents();
		applyPreferences();
	}

	/**
	 * @return the table
	 */
	public final FilterTable<Entity, Attribute<?>> table() {
		if (table == null) {
			throw new IllegalStateException("The table is not initialized until after configuration has finished");
		}

		return table;
	}

	/**
	 * @param <T> the table model type
	 * @return the EntityTableModel used by this EntityTablePanel
	 */
	public final <T extends SwingEntityTableModel> T tableModel() {
		return (T) tableModel;
	}

	/**
	 * @param <T> the condition panel type
	 * @return the condition panel
	 * @throws IllegalStateException in case a condition panel is not available
	 * @see Config#includeConditionPanel(boolean)
	 */
	public final <T extends TableConditionPanel<Attribute<?>>> T conditionPanel() {
		if (!configuration.includeConditionPanel) {
			throw new IllegalStateException("No condition panel is available");
		}
		if (conditionPanel == null) {
			initializeConditionPanel();
		}

		return (T) conditionPanel;
	}

	/**
	 * @return the state controlling whether the summary panel is visible
	 */
	public final State summaryPanelVisible() {
		return summaryPanelVisibleState;
	}

	/**
	 * Allows the user to select one of the available search condition panels
	 */
	public final void selectConditionPanel() {
		if (configuration.includeConditionPanel) {
			selectConditionPanel(conditionPanel, conditionPanel.state(),
							table, FrameworkMessages.selectSearchField(), tableModel.entityDefinition());
		}
	}

	/**
	 * Allows the user to select one of the available filter condition panels
	 */
	public final void selectFilterPanel() {
		if (configuration.includeFilterPanel) {
			selectConditionPanel((TableConditionPanel<Attribute<?>>) table.filterPanel(), table.filterPanel().state(),
							table, FrameworkMessages.selectFilterField(), tableModel.entityDefinition());
		}
	}

	/**
	 * Specifies whether the current sort order is used as a basis for the query order by clause.
	 * Note that this only applies to column attributes.
	 * @return the State controlling whether the current sort order should be used as a basis for the query order by clause
	 */
	public final State orderQueryBySortOrder() {
		return orderQueryBySortOrder;
	}

	/**
	 * Returns whether the values of hidden columns are included when querying data
	 * @return the State controlling whether the values of hidden columns are included when querying data
	 */
	public final State queryHiddenColumns() {
		return queryHiddenColumns;
	}

	/**
	 * @param additionalPopupMenuControls a set of controls to add to the table popup menu
	 * @throws IllegalStateException in case this panel has already been initialized
	 */
	public void addPopupMenuControls(Controls additionalPopupMenuControls) {
		throwIfInitialized();
		this.additionalPopupControls.add(requireNonNull(additionalPopupMenuControls));
	}

	/**
	 * @param additionalToolBarControls a set of controls to add to the table toolbar menu
	 * @throws IllegalStateException in case this panel has already been initialized
	 */
	public void addToolBarControls(Controls additionalToolBarControls) {
		throwIfInitialized();
		this.additionalToolBarControls.add(requireNonNull(additionalToolBarControls));
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + ": " + tableModel.entityType();
	}

	/**
	 * Returns a {@link Value} containing the control associated with {@code control},
	 * an empty {@link Value} if no such control is available.
	 * Note that standard controls are populated during initialization, so until then, these values may be empty.
	 * @param control the control
	 * @return the {@link Value} containing the control associated with {@code control}
	 */
	public final Value<Control> control(EntityTablePanelControl control) {
		return controls.get(requireNonNull(control));
	}

	/**
	 * Displays a selection dialog for selecting an attribute to edit and
	 * retrieves a new value via input dialog and performs an update on the selected entities
	 * assigning the value to the attribute
	 * @see Config#editComponentFactory(Attribute, EntityComponentFactory)
	 */
	public final void editSelected() {
		List<AttributeDefinition<?>> sortedDefinitions = configuration.editable.get().stream()
						.map(attribute -> tableModel.entityDefinition().attributes().definition(attribute))
						.sorted(AttributeDefinition.definitionComparator())
						.collect(toList());
		Dialogs.selectionDialog(sortedDefinitions)
						.owner(this)
						.selectSingle()
						.map(AttributeDefinition::attribute)
						.ifPresent(this::editSelected);
	}

	/**
	 * Retrieves a new value via input dialog and performs an update on the selected entities
	 * assigning the value to the attribute
	 * @param attributeToEdit the attribute which value to edit
	 * @see Config#editComponentFactory(Attribute, EntityComponentFactory)
	 */
	public final void editSelected(Attribute<?> attributeToEdit) {
		requireNonNull(attributeToEdit);
		if (!tableModel.selectionModel().isSelectionEmpty()) {
			editDialogBuilder(attributeToEdit)
							.edit(tableModel.selectionModel().getSelectedItems());
		}
	}

	/**
	 * Displays a dialog containing tables of entities depending on the selected entities via non-soft foreign keys
	 */
	public final void viewDependencies() {
		if (!tableModel.selectionModel().isSelectionEmpty()) {
			displayDependenciesDialog(tableModel.selectionModel().getSelectedItems(), tableModel.connectionProvider(), this);
		}
	}

	/**
	 * Deletes the entities selected in the underlying table model after asking for confirmation using
	 * the confirmer specified via {@link Config#deleteConfirmer(Confirmer)}
	 * @return true if the delete operation was successful
	 * @see Config#deleteConfirmer(Confirmer)
	 */
	public final boolean deleteSelectedWithConfirmation() {
		if (confirmDelete()) {
			return deleteSelected();
		}

		return false;
	}

	/**
	 * Deletes the entities selected in the underlying table model without asking for confirmation.
	 * @return true if the delete operation was successful
	 */
	public final boolean deleteSelected() {
		try {
			tableModel.deleteSelected();

			return true;
		}
		catch (ReferentialIntegrityException e) {
			LOG.debug(e.getMessage(), e);
			onException(e);
		}
		catch (Exception e) {
			LOG.error(e.getMessage(), e);
			onException(e);
		}

		return false;
	}

	/**
	 * Returns the key used to identify user preferences for this table panel, that is column positions, widths and such.
	 * The default implementation is:
	 * <pre>
	 * {@code
	 * return tableModel().getClass().getSimpleName() + "-" + entityType();
	 * }
	 * </pre>
	 * Override in case this key is not unique.
	 * @return the key used to identify user preferences for this table model
	 */
	public String userPreferencesKey() {
		return tableModel.getClass().getSimpleName() + "-" + tableModel.entityType();
	}

	/**
	 * Saves user preferences
	 * @see #userPreferencesKey()
	 * @see EntityPanel.Config#USE_CLIENT_PREFERENCES
	 */
	public void savePreferences() {
		if (EntityPanel.Config.USE_CLIENT_PREFERENCES.get()) {
			try {
				UserPreferences.setUserPreference(userPreferencesKey() + COLUMN_PREFERENCES,
								ColumnPreferences.toString(createColumnPreferences()));
			}
			catch (Exception e) {
				LOG.error("Error while saving column preferences", e);
			}
			try {
				UserPreferences.setUserPreference(userPreferencesKey() + CONDITIONS_PREFERENCES,
								ConditionPreferences.toString(createConditionPreferences()));
			}
			catch (Exception e) {
				LOG.error("Error while saving condition preferences", e);
			}
		}
	}

	/**
	 * Initializes the UI, while presenting a wait cursor to the user.
	 * Note that calling this method more than once has no effect.
	 * @return this EntityTablePanel instance
	 */
	public final EntityTablePanel initialize() {
		if (!initialized) {
			try {
				setupComponents();
				setupControls();
				setupStandardControls();
				addTablePopupMenu();
				layoutPanel(tablePanel, configuration.includeSouthPanel ? initializeSouthPanel() : null);
				setSummaryPanelVisible(summaryPanelVisibleState.get());
				bindEvents();
				setupKeyboardActions();
				updateComponentTreeUI(this);
			}
			finally {
				initialized = true;
			}
		}

		return this;
	}

	/**
	 * Override to setup any custom controls. This default implementation is empty.
	 * This method is called after all standard controls have been initialized.
	 * @see #control(EntityTablePanelControl)
	 */
	protected void setupControls() {}

	/**
	 * Initializes the south panel, override and return null for no south panel.
	 * Not called if the south panel has been disabled via {@link Config#includeSouthPanel(boolean)}.
	 * @return the south panel, or null if no south panel should be included
	 * @see Config#includeSouthPanel(boolean)
	 */
	protected JPanel initializeSouthPanel() {
		return new SouthPanel();
	}

	protected void setupKeyboardActions() {
		configuration.shortcuts.keyStroke(REFRESH).optional().ifPresent(keyStroke ->
						control(REFRESH).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
														.action(control)
														.enable(this)));
		configuration.shortcuts.keyStroke(REQUEST_TABLE_FOCUS).optional().ifPresent(keyStroke ->
						control(REQUEST_TABLE_FOCUS).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
														.action(control)
														.enable(this)));
		configuration.shortcuts.keyStroke(SELECT_CONDITION_PANEL).optional().ifPresent(keyStroke ->
						control(SELECT_CONDITION_PANEL).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
														.action(control)
														.enable(this)));
		configuration.shortcuts.keyStroke(TOGGLE_CONDITION_PANEL).optional().ifPresent(keyStroke ->
						control(TOGGLE_CONDITION_PANEL).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
														.action(control)
														.enable(this)));
		configuration.shortcuts.keyStroke(TOGGLE_FILTER_PANEL).optional().ifPresent(keyStroke ->
						control(TOGGLE_FILTER_PANEL).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
														.action(control)
														.enable(this)));
		configuration.shortcuts.keyStroke(SELECT_FILTER_PANEL).optional().ifPresent(keyStroke ->
						control(SELECT_FILTER_PANEL).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
														.action(control)
														.enable(this)));
		configuration.shortcuts.keyStroke(PRINT).optional().ifPresent(keyStroke ->
						control(PRINT).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
														.action(control)
														.enable(this)));
		configuration.shortcuts.keyStroke(ADD).optional().ifPresent(keyStroke ->
						control(ADD).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.action(control)
														.enable(table)));
		configuration.shortcuts.keyStroke(EDIT).optional().ifPresent(keyStroke ->
						control(EDIT).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.action(control)
														.enable(table)));
		configuration.shortcuts.keyStroke(EDIT_SELECTED_ATTRIBUTE).optional().ifPresent(keyStroke ->
						control(EDIT_SELECTED_ATTRIBUTE).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.action(control)
														.enable(table)));
		configuration.shortcuts.keyStroke(DELETE).optional().ifPresent(keyStroke ->
						control(DELETE).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.action(control)
														.enable(table)));
		configuration.shortcuts.keyStroke(MOVE_SELECTION_UP).optional().ifPresent(keyStroke ->
						control(MOVE_SELECTION_UP).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.action(control)
														.enable(table)));
		configuration.shortcuts.keyStroke(MOVE_SELECTION_DOWN).optional().ifPresent(keyStroke ->
						control(MOVE_SELECTION_DOWN).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.action(control)
														.enable(table)));
		configuration.shortcuts.keyStroke(DISPLAY_ENTITY_MENU).optional().ifPresent(keyStroke ->
						control(DISPLAY_ENTITY_MENU).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.action(control)
														.enable(table)));
		configuration.shortcuts.keyStroke(DISPLAY_POPUP_MENU).optional().ifPresent(keyStroke ->
						control(DISPLAY_POPUP_MENU).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.action(control)
														.enable(table)));
	}

	/**
	 * Configures the toolbar controls.<br>
	 * Note that the {@link Controls.Config} instance has pre-configured defaults,
	 * which must be cleared in order to start with an empty configuration.
	 * <pre>
	 *   configureToolBar(config -> config.clear()
	 *           .standard(EntityTablePanelControl.REFRESH)
	 *           .separator()
	 *           .control(createCustomControl())
	 *           .separator()
	 *           .defaults())
	 * </pre>
	 * Defaults:
	 * <ul>
	 *   <li>{@link EntityTablePanelControl#TOGGLE_SUMMARY_PANEL EntityTablePanelControl#TOGGLE_SUMMARY_PANEL}</li>
	 * 	 <li>{@link EntityTablePanelControl#TOGGLE_CONDITION_PANEL EntityTablePanelControl#TOGGLE_CONDITION_PANEL}</li>
	 * 	 <li>{@link EntityTablePanelControl#TOGGLE_FILTER_PANEL EntityTablePanelControl#TOGGLE_FILTER_PANEL}</li>
	 * 	 <li>Separator</li>
	 * 	 <li>{@link EntityTablePanelControl#ADD EntityTablePanelControl#ADD} (If an EditPanel is available)</li>
	 * 	 <li>{@link EntityTablePanelControl#EDIT EntityTablePanelControl#EDIT} (If an EditPanel is available)</li>
	 * 	 <li>{@link EntityTablePanelControl#DELETE EntityTablePanelControl#DELETE}</li>
	 * 	 <li>Separator</li>
	 * 	 <li>{@link EntityTablePanelControl#EDIT_SELECTED_ATTRIBUTE EntityTablePanelControl#EDIT_SELECTED_ATTRIBUTE}</li>
	 * 	 <li>Separator</li>
	 * 	 <li>{@link EntityTablePanelControl#PRINT EntityTablePanelControl#PRINT}</li>
	 * 	 <li>Separator</li>
	 * 	 <li>{@link EntityTablePanelControl#ADDITIONAL_TOOLBAR_CONTROLS EntityTablePanelControl#ADDITIONAL_TOOLBAR_CONTROLS}</li>
	 * </ul>
	 * @param toolBarConfig provides access to the toolbar configuration
	 * @see Controls.Config#clear()
	 */
	protected final void configureToolBar(Consumer<Controls.Config<EntityTablePanelControl>> toolBarConfig) {
		throwIfInitialized();
		requireNonNull(toolBarConfig).accept(this.toolBarConfiguration);
	}

	/**
	 * Configures the popup menu controls.<br>
	 * Note that the {@link Controls.Config} instance has pre-configured defaults,
	 * which must be cleared in order to start with an empty configuration.
	 * <pre>
	 *   configurePopupMenu(config -> config.clear()
	 *           .standard(EntityTablePanelControl.REFRESH)
	 *           .separator()
	 *           .control(createCustomControl())
	 *           .separator()
	 *           .defaults())
	 * </pre>
	 * Defaults:
	 * <ul>
	 *   <li>{@link EntityTablePanelControl#REFRESH EntityTablePanelControl#REFRESH}</li>
	 *   <li>{@link EntityTablePanelControl#CLEAR EntityTablePanelControl#CLEAR}</li>
	 *   <li>Separator</li>
	 *   <li>{@link EntityTablePanelControl#ADD EntityTablePanelControl#ADD} (If an EditPanel is available)</li>
	 *   <li>{@link EntityTablePanelControl#EDIT EntityTablePanelControl#EDIT} (If an EditPanel is available)</li>
	 *   <li>{@link EntityTablePanelControl#DELETE EntityTablePanelControl#DELETE}</li>
	 *   <li>Separator</li>
	 *   <li>{@link EntityTablePanelControl#EDIT_SELECTED_ATTRIBUTE EntityTablePanelControl#EDIT_SELECTED_ATTRIBUTE} or {@link EntityTablePanelControl#EDIT_ATTRIBUTE_CONTROLS EntityTablePanelControl#EDIT_ATTRIBUTE_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link EntityTablePanelControl#VIEW_DEPENDENCIES EntityTablePanelControl#VIEW_DEPENDENCIES}</li>
	 *   <li>Separator</li>
	 *   <li>{@link EntityTablePanelControl#ADDITIONAL_POPUP_MENU_CONTROLS EntityTablePanelControl#ADDITIONAL_POPUP_MENU_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link EntityTablePanelControl#PRINT_CONTROLS EntityTablePanelControl#PRINT_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link EntityTablePanelControl#COLUMN_CONTROLS EntityTablePanelControl#COLUMN_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link EntityTablePanelControl#SELECTION_MODE EntityTablePanelControl#SELECTION_MODE}</li>
	 *   <li>Separator</li>
	 *   <li>{@link EntityTablePanelControl#CONDITION_CONTROLS EntityTablePanelControl#CONDITION_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link EntityTablePanelControl#FILTER_CONTROLS EntityTablePanelControl#FILTER_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link EntityTablePanelControl#COPY_CONTROLS EntityTablePanelControl#COPY_CONTROLS}</li>
	 * </ul>
	 * @param popupMenuConfig provides access to the popup menu configuration
	 * @see Controls.Config#clear()
	 */
	protected final void configurePopupMenu(Consumer<Controls.Config<EntityTablePanelControl>> popupMenuConfig) {
		throwIfInitialized();
		requireNonNull(popupMenuConfig).accept(this.popupMenuConfiguration);
	}

	/**
	 * @param <T> the edit panel type
	 * @return the edit panel
	 * @throws IllegalStateException in case no edit panel is available
	 */
	protected final <T extends EntityEditPanel> T editPanel() {
		if (editPanel == null) {
			throw new IllegalStateException("No editPanel is available");
		}

		return (T) editPanel;
	}

	/**
	 * Creates a TableCellEditor for the given attribute, returns null if no editor is available,
	 * such as for non-editable attributes.
	 * @param attribute the attribute
	 * @return a TableCellEditor for the given attribute, null in case none is available
	 * @see Config#editable(Consumer)
	 */
	protected TableCellEditor createTableCellEditor(Attribute<?> attribute) {
		if (!configuration.editable.contains(attribute)) {
			return null;
		}
		if (nonUpdatableForeignKey(attribute)) {
			return null;
		}

		return new EntityTableCellEditor<>(() -> cellEditorComponentValue(attribute, null));
	}

	/**
	 * @return the table condition panel
	 * @see FilterTableConditionPanel#filterTableConditionPanel(TableConditionModel, Collection, FilterTableColumnModel)
	 */
	protected TableConditionPanel<Attribute<?>> createConditionPanel() {
		return (TableConditionPanel<Attribute<?>>) filterTableConditionPanel(
						tableModel.conditionModel(), createConditionPanels(), table.getColumnModel());
	}

	/**
	 * This method simply adds {@code tablePanel} at location BorderLayout.CENTER and,
	 * if non-null, the given {@code southPanel} to the {@code BorderLayout.SOUTH} location.
	 * By overriding this method you can override the default layout.
	 * @param tableComponent the component containing the table, condition and summary panel
	 * @param southPanel the south toolbar panel, null if not required
	 * @see #initializeSouthPanel()
	 */
	protected void layoutPanel(JComponent tableComponent, JPanel southPanel) {
		requireNonNull(tableComponent, "tableComponent");
		setLayout(new BorderLayout());
		add(tableComponent, BorderLayout.CENTER);
		if (southPanel != null) {
			add(southPanel, BorderLayout.SOUTH);
		}
	}

	/**
	 * Propagates the exception to {@link #onValidationException(ValidationException)} or
	 * {@link #onReferentialIntegrityException(ReferentialIntegrityException)} depending on type,
	 * otherwise displays the exception.
	 * @param exception the exception to handle
	 * @see #displayException(Exception)
	 */
	protected void onException(Exception exception) {
		if (exception instanceof ValidationException) {
			onValidationException((ValidationException) exception);
		}
		else if (exception instanceof ReferentialIntegrityException) {
			onReferentialIntegrityException((ReferentialIntegrityException) exception);
		}
		else {
			displayException(exception);
		}
	}

	/**
	 * Called when a {@link ReferentialIntegrityException} occurs during a delete operation on the selected entities.
	 * If the referential error handling is {@link ReferentialIntegrityErrorHandling#DISPLAY_DEPENDENCIES},
	 * the dependencies of the entities involved are displayed to the user, otherwise {@link #onException(Exception)} is called.
	 * @param exception the exception
	 * @see Config#referentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling)
	 */
	protected void onReferentialIntegrityException(ReferentialIntegrityException exception) {
		requireNonNull(exception);
		if (configuration.referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES) {
			displayDependenciesDialog(tableModel.selectionModel().getSelectedItems(), tableModel.connectionProvider(),
							this, EDIT_PANEL_MESSAGES.getString("unknown_dependent_records"));
		}
		else {
			displayException(exception);
		}
	}

	/**
	 * Displays the exception message.
	 * @param exception the exception
	 */
	protected void onValidationException(ValidationException exception) {
		requireNonNull(exception);
		String title = tableModel.entities()
						.definition(exception.attribute().entityType())
						.attributes().definition(exception.attribute())
						.caption();
		JOptionPane.showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Override to customize the edit dialog used when multiple entities are edited.
	 * @param attribute the attribute to edit
	 * @param <T> the attribute type
	 * @return a edit dialog builder
	 */
	protected <T> EntityDialogs.EditAttributeDialogBuilder<T> editDialogBuilder(Attribute<T> attribute) {
		return EntityDialogs.editAttributeDialog(tableModel.editModel(), attribute)
						.owner(this)
						.componentFactory((EntityComponentFactory<T, Attribute<T>, ?>) configuration.editComponentFactories.get(attribute));
	}

	/**
	 * Displays the exception in a dialog, with the dialog owner as the current focus owner
	 * or this panel if none is available.
	 * @param exception the exception to display
	 */
	protected final void displayException(Exception exception) {
		Component focusOwner = getCurrentKeyboardFocusManager().getFocusOwner();
		if (focusOwner == null) {
			focusOwner = EntityTablePanel.this;
		}
		Dialogs.displayExceptionDialog(exception, parentWindow(focusOwner));
	}

	/**
	 * @return true if confirmed
	 * @see Config#deleteConfirmer(Confirmer)
	 */
	protected final boolean confirmDelete() {
		return configuration.deleteConfirmer.confirm(this);
	}

	/**
	 * Clears any user preferences saved for this table model
	 */
	final void clearPreferences() {
		String userPreferencesKey = userPreferencesKey();
		UserPreferences.removeUserPreference(userPreferencesKey + COLUMN_PREFERENCES);
		UserPreferences.removeUserPreference(userPreferencesKey + CONDITIONS_PREFERENCES);
	}

	/**
	 * Creates a {@link Control} for adding a new entity via the available edit panel.
	 * @return the add control
	 */
	private Control createAddControl() {
		return Control.builder(() -> addEntityDialog(() -> editPanel)
										.owner(this)
										.closeDialog(false)
										.show())
						.name(FrameworkMessages.add())
						.mnemonic(FrameworkMessages.addMnemonic())
						.smallIcon(ICONS.add())
						.build();
	}

	/**
	 * Creates a {@link Control} for editing the selected component via the available edit panel.
	 * @return the edit control
	 */
	private Control createEditControl() {
		return Control.builder(() -> editEntityDialog(() -> editPanel)
										.owner(this)
										.show())
						.name(FrameworkMessages.edit())
						.mnemonic(FrameworkMessages.editMnemonic())
						.smallIcon(ICONS.edit())
						.enabled(tableModel().selectionModel().singleSelection())
						.build();
	}

	/**
	 * Creates a {@link Control} for editing the value of a single attribute
	 * for the selected entities, enabled as long as the selection is not empty
	 * and {@link EntityEditModel#updateEnabled()} is enabled.
	 * @return the edit control
	 * @see Config#editable(Consumer)
	 * @see EntityEditModel#updateEnabled()
	 */
	private Control createEditSelectedAttributeControl() {
		return Control.builder(this::editSelected)
						.name(FrameworkMessages.edit())
						.enabled(createEditSelectedEnabledObserver())
						.smallIcon(ICONS.edit())
						.description(FrameworkMessages.editSelectedTip())
						.build();
	}

	/**
	 * Creates a {@link Controls} containing controls for editing the value of a single attribute
	 * for the selected entities. These controls are enabled as long as the selection is not empty
	 * and {@link EntityEditModel#updateEnabled()} is enabled.
	 * @return the edit controls
	 * @see Config#editable(Consumer)
	 * @see EntityEditModel#updateEnabled()
	 */
	private Controls createEditAttributeControls() {
		StateObserver editSelectedEnabledObserver = createEditSelectedEnabledObserver();
		Controls editControls = Controls.builder()
						.name(FrameworkMessages.edit())
						.enabled(editSelectedEnabledObserver)
						.smallIcon(ICONS.edit())
						.description(FrameworkMessages.editSelectedTip())
						.build();
		configuration.editable.get().stream()
						.map(attribute -> tableModel.entityDefinition().attributes().definition(attribute))
						.sorted(AttributeDefinition.definitionComparator())
						.forEach(attributeDefinition -> editControls.add(Control.builder(() -> editSelected(attributeDefinition.attribute()))
										.name(attributeDefinition.caption() == null ? attributeDefinition.attribute().name() : attributeDefinition.caption())
										.enabled(editSelectedEnabledObserver)
										.build()));

		return editControls.empty() ? null : editControls;
	}

	private StateObserver createEditSelectedEnabledObserver() {
		StateObserver selectionNotEmpty = tableModel.selectionModel().selectionNotEmpty();
		StateObserver updateEnabled = tableModel.editModel().updateEnabled();
		StateObserver updateMultipleEnabledOrSingleSelection =
						State.or(tableModel.editModel().updateMultipleEnabled(),
										tableModel.selectionModel().singleSelection());

		return State.and(selectionNotEmpty, updateEnabled, updateMultipleEnabledOrSingleSelection);
	}

	/**
	 * @return a control for showing the dependencies dialog
	 */
	private Control createViewDependenciesControl() {
		return Control.builder(this::viewDependencies)
						.name(FrameworkMessages.dependencies())
						.enabled(tableModel.selectionModel().selectionNotEmpty())
						.description(FrameworkMessages.dependenciesTip())
						.smallIcon(ICONS.dependencies())
						.build();
	}

	/**
	 * @return a control for deleting the selected entities
	 * @throws IllegalStateException in case the underlying model is read only or if deleting is not enabled
	 */
	private Control createDeleteControl() {
		return Control.builder(new DeleteCommand())
						.name(FrameworkMessages.delete())
						.enabled(State.and(
										tableModel.editModel().deleteEnabled(),
										tableModel.selectionModel().selectionNotEmpty()))
						.description(FrameworkMessages.deleteSelectedTip())
						.smallIcon(ICONS.delete())
						.build();
	}

	/**
	 * @return a Control for refreshing the underlying table data
	 */
	private Control createRefreshControl() {
		return Control.builder(tableModel::refresh)
						.name(Messages.refresh())
						.description(Messages.refreshTip())
						.mnemonic(Messages.refreshMnemonic())
						.smallIcon(ICONS.refresh())
						.enabled(tableModel.refresher().observer().not())
						.build();
	}

	private Control createColumnSelectionControl() {
		return configuration.columnSelection == ColumnSelection.DIALOG ?
						table.createSelectColumnsControl() : table.createToggleColumnsControls();
	}

	/**
	 * @return a Control for clearing the underlying table model, that is, removing all rows
	 */
	private Control createClearControl() {
		return Control.builder(tableModel::clear)
						.name(Messages.clear())
						.description(Messages.clearTip())
						.mnemonic(Messages.clearMnemonic())
						.smallIcon(ICONS.clear())
						.build();
	}

	private Controls createPrintControls() {
		Controls.Builder builder = Controls.builder()
						.name(Messages.print())
						.mnemonic(Messages.printMnemonic())
						.smallIcon(ICONS.print());
		control(PRINT).optional().ifPresent(builder::control);

		Controls printControls = builder.build();

		return printControls.empty() ? null : printControls;
	}

	private Controls createAdditionalPopupControls() {
		Controls additionalControls = Controls.controls();
		additionalPopupControls.forEach(controlList -> {
			if (nullOrEmpty(controlList.getName())) {
				additionalControls.addAll(controlList);
			}
			else {
				additionalControls.add(controlList);
			}
		});

		return additionalControls.empty() ? null : additionalControls;
	}

	private Controls createAdditionalToolbarControls() {
		Controls additionalControls = Controls.controls();
		additionalToolBarControls.forEach(controlsList -> {
			if (nullOrEmpty(controlsList.getName())) {
				additionalControls.addAll(controlsList);
			}
			else {
				additionalControls.add(controlsList);
			}
		});

		return additionalControls.empty() ? null : additionalControls;
	}

	private Control createToggleConditionPanelControl() {
		return Control.builder(this::toggleConditionPanel)
						.smallIcon(ICONS.search())
						.description(MESSAGES.getString("show_condition_panel"))
						.build();
	}

	private Control createSelectConditionPanelControl() {
		return Control.control(this::selectConditionPanel);
	}

	private Controls createConditionControls() {
		if (!configuration.includeConditionPanel || conditionPanel == null) {
			return null;
		}
		Controls conditionControls = Controls.builder()
						.name(FrameworkMessages.searchNoun())
						.smallIcon(ICONS.search())
						.build();
		Controls conditionPanelControls = conditionPanel.controls();
		if (conditionPanelControls.notEmpty()) {
			conditionControls.addAll(conditionPanelControls);
			conditionControls.addSeparator();
		}
		conditionControls.add(ToggleControl.builder(tableModel.conditionRequired())
						.name(MESSAGES.getString("require_query_condition"))
						.description(MESSAGES.getString("require_query_condition_description"))
						.build());

		return conditionControls.empty() ? null : conditionControls;
	}

	private Control createToggleFilterPanelControl() {
		return Control.builder(this::toggleFilterPanel)
						.smallIcon(ICONS.filter())
						.description(MESSAGES.getString("show_filter_panel"))
						.build();
	}

	private Control createSelectFilterPanelControl() {
		return Control.control(this::selectFilterPanel);
	}

	private void toggleConditionPanel() {
		toggleConditionPanel(conditionPanelScrollPane, conditionPanel.state());
	}

	private void toggleFilterPanel() {
		toggleConditionPanel(filterPanelScrollPane, table.filterPanel().state());
	}

	private Controls createFilterControls() {
		if (!configuration.includeFilterPanel) {
			return null;
		}
		Controls filterControls = Controls.builder()
						.name(FrameworkMessages.filterNoun())
						.smallIcon(ICONS.filter())
						.build();
		Controls filterPanelControls = table.filterPanel().controls();
		if (filterPanelControls.notEmpty()) {
			filterControls.addAll(filterPanelControls);
		}

		return filterControls.empty() ? null : filterControls;
	}

	private Control createToggleSummaryPanelControl() {
		return ToggleControl.builder(summaryPanelVisibleState)
						.smallIcon(ICONS.summary())
						.description(MESSAGES.getString("toggle_summary_tip"))
						.build();
	}

	private Control createClearSelectionControl() {
		return Control.builder(tableModel.selectionModel()::clearSelection)
						.enabled(tableModel.selectionModel().selectionNotEmpty())
						.smallIcon(ICONS.clearSelection())
						.description(MESSAGES.getString("clear_selection_tip"))
						.build();
	}

	private Control createMoveSelectionDownControl() {
		return Control.builder(tableModel.selectionModel()::moveSelectionDown)
						.smallIcon(ICONS.down())
						.description(MESSAGES.getString("selection_down_tip"))
						.build();
	}

	private Control createMoveSelectionUpControl() {
		return Control.builder(tableModel.selectionModel()::moveSelectionUp)
						.smallIcon(ICONS.up())
						.description(MESSAGES.getString("selection_up_tip"))
						.build();
	}

	private Control createRequestTableFocusControl() {
		return Control.control(table::requestFocus);
	}

	private Control createRequestSearchFieldFocusControl() {
		return Control.control(table.searchField()::requestFocusInWindow);
	}

	private Controls createColumnControls() {
		Controls.Builder builder = Controls.builder()
						.name(MESSAGES.getString("columns"))
						.smallIcon(ICONS.columns());
		control(SELECT_COLUMNS).optional().ifPresent(builder::control);
		control(RESET_COLUMNS).optional().ifPresent(builder::control);
		control(COLUMN_AUTO_RESIZE_MODE).optional().ifPresent(builder::control);

		Controls columnControls = builder.build();

		return columnControls.empty() ? null : columnControls;
	}

	private Controls createCopyControls() {
		Controls.Builder builder = Controls.builder()
						.name(Messages.copy())
						.smallIcon(ICONS.copy());
		control(COPY_CELL).optional().ifPresent(builder::control);
		control(COPY_ROWS).optional().ifPresent(builder::control);

		Controls copyControls = builder.build();

		return copyControls.empty() ? null : copyControls;
	}

	private Control createCopyRowsControl() {
		return Control.builder(table::copyToClipboard)
						.name(FrameworkMessages.copyTableWithHeader())
						.build();
	}

	private boolean includeAddControl() {
		return editPanel != null && configuration.includeAddControl &&
						!tableModel.editModel().readOnly().get() &&
						tableModel.editModel().insertEnabled().get();
	}

	private boolean includeEditControl() {
		return editPanel != null && updatable() &&
						configuration.includeEditControl;
	}

	private boolean includeEditAttributeControls() {
		return !configuration.editable.empty() && updatable() &&
						configuration.includeEditAttributeControl;
	}

	private boolean updatable() {
		return !tableModel.editModel().readOnly().get() &&
						tableModel.editModel().updateEnabled().get();
	}

	private boolean includeDeleteControl() {
		return !tableModel.editModel().readOnly().get() && tableModel.editModel().deleteEnabled().get();
	}

	private Control createConditionRefreshControl() {
		return Control.builder(tableModel::refresh)
						.enabled(tableModel.conditionChanged())
						.smallIcon(ICONS.refreshRequired())
						.build();
	}

	private <T> ComponentValue<T, ? extends JComponent> cellEditorComponentValue(Attribute<T> attribute, T initialValue) {
		return ((EntityComponentFactory<T, Attribute<T>, ?>) configuration.cellEditorComponentFactories.computeIfAbsent(attribute, a ->
						new DefaultEntityComponentFactory<T, Attribute<T>, JComponent>())).componentValue(attribute, tableModel.editModel(), initialValue);
	}

	private JToolBar createRefreshButtonToolBar() {
		KeyEvents.builder(VK_F5)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(conditionRefreshControl)
						.enable(this);

		return toolBar()
						.action(conditionRefreshControl)
						.floatable(false)
						.rollover(false)
						.visible(configuration.refreshButtonVisible == RefreshButtonVisible.ALWAYS ||
										(conditionPanel != null && conditionPanel.state().isNotEqualTo(ConditionState.HIDDEN)))
						.build();
	}

	private Collection<ColumnConditionPanel<? extends Attribute<?>, ?>> createConditionPanels() {
		return tableModel.conditionModel().conditionModels().values().stream()
						.filter(conditionModel -> table.columnModel().containsColumn(conditionModel.columnIdentifier()))
						.filter(conditionModel -> configuration.conditionFieldFactory.supportsType(conditionModel.columnClass()))
						.map(this::createConditionPanel)
						.collect(toList());
	}

	private FilterColumnConditionPanel<? extends Attribute<?>, ?> createConditionPanel(ColumnConditionModel<? extends Attribute<?>, ?> conditionModel) {
		FilterColumnConditionPanel<? extends Attribute<?>, ?> columnConditionPanel =
						filterColumnConditionPanel(conditionModel, (FieldFactory<Attribute<?>>) configuration.conditionFieldFactory);
		columnConditionPanel.components().forEach(component ->
						configureComponent(component, conditionModel.columnIdentifier()));

		return columnConditionPanel;
	}

	private JComponent configureComponent(JComponent component, Attribute<?> attribute) {
		if (component instanceof JTextField) {
			((JTextField) component).setColumns(0);
			TableCellRenderer cellRenderer = table().columnModel().column(attribute).getCellRenderer();
			if (cellRenderer instanceof DefaultTableCellRenderer) {
				((JTextField) component).setHorizontalAlignment(((DefaultTableCellRenderer) cellRenderer).getHorizontalAlignment());
			}
		}
		else if (component instanceof JCheckBox) {
			((JCheckBox) component).setHorizontalAlignment(SwingConstants.CENTER);
		}

		return component;
	}

	private void initializeConditionPanel() {
		TableConditionPanel<Attribute<?>> panel = createConditionPanel();
		if (!(panel instanceof JComponent)) {
			throw new IllegalStateException("Condition panel must extend JComponent");
		}
		conditionPanel = panel;
		conditionPanelScrollPane = createLinkedScrollPane((JComponent) conditionPanel);
		conditionPanelStateChanged(conditionPanel.state().get());
		bindConditionPanelEvents();
	}

	private void bindConditionPanelEvents() {
		conditionPanel.state().addConsumer(this::conditionPanelStateChanged);
		table.filterPanel().state().addConsumer(this::filterPanelStateChanged);
	}

	private void conditionPanelStateChanged(ConditionState conditionState) {
		refreshButtonToolBar.setVisible(configuration.refreshButtonVisible == RefreshButtonVisible.ALWAYS
						|| conditionState != ConditionState.HIDDEN);
		if (conditionPanelScrollPane != null) {
			if (conditionState == ConditionState.HIDDEN) {
				remove(conditionPanelScrollPane);
			}
			else {
				add(conditionPanelScrollPane, BorderLayout.NORTH);
			}
		}
	}

	private void filterPanelStateChanged(ConditionState conditionState) {
		filterPanelStateChanged(conditionState, tablePanel.tableSouthPanel);
	}

	private void filterPanelStateChanged(ConditionState conditionState, JPanel parentPanel) {
		if (conditionState == ConditionState.HIDDEN) {
			parentPanel.remove(filterPanelScrollPane);
		}
		else {
			parentPanel.add(filterPanelScrollPane, BorderLayout.SOUTH);
		}
	}

	private void bindTableEvents() {
		Runnable setSelectAttributes = () -> tableModel.attributes().set(selectAttributes());
		table.columnModel().columnShownEvent().addListener(setSelectAttributes);
		table.columnModel().columnHiddenEvent().addListener(setSelectAttributes);
		table.columnModel().columnHiddenEvent().addConsumer(this::onColumnHidden);
		queryHiddenColumns.addListener(setSelectAttributes);
		orderQueryBySortOrder.addConsumer(enabled ->
						tableModel.orderBy().set(enabled ? orderByFromSortModel() : null));
		table.sortModel().sortingChangedEvent().addListener(() ->
						tableModel.orderBy().set(orderQueryBySortOrder.get() ? orderByFromSortModel() : null));
	}

	private void bindEvents() {
		summaryPanelVisibleState.addConsumer(this::setSummaryPanelVisible);
		tableModel.conditionModel().conditionChangedEvent().addListener(this::onConditionChanged);
		tableModel.refresher().observer().addConsumer(this::onRefreshingChanged);
		tableModel.refresher().refreshFailedEvent().addConsumer(this::onException);
		tableModel.editModel().insertUpdateOrDeleteEvent().addListener(table::repaint);
		if (conditionPanel != null) {
			enableConditionPanelRefreshOnEnter();
			conditionPanel.conditionPanels().forEach(panel -> panel.components().forEach(component ->
							component.addFocusListener(new ScrollToColumn(panel.conditionModel().columnIdentifier()))));
			conditionPanel.state().addListener(this::revalidate);
		}
		if (configuration.includeFilterPanel) {
			table.filterPanel().conditionPanels().forEach(panel -> panel.components().forEach(component ->
							component.addFocusListener(new ScrollToColumn(panel.conditionModel().columnIdentifier()))));
			table.filterPanel().state().addListener(this::revalidate);
		}
	}

	private void enableConditionPanelRefreshOnEnter() {
		KeyEvents.builder(VK_ENTER)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(conditionRefreshControl)
						.enable((JComponent) conditionPanel);
		conditionPanel.conditionPanels().stream()
						.flatMap(panel -> panel.components().stream())
						.forEach(this::enableConditionPanelRefreshOnEnter);
	}

	private void enableConditionPanelRefreshOnEnter(JComponent component) {
		if (component instanceof JComboBox) {
			new ComboBoxEnterPressedAction((JComboBox<?>) component, conditionRefreshControl);
		}
		else if (component instanceof TemporalField) {
			((TemporalField<?>) component).addActionListener(conditionRefreshControl);
		}
	}

	private void setSummaryPanelVisible(boolean visible) {
		if (summaryPanelScrollPane != null) {
			summaryPanelScrollPane.setVisible(visible);
			revalidate();
		}
	}

	private void setupComponents() {
		tableScrollPane.setViewportView(table());
		tablePanel = new TablePanel();
		table.getColumnModel().columns().forEach(this::configureColumn);
		summaryPanelVisibleState.addValidator(new ComponentAvailableValidator(summaryPanel, "summary"));
	}

	private Map<EntityTablePanelControl, Value<Control>> createControls() {
		Value.Validator<Control> controlValueValidator = control -> {
			if (initialized) {
				throw new IllegalStateException("Controls must be configured before the panel is initialized");
			}
		};
		Map<EntityTablePanelControl, Value<Control>> controlMap = Stream.of(values())
						.collect(toMap(Function.identity(), controlCode -> Value.<Control>nullable()
										.validator(controlValueValidator)
										.build()));
		if (includeDeleteControl()) {
			controlMap.get(DELETE).set(createDeleteControl());
		}
		if (includeAddControl()) {
			controlMap.get(ADD).set(createAddControl());
		}
		if (includeEditControl()) {
			controlMap.get(EDIT).set(createEditControl());
		}
		if (includeEditAttributeControls()) {
			controlMap.get(EDIT_ATTRIBUTE_CONTROLS).set(createEditAttributeControls());
			controlMap.get(EDIT_SELECTED_ATTRIBUTE).set(createEditSelectedAttributeControl());
		}
		if (configuration.includeClearControl) {
			controlMap.get(CLEAR).set(createClearControl());
		}
		controlMap.get(REFRESH).set(createRefreshControl());
		controlMap.get(SELECT_COLUMNS).set(createColumnSelectionControl());
		controlMap.get(RESET_COLUMNS).set(table.createResetColumnsControl());
		controlMap.get(COLUMN_AUTO_RESIZE_MODE).set(table.createAutoResizeModeControl());
		if (includeViewDependenciesControl()) {
			controlMap.get(VIEW_DEPENDENCIES).set(createViewDependenciesControl());
		}
		if (configuration.includeSummaryPanel) {
			controlMap.get(TOGGLE_SUMMARY_PANEL).set(createToggleSummaryPanelControl());
		}
		if (configuration.includeConditionPanel) {
			controlMap.get(TOGGLE_CONDITION_PANEL).set(createToggleConditionPanelControl());
			controlMap.get(SELECT_CONDITION_PANEL).set(createSelectConditionPanelControl());
		}
		if (configuration.includeFilterPanel) {
			controlMap.get(TOGGLE_FILTER_PANEL).set(createToggleFilterPanelControl());
			controlMap.get(SELECT_FILTER_PANEL).set(createSelectFilterPanelControl());
		}
		controlMap.get(CLEAR_SELECTION).set(createClearSelectionControl());
		controlMap.get(MOVE_SELECTION_UP).set(createMoveSelectionUpControl());
		controlMap.get(MOVE_SELECTION_DOWN).set(createMoveSelectionDownControl());
		controlMap.get(COPY_CELL).set(table.createCopyCellControl());
		controlMap.get(COPY_ROWS).set(createCopyRowsControl());
		if (configuration.includeEntityMenu) {
			controlMap.get(DISPLAY_ENTITY_MENU).set(Control.control(this::showEntityMenu));
		}
		if (configuration.includePopupMenu) {
			controlMap.get(DISPLAY_POPUP_MENU).set(Control.control(this::showPopupMenu));
		}
		if (configuration.includeSelectionModeControl) {
			controlMap.get(SELECTION_MODE).set(table.createSingleSelectionModeControl());
		}
		controlMap.get(REQUEST_TABLE_FOCUS).set(createRequestTableFocusControl());
		controlMap.get(REQUEST_SEARCH_FIELD_FOCUS).set(createRequestSearchFieldFocusControl());

		return unmodifiableMap(controlMap);
	}

	private void setupStandardControls() {
		controls.get(ADDITIONAL_POPUP_MENU_CONTROLS).set(createAdditionalPopupControls());
		controls.get(ADDITIONAL_TOOLBAR_CONTROLS).set(createAdditionalToolbarControls());
		controls.get(PRINT_CONTROLS).set(createPrintControls());
		controls.get(CONDITION_CONTROLS).set(createConditionControls());
		controls.get(FILTER_CONTROLS).set(createFilterControls());
		controls.get(COLUMN_CONTROLS).set(createColumnControls());
		controls.get(COPY_CONTROLS).set(createCopyControls());
	}

	private boolean includeViewDependenciesControl() {
		return tableModel.entities().definitions().stream()
						.flatMap(entityDefinition -> entityDefinition.foreignKeys().definitions().stream())
						.filter(foreignKeyDefinition -> !foreignKeyDefinition.soft())
						.anyMatch(foreignKeyDefinition -> foreignKeyDefinition.attribute().referencedType().equals(tableModel.entityType()));
	}

	private void configureColumn(FilterTableColumn<Attribute<?>> column) {
		TableCellEditor tableCellEditor = createTableCellEditor(column.getIdentifier());
		if (tableCellEditor != null) {
			column.setCellEditor(tableCellEditor);
		}
		column.setHeaderRenderer(new HeaderRenderer(column.getHeaderRenderer()));
	}

	private void addTablePopupMenu() {
		if (configuration.includePopupMenu) {
			Controls popupControls = popupMenuConfiguration.create();
			if (popupControls == null || popupControls.empty()) {
				return;
			}

			JPopupMenu popupMenu = menu(popupControls).createPopupMenu();
			table.setComponentPopupMenu(popupMenu);
			tableScrollPane.setComponentPopupMenu(popupMenu);
		}
	}

	private void showEntityMenu() {
		Point location = popupLocation(table);
		tableModel.selectionModel().selectedItem().ifPresent(selected ->
						new EntityPopupMenu(selected.copy(), tableModel.connection()).show(table, location.x, location.y));
	}

	private void showPopupMenu() {
		Point location = popupLocation(table);
		table.getComponentPopupMenu().show(table, location.x, location.y);
	}

	private void onConditionChanged() {
		if (table != null) {
			table.getTableHeader().repaint();
			table.repaint();
		}
	}

	private void onRefreshingChanged(boolean refreshing) {
		if (refreshing) {
			setCursor(Cursors.WAIT);
		}
		else {
			setCursor(Cursors.DEFAULT);
		}
	}

	private void toggleConditionPanel(JScrollPane scrollPane, Value<ConditionState> conditionState) {
		if (scrollPane != null) {
			switch (conditionState.get()) {
				case HIDDEN:
					conditionState.set(ConditionState.SIMPLE);
					break;
				case SIMPLE:
					conditionState.set(ConditionState.ADVANCED);
					break;
				case ADVANCED:
					setConditionStateAdvance(scrollPane, conditionState);
					break;
			}
		}
	}

	private void setConditionStateAdvance(JScrollPane scrollPane, Value<ConditionState> conditionState) {
		boolean parentOfFocusOwner = parentOfType(JScrollPane.class,
						getCurrentKeyboardFocusManager().getFocusOwner()) == scrollPane;
		conditionState.set(ConditionState.HIDDEN);
		if (parentOfFocusOwner) {
			table.requestFocusInWindow();
		}
	}

	private boolean nonUpdatableForeignKey(Attribute<?> attribute) {
		if (attribute instanceof ForeignKey) {
			ForeignKey foreignKey = (ForeignKey) attribute;

			return foreignKey.references().stream()
							.map(ForeignKey.Reference::column)
							.map(referenceAttribute -> tableModel.entityDefinition().columns().definition(referenceAttribute))
							.filter(ColumnDefinition.class::isInstance)
							.map(ColumnDefinition.class::cast)
							.noneMatch(ColumnDefinition::updatable);
		}

		return false;
	}

	private EntityEditPanel validateEditModel(EntityEditPanel editPanel) {
		if (editPanel.editModel() != tableModel.editModel()) {
			throw new IllegalArgumentException("Edit panel model must be the same as the table edit model");
		}

		return editPanel;
	}

	private Map<Attribute<?>, ColumnPreferences> createColumnPreferences() {
		Map<Attribute<?>, ColumnPreferences> columnPreferencesMap = new HashMap<>();
		FilterTableColumnModel<Attribute<?>> columnModel = table.getColumnModel();
		for (FilterTableColumn<Attribute<?>> column : columnModel.columns()) {
			Attribute<?> attribute = column.getIdentifier();
			int index = columnModel.visible(attribute).get() ? columnModel.getColumnIndex(attribute) : -1;
			columnPreferencesMap.put(attribute, columnPreferences(attribute, index, column.getWidth()));
		}

		return columnPreferencesMap;
	}

	private Map<Attribute<?>, ConditionPreferences> createConditionPreferences() {
		Map<Attribute<?>, ConditionPreferences> conditionPreferencesMap = new HashMap<>();
		for (Attribute<?> attribute : tableModel.columns().identifiers()) {
			ColumnConditionModel<?, ?> columnConditionModel = tableModel.conditionModel().conditionModels().get(attribute);
			if (columnConditionModel != null) {
				conditionPreferencesMap.put(attribute, ConditionPreferences.conditionPreferences(attribute,
								columnConditionModel.autoEnable().get(),
								columnConditionModel.caseSensitive().get(),
								columnConditionModel.automaticWildcard().get()));
			}
		}

		return conditionPreferencesMap;
	}

	private void applyConditionPreferences(String preferencesString) {
		try {
			ConditionPreferences.apply(tableModel, tableModel.columns().identifiers(), preferencesString);
		}
		catch (Exception e) {
			LOG.error("Error while applying condition preferences: {}", preferencesString, e);
		}
	}

	private Collection<Attribute<?>> selectAttributes() {
		FilterTableColumnModel<Attribute<?>> columnModel = table.getColumnModel();
		if (queryHiddenColumns.get() || columnModel.hidden().isEmpty()) {
			return emptyList();
		}

		return tableModel.entityDefinition().attributes().selected().stream()
						.filter(this::columnNotHidden)
						.collect(toList());
	}

	private boolean columnNotHidden(Attribute<?> attribute) {
		return !table.getColumnModel().containsColumn(attribute) || table.getColumnModel().visible(attribute).get();
	}

	private OrderBy orderByFromSortModel() {
		if (!table.sortModel().sorted()) {
			return null;
		}
		OrderBy.Builder builder = OrderBy.builder();
		table.sortModel().columnSortOrder().stream()
						.filter(columnSortOrder -> isColumn(columnSortOrder.columnIdentifier()))
						.forEach(columnSortOrder -> {
							switch (columnSortOrder.sortOrder()) {
								case ASCENDING:
									builder.ascending((Column<?>) columnSortOrder.columnIdentifier());
									break;
								case DESCENDING:
									builder.descending((Column<?>) columnSortOrder.columnIdentifier());
									break;
								default:
							}
						});

		return builder.build();
	}

	private boolean isColumn(Attribute<?> attribute) {
		return tableModel.entityDefinition().attributes().definition(attribute) instanceof ColumnDefinition;
	}

	private void onColumnHidden(Attribute<?> attribute) {
		//disable the condition model for the column to be hidden, to prevent confusion
		ColumnConditionModel<?, ?> columnConditionModel = tableModel.conditionModel().conditionModels().get(attribute);
		if (columnConditionModel != null && !columnConditionModel.locked().get()) {
			columnConditionModel.enabled().set(false);
		}
	}

	private void applyPreferences() {
		if (EntityPanel.Config.USE_CLIENT_PREFERENCES.get()) {
			String columnPreferencesString = UserPreferences.getUserPreference(userPreferencesKey() + COLUMN_PREFERENCES, "");
			if (columnPreferencesString.isEmpty()) {//todo remove: see if a legacy one without "-columns" postfix exists
				columnPreferencesString = UserPreferences.getUserPreference(userPreferencesKey(), "");
			}
			if (!columnPreferencesString.isEmpty()) {
				applyColumnPreferences(columnPreferencesString);
			}

			String conditionPreferencesString = UserPreferences.getUserPreference(userPreferencesKey() + CONDITIONS_PREFERENCES, "");
			if (!conditionPreferencesString.isEmpty()) {
				applyConditionPreferences(conditionPreferencesString);
			}
		}
	}

	private void applyColumnPreferences(String preferencesString) {
		List<Attribute<?>> columnAttributes = table.getColumnModel().columns().stream()
						.map(FilterTableColumn::getIdentifier)
						.collect(toList());
		try {
			ColumnPreferences.apply(this, columnAttributes, preferencesString, (attribute, columnWidth) ->
							table.getColumnModel().column(attribute).setPreferredWidth(columnWidth));
		}
		catch (Exception e) {
			LOG.error("Error while applying column preferences: {}", preferencesString, e);
		}
	}

	private void throwIfInitialized() {
		if (initialized) {
			throw new IllegalStateException("Method must be called before the panel is initialized");
		}
	}

	private Config configure(Consumer<Config> configuration) {
		Config config = new Config(this);
		requireNonNull(configuration).accept(config);

		return new Config(config);
	}

	private Controls.Config<EntityTablePanelControl> createPopupMenuConfiguration() {
		return Controls.config(identifier -> control(identifier).optional(), asList(
						REFRESH,
						CLEAR,
						null,
						ADD,
						EDIT,
						DELETE,
						null,
						this.configuration.popupMenuEditAttributeControl(),
						null,
						VIEW_DEPENDENCIES,
						null,
						ADDITIONAL_POPUP_MENU_CONTROLS,
						null,
						PRINT_CONTROLS,
						null,
						COLUMN_CONTROLS,
						null,
						SELECTION_MODE,
						null,
						CONDITION_CONTROLS,
						null,
						FILTER_CONTROLS,
						null,
						COPY_CONTROLS
		));
	}

	private Controls.Config<EntityTablePanelControl> createToolBarConfiguration() {
		return Controls.config(identifier -> control(identifier).optional(), asList(
						TOGGLE_SUMMARY_PANEL,
						TOGGLE_CONDITION_PANEL,
						TOGGLE_FILTER_PANEL,
						null,
						ADD,
						EDIT,
						DELETE,
						null,
						editPanel == null ? EDIT_SELECTED_ATTRIBUTE : null,
						null,
						PRINT,
						null,
						ADDITIONAL_TOOLBAR_CONTROLS
		));
	}

	private final class DeleteCommand implements Control.Command {

		@Override
		public void execute() {
			if (confirmDelete()) {
				List<Entity> selectedItems = tableModel().selectionModel().getSelectedItems();
				progressWorkerDialog(tableModel().editModel().createDelete(selectedItems).prepare()::perform)
								.title(EDIT_PANEL_MESSAGES.getString("deleting"))
								.owner(EntityTablePanel.this)
								.onException(this::onException)
								.onResult(Delete.Result::handle)
								.execute();
			}
		}

		private void onException(Exception exception) {
			LOG.error(exception.getMessage(), exception);
			EntityTablePanel.this.onException(exception);
		}
	}

	private static final void selectConditionPanel(TableConditionPanel<Attribute<?>> tableConditionPanel,
																								 Value<ConditionState> conditionState,
																								 FilterTable<Entity, Attribute<?>> table,
																								 String dialogTitle, EntityDefinition entityDefinition) {
		List<Attribute<?>> attributes = tableConditionPanel.conditionPanels().stream()
						.map(panel -> panel.conditionModel().columnIdentifier())
						.filter(attribute -> table.getColumnModel().visible(attribute).get())
						.collect(toList());
		if (attributes.size() == 1) {
			conditionState.map(panelState -> panelState == ConditionState.HIDDEN ? ConditionState.SIMPLE : panelState);
			tableConditionPanel.conditionPanel(attributes.get(0))
							.ifPresent(ColumnConditionPanel::requestInputFocus);
		}
		else if (!attributes.isEmpty()) {
			List<AttributeDefinition<?>> sortedDefinitions = attributes.stream()
							.map(attribute -> entityDefinition.attributes().definition(attribute))
							.sorted(AttributeDefinition.definitionComparator())
							.collect(toList());
			Dialogs.selectionDialog(sortedDefinitions)
							.owner(table)
							.locationRelativeTo(table.getParent())
							.title(dialogTitle)
							.selectSingle()
							.flatMap(attributeDefinition -> tableConditionPanel.conditionPanel(attributeDefinition.attribute()))
							.ifPresent(conditionPanel -> {
								conditionState.map(panelState -> panelState == ConditionState.HIDDEN ? ConditionState.SIMPLE : panelState);
								((ColumnConditionPanel<?, ?>) conditionPanel).requestInputFocus();
							});
		}
	}

	private static GridBagConstraints createHorizontalFillConstraints() {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;

		return constraints;
	}

	private JScrollPane createLinkedScrollPane(JComponent componentToScroll) {
		return Components.scrollPane(componentToScroll)
						.horizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
						.verticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
						.onBuild(scrollPane -> linkBoundedRangeModels(
										tableScrollPane.getHorizontalScrollBar().getModel(),
										scrollPane.getHorizontalScrollBar().getModel()))
						.build();
	}

	private static Point popupLocation(JTable table) {
		Rectangle visibleRect = table.getVisibleRect();
		int x = visibleRect.x + visibleRect.width / 2;
		int y = table.getSelectionModel().isSelectionEmpty() ?
						visibleRect.y + visibleRect.height / 2 :
						table.getCellRect(table.getSelectedRow(), table.getSelectedColumn(), true).y;

		return new Point(x, y + table.getRowHeight() / 2);
	}

	private final class ScrollToColumn extends FocusAdapter {

		private final Attribute<?> attribute;

		private ScrollToColumn(Attribute<?> attribute) {
			this.attribute = attribute;
		}

		@Override
		public void focusGained(FocusEvent e) {
			table.scrollToColumn(attribute);
		}
	}

	private final class HeaderRenderer implements TableCellRenderer {

		private final TableCellRenderer wrappedRenderer;

		private HeaderRenderer(TableCellRenderer wrappedRenderer) {
			this.wrappedRenderer = wrappedRenderer;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
																									 boolean hasFocus, int row, int column) {
			Component component = wrappedRenderer == null ?
							table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) :
							wrappedRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			FilterTableColumn<Attribute<?>> tableColumn = table().getColumnModel().getColumn(column);
			TableCellRenderer renderer = tableColumn.getCellRenderer();
			boolean useBoldFont = renderer instanceof FilterTableCellRenderer
							&& ((FilterTableCellRenderer) renderer).columnShading()
							&& tableModel.conditionModel().enabled(tableColumn.getIdentifier());
			Font defaultFont = component.getFont();
			component.setFont(useBoldFont ? defaultFont.deriveFont(defaultFont.getStyle() | Font.BOLD) : defaultFont);

			return component;
		}
	}

	private static final class DeleteConfirmer implements Confirmer {

		private final FilterTableSelectionModel<?> selectionModel;

		private DeleteConfirmer(FilterTableSelectionModel<?> selectionModel) {
			this.selectionModel = selectionModel;
		}

		@Override
		public boolean confirm(JComponent dialogOwner) {
			return confirm(dialogOwner, FrameworkMessages.confirmDelete(
							selectionModel.selectionCount()), FrameworkMessages.delete());
		}
	}

	/**
	 * Contains configuration settings for a {@link EntityTablePanel} which must be set before the panel is initialized.
	 */
	public static final class Config {

		/**
		 * Specifies whether the values of hidden columns are included in the underlying query<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> QUERY_HIDDEN_COLUMNS = Configuration.booleanValue(EntityTablePanel.class.getName() + ".queryHiddenColumns", true);

		/**
		 * Specifies whether the table model sort order is used as a basis for the query order by clause.
		 * Note that this only applies to column attributes.
		 * Value type: Boolean<br>
		 * Default value: false
		 */
		public static final PropertyValue<Boolean> ORDER_QUERY_BY_SORT_ORDER = Configuration.booleanValue(EntityTablePanel.class.getName() + ".orderQueryBySortOrder", false);

		/**
		 * Specifies the default initial table condition panel state<br>
		 * Value type: {@link ConditionState}<br>
		 * Default value: {@link ConditionState#HIDDEN}
		 */
		public static final PropertyValue<ConditionState> CONDITION_STATE =
						Configuration.enumValue(EntityTablePanel.class.getName() + ".conditionState",
										ConditionState.class, ConditionState.HIDDEN);

		/**
		 * Specifies the default initial table filter panel state<br>
		 * Value type: {@link ConditionState}<br>
		 * Default value: {@link ConditionState#HIDDEN}
		 */
		public static final PropertyValue<ConditionState> FILTER_STATE =
						Configuration.enumValue(EntityTablePanel.class.getName() + ".filterState",
										ConditionState.class, ConditionState.HIDDEN);

		/**
		 * Specifies whether table summary panel should be visible or not by default<br>
		 * Value type: Boolean<br>
		 * Default value: false
		 */
		public static final PropertyValue<Boolean> SUMMARY_PANEL_VISIBLE =
						Configuration.booleanValue(EntityTablePanel.class.getName() + ".summaryPanelVisible", false);

		/**
		 * Specifies whether to include the default popup menu on entity tables<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> INCLUDE_POPUP_MENU =
						Configuration.booleanValue(EntityTablePanel.class.getName() + ".includePopupMenu", true);

		/**
		 * Specifies whether to include a {@link EntityPopupMenu} on this table, triggered with CTRL-ALT-V.<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> INCLUDE_ENTITY_MENU =
						Configuration.booleanValue(EntityTablePanel.class.getName() + ".includeEntityMenu", true);

		/**
		 * Specifies whether to include a 'Clear' control in the popup menu.<br>
		 * Value type: Boolean<br>
		 * Default value: false
		 */
		public static final PropertyValue<Boolean> INCLUDE_CLEAR_CONTROL =
						Configuration.booleanValue(EntityTablePanel.class.getName() + ".includeClearControl", false);

		/**
		 * Specifies whether to include a condition panel.<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> INCLUDE_CONDITION_PANEL =
						Configuration.booleanValue(EntityTablePanel.class.getName() + ".includeConditionPanel", true);

		/**
		 * Specifies whether to include a filter panel.<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> INCLUDE_FILTER_PANEL =
						Configuration.booleanValue(EntityTablePanel.class.getName() + ".includeFilterPanel", false);

		/**
		 * Specifies whether to include a summary panel.<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> INCLUDE_SUMMARY_PANEL =
						Configuration.booleanValue(EntityTablePanel.class.getName() + ".includeSummaryPanel", true);

		/**
		 * Specifies whether to include a popup menu for configuring the table model limit.<br>
		 * Value type: Boolean<br>
		 * Default value: false
		 */
		public static final PropertyValue<Boolean> INCLUDE_LIMIT_MENU =
						Configuration.booleanValue(EntityTablePanel.class.getName() + ".includeLimitMenu", false);

		/**
		 * Specifies whether to show an indeterminate progress bar while the model is refreshing.<br>
		 * Value type: Boolean<br>
		 * Default value: false
		 */
		public static final PropertyValue<Boolean> SHOW_REFRESH_PROGRESS_BAR =
						Configuration.booleanValue(EntityTablePanel.class.getName() + ".showRefreshProgressBar", false);

		/**
		 * Specifies whether the refresh button should always be visible or only when the condition panel is visible<br>
		 * Value type: Boolean<br>
		 * Default value: {@link RefreshButtonVisible#WHEN_CONDITION_PANEL_IS_VISIBLE}
		 */
		public static final PropertyValue<RefreshButtonVisible> REFRESH_BUTTON_VISIBLE =
						Configuration.enumValue(EntityTablePanel.class.getName() + ".refreshButtonVisible",
										RefreshButtonVisible.class, RefreshButtonVisible.WHEN_CONDITION_PANEL_IS_VISIBLE);

		/**
		 * Specifies how column selection is presented to the user.<br>
		 * Value type: {@link ColumnSelection}<br>
		 * Default value: {@link ColumnSelection#DIALOG}
		 */
		public static final PropertyValue<ColumnSelection> COLUMN_SELECTION =
						Configuration.enumValue(EntityTablePanel.class.getName() + ".columnSelection", ColumnSelection.class, ColumnSelection.DIALOG);

		/**
		 * Specifies how the edit an attribute action is presented to the user.<br>
		 * Value type: {@link EditAttributeSelection}<br>
		 * Default value: {@link EditAttributeSelection#MENU}
		 */
		public static final PropertyValue<EditAttributeSelection> EDIT_ATTRIBUTE_SELECTION =
						Configuration.enumValue(EntityTablePanel.class.getName() + ".editAttributeSelection", EditAttributeSelection.class, EditAttributeSelection.MENU);

		/**
		 * The default keyboard shortcut keyStrokes.
		 */
		public static final KeyboardShortcuts<EntityTablePanelControl> KEYBOARD_SHORTCUTS = keyboardShortcuts(EntityTablePanelControl.class);

		private static final Function<SwingEntityTableModel, String> DEFAULT_STATUS_MESSAGE = new DefaultStatusMessage();

		private final EntityTablePanel tablePanel;
		private final EntityDefinition entityDefinition;
		private final ValueSet<Attribute<?>> editable;
		private final Map<Attribute<?>, EntityComponentFactory<?, ?, ?>> editComponentFactories;
		private final Map<Attribute<?>, EntityComponentFactory<?, ?, ?>> cellEditorComponentFactories;
		private final FilterTable.Builder<Entity, Attribute<?>> tableBuilder;

		private FieldFactory<? extends Attribute<?>> conditionFieldFactory;
		private boolean includeSouthPanel = true;
		private boolean includeConditionPanel = INCLUDE_CONDITION_PANEL.get();
		private boolean includeFilterPanel = INCLUDE_FILTER_PANEL.get();
		private boolean includeSummaryPanel = INCLUDE_SUMMARY_PANEL.get();
		private boolean includeClearControl = INCLUDE_CLEAR_CONTROL.get();
		private boolean includeLimitMenu = INCLUDE_LIMIT_MENU.get();
		private boolean includeEntityMenu = INCLUDE_ENTITY_MENU.get();
		private boolean includePopupMenu = INCLUDE_POPUP_MENU.get();
		private boolean includeSelectionModeControl = false;
		private boolean includeAddControl = true;
		private boolean includeEditControl = true;
		private boolean includeEditAttributeControl = true;
		private boolean includeToolBar = true;
		private ColumnSelection columnSelection = COLUMN_SELECTION.get();
		private EditAttributeSelection editAttributeSelection = EDIT_ATTRIBUTE_SELECTION.get();
		private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling;
		private RefreshButtonVisible refreshButtonVisible;
		private Function<SwingEntityTableModel, String> statusMessage = DEFAULT_STATUS_MESSAGE;
		private boolean showRefreshProgressBar = SHOW_REFRESH_PROGRESS_BAR.get();
		private Confirmer deleteConfirmer;

		final KeyboardShortcuts<EntityTablePanelControl> shortcuts;

		private Config(EntityTablePanel tablePanel) {
			this.tablePanel = tablePanel;
			this.entityDefinition = tablePanel.tableModel.entityDefinition();
			this.tableBuilder = FilterTable.builder(tablePanel.tableModel, entityTableColumns(entityDefinition))
							.summaryValuesFactory(new EntitySummaryValuesFactory(entityDefinition, tablePanel.tableModel))
							.cellRendererFactory(new EntityTableCellRendererFactory(tablePanel.tableModel))
							.onBuild(filterTable -> filterTable.setRowHeight(filterTable.getFont().getSize() + FONT_SIZE_TO_ROW_HEIGHT));
			this.conditionFieldFactory = new EntityFieldFactory(entityComponents(entityDefinition));
			this.shortcuts = KEYBOARD_SHORTCUTS.copy();
			this.editable = valueSet(entityDefinition.attributes().updatable().stream()
							.map(AttributeDefinition::attribute)
							.collect(toSet()));
			this.editable.addValidator(new EditMenuAttributeValidator(entityDefinition));
			this.editComponentFactories = new HashMap<>();
			this.cellEditorComponentFactories = new HashMap<>();
			this.referentialIntegrityErrorHandling = ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.get();
			this.refreshButtonVisible = RefreshButtonVisible.WHEN_CONDITION_PANEL_IS_VISIBLE;
			this.deleteConfirmer = new DeleteConfirmer(tablePanel.tableModel.selectionModel());
		}

		private Config(Config config) {
			this.tablePanel = config.tablePanel;
			this.entityDefinition = config.entityDefinition;
			this.tableBuilder = config.tableBuilder;
			this.shortcuts = config.shortcuts.copy();
			this.editable = valueSet(config.editable.get());
			this.includeSouthPanel = config.includeSouthPanel;
			this.includeConditionPanel = config.includeConditionPanel;
			this.includeFilterPanel = config.includeFilterPanel;
			this.includeSummaryPanel = config.includeSummaryPanel;
			this.includeClearControl = config.includeClearControl;
			this.includeLimitMenu = config.includeLimitMenu;
			this.includeEntityMenu = config.includeEntityMenu;
			this.includePopupMenu = config.includePopupMenu;
			this.includeSelectionModeControl = config.includeSelectionModeControl;
			this.includeAddControl = config.includeAddControl;
			this.includeEditControl = config.includeEditControl;
			this.includeEditAttributeControl = config.includeEditAttributeControl;
			this.columnSelection = config.columnSelection;
			this.editAttributeSelection = config.editAttributeSelection;
			this.editComponentFactories = new HashMap<>(config.editComponentFactories);
			this.cellEditorComponentFactories = new HashMap<>(config.cellEditorComponentFactories);
			this.referentialIntegrityErrorHandling = config.referentialIntegrityErrorHandling;
			this.refreshButtonVisible = config.refreshButtonVisible;
			this.statusMessage = config.statusMessage;
			this.showRefreshProgressBar = config.showRefreshProgressBar;
			this.deleteConfirmer = config.deleteConfirmer;
			this.includeToolBar = config.includeToolBar;
			this.conditionFieldFactory = config.conditionFieldFactory;
		}

		/**
		 * @return the table panel
		 */
		public EntityTablePanel tablePanel() {
			return tablePanel;
		}

		/**
		 * Provides access to the builder for the underlying {@link FilterTable}
		 * @param tableBuilder the table builder
		 * @return this Config instance
		 */
		public Config configureTable(Consumer<FilterTable.Builder<Entity, Attribute<?>>> tableBuilder) {
			requireNonNull(tableBuilder).accept(this.tableBuilder);
			return this;
		}

		/**
		 * @param conditionFieldFactory the condition field factory
		 * @return this Config instance
		 * @see EntityTablePanel#conditionPanel()
		 */
		public Config conditionFieldFactory(FieldFactory<? extends Attribute<?>> conditionFieldFactory) {
			this.conditionFieldFactory = requireNonNull(conditionFieldFactory);
			return this;
		}

		/**
		 * @param includeSouthPanel true if the south panel should be included
		 * @return this Config instance
		 */
		public Config includeSouthPanel(boolean includeSouthPanel) {
			this.includeSouthPanel = includeSouthPanel;
			return this;
		}

		/**
		 * @param includeConditionPanel true if the condition panel should be included
		 * @return this Config instance
		 */
		public Config includeConditionPanel(boolean includeConditionPanel) {
			this.includeConditionPanel = includeConditionPanel;
			return this;
		}

		/**
		 * @param includeFilterPanel true if the filter panel should be included
		 * @return this Config instance
		 */
		public Config includeFilterPanel(boolean includeFilterPanel) {
			this.includeFilterPanel = includeFilterPanel;
			return this;
		}

		/**
		 * @param includeSummaryPanel true if the summary panel should be included
		 * @return this Config instance
		 */
		public Config includeSummaryPanel(boolean includeSummaryPanel) {
			this.includeSummaryPanel = includeSummaryPanel;
			return this;
		}

		/**
		 * @param includePopupMenu true if a popup menu should be included
		 * @return this Config instance
		 */
		public Config includePopupMenu(boolean includePopupMenu) {
			this.includePopupMenu = includePopupMenu;
			return this;
		}

		/**
		 * @param includeClearControl true if a 'Clear' control should be included in the popup menu
		 * @return this Config instance
		 * @throws IllegalStateException in case the panel has already been initialized
		 */
		public Config includeClearControl(boolean includeClearControl) {
			this.includeClearControl = includeClearControl;
			return this;
		}

		/**
		 * @param includeLimitMenu true if a popup menu for configuring the table model limit should be included
		 * @return this Config instance
		 */
		public Config includeLimitMenu(boolean includeLimitMenu) {
			this.includeLimitMenu = includeLimitMenu;
			return this;
		}

		/**
		 * @param includeEntityMenu true if a {@link EntityPopupMenu} should be available in this table, triggered with CTRL-ALT-V.<br>
		 * @return this Config instance
		 */
		public Config includeEntityMenu(boolean includeEntityMenu) {
			this.includeEntityMenu = includeEntityMenu;
			return this;
		}

		/**
		 * @param includeSelectionModeControl true if a 'Single Selection' control should be included in the popup menu
		 * @return this Config instance
		 */
		public Config includeSelectionModeControl(boolean includeSelectionModeControl) {
			this.includeSelectionModeControl = includeSelectionModeControl;
			return this;
		}

		/**
		 * @param includeToolBar true if a toolbar should be included on the south panel<br>
		 * @return this Config instance
		 */
		public Config includeToolBar(boolean includeToolBar) {
			this.includeToolBar = includeToolBar;
			return this;
		}

		/**
		 * @param includeAddControl true if a Add control should be included if a edit panel is available<br>
		 * @return this Config instance
		 */
		public Config includeAddControl(boolean includeAddControl) {
			this.includeAddControl = includeAddControl;
			return this;
		}

		/**
		 * @param includeEditControl true if a Edit control should be included if a edit panel is available<br>
		 * @return this Config instance
		 */
		public Config includeEditControl(boolean includeEditControl) {
			this.includeEditControl = includeEditControl;
			return this;
		}

		/**
		 * @param includeEditAttributeControl true if a 'Edit' attribute control should be included<br>
		 * @return this Config instance
		 * @see #editAttributeSelection(EditAttributeSelection)
		 */
		public Config includeEditAttributeControl(boolean includeEditAttributeControl) {
			this.includeEditAttributeControl = includeEditAttributeControl;
			return this;
		}

		/**
		 * @param columnSelection specifies how columns are selected
		 * @return this Config instance
		 */
		public Config columnSelection(ColumnSelection columnSelection) {
			this.columnSelection = requireNonNull(columnSelection);
			return this;
		}

		/**
		 * @param editAttributeSelection specifies attributes are selected when editing the selected records
		 * @return this Config instance
		 */
		public Config editAttributeSelection(EditAttributeSelection editAttributeSelection) {
			this.editAttributeSelection = requireNonNull(editAttributeSelection);
			return this;
		}

		/**
		 * @param shortcuts provides this tables {@link KeyboardShortcuts} instance.
		 * @return this Config instance
		 */
		public Config keyStrokes(Consumer<KeyboardShortcuts<EntityTablePanelControl>> shortcuts) {
			requireNonNull(shortcuts).accept(this.shortcuts);
			return this;
		}

		/**
		 * @param attributes provides this tables editable attribute value set
		 * @return this Config instance
		 */
		public Config editable(Consumer<ValueSet<Attribute<?>>> attributes) {
			requireNonNull(attributes).accept(this.editable);
			return this;
		}

		/**
		 * @param deleteConfirmer the delete confirmer
		 * @return this Config instance
		 */
		public Config deleteConfirmer(Confirmer deleteConfirmer) {
			this.deleteConfirmer = requireNonNull(deleteConfirmer);
			return this;
		}

		/**
		 * Sets the component factory for the given attribute, used when editing entities via {@link EntityTablePanel#editSelected(Attribute)}.
		 * @param attribute the attribute
		 * @param componentFactory the component factory
		 * @param <T> the value type
		 * @param <A> the attribute type
		 * @param <C> the component type
		 * @return this Config instance
		 */
		public <T, A extends Attribute<T>, C extends JComponent> Config editComponentFactory(A attribute,
																																												 EntityComponentFactory<T, A, C> componentFactory) {
			entityDefinition.attributes().definition(attribute);
			editComponentFactories.put(attribute, requireNonNull(componentFactory));
			return this;
		}

		/**
		 * Sets the table cell editor component factory for the given attribute.
		 * @param attribute the attribute
		 * @param componentFactory the component factory
		 * @param <T> the value type
		 * @param <A> the attribute type
		 * @param <C> the component type
		 * @return this Config instance
		 */
		public <T, A extends Attribute<T>, C extends JComponent> Config tableCellEditorFactory(A attribute,
																																													 EntityComponentFactory<T, A, C> componentFactory) {
			entityDefinition.attributes().definition(attribute);
			cellEditorComponentFactories.put(attribute, requireNonNull(componentFactory));
			return this;
		}

		/**
		 * @param referentialIntegrityErrorHandling the action to take on a referential integrity error on delete
		 * @return this Config instance
		 */
		public Config referentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling) {
			this.referentialIntegrityErrorHandling = requireNonNull(referentialIntegrityErrorHandling);
			return this;
		}

		/**
		 * @param refreshButtonVisible the refresh button visible setting
		 * @return this Config instance
		 */
		public Config refreshButtonVisible(RefreshButtonVisible refreshButtonVisible) {
			this.refreshButtonVisible = requireNonNull(refreshButtonVisible);
			return this;
		}

		/**
		 * @param statusMessage the function used for creating the table status message
		 * @return this Config instance
		 */
		public Config statusMessage(Function<SwingEntityTableModel, String> statusMessage) {
			this.statusMessage = requireNonNull(statusMessage);
			return this;
		}

		/**
		 * @param showRefreshProgressBar controls whether an indeterminate progress bar should be shown while the model is refreshing
		 * @return this Config instance
		 * @see #SHOW_REFRESH_PROGRESS_BAR
		 */
		public Config showRefreshProgressBar(boolean showRefreshProgressBar) {
			this.showRefreshProgressBar = showRefreshProgressBar;
			return this;
		}

		private EntityTablePanelControl popupMenuEditAttributeControl() {
			return editAttributeSelection == EditAttributeSelection.MENU ?
							EDIT_ATTRIBUTE_CONTROLS :
							EDIT_SELECTED_ATTRIBUTE;
		}

		private static final class EditMenuAttributeValidator implements Value.Validator<Set<Attribute<?>>> {

			private final EntityDefinition entityDefinition;

			private EditMenuAttributeValidator(EntityDefinition entityDefinition) {
				this.entityDefinition = entityDefinition;
			}

			@Override
			public void validate(Set<Attribute<?>> attributes) {
				//validate that the attributes exists
				attributes.forEach(attribute -> entityDefinition.attributes().definition(attribute));
			}
		}
	}

	private static final class EntitySummaryValuesFactory implements ColumnSummaryModel.SummaryValues.Factory<Attribute<?>> {

		private final EntityDefinition entityDefinition;
		private final FilterTableModel<?, Attribute<?>> tableModel;

		private EntitySummaryValuesFactory(EntityDefinition entityDefinition, FilterTableModel<?, Attribute<?>> tableModel) {
			this.entityDefinition = requireNonNull(entityDefinition);
			this.tableModel = requireNonNull(tableModel);
		}

		@Override
		public <T extends Number> Optional<ColumnSummaryModel.SummaryValues<T>> createSummaryValues(Attribute<?> attribute, Format format) {
			AttributeDefinition<?> attributeDefinition = entityDefinition.attributes().definition(attribute);
			if (attribute.type().isNumerical() && attributeDefinition.items().isEmpty()) {
				return Optional.of(FilterTable.summaryValues(attribute, tableModel, format));
			}

			return Optional.empty();
		}
	}

	private static final class ComponentAvailableValidator implements Value.Validator<Boolean> {

		private final JComponent component;
		private final String panelType;

		private ComponentAvailableValidator(JComponent component, String panelType) {
			this.component = component;
			this.panelType = panelType;
		}

		@Override
		public void validate(Boolean visible) throws IllegalArgumentException {
			if (visible && component == null) {
				throw new IllegalArgumentException("No " + panelType + " panel available");
			}
		}
	}

	private static final class DefaultStatusMessage implements Function<SwingEntityTableModel, String> {

		private static final NumberFormat STATUS_MESSAGE_NUMBER_FORMAT = NumberFormat.getIntegerInstance();

		@Override
		public String apply(SwingEntityTableModel tableModel) {
			int rowCount = tableModel.getRowCount();
			int filteredCount = tableModel.filteredCount();
			if (rowCount == 0 && filteredCount == 0) {
				return "";
			}
			int selectionCount = tableModel.selectionModel().selectionCount();
			StringBuilder builder = new StringBuilder();
			if (tableModel.limit().isEqualTo(tableModel.getRowCount())) {
				builder.append(MESSAGES.getString("limited_to")).append(" ");
			}
			builder.append(STATUS_MESSAGE_NUMBER_FORMAT.format(rowCount));
			if (selectionCount > 0 || filteredCount > 0) {
				builder.append(" (");
				if (selectionCount > 0) {
					builder.append(STATUS_MESSAGE_NUMBER_FORMAT.format(selectionCount)).append(" ").append(MESSAGES.getString("selected"));
				}
				if (filteredCount > 0) {
					if (selectionCount > 0) {
						builder.append(" - ");
					}
					builder.append(STATUS_MESSAGE_NUMBER_FORMAT.format(filteredCount)).append(" ").append(MESSAGES.getString("filtered"));
				}
				builder.append(")");
			}

			return builder.toString();
		}
	}

	private final class TablePanel extends JPanel {

		private final JPanel tableSouthPanel;

		private TablePanel() {
			super(new BorderLayout());
			if (configuration.includeConditionPanel) {
				if (conditionPanel == null) {
					initializeConditionPanel();
				}
			}
			tableSouthPanel = new JPanel(new BorderLayout());
			if (configuration.includeSummaryPanel && containsSummaryModels(table)) {
				summaryPanel = createSummaryPanel();
				if (summaryPanel != null) {
					summaryPanelScrollPane = createLinkedScrollPane(summaryPanel);
					summaryPanelScrollPane.setVisible(false);
					tableSouthPanel.add(summaryPanelScrollPane, BorderLayout.NORTH);
				}
			}
			if (configuration.includeFilterPanel) {
				filterPanelScrollPane = createLinkedScrollPane(table.filterPanel());
				filterPanelStateChanged(table.filterPanel().state().get(), tableSouthPanel);
			}
			add(tableScrollPane, BorderLayout.CENTER);
			add(tableSouthPanel, BorderLayout.SOUTH);
		}

		private static boolean containsSummaryModels(FilterTable<Entity, Attribute<?>> table) {
			return table.getColumnModel().columns().stream()
							.map(FilterTableColumn::getIdentifier)
							.map(table.summaryModel()::summaryModel)
							.anyMatch(Optional::isPresent);
		}

		private FilterTableColumnComponentPanel<Attribute<?>> createSummaryPanel() {
			Map<Attribute<?>, JComponent> columnSummaryPanels = createColumnSummaryPanels();
			if (columnSummaryPanels.isEmpty()) {
				return null;
			}

			return filterTableColumnComponentPanel(table.getColumnModel(), columnSummaryPanels);
		}

		private Map<Attribute<?>, JComponent> createColumnSummaryPanels() {
			Map<Attribute<?>, JComponent> components = new HashMap<>();
			table.getColumnModel().columns().forEach(column ->
							table.summaryModel().summaryModel(column.getIdentifier())
											.ifPresent(columnSummaryModel ->
															components.put(column.getIdentifier(), columnSummaryPanel(columnSummaryModel,
																			((FilterTableCellRenderer) column.getCellRenderer()).horizontalAlignment()))));

			return components;
		}
	}

	private final class SouthPanel extends JPanel {

		private SouthPanel() {
			super(new BorderLayout());
			add(Components.splitPane()
							.continuousLayout(true)
							.leftComponent(Components.panel(new GridBagLayout())
											.add(table.searchField(), createHorizontalFillConstraints())
											.build())
							.rightComponent(statusPanel())
							.build(), BorderLayout.CENTER);
			add(refreshButtonToolBar, BorderLayout.WEST);
			if (configuration.includeToolBar) {
				JToolBar southToolBar = createToolBar();
				if (southToolBar != null) {
					add(southToolBar, BorderLayout.EAST);
				}
			}
		}

		private StatusPanel statusPanel() {
			if (statusPanel == null) {
				statusPanel = new StatusPanel();
			}

			return statusPanel;
		}

		private JToolBar createToolBar() {
			Controls toolbarControls = toolBarConfiguration.create();
			if (toolbarControls == null || toolbarControls.empty()) {
				return null;
			}

			return toolBar()
							.controls(toolbarControls)
							.floatable(false)
							.rollover(true)
							.build(toolBar -> Arrays.stream(toolBar.getComponents())
											.map(JComponent.class::cast)
											.forEach(component -> component.setToolTipText(null)));
		}
	}

	private final class StatusPanel extends JPanel {

		private final Value<String> statusMessage = Value.nonNull("")
						.initialValue(configuration.statusMessage.apply(tableModel))
						.build();
		private final JLabel label = createStatusLabel();
		private final JPanel progressPanel = createProgressPanel();

		private StatusPanel() {
			super(new BorderLayout());
			add(label, BorderLayout.CENTER);
			tableModel.refresher().observer().addConsumer(new ConfigurePanel());
			tableModel.selectionModel().addListSelectionListener(e -> updateStatusMessage());
			tableModel.dataChangedEvent().addListener(this::updateStatusMessage);
			if (configuration.includeLimitMenu) {
				setComponentPopupMenu(createLimitMenu());
			}
		}

		private JLabel createStatusLabel() {
			return Components.label(statusMessage)
							.horizontalAlignment(SwingConstants.CENTER)
							.build();
		}

		private static JPanel createProgressPanel() {
			return Components.panel(new GridBagLayout())
							.add(Components.progressBar()
											.indeterminate(true)
											.string(MESSAGES.getString("refreshing"))
											.stringPainted(true)
											.build(), createHorizontalFillConstraints())
							.build();
		}

		private JPopupMenu createLimitMenu() {
			JPopupMenu popupMenu = new JPopupMenu();
			popupMenu.add(Control.builder(this::configureLimit)
							.name(MESSAGES.getString("row_limit"))
							.build());

			return popupMenu;
		}

		private void configureLimit() {
			ComponentValue<Integer, NumberField<Integer>> limitValue = Components.integerField()
							.initialValue(tableModel.limit().get())
							.groupingUsed(true)
							.minimumValue(0)
							.columns(6)
							.buildValue();
			tableModel.limit().set(Dialogs.inputDialog(limitValue)
							.title(MESSAGES.getString("row_limit"))
							.owner(EntityTablePanel.this)
							.validator(new LimitValidator())
							.show());
		}

		private void updateStatusMessage() {
			statusMessage.set(configuration.statusMessage.apply(tableModel));
		}

		private final class ConfigurePanel implements Consumer<Boolean> {

			@Override
			public void accept(Boolean isRefreshing) {
				if (configuration.showRefreshProgressBar) {
					removeAll();
					add(isRefreshing ? progressPanel : label, BorderLayout.CENTER);
					revalidate();
					repaint();
				}
			}
		}

		private final class LimitValidator implements Predicate<Integer> {

			@Override
			public boolean test(Integer limit) {
				try {
					tableModel.limit().validate(limit);
					return true;
				}
				catch (IllegalArgumentException e) {
					return false;
				}
			}
		}
	}
}
