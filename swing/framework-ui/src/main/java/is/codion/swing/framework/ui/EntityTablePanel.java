/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.i18n.Messages;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.ui.Cursors;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilteredTableCellRendererFactory;
import is.codion.swing.common.ui.component.table.FilteredTableColumnComponentPanel;
import is.codion.swing.common.ui.component.table.FilteredTableConditionPanel;
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

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.common.value.Value.value;
import static is.codion.common.value.ValueSet.valueSet;
import static is.codion.swing.common.ui.Utilities.*;
import static is.codion.swing.common.ui.component.Components.menu;
import static is.codion.swing.common.ui.component.Components.toolBar;
import static is.codion.swing.common.ui.component.table.ColumnSummaryPanel.columnSummaryPanel;
import static is.codion.swing.common.ui.component.table.FilteredTableColumnComponentPanel.filteredTableColumnComponentPanel;
import static is.codion.swing.common.ui.component.table.FilteredTableConditionPanel.filteredTableConditionPanel;
import static is.codion.swing.common.ui.dialog.Dialogs.progressWorkerDialog;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.displayDependenciesDialog;
import static is.codion.swing.framework.ui.EntityTablePanel.KeyboardShortcut.*;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Objects.requireNonNull;
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
 * |                entityTable (FilteredTable)         |
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

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityTablePanel.class.getName());
  private static final ResourceBundle EDIT_PANEL_MESSAGES = ResourceBundle.getBundle(EntityEditPanel.class.getName());

  /**
   * Specifies whether table condition panels should be visible or not by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> CONDITION_PANEL_VISIBLE =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.conditionPanelVisible", false);

  /**
   * Specifies whether table filter panels should be visible or not by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> FILTER_PANEL_VISIBLE =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.filterPanelVisible", false);

  /**
   * Specifies whether to include the default popup menu on entity tables<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_POPUP_MENU =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includePopupMenu", true);

  /**
   * Specifies whether to include a {@link EntityPopupMenu} on this table, triggered with CTRL-ALT-V.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_ENTITY_MENU =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includeEntityMenu", true);

  /**
   * Specifies whether to include a 'Clear' control in the popup menu.<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> INCLUDE_CLEAR_CONTROL =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includeClearControl", false);

  /**
   * Specifies whether to include a condition panel.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_CONDITION_PANEL =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includeConditionPanel", true);

  /**
   * Specifies whether to include a filter panel.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_FILTER_PANEL =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includeFilterPanel", false);

  /**
   * Specifies whether to include a summary panel.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_SUMMARY_PANEL =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includeSummaryPanel", true);

  /**
   * Specifies whether to include a popup menu for configuring the table model limit.<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> INCLUDE_LIMIT_MENU =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includeLimitMenu", false);

  /**
   * Specifies whether to show an indeterminate progress bar while the model is refreshing.<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> SHOW_REFRESH_PROGRESS_BAR =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.showRefreshProgressBar", false);

  /**
   * Specifies whether the refresh button should always be visible or only when the condition panel is visible<br>
   * Value type: Boolean<br>
   * Default value: {@link RefreshButtonVisible#WHEN_CONDITION_PANEL_IS_VISIBLE}
   */
  public static final PropertyValue<RefreshButtonVisible> REFRESH_BUTTON_VISIBLE =
          Configuration.enumValue("is.codion.swing.framework.ui.EntityTablePanel.refreshButtonVisible",
                  RefreshButtonVisible.class, RefreshButtonVisible.WHEN_CONDITION_PANEL_IS_VISIBLE);

  /**
   * Specifies how column selection is presented to the user.<br>
   * Value type: {@link ColumnSelection}<br>
   * Default value: {@link ColumnSelection#DIALOG}
   */
  public static final PropertyValue<ColumnSelection> COLUMN_SELECTION =
          Configuration.enumValue("is.codion.swing.framework.ui.EntityTablePanel.columnSelection", ColumnSelection.class, ColumnSelection.DIALOG);

  /**
   * The default keyboard shortcut keyStrokes.
   */
  public static final KeyboardShortcuts<KeyboardShortcut> KEYBOARD_SHORTCUTS =
          keyboardShortcuts(KeyboardShortcut.class, EntityTablePanel::defaultKeyStroke);

  /**
   * The keyboard shortcuts available for {@link EntityTablePanel}s.
   * Note that changing the shortcut keystroke after the panel
   * has been initialized has no effect.
   */
  public enum KeyboardShortcut {
    /**
     * Requests focus for the table.
     */
    REQUEST_TABLE_FOCUS,
    /**
     * Toggles the condition panel between hidden, visible and advanced.
     */
    TOGGLE_CONDITION_PANEL,
    /**
     * Displays a dialog for selecting a column condition panel.
     */
    SELECT_CONDITION_PANEL,
    /**
     * Toggles the filter panel between hidden, visible and advanced.
     */
    TOGGLE_FILTER_PANEL,
    /**
     * Displays a dialog for selecting a column filter panel.
     */
    SELECT_FILTER_PANEL,
    /**
     * Triggers the {@link TableControl#PRINT} control.
     */
    PRINT,
    /**
     * Triggers the {@link TableControl#DELETE_SELECTED} control.
     */
    DELETE_SELECTED,
    /**
     * Displays the table popup menu, if one is available.
     */
    DISPLAY_POPUP_MENU
  }

  /**
   * The standard controls available in a table panel
   */
  public enum TableControl {
    PRINT,
    DELETE_SELECTED,
    VIEW_DEPENDENCIES,
    EDIT_SELECTED,
    SELECT_COLUMNS,
    RESET_COLUMNS,
    COLUMN_AUTO_RESIZE_MODE,
    SELECTION_MODE,
    CLEAR,
    REFRESH,
    TOGGLE_SUMMARY_PANEL,
    TOGGLE_CONDITION_PANEL,
    CONDITION_PANEL_VISIBLE,
    TOGGLE_FILTER_PANEL,
    FILTER_PANEL_VISIBLE,
    CLEAR_SELECTION,
    MOVE_SELECTION_UP,
    MOVE_SELECTION_DOWN,
    COPY_TABLE_DATA,
    REQUEST_TABLE_FOCUS,
    SELECT_CONDITION_PANEL,
    SELECT_FILTER_PANEL,
    CONFIGURE_COLUMNS
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

  private static final int FONT_SIZE_TO_ROW_HEIGHT = 4;
  private static final Function<SwingEntityTableModel, String> DEFAULT_STATUS_MESSAGE = new DefaultStatusMessage();

  private final State conditionPanelVisibleState = State.state(CONDITION_PANEL_VISIBLE.get());
  private final State filterPanelVisibleState = State.state(FILTER_PANEL_VISIBLE.get());
  private final State summaryPanelVisibleState = State.state();

  private final Map<TableControl, Value<Control>> controls = createControlsMap();
  private final Config configuration;
  private final SwingEntityTableModel tableModel;
  private final Value<Confirmer> deleteConfirmer = createDeleteConfirmer();
  private final Control conditionRefreshControl;
  private final JToolBar refreshButtonToolBar;
  private final List<Controls> additionalPopupControls = new ArrayList<>();
  private final List<Controls> additionalToolBarControls = new ArrayList<>();

  private FilteredTable<Entity, Attribute<?>> table;
  private StatusPanel statusPanel;
  private JScrollPane tableScrollPane;
  private FilteredTableConditionPanel<Attribute<?>> conditionPanel;
  private JScrollPane conditionPanelScrollPane;
  private JScrollPane filterPanelScrollPane;
  private FilteredTableColumnComponentPanel<Attribute<?>, JPanel> summaryPanel;
  private JScrollPane summaryPanelScrollPane;
  private TablePanel tablePanel;

  private boolean initialized = false;

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the SwingEntityTableModel instance
   */
  public EntityTablePanel(SwingEntityTableModel tableModel) {
    this(tableModel, c -> {});
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the SwingEntityTableModel instance
   * @param configuration provides access to the table panel configuration
   */
  public EntityTablePanel(SwingEntityTableModel tableModel, Consumer<Config> configuration) {
    this.tableModel = requireNonNull(tableModel, "tableModel");
    this.conditionRefreshControl = createConditionRefreshControl();
    this.configuration = configure(tableModel.entityDefinition(), configuration);
    this.refreshButtonToolBar = createRefreshButtonToolBar();
  }

  /**
   * @return the table
   */
  public final FilteredTable<Entity, Attribute<?>> table() {
    if (table == null) {
      table = createTable();
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
   * @return the condition panel
   * @throws IllegalStateException in case no condition panel is available
   */
  public final FilteredTableConditionPanel<Attribute<?>> conditionPanel() {
    if (conditionPanel == null) {
      conditionPanel = createConditionPanel();
      if (conditionPanel == null) {
        throw new IllegalStateException("No condition panel is available");
      }
    }

    return conditionPanel;
  }

  /**
   * @return the state controlling whether the condition panel is visible
   */
  public final State conditionPanelVisible() {
    return conditionPanelVisibleState;
  }

  /**
   * @return the state controlling whether the filter panel is visible
   */
  public final State filterPanelVisible() {
    return filterPanelVisibleState;
  }

  /**
   * @return the state controlling whether the summary panel is visible
   */
  public final State summaryPanelVisible() {
    return summaryPanelVisibleState;
  }

  /**
   * Toggles the condition panel through the states hidden, visible and advanced
   */
  public final void toggleConditionPanel() {
    if (conditionPanelScrollPane != null) {
      toggleConditionPanel(conditionPanelScrollPane, conditionPanel.advanced(), conditionPanelVisibleState);
    }
  }

  /**
   * Toggles the filter panel through the states hidden, visible and advanced
   */
  public final void toggleFilterPanel() {
    if (filterPanelScrollPane != null) {
      toggleConditionPanel(filterPanelScrollPane, table.filterPanel().advanced(), filterPanelVisibleState);
    }
  }

  /**
   * Allows the user to select one of the available search condition panels
   */
  public final void selectConditionPanel() {
    if (configuration.includeConditionPanel) {
      selectConditionPanel(conditionPanel, conditionPanelScrollPane, conditionPanel.advanced(),
              conditionPanelVisibleState, tableModel, this, FrameworkMessages.selectSearchField());
    }
  }

  /**
   * Allows the user to select one of the available filter condition panels
   */
  public final void selectFilterPanel() {
    if (configuration.includeFilterPanel) {
      selectConditionPanel(table.filterPanel(), filterPanelScrollPane, table.filterPanel().advanced(),
              filterPanelVisibleState, tableModel, this, FrameworkMessages.selectFilterField());
    }
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

  /**
   * If set to null the default delete confirmer is used.
   * @return the {@link Value} controlling the delete confirmer
   */
  public final Value<Confirmer> deleteConfirmer() {
    return deleteConfirmer;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + tableModel.entityType();
  }

  /**
   * Returns a {@link Value} containing the control associated with {@code controlCode},
   * an empty {@link Value} if no such control is available.
   * Note that standard controls are populated during initialization, so until then, these values may be empty.
   * @param tableControl the table control code
   * @return the {@link Value} containing the control associated with {@code controlCode}
   */
  public final Value<Control> control(TableControl tableControl) {
    return controls.get(requireNonNull(tableControl));
  }

  /**
   * Retrieves a new value via input dialog and performs an update on the selected entities
   * assigning the value to the attribute
   * @param attributeToEdit the attribute which value to edit
   * @param <T> the attribute value type
   * @see Config#editComponentFactory(Attribute, EntityComponentFactory)
   */
  public final <T> void editSelected(Attribute<T> attributeToEdit) {
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
   * the {@link Confirmer} set via {@link #deleteConfirmer()}.
   * @return true if the delete operation was successful
   * @see #deleteConfirmer()
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
   * Initializes the UI, while presenting a wait cursor to the user.
   * Note that calling this method more than once has no effect.
   * @return this EntityTablePanel instance
   */
  public final EntityTablePanel initialize() {
    if (!initialized) {
      try {
        setupComponents();
        setupStandardControls();
        setupControls();
        addTablePopupMenu();
        layoutPanel(tablePanel, configuration.includeSouthPanel ? initializeSouthPanel() : null);
        setConditionPanelVisible(conditionPanelVisibleState.get());
        setFilterPanelVisible(filterPanelVisibleState.get());
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
   * @see #control(TableControl)
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
    control(TableControl.REQUEST_TABLE_FOCUS).optional().ifPresent(control ->
            KeyEvents.builder(configuration.shortcuts.keyStroke(REQUEST_TABLE_FOCUS).get())
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(TableControl.SELECT_CONDITION_PANEL).optional().ifPresent(control ->
            KeyEvents.builder(configuration.shortcuts.keyStroke(SELECT_CONDITION_PANEL).get())
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(TableControl.TOGGLE_CONDITION_PANEL).optional().ifPresent(control ->
            KeyEvents.builder(configuration.shortcuts.keyStroke(TOGGLE_CONDITION_PANEL).get())
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(TableControl.TOGGLE_FILTER_PANEL).optional().ifPresent(control ->
            KeyEvents.builder(configuration.shortcuts.keyStroke(TOGGLE_FILTER_PANEL).get())
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(TableControl.SELECT_FILTER_PANEL).optional().ifPresent(control ->
            KeyEvents.builder(configuration.shortcuts.keyStroke(SELECT_FILTER_PANEL).get())
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(TableControl.PRINT).optional().ifPresent(control ->
            KeyEvents.builder(configuration.shortcuts.keyStroke(PRINT).get())
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
  }

  protected Controls createToolBarControls(List<Controls> additionalToolBarControls) {
    requireNonNull(additionalToolBarControls);
    Controls toolbarControls = Controls.controls();
    control(TableControl.TOGGLE_SUMMARY_PANEL).optional().ifPresent(toolbarControls::add);
    Control toggleConditionPanelControl = control(TableControl.TOGGLE_CONDITION_PANEL).optional().orElse(null);
    Control toggleFilterPanelControl = control(TableControl.TOGGLE_FILTER_PANEL).optional().orElse(null);
    if (toggleConditionPanelControl != null || toggleFilterPanelControl != null) {
      if (toggleConditionPanelControl != null) {
        toolbarControls.add(toggleConditionPanelControl);
      }
      if (toggleFilterPanelControl != null) {
        toolbarControls.add(toggleFilterPanelControl);
      }
      toolbarControls.addSeparator();
    }
    control(TableControl.DELETE_SELECTED).optional().ifPresent(toolbarControls::add);
    control(TableControl.PRINT).optional().ifPresent(toolbarControls::add);
    control(TableControl.CLEAR_SELECTION).optional().ifPresent(control -> {
      toolbarControls.add(control);
      toolbarControls.addSeparator();
    });
    control(TableControl.MOVE_SELECTION_UP).optional().ifPresent(toolbarControls::add);
    control(TableControl.MOVE_SELECTION_DOWN).optional().ifPresent(toolbarControls::add);
    additionalToolBarControls.forEach(additionalControls -> {
      toolbarControls.addSeparator();
      additionalControls.actions().forEach(toolbarControls::add);
    });

    return toolbarControls;
  }

  /**
   * Creates a Controls instance containing the controls to include in the table popup menu.
   * Returning null or an empty Controls instance indicates that no popup menu should be included.
   * @param additionalPopupMenuControls any additional controls to include in the popup menu
   * @return Controls on which to base the table popup menu, null or an empty Controls instance
   * if no popup menu should be included
   */
  protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
    requireNonNull(additionalPopupMenuControls);
    Controls popupControls = Controls.controls();
    control(TableControl.REFRESH).optional().ifPresent(popupControls::add);
    control(TableControl.CLEAR).optional().ifPresent(popupControls::add);
    if (popupControls.notEmpty()) {
      popupControls.addSeparator();
    }
    addAdditionalControls(popupControls, additionalPopupMenuControls);
    State separatorRequired = State.state();
    control(TableControl.EDIT_SELECTED).optional().ifPresent(control -> {
      popupControls.add(control);
      separatorRequired.set(true);
    });
    control(TableControl.DELETE_SELECTED).optional().ifPresent(control -> {
      popupControls.add(control);
      separatorRequired.set(true);
    });
    if (separatorRequired.get()) {
      popupControls.addSeparator();
      separatorRequired.set(false);
    }
    control(TableControl.VIEW_DEPENDENCIES).optional().ifPresent(control -> {
      popupControls.add(control);
      separatorRequired.set(true);
    });
    if (separatorRequired.get()) {
      popupControls.addSeparator();
      separatorRequired.set(false);
    }
    Controls printControls = createPrintMenuControls();
    if (printControls != null && printControls.notEmpty()) {
      popupControls.add(printControls);
      separatorRequired.set(true);
    }
    Controls columnControls = createColumnControls();
    if (columnControls.notEmpty()) {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      popupControls.add(columnControls);
      separatorRequired.set(true);
    }
    control(TableControl.SELECTION_MODE).optional().ifPresent(control -> {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      popupControls.add(control);
      separatorRequired.set(true);
    });
    if (configuration.includeConditionPanel && conditionPanel != null) {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      addConditionControls(popupControls);
      separatorRequired.set(true);
    }
    if (configuration.includeFilterPanel) {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      addFilterControls(popupControls);
      separatorRequired.set(true);
    }
    control(TableControl.COPY_TABLE_DATA).optional().ifPresent(control -> {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      popupControls.add(control);
    });

    return popupControls;
  }

  /**
   * By default this method returns a {@link Controls} instance containing
   * the {@link Control} associated with {@link TableControl#PRINT}.
   * If no {@link Control} has been assigned to that control key,
   * an empty {@link Controls} instance is returned.
   * Override to add print actions, which will then appear in the table popup menu.
   * @return the print controls to display in the table popup menu
   */
  protected Controls createPrintMenuControls() {
    Controls.Builder builder = Controls.builder()
            .name(Messages.print())
            .mnemonic(Messages.printMnemonic())
            .smallIcon(FrameworkIcons.instance().print());
    control(TableControl.PRINT).optional().ifPresent(builder::control);

    return builder.build();
  }

  /**
   * Creates a TableCellRenderer to use for the given attribute in this EntityTablePanel
   * @param attribute the attribute
   * @return the TableCellRenderer for the given attribute
   */
  protected TableCellRenderer createTableCellRenderer(Attribute<?> attribute) {
    return EntityTableCellRenderer.builder(tableModel, attribute).build();
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
   * Creates the table toolbar, by default based on {@link #createToolBarControls(List)}
   * @return the toolbar, null if none should be included
   */
  protected JToolBar createToolBar() {
    Controls toolbarControls = createToolBarControls(additionalToolBarControls);
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

  /**
   * Propagates the exception to {@link #onValidationException(ValidationException)} or
   * {@link #onReferentialIntegrityException(ReferentialIntegrityException)} depending on type,
   * otherwise displays the exception.
   * @param exception the exception to handle
   * @see #displayException(Throwable)
   */
  protected void onException(Throwable exception) {
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
   * the dependencies of the entities involved are displayed to the user, otherwise {@link #onException(Throwable)} is called.
   * @param exception the exception
   * @see Config#referentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling)
   */
  protected void onReferentialIntegrityException(ReferentialIntegrityException exception) {
    requireNonNull(exception);
    if (configuration.referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES) {
      displayDependenciesDialog(tableModel.selectionModel().getSelectedItems(), tableModel.connectionProvider(),
              this, MESSAGES.getString("unknown_dependent_records"));
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
   * @return a edit dialog builder
   * @param <T> the attribute type
   */
  protected <T> EntityDialogs.EditDialogBuilder<T> editDialogBuilder(Attribute<T> attribute) {
    return EntityDialogs.editDialog(tableModel.editModel(), attribute)
            .owner(this)
            .componentFactory((EntityComponentFactory<T, Attribute<T>, ?>) configuration.editComponentFactories.get(attribute));
  }

  /**
   * Displays the exception in a dialog, with the dialog owner as the current focus owner
   * or this panel if none is available.
   * @param exception the exception to display
   */
  protected final void displayException(Throwable exception) {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusOwner == null) {
      focusOwner = EntityTablePanel.this;
    }
    Dialogs.displayExceptionDialog(exception, parentWindow(focusOwner));
  }

  /**
   * @return true if confirmed
   * @see #deleteConfirmer()
   */
  protected final boolean confirmDelete() {
    return deleteConfirmer.get().confirm(this);
  }

  /**
   * Creates a {@link Controls} containing controls for editing the value of a single attribute
   * for the selected entities. These controls are enabled as long as the selection is not empty
   * and {@link EntityEditModel#updateEnabled()} is enabled.
   * @return the edit controls
   * @see Config#editable(Consumer)
   * @see EntityEditModel#updateEnabled()
   */
  private Controls createEditSelectedControls() {
    StateObserver selectionNotEmpty = tableModel.selectionModel().selectionNotEmpty();
    StateObserver updateEnabled = tableModel.editModel().updateEnabled();
    StateObserver updateMultipleEnabledOrSingleSelection =
            State.or(tableModel.editModel().updateMultipleEnabled(),
                    tableModel.selectionModel().singleSelection());
    StateObserver enabledState = State.and(selectionNotEmpty, updateEnabled, updateMultipleEnabledOrSingleSelection);
    Controls editControls = Controls.builder()
            .name(FrameworkMessages.edit())
            .enabled(enabledState)
            .smallIcon(FrameworkIcons.instance().edit())
            .description(FrameworkMessages.editSelectedTip())
            .build();
    configuration.editable.get().stream()
            .map(attribute -> tableModel.entityDefinition().attributes().definition(attribute))
            .sorted(AttributeDefinition.definitionComparator())
            .forEach(attributeDefinition -> editControls.add(Control.builder(() -> editSelected(attributeDefinition.attribute()))
                    .name(attributeDefinition.caption() == null ? attributeDefinition.attribute().name() : attributeDefinition.caption())
                    .enabled(enabledState)
                    .build()));

    return editControls;
  }

  /**
   * @return a control for showing the dependencies dialog
   */
  private Control createViewDependenciesControl() {
    return Control.builder(this::viewDependencies)
            .name(FrameworkMessages.dependencies())
            .enabled(tableModel.selectionModel().selectionNotEmpty())
            .description(FrameworkMessages.dependenciesTip())
            .smallIcon(FrameworkIcons.instance().dependencies())
            .build();
  }

  /**
   * @return a control for deleting the selected entities
   * @throws IllegalStateException in case the underlying model is read only or if deleting is not enabled
   */
  private Control createDeleteSelectedControl() {
    return Control.builder(new DeleteCommand())
            .name(FrameworkMessages.delete())
            .enabled(State.and(
                    tableModel.editModel().deleteEnabled(),
                    tableModel.selectionModel().selectionNotEmpty()))
            .description(FrameworkMessages.deleteSelectedTip())
            .smallIcon(FrameworkIcons.instance().delete())
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
            .smallIcon(FrameworkIcons.instance().refresh())
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
            .smallIcon(FrameworkIcons.instance().clear())
            .build();
  }

  private Control createToggleConditionPanelControl() {
    return Control.builder(this::toggleConditionPanel)
            .smallIcon(FrameworkIcons.instance().search())
            .description(MESSAGES.getString("show_condition_panel"))
            .build();
  }

  private Control createSelectConditionPanelControl() {
    return Control.control(this::selectConditionPanel);
  }

  private Control createToggleFilterPanelControl() {
    return Control.builder(this::toggleFilterPanel)
            .smallIcon(FrameworkIcons.instance().filter())
            .description(MESSAGES.getString("show_filter_panel"))
            .build();
  }

  private Control createSelectFilterPanelControl() {
    return Control.control(this::selectFilterPanel);
  }

  private Control createToggleSummaryPanelControl() {
    return ToggleControl.builder(summaryPanelVisibleState)
            .smallIcon(FrameworkIcons.instance().summary())
            .description(MESSAGES.getString("toggle_summary_tip"))
            .build();
  }

  private Control createClearSelectionControl() {
    return Control.builder(tableModel.selectionModel()::clearSelection)
            .enabled(tableModel.selectionModel().selectionNotEmpty())
            .smallIcon(FrameworkIcons.instance().clearSelection())
            .description(MESSAGES.getString("clear_selection_tip"))
            .build();
  }

  private Control createMoveSelectionDownControl() {
    return Control.builder(tableModel.selectionModel()::moveSelectionDown)
            .smallIcon(FrameworkIcons.instance().down())
            .description(MESSAGES.getString("selection_down_tip"))
            .build();
  }

  private Control createMoveSelectionUpControl() {
    return Control.builder(tableModel.selectionModel()::moveSelectionUp)
            .smallIcon(FrameworkIcons.instance().up())
            .description(MESSAGES.getString("selection_up_tip"))
            .build();
  }

  private Control createRequestTableFocusControl() {
    return Control.control(table::requestFocus);
  }

  private Controls createColumnControls() {
    Controls.Builder builder = Controls.builder()
            .name(MESSAGES.getString("columns"));
    control(TableControl.SELECT_COLUMNS).optional().ifPresent(builder::control);
    control(TableControl.RESET_COLUMNS).optional().ifPresent(builder::control);
    control(TableControl.COLUMN_AUTO_RESIZE_MODE).optional().ifPresent(builder::control);

    return builder.build();
  }

  private Control createConditionPanelControl() {
    return ToggleControl.builder(conditionPanelVisibleState)
            .name(FrameworkMessages.show())
            .build();
  }

  private Control createFilterPanelControl() {
    return ToggleControl.builder(filterPanelVisibleState)
            .name(FrameworkMessages.show())
            .build();
  }

  private Controls createCopyControls() {
    return Controls.builder()
            .name(Messages.copy())
            .smallIcon(FrameworkIcons.instance().copy())
            .controls(createCopyCellControl(), createCopyTableRowsWithHeaderControl())
            .build();
  }

  private Control createCopyCellControl() {
    return Control.builder(table::copySelectedCell)
            .name(FrameworkMessages.copyCell())
            .enabled(tableModel.selectionModel().selectionNotEmpty())
            .build();
  }

  private Control createCopyTableRowsWithHeaderControl() {
    return Control.builder(table::copyRowsAsTabDelimitedString)
            .name(FrameworkMessages.copyTableWithHeader())
            .build();
  }

  private boolean includeEditSelectedControls() {
    return !configuration.editable.empty() &&
            !tableModel.editModel().readOnly().get() &&
            tableModel.editModel().updateEnabled().get();
  }

  private boolean includeDeleteSelectedControl() {
    return !tableModel.editModel().readOnly().get() && tableModel.editModel().deleteEnabled().get();
  }

  private FilteredTable<Entity, Attribute<?>> createTable() {
    return FilteredTable.builder(tableModel)
            .cellRendererFactory(new EntityTableCellRendererFactory())
            .onBuild(filteredTable -> filteredTable.setRowHeight(filteredTable.getFont().getSize() + FONT_SIZE_TO_ROW_HEIGHT))
            .build();
  }

  private Control createConditionRefreshControl() {
    return Control.builder(tableModel::refresh)
            .enabled(tableModel.conditionChanged())
            .smallIcon(FrameworkIcons.instance().refreshRequired())
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
            .visible(configuration.refreshButtonVisible == RefreshButtonVisible.ALWAYS || conditionPanelVisibleState.get())
            .build();
  }

  private FilteredTableConditionPanel<Attribute<?>> createConditionPanel() {
    return configuration.includeConditionPanel ? filteredTableConditionPanel(tableModel.conditionModel(), tableModel.columnModel(), configuration.conditionPanelFactory) : null;
  }

  private void bindEvents() {
    if (configuration.includeEntityMenu) {
      KeyEvents.builder(VK_V)
              .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
              .action(Control.control(this::showEntityMenu))
              .enable(table);
    }
    control(TableControl.DELETE_SELECTED).optional().ifPresent(control ->
            KeyEvents.builder(configuration.shortcuts.keyStroke(DELETE_SELECTED).get())
                    .action(control)
                    .enable(table));
    conditionPanelVisibleState.addDataListener(this::setConditionPanelVisible);
    filterPanelVisibleState.addDataListener(this::setFilterPanelVisible);
    summaryPanelVisibleState.addDataListener(this::setSummaryPanelVisible);
    tableModel.conditionModel().addChangeListener(this::onConditionChanged);
    tableModel.refresher().observer().addDataListener(this::onRefreshingChanged);
    tableModel.refresher().addRefreshFailedListener(this::onException);
    tableModel.editModel().addInsertUpdateOrDeleteListener(table::repaint);
    if (conditionPanel != null) {
      enableConditionPanelRefreshOnEnter();
      conditionPanel.addFocusGainedListener(table::scrollToColumn);
      conditionPanel.advanced().addDataListener(advanced -> {
        if (conditionPanelVisibleState.get()) {
          revalidate();
        }
      });
    }
    if (configuration.includeFilterPanel) {
      table.filterPanel().addFocusGainedListener(table::scrollToColumn);
      table.filterPanel().advanced().addDataListener(advanced -> {
        if (filterPanelVisibleState.get()) {
          revalidate();
        }
      });
    }
  }

  private void enableConditionPanelRefreshOnEnter() {
    KeyEvents.builder(VK_ENTER)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(conditionRefreshControl)
            .enable(conditionPanel);
    tableModel.columnModel().columns().stream()
            .map(column -> conditionPanel.conditionPanel(column.getIdentifier()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(panel -> Stream.of(panel.operatorComboBox(), panel.equalField(), panel.lowerBoundField(), panel.upperBoundField()))
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

  private void setConditionPanelVisible(boolean visible) {
    if (conditionPanelScrollPane != null) {
      conditionPanelScrollPane.setVisible(visible);
      refreshButtonToolBar.setVisible(configuration.refreshButtonVisible == RefreshButtonVisible.ALWAYS || visible);
      revalidate();
    }
  }

  private void setFilterPanelVisible(boolean visible) {
    if (filterPanelScrollPane != null) {
      filterPanelScrollPane.setVisible(visible);
      revalidate();
    }
  }

  private void setSummaryPanelVisible(boolean visible) {
    if (summaryPanelScrollPane != null) {
      summaryPanelScrollPane.setVisible(visible);
      revalidate();
    }
  }

  private void setupComponents() {
    tableScrollPane = new JScrollPane(table());
    tablePanel = new TablePanel();
    tableModel.columnModel().columns().forEach(this::configureColumn);
    conditionPanelVisibleState.addValidator(new PanelAvailableValidator(conditionPanel, "condition"));
    filterPanelVisibleState.addValidator(new PanelAvailableValidator(table.filterPanel(), "filter"));
    summaryPanelVisibleState.addValidator(new PanelAvailableValidator(summaryPanel, "summary"));
  }

  private void setupStandardControls() {
    if (includeDeleteSelectedControl()) {
      controls.get(TableControl.DELETE_SELECTED).mapNull(this::createDeleteSelectedControl);
    }
    if (includeEditSelectedControls()) {
      controls.get(TableControl.EDIT_SELECTED).mapNull(this::createEditSelectedControls);
    }
    if (configuration.includeClearControl) {
      controls.get(TableControl.CLEAR).mapNull(this::createClearControl);
    }
    controls.get(TableControl.REFRESH).mapNull(this::createRefreshControl);
    controls.get(TableControl.SELECT_COLUMNS).mapNull(this::createColumnSelectionControl);
    controls.get(TableControl.RESET_COLUMNS).mapNull(table::createResetColumnsControl);
    controls.get(TableControl.COLUMN_AUTO_RESIZE_MODE).mapNull(table::createAutoResizeModeControl);
    if (includeViewDependenciesControl()) {
      controls.get(TableControl.VIEW_DEPENDENCIES).mapNull(this::createViewDependenciesControl);
    }
    if (summaryPanelScrollPane != null) {
      controls.get(TableControl.TOGGLE_SUMMARY_PANEL).mapNull(this::createToggleSummaryPanelControl);
    }
    if (configuration.includeConditionPanel && conditionPanel != null) {
      controls.get(TableControl.CONDITION_PANEL_VISIBLE).mapNull(this::createConditionPanelControl);
      controls.get(TableControl.TOGGLE_CONDITION_PANEL).mapNull(this::createToggleConditionPanelControl);
      controls.get(TableControl.SELECT_CONDITION_PANEL).mapNull(this::createSelectConditionPanelControl);
    }
    if (configuration.includeFilterPanel) {
      controls.get(TableControl.FILTER_PANEL_VISIBLE).mapNull(this::createFilterPanelControl);
      controls.get(TableControl.TOGGLE_FILTER_PANEL).mapNull(this::createToggleFilterPanelControl);
      controls.get(TableControl.SELECT_FILTER_PANEL).mapNull(this::createSelectFilterPanelControl);
    }
    controls.get(TableControl.CLEAR_SELECTION).mapNull(this::createClearSelectionControl);
    controls.get(TableControl.MOVE_SELECTION_UP).mapNull(this::createMoveSelectionDownControl);
    controls.get(TableControl.MOVE_SELECTION_DOWN).mapNull(this::createMoveSelectionUpControl);
    controls.get(TableControl.COPY_TABLE_DATA).mapNull(this::createCopyControls);
    if (configuration.includeSelectionModeControl) {
      controls.get(TableControl.SELECTION_MODE).mapNull(table::createSingleSelectionModeControl);
    }
    controls.get(TableControl.REQUEST_TABLE_FOCUS).mapNull(this::createRequestTableFocusControl);
    controls.get(TableControl.CONFIGURE_COLUMNS).mapNull(this::createColumnControls);
  }

  private boolean includeViewDependenciesControl() {
    return tableModel.entities().definitions().stream()
            .flatMap(entityDefinition -> entityDefinition.foreignKeys().definitions().stream())
            .filter(foreignKeyDefinition -> !foreignKeyDefinition.soft())
            .anyMatch(foreignKeyDefinition -> foreignKeyDefinition.attribute().referencedType().equals(tableModel.entityType()));
  }

  private void configureColumn(FilteredTableColumn<Attribute<?>> column) {
    TableCellEditor tableCellEditor = createTableCellEditor(column.getIdentifier());
    if (tableCellEditor != null) {
      column.setCellEditor(tableCellEditor);
    }
    column.setHeaderRenderer(new HeaderRenderer(column.getHeaderRenderer()));
  }

  private void addTablePopupMenu() {
    if (!configuration.includePopupMenu) {
      return;
    }
    Controls popupControls = createPopupMenuControls(additionalPopupControls);
    if (popupControls == null || popupControls.empty()) {
      return;
    }

    JPopupMenu popupMenu = menu(popupControls).createPopupMenu();
    table.setComponentPopupMenu(popupMenu);
    tableScrollPane.setComponentPopupMenu(popupMenu);
    KeyEvents.builder(configuration.shortcuts.keyStroke(DISPLAY_POPUP_MENU).get())
            .action(Control.control(() -> {
              Point location = popupLocation(table);
              popupMenu.show(table, location.x, location.y);
            }))
            .enable(table);
  }

  private void addConditionControls(Controls popupControls) {
    Controls conditionControls = Controls.builder()
            .name(FrameworkMessages.search())
            .smallIcon(FrameworkIcons.instance().search())
            .build();
    control(TableControl.CONDITION_PANEL_VISIBLE).optional().ifPresent(conditionControls::add);
    Controls conditionPanelControls = conditionPanel.controls();
    if (conditionPanelControls.notEmpty()) {
      conditionControls.addAll(conditionPanelControls);
      conditionControls.addSeparator();
    }
    conditionControls.add(ToggleControl.builder(tableModel.conditionRequired())
            .name(MESSAGES.getString("require_query_condition"))
            .description(MESSAGES.getString("require_query_condition_description"))
            .build());
    if (conditionControls.notEmpty()) {
      popupControls.add(conditionControls);
    }
  }

  private void addFilterControls(Controls popupControls) {
    Controls filterControls = Controls.builder()
            .name(FrameworkMessages.filter())
            .smallIcon(FrameworkIcons.instance().filter())
            .build();
    control(TableControl.FILTER_PANEL_VISIBLE).optional().ifPresent(filterControls::add);
    Controls filterPanelControls = table.filterPanel().controls();
    if (filterPanelControls.notEmpty()) {
      filterControls.addAll(filterPanelControls);
    }
    if (filterControls.notEmpty()) {
      popupControls.add(filterControls);
    }
  }

  private void showEntityMenu() {
    Point location = popupLocation(table);
    tableModel.selectionModel().selectedItem().ifPresent(selected ->
            new EntityPopupMenu(selected.copy(), tableModel.connectionProvider().connection()).show(table, location.x, location.y));
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

  private void toggleConditionPanel(JScrollPane scrollPane, State advancedState, State visibleState) {
    if (scrollPane != null && scrollPane.isVisible()) {
      if (advancedState.get()) {
        boolean parentOfFocusOwner = parentOfType(JScrollPane.class,
                getCurrentKeyboardFocusManager().getFocusOwner()) == scrollPane;
        visibleState.set(false);
        if (parentOfFocusOwner) {
          table.requestFocusInWindow();
        }
      }
      else {
        advancedState.set(true);
      }
    }
    else {
      advancedState.set(false);
      visibleState.set(true);
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

  private Map<TableControl, Value<Control>> createControlsMap() {
    Value.Validator<Control> controlValueValidator = control -> {
      if (initialized) {
        throw new IllegalStateException("TablePanel has already been initialized");
      }
    };

    return Stream.of(TableControl.values())
            .collect(toMap(Function.identity(), controlCode -> {
              Value<Control> value = value();
              value.addValidator(controlValueValidator);

              return value;
            }));
  }

  private Value<Confirmer> createDeleteConfirmer() {
    DeleteConfirmer defaultDeleteConfirmer = new DeleteConfirmer();

    return value(defaultDeleteConfirmer, defaultDeleteConfirmer);
  }

  private void throwIfInitialized() {
    if (initialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
  }

  private final class DeleteCommand implements Control.Command {

    @Override
    public void execute() {
      if (confirmDelete()) {
        EntityEditModel.Delete delete = tableModel().editModel().createDelete(tableModel().selectionModel().getSelectedItems());
        delete.notifyBeforeDelete();
        progressWorkerDialog(delete::delete)
                .title(EDIT_PANEL_MESSAGES.getString("deleting"))
                .owner(EntityTablePanel.this)
                .onException(this::onException)
                .onResult(delete::notifyAfterDelete)
                .execute();
      }
    }

    private void onException(Throwable exception) {
      LOG.error(exception.getMessage(), exception);
      EntityTablePanel.this.onException(exception);
    }
  }

  private static final void selectConditionPanel(FilteredTableConditionPanel<Attribute<?>> tableConditionPanel,
                                                 JScrollPane conditionPanelScrollPane, State conditionPanelAdvancedState,
                                                 State conditionPanelVisibleState, SwingEntityTableModel tableModel,
                                                 JComponent dialogOwner, String dialogTitle) {
    if (tableConditionPanel != null) {
      List<AttributeDefinition<?>> attributeDefinitions = tableConditionPanel.conditionPanels().stream()
              .filter(panel -> tableModel.columnModel().visible(panel.model().columnIdentifier()).get())
              .map(panel -> tableModel.entityDefinition().attributes().definition(panel.model().columnIdentifier()))
              .sorted(AttributeDefinition.definitionComparator())
              .collect(toList());
      if (attributeDefinitions.size() == 1) {
        displayConditionPanel(conditionPanelScrollPane, conditionPanelAdvancedState, conditionPanelVisibleState);
        tableConditionPanel.conditionPanel(attributeDefinitions.get(0).attribute())
                .ifPresent(ColumnConditionPanel::requestInputFocus);
      }
      else if (!attributeDefinitions.isEmpty()) {
        Dialogs.selectionDialog(attributeDefinitions)
                .owner(dialogOwner)
                .title(dialogTitle)
                .selectSingle()
                .flatMap(attributeDefinition -> tableConditionPanel.conditionPanel(attributeDefinition.attribute()))
                .ifPresent(conditionPanel -> {
                  displayConditionPanel(conditionPanelScrollPane, conditionPanelAdvancedState, conditionPanelVisibleState);
                  conditionPanel.requestInputFocus();
                });
      }
    }
  }

  private static void displayConditionPanel(JScrollPane conditionPanelScrollPane,
                                            State conditionPanelAdvancedState,
                                            State conditionPanelVisibleState) {
    if (conditionPanelScrollPane != null && !conditionPanelScrollPane.isVisible()) {
      conditionPanelAdvancedState.set(false);
      conditionPanelVisibleState.set(true);
    }
  }

  private static GridBagConstraints createHorizontalFillConstraints() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;

    return constraints;
  }

  private static void addAdditionalControls(Controls popupControls, List<Controls> additionalPopupControls) {
    additionalPopupControls.forEach(controls -> {
      if (nullOrEmpty(controls.getName())) {
        popupControls.addAll(controls);
      }
      else {
        popupControls.add(controls);
      }
      popupControls.addSeparator();
    });
  }

  private static JScrollPane createHiddenLinkedScrollPane(JScrollPane parentScrollPane, JPanel panelToScroll) {
    return Components.scrollPane(panelToScroll)
            .horizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
            .verticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
            .visible(false)
            .onBuild(scrollPane -> linkBoundedRangeModels(
                    parentScrollPane.getHorizontalScrollBar().getModel(),
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

  private static Config configure(EntityDefinition entityDefinition, Consumer<Config> configuration) {
    Config config = new Config(entityDefinition);
    requireNonNull(configuration).accept(config);

    return new Config(config);
  }

  private static KeyStroke defaultKeyStroke(KeyboardShortcut shortcut) {
    switch (shortcut) {
      case REQUEST_TABLE_FOCUS: return keyStroke(VK_T, CTRL_DOWN_MASK);
      case SELECT_CONDITION_PANEL: return keyStroke(VK_S, CTRL_DOWN_MASK);
      case TOGGLE_CONDITION_PANEL: return keyStroke(VK_S, CTRL_DOWN_MASK | ALT_DOWN_MASK);
      case SELECT_FILTER_PANEL: return keyStroke(VK_F, CTRL_DOWN_MASK | SHIFT_DOWN_MASK);
      case TOGGLE_FILTER_PANEL: return keyStroke(VK_F, CTRL_DOWN_MASK | ALT_DOWN_MASK);
      case PRINT: return keyStroke(VK_P, CTRL_DOWN_MASK);
      case DELETE_SELECTED: return keyStroke(VK_DELETE);
      case DISPLAY_POPUP_MENU: return keyStroke(VK_G, CTRL_DOWN_MASK);
      default: throw new IllegalArgumentException();
    }
  }

  private final class EntityTableCellRendererFactory implements FilteredTableCellRendererFactory<Attribute<?>> {

    @Override
    public TableCellRenderer tableCellRenderer(FilteredTableColumn<Attribute<?>> column) {
      return createTableCellRenderer(column.getIdentifier());
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
      FilteredTableColumn<Attribute<?>> tableColumn = tableModel.columnModel().getColumn(column);
      TableCellRenderer renderer = tableColumn.getCellRenderer();
      boolean useBoldFont = renderer instanceof FilteredTableCellRenderer
              && ((FilteredTableCellRenderer) renderer).columnShadingEnabled()
              && tableModel.conditionModel().enabled(tableColumn.getIdentifier());
      Font defaultFont = component.getFont();
      component.setFont(useBoldFont ? defaultFont.deriveFont(defaultFont.getStyle() | Font.BOLD) : defaultFont);

      return component;
    }
  }

  private final class DeleteConfirmer implements Confirmer {

    @Override
    public boolean confirm(JComponent dialogOwner) {
      return confirm(dialogOwner, FrameworkMessages.confirmDeleteSelected(
              tableModel.selectionModel().selectionCount()), FrameworkMessages.delete());
    }
  }

  /**
   * Contains configuration settings for a {@link EntityTablePanel} which must be set before the panel is initialized.
   */
  public static final class Config {

    private final EntityDefinition entityDefinition;

    private final KeyboardShortcuts<KeyboardShortcut> shortcuts;
    private final ValueSet<Attribute<?>> editable;
    private final Map<Attribute<?>, EntityComponentFactory<?, ?, ?>> editComponentFactories;
    private final Map<Attribute<?>, EntityComponentFactory<?, ?, ?>> cellEditorComponentFactories;

    private EntityConditionPanelFactory conditionPanelFactory;
    private boolean includeSouthPanel = true;
    private boolean includeConditionPanel = INCLUDE_CONDITION_PANEL.get();
    private boolean includeFilterPanel = INCLUDE_FILTER_PANEL.get();
    private boolean includeSummaryPanel = INCLUDE_SUMMARY_PANEL.get();
    private boolean includeClearControl = INCLUDE_CLEAR_CONTROL.get();
    private boolean includeLimitMenu = INCLUDE_LIMIT_MENU.get();
    private boolean includeEntityMenu = INCLUDE_ENTITY_MENU.get();
    private boolean includePopupMenu = INCLUDE_POPUP_MENU.get();
    private boolean includeSelectionModeControl = false;
    private ColumnSelection columnSelection = COLUMN_SELECTION.get();
    private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling;
    private RefreshButtonVisible refreshButtonVisible;
    private Function<SwingEntityTableModel, String> statusMessage = DEFAULT_STATUS_MESSAGE;
    private boolean showRefreshProgressBar = SHOW_REFRESH_PROGRESS_BAR.get();

    private Config(EntityDefinition entityDefinition) {
      this.entityDefinition = entityDefinition;
      this.shortcuts = KEYBOARD_SHORTCUTS.copy();
      this.conditionPanelFactory = new EntityConditionPanelFactory(entityDefinition);
      this.editable = valueSet(entityDefinition.attributes().updatable().stream()
              .map(AttributeDefinition::attribute)
              .collect(toSet()));
      this.editable.addValidator(new EditMenuAttributeValidator(entityDefinition));
      this.editComponentFactories = new HashMap<>();
      this.cellEditorComponentFactories = new HashMap<>();
      this.referentialIntegrityErrorHandling = ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.get();
      this.refreshButtonVisible = RefreshButtonVisible.WHEN_CONDITION_PANEL_IS_VISIBLE;
    }

    private Config(Config config) {
      this.entityDefinition = config.entityDefinition;
      this.shortcuts = config.shortcuts.copy();
      this.editable = valueSet(config.editable.get());
      this.conditionPanelFactory = config.conditionPanelFactory;
      this.includeSouthPanel = config.includeSouthPanel;
      this.includeConditionPanel = config.includeConditionPanel;
      this.includeFilterPanel = config.includeFilterPanel;
      this.includeSummaryPanel = config.includeSummaryPanel;
      this.includeClearControl = config.includeClearControl;
      this.includeLimitMenu = config.includeLimitMenu;
      this.includeEntityMenu = config.includeEntityMenu;
      this.includePopupMenu = config.includePopupMenu;
      this.includeSelectionModeControl = config.includeSelectionModeControl;
      this.columnSelection = config.columnSelection;
      this.editComponentFactories = new HashMap<>(config.editComponentFactories);
      this.cellEditorComponentFactories = new HashMap<>(config.cellEditorComponentFactories);
      this.referentialIntegrityErrorHandling = config.referentialIntegrityErrorHandling;
      this.refreshButtonVisible = config.refreshButtonVisible;
      this.statusMessage = config.statusMessage;
      this.showRefreshProgressBar = config.showRefreshProgressBar;
    }

    /**
     * @param conditionPanelFactory the condition panel factory
     * @return this Config instance
     */
    public Config conditionPanelFactory(EntityConditionPanelFactory conditionPanelFactory) {
      this.conditionPanelFactory = requireNonNull(conditionPanelFactory);
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
     * @param includeEntityMenu true a {@link EntityPopupMenu} should be available in this table, triggered with CTRL-ALT-V.<br>
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
     * @param columnSelection specifies how columns are selected
     * @return this Config instance
     */
    public Config columnSelection(ColumnSelection columnSelection) {
      this.columnSelection = requireNonNull(columnSelection);
      return this;
    }

    /**
     * @param shortcuts provides this tables {@link KeyboardShortcuts} instance.
     * @return this Config instance
     */
    public Config keyStrokes(Consumer<KeyboardShortcuts<KeyboardShortcut>> shortcuts) {
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

  private static final class PanelAvailableValidator implements Value.Validator<Boolean> {

    private final JPanel panel;
    private final String panelType;

    private PanelAvailableValidator(JPanel panel, String panelType) {
      this.panel = panel;
      this.panelType = panelType;
    }

    @Override
    public void validate(Boolean visible) throws IllegalArgumentException {
      if (visible && panel == null) {
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

    private TablePanel() {
      super(new BorderLayout());
      if (configuration.includeConditionPanel) {
        if (conditionPanel == null) {
          conditionPanel = createConditionPanel();
        }
        conditionPanelScrollPane = createHiddenLinkedScrollPane(tableScrollPane, conditionPanel);
        add(conditionPanelScrollPane, BorderLayout.NORTH);
      }
      JPanel tableSouthPanel = new JPanel(new BorderLayout());
      if (configuration.includeSummaryPanel) {
        summaryPanel = createSummaryPanel();
        if (summaryPanel != null) {
          summaryPanelScrollPane = createHiddenLinkedScrollPane(tableScrollPane, summaryPanel);
          tableSouthPanel.add(summaryPanelScrollPane, BorderLayout.NORTH);
        }
      }
      if (configuration.includeFilterPanel) {
        filterPanelScrollPane = createHiddenLinkedScrollPane(tableScrollPane, table.filterPanel());
        tableSouthPanel.add(filterPanelScrollPane, BorderLayout.CENTER);
      }
      add(tableScrollPane, BorderLayout.CENTER);
      add(tableSouthPanel, BorderLayout.SOUTH);
    }

    private FilteredTableColumnComponentPanel<Attribute<?>, JPanel> createSummaryPanel() {
      Map<Attribute<?>, JPanel> columnSummaryPanels = createColumnSummaryPanels(tableModel);
      if (columnSummaryPanels.isEmpty()) {
        return null;
      }

      return filteredTableColumnComponentPanel(tableModel.columnModel(), columnSummaryPanels);
    }

    private Map<Attribute<?>, JPanel> createColumnSummaryPanels(FilteredTableModel<?, Attribute<?>> tableModel) {
      Map<Attribute<?>, JPanel> components = new HashMap<>();
      tableModel.columnModel().columns().forEach(column ->
              tableModel.summaryModel().summaryModel(column.getIdentifier())
                      .ifPresent(columnSummaryModel ->
                              components.put(column.getIdentifier(), columnSummaryPanel(columnSummaryModel,
                                      ((FilteredTableCellRenderer) column.getCellRenderer()).horizontalAlignment()))));

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
      JToolBar southToolBar = createToolBar();
      if (southToolBar != null) {
        add(southToolBar, BorderLayout.EAST);
      }
    }

    private StatusPanel statusPanel() {
      if (statusPanel == null) {
        statusPanel = new StatusPanel();
      }

      return statusPanel;
    }
  }

  private final class StatusPanel extends JPanel {

    private static final String STATUS = "status";
    private static final String REFRESHING = "refreshing";

    private final Value<String> statusMessage = value("", "");

    private StatusPanel() {
      super(new CardLayout());
      add(Components.label(statusMessage)
              .horizontalAlignment(SwingConstants.CENTER)
              .build(), STATUS);
      add(createRefreshingProgressPanel(), REFRESHING);
      CardLayout layout = (CardLayout) getLayout();
      tableModel.refresher().observer().addDataListener(isRefreshing -> {
        if (configuration.showRefreshProgressBar) {
          layout.show(this, isRefreshing ? REFRESHING : STATUS);
        }
      });
      if (configuration.includeLimitMenu) {
        setComponentPopupMenu(createPopupMenu());
      }
      tableModel.selectionModel().addListSelectionListener(e -> updateStatusMessage());
      tableModel.addDataChangedListener(this::updateStatusMessage);
      updateStatusMessage();
    }

    private JPanel createRefreshingProgressPanel() {
      return Components.panel(new GridBagLayout())
              .add(Components.progressBar()
                      .indeterminate(true)
                      .string(MESSAGES.getString(REFRESHING))
                      .stringPainted(true)
                      .build(), createHorizontalFillConstraints())
              .build();
    }

    private JPopupMenu createPopupMenu() {
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
              .show());
    }

    private void updateStatusMessage() {
      statusMessage.set(configuration.statusMessage.apply(tableModel));
    }
  }
}
