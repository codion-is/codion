/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Configuration;
import org.jminor.common.Conjunction;
import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.FileUtil;
import org.jminor.common.Serializer;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.swing.common.model.table.FilteredTableModel;
import org.jminor.swing.common.ui.DefaultDialogExceptionHandler;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.input.BooleanInputProvider;
import org.jminor.swing.common.ui.input.DateInputProvider;
import org.jminor.swing.common.ui.input.DoubleInputProvider;
import org.jminor.swing.common.ui.input.InputProvider;
import org.jminor.swing.common.ui.input.InputProviderPanel;
import org.jminor.swing.common.ui.input.IntegerInputProvider;
import org.jminor.swing.common.ui.input.LongInputProvider;
import org.jminor.swing.common.ui.input.TextInputProvider;
import org.jminor.swing.common.ui.input.ValueListInputProvider;
import org.jminor.swing.common.ui.table.FilteredTablePanel;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import javax.swing.AbstractButton;
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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
 * @see EntityTableModel
 */
public class EntityTablePanel extends FilteredTablePanel<Entity, Property> {

  /**
   * Specifies whether or not columns can be rearranged in tables<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final Value<Boolean> ALLOW_COLUMN_REORDERING = Configuration.booleanValue("jminor.client.allowColumnReordering", true);

  /**
   * Specifies whether the table condition panels should be visible or not by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final Value<Boolean> TABLE_CONDITION_PANEL_VISIBLE = Configuration.booleanValue("jminor.swing.tableConditionPanelVisible", false);

  /**
   * Specifies the default table column resize mode for tables in the application<br>
   * Value type: Integer (JTable.AUTO_RESIZE_*)<br>
   * Default value: JTable.AUTO_RESIZE_OFF
   */
  public static final Value<Integer> TABLE_AUTO_RESIZE_MODE = Configuration.integerValue("jminor.swing.tableAutoResizeMode", JTable.AUTO_RESIZE_OFF);

  public static final String PRINT_TABLE = "printTable";
  public static final String DELETE_SELECTED = "deleteSelected";
  public static final String VIEW_DEPENDENCIES = "viewDependencies";
  public static final String UPDATE_SELECTED = "updateSelected";
  public static final String CONFIGURE_QUERY = "configureQuery";
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
  private static final String TRIPLEDOT = "...";

  private final Event tableDoubleClickedEvent = Events.event();
  private final Event<Boolean> conditionPanelVisibilityChangedEvent = Events.event();

  private final Map<String, Control> controlMap = new HashMap<>();

  /**
   * the condition panel
   */
  private final EntityTableConditionPanel conditionPanel;

  /**
   * the scroll pane used for the condition panel
   */
  private final JScrollPane conditionScrollPane;

  /**
   * the toolbar containing the refresh button
   */
  private final JToolBar refreshToolBar;

  /**
   * the label for showing the status of the table, that is, the number of rows, number of selected rows etc.
   */
  private final JLabel statusMessageLabel;

  private final List<ControlSet> additionalPopupControlSets = new ArrayList<>();
  private final List<ControlSet> additionalToolbarControlSets = new ArrayList<>();

  /**
   * the action performed when the table is double clicked
   */
  private Action tableDoubleClickAction;

  /**
   * specifies whether or not to include the south panel
   */
  private boolean includeSouthPanel = true;

  /**
   * specifies whether or not to include the condition panel
   */
  private boolean includeConditionPanel = true;

  /**
   * specifies whether or not to include a popup menu
   */
  private boolean includePopupMenu = true;

