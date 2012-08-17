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
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.Serializer;
import org.jminor.common.model.StateObserver;
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

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
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
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * @see org.jminor.framework.client.model.EntityTableModel
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
   * the column summary panel
   */
  private final EntityTableSummaryPanel summaryPanel;

  /**
   * a panel for the table, column search and column summary panels
   */
  private final JPanel tableSearchAndSummaryPanel = new JPanel(new BorderLayout());

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

  private final List<ControlSet> additionalPopupControlSets = new ArrayList<ControlSet>();
  private final List<ControlSet> additionalToolbarControlSets = new ArrayList<ControlSet>();

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

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   */
  public EntityTablePanel(final EntityTableModel tableModel) {
    this(tableModel, new EntityTableSearchPanel(tableModel.getSearchModel(), tableModel.getColumnModel()),
            new EntityTableSummaryPanel(tableModel));
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param searchPanel the search panel
   */
  public EntityTablePanel(final EntityTableModel tableModel, final EntityTableSearchPanel searchPanel) {
    this(tableModel, searchPanel, new EntityTableSummaryPanel(tableModel));
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param summaryPanel the summary panel
   */
  public EntityTablePanel(final EntityTableModel tableModel, final EntityTableSummaryPanel summaryPanel) {
    this(tableModel, new EntityTableSearchPanel(tableModel.getSearchModel(), tableModel.getColumnModel()),
            summaryPanel);
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param searchPanel the search panel
   * @param summaryPanel the summary panel
   */
  public EntityTablePanel(final EntityTableModel tableModel, final EntityTableSearchPanel searchPanel,
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
   * @param value true if the search panel should be included
   * @see #initializePanel()
   * @throws IllegalStateException in case the panel has already been initialized
   */
  public final void setIncludeSearchPanel(final boolean value) {
    checkIfInitialized();
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
        @Override
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
      searchScrollPane.getViewport().setView(visible ? searchPanel : null);
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
    if (!searchPanel.isSimpleSearch()) {
      if (isSearchPanelVisible()) {
        if (searchPanel.isAdvanced()) {
          setSearchPanelVisible(false);
        }
        else {
          searchPanel.setAdvanced(true);
        }
      }
      else {
        searchPanel.setAdvanced(false);
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
   * @throws IllegalArgumentException in case no control is associated with the given control code
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
   * @see #initializePanel()
   * @throws IllegalStateException in case the underlying model is read only or if updating is not allowed
   * @see #includeUpdateSelectedProperty(org.jminor.framework.domain.Property)
   */
  public ControlSet getUpdateSelectedControlSet() {
    if (getEntityTableModel().isReadOnly() || !getEntityTableModel().isUpdateAllowed()
            || !getEntityTableModel().isBatchUpdateAllowed()) {
      throw new IllegalStateException("Table model is read only or does not allow updates");
    }
    final StateObserver enabled = getEntityTableModel().getSelectionEmptyObserver().getReversedObserver();
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED),
            (char) 0, Images.loadImage("Modify16.gif"), enabled);
    controlSet.setDescription(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP));
    for (final Property property : EntityUtil.getUpdatableProperties(getEntityTableModel().getEntityID())) {
      if (includeUpdateSelectedProperty(property)) {
        final String caption = property.getCaption() == null ? property.getPropertyID() : property.getCaption();
        controlSet.add(UiUtil.linkToEnabledState(enabled, new AbstractAction(caption) {
          /** {@inheritDoc} */
          @Override
          public void actionPerformed(final ActionEvent e) {
            updateSelectedEntities(property);
          }
        }));
      }
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
            getEntityTableModel().getSelectionEmptyObserver().getReversedObserver(),
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
    return Controls.methodControl(this, "delete", FrameworkMessages.get(FrameworkMessages.DELETE),
            States.aggregateState(Conjunction.AND,
                    getEntityTableModel().getEditModel().getAllowDeleteObserver(),
                    getEntityTableModel().getSelectionEmptyObserver().getReversedObserver()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP), 0, null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for exporting the selected records to file
   */
  public final Control getExportControl() {
    return Controls.methodControl(this, "exportSelected",
            FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED) + TRIPLEDOT,
            getEntityTableModel().getSelectionEmptyObserver().getReversedObserver(),
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
    final Property toUpdate = getPropertyToUpdate();
    if (toUpdate != null) {//null if cancelled
      updateSelectedEntities(toUpdate);
    }
  }

  /**
   * Retrieves a new property value via input dialog and performs an update on the selected entities
   * @param propertyToUpdate the property to update
   * @see #getInputProvider(org.jminor.framework.domain.Property, java.util.List)
   */
  public final void updateSelectedEntities(final Property propertyToUpdate) {
    if (getEntityTableModel().getSelectionEmptyObserver().isActive()) {
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
    if (inputPanel.isInputAccepted()) {
      EntityUtil.setPropertyValue(propertyToUpdate.getPropertyID(), inputPanel.getValue(), selectedEntities);
      try {
        UiUtil.setWaitCursor(true, this);
        getEntityTableModel().update(selectedEntities);
      }
      catch (ValidationException e) {
        handleException(e);
      }
      catch (DatabaseException e) {
        handleException(e);
      }
      catch (CancelException e) {
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
    if (getTableModel().isSelectionEmpty()) {
      return;
    }

    final EntityTableModel tableModel = getEntityTableModel();
    try {
      UiUtil.setWaitCursor(true, this);
      final Map<String, Collection<Entity>> dependencies =
              tableModel.getConnectionProvider().getConnection().selectDependentEntities(tableModel.getSelectedItems());
      if (!dependencies.isEmpty()) {
        showDependenciesDialog(dependencies, tableModel.getConnectionProvider(), this);
      }
      else {
        JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.NONE_FOUND),
                FrameworkMessages.get(FrameworkMessages.NO_DEPENDENT_RECORDS), JOptionPane.INFORMATION_MESSAGE);
      }
    }
    catch (DatabaseException e) {
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
      catch (DatabaseException e) {
        handleException(e);
      }
      catch (CancelException e) {
        handleException(e);
      }
      finally {
        UiUtil.setWaitCursor(false, this);
      }
    }
  }

  /**
   * Exports the selected records as a text file using the available serializer
   * @see org.jminor.framework.domain.EntityUtil#getEntitySerializer()
   * @see Configuration#ENTITY_SERIALIZER_CLASS
   */
  public final void exportSelected() {
    try {
      final List<Entity> selected = getEntityTableModel().getSelectedItems();
      Util.writeFile(EntityUtil.getEntitySerializer().serialize(selected), UiUtil.chooseFileToSave(this, null, null));
      JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED_DONE));
    }
    catch (IOException e) {
      handleException(e);
    }
    catch (Serializer.SerializeException e) {
      handleException(e);
    }
    catch (CancelException e) {
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
   * @see DefaultExceptionHandler#handleException(Throwable, javax.swing.JComponent)
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

    final Control toggleControl = new Control() {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        toggleSearchPanel();
      }
    };
    toggleControl.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    toggleControl.setDescription(FrameworkMessages.get(FrameworkMessages.SEARCH));

    return toggleControl;
  }

  /**
   * @return a control for clearing the table selection
   */
  public final Control getClearSelectionControl() {
    final Control clearSelection = Controls.methodControl(getEntityTableModel(), "clearSelection", null,
            getEntityTableModel().getSelectionEmptyObserver().getReversedObserver(), null, -1, null,
            Images.loadImage("ClearSelection16.gif"));
    clearSelection.setDescription(FrameworkMessages.get(FrameworkMessages.CLEAR_SELECTION_TIP));

    return clearSelection;
  }

  /**
   * @return a control for moving the table selection down one index
   */
  public final Control getMoveSelectionDownControl() {
    final Control selectionDown = Controls.methodControl(getEntityTableModel(), "moveSelectionDown",
            Images.loadImage(Images.IMG_DOWN_16));
    selectionDown.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_DOWN_TIP));

    return selectionDown;
  }

  /**
   * @return a control for moving the table selection up one index
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
  public final void addSearchPanelVisibleListener(final EventListener listener) {
    evtSearchPanelVisibilityChanged.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeSearchPanelVisibleListener(final EventListener listener) {
    evtSearchPanelVisibilityChanged.removeListener(listener);
  }

  /**
   * @param listener a listener notified each time the summary panel visibility changes
   */
  public final void addSummaryPanelVisibleListener(final EventListener listener) {
    evtSummaryPanelVisibilityChanged.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeSummaryPanelVisibleListener(final EventListener listener) {
    evtSummaryPanelVisibilityChanged.removeListener(listener);
  }

  /**
   * @param listener a listener notified each time the table is double clicked
   */
  public final void addTableDoubleClickListener(final EventListener listener) {
    evtTableDoubleClicked.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeTableDoubleClickListener(final EventListener listener) {
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
      throw new IllegalArgumentException("Cannot create a static EntityTablePanel without the entities");
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
    final EntityTablePanel tablePanel = new EntityTablePanel(tableModel, null, null);
    tablePanel.initializePanel();
    tableModel.refresh();

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
        UiUtil.setWaitCursor(true, this);
        setupControlsInternal();
        setupControls();
        initializeTable();
        initializeUI();
        bindEvents();
        updateStatusMessage();
        initialize();
      }
      finally {
        panelInitialized = true;
        UiUtil.setWaitCursor(false, this);
      }
    }

    return this;
  }

  /**
   * Override to add code that should be called during the initialization routine after the panel has been initialized
   * @see #initializePanel()
   */
  protected void initialize() {}

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
   * Adds a popup menu to <code>table</code>, null or an empty ControlSet mean no popup menu
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
    UiUtil.addKeyEvent(table, KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK,
            new PopupMenuAction(popupMenu, table));
    UiUtil.addKeyEvent(table, KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK,
            new AbstractAction("EntityTablePanel.showEntityMenu") {
              /** {@inheritDoc} */
              @Override
              public void actionPerformed(final ActionEvent e) {
                showEntityMenu(getPopupLocation(table));
              }
            });
  }

  /**
   * Override the default controls by mapping them to their respective control codes
   * ({@link EntityTablePanel#UPDATE_SELECTED}, {@link EntityTablePanel#DELETE_SELECTED} etc)
   * via the <code>setControl(String, Control) method,
   * these can then be retrieved via the <code>getControl(String)</code> method.
   * @see org.jminor.common.ui.control.Control
   * @see #setControl(String, org.jminor.common.ui.control.Control)
   * @see #getControl(String)
   */
  protected void setupControls() {}

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

  protected ControlSet getToolbarControls(final List<ControlSet> additionalToolbarControlSets) {
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
    }
    toolbarControls.add(getPrintTableControl());
    toolbarControls.add(controlMap.get(CLEAR_SELECTION));
    toolbarControls.addSeparator();
    toolbarControls.add(controlMap.get(MOVE_SELECTION_UP));
    toolbarControls.add(controlMap.get(MOVE_SELECTION_DOWN));
    for (final ControlSet controlSet : additionalToolbarControlSets) {
      toolbarControls.addSeparator();
      for (final Action action : controlSet.getActions()) {
        toolbarControls.add(action);
      }
    }

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
    for (final ControlSet controlSet : additionalPopupControlSets) {
      if (controlSet.hasName()) {
        popupControls.add(controlSet);
      }
      else {
        popupControls.addAll(controlSet);
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
        final ControlSet searchControls = new ControlSet(FrameworkMessages.get(FrameworkMessages.SEARCH));
        if (controlMap.containsKey(SEARCH_PANEL_VISIBLE)) {
          searchControls.add(getControl(SEARCH_PANEL_VISIBLE));
        }
        final ControlSet searchPanelControls = searchPanel.getControls();
        if (searchPanelControls != null) {
          searchControls.addAll(searchPanelControls);
        }
        if (searchControls.size() > 0) {
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

  protected String[] getConfirmDeleteMessages() {
    return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_SELECTED),
            FrameworkMessages.get(FrameworkMessages.DELETE)};
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
      return new EntityLookupProvider(editModel.createEntityLookupModel(foreignKeyProperty), currentValue);
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
   * Sets the layout and lays out the given components, as well as any additional components, on this table panel
   * @param tableSearchAndSummaryPanel the panel containing the actual table, with search and summary panels
   * @param southPanel the panel to add to the bottom of the panel
   */
  protected void layoutPanel(final JPanel tableSearchAndSummaryPanel, final JPanel southPanel) {
    setLayout(new BorderLayout());
    add(tableSearchAndSummaryPanel, BorderLayout.CENTER);
    if (southPanel != null) {
      add(southPanel, BorderLayout.SOUTH);
    }
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

  private void setupControlsInternal() {
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
    if (includeSearchPanel && searchPanel != null) {
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
            getEntityTableModel().getSelectionEmptyObserver().getReversedObserver()) {
      /** {@inheritDoc} */
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
    UiUtil.setClipboard(Util.getDelimitedString(header, data, "\t"));
  }

  private void initializeUI() {
    if (includeSearchPanel && searchScrollPane != null) {
      tableSearchAndSummaryPanel.add(searchScrollPane, BorderLayout.NORTH);
      if (!searchPanel.isSimpleSearch()) {
        searchScrollPane.getHorizontalScrollBar().setModel(getTableScrollPane().getHorizontalScrollBar().getModel());
        searchPanel.addAdvancedListener(new EventAdapter() {
          /** {@inheritDoc} */
          @Override
          public void eventOccurred() {
            if (isSearchPanelVisible()) {
              revalidate();
            }
          }
        });
      }
    }
    final JScrollPane tableScrollPane = getTableScrollPane();
    tableSearchAndSummaryPanel.add(tableScrollPane, BorderLayout.CENTER);
    if (summaryScrollPane != null) {
      tableScrollPane.getViewport().addChangeListener(new ChangeListener() {
        /** {@inheritDoc} */
        @Override
        public void stateChanged(final ChangeEvent e) {
          horizontalTableScrollBar.setVisible(tableScrollPane.getViewport().getViewSize().width > tableScrollPane.getSize().width);
          revalidate();
        }
      });
      summaryScrollPane.getHorizontalScrollBar().setModel(horizontalTableScrollBar.getModel());
      tableSearchAndSummaryPanel.add(summaryBasePanel, BorderLayout.SOUTH);
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
    layoutPanel(tableSearchAndSummaryPanel, southPanel);
  }

  /**
   * @return the refresh toolbar
   */
  private JToolBar initializeRefreshToolbar() {
    final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    final String keyName = keyStroke.toString().replace("pressed ", "");
    final Control refresh = Controls.methodControl(getEntityTableModel(), "refresh", null,
            getEntityTableModel().getSearchModel().getSearchStateObserver(), FrameworkMessages.get(FrameworkMessages.REFRESH_TIP)
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

  private void showEntityMenu(final Point location) {
    final Entity entity = getEntityTableModel().getSelectedItem();
    if (entity != null) {
      final JPopupMenu popupMenu = new JPopupMenu();
      populateEntityMenu(popupMenu, (Entity) entity.getCopy(), getEntityTableModel().getConnectionProvider());
      popupMenu.show(getTableScrollPane(), location.x, (int) location.getY() - (int) getTableScrollPane().getViewport().getViewPosition().getY());
    }
  }

  private Property getPropertyToUpdate() {
    final JComboBox box = new JComboBox(EntityUtil.getUpdatableProperties(getEntityTableModel().getEntityID()).toArray());
    final int ret = JOptionPane.showOptionDialog(this, box,
            FrameworkMessages.get(FrameworkMessages.SELECT_PROPERTY_FOR_UPDATE),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);

    if (ret == JOptionPane.OK_OPTION) {
      return (Property) box.getSelectedItem();
    }

    return null;
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
      UiUtil.addKeyEvent(getJTable(), KeyEvent.VK_DELETE, getDeleteSelectedControl());
    }
    final EventListener statusListener = new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        updateStatusMessage();
      }
    };
    getEntityTableModel().addSelectionChangedListener(statusListener);
    getEntityTableModel().addFilteringListener(statusListener);
    getEntityTableModel().addTableDataChangedListener(statusListener);

    getEntityTableModel().getSearchModel().getSearchStateObserver().addListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        getJTable().getTableHeader().repaint();
        getJTable().repaint();
      }
    });

    if (getEntityTableModel().hasEditModel()) {
      getEntityTableModel().getEditModel().addEntitiesChangedListener(new EventAdapter() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred() {
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
      @Override
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
    UiUtil.addKeyEvent(getJTable(), KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK, new ResizeSelectedColumnAction(getJTable(), false));
    UiUtil.addKeyEvent(getJTable(), KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK, new ResizeSelectedColumnAction(getJTable(), true));
    final TableCellRenderer tableCellRenderer = initializeTableCellRenderer();
    final Enumeration<TableColumn> columnEnumeration = getTableModel().getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final TableColumn column = columnEnumeration.nextElement();
      column.setCellRenderer(tableCellRenderer);
      column.setResizable(true);
    }
    setTablePopupMenu(getJTable(), getPopupControls(additionalPopupControlSets));
  }

  private void checkIfInitialized() {
    if (panelInitialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
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
      final JMenuItem menuItem = new JMenuItem("[PK] " + property.getColumnName() + ": " + entity.getValueAsString(property.getPropertyID()));
      menuItem.setToolTipText(property.getColumnName());
      rootMenu.add(menuItem);
    }
  }

  private static void populateForeignKeyMenu(final JComponent rootMenu, final Entity entity,
                                             final EntityConnectionProvider connectionProvider,
                                             final List<Property.ForeignKeyProperty> fkProperties) {
    try {
      Util.collate(fkProperties);
      for (final Property.ForeignKeyProperty property : fkProperties) {
        final String toolTipText = getReferenceColumnNames(property);
        final boolean fkValueNull = entity.isForeignKeyNull(property);
        if (!fkValueNull) {
          boolean queried = false;
          final Entity referencedEntity;
          if (entity.isLoaded(property.getPropertyID())) {
            referencedEntity = entity.getForeignKeyValue(property.getPropertyID());
          }
          else {
            referencedEntity = connectionProvider.getConnection().selectSingle(entity.getReferencedPrimaryKey(property));
            entity.removeValue(property.getPropertyID());
            entity.setValue(property, referencedEntity);
            queried = true;
          }
          final StringBuilder text = new StringBuilder("[FK").append(queried ? "+" : "")
                  .append("] ").append(property.getCaption()).append(": ");
          text.append(referencedEntity.toString());
          final JMenu foreignKeyMenu = new JMenu(text.toString());
          foreignKeyMenu.setToolTipText(toolTipText);
          populateEntityMenu(foreignKeyMenu, entity.getForeignKeyValue(property.getPropertyID()), connectionProvider);
          rootMenu.add(foreignKeyMenu);
        }
        else {
          final StringBuilder text = new StringBuilder("[FK] ").append(property.getCaption()).append(": <null>");
          final JMenuItem menuItem = new JMenuItem(text.toString());
          menuItem.setToolTipText(toolTipText);
          rootMenu.add(menuItem);
        }
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getReferenceColumnNames(final Property.ForeignKeyProperty property) {
    final List<String> columnNames = new ArrayList<String>(property.getReferenceProperties().size());
    for (final Property.ColumnProperty referenceProperty : property.getReferenceProperties()) {
      columnNames.add(referenceProperty.getColumnName());
    }

    return Util.getArrayContentsAsString(columnNames.toArray(), false);
  }

  private static void populateValueMenu(final JComponent rootMenu, final Entity entity, final List<Property> properties) {
    Util.collate(properties);
    final int maxValueLength = 20;
    for (final Property property : properties) {
      final boolean isForeignKeyProperty = property instanceof Property.ColumnProperty
              && ((Property.ColumnProperty) property).isForeignKeyProperty();
      if (!isForeignKeyProperty && !(property instanceof Property.ForeignKeyProperty)) {
        final String prefix = "[" + property.getTypeClass().getSimpleName().substring(0, 1)
                + (property instanceof Property.DenormalizedViewProperty ? "*" : "")
                + (property instanceof Property.DenormalizedProperty ? "+" : "") + "] ";
        final String value = entity.isValueNull(property.getPropertyID()) ? "<null>" : entity.getValueAsString(property.getPropertyID());
        final boolean longValue = value != null && value.length() > maxValueLength;
        final JMenuItem menuItem = new JMenuItem(prefix + property + ": " + (longValue ? value.substring(0, maxValueLength) + "..." : value));
        String toolTipText = "";
        if (property instanceof Property.ColumnProperty) {
          toolTipText = ((Property.ColumnProperty) property).getColumnName();
        }
        if (longValue) {
          toolTipText += (value.length() > 1000 ? value.substring(0, 1000) : value);
        }
        menuItem.setToolTipText(toolTipText);
        rootMenu.add(menuItem);
      }
    }
  }

  private static final class PopupMenuAction extends AbstractAction {
    private final JPopupMenu popupMenu;
    private final JTable table;

    private PopupMenuAction(final JPopupMenu popupMenu, final JTable table) {
      super("EntityTablePanel.showPopupMenu");
      this.popupMenu = popupMenu;
      this.table = table;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(final ActionEvent e) {
      final Point location = getPopupLocation(table);
      popupMenu.show(table, location.x, location.y);
    }
  }

  /**
   * Resizes the selected table column by 10 pixels.
   */
  private static class ResizeSelectedColumnAction extends  AbstractAction {

    private final JTable table;
    private final boolean enlarge;

    public ResizeSelectedColumnAction(final JTable table, final boolean enlarge) {
      super("EntityTablePanel.column" + (enlarge ? "Larger" : "Smaller"));
      this.table = table;
      this.enlarge = enlarge;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(final ActionEvent e) {
      final int selectedColumnIndex = table.getSelectedColumn();
      if (selectedColumnIndex != -1) {
        final TableColumn column = table.getColumnModel().getColumn(selectedColumnIndex);
        column.setPreferredWidth(column.getWidth() + (enlarge ? 10 : -10));
      }
    }
  }
}
