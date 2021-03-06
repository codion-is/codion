/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.Conjunction;
import is.codion.common.Util;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.event.EventListener;
import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnSummaryModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.PropertyValue;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import is.codion.swing.common.ui.dialog.DialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.dialog.Modal;
import is.codion.swing.common.ui.table.ColumnConditionPanel;
import is.codion.swing.common.ui.table.ColumnSummaryPanel;
import is.codion.swing.common.ui.table.ConditionPanelFactory;
import is.codion.swing.common.ui.table.FilteredTable;
import is.codion.swing.common.ui.table.TableColumnComponentPanel;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValuePanel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import static is.codion.common.Util.nullOrEmpty;
import static is.codion.swing.common.ui.Components.hideWaitCursor;
import static is.codion.swing.common.ui.Components.showWaitCursor;
import static is.codion.swing.common.ui.Windows.getParentWindow;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.framework.ui.EntityTableCellRenderer.entityTableCellRenderer;
import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;
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
   * Specifies whether or not columns can be rearranged in tables<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> ALLOW_COLUMN_REORDERING = Configuration.booleanValue(
          "is.codion.swing.framework.ui.EntityTablePanel.allowColumnReordering", true);

  /**
   * Specifies whether the table condition panels should be visible or not by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> TABLE_CONDITION_PANEL_VISIBLE = Configuration.booleanValue(
          "is.codion.swing.framework.ui.EntityTablePanel.tableConditionPanelVisible", false);

  /**
   * Specifies the default table column resize mode for tables in the application<br>
   * Value type: Integer (JTable.AUTO_RESIZE_*)<br>
   * Default value: JTable.AUTO_RESIZE_OFF
   */
  public static final PropertyValue<Integer> TABLE_AUTO_RESIZE_MODE = Configuration.integerValue(
          "is.codion.swing.framework.ui.EntityTablePanel.tableAutoResizeMode", JTable.AUTO_RESIZE_OFF);

  /**
   * Specifies whether to include a {@link EntityPopupMenu} on this table, triggered with CTRL-ALT-V.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_ENTITY_MENU = Configuration.booleanValue(
          "is.codion.swing.framework.ui.EntityTablePanel.includeEntityMenu", true);

  /**
   * Specifies whether to include a 'Clear' control in the popup menu.<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> INCLUDE_CLEAR_CONTROL = Configuration.booleanValue(
          "is.codion.swing.framework.ui.EntityTablePanel.includeClearControl", false);

  /**
   * Specifies whether the refresh button toolbar should be hidden automatically when the condition panel is not visible.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> AUTOMATICALLY_HIDE_REFRESH_TOOLBAR = Configuration.booleanValue(
          "is.codion.swing.framework.ui.EntityTablePanel.automaticallyHideRefreshToolbar", true);

  /**
   * The standard controls available to the TablePanel
   */
  public enum ControlCode {
    PRINT_TABLE,
    DELETE_SELECTED,
    VIEW_DEPENDENCIES,
    UPDATE_SELECTED,
    SELECT_COLUMNS,
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
    COPY_TABLE_DATA
  }

  private static final Dimension TOOLBAR_BUTTON_SIZE = TextFields.DIMENSION_TEXT_FIELD_SQUARE;
  private static final int STATUS_MESSAGE_FONT_SIZE = 12;
  private static final int POPUP_LOCATION_X_OFFSET = 42;
  private static final int POPUP_LOCATION_EMPTY_SELECTION = 100;
  private static final int FONT_SIZE_TO_ROW_HEIGHT = 4;

  private final State conditionPanelVisibleState = State.state();
  private final State summaryPanelVisibleState = State.state();

  private final Map<ControlCode, Control> controls = new EnumMap<>(ControlCode.class);

  private final SwingEntityTableModel tableModel;

  private final FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> table;

  private final EntityComponentValues componentValues;

  private final AbstractEntityTableConditionPanel conditionPanel;

  private final JScrollPane conditionScrollPane;

  private final JScrollPane summaryScrollPane;

  /**
   * Base panel for the table, condition and summary panels
   */
  private final JPanel tablePanel;

  /**
   * the toolbar containing the refresh button
   */
  private final JToolBar refreshToolBar;

  /**
   * the label for showing the status of the table, that is, the number of rows, number of selected rows etc.
   */
  private final JLabel statusMessageLabel = initializeStatusMessageLabel();

  private final List<ControlList> additionalPopupControls = new ArrayList<>();
  private final List<ControlList> additionalToolBarControls = new ArrayList<>();
  private final Set<Attribute<?>> excludeFromUpdateMenu = new HashSet<>();

  /**
   * specifies whether to automatically hide the refresh toolbar along with the condition panel
   */
  private boolean automaticallyHideRefreshToolbar = AUTOMATICALLY_HIDE_REFRESH_TOOLBAR.get();

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
  public EntityTablePanel(final SwingEntityTableModel tableModel) {
    this(tableModel, new EntityComponentValues());
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param conditionPanel the condition panel, if any
   */
  public EntityTablePanel(final SwingEntityTableModel tableModel, final AbstractEntityTableConditionPanel conditionPanel) {
    this(tableModel, new EntityComponentValues(), conditionPanel);
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param componentValues the component value provider for this table panel
   */
  public EntityTablePanel(final SwingEntityTableModel tableModel, final EntityComponentValues componentValues) {
    this(tableModel, componentValues, new EntityTableConditionPanel(tableModel.getTableConditionModel(),
            tableModel.getColumnModel(), tableModel::refresh, tableModel.getQueryConditionRequiredState()));
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param componentValues the component value provider for this table panel
   * @param conditionPanel the condition panel, if any
   */
  public EntityTablePanel(final SwingEntityTableModel tableModel, final EntityComponentValues componentValues,
                          final AbstractEntityTableConditionPanel conditionPanel) {
    this.tableModel = requireNonNull(tableModel, "tableModel");
    this.table = initializeFilteredTable();
    this.componentValues = requireNonNull(componentValues, "componentValues");
    this.conditionPanel = conditionPanel;
    final JScrollPane tableScrollPane = new JScrollPane(table);
    this.conditionScrollPane = initializeConditionScrollPane(tableScrollPane);
    this.summaryScrollPane = initializeSummaryScrollPane(tableScrollPane);
    this.tablePanel = initializeTablePanel(tableScrollPane);
    this.refreshToolBar = initializeRefreshToolBar();
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
  public final void excludeFromUpdateMenu(final Attribute<?> attribute) {
    checkIfInitialized();
    getTableModel().getEntityDefinition().getProperty(attribute);//just validating that the property exists
    excludeFromUpdateMenu.add(attribute);
  }

  /**
   * @param additionalPopupControls a set of controls to add to the table popup menu
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void addPopupControls(final ControlList additionalPopupControls) {
    checkIfInitialized();
    this.additionalPopupControls.add(requireNonNull(additionalPopupControls));
  }

  /**
   * @param additionalToolBarControls a set of controls to add to the table toolbar menu
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void addToolBarControls(final ControlList additionalToolBarControls) {
    checkIfInitialized();
    this.additionalToolBarControls.add(requireNonNull(additionalToolBarControls));
  }

  /**
   * @param includeSouthPanel true if the south panel should be included
   * @see #initializeSouthPanel()
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludeSouthPanel(final boolean includeSouthPanel) {
    checkIfInitialized();
    this.includeSouthPanel = includeSouthPanel;
  }

  /**
   * @param includeConditionPanel true if the condition panel should be included
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludeConditionPanel(final boolean includeConditionPanel) {
    checkIfInitialized();
    this.includeConditionPanel = includeConditionPanel;
  }

  /**
   * @param includePopupMenu true if a popup menu should be included
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludePopupMenu(final boolean includePopupMenu) {
    checkIfInitialized();
    this.includePopupMenu = includePopupMenu;
  }

  /**
   * @param includeClearControl true if a 'Clear' control should be included in the popup menu
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludeClearControl(final boolean includeClearControl) {
    checkIfInitialized();
    this.includeClearControl = includeClearControl;
  }

  /**
   * @return true if the refresh toolbar should be hidden unless the condition panel is visible
   */
  public final boolean isAutomaticallyHideRefreshToolbar() {
    return automaticallyHideRefreshToolbar;
  }

  /**
   * @param automaticallyHideRefreshToolbar true if the refresh toolbar should be hidden unless the condition panel is visible
   */
  public final void setAutomaticallyHideRefreshToolbar(final boolean automaticallyHideRefreshToolbar) {
    this.automaticallyHideRefreshToolbar = automaticallyHideRefreshToolbar;
    this.refreshToolBar.setVisible(!automaticallyHideRefreshToolbar || isConditionPanelVisible());
  }

  /**
   * Hides or shows the column condition panel for this EntityTablePanel
   * @param visible if true the condition panel is shown, if false it is hidden
   */
  public final void setConditionPanelVisible(final boolean visible) {
    conditionPanelVisibleState.set(visible);
  }

  /**
   * @return true if the condition panel is visible, false if it is hidden
   */
  public final boolean isConditionPanelVisible() {
    return conditionScrollPane != null && conditionScrollPane.isVisible();
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
      final State advancedState = conditionPanel.getAdvancedState();
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
  public final void setSummaryPanelVisible(final boolean visible) {
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
  public final void setReferentialIntegrityErrorHandling(final ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling) {
    this.referentialIntegrityErrorHandling = requireNonNull(referentialIntegrityErrorHandling);
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + tableModel.getEntityType();
  }

  /**
   * @param controlCode the control code
   * @return the control associated with {@code controlCode}
   * @throws IllegalArgumentException in case no control is associated with the given control code
   */
  public final Control getControl(final ControlCode controlCode) {
    if (!controls.containsKey(requireNonNull(controlCode))) {
      throw new IllegalArgumentException(controlCode + " control not available in panel: " + this);
    }

    return controls.get(controlCode);
  }

  /**
   * Creates a {@link ControlList} containing controls for updating the value of a single property
   * for the selected entities. These controls are enabled as long as the selection is not empty
   * and {@link EntityEditModel#getUpdateEnabledObserver()} is enabled.
   * @return a control list containing controls, one for each updatable property in the
   * underlying entity, for performing an update on the selected entities
   * @throws IllegalStateException in case the underlying edit model is read only or updating is not enabled
   * @see #excludeFromUpdateMenu(Attribute)
   * @see EntityEditModel#getUpdateEnabledObserver()
   */
  public final ControlList createUpdateSelectedControls() {
    if (!includeUpdateSelectedControls()) {
      throw new IllegalStateException("Table model is read only or does not allow updates");
    }
    final StateObserver selectionNotEmpty = tableModel.getSelectionModel().getSelectionNotEmptyObserver();
    final StateObserver updateEnabled = tableModel.getEditModel().getUpdateEnabledObserver();
    final StateObserver enabled = State.combination(Conjunction.AND, selectionNotEmpty, updateEnabled);
    final ControlList controlList = ControlList.builder()
            .name(FrameworkMessages.get(FrameworkMessages.UPDATE))
            .enabledState(enabled)
            .icon(frameworkIcons().edit()).build();
    controlList.setDescription(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP));
    Properties.sort(tableModel.getEntityDefinition().getUpdatableProperties()).forEach(property -> {
      if (!excludeFromUpdateMenu.contains(property.getAttribute())) {
        final String caption = property.getCaption() == null ? property.getAttribute().getName() : property.getCaption();
        controlList.add(Control.builder().command(() -> updateSelectedEntities(property)).name(caption).enabledState(enabled).build());
      }
    });

    return controlList;
  }

  /**
   * @return a control for showing the dependencies dialog
   */
  public final Control createViewDependenciesControl() {
    return Control.builder().command(this::viewSelectionDependencies)
            .name(FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES) + "...")
            .enabledState(tableModel.getSelectionModel().getSelectionNotEmptyObserver())
            .description(FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES_TIP))
            .mnemonic('W')
            .icon(frameworkIcons().dependencies())
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
    return Control.builder()
            .command(this::delete)
            .name(FrameworkMessages.get(FrameworkMessages.DELETE))
            .enabledState(State.combination(Conjunction.AND,
                    tableModel.getEditModel().getDeleteEnabledObserver(),
                    tableModel.getSelectionModel().getSelectionNotEmptyObserver()))
            .description(FrameworkMessages.get(FrameworkMessages.DELETE_TIP))
            .icon(frameworkIcons().delete())
            .build();
  }

  /**
   * @return a control for printing the table
   */
  public final Control createPrintTableControl() {
    final String printCaption = MESSAGES.getString("print_table");
    return Control.builder()
            .command(this::printTable)
            .name(printCaption)
            .description(printCaption)
            .mnemonic(printCaption.charAt(0))
            .icon(frameworkIcons().print())
            .build();
  }

  /**
   * @return a Control for refreshing the underlying table data
   */
  public final Control createRefreshControl() {
    final String refreshCaption = FrameworkMessages.get(FrameworkMessages.REFRESH);
    return Control.builder()
            .command(tableModel::refresh)
            .name(refreshCaption)
            .description(FrameworkMessages.get(FrameworkMessages.REFRESH_TIP))
            .mnemonic(refreshCaption.charAt(0))
            .icon(frameworkIcons().refresh()).build();
  }

  /**
   * @return a Control for clearing the underlying table model, that is, removing all rows
   */
  public final Control createClearControl() {
    final String clearCaption = FrameworkMessages.get(FrameworkMessages.CLEAR);
    return Control.builder()
            .command(tableModel::clear)
            .name(clearCaption)
            .mnemonic(clearCaption.charAt(0))
            .build();
  }

  /**
   * Retrieves a new property value via input dialog and performs an update on the selected entities
   * @param propertyToUpdate the property to update
   * @param <T> the property type
   * @see EntityComponentValues#createComponentValue(Attribute, SwingEntityEditModel, Object)
   */
  public final <T> void updateSelectedEntities(final Property<T> propertyToUpdate) {
    requireNonNull(propertyToUpdate);
    if (tableModel.getSelectionModel().isSelectionEmpty()) {
      return;
    }

    final List<Entity> selectedEntities = tableModel.getEntities().deepCopyEntities(tableModel.getSelectionModel().getSelectedItems());
    final Collection<T> values = Entity.getDistinct(propertyToUpdate.getAttribute(), selectedEntities);
    final T initialValue = values.size() == 1 ? values.iterator().next() : null;
    final ComponentValuePanel<T, JComponent> inputPanel = new ComponentValuePanel<>(propertyToUpdate.getCaption(),
            componentValues.createComponentValue(propertyToUpdate.getAttribute(), tableModel.getEditModel(), initialValue));
    Dialogs.displayInDialog(this, inputPanel, FrameworkMessages.get(FrameworkMessages.SET_PROPERTY_VALUE), Modal.YES,
            inputPanel.getOkAction(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted()) {
      Entity.put(propertyToUpdate.getAttribute(), inputPanel.getValue(), selectedEntities);
      try {
        showWaitCursor(this);
        tableModel.update(selectedEntities);
      }
      catch (final Exception e) {
        LOG.error(e.getMessage(), e);
        onException(e);
      }
      finally {
        hideWaitCursor(this);
      }
    }
  }

  /**
   * Shows a dialog containing lists of entities depending on the selected entities via foreign key
   */
  public final void viewSelectionDependencies() {
    if (tableModel.getSelectionModel().isSelectionEmpty()) {
      return;
    }

    try {
      showWaitCursor(this);
      final Map<EntityType<?>, Collection<Entity>> dependencies =
              tableModel.getConnectionProvider().getConnection()
                      .selectDependencies(tableModel.getSelectionModel().getSelectedItems());
      if (!dependencies.isEmpty()) {
        showDependenciesDialog(dependencies, tableModel.getConnectionProvider(), this,
                MESSAGES.getString("dependent_records_found"));
      }
      else {
        JOptionPane.showMessageDialog(this, MESSAGES.getString("none_found"),
                MESSAGES.getString("no_dependent_records"), JOptionPane.INFORMATION_MESSAGE);
      }
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      onException(e);
    }
    finally {
      hideWaitCursor(this);
    }
  }

  /**
   * Deletes the entities selected in the underlying table model
   * @see #confirmDelete()
   */
  public final void delete() {
    try {
      if (confirmDelete()) {
        try {
          showWaitCursor(this);
          tableModel.deleteSelected();
        }
        finally {
          hideWaitCursor(this);
        }
      }
    }
    catch (final ReferentialIntegrityException e) {
      LOG.debug(e.getMessage(), e);
      onReferentialIntegrityException(e, tableModel.getSelectionModel().getSelectedItems());
    }
    catch (final Exception e) {
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
   * the dependencies of the given entity are displayed to the user, otherwise {@link #onException(Exception)} is called.
   * @param exception the exception
   * @param entities the entities causing the exception
   * @see #setReferentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling)
   */
  public void onReferentialIntegrityException(final ReferentialIntegrityException exception,
                                              final List<Entity> entities) {
    if (referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DEPENDENCIES) {
      showDependenciesDialog(entities, tableModel.getConnectionProvider(), this);
    }
    else {
      onException(exception);
    }
  }

  /**
   * Handles the given exception, simply displays the error message to the user by default.
   * @param exception the exception to handle
   * @see #displayException(Throwable, Window)
   */
  public void onException(final Exception exception) {
    displayException(exception, getParentWindow(this));
  }

  @Override
  public final void displayException(final Throwable throwable, final Window dialogParent) {
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

    return Control.builder()
            .command(this::toggleConditionPanel)
            .icon(frameworkIcons().filter())
            .description(MESSAGES.getString("show_condition_panel")).build();
  }

  /**
   * Initializes the button used to toggle the summary panel state (hidden and visible)
   * @return a summary panel toggle button
   */
  public final Control createToggleSummaryPanelControl() {
    return ToggleControl.builder()
            .state(summaryPanelVisibleState)
            .icon(frameworkIcons().summary())
            .description(MESSAGES.getString("toggle_summary_tip"))
            .build();
  }

  /**
   * @return a control for clearing the table selection
   */
  public final Control createClearSelectionControl() {
    return Control.builder()
            .command(tableModel.getSelectionModel()::clearSelection)
            .enabledState(tableModel.getSelectionModel().getSelectionNotEmptyObserver())
            .icon(frameworkIcons().clearSelection())
            .description(MESSAGES.getString("clear_selection_tip")).build();
  }

  /**
   * @return a control for moving the table selection down one index
   */
  public final Control createMoveSelectionDownControl() {
    return Control.builder()
            .command(tableModel.getSelectionModel()::moveSelectionDown)
            .icon(frameworkIcons().down())
            .description(MESSAGES.getString("selection_down_tip")).build();
  }

  /**
   * @return a control for moving the table selection up one index
   */
  public final Control createMoveSelectionUpControl() {
    return Control.builder()
            .command(tableModel.getSelectionModel()::moveSelectionUp)
            .icon(frameworkIcons().up())
            .description(MESSAGES.getString("selection_up_tip")).build();
  }

  /**
   * Displays a dialog with the entities depending on the given entities.
   * @param entities the entities for which to display dependencies
   * @param connectionProvider the connection provider
   * @param dialogParent the dialog parent
   */
  public static void showDependenciesDialog(final Collection<Entity> entities, final EntityConnectionProvider connectionProvider,
                                            final JComponent dialogParent) {
    requireNonNull(entities);
    requireNonNull(connectionProvider);
    requireNonNull(dialogParent);
    try {
      final Map<EntityType<?>, Collection<Entity>> dependencies = connectionProvider.getConnection().selectDependencies(entities);
      showDependenciesDialog(dependencies, connectionProvider, dialogParent, MESSAGES.getString("delete_dependent_records"));
    }
    catch (final DatabaseException e) {
      DefaultDialogExceptionHandler.getInstance().displayException(e, getParentWindow(dialogParent));
    }
  }

  /**
   * Creates a static read-only entity table panel showing the given entities
   * @param entities the entities to show in the panel
   * @param connectionProvider the EntityConnectionProvider, in case the returned panel should require one
   * @return a static EntityTablePanel showing the given entities
   */
  public static EntityTablePanel createReadOnlyEntityTablePanel(final Collection<Entity> entities,
                                                                final EntityConnectionProvider connectionProvider) {
    if (Util.nullOrEmpty(entities)) {
      throw new IllegalArgumentException("Cannot create a EntityTablePanel without the entities");
    }

    final SwingEntityTableModel tableModel = new SwingEntityTableModel(entities.iterator().next().getEntityType(), connectionProvider) {
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
  public static EntityTablePanel createEntityTablePanel(final Collection<Entity> entities,
                                                        final EntityConnectionProvider connectionProvider) {
    if (Util.nullOrEmpty(entities)) {
      throw new IllegalArgumentException("Cannot create a EntityTablePanel without the entities");
    }

    final EntityType<?> entityType = entities.iterator().next().getEntityType();
    final SwingEntityEditModel editModel = new SwingEntityEditModel(entityType, connectionProvider);
    final SwingEntityTableModel tableModel = new SwingEntityTableModel(entityType, connectionProvider) {
      @Override
      protected Collection<Entity> refreshItems() {
        return entities;
      }
    };
    tableModel.setEditModel(editModel);
    tableModel.refresh();

    return createEntityTablePanel(tableModel);
  }

  /**
   * Creates a entity table panel based on the given table model.
   * If the table model is not read only, a popup menu for updating or deleting the selected entities is provided.
   * @param tableModel the table model
   * @return a entity table panel based on the given model
   */
  public static EntityTablePanel createEntityTablePanel(final SwingEntityTableModel tableModel) {
    final EntityTablePanel tablePanel = new EntityTablePanel(tableModel) {
      @Override
      protected ControlList getPopupControls(final List<ControlList> additionalPopupControls) {
        return additionalPopupControls.get(0);
      }
    };
    final ControlList popupControls = ControlList.controlList();
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
      try {
        showWaitCursor(this);
        setupControls();
        initializeTable();
        layoutPanel(tablePanel, includeSouthPanel ? initializeSouthPanel() : null);
        setConditionPanelVisibleInternal(conditionPanelVisibleState.get());
        setSummaryPanelVisibleInternal(summaryPanelVisibleState.get());
        bindEvents();
        updateStatusMessage();
      }
      finally {
        panelInitialized = true;
        hideWaitCursor(this);
      }
    }

    return this;
  }

  /**
   * Initializes the south panel, override and return null for no south panel.
   * @return the south panel, or null if no south panel should be included
   */
  protected JPanel initializeSouthPanel() {
    final JSplitPane southCenterSplitPane = new JSplitPane();
    southCenterSplitPane.setContinuousLayout(true);
    southCenterSplitPane.setResizeWeight(0.35);
    southCenterSplitPane.setTopComponent(table.getSearchField());
    southCenterSplitPane.setBottomComponent(statusMessageLabel);
    final JPanel southPanel = new JPanel(new BorderLayout());
    southPanel.add(southCenterSplitPane, BorderLayout.CENTER);
    southPanel.setBorder(BorderFactory.createEtchedBorder());
    southPanel.add(refreshToolBar, BorderLayout.WEST);
    final JToolBar southToolBar = initializeSouthToolBar();
    if (southToolBar != null) {
      southPanel.add(southToolBar, BorderLayout.EAST);
    }

    return southPanel;
  }

  /**
   * Associates {@code control} with {@code controlCode}
   * @param controlCode the control code
   * @param control the control to associate with {@code controlCode}
   */
  protected final void setControl(final ControlCode controlCode, final Control control) {
    requireNonNull(controlCode);
    if (control == null) {
      controls.remove(controlCode);
    }
    else {
      controls.put(controlCode, control);
    }
  }

  protected ControlList getToolBarControls(final List<ControlList> additionalToolBarControlLists) {
    requireNonNull(additionalToolBarControlLists);
    final ControlList toolbarControls = ControlList.controlList();
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
    toolbarControls.add(createPrintTableControl());
    toolbarControls.add(controls.get(ControlCode.CLEAR_SELECTION));
    toolbarControls.addSeparator();
    toolbarControls.add(controls.get(ControlCode.MOVE_SELECTION_UP));
    toolbarControls.add(controls.get(ControlCode.MOVE_SELECTION_DOWN));
    additionalToolBarControlLists.forEach(controlList -> {
      toolbarControls.addSeparator();
      controlList.getActions().forEach(toolbarControls::add);
    });

    return toolbarControls;
  }

  /**
   * Constructs a ControlList containing the controls to include in the table popup menu.
   * Returns null or an empty ControlList to indicate that no popup menu should be included.
   * @param additionalPopupControls any additional controls to include in the popup menu
   * @return the ControlList on which to base the table popup menu, null or an empty ControlList
   * if no popup menu should be included
   */
  protected ControlList getPopupControls(final List<ControlList> additionalPopupControls) {
    requireNonNull(additionalPopupControls);
    final ControlList popupControls = ControlList.controlList();
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
    final ControlList printControls = createPrintControls();
    if (printControls != null) {
      popupControls.add(printControls);
      separatorRequired = true;
    }
    if (controls.containsKey(ControlCode.SELECT_COLUMNS)) {
      if (separatorRequired) {
        popupControls.addSeparator();
      }
      popupControls.add(controls.get(ControlCode.SELECT_COLUMNS));
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

  protected ControlList createPrintControls() {
    final String printCaption = Messages.get(Messages.PRINT);
    final ControlList printControls = ControlList.builder()
            .name(printCaption)
            .mnemonic(printCaption.charAt(0))
            .icon(frameworkIcons().print()).build();
    printControls.add(controls.get(ControlCode.PRINT_TABLE));

    return printControls;
  }

  protected final Control createConditionPanelControl() {
    return ToggleControl.builder().state(conditionPanelVisibleState).name(FrameworkMessages.get(FrameworkMessages.SHOW)).build();
  }

  protected final ControlList createCopyControls() {
    return ControlList.builder()
            .name(Messages.get(Messages.COPY))
            .icon(frameworkIcons().copy())
            .controls(createCopyCellControl(), createCopyTableWithHeaderControl()).build();
  }

  protected final Control createCopyCellControl() {
    return Control.builder()
            .command(this::copySelectedCell)
            .name(FrameworkMessages.get(FrameworkMessages.COPY_CELL))
            .enabledState(tableModel.getSelectionModel().getSelectionNotEmptyObserver()).build();
  }

  protected final Control createCopyTableWithHeaderControl() {
    return Control.builder().command(this::copyTableAsDelimitedString).name(FrameworkMessages.get(FrameworkMessages.COPY_TABLE_WITH_HEADER)).build();
  }

  /**
   * Called before a delete is performed, if true is returned the delete action is performed otherwise it is canceled
   * @return true if the delete action should be performed
   */
  protected final boolean confirmDelete() {
    final String[] messages = getConfirmDeleteMessages();
    final int res = JOptionPane.showConfirmDialog(this, messages[0], messages[1], JOptionPane.OK_CANCEL_OPTION);

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
  protected TableCellRenderer initializeTableCellRenderer(final Property<?> property) {
    return entityTableCellRenderer(tableModel, property);
  }

  /**
   * Creates a TableCellEditor for the given property, returns null if no editor is available
   * @param property the property
   * @return a TableCellEditor for the given property
   */
  protected TableCellEditor initializeTableCellEditor(final Property<?> property) {
    if (property instanceof ColumnProperty && !((ColumnProperty<?>) property).isUpdatable()) {
      return null;
    }

    if (property instanceof ForeignKeyProperty) {
      return new ForeignKeyTableCellEditor(tableModel.getConnectionProvider(),
              getTableModel().getEntityDefinition(), (ForeignKey) property.getAttribute());
    }

    return new EntityTableCellEditor<>(getTableModel().getEntityDefinition(), property.getAttribute());
  }

  /**
   * This method simply adds {@code tablePanel} at location BorderLayout.CENTER and,
   * if non-null, the given {@code southPanel} to the {@code BorderLayout.SOUTH} location.
   * By overriding this method you can override the default layout.
   * @param tablePanel the panel containing the table, condition and summary panel
   * @param southPanel the south toolbar panel, null if not required
   * @see #initializeSouthPanel()
   */
  protected void layoutPanel(final JPanel tablePanel, final JPanel southPanel) {
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
    final ControlList toolbarControlList = getToolBarControls(additionalToolBarControls);
    if (toolbarControlList != null) {
      final JToolBar southToolBar = Controls.toolBar(toolbarControlList, JToolBar.HORIZONTAL);
      for (final Component component : southToolBar.getComponents()) {
        component.setPreferredSize(TOOLBAR_BUTTON_SIZE);
      }
      southToolBar.setFocusable(false);
      southToolBar.setFloatable(false);
      southToolBar.setRollover(true);

      return southToolBar;
    }

    return null;
  }

  private void setupControls() {
    if (includeDeleteSelectedControl()) {
      setControl(ControlCode.DELETE_SELECTED, createDeleteSelectedControl());
    }
    if (includeUpdateSelectedControls()) {
      setControl(ControlCode.UPDATE_SELECTED, createUpdateSelectedControls());
    }
    if (includeClearControl) {
      setControl(ControlCode.CLEAR, createClearControl());
    }
    setControl(ControlCode.REFRESH, createRefreshControl());
    setControl(ControlCode.SELECT_COLUMNS, table.createSelectColumnsControl());
    setControl(ControlCode.VIEW_DEPENDENCIES, createViewDependenciesControl());
    if (summaryScrollPane != null) {
      setControl(ControlCode.TOGGLE_SUMMARY_PANEL, createToggleSummaryPanelControl());
    }
    if (includeConditionPanel && conditionPanel != null) {
      setControl(ControlCode.CONDITION_PANEL_VISIBLE, createConditionPanelControl());
      setControl(ControlCode.TOGGLE_CONDITION_PANEL, createToggleConditionPanelControl());
    }
    setControl(ControlCode.PRINT_TABLE, createPrintTableControl());
    setControl(ControlCode.CLEAR_SELECTION, createClearSelectionControl());
    setControl(ControlCode.MOVE_SELECTION_UP, createMoveSelectionDownControl());
    setControl(ControlCode.MOVE_SELECTION_DOWN, createMoveSelectionUpControl());
    setControl(ControlCode.COPY_TABLE_DATA, createCopyControls());
    setControl(ControlCode.SELECTION_MODE, table.createSingleSelectionModeControl());
  }

  private void copySelectedCell() {
    final Object value = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn());
    Components.setClipboard(value == null ? "" : value.toString());
  }

  private void copyTableAsDelimitedString() {
    Components.setClipboard(tableModel.getTableDataAsDelimitedString('\t'));
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
    final FilteredTable<Entity, Attribute<?>, SwingEntityTableModel> filteredTable =
            new FilteredTable<>(tableModel, new DefaultConditionPanelFactory(tableModel));
    filteredTable.setAutoResizeMode(TABLE_AUTO_RESIZE_MODE.get());
    filteredTable.getTableHeader().setReorderingAllowed(ALLOW_COLUMN_REORDERING.get());
    filteredTable.setRowHeight(filteredTable.getFont().getSize() + FONT_SIZE_TO_ROW_HEIGHT);
    filteredTable.setAutoStartsEdit(false);

    return filteredTable;
  }

  /**
   * @return the refresh toolbar
   */
  private JToolBar initializeRefreshToolBar() {
    final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    final String keyName = keyStroke.toString().replace("pressed ", "");
    final Control refresh = Control.builder()
            .command(tableModel::refresh)
            .enabledState(tableModel.getTableConditionModel().getConditionObserver())
            .description(FrameworkMessages.get(FrameworkMessages.REFRESH_TIP) + " (" + keyName + ")")
            .icon(frameworkIcons().refreshRequired()).build();

    KeyEvents.addKeyEvent(this, KeyEvent.VK_F5, 0, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, refresh);

    final JButton button = new JButton(refresh);
    button.setPreferredSize(TOOLBAR_BUTTON_SIZE);
    button.setFocusable(false);

    final JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
    toolBar.setFocusable(false);
    toolBar.setFloatable(false);
    toolBar.setRollover(true);
    toolBar.add(button);
    if (automaticallyHideRefreshToolbar) {
      //made visible when condition panel is visible
      toolBar.setVisible(false);
    }

    return toolBar;
  }

  private JScrollPane initializeConditionScrollPane(final JScrollPane tableScrollPane) {
    return conditionPanel == null ? null : createHiddenLinkedScrollPane(tableScrollPane, conditionPanel);
  }

  private JScrollPane initializeSummaryScrollPane(final JScrollPane tableScrollPane) {
    final Map<TableColumn, JPanel> columnSummaryPanels = createColumnSummaryPanels(tableModel);
    if (columnSummaryPanels.isEmpty()) {
      return null;
    }

    return createHiddenLinkedScrollPane(tableScrollPane, new TableColumnComponentPanel<>(tableModel.getColumnModel(), columnSummaryPanels));
  }

  private JPanel initializeTablePanel(final JScrollPane tableScrollPane) {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(tableScrollPane, BorderLayout.CENTER);
    if (conditionScrollPane != null) {
      panel.add(conditionScrollPane, BorderLayout.NORTH);
    }
    if (summaryScrollPane != null) {
      panel.add(summaryScrollPane, BorderLayout.SOUTH);
    }

    return panel;
  }

  private void updateStatusMessage() {
    if (statusMessageLabel != null) {
      final String status = getStatusMessage();
      statusMessageLabel.setText(status);
      statusMessageLabel.setToolTipText(status);
    }
  }

  private String getStatusMessage() {
    final int filteredItemCount = tableModel.getFilteredItemCount();

    return tableModel.getRowCount() + " (" + tableModel.getSelectionModel().getSelectionCount() + " " +
            MESSAGES.getString("selected") + (filteredItemCount > 0 ? ", " +
            filteredItemCount + " " + MESSAGES.getString("hidden") + ")" : ")");
  }

  private void bindEvents() {
    if (includeDeleteSelectedControl()) {
      KeyEvents.addKeyEvent(table, KeyEvent.VK_DELETE, createDeleteSelectedControl());
    }
    if (INCLUDE_ENTITY_MENU.get()) {
      KeyEvents.addKeyEvent(table, KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
              control(this::showEntityMenu));
    }
    conditionPanelVisibleState.addDataListener(this::setConditionPanelVisibleInternal);
    summaryPanelVisibleState.addDataListener(this::setSummaryPanelVisibleInternal);
    final EventListener statusListener = () -> SwingUtilities.invokeLater(EntityTablePanel.this::updateStatusMessage);
    tableModel.getSelectionModel().addSelectionChangedListener(statusListener);
    tableModel.addFilterListener(statusListener);
    tableModel.addTableDataChangedListener(statusListener);
    tableModel.getTableConditionModel().addConditionListener(this::onConditionChanged);
    tableModel.addSortListener(table.getTableHeader()::repaint);
    tableModel.addRefreshStartedListener(() -> Components.showWaitCursor(EntityTablePanel.this));
    tableModel.addRefreshDoneListener(() -> Components.hideWaitCursor(EntityTablePanel.this));
    if (tableModel.hasEditModel()) {
      tableModel.getEditModel().addEntitiesEditedListener(table::repaint);
    }
    if (conditionPanel != null) {
      conditionPanel.addFocusGainedListener(table::scrollToColumn);
      if (conditionPanel.hasAdvancedView()) {
        conditionPanel.addAdvancedListener(advanced -> {
          if (isConditionPanelVisible()) {
            revalidate();
          }
        });
      }
    }
  }

  private void setConditionPanelVisibleInternal(final boolean visible) {
    if (conditionScrollPane != null) {
      conditionScrollPane.setVisible(visible);
      if (automaticallyHideRefreshToolbar) {
        refreshToolBar.setVisible(visible);
      }
      revalidate();
    }
  }

  private void setSummaryPanelVisibleInternal(final boolean visible) {
    if (summaryScrollPane != null) {
      summaryScrollPane.setVisible(visible);
      revalidate();
    }
  }

  private void initializeTable() {
    tableModel.getColumnModel().getAllColumns().forEach(this::configureColumn);
    final JTableHeader header = table.getTableHeader();
    header.setDefaultRenderer(new HeaderRenderer(header.getDefaultRenderer(), table.getFont()));
    header.setFocusable(false);
    if (includePopupMenu) {
      addTablePopupMenu();
    }
  }

  private void configureColumn(final TableColumn column) {
    final Property<?> property = tableModel.getEntityDefinition().getProperty((Attribute<?>) column.getIdentifier());
    column.setCellRenderer(initializeTableCellRenderer(property));
    column.setCellEditor(initializeTableCellEditor(property));
    column.setResizable(true);
  }

  private void addTablePopupMenu() {
    final ControlList popupControls = getPopupControls(additionalPopupControls);
    if (popupControls == null || popupControls.isEmpty()) {
      return;
    }

    final JPopupMenu popupMenu = Controls.popupMenu(popupControls);
    table.setComponentPopupMenu(popupMenu);
    table.getTableHeader().setComponentPopupMenu(popupMenu);
    if (table.getParent() != null) {
      ((JComponent) table.getParent()).setComponentPopupMenu(popupMenu);
    }
    KeyEvents.addKeyEvent(table, KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK, control(() -> {
      final Point location = getPopupLocation(table);
      popupMenu.show(table, location.x, location.y);
    }));
  }

  private void checkIfInitialized() {
    if (panelInitialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
  }

  private void addConditionControls(final ControlList popupControls) {
    final ControlList controlList = ControlList.builder()
            .name(FrameworkMessages.get(FrameworkMessages.SEARCH))
            .icon(frameworkIcons().filter()).build();
    if (this.controls.containsKey(ControlCode.CONDITION_PANEL_VISIBLE)) {
      controlList.add(getControl(ControlCode.CONDITION_PANEL_VISIBLE));
    }
    final ControlList searchPanelControls = conditionPanel.getControls();
    if (searchPanelControls != null && !searchPanelControls.isEmpty()) {
      controlList.addAll(searchPanelControls);
    }
    if (!controlList.isEmpty()) {
      popupControls.add(controlList);
    }
  }

  private void showEntityMenu() {
    final Entity selected = tableModel.getSelectionModel().getSelectedItem();
    if (selected != null) {
      final Point location = getPopupLocation(table);
      new EntityPopupMenu(tableModel.getConnectionProvider().getEntities().copyEntity(selected),
              tableModel.getConnectionProvider()).show(this, location.x, location.y);
    }
  }

  private void onConditionChanged() {
    table.getTableHeader().repaint();
    table.repaint();
  }

  private static void addAdditionalControls(final ControlList popupControls, final List<ControlList> additionalPopupControlLists) {
    additionalPopupControlLists.forEach(controlList -> {
      if (nullOrEmpty(controlList.getName())) {
        popupControls.addAll(controlList);
      }
      else {
        popupControls.add(controlList);
      }
      popupControls.addSeparator();
    });
  }

  private static void showDependenciesDialog(final Map<EntityType<?>, Collection<Entity>> dependencies,
                                             final EntityConnectionProvider connectionProvider,
                                             final JComponent dialogParent, final String title) {
    JPanel dependenciesPanel;
    try {
      showWaitCursor(dialogParent);
      dependenciesPanel = createDependenciesPanel(dependencies, connectionProvider);
    }
    finally {
      hideWaitCursor(dialogParent);
    }
    Dialogs.displayInDialog(getParentWindow(dialogParent), dependenciesPanel, title);
  }

  private static Map<TableColumn, JPanel> createColumnSummaryPanels(final AbstractFilteredTableModel<?, Attribute<?>> tableModel) {
    final Map<TableColumn, JPanel> components = new HashMap<>();
    tableModel.getColumnModel().getAllColumns().forEach(column -> {
      final ColumnSummaryModel columnSummaryModel = tableModel.getColumnSummaryModel((Attribute<?>) column.getIdentifier());
      if (columnSummaryModel != null) {
        components.put(column, new ColumnSummaryPanel(columnSummaryModel));
      }
    });

    return components;
  }

  private static JScrollPane createHiddenLinkedScrollPane(final JScrollPane masterScrollPane, final JPanel panel) {
    final JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    Components.linkBoundedRangeModels(masterScrollPane.getHorizontalScrollBar().getModel(), scrollPane.getHorizontalScrollBar().getModel());
    scrollPane.setVisible(false);

    return scrollPane;
  }

  private static JLabel initializeStatusMessageLabel() {
    final JLabel label = new JLabel("", JLabel.CENTER);
    label.setFont(new Font(label.getFont().getName(), Font.PLAIN, STATUS_MESSAGE_FONT_SIZE));

    return label;
  }

  private static JPanel createDependenciesPanel(final Map<EntityType<?>, Collection<Entity>> dependencies,
                                                final EntityConnectionProvider connectionProvider) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    for (final Map.Entry<EntityType<?>, Collection<Entity>> entry : dependencies.entrySet()) {
      final Collection<Entity> dependantEntities = entry.getValue();
      if (!dependantEntities.isEmpty()) {
        tabPane.addTab(connectionProvider.getEntities().getDefinition(entry.getKey()).getCaption(),
                createEntityTablePanel(dependantEntities, connectionProvider));
      }
    }
    panel.add(tabPane, BorderLayout.CENTER);

    return panel;
  }

  private static Point getPopupLocation(final JTable table) {
    final int x = table.getBounds().getLocation().x + POPUP_LOCATION_X_OFFSET;
    final int y = table.getSelectionModel().isSelectionEmpty() ? POPUP_LOCATION_EMPTY_SELECTION :
            (table.getSelectedRow() + 1) * table.getRowHeight();

    return new Point(x, y);
  }

  private final class HeaderRenderer implements TableCellRenderer {

    private final TableCellRenderer defaultHeaderRenderer;
    private final Font defaultFont;
    private final Font searchFont;

    public HeaderRenderer(final TableCellRenderer defaultHeaderRenderer, final Font defaultFont) {
      this.defaultHeaderRenderer = defaultHeaderRenderer;
      this.defaultFont = defaultFont;
      this.searchFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
      final JLabel label = (JLabel) defaultHeaderRenderer.getTableCellRendererComponent(table, value, isSelected,
              hasFocus, row, column);
      final TableColumn tableColumn = tableModel.getColumnModel().getColumn(column);
      final TableCellRenderer renderer = tableColumn.getCellRenderer();
      final Attribute<?> attribute = (Attribute<?>) tableColumn.getIdentifier();
      final boolean displayConditionStatus = renderer instanceof EntityTableCellRenderer
              && ((EntityTableCellRenderer) renderer).isDisplayConditionStatus()
              && tableModel.getTableConditionModel().isConditionEnabled(attribute);
      label.setFont(displayConditionStatus ? searchFont : defaultFont);

      return label;
    }
  }

  private static final class DefaultConditionPanelFactory<C extends Attribute<T>, T> implements ConditionPanelFactory<Entity, C, T> {

    private final SwingEntityTableModel tableModel;

    private DefaultConditionPanelFactory(final SwingEntityTableModel tableModel) {
      this.tableModel = requireNonNull(tableModel);
    }

    @Override
    public ColumnConditionPanel<Entity, C, T> createConditionPanel(final TableColumn column) {
      final Attribute<T> identifier = (Attribute<T>) column.getIdentifier();
      final ColumnConditionModel<Entity, Attribute<T>, T> filterModel =
              tableModel.getTableConditionModel().getFilterModel(identifier);

      return (ColumnConditionPanel<Entity, C, T>) new AttributeFilterPanel<>(filterModel);
    }
  }
}