  /**
   * True after {@code initializePanel()} has been called
   */
  private boolean panelInitialized = false;

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
    super((FilteredTableModel<Entity, Property>) tableModel, column ->
            new PropertyFilterPanel(tableModel.getConditionModel().getPropertyFilterModel(
                    ((Property) column.getIdentifier()).getPropertyId()), true, true));
    getJTable().setAutoResizeMode(TABLE_AUTO_RESIZE_MODE.get());
    this.conditionPanel = conditionPanel;
    if (conditionPanel != null) {
      this.conditionScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }
    else {
      this.conditionScrollPane = null;
    }
    this.statusMessageLabel = initializeStatusMessageLabel();
    this.refreshToolBar = initializeRefreshToolbar();
  }

  /**
   * @param doubleClickAction the action to perform when a double click is performed on the table
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setTableDoubleClickAction(final Action doubleClickAction) {
    checkIfInitialized();
    this.tableDoubleClickAction = doubleClickAction;
  }

  /**
   * @return the Action performed when the table receives a double click
   */
  public final Action getTableDoubleClickAction() {
    return tableDoubleClickAction;
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
   * @param additionalToolbarControls a set of controls to add to the table toolbar menu
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void addToolbarControls(final ControlSet additionalToolbarControls) {
    checkIfInitialized();
    this.additionalToolbarControlSets.add(additionalToolbarControls);
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
   * @return the EntityTableModel used by this EntityTablePanel
   */
  public final SwingEntityTableModel getEntityTableModel() {
    return (SwingEntityTableModel) super.getTableModel();
  }

  /**
   * Shows a dialog for configuring the underlying EntityTableModel query.
   * If the underlying table model does not allow query configuration this
   * method returns silently
   * @see EntityTableModel#isQueryConfigurationAllowed()
   */
  public final void configureQuery() {
    if (!getEntityTableModel().isQueryConfigurationAllowed()) {
      return;
    }

    final EntityConditionPanel panel;
    Control refreshControl;
    try {
      UiUtil.setWaitCursor(true, this);
      panel = new EntityConditionPanel(getEntityTableModel());
      refreshControl = Controls.control(getEntityTableModel()::refresh, FrameworkMessages.get(FrameworkMessages.APPLY), null,
              null, FrameworkMessages.get(FrameworkMessages.APPLY_MNEMONIC).charAt(0));
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
    UiUtil.displayInDialog(this, panel, FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY), refreshControl);
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
      conditionScrollPane.getViewport().setView(visible ? conditionPanel : null);
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
    return conditionScrollPane != null && conditionScrollPane.getViewport().getView() == conditionPanel;
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

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + getEntityTableModel().getEntityId();
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
   * and {@link EntityEditModel#getAllowUpdateObserver()} is enabled.
   * @return a control set containing a set of controls, one for each updatable property in the
   * underlying entity, for performing an update on the selected entities
   * @see #initializePanel()
   * @throws IllegalStateException in case the underlying edit model is read only or updating is not allowed
   * @see #includeUpdateSelectedProperty(org.jminor.framework.domain.Property)
   * @see EntityEditModel#getAllowUpdateObserver()
   */
  public ControlSet getUpdateSelectedControlSet() {
    if (getEntityTableModel().isReadOnly() || !getEntityTableModel().isUpdateAllowed()
            || !getEntityTableModel().isBatchUpdateAllowed()) {
      throw new IllegalStateException("Table model is read only or does not allow updates");
    }
    final StateObserver selectionNotEmpty = getEntityTableModel().getSelectionModel().getSelectionEmptyObserver().getReversedObserver();
    final StateObserver updateAllowed = getEntityTableModel().getEditModel().getAllowUpdateObserver();
    final StateObserver enabled = States.aggregateState(Conjunction.AND, selectionNotEmpty, updateAllowed);
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED),
            (char) 0, Images.loadImage("Modify16.gif"), enabled);
    controlSet.setDescription(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP));
    getEntityTableModel().getConnectionProvider().getEntities().getUpdatableProperties(
            getEntityTableModel().getEntityId()).forEach(property -> {
      if (includeUpdateSelectedProperty(property)) {
        final String caption = property.getCaption() == null ? property.getPropertyId() : property.getCaption();
        controlSet.add(Controls.control(() -> updateSelectedEntities(property),caption, enabled));
      }
    });

    return controlSet;
  }

  /**
   * @return a control for showing the query configuration dialog
   */
  public final Control getConfigureQueryControl() {
    return Controls.control(this::configureQuery,
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY) + TRIPLEDOT, null,
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY), 0,
            null, Images.loadImage(Images.IMG_PREFERENCES_16));
  }

  /**
   * @return a control for showing the dependencies dialog
   */
  public final Control getViewDependenciesControl() {
    return Controls.control(this::viewSelectionDependencies,
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES) + TRIPLEDOT,
            getEntityTableModel().getSelectionModel().getSelectionEmptyObserver().getReversedObserver(),
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES_TIP), 'W');
  }

  /**
   * @return a control for deleting the selected entities
   * @throws IllegalStateException in case the underlying model is read only or if deleting is not allowed
   */
  public final Control getDeleteSelectedControl() {
    if (getEntityTableModel().isReadOnly() || !getEntityTableModel().isDeleteAllowed()) {
      throw new IllegalStateException("Table model is read only or does not allow delete");
    }
    return Controls.control(this::delete, FrameworkMessages.get(FrameworkMessages.DELETE),
            States.aggregateState(Conjunction.AND,
                    getEntityTableModel().getEditModel().getAllowDeleteObserver(),
                    getEntityTableModel().getSelectionModel().getSelectionEmptyObserver().getReversedObserver()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP), 0, null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for exporting the selected records to file
   */
  public final Control getExportControl() {
    return Controls.control(this::exportSelected,
            FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED) + TRIPLEDOT,
            getEntityTableModel().getSelectionModel().getSelectionEmptyObserver().getReversedObserver(),
            FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED_TIP), 0, null,
            Images.loadImage(Images.IMG_SAVE_16));
  }

  /**
   * @return a control for printing the table
   */
  public final Control getPrintTableControl() {
    final String printCaption = FrameworkMessages.get(FrameworkMessages.PRINT_TABLE);
    return Controls.control(this::printTable, printCaption, null,
            printCaption, printCaption.charAt(0), null, Images.loadImage("Print16.gif"));
  }

  /**
   * @return a Control for refreshing the underlying table data
   */
  public final Control getRefreshControl() {
    final String refreshCaption = FrameworkMessages.get(FrameworkMessages.REFRESH);
    return Controls.control(getEntityTableModel()::refresh, refreshCaption,
            null, FrameworkMessages.get(FrameworkMessages.REFRESH_TIP), refreshCaption.charAt(0),
            null, Images.loadImage(Images.IMG_REFRESH_16));
  }

  /**
   * @return a Control for clearing the underlying table model, that is, removing all rows
   */
  public final Control getClearControl() {
    final String clearCaption = FrameworkMessages.get(FrameworkMessages.CLEAR);
    return Controls.control(getEntityTableModel()::clear, clearCaption,
            null, null, clearCaption.charAt(0), null, null);
  }

  /**
   * Retrieves a new property value via input dialog and performs an update on the selected entities
   * @param propertyToUpdate the property to update
   * @see #getInputProvider(org.jminor.framework.domain.Property, java.util.List)
   */
  public final void updateSelectedEntities(final Property propertyToUpdate) {
    if (getEntityTableModel().getSelectionModel().isSelectionEmpty()) {
      return;
    }

    final List<Entity> selectedEntities = Entities.copyEntities(getEntityTableModel().getSelectionModel().getSelectedItems());
    final InputProviderPanel inputPanel = new InputProviderPanel(propertyToUpdate.getCaption(),
            getInputProvider(propertyToUpdate, selectedEntities));
    UiUtil.displayInDialog(this, inputPanel, FrameworkMessages.get(FrameworkMessages.SET_PROPERTY_VALUE), true,
            inputPanel.getOkButton(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted()) {
      Entities.put(propertyToUpdate.getPropertyId(), inputPanel.getValue(), selectedEntities);
      try {
        UiUtil.setWaitCursor(true, this);
        getEntityTableModel().update(selectedEntities);
      }
      catch (ValidationException | CancelException | DatabaseException e) {
        handleException(e);
      }
      finally {
        UiUtil.setWaitCursor(false, this);
      }
    }
  }

  /**
   * Shows a dialog containing lists of entities depending on the selected entities via foreign key
   */
  public final void viewSelectionDependencies() {
    if (getTableModel().getSelectionModel().isSelectionEmpty()) {
      return;
    }

    final SwingEntityTableModel tableModel = getEntityTableModel();
    try {
      UiUtil.setWaitCursor(true, this);
      final Map<String, Collection<Entity>> dependencies =
              tableModel.getConnectionProvider().getConnection().selectDependentEntities(tableModel.getSelectionModel().getSelectedItems());
      if (!dependencies.isEmpty()) {
        showDependenciesDialog(dependencies, tableModel.getConnectionProvider(), this);
      }
      else {
        JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.NONE_FOUND),
                FrameworkMessages.get(FrameworkMessages.NO_DEPENDENT_RECORDS), JOptionPane.INFORMATION_MESSAGE);
      }
    }
    catch (final DatabaseException e) {
      handleException(e);
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
  }

  /**
   * Deletes the entities selected in the underlying table model
   * @see #confirmDelete()
   */
  public final void delete() {
    if (confirmDelete()) {
      try {
        UiUtil.setWaitCursor(true, this);
        getEntityTableModel().deleteSelected();
      }
      catch (DatabaseException | CancelException e) {
        handleException(e);
      }
      finally {
        UiUtil.setWaitCursor(false, this);
      }
    }
  }

  /**
   * Exports the selected records as a text file using the available serializer
   * @see org.jminor.framework.domain.Entities#getEntitySerializer()
   * @see Entities#ENTITY_SERIALIZER_CLASS
   */
  public final void exportSelected() {
    try {
      final List<Entity> selected = getEntityTableModel().getSelectionModel().getSelectedItems();
      FileUtil.writeFile(getEntityTableModel().getConnectionProvider().getEntities().getEntitySerializer()
              .serialize(selected), UiUtil.selectFileToSave(this, null, null));
      JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED_DONE));
    }
    catch (IOException | CancelException | Serializer.SerializeException e) {
      handleException(e);
    }
  }

  /**
   * Prints the table
   * @see JTable#print()
   * @throws java.awt.print.PrinterException in case of a print exception
   */
  public final void printTable() throws PrinterException {
    getJTable().print();
  }

  /**
   * Uses the default exception handler to handle the given exception
   * @param exception the exception to handle
   * @see DefaultDialogExceptionHandler#handleException(Throwable, javax.swing.JComponent)
   */
  public final void handleException(final Exception exception) {
    DefaultDialogExceptionHandler.getInstance().handleException(exception, UiUtil.getParentWindow(this));
  }

  /**
   * Initializes the button used to toggle the condition panel state (hidden, visible and advanced)
   * @return a condition panel toggle button
   */
  public final Control getToggleConditionPanelControl() {
    if (!getEntityTableModel().isQueryConfigurationAllowed()) {
      return null;
    }

    final Control toggleControl = new Control() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        toggleConditionPanel();
      }
    };
    toggleControl.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    toggleControl.setDescription(FrameworkMessages.get(FrameworkMessages.SHOW_CONDITION_PANEL));

    return toggleControl;
  }

  /**
   * @return a control for clearing the table selection
   */
  public final Control getClearSelectionControl() {
    final Control clearSelection = Controls.control(getEntityTableModel().getSelectionModel()::clearSelection, null,
            getEntityTableModel().getSelectionModel().getSelectionEmptyObserver().getReversedObserver(), null, -1, null,
            Images.loadImage("ClearSelection16.gif"));
    clearSelection.setDescription(FrameworkMessages.get(FrameworkMessages.CLEAR_SELECTION_TIP));

    return clearSelection;
  }

  /**
   * @return a control for moving the table selection down one index
   */
  public final Control getMoveSelectionDownControl() {
    final Control selectionDown = Controls.control(getEntityTableModel().getSelectionModel()::moveSelectionDown,
            Images.loadImage(Images.IMG_DOWN_16));
    selectionDown.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_DOWN_TIP));

    return selectionDown;
  }

  /**
   * @return a control for moving the table selection up one index
   */
  public final Control getMoveSelectionUpControl() {
    final Control selectionUp = Controls.control(getEntityTableModel().getSelectionModel()::moveSelectionUp,
            Images.loadImage(Images.IMG_UP_16));
    selectionUp.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_UP_TIP));

    return selectionUp;
  }

  /**
   * @param listener a listener notified each time the condition panel visibility changes
   */
  public final void addConditionPanelVisibleListener(final EventListener listener) {
    conditionPanelVisibilityChangedEvent.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeConditionPanelVisibleListener(final EventListener listener) {
    conditionPanelVisibilityChangedEvent.removeListener(listener);
  }

  /**
   * @param listener a listener notified each time the table is double clicked
   */
  public final void addTableDoubleClickListener(final EventListener listener) {
    tableDoubleClickedEvent.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeTableDoubleClickListener(final EventListener listener) {
    tableDoubleClickedEvent.removeListener(listener);
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

    return createStaticEntityTablePanel(entities, connectionProvider, entities.iterator().next().getEntityId());
  }

  /**
   * Creates a static read-only entity table panel showing the given entities.
   * @param entities the entities to show in the panel
   * @param connectionProvider the EntityConnectionProvider, in case the returned panel should require one
   * @param entityId the entityId
   * @return a static EntityTablePanel showing the given entities
   */
  public static EntityTablePanel createStaticEntityTablePanel(final Collection<Entity> entities,
                                                              final EntityConnectionProvider connectionProvider,
                                                              final String entityId) {
    final SwingEntityTableModel tableModel = new SwingEntityTableModel(entityId, connectionProvider) {
      @Override
      protected List<Entity> performQuery() {
        return new ArrayList<>(entities);
      }
    };
    tableModel.setQueryConfigurationAllowed(false);
    tableModel.refresh();
    final EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
    tablePanel.setIncludePopupMenu(false);
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
  public static Collection<Entity> selectEntities(final SwingEntityTableModel lookupModel, final JComponent dialogOwner,
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
  public static Collection<Entity> selectEntities(final SwingEntityTableModel lookupModel, final JComponent dialogOwner,
                                                  final boolean singleSelection, final String dialogTitle,
                                                  final Dimension preferredSize) {
    Objects.requireNonNull(lookupModel, "lookupModel");
    final Collection<Entity> selected = new ArrayList<>();
    final JDialog dialog = new JDialog(UiUtil.getParentWindow(dialogOwner), dialogTitle);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final Control okControl = Controls.control(() -> {
      selected.addAll(lookupModel.getSelectionModel().getSelectedItems());
      dialog.dispose();
    }, Messages.get(Messages.OK));
    final Control cancelControl = Controls.control(() -> {
      selected.add(null);//hack to indicate cancel
      dialog.dispose();
    }, Messages.get(Messages.CANCEL));

    final SwingEntityModel model = new SwingEntityModel(lookupModel);
    model.getEditModel().setReadOnly(true);
    final EntityTablePanel entityTablePanel = new EntityTablePanel(lookupModel);
    entityTablePanel.initializePanel();
    entityTablePanel.addTableDoubleClickListener(() -> {
      if (!lookupModel.getSelectionModel().isSelectionEmpty()) {
        okControl.actionPerformed(null);
      }
    });
    entityTablePanel.setConditionPanelVisible(true);
    if (singleSelection) {
      entityTablePanel.getJTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    final Control searchControl = Controls.control(() -> {
      lookupModel.refresh();
      if (lookupModel.getRowCount() > 0) {
        lookupModel.getSelectionModel().setSelectedIndexes(Collections.singletonList(0));
        entityTablePanel.getJTable().requestFocusInWindow();
      }
      else {
        JOptionPane.showMessageDialog(UiUtil.getParentWindow(entityTablePanel),
                FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CONDITION));
      }
    }, FrameworkMessages.get(FrameworkMessages.SEARCH));

    final JButton btnOk  = new JButton(okControl);
    final JButton btnCancel = new JButton(cancelControl);
    final JButton btnSearch = new JButton(searchControl);
    final String cancelMnemonic = Messages.get(Messages.CANCEL_MNEMONIC);
    final String okMnemonic = Messages.get(Messages.OK_MNEMONIC);
    final String searchMnemonic = FrameworkMessages.get(FrameworkMessages.SEARCH_MNEMONIC);
    btnOk.setMnemonic(okMnemonic.charAt(0));
    btnCancel.setMnemonic(cancelMnemonic.charAt(0));
    btnSearch.setMnemonic(searchMnemonic.charAt(0));
    UiUtil.addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, 0, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, cancelControl);
    entityTablePanel.getJTable().getInputMap(
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    dialog.setLayout(new BorderLayout());
    if (preferredSize != null) {
      entityTablePanel.setPreferredSize(preferredSize);
    }
    dialog.add(entityTablePanel, BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(UiUtil.createFlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(btnSearch);
    buttonPanel.add(btnOk);
    buttonPanel.add(btnCancel);
    dialog.getRootPane().setDefaultButton(btnOk);
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
        UiUtil.setWaitCursor(true, this);
        setupControls();
        initializeTable();
        initializeUI();
        bindPanelEvents();
        updateStatusMessage();
      }
      finally {
        panelInitialized = true;
        UiUtil.setWaitCursor(false, this);
      }
    }

    return this;
  }

  /**
   * Initializes the south panel, override and return null for no south panel.
   * @return the south panel, or null if no south panel should be used
   */
  protected JPanel initializeSouthPanel() {
    initializeStatusMessageLabel();
    final JPanel centerPanel = new JPanel(UiUtil.createBorderLayout());
    final JTextField searchField = getSearchField();
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
    UiUtil.addKeyEvent(table, KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK, Controls.control(() -> {
      final Point location = getPopupLocation(table);
      popupMenu.show(table, location.x, location.y);
    }, "EntityTablePanel.showPopupMenu"));
    UiUtil.addKeyEvent(table, KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            Controls.control(() -> EntityUiUtil.showEntityMenu(getEntityTableModel().getSelectionModel().getSelectedItem(),
                    EntityTablePanel.this, getPopupLocation(table), getEntityTableModel().getConnectionProvider()),
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

  protected ControlSet getToolbarControls(final List<ControlSet> additionalToolbarControlSets) {
    final ControlSet toolbarControls = new ControlSet("");
    if (controlMap.containsKey(TOGGLE_SUMMARY_PANEL)) {
      toolbarControls.add(controlMap.get(TOGGLE_SUMMARY_PANEL));
    }
    if (controlMap.containsKey(TOGGLE_CONDITION_PANEL)) {
      toolbarControls.add(controlMap.get(TOGGLE_CONDITION_PANEL));
    }
    if (controlMap.containsKey(CONFIGURE_QUERY)) {
      toolbarControls.add(controlMap.get(CONFIGURE_QUERY));
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
    additionalToolbarControlSets.forEach(controlSet -> {
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
    if (controlMap.containsKey(CONFIGURE_QUERY)) {
      if (separatorRequired) {
        popupControls.addSeparator();
        separatorRequired = false;
      }
      addConditionControls(popupControls);
    }
    if (separatorRequired) {
      popupControls.addSeparator();
    }
    popupControls.add(controlMap.get(COPY_TABLE_DATA));

    return popupControls;
  }

  private void addAdditionalControls(final ControlSet popupControls, final List<ControlSet> additionalPopupControlSets) {
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

  private void addConditionControls(final ControlSet popupControls) {
    popupControls.add(controlMap.get(CONFIGURE_QUERY));
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

  protected ControlSet getPrintControls() {
    final String printCaption = Messages.get(Messages.PRINT);
    final ControlSet printControls = new ControlSet(printCaption, printCaption.charAt(0), Images.loadImage("Print16.gif"));
    printControls.add(controlMap.get(PRINT_TABLE));

    return printControls;
  }

  protected final Control getConditionPanelControl() {
    return Controls.toggleControl(this, "conditionPanelVisible",
            FrameworkMessages.get(FrameworkMessages.SHOW), conditionPanelVisibilityChangedEvent);
  }

  protected final ControlSet getCopyControlSet() {
    return new ControlSet(Messages.get(Messages.COPY), getCopyCellControl(), getCopyTableWithHeaderControl());
  }

  /**
   * Override to exclude properties from the update selected menu.
   * @param property the property
   * @return true if the given property should be included in the update selected menu.
   * @see #getUpdateSelectedControlSet()
   */
  @SuppressWarnings("UnusedParameters")
  protected boolean includeUpdateSelectedProperty(final Property property) {
    return true;
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
    return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_SELECTED),
            FrameworkMessages.get(FrameworkMessages.DELETE)};
  }

  /**
   * Provides value input components for multiple entity update, override to supply
   * specific InputValueProvider implementations for properties.
   * Remember to return with a call to super.getInputProviderInputProvider().
   * @param property the property for which to get the InputProvider
   * @param toUpdate the entities that are about to be updated
   * @return the InputProvider handling input for {@code property}
   * @see #updateSelectedEntities(org.jminor.framework.domain.Property)
   */
  protected InputProvider getInputProvider(final Property property, final List<Entity> toUpdate) {
    final Collection values = Entities.getDistinctValues(property.getPropertyId(), toUpdate);
    final Object currentValue = values.size() == 1 ? values.iterator().next() : null;
    if (property instanceof Property.ValueListProperty) {
      return new ValueListInputProvider(currentValue, ((Property.ValueListProperty) property).getValues());
    }
    if (property instanceof Property.ForeignKeyProperty) {
      return createEntityInputProvider((Property.ForeignKeyProperty) property, (Entity) currentValue, getEntityTableModel().getEditModel());
    }
    switch (property.getType()) {
      case Types.BOOLEAN:
        return new BooleanInputProvider((Boolean) currentValue);
      case Types.DATE:
        return new DateInputProvider((Date) currentValue, Property.getDefaultDateFormat());
      case Types.TIMESTAMP:
        return new DateInputProvider((Date) currentValue, Property.getDefaultTimestampFormat());
      case Types.TIME:
        return new DateInputProvider((Date) currentValue, Property.getDefaultTimeFormat());
      case Types.DOUBLE:
        return new DoubleInputProvider((Double) currentValue);
      case Types.INTEGER:
        return new IntegerInputProvider((Integer) currentValue);
      case Types.BIGINT:
        return new LongInputProvider((Long) currentValue);
      case Types.CHAR:
        return new TextInputProvider(property.getCaption(), getEntityTableModel().getEditModel().getValueProvider(property),
                (String) currentValue, 1);
      case Types.VARCHAR:
        return new TextInputProvider(property.getCaption(), getEntityTableModel().getEditModel().getValueProvider(property),
                (String) currentValue, property.getMaxLength());
      default:
        throw new IllegalArgumentException("Unsupported property type: " + property.getType());
    }
  }

  /**
   * Creates a InputProvider for the given foreign key property
   * @param foreignKeyProperty the property
   * @param currentValue the current value to initialize the InputProvider with
   * @param editModel the edit model involved in the updating
   * @return a Entity InputProvider
   */
  protected final InputProvider createEntityInputProvider(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final Entity currentValue,
                                                          final EntityEditModel editModel) {
    if (getEntityTableModel().getConnectionProvider().getEntities().isSmallDataset(foreignKeyProperty.getForeignEntityId())) {
      return new EntityComboProvider(((SwingEntityEditModel) editModel).createForeignKeyComboBoxModel(foreignKeyProperty), currentValue);
    }
    else {
      return new EntityLookupProvider(editModel.createForeignKeyLookupModel(foreignKeyProperty), currentValue);
    }
  }

  /**
   * Returns the TableCellRenderer used for the given property in this EntityTablePanel
   * @param property the property
   * @return the TableCellRenderer for the given property
   */
  protected TableCellRenderer initializeTableCellRenderer(final Property property) {
    return EntityTableCellRenderers.getTableCellRenderer(getEntityTableModel(), property);
  }

  /**
   * This method simply adds the given {@code southPanel} to the {@code BorderLayout.SOUTH} location, assuming the
   * {@code basePanel} is at location BorderLayout.CENTER.
   * By overriding this method you can override the default layout.
   * @param southPanel the panel to add at the BorderLayout.SOUTH position, if any
   * @see #getBasePanel()
   */
  protected void layoutPanel(final JPanel southPanel) {
    if (southPanel != null) {
      add(southPanel, BorderLayout.SOUTH);
    }
  }

  /**
   * Initialize the MouseListener for the table component handling
   * double click and right click, or popup click with ALT down. Double clicking
   * simply invokes the action returned by {@link #getTableDoubleClickAction()}
   * with the JTable as the ActionEvent source while right click with ALT down
   * invokes {@link EntityUiUtil#showEntityMenu(Entity, JComponent, Point, EntityConnectionProvider)}
   * @return the MouseListener for the table
   * @see #getTableDoubleClickAction()
   */
  protected final MouseListener initializeTableMouseListener() {
    return new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (e.getClickCount() == 2) {
          final Action doubleClickAction = getTableDoubleClickAction();
          if (doubleClickAction != null) {
            doubleClickAction.actionPerformed(new ActionEvent(getJTable(), -1, "doubleClick"));
          }
          tableDoubleClickedEvent.fire();
        }
        else if (e.isPopupTrigger() && e.isAltDown()) {
          EntityUiUtil.showEntityMenu(getEntityTableModel().getSelectionModel().getSelectedItem(), EntityTablePanel.this,
                  e.getPoint(), getEntityTableModel().getConnectionProvider());
        }
      }
    };
  }

  /**
   * Initializes the south panel toolbar, by default based on {@code getToolbarControls()}
   * @return the toolbar to add to the south panel
   */
  protected JToolBar initializeToolbar() {
    final ControlSet toolbarControlSet = getToolbarControls(additionalToolbarControlSets);
    if (toolbarControlSet != null) {
      final JToolBar southToolBar = ControlProvider.createToolbar(toolbarControlSet, JToolBar.HORIZONTAL);
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
    if (!getEntityTableModel().isReadOnly() && getEntityTableModel().isDeleteAllowed()) {
      setControl(DELETE_SELECTED, getDeleteSelectedControl());
    }
    if (!getEntityTableModel().isReadOnly() && getEntityTableModel().isUpdateAllowed() && getEntityTableModel().isBatchUpdateAllowed()) {
      setControl(UPDATE_SELECTED, getUpdateSelectedControlSet());
    }
    if (getEntityTableModel().isQueryConfigurationAllowed()) {
      setControl(CONFIGURE_QUERY, getConfigureQueryControl());
      setControl(CONDITION_PANEL_VISIBLE, getConditionPanelControl());
    }
    setControl(CLEAR, getClearControl());
    setControl(REFRESH, getRefreshControl());
    setControl(SELECT_COLUMNS, getSelectColumnsControl());
    if (getEntityTableModel().getConnectionProvider().getEntities().entitySerializerAvailable()) {
      setControl(EXPORT_JSON, getExportControl());
    }
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

  private Control getCopyCellControl() {
    return new Control(FrameworkMessages.get(FrameworkMessages.COPY_CELL),
            getEntityTableModel().getSelectionModel().getSelectionEmptyObserver().getReversedObserver()) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final JTable table = getJTable();
        final Object value = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn());
        UiUtil.setClipboard(value == null ? "" : value.toString());
      }
    };
  }

  private Control getCopyTableWithHeaderControl() {
    return new Control(FrameworkMessages.get(FrameworkMessages.COPY_TABLE_WITH_HEADER)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        copyTableAsDelimitedString();
      }
    };
  }

  private void copyTableAsDelimitedString() {
    UiUtil.setClipboard(getEntityTableModel().getTableDataAsDelimitedString('\t'));
  }

  private void initializeUI() {
    if (includeConditionPanel && conditionScrollPane != null) {
      getBasePanel().add(conditionScrollPane, BorderLayout.NORTH);
      if (conditionPanel.canToggleAdvanced()) {
        conditionScrollPane.getHorizontalScrollBar().setModel(getTableScrollPane().getHorizontalScrollBar().getModel());
        conditionPanel.addAdvancedListener(info -> {
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
        final JToolBar southToolBar = initializeToolbar();
        if (southToolBar != null) {
          southPanelCenter.add(southToolBar, BorderLayout.EAST);
        }
        southPanel.add(southPanelCenter, BorderLayout.SOUTH);
      }
    }
    layoutPanel(southPanel);
  }

  /**
   * @return the refresh toolbar
   */
  private JToolBar initializeRefreshToolbar() {
    final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    final String keyName = keyStroke.toString().replace("pressed ", "");
    final Control refresh = Controls.control(getEntityTableModel()::refresh, null,
            getEntityTableModel().getConditionModel().getConditionStateObserver(), FrameworkMessages.get(FrameworkMessages.REFRESH_TIP)
                    + " (" + keyName + ")", 0, null, Images.loadImage(Images.IMG_STOP_16));

    final InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    final ActionMap actionMap = getActionMap();

    inputMap.put(keyStroke, "EntityTablePanel.refreshControl");
    actionMap.put("EntityTablePanel.refreshControl", refresh);

    final AbstractButton button = ControlProvider.createButton(refresh);
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
      final String status = getEntityTableModel().getStatusMessage();
      statusMessageLabel.setText(status);
      statusMessageLabel.setToolTipText(status);
    }
  }

  private void bindPanelEvents() {
    if (!getEntityTableModel().isReadOnly() && getEntityTableModel().isDeleteAllowed()) {
      UiUtil.addKeyEvent(getJTable(), KeyEvent.VK_DELETE, getDeleteSelectedControl());
    }
    final EventListener statusListener = () -> SwingUtilities.invokeLater(EntityTablePanel.this::updateStatusMessage);
    getEntityTableModel().getSelectionModel().addSelectionChangedListener(statusListener);
    getEntityTableModel().addFilteringListener(statusListener);
    getEntityTableModel().addTableDataChangedListener(statusListener);

    getEntityTableModel().getConditionModel().getPropertyConditionModels().forEach(conditionModel ->
            conditionModel.addConditionStateListener(() -> SwingUtilities.invokeLater(() -> {
              getJTable().getTableHeader().repaint();
              getJTable().repaint();
            })));
    if (conditionPanel != null) {
      conditionPanel.addFocusGainedListener(this::scrollToColumn);
    }
    if (getEntityTableModel().hasEditModel()) {
      getEntityTableModel().getEditModel().addEntitiesChangedListener(() -> SwingUtilities.invokeLater(getJTable()::repaint));
    }
  }

  private void initializeTable() {
    getJTable().addMouseListener(initializeTableMouseListener());

    final Enumeration<TableColumn> columnEnumeration = getTableModel().getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final TableColumn column = columnEnumeration.nextElement();
      final Property property = (Property) column.getIdentifier();
      column.setCellRenderer(initializeTableCellRenderer(property));
      column.setResizable(true);
    }
    final JTableHeader header = getJTable().getTableHeader();
    final TableCellRenderer defaultHeaderRenderer = header.getDefaultRenderer();
    final Font defaultFont = getJTable().getFont();
    final Font searchFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
    header.setDefaultRenderer((table, value, isSelected, hasFocus, row, column) -> {
      final JLabel label = (JLabel) defaultHeaderRenderer.getTableCellRendererComponent(table, value, isSelected,
              hasFocus, row, column);
      final EntityTableModel tableModel1 = getEntityTableModel();
      final TableColumn tableColumn = ((FilteredTableModel) tableModel1).getColumnModel().getColumn(column);
      final TableCellRenderer renderer = tableColumn.getCellRenderer();
      final Property property = (Property) tableColumn.getIdentifier();
      final boolean indicateSearch = renderer instanceof EntityTableCellRenderer
              && ((EntityTableCellRenderer) renderer).isIndicateCondition()
              && tableModel1.getConditionModel().isEnabled(property.getPropertyId());
      label.setFont(indicateSearch ? searchFont : defaultFont);

      return label;
    });
    header.setFocusable(false);
    header.setReorderingAllowed(ALLOW_COLUMN_REORDERING.get());
    if (includePopupMenu) {
      setTablePopupMenu(getJTable(), getPopupControls(additionalPopupControlSets));
    }
  }

  private void checkIfInitialized() {
    if (panelInitialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
  }

  private void showDependenciesDialog(final Map<String, Collection<Entity>> dependencies,
                                             final EntityConnectionProvider connectionProvider,
                                             final JComponent dialogParent) {
    JPanel dependenciesPanel;
    try {
      UiUtil.setWaitCursor(true, dialogParent);
      dependenciesPanel = createDependenciesPanel(dependencies, connectionProvider);
    }
    finally {
      UiUtil.setWaitCursor(false, dialogParent);
    }
    UiUtil.displayInDialog(UiUtil.getParentWindow(dialogParent), dependenciesPanel,
            FrameworkMessages.get(FrameworkMessages.DEPENDENT_RECORDS_FOUND));
  }

  private static JLabel initializeStatusMessageLabel() {
    final JLabel label  = new JLabel("", JLabel.CENTER);
    label.setFont(new Font(label.getFont().getName(), Font.PLAIN, STATUS_MESSAGE_FONT_SIZE));

    return label;
  }

  private JPanel createDependenciesPanel(final Map<String, Collection<Entity>> dependencies,
                                         final EntityConnectionProvider connectionProvider) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    for (final Map.Entry<String, Collection<Entity>> entry : dependencies.entrySet()) {
      final Collection<Entity> dependantEntities = entry.getValue();
      if (!dependantEntities.isEmpty()) {
        tabPane.addTab(connectionProvider.getEntities().getCaption(entry.getKey()), createStaticEntityTablePanel(dependantEntities, connectionProvider));
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
}
