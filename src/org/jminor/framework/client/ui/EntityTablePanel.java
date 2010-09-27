/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.Serializer;
import org.jminor.common.model.State;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.common.ui.ColumnSearchPanel;
import org.jminor.common.ui.DefaultExceptionHandler;
import org.jminor.common.ui.FilteredTablePanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.control.ToggleBeanValueLink;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.input.BooleanInputProvider;
import org.jminor.common.ui.input.DateInputProvider;
import org.jminor.common.ui.input.DoubleInputProvider;
import org.jminor.common.ui.input.InputProvider;
import org.jminor.common.ui.input.InputProviderPanel;
import org.jminor.common.ui.input.IntInputProvider;
import org.jminor.common.ui.input.TextInputProvider;
import org.jminor.common.ui.input.ValueListInputProvider;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.DefaultEntityTableModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The EntityTablePanel class consists of a JTable as well as filtering/searching and summary facilities.
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
 */
public class EntityTablePanel extends FilteredTablePanel<Entity, Property> {

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
  public static final String TOGGLE_SEARCH_PANEL = "toggleSearchPanel";
  public static final String SEARCH_PANEL_VISIBLE = "searchPanelVisible";
  public static final String CLEAR_SELECTION = "clearSelection";
  public static final String MOVE_SELECTION_UP = "moveSelectionUp";
  public static final String MOVE_SELECTION_DOWN = "moveSelectionDown";
  public static final String COPY_TABLE_DATA = "copyTableData";

  private static final Dimension TOOLBAR_BUTTON_SIZE = new Dimension(20, 20);
  private static final int STATUS_MESSAGE_FONT_SIZE = 12;
  private static final String TRIPLEDOT = "...";

  private final Event evtTableDoubleClicked = Events.event();
  private final Event evtSearchPanelVisibilityChanged = Events.event();
  private final Event evtSummaryPanelVisibilityChanged = Events.event();

  private final Map<String, Control> controlMap = new HashMap<String, Control>();

  /**
   * the search panel
   */
  private final EntityTableSearchPanel searchPanel;

  /**
   * the scroll pane used for the search panel
   */
  private final JScrollPane searchScrollPane;

  /**
   * the horizontal table scroll bar
   */
  private final JScrollBar horizontalTableScrollBar;

  /**
   * the summary panel
   */
  private final EntityTableSummaryPanel summaryPanel;

  /**
   * the panel used as a base panel for the summary panels, used for showing/hiding the summary panels
   */
  private final JPanel summaryBasePanel;

  /**
   * the scroll pane used for the summary panel
   */
  private final JScrollPane summaryScrollPane;

  /**
   * the toolbar containing the refresh button
   */
  private final JToolBar refreshToolBar;

  /**
   * the label for showing the status of the table, that is, the number of rows, number of selected rows etc.
   */
  private final JLabel statusMessageLabel;

  /**
   * the action performed when the table is double clicked
   */
  private Action tableDoubleClickAction;

  /**
   * specifies whether or not to include the south panel
   */
  private boolean includeSouthPanel = true;

  /**
   * specifies whether or not to include the search panel
   */
  private boolean includeSearchPanel = true;

  /**
   * True after <code>initializePanel()</code> has been called
   */
  private boolean panelInitialized = false;

