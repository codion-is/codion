/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Configuration;
import org.jminor.common.Conjunction;
import org.jminor.common.Util;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.ReferentialIntegrityException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.Events;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Properties;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.ValueListProperty;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.swing.common.ui.DefaultDialogExceptionHandler;
import org.jminor.swing.common.ui.DialogExceptionHandler;
import org.jminor.swing.common.ui.LocalDateInputPanel;
import org.jminor.swing.common.ui.LocalDateTimeInputPanel;
import org.jminor.swing.common.ui.LocalTimeInputPanel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.input.BigDecimalInputProvider;
import org.jminor.swing.common.ui.input.BlobInputProvider;
import org.jminor.swing.common.ui.input.BooleanInputProvider;
import org.jminor.swing.common.ui.input.DoubleInputProvider;
import org.jminor.swing.common.ui.input.InputProvider;
import org.jminor.swing.common.ui.input.InputProviderPanel;
import org.jminor.swing.common.ui.input.IntegerInputProvider;
import org.jminor.swing.common.ui.input.LongInputProvider;
import org.jminor.swing.common.ui.input.TemporalInputProvider;
import org.jminor.swing.common.ui.input.TextInputProvider;
import org.jminor.swing.common.ui.input.ValueListInputProvider;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;
import org.jminor.swing.common.ui.table.ColumnConditionPanelProvider;
import org.jminor.swing.common.ui.table.FilteredTable;
import org.jminor.swing.common.ui.table.FilteredTableSummaryPanel;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.jminor.swing.common.ui.UiUtil.getParentWindow;
import static org.jminor.swing.common.ui.UiUtil.setWaitCursor;
import static org.jminor.swing.common.ui.control.Controls.control;

/**
 * The EntityTablePanel is a UI class based on the EntityTableModel class.
 * It consists of a JTable as well as filtering/searching and summary panels.
 *
 * The default layout is as follows
 * <pre>
 *  ____________________________________________________
 * |                searchPanel                         |
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
 * The search and summary panels can be hidden
 * Note that {@link #initializePanel()} must be called to initialize this panel before displaying it.
 * @see EntityTableModel
 */
