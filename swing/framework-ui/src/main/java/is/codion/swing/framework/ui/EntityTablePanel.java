/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.i18n.Messages;
import is.codion.common.properties.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilteredTableCellRendererFactory;
import is.codion.swing.common.ui.component.table.FilteredTableColumnComponentPanel;
import is.codion.swing.common.ui.component.table.FilteredTableConditionPanel;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.component.EntityComponents;
import is.codion.swing.framework.ui.icons.FrameworkIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.swing.common.ui.Utilities.getParentWindow;
import static is.codion.swing.common.ui.Utilities.linkBoundedRangeModels;
import static is.codion.swing.common.ui.component.table.ColumnSummaryPanel.columnSummaryPanel;
import static is.codion.swing.common.ui.component.table.FilteredTableColumnComponentPanel.filteredTableColumnComponentPanel;
import static is.codion.swing.common.ui.component.table.FilteredTableConditionPanel.filteredTableConditionPanel;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.displayDependenciesDialog;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Objects.requireNonNull;
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
 * Note that {@link #initializePanel()} must be called to initialize this panel before displaying it.
 * @see EntityTableModel
 * @see #entityTablePanel(SwingEntityTableModel)
 * @see #entityTablePanel(Collection, EntityConnectionProvider)
 * @see #entityTablePanelReadOnly(Collection, EntityConnectionProvider)
 */
public class EntityTablePanel extends JPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityTablePanel.class);

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityTablePanel.class.getName());

  /**
   * Specifies whether table condition panels should be visible or not by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> CONDITION_PANEL_VISIBLE =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.conditionPanelVisible", false);

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
   * Specifies whether to include a filter panel.<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> INCLUDE_FILTER_PANEL =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.includeFilterPanel", false);

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
   * The standard controls available
   */
  public enum ControlCode {
    PRINT_TABLE,
    DELETE_SELECTED,
    VIEW_DEPENDENCIES,
    UPDATE_SELECTED,
    SELECT_COLUMNS,
    RESET_COLUMNS,
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

  private final State conditionPanelVisibleState = State.state();
  private final State filterPanelVisibleState = State.state();
  private final State summaryPanelVisibleState = State.state();

  private final Map<ControlCode, Control> controls = new EnumMap<>(ControlCode.class);

  private final Map<Attribute<?>, EntityComponentFactory<?, ?, ?>> updateSelectedComponentFactories = new HashMap<>();
  private final Map<Attribute<?>, EntityComponentFactory<?, ?, ?>> tableCellEditorComponentFactories = new HashMap<>();

  private final SwingEntityTableModel tableModel;

  private final FilteredTable<SwingEntityTableModel, Entity, Attribute<?>> table;

  private final JScrollPane tableScrollPane;

  private final FilteredTableConditionPanel<Attribute<?>> conditionPanel;

  private final JScrollPane conditionPanelScrollPane;

  private final FilteredTableConditionPanel<Attribute<?>> filterPanel;

  private final JScrollPane filterPanelScrollPane;

  private final FilteredTableColumnComponentPanel<Attribute<?>, JPanel> summaryPanel;

  private final JScrollPane summaryPanelScrollPane;

  private final JPanel southPanel = new JPanel(new BorderLayout());

  /**
   * Base panel for the table, condition and summary panels
   */
  private final JPanel tablePanel;

  /**
   * the toolbar containing the refresh button
   */
  private final JToolBar refreshButtonToolBar;

  /**
   * displays a status message or a refresh progress bar when refreshing
   */
  private final StatusPanel statusPanel;

  private final List<Controls> additionalPopupControls = new ArrayList<>();
  private final List<Controls> additionalToolBarControls = new ArrayList<>();
  private final Set<Attribute<?>> excludeFromUpdateMenu = new HashSet<>();

  private final Control conditionRefreshControl;

  private JPanel searchFieldPanel;

  private JSplitPane southPanelSplitPane;

  private JToolBar southToolBar;

  /**
   * specifies when the refresh button toolbar should be visible
   */
  private RefreshButtonVisible refreshButtonVisible = REFRESH_BUTTON_VISIBLE.get();

  /**
   * specifies whether to include the south panel
   */
  private boolean includeSouthPanel = true;

  /**
   * specifies whether to include the condition panel
   */
  private boolean includeConditionPanel = true;

  /**
   * specifies whether to include the table filter panel
   */
  private boolean includeFilterPanel = INCLUDE_FILTER_PANEL.get();

  /**
   * specifies whether to include a 'Clear' control in the popup menu.
   */
  private boolean includeClearControl = INCLUDE_CLEAR_CONTROL.get();

  /**
   * specifies whether to include the selection mode control in the popup menu
   */
  private boolean includeSelectionModeControl = false;

  /**
   * Specifies how column selection is presented.
   */
  private ColumnSelection columnSelection = COLUMN_SELECTION.get();

  /**
   * specifies whether to include a popup menu
   */
  private boolean includePopupMenu = true;

  /**
   * True after {@code initializePanel()} has been called
   */
  private boolean panelInitialized = false;

  /**
   * The action to take when a referential integrity error occurs on delete
   */
  private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling =
          ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.get();

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the SwingEntityTableModel instance
   */
  public EntityTablePanel(SwingEntityTableModel tableModel) {
    this(requireNonNull(tableModel), new EntityConditionPanelFactory(tableModel.entityDefinition()));
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the SwingEntityTableModel instance
   * @param conditionPanelFactory the condition panel factory, if any
   */
  public EntityTablePanel(SwingEntityTableModel tableModel,
                          ColumnConditionPanel.Factory<Attribute<?>> conditionPanelFactory) {
    this.tableModel = requireNonNull(tableModel, "tableModel");
    this.table = createTable();
    this.conditionRefreshControl = createConditionRefreshControl();
    this.conditionPanel = createConditionPanel(conditionPanelFactory);
    this.tableScrollPane = new JScrollPane(table);
    this.conditionPanelScrollPane = createConditionPanelScrollPane();
    this.filterPanel = table.conditionPanel();
    this.filterPanelScrollPane = createFilterPanelScrollPane();
    this.summaryPanel = createSummaryPanel();
    this.summaryPanelScrollPane = createSummaryPanelScrollPane();
    this.tablePanel = createTablePanel();
    this.refreshButtonToolBar = createRefreshButtonToolBar();
    this.statusPanel = new StatusPanel(tableModel);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(tablePanel, table, statusPanel, conditionPanel, conditionPanelScrollPane,
            filterPanel, filterPanelScrollPane, summaryPanelScrollPane, summaryPanel, southPanel,
            refreshButtonToolBar, southToolBar, southPanelSplitPane, searchFieldPanel);
    if (tableScrollPane != null) {
      Utilities.updateUI(tableScrollPane, tableScrollPane.getViewport(),
              tableScrollPane.getHorizontalScrollBar(), tableScrollPane.getVerticalScrollBar());
    }
  }

  /**
   * @return the table
   */
  public final FilteredTable<SwingEntityTableModel, Entity, Attribute<?>> table() {
    return table;
  }

  /**
   * @return the EntityTableModel used by this EntityTablePanel
   */
  public final SwingEntityTableModel tableModel() {
    return tableModel;
  }

  /**
   * Specifies that the given property should be excluded from the update selected entities menu.
   * @param attribute the id of the property to exclude from the update menu
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void excludeFromUpdateMenu(Attribute<?> attribute) {
    checkIfInitialized();
    tableModel().entityDefinition().property(attribute);//just validating that the property exists
    excludeFromUpdateMenu.add(attribute);
  }

  /**
   * @param additionalPopupMenuControls a set of controls to add to the table popup menu
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void addPopupMenuControls(Controls additionalPopupMenuControls) {
    checkIfInitialized();
    this.additionalPopupControls.add(requireNonNull(additionalPopupMenuControls));
  }

  /**
   * @param additionalToolBarControls a set of controls to add to the table toolbar menu
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void addToolBarControls(Controls additionalToolBarControls) {
    checkIfInitialized();
    this.additionalToolBarControls.add(requireNonNull(additionalToolBarControls));
  }

  /**
   * @param includeSouthPanel true if the south panel should be included
   * @see #initializeSouthPanel()
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludeSouthPanel(boolean includeSouthPanel) {
    checkIfInitialized();
    this.includeSouthPanel = includeSouthPanel;
  }

  /**
   * @param includeConditionPanel true if the condition panel should be included
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludeConditionPanel(boolean includeConditionPanel) {
    checkIfInitialized();
    this.includeConditionPanel = includeConditionPanel;
  }

  /**
   * @param includeFilterPanel true if the filter panel should be included
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludeFilterPanel(boolean includeFilterPanel) {
    checkIfInitialized();
    this.includeFilterPanel = includeFilterPanel;
  }

  /**
   * @param includePopupMenu true if a popup menu should be included
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludePopupMenu(boolean includePopupMenu) {
    checkIfInitialized();
    this.includePopupMenu = includePopupMenu;
  }

  /**
   * @param includeClearControl true if a 'Clear' control should be included in the popup menu
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludeClearControl(boolean includeClearControl) {
    checkIfInitialized();
    this.includeClearControl = includeClearControl;
  }

  /**
   * @param includeSelectionModeControl true if a 'Single Selection' control should be included in the popup menu
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludeSelectionModeControl(boolean includeSelectionModeControl) {
    checkIfInitialized();
    this.includeSelectionModeControl = includeSelectionModeControl;
  }

  /**
   * @param columnSelection specifies how columns are selected
   */
  public final void setColumnSelection(ColumnSelection columnSelection) {
    checkIfInitialized();
    this.columnSelection = requireNonNull(columnSelection);
  }

  /**
   * @return the refresh button visible setting
   */
  public final RefreshButtonVisible getRefreshButtonVisible() {
    return refreshButtonVisible;
  }

  /**
   * @param refreshButtonVisible the refresh button visible setting
   */
  public final void setRefreshButtonVisible(RefreshButtonVisible refreshButtonVisible) {
    this.refreshButtonVisible = requireNonNull(refreshButtonVisible);
    this.refreshButtonToolBar.setVisible(refreshButtonVisible == RefreshButtonVisible.ALWAYS || isConditionPanelVisible());
  }

  /**
   * @return true if a progress bar is shown while the model is refreshing
   */
  public final boolean isShowRefreshProgressBar() {
    return statusPanel.showRefreshProgressBar;
  }

  /**
   * @param showRefreshProgressBar true if an indeterminate progress bar should be shown while the model is refreshing
   */
  public final void setShowRefreshProgressBar(boolean showRefreshProgressBar) {
    statusPanel.showRefreshProgressBar = showRefreshProgressBar;
  }

  /**
   * Hides or shows the column condition panel for this EntityTablePanel
   * @param visible if true the condition panel is shown, if false it is hidden
   */
  public final void setConditionPanelVisible(boolean visible) {
    conditionPanelVisibleState.set(visible);
  }

  /**
   * @return true if the condition panel is visible, false if it is hidden
   */
  public final boolean isConditionPanelVisible() {
    return conditionPanelScrollPane != null && conditionPanelScrollPane.isVisible();
  }

  /**
   * Hides or shows the column filter panel for this EntityTablePanel
   * @param visible if true the filkter panel is shown, if false it is hidden
   */
  public final void setFilterPanelVisible(boolean visible) {
    filterPanelVisibleState.set(visible);
  }

  /**
   * @return true if the filter panel is visible, false if it is hidden
   */
  public final boolean isFilterPanelVisible() {
    return filterPanelScrollPane != null && filterPanelScrollPane.isVisible();
  }

  /**
   * Sets the component factory for the given attribute, used when updating entities via {@link #updateSelectedEntities(Attribute)}.
   * @param attribute the attribute
   * @param componentFactory the component factory
   * @param <T> the value type
   * @param <A> the attribute type
   * @param <C> the component type
   */
  public final <T, A extends Attribute<T>, C extends JComponent> void setUpdateSelectedComponentFactory(A attribute,
                                                                                                        EntityComponentFactory<T, A, C> componentFactory) {
    tableModel().entityDefinition().property(attribute);
    updateSelectedComponentFactories.put(attribute, requireNonNull(componentFactory));
  }

  /**
   * Sets the table cell editor component factory for the given attribute.
   * @param attribute the attribute
   * @param componentFactory the component factory
   * @param <T> the value type
   * @param <A> the attribute type
   * @param <C> the component type
   */
  public final <T, A extends Attribute<T>, C extends JComponent> void setTableCellEditorComponentFactory(A attribute,
                                                                                                         EntityComponentFactory<T, A, C> componentFactory) {
    tableModel().entityDefinition().property(attribute);
    tableCellEditorComponentFactories.put(attribute, requireNonNull(componentFactory));
  }

  /**
   * Toggles the condition panel through the states hidden, visible and advanced
   */
  public final void toggleConditionPanel() {
    if (conditionPanel != null) {
      toggleConditionPanel(conditionPanelScrollPane, conditionPanel.advancedViewState(), conditionPanelVisibleState);
    }
  }

  /**
   * Toggles the filter panel through the states hidden, visible and advanced
   */
  public final void toggleFilterPanel() {
    if (filterPanel != null) {
      toggleConditionPanel(filterPanelScrollPane, filterPanel.advancedViewState(), filterPanelVisibleState);
    }
  }

  /**
   * Allows the user to select on of the available search condition panels
   */
  public final void selectConditionPanel() {
    if (includeConditionPanel) {
      selectConditionPanel(conditionPanel, conditionPanelScrollPane, conditionPanelVisibleState,
              tableModel, this, FrameworkMessages.selectSearchField());
    }
  }

  /**
   * Allows the user to select on of the available filter condition panels
   */
  public final void selectFilterPanel() {
    if (includeFilterPanel) {
      selectConditionPanel(filterPanel, filterPanelScrollPane, filterPanelVisibleState,
              tableModel, this, FrameworkMessages.selectFilterField());
    }
  }

  /**
   * Hides or shows the column summary panel for this EntityTablePanel, if no summary panel
   * is available calling this method has no effect.
   * @param visible if true then the summary panel is shown, if false it is hidden
   */
  public final void setSummaryPanelVisible(boolean visible) {
    summaryPanelVisibleState.set(visible);
  }

  /**
   * @return true if the column summary panel is visible, false if it is hidden
   */
  public final boolean isSummaryPanelVisible() {
    return summaryPanelScrollPane != null && summaryPanelScrollPane.isVisible();
  }

  /**
   * @param referentialIntegrityErrorHandling the action to take on a referential integrity error during delete
   */
  public final void setReferentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling) {
    this.referentialIntegrityErrorHandling = requireNonNull(referentialIntegrityErrorHandling);
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + tableModel.entityType();
  }

  /**
   * @param controlCode the control code
   * @return the control associated with {@code controlCode} or an empty Optional if the control is not available
   */
  public final Optional<Control> control(ControlCode controlCode) {
    return Optional.ofNullable(controls.get(controlCode));
  }

  /**
   * Retrieves a new value via input dialog and performs an update on the selected entities
   * assigning the value to the attribute
   * @param attributeToUpdate the attribute to update
   * @param <T> the property type
   * @see #setUpdateSelectedComponentFactory(Attribute, EntityComponentFactory)
   */
  public final <T> void updateSelectedEntities(Attribute<T> attributeToUpdate) {
    requireNonNull(attributeToUpdate);
    if (tableModel.selectionModel().isSelectionEmpty()) {
      return;
    }

    Property<T> property = tableModel.entityDefinition().property(attributeToUpdate);
    List<Entity> selectedEntities = Entity.deepCopy(tableModel.selectionModel().getSelectedItems());
    Collection<T> values = Entity.getDistinct(attributeToUpdate, selectedEntities);
    T initialValue = values.size() == 1 ? values.iterator().next() : null;
    ComponentValue<T, ?> componentValue = createUpdateSelectedComponentValue(attributeToUpdate, initialValue);
    State validValueState = State.state(initialValue != null || property.isNullable());
    componentValue.addDataListener(value -> validValueState.set(value != null || property.isNullable()));
    boolean updatePerformed = false;
    while (!updatePerformed) {
      T newValue = Dialogs.inputDialog(componentValue)
              .owner(this)
              .title(MESSAGES.getString("update"))
              .caption(property.caption())
              .inputValidState(validValueState)
              .show();
      Entity.put(attributeToUpdate, newValue, selectedEntities);
      updatePerformed = update(selectedEntities);
    }
  }

  /**
   * Shows a dialog containing lists of entities depending on the selected entities via foreign key
   */
  public final void viewSelectionDependencies() {
    if (!tableModel.selectionModel().isSelectionEmpty()) {
      displayDependenciesDialog(tableModel.selectionModel().getSelectedItems(), tableModel.connectionProvider(), this);
      table.requestFocusInWindow();//otherwise the JRootPane keeps the focus after the popup menu has been closed
    }
  }

  /**
   * Deletes the entities selected in the underlying table model
   * @see #confirmDelete()
   */
  public final void deleteWithConfirmation() {
    try {
      if (confirmDelete()) {
        WaitCursor.show(this);
        try {
          tableModel.deleteSelected();
        }
        finally {
          WaitCursor.hide(this);
        }
      }
    }
    catch (ReferentialIntegrityException e) {
      LOG.debug(e.getMessage(), e);
      onReferentialIntegrityException(e, tableModel.selectionModel().getSelectedItems());
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      onException(e);
    }
  }

  /**
   * Prints the table
   * @see JTable#print()
   * @throws java.awt.print.PrinterException in case of a print exception
   */
  public final void printTable() throws PrinterException {
    table.print();
  }

  /**
   * Handles the given exception. If the referential error handling is {@link ReferentialIntegrityErrorHandling#DISPLAY_DEPENDENCIES},
   * the dependencies of the given entity are displayed to the user, otherwise {@link #onException(Throwable)} is called.
   * @param exception the exception
   * @param entities the entities causing the exception
   * @see #setReferentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling)
   */
  public void onReferentialIntegrityException(ReferentialIntegrityException exception, List<Entity> entities) {
    requireNonNull(exception);
    requireNonNull(entities);
    if (referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES) {
      displayDependenciesDialog(entities, tableModel.connectionProvider(),
              this, MESSAGES.getString("unknown_dependent_records"));
    }
    else {
      onException(exception);
    }
  }

  /**
   * Displays the exception message.
   * @param exception the exception
   */
  public void onValidationException(ValidationException exception) {
    requireNonNull(exception);
    String title = tableModel.entities()
            .definition(exception.attribute().entityType())
            .property(exception.attribute())
            .caption();
    JOptionPane.showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Handles the given exception, simply displays the error message to the user by default.
   * @param exception the exception to handle
   * @see #displayException(Throwable)
   */
  public void onException(Throwable exception) {
    displayException(exception);
  }

  /**
   * Displays the exception in a dialog, with the dialog owner as the current focus owner
   * or this panel if none is available.
   * @param exception the exception to display
   */
  public final void displayException(Throwable exception) {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusOwner == null) {
      focusOwner = EntityTablePanel.this;
    }
    Dialogs.displayExceptionDialog(exception, getParentWindow(focusOwner));
  }

  /**
   * Creates a static read-only entity table panel showing the given entities
   * @param entities the entities to show in the panel, assumed to be of the same type
   * @param connectionProvider the EntityConnectionProvider, in case the returned panel should require one
   * @return a static EntityTablePanel showing the given entities
   */
  public static EntityTablePanel entityTablePanelReadOnly(Collection<Entity> entities,
                                                          EntityConnectionProvider connectionProvider) {
    if (nullOrEmpty(entities)) {
      throw new IllegalArgumentException("Cannot create a EntityTablePanel without the entities");
    }

    SwingEntityEditModel editModel = new SwingEntityEditModel(entities.iterator().next().type(), connectionProvider);
    editModel.setReadOnly(true);
    SwingEntityTableModel tableModel = new SwingEntityTableModel(editModel) {
      @Override
      protected Collection<Entity> refreshItems() {
        return entities;
      }
    };
    tableModel.refresh();

    return entityTablePanel(tableModel);
  }

  /**
   * Creates a static entity table panel showing the given entities, note that this table panel will
   * provide a popup menu for updating and deleting the selected entities unless the underlying entities are read-only.
   * @param entities the entities to show in the panel
   * @param connectionProvider the EntityConnectionProvider, in case the returned panel should require one
   * @return a static EntityTablePanel showing the given entities
   */
  public static EntityTablePanel entityTablePanel(Collection<Entity> entities,
                                                  EntityConnectionProvider connectionProvider) {
    if (nullOrEmpty(entities)) {
      throw new IllegalArgumentException("Cannot create a EntityTablePanel without the entities");
    }

    EntityType entityType = entities.iterator().next().type();
    SwingEntityTableModel tableModel = new SwingEntityTableModel(entityType, connectionProvider) {
      @Override
      protected Collection<Entity> refreshItems() {
        return entities;
      }
    };
    tableModel.refresh();

    return entityTablePanel(tableModel);
  }

  /**
   * Creates an entity table panel based on the given table model.
   * If the table model is not read only, a popup menu for updating or deleting the selected entities is provided.
   * @param tableModel the table model
   * @return an entity table panel based on the given model
   */
  public static EntityTablePanel entityTablePanel(SwingEntityTableModel tableModel) {
    EntityTablePanel tablePanel = new EntityTablePanel(tableModel) {
      @Override
      protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
        return additionalPopupMenuControls.get(0);
      }
    };
    Controls popupMenuControls = Controls.controls();
    tablePanel.control(ControlCode.UPDATE_SELECTED).ifPresent(popupMenuControls::add);
    tablePanel.control(ControlCode.DELETE_SELECTED).ifPresent(popupMenuControls::add);
    if (!popupMenuControls.isEmpty()) {
      popupMenuControls.addSeparator();
    }
    tablePanel.control(ControlCode.VIEW_DEPENDENCIES).ifPresent(popupMenuControls::add);
    tablePanel.addPopupMenuControls(popupMenuControls);
    tablePanel.setIncludeConditionPanel(false);
    tablePanel.initializePanel();

    return tablePanel;
  }

  /**
   * Initializes the UI, while presenting a wait cursor to the user.
   * Note that calling this method more than once has no effect.
   * @return this EntityTablePanel instance
   */
  public final EntityTablePanel initializePanel() {
    if (!panelInitialized) {
      WaitCursor.show(this);
      try {
        setupControls();
        initializeTable();
        layoutPanel(tablePanel, includeSouthPanel ? initializeSouthPanel() : null);
        setConditionPanelVisibleInternal(conditionPanelVisibleState.get());
        setSummaryPanelVisibleInternal(summaryPanelVisibleState.get());
        bindEvents();
        setupKeyboardActions();
      }
      finally {
        panelInitialized = true;
        WaitCursor.hide(this);
      }
    }

    return this;
  }

  /**
   * Initializes the south panel, override and return null for no south panel.
   * Not called if the south panel has been disabled via {@link #setIncludeSouthPanel(boolean)}.
   * @return the south panel, or null if no south panel should be included
   */
  protected JPanel initializeSouthPanel() {
    searchFieldPanel = Components.panel(new GridBagLayout())
            .add(table.searchField(), createHorizontalFillConstraints())
            .build();
    southPanelSplitPane = Components.splitPane()
            .continuousLayout(true)
            .resizeWeight(0.35)
            .leftComponent(searchFieldPanel)
            .rightComponent(statusPanel)
            .build();
    southPanel.add(southPanelSplitPane, BorderLayout.CENTER);
    southPanel.add(refreshButtonToolBar, BorderLayout.WEST);
    southToolBar = createSouthToolBar();
    if (southToolBar != null) {
      southPanel.add(southToolBar, BorderLayout.EAST);
    }

    return southPanel;
  }

  /**
   * Sets up the default keyboard actions.
   */
  protected void setupKeyboardActions() {
    control(ControlCode.REQUEST_TABLE_FOCUS).ifPresent(control ->
            KeyEvents.builder(VK_T)
                    .modifiers(CTRL_DOWN_MASK)
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(ControlCode.SELECT_CONDITION_PANEL).ifPresent(control ->
            KeyEvents.builder(VK_S)
                    .modifiers(CTRL_DOWN_MASK)
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(ControlCode.TOGGLE_CONDITION_PANEL).ifPresent(control ->
            KeyEvents.builder(VK_S)
                    .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(ControlCode.TOGGLE_FILTER_PANEL).ifPresent(control ->
            KeyEvents.builder(VK_F)
                    .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
    control(ControlCode.SELECT_FILTER_PANEL).ifPresent(control ->
            KeyEvents.builder(VK_F)
                    .modifiers(CTRL_DOWN_MASK | SHIFT_DOWN_MASK)
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .action(control)
                    .enable(this));
  }

  /**
   * Associates {@code control} with {@code controlCode}
   * @param controlCode the control code
   * @param control the control to associate with {@code controlCode}, null for none
   * @throws IllegalStateException in case the panel has already been initialized
   */
  protected final void setControl(ControlCode controlCode, Control control) {
    checkIfInitialized();
    requireNonNull(controlCode);
    if (control == null) {
      controls.remove(controlCode);
    }
    else {
      controls.put(controlCode, control);
    }
  }

  protected Controls createToolBarControls(List<Controls> additionalToolBarControls) {
    requireNonNull(additionalToolBarControls);
    Controls toolbarControls = Controls.controls();
    control(ControlCode.TOGGLE_SUMMARY_PANEL).ifPresent(toolbarControls::add);
    Control toggleConditionPanelControl = control(ControlCode.TOGGLE_CONDITION_PANEL).orElse(null);
    Control toggleFilterPanelControl = control(ControlCode.TOGGLE_FILTER_PANEL).orElse(null);
    if (toggleConditionPanelControl != null || toggleFilterPanelControl != null) {
      if (toggleConditionPanelControl != null) {
        toolbarControls.add(toggleConditionPanelControl);
      }
      if (toggleFilterPanelControl != null) {
        toolbarControls.add(toggleFilterPanelControl);
      }
      toolbarControls.addSeparator();
    }
    control(ControlCode.DELETE_SELECTED).ifPresent(toolbarControls::add);
    control(ControlCode.PRINT_TABLE).ifPresent(toolbarControls::add);
    control(ControlCode.CLEAR_SELECTION).ifPresent(control -> {
      toolbarControls.add(control);
      toolbarControls.addSeparator();
    });
    control(ControlCode.MOVE_SELECTION_UP).ifPresent(toolbarControls::add);
    control(ControlCode.MOVE_SELECTION_DOWN).ifPresent(toolbarControls::add);
    additionalToolBarControls.forEach(additionalControls -> {
      toolbarControls.addSeparator();
      additionalControls.actions().forEach(toolbarControls::add);
    });

    return toolbarControls;
  }

  /**
   * Creates a Controls instance containing the controls to include in the table popup menu.
   * Returns null or an empty Controls instance to indicate that no popup menu should be included.
   * @param additionalPopupMenuControls any additional controls to include in the popup menu
   * @return Controls on which to base the table popup menu, null or an empty Controls instance
   * if no popup menu should be included
   */
  protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
    requireNonNull(additionalPopupMenuControls);
    Controls popupControls = Controls.controls();
    control(ControlCode.REFRESH).ifPresent(popupControls::add);
    control(ControlCode.CLEAR).ifPresent(popupControls::add);
    if (!popupControls.isEmpty()) {
      popupControls.addSeparator();
    }
    addAdditionalControls(popupControls, additionalPopupMenuControls);
    State separatorRequired = State.state();
    control(ControlCode.UPDATE_SELECTED).ifPresent(control -> {
      popupControls.add(control);
      separatorRequired.set(true);
    });
    control(ControlCode.DELETE_SELECTED).ifPresent(control -> {
      popupControls.add(control);
      separatorRequired.set(true);
    });
    if (separatorRequired.get()) {
      popupControls.addSeparator();
      separatorRequired.set(false);
    }
    control(ControlCode.VIEW_DEPENDENCIES).ifPresent(control -> {
      popupControls.add(control);
      separatorRequired.set(true);
    });
    if (separatorRequired.get()) {
      popupControls.addSeparator();
      separatorRequired.set(false);
    }
    Controls printControls = createPrintMenuControls();
    if (printControls != null && !printControls.isEmpty()) {
      popupControls.add(printControls);
      separatorRequired.set(true);
    }
    Controls columnControls = createColumnControls();
    if (!columnControls.isEmpty()) {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      popupControls.add(columnControls);
      separatorRequired.set(true);
    }
    control(ControlCode.SELECTION_MODE).ifPresent(control -> {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      popupControls.add(control);
      separatorRequired.set(true);
    });
    if (includeConditionPanel && conditionPanel != null) {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      addConditionControls(popupControls);
      separatorRequired.set(true);
    }
    if (includeFilterPanel && filterPanel != null) {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      addFilterControls(popupControls);
      separatorRequired.set(true);
    }
    control(ControlCode.COPY_TABLE_DATA).ifPresent(control -> {
      if (separatorRequired.get()) {
        popupControls.addSeparator();
      }
      popupControls.add(control);
    });

    return popupControls;
  }

  protected Controls createPrintMenuControls() {
    Controls.Builder builder = Controls.builder()
            .caption(Messages.print())
            .mnemonic(Messages.printMnemonic())
            .smallIcon(FrameworkIcons.instance().print());
    control(ControlCode.PRINT_TABLE).ifPresent(builder::control);

    return builder.build();
  }

  /**
   * Called before delete is performed, if true is returned the delete action is performed otherwise it is cancelled
   * @return true if the delete action should be performed
   */
  protected boolean confirmDelete() {
    ConfirmationMessage messages = confirmDeleteMessages();
    int res = JOptionPane.showConfirmDialog(this, messages.message(), messages.title(), JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  /**
   * @return Message and title to display in the confirm delete dialog
   */
  protected ConfirmationMessage confirmDeleteMessages() {
    return ConfirmationMessage.confirmationMessage(FrameworkMessages.confirmDeleteSelected(), FrameworkMessages.delete());
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
   * Creates a TableCellEditor for the given attribute, returns null if no editor is available
   * @param attribute the attribute
   * @return a TableCellEditor for the given attribute
   */
  protected TableCellEditor createTableCellEditor(Attribute<?> attribute) {
    Property<?> property = tableModel.entityDefinition().property(attribute);
    if (attribute instanceof ColumnProperty && !((ColumnProperty<?>) property).isUpdatable()) {
      return null;
    }

    return new EntityTableCellEditor<>(() -> createCellEditorComponentValue(attribute, null));
  }

  /**
   * This method simply adds {@code tablePanel} at location BorderLayout.CENTER and,
   * if non-null, the given {@code southPanel} to the {@code BorderLayout.SOUTH} location.
   * By overriding this method you can override the default layout.
   * @param tablePanel the panel containing the table, condition and summary panel
   * @param southPanel the south toolbar panel, null if not required
   * @see #initializeSouthPanel()
   */
  protected void layoutPanel(JPanel tablePanel, JPanel southPanel) {
    requireNonNull(tablePanel, "tablePanel");
    setLayout(new BorderLayout());
    add(tablePanel, BorderLayout.CENTER);
    if (southPanel != null) {
      add(southPanel, BorderLayout.SOUTH);
    }
  }

  /**
   * Creates the south panel toolbar, by default based on {@link #createToolBarControls(List)}
   * @return the toolbar to add to the south panel, null if none should be included
   */
  protected JToolBar createSouthToolBar() {
    Controls toolbarControls = createToolBarControls(additionalToolBarControls);
    if (toolbarControls != null && !toolbarControls.isEmpty()) {
      JToolBar toolBar = toolbarControls.createHorizontalToolBar();
      Arrays.stream(toolBar.getComponents())
              .map(JComponent.class::cast)
              .forEach(component -> component.setToolTipText(null));
      toolBar.setFocusable(false);
      toolBar.setFloatable(false);
      toolBar.setRollover(true);

      return toolBar;
    }

    return null;
  }

  private void setupControls() {
    if (includeDeleteSelectedControl()) {
      controls.putIfAbsent(ControlCode.DELETE_SELECTED, createDeleteSelectedControl());
    }
    if (includeUpdateSelectedControls()) {
      controls.putIfAbsent(ControlCode.UPDATE_SELECTED, createUpdateSelectedControls());
    }
    if (includeClearControl) {
      controls.putIfAbsent(ControlCode.CLEAR, createClearControl());
    }
    controls.putIfAbsent(ControlCode.REFRESH, createRefreshControl());
    controls.putIfAbsent(ControlCode.SELECT_COLUMNS, columnSelection == ColumnSelection.DIALOG ?
            table.createSelectColumnsControl() : table.createToggleColumnsControls());
    controls.putIfAbsent(ControlCode.RESET_COLUMNS, table.createResetColumnsControl());
    controls.putIfAbsent(ControlCode.VIEW_DEPENDENCIES, createViewDependenciesControl());
    if (summaryPanelScrollPane != null) {
      controls.putIfAbsent(ControlCode.TOGGLE_SUMMARY_PANEL, createToggleSummaryPanelControl());
    }
    if (includeConditionPanel && conditionPanel != null) {
      controls.putIfAbsent(ControlCode.CONDITION_PANEL_VISIBLE, createConditionPanelControl());
      controls.putIfAbsent(ControlCode.TOGGLE_CONDITION_PANEL, createToggleConditionPanelControl());
      controls.put(ControlCode.SELECT_CONDITION_PANEL, Control.control(this::selectConditionPanel));
    }
    if (includeFilterPanel && filterPanel != null) {
      controls.putIfAbsent(ControlCode.FILTER_PANEL_VISIBLE, createFilterPanelControl());
      controls.putIfAbsent(ControlCode.TOGGLE_FILTER_PANEL, createToggleFilterPanelControl());
      controls.put(ControlCode.SELECT_FILTER_PANEL, Control.control(this::selectFilterPanel));
    }
    controls.putIfAbsent(ControlCode.PRINT_TABLE, createPrintTableControl());
    controls.putIfAbsent(ControlCode.CLEAR_SELECTION, createClearSelectionControl());
    controls.putIfAbsent(ControlCode.MOVE_SELECTION_UP, createMoveSelectionDownControl());
    controls.putIfAbsent(ControlCode.MOVE_SELECTION_DOWN, createMoveSelectionUpControl());
    controls.putIfAbsent(ControlCode.COPY_TABLE_DATA, createCopyControls());
    if (includeSelectionModeControl) {
      controls.putIfAbsent(ControlCode.SELECTION_MODE, table.createSingleSelectionModeControl());
    }
    controls.put(ControlCode.REQUEST_TABLE_FOCUS, Control.control(table()::requestFocus));
    controls.put(ControlCode.CONFIGURE_COLUMNS, createColumnControls());
  }

  /**
   * Creates a {@link Controls} containing controls for updating the value of a single property
   * for the selected entities. These controls are enabled as long as the selection is not empty
   * and {@link EntityEditModel#updateEnabledObserver()} is enabled.
   * @return controls containing a control for each updatable property in the
   * underlying entity, for performing an update on the selected entities
   * @throws IllegalStateException in case the underlying edit model is read only or updating is not enabled
   * @see #excludeFromUpdateMenu(Attribute)
   * @see EntityEditModel#updateEnabledObserver()
   */
  private Controls createUpdateSelectedControls() {
    if (!includeUpdateSelectedControls()) {
      throw new IllegalStateException("Table model is read only or does not allow updates");
    }
    StateObserver selectionNotEmpty = tableModel.selectionModel().selectionNotEmptyObserver();
    StateObserver updateEnabled = tableModel.editModel().updateEnabledObserver();
    StateObserver enabledState = State.and(selectionNotEmpty, updateEnabled);
    Controls updateControls = Controls.builder()
            .caption(FrameworkMessages.update())
            .enabledState(enabledState)
            .smallIcon(FrameworkIcons.instance().edit())
            .description(FrameworkMessages.updateSelectedTip())
            .build();
    tableModel.entityDefinition().updatableProperties().stream()
            .filter(property -> !excludeFromUpdateMenu.contains(property.attribute()))
            .sorted(Property.propertyComparator())
            .forEach(property -> updateControls.add(Control.builder(() -> updateSelectedEntities(property.attribute()))
                    .caption(property.caption() == null ? property.attribute().name() : property.caption())
                    .enabledState(enabledState)
                    .build()));

    return updateControls;
  }

  /**
   * @return a control for showing the dependencies dialog
   */
  private Control createViewDependenciesControl() {
    return Control.builder(this::viewSelectionDependencies)
            .caption(FrameworkMessages.dependencies())
            .enabledState(tableModel.selectionModel().selectionNotEmptyObserver())
            .description(FrameworkMessages.dependenciesTip())
            .smallIcon(FrameworkIcons.instance().dependencies())
            .build();
  }

  /**
   * @return a control for deleting the selected entities
   * @throws IllegalStateException in case the underlying model is read only or if deleting is not enabled
   */
  private Control createDeleteSelectedControl() {
    if (!includeDeleteSelectedControl()) {
      throw new IllegalStateException("Table model is read only or does not allow delete");
    }
    return Control.builder(this::deleteWithConfirmation)
            .caption(FrameworkMessages.delete())
            .enabledState(State.and(
                    tableModel.editModel().deleteEnabledObserver(),
                    tableModel.selectionModel().selectionNotEmptyObserver()))
            .description(FrameworkMessages.deleteSelectedTip())
            .smallIcon(FrameworkIcons.instance().delete())
            .build();
  }

  /**
   * @return a control for printing the table
   */
  private Control createPrintTableControl() {
    String printCaption = MESSAGES.getString("print_table");
    return Control.builder(this::printTable)
            .caption(printCaption)
            .description(printCaption)
            .mnemonic(Messages.printMnemonic())
            .smallIcon(FrameworkIcons.instance().print())
            .build();
  }

  /**
   * @return a Control for refreshing the underlying table data
   */
  private Control createRefreshControl() {
    return Control.builder(tableModel::refresh)
            .caption(FrameworkMessages.refresh())
            .description(FrameworkMessages.refreshTip())
            .mnemonic(FrameworkMessages.refreshMnemonic())
            .smallIcon(FrameworkIcons.instance().refresh())
            .enabledState(tableModel.refreshingObserver().reversedObserver())
            .build();
  }

  /**
   * @return a Control for clearing the underlying table model, that is, removing all rows
   */
  private Control createClearControl() {
    return Control.builder(tableModel::clear)
            .caption(Messages.clear())
            .description(Messages.clearTip())
            .mnemonic(Messages.clearMnemonic())
            .smallIcon(FrameworkIcons.instance().clear())
            .build();
  }

  private Control createToggleConditionPanelControl() {
    if (conditionPanel == null) {
      return null;
    }

    return Control.builder(this::toggleConditionPanel)
            .smallIcon(FrameworkIcons.instance().search())
            .description(MESSAGES.getString("show_condition_panel"))
            .build();
  }

  private Control createToggleFilterPanelControl() {
    if (filterPanel == null) {
      return null;
    }

    return Control.builder(this::toggleFilterPanel)
            .smallIcon(FrameworkIcons.instance().filter())
            .description(MESSAGES.getString("show_filter_panel"))
            .build();
  }

  private Control createToggleSummaryPanelControl() {
    return ToggleControl.builder(summaryPanelVisibleState)
            .smallIcon(FrameworkIcons.instance().summary())
            .description(MESSAGES.getString("toggle_summary_tip"))
            .build();
  }

  private Control createClearSelectionControl() {
    return Control.builder(tableModel.selectionModel()::clearSelection)
            .enabledState(tableModel.selectionModel().selectionNotEmptyObserver())
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

  private Controls createColumnControls() {
    Controls.Builder builder = Controls.builder()
            .caption(MESSAGES.getString("columns"));
    control(ControlCode.SELECT_COLUMNS).ifPresent(builder::control);
    control(ControlCode.RESET_COLUMNS).ifPresent(builder::control);

    return builder.build();
  }

  private Control createConditionPanelControl() {
    return ToggleControl.builder(conditionPanelVisibleState)
            .caption(FrameworkMessages.show())
            .build();
  }

  private Control createFilterPanelControl() {
    return ToggleControl.builder(filterPanelVisibleState)
            .caption(FrameworkMessages.show())
            .build();
  }

  private Controls createCopyControls() {
    return Controls.builder()
            .caption(Messages.copy())
            .smallIcon(FrameworkIcons.instance().copy())
            .controls(createCopyCellControl(), createCopyTableRowsWithHeaderControl())
            .build();
  }

  private Control createCopyCellControl() {
    return Control.builder(table::copySelectedCell)
            .caption(FrameworkMessages.copyCell())
            .enabledState(tableModel.selectionModel().selectionNotEmptyObserver())
            .build();
  }

  private Control createCopyTableRowsWithHeaderControl() {
    return Control.builder(table::copyRowsAsTabDelimitedString)
            .caption(FrameworkMessages.copyTableWithHeader())
            .build();
  }

  private boolean includeUpdateSelectedControls() {
    long attributeCount = tableModel.entityDefinition().updatableProperties().stream()
            .map(Property::attribute)
            .filter(attribute -> !excludeFromUpdateMenu.contains(attribute))
            .count();

    return attributeCount > 0 &&
            !tableModel.isReadOnly() &&
            tableModel.isUpdateEnabled() &&
            tableModel.isBatchUpdateEnabled();
  }

  private boolean includeDeleteSelectedControl() {
    return !tableModel.isReadOnly() && tableModel.isDeleteEnabled();
  }

  private FilteredTable<SwingEntityTableModel, Entity, Attribute<?>> createTable() {
    FilteredTable<SwingEntityTableModel, Entity, Attribute<?>> filteredTable = FilteredTable.builder(tableModel)
            .cellRendererFactory(new EntityTableCellRendererFactory())
            .build();
    filteredTable.setRowHeight(filteredTable.getFont().getSize() + FONT_SIZE_TO_ROW_HEIGHT);

    return filteredTable;
  }

  private Control createConditionRefreshControl() {
    return Control.builder(tableModel::refresh)
            .enabledState(tableModel.conditionChangedObserver())
            .smallIcon(FrameworkIcons.instance().refreshRequired())
            .build();
  }

  private <T> ComponentValue<T, ? extends JComponent> createUpdateSelectedComponentValue(Attribute<T> attribute, T initialValue) {
    return ((EntityComponentFactory<T, Attribute<T>, ?>) updateSelectedComponentFactories.computeIfAbsent(attribute, a ->
            new UpdateSelectedComponentFactory<T, Attribute<T>, JComponent>())).createComponentValue(attribute, tableModel.editModel(), initialValue);
  }

  private <T> ComponentValue<T, ? extends JComponent> createCellEditorComponentValue(Attribute<T> attribute, T initialValue) {
    return ((EntityComponentFactory<T, Attribute<T>, ?>) tableCellEditorComponentFactories.computeIfAbsent(attribute, a ->
            new DefaultEntityComponentFactory<T, Attribute<T>, JComponent>())).createComponentValue(attribute, tableModel.editModel(), initialValue);
  }

  private JToolBar createRefreshButtonToolBar() {
    KeyEvents.builder(VK_F5)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(conditionRefreshControl)
            .enable(this);

    JToolBar toolBar = Controls.controls(conditionRefreshControl).createHorizontalToolBar();
    toolBar.setFocusable(false);
    toolBar.getComponentAtIndex(0).setFocusable(false);
    toolBar.setFloatable(false);
    toolBar.setRollover(true);
    //made visible when condition panel is visible
    toolBar.setVisible(false);

    return toolBar;
  }

  private FilteredTableConditionPanel<Attribute<?>> createConditionPanel(ColumnConditionPanel.Factory<Attribute<?>> conditionPanelFactory) {
    return conditionPanelFactory == null ? null : filteredTableConditionPanel(tableModel.conditionModel(), tableModel.columnModel(), conditionPanelFactory);
  }

  private JScrollPane createConditionPanelScrollPane() {
    return conditionPanel == null ? null : createHiddenLinkedScrollPane(tableScrollPane, conditionPanel);
  }

  private JScrollPane createFilterPanelScrollPane() {
    return filterPanel == null ? null : createHiddenLinkedScrollPane(tableScrollPane, filterPanel);
  }

  private FilteredTableColumnComponentPanel<Attribute<?>, JPanel> createSummaryPanel() {
    Map<Attribute<?>, JPanel> columnSummaryPanels = createColumnSummaryPanels(tableModel);
    if (columnSummaryPanels.isEmpty()) {
      return null;
    }

    return filteredTableColumnComponentPanel(tableModel.columnModel(), columnSummaryPanels);
  }

  private JScrollPane createSummaryPanelScrollPane() {
    if (summaryPanel == null) {
      return null;
    }

    return createHiddenLinkedScrollPane(tableScrollPane, summaryPanel);
  }

  private JPanel createTablePanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(tableScrollPane, BorderLayout.CENTER);
    if (conditionPanelScrollPane != null) {
      panel.add(conditionPanelScrollPane, BorderLayout.NORTH);
    }
    JPanel southPanel = new JPanel(new BorderLayout());
    if (summaryPanelScrollPane != null) {
      southPanel.add(summaryPanelScrollPane, BorderLayout.NORTH);
    }
    if (filterPanelScrollPane != null) {
      southPanel.add(filterPanelScrollPane, BorderLayout.CENTER);
    }
    panel.add(southPanel, BorderLayout.SOUTH);

    return panel;
  }

  private void bindEvents() {
    if (includeDeleteSelectedControl()) {
      KeyEvents.builder(VK_DELETE)
              .action(controls.get(ControlCode.DELETE_SELECTED))
              .enable(table);
    }
    if (INCLUDE_ENTITY_MENU.get()) {
      KeyEvents.builder(VK_V)
              .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
              .action(Control.control(this::showEntityMenu))
              .enable(table);
    }
    conditionPanelVisibleState.addDataListener(this::setConditionPanelVisibleInternal);
    filterPanelVisibleState.addDataListener(this::setFilterPanelVisibleInternal);
    summaryPanelVisibleState.addDataListener(this::setSummaryPanelVisibleInternal);
    tableModel.conditionModel().addChangeListener(condition -> onConditionChanged());
    tableModel.refreshingObserver().addDataListener(this::onRefreshingChanged);
    tableModel.addRefreshFailedListener(this::onException);
    tableModel.editModel().addEntitiesEditedListener(table::repaint);
    if (conditionPanel != null) {
      KeyEvents.builder(VK_ENTER)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(conditionRefreshControl)
              .enable(conditionPanel);
      conditionPanel.addFocusGainedListener(table::scrollToColumn);
      addRefreshOnEnterControl(tableModel.columnModel().columns(), conditionPanel, conditionRefreshControl);
      conditionPanel.addAdvancedViewListener(advanced -> {
        if (isConditionPanelVisible()) {
          revalidate();
        }
      });
    }
    if (filterPanel != null) {
      filterPanel.addFocusGainedListener(table::scrollToColumn);
      filterPanel.addAdvancedViewListener(advanced -> {
        if (isFilterPanelVisible()) {
          revalidate();
        }
      });
    }
  }

  private void setConditionPanelVisibleInternal(boolean visible) {
    if (conditionPanelScrollPane != null) {
      conditionPanelScrollPane.setVisible(visible);
      refreshButtonToolBar.setVisible(refreshButtonVisible == RefreshButtonVisible.ALWAYS || visible);
      revalidate();
    }
  }

  private void setFilterPanelVisibleInternal(boolean visible) {
    if (filterPanelScrollPane != null) {
      filterPanelScrollPane.setVisible(visible);
      revalidate();
    }
  }

  private void setSummaryPanelVisibleInternal(boolean visible) {
    if (summaryPanelScrollPane != null) {
      summaryPanelScrollPane.setVisible(visible);
      revalidate();
    }
  }

  private void initializeTable() {
    tableModel.columnModel().columns().forEach(this::configureColumn);
    JTableHeader header = table.getTableHeader();
    header.setFocusable(false);
    if (includePopupMenu) {
      addTablePopupMenu();
    }
  }

  private void configureColumn(FilteredTableColumn<Attribute<?>> column) {
    column.setCellEditor(createTableCellEditor(column.getIdentifier()));
    column.setHeaderRenderer(new HeaderRenderer(column.getHeaderRenderer()));
  }

  private void addTablePopupMenu() {
    Controls popupControls = createPopupMenuControls(additionalPopupControls);
    if (popupControls == null || popupControls.isEmpty()) {
      return;
    }

    JPopupMenu popupMenu = popupControls.createPopupMenu();
    table.setComponentPopupMenu(popupMenu);
    if (table.getParent() != null) {
      ((JComponent) table.getParent()).setComponentPopupMenu(popupMenu);
    }
    KeyEvents.builder(VK_G)
            .modifiers(CTRL_DOWN_MASK)
            .action(Control.control(() -> {
              Point location = popupLocation(table);
              popupMenu.show(table, location.x, location.y);
            }))
            .enable(table);
  }

  private void checkIfInitialized() {
    if (panelInitialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
  }

  private void addConditionControls(Controls popupControls) {
    Controls conditionControls = Controls.builder()
            .caption(FrameworkMessages.search())
            .smallIcon(FrameworkIcons.instance().search())
            .build();
    control(ControlCode.CONDITION_PANEL_VISIBLE).ifPresent(conditionControls::add);
    Controls conditionPanelControls = conditionPanel.controls();
    if (!conditionPanelControls.isEmpty()) {
      conditionControls.addAll(conditionPanelControls);
      conditionControls.addSeparator();
    }
    conditionControls.add(ToggleControl.builder(tableModel.queryConditionRequiredState())
            .caption(MESSAGES.getString("require_query_condition"))
            .description(MESSAGES.getString("require_query_condition_description"))
            .build());
    if (!conditionControls.isEmpty()) {
      popupControls.add(conditionControls);
    }
  }

  private void addFilterControls(Controls popupControls) {
    Controls filterControls = Controls.builder()
            .caption(FrameworkMessages.filter())
            .smallIcon(FrameworkIcons.instance().filter())
            .build();
    control(ControlCode.FILTER_PANEL_VISIBLE).ifPresent(filterControls::add);
    Controls filterPanelControls = filterPanel.controls();
    if (!filterPanelControls.isEmpty()) {
      filterControls.addAll(filterPanelControls);
    }
    if (!filterControls.isEmpty()) {
      popupControls.add(filterControls);
    }
  }

  private void showEntityMenu() {
    Entity selected = tableModel.selectionModel().getSelectedItem();
    if (selected != null) {
      Point location = popupLocation(table);
      new EntityPopupMenu(selected.copy(), tableModel.connectionProvider().connection()).show(table, location.x, location.y);
    }
  }

  private void onConditionChanged() {
    table.getTableHeader().repaint();
    table.repaint();
  }

  private boolean update(List<Entity> entities) {
    try {
      WaitCursor.show(this);
      try {
        tableModel.update(entities);

        return true;
      }
      finally {
        WaitCursor.hide(this);
      }
    }
    catch (ValidationException e) {
      LOG.debug(e.getMessage(), e);
      onValidationException(e);
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      onException(e);
    }

    return false;
  }

  private void onRefreshingChanged(boolean refreshing) {
    if (refreshing) {
      WaitCursor.show(EntityTablePanel.this);
    }
    else {
      WaitCursor.hide(EntityTablePanel.this);
    }
  }

  private static final void toggleConditionPanel(JScrollPane scrollPane, State advancedState, State visibleState) {
    if (scrollPane != null && scrollPane.isVisible()) {
      if (advancedState.get()) {
        visibleState.set(false);
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

  private static final void selectConditionPanel(FilteredTableConditionPanel<Attribute<?>> tableConditionPanel,
                                                 JScrollPane conditionPanelScrollPane, State conditionPanelVisibleState,
                                                 SwingEntityTableModel tableModel, JComponent dialogOwner, String dialogTitle) {
    if (tableConditionPanel != null) {
      if (!(conditionPanelScrollPane != null && conditionPanelScrollPane.isVisible())) {
        conditionPanelVisibleState.set(true);
      }
      List<Property<?>> properties = tableConditionPanel.componentPanel().columnComponents().values().stream()
              .filter(panel -> tableModel.columnModel().isColumnVisible(panel.model().columnIdentifier()))
              .map(panel -> tableModel.entityDefinition().property(panel.model().columnIdentifier()))
              .sorted(Property.propertyComparator())
              .collect(toList());
      if (properties.size() == 1) {
        tableConditionPanel.conditionPanel(properties.get(0).attribute()).requestInputFocus();
      }
      else if (!properties.isEmpty()) {
        Dialogs.selectionDialog(properties)
                .owner(dialogOwner)
                .title(dialogTitle)
                .selectSingle()
                .ifPresent(property -> tableConditionPanel.conditionPanel(property.attribute()).requestInputFocus());
      }
    }
  }

  private static GridBagConstraints createHorizontalFillConstraints() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    constraints.insets = new Insets(0, Layouts.HORIZONTAL_VERTICAL_GAP.get(), 0, Layouts.HORIZONTAL_VERTICAL_GAP.get());

    return constraints;
  }

  private static void addRefreshOnEnterControl(Collection<FilteredTableColumn<Attribute<?>>> columns,
                                               FilteredTableConditionPanel<Attribute<?>> tableConditionPanel,
                                               Control refreshControl) {
    columns.forEach(column -> {
      ColumnConditionPanel<?, ?> columnConditionPanel =
              tableConditionPanel.componentPanel().columnComponents().get(column.getIdentifier());
      if (columnConditionPanel != null) {
        enableRefreshOnEnterControl(columnConditionPanel.operatorComboBox(), refreshControl);
        enableRefreshOnEnterControl(columnConditionPanel.equalField(), refreshControl);
        enableRefreshOnEnterControl(columnConditionPanel.lowerBoundField(), refreshControl);
        enableRefreshOnEnterControl(columnConditionPanel.upperBoundField(), refreshControl);
      }
    });
  }

  private static void enableRefreshOnEnterControl(JComponent component, Control refreshControl) {
    if (component instanceof JComboBox) {
      new ComboBoxEnterPressedAction((JComboBox<?>) component, refreshControl);
    }
    else if (component instanceof TemporalField) {
      ((TemporalField<?>) component).addActionListener(refreshControl);
    }
  }

  private static void addAdditionalControls(Controls popupControls, List<Controls> additionalPopupControls) {
    additionalPopupControls.forEach(controls -> {
      if (nullOrEmpty(controls.getCaption())) {
        popupControls.addAll(controls);
      }
      else {
        popupControls.add(controls);
      }
      popupControls.addSeparator();
    });
  }

  private static Map<Attribute<?>, JPanel> createColumnSummaryPanels(FilteredTableModel<?, Attribute<?>> tableModel) {
    Map<Attribute<?>, JPanel> components = new HashMap<>();
    tableModel.columnModel().columns().forEach(column ->
            tableModel.columnSummaryModel(column.getIdentifier())
                    .ifPresent(columnSummaryModel ->
                            components.put(column.getIdentifier(), columnSummaryPanel(columnSummaryModel))));

    return components;
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
              && ((FilteredTableCellRenderer) renderer).isColumnShadingEnabled()
              && tableModel.conditionModel().isEnabled(tableColumn.getIdentifier());
      Font defaultFont = component.getFont();
      component.setFont(useBoldFont ? defaultFont.deriveFont(defaultFont.getStyle() | Font.BOLD) : defaultFont);

      return component;
    }
  }

  private static final class UpdateSelectedComponentFactory<T, A extends Attribute<T>, C extends JComponent> extends DefaultEntityComponentFactory<T, A, C> {

    @Override
    public ComponentValue<T, C> createComponentValue(A attribute, SwingEntityEditModel editModel, T initialValue) {
      requireNonNull(attribute, "attribute");
      requireNonNull(editModel, "editModel");
      Property<T> property = editModel.entityDefinition().property(attribute);
      if (!(property instanceof ItemProperty) && attribute.isString()) {
        //special handling for non-item based String properties, text input panel instead of a text field
        return (ComponentValue<T, C>) new EntityComponents(editModel.entityDefinition())
                .textInputPanel((Attribute<String>) attribute)
                .initialValue((String) initialValue)
                .buildValue();
      }

      return super.createComponentValue(attribute, editModel, initialValue);
    }
  }

  private static final class StatusPanel extends JPanel {

    private static final String STATUS = "status";
    private static final String REFRESHING = "refreshing";

    private boolean showRefreshProgressBar = false;

    private StatusPanel(SwingEntityTableModel tableModel) {
      super(new CardLayout());
      add(Components.label(tableModel.statusMessageObserver())
              .horizontalAlignment(SwingConstants.CENTER)
              .build(), STATUS);
      add(createRefreshingProgressPanel(), REFRESHING);
      CardLayout layout = (CardLayout) getLayout();
      tableModel.refreshingObserver().addDataListener(isRefreshing -> {
        if (showRefreshProgressBar) {
          layout.show(this, isRefreshing ? REFRESHING : STATUS);
        }
      });
    }

    private static JPanel createRefreshingProgressPanel() {
      JProgressBar progressBar = new JProgressBar();
      progressBar.setIndeterminate(true);
      progressBar.setString(MESSAGES.getString(REFRESHING));
      progressBar.setStringPainted(true);

      return Components.panel(new GridBagLayout())
              .add(progressBar, createHorizontalFillConstraints())
              .build();
    }
  }
}
