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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.i18n.Messages;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.model.summary.SummaryModel;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueSet;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityEditModel.EditTask;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.action.DelayedAction;
import is.codion.swing.common.model.component.list.FilterListSelection;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ComponentFactory;
import is.codion.swing.common.ui.component.table.ConditionPanel;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.component.table.TableConditionPanel;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.Controls.ControlsKey;
import is.codion.swing.common.ui.control.ControlsBuilder;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityEditComponentPanel.AttributeDefinitionComparator;
import is.codion.swing.framework.ui.EntityEditPanel.Confirmer;
import is.codion.swing.framework.ui.component.DefaultEditComponentFactory;
import is.codion.swing.framework.ui.component.EditComponentFactory;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.prefs.Preferences;

import static is.codion.common.reactive.value.ValueSet.valueSet;
import static is.codion.common.utilities.Configuration.*;
import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.model.action.DelayedAction.delayedAction;
import static is.codion.swing.common.ui.Utilities.*;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.table.ColumnSummaryPanel.columnSummaryPanel;
import static is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView.*;
import static is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel.filterTableColumnComponentPanel;
import static is.codion.swing.common.ui.component.table.FilterTableConditionPanel.filterTableConditionPanel;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.framework.ui.EntityDialogs.*;
import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.*;
import static is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;
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
 * |                entityTable (FilterTable)           |
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

	/**
	 * The Controls available in a {@link EntityTablePanel}
	 * <p>Note: CTRL in key stroke descriptions represents the platform menu shortcut key (CTRL on Windows/Linux, ⌘ on macOS).
	 */
	public static final class ControlKeys {

		/**
		 * Add a new entity instance.<br>
		 * Default key stroke: INSERT
		 * @see EntityTablePanel#EntityTablePanel(SwingEntityTableModel, EntityEditPanel)
		 * @see EntityTablePanel#EntityTablePanel(SwingEntityTableModel, EntityEditPanel, Consumer)
		 */
		public static final ControlKey<CommandControl> ADD = CommandControl.key("add", keyStroke(VK_INSERT));
		/**
		 * Edit the selected entity instance.<br>
		 * Default key stroke: CTRL-INSERT
		 * @see EntityTablePanel#EntityTablePanel(SwingEntityTableModel, EntityEditPanel)
		 * @see EntityTablePanel#EntityTablePanel(SwingEntityTableModel, EntityEditPanel, Consumer)
		 */
		public static final ControlKey<CommandControl> EDIT = CommandControl.key("edit", keyStroke(VK_INSERT, MENU_SHORTCUT_MASK));
		/**
		 * Select and edit a single attribute value for the selected entity instances.<br>
		 * Default key stroke: SHIFT-INSERT
		 * @see Config#editAttributeSelection(SelectionMode)
		 */
		public static final ControlKey<CommandControl> EDIT_ATTRIBUTE = CommandControl.key("editAttribute", keyStroke(VK_INSERT, SHIFT_DOWN_MASK));
		/**
		 * Requests focus for the table.<br>
		 * Default key stroke: CTRL-T
		 */
		public static final ControlKey<CommandControl> REQUEST_TABLE_FOCUS = CommandControl.key("requestTableFocus", keyStroke(VK_T, MENU_SHORTCUT_MASK));
		/**
		 * Toggles the condition panel between the hidden, simple and advanced views.<br>
		 * Default key stroke: CTRL-ALT-S
		 * @see TableConditionPanel#view()
		 */
		public static final ControlKey<CommandControl> TOGGLE_CONDITION_VIEW = CommandControl.key("toggleConditionView", keyStroke(VK_S, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * Displays a dialog for selecting a column condition panel.<br>
		 * Default key stroke: CTRL-S
		 */
		public static final ControlKey<CommandControl> SELECT_CONDITION = CommandControl.key("selectCondition", keyStroke(VK_S, MENU_SHORTCUT_MASK));
		/**
		 * Toggles the filter panel between the hidden, simple and advanced views.<br>
		 * Default key stroke: CTRL-ALT-F
		 * @see TableConditionPanel#view()
		 */
		public static final ControlKey<CommandControl> TOGGLE_FILTER_VIEW = CommandControl.key("toggleFilterView", keyStroke(VK_F, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * Displays a dialog for selecting a column filter panel.<br>
		 * Default key stroke: CTRL-SHIFT-F
		 */
		public static final ControlKey<CommandControl> SELECT_FILTER = CommandControl.key("selectFilter", keyStroke(VK_F, MENU_SHORTCUT_MASK | SHIFT_DOWN_MASK));
		/**
		 * Decrements the selected indexes, moving the selection up.<br>
		 * Default key stroke: CTRL-SHIFT-UP
		 * @see MultiSelection.Indexes#decrement()
		 */
		public static final ControlKey<CommandControl> DECREMENT_SELECTION = CommandControl.key("decrementSelection", keyStroke(VK_UP, MENU_SHORTCUT_MASK | SHIFT_DOWN_MASK));
		/**
		 * Increments the selected indexes, moving the selection down.<br>
		 * Default key stroke: CTRL-SHIFT-DOWN
		 * @see MultiSelection.Indexes#increment()
		 */
		public static final ControlKey<CommandControl> INCREMENT_SELECTION = CommandControl.key("incrementSelection", keyStroke(VK_DOWN, MENU_SHORTCUT_MASK | SHIFT_DOWN_MASK));
		/**
		 * The main print action<br>
		 * Default key stroke: CTRL-P
		 */
		public static final ControlKey<CommandControl> PRINT = CommandControl.key("print", keyStroke(VK_P, MENU_SHORTCUT_MASK));
		/**
		 * Triggers the {@link ControlKeys#DELETE} control.<br>
		 * Default key stroke: DELETE
		 */
		public static final ControlKey<CommandControl> DELETE = CommandControl.key("delete", keyStroke(VK_DELETE));
		/**
		 * Displays the table popup menu, if one is available.<br>
		 * Default key stroke: CTRL-G
		 */
		public static final ControlKey<CommandControl> DISPLAY_POPUP_MENU = CommandControl.key("displayPopupMenu", keyStroke(VK_G, MENU_SHORTCUT_MASK));
		/**
		 * Displays the query inspector, if one is available.<br>
		 * Default key stroke: CTRL-ALT-Q
		 */
		public static final ControlKey<CommandControl> DISPLAY_QUERY_INSPECTOR = CommandControl.key("displayQueryInspector", keyStroke(VK_Q, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * Displays the entity menu, if one is available.<br>
		 * Default key stroke: CTRL-ALT-V
		 */
		public static final ControlKey<CommandControl> DISPLAY_ENTITY_MENU = CommandControl.key("displayEntityMenu", keyStroke(VK_V, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * A {@link Controls} instance containing controls for printing.
		 */
		public static final ControlKey<Controls> PRINT_CONTROLS = Controls.key("printControls");
		/**
		 * A {@link Controls} instance containing any additional popup menu controls.
		 * @see #addPopupMenuControls(Controls)
		 */
		public static final ControlKey<Controls> ADDITIONAL_POPUP_MENU_CONTROLS = Controls.key("additionalPopupMenuControls");
		/**
		 * A {@link Controls} instance containing any additional toolbar controls.
		 * @see #addToolBarControls(Controls)
		 */
		public static final ControlKey<Controls> ADDITIONAL_TOOLBAR_CONTROLS = Controls.key("additionalToolBarControls");
		/**
		 * A {@link Control} for viewing the dependencies of the selected entities.
		 */
		public static final ControlKey<CommandControl> VIEW_DEPENDENCIES = CommandControl.key("viewDependencies");
		/**
		 * A {@link Controls} instance containing edit controls for all editable attributes.
		 * @see Config#editAttributeSelection(EditAttributeSelection)
		 */
		public static final ControlKey<Controls> EDIT_ATTRIBUTE_CONTROLS = Controls.key("editAttributeControls");
		/**
		 * A {@link Control} for displaying a dialog for selecting the visible table columns.
		 * @see Config#columnSelection(ColumnSelection)
		 */
		public static final ControlKey<CommandControl> SELECT_COLUMNS = CommandControl.key("selectColumns");
		/**
		 * A {@link Controls} instance containing a {@link ToggleControl} for each columns' visibility.
		 * @see Config#columnSelection(ColumnSelection)
		 */
		public static final ControlKey<Controls> TOGGLE_COLUMN_CONTROLS = Controls.key("toggleColumnControls");
		/**
		 * A {@link Control} for resetting the columns to their original visibility and location.
		 */
		public static final ControlKey<CommandControl> RESET_COLUMNS = CommandControl.key("resetColumns");
		/**
		 * A {@link Control} for displaying a dialog for configuring the column auto-resize-mode.
		 * @see JTable#setAutoResizeMode(int)
		 */
		public static final ControlKey<CommandControl> SELECT_AUTO_RESIZE_MODE = CommandControl.key("selectAutoResizeMode");
		/**
		 * A {@link Controls} instance containing a {@link ToggleControl} for each auto-resize-mode.
		 * @see JTable#setAutoResizeMode(int)
		 */
		public static final ControlKey<Controls> TOGGLE_AUTO_RESIZE_MODE_CONTROLS = Controls.key("toggleAutoResizeModeControls");
		/**
		 * A {@link Control} for toggling between single and multi selection mode.
		 */
		public static final ControlKey<ToggleControl> SINGLE_SELECTION = ToggleControl.key("singleSelection");
		/**
		 * A {@link Control} for clearing the data from the table.
		 * @see FilterTableModel.Items#clear()
		 */
		public static final ControlKey<CommandControl> CLEAR = CommandControl.key("clear");
		/**
		 * A {@link Control} for refreshing the table items.<br>
		 * Default key stroke: ALT-R
		 */
		public static final ControlKey<CommandControl> REFRESH = CommandControl.key("refresh", keyStroke(VK_R, ALT_DOWN_MASK));
		/**
		 * A {@link ToggleControl} for showing/hiding the summary panel.
		 */
		public static final ControlKey<ToggleControl> TOGGLE_SUMMARIES = ToggleControl.key("toggleSummaries");
		/**
		 * A {@link Controls} instance containing the condition panel controls.
		 */
		public static final ControlKey<Controls> CONDITION_CONTROLS = Controls.key("conditionControls");
		/**
		 * A {@link Controls} instance containing the filter panel controls.
		 */
		public static final ControlKey<Controls> FILTER_CONTROLS = Controls.key("filterControls");
		/**
		 * A {@link Control} for clearing the table selection.
		 */
		public static final ControlKey<CommandControl> CLEAR_SELECTION = CommandControl.key("clearSelection");
		/**
		 * A {@link Control} for copying the selected cell data.<br>
		 * Default key stroke: CTRL-ALT-C
		 */
		public static final ControlKey<CommandControl> COPY_CELL = CommandControl.key("copyCell", keyStroke(VK_C, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * A {@link Control} for copying the table rows with header.
		 */
		public static final ControlKey<CommandControl> COPY_ROWS = CommandControl.key("copyRows");
		/**
		 * A {@link Control} for exporting the table data.
		 */
		public static final ControlKey<CommandControl> EXPORT_DATA = CommandControl.key("exportData");
		/**
		 * A {@link Controls} instance containing controls for copying either cell or table data.
		 * <ul>
		 * <li>{@link ControlKeys#COPY_CELL ControlKeys#COPY_CELL}
		 * <li>{@link ControlKeys#COPY_ROWS ControlKeys#COPY_ROWS}
		 * <li>{@link ControlKeys#EXPORT_DATA ControlKeys#EXPORT_DATA}
		 * </ul>
		 * @see #COPY_CELL
		 * @see #COPY_ROWS
		 * @see #EXPORT_DATA
		 */
		public static final ControlsKey COPY_CONTROLS = Controls.key("copyControls", Controls.layout(asList(COPY_CELL, COPY_ROWS, EXPORT_DATA)));
		/**
		 * A {@link Controls} instance containing controls for configuring columns.
		 * <ul>
		 * <li>{@link ControlKeys#SELECT_COLUMNS ControlKeys#SELECT_COLUMNS} or {@link ControlKeys#TOGGLE_COLUMN_CONTROLS ControlKeys#TOGGLE_COLUMN_CONTROLS}
		 * <li>{@link ControlKeys#RESET_COLUMNS ControlKeys#RESET_COLUMNS}
		 * <li>{@link ControlKeys#SELECT_AUTO_RESIZE_MODE ControlKeys#SELECT_AUTO_RESIZE_MODE} or {@link ControlKeys#TOGGLE_AUTO_RESIZE_MODE_CONTROLS ControlKeys#TOGGLE_AUTO_RESIZE_MODE_CONTROLS}
		 * </ul>
		 * @see #SELECT_COLUMNS
		 * @see #TOGGLE_COLUMN_CONTROLS
		 * @see #RESET_COLUMNS
		 * @see #SELECT_AUTO_RESIZE_MODE
		 * @see #TOGGLE_AUTO_RESIZE_MODE_CONTROLS
		 */
		public static final ControlsKey COLUMN_CONTROLS = Controls.key("columnControls", Controls.layout(asList(SELECT_COLUMNS, TOGGLE_COLUMN_CONTROLS, RESET_COLUMNS, SELECT_AUTO_RESIZE_MODE)));
		/**
		 * Requests focus for the table search field.<br>
		 * Default key stroke: CTRL-F
		 */
		public static final ControlKey<CommandControl> REQUEST_SEARCH_FIELD_FOCUS = CommandControl.key("requestSearchFieldFocus", keyStroke(VK_F, MENU_SHORTCUT_MASK));

		private ControlKeys() {}
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
	 * Specifies how a selection is presented.
	 */
	public enum SelectionMode {
		/**
		 * Display a dialog.
		 */
		DIALOG,
		/**
		 * Display a menu.
		 */
		MENU
	}

	private static final int FONT_SIZE_TO_ROW_HEIGHT = 4;
	private static final Consumer<Config> NO_CONFIGURATION = c -> {};

	private final State summaryPanelVisibleState = State.state(Config.SUMMARY_PANEL_VISIBLE.getOrThrow());

	private final FilterTable<Entity, Attribute<?>> table;
	private final JScrollPane tableScrollPane = new JScrollPane();
	private final TablePanel tablePanel = new TablePanel();
	private final @Nullable EntityEditPanel editPanel;
	private final @Nullable TableConditionPanel<Attribute<?>> tableConditionPanel;
	private final Controls.Layout popupMenuLayout;
	private final Controls.Layout toolBarLayout;
	private final SwingEntityTableModel tableModel;
	private final @Nullable EntityTableExportPanel exportPanel;
	private final Control conditionRefreshControl;
	private final JToolBar refreshButtonToolBar;
	private final List<Controls> additionalPopupControls = new ArrayList<>();
	private final List<Controls> additionalToolBarControls = new ArrayList<>();

	private final Map<EntityType, EntityTablePanelPreferences> dependencyPanelPreferences = new HashMap<>();
	private final AtomicReference<Dimension> dependenciesDialogSize = new AtomicReference<>();

	private @Nullable JScrollPane conditionPanelScrollPane;
	private @Nullable JScrollPane filterPanelScrollPane;
	private @Nullable StatusPanel statusPanel;
	private @Nullable FilterTableColumnComponentPanel<Attribute<?>> summaryPanel;
	private @Nullable JScrollPane summaryPanelScrollPane;
	private @Nullable SelectQueryInspector queryInspector;

	final Config configuration;

	private boolean initialized = false;

	/**
	 * Instantiates a new EntityTablePanel instance
	 * @param tableModel the SwingEntityTableModel instance
	 */
	public EntityTablePanel(SwingEntityTableModel tableModel) {
		this(tableModel, NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityTablePanel instance
	 * @param tableModel the SwingEntityTableModel instance
	 * @param config provides access to the table panel configuration
	 */
	public EntityTablePanel(SwingEntityTableModel tableModel, Consumer<Config> config) {
		this.tableModel = requireNonNull(tableModel);
		this.editPanel = null;
		this.conditionRefreshControl = createConditionRefreshControl();
		this.configuration = configure(config);
		this.table = configuration.buildTable();
		this.tableConditionPanel = createTableConditionPanel();
		this.refreshButtonToolBar = createRefreshButtonToolBar();
		this.popupMenuLayout = createPopupMenuLayout();
		this.toolBarLayout = createToolBarLayout();
		this.exportPanel = createExportPanel();
		initializeConditionsAndFilters();
		createControls();
		configureExcludedColumns();
	}

	/**
	 * Instantiates a new EntityTablePanel instance
	 * @param tableModel the SwingEntityTableModel instance
	 * @param editPanel the edit panel
	 */
	public EntityTablePanel(SwingEntityTableModel tableModel, EntityEditPanel editPanel) {
		this(tableModel, editPanel, NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityTablePanel instance
	 * @param tableModel the SwingEntityTableModel instance
	 * @param editPanel the edit panel
	 * @param config provides access to the table panel configuration
	 */
	public EntityTablePanel(SwingEntityTableModel tableModel, EntityEditPanel editPanel, Consumer<Config> config) {
		this.tableModel = requireNonNull(tableModel);
		this.editPanel = validateEditModel(requireNonNull(editPanel));
		this.conditionRefreshControl = createConditionRefreshControl();
		this.configuration = configure(config);
		this.table = configuration.buildTable();
		this.tableConditionPanel = createTableConditionPanel();
		this.refreshButtonToolBar = createRefreshButtonToolBar();
		this.popupMenuLayout = createPopupMenuLayout();
		this.toolBarLayout = createToolBarLayout();
		this.exportPanel = createExportPanel();
		initializeConditionsAndFilters();
		createControls();
		configureExcludedColumns();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(conditionPanelScrollPane, filterPanelScrollPane, tableConditionPanel, editPanel, queryInspector, exportPanel);
	}

	/**
	 * @return the table
	 * @throws IllegalStateException in case this method is called during configuration
	 */
	public final FilterTable<Entity, Attribute<?>> table() {
		if (table == null) {
			throw new IllegalStateException("The table is not available until configuration has finished");
		}

		return table;
	}

	/**
	 * @return the EntityTableModel used by this EntityTablePanel
	 */
	public final SwingEntityTableModel tableModel() {
		return tableModel;
	}

	/**
	 * @return the condition panel
	 * @throws IllegalStateException in case a condition panel is not available
	 * @see Config#includeConditions(boolean)
	 */
	public final TableConditionPanel<Attribute<?>> condition() {
		if (tableConditionPanel == null) {
			throw new IllegalStateException("No condition panel is available");
		}

		return tableConditionPanel;
	}

	/**
	 * @return the {@link State} controlling whether the summary panel is visible
	 */
	public final State summaryPanelVisible() {
		return summaryPanelVisibleState;
	}

	/**
	 * @param additionalPopupMenuControls a set of controls to add to the table popup menu
	 * @throws IllegalStateException in case this panel has already been initialized
	 */
	public final void addPopupMenuControls(Controls additionalPopupMenuControls) {
		throwIfInitialized();
		this.additionalPopupControls.add(requireNonNull(additionalPopupMenuControls));
	}

	/**
	 * @param additionalToolBarControls a set of controls to add to the table toolbar menu
	 * @throws IllegalStateException in case this panel has already been initialized
	 */
	public final void addToolBarControls(Controls additionalToolBarControls) {
		throwIfInitialized();
		this.additionalToolBarControls.add(requireNonNull(additionalToolBarControls));
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + ": " + tableModel.entityType();
	}

	/**
	 * Returns a {@link Value} containing the control associated with {@code controlKey},
	 * an empty {@link Value} if no such control is available.
	 * @param <T> the control type
	 * @param controlKey the control key
	 * @return the {@link Value} containing the control associated with {@code controlKey}
	 */
	public final <T extends Control> Value<T> control(ControlKey<T> controlKey) {
		return configuration.controlMap.control(requireNonNull(controlKey));
	}

	/**
	 * Displays a selection dialog for selecting an attribute to edit and
	 * retrieves a new value via input dialog and performs an update on the selected entities
	 * assigning the value to the attribute
	 * @see Config#editComponentFactory(Attribute, EditComponentFactory)
	 */
	public final void editSelected() {
		List<AttributeDefinition<?>> sortedDefinitions = configuration.editable.get().stream()
						.map(attribute -> tableModel.entityDefinition().attributes().definition(attribute))
						.sorted(new AttributeDefinitionComparator())
						.collect(toList());
		Dialogs.select()
						.list(sortedDefinitions)
						.owner(this)
						.select()
						.single()
						.map(AttributeDefinition::attribute)
						.ifPresent(this::editSelected);
	}

	/**
	 * Retrieves a new value via input dialog and performs an update on the selected entities
	 * assigning the value to the attribute
	 * @param attributeToEdit the attribute which value to edit
	 * @see Config#editComponentFactory(Attribute, EditComponentFactory)
	 */
	public final void editSelected(Attribute<?> attributeToEdit) {
		requireNonNull(attributeToEdit);
		if (!tableModel.selection().empty().is()) {
			editDialogBuilder(attributeToEdit)
							.edit(tableModel.selection().items().get());
		}
	}

	/**
	 * Displays a dialog containing tables of entities depending on the selected entities via non-soft foreign keys
	 */
	public final void viewDependencies() {
		if (!tableModel.selection().empty().is()) {
			displayDependencies(false);
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
	 * Writes user preferences.
	 * <p>Remember to call {@code super.writePreferences(preferences)} when overriding.
	 * @param preferences the preferences instance to write to
	 * @see #preferencesKey()
	 */
	public void writePreferences(Preferences preferences) {
		requireNonNull(preferences);
		new EntityTablePanelPreferences(this).save(preferences);
	}

	/**
	 * Applies any user preferences previously written via {@link #writePreferences(Preferences)}
	 * <p>Remember to call {@code super.applyPreferences(preferences)} when overriding.
	 * @param preferences the preferences instance containing the preferences to apply
	 */
	public void applyPreferences(Preferences preferences) {
		requireNonNull(preferences);
		new EntityTablePanelPreferences(this, preferences).apply(this);
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
				addDoubleClickAction();
				layoutPanel(tablePanel, configuration.includeSouthPanel ? initializeSouthPanel() : null);
				setSummaryPanelVisible(summaryPanelVisibleState.is());
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
	 * Override to set up any custom controls. This default implementation is empty.
	 * This method is called after all standard controls have been initialized.
	 * @see #control(ControlKey)
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

	/**
	 * Sets up the keyboard shortcuts.
	 * @see ControlKeys#REFRESH
	 * @see ControlKeys#REQUEST_TABLE_FOCUS
	 * @see ControlKeys#SELECT_CONDITION
	 * @see ControlKeys#TOGGLE_CONDITION_VIEW
	 * @see ControlKeys#SELECT_FILTER
	 * @see ControlKeys#TOGGLE_FILTER_VIEW
	 * @see ControlKeys#PRINT
	 * @see ControlKeys#ADD
	 * @see ControlKeys#EDIT
	 * @see ControlKeys#EDIT_ATTRIBUTE
	 * @see ControlKeys#DELETE
	 * @see ControlKeys#DECREMENT_SELECTION
	 * @see ControlKeys#INCREMENT_SELECTION
	 * @see ControlKeys#DISPLAY_ENTITY_MENU
	 * @see ControlKeys#DISPLAY_POPUP_MENU
	 */
	protected void setupKeyboardActions() {
		configuration.controlMap.keyEvent(REFRESH).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(REQUEST_TABLE_FOCUS).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(SELECT_CONDITION).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(TOGGLE_CONDITION_VIEW).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(TOGGLE_FILTER_VIEW).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(SELECT_FILTER).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(PRINT).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(ADD).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(EDIT).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(EDIT_ATTRIBUTE).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(DELETE).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(DECREMENT_SELECTION).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(INCREMENT_SELECTION).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(DISPLAY_QUERY_INSPECTOR).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(DISPLAY_ENTITY_MENU).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(DISPLAY_POPUP_MENU).ifPresent(keyEvent -> keyEvent.enable(table));
	}

	/**
	 * Configures the toolbar controls layout.<br>
	 * Note that the {@link Controls.Layout} instance has pre-configured defaults,
	 * which must be cleared in order to start with an empty configuration.
	 * {@snippet :
	 *   configureToolBar(layout -> layout.clear()
	 *           .control(ControlKeys.REFRESH)
	 *           .separator()
	 *           .control(createCustomControl())
	 *           .separator()
	 *           .defaults())
	 *}
	 * Defaults:
	 * <ul>
	 *   <li>{@link ControlKeys#TOGGLE_SUMMARIES ControlKeys#TOGGLE_SUMMARY_PANEL}
	 * 	 <li>{@link ControlKeys#TOGGLE_CONDITION_VIEW ControlKeys#TOGGLE_CONDITION_VIEW}
	 * 	 <li>{@link ControlKeys#TOGGLE_FILTER_VIEW ControlKeys#TOGGLE_FILTER_VIEW}
	 * 	 <li>Separator
	 * 	 <li>{@link ControlKeys#ADD ControlKeys#ADD} (If an EditPanel is available)
	 * 	 <li>{@link ControlKeys#EDIT ControlKeys#EDIT} (If an EditPanel is available)
	 * 	 <li>{@link ControlKeys#DELETE ControlKeys#DELETE}
	 * 	 <li>Separator
	 * 	 <li>{@link ControlKeys#EDIT_ATTRIBUTE ControlKeys#EDIT_ATTRIBUTE}
	 * 	 <li>Separator
	 * 	 <li>{@link ControlKeys#PRINT ControlKeys#PRINT}
	 * 	 <li>Separator
	 * 	 <li>{@link ControlKeys#ADDITIONAL_TOOLBAR_CONTROLS ControlKeys#ADDITIONAL_TOOLBAR_CONTROLS}
	 * </ul>
	 * @param toolBarLayout provides access to the toolbar configuration
	 * @see Controls.Layout#clear()
	 */
	protected final void configureToolBar(Consumer<Controls.Layout> toolBarLayout) {
		throwIfInitialized();
		requireNonNull(toolBarLayout).accept(this.toolBarLayout);
	}

	/**
	 * Configures the popup menu controls layout.<br>
	 * Note that the {@link Controls.Layout} instance has pre-configured defaults,
	 * which must be cleared in order to start with an empty configuration.
	 * {@snippet :
	 *   configurePopupMenu(layout -> layout.clear()
	 *           .control(ControlKeys.REFRESH)
	 *           .separator()
	 *           .control(createCustomControl())
	 *           .separator()
	 *           .defaults())
	 *}
	 * Defaults:
	 * <ul>
	 *   <li>{@link ControlKeys#REFRESH ControlKeys#REFRESH}
	 *   <li>{@link ControlKeys#CLEAR ControlKeys#CLEAR}
	 *   <li>Separator
	 *   <li>{@link ControlKeys#ADD ControlKeys#ADD} (If an EditPanel is available)
	 *   <li>{@link ControlKeys#EDIT ControlKeys#EDIT} (If an EditPanel is available)
	 *   <li>{@link ControlKeys#DELETE ControlKeys#DELETE}
	 *   <li>Separator
	 *   <li>{@link ControlKeys#EDIT_ATTRIBUTE ControlKeys#EDIT_ATTRIBUTE} or {@link ControlKeys#EDIT_ATTRIBUTE_CONTROLS ControlKeys#EDIT_ATTRIBUTE_CONTROLS}
	 *   <li>Separator
	 *   <li>{@link ControlKeys#VIEW_DEPENDENCIES ControlKeys#VIEW_DEPENDENCIES}
	 *   <li>Separator
	 *   <li>{@link ControlKeys#ADDITIONAL_POPUP_MENU_CONTROLS ControlKeys#ADDITIONAL_POPUP_MENU_CONTROLS}
	 *   <li>Separator
	 *   <li>{@link ControlKeys#PRINT_CONTROLS ControlKeys#PRINT_CONTROLS}
	 *   <li>Separator
	 *   <li>{@link ControlKeys#COLUMN_CONTROLS ControlKeys#COLUMN_CONTROLS}
	 *   <li>Separator
	 *   <li>{@link ControlKeys#SINGLE_SELECTION ControlKeys#SINGLE_SELECTION}
	 *   <li>Separator
	 *   <li>{@link ControlKeys#CONDITION_CONTROLS ControlKeys#CONDITION_CONTROLS}
	 *   <li>Separator
	 *   <li>{@link ControlKeys#FILTER_CONTROLS ControlKeys#FILTER_CONTROLS}
	 *   <li>Separator
	 *   <li>{@link ControlKeys#COPY_CONTROLS ControlKeys#COPY_CONTROLS}
	 * </ul>
	 * @param popupMenuLayout provides access to the popup menu layout
	 * @see Controls.Layout#clear()
	 */
	protected final void configurePopupMenu(Consumer<Controls.Layout> popupMenuLayout) {
		throwIfInitialized();
		requireNonNull(popupMenuLayout).accept(this.popupMenuLayout);
	}

	/**
	 * @return the edit panel
	 * @throws IllegalStateException in case no edit panel is available
	 */
	protected final EntityEditPanel editPanel() {
		if (editPanel == null) {
			throw new IllegalStateException("No editPanel is available");
		}

		return editPanel;
	}

	/**
	 * This method simply adds {@code tablePanel} at location BorderLayout.CENTER and,
	 * if non-null, the given {@code southPanel} to the {@code BorderLayout.SOUTH} location.
	 * By overriding this method you can override the default layout.
	 * @param tableComponent the component containing the table, condition and summary panel
	 * @param southPanel the south toolbar panel, null if not required
	 * @see #initializeSouthPanel()
	 */
	protected void layoutPanel(JComponent tableComponent, @Nullable JPanel southPanel) {
		requireNonNull(tableComponent);
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
	 * the dependencies of the entities involved are displayed to the user, otherwise {@link #displayException(Exception)} is called.
	 * @param exception the exception
	 * @see Config#referentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling)
	 */
	protected void onReferentialIntegrityException(ReferentialIntegrityException exception) {
		requireNonNull(exception);
		if (configuration.referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES) {
			displayDependencies(true);
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
	protected <T> EditAttributeDialogBuilder<T> editDialogBuilder(Attribute<T> attribute) {
		return editAttributeDialog(tableModel.editModel(), attribute)
						.owner(this)
						.editComponentFactory((EditComponentFactory<?, T>) configuration.editComponentFactories
										.getOrDefault(attribute, new DefaultEditComponentFactory<>(attribute)));
	}

	/**
	 * Returns the key used to identify user preferences for this table panel, that is column positions, widths and such.
	 * The default implementation is:
	 * {@snippet :
	 * return tableModel().getClass().getSimpleName() + "-" + tableModel().entityType();
	 *}
	 * Override in case this key is not unique within the application.
	 * @return the key used to identify user preferences for this table panel
	 */
	protected String preferencesKey() {
		return tableModel.getClass().getSimpleName() + "-" + tableModel.entityType();
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
		Dialogs.displayException(exception, parentWindow(focusOwner));
	}

	/**
	 * @return true if confirmed
	 * @see Config#deleteConfirmer(Confirmer)
	 */
	protected final boolean confirmDelete() {
		return configuration.deleteConfirmer.confirm(this);
	}

	@Nullable EntityTableExportPanel exportPanel() {
		return exportPanel;
	}

	final void writeLegacyPreferences() {
		new EntityTablePanelPreferences(this).saveLegacy();
	}

	final void applyLegacyPreferences() {
		EntityTablePanelPreferences.applyLegacy(this);
	}

	/**
	 * Creates a {@link Control} for adding a new entity via the available edit panel.
	 * @return the add control
	 */
	private CommandControl createAddControl() {
		return Control.builder()
						.command(new AddCommand())
						.caption(FrameworkMessages.add())
						.mnemonic(FrameworkMessages.addMnemonic())
						.icon(ICONS.add())
						.description(FrameworkMessages.addTip())
						.build();
	}

	/**
	 * Creates a {@link Control} for editing the selected entity via the edit panel.
	 * @return the edit control
	 */
	private CommandControl createEditControl() {
		return Control.builder()
						.command(new EditCommand())
						.caption(FrameworkMessages.edit())
						.enabled(tableModel().selection().single())
						.mnemonic(FrameworkMessages.editMnemonic())
						.icon(ICONS.edit())
						.description(FrameworkMessages.editSelectedTip())
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
	private CommandControl createEditAttributeControl() {
		return Control.builder()
						.command(this::editSelected)
						.caption(FrameworkMessages.edit())
						.enabled(createEditAttributeEnabledState())
						.mnemonic(FrameworkMessages.editMnemonic())
						.icon(ICONS.edit())
						.description(FrameworkMessages.editValueTip())
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
	private @Nullable Controls createEditAttributeControls() {
		ObservableState enabled = createEditAttributeEnabledState();
		ControlsBuilder builder = Controls.builder()
						.caption(FrameworkMessages.edit())
						.enabled(enabled)
						.icon(ICONS.edit())
						.description(FrameworkMessages.editValueTip());
		configuration.editable.get().stream()
						.map(attribute -> tableModel.entityDefinition().attributes().definition(attribute))
						.sorted(new AttributeDefinitionComparator())
						.forEach(definition ->
										builder.control(createEditAttributeControl(definition, enabled)));
		Controls editControls = builder.build();

		return editControls.size() == 0 ? null : editControls;
	}

	private ObservableState createEditAttributeEnabledState() {
		ObservableState selectionNotEmpty = tableModel.selection().empty().not();
		ObservableState updateEnabled = tableModel.editModel().settings().updateEnabled();
		ObservableState updateMultipleEnabledOrSingleSelection =
						State.or(tableModel.editModel().settings().updateMultipleEnabled(),
										tableModel.selection().single());

		return State.and(selectionNotEmpty, updateEnabled, updateMultipleEnabledOrSingleSelection);
	}

	private Control createEditAttributeControl(AttributeDefinition<?> definition, ObservableState enabled) {
		return Control.builder()
						.command(new EditAttributeCommand(definition.attribute()))
						.caption(definition.caption())
						.enabled(enabled)
						.build();
	}

	/**
	 * @return a control for showing the dependencies dialog
	 */
	private CommandControl createViewDependenciesControl() {
		return Control.builder()
						.command(this::viewDependencies)
						.caption(FrameworkMessages.dependencies())
						.enabled(tableModel.selection().empty().not())
						.description(FrameworkMessages.dependenciesTip())
						.icon(ICONS.dependencies())
						.build();
	}

	/**
	 * @return a control for deleting the selected entities
	 * @throws IllegalStateException in case the underlying model is read only or if deleting is not enabled
	 */
	private CommandControl createDeleteControl() {
		return Control.builder()
						.command(new DeleteCommand())
						.caption(FrameworkMessages.delete())
						.enabled(State.and(
										tableModel.editModel().settings().deleteEnabled(),
										tableModel.selection().empty().not()))
						.description(FrameworkMessages.deleteSelectedTip())
						.icon(ICONS.delete())
						.build();
	}

	/**
	 * @return a Control for refreshing the underlying table data
	 */
	private CommandControl createRefreshControl() {
		return Control.builder()
						.command(tableModel.items()::refresh)
						.caption(Messages.refresh())
						.description(Messages.refreshTip())
						.mnemonic(Messages.refreshMnemonic())
						.icon(ICONS.refresh())
						.enabled(tableModel.items().refresher().active().not())
						.build();
	}

	/**
	 * @return a Control for clearing the underlying table model, that is, removing all rows
	 */
	private CommandControl createClearControl() {
		return Control.builder()
						.command(tableModel.items()::clear)
						.caption(Messages.clear())
						.description(Messages.clearTip())
						.mnemonic(Messages.clearMnemonic())
						.icon(ICONS.clear())
						.build();
	}

	private @Nullable Controls createPrintControls() {
		ControlsBuilder builder = Controls.builder()
						.caption(Messages.print())
						.mnemonic(Messages.printMnemonic())
						.icon(ICONS.print());
		control(PRINT).optional().ifPresent(builder::control);

		Controls printControls = builder.build();

		return printControls.size() == 0 ? null : printControls;
	}

	private @Nullable Controls createAdditionalPopupControls() {
		ControlsBuilder builder = Controls.builder();
		additionalPopupControls.forEach(additionalControls -> {
			if (!additionalControls.caption().isPresent()) {
				builder.actions(additionalControls.actions());
			}
			else {
				builder.control(additionalControls);
			}
		});
		Controls additionalControls = builder.build();

		return additionalControls.size() == 0 ? null : additionalControls;
	}

	private @Nullable Controls createAdditionalToolbarControls() {
		ControlsBuilder builder = Controls.builder();
		additionalToolBarControls.forEach(additionalControls -> {
			if (!additionalControls.caption().isPresent()) {
				builder.actions(additionalControls.actions());
			}
			else {
				builder.control(additionalControls);
			}
		});
		Controls additionalControls = builder.build();

		return additionalControls.size() == 0 ? null : additionalControls;
	}

	private CommandControl createToggleConditionViewControl() {
		return Control.builder()
						.command(this::toggleConditionView)
						.icon(ICONS.search())
						.description(MESSAGES.getString("show_condition_panel"))
						.build();
	}

	private CommandControl createSelectConditionControl() {
		return command(() -> condition().select(this));
	}

	private @Nullable Controls createConditionControls() {
		if (!configuration.includeConditions || tableConditionPanel == null) {
			return null;
		}

		return tableConditionPanel.controls().copy()
						.caption(FrameworkMessages.searchNoun())
						.icon(ICONS.search())
						.separator()
						.control(Control.builder()
										.toggle(tableModel.queryModel().conditionRequired())
										.caption(MESSAGES.getString("condition_required"))
										.description(MESSAGES.getString("condition_required_description")))
						.build();
	}

	private CommandControl createToggleFilterViewControl() {
		return Control.builder()
						.command(this::toggleFilterView)
						.icon(ICONS.filter())
						.description(MESSAGES.getString("show_filter_panel"))
						.build();
	}

	private CommandControl createSelectFilterControl() {
		return command(() -> table.filters().select(this));
	}

	private void toggleConditionView() {
		toggleView(condition().view(), conditionPanelScrollPane);
	}

	private void toggleFilterView() {
		toggleView(table.filters().view(), filterPanelScrollPane);
	}

	private void toggleView(Value<ConditionView> conditionView, JScrollPane conditionScrollPane) {
		switch (conditionView.getOrThrow()) {
			case HIDDEN:
				conditionView.set(SIMPLE);
				break;
			case SIMPLE:
				conditionView.set(ADVANCED);
				break;
			case ADVANCED:
				setConditionViewHidden(conditionScrollPane, conditionView);
				break;
		}
	}

	private @Nullable Controls createFilterControls() {
		if (!configuration.includeFilters) {
			return null;
		}
		ControlsBuilder builder = Controls.builder()
						.caption(FrameworkMessages.filterNoun())
						.icon(ICONS.filter());
		Controls filterPanelControls = table.filters().controls();
		if (filterPanelControls.size() > 0) {
			builder.actions(filterPanelControls.actions());
		}
		Controls filterControls = builder.build();

		return filterControls.size() == 0 ? null : filterControls;
	}

	private ToggleControl createToggleSummariesControl() {
		return Control.builder()
						.toggle(summaryPanelVisibleState)
						.icon(ICONS.summary())
						.description(MESSAGES.getString("toggle_summary_tip"))
						.build();
	}

	private CommandControl createClearSelectionControl() {
		return Control.builder()
						.command(tableModel.selection()::clear)
						.enabled(tableModel.selection().empty().not())
						.icon(ICONS.clearSelection())
						.description(MESSAGES.getString("clear_selection_tip"))
						.build();
	}

	private CommandControl createIncrementSelectionControl() {
		return Control.builder()
						.command(tableModel.selection().indexes()::increment)
						.icon(ICONS.down())
						.description(MESSAGES.getString("increment_selection_tip"))
						.build();
	}

	private CommandControl createDecrementSelectionControl() {
		return Control.builder()
						.command(tableModel.selection().indexes()::decrement)
						.icon(ICONS.up())
						.description(MESSAGES.getString("decrement_selection_tip"))
						.build();
	}

	private CommandControl createRequestTableFocusControl() {
		return command(table::requestFocus);
	}

	private CommandControl createRequestSearchFieldFocusControl() {
		return command(table.searchField()::requestFocusInWindow);
	}

	private @Nullable Controls createColumnControls() {
		ControlsBuilder builder = Controls.builder()
						.caption(MESSAGES.getString("columns"))
						.icon(ICONS.columns());
		if (configuration.columnSelection == SelectionMode.DIALOG) {
			control(SELECT_COLUMNS).optional().ifPresent(builder::control);
		}
		else {
			control(TOGGLE_COLUMN_CONTROLS).optional().ifPresent(builder::control);
		}
		control(RESET_COLUMNS).optional().ifPresent(builder::control);
		if (configuration.autoResizeModeSelection == SelectionMode.DIALOG) {
			control(SELECT_AUTO_RESIZE_MODE).optional().ifPresent(builder::control);
		}
		else {
			control(TOGGLE_AUTO_RESIZE_MODE_CONTROLS).optional().ifPresent(builder::control);
		}

		Controls columnControls = builder.build();

		return columnControls.size() == 0 ? null : columnControls;
	}

	private @Nullable Controls createCopyControls() {
		ControlsBuilder builder = Controls.builder()
						.caption(Messages.copy())
						.icon(ICONS.copy());
		control(COPY_CELL).optional().ifPresent(builder::control);
		control(COPY_ROWS).optional().ifPresent(builder::control);
		control(EXPORT_DATA).optional().ifPresent(builder::control);

		Controls copyControls = builder.build();

		return copyControls.size() == 0 ? null : copyControls;
	}

	private CommandControl createCopyRowsControl() {
		return Control.builder()
						.command(table::copyToClipboard)
						.caption(FrameworkMessages.copyTableWithHeader())
						.build();
	}

	private CommandControl createExportControl() {
		return Control.builder()
						.command(this::export)
						.caption(MESSAGES.getString("export_data") + "...")
						.build();
	}

	private void export() {
		exportPanel.export(this);
	}

	private boolean includeAddControl() {
		return editPanel != null && configuration.includeAddControl &&
						!tableModel.editModel().settings().readOnly().is() &&
						tableModel.editModel().settings().insertEnabled().is();
	}

	private boolean includeEditControl() {
		return editPanel != null && updatable() &&
						configuration.includeEditControl;
	}

	private boolean includeEditAttributeControls() {
		return !configuration.editable.isEmpty() && updatable() &&
						configuration.includeEditAttributeControl;
	}

	private boolean updatable() {
		return !tableModel.editModel().settings().readOnly().is() &&
						tableModel.editModel().settings().updateEnabled().is();
	}

	private boolean includeDeleteControl() {
		return !tableModel.editModel().settings().readOnly().is() && tableModel.editModel().settings().deleteEnabled().is();
	}

	private boolean includeViewDependenciesControl() {
		return tableModel.entities().definitions().stream()
						.flatMap(entityDefinition -> entityDefinition.foreignKeys().definitions().stream())
						.filter(foreignKeyDefinition -> !foreignKeyDefinition.soft())
						.anyMatch(foreignKeyDefinition -> foreignKeyDefinition.attribute().referencedType().equals(tableModel.entityType()));
	}

	private boolean includeToggleSummaryPanelControl() {
		return configuration.includeSummaries && containsSummaryModels(table);
	}

	private Control createConditionRefreshControl() {
		return Control.builder()
						.command(tableModel.items()::refresh)
						.enabled(tableModel.queryModel().conditionChanged())
						.icon(ICONS.refresh())
						.build();
	}

	private JToolBar createRefreshButtonToolBar() {
		KeyEvents.builder()
						.keyCode(VK_F5)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(conditionRefreshControl)
						.enable(this);

		return toolBar()
						.action(conditionRefreshControl)
						.floatable(false)
						.rollover(false)
						.visible(configuration.refreshButtonVisible == RefreshButtonVisible.ALWAYS ||
										(tableConditionPanel != null && tableConditionPanel.view().isNot(HIDDEN)))
						.build();
	}

	private @Nullable EntityTableExportPanel createExportPanel() {
		if (!configuration.includeExport) {
			return null;
		}

		return new EntityTableExportPanel(tableModel, new EntityTableExportModel(tableModel, table.columnModel()));
	}

	private @Nullable TableConditionPanel<Attribute<?>> createTableConditionPanel() {
		if (!configuration.includeConditions) {
			return null;
		}
		TableConditionPanel<Attribute<?>> conditionPanel = configuration.conditionPanelFactory
						.create(tableModel.queryModel().condition().conditionModel(), createConditionPanels(),
										table.columnModel(), this::configureTableConditionPanel);
		KeyEvents.builder()
						.keyCode(VK_ENTER)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(conditionRefreshControl)
						.enable(conditionPanel);
		conditionPanel.view().addConsumer(tablePanel::conditionViewChanged);

		return conditionPanel;
	}

	private Map<Attribute<?>, ConditionPanel<?>> createConditionPanels() {
		Map<Attribute<?>, ConditionPanel<?>> conditionPanels = new HashMap<>();
		for (Map.Entry<Attribute<?>, ConditionModel<?>> conditionEntry : tableModel.queryModel().condition().get().entrySet()) {
			Attribute<?> attribute = conditionEntry.getKey();
			if (table.columnModel().contains(attribute)) {
				ComponentFactory componentFactory = configuration.conditionComponentFactories.getOrDefault(attribute,
								new EntityConditionComponentFactory(tableModel.entityDefinition(), attribute));
				if (componentFactory.supportsType(attribute.type().valueClass())) {
					conditionPanels.put(attribute, createConditionPanel(conditionEntry.getValue(), attribute, componentFactory));
				}
			}
		}

		return conditionPanels;
	}

	private <C extends Attribute<?>> ColumnConditionPanel<?> createConditionPanel(ConditionModel<?> conditionModel, C identifier,
																																								ComponentFactory componentFactory) {
		return ColumnConditionPanel.builder()
						.model(conditionModel)
						.componentFactory(componentFactory)
						.tableColumn(table.columnModel().column(identifier))
						.build();
	}

	private void configureTableConditionPanel(TableConditionPanel<Attribute<?>> tableConditionPanel) {
		tableConditionPanel.panels().forEach(this::configureConditionPanel);
	}

	private void configureConditionPanel(Attribute<?> attribute, ConditionPanel<?> conditionPanel) {
		conditionPanel.focusGained().ifPresent(focusGained ->
						focusGained.addListener(() -> table.scrollToColumn(attribute)));
		conditionPanel.components().forEach(this::enableConditionPanelRefreshOnEnter);
	}

	private void configureExcludedColumns() {
		if (configuration.excludeHiddenColumns) {
			ValueSet<Attribute<?>> exclude = tableModel.queryModel().attributes().exclude();
			table.columnModel().hidden().addConsumer(exclude::set);
			exclude.set(table.columnModel().hidden().get());
		}
	}

	private void bindEvents() {
		summaryPanelVisibleState.addConsumer(this::setSummaryPanelVisible);
		tableModel.queryModel().condition().changed().addListener(this::onConditionChanged);
		tableModel.editModel().afterInsertUpdateOrDelete().addListener(table::repaint);
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
		tablePanel.initialize();
		table.columnModel().columns().forEach(this::configureColumn);
		summaryPanelVisibleState.addValidator(new ComponentAvailableValidator(summaryPanel, "summary"));
	}

	private void initializeConditionsAndFilters() {
		if (tableConditionPanel != null) {
			tableConditionPanel.view().set(configuration.conditionView);
		}
		if (configuration.includeFilters) {
			table().filters().view().set(configuration.filterView);
		}
	}

	private void createControls() {
		Value.Validator<Control> controlValueValidator = control -> {
			if (initialized) {
				throw new IllegalStateException("Controls must be configured before the panel is initialized");
			}
		};
		ControlMap controlMap = configuration.controlMap;
		controlMap.controls().forEach(control -> control.addValidator(controlValueValidator));
		if (includeDeleteControl()) {
			controlMap.control(DELETE).set(createDeleteControl());
		}
		if (includeAddControl()) {
			controlMap.control(ADD).set(createAddControl());
		}
		if (includeEditControl()) {
			controlMap.control(EDIT).set(createEditControl());
		}
		if (includeEditAttributeControls()) {
			controlMap.control(EDIT_ATTRIBUTE_CONTROLS).set(createEditAttributeControls());
			controlMap.control(EDIT_ATTRIBUTE).set(createEditAttributeControl());
		}
		if (configuration.includeClearControl) {
			controlMap.control(CLEAR).set(createClearControl());
		}
		controlMap.control(REFRESH).set(createRefreshControl());
		controlMap.control(SELECT_COLUMNS).set(table.createSelectColumnsControl());
		controlMap.control(TOGGLE_COLUMN_CONTROLS).set(table.createToggleColumnsControls());
		controlMap.control(RESET_COLUMNS).set(table.createResetColumnsControl());
		controlMap.control(SELECT_AUTO_RESIZE_MODE).set(table.createSelectAutoResizeModeControl());
		controlMap.control(TOGGLE_AUTO_RESIZE_MODE_CONTROLS).set(table.createToggleAutoResizeModeControls());
		if (includeViewDependenciesControl()) {
			controlMap.control(VIEW_DEPENDENCIES).set(createViewDependenciesControl());
		}
		if (includeToggleSummaryPanelControl()) {
			controlMap.control(TOGGLE_SUMMARIES).set(createToggleSummariesControl());
		}
		if (configuration.includeConditions) {
			controlMap.control(TOGGLE_CONDITION_VIEW).set(createToggleConditionViewControl());
			controlMap.control(SELECT_CONDITION).set(createSelectConditionControl());
		}
		if (configuration.includeFilters) {
			controlMap.control(TOGGLE_FILTER_VIEW).set(createToggleFilterViewControl());
			controlMap.control(SELECT_FILTER).set(createSelectFilterControl());
		}
		controlMap.control(CLEAR_SELECTION).set(createClearSelectionControl());
		controlMap.control(DECREMENT_SELECTION).set(createDecrementSelectionControl());
		controlMap.control(INCREMENT_SELECTION).set(createIncrementSelectionControl());
		controlMap.control(COPY_CELL).set(table.createCopyCellControl());
		controlMap.control(COPY_ROWS).set(createCopyRowsControl());
		if (configuration.includeExport) {
			controlMap.control(EXPORT_DATA).set(createExportControl());
		}
		if (configuration.includeEntityMenu) {
			controlMap.control(DISPLAY_ENTITY_MENU).set(command(this::showEntityMenu));
		}
		if (configuration.includeQueryInspector) {
			controlMap.control(DISPLAY_QUERY_INSPECTOR).set(command(this::showQueryInspector));
		}
		if (configuration.includePopupMenu) {
			controlMap.control(DISPLAY_POPUP_MENU).set(command(this::showPopupMenu));
		}
		if (configuration.includeSingleSelectionControl) {
			controlMap.control(SINGLE_SELECTION).set(table.createSingleSelectionControl());
		}
		controlMap.control(REQUEST_TABLE_FOCUS).set(createRequestTableFocusControl());
		controlMap.control(REQUEST_SEARCH_FIELD_FOCUS).set(createRequestSearchFieldFocusControl());
	}

	private void setupStandardControls() {
		control(ADDITIONAL_POPUP_MENU_CONTROLS).map(new ReplaceIfNull(this::createAdditionalPopupControls));
		control(ADDITIONAL_TOOLBAR_CONTROLS).map(new ReplaceIfNull(this::createAdditionalToolbarControls));
		control(PRINT_CONTROLS).map(new ReplaceIfNull(this::createPrintControls));
		control(CONDITION_CONTROLS).map(new ReplaceIfNull(this::createConditionControls));
		control(FILTER_CONTROLS).map(new ReplaceIfNull(this::createFilterControls));
		control(COLUMN_CONTROLS).map(new ReplaceIfNull(this::createColumnControls));
		control(COPY_CONTROLS).map(new ReplaceIfNull(this::createCopyControls));
	}

	private void configureColumn(FilterTableColumn<Attribute<?>> column) {
		column.setHeaderRenderer(new HeaderRenderer(column.getHeaderRenderer()));
	}

	private void addTablePopupMenu() {
		if (configuration.includePopupMenu) {
			Controls popupControls = popupMenuLayout.create(configuration.controlMap);
			if (popupControls == null || popupControls.size() == 0) {
				return;
			}

			JPopupMenu popupMenu = menu()
							.controls(popupControls)
							.buildPopupMenu();
			table.setComponentPopupMenu(popupMenu);
			tableScrollPane.setComponentPopupMenu(popupMenu);
		}
	}

	private void addDoubleClickAction() {
		if (table.doubleClick().isNull()) {
			control(EDIT).optional().ifPresent(table.doubleClick()::set);
		}
	}

	private void showEntityMenu() {
		Point location = popupLocation(table);
		tableModel.selection().item().optional().ifPresent(selected ->
						new EntityPopupMenu(selected, tableModel.connection()).show(table, location.x, location.y));
	}

	private void showQueryInspector() {
		if (queryInspector == null) {
			queryInspector = new SelectQueryInspector(tableModel.queryModel());
		}
		if (queryInspector.isShowing()) {
			parentWindow(queryInspector).toFront();
		}
		else {
			Dialogs.builder()
							.component(queryInspector)
							.owner(this)
							.title(tableModel.entityDefinition().caption() + " Query")
							.modal(false)
							.show();
		}
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

	private void setConditionViewHidden(JScrollPane scrollPane, Value<ConditionView> conditionView) {
		KeyboardFocusManager focusManager = getCurrentKeyboardFocusManager();
		boolean conditionPanelHasFocus = parentOfType(JScrollPane.class,
						focusManager.getFocusOwner()) == scrollPane;
		if (conditionPanelHasFocus) {
			focusManager.clearFocusOwner();
		}
		conditionView.set(HIDDEN);
		if (conditionPanelHasFocus) {
			table.requestFocusInWindow();
		}
	}

	private EntityEditPanel validateEditModel(EntityEditPanel editPanel) {
		if (editPanel.editModel() != tableModel.editModel()) {
			throw new IllegalArgumentException("Edit panel model must be the same as the table edit model");
		}

		return editPanel;
	}

	private void displayDependencies(boolean dependenciesExpected) {
		EntityDependenciesPanel.displayDependencies(tableModel.selection().items().get(), tableModel.connectionProvider(),
						this, dependenciesDialogSize, dependencyPanelPreferences, dependenciesExpected);
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

	private Controls.Layout createPopupMenuLayout() {
		return Config.POPUP_MENU_LAYOUT.optional().orElse(Controls.layout(asList(
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
						SINGLE_SELECTION,
						null,
						CONDITION_CONTROLS,
						null,
						FILTER_CONTROLS,
						null,
						COPY_CONTROLS
		)));
	}

	private Controls.Layout createToolBarLayout() {
		return Config.TOOLBAR_LAYOUT.optional().orElse(Controls.layout(asList(
						TOGGLE_SUMMARIES,
						TOGGLE_CONDITION_VIEW,
						TOGGLE_FILTER_VIEW,
						null,
						ADD,
						EDIT,
						DELETE,
						null,
						editPanel == null ? EDIT_ATTRIBUTE : null,
						null,
						PRINT,
						null,
						ADDITIONAL_TOOLBAR_CONTROLS
		)));
	}

	private final class AddCommand implements Control.Command {

		@Override
		public void execute() {
			addEntityDialog(editPanel())
							.owner(EntityTablePanel.this)
							.closeDialog(false)
							.show();
		}
	}

	private final class EditCommand implements Control.Command {

		@Override
		public void execute() {
			editEntityDialog(editPanel())
							.owner(EntityTablePanel.this)
							.show();
		}
	}

	private final class DeleteCommand implements Control.Command {

		@Override
		public void execute() {
			if (confirmDelete()) {
				List<Entity> selectedItems = tableModel().selection().items().get();
				Dialogs.progressWorker()
								.task(tableModel().editModel().deleteTask(selectedItems).prepare()::perform)
								.title(EDIT_PANEL_MESSAGES.getString("deleting"))
								.owner(EntityTablePanel.this)
								.onException(this::onException)
								.onResult(EditTask.Result::handle)
								.execute();
			}
		}

		private void onException(Exception exception) {
			LOG.error(exception.getMessage(), exception);
			EntityTablePanel.this.onException(exception);
		}
	}

	private final class EditAttributeCommand implements Control.Command {

		private final Attribute<?> attribute;

		private EditAttributeCommand(Attribute<?> attribute) {
			this.attribute = attribute;
		}

		@Override
		public void execute() {
			editSelected(attribute);
		}
	}

	private static GridBagConstraints createHorizontalFillConstraints() {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;

		return constraints;
	}

	private static Point popupLocation(JTable table) {
		Rectangle visibleRect = table.getVisibleRect();
		int x = visibleRect.x + visibleRect.width / 2;
		int y = table.getSelectionModel().isSelectionEmpty() ?
						visibleRect.y + visibleRect.height / 2 :
						table.getCellRect(table.getSelectedRow(), table.getSelectedColumn(), true).y;

		return new Point(x, y + table.getRowHeight() / 2);
	}

	private static boolean containsSummaryModels(FilterTable<Entity, Attribute<?>> table) {
		return table.columnModel().identifiers().stream()
						.map(table.summaries()::get)
						.anyMatch(Optional::isPresent);
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
			FilterTableColumn<Attribute<?>> tableColumn = table().columnModel().getColumn(column);
			TableCellRenderer renderer = tableColumn.getCellRenderer();
			boolean useBoldFont = renderer instanceof FilterTableCellRenderer
							&& ((FilterTableCellRenderer<?>) renderer).filterIndicator()
							&& tableModel.queryModel().condition().optional(tableColumn.identifier())
							.map(conditionModel -> conditionModel.enabled().is()).orElse(false);
			Font defaultFont = component.getFont();
			component.setFont(useBoldFont ? defaultFont.deriveFont(defaultFont.getStyle() | Font.BOLD) : defaultFont);

			return component;
		}
	}

	private static final class DeleteConfirmer implements Confirmer {

		private final FilterListSelection<?> selection;

		private DeleteConfirmer(FilterListSelection<?> selection) {
			this.selection = selection;
		}

		@Override
		public boolean confirm(JComponent dialogOwner) {
			return confirm(dialogOwner, FrameworkMessages.confirmDelete(
							selection.count()), FrameworkMessages.delete());
		}
	}

	private static final class ReplaceIfNull implements UnaryOperator<Controls> {

		private final Supplier<Controls> controls;

		private ReplaceIfNull(Supplier<@Nullable Controls> controls) {
			this.controls = controls;
		}

		@Override
		public Controls apply(@Nullable Controls control) {
			return control == null ? controls.get() : control;
		}
	}

	/**
	 * Contains configuration settings for a {@link EntityTablePanel} which must be set before the panel is initialized.
	 */
	public static final class Config {

		/**
		 * Specifies whether the values of hidden columns should be excluded in the underlying query
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: false
		 * </ul>
		 */
		public static final PropertyValue<Boolean> EXCLUDE_HIDDEN_COLUMNS = booleanValue(EntityTablePanel.class.getName() + ".excludeHiddenColumns", false);

		/**
		 * Specifies the default initial table condition panel view
		 * <ul>
		 * <li>Value type: {@link ConditionView}
		 * <li>Default value: {@link ConditionView#HIDDEN}
		 * </ul>
		 */
		public static final PropertyValue<ConditionView> CONDITION_VIEW =
						enumValue(EntityTablePanel.class.getName() + ".conditionView",
										ConditionView.class, HIDDEN);

		/**
		 * Specifies the default initial table filter panel view
		 * <ul>
		 * <li>Value type: {@link ConditionView}
		 * <li>Default value: {@link ConditionView#HIDDEN}
		 * </ul>
		 */
		public static final PropertyValue<ConditionView> FILTER_VIEW =
						enumValue(EntityTablePanel.class.getName() + ".filterView",
										ConditionView.class, HIDDEN);

		/**
		 * Specifies whether table summary panel should be visible or not by default
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: false
		 * </ul>
		 */
		public static final PropertyValue<Boolean> SUMMARY_PANEL_VISIBLE =
						booleanValue(EntityTablePanel.class.getName() + ".summaryPanelVisible", false);

		/**
		 * Specifies whether to include the default popup menu on entity tables
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_POPUP_MENU =
						booleanValue(EntityTablePanel.class.getName() + ".includePopupMenu", true);

		/**
		 * Specifies whether to include a {@link EntityPopupMenu} on this table, triggered with CTRL-ALT-V.
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_ENTITY_MENU =
						booleanValue(EntityTablePanel.class.getName() + ".includeEntityMenu", true);

		/**
		 * Specifies whether to include a Query Inspector on this table, triggered with CTRL-ALT-Q.
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: false
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_QUERY_INSPECTOR =
						booleanValue(EntityTablePanel.class.getName() + ".includeQueryInspector", false);

		/**
		 * Specifies whether to include a 'Clear' control in the popup menu.
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: false
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_CLEAR_CONTROL =
						booleanValue(EntityTablePanel.class.getName() + ".includeClearControl", false);

		/**
		 * Specifies whether to include an export panel (Copy -> Copy Expanded...).
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: false
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_EXPORT =
						booleanValue(EntityTablePanel.class.getName() + ".includeExport", false);

		/**
		 * Specifies whether to include a condition panel.
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_CONDITIONS =
						booleanValue(EntityTablePanel.class.getName() + ".includeConditions", true);

		/**
		 * Specifies whether to include a filter panel.
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_FILTERS =
						booleanValue(EntityTablePanel.class.getName() + ".includeFilters", false);

		/**
		 * Specifies whether to include a summary panel.
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_SUMMARY =
						booleanValue(EntityTablePanel.class.getName() + ".includeSummary", true);

		/**
		 * Specifies whether to include a popup menu for configuring the table model limit.
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: false
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_LIMIT_MENU =
						booleanValue(EntityTablePanel.class.getName() + ".includeLimitMenu", false);

		/**
		 * Specifies whether to show an indeterminate progress bar while the model is refreshing.
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> REFRESH_PROGRESS_BAR =
						booleanValue(EntityTablePanel.class.getName() + ".refreshProgressBar", true);

		/**
		 * Specifies the number of milliseconds to delay showing the refresh progress bar, if enabled.
		 * <ul>
		 * <li>Value type: Integer
		 * <li>Default value: 350
		 * </ul>
		 * @see #REFRESH_PROGRESS_BAR
		 */
		public static final PropertyValue<Integer> REFRESH_PROGRESS_BAR_DELAY =
						integerValue(EntityTablePanel.class.getName() + ".refreshProgressBarDelay", 350);

		/**
		 * Specifies whether the refresh button should always be visible or only when the condition panel is visible
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: {@link RefreshButtonVisible#WHEN_CONDITION_PANEL_IS_VISIBLE}
		 * </ul>
		 */
		public static final PropertyValue<RefreshButtonVisible> REFRESH_BUTTON_VISIBLE =
						enumValue(EntityTablePanel.class.getName() + ".refreshButtonVisible",
										RefreshButtonVisible.class, RefreshButtonVisible.WHEN_CONDITION_PANEL_IS_VISIBLE);

		/**
		 * Specifies how column selection is presented to the user.
		 * <ul>
		 * <li>Value type: {@link SelectionMode}
		 * <li>Default value: {@link SelectionMode#DIALOG}
		 * </ul>
		 */
		public static final PropertyValue<SelectionMode> COLUMN_SELECTION =
						enumValue(EntityTablePanel.class.getName() + ".columnSelection", SelectionMode.class, SelectionMode.DIALOG);

		/**
		 * Specifies how column selection is presented to the user.
		 * <ul>
		 * <li>Value type: {@link SelectionMode}
		 * <li>Default value: {@link SelectionMode#DIALOG}
		 * </ul>
		 */
		public static final PropertyValue<SelectionMode> AUTO_RESIZE_MODE_SELECTION =
						enumValue(EntityTablePanel.class.getName() + ".autoResizeModeSelection", SelectionMode.class, SelectionMode.DIALOG);

		/**
		 * Specifies how the edit an attribute action is presented to the user.
		 * <ul>
		 * <li>Value type: {@link SelectionMode}
		 * <li>Default value: {@link SelectionMode#MENU}
		 * </ul>
		 */
		public static final PropertyValue<SelectionMode> EDIT_ATTRIBUTE_SELECTION =
						enumValue(EntityTablePanel.class.getName() + ".editAttributeSelection", SelectionMode.class, SelectionMode.MENU);

		/**
		 * Specifies the default popup menu layout.
		 * {@snippet :
		 *  EntityTablePanel.Config.POPUP_MENU_LAYOUT.set(Controls.layout(asList(
		 *      EntityTablePanel.ControlKeys.REFRESH,
		 *      null, // <- separator
		 *      EntityTablePanel.ControlKeys.ADDITIONAL_POPUP_MENU_CONTROLS,
		 *      null,
		 *      EntityTablePanel.ControlKeys.CONDITION_CONTROLS,
		 *      null,
		 *      EntityTablePanel.ControlKeys.COPY_CONTROLS
		 *  )));
		 *}
		 * <ul>
		 * <li>Value type: {@link Controls.Layout}
		 * <li>Default value: null
		 * </ul>
		 * @see EntityTablePanel#configurePopupMenu(Consumer)
		 */
		public static final PropertyValue<Controls.Layout> POPUP_MENU_LAYOUT =
						value(EntityTablePanel.class.getName() + ".popupMenuLayout", string -> {
							throw new UnsupportedOperationException("Parsing the popup menu layout from a system property is not supported");
						});

		/**
		 * Specifies the default toolbar layout.
		 * {@snippet :
		 *  EntityTablePanel.Config.TOOLBAR_LAYOUT.set(Controls.layout(asList(
		 *      EntityTablePanel.ControlKeys.TOGGLE_CONDITION_VIEW,
		 *      EntityTablePanel.ControlKeys.TOGGLE_FILTER_VIEW,
		 *      null, // <- separator
		 *      EntityTablePanel.ControlKeys.ADDITIONAL_TOOLBAR_CONTROLS
		 *  )));
		 *}
		 * <ul>
		 * <li>Value type: {@link Controls.Layout}
		 * <li>Default value: null
		 * </ul>
		 * <ul>
		 * <li>Value type: {@link Controls.Layout}
		 * <li>Default value: null
		 * </ul>
		 * @see EntityTablePanel#configureToolBar(Consumer)
		 */
		public static final PropertyValue<Controls.Layout> TOOLBAR_LAYOUT =
						value(EntityTablePanel.class.getName() + ".toolBarLayout", string -> {
							throw new UnsupportedOperationException("Parsing the toolbar layout from a system property is not supported");
						});

		private static final Function<SwingEntityTableModel, String> DEFAULT_STATUS_MESSAGE = new DefaultStatusMessage();

		private final EntityTablePanel tablePanel;
		private final EntityDefinition entityDefinition;
		private final ValueSet<Attribute<?>> editable;
		private final Map<Attribute<?>, EditComponentFactory<?, ?>> editComponentFactories;
		private final Map<Attribute<?>, ComponentFactory> conditionComponentFactories;

		private FilterTable.@Nullable Builder<Entity, Attribute<?>> tableBuilder;
		private TableConditionPanel.Factory<Attribute<?>> conditionPanelFactory = new DefaultConditionPanelFactory();
		private boolean includeSouthPanel = true;
		private boolean includeExport = INCLUDE_EXPORT.getOrThrow();
		private boolean includeConditions = INCLUDE_CONDITIONS.getOrThrow();
		private ConditionView conditionView = CONDITION_VIEW.getOrThrow();
		private boolean includeFilters = INCLUDE_FILTERS.getOrThrow();
		private ConditionView filterView = FILTER_VIEW.getOrThrow();
		private boolean includeSummaries = INCLUDE_SUMMARY.getOrThrow();
		private boolean includeClearControl = INCLUDE_CLEAR_CONTROL.getOrThrow();
		private boolean includeLimitMenu = INCLUDE_LIMIT_MENU.getOrThrow();
		private boolean includeEntityMenu = INCLUDE_ENTITY_MENU.getOrThrow();
		private boolean includeQueryInspector = INCLUDE_QUERY_INSPECTOR.getOrThrow();
		private boolean includePopupMenu = INCLUDE_POPUP_MENU.getOrThrow();
		private boolean includeSingleSelectionControl = false;
		private boolean includeAddControl = true;
		private boolean includeEditControl = true;
		private boolean includeEditAttributeControl = true;
		private boolean includeToolBar = true;
		private boolean excludeHiddenColumns = EXCLUDE_HIDDEN_COLUMNS.getOrThrow();
		private SelectionMode columnSelection = COLUMN_SELECTION.getOrThrow();
		private SelectionMode autoResizeModeSelection = AUTO_RESIZE_MODE_SELECTION.getOrThrow();
		private SelectionMode editAttributeSelection = EDIT_ATTRIBUTE_SELECTION.getOrThrow();
		private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling = REFERENTIAL_INTEGRITY_ERROR_HANDLING.getOrThrow();
		private RefreshButtonVisible refreshButtonVisible = REFRESH_BUTTON_VISIBLE.getOrThrow();
		private Function<SwingEntityTableModel, String> statusMessage = DEFAULT_STATUS_MESSAGE;
		private boolean refreshProgressBar = REFRESH_PROGRESS_BAR.getOrThrow();
		private int refreshProgressBarDelay = REFRESH_PROGRESS_BAR_DELAY.getOrThrow();
		private Confirmer deleteConfirmer;

		final ControlMap controlMap;

		private Config(EntityTablePanel tablePanel) {
			this.tablePanel = tablePanel;
			this.entityDefinition = tablePanel.tableModel.entityDefinition();
			this.tableBuilder = FilterTable.builder()
							.model(tablePanel.tableModel)
							.summaryValuesFactory(new EntitySummaryValuesFactory(entityDefinition, tablePanel.tableModel))
							.cellRendererFactory(EntityTableCellRenderer.factory())
							.cellEditorFactory(new EntityTableCellEditorFactory(tablePanel.tableModel.editModel()))
							.cellEditable(new EntityCellEditable(tablePanel.tableModel.entities()))
							.scrollToAddedItem(true)
							.onBuild(filterTable -> filterTable.setRowHeight(filterTable.getFont().getSize() + FONT_SIZE_TO_ROW_HEIGHT));
			this.conditionPanelFactory = new DefaultConditionPanelFactory();
			this.conditionComponentFactories = new HashMap<>();
			this.controlMap = ControlMap.controlMap(ControlKeys.class);
			this.editable = valueSet(editableAttributes());
			this.editable.addValidator(new EditMenuAttributeValidator(entityDefinition));
			this.editComponentFactories = new HashMap<>();
			this.deleteConfirmer = new DeleteConfirmer(tablePanel.tableModel.selection());
		}

		private Config(Config config) {
			this.tablePanel = config.tablePanel;
			this.entityDefinition = config.entityDefinition;
			this.tableBuilder = config.tableBuilder;
			this.controlMap = config.controlMap.copy();
			this.editable = valueSet(config.editable.get());
			this.includeSouthPanel = config.includeSouthPanel;
			this.includeExport = config.includeExport;
			this.includeConditions = config.includeConditions;
			this.conditionView = config.conditionView;
			this.includeFilters = config.includeFilters;
			this.filterView = config.filterView;
			this.includeSummaries = config.includeSummaries;
			this.includeClearControl = config.includeClearControl;
			this.includeLimitMenu = config.includeLimitMenu;
			this.includeEntityMenu = config.includeEntityMenu;
			this.includeQueryInspector = config.includeQueryInspector;
			this.includePopupMenu = config.includePopupMenu;
			this.includeSingleSelectionControl = config.includeSingleSelectionControl;
			this.includeAddControl = config.includeAddControl;
			this.includeEditControl = config.includeEditControl;
			this.includeEditAttributeControl = config.includeEditAttributeControl;
			this.columnSelection = config.columnSelection;
			this.autoResizeModeSelection = config.autoResizeModeSelection;
			this.editAttributeSelection = config.editAttributeSelection;
			this.editComponentFactories = new HashMap<>(config.editComponentFactories);
			this.referentialIntegrityErrorHandling = config.referentialIntegrityErrorHandling;
			this.refreshButtonVisible = config.refreshButtonVisible;
			this.statusMessage = config.statusMessage;
			this.refreshProgressBar = config.refreshProgressBar;
			this.refreshProgressBarDelay = config.refreshProgressBarDelay;
			this.deleteConfirmer = config.deleteConfirmer;
			this.includeToolBar = config.includeToolBar;
			this.conditionPanelFactory = config.conditionPanelFactory;
			this.conditionComponentFactories = new HashMap<>(config.conditionComponentFactories);
			this.excludeHiddenColumns = config.excludeHiddenColumns;
		}

		/**
		 * @return the table panel
		 */
		public EntityTablePanel tablePanel() {
			return tablePanel;
		}

		/**
		 * Provides access to the builder for the underlying {@link FilterTable}
		 * @param builder the table builder
		 * @return this Config instance
		 */
		public Config table(Consumer<FilterTable.Builder<Entity, Attribute<?>>> builder) {
			requireNonNull(builder).accept(this.tableBuilder);
			return this;
		}

		/**
		 * @param conditionPanelFactory the table condition panel factory
		 * @return this Config instance
		 */
		public Config conditionPanelFactory(TableConditionPanel.Factory<Attribute<?>> conditionPanelFactory) {
			this.conditionPanelFactory = requireNonNull(conditionPanelFactory);
			return this;
		}

		/**
		 * @param attribute the attribute
		 * @param componentFactory the component factory for the given attribute
		 * @return this Config instance
		 * @see EntityTablePanel#condition()
		 */
		public Config conditionComponentFactory(Attribute<?> attribute, ComponentFactory componentFactory) {
			this.conditionComponentFactories.put(attribute, requireNonNull(componentFactory));
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
		 * @param includeExport true if an export panel should be included (Copy -> Copy Expanded...)
		 * @return this Config instance
		 */
		public Config includeExport(boolean includeExport) {
			this.includeExport = includeExport;
			return this;
		}

		/**
		 * @param includeConditions true if the condition panel should be included
		 * @return this Config instance
		 */
		public Config includeConditions(boolean includeConditions) {
			this.includeConditions = includeConditions;
			return this;
		}

		/**
		 * @param conditionView the initial condition view
		 * @return this Config instance
		 */
		public Config conditionView(ConditionView conditionView) {
			this.conditionView = requireNonNull(conditionView);
			return this;
		}

		/**
		 * @param includeFilters true if the filter panel should be included
		 * @return this Config instance
		 */
		public Config includeFilters(boolean includeFilters) {
			this.includeFilters = includeFilters;
			return this;
		}

		/**
		 * @param filterView the initial filter view
		 * @return this Config instance
		 */
		public Config filterView(ConditionView filterView) {
			this.filterView = requireNonNull(filterView);
			return this;
		}

		/**
		 * @param includeSummaries true if the summary panel should be included
		 * @return this Config instance
		 */
		public Config includeSummaries(boolean includeSummaries) {
			this.includeSummaries = includeSummaries;
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
		 * @param includeEntityMenu true if a {@link EntityPopupMenu} should be available in this table, triggered with CTRL-ALT-V.
		 * @return this Config instance
		 */
		public Config includeEntityMenu(boolean includeEntityMenu) {
			this.includeEntityMenu = includeEntityMenu;
			return this;
		}

		/**
		 * @param includeQueryInspector true if a Query Inspector should be available in this table, triggered with CTRL-ALT-Q.
		 * @return this Config instance
		 */
		public Config includeQueryInspector(boolean includeQueryInspector) {
			this.includeQueryInspector = includeQueryInspector;
			return this;
		}

		/**
		 * @param includeSingleSelectionControl true if a 'Single Selection' control should be included in the popup menu
		 * @return this Config instance
		 */
		public Config includeSingleSelectionControl(boolean includeSingleSelectionControl) {
			this.includeSingleSelectionControl = includeSingleSelectionControl;
			return this;
		}

		/**
		 * @param includeToolBar true if a toolbar should be included on the south panel
		 * @return this Config instance
		 */
		public Config includeToolBar(boolean includeToolBar) {
			this.includeToolBar = includeToolBar;
			return this;
		}

		/**
		 * @param includeAddControl true if an Add control should be included if an edit panel is available
		 * @return this Config instance
		 */
		public Config includeAddControl(boolean includeAddControl) {
			this.includeAddControl = includeAddControl;
			return this;
		}

		/**
		 * @param includeEditControl true if an Edit control should be included if an edit panel is available
		 * @return this Config instance
		 */
		public Config includeEditControl(boolean includeEditControl) {
			this.includeEditControl = includeEditControl;
			return this;
		}

		/**
		 * @param includeEditAttributeControl true if an 'Edit' attribute control should be included
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
		public Config columnSelection(SelectionMode columnSelection) {
			this.columnSelection = requireNonNull(columnSelection);
			return this;
		}

		/**
		 * @param autoResizeModeSelection specifies how auto-resize-mode is selected
		 * @return this Config instance
		 */
		public Config autoResizeModeSelection(SelectionMode autoResizeModeSelection) {
			this.autoResizeModeSelection = requireNonNull(autoResizeModeSelection);
			return this;
		}

		/**
		 * @param editAttributeSelection specifies how attribute selection is presented selected when editing the selected records
		 * @return this Config instance
		 */
		public Config editAttributeSelection(SelectionMode editAttributeSelection) {
			this.editAttributeSelection = requireNonNull(editAttributeSelection);
			return this;
		}

		/**
		 * @param controlKey the control key
		 * @param keyStroke provides access to the {@link Value} controlling the keyStroke for the given control
		 * @return this Config instance
		 */
		public Config keyStroke(ControlKey<?> controlKey, Consumer<Value<KeyStroke>> keyStroke) {
			requireNonNull(keyStroke).accept(controlMap.keyStroke(controlKey));
			return this;
		}

		/**
		 * By default, all attributes are editable via the table popup menu or the {@link ControlKeys#EDIT_ATTRIBUTE} control,
		 * use this method to exclude one or more attributes from being editable.
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
		 * @param surrendersFocusOnKeystroke true if the table should surrenders focus on keystroke
		 * @return this builder instance
		 * @see JTable#setSurrendersFocusOnKeystroke(boolean)
		 */
		public Config surrendersFocusOnKeystroke(boolean surrendersFocusOnKeystroke) {
			tableBuilder.surrendersFocusOnKeystroke(surrendersFocusOnKeystroke);
			return this;
		}

		/**
		 * Sets the component factory for the given attribute, used when editing entities via {@link EntityTablePanel#editSelected(Attribute)}.
		 * @param attribute the attribute
		 * @param editComponentFactory the edit component factory
		 * @param <C> the component type
		 * @param <T> the value type
		 * @param <A> the attribute type
		 * @return this Config instance
		 */
		public <C extends JComponent, T, A extends Attribute<T>> Config editComponentFactory(A attribute,
																																												 EditComponentFactory<C, T> editComponentFactory) {
			entityDefinition.attributes().definition(attribute);
			editComponentFactories.put(attribute, requireNonNull(editComponentFactory));
			return this;
		}

		/**
		 * Sets the cell editor for the given attribute
		 * @param attribute the attribute
		 * @param cellEditor the cell editor
		 * @param <T> the value type
		 * @param <A> the attribute type
		 * @return this Config instance
		 * @see FilterTable.Builder#cellEditor(Object, FilterTableCellEditor)
		 */
		public <T, A extends Attribute<T>> Config cellEditor(A attribute, FilterTableCellEditor<T> cellEditor) {
			entityDefinition.attributes().definition(attribute);
			tableBuilder.cellEditor(attribute, requireNonNull(cellEditor));
			return this;
		}

		/**
		 * Overridden by {@link #cellEditor(Attribute, FilterTableCellEditor)}.
		 * @param cellEditorFactory the cell editor factory
		 * @return this Config instance
		 * @see FilterTable.Builder#cellRendererFactory(FilterTableCellRenderer.Factory)
		 */
		public Config cellEditorFactory(FilterTableCellEditor.Factory<Attribute<?>> cellEditorFactory) {
			tableBuilder.cellEditorFactory(requireNonNull(cellEditorFactory));
			return this;
		}

		/**
		 * Sets the cell renderer for the given attribute
		 * @param attribute the attribute
		 * @param cellRenderer the cell renderer
		 * @param <T> the value type
		 * @param <A> the attribute type
		 * @return this Config instance
		 * @see FilterTable.Builder#cellRenderer(Object, FilterTableCellRenderer)
		 */
		public <T, A extends Attribute<T>> Config cellRenderer(A attribute, FilterTableCellRenderer<T> cellRenderer) {
			entityDefinition.attributes().definition(attribute);
			tableBuilder.cellRenderer(attribute, requireNonNull(cellRenderer));
			return this;
		}

		/**
		 * Overridden by {@link #cellRenderer(Attribute, FilterTableCellRenderer)}.
		 * @param cellRendererFactory the cell renderer factory
		 * @return this Config instance
		 * @see FilterTable.Builder#cellRendererFactory(FilterTableCellRenderer.Factory)
		 */
		public Config cellRendererFactory(EntityTableCellRenderer.Factory cellRendererFactory) {
			tableBuilder.cellRendererFactory(cellRendererFactory);
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
		 * @param refreshProgressBar controls whether an indeterminate progress bar should be shown while the model is refreshing
		 * @return this Config instance
		 * @see #REFRESH_PROGRESS_BAR
		 */
		public Config refreshProgressBar(boolean refreshProgressBar) {
			this.refreshProgressBar = refreshProgressBar;
			return this;
		}

		/**
		 * @param refreshProgressBarDelay controls the delay before the refresh progress bar is shown, if enabled
		 * @return this Config instance
		 * @see #refreshProgressBar(boolean)
		 * @see #REFRESH_PROGRESS_BAR_DELAY
		 */
		public Config refreshProgressBarDelay(int refreshProgressBarDelay) {
			this.refreshProgressBarDelay = refreshProgressBarDelay;
			return this;
		}

		/**
		 * Specifies whether the values of hidden columns are excluded when querying data
		 * @return this Config instance
		 * @see #EXCLUDE_HIDDEN_COLUMNS
		 */
		public Config excludeHiddenColumns(boolean excludeHiddenColumns) {
			this.excludeHiddenColumns = excludeHiddenColumns;
			return this;
		}

		private ControlKey<?> popupMenuEditAttributeControl() {
			return editAttributeSelection == SelectionMode.MENU ?
							EDIT_ATTRIBUTE_CONTROLS :
							EDIT_ATTRIBUTE;
		}

		private FilterTable<Entity, Attribute<?>> buildTable() {
			FilterTable<Entity, Attribute<?>> filterTable = tableBuilder.build();
			tableBuilder = null;

			return filterTable;
		}

		private Collection<Attribute<?>> editableAttributes() {
			List<Column<?>> updatableColumns = entityDefinition.columns().definitions().stream()
							.filter(ColumnDefinition::updatable)
							.filter(column -> (!column.primaryKey() || !column.generated()))
							.map(ColumnDefinition::attribute)
							.collect(toList());
			EntityDefinition.ForeignKeys foreignKeys = entityDefinition.foreignKeys();
			updatableColumns.removeIf(foreignKeys::foreignKeyColumn);
			List<Attribute<?>> updatable = new ArrayList<>(updatableColumns);
			for (ForeignKey foreignKey : entityDefinition.foreignKeys().get()) {
				if (foreignKeys.updatable(foreignKey)) {
					updatable.add(foreignKey);
				}
			}

			return updatable;
		}

		private static final class DefaultConditionPanelFactory
						implements TableConditionPanel.Factory<Attribute<?>> {

			@Override
			public TableConditionPanel<Attribute<?>> create(TableConditionModel<Attribute<?>> tableConditionModel,
																											Map<Attribute<?>, ConditionPanel<?>> conditionPanels,
																											FilterTableColumnModel<Attribute<?>> columnModel,
																											Consumer<TableConditionPanel<Attribute<?>>> onPanelInitialized) {
				return filterTableConditionPanel(tableConditionModel, conditionPanels, columnModel, onPanelInitialized);
			}
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

	private static final class EntitySummaryValuesFactory implements SummaryModel.SummaryValues.Factory<Attribute<?>> {

		private final EntityDefinition entityDefinition;
		private final FilterTableModel<?, Attribute<?>> tableModel;

		private EntitySummaryValuesFactory(EntityDefinition entityDefinition, FilterTableModel<?, Attribute<?>> tableModel) {
			this.entityDefinition = requireNonNull(entityDefinition);
			this.tableModel = requireNonNull(tableModel);
		}

		@Override
		public <T extends Number> Optional<SummaryModel.SummaryValues<T>> createSummaryValues(Attribute<?> identifier, Format format) {
			AttributeDefinition<?> definition = entityDefinition.attributes().definition(identifier);
			if (definition instanceof ValueAttributeDefinition<?>) {
				ValueAttributeDefinition<?> attributeDefinition = (ValueAttributeDefinition<?>) definition;
				if (identifier.type().isNumeric() && attributeDefinition.items().isEmpty()) {
					return Optional.of(FilterTable.summaryValues(identifier, tableModel, format));
				}
			}

			return Optional.empty();
		}
	}

	private static final class EntityCellEditable implements BiPredicate<Entity, Attribute<?>> {

		private final Entities entities;

		private EntityCellEditable(Entities entities) {
			this.entities = entities;
		}

		@Override
		public boolean test(Entity entity, Attribute<?> attribute) {
			if (attribute instanceof ForeignKey) {
				EntityDefinition entityDefinition = entities.definition(((ForeignKey) attribute).referencedType());
				if (entityDefinition.columns().searchable().isEmpty() && !entityDefinition.smallDataset()) {
					// Neither EntitySearchField nor EntityComboBox can be created for editing
					return false;
				}
			}

			return true;
		}
	}

	private static final class ComponentAvailableValidator implements Value.Validator<Boolean> {

		private final @Nullable JComponent component;
		private final String panelType;

		private ComponentAvailableValidator(@Nullable JComponent component, String panelType) {
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
			int rowCount = tableModel.items().included().size();
			int filteredCount = tableModel.items().filtered().size();
			if (rowCount == 0 && filteredCount == 0) {
				return "";
			}
			int selectionCount = tableModel.selection().count();
			StringBuilder builder = new StringBuilder();
			if (tableModel.queryModel().limit().is(rowCount)) {
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

		private final JPanel tableSouthPanel = new JPanel(new BorderLayout());

		private TablePanel() {
			super(new BorderLayout());
			add(tableScrollPane, BorderLayout.CENTER);
			add(tableSouthPanel, BorderLayout.SOUTH);
		}

		private void initialize() {
			if (includeToggleSummaryPanelControl()) {
				summaryPanel = createSummaryPanel();
				if (summaryPanel != null) {
					summaryPanelScrollPane = createLinkedScrollPane(summaryPanel);
					summaryPanelScrollPane.setVisible(false);
					tableSouthPanel.add(summaryPanelScrollPane, BorderLayout.NORTH);
				}
			}
			if (configuration.includeFilters) {
				filterPanelScrollPane = createLinkedScrollPane(table.filters());
				table.filters().view().addConsumer(this::filterViewChanged);
				if (table.filters().view().isNot(HIDDEN)) {
					tableSouthPanel.add(filterPanelScrollPane, BorderLayout.SOUTH);
				}
			}
		}

		private void conditionViewChanged(ConditionView conditionView) {
			initializeConditionPanel();
			refreshButtonToolBar.setVisible(configuration.refreshButtonVisible == RefreshButtonVisible.ALWAYS
							|| conditionView != HIDDEN);
			if (conditionView == HIDDEN) {
				remove(conditionPanelScrollPane);
			}
			else {
				add(conditionPanelScrollPane, BorderLayout.NORTH);
			}
			revalidate();
		}

		private void initializeConditionPanel() {
			if (conditionPanelScrollPane == null) {
				conditionPanelScrollPane = createLinkedScrollPane(tableConditionPanel);
				if (tableConditionPanel.view().isNot(HIDDEN)) {
					tablePanel.add(conditionPanelScrollPane, BorderLayout.NORTH);
				}
				refreshButtonToolBar.setVisible(configuration.refreshButtonVisible == RefreshButtonVisible.ALWAYS
								|| tableConditionPanel.view().isNot(HIDDEN));
			}
		}

		private JScrollPane createLinkedScrollPane(JComponent componentToScroll) {
			return Components.scrollPane()
							.view(componentToScroll)
							.horizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
							.verticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
							.onBuild(scrollPane -> link(
											tableScrollPane.getHorizontalScrollBar().getModel(),
											scrollPane.getHorizontalScrollBar().getModel()))
							.build();
		}

		private void filterViewChanged(ConditionView conditionView) {
			if (conditionView == HIDDEN) {
				tableSouthPanel.remove(filterPanelScrollPane);
			}
			else {
				tableSouthPanel.add(filterPanelScrollPane, BorderLayout.SOUTH);
			}
			revalidate();
		}

		private @Nullable FilterTableColumnComponentPanel<Attribute<?>> createSummaryPanel() {
			Map<Attribute<?>, JComponent> columnSummaryPanels = createColumnSummaryPanels();
			if (columnSummaryPanels.isEmpty()) {
				return null;
			}

			return filterTableColumnComponentPanel(table.columnModel(), columnSummaryPanels);
		}

		private Map<Attribute<?>, JComponent> createColumnSummaryPanels() {
			Map<Attribute<?>, JComponent> components = new HashMap<>();
			table.columnModel().columns().forEach(column ->
							table.summaries().get(column.identifier())
											.ifPresent(columnSummaryModel ->
															components.put(column.identifier(), columnSummaryPanel(columnSummaryModel,
																			horizontalAlignment(column.getCellRenderer())))));

			return components;
		}

		private int horizontalAlignment(TableCellRenderer cellRenderer) {
			if (cellRenderer instanceof DefaultTableCellRenderer) {
				return ((DefaultTableCellRenderer) cellRenderer).getHorizontalAlignment();
			}

			return SwingConstants.CENTER;
		}
	}

	private final class SouthPanel extends JPanel {

		private SouthPanel() {
			super(new BorderLayout());
			add(Components.splitPane()
							.continuousLayout(true)
							.leftComponent(Components.panel()
											.layout(new GridBagLayout())
											.add(table.searchField(), createHorizontalFillConstraints()))
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

		@Override
		public void updateUI() {
			super.updateUI();
			Utilities.updateUI(statusPanel);
		}

		private StatusPanel statusPanel() {
			if (statusPanel == null) {
				statusPanel = new StatusPanel();
			}

			return statusPanel;
		}

		private @Nullable JToolBar createToolBar() {
			Controls toolbarControls = toolBarLayout.create(configuration.controlMap);
			if (toolbarControls == null || toolbarControls.size() == 0) {
				return null;
			}

			return toolBar()
							.controls(toolbarControls)
							.floatable(false)
							.rollover(true)
							.build();
		}
	}

	private final class StatusPanel extends JPanel {

		private final Value<String> statusMessage = Value.builder()
						.nonNull("")
						.value(configuration.statusMessage.apply(tableModel))
						.build();
		private final JLabel label = Components.label()
						.text(statusMessage)
						.horizontalAlignment(SwingConstants.CENTER)
						.build();
		private final JProgressBar progressBar = Components.progressBar()
						.indeterminate(true)
						.string(MESSAGES.getString("refreshing"))
						.stringPainted(true)
						.build();
		private final JPanel progressPanel = Components.panel()
						.layout(new GridBagLayout())
						.add(progressBar, createHorizontalFillConstraints())
						.build();
		private @Nullable DelayedAction showProgressBarAction;

		private StatusPanel() {
			super(new BorderLayout());
			add(label, BorderLayout.CENTER);
			tableModel.items().refresher().active().addConsumer(this::refresherActive);
			tableModel.selection().indexes().addListener(this::updateStatusMessage);
			tableModel.items().included().addListener(this::updateStatusMessage);
			if (configuration.includeLimitMenu) {
				setComponentPopupMenu(menu()
								.control(Control.builder()
												.command(this::configureLimit)
												.caption(MESSAGES.getString("row_limit"))
												.build())
								.buildPopupMenu());
				addMouseListener(new ConfigureLimit());
			}
		}

		@Override
		public void updateUI() {
			super.updateUI();
			Utilities.updateUI(label, progressBar, progressPanel);
		}

		private void configureLimit() {
			tableModel.queryModel().limit().set(Dialogs.input()
							.component(integerField()
											.value(tableModel.queryModel().limit().get())
											.selectAllOnFocusGained(true)
											.grouping(true)
											.minimum(0)
											.columns(6))
							.title(MESSAGES.getString("row_limit"))
							.owner(EntityTablePanel.this)
							.validator(new LimitValidator())
							.show());
		}

		private void updateStatusMessage() {
			statusMessage.set(configuration.statusMessage.apply(tableModel));
		}

		private void refresherActive(boolean refresherActive) {
			if (configuration.refreshProgressBar) {
				if (refresherActive) {
					showProgressBarDelayed();
				}
				else {
					hideProgressBar();
				}
			}
		}

		private void showProgressBarDelayed() {
			showProgressBarAction = delayedAction(configuration.refreshProgressBarDelay, () -> {
				removeAll();
				add(progressPanel, BorderLayout.CENTER);
				revalidate();
				repaint();
			});
		}

		private void hideProgressBar() {
			cancelShowProgressBar();
			removeAll();
			add(label, BorderLayout.CENTER);
			revalidate();
			repaint();
		}

		private void cancelShowProgressBar() {
			if (showProgressBarAction != null) {
				showProgressBarAction.cancel();
				showProgressBarAction = null;
			}
		}

		private final class LimitValidator implements Predicate<Integer> {

			@Override
			public boolean test(Integer limit) {
				try {
					tableModel.queryModel().limit().validate(limit);
					return true;
				}
				catch (IllegalArgumentException e) {
					return false;
				}
			}
		}

		private final class ConfigureLimit extends MouseAdapter {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					configureLimit();
				}
			}
		}
	}

	private static final class ComboBoxEnterPressedAction extends AbstractAction {

		private static final String ENTER_PRESSED = "enterPressed";

		private final JComboBox<?> comboBox;
		private final Action action;
		private final Action enterPressedAction;

		private ComboBoxEnterPressedAction(JComboBox<?> comboBox, Action action) {
			this.comboBox = comboBox;
			this.action = action;
			this.enterPressedAction = comboBox.getActionMap().get(ENTER_PRESSED);
			this.comboBox.getActionMap().put(ENTER_PRESSED, this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (comboBox.isPopupVisible()) {
				enterPressedAction.actionPerformed(e);
			}
			else if (action.isEnabled()) {
				action.actionPerformed(e);
			}
		}
	}
}