public class EntityTablePanel extends JPanel implements DialogExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(EntityTablePanel.class);

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityTablePanel.class.getName(), Locale.getDefault());

  /**
   * Specifies whether or not columns can be rearranged in tables<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> ALLOW_COLUMN_REORDERING = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityTablePanel.allowColumnReordering", true);

  /**
   * Specifies whether the table condition panels should be visible or not by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> TABLE_CONDITION_PANEL_VISIBLE = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityTablePanel.tableConditionPanelVisible", false);

  /**
   * Specifies the default table column resize mode for tables in the application<br>
   * Value type: Integer (JTable.AUTO_RESIZE_*)<br>
   * Default value: JTable.AUTO_RESIZE_OFF
   */
  public static final PropertyValue<Integer> TABLE_AUTO_RESIZE_MODE = Configuration.integerValue(
          "org.jminor.swing.framework.ui.EntityTablePanel.tableAutoResizeMode", JTable.AUTO_RESIZE_OFF);

  /**
   * Specifies whether to display the error message or the dependent entities in case of a referential integrity error on delete<br>
   * Value type: {@link ReferentialIntegrityErrorHandling}<br>
   * Default value: {@link ReferentialIntegrityErrorHandling#ERROR}
   */
  public static final PropertyValue<ReferentialIntegrityErrorHandling> REFERENTIAL_INTEGRITY_ERROR_HANDLING = Configuration.value(
          "org.jminor.swing.framework.ui.EntityTablePanel.referentialIntegrityErrorHandling", ReferentialIntegrityErrorHandling.ERROR,
          ReferentialIntegrityErrorHandling::valueOf);

  /**
   * The possible actions to take on a referential integrity error
   */
  public enum ReferentialIntegrityErrorHandling {
    /** Display the error */
    ERROR,
    /** Display the dependencies causing the error */
    DEPENDENCIES
  }

  public static final String PRINT_TABLE = "printTable";
  public static final String DELETE_SELECTED = "deleteSelected";
  public static final String VIEW_DEPENDENCIES = "viewDependencies";
  public static final String UPDATE_SELECTED = "updateSelected";
  public static final String SELECT_COLUMNS = "selectTableColumns";
  public static final String EXPORT_JSON = "exportJSON";
  public static final String CLEAR = "clear";
  public static final String REFRESH = "refresh";
  public static final String TOGGLE_SUMMARY_PANEL = "toggleSummaryPanel";
  public static final String TOGGLE_CONDITION_PANEL = "toggleConditionPanel";
  public static final String CONDITION_PANEL_VISIBLE = "conditionPanelVisible";
  public static final String CLEAR_SELECTION = "clearSelection";
  public static final String MOVE_SELECTION_UP = "moveSelectionUp";
  public static final String MOVE_SELECTION_DOWN = "moveSelectionDown";
  public static final String COPY_TABLE_DATA = "copyTableData";

  private static final Dimension TOOLBAR_BUTTON_SIZE = new Dimension(20, 20);
  private static final int STATUS_MESSAGE_FONT_SIZE = 12;
  private static final int POPUP_LOCATION_X_OFFSET = 42;
  private static final int POPUP_LOCATION_EMPTY_SELECTION = 100;
  private static final int FONT_SIZE_TO_ROW_HEIGHT = 4;

  private final Event<Boolean> conditionPanelVisibilityChangedEvent = Events.event();
  private final Event<Boolean> summaryPanelVisibleChangedEvent = Events.event();

  private final Map<String, Control> controlMap = new HashMap<>();

  private final SwingEntityTableModel tableModel;

  private final FilteredTable<Entity, Property, SwingEntityTableModel> table;

  private final JScrollPane tableScrollPane;

  private final EntityTableConditionPanel conditionPanel;

  private final JScrollPane conditionScrollPane;

  private final FilteredTableSummaryPanel summaryPanel;

  private final JScrollPane summaryScrollPane;

  /**
   * Base panel for the table, condition and summary panels
   */
  private final JPanel tablePanel = new JPanel(new BorderLayout());

  /**
   * the toolbar containing the refresh button
   */
  private final JToolBar refreshToolBar;

  /**
   * the label for showing the status of the table, that is, the number of rows, number of selected rows etc.
   */
  private final JLabel statusMessageLabel = initializeStatusMessageLabel();

  private final List<ControlSet> additionalPopupControlSets = new ArrayList<>();
  private final List<ControlSet> additionalToolBarControlSets = new ArrayList<>();

  /**
   * specifies whether to include the south panel
   */
  private boolean includeSouthPanel = true;

  /**
   * specifies whether to include the condition panel
   */
  private boolean includeConditionPanel = true;

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
  private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling = REFERENTIAL_INTEGRITY_ERROR_HANDLING.get();

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   */
  public EntityTablePanel(final SwingEntityTableModel tableModel) {
    this(tableModel, new EntityTableConditionPanel(tableModel));
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param conditionPanel the condition panel
   */
  public EntityTablePanel(final SwingEntityTableModel tableModel, final EntityTableConditionPanel conditionPanel) {
    this.tableModel = tableModel;
    this.table = initializeTable(tableModel);
    this.tableScrollPane = new JScrollPane(table);
    this.tableScrollPane.getViewport().addChangeListener(e -> {
      tableScrollPane.getHorizontalScrollBar().setVisible(tableScrollPane.getViewport().getViewSize().width > tableScrollPane.getSize().width);
      revalidate();
    });
    this.conditionPanel = conditionPanel;
    this.conditionScrollPane = conditionPanel == null ? null : createHiddenLinkedScrollPane(tableScrollPane, conditionPanel);
    this.summaryPanel = new FilteredTableSummaryPanel(tableModel);
    this.summaryScrollPane = createHiddenLinkedScrollPane(tableScrollPane, summaryPanel);
    this.tablePanel.add(tableScrollPane, BorderLayout.CENTER);
    this.tablePanel.add(summaryScrollPane, BorderLayout.SOUTH);
    this.refreshToolBar = initializeRefreshToolBar();
    bindEvents();
  }

  /**
   * @return the filtered table instance
   */
  public final FilteredTable<Entity, Property, SwingEntityTableModel> getTable() {
    return table;
  }

  /**
   * @return the EntityTableModel used by this EntityTablePanel
   */
  public final SwingEntityTableModel getTableModel() {
    return tableModel;
  }

  /**
   * @param additionalPopupControls a set of controls to add to the table popup menu
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void addPopupControls(final ControlSet additionalPopupControls) {
    checkIfInitialized();
    this.additionalPopupControlSets.add(additionalPopupControls);
  }

  /**
   * @param additionalToolBarControls a set of controls to add to the table toolbar menu
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void addToolBarControls(final ControlSet additionalToolBarControls) {
    checkIfInitialized();
    this.additionalToolBarControlSets.add(additionalToolBarControls);
  }

  /**
   * @param value true if the south panel should be included
   * @see #initializeSouthPanel()
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludeSouthPanel(final boolean value) {
    checkIfInitialized();
    this.includeSouthPanel = value;
  }

  /**
   * @param value true if the condition panel should be included
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludeConditionPanel(final boolean value) {
    checkIfInitialized();
    this.includeConditionPanel = value;
  }

  /**
   * @param value true if a popup menu should be included
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludePopupMenu(final boolean value) {
    checkIfInitialized();
    this.includePopupMenu = value;
  }

  /**
   * Hides or shows the column condition panel for this EntityTablePanel
   * @param visible if true the condition panel is shown, if false it is hidden
   */
  public final void setConditionPanelVisible(final boolean visible) {
    if (visible && isConditionPanelVisible()) {
      return;
    }

    if (conditionScrollPane != null) {
      conditionScrollPane.setVisible(visible);
      if (refreshToolBar != null) {
        refreshToolBar.setVisible(visible);
      }
      revalidate();
      conditionPanelVisibilityChangedEvent.fire(visible);
    }
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
  public final EntityTableConditionPanel getConditionPanel() {
    return conditionPanel;
  }

  /**
   * Toggles the condition panel through the states hidden, visible and in case it is a EntityTableConditionPanel, advanced
   */
  public final void toggleConditionPanel() {
    if (conditionPanel.canToggleAdvanced()) {
      if (isConditionPanelVisible()) {
        if (conditionPanel.isAdvanced()) {
          setConditionPanelVisible(false);
        }
        else {
          conditionPanel.setAdvanced(true);
        }
      }
      else {
        conditionPanel.setAdvanced(false);
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
    if (conditionPanel != null) {
      if (!isConditionPanelVisible()) {
        setConditionPanelVisible(true);
      }
      conditionPanel.selectConditionPanel();
    }
  }

  /**
   * Hides or shows the column summary panel for this EntityTablePanel
   * @param visible if true then the summary panel is shown, if false it is hidden
   */
  public final void setSummaryPanelVisible(final boolean visible) {
    if (visible && isSummaryPanelVisible()) {
      return;
    }

    summaryScrollPane.setVisible(visible);
    revalidate();
    summaryPanelVisibleChangedEvent.fire(visible);
  }

  /**
   * @return true if the column summary panel is visible, false if it is hidden
   */
  public final boolean isSummaryPanelVisible() {
    return summaryScrollPane.isVisible();
  }

  /**
   * @param referentialIntegrityErrorHandling the action to take on a referential integrity error on delete
   */
  public final void setReferentialIntegrityErrorHandling(final ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling) {
    this.referentialIntegrityErrorHandling = referentialIntegrityErrorHandling;
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + tableModel.getEntityId();
  }

  /**
   * @param controlCode the control code
   * @return the control associated with {@code controlCode}
   * @throws IllegalArgumentException in case no control is associated with the given control code
   */
  public final Control getControl(final String controlCode) {
    if (!controlMap.containsKey(controlCode)) {
      throw new IllegalArgumentException(controlCode + " control not available in panel: " + this);
    }

    return controlMap.get(controlCode);
  }

  /**
   * Creates a {@link ControlSet} containing controls for updating the value of a single property
   * for the selected entities. These controls are enabled as long as the selection is not empty
   * and {@link EntityEditModel#getUpdateEnabledObserver()} is enabled.
   * @return a control set containing a set of controls, one for each updatable property in the
   * underlying entity, for performing an update on the selected entities
   * @see #initializePanel()
   * @throws IllegalStateException in case the underlying edit model is read only or updating is not enabled
   * @see #includeUpdateSelectedProperty(Property)
   * @see EntityEditModel#getUpdateEnabledObserver()
   */
  public ControlSet getUpdateSelectedControlSet() {
    if (!includeUpdateSelectedControls()) {
      throw new IllegalStateException("Table model is read only or does not allow updates");
    }
    final StateObserver selectionNotEmpty = tableModel.getSelectionModel().getSelectionEmptyObserver().getReversedObserver();
    final StateObserver updateEnabled = tableModel.getEditModel().getUpdateEnabledObserver();
    final StateObserver enabled = States.aggregateState(Conjunction.AND, selectionNotEmpty, updateEnabled);
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED),
            (char) 0, Images.loadImage("Modify16.gif"), enabled);
    controlSet.setDescription(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP));
    Properties.sort(tableModel.getEntityDefinition().getUpdatableProperties()).forEach(property -> {
      if (includeUpdateSelectedProperty(property)) {
        final String caption = property.getCaption() == null ? property.getPropertyId() : property.getCaption();
        controlSet.add(control(() -> updateSelectedEntities(property), caption, enabled));
      }
    });

    return controlSet;
  }

  /**
   * @return a control for showing the dependencies dialog
   */
  public final Control getViewDependenciesControl() {
    return control(this::viewSelectionDependencies,
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES) + "...",
            tableModel.getSelectionModel().getSelectionEmptyObserver().getReversedObserver(),
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES_TIP), 'W');
  }

  /**
   * @return a control for deleting the selected entities
   * @throws IllegalStateException in case the underlying model is read only or if deleting is not enabled
   */
  public final Control getDeleteSelectedControl() {
    if (!includeDeleteSelectedControl()) {
      throw new IllegalStateException("Table model is read only or does not allow delete");
    }
    return control(this::delete, FrameworkMessages.get(FrameworkMessages.DELETE),
            States.aggregateState(Conjunction.AND,
                    tableModel.getEditModel().getDeleteEnabledObserver(),
                    tableModel.getSelectionModel().getSelectionEmptyObserver().getReversedObserver()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP), 0, null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for printing the table
   */
  public final Control getPrintTableControl() {
    final String printCaption = MESSAGES.getString("print_table");
    return control(this::printTable, printCaption, null,
            printCaption, printCaption.charAt(0), null, Images.loadImage("Print16.gif"));
  }

  /**
   * @return a Control for refreshing the underlying table data
   */
  public final Control getRefreshControl() {
    final String refreshCaption = FrameworkMessages.get(FrameworkMessages.REFRESH);
    return control(tableModel::refresh, refreshCaption,
            null, FrameworkMessages.get(FrameworkMessages.REFRESH_TIP), refreshCaption.charAt(0),
            null, Images.loadImage(Images.IMG_REFRESH_16));
  }

  /**
   * @return a Control for clearing the underlying table model, that is, removing all rows
   */
  public final Control getClearControl() {
    final String clearCaption = FrameworkMessages.get(FrameworkMessages.CLEAR);
    return control(tableModel::clear, clearCaption,
            null, null, clearCaption.charAt(0), null, null);
  }

  /**
   * Retrieves a new property value via input dialog and performs an update on the selected entities
   * @param propertyToUpdate the property to update
   * @see #getInputProvider(Property, java.util.List)
   */
  public final void updateSelectedEntities(final Property propertyToUpdate) {
    if (tableModel.getSelectionModel().isSelectionEmpty()) {
      return;
    }

    final List<Entity> selectedEntities = tableModel.getDomain().deepCopyEntities(tableModel.getSelectionModel().getSelectedItems());
    final InputProviderPanel inputPanel = new InputProviderPanel(propertyToUpdate.getCaption(),
            getInputProvider(propertyToUpdate, selectedEntities));
    UiUtil.displayInDialog(this, inputPanel, FrameworkMessages.get(FrameworkMessages.SET_PROPERTY_VALUE), true,
            inputPanel.getOkButton(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted()) {
      Entities.put(propertyToUpdate.getPropertyId(), inputPanel.getValue(), selectedEntities);
      try {
        setWaitCursor(true, this);
        tableModel.update(selectedEntities);
      }
      catch (final Exception e) {
        handleException(e);
      }
      finally {
        setWaitCursor(false, this);
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
      setWaitCursor(true, this);
      final Map<String, Collection<Entity>> dependencies =
              tableModel.getConnectionProvider().getConnection()
                      .selectDependencies(tableModel.getSelectionModel().getSelectedItems());
      if (!dependencies.isEmpty()) {
        showDependenciesDialog(dependencies, tableModel.getConnectionProvider(), this, MESSAGES.getString("dependent_records_found"));
      }
      else {
        JOptionPane.showMessageDialog(this, MESSAGES.getString("none_found"),
                MESSAGES.getString("no_dependent_records"), JOptionPane.INFORMATION_MESSAGE);
      }
    }
    catch (final Exception e) {
      handleException(e);
    }
    finally {
      setWaitCursor(false, this);
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
          setWaitCursor(true, this);
          tableModel.deleteSelected();
        }
        finally {
          setWaitCursor(false, this);
        }
      }
    }
    catch (final ReferentialIntegrityException e) {
      if (referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DEPENDENCIES) {
        showDependenciesDialog(tableModel.getSelectionModel().getSelectedItems(),
                tableModel.getConnectionProvider(), this);
      }
      else {
        handleException(e);
      }
    }
    catch (final Exception e) {
      handleException(e);
    }
  }

  /**
   * Prints the table
   * @see JTable#print()
   * @throws java.awt.print.PrinterException in case of a print exception
   */
  public final void printTable() throws PrinterException {
    getTable().print();
  }

  /**
   * Handles the given exception, which usually means simply displaying it to the user
   * @param throwable the exception to handle
   */
  public final void handleException(final Throwable throwable) {
    LOG.error(throwable.getMessage(), throwable);
    if (throwable instanceof ValidationException) {
      handleException((ValidationException) throwable);
    }
    else if (throwable instanceof DatabaseException) {
      handleException((DatabaseException) throwable);
    }
    else {
      displayException(throwable, getParentWindow(this));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void displayException(final Throwable throwable, final Window dialogParent) {
    DefaultDialogExceptionHandler.getInstance().displayException(throwable, dialogParent);
  }

  /**
   * Initializes the button used to toggle the condition panel state (hidden, visible and advanced)
   * @return a condition panel toggle button
   */
  public final Control getToggleConditionPanelControl() {
    if (!includeConditionPanel) {
      return null;
    }

    final Control toggleControl = control(this::toggleConditionPanel, Images.loadImage(Images.IMG_FILTER_16));
    toggleControl.setDescription(MESSAGES.getString("show_condition_panel"));

    return toggleControl;
  }

  /**
   * Initializes the button used to toggle the summary panel state (hidden and visible)
   * @return a summary panel toggle button
   */
  public final Control getToggleSummaryPanelControl() {
    final Control toggleControl = Controls.toggleControl(this, "summaryPanelVisible", null,
            summaryPanelVisibleChangedEvent);
    toggleControl.setIcon(Images.loadImage("Sum16.gif"));
    toggleControl.setDescription(MESSAGES.getString("toggle_summary_tip"));

    return toggleControl;
  }

  /**
   * @return a control for clearing the table selection
   */
  public final Control getClearSelectionControl() {
    final Control clearSelection = control(tableModel.getSelectionModel()::clearSelection, null,
            tableModel.getSelectionModel().getSelectionEmptyObserver().getReversedObserver(), null, -1, null,
            Images.loadImage("ClearSelection16.gif"));
    clearSelection.setDescription(MESSAGES.getString("clear_selection_tip"));

    return clearSelection;
  }

  /**
   * @return a control for moving the table selection down one index
   */
  public final Control getMoveSelectionDownControl() {
    final Control selectionDown = control(tableModel.getSelectionModel()::moveSelectionDown,
            Images.loadImage(Images.IMG_DOWN_16));
    selectionDown.setDescription(MESSAGES.getString("selection_down_tip"));

    return selectionDown;
  }

  /**
   * @return a control for moving the table selection up one index
   */
  public final Control getMoveSelectionUpControl() {
    final Control selectionUp = control(tableModel.getSelectionModel()::moveSelectionUp,
            Images.loadImage(Images.IMG_UP_16));
    selectionUp.setDescription(MESSAGES.getString("selection_up_tip"));

    return selectionUp;
  }

  /**
   * @param listener a listener notified each time the condition panel visibility changes
   */
  public final void addConditionPanelVisibleListener(final EventDataListener<Boolean> listener) {
    conditionPanelVisibilityChangedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeConditionPanelVisibleListener(final EventDataListener listener) {
    conditionPanelVisibilityChangedEvent.removeDataListener(listener);
  }

  /**
   * Displays a dialog with the entities depending on the given entities.
   * @param entities the entities for which to display dependencies
   * @param connectionProvider the connection provider
   * @param dialogParent the dialog parent
   */
  public static void showDependenciesDialog(final Collection<Entity> entities, final EntityConnectionProvider connectionProvider,
                                            final JComponent dialogParent) {
    try {
      final Map<String, Collection<Entity>> dependencies = connectionProvider.getConnection().selectDependencies(entities);
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
  public static EntityTablePanel createStaticEntityTablePanel(final Collection<Entity> entities,
                                                              final EntityConnectionProvider connectionProvider) {
    if (Util.nullOrEmpty(entities)) {
      throw new IllegalArgumentException("Cannot create a static EntityTablePanel without the entities");
    }

    final SwingEntityTableModel tableModel = new SwingEntityTableModel(entities.iterator().next().getEntityId(), connectionProvider) {
      @Override
      protected List<Entity> performQuery() {
        return new ArrayList<>(entities);
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
      throw new IllegalArgumentException("Cannot create a static EntityTablePanel without the entities");
    }

    final String entityId = entities.iterator().next().getEntityId();
    final SwingEntityEditModel editModel = new SwingEntityEditModel(entityId, connectionProvider);
    final SwingEntityTableModel tableModel = new SwingEntityTableModel(entityId, connectionProvider) {
      @Override
      protected List<Entity> performQuery() {
        return new ArrayList<>(entities);
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
      protected ControlSet getPopupControls(final List<ControlSet> additionalPopupControlSets) {
        return additionalPopupControlSets.get(0);
      }
    };
    final ControlSet popupControls = new ControlSet();
    if (tablePanel.includeUpdateSelectedControls()) {
      popupControls.add(tablePanel.getUpdateSelectedControlSet());
    }
    if (tablePanel.includeDeleteSelectedControl()) {
      popupControls.add(tablePanel.getDeleteSelectedControl());
    }
    if (popupControls.size() > 0) {
      popupControls.addSeparator();
    }
    popupControls.add(tablePanel.getViewDependenciesControl());
    tablePanel.addPopupControls(popupControls);
    tablePanel.setIncludeConditionPanel(false);
    tablePanel.setIncludeSouthPanel(false);
    tablePanel.initializePanel();

    return tablePanel;
  }

  /**
   * Displays a entity table in a dialog for selecting one or more entities
   * @param lookupModel the table model on which to base the table panel
   * @param dialogOwner the dialog owner
   * @param singleSelection if true then only a single item can be selected
   * @param dialogTitle the dialog title
   * @return a Collection containing the selected entities
   * @throws CancelException in case the user cancels the operation
   */
  public static Collection<Entity> selectEntities(final SwingEntityTableModel lookupModel, final Container dialogOwner,
                                                  final boolean singleSelection, final String dialogTitle) {
    return selectEntities(lookupModel, dialogOwner, singleSelection, dialogTitle, null);
  }

  /**
   * Displays a entity table in a dialog for selecting one or more entities
   * @param lookupModel the table model on which to base the table panel
   * @param dialogOwner the dialog owner
   * @param singleSelection if true then only a single item can be selected
   * @param dialogTitle the dialog title
   * @param preferredSize the preferred size of the dialog
   * @return a Collection containing the selected entities
   * @throws CancelException in case the user cancels the operation
   */
  public static Collection<Entity> selectEntities(final SwingEntityTableModel lookupModel, final Container dialogOwner,
                                                  final boolean singleSelection, final String dialogTitle,
                                                  final Dimension preferredSize) {
    requireNonNull(lookupModel, "lookupModel");
    final Collection<Entity> selected = new ArrayList<>();
    final JDialog dialog = new JDialog(dialogOwner instanceof Window ? (Window) dialogOwner : getParentWindow(dialogOwner), dialogTitle);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final Control okControl = control(() -> {
      selected.addAll(lookupModel.getSelectionModel().getSelectedItems());
      dialog.dispose();
    }, Messages.get(Messages.OK), null, null, Messages.get(Messages.OK_MNEMONIC).charAt(0));
    final Control cancelControl = control(() -> {
      selected.add(null);//hack to indicate cancel
      dialog.dispose();
    }, Messages.get(Messages.CANCEL), null, null, Messages.get(Messages.CANCEL_MNEMONIC).charAt(0));

    final SwingEntityModel model = new SwingEntityModel(lookupModel);
    model.getEditModel().setReadOnly(true);
    final EntityTablePanel entityTablePanel = new EntityTablePanel(lookupModel);
    entityTablePanel.initializePanel();
    entityTablePanel.getTable().addDoubleClickListener(mouseEvent -> {
      if (!lookupModel.getSelectionModel().isSelectionEmpty()) {
        okControl.actionPerformed(null);
      }
    });
    entityTablePanel.setConditionPanelVisible(true);
    if (singleSelection) {
      entityTablePanel.getTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    final Control searchControl = control(() -> {
      lookupModel.refresh();
      if (lookupModel.getRowCount() > 0) {
        lookupModel.getSelectionModel().setSelectedIndexes(singletonList(0));
        entityTablePanel.getTable().requestFocusInWindow();
      }
      else {
        JOptionPane.showMessageDialog(getParentWindow(entityTablePanel),
                FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CONDITION));
      }
    }, FrameworkMessages.get(FrameworkMessages.SEARCH), null, null, FrameworkMessages.get(FrameworkMessages.SEARCH_MNEMONIC).charAt(0));

    final JButton okButton = new JButton(okControl);
    final JButton cancelButton = new JButton(cancelControl);
    final JButton searchButton = new JButton(searchControl);
    UiUtil.addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, 0, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, cancelControl);
    entityTablePanel.getTable().getInputMap(
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    dialog.setLayout(new BorderLayout());
    if (preferredSize != null) {
      entityTablePanel.setPreferredSize(preferredSize);
    }
    dialog.add(entityTablePanel, BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(UiUtil.createFlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(searchButton);
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);
    dialog.getRootPane().setDefaultButton(okButton);
    dialog.add(buttonPanel, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(dialogOwner);
    dialog.setModal(true);
    dialog.setResizable(true);
    dialog.setVisible(true);

    if (selected.isEmpty() || (selected.size() == 1 && selected.contains(null))) {
      throw new CancelException();
    }
    else {
      return selected;
    }
  }

  /**
   * Initializes the UI, while presenting a wait cursor to the user.
   * Note that calling this method more than once has no effect.
   * @return this EntityTablePanel instance
   */
  public final EntityTablePanel initializePanel() {
    if (!panelInitialized) {
      try {
        setWaitCursor(true, this);
        setupControls();
        initializeTable();
        initializeUI();
        bindPanelEvents();
        updateStatusMessage();
      }
      finally {
        panelInitialized = true;
        setWaitCursor(false, this);
      }
    }

    return this;
  }

  /**
   * Initializes the south panel, override and return null for no south panel.
   * @return the south panel, or null if no south panel should be used
   */
  protected JPanel initializeSouthPanel() {
    final JPanel centerPanel = new JPanel(UiUtil.createBorderLayout());
    final JTextField searchField = table.getSearchField();
    final JPanel searchFieldPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    searchFieldPanel.add(searchField);
    centerPanel.add(statusMessageLabel, BorderLayout.CENTER);
    centerPanel.add(searchFieldPanel, BorderLayout.WEST);
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(centerPanel, BorderLayout.CENTER);
    panel.setBorder(BorderFactory.createEtchedBorder());
    if (refreshToolBar != null) {
      panel.add(refreshToolBar, BorderLayout.WEST);
    }

    return panel;
  }

  /**
   * Adds a popup menu to {@code table}, null or an empty ControlSet mean no popup menu
   * @param table the table
   * @param popupControls a ControlSet specifying the controls in the popup menu
   */
  protected final void setTablePopupMenu(final JTable table, final ControlSet popupControls) {
    if (popupControls == null || popupControls.size() == 0) {
      return;
    }

    final JPopupMenu popupMenu = ControlProvider.createPopupMenu(popupControls);
    table.setComponentPopupMenu(popupMenu);
    table.getTableHeader().setComponentPopupMenu(popupMenu);
    if (table.getParent() != null) {
      ((JComponent) table.getParent()).setComponentPopupMenu(popupMenu);
    }
    UiUtil.addKeyEvent(table, KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK, control(() -> {
      final Point location = getPopupLocation(table);
      popupMenu.show(table, location.x, location.y);
    }, "EntityTablePanel.showPopupMenu"));
    UiUtil.addKeyEvent(table, KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            control(() -> EntityUiUtil.showEntityMenu(tableModel.getSelectionModel().getSelectedItem(),
                    EntityTablePanel.this, getPopupLocation(table), tableModel.getConnectionProvider()),
                    "EntityTablePanel.showEntityMenu"));
  }

  /**
   * Associates {@code control} with {@code controlCode}
   * @param controlCode the control code
   * @param control the control to associate with {@code controlCode}
   */
  protected final void setControl(final String controlCode, final Control control) {
    if (control == null) {
      controlMap.remove(controlCode);
    }
    else {
      controlMap.put(controlCode, control);
    }
  }

  protected ControlSet getToolBarControls(final List<ControlSet> additionalToolBarControlSets) {
    final ControlSet toolbarControls = new ControlSet("");
    if (controlMap.containsKey(TOGGLE_SUMMARY_PANEL)) {
      toolbarControls.add(controlMap.get(TOGGLE_SUMMARY_PANEL));
    }
    if (controlMap.containsKey(TOGGLE_CONDITION_PANEL)) {
      toolbarControls.add(controlMap.get(TOGGLE_CONDITION_PANEL));
      toolbarControls.addSeparator();
    }
    if (controlMap.containsKey(DELETE_SELECTED)) {
      toolbarControls.add(controlMap.get(DELETE_SELECTED));
    }
    toolbarControls.add(getPrintTableControl());
    toolbarControls.add(controlMap.get(CLEAR_SELECTION));
    toolbarControls.addSeparator();
    toolbarControls.add(controlMap.get(MOVE_SELECTION_UP));
    toolbarControls.add(controlMap.get(MOVE_SELECTION_DOWN));
    additionalToolBarControlSets.forEach(controlSet -> {
      toolbarControls.addSeparator();
      for (final Action action : controlSet.getActions()) {
        toolbarControls.add(action);
      }
    });

    return toolbarControls;
  }

  /**
   * Constructs a ControlSet containing the controls to include in the table popup menu.
   * Returns null or an empty ControlSet to indicate that no popup menu should be included.
   * @param additionalPopupControlSets any additional controls to include in the popup menu
   * @return the ControlSet on which to base the table popup menu, null or an empty ControlSet
   * if no popup menu should be included
   */
  protected ControlSet getPopupControls(final List<ControlSet> additionalPopupControlSets) {
    final ControlSet popupControls = new ControlSet("");
    popupControls.add(controlMap.get(REFRESH));
    popupControls.add(controlMap.get(CLEAR));
    popupControls.addSeparator();
    addAdditionalControls(popupControls, additionalPopupControlSets);
    boolean separatorRequired = false;
    if (controlMap.containsKey(UPDATE_SELECTED)) {
      popupControls.add(controlMap.get(UPDATE_SELECTED));
      separatorRequired = true;
    }
    if (controlMap.containsKey(DELETE_SELECTED)) {
      popupControls.add(controlMap.get(DELETE_SELECTED));
      separatorRequired = true;
    }
    if (controlMap.containsKey(EXPORT_JSON)) {
      popupControls.add(controlMap.get(EXPORT_JSON));
      separatorRequired = true;
    }
    if (separatorRequired) {
      popupControls.addSeparator();
      separatorRequired = false;
    }
    if (controlMap.containsKey(VIEW_DEPENDENCIES)) {
      popupControls.add(controlMap.get(VIEW_DEPENDENCIES));
      separatorRequired = true;
    }
    if (separatorRequired) {
      popupControls.addSeparator();
      separatorRequired = false;
    }
    final ControlSet printControls = getPrintControls();
    if (printControls != null) {
      popupControls.add(printControls);
      separatorRequired = true;
    }
    if (controlMap.containsKey(SELECT_COLUMNS)) {
      if (separatorRequired) {
        popupControls.addSeparator();
      }
      popupControls.add(controlMap.get(SELECT_COLUMNS));
    }
    if (includeConditionPanel) {
      if (separatorRequired) {
        popupControls.addSeparator();
      }
      addConditionControls(popupControls);
      separatorRequired = true;
    }
    if (separatorRequired) {
      popupControls.addSeparator();
    }
    popupControls.add(controlMap.get(COPY_TABLE_DATA));

    return popupControls;
  }

  protected ControlSet getPrintControls() {
    final String printCaption = Messages.get(Messages.PRINT);
    final ControlSet printControls = new ControlSet(printCaption, printCaption.charAt(0), Images.loadImage("Print16.gif"));
    printControls.add(controlMap.get(PRINT_TABLE));

    return printControls;
  }

  protected final Control getConditionPanelControl() {
    return Controls.toggleControl(this, CONDITION_PANEL_VISIBLE,
            FrameworkMessages.get(FrameworkMessages.SHOW), conditionPanelVisibilityChangedEvent);
  }

  protected final ControlSet getCopyControlSet() {
    return new ControlSet(Messages.get(Messages.COPY), getCopyCellControl(), getCopyTableWithHeaderControl());
  }

  protected final Control getCopyCellControl() {
    return control(this::copySelectedCell, FrameworkMessages.get(FrameworkMessages.COPY_CELL),
            tableModel.getSelectionModel().getSelectionEmptyObserver().getReversedObserver());
  }

  protected final Control getCopyTableWithHeaderControl() {
    return control(this::copyTableAsDelimitedString, FrameworkMessages.get(FrameworkMessages.COPY_TABLE_WITH_HEADER));
  }

  /**
   * Override to exclude properties from the update selected menu.
   * @param property the property
   * @return true if the given property should be included in the update selected menu.
   * @see #getUpdateSelectedControlSet()
   */
  protected boolean includeUpdateSelectedProperty(final Property property) {
    return true;
  }

  /**
   * Handles ValidationExceptions.
   * By default displays the exception message to the user.
   * @param exception the exception to handle
   */
  protected void handleException(final ValidationException exception) {
    displayException(exception, getParentWindow(this));
  }

  /**
   * Handles DatabaseExceptions
   * By default displays the exception message to the user.
   * @param exception the exception to handle
   */
  protected void handleException(final DatabaseException exception) {
    displayException(exception, getParentWindow(this));
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
   * Provides value input components for multiple entity update, override to supply
   * specific InputValueProvider implementations for properties.
   * Remember to return with a call to super.getInputProvider() after handling your case.
   * @param property the property for which to get the InputProvider
   * @param toUpdate the entities that are about to be updated
   * @return the InputProvider handling input for {@code property}
   * @see #updateSelectedEntities(Property)
   */
  protected InputProvider getInputProvider(final Property property, final List<Entity> toUpdate) {
    final Collection values = Entities.getDistinctValues(property.getPropertyId(), toUpdate);
    final Object currentValue = values.size() == 1 ? values.iterator().next() : null;
    if (property instanceof ForeignKeyProperty) {
      return createEntityInputProvider((ForeignKeyProperty) property, (Entity) currentValue, tableModel.getEditModel());
    }
    if (property instanceof ValueListProperty) {
      return new ValueListInputProvider(currentValue, ((ValueListProperty) property).getValues());
    }
    switch (property.getType()) {
      case Types.BOOLEAN:
        return new BooleanInputProvider((Boolean) currentValue);
      case Types.DATE:
        return new TemporalInputProvider<>(new LocalDateInputPanel((LocalDate) currentValue, property.getDateTimeFormatPattern()));
      case Types.TIMESTAMP:
        return new TemporalInputProvider<>(new LocalDateTimeInputPanel((LocalDateTime) currentValue, property.getDateTimeFormatPattern()));
      case Types.TIME:
        return new TemporalInputProvider<>(new LocalTimeInputPanel((LocalTime) currentValue, property.getDateTimeFormatPattern()));
      case Types.DOUBLE:
        return new DoubleInputProvider((Double) currentValue);
      case Types.DECIMAL:
        return new BigDecimalInputProvider((BigDecimal) currentValue);
      case Types.INTEGER:
        return new IntegerInputProvider((Integer) currentValue);
      case Types.BIGINT:
        return new LongInputProvider((Long) currentValue);
      case Types.CHAR:
        return new TextInputProvider(property.getCaption(), (String) currentValue, 1);
      case Types.VARCHAR:
        return new TextInputProvider(property.getCaption(), (String) currentValue, property.getMaxLength());
      case Types.BLOB:
        return new BlobInputProvider();
      default:
        throw new IllegalArgumentException("No InputProvider implementation available for property: " + property + " (type: " + property.getType() + ")");
    }
  }

  /**
   * Creates a InputProvider for the given foreign key property
   * @param foreignKeyProperty the property
   * @param currentValue the current value to initialize the InputProvider with
   * @param editModel the edit model involved in the updating
   * @return a Entity InputProvider
   */
  protected final InputProvider createEntityInputProvider(final ForeignKeyProperty foreignKeyProperty,
                                                          final Entity currentValue,
                                                          final SwingEntityEditModel editModel) {
    if (tableModel.getConnectionProvider().getDomain().getDefinition(foreignKeyProperty.getForeignEntityId()).isSmallDataset()) {
      return new EntityComboBoxInputProvider(editModel.createForeignKeyComboBoxModel(foreignKeyProperty), currentValue);
    }

    return new EntityLookupFieldInputProvider(editModel.createForeignKeyLookupModel(foreignKeyProperty), currentValue);
  }

  /**
   * Returns the TableCellRenderer used for the given property in this EntityTablePanel
   * @param property the property
   * @return the TableCellRenderer for the given property
   */
  protected TableCellRenderer initializeTableCellRenderer(final Property property) {
    return EntityTableCellRenderers.createTableCellRenderer(tableModel, property);
  }

  /**
   * Creates a TableCellEditor for the given property, returns null if no editor is available
   * @param property the property
   * @return a TableCellEditor for the given property
   */
  protected TableCellEditor initializeTableCellEditor(final Property property) {
    if (property.isReadOnly()) {
      return null;
    }

    if (property instanceof ForeignKeyProperty) {
      return new ForeignKeyTableCellEditor(tableModel.getConnectionProvider(), (ForeignKeyProperty) property);
    }

    return new EntityTableCellEditor(property);
  }

  /**
   * This method simply adds the given {@code southPanel} to the {@code BorderLayout.SOUTH} location and the
   * {@code tablePanel} at location BorderLayout.CENTER.
   * By overriding this method you can override the default layout.
   * @param tablePanel the panel containing the table, condition and summary panel
   * @param southPanel the south toolbar panel
   */
  protected void layoutPanel(final JPanel tablePanel, final JPanel southPanel) {
    setLayout(new BorderLayout());
    add(tablePanel, BorderLayout.CENTER);
    if (southPanel != null) {
      add(southPanel, BorderLayout.SOUTH);
    }
  }

  /**
   * Initializes the south panel toolbar, by default based on {@code getToolBarControls()}
   * @return the toolbar to add to the south panel
   */
  protected JToolBar initializeSouthToolBar() {
    final ControlSet toolbarControlSet = getToolBarControls(additionalToolBarControlSets);
    if (toolbarControlSet != null) {
      final JToolBar southToolBar = ControlProvider.createToolBar(toolbarControlSet, JToolBar.HORIZONTAL);
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
      setControl(DELETE_SELECTED, getDeleteSelectedControl());
    }
    if (includeUpdateSelectedControls()) {
      setControl(UPDATE_SELECTED, getUpdateSelectedControlSet());
    }
    if (includeConditionPanel) {
      setControl(CONDITION_PANEL_VISIBLE, getConditionPanelControl());
    }
    setControl(CLEAR, getClearControl());
    setControl(REFRESH, getRefreshControl());
    setControl(SELECT_COLUMNS, getTable().getSelectColumnsControl());
    setControl(VIEW_DEPENDENCIES, getViewDependenciesControl());
    setControl(TOGGLE_SUMMARY_PANEL, getToggleSummaryPanelControl());
    if (includeConditionPanel && conditionPanel != null) {
      setControl(TOGGLE_CONDITION_PANEL, getToggleConditionPanelControl());
    }
    setControl(PRINT_TABLE, getPrintTableControl());
    setControl(CLEAR_SELECTION, getClearSelectionControl());
    setControl(MOVE_SELECTION_UP, getMoveSelectionDownControl());
    setControl(MOVE_SELECTION_DOWN, getMoveSelectionUpControl());
    setControl(COPY_TABLE_DATA, getCopyControlSet());
  }

  private void copySelectedCell() {
    final Object value = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn());
    UiUtil.setClipboard(value == null ? "" : value.toString());
  }

  private void copyTableAsDelimitedString() {
    UiUtil.setClipboard(tableModel.getTableDataAsDelimitedString('\t'));
  }

  private boolean includeUpdateSelectedControls() {
    final SwingEntityTableModel entityTableModel = tableModel;

    return !entityTableModel.isReadOnly() && entityTableModel.isUpdateEnabled() &&
            entityTableModel.isBatchUpdateEnabled() &&
            !entityTableModel.getEntityDefinition().getUpdatableProperties().isEmpty();
  }

  private boolean includeDeleteSelectedControl() {
    final SwingEntityTableModel entityTableModel = tableModel;

    return !entityTableModel.isReadOnly() && entityTableModel.isDeleteEnabled();
  }

  private void initializeUI() {
    if (includeConditionPanel && conditionScrollPane != null) {
      tablePanel.add(conditionScrollPane, BorderLayout.NORTH);
      if (conditionPanel.canToggleAdvanced()) {
        conditionPanel.addAdvancedListener(data -> {
          if (isConditionPanelVisible()) {
            revalidate();
          }
        });
      }
    }
    JPanel southPanel = null;
    if (includeSouthPanel) {
      southPanel = new JPanel(UiUtil.createBorderLayout());
      final JPanel southPanelCenter = initializeSouthPanel();
      if (southPanelCenter != null) {
        final JToolBar southToolBar = initializeSouthToolBar();
        if (southToolBar != null) {
          southPanelCenter.add(southToolBar, BorderLayout.EAST);
        }
        southPanel.add(southPanelCenter, BorderLayout.SOUTH);
      }
    }
    layoutPanel(tablePanel, southPanel);
  }

  /**
   * @return the refresh toolbar
   */
  private JToolBar initializeRefreshToolBar() {
    final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    final String keyName = keyStroke.toString().replace("pressed ", "");
    final Control refresh = control(tableModel::refresh, null,
            tableModel.getConditionModel().getConditionStateObserver(), FrameworkMessages.get(FrameworkMessages.REFRESH_TIP)
                    + " (" + keyName + ")", 0, null, Images.loadImage(Images.IMG_STOP_16));

    final InputMap inputMap = table.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    final ActionMap actionMap = table.getActionMap();//todo

    inputMap.put(keyStroke, "EntityTablePanel.refreshControl");
    actionMap.put("EntityTablePanel.refreshControl", refresh);

    final JButton button = new JButton(refresh);
    button.setPreferredSize(TOOLBAR_BUTTON_SIZE);
    button.setFocusable(false);

    final JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
    toolBar.setFocusable(false);
    toolBar.setFloatable(false);
    toolBar.setRollover(true);

    toolBar.add(button);

    return toolBar;
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
    table.getModel().addSortListener(table.getTableHeader()::repaint);
    table.getModel().addRefreshStartedListener(() -> UiUtil.setWaitCursor(true, EntityTablePanel.this));
    table.getModel().addRefreshDoneListener(() -> UiUtil.setWaitCursor(false, EntityTablePanel.this));
  }

  private void bindPanelEvents() {
    if (includeDeleteSelectedControl()) {
      UiUtil.addKeyEvent(getTable(), KeyEvent.VK_DELETE, getDeleteSelectedControl());
    }
    final EventListener statusListener = () -> SwingUtilities.invokeLater(EntityTablePanel.this::updateStatusMessage);
    tableModel.getSelectionModel().addSelectionChangedListener(statusListener);
    tableModel.addFilteringListener(statusListener);
    tableModel.addTableDataChangedListener(statusListener);

    tableModel.getConditionModel().getPropertyConditionModels().forEach(conditionModel ->
            conditionModel.addConditionStateListener(() -> SwingUtilities.invokeLater(() -> {
              getTable().getTableHeader().repaint();
              getTable().repaint();
            })));
    if (conditionPanel != null) {
      conditionPanel.addFocusGainedListener(table::scrollToColumn);
    }
    if (tableModel.hasEditModel()) {
      tableModel.getEditModel().addEntitiesChangedListener(() -> SwingUtilities.invokeLater(getTable()::repaint));
    }
  }

  private void initializeTable() {
    table.putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);
    tableModel.getColumnModel().getAllColumns().forEach(column -> {
      final Property property = (Property) column.getIdentifier();
      column.setCellRenderer(initializeTableCellRenderer(property));
      column.setCellEditor(initializeTableCellEditor(property));
      column.setResizable(true);
    });
    final JTableHeader header = getTable().getTableHeader();
    final TableCellRenderer defaultHeaderRenderer = header.getDefaultRenderer();
    final Font defaultFont = getTable().getFont();
    final Font searchFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
    header.setDefaultRenderer((table, value, isSelected, hasFocus, row, column) -> {
      final JLabel label = (JLabel) defaultHeaderRenderer.getTableCellRendererComponent(table, value, isSelected,
              hasFocus, row, column);
      final TableColumn tableColumn = tableModel.getColumnModel().getColumn(column);
      final TableCellRenderer renderer = tableColumn.getCellRenderer();
      final Property property = (Property) tableColumn.getIdentifier();
      final boolean indicateSearch = renderer instanceof EntityTableCellRenderer
              && ((EntityTableCellRenderer) renderer).isIndicateCondition()
              && tableModel.getConditionModel().isEnabled(property.getPropertyId());
      label.setFont(indicateSearch ? searchFont : defaultFont);

      return label;
    });
    header.setFocusable(false);
    if (includePopupMenu) {
      setTablePopupMenu(getTable(), getPopupControls(additionalPopupControlSets));
    }
  }

  private void checkIfInitialized() {
    if (panelInitialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
  }

  private void addConditionControls(final ControlSet popupControls) {
    if (conditionPanel != null) {
      final ControlSet controls = new ControlSet(FrameworkMessages.get(FrameworkMessages.SEARCH));
      if (controlMap.containsKey(CONDITION_PANEL_VISIBLE)) {
        controls.add(getControl(CONDITION_PANEL_VISIBLE));
      }
      final ControlSet searchPanelControls = conditionPanel.getControls();
      if (searchPanelControls != null) {
        controls.addAll(searchPanelControls);
      }
      if (controls.size() > 0) {
        popupControls.add(controls);
      }
    }
  }

  private static void addAdditionalControls(final ControlSet popupControls, final List<ControlSet> additionalPopupControlSets) {
    additionalPopupControlSets.forEach(controlSet -> {
      if (controlSet.hasName()) {
        popupControls.add(controlSet);
      }
      else {
        popupControls.addAll(controlSet);
      }
      popupControls.addSeparator();
    });
  }

  private static void showDependenciesDialog(final Map<String, Collection<Entity>> dependencies,
                                             final EntityConnectionProvider connectionProvider,
                                             final JComponent dialogParent, final String title) {
    JPanel dependenciesPanel;
    try {
      setWaitCursor(true, dialogParent);
      dependenciesPanel = createDependenciesPanel(dependencies, connectionProvider);
    }
    finally {
      setWaitCursor(false, dialogParent);
    }
    UiUtil.displayInDialog(getParentWindow(dialogParent), dependenciesPanel, title);
  }

  private static FilteredTable<Entity, Property, SwingEntityTableModel> initializeTable(final SwingEntityTableModel tableModel) {
    final FilteredTable<Entity, Property, SwingEntityTableModel> filteredTable =
            new FilteredTable<>(tableModel, new DefaultColumnConditionPanelProvider(tableModel));
    filteredTable.setAutoResizeMode(TABLE_AUTO_RESIZE_MODE.get());
    filteredTable.getTableHeader().setReorderingAllowed(ALLOW_COLUMN_REORDERING.get());
    filteredTable.setRowHeight(filteredTable.getFont().getSize() + FONT_SIZE_TO_ROW_HEIGHT);

    return filteredTable;
  }

  private static JScrollPane createHiddenLinkedScrollPane(final JScrollPane masterScrollPane, final JPanel panel) {
    final JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    UiUtil.linkBoundedRangeModels(masterScrollPane.getHorizontalScrollBar().getModel(), scrollPane.getHorizontalScrollBar().getModel());
    scrollPane.setVisible(false);

    return scrollPane;
  }

  private static JLabel initializeStatusMessageLabel() {
    final JLabel label = new JLabel("", JLabel.CENTER);
    label.setFont(new Font(label.getFont().getName(), Font.PLAIN, STATUS_MESSAGE_FONT_SIZE));

    return label;
  }

  private static JPanel createDependenciesPanel(final Map<String, Collection<Entity>> dependencies,
                                                final EntityConnectionProvider connectionProvider) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    for (final Map.Entry<String, Collection<Entity>> entry : dependencies.entrySet()) {
      final Collection<Entity> dependantEntities = entry.getValue();
      if (!dependantEntities.isEmpty()) {
        tabPane.addTab(connectionProvider.getDomain().getDefinition(entry.getKey()).getCaption(),
                createEntityTablePanel(dependantEntities, connectionProvider));
      }
    }
    panel.add(tabPane, BorderLayout.CENTER);

    return panel;
  }

  private static Point getPopupLocation(final JTable table) {
    final int x = table.getBounds().getLocation().x + POPUP_LOCATION_X_OFFSET;
    final int y = table.getSelectionModel().isSelectionEmpty() ? POPUP_LOCATION_EMPTY_SELECTION : (table.getSelectedRow() + 1) * table.getRowHeight();

    return new Point(x, y);
  }

  private static final class DefaultColumnConditionPanelProvider implements ColumnConditionPanelProvider<Property> {

    private final SwingEntityTableModel tableModel;

    private DefaultColumnConditionPanelProvider(final SwingEntityTableModel tableModel) {
      this.tableModel = requireNonNull(tableModel);
    }

    @Override
    public ColumnConditionPanel<Property> createColumnConditionPanel(final TableColumn column) {
      return new PropertyFilterPanel(tableModel.getConditionModel().getPropertyFilterModel(
              ((Property) column.getIdentifier()).getPropertyId()), true, true);
    }
  }
}
