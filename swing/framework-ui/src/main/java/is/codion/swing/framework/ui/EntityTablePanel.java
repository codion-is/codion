/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.swing.common.ui.component.table.ConditionPanelFactory;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.component.table.TableColumnComponentPanel;
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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
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
import static is.codion.swing.common.ui.Utilities.getParentWindow;
import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.columnConditionPanel;
import static is.codion.swing.common.ui.component.table.ColumnSummaryPanel.columnSummaryPanel;
import static is.codion.swing.common.ui.component.table.TableColumnComponentPanel.tableColumnComponentPanel;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.framework.ui.EntityTableConditionPanel.entityTableConditionPanel;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.util.Objects.requireNonNull;

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
 */
public class EntityTablePanel extends JPanel {

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
    SELECT_CONDITION_PANEL,
    CONFIGURE_COLUMNS
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
   * displays a status message or a refresh progress bar when refreshing
   */
  private final JPanel statusPanel;

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
   * specifies whether to display an indeterminate progress bar while the model is refreshing
   */
  private boolean showRefreshingProgressBar = false;

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
    this(tableModel, entityTableConditionPanel(tableModel.tableConditionModel(), tableModel.columnModel()));
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param conditionPanel the condition panel, if any
   */
  public EntityTablePanel(SwingEntityTableModel tableModel, AbstractEntityTableConditionPanel conditionPanel) {
    this.tableModel = requireNonNull(tableModel, "tableModel");
    this.table = createTable();
    this.conditionPanel = conditionPanel;
    this.tableScrollPane = new JScrollPane(table);
    this.conditionScrollPane = createConditionScrollPane(tableScrollPane);
    this.summaryPanel = createSummaryPanel();
    this.summaryScrollPane = createSummaryScrollPane(tableScrollPane);
    this.tablePanel = createTablePanel(tableScrollPane);
    this.refreshToolBar = createRefreshToolBar();
    this.statusPanel = createStatusPanel();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(tablePanel, table, statusPanel, conditionPanel, conditionScrollPane,
            summaryScrollPane, summaryPanel, southPanel, refreshToolBar, southToolBar, southPanelSplitPane, searchFieldPanel);
    if (tableScrollPane != null) {
      Utilities.updateUI(tableScrollPane, tableScrollPane.getViewport(),
              tableScrollPane.getHorizontalScrollBar(), tableScrollPane.getVerticalScrollBar());
    }
  }

