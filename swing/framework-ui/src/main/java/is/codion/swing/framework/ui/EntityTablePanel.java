/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.Util;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnFilterModel;
import is.codion.common.properties.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ToggleAdvancedButton;
import is.codion.swing.common.ui.component.table.ColumnSummaryPanel;
import is.codion.swing.common.ui.component.table.ConditionPanelFactory;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.component.table.TableColumnComponentPanel;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import is.codion.swing.common.ui.dialog.DialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityTableModel;

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
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import static is.codion.common.Util.nullOrEmpty;
import static is.codion.swing.common.ui.Windows.getParentWindow;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.util.Objects.requireNonNull;

/**
 * The EntityTablePanel is a UI class based on the EntityTableModel class.
 * It consists of a JTable as well as filtering/searching and summary panels.
 *
 * The default layout is as follows
 * <pre>
 *  ____________________________________________________
 * |                conditionPanel                      |
 * |____________________________________________________|
 * |                                                    |
 * |                                                    |
 * |                                                    |
 * |                                                    |
 * |                entityTable (JTable)                |
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
 */
public class EntityTablePanel extends JPanel implements DialogExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(EntityTablePanel.class);

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityTablePanel.class.getName());

  /**
   * Specifies whether columns can be rearranged in tables<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> ALLOW_COLUMN_REORDERING =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.allowColumnReordering", true);

  /**
   * Specifies whether table condition panels should be visible or not by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> CONDITION_PANEL_VISIBLE =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.conditionPanelVisible", false);

  /**
   * Specifies the default table column resize mode for tables in the application<br>
   * Value type: Integer (JTable.AUTO_RESIZE_*)<br>
   * Default value: JTable.AUTO_RESIZE_OFF
   */
  public static final PropertyValue<Integer> TABLE_AUTO_RESIZE_MODE =
          Configuration.integerValue("is.codion.swing.framework.ui.EntityTablePanel.tableAutoResizeMode", JTable.AUTO_RESIZE_OFF);

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
   * Specifies whether the refresh button toolbar should always be visible or hidden when the condition panel is not visible<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> REFRESH_TOOLBAR_ALWAYS_VISIBLE =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityTablePanel.refreshToolbarAlwaysVisible", false);

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
    EXPORT_JSON,
    SELECTION_MODE,
    CLEAR,
    REFRESH,
    TOGGLE_SUMMARY_PANEL,
    TOGGLE_CONDITION_PANEL,
    CONDITION_PANEL_VISIBLE,
    CLEAR_SELECTION,
    MOVE_SELECTION_UP,
    MOVE_SELECTION_DOWN,
    COPY_TABLE_DATA,
    REQUEST_TABLE_FOCUS,
    REQUEST_SEARCH_FIELD_FOCUS,
    SELECT_CONDITION_PANEL
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
  private final State summaryPanelVisibleState = State.state();

  private final Map<ControlCode, Control> controls = new EnumMap<>(ControlCode.class);

  private final Map<Attribute<?>, EntityComponentFactory<?, ?, ?>> updateSelectedComponentFactories = new HashMap<>();
  private final Map<Attribute<?>, EntityComponentFactory<?, ?, ?>> tableCellEditorComponentFactories = new HashMap<>();

  private final SwingEntityTableModel tableModel;

  private final FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> table;

  private final JScrollPane tableScrollPane;

  private final AbstractEntityTableConditionPanel conditionPanel;

  private final JScrollPane conditionScrollPane;

  private final TableColumnComponentPanel<JPanel> summaryPanel;

  private final JScrollPane summaryScrollPane;

  private final JPanel southPanel = new JPanel(new BorderLayout());

  /**
   * Base panel for the table, condition and summary panels
   */
  private final JPanel tablePanel;

  /**
   * the toolbar containing the refresh button
   */
  private final JToolBar refreshToolBar;

  /**
   * the label for showing the status of the table model, that is, the number of rows, number of selected rows etc.
   */
  private final JLabel statusMessageLabel;

  private final List<Controls> additionalPopupControls = new ArrayList<>();
  private final List<Controls> additionalToolBarControls = new ArrayList<>();
  private final Set<Attribute<?>> excludeFromUpdateMenu = new HashSet<>();

  private JPanel searchFieldPanel;

  private JSplitPane southPanelSplitPane;

  private JToolBar southToolBar;

  /**
   * specifies whether the refresh toolbar should always be visible, instead of being hidding along with the condition panel
   */
  private boolean refreshToolbarAlwaysVisible = REFRESH_TOOLBAR_ALWAYS_VISIBLE.get();

  /**
   * specifies whether to include the south panel
   */
  private boolean includeSouthPanel = true;

  /**
   * specifies whether to include the condition panel
   */
  private boolean includeConditionPanel = true;

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
   * @param tableModel the EntityTableModel instance
   */
  public EntityTablePanel(SwingEntityTableModel tableModel) {
    this(tableModel, new EntityTableConditionPanel(tableModel.getTableConditionModel(), tableModel.getColumnModel()));
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param conditionPanel the condition panel, if any
   */
  public EntityTablePanel(SwingEntityTableModel tableModel, AbstractEntityTableConditionPanel conditionPanel) {
    this.tableModel = requireNonNull(tableModel, "tableModel");
    this.table = initializeFilteredTable();
    this.conditionPanel = conditionPanel;
    this.tableScrollPane = new JScrollPane(table);
    this.conditionScrollPane = initializeConditionScrollPane(tableScrollPane);
    this.summaryPanel = initializeSummaryPanel();
    this.summaryScrollPane = initializeSummaryScrollPane(tableScrollPane);
    this.tablePanel = initializeTablePanel(tableScrollPane);
    this.refreshToolBar = initializeRefreshToolBar();
    this.statusMessageLabel = Components.label(tableModel.getStatusMessageObserver())
            .horizontalAlignment(SwingConstants.CENTER)
            .build();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(tablePanel, table, statusMessageLabel, conditionPanel, conditionScrollPane,
            summaryScrollPane, summaryPanel, southPanel, refreshToolBar, southToolBar, southPanelSplitPane, searchFieldPanel);
    if (tableScrollPane != null) {
      Utilities.updateUI(tableScrollPane, tableScrollPane.getViewport(),
              tableScrollPane.getHorizontalScrollBar(), tableScrollPane.getVerticalScrollBar());
    }
  }

  /**
   * @return the filtered table instance
   */
  public final FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> getTable() {
    return table;
  }

  /**
   * @return the EntityTableModel used by this EntityTablePanel
   */
  public final SwingEntityTableModel getTableModel() {
    return tableModel;
  }

  /**
   * Specifies that the given property should be excluded from the update selected entities menu.
   * @param attribute the id of the property to exclude from the update menu
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void excludeFromUpdateMenu(Attribute<?> attribute) {
    checkIfInitialized();
    getTableModel().getEntityDefinition().getProperty(attribute);//just validating that the property exists
    excludeFromUpdateMenu.add(attribute);
  }

  /**
   * @param additionalPopupControls a set of controls to add to the table popup menu
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void addPopupControls(Controls additionalPopupControls) {
    checkIfInitialized();
    this.additionalPopupControls.add(requireNonNull(additionalPopupControls));
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
   * @return true if the refresh toolbar should always be visible
   */
  public final boolean isRefreshToolbarAlwaysVisible() {
    return refreshToolbarAlwaysVisible;
  }

  /**
   * @param refreshToolbarAlwaysVisible true if the refresh toolbar should always be visible,
   * instead of being hidden when the condition panel is not visible
   */
  public final void setRefreshToolbarAlwaysVisible(boolean refreshToolbarAlwaysVisible) {
    this.refreshToolbarAlwaysVisible = refreshToolbarAlwaysVisible;
    this.refreshToolBar.setVisible(refreshToolbarAlwaysVisible || isConditionPanelVisible());
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
    return conditionScrollPane != null && conditionScrollPane.isVisible();
  }

  /**
   * Sets the component factory for the given attribute, used when updating entities via {@link #updateSelectedEntities(Property)}.
   * @param attribute the attribute
   * @param componentFactory the component factory
   * @param <T> the value type
   * @param <A> the attribute type
   * @param <C> the component type
   */
  public final <T, A extends Attribute<T>, C extends JComponent> void setUpdateSelectedComponentFactory(A attribute,
                                                                                                        EntityComponentFactory<T, A, C> componentFactory) {
    getTableModel().getEntityDefinition().getProperty(attribute);
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
    getTableModel().getEntityDefinition().getProperty(attribute);
    tableCellEditorComponentFactories.put(attribute, requireNonNull(componentFactory));
  }

  /**
   * @return the condition panel being used by this EntityTablePanel
   */
  public final AbstractEntityTableConditionPanel getConditionPanel() {
    return conditionPanel;
  }

  /**
   * Toggles the condition panel through the states hidden, visible and in case it can, advanced
   * @see AbstractEntityTableConditionPanel#hasAdvancedView()
   */
  public final void toggleConditionPanel() {
    if (conditionPanel == null) {
      return;
    }
    if (conditionPanel.hasAdvancedView()) {
      State advancedState = conditionPanel.getAdvancedState();
      if (isConditionPanelVisible()) {
        if (advancedState.get()) {
          setConditionPanelVisible(false);
        }
        else {
          advancedState.set(true);
        }
      }
      else {
        advancedState.set(false);
        setConditionPanelVisible(true);
      }
    }
    else {
      setConditionPanelVisible(!isConditionPanelVisible());
    }
  }

  /**
   * Allows the user to select on of the available search condition fields
   * @see EntityTableConditionPanel#selectConditionPanel()
   */
  public final void selectConditionPanel() {
    if (includeConditionPanel && conditionPanel != null) {
      if (!isConditionPanelVisible()) {
        setConditionPanelVisible(true);
      }
      conditionPanel.selectConditionPanel();
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
    return summaryScrollPane != null && summaryScrollPane.isVisible();
  }

  /**
   * @param referentialIntegrityErrorHandling the action to take on a referential integrity error during delete
   */
  public final void setReferentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling) {
    this.referentialIntegrityErrorHandling = requireNonNull(referentialIntegrityErrorHandling);
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + tableModel.getEntityType();
  }

  /**
   * @param controlCode the control code
   * @return true if this table panel contains the given control
   */
  public final boolean containsControl(ControlCode controlCode) {
    return controls.containsKey(requireNonNull(controlCode));
  }

  /**
   * @param controlCode the control code
   * @return the control associated with {@code controlCode}
   * @throws IllegalArgumentException in case no control is associated with the given control code
   * @see #containsControl(ControlCode)
   */
  public final Control getControl(ControlCode controlCode) {
    if (!containsControl(controlCode)) {
      throw new IllegalArgumentException(controlCode + " control not available in panel: " + this);
    }

    return controls.get(controlCode);
  }

  /**
   * Creates a {@link Controls} containing controls for updating the value of a single property
   * for the selected entities. These controls are enabled as long as the selection is not empty
   * and {@link EntityEditModel#getUpdateEnabledObserver()} is enabled.
   * @return controls containing a control for each updatable property in the
   * underlying entity, for performing an update on the selected entities
   * @throws IllegalStateException in case the underlying edit model is read only or updating is not enabled
   * @see #excludeFromUpdateMenu(Attribute)
   * @see EntityEditModel#getUpdateEnabledObserver()
   */
  public final Controls createUpdateSelectedControls() {
    if (!includeUpdateSelectedControls()) {
      throw new IllegalStateException("Table model is read only or does not allow updates");
    }
    StateObserver selectionNotEmpty = tableModel.getSelectionModel().getSelectionNotEmptyObserver();
    StateObserver updateEnabled = tableModel.getEditModel().getUpdateEnabledObserver();
    StateObserver enabled = State.and(selectionNotEmpty, updateEnabled);
    Controls updateControls = Controls.builder()
            .caption(FrameworkMessages.get(FrameworkMessages.UPDATE))
            .enabledState(enabled)
            .smallIcon(frameworkIcons().edit())
            .description(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP))
            .build();
    Properties.sort(tableModel.getEntityDefinition().getUpdatableProperties()).forEach(property -> {
      if (!excludeFromUpdateMenu.contains(property.getAttribute())) {
        String caption = property.getCaption() == null ? property.getAttribute().getName() : property.getCaption();
        updateControls.add(Control.builder(() -> updateSelectedEntities(property))
                .caption(caption)
                .enabledState(enabled)
                .build());
      }
    });

    return updateControls;
  }

  /**
   * @return a control for showing the dependencies dialog
   */
  public final Control createViewDependenciesControl() {
    return Control.builder(this::viewSelectionDependencies)
            .caption(FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES) + "...")
            .enabledState(tableModel.getSelectionModel().getSelectionNotEmptyObserver())
            .description(FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES_TIP))
            .mnemonic('W')
            .smallIcon(frameworkIcons().dependencies())
            .build();
  }

  /**
   * @return a control for deleting the selected entities
   * @throws IllegalStateException in case the underlying model is read only or if deleting is not enabled
   */
  public final Control createDeleteSelectedControl() {
    if (!includeDeleteSelectedControl()) {
      throw new IllegalStateException("Table model is read only or does not allow delete");
    }
    return Control.builder(this::delete)
            .caption(FrameworkMessages.get(FrameworkMessages.DELETE))
            .enabledState(State.and(
                    tableModel.getEditModel().getDeleteEnabledObserver(),
                    tableModel.getSelectionModel().getSelectionNotEmptyObserver()))
            .description(FrameworkMessages.get(FrameworkMessages.DELETE_TIP))
            .smallIcon(frameworkIcons().delete())
            .build();
  }

  /**
   * @return a control for printing the table
   */
  public final Control createPrintTableControl() {
    String printCaption = MESSAGES.getString("print_table");
    return Control.builder(this::printTable)
            .caption(printCaption)
            .description(printCaption)
            .mnemonic(printCaption.charAt(0))
            .smallIcon(frameworkIcons().print())
            .build();
  }

  /**
   * @return a Control for refreshing the underlying table data
   */
  public final Control createRefreshControl() {
    String refreshCaption = FrameworkMessages.get(FrameworkMessages.REFRESH);
    return Control.builder(tableModel::refresh)
            .caption(refreshCaption)
            .description(FrameworkMessages.get(FrameworkMessages.REFRESH_TIP))
            .mnemonic(refreshCaption.charAt(0))
            .smallIcon(frameworkIcons().refresh())
            .enabledState(tableModel.getRefreshingObserver().getReversedObserver())
            .build();
  }

  /**
   * @return a Control for clearing the underlying table model, that is, removing all rows
   */
  public final Control createClearControl() {
    String clearCaption = FrameworkMessages.get(FrameworkMessages.CLEAR);
    return Control.builder(tableModel::clear)
            .caption(clearCaption)
            .mnemonic(clearCaption.charAt(0))
            .smallIcon(frameworkIcons().clear())
            .build();
  }

  /**
   * Retrieves a new property value via input dialog and performs an update on the selected entities
   * @param propertyToUpdate the property to update
   * @param <T> the property type
   * @see #setUpdateSelectedComponentFactory(Attribute, EntityComponentFactory)
   */
  public final <T> void updateSelectedEntities(Property<T> propertyToUpdate) {
    requireNonNull(propertyToUpdate);
    if (tableModel.getSelectionModel().isSelectionEmpty()) {
      return;
    }

    List<Entity> selectedEntities = Entity.deepCopy(tableModel.getSelectionModel().getSelectedItems());
    Collection<T> values = Entity.getDistinct(propertyToUpdate.getAttribute(), selectedEntities);
    T initialValue = values.size() == 1 ? values.iterator().next() : null;
    ComponentValue<T, ?> componentValue = createUpdateSelectedComponentValue(propertyToUpdate.getAttribute(), initialValue);
    boolean updatePerformed = false;
    while (!updatePerformed) {
      T newValue = Dialogs.showInputDialog(componentValue, this, propertyToUpdate.getCaption());
      Entity.put(propertyToUpdate.getAttribute(), newValue, selectedEntities);
      updatePerformed = update(selectedEntities);
    }
  }

  /**
   * Shows a dialog containing lists of entities depending on the selected entities via foreign key
   */
  public final void viewSelectionDependencies() {
    if (tableModel.getSelectionModel().isSelectionEmpty()) {
      return;
    }

    WaitCursor.show(this);
    try {
      Map<EntityType, Collection<Entity>> dependencies =
              tableModel.getConnectionProvider().getConnection()
                      .selectDependencies(tableModel.getSelectionModel().getSelectedItems());
      showDependenciesDialog(dependencies, tableModel.getConnectionProvider(), this,
              MESSAGES.getString("no_dependent_records"));
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      onException(e);
    }
    finally {
      WaitCursor.hide(this);
    }
  }

  /**
   * Deletes the entities selected in the underlying table model
   * @see #confirmDelete()
   */
  public final void delete() {
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
      onReferentialIntegrityException(e, tableModel.getSelectionModel().getSelectedItems());
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
   * Handles the given exception. If the referential error handling is {@link ReferentialIntegrityErrorHandling#DEPENDENCIES},
   * the dependencies of the given entity are displayed to the user, otherwise {@link #onException(Throwable)} is called.
   * @param exception the exception
   * @param entities the entities causing the exception
   * @see #setReferentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling)
   */
  public void onReferentialIntegrityException(ReferentialIntegrityException exception, List<Entity> entities) {
    WaitCursor.show(this);
    try {
      if (referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DEPENDENCIES) {
        Map<EntityType, Collection<Entity>> dependencies =
                tableModel.getConnectionProvider().getConnection().selectDependencies(entities);
        showDependenciesDialog(dependencies, tableModel.getConnectionProvider(), this,
                MESSAGES.getString("unknown_dependent_records"));
      }
      else {
        onException(exception);
      }
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      onException(e);
    }
    finally {
      WaitCursor.hide(this);
    }
  }

  /**
   * Handles the given exception, simply displays the error message to the user by default.
   * @param exception the exception to handle
   * @see #displayException(Throwable, Window)
   */
  public void onException(Throwable exception) {
    displayException(exception, getParentWindow(this).orElse(null));
  }

  @Override
  public final void displayException(Throwable throwable, Window dialogParent) {
    requireNonNull(throwable);
    DefaultDialogExceptionHandler.getInstance().displayException(throwable, dialogParent);
  }

  /**
   * Initializes the button used to toggle the condition panel state (hidden, visible and advanced)
   * @return a condition panel toggle button
   */
  public final Control createToggleConditionPanelControl() {
    if (conditionPanel == null) {
      return null;
    }

    return Control.builder(this::toggleConditionPanel)
            .smallIcon(frameworkIcons().filter())
            .description(MESSAGES.getString("show_condition_panel"))
            .build();
  }

  /**
   * Initializes the button used to toggle the summary panel state (hidden and visible)
   * @return a summary panel toggle button
   */
  public final Control createToggleSummaryPanelControl() {
    return ToggleControl.builder(summaryPanelVisibleState)
            .smallIcon(frameworkIcons().summary())
            .description(MESSAGES.getString("toggle_summary_tip"))
            .build();
  }

  /**
   * @return a control for clearing the table selection
   */
  public final Control createClearSelectionControl() {
    return Control.builder(tableModel.getSelectionModel()::clearSelection)
            .enabledState(tableModel.getSelectionModel().getSelectionNotEmptyObserver())
            .smallIcon(frameworkIcons().clearSelection())
            .description(MESSAGES.getString("clear_selection_tip"))
            .build();
  }

  /**
   * @return a control for moving the table selection down one index
   */
  public final Control createMoveSelectionDownControl() {
    return Control.builder(tableModel.getSelectionModel()::moveSelectionDown)
            .smallIcon(frameworkIcons().down())
            .description(MESSAGES.getString("selection_down_tip"))
            .build();
  }

  /**
   * @return a control for moving the table selection up one index
   */
  public final Control createMoveSelectionUpControl() {
    return Control.builder(tableModel.getSelectionModel()::moveSelectionUp)
            .smallIcon(frameworkIcons().up())
            .description(MESSAGES.getString("selection_up_tip"))
            .build();
  }

  /**
   * Shows a dialog containing the entities depending on the given entities.
   * @param entities the entities for which to display dependencies
   * @param connectionProvider the connection provider
   * @param dialogParent the dialog parent
   */
  public static void showDependenciesDialog(Collection<Entity> entities, EntityConnectionProvider connectionProvider,
                                            JComponent dialogParent) {
    showDependenciesDialog(entities, connectionProvider, dialogParent, MESSAGES.getString("no_dependent_records"));
  }

  /**
   * Shows a dialog containing the entities depending on the given entities.
   * @param entities the entities for which to display dependencies
   * @param connectionProvider the connection provider
   * @param dialogParent the dialog parent
   * @param noDependenciesMessage the message to show in case of no dependencies
   */
  public static void showDependenciesDialog(Collection<Entity> entities, EntityConnectionProvider connectionProvider,
                                            JComponent dialogParent, String noDependenciesMessage) {
    requireNonNull(entities);
    requireNonNull(connectionProvider);
    requireNonNull(dialogParent);
    try {
      Map<EntityType, Collection<Entity>> dependencies = connectionProvider.getConnection().selectDependencies(entities);
      showDependenciesDialog(dependencies, connectionProvider, dialogParent, noDependenciesMessage);
    }
    catch (DatabaseException e) {
      DefaultDialogExceptionHandler.getInstance().displayException(e, getParentWindow(dialogParent).orElse(null));
    }
  }

  /**
   * Creates a static read-only entity table panel showing the given entities
   * @param entities the entities to show in the panel
   * @param connectionProvider the EntityConnectionProvider, in case the returned panel should require one
   * @return a static EntityTablePanel showing the given entities
   */
  public static EntityTablePanel createReadOnlyEntityTablePanel(Collection<Entity> entities,
                                                                EntityConnectionProvider connectionProvider) {
    if (Util.nullOrEmpty(entities)) {
      throw new IllegalArgumentException("Cannot create a EntityTablePanel without the entities");
    }

    SwingEntityTableModel tableModel = new SwingEntityTableModel(entities.iterator().next().getEntityType(), connectionProvider) {
      @Override
      protected Collection<Entity> refreshItems() {
        return entities;
      }
    };
    tableModel.refresh();

    return createEntityTablePanel(tableModel);
  }

  /**
   * Creates a static entity table panel showing the given entities, note that this table panel will
   * provide a popup menu for updating and deleting the selected entities unless the underlying entities are read-only.
   * @param entities the entities to show in the panel
   * @param connectionProvider the EntityConnectionProvider, in case the returned panel should require one
   * @return a static EntityTablePanel showing the given entities
   */
  public static EntityTablePanel createEntityTablePanel(Collection<Entity> entities,
                                                        EntityConnectionProvider connectionProvider) {
    if (Util.nullOrEmpty(entities)) {
      throw new IllegalArgumentException("Cannot create a EntityTablePanel without the entities");
    }

    EntityType entityType = entities.iterator().next().getEntityType();
    SwingEntityTableModel tableModel = new SwingEntityTableModel(entityType, connectionProvider) {
      @Override
      protected Collection<Entity> refreshItems() {
        return entities;
      }
    };
    tableModel.refresh();

    return createEntityTablePanel(tableModel);
  }

  /**
   * Creates an entity table panel based on the given table model.
   * If the table model is not read only, a popup menu for updating or deleting the selected entities is provided.
   * @param tableModel the table model
   * @return an entity table panel based on the given model
   */
  public static EntityTablePanel createEntityTablePanel(SwingEntityTableModel tableModel) {
    EntityTablePanel tablePanel = new EntityTablePanel(tableModel) {
      @Override
      protected Controls getPopupControls(List<Controls> additionalPopupControls) {
        return additionalPopupControls.get(0);
      }
    };
    Controls popupControls = Controls.controls();
    if (tablePanel.includeUpdateSelectedControls()) {
      popupControls.add(tablePanel.createUpdateSelectedControls());
    }
    if (tablePanel.includeDeleteSelectedControl()) {
      popupControls.add(tablePanel.createDeleteSelectedControl());
    }
    if (!popupControls.isEmpty()) {
      popupControls.addSeparator();
    }
    popupControls.add(tablePanel.createViewDependenciesControl());
    tablePanel.addPopupControls(popupControls);
    tablePanel.setIncludeConditionPanel(false);
    tablePanel.setIncludeSouthPanel(false);
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
        initializeKeyboardActions();
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
            .add(table.getSearchField(), createSearchFieldConstraints())
            .build();
    southPanelSplitPane = Components.splitPane()
            .continuousLayout(true)
            .resizeWeight(0.35)
            .leftComponent(searchFieldPanel)
            .rightComponent(statusMessageLabel)
            .build();
    southPanel.add(southPanelSplitPane, BorderLayout.CENTER);
    southPanel.add(refreshToolBar, BorderLayout.WEST);
    southToolBar = initializeSouthToolBar();
    if (southToolBar != null) {
      southPanel.add(southToolBar, BorderLayout.EAST);
    }

    return southPanel;
  }

  /**
   * Initializes the default keyboard actions.
   * CTRL-T transfers focus to the table in case one is available,
   * CTR-S opens a select search condition panel dialog, in case one is available,
   * CTR-F selects the table search field
   */
  protected void initializeKeyboardActions() {
    KeyEvents.builder(KeyEvent.VK_T)
            .modifiers(CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(getControl(ControlCode.REQUEST_TABLE_FOCUS))
            .enable(this);
    KeyEvents.builder(KeyEvent.VK_F)
            .modifiers(CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(getControl(ControlCode.REQUEST_SEARCH_FIELD_FOCUS))
            .enable(this);
    if (getConditionPanel() != null) {
      KeyEvents.builder(KeyEvent.VK_S)
              .modifiers(CTRL_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(getControl(ControlCode.SELECT_CONDITION_PANEL))
              .enable(this);
    }
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

  protected Controls getToolBarControls(List<Controls> additionalToolBarControls) {
    requireNonNull(additionalToolBarControls);
    Controls toolbarControls = Controls.controls();
    if (controls.containsKey(ControlCode.TOGGLE_SUMMARY_PANEL)) {
      toolbarControls.add(controls.get(ControlCode.TOGGLE_SUMMARY_PANEL));
    }
    if (controls.containsKey(ControlCode.TOGGLE_CONDITION_PANEL)) {
      toolbarControls.add(controls.get(ControlCode.TOGGLE_CONDITION_PANEL));
      toolbarControls.addSeparator();
    }
    if (controls.containsKey(ControlCode.DELETE_SELECTED)) {
      toolbarControls.add(controls.get(ControlCode.DELETE_SELECTED));
    }
    if (controls.containsKey(ControlCode.PRINT_TABLE)) {
      toolbarControls.add(controls.get(ControlCode.PRINT_TABLE));
    }
    toolbarControls.add(controls.get(ControlCode.CLEAR_SELECTION));
    toolbarControls.addSeparator();
    toolbarControls.add(controls.get(ControlCode.MOVE_SELECTION_UP));
    toolbarControls.add(controls.get(ControlCode.MOVE_SELECTION_DOWN));
    additionalToolBarControls.forEach(additionalControls -> {
      toolbarControls.addSeparator();
      additionalControls.getActions().forEach(toolbarControls::add);
    });

    return toolbarControls;
  }

  /**
   * Constructs a Controls instance containing the controls to include in the table popup menu.
   * Returns null or an empty Controls instance to indicate that no popup menu should be included.
   * @param additionalPopupControls any additional controls to include in the popup menu
   * @return Controls on which to base the table popup menu, null or an empty Controls instance
   * if no popup menu should be included
   */
  protected Controls getPopupControls(List<Controls> additionalPopupControls) {
    requireNonNull(additionalPopupControls);
    Controls popupControls = Controls.controls();
    popupControls.add(controls.get(ControlCode.REFRESH));
    if (controls.containsKey(ControlCode.CLEAR)) {
      popupControls.add(controls.get(ControlCode.CLEAR));
    }
    popupControls.addSeparator();
    addAdditionalControls(popupControls, additionalPopupControls);
    boolean separatorRequired = false;
    if (controls.containsKey(ControlCode.UPDATE_SELECTED)) {
      popupControls.add(controls.get(ControlCode.UPDATE_SELECTED));
      separatorRequired = true;
    }
    if (controls.containsKey(ControlCode.DELETE_SELECTED)) {
      popupControls.add(controls.get(ControlCode.DELETE_SELECTED));
      separatorRequired = true;
    }
    if (controls.containsKey(ControlCode.EXPORT_JSON)) {
      popupControls.add(controls.get(ControlCode.EXPORT_JSON));
      separatorRequired = true;
    }
    if (separatorRequired) {
      popupControls.addSeparator();
      separatorRequired = false;
    }
    if (controls.containsKey(ControlCode.VIEW_DEPENDENCIES)) {
      popupControls.add(controls.get(ControlCode.VIEW_DEPENDENCIES));
      separatorRequired = true;
    }
    if (separatorRequired) {
      popupControls.addSeparator();
      separatorRequired = false;
    }
    Controls printControls = createPrintControls();
    if (printControls != null && !printControls.isEmpty()) {
      popupControls.add(printControls);
      separatorRequired = true;
    }
    Controls columnControls = createColumnControls();
    if (!columnControls.isEmpty()) {
      if (separatorRequired) {
        popupControls.addSeparator();
      }
      popupControls.add(columnControls);
      separatorRequired = true;
    }
    if (controls.containsKey(ControlCode.SELECTION_MODE)) {
      if (separatorRequired) {
        popupControls.addSeparator();
      }
      popupControls.add(controls.get(ControlCode.SELECTION_MODE));
      separatorRequired = true;
    }
    if (includeConditionPanel && conditionPanel != null) {
      if (separatorRequired) {
        popupControls.addSeparator();
      }
      addConditionControls(popupControls);
      separatorRequired = true;
    }
    if (separatorRequired) {
      popupControls.addSeparator();
    }
    popupControls.add(controls.get(ControlCode.COPY_TABLE_DATA));

    return popupControls;
  }

  protected Controls createPrintControls() {
    String printCaption = Messages.get(Messages.PRINT);
    Controls.Builder builder = Controls.builder()
            .caption(printCaption)
            .mnemonic(printCaption.charAt(0))
            .smallIcon(frameworkIcons().print());
    if (controls.containsKey(ControlCode.PRINT_TABLE)) {
      builder.control(controls.get(ControlCode.PRINT_TABLE));
    }

    return builder.build();
  }

  protected final Controls createColumnControls() {
    Controls.Builder builder = Controls.builder()
            .caption(MESSAGES.getString("columns"));
    if (controls.containsKey(ControlCode.SELECT_COLUMNS)) {
      builder.control(controls.get(ControlCode.SELECT_COLUMNS));
    }
    if (controls.containsKey(ControlCode.RESET_COLUMNS)) {
      builder.control(controls.get(ControlCode.RESET_COLUMNS));
    }

    return builder.build();
  }

  protected final Control createConditionPanelControl() {
    return ToggleControl.builder(conditionPanelVisibleState)
            .caption(FrameworkMessages.get(FrameworkMessages.SHOW))
            .build();
  }

  protected final Controls createCopyControls() {
    return Controls.builder()
            .caption(Messages.get(Messages.COPY))
            .smallIcon(frameworkIcons().copy())
            .controls(createCopyCellControl(), createCopyTableWithHeaderControl())
            .build();
  }

  protected final Control createCopyCellControl() {
    return Control.builder(table::copySelectedCell)
            .caption(FrameworkMessages.get(FrameworkMessages.COPY_CELL))
            .enabledState(tableModel.getSelectionModel().getSelectionNotEmptyObserver())
            .build();
  }

  protected final Control createCopyTableWithHeaderControl() {
    return Control.builder(this::copyTableAsDelimitedString)
            .caption(FrameworkMessages.get(FrameworkMessages.COPY_TABLE_WITH_HEADER))
            .build();
  }

  /**
   * Called before delete is performed, if true is returned the delete action is performed otherwise it is canceled
   * @return true if the delete action should be performed
   */
  protected boolean confirmDelete() {
    String[] messages = getConfirmDeleteMessages();
    int res = JOptionPane.showConfirmDialog(this, messages[0], messages[1], JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  /**
   * @return Strings to display in the confirm delete dialog, index 0 = message, index 1 = title
   */
  protected String[] getConfirmDeleteMessages() {
    return new String[] {FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_SELECTED),
            FrameworkMessages.get(FrameworkMessages.DELETE)};
  }

  /**
   * Returns the TableCellRenderer used for the given property in this EntityTablePanel
   * @param property the property
   * @return the TableCellRenderer for the given property
   */
  protected TableCellRenderer initializeTableCellRenderer(Property<?> property) {
    return EntityTableCellRenderer.builder(tableModel, property).build();
  }

  /**
   * Creates a TableCellEditor for the given property, returns null if no editor is available
   * @param <T> the property type
   * @param property the property
   * @return a TableCellEditor for the given property
   */
  protected <T> TableCellEditor initializeTableCellEditor(Property<T> property) {
    if (property instanceof ColumnProperty && !((ColumnProperty<T>) property).isUpdatable()) {
      return null;
    }
    //TODO handle Enter key correctly for foreign key input fields
    return new EntityTableCellEditor<>(() -> createCellEditorComponentValue(property.getAttribute(), null));
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
   * Initializes the south panel toolbar, by default based on {@link #getToolBarControls(List)}
   * @return the toolbar to add to the south panel
   */
  protected JToolBar initializeSouthToolBar() {
    Controls toolbarControls = getToolBarControls(additionalToolBarControls);
    if (toolbarControls != null) {
      JToolBar southToolBar = toolbarControls.createHorizontalToolBar();
      Arrays.stream(southToolBar.getComponents())
              .map(JComponent.class::cast)
              .forEach(component -> component.setToolTipText(null));
      southToolBar.setFocusable(false);
      southToolBar.setFloatable(false);
      southToolBar.setRollover(true);

      return southToolBar;
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
    if (summaryScrollPane != null) {
      controls.putIfAbsent(ControlCode.TOGGLE_SUMMARY_PANEL, createToggleSummaryPanelControl());
    }
    if (includeConditionPanel && conditionPanel != null) {
      controls.putIfAbsent(ControlCode.CONDITION_PANEL_VISIBLE, createConditionPanelControl());
      controls.putIfAbsent(ControlCode.TOGGLE_CONDITION_PANEL, createToggleConditionPanelControl());
    }
    controls.putIfAbsent(ControlCode.PRINT_TABLE, createPrintTableControl());
    controls.putIfAbsent(ControlCode.CLEAR_SELECTION, createClearSelectionControl());
    controls.putIfAbsent(ControlCode.MOVE_SELECTION_UP, createMoveSelectionDownControl());
    controls.putIfAbsent(ControlCode.MOVE_SELECTION_DOWN, createMoveSelectionUpControl());
    controls.putIfAbsent(ControlCode.COPY_TABLE_DATA, createCopyControls());
    if (includeSelectionModeControl) {
      controls.putIfAbsent(ControlCode.SELECTION_MODE, table.createSingleSelectionModeControl());
    }
    controls.put(ControlCode.REQUEST_TABLE_FOCUS, Control.control(getTable()::requestFocus));
    controls.put(ControlCode.REQUEST_SEARCH_FIELD_FOCUS, Control.control(getTable().getSearchField()::requestFocus));
    controls.put(ControlCode.SELECT_CONDITION_PANEL, Control.control(this::selectConditionPanel));
  }

  private void copyTableAsDelimitedString() {
    Utilities.setClipboard(tableModel.getTableDataAsDelimitedString('\t'));
  }

  private boolean includeUpdateSelectedControls() {
    return !tableModel.isReadOnly() && tableModel.isUpdateEnabled() &&
            tableModel.isBatchUpdateEnabled() &&
            !tableModel.getEntityDefinition().getUpdatableProperties().isEmpty();
  }

  private boolean includeDeleteSelectedControl() {
    return !tableModel.isReadOnly() && tableModel.isDeleteEnabled();
  }

  private FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> initializeFilteredTable() {
    FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> filteredTable =
            new FilteredTable<>(tableModel, new DefaultFilterPanelFactory(tableModel));
    filteredTable.setAutoResizeMode(TABLE_AUTO_RESIZE_MODE.get());
    filteredTable.getTableHeader().setReorderingAllowed(ALLOW_COLUMN_REORDERING.get());
    filteredTable.setRowHeight(filteredTable.getFont().getSize() + FONT_SIZE_TO_ROW_HEIGHT);
    filteredTable.setAutoStartsEdit(false);

    return filteredTable;
  }

  private <T> ComponentValue<T, ? extends JComponent> createUpdateSelectedComponentValue(Attribute<T> attribute, T initialValue) {
    return ((EntityComponentFactory<T, Attribute<T>, ?>) updateSelectedComponentFactories.computeIfAbsent(attribute, a ->
            new DefaultEntityComponentFactory<T, Attribute<T>, JComponent>())).createComponentValue(attribute, tableModel.getEditModel(), initialValue);
  }

  private <T> ComponentValue<T, ? extends JComponent> createCellEditorComponentValue(Attribute<T> attribute, T initialValue) {
    return ((EntityComponentFactory<T, Attribute<T>, ?>) tableCellEditorComponentFactories.computeIfAbsent(attribute, a ->
            new DefaultEntityComponentFactory<T, Attribute<T>, JComponent>())).createComponentValue(attribute, tableModel.getEditModel(), initialValue);
  }

  /**
   * @return the refresh toolbar
   */
  private JToolBar initializeRefreshToolBar() {
    Control refreshControl = Control.builder(tableModel::refresh)
            .enabledState(tableModel.getTableConditionModel().getConditionChangedObserver())
            .smallIcon(frameworkIcons().refreshRequired())
            .build();

    KeyEvents.builder(KeyEvent.VK_F5)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(refreshControl)
            .enable(this);

    JToolBar toolBar = Controls.controls(refreshControl).createHorizontalToolBar();
    toolBar.setFocusable(false);
    toolBar.getComponentAtIndex(0).setFocusable(false);
    toolBar.setFloatable(false);
    toolBar.setRollover(true);
    //made visible when condition panel is visible
    toolBar.setVisible(false);

    return toolBar;
  }

  private JScrollPane initializeConditionScrollPane(JScrollPane tableScrollPane) {
    return conditionPanel == null ? null : createHiddenLinkedScrollPane(tableScrollPane, conditionPanel);
  }

  private TableColumnComponentPanel<JPanel> initializeSummaryPanel() {
    Map<TableColumn, JPanel> columnSummaryPanels = createColumnSummaryPanels(tableModel);
    if (columnSummaryPanels.isEmpty()) {
      return null;
    }

    return new TableColumnComponentPanel<>(tableModel.getColumnModel(), columnSummaryPanels);
  }

  private JScrollPane initializeSummaryScrollPane(JScrollPane tableScrollPane) {
    if (summaryPanel == null) {
      return null;
    }

    return createHiddenLinkedScrollPane(tableScrollPane, summaryPanel);
  }

  private JPanel initializeTablePanel(JScrollPane tableScrollPane) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(tableScrollPane, BorderLayout.CENTER);
    if (conditionScrollPane != null) {
      panel.add(conditionScrollPane, BorderLayout.NORTH);
    }
    if (summaryScrollPane != null) {
      panel.add(summaryScrollPane, BorderLayout.SOUTH);
    }

    return panel;
  }

  private void bindEvents() {
    KeyEvents.builder(KeyEvent.VK_C)
            .action(Control.control(table::copySelectedCell))
            .modifiers(InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK)
            .enable(table);
    if (includeDeleteSelectedControl()) {
      KeyEvents.builder(KeyEvent.VK_DELETE)
              .action(createDeleteSelectedControl())
              .enable(table);
    }
    if (INCLUDE_ENTITY_MENU.get()) {
      KeyEvents.builder(KeyEvent.VK_V)
              .modifiers(InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK)
              .action(control(this::showEntityMenu))
              .enable(table);
    }
    conditionPanelVisibleState.addDataListener(this::setConditionPanelVisibleInternal);
    summaryPanelVisibleState.addDataListener(this::setSummaryPanelVisibleInternal);
    tableModel.getTableConditionModel().getConditionModels().values().forEach(conditionModel ->
            conditionModel.addConditionChangedListener(this::onConditionChanged));
    tableModel.getRefreshingObserver().addDataListener(this::onRefreshingChanged);
    tableModel.addRefreshFailedListener(this::onException);
    tableModel.getEditModel().addEntitiesEditedListener(table::repaint);
    if (conditionPanel != null) {
      Control refreshControl = Control.builder(tableModel::refresh)
              .enabledState(tableModel.getTableConditionModel().getConditionChangedObserver())
              .build();
      KeyEvents.builder(KeyEvent.VK_ENTER)
              .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(refreshControl)
              .enable(conditionPanel);
      conditionPanel.addFocusGainedListener(table::scrollToColumn);
      if (conditionPanel instanceof EntityTableConditionPanel) {
        addRefreshOnEnterControl((EntityTableConditionPanel) conditionPanel, refreshControl);
      }
      if (conditionPanel.hasAdvancedView()) {
        conditionPanel.addAdvancedListener(advanced -> {
          if (isConditionPanelVisible()) {
            revalidate();
          }
        });
      }
    }
  }

  private void setConditionPanelVisibleInternal(boolean visible) {
    if (conditionScrollPane != null) {
      conditionScrollPane.setVisible(visible);
      refreshToolBar.setVisible(refreshToolbarAlwaysVisible || visible);
      revalidate();
    }
  }

  private void setSummaryPanelVisibleInternal(boolean visible) {
    if (summaryScrollPane != null) {
      summaryScrollPane.setVisible(visible);
      revalidate();
    }
  }

  private void initializeTable() {
    tableModel.getColumnModel().getAllColumns().forEach(this::configureColumn);
    JTableHeader header = table.getTableHeader();
    header.setFocusable(false);
    if (includePopupMenu) {
      addTablePopupMenu();
    }
  }

  private void configureColumn(TableColumn column) {
    Property<?> property = tableModel.getEntityDefinition().getProperty((Attribute<?>) column.getIdentifier());
    column.setCellRenderer(initializeTableCellRenderer(property));
    column.setCellEditor(initializeTableCellEditor(property));
    column.setResizable(true);
    column.setHeaderRenderer(new HeaderRenderer(column.getHeaderRenderer()));
  }

  private void addTablePopupMenu() {
    Controls popupControls = getPopupControls(additionalPopupControls);
    if (popupControls == null || popupControls.isEmpty()) {
      return;
    }

    JPopupMenu popupMenu = popupControls.createPopupMenu();
    table.setComponentPopupMenu(popupMenu);
    if (table.getParent() != null) {
      ((JComponent) table.getParent()).setComponentPopupMenu(popupMenu);
    }
    KeyEvents.builder(KeyEvent.VK_G)
            .modifiers(InputEvent.CTRL_DOWN_MASK)
            .action(control(() -> {
              Point location = getPopupLocation(table);
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
            .caption(FrameworkMessages.get(FrameworkMessages.SEARCH))
            .smallIcon(frameworkIcons().filter())
            .build();
    if (this.controls.containsKey(ControlCode.CONDITION_PANEL_VISIBLE)) {
      conditionControls.add(getControl(ControlCode.CONDITION_PANEL_VISIBLE));
    }
    Controls searchPanelControls = conditionPanel.getControls();
    if (!searchPanelControls.isEmpty()) {
      conditionControls.addAll(searchPanelControls);
      conditionControls.addSeparator();
    }
    conditionControls.add(ToggleControl.builder(tableModel.getQueryConditionRequiredState())
            .caption(MESSAGES.getString("require_query_condition"))
            .description(MESSAGES.getString("require_query_condition_description"))
            .build());
    if (!conditionControls.isEmpty()) {
      popupControls.add(conditionControls);
    }
  }

  private void showEntityMenu() {
    Entity selected = tableModel.getSelectionModel().getSelectedItem();
    if (selected != null) {
      Point location = getPopupLocation(table);
      new EntityPopupMenu(selected.copy(), tableModel.getConnectionProvider().getConnection()).show(table, location.x, location.y);
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
      JOptionPane.showMessageDialog(this, e.getMessage(),
              Messages.get(Messages.ERROR), JOptionPane.ERROR_MESSAGE);
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

  private static void enableRefreshOnEnterControl(JComponent component, Control refreshControl) {
    if (component instanceof JComboBox) {
      new RefreshOnEnterAction((JComboBox<?>) component, refreshControl);
    }
    else if (component instanceof TemporalField) {
      ((TemporalField<?>) component).addActionListener(refreshControl);
    }
  }

  private static GridBagConstraints createSearchFieldConstraints() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    constraints.insets = new Insets(0, Layouts.HORIZONTAL_VERTICAL_GAP.get(), 0, Layouts.HORIZONTAL_VERTICAL_GAP.get());

    return constraints;
  }

  private static void addRefreshOnEnterControl(EntityTableConditionPanel tableConditionPanel, Control refreshControl) {
    tableConditionPanel.getTableColumns().forEach(column -> {
      ColumnConditionPanel<? extends Attribute<?>, ?> columnConditionPanel = tableConditionPanel.getConditionPanel((Attribute<?>) column.getIdentifier());
      if (columnConditionPanel != null) {
        enableRefreshOnEnterControl(columnConditionPanel.getOperatorComboBox(), refreshControl);
        enableRefreshOnEnterControl(columnConditionPanel.getEqualField(), refreshControl);
        enableRefreshOnEnterControl(columnConditionPanel.getLowerBoundField(), refreshControl);
        enableRefreshOnEnterControl(columnConditionPanel.getUpperBoundField(), refreshControl);
      }
    });
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

  private static void showDependenciesDialog(Map<EntityType, Collection<Entity>> dependencies,
                                             EntityConnectionProvider connectionProvider,
                                             JComponent dialogParent, String noDependenciesMessage) {
    if (dependencies.isEmpty()) {
      JOptionPane.showMessageDialog(dialogParent, noDependenciesMessage,
              MESSAGES.getString("none_found"), JOptionPane.INFORMATION_MESSAGE);
    }
    else {
      Dialogs.componentDialog(createDependenciesPanel(dependencies, connectionProvider))
              .owner(dialogParent)
              .title(MESSAGES.getString("dependent_records_found"))
              .show();
    }
  }

  private static Map<TableColumn, JPanel> createColumnSummaryPanels(FilteredTableModel<?, Attribute<?>> tableModel) {
    Map<TableColumn, JPanel> components = new HashMap<>();
    tableModel.getColumnModel().getAllColumns().forEach(column ->
            tableModel.getColumnSummaryModel((Attribute<?>) column.getIdentifier())
                    .ifPresent(columnSummaryModel ->
                            components.put(column, new ColumnSummaryPanel(columnSummaryModel))));

    return components;
  }

  private static JScrollPane createHiddenLinkedScrollPane(JScrollPane masterScrollPane, JPanel panel) {
    JScrollPane scrollPane = new JScrollPane(panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    Utilities.linkBoundedRangeModels(masterScrollPane.getHorizontalScrollBar().getModel(), scrollPane.getHorizontalScrollBar().getModel());
    scrollPane.setVisible(false);

    return scrollPane;
  }

  private static JPanel createDependenciesPanel(Map<EntityType, Collection<Entity>> dependencies,
                                                EntityConnectionProvider connectionProvider) {
    JPanel panel = new JPanel(new BorderLayout());
    JTabbedPane tabPane = new JTabbedPane(SwingConstants.TOP);
    for (Map.Entry<EntityType, Collection<Entity>> entry : dependencies.entrySet()) {
      Collection<Entity> dependentEntities = entry.getValue();
      if (!dependentEntities.isEmpty()) {
        tabPane.addTab(connectionProvider.getEntities().getDefinition(entry.getKey()).getCaption(),
                createEntityTablePanel(dependentEntities, connectionProvider));
      }
    }
    panel.add(tabPane, BorderLayout.CENTER);

    return panel;
  }

  private static Point getPopupLocation(JTable table) {
    Rectangle visibleRect = table.getVisibleRect();
    int x = visibleRect.x + visibleRect.width / 2;
    int y = table.getSelectionModel().isSelectionEmpty() ?
            visibleRect.y + visibleRect.height / 2 :
            table.getCellRect(table.getSelectedRow(), table.getSelectedColumn(), true).y;

    return new Point(x, y + table.getRowHeight() / 2);
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
      TableColumn tableColumn = tableModel.getColumnModel().getColumn(column);
      TableCellRenderer renderer = tableColumn.getCellRenderer();
      Attribute<?> attribute = (Attribute<?>) tableColumn.getIdentifier();
      boolean displayConditionState = renderer instanceof EntityTableCellRenderer
              && ((EntityTableCellRenderer) renderer).isDisplayConditionState()
              && tableModel.getTableConditionModel().isConditionEnabled(attribute);
      Font defaultFont = component.getFont();
      component.setFont(displayConditionState ? defaultFont.deriveFont(defaultFont.getStyle() | Font.BOLD) : defaultFont);

      return component;
    }
  }

  private static final class DefaultFilterPanelFactory implements ConditionPanelFactory {

    private final SwingEntityTableModel tableModel;

    private DefaultFilterPanelFactory(SwingEntityTableModel tableModel) {
      this.tableModel = requireNonNull(tableModel);
    }

    @Override
    public ColumnConditionPanel<?, ?> createConditionPanel(TableColumn column) {
      ColumnFilterModel<Entity, Attribute<?>, ?> filterModel =
              tableModel.getTableConditionModel().getFilterModels().get(column.getIdentifier());
      if (filterModel == null) {
        return null;
      }

      return new ColumnConditionPanel<>(filterModel, ToggleAdvancedButton.YES);
    }
  }

  private static final class RefreshOnEnterAction extends AbstractAction {

    private static final String ENTER_PRESSED = "enterPressed";

    private final JComboBox<?> comboBox;
    private final Control refreshControl;
    private final Action enterPressedAction;

    private RefreshOnEnterAction(JComboBox<?> comboBox, Control refreshControl) {
      this.comboBox = comboBox;
      this.refreshControl = refreshControl;
      this.enterPressedAction = comboBox.getActionMap().get(ENTER_PRESSED);
      this.comboBox.getActionMap().put(ENTER_PRESSED, this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (comboBox.isPopupVisible()) {
        enterPressedAction.actionPerformed(e);
      }
      else if (refreshControl.isEnabled()) {
        refreshControl.actionPerformed(e);
      }
    }
  }
}