  private ControlSet additionalPopupControls;
  private ControlSet additionalToolbarControls;

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   */
  public EntityTablePanel(final EntityTableModel tableModel) {
    this(tableModel, (ControlSet) null);
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param additionalPopupControls a ControlSet which will be added to the popup control set
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet additionalPopupControls) {
    this(tableModel, additionalPopupControls, null, tableModel.getSearchModel().isSimpleSearch() ?
            new EntityTableSearchSimplePanel(tableModel.getSearchModel(), tableModel) :
            new EntityTableSearchAdvancedPanel(tableModel.getSearchModel(), tableModel.getColumnModel()),
            new EntityTableSummaryPanel(tableModel));
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param searchPanel the search panel
   */
  public EntityTablePanel(final EntityTableModel tableModel, final EntityTableSearchPanel searchPanel) {
    this(tableModel, null, null, searchPanel);
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param summaryPanel the summary panel
   */
  public EntityTablePanel(final EntityTableModel tableModel, final EntityTableSummaryPanel summaryPanel) {
    this(tableModel, null, null, summaryPanel);
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param additionalPopupControls a ControlSet which will be added to the popup control set
   * @param additionalToolbarControls a ControlSet which will be added to the toolbar control set
   * @param searchPanel the search panel
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet additionalPopupControls,
                          final ControlSet additionalToolbarControls,
                          final EntityTableSearchPanel searchPanel) {
    this(tableModel, additionalPopupControls, additionalToolbarControls, searchPanel, new EntityTableSummaryPanel(tableModel));
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param additionalPopupControls a ControlSet which will be added to the popup control set
   * @param additionalToolbarControls a ControlSet which will be added to the toolbar control set
   * @param summaryPanel the summary panel
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet additionalPopupControls,
                          final ControlSet additionalToolbarControls,
                          final EntityTableSummaryPanel summaryPanel) {
    this(tableModel, additionalPopupControls, additionalToolbarControls, tableModel.getSearchModel().isSimpleSearch() ?
            new EntityTableSearchSimplePanel(tableModel.getSearchModel(), tableModel) :
            new EntityTableSearchAdvancedPanel(tableModel.getSearchModel(), tableModel.getColumnModel()),
            summaryPanel);
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param additionalPopupControls a ControlSet which will be added to the popup control set
   * @param additionalToolbarControls a ControlSet which will be added to the toolbar control set
   * @param searchPanel the search panel
   * @param summaryPanel the summary panel
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet additionalPopupControls,
                          final ControlSet additionalToolbarControls, final EntityTableSearchPanel searchPanel,
                          final EntityTableSummaryPanel summaryPanel) {
    super(tableModel, initializeFilterPanels(tableModel));
    this.searchPanel = searchPanel;
    if (searchPanel != null) {
      this.searchScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }
    else {
      this.searchScrollPane = null;
    }
    this.summaryPanel = summaryPanel;
    if (summaryPanel != null) {
      summaryBasePanel = new JPanel(new BorderLayout());
      summaryScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }
    else {
      summaryBasePanel = null;
      summaryScrollPane = null;
    }
    this.statusMessageLabel = initializeStatusMessageLabel();
    this.refreshToolBar = initializeRefreshToolbar();
    this.horizontalTableScrollBar = getTableScrollPane().getHorizontalScrollBar();
    this.additionalPopupControls = additionalPopupControls;
    this.additionalToolbarControls = additionalToolbarControls;
    initializeTable();
  }

  /**
   * @param doubleClickAction the action to perform when a double click is performed on the table
   */
  public final void setTableDoubleClickAction(final Action doubleClickAction) {
    if (panelInitialized) {
      throw new IllegalStateException("tableDoubleClickAction must be set before the panel is initialized");
    }
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
   */
  public final void setAdditionalPopupControls(final ControlSet additionalPopupControls) {
    if (panelInitialized) {
      throw new IllegalStateException("Additional popup controls must be set before the panel is initialized");
    }
    this.additionalPopupControls = additionalPopupControls;
  }

  /**
   * @param additionalToolbarControls a set of controls to add to the table toolbar menu
   */
  public final void setAdditionalToolbarControls(final ControlSet additionalToolbarControls) {
    if (panelInitialized) {
      throw new IllegalStateException("Additional toolbar controls must be set before the panel is initialized");
    }
    this.additionalToolbarControls = additionalToolbarControls;
  }

  /**
   * @param value true if the south panel should be included
   * @see #initializeSouthPanel()
   */
  public final void setIncludeSouthPanel(final boolean value) {
    if (panelInitialized) {
      throw new IllegalStateException("includeSouthPanel must be set before the panel is initialized");
    }
    this.includeSouthPanel = value;
  }

  /**
   * @param value true if the search panel should be included
   */
  public final void setIncludeSearchPanel(final boolean value) {
    if (panelInitialized) {
      throw new IllegalStateException("includeSearcPanel must be set before the panel is initialized");
    }
    this.includeSearchPanel = value;
  }

  /**
   * @return the EntityTableModel used by this EntityTablePanel
   */
  public final EntityTableModel getEntityTableModel() {
    return (EntityTableModel) super.getTableModel();
  }

  /**
   * Shows a dialog for configuring the underlying EntityTableModel query.
   * If the underlying table model does not allow query configuration this
   * method returns silently
   * @see org.jminor.framework.client.model.EntityTableModel#isQueryConfigurationAllowed()
   */
  public final void configureQuery() {
    if (!getEntityTableModel().isQueryConfigurationAllowed()) {
      return;
    }

    final EntityCriteriaPanel panel;
    AbstractAction action;
    try {
      UiUtil.setWaitCursor(true, this);
      panel = new EntityCriteriaPanel(getEntityTableModel());
      action = new AbstractAction(FrameworkMessages.get(FrameworkMessages.APPLY)) {
        public void actionPerformed(final ActionEvent e) {
          getEntityTableModel().refresh();
        }
      };
      action.putValue(Action.MNEMONIC_KEY, FrameworkMessages.get(FrameworkMessages.APPLY_MNEMONIC).charAt(0));
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
    UiUtil.showInDialog(UiUtil.getParentWindow(this), panel, false,
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY), true, false, action);
  }

  /**
   * Hides or shows the column summary panel for this EntityTablePanel
   * @param visible if true then the summary panel is shown, if false it is hidden
   */
  public final void setSummaryPanelVisible(final boolean visible) {
    if (visible && isSummaryPanelVisible()) {
      return;
    }

    if (summaryScrollPane != null) {
      summaryScrollPane.getViewport().setView(visible ? summaryPanel : null);
      if (visible) {
        summaryBasePanel.add(summaryScrollPane, BorderLayout.NORTH);
        summaryBasePanel.add(horizontalTableScrollBar, BorderLayout.SOUTH);
      }
      else {
        summaryBasePanel.remove(horizontalTableScrollBar);
        getTableScrollPane().setHorizontalScrollBar(horizontalTableScrollBar);
      }
      revalidate();
      evtSummaryPanelVisibilityChanged.fire();
    }
  }

  /**
   * @return true if the column summary panel is visible, false if it is hidden
   */
  public final boolean isSummaryPanelVisible() {
    return summaryScrollPane != null && summaryScrollPane.getViewport().getView() == summaryPanel;
  }

  /**
   * Hides or shows the column search panel for this EntityTablePanel
   * @param visible if true the search panel is shown, if false it is hidden
   */
  public final void setSearchPanelVisible(final boolean visible) {
    if (visible && isSearchPanelVisible()) {
      return;
    }

    if (searchScrollPane != null) {
      searchScrollPane.getViewport().setView(visible ? (JPanel) searchPanel : null);
      if (refreshToolBar != null) {
        refreshToolBar.setVisible(visible);
      }
      revalidate();
      evtSearchPanelVisibilityChanged.fire();
    }
  }

  /**
   * @return true if the search panel is visible, false if it is hidden
   */
  public final boolean isSearchPanelVisible() {
    return searchScrollPane != null && searchScrollPane.getViewport().getView() == searchPanel;
  }

  /**
   * @return the search panel being used by this EntityTablePanel
   */
  public final EntityTableSearchPanel getSearchPanel() {
    return searchPanel;
  }

  /**
   * Toggles the search panel through the states hidden, visible and
   * in case it is a EntityTableSearchPanel advanced
   */
  public final void toggleSearchPanel() {
    if (searchPanel instanceof EntityTableSearchAdvancedPanel) {
      if (isSearchPanelVisible()) {
        if (((EntityTableSearchAdvancedPanel) searchPanel).isAdvanced()) {
          setSearchPanelVisible(false);
        }
        else {
          ((EntityTableSearchAdvancedPanel) searchPanel).setAdvanced(true);
        }
      }
      else {
        ((EntityTableSearchAdvancedPanel) searchPanel).setAdvanced(false);
        setSearchPanelVisible(true);
      }
    }
    else {
      setSearchPanelVisible(!isSearchPanelVisible());
    }
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + getEntityTableModel().getEntityID();
  }

  /**
   * @param controlCode the control code
   * @return the control associated with <code>controlCode</code>
   * @throws RuntimeException in case no control is associated with the given control code
   */
  public final Control getControl(final String controlCode) {
    if (!controlMap.containsKey(controlCode)) {
      throw new IllegalArgumentException(controlCode + " control not available in panel: " + this);
    }

    return controlMap.get(controlCode);
  }

  /**
   * @return a control set containing a set of controls, one for each updatable property in the
   * underlying entity, for performing an update on the selected entities
   */
  public ControlSet getUpdateSelectedControlSet() {
    if (getEntityTableModel().isReadOnly() || !getEntityTableModel().isUpdateAllowed()
            || !getEntityTableModel().isBatchUpdateAllowed()) {
      throw new IllegalStateException("Table model is read only or does not allow updates");
    }
    final State enabled = States.aggregateState(Conjunction.AND,
            getEntityTableModel().getBatchUpdateAllowedState(),
            getEntityTableModel().getSelectionEmptyState().getReversedState());
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED),
            (char) 0, Images.loadImage("Modify16.gif"), enabled);
    controlSet.setDescription(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP));
    for (final Property property : EntityUtil.getUpdatableProperties(getEntityTableModel().getEntityID())) {
      final String caption = property.getCaption() == null ? property.getPropertyID() : property.getCaption();
      controlSet.add(UiUtil.linkToEnabledState(enabled, new AbstractAction(caption) {
        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent e) {
          updateSelectedEntities(property);
        }
      }));
    }