  /**
   * @return the filtered table instance
   */
  public final FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> table() {
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
   * @return true if a progress bar is shown while the model is refreshing
   */
  public final boolean isShowRefreshingProgressBar() {
    return showRefreshingProgressBar;
  }

  /**
   * @param showRefreshingProgressBar true if an indeterminate progress bar should be shown while the model is refreshing
   */
  public final void setShowRefreshingProgressBar(boolean showRefreshingProgressBar) {
    this.showRefreshingProgressBar = showRefreshingProgressBar;
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
   * @return the condition panel being used by this EntityTablePanel
   */
  public final AbstractEntityTableConditionPanel conditionPanel() {
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
      State advancedState = conditionPanel.advancedState();
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
    return getClass().getSimpleName() + ": " + tableModel.entityType();
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
   * and {@link EntityEditModel#updateEnabledObserver()} is enabled.
   * @return controls containing a control for each updatable property in the
   * underlying entity, for performing an update on the selected entities
   * @throws IllegalStateException in case the underlying edit model is read only or updating is not enabled
   * @see #excludeFromUpdateMenu(Attribute)
   * @see EntityEditModel#updateEnabledObserver()
   */
  public final Controls createUpdateSelectedControls() {
    if (!includeUpdateSelectedControls()) {
      throw new IllegalStateException("Table model is read only or does not allow updates");
    }
    StateObserver selectionNotEmpty = tableModel.selectionModel().selectionNotEmptyObserver();
    StateObserver updateEnabled = tableModel.editModel().updateEnabledObserver();
    StateObserver enabled = State.and(selectionNotEmpty, updateEnabled);
    Controls updateControls = Controls.builder()
            .caption(FrameworkMessages.update())
            .enabledState(enabled)
            .smallIcon(FrameworkIcons.instance().edit())
            .description(FrameworkMessages.updateSelectedTip())
            .build();
    Properties.sort(tableModel.entityDefinition().updatableProperties()).forEach(property -> {
      if (!excludeFromUpdateMenu.contains(property.attribute())) {
        String caption = property.caption() == null ? property.attribute().name() : property.caption();
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
            .caption(FrameworkMessages.viewDependencies() + "...")
            .enabledState(tableModel.selectionModel().selectionNotEmptyObserver())
            .description(FrameworkMessages.viewDependenciesTip())
            .mnemonic('W')
            .smallIcon(FrameworkIcons.instance().dependencies())
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
  public final Control createPrintTableControl() {
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
  public final Control createRefreshControl() {
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
  public final Control createClearControl() {
    return Control.builder(tableModel::clear)
            .caption(FrameworkMessages.clear())
            .description(FrameworkMessages.clearTip())
            .mnemonic(FrameworkMessages.clearMnemonic())
            .smallIcon(FrameworkIcons.instance().clear())
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
    if (tableModel.selectionModel().isSelectionEmpty()) {
      return;
    }

    List<Entity> selectedEntities = Entity.deepCopy(tableModel.selectionModel().getSelectedItems());
    Collection<T> values = Entity.getDistinct(propertyToUpdate.attribute(), selectedEntities);
    T initialValue = values.size() == 1 ? values.iterator().next() : null;
    ComponentValue<T, ?> componentValue = createUpdateSelectedComponentValue(propertyToUpdate.attribute(), initialValue);
    boolean updatePerformed = false;
    while (!updatePerformed) {
      T newValue = Dialogs.showInputDialog(componentValue, this, propertyToUpdate.caption());
      Entity.put(propertyToUpdate.attribute(), newValue, selectedEntities);
      updatePerformed = update(selectedEntities);
    }
  }

  /**
   * Shows a dialog containing lists of entities depending on the selected entities via foreign key
   */
  public final void viewSelectionDependencies() {
    if (tableModel.selectionModel().isSelectionEmpty()) {
      return;
    }

    WaitCursor.show(this);
    try {
      Map<EntityType, Collection<Entity>> dependencies =
              tableModel.connectionProvider().connection()
                      .selectDependencies(tableModel.selectionModel().getSelectedItems());
      showDependenciesDialog(dependencies, tableModel.connectionProvider(), this,
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
    WaitCursor.show(this);
    try {
      if (referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES) {
        Map<EntityType, Collection<Entity>> dependencies =
                tableModel.connectionProvider().connection().selectDependencies(entities);
        showDependenciesDialog(dependencies, tableModel.connectionProvider(), this,
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
   * @see #displayException(Throwable)
   */
  public void onException(Throwable exception) {
    displayException(exception);
  }

  /**
   * Displays the exception in a dialog
   * @param exception the exception to display
   */
  public final void displayException(Throwable exception) {
    Dialogs.showExceptionDialog(exception, getParentWindow(this));
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
      Map<EntityType, Collection<Entity>> dependencies = connectionProvider.connection().selectDependencies(entities);
      showDependenciesDialog(dependencies, connectionProvider, dialogParent, noDependenciesMessage);
    }
    catch (DatabaseException e) {
      Dialogs.showExceptionDialog(e, getParentWindow(dialogParent));
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

    SwingEntityEditModel editModel = new SwingEntityEditModel(entities.iterator().next().type(), connectionProvider);
    editModel.setReadOnly(true);
    SwingEntityTableModel tableModel = new SwingEntityTableModel(editModel) {
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

    EntityType entityType = entities.iterator().next().type();
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
      protected Controls createPopupControls(List<Controls> additionalPopupControls) {
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
    southPanel.add(refreshToolBar, BorderLayout.WEST);
    southToolBar = createSouthToolBar();
    if (southToolBar != null) {
      southPanel.add(southToolBar, BorderLayout.EAST);
    }

    return southPanel;
  }

  /**
   * Sets up the default keyboard actions.
   * CTRL-T transfers focus to the table in case one is available,
   * CTR-S opens a select search condition panel dialog, in case one is available,
   * CTR-F selects the table search field
   */
  protected void setupKeyboardActions() {
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
    if (conditionPanel() != null) {
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

  protected Controls createToolBarControls(List<Controls> additionalToolBarControls) {
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
   * Creates a Controls instance containing the controls to include in the table popup menu.
   * Returns null or an empty Controls instance to indicate that no popup menu should be included.
   * @param additionalPopupControls any additional controls to include in the popup menu
   * @return Controls on which to base the table popup menu, null or an empty Controls instance
   * if no popup menu should be included
   */
  protected Controls createPopupControls(List<Controls> additionalPopupControls) {
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
    Controls.Builder builder = Controls.builder()
            .caption(Messages.print())
            .mnemonic(Messages.printMnemonic())
            .smallIcon(FrameworkIcons.instance().print());
    if (controls.containsKey(ControlCode.PRINT_TABLE)) {
      builder.control(controls.get(ControlCode.PRINT_TABLE));
    }

    return builder.build();
  }

  /**
   * Called before delete is performed, if true is returned the delete action is performed otherwise it is canceled
   * @return true if the delete action should be performed
   */
  protected boolean confirmDelete() {
    String[] messages = confirmDeleteMessages();
    int res = JOptionPane.showConfirmDialog(this, messages[0], messages[1], JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  /**
   * @return Strings to display in the confirm delete dialog, index 0 = message, index 1 = title
   */
  protected String[] confirmDeleteMessages() {
    return new String[] {FrameworkMessages.confirmDeleteSelected(), FrameworkMessages.delete()};
  }

  /**
   * Creates a TableCellRenderer to use for the given property in this EntityTablePanel
   * @param <T> the property type
   * @param property the property
   * @return the TableCellRenderer for the given property
   */
  protected <T> TableCellRenderer createTableCellRenderer(Property<T> property) {
    return EntityTableCellRenderer.builder(tableModel, property).build();
  }

  /**
   * Creates a TableCellEditor for the given property, returns null if no editor is available
   * @param <T> the property type
   * @param property the property
   * @return a TableCellEditor for the given property
   */
  protected <T> TableCellEditor createTableCellEditor(Property<T> property) {
    if (property instanceof ColumnProperty && !((ColumnProperty<T>) property).isUpdatable()) {
      return null;
    }
    //TODO handle Enter key correctly for foreign key input fields
    return new EntityTableCellEditor<>(() -> createCellEditorComponentValue(property.attribute(), null));
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
    controls.put(ControlCode.REQUEST_TABLE_FOCUS, Control.control(table()::requestFocus));
    controls.put(ControlCode.REQUEST_SEARCH_FIELD_FOCUS, Control.control(table().searchField()::requestFocus));
    controls.put(ControlCode.SELECT_CONDITION_PANEL, Control.control(this::selectConditionPanel));
    controls.put(ControlCode.CONFIGURE_COLUMNS, createColumnControls());
  }

  private Control createToggleConditionPanelControl() {
    if (conditionPanel == null) {
      return null;
    }

    return Control.builder(this::toggleConditionPanel)
            .smallIcon(FrameworkIcons.instance().filter())
            .description(MESSAGES.getString("show_condition_panel"))
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
    if (controls.containsKey(ControlCode.SELECT_COLUMNS)) {
      builder.control(controls.get(ControlCode.SELECT_COLUMNS));
    }
    if (controls.containsKey(ControlCode.RESET_COLUMNS)) {
      builder.control(controls.get(ControlCode.RESET_COLUMNS));
    }

    return builder.build();
  }

  private Control createConditionPanelControl() {
    return ToggleControl.builder(conditionPanelVisibleState)
            .caption(FrameworkMessages.show())
            .build();
  }

  private Controls createCopyControls() {
    return Controls.builder()
            .caption(Messages.copy())
            .smallIcon(FrameworkIcons.instance().copy())
            .controls(createCopyCellControl(), createCopyTableWithHeaderControl())
            .build();
  }

  private Control createCopyCellControl() {
    return Control.builder(table::copySelectedCell)
            .caption(FrameworkMessages.copyCell())
            .enabledState(tableModel.selectionModel().selectionNotEmptyObserver())
            .build();
  }

  private Control createCopyTableWithHeaderControl() {
    return Control.builder(this::copyTableAsDelimitedString)
            .caption(FrameworkMessages.copyTableWithHeader())
            .build();
  }

  private void copyTableAsDelimitedString() {
    Utilities.setClipboard(tableModel.tableDataAsDelimitedString('\t'));
  }

  private boolean includeUpdateSelectedControls() {
    return !tableModel.isReadOnly() && tableModel.isUpdateEnabled() &&
            tableModel.isBatchUpdateEnabled() &&
            !tableModel.entityDefinition().updatableProperties().isEmpty();
  }

  private boolean includeDeleteSelectedControl() {
    return !tableModel.isReadOnly() && tableModel.isDeleteEnabled();
  }

  private FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> createTable() {
    FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> filteredTable =
            FilteredTable.filteredTable(tableModel, new DefaultFilterPanelFactory(tableModel));
    filteredTable.setAutoResizeMode(TABLE_AUTO_RESIZE_MODE.get());
    filteredTable.getTableHeader().setReorderingAllowed(ALLOW_COLUMN_REORDERING.get());
    filteredTable.setRowHeight(filteredTable.getFont().getSize() + FONT_SIZE_TO_ROW_HEIGHT);
    filteredTable.setAutoStartsEdit(false);

    return filteredTable;
  }

  private <T> ComponentValue<T, ? extends JComponent> createUpdateSelectedComponentValue(Attribute<T> attribute, T initialValue) {
    return ((EntityComponentFactory<T, Attribute<T>, ?>) updateSelectedComponentFactories.computeIfAbsent(attribute, a ->
            new UpdateSelectedComponentFactory<T, Attribute<T>, JComponent>())).createComponentValue(attribute, tableModel.editModel(), initialValue);
  }

  private <T> ComponentValue<T, ? extends JComponent> createCellEditorComponentValue(Attribute<T> attribute, T initialValue) {
    return ((EntityComponentFactory<T, Attribute<T>, ?>) tableCellEditorComponentFactories.computeIfAbsent(attribute, a ->
            new DefaultEntityComponentFactory<T, Attribute<T>, JComponent>())).createComponentValue(attribute, tableModel.editModel(), initialValue);
  }

  /**
   * @return the refresh toolbar
   */
  private JToolBar createRefreshToolBar() {
    Control refreshControl = Control.builder(tableModel::refresh)
            .enabledState(tableModel.conditionChangedObserver())
            .smallIcon(FrameworkIcons.instance().refreshRequired())
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

  private JPanel createStatusPanel() {
    String status = "status";
    String refreshing = "refreshing";
    CardLayout refreshStatusLayout = new CardLayout();

    return Components.panel(refreshStatusLayout)
            .add(Components.label(tableModel.statusMessageObserver())
                    .horizontalAlignment(SwingConstants.CENTER)
                    .build(), status)
            .add(createRefreshingProgressPanel(), refreshing)
            .onBuild(panel -> tableModel().refreshingObserver().addDataListener(isRefreshing -> {
              if (showRefreshingProgressBar) {
                refreshStatusLayout.show(panel, isRefreshing ? refreshing : status);
              }
            }))
            .build();
  }

  private JScrollPane createConditionScrollPane(JScrollPane tableScrollPane) {
    return conditionPanel == null ? null : createHiddenLinkedScrollPane(tableScrollPane, conditionPanel);
  }

  private TableColumnComponentPanel<JPanel> createSummaryPanel() {
    Map<TableColumn, JPanel> columnSummaryPanels = createColumnSummaryPanels(tableModel);
    if (columnSummaryPanels.isEmpty()) {
      return null;
    }

    return tableColumnComponentPanel(tableModel.columnModel(), columnSummaryPanels);
  }

  private JScrollPane createSummaryScrollPane(JScrollPane tableScrollPane) {
    if (summaryPanel == null) {
      return null;
    }

    return createHiddenLinkedScrollPane(tableScrollPane, summaryPanel);
  }

  private JPanel createTablePanel(JScrollPane tableScrollPane) {
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
    tableModel.tableConditionModel().conditionModels().values().forEach(conditionModel ->
            conditionModel.addConditionChangedListener(this::onConditionChanged));
    tableModel.refreshingObserver().addDataListener(this::onRefreshingChanged);
    tableModel.addRefreshFailedListener(this::onException);
    tableModel.editModel().addEntitiesEditedListener(table::repaint);
    if (conditionPanel != null) {
      Control refreshControl = Control.builder(tableModel::refresh)
              .enabledState(tableModel.conditionChangedObserver())
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
        conditionPanel.addAdvancedViewListener(advanced -> {
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
    tableModel.columnModel().columns().forEach(this::configureColumn);
    JTableHeader header = table.getTableHeader();
    header.setFocusable(false);
    if (includePopupMenu) {
      addTablePopupMenu();
    }
  }

  private <T> void configureColumn(TableColumn column) {
    Property<T> property = tableModel.entityDefinition().property((Attribute<T>) column.getIdentifier());
    column.setCellRenderer(createTableCellRenderer(property));
    column.setCellEditor(createTableCellEditor(property));
    column.setResizable(true);
    column.setHeaderRenderer(new HeaderRenderer(column.getHeaderRenderer()));
  }

  private void addTablePopupMenu() {
    Controls popupControls = createPopupControls(additionalPopupControls);
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
            .smallIcon(FrameworkIcons.instance().filter())
            .build();
    if (this.controls.containsKey(ControlCode.CONDITION_PANEL_VISIBLE)) {
      conditionControls.add(getControl(ControlCode.CONDITION_PANEL_VISIBLE));
    }
    Controls searchPanelControls = conditionPanel.controls();
    if (!searchPanelControls.isEmpty()) {
      conditionControls.addAll(searchPanelControls);
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
      JOptionPane.showMessageDialog(this, e.getMessage(),
              Messages.error(), JOptionPane.ERROR_MESSAGE);
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

  private static JPanel createRefreshingProgressPanel() {
    JProgressBar progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setString(MESSAGES.getString("refreshing"));
    progressBar.setStringPainted(true);

    return Components.panel(new GridBagLayout())
            .add(progressBar, createHorizontalFillConstraints())
            .build();
  }

  private static void enableRefreshOnEnterControl(JComponent component, Control refreshControl) {
    if (component instanceof JComboBox) {
      new RefreshOnEnterAction((JComboBox<?>) component, refreshControl);
    }
    else if (component instanceof TemporalField) {
      ((TemporalField<?>) component).addActionListener(refreshControl);
    }
  }

  private static GridBagConstraints createHorizontalFillConstraints() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    constraints.insets = new Insets(0, Layouts.HORIZONTAL_VERTICAL_GAP.get(), 0, Layouts.HORIZONTAL_VERTICAL_GAP.get());

    return constraints;
  }

  private static void addRefreshOnEnterControl(EntityTableConditionPanel tableConditionPanel, Control refreshControl) {
    tableConditionPanel.tableColumns().forEach(column -> {
      ColumnConditionPanel<?, ?> columnConditionPanel = tableConditionPanel.conditionPanel((Attribute<?>) column.getIdentifier());
      if (columnConditionPanel != null) {
        enableRefreshOnEnterControl(columnConditionPanel.operatorComboBox(), refreshControl);
        enableRefreshOnEnterControl(columnConditionPanel.equalField(), refreshControl);
        enableRefreshOnEnterControl(columnConditionPanel.lowerBoundField(), refreshControl);
        enableRefreshOnEnterControl(columnConditionPanel.upperBoundField(), refreshControl);
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
    tableModel.columnModel().columns().forEach(column ->
            tableModel.columnSummaryModel((Attribute<?>) column.getIdentifier())
                    .ifPresent(columnSummaryModel ->
                            components.put(column, columnSummaryPanel(columnSummaryModel))));

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
        tabPane.addTab(connectionProvider.entities().definition(entry.getKey()).caption(),
                createEntityTablePanel(dependentEntities, connectionProvider));
      }
    }
    panel.add(tabPane, BorderLayout.CENTER);

    return panel;
  }

  private static Point popupLocation(JTable table) {
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
      TableColumn tableColumn = tableModel.columnModel().getColumn(column);
      TableCellRenderer renderer = tableColumn.getCellRenderer();
      Attribute<?> attribute = (Attribute<?>) tableColumn.getIdentifier();
      boolean displayConditionState = renderer instanceof EntityTableCellRenderer
              && ((EntityTableCellRenderer) renderer).isDisplayConditionState()
              && tableModel.tableConditionModel().isConditionEnabled(attribute);
      Font defaultFont = component.getFont();
      component.setFont(displayConditionState ? defaultFont.deriveFont(defaultFont.getStyle() | Font.BOLD) : defaultFont);

      return component;
    }
  }

  private static final class UpdateSelectedComponentFactory<T, A extends Attribute<T>, C extends JComponent> extends DefaultEntityComponentFactory<T, A, C> {

    @Override
    public ComponentValue<T, C> createComponentValue(A attribute, SwingEntityEditModel editModel, T initialValue) {
      requireNonNull(attribute, "attribute");
      requireNonNull(editModel, "editModel");
      if (attribute.isString()) {
        return (ComponentValue<T, C>) new EntityComponents(editModel.entityDefinition())
                .textInputPanel((Attribute<String>) attribute)
                .initialValue((String) initialValue)
                .buildComponentValue();
      }

      return super.createComponentValue(attribute, editModel, initialValue);
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
              tableModel.tableConditionModel().filterModels().get(column.getIdentifier());
      if (filterModel == null) {
        return null;
      }

      return columnConditionPanel(filterModel, ToggleAdvancedButton.YES);
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
