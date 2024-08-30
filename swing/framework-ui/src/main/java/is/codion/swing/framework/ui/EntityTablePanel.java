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
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityEditModel.Delete;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableSelectionModel;
import is.codion.swing.common.ui.Cursors;
import is.codion.swing.common.ui.Utilities;
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
import is.codion.swing.common.ui.component.table.TableConditionPanel;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.Controls.ControlsBuilder;
import is.codion.swing.common.ui.control.Controls.ControlsKey;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityEditPanel.Confirmer;
import is.codion.swing.framework.ui.component.EntityComponentFactory;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.common.value.ValueSet.valueSet;
import static is.codion.swing.common.ui.Utilities.*;
import static is.codion.swing.common.ui.component.Components.menu;
import static is.codion.swing.common.ui.component.Components.toolBar;
import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState.ADVANCED;
import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState.SIMPLE;
import static is.codion.swing.common.ui.component.table.ColumnSummaryPanel.columnSummaryPanel;
import static is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel.filterTableColumnComponentPanel;
import static is.codion.swing.common.ui.component.table.FilterTableConditionPanel.filterTableConditionPanel;
import static is.codion.swing.common.ui.control.Control.commandControl;
import static is.codion.swing.common.ui.dialog.Dialogs.progressWorkerDialog;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.framework.ui.ColumnPreferences.columnPreferences;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.displayDependenciesDialog;
import static is.codion.swing.framework.ui.EntityDialogs.addEntityDialog;
import static is.codion.swing.framework.ui.EntityDialogs.editEntityDialog;
import static is.codion.swing.framework.ui.EntityTableColumns.entityTableColumns;
import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.*;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
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

	private static final String COLUMN_PREFERENCES = "-columns";
	private static final String CONDITIONS_PREFERENCES = "-conditions";

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
		public static final ControlKey<CommandControl> EDIT = CommandControl.key("edit", keyStroke(VK_INSERT, CTRL_DOWN_MASK));
		/**
		 * Select and edit a single attribute value for the selected entity instances.<br>
		 * Default key stroke: SHIFT-INSERT
		 * @see Config#editAttributeSelection(EditAttributeSelection)
		 */
		public static final ControlKey<CommandControl> EDIT_SELECTED_ATTRIBUTE = CommandControl.key("editSelectedAttribute", keyStroke(VK_INSERT, SHIFT_DOWN_MASK));
		/**
		 * Requests focus for the table.<br>
		 * Default key stroke: CTRL-T
		 */
		public static final ControlKey<CommandControl> REQUEST_TABLE_FOCUS = CommandControl.key("requestTableFocus", keyStroke(VK_T, CTRL_DOWN_MASK));
		/**
		 * Toggles the condition panel between hidden, visible and advanced.<br>
		 * Default key stroke: CTRL-ALT-S
		 * @see TableConditionPanel#state()
		 */
		public static final ControlKey<CommandControl> TOGGLE_CONDITION_PANEL = CommandControl.key("toggleConditionPanel", keyStroke(VK_S, CTRL_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * Displays a dialog for selecting a column condition panel.<br>
		 * Default key stroke: CTRL-S
		 */
		public static final ControlKey<CommandControl> SELECT_CONDITION_PANEL = CommandControl.key("selectConditionPanel", keyStroke(VK_S, CTRL_DOWN_MASK));
		/**
		 * Toggles the filter panel between hidden, visible and advanced.<br>
		 * Default key stroke: CTRL-ALT-F
		 * @see TableConditionPanel#state()
		 */
		public static final ControlKey<CommandControl> TOGGLE_FILTER_PANEL = CommandControl.key("toggleFilterPanel", keyStroke(VK_F, CTRL_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * Displays a dialog for selecting a column filter panel.<br>
		 * Default key stroke: CTRL-SHIFT-F
		 */
		public static final ControlKey<CommandControl> SELECT_FILTER_PANEL = CommandControl.key("selectFilterPanel", keyStroke(VK_F, CTRL_DOWN_MASK | SHIFT_DOWN_MASK));
		/**
		 * Moves the selection up.<br>
		 * Default key stroke: ALT-SHIFT-UP
		 */
		public static final ControlKey<CommandControl> MOVE_SELECTION_UP = CommandControl.key("moveSelectionUp", keyStroke(VK_UP, ALT_DOWN_MASK | SHIFT_DOWN_MASK));
		/**
		 * Moves the selection down.<br>
		 * Default key stroke: ALT-SHIFT-DOWN
		 */
		public static final ControlKey<CommandControl> MOVE_SELECTION_DOWN = CommandControl.key("moveSelectionDown", keyStroke(VK_DOWN, ALT_DOWN_MASK | SHIFT_DOWN_MASK));
		/**
		 * The main print action<br>
		 * Default key stroke: CTRL-P
		 */
		public static final ControlKey<CommandControl> PRINT = CommandControl.key("print", keyStroke(VK_P, CTRL_DOWN_MASK));
		/**
		 * Triggers the {@link ControlKeys#DELETE} control.<br>
		 * Default key stroke: DELETE
		 */
		public static final ControlKey<CommandControl> DELETE = CommandControl.key("delete", keyStroke(VK_DELETE));
		/**
		 * Displays the table popup menu, if one is available.<br>
		 * Default key stroke: CTRL-G
		 */
		public static final ControlKey<CommandControl> DISPLAY_POPUP_MENU = CommandControl.key("displayPopupMenu", keyStroke(VK_G, CTRL_DOWN_MASK));
		/**
		 * Displays the entity menu, if one is available.<br>
		 * Default key stroke: CTRL-ALT-V
		 */
		public static final ControlKey<CommandControl> DISPLAY_ENTITY_MENU = CommandControl.key("displayEntityMenu", keyStroke(VK_V, CTRL_DOWN_MASK | ALT_DOWN_MASK));
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
		 * A {@link Controls} instance containing a {@link ToggleControl} for each columns visibility.
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
		public static final ControlKey<ToggleControl> SELECTION_MODE = ToggleControl.key("selectionMode");
		/**
		 * A {@link Control} for clearing the data from the table.
		 * @see SwingEntityTableModel#clear()
		 */
		public static final ControlKey<CommandControl> CLEAR = CommandControl.key("clear");
		/**
		 * A {@link Control} for refreshing the table data.<br>
		 * Default key stroke: ALT-R
		 * @see SwingEntityTableModel#refresh()
		 */
		public static final ControlKey<CommandControl> REFRESH = CommandControl.key("refresh", keyStroke(VK_R, ALT_DOWN_MASK));
		/**
		 * A {@link ToggleControl} for showing/hiding the summary panel.
		 */
		public static final ControlKey<ToggleControl> TOGGLE_SUMMARY_PANEL = ToggleControl.key("toggleSummaryPanel");
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
		public static final ControlKey<CommandControl> COPY_CELL = CommandControl.key("copyCell", keyStroke(VK_C, CTRL_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * A {@link Control} for copying the table rows with header.
		 */
		public static final ControlKey<CommandControl> COPY_ROWS = CommandControl.key("copyRows");
		/**
		 * A {@link Controls} instance containing controls for copying either cell or table data.
		 * <li>{@link ControlKeys#COPY_ROWS ControlKeys#COPY_ROWS}</li>
		 * <li>{@link ControlKeys#COPY_CELL ControlKeys#COPY_CELL}</li>
		 * @see #COPY_CELL
		 * @see #COPY_ROWS
		 */
		public static final ControlsKey COPY_CONTROLS = Controls.key("copyControls", Controls.layout(asList(COPY_CELL, COPY_ROWS)));
		/**
		 * A {@link Controls} instance containing controls for configuring columns.
		 * <li>{@link ControlKeys#SELECT_COLUMNS ControlKeys#SELECT_COLUMNS} or {@link ControlKeys#TOGGLE_COLUMN_CONTROLS ControlKeys#TOGGLE_COLUMN_CONTROLS}</li>
		 * <li>{@link ControlKeys#RESET_COLUMNS ControlKeys#RESET_COLUMNS}</li>
		 * <li>{@link ControlKeys#SELECT_AUTO_RESIZE_MODE ControlKeys#SELECT_AUTO_RESIZE_MODE} or {@link ControlKeys#TOGGLE_AUTO_RESIZE_MODE_CONTROLS ControlKeys#TOGGLE_AUTO_RESIZE_MODE_CONTROLS}</li>
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
		public static final ControlKey<CommandControl> REQUEST_SEARCH_FIELD_FOCUS = CommandControl.key("requestSearchFieldFocus", keyStroke(VK_F, CTRL_DOWN_MASK));

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
	 * Specifies how auto-resize-mode selection is presented.
	 */
	public enum AutoResizeModeSelection {
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
	private final TablePanel tablePanel = new TablePanel();
	private final EntityEditPanel editPanel;
	private final TableConditionPanel<Attribute<?>> tableConditionPanel;
	private final Controls.Layout popupMenuLayout;
	private final Controls.Layout toolBarLayout;
	private final SwingEntityTableModel tableModel;
	private final Control conditionRefreshControl;
	private final JToolBar refreshButtonToolBar;
	private final List<Controls> additionalPopupControls = new ArrayList<>();
	private final List<Controls> additionalToolBarControls = new ArrayList<>();
	private final ScrollToColumn scrollToColumn = new ScrollToColumn();

	private JScrollPane conditionPanelScrollPane;
	private JScrollPane filterPanelScrollPane;
	private StatusPanel statusPanel;
	private FilterTableColumnComponentPanel<Attribute<?>> summaryPanel;
	private JScrollPane summaryPanelScrollPane;

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
		this.tableModel = requireNonNull(tableModel, "tableModel");
		this.editPanel = null;
		this.conditionRefreshControl = createConditionRefreshControl();
		this.configuration = configure(config);
		this.table = configuration.tableBuilder.build();
		this.tableConditionPanel = createTableConditionPanel();
		this.refreshButtonToolBar = createRefreshButtonToolBar();
		this.popupMenuLayout = createPopupMenuLayout();
		this.toolBarLayout = createToolBarLayout();
		createControls();
		bindTableEvents();
		applyPreferences();
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
		this.tableModel = requireNonNull(tableModel, "tableModel");
		this.editPanel = validateEditModel(requireNonNull(editPanel, "editPanel"));
		this.conditionRefreshControl = createConditionRefreshControl();
		this.configuration = configure(config);
		this.table = configuration.tableBuilder.build();
		this.tableConditionPanel = createTableConditionPanel();
		this.refreshButtonToolBar = createRefreshButtonToolBar();
		this.popupMenuLayout = createPopupMenuLayout();
		this.toolBarLayout = createToolBarLayout();
		createControls();
		bindTableEvents();
		applyPreferences();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(conditionPanelScrollPane, filterPanelScrollPane);
		Utilities.updateUI(tableConditionPanel);
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

		return (T) tableConditionPanel;
	}

	/**
	 * @return the {@link State} controlling whether the summary panel is visible
	 */
	public final State summaryPanelVisible() {
		return summaryPanelVisibleState;
	}

	/**
	 * Specifies whether the current sort order is used as a basis for the query order by clause.
	 * Note that this only applies to column attributes.
	 * @return the {@link State} controlling whether the current sort order should be used as a basis for the query order by clause
	 */
	public final State orderQueryBySortOrder() {
		return orderQueryBySortOrder;
	}

	/**
	 * Returns whether the values of hidden columns are included when querying data
	 * @return the {@link State} controlling whether the values of hidden columns are included when querying data
	 */
	public final State queryHiddenColumns() {
		return queryHiddenColumns;
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
							.edit(tableModel.selectionModel().selectedItems());
		}
	}

	/**
	 * Displays a dialog containing tables of entities depending on the selected entities via non-soft foreign keys
	 */
	public final void viewDependencies() {
		if (!tableModel.selectionModel().isSelectionEmpty()) {
			displayDependenciesDialog(tableModel.selectionModel().selectedItems(), tableModel.connectionProvider(), this);
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
	 * Saves user preferences
	 * @see #userPreferencesKey()
	 */
	public void savePreferences() {
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

	protected void setupKeyboardActions() {
		configuration.controlMap.keyEvent(REFRESH).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(REQUEST_TABLE_FOCUS).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(SELECT_CONDITION_PANEL).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(TOGGLE_CONDITION_PANEL).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(TOGGLE_FILTER_PANEL).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(SELECT_FILTER_PANEL).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(PRINT).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(ADD).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(EDIT).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(EDIT_SELECTED_ATTRIBUTE).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(DELETE).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(MOVE_SELECTION_UP).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(MOVE_SELECTION_DOWN).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(DISPLAY_ENTITY_MENU).ifPresent(keyEvent -> keyEvent.enable(table));
		configuration.controlMap.keyEvent(DISPLAY_POPUP_MENU).ifPresent(keyEvent -> keyEvent.enable(table));
	}

	/**
	 * Configures the toolbar controls layout.<br>
	 * Note that the {@link Controls.Layout} instance has pre-configured defaults,
	 * which must be cleared in order to start with an empty configuration.
	 * <pre>
	 *   configureToolBar(layout -> layout.clear()
	 *           .standard(ControlKeys.REFRESH)
	 *           .separator()
	 *           .control(createCustomControl())
	 *           .separator()
	 *           .defaults())
	 * </pre>
	 * Defaults:
	 * <ul>
	 *   <li>{@link ControlKeys#TOGGLE_SUMMARY_PANEL ControlKeys#TOGGLE_SUMMARY_PANEL}</li>
	 * 	 <li>{@link ControlKeys#TOGGLE_CONDITION_PANEL ControlKeys#TOGGLE_CONDITION_PANEL}</li>
	 * 	 <li>{@link ControlKeys#TOGGLE_FILTER_PANEL ControlKeys#TOGGLE_FILTER_PANEL}</li>
	 * 	 <li>Separator</li>
	 * 	 <li>{@link ControlKeys#ADD ControlKeys#ADD} (If an EditPanel is available)</li>
	 * 	 <li>{@link ControlKeys#EDIT ControlKeys#EDIT} (If an EditPanel is available)</li>
	 * 	 <li>{@link ControlKeys#DELETE ControlKeys#DELETE}</li>
	 * 	 <li>Separator</li>
	 * 	 <li>{@link ControlKeys#EDIT_SELECTED_ATTRIBUTE ControlKeys#EDIT_SELECTED_ATTRIBUTE}</li>
	 * 	 <li>Separator</li>
	 * 	 <li>{@link ControlKeys#PRINT ControlKeys#PRINT}</li>
	 * 	 <li>Separator</li>
	 * 	 <li>{@link ControlKeys#ADDITIONAL_TOOLBAR_CONTROLS ControlKeys#ADDITIONAL_TOOLBAR_CONTROLS}</li>
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
	 * <pre>
	 *   configurePopupMenu(layout -> layout.clear()
	 *           .standard(ControlKeys.REFRESH)
	 *           .separator()
	 *           .control(createCustomControl())
	 *           .separator()
	 *           .defaults())
	 * </pre>
	 * Defaults:
	 * <ul>
	 *   <li>{@link ControlKeys#REFRESH ControlKeys#REFRESH}</li>
	 *   <li>{@link ControlKeys#CLEAR ControlKeys#CLEAR}</li>
	 *   <li>Separator</li>
	 *   <li>{@link ControlKeys#ADD ControlKeys#ADD} (If an EditPanel is available)</li>
	 *   <li>{@link ControlKeys#EDIT ControlKeys#EDIT} (If an EditPanel is available)</li>
	 *   <li>{@link ControlKeys#DELETE ControlKeys#DELETE}</li>
	 *   <li>Separator</li>
	 *   <li>{@link ControlKeys#EDIT_SELECTED_ATTRIBUTE ControlKeys#EDIT_SELECTED_ATTRIBUTE} or {@link ControlKeys#EDIT_ATTRIBUTE_CONTROLS ControlKeys#EDIT_ATTRIBUTE_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link ControlKeys#VIEW_DEPENDENCIES ControlKeys#VIEW_DEPENDENCIES}</li>
	 *   <li>Separator</li>
	 *   <li>{@link ControlKeys#ADDITIONAL_POPUP_MENU_CONTROLS ControlKeys#ADDITIONAL_POPUP_MENU_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link ControlKeys#PRINT_CONTROLS ControlKeys#PRINT_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link ControlKeys#COLUMN_CONTROLS ControlKeys#COLUMN_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link ControlKeys#SELECTION_MODE ControlKeys#SELECTION_MODE}</li>
	 *   <li>Separator</li>
	 *   <li>{@link ControlKeys#CONDITION_CONTROLS ControlKeys#CONDITION_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link ControlKeys#FILTER_CONTROLS ControlKeys#FILTER_CONTROLS}</li>
	 *   <li>Separator</li>
	 *   <li>{@link ControlKeys#COPY_CONTROLS ControlKeys#COPY_CONTROLS}</li>
	 * </ul>
	 * @param popupMenuLayout provides access to the popup menu layout
	 * @see Controls.Layout#clear()
	 */
	protected final void configurePopupMenu(Consumer<Controls.Layout> popupMenuLayout) {
		throwIfInitialized();
		requireNonNull(popupMenuLayout).accept(this.popupMenuLayout);
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
			displayDependenciesDialog(tableModel.selectionModel().selectedItems(), tableModel.connectionProvider(),
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
	 * Returns the key used to identify user preferences for this table panel, that is column positions, widths and such.
	 * The default implementation is:
	 * <pre>
	 * {@code
	 * return tableModel().getClass().getSimpleName() + "-" + entityType();
	 * }
	 * </pre>
	 * Override in case this key is not unique within the application.
	 * @return the key used to identify user preferences for this table panel
	 */
	protected String userPreferencesKey() {
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
	private CommandControl createAddControl() {
		return Control.builder()
						.command(() -> addEntityDialog(() -> editPanel)
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
	private CommandControl createEditControl() {
		return Control.builder()
						.command(() -> editEntityDialog(() -> editPanel)
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
	private CommandControl createEditSelectedAttributeControl() {
		return Control.builder()
						.command(this::editSelected)
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
		ControlsBuilder builder = Controls.builder()
						.name(FrameworkMessages.edit())
						.enabled(editSelectedEnabledObserver)
						.smallIcon(ICONS.edit())
						.description(FrameworkMessages.editSelectedTip());
		configuration.editable.get().stream()
						.map(attribute -> tableModel.entityDefinition().attributes().definition(attribute))
						.sorted(AttributeDefinition.definitionComparator())
						.forEach(attributeDefinition -> builder.control(Control.builder()
										.command(() -> editSelected(attributeDefinition.attribute()))
										.name(attributeDefinition.caption() == null ? attributeDefinition.attribute().name() : attributeDefinition.caption())
										.enabled(editSelectedEnabledObserver)
										.build()));
		Controls editControls = builder.build();

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
	private CommandControl createViewDependenciesControl() {
		return Control.builder()
						.command(this::viewDependencies)
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
	private CommandControl createDeleteControl() {
		return Control.builder()
						.command(new DeleteCommand())
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
	private CommandControl createRefreshControl() {
		return Control.builder()
						.command(tableModel::refresh)
						.name(Messages.refresh())
						.description(Messages.refreshTip())
						.mnemonic(Messages.refreshMnemonic())
						.smallIcon(ICONS.refresh())
						.enabled(tableModel.refresher().observer().not())
						.build();
	}

	/**
	 * @return a Control for clearing the underlying table model, that is, removing all rows
	 */
	private CommandControl createClearControl() {
		return Control.builder()
						.command(tableModel::clear)
						.name(Messages.clear())
						.description(Messages.clearTip())
						.mnemonic(Messages.clearMnemonic())
						.smallIcon(ICONS.clear())
						.build();
	}

	private Controls createPrintControls() {
		ControlsBuilder builder = Controls.builder()
						.name(Messages.print())
						.mnemonic(Messages.printMnemonic())
						.smallIcon(ICONS.print());
		control(PRINT).optional().ifPresent(builder::control);

		Controls printControls = builder.build();

		return printControls.empty() ? null : printControls;
	}

	private Controls createAdditionalPopupControls() {
		ControlsBuilder builder = Controls.builder();
		additionalPopupControls.forEach(additionalControls -> {
			if (!additionalControls.name().isPresent()) {
				builder.actions(additionalControls.actions());
			}
			else {
				builder.control(additionalControls);
			}
		});
		Controls additionalControls = builder.build();

		return additionalControls.empty() ? null : additionalControls;
	}

	private Controls createAdditionalToolbarControls() {
		ControlsBuilder builder = Controls.builder();
		additionalToolBarControls.forEach(additionalControls -> {
			if (!additionalControls.name().isPresent()) {
				builder.actions(additionalControls.actions());
			}
			else {
				builder.control(additionalControls);
			}
		});
		Controls additionalControls = builder.build();

		return additionalControls.empty() ? null : additionalControls;
	}

	private CommandControl createToggleConditionPanelControl() {
		return Control.builder()
						.command(this::toggleConditionPanel)
						.smallIcon(ICONS.search())
						.description(MESSAGES.getString("show_condition_panel"))
						.build();
	}

	private CommandControl createSelectConditionPanelControl() {
		return commandControl(() -> conditionPanel().selectConditionPanel(this));
	}

	private Controls createConditionControls() {
		if (!configuration.includeConditionPanel || tableConditionPanel == null) {
			return null;
		}
		ControlsBuilder builder = Controls.builder()
						.name(FrameworkMessages.searchNoun())
						.smallIcon(ICONS.search());
		Controls conditionPanelControls = tableConditionPanel.controls();
		if (conditionPanelControls.notEmpty()) {
			builder.actions(conditionPanelControls.actions());
			builder.separator();
		}
		builder.control(Control.builder()
						.toggle(tableModel.conditionRequired())
						.name(MESSAGES.getString("require_query_condition"))
						.description(MESSAGES.getString("require_query_condition_description"))
						.build());

		Controls conditionControls = builder.build();

		return conditionControls.empty() ? null : conditionControls;
	}

	private CommandControl createToggleFilterPanelControl() {
		return Control.builder()
						.command(this::toggleFilterPanel)
						.smallIcon(ICONS.filter())
						.description(MESSAGES.getString("show_filter_panel"))
						.build();
	}

	private CommandControl createSelectFilterPanelControl() {
		return commandControl(() -> table.filterPanel().selectConditionPanel(this));
	}

	private void toggleConditionPanel() {
		Value<ConditionState> conditionState = conditionPanel().state();
		switch (conditionState.get()) {
			case HIDDEN:
				conditionState.set(SIMPLE);
				break;
			case SIMPLE:
				conditionState.set(ADVANCED);
				break;
			case ADVANCED:
				setConditionStateHidden(conditionPanelScrollPane, conditionState);
				break;
		}
	}

	private void toggleFilterPanel() {
		Value<ConditionState> conditionState = table.filterPanel().state();
		switch (conditionState.get()) {
			case HIDDEN:
				conditionState.set(SIMPLE);
				break;
			case SIMPLE:
				conditionState.set(ADVANCED);
				break;
			case ADVANCED:
				setConditionStateHidden(filterPanelScrollPane, conditionState);
				break;
		}
	}

	private Controls createFilterControls() {
		if (!configuration.includeFilterPanel) {
			return null;
		}
		ControlsBuilder builder = Controls.builder()
						.name(FrameworkMessages.filterNoun())
						.smallIcon(ICONS.filter());
		Controls filterPanelControls = table.filterPanel().controls();
		if (filterPanelControls.notEmpty()) {
			builder.actions(filterPanelControls.actions());
		}
		Controls filterControls = builder.build();

		return filterControls.empty() ? null : filterControls;
	}

	private ToggleControl createToggleSummaryPanelControl() {
		return Control.builder()
						.toggle(summaryPanelVisibleState)
						.smallIcon(ICONS.summary())
						.description(MESSAGES.getString("toggle_summary_tip"))
						.build();
	}

	private CommandControl createClearSelectionControl() {
		return Control.builder()
						.command(tableModel.selectionModel()::clearSelection)
						.enabled(tableModel.selectionModel().selectionNotEmpty())
						.smallIcon(ICONS.clearSelection())
						.description(MESSAGES.getString("clear_selection_tip"))
						.build();
	}

	private CommandControl createMoveSelectionDownControl() {
		return Control.builder()
						.command(tableModel.selectionModel()::moveSelectionDown)
						.smallIcon(ICONS.down())
						.description(MESSAGES.getString("selection_down_tip"))
						.build();
	}

	private CommandControl createMoveSelectionUpControl() {
		return Control.builder()
						.command(tableModel.selectionModel()::moveSelectionUp)
						.smallIcon(ICONS.up())
						.description(MESSAGES.getString("selection_up_tip"))
						.build();
	}

	private CommandControl createRequestTableFocusControl() {
		return commandControl(table::requestFocus);
	}

	private CommandControl createRequestSearchFieldFocusControl() {
		return commandControl(table.searchField()::requestFocusInWindow);
	}

	private Controls createColumnControls() {
		ControlsBuilder builder = Controls.builder()
						.name(MESSAGES.getString("columns"))
						.smallIcon(ICONS.columns());
		if (configuration.columnSelection == ColumnSelection.DIALOG) {
			control(SELECT_COLUMNS).optional().ifPresent(builder::control);
		}
		else {
			control(TOGGLE_COLUMN_CONTROLS).optional().ifPresent(builder::control);
		}
		control(RESET_COLUMNS).optional().ifPresent(builder::control);
		if (configuration.autoResizeModeSelection == AutoResizeModeSelection.DIALOG) {
			control(SELECT_AUTO_RESIZE_MODE).optional().ifPresent(builder::control);
		}
		else {
			control(TOGGLE_AUTO_RESIZE_MODE_CONTROLS).optional().ifPresent(builder::control);
		}

		Controls columnControls = builder.build();

		return columnControls.empty() ? null : columnControls;
	}

	private Controls createCopyControls() {
		ControlsBuilder builder = Controls.builder()
						.name(Messages.copy())
						.smallIcon(ICONS.copy());
		control(COPY_CELL).optional().ifPresent(builder::control);
		control(COPY_ROWS).optional().ifPresent(builder::control);

		Controls copyControls = builder.build();

		return copyControls.empty() ? null : copyControls;
	}

	private CommandControl createCopyRowsControl() {
		return Control.builder()
						.command(table::copyToClipboard)
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

	private boolean includeViewDependenciesControl() {
		return tableModel.entities().definitions().stream()
						.flatMap(entityDefinition -> entityDefinition.foreignKeys().definitions().stream())
						.filter(foreignKeyDefinition -> !foreignKeyDefinition.soft())
						.anyMatch(foreignKeyDefinition -> foreignKeyDefinition.attribute().referencedType().equals(tableModel.entityType()));
	}

	private boolean includeToggleSummaryPanelControl() {
		return configuration.includeSummaryPanel && containsSummaryModels(table);
	}

	private Control createConditionRefreshControl() {
		return Control.builder()
						.command(tableModel::refresh)
						.enabled(tableModel.conditionChanged())
						.smallIcon(ICONS.refreshRequired())
						.build();
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
										(tableConditionPanel != null && tableConditionPanel.state().isNotEqualTo(ConditionState.HIDDEN)))
						.build();
	}

	private TableConditionPanel<Attribute<?>> createTableConditionPanel() {
		if (configuration.includeConditionPanel) {
			TableConditionPanel<Attribute<?>> conditionPanel = configuration.tableConditionPanelFactory
							.create(tableModel.conditionModel(), createColumnConditionPanels(),
											table.getColumnModel(), this::configureTableConditionPanel);
			KeyEvents.builder(VK_ENTER)
							.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
							.action(conditionRefreshControl)
							.enable(conditionPanel);
			conditionPanel.state().addConsumer(tablePanel::conditionPanelStateChanged);

			return conditionPanel;
		}

		return null;
	}

	private Collection<ColumnConditionPanel<Attribute<?>, ?>> createColumnConditionPanels() {
		return tableModel.conditionModel().conditionModels().values().stream()
						.filter(conditionModel -> table.columnModel().containsColumn(conditionModel.identifier()))
						.filter(conditionModel -> configuration.conditionFieldFactory.supportsType(conditionModel.columnClass()))
						.map(this::createColumnConditionPanel)
						.collect(toList());
	}

	private FilterColumnConditionPanel<Attribute<?>, ?> createColumnConditionPanel(ColumnConditionModel<Attribute<?>, ?> conditionModel) {
		return FilterColumnConditionPanel.builder(conditionModel)
						.fieldFactory(configuration.conditionFieldFactory)
						.tableColumn(table.columnModel().column(conditionModel.identifier()))
						.caption(Objects.toString(table.columnModel().column(conditionModel.identifier()).getHeaderValue()))
						.build();
	}

	private void configureTableConditionPanel(TableConditionPanel<Attribute<?>> tableConditionPanel) {
		tableConditionPanel.conditionPanels().forEach(this::configureColumnConditionPanel);
	}

	private void configureColumnConditionPanel(ColumnConditionPanel<Attribute<?>, ?> conditionPanel) {
		conditionPanel.focusGainedEvent().ifPresent(focusGainedEvent ->
						focusGainedEvent.addConsumer(scrollToColumn));
		conditionPanel.components().forEach(this::enableConditionPanelRefreshOnEnter);
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
		if (configuration.includeFilterPanel) {
			table.filterPanel().conditionPanels().forEach(conditionPanel ->
							conditionPanel.focusGainedEvent().ifPresent(focusGainedEvent ->
											focusGainedEvent.addConsumer(scrollToColumn)));
		}
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
		table.getColumnModel().columns().forEach(this::configureColumn);
		summaryPanelVisibleState.addValidator(new ComponentAvailableValidator(summaryPanel, "summary"));
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
			controlMap.control(EDIT_SELECTED_ATTRIBUTE).set(createEditSelectedAttributeControl());
		}
		if (configuration.includeClearControl) {
			controlMap.control(CLEAR).set(createClearControl());
		}
		controlMap.control(REFRESH).set(createRefreshControl());
		controlMap.control(SELECT_COLUMNS).set(table.createSelectColumnsControl());
		controlMap.control(TOGGLE_COLUMN_CONTROLS).set(table.createToggleColumnsControls());
		controlMap.control(RESET_COLUMNS).set(table.createResetColumnsControl());
		controlMap.control(SELECT_AUTO_RESIZE_MODE).set(table.createSelectAutoResizeModeControl());
		controlMap.control(TOGGLE_AUTO_RESIZE_MODE_CONTROLS).set(table.createToggleAutoResizeModelControls());
		if (includeViewDependenciesControl()) {
			controlMap.control(VIEW_DEPENDENCIES).set(createViewDependenciesControl());
		}
		if (includeToggleSummaryPanelControl()) {
			controlMap.control(TOGGLE_SUMMARY_PANEL).set(createToggleSummaryPanelControl());
		}
		if (configuration.includeConditionPanel) {
			controlMap.control(TOGGLE_CONDITION_PANEL).set(createToggleConditionPanelControl());
			controlMap.control(SELECT_CONDITION_PANEL).set(createSelectConditionPanelControl());
		}
		if (configuration.includeFilterPanel) {
			controlMap.control(TOGGLE_FILTER_PANEL).set(createToggleFilterPanelControl());
			controlMap.control(SELECT_FILTER_PANEL).set(createSelectFilterPanelControl());
		}
		controlMap.control(CLEAR_SELECTION).set(createClearSelectionControl());
		controlMap.control(MOVE_SELECTION_UP).set(createMoveSelectionUpControl());
		controlMap.control(MOVE_SELECTION_DOWN).set(createMoveSelectionDownControl());
		controlMap.control(COPY_CELL).set(table.createCopyCellControl());
		controlMap.control(COPY_ROWS).set(createCopyRowsControl());
		if (configuration.includeEntityMenu) {
			controlMap.control(DISPLAY_ENTITY_MENU).set(commandControl(this::showEntityMenu));
		}
		if (configuration.includePopupMenu) {
			controlMap.control(DISPLAY_POPUP_MENU).set(commandControl(this::showPopupMenu));
		}
		if (configuration.includeSelectionModeControl) {
			controlMap.control(SELECTION_MODE).set(table.createSingleSelectionModeControl());
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
			if (popupControls == null || popupControls.empty()) {
				return;
			}

			JPopupMenu popupMenu = menu(popupControls).createPopupMenu();
			table.setComponentPopupMenu(popupMenu);
			tableScrollPane.setComponentPopupMenu(popupMenu);
		}
	}

	private void addDoubleClickAction() {
		if (table.doubleClickAction().isNull()) {
			control(EDIT).optional().ifPresent(table.doubleClickAction()::set);
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

	private void setConditionStateHidden(JScrollPane scrollPane, Value<ConditionState> conditionState) {
		boolean parentOfFocusOwner = parentOfType(JScrollPane.class,
						getCurrentKeyboardFocusManager().getFocusOwner()) == scrollPane;
		conditionState.set(ConditionState.HIDDEN);
		if (parentOfFocusOwner) {
			table.requestFocusInWindow();
		}
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
			Attribute<?> attribute = column.identifier();
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
						.filter(columnSortOrder -> isColumn(columnSortOrder.identifier()))
						.forEach(columnSortOrder -> {
							switch (columnSortOrder.sortOrder()) {
								case ASCENDING:
									builder.ascending((Column<?>) columnSortOrder.identifier());
									break;
								case DESCENDING:
									builder.descending((Column<?>) columnSortOrder.identifier());
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

	private void applyColumnPreferences(String preferencesString) {
		List<Attribute<?>> columnAttributes = table.getColumnModel().columns().stream()
						.map(FilterTableColumn::identifier)
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

	private Controls.Layout createPopupMenuLayout() {
		return Controls.layout(asList(
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

	private Controls.Layout createToolBarLayout() {
		return Controls.layout(asList(
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
				List<Entity> selectedItems = tableModel().selectionModel().selectedItems();
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
		return table.getColumnModel().columns().stream()
						.map(FilterTableColumn::identifier)
						.map(table.summaryModel()::summaryModel)
						.anyMatch(Optional::isPresent);
	}

	private final class ScrollToColumn implements Consumer<Attribute<?>> {
		@Override
		public void accept(Attribute<?> attribute) {
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
							&& tableModel.conditionModel().enabled(tableColumn.identifier());
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

	private static final class ReplaceIfNull implements Function<Controls, Controls> {

		private final Supplier<Controls> controls;

		private ReplaceIfNull(Supplier<Controls> controls) {
			this.controls = controls;
		}

		@Override
		public Controls apply(Controls control) {
			return control == null ? controls.get() : control;
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
		 * Specifies how column selection is presented to the user.<br>
		 * Value type: {@link AutoResizeModeSelection}<br>
		 * Default value: {@link AutoResizeModeSelection#DIALOG}
		 */
		public static final PropertyValue<AutoResizeModeSelection> AUTO_RESIZE_MODE_SELECTION =
						Configuration.enumValue(EntityTablePanel.class.getName() + ".autoResizeModeSelection", AutoResizeModeSelection.class, AutoResizeModeSelection.DIALOG);

		/**
		 * Specifies how the edit an attribute action is presented to the user.<br>
		 * Value type: {@link EditAttributeSelection}<br>
		 * Default value: {@link EditAttributeSelection#MENU}
		 */
		public static final PropertyValue<EditAttributeSelection> EDIT_ATTRIBUTE_SELECTION =
						Configuration.enumValue(EntityTablePanel.class.getName() + ".editAttributeSelection", EditAttributeSelection.class, EditAttributeSelection.MENU);

		private static final Function<SwingEntityTableModel, String> DEFAULT_STATUS_MESSAGE = new DefaultStatusMessage();

		private final EntityTablePanel tablePanel;
		private final EntityDefinition entityDefinition;
		private final ValueSet<Attribute<?>> editable;
		private final Map<Attribute<?>, EntityComponentFactory<?, ?, ?>> editComponentFactories;
		private final FilterTable.Builder<Entity, Attribute<?>> tableBuilder;

		private TableConditionPanel.Factory<Attribute<?>> tableConditionPanelFactory = new DefaultTableConditionPanelFactory();
		private FieldFactory<Attribute<?>> conditionFieldFactory;
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
		private AutoResizeModeSelection autoResizeModeSelection = AUTO_RESIZE_MODE_SELECTION.get();
		private EditAttributeSelection editAttributeSelection = EDIT_ATTRIBUTE_SELECTION.get();
		private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling;
		private RefreshButtonVisible refreshButtonVisible;
		private Function<SwingEntityTableModel, String> statusMessage = DEFAULT_STATUS_MESSAGE;
		private boolean showRefreshProgressBar = SHOW_REFRESH_PROGRESS_BAR.get();
		private Confirmer deleteConfirmer;

		final ControlMap controlMap;

		private Config(EntityTablePanel tablePanel) {
			this.tablePanel = tablePanel;
			this.entityDefinition = tablePanel.tableModel.entityDefinition();
			this.tableBuilder = FilterTable.builder(tablePanel.tableModel, entityTableColumns(entityDefinition))
							.summaryValuesFactory(new EntitySummaryValuesFactory(entityDefinition, tablePanel.tableModel))
							.cellRendererFactory(new EntityTableCellRendererFactory(tablePanel.tableModel))
							.cellEditorFactory(new EntityTableCellEditorFactory(tablePanel.tableModel.editModel()))
							.onBuild(filterTable -> filterTable.setRowHeight(filterTable.getFont().getSize() + FONT_SIZE_TO_ROW_HEIGHT));
			this.tableConditionPanelFactory = new DefaultTableConditionPanelFactory();
			this.conditionFieldFactory = new EntityConditionFieldFactory(entityDefinition);
			this.controlMap = ControlMap.controlMap(ControlKeys.class);
			this.editable = valueSet(entityDefinition.attributes().updatable().stream()
							.map(AttributeDefinition::attribute)
							.collect(toSet()));
			this.editable.addValidator(new EditMenuAttributeValidator(entityDefinition));
			this.editComponentFactories = new HashMap<>();
			this.referentialIntegrityErrorHandling = ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.get();
			this.refreshButtonVisible = RefreshButtonVisible.WHEN_CONDITION_PANEL_IS_VISIBLE;
			this.deleteConfirmer = new DeleteConfirmer(tablePanel.tableModel.selectionModel());
		}

		private Config(Config config) {
			this.tablePanel = config.tablePanel;
			this.entityDefinition = config.entityDefinition;
			this.tableBuilder = config.tableBuilder;
			this.controlMap = config.controlMap.copy();
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
			this.autoResizeModeSelection = config.autoResizeModeSelection;
			this.editAttributeSelection = config.editAttributeSelection;
			this.editComponentFactories = new HashMap<>(config.editComponentFactories);
			this.referentialIntegrityErrorHandling = config.referentialIntegrityErrorHandling;
			this.refreshButtonVisible = config.refreshButtonVisible;
			this.statusMessage = config.statusMessage;
			this.showRefreshProgressBar = config.showRefreshProgressBar;
			this.deleteConfirmer = config.deleteConfirmer;
			this.includeToolBar = config.includeToolBar;
			this.tableConditionPanelFactory = config.tableConditionPanelFactory;
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
		 * @param tableConditionPanelFactory the table condition panel factory
		 * @return this Config instance
		 */
		public Config tableConditionPanelFactory(TableConditionPanel.Factory<Attribute<?>> tableConditionPanelFactory) {
			this.tableConditionPanelFactory = requireNonNull(tableConditionPanelFactory);
			return this;
		}

		/**
		 * @param conditionFieldFactory the condition field factory
		 * @return this Config instance
		 * @see EntityTablePanel#conditionPanel()
		 */
		public Config conditionFieldFactory(FieldFactory<Attribute<?>> conditionFieldFactory) {
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
		 * @param autoResizeModeSelection specifies how auto-resize-mode is selected
		 * @return this Config instance
		 */
		public Config autoResizeModeSelection(AutoResizeModeSelection autoResizeModeSelection) {
			this.autoResizeModeSelection = requireNonNull(autoResizeModeSelection);
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
		 * @param controlKey the control key
		 * @param keyStroke provides access to the {@link Value} controlling the key stroke for the given control
		 * @return this Config instance
		 */
		public Config keyStroke(ControlKey<?> controlKey, Consumer<Value<KeyStroke>> keyStroke) {
			requireNonNull(keyStroke).accept(controlMap.keyStroke(controlKey));
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

		private ControlKey<?> popupMenuEditAttributeControl() {
			return editAttributeSelection == EditAttributeSelection.MENU ?
							EDIT_ATTRIBUTE_CONTROLS :
							EDIT_SELECTED_ATTRIBUTE;
		}

		private static final class DefaultTableConditionPanelFactory
						implements TableConditionPanel.Factory<Attribute<?>> {

			@Override
			public TableConditionPanel<Attribute<?>> create(TableConditionModel<Attribute<?>> conditionModel,
																											Collection<ColumnConditionPanel<Attribute<?>, ?>> columnConditionPanels,
																											FilterTableColumnModel<Attribute<?>> columnModel,
																											Consumer<TableConditionPanel<Attribute<?>>> onPanelInitialized) {
				return filterTableConditionPanel(conditionModel, columnConditionPanels, columnModel, onPanelInitialized);
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

	private static final class EntitySummaryValuesFactory implements ColumnSummaryModel.SummaryValues.Factory<Attribute<?>> {

		private final EntityDefinition entityDefinition;
		private final FilterTableModel<?, Attribute<?>> tableModel;

		private EntitySummaryValuesFactory(EntityDefinition entityDefinition, FilterTableModel<?, Attribute<?>> tableModel) {
			this.entityDefinition = requireNonNull(entityDefinition);
			this.tableModel = requireNonNull(tableModel);
		}

		@Override
		public <T extends Number> Optional<ColumnSummaryModel.SummaryValues<T>> createSummaryValues(Attribute<?> identifier, Format format) {
			AttributeDefinition<?> attributeDefinition = entityDefinition.attributes().definition(identifier);
			if (identifier.type().isNumerical() && attributeDefinition.items().isEmpty()) {
				return Optional.of(FilterTable.summaryValues(identifier, tableModel, format));
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
			int rowCount = tableModel.rowCount();
			int filteredCount = tableModel.filteredCount();
			if (rowCount == 0 && filteredCount == 0) {
				return "";
			}
			int selectionCount = tableModel.selectionModel().selectionCount();
			StringBuilder builder = new StringBuilder();
			if (tableModel.limit().isEqualTo(tableModel.rowCount())) {
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
			if (configuration.includeFilterPanel) {
				filterPanelScrollPane = createLinkedScrollPane(table.filterPanel());
				table.filterPanel().state().addConsumer(this::filterPanelStateChanged);
				if (table.filterPanel().state().isNotEqualTo(ConditionState.HIDDEN)) {
					tableSouthPanel.add(filterPanelScrollPane, BorderLayout.SOUTH);
				}
			}
		}

		private void conditionPanelStateChanged(ConditionState conditionState) {
			initializeConditionPanel();
			refreshButtonToolBar.setVisible(configuration.refreshButtonVisible == RefreshButtonVisible.ALWAYS
							|| conditionState != ConditionState.HIDDEN);
			if (conditionState == ConditionState.HIDDEN) {
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
				if (tableConditionPanel.state().isNotEqualTo(ConditionState.HIDDEN)) {
					tablePanel.add(conditionPanelScrollPane, BorderLayout.NORTH);
				}
				refreshButtonToolBar.setVisible(configuration.refreshButtonVisible == RefreshButtonVisible.ALWAYS
								|| tableConditionPanel.state().isNotEqualTo(ConditionState.HIDDEN));
			}
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

		private void filterPanelStateChanged(ConditionState conditionState) {
			if (conditionState == ConditionState.HIDDEN) {
				tableSouthPanel.remove(filterPanelScrollPane);
			}
			else {
				tableSouthPanel.add(filterPanelScrollPane, BorderLayout.SOUTH);
			}
			revalidate();
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
							table.summaryModel().summaryModel(column.identifier())
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

		private JToolBar createToolBar() {
			Controls toolbarControls = toolBarLayout.create(configuration.controlMap);
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

		private final Value<String> statusMessage = Value.builder()
						.nonNull("")
						.initialValue(configuration.statusMessage.apply(tableModel))
						.build();
		private final JLabel label = Components.label(statusMessage)
						.horizontalAlignment(SwingConstants.CENTER)
						.build();
		private final JProgressBar progressBar = Components.progressBar()
						.indeterminate(true)
						.string(MESSAGES.getString("refreshing"))
						.stringPainted(true)
						.build();
		private final JPanel progressPanel = Components.panel(new GridBagLayout())
						.add(progressBar, createHorizontalFillConstraints())
						.build();

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

		@Override
		public void updateUI() {
			super.updateUI();
			Utilities.updateUI(label, progressBar, progressPanel);
		}

		private JPopupMenu createLimitMenu() {
			JPopupMenu popupMenu = new JPopupMenu();
			popupMenu.add(Control.builder()
							.command(this::configureLimit)
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