    return controlSet;
  }

  /**
   * @return a control for showing the query configuration dialog
   */
  public final Control getConfigureQueryControl() {
    return Controls.methodControl(this, "configureQuery",
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY) + TRIPLEDOT, null,
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY), 0,
            null, Images.loadImage(Images.IMG_PREFERENCES_16));
  }

  /**
   * @return a control for showing the dependencies dialog
   */
  public final Control getViewDependenciesControl() {
    return Controls.methodControl(this, "viewSelectionDependencies",
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES) + TRIPLEDOT,
            getEntityTableModel().getSelectionEmptyState().getReversedState(),
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES_TIP), 'W');
  }

  /**
   * @return a control for deleting the selected entities
   */
  public final Control getDeleteSelectedControl() {
    if (getEntityTableModel().isReadOnly() || !getEntityTableModel().isDeleteAllowed()) {
      throw new IllegalStateException("Table model is read only or does not allow delete");
    }
    return Controls.methodControl(this, "delete", FrameworkMessages.get(FrameworkMessages.DELETE),
            States.aggregateState(Conjunction.AND,
                    getEntityTableModel().getEditModel().getAllowDeleteState(),
                    getEntityTableModel().getSelectionEmptyState().getReversedState()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP), 0, null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for exporting the selected records to file
   */
  public final Control getExportControl() {
    return Controls.methodControl(this, "exportSelected",
            FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED) + TRIPLEDOT,
            getEntityTableModel().getSelectionEmptyState().getReversedState(),
            FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED_TIP), 0, null,
            Images.loadImage(Images.IMG_SAVE_16));
  }

  /**
   * @return a control for printing the table
   */
  public final Control getPrintTableControl() {
    final String printCaption = FrameworkMessages.get(FrameworkMessages.PRINT_TABLE);
    return Controls.methodControl(this, "printTable", printCaption, null,
            printCaption, printCaption.charAt(0), null, Images.loadImage("Print16.gif"));
  }

  /**
   * Queries the user on which property to update, after which it calls the
   * <code>updateSelectedEntities(property)</code> with that property
   * @see #updateSelectedEntities(org.jminor.framework.domain.Property)
   * @see #getInputProvider(org.jminor.framework.domain.Property, java.util.List)
   */
  public final void updateSelectedEntities() {
    try {
      updateSelectedEntities(getPropertyToUpdate());
    }
    catch (CancelException e) {/**/}
  }

  /**
   * Retrieves a new property value via input dialog and performs an update on the selected entities
   * @param propertyToUpdate the property to update
   * @see #getInputProvider(org.jminor.framework.domain.Property, java.util.List)
   */
  public final void updateSelectedEntities(final Property propertyToUpdate) {
    if (getEntityTableModel().getSelectionEmptyState().isActive()) {
      return;
    }

    final List<Entity> selectedEntities = EntityUtil.copyEntities(getEntityTableModel().getSelectedItems());
    if (!getEntityTableModel().isBatchUpdateAllowed() && selectedEntities.size() > 1) {
      throw new UnsupportedOperationException("Update of multiple entities is not allowed!");
    }

    final InputProviderPanel inputPanel = new InputProviderPanel(propertyToUpdate.getCaption(),
            getInputProvider(propertyToUpdate, selectedEntities));
    UiUtil.showInDialog(this, inputPanel, true, FrameworkMessages.get(FrameworkMessages.SET_PROPERTY_VALUE),
            null, inputPanel.getOkButton(), inputPanel.getButtonClickObserver());
    if (inputPanel.isEditAccepted()) {
      EntityUtil.setPropertyValue(propertyToUpdate.getPropertyID(), inputPanel.getValue(), selectedEntities);
      try {
        UiUtil.setWaitCursor(true, this);
        getEntityTableModel().update(selectedEntities);
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
      catch (ValidationException e) {
        throw new RuntimeException(e);
      }
      catch (CancelException e) {/**/}
      finally {
        UiUtil.setWaitCursor(false, this);
      }
    }
  }

  /**
   * Shows a dialog containing lists of entities depending on the selected entities via foreign key
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   */
  public final void viewSelectionDependencies() throws DatabaseException {
    final Map<String, Collection<Entity>> dependencies;
    try {
      UiUtil.setWaitCursor(true, this);
      dependencies = getEntityTableModel().getSelectionDependencies();
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
    if (!dependencies.isEmpty()) {
      showDependenciesDialog(dependencies, getEntityTableModel().getConnectionProvider(), this);
    }
    else {
      JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.NONE_FOUND),
              FrameworkMessages.get(FrameworkMessages.NO_DEPENDENT_RECORDS), JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * Performs a delete on the active entity or if a table model is available, the selected entities
   * @throws org.jminor.common.db.exception.DatabaseException in case of a database exception
   * @throws org.jminor.common.model.CancelException in the delete action is cancelled
   */
  public final void delete() throws DatabaseException, CancelException {
    if (confirmDelete()) {
      try {
        UiUtil.setWaitCursor(true, this);
        getEntityTableModel().deleteSelected();
      }
      finally {
        UiUtil.setWaitCursor(false, this);
      }
    }
  }

  /**
   * Exports the selected records as a text file
   * @throws CancelException in case the action is cancelled
   * @throws org.jminor.common.model.Serializer.SerializeException in case of an exception
   */
  public final void exportSelected() throws CancelException, Serializer.SerializeException {
    final List<Entity> selected = getEntityTableModel().getSelectedItems();
    Util.writeFile(EntityUtil.getEntitySerializer().serialize(selected), UiUtil.chooseFileToSave(this, null, null));
    JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED_DONE));
  }

  /**
   * Prints the table if one is available
   */
  public final void printTable() {
    try {
      getJTable().print();
    }
    catch (PrinterException pr) {
      throw new RuntimeException(pr);
    }
  }

  /**
   * By default this delegates to the edit panel
   * @param exception the exception to handle
   */
  public final void handleException(final Exception exception) {
    DefaultExceptionHandler.getInstance().handleException(exception, this);
  }

  /**
   * Initializes the button used to toggle the summary panel state (hidden and visible)
   * @return a summary panel toggle button
   */
  public final Control getToggleSummaryPanelControl() {
    final ToggleBeanValueLink toggle = Controls.toggleControl(this, "summaryPanelVisible", null,
            evtSummaryPanelVisibilityChanged);
    toggle.setIcon(Images.loadImage("Sum16.gif"));
    toggle.setDescription(FrameworkMessages.get(FrameworkMessages.TOGGLE_SUMMARY_TIP));

    return toggle;
  }

  /**
   * Initializes the button used to toggle the search panel state (hidden, visible and advanced)
   * @return a search panel toggle button
   */
  public final Control getToggleSearchPanelControl() {
    if (!getEntityTableModel().isQueryConfigurationAllowed()) {
      return null;
    }

    final Control ret = new Control() {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        toggleSearchPanel();
      }
    };
    ret.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    ret.setDescription(FrameworkMessages.get(FrameworkMessages.SEARCH));

    return ret;
  }

  /**
   * @return a control for clearing the table selection
   */
  public final Control getClearSelectionControl() {
    final Control clearSelection = Controls.methodControl(getEntityTableModel(), "clearSelection", null,
            getEntityTableModel().getSelectionEmptyState().getReversedState(), null, -1, null,
            Images.loadImage("ClearSelection16.gif"));
    clearSelection.setDescription(FrameworkMessages.get(FrameworkMessages.CLEAR_SELECTION_TIP));

    return clearSelection;
  }

  /**
   * @return a control for moving the table selection one index down
   */
  public final Control getMoveSelectionDownControl() {
    final Control selectionDown = Controls.methodControl(getEntityTableModel(), "moveSelectionDown",
            Images.loadImage(Images.IMG_DOWN_16));
    selectionDown.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_DOWN_TIP));

    return selectionDown;
  }

  /**
   * @return a control for moving the table selection one index up
   */
  public final Control getMoveSelectionUpControl() {
    final Control selectionUp = Controls.methodControl(getEntityTableModel(), "moveSelectionUp",
            Images.loadImage(Images.IMG_UP_16));
    selectionUp.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_UP_TIP));

    return selectionUp;
  }

  /**
   * @param listener a listener notified each time the search panel visibility changes
   */
  public final void addSearchPanelVisibleListener(final ActionListener listener) {
    evtSearchPanelVisibilityChanged.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeSearchPanelVisibleListener(final ActionListener listener) {
    evtSearchPanelVisibilityChanged.removeListener(listener);
  }

  /**
   * @param listener a listener notified each time the summary panel visibility changes
   */
  public final void addSummaryPanelVisibleListener(final ActionListener listener) {
    evtSummaryPanelVisibilityChanged.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeSummaryPanelVisibleListener(final ActionListener listener) {
    evtSummaryPanelVisibilityChanged.removeListener(listener);
  }

  /**
   * @param listener a listener notified each time the table is double clicked
   */
  public final void addTableDoubleClickListener(final ActionListener listener) {
    evtTableDoubleClicked.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeTableDoubleClickListener(final ActionListener listener) {
    evtTableDoubleClicked.removeListener(listener);
  }

  /**
   * Creates a static entity table panel showing the given entities
   * @param entities the entities to show in the panel
   * @param connectionProvider the EntityConnectionProvider, in case the returned panel should require one
   * @return a static EntityTablePanel showing the given entities
   */
  public static EntityTablePanel createStaticEntityTablePanel(final Collection<Entity> entities,
                                                              final EntityConnectionProvider connectionProvider) {
    if (entities == null || entities.isEmpty()) {
      throw new IllegalArgumentException("Cannot create an EntityPanel without the entities");
    }

    return createStaticEntityTablePanel(entities, connectionProvider, entities.iterator().next().getEntityID());
  }

  /**
   * Creates a static entity table panel showing the given entities
   * @param entities the entities to show in the panel
   * @param connectionProvider the EntityConnectionProvider, in case the returned panel should require one
   * @param entityID the entityID
   * @return a static EntityTablePanel showing the given entities
   */
  public static EntityTablePanel createStaticEntityTablePanel(final Collection<Entity> entities,
                                                              final EntityConnectionProvider connectionProvider,
                                                              final String entityID) {
    final EntityEditModel editModel = new DefaultEntityEditModel(entityID, connectionProvider);
    final EntityTableModel tableModel = new DefaultEntityTableModel(entityID, connectionProvider) {
      /** {@inheritDoc} */
      @Override
      protected List<Entity> performQuery(final Criteria<Property.ColumnProperty> criteria) {
        return new ArrayList<Entity>(entities);
      }
    };
    tableModel.setQueryConfigurationAllowed(false);
    tableModel.setEditModel(editModel);
    final EntityTablePanel tablePanel = new EntityTablePanel(tableModel, null, null, null, null);
    tablePanel.initializePanel();
    tableModel.refresh();

    return tablePanel;
  }

  /**
   * Initializes the UI
   * @return this EntityTablePanel instance
   */
  public final EntityTablePanel initializePanel() {
    if (!panelInitialized) {
      try {
        setupControls();
        final TableCellRenderer tableCellRenderer = initializeTableCellRenderer();
        final Enumeration<TableColumn> columnEnumeration = getTableModel().getColumnModel().getColumns();
        while (columnEnumeration.hasMoreElements()) {
          final TableColumn column = columnEnumeration.nextElement();
          column.setCellRenderer(tableCellRenderer);
          column.setResizable(true);
        }
        setTablePopupMenu(getJTable(), getPopupControls(additionalPopupControls));
        final JPanel tableSearchAndSummaryPanel = new JPanel(new BorderLayout());
        setLayout(new BorderLayout());
        if (includeSearchPanel && searchScrollPane != null) {
          tableSearchAndSummaryPanel.add(searchScrollPane, BorderLayout.NORTH);
          if (searchPanel instanceof EntityTableSearchAdvancedPanel) {
            searchScrollPane.getHorizontalScrollBar().setModel(getTableScrollPane().getHorizontalScrollBar().getModel());
          }
          if (searchPanel instanceof EntityTableSearchAdvancedPanel) {
            ((EntityTableSearchAdvancedPanel) searchPanel).addAdvancedListener(new ActionListener() {
              /** {@inheritDoc} */
              public void actionPerformed(final ActionEvent e) {
                if (isSearchPanelVisible()) {
                  revalidate();
                }
              }
            });
          }
        }
        final JScrollPane tableScrollPane = getTableScrollPane();
        tableSearchAndSummaryPanel.add(tableScrollPane, BorderLayout.CENTER);
        add(tableSearchAndSummaryPanel, BorderLayout.CENTER);
        if (summaryScrollPane != null) {
          tableScrollPane.getViewport().addChangeListener(new ChangeListener() {
            /** {@inheritDoc} */
            public void stateChanged(final ChangeEvent e) {
              horizontalTableScrollBar.setVisible(tableScrollPane.getViewport().getViewSize().width > tableScrollPane.getSize().width);
              revalidate();
            }
          });
          summaryScrollPane.getHorizontalScrollBar().setModel(horizontalTableScrollBar.getModel());
          tableSearchAndSummaryPanel.add(summaryBasePanel, BorderLayout.SOUTH);
        }

        if (includeSouthPanel) {
          final JPanel southPanel = new JPanel(new BorderLayout(5, 5));
          final JPanel southPanelCenter = initializeSouthPanel();
          if (southPanelCenter != null) {
            final JToolBar southToolBar = initializeToolbar();
            if (southToolBar != null) {
              southPanelCenter.add(southToolBar, BorderLayout.EAST);
            }
            southPanel.add(southPanelCenter, BorderLayout.SOUTH);
            add(southPanel, BorderLayout.SOUTH);
          }
        }
        bindEvents();
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
    final JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
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
   * Adds a popup menu to <code>table</code>
   * @param table the table
   * @param popupControls a ControlSet specifying the controls in the popup menu
   */
  protected final void setTablePopupMenu(final JTable table, final ControlSet popupControls) {
    if (popupControls.size() == 0) {
      return;
    }

    final JPopupMenu popupMenu = ControlProvider.createPopupMenu(popupControls);
    table.setComponentPopupMenu(popupMenu);
    table.getTableHeader().setComponentPopupMenu(popupMenu);
    if (table.getParent() != null) {
      ((JComponent) table.getParent()).setComponentPopupMenu(popupMenu);
    }
    UiUtil.addKeyEvent(table, KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_FOCUSED,
            new PopupMenuAction(popupMenu, table));
    UiUtil.addKeyEvent(table, KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_FOCUSED,
            new AbstractAction("showEntityMenu") {
              /** {@inheritDoc} */
              public void actionPerformed(final ActionEvent e) {
                showEntityMenu(getPopupLocation(table));
              }
            });
  }

  /**
   * Associates <code>control</code> with <code>controlCode</code>
   * @param controlCode the control code
   * @param control the control to associate with <code>controlCode</code>
   */
  protected final void setControl(final String controlCode, final Control control) {
    if (control == null) {
      controlMap.remove(controlCode);
    }
    else {
      controlMap.put(controlCode, control);
    }
  }

  protected ControlSet getToolbarControls(final ControlSet additionalToolbarControls) {
    final ControlSet toolbarControls = new ControlSet("");
    if (controlMap.containsKey(TOGGLE_SUMMARY_PANEL)) {
      toolbarControls.add(controlMap.get(TOGGLE_SUMMARY_PANEL));
    }
    if (controlMap.containsKey(TOGGLE_SEARCH_PANEL)) {
      toolbarControls.add(controlMap.get(TOGGLE_SEARCH_PANEL));
    }
    if (controlMap.containsKey(CONFIGURE_QUERY)) {
      toolbarControls.add(controlMap.get(CONFIGURE_QUERY));
      toolbarControls.addSeparator();
    }
    if (controlMap.containsKey(DELETE_SELECTED)) {
      toolbarControls.add(controlMap.get(DELETE_SELECTED));
      toolbarControls.addSeparator();
    }
    toolbarControls.add(getPrintTableControl());
    toolbarControls.addSeparator();
    toolbarControls.add(controlMap.get(CLEAR_SELECTION));
    toolbarControls.addSeparator();
    toolbarControls.add(controlMap.get(MOVE_SELECTION_UP));
    toolbarControls.add(controlMap.get(MOVE_SELECTION_DOWN));
    if (additionalToolbarControls != null && additionalToolbarControls.size() > 0) {
      toolbarControls.addSeparator();
      for (final Action action : additionalToolbarControls.getActions()) {
        if (action == null) {
          toolbarControls.addSeparator();
        }
        else {
          toolbarControls.add(action);
        }
      }
    }

    return toolbarControls;
  }

  protected ControlSet getPopupControls(final ControlSet additionalPopupControls) {
    final ControlSet popupControls = new ControlSet("");
    popupControls.add(controlMap.get(REFRESH));
    popupControls.add(controlMap.get(CLEAR));
    popupControls.addSeparator();
    if (additionalPopupControls != null && !additionalPopupControls.getActions().isEmpty()) {
      if (additionalPopupControls.hasName()) {
        popupControls.add(additionalPopupControls);
      }
      else {
        popupControls.addAll(additionalPopupControls);
      }
      popupControls.addSeparator();
    }
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
      popupControls.add(controlMap.get(CONFIGURE_QUERY));
      if (searchPanel != null) {
        final ControlSet searchControls = searchPanel.getControls();
        if (controlMap.containsKey(SEARCH_PANEL_VISIBLE)) {
          searchControls.add(getControl(SEARCH_PANEL_VISIBLE));
        }
        if (searchControls != null) {
          popupControls.add(searchControls);
        }
      }
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

  protected final ToggleBeanValueLink getSearchPanelControl() {
    return Controls.toggleControl(this, "searchPanelVisible",
            FrameworkMessages.get(FrameworkMessages.SHOW), evtSearchPanelVisibilityChanged);
  }

  protected final ControlSet getCopyControlSet() {
    return new ControlSet(Messages.get(Messages.COPY), getCopyCellControl(), getCopyTableWithHeaderControl());
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

  protected String[] getConfirmDeleteMessages() {
    return FrameworkMessages.getDefaultConfirmDeleteMessages();
  }

  /**
   * @return a Control for refreshing the underlying table data
   */
  protected final Control getRefreshControl() {
    final String refreshCaption = FrameworkMessages.get(FrameworkMessages.REFRESH);
    return Controls.methodControl(getEntityTableModel(), "refresh", refreshCaption,
            null, FrameworkMessages.get(FrameworkMessages.REFRESH_TIP), refreshCaption.charAt(0),
            null, Images.loadImage(Images.IMG_REFRESH_16));
  }

  /**
   * @return a Control for clearing the underlying table model, that is, removing all rows
   */
  protected final Control getClearControl() {
    final String clearCaption = FrameworkMessages.get(FrameworkMessages.CLEAR);
    return Controls.methodControl(getEntityTableModel(), "clear", clearCaption,
            null, null, clearCaption.charAt(0), null, null);
  }

  /**
   * Initializes the panel containing the table column summary panels
   * @return the summary panel
   */
  protected final EntityTableSummaryPanel initializeSummaryPanel() {
    final EntityTableSummaryPanel panel = new EntityTableSummaryPanel(getEntityTableModel());
    panel.setVerticalFillerWidth(UiUtil.getPreferredScrollBarWidth());

    return panel;
  }

  /**
   * Provides value input components for multiple entity update, override to supply
   * specific InputValueProvider implementations for properties.
   * Remember to return with a call to super.getInputProviderInputProvider().
   * @param property the property for which to get the InputProvider
   * @param toUpdate the entities that are about to be updated
   * @return the InputProvider handling input for <code>property</code>
   * @see #updateSelectedEntities
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected InputProvider getInputProvider(final Property property, final List<Entity> toUpdate) {
    final Collection<Object> values = EntityUtil.getDistinctPropertyValues(property.getPropertyID(), toUpdate);
    final Object currentValue = values.size() == 1 ? values.iterator().next() : null;
    if (property instanceof Property.ValueListProperty) {
      return new ValueListInputProvider(currentValue, ((Property.ValueListProperty) property).getValues());
    }
    if (property.isTimestamp()) {
      return new DateInputProvider((Date) currentValue, Configuration.getDefaultTimestampFormat());
    }
    if (property.isDate()) {
      return new DateInputProvider((Date) currentValue, Configuration.getDefaultDateFormat());
    }
    if (property.isDouble()) {
      return new DoubleInputProvider((Double) currentValue);
    }
    if (property.isInteger()) {
      return new IntInputProvider((Integer) currentValue);
    }
    if (property.isBoolean()) {
      return new BooleanInputProvider((Boolean) currentValue);
    }
    if (property.isString()) {
      return new TextInputProvider(property.getCaption(), getEntityTableModel().getEditModel().getValueProvider(property), (String) currentValue);
    }
    if (property instanceof Property.ForeignKeyProperty) {
      return createEntityInputProvider((Property.ForeignKeyProperty) property, (Entity) currentValue, getEntityTableModel().getEditModel());
    }

    throw new IllegalArgumentException("Unsupported property type: " + property.getType());
  }

  /**
   * Creates a InputProvider for the given foreign key property
   * @param foreignKeyProperty the property
   * @param currentValue the current value to initialize the InputProvider with
   * @param editModel the edit model involved in the updating
   * @return a Entity InputProvider
   */
  protected final InputProvider createEntityInputProvider(final Property.ForeignKeyProperty foreignKeyProperty, final Entity currentValue,
                                                          final EntityEditModel editModel) {
    if (Entities.isSmallDataset(foreignKeyProperty.getReferencedEntityID())) {
      return new EntityComboProvider(editModel.createEntityComboBoxModel(foreignKeyProperty), currentValue);
    }
    else {
      final List<Property.ColumnProperty> searchProperties = Entities.getSearchProperties(foreignKeyProperty.getReferencedEntityID());
      if (searchProperties.isEmpty()) {
        throw new IllegalArgumentException("No searchable properties found for entity: " + foreignKeyProperty.getReferencedEntityID());
      }

      return new EntityLookupProvider(editModel.createEntityLookupModel(foreignKeyProperty.getReferencedEntityID(), searchProperties, null), currentValue);
    }
  }

  /**
   * Returns the TableCellRenderer used for this EntityTablePanel
   * @return the TableCellRenderer
   */
  protected TableCellRenderer initializeTableCellRenderer() {
    return new EntityTableCellRenderer(getEntityTableModel());
  }

  /**
   * Initialize the MouseListener for the table component.
   * The default implementation simply invokes the action returned
   * by <code>getDoubleClickAction()</code> on a double click with
   * the JTable as the ActionEvent source.
   * @return the MouseListener for the table
   * @see #getTableDoubleClickAction()
   */
  protected final MouseListener initializeTableMouseListener() {
    return new MouseAdapter() {
      /** {@inheritDoc} */
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (e.getClickCount() == 2) {
          final Action doubleClickAction = getTableDoubleClickAction();
          if (doubleClickAction != null) {
            doubleClickAction.actionPerformed(new ActionEvent(getJTable(), -1, "doubleClick"));
          }
        }
        else if (e.isAltDown()) {
          showEntityMenu(e.getPoint());
        }
      }
    };
  }

  /**
   * Initializes the south panel toolbar, by default based on <code>getToolbarControls()</code>
   * @return the toolbar to add to the south panel
   */
  protected JToolBar initializeToolbar() {
    final ControlSet toolbarControlSet = getToolbarControls(additionalToolbarControls);
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

  /**
   * Initializes the controls available to this EntityTablePanel by mapping them to their respective
   * control codes (EntityTablePanel.UPDATE_SELECTED, DELETE_SELECTED etc) via the <code>setControl(String, Control) method,
   * these can then be retrieved via the <code>getControl(String)</code> method.
   * @see org.jminor.common.ui.control.Control
   * @see #setControl(String, org.jminor.common.ui.control.Control)
   * @see #getControl(String)
   */
  private void setupControls() {
    if (!getEntityTableModel().isReadOnly() && getEntityTableModel().isDeleteAllowed()) {
      setControl(DELETE_SELECTED, getDeleteSelectedControl());
    }
    if (!getEntityTableModel().isReadOnly() && getEntityTableModel().isUpdateAllowed() && getEntityTableModel().isBatchUpdateAllowed()) {
      setControl(UPDATE_SELECTED, getUpdateSelectedControlSet());
    }
    if (getEntityTableModel().isQueryConfigurationAllowed()) {
      setControl(CONFIGURE_QUERY, getConfigureQueryControl());
      setControl(SEARCH_PANEL_VISIBLE, getSearchPanelControl());
    }
    setControl(CLEAR, getClearControl());
    setControl(REFRESH, getRefreshControl());
    setControl(SELECT_COLUMNS, getSelectColumnsControl());
    if (Configuration.entitySerializerAvailable()) {
      setControl(EXPORT_JSON, getExportControl());
    }
    setControl(VIEW_DEPENDENCIES, getViewDependenciesControl());
    if (summaryPanel != null) {
      setControl(TOGGLE_SUMMARY_PANEL, getToggleSummaryPanelControl());
    }
    if (searchPanel != null) {
      setControl(TOGGLE_SEARCH_PANEL, getToggleSearchPanelControl());
    }
    setControl(PRINT_TABLE, getPrintTableControl());
    setControl(CLEAR_SELECTION, getClearSelectionControl());
    setControl(MOVE_SELECTION_UP, getMoveSelectionDownControl());
    setControl(MOVE_SELECTION_DOWN, getMoveSelectionUpControl());
    setControl(COPY_TABLE_DATA, getCopyControlSet());
  }

  private Control getCopyCellControl() {
    return new Control(FrameworkMessages.get(FrameworkMessages.COPY_CELL),
            getEntityTableModel().getSelectionEmptyState().getReversedState()) {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        final JTable table = getJTable();
        final Object value = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn());
        Util.setClipboard(value == null ? "" : value.toString());
      }
    };
  }

  private Control getCopyTableWithHeaderControl() {
    return new Control(FrameworkMessages.get(FrameworkMessages.COPY_TABLE_WITH_HEADER)) {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        copyTableAsDelimitedString();
      }
    };
  }

  private void copyTableAsDelimitedString() {
    final List<String> headerValues = new ArrayList<String>();
    final List<Property> properties = new ArrayList<Property>(getEntityTableModel().getTableColumnProperties());
    for (final Property property : properties) {
      headerValues.add(property.getCaption());
    }

    final String[][] header = {headerValues.toArray(new String[headerValues.size()])};

    final List<Entity> entities = getEntityTableModel().isSelectionEmpty()
            ? getEntityTableModel().getVisibleItems() : getEntityTableModel().getSelectedItems();

    final String[][] data = new String[entities.size()][];
    for (int i = 0; i < data.length; i++) {
      final List<String> line = new ArrayList<String>();
      for (final Property property : properties) {
        line.add(entities.get(i).getValueAsString(property));
      }

      data[i] = line.toArray(new String[line.size()]);
    }
    Util.setClipboard(Util.getDelimitedString(header, data, "\t"));
  }

  /**
   * @return the refresh toolbar
   */
  private JToolBar initializeRefreshToolbar() {
    final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    final String keyName = stroke.toString().replace("pressed ", "");
    final Control refresh = Controls.methodControl(getEntityTableModel(), "refresh", null,
            getEntityTableModel().getSearchModel().getSearchStateChangedState(), FrameworkMessages.get(FrameworkMessages.REFRESH_TIP)
                    + " (" + keyName + ")", 0, null, Images.loadImage(Images.IMG_STOP_16));

    final InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
    final ActionMap actionMap = getActionMap();

    inputMap.put(stroke, "refreshControl");
    actionMap.put("refreshControl", refresh);

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

  private void showEntityMenu(final Point location) {
    final Entity entity = getEntityTableModel().getSelectedItem();
    if (entity != null) {
      final JPopupMenu popupMenu = new JPopupMenu();
      populateEntityMenu(popupMenu, (Entity) entity.getCopy(), getEntityTableModel().getConnectionProvider());
      popupMenu.show(getTableScrollPane(), location.x, (int) location.getY() - (int) getTableScrollPane().getViewport().getViewPosition().getY());
    }
  }

  private Property getPropertyToUpdate() throws CancelException {
    final JComboBox box = new JComboBox(EntityUtil.getUpdatableProperties(getEntityTableModel().getEntityID()).toArray());
    final int ret = JOptionPane.showOptionDialog(this, box,
            FrameworkMessages.get(FrameworkMessages.SELECT_PROPERTY_FOR_UPDATE),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);

    if (ret == JOptionPane.OK_OPTION) {
      return (Property) box.getSelectedItem();
    }
    else {
      throw new CancelException();
    }
  }

  private void updateStatusMessage() {
    if (statusMessageLabel != null) {
      final String status = getEntityTableModel().getStatusMessage();
      statusMessageLabel.setText(status);
      statusMessageLabel.setToolTipText(status);
    }
  }

  private void bindEvents() {
    if (!getEntityTableModel().isReadOnly() && getEntityTableModel().isDeleteAllowed()) {
      getJTable().addKeyListener(new KeyAdapter() {
        /** {@inheritDoc} */
        @Override
        public void keyTyped(final KeyEvent e) {
          if (e.getKeyChar() == KeyEvent.VK_DELETE && !getEntityTableModel().getSelectionEmptyState().isActive()) {
            try {
              delete();
            }
            catch (DatabaseException ex) {
              throw new RuntimeException(ex);
            }
            catch (CancelException ce) {/**/}
          }
          else if (getJTable().getParent() != null) {
            getJTable().getParent().dispatchEvent(e);
          }
        }
      });
    }
    getEntityTableModel().addRefreshStartedListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        UiUtil.setWaitCursor(true, EntityTablePanel.this);
      }
    });
    getEntityTableModel().addRefreshDoneListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        UiUtil.setWaitCursor(false, EntityTablePanel.this);
      }
    });
    final ActionListener statusListener = new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        updateStatusMessage();
      }
    };
    getEntityTableModel().addSelectionChangedListener(statusListener);
    getEntityTableModel().addFilteringListener(statusListener);
    getEntityTableModel().addTableDataChangedListener(statusListener);

    getEntityTableModel().addSelectedIndexListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        if (!getEntityTableModel().getSelectionEmptyState().isActive()) {
          scrollToCoordinate(getEntityTableModel().getSelectedIndex(), getJTable().getSelectedColumn());
        }
      }
    });

    getEntityTableModel().getSearchModel().getSearchStateChangedState().addListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        getJTable().getTableHeader().repaint();
        getJTable().repaint();
      }
    });

    if (getEntityTableModel().hasEditModel()) {
      getEntityTableModel().getEditModel().addEntitiesChangedListener(new ActionListener() {
        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent e) {
          getJTable().repaint();
        }
      });
    }
  }

  private void initializeTable() {
    getJTable().addMouseListener(initializeTableMouseListener());

    final JTableHeader header = getJTable().getTableHeader();
    final TableCellRenderer defaultHeaderRenderer = header.getDefaultRenderer();
    final Font defaultFont = getJTable().getFont();
    final Font searchFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
    header.setDefaultRenderer(new TableCellRenderer() {
      /** {@inheritDoc} */
      public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                     final boolean hasFocus, final int row, final int column) {
        final JLabel label = (JLabel) defaultHeaderRenderer.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
        final EntityTableModel tableModel = getEntityTableModel();
        final Property property = (Property) tableModel.getColumnModel().getColumn(column).getIdentifier();
        label.setFont(tableModel.getSearchModel().isSearchEnabled(property.getPropertyID()) ? searchFont : defaultFont);

        return label;
      }
    });
    header.setFocusable(false);
    header.setReorderingAllowed(Configuration.getBooleanValue(Configuration.ALLOW_COLUMN_REORDERING));
    getJTable().setAutoResizeMode(Configuration.getIntValue(Configuration.TABLE_AUTO_RESIZE_MODE));
  }

  private static void showDependenciesDialog(final Map<String, Collection<Entity>> dependencies,
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
    UiUtil.showInDialog(UiUtil.getParentWindow(dialogParent), dependenciesPanel,
            true, FrameworkMessages.get(FrameworkMessages.DEPENDENT_RECORDS_FOUND), true, true, null);
  }

  private static JLabel initializeStatusMessageLabel() {
    final JLabel label  = new JLabel("", JLabel.CENTER);
    label.setFont(new Font(label.getFont().getName(), Font.PLAIN, STATUS_MESSAGE_FONT_SIZE));

    return label;
  }

  private static JPanel createDependenciesPanel(final Map<String, Collection<Entity>> dependencies,
                                                final EntityConnectionProvider connectionProvider) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    tabPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final Map.Entry<String, Collection<Entity>> entry : dependencies.entrySet()) {
      final Collection<Entity> dependantEntities = entry.getValue();
      if (!dependantEntities.isEmpty()) {
        tabPane.addTab(Entities.getCaption(entry.getKey()), createStaticEntityTablePanel(dependantEntities, connectionProvider));
      }
    }
    panel.add(tabPane, BorderLayout.CENTER);

    return panel;
  }

  @SuppressWarnings({"unchecked"})
  private static List<ColumnSearchPanel<Property>> initializeFilterPanels(final EntityTableModel tableModel) {
    final List<ColumnSearchPanel<Property>> filterPanels = new ArrayList<ColumnSearchPanel<Property>>(tableModel.getColumnCount());
    final Enumeration<TableColumn> columns = tableModel.getColumnModel().getColumns();
    while (columns.hasMoreElements()) {
      final Property columnProperty = (Property) columns.nextElement().getIdentifier();
      final ColumnSearchModel<Property> model = tableModel.getSearchModel().getPropertyFilterModel(columnProperty.getPropertyID());
      filterPanels.add(new PropertyFilterPanel(model, true, true));
    }

    return filterPanels;
  }

  private static Point getPopupLocation(final JTable table) {
    final int x = table.getBounds().getLocation().x + 42;
    final int y = table.getSelectionModel().isSelectionEmpty() ? 100 : (table.getSelectedRow() + 1) * table.getRowHeight();

    return new Point(x, y);
  }

  /**
   * Populates the given root menu with the property values of the given entity
   * @param rootMenu the menu to populate
   * @param entity the entity
   * @param connectionProvider if provided then lazy loaded entity references are loaded so that the full object graph can be shown
   */
  private static void populateEntityMenu(final JComponent rootMenu, final Entity entity,
                                         final EntityConnectionProvider connectionProvider) {
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<Property.PrimaryKeyProperty>(Entities.getPrimaryKeyProperties(entity.getEntityID())));
    populateForeignKeyMenu(rootMenu, entity, connectionProvider, new ArrayList<Property.ForeignKeyProperty>(Entities.getForeignKeyProperties(entity.getEntityID())));
    populateValueMenu(rootMenu, entity, new ArrayList<Property>(Entities.getProperties(entity.getEntityID(), true)));
  }

  private static void populatePrimaryKeyMenu(final JComponent rootMenu, final Entity entity, final List<Property.PrimaryKeyProperty> primaryKeyProperties) {
    Util.collate(primaryKeyProperties);
    for (final Property.PrimaryKeyProperty property : primaryKeyProperties) {
      rootMenu.add(new JMenuItem("[PK] " + property.getColumnName() + ": " + entity.getValueAsString(property.getPropertyID())));
    }
  }

  private static void populateForeignKeyMenu(final JComponent rootMenu, final Entity entity,
                                             final EntityConnectionProvider connectionProvider,
                                             final List<Property.ForeignKeyProperty> fkProperties) {
    try {
      Util.collate(fkProperties);
      for (final Property.ForeignKeyProperty property : fkProperties) {
        final boolean fkValueNull = entity.isForeignKeyNull(property);
        if (!fkValueNull) {
          boolean queried = false;
          Entity referencedEntity = entity.getForeignKeyValue(property.getPropertyID());
          if (referencedEntity == null) {
            referencedEntity = connectionProvider.getConnection().selectSingle(entity.getReferencedPrimaryKey(property));
            entity.removeValue(property.getPropertyID());
            entity.setValue(property, referencedEntity);
            queried = true;
          }
          final StringBuilder text = new StringBuilder("[FK").append(queried ? "+" : "")
                  .append("] ").append(property.getCaption()).append(": ");
          text.append(referencedEntity.toString());
          final JMenu foreignKeyMenu = new JMenu(text.toString());
          populateEntityMenu(foreignKeyMenu, entity.getForeignKeyValue(property.getPropertyID()), connectionProvider);
          rootMenu.add(foreignKeyMenu);
        }
        else {
          final StringBuilder text = new StringBuilder("[FK] ").append(property.getCaption()).append(": <null>");
          rootMenu.add(new JMenuItem(text.toString()));
        }
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static void populateValueMenu(final JComponent rootMenu, final Entity entity, final List<Property> properties) {
    Util.collate(properties);
    final int maxValueLength = 20;
    for (final Property property : properties) {
      if (!property.hasParentProperty() && !(property instanceof Property.ForeignKeyProperty)) {
        final String prefix = "[" + Util.getTypeClass(property.getType()).getSimpleName().substring(0, 1)
                + (property instanceof Property.DenormalizedViewProperty ? "*" : "")
                + (property instanceof Property.DenormalizedProperty ? "+" : "") + "] ";
        final String value = entity.isValueNull(property.getPropertyID()) ? "<null>" : entity.getValueAsString(property.getPropertyID());
        final boolean longValue = value != null && value.length() > maxValueLength;
        final JMenuItem menuItem = new JMenuItem(prefix + property + ": " + (longValue ? value.substring(0, maxValueLength) + "..." : value));
        if (longValue) {
          menuItem.setToolTipText(value.length() > 1000 ? value.substring(0, 1000) : value);
        }
        rootMenu.add(menuItem);
      }
    }
  }

  private static final class PopupMenuAction extends AbstractAction {
    private final JPopupMenu popupMenu;
    private final JTable table;

    private PopupMenuAction(final JPopupMenu popupMenu, final JTable table) {
      super("showPopupMenu");
      this.popupMenu = popupMenu;
      this.table = table;
    }

    /** {@inheritDoc} */
    public void actionPerformed(final ActionEvent e) {
      final Point location = getPopupLocation(table);
      popupMenu.show(table, location.x, location.y);
    }
  }
}
