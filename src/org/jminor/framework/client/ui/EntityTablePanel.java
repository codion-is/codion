/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.SearchModel;
import org.jminor.common.model.Serializer;
import org.jminor.common.model.State;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.ui.AbstractFilteredTablePanel;
import org.jminor.common.ui.AbstractSearchPanel;
import org.jminor.common.ui.DefaultExceptionHandler;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
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
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
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
import java.util.ListIterator;
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
public class EntityTablePanel extends AbstractFilteredTablePanel<Entity, Property> {

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

  private static final int TOOLBAR_BUTTON_SIZE = 20;
  private static final int STATUS_MESSAGE_FONT_SIZE = 12;

  private final Event evtTableDoubleClicked = Events.event();
  private final Event evtSearchPanelVisibilityChanged = Events.event();
  private final Event evtSummaryPanelVisibilityChanged = Events.event();

  private final Map<String, Control> controlMap = new HashMap<String, Control>();

  /**
   * the horizontal table scroll bar
   */
  private JScrollBar horizontalTableScrollBar;

  /**
   * the search panel
   */
  private final EntityTableSearchPanel searchPanel;

  /**
   * the scroll pane used for the search panel
   */
  private JScrollPane searchScrollPane;

  /**
   * the south panel
   */
  private JPanel southPanel;

  /**
   * the summary panel
   */
  private final EntityTableSummaryPanel summaryPanel;

  /**
   * the panel used as a base panel for the summary panels, used for showing/hiding the summary panels
   */
  private JPanel summaryBasePanel;

  /**
   * the scroll pane used for the summary panel
   */
  private JScrollPane summaryScrollPane;

  /**
   * the toolbar containing the refresh button
   */
  private JToolBar refreshToolBar;

  /**
   * the label for showing the status of the table, that is, the number of rows, number of selected rows etc.
   */
  private JLabel statusMessageLabel;

  /**
   * the action performed when the table is double clicked
   */
  private Action tableDoubleClickAction;

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
                          final ControlSet additionalToolbarControls,
                          final EntityTableSearchPanel searchPanel,
                          final EntityTableSummaryPanel summaryPanel) {
    super(tableModel);
    this.searchPanel = searchPanel;
    this.summaryPanel = summaryPanel;
    setupControls();
    initializeUI();
    initializePopupMenu(additionalPopupControls);
    initializeToolbar(additionalToolbarControls);
    bindEvents();
    updateStatusMessage();
    if (Configuration.getBooleanValue(Configuration.SEARCH_PANELS_VISIBLE)) {
      setSearchPanelVisible(true);
    }
  }

  public final EntityTablePanel initializePopupMenu(final ControlSet additionalPopupControls) {
    setTablePopupMenu(getJTable(), getPopupControls(additionalPopupControls));
    return this;
  }

  public final EntityTablePanel initializeToolbar(final ControlSet additionalToolbarControls) {
    final ControlSet toolbarControlSet = getToolbarControls(additionalToolbarControls);
    if (toolbarControlSet != null) {
      final JToolBar southToolBar = ControlProvider.createToolbar(toolbarControlSet, JToolBar.HORIZONTAL);
      for (final Component component : southToolBar.getComponents()) {
        component.setPreferredSize(new Dimension(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE));
      }
      southToolBar.setFocusable(false);
      southToolBar.setFloatable(false);
      southToolBar.setRollover(true);
      southPanel.add(southToolBar, BorderLayout.EAST);
    }
    return this;
  }

  /**
   * @param doubleClickAction the action to perform when a double click is performed on the table
   */
  public final void setTableDoubleClickAction(final Action doubleClickAction) {
    this.tableDoubleClickAction = doubleClickAction;
  }

  /**
   * @return the Action performed when the table receives a double click
   */
  public final Action getTableDoubleClickAction() {
    return tableDoubleClickAction;
  }

  /**
   * @return the EntityTableModel used by this EntityTablePanel
   */
  @Override
  public final EntityTableModel getTableModel() {
    return (EntityTableModel) super.getTableModel();
  }

  /**
   * Shows a dialog for configuring the underlying EntityTableModel query.
   * If the underlying table model does not allow query configuration this
   * method returns silently
   * @see org.jminor.framework.client.model.EntityTableModel#isQueryConfigurationAllowed()
   */
  public final void configureQuery() {
    if (!getTableModel().isQueryConfigurationAllowed()) {
      return;
    }

    final EntityCriteriaPanel panel;
    AbstractAction action;
    try {
      UiUtil.setWaitCursor(true, this);
      panel = new EntityCriteriaPanel(getTableModel());
      action = new AbstractAction(FrameworkMessages.get(FrameworkMessages.APPLY)) {
        public void actionPerformed(final ActionEvent e) {
          getTableModel().refresh();
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
      summaryScrollPane.setVisible(visible);
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
    return summaryScrollPane != null && summaryScrollPane.isVisible();
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

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + getTableModel().getEntityID();
  }

  /**
   * @param controlCode the control code
   * @return the control associated with <code>controlCode</code>
   * @throws RuntimeException in case no control is associated with the given control code
   */
  public final Control getControl(final String controlCode) {
    if (!controlMap.containsKey(controlCode)) {
      throw new RuntimeException(controlCode + " control not available in panel: " + this);
    }

    return controlMap.get(controlCode);
  }

  /**
   * @return a control set containing a set of controls, one for each updatable property in the
   * underlying entity, for performing an update on the selected entities
   */
  public ControlSet getUpdateSelectedControlSet() {
    final State enabled = States.aggregateState(Conjunction.AND,
            getTableModel().stateAllowMultipleUpdate(),
            getTableModel().stateSelectionEmpty().getReversedState());
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED),
            (char) 0, Images.loadImage("Modify16.gif"), enabled);
    controlSet.setDescription(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP));
    for (final Property property : EntityUtil.getUpdateProperties(getTableModel().getEntityID())) {
      final String caption = property.getCaption() == null ? property.getPropertyID() : property.getCaption();
      controlSet.add(UiUtil.linkToEnabledState(enabled, new AbstractAction(caption) {
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
    return ControlFactory.methodControl(this, "configureQuery",
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY) + "...", null,
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY), 0,
            null, Images.loadImage(Images.IMG_PREFERENCES_16));
  }

  /**
   * @return a control for showing the dependencies dialog
   */
  public final Control getViewDependenciesControl() {
    return ControlFactory.methodControl(this, "viewSelectionDependencies",
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES) + "...",
            getTableModel().stateSelectionEmpty().getReversedState(),
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES_TIP), 'W');
  }

  /**
   * @return a control for deleting the selected entities
   */
  public final Control getDeleteSelectedControl() {
    return ControlFactory.methodControl(this, "delete", FrameworkMessages.get(FrameworkMessages.DELETE),
            States.aggregateState(Conjunction.AND,
                    getTableModel().stateAllowDelete(),
                    getTableModel().stateSelectionEmpty().getReversedState()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP), 0, null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for exporting the selected records to file
   */
  public final Control getExportControl() {
    return ControlFactory.methodControl(this, "exportSelected",
            FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED) + "...",
            getTableModel().stateSelectionEmpty().getReversedState(),
            FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED_TIP), 0, null,
            Images.loadImage(Images.IMG_SAVE_16));
  }

  /**
   * @return a control for printing the table
   */
  public final Control getPrintTableControl() {
    final String printCaption = FrameworkMessages.get(FrameworkMessages.PRINT_TABLE);
    return ControlFactory.methodControl(this, "printTable", printCaption, null,
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
    if (getTableModel().stateSelectionEmpty().isActive()) {
      return;
    }

    final List<Entity> selectedEntities = EntityUtil.copyEntities(getTableModel().getSelectedItems());
    if (!getTableModel().isMultipleUpdateAllowed() && selectedEntities.size() > 1) {
      throw new RuntimeException("Update of multiple entities is not allowed!");
    }

    final InputProviderPanel inputPanel = new InputProviderPanel(propertyToUpdate.getCaption(),
            getInputProvider(propertyToUpdate, selectedEntities));
    UiUtil.showInDialog(this, inputPanel, true, FrameworkMessages.get(FrameworkMessages.SET_PROPERTY_VALUE),
            null, inputPanel.getOkButton(), inputPanel.buttonClickObserver());
    if (inputPanel.isEditAccepted()) {
      EntityUtil.setPropertyValue(propertyToUpdate.getPropertyID(), inputPanel.getValue(), selectedEntities);
      try {
        UiUtil.setWaitCursor(true, this);
        getTableModel().update(selectedEntities);
      }
      catch (RuntimeException re) {
        throw re;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
      finally {
        UiUtil.setWaitCursor(false, this);
      }
    }
  }

  /**
   * Shows a dialog containing lists of entities depending on the selected entities via foreign key
   * @throws org.jminor.common.db.exception.DbException in case of a database exception
   */
  public final void viewSelectionDependencies() throws DbException {
    final Map<String, Collection<Entity>> dependencies;
    try {
      UiUtil.setWaitCursor(true, this);
      dependencies = getTableModel().getSelectionDependencies();
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
    if (!dependencies.isEmpty()) {
      showDependenciesDialog(dependencies, getTableModel().getDbProvider(), this);
    }
    else {
      JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.NONE_FOUND),
              FrameworkMessages.get(FrameworkMessages.NO_DEPENDENT_RECORDS), JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * Performs a delete on the active entity or if a table model is available, the selected entities
   * @throws org.jminor.common.db.exception.DbException in case of a database exception
   * @throws org.jminor.common.model.CancelException in the delete action is cancelled
   */
  public final void delete() throws DbException, CancelException {
    if (confirmDelete()) {
      try {
        UiUtil.setWaitCursor(true, this);
        getTableModel().deleteSelected();
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
    final List<Entity> selected = getTableModel().getSelectedItems();
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

  public final Control getToggleSummaryPanelControl() {
    final ToggleBeanValueLink toggle = ControlFactory.toggleControl(this, "summaryPanelVisible", null,
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
    if (!getTableModel().isQueryConfigurationAllowed()) {
      return null;
    }

    final Control ret = new Control() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        toggleSearchPanel();
      }
    };
    ret.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    ret.setDescription(FrameworkMessages.get(FrameworkMessages.SEARCH));

    return ret;
  }

  public final Control getClearSelectionControl() {
    final Control clearSelection = ControlFactory.methodControl(getTableModel(), "clearSelection", null,
            getTableModel().stateSelectionEmpty().getReversedState(), null, -1, null,
            Images.loadImage("ClearSelection16.gif"));
    clearSelection.setDescription(FrameworkMessages.get(FrameworkMessages.CLEAR_SELECTION_TIP));

    return clearSelection;
  }

  public final Control getMoveSelectionDownControl() {
    final Control selectionDown = ControlFactory.methodControl(getTableModel(), "moveSelectionDown",
            Images.loadImage(Images.IMG_DOWN_16));
    selectionDown.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_DOWN_TIP));

    return selectionDown;
  }

  public final Control getMoveSelectionUpControl() {
    final Control selectionUp = ControlFactory.methodControl(getTableModel(), "moveSelectionUp",
            Images.loadImage(Images.IMG_UP_16));
    selectionUp.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_UP_TIP));

    return selectionUp;
  }

  public final void addSearchPanelVisibleListener(final ActionListener listener) {
    evtSearchPanelVisibilityChanged.addListener(listener);
  }

  public final void removeSearchPanelVisibleListener(final ActionListener listener) {
    evtSearchPanelVisibilityChanged.removeListener(listener);
  }

  public final void addSummaryPanelVisibleListener(final ActionListener listener) {
    evtSummaryPanelVisibilityChanged.addListener(listener);
  }

  public final void removeSummaryPanelVisibleListener(final ActionListener listener) {
    evtSummaryPanelVisibilityChanged.removeListener(listener);
  }

  public final void addTableDoubleClickListener(final ActionListener listener) {
    evtTableDoubleClicked.addListener(listener);
  }

  public final void removeTableDoubleClickListener(final ActionListener listener) {
    evtTableDoubleClicked.removeListener(listener);
  }

  /**
   * Creates a static entity table panel showing the given entities
   * @param entities the entities to show in the panel
   * @param dbProvider the EntityDbProvider, in case the returned panel should require one
   * @return a static EntityTablePanel showing the given entities
   */
  public static EntityTablePanel createStaticEntityTablePanel(final Collection<Entity> entities, final EntityDbProvider dbProvider) {
    if (entities == null || entities.isEmpty()) {
      throw new RuntimeException("Cannot create an EntityPanel without the entities");
    }

    return createStaticEntityTablePanel(entities, dbProvider, entities.iterator().next().getEntityID());
  }

  /**
   * Creates a static entity table panel showing the given entities
   * @param entities the entities to show in the panel
   * @param dbProvider the EntityDbProvider, in case the returned panel should require one
   * @param entityID the entityID
   * @return a static EntityTablePanel showing the given entities
   */
  public static EntityTablePanel createStaticEntityTablePanel(final Collection<Entity> entities, final EntityDbProvider dbProvider,
                                                              final String entityID) {
    final EntityEditModel editModel = new DefaultEntityEditModel(entityID, dbProvider);
    final EntityTableModel tableModel = new DefaultEntityTableModel(entityID, dbProvider) {
      @Override
      protected List<Entity> performQuery(final Criteria<Property.ColumnProperty> criteria) {
        return new ArrayList<Entity>(entities);
      }
    };
    tableModel.setQueryConfigurationAllowed(false);
    tableModel.setEditModel(editModel);
    final EntityTablePanel tablePanel = new EntityTablePanel(tableModel, null, null, null, null);
    tableModel.refresh();

    return tablePanel;
  }

  /**
   * Initializes the UI
   */
  protected void initializeUI() {
    final JPanel tableSearchAndSummaryPanel = new JPanel(new BorderLayout());
    setLayout(new BorderLayout());
    if (searchPanel != null) {
      searchScrollPane = new JScrollPane((JPanel) searchPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      if (searchPanel instanceof EntityTableSearchAdvancedPanel) {
        searchScrollPane.getHorizontalScrollBar().setModel(getTableScrollPane().getHorizontalScrollBar().getModel());
      }
      tableSearchAndSummaryPanel.add(searchScrollPane, BorderLayout.NORTH);
    }

    if (searchPanel instanceof EntityTableSearchAdvancedPanel) {
      ((EntityTableSearchAdvancedPanel) searchPanel).eventAdvancedChanged().addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          if (isSearchPanelVisible()) {
            revalidateAndShowSearchPanel();
          }
        }
      });
    }

    final JScrollPane tableScrollPane = getTableScrollPane();
    tableSearchAndSummaryPanel.add(tableScrollPane, BorderLayout.CENTER);
    add(tableSearchAndSummaryPanel, BorderLayout.CENTER);
    if (summaryPanel != null) {
      summaryScrollPane = new JScrollPane(summaryPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      horizontalTableScrollBar = tableScrollPane.getHorizontalScrollBar();
      tableScrollPane.getViewport().addChangeListener(new ChangeListener() {
        public void stateChanged(final ChangeEvent e) {
          horizontalTableScrollBar.setVisible(tableScrollPane.getViewport().getViewSize().width > tableScrollPane.getSize().width);
          revalidate();
        }
      });
      summaryScrollPane.getHorizontalScrollBar().setModel(horizontalTableScrollBar.getModel());
      summaryScrollPane.setVisible(false);
      summaryBasePanel = new JPanel(new BorderLayout());
      tableSearchAndSummaryPanel.add(summaryBasePanel, BorderLayout.SOUTH);
    }

    southPanel = initializeSouthPanel();
    if (southPanel != null) {
      add(southPanel, BorderLayout.SOUTH);
    }

    setSearchPanelVisible(Configuration.getBooleanValue(Configuration.DEFAULT_SEARCH_PANEL_STATE));
  }

  /**
   * Initializes the south panel, override and return null for no south panel.
   * @return the south panel, or null if no south panel should be used
   */
  protected JPanel initializeSouthPanel() {
    statusMessageLabel = new JLabel("", JLabel.CENTER);
    statusMessageLabel.setFont(new Font(statusMessageLabel.getFont().getName(), Font.PLAIN, STATUS_MESSAGE_FONT_SIZE));
    final JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
    final JTextField searchField = getSearchField();
    final JPanel searchFieldPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    searchFieldPanel.add(searchField);
    centerPanel.add(statusMessageLabel, BorderLayout.CENTER);
    centerPanel.add(searchFieldPanel, BorderLayout.WEST);
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(centerPanel, BorderLayout.CENTER);
    panel.setBorder(BorderFactory.createEtchedBorder());
    refreshToolBar = initializeRefreshToolbar();
    if (refreshToolBar != null) {
      panel.add(refreshToolBar, BorderLayout.WEST);
    }

    return panel;
  }

  @Override
  protected AbstractSearchPanel<Property> initializeFilterPanel(final SearchModel<Property> model) {
    return new PropertyFilterPanel(model, true, true);
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
              public void actionPerformed(final ActionEvent e) {
                showEntityMenu(new Point(100, table.getSelectedRow() * table.getRowHeight()));
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
    return ControlFactory.toggleControl(this, "searchPanelVisible",
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
    return ControlFactory.methodControl(getTableModel(), "refresh", refreshCaption,
            null, FrameworkMessages.get(FrameworkMessages.REFRESH_TIP), refreshCaption.charAt(0),
            null, Images.loadImage(Images.IMG_REFRESH_16));
  }

  /**
   * @return a Control for clearing the underlying table model, that is, removing all rows
   */
  protected final Control getClearControl() {
    final String clearCaption = FrameworkMessages.get(FrameworkMessages.CLEAR);
    return ControlFactory.methodControl(getTableModel(), "clear", clearCaption,
            null, null, clearCaption.charAt(0), null, null);
  }

  /**
   * Initializes the panel containing the table column summary panels
   * @return the summary panel
   */
  protected final EntityTableSummaryPanel initializeSummaryPanel() {
    final EntityTableSummaryPanel panel = new EntityTableSummaryPanel(getTableModel());
    panel.setVerticalFillerWidth(UiUtil.getPreferredScrollBarWidth());

    return panel;
  }

  /**
   * @return the refresh toolbar
   */
  protected final JToolBar initializeRefreshToolbar() {
    final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    final String keyName = stroke.toString().replace("pressed ", "");
    final Control refresh = ControlFactory.methodControl(getTableModel(), "refresh", null,
            getTableModel().getSearchModel().stateSearchStateChanged(), FrameworkMessages.get(FrameworkMessages.REFRESH_TIP)
                    + " (" + keyName + ")", 0, null, Images.loadImage(Images.IMG_STOP_16));

    final InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
    final ActionMap actionMap = getActionMap();

    inputMap.put(stroke, "refreshControl");
    actionMap.put("refreshControl", refresh);

    final AbstractButton button = ControlProvider.createButton(refresh);
    button.setPreferredSize(new Dimension(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE));
    button.setFocusable(false);

    final JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
    toolBar.setFocusable(false);
    toolBar.setFloatable(false);
    toolBar.setRollover(true);

    toolBar.add(button);

    return toolBar;
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
    final Collection<Object> values = EntityUtil.getDistinctPropertyValues(toUpdate, property.getPropertyID());
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
      return new TextInputProvider(property.getCaption(), getTableModel().getEditModel().getValueProvider(property), (String) currentValue);
    }
    if (property.isReference()) {
      return createEntityInputProvider((Property.ForeignKeyProperty) property, (Entity) currentValue, getTableModel().getEditModel());
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
    if (!EntityRepository.isLargeDataset(foreignKeyProperty.getReferencedEntityID())) {
      return new EntityComboProvider(editModel.createEntityComboBoxModel(foreignKeyProperty), currentValue);
    }
    else {
      final List<Property.ColumnProperty> searchProperties = EntityRepository.getSearchProperties(foreignKeyProperty.getReferencedEntityID());
      if (searchProperties.isEmpty()) {
        throw new RuntimeException("No searchable properties found for entity: " + foreignKeyProperty.getReferencedEntityID());
      }

      return new EntityLookupProvider(editModel.createEntityLookupModel(foreignKeyProperty.getReferencedEntityID(), searchProperties, null), currentValue);
    }
  }

  /**
   * Returns the TableCellRenderer used for this EntityTablePanel
   * @return the TableCellRenderer
   */
  protected TableCellRenderer initializeTableCellRenderer() {
    return new EntityTableCellRenderer(getTableModel(), isRowColoring());
  }

  /**
   * By default this returns the result of isRowColoring from the EntityRepository
   * @return true if the table rows should be colored according to the underlying entity
   * @see org.jminor.framework.domain.EntityDefinition#setRowColoring(boolean)
   * @see org.jminor.framework.domain.EntityRepository#isRowColoring(String)
   */
  protected boolean isRowColoring() {
    return EntityRepository.isRowColoring(getTableModel().getEntityID());
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
   * Initializes the JTable instance
   * @return the JTable instance
   * @see org.jminor.framework.domain.EntityDefinition#setRowColoring(boolean)
   * @see org.jminor.framework.domain.EntityRepository.Proxy#getBackgroundColor(org.jminor.framework.domain.Entity)
   */
  @Override
  protected final JTable initializeJTable() {
    final TableColumnModel columnModel = getTableModel().getColumnModel();
    final JTable jTable = new JTable(getTableModel(), columnModel, getTableModel().getSelectionModel());
    final TableCellRenderer tableCellRenderer = initializeTableCellRenderer();
    final Enumeration<TableColumn> columnEnumeration = columnModel.getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final TableColumn column = columnEnumeration.nextElement();
      column.setCellRenderer(tableCellRenderer);
      column.setResizable(true);
    }

    jTable.addMouseListener(initializeTableMouseListener());

    final JTableHeader header = jTable.getTableHeader();
    final TableCellRenderer defaultHeaderRenderer = header.getDefaultRenderer();
    final Font defaultFont = jTable.getFont();
    final Font searchFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
    header.setDefaultRenderer(new TableCellRenderer() {
      public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                     final boolean hasFocus, final int row, final int column) {
        final JLabel label = (JLabel) defaultHeaderRenderer.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
        label.setFont(getTableModel().getSearchModel().isSearchEnabled(
                getTableModel().getColumnProperty(column).getPropertyID()) ? searchFont : defaultFont);

        return label;
      }
    });
    header.setFocusable(false);
    header.setReorderingAllowed(allowColumnReordering());

    jTable.setAutoResizeMode(getAutoResizeMode());

    return jTable;
  }

  /**
   * @return the auto resize mode to use for the JTable, by default this returns the value of Configuration.TABLE_AUTO_RESIZE_MODE
   * @see Configuration#TABLE_AUTO_RESIZE_MODE
   */
  protected int getAutoResizeMode() {
    return Configuration.getIntValue(Configuration.TABLE_AUTO_RESIZE_MODE);
  }

  /**
   * Specifies whether or not column reordering is allowed in this table, called during JTable initialization.
   * By default this method returns the configuration value ALLOW_COLUMN_REORDERING
   * @return true if this table should allow column reordering
   * @see org.jminor.framework.Configuration#ALLOW_COLUMN_REORDERING
   */
  protected boolean allowColumnReordering() {
    return Configuration.getBooleanValue(Configuration.ALLOW_COLUMN_REORDERING);
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
    if (!getTableModel().isReadOnly() && getTableModel().isDeleteAllowed()) {
      setControl(DELETE_SELECTED, getDeleteSelectedControl());
    }
    if (!getTableModel().isReadOnly() && getTableModel().isMultipleUpdateAllowed()) {
      setControl(UPDATE_SELECTED, getUpdateSelectedControlSet());
    }
    if (getTableModel().isQueryConfigurationAllowed()) {
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
            getTableModel().stateSelectionEmpty().getReversedState()) {
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
      @Override
      public void actionPerformed(final ActionEvent e) {
        try {
          copyTableAsDelimitedString();
        }
        catch (CancelException ex) {/**/}
      }
    };
  }

  private void copyTableAsDelimitedString() throws CancelException {
    final List<String> headerValues = new ArrayList<String>();
    final List<Property> properties = new ArrayList<Property>(getTableModel().getTableColumnProperties());
    final ListIterator<Property> iterator = properties.listIterator();
    //remove hidden columns
    while (iterator.hasNext()) {
      if (!getTableModel().isColumnVisible(iterator.next())) {
        iterator.remove();
      }
    }
    for (final Property property : properties) {
      headerValues.add(property.getCaption());
    }

    final String[][] header = {headerValues.toArray(new String[headerValues.size()])};

    final List<Entity> entities = getTableModel().isSelectionEmpty()
            ? getTableModel().getVisibleItems() : getTableModel().getSelectedItems();

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

  private void showEntityMenu(final Point location) {
    try {
      final Entity entity = getTableModel().getSelectedItem();
      if (entity != null) {
        final JPopupMenu popupMenu = new JPopupMenu();
        populateEntityMenu(popupMenu, (Entity) entity.getCopy(), getTableModel().getDbProvider());
        popupMenu.show(getTableScrollPane(), location.x, (int) location.getY() - (int) getTableScrollPane().getViewport().getViewPosition().getY());
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Property getPropertyToUpdate() throws CancelException {
    final JComboBox box = new JComboBox(EntityUtil.getUpdateProperties(getTableModel().getEntityID()).toArray());
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
      final String status = getTableModel().getStatusMessage();
      statusMessageLabel.setText(status);
      statusMessageLabel.setToolTipText(status);
    }
  }

  private void bindEvents() {
    if (!getTableModel().isReadOnly() && getTableModel().isDeleteAllowed()) {
      getJTable().addKeyListener(new KeyAdapter() {
        @Override
        public void keyTyped(final KeyEvent e) {
          if (e.getKeyChar() == KeyEvent.VK_DELETE && !getTableModel().stateSelectionEmpty().isActive()) {
            try {
              delete();
            }
            catch (DbException ex) {
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
    getTableModel().addRefreshStartedListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        UiUtil.setWaitCursor(true, EntityTablePanel.this);
      }
    });
    getTableModel().addRefreshDoneListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        UiUtil.setWaitCursor(false, EntityTablePanel.this);
      }
    });
    getJTable().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if(e.getClickCount() == 2) {
          evtTableDoubleClicked.fire();
        }
      }
    });
    final ActionListener statusListener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateStatusMessage();
      }
    };
    getTableModel().addSelectionChangedListener(statusListener);
    getTableModel().addFilteringListener(statusListener);
    getTableModel().addTableDataChangedListener(statusListener);

    getTableModel().addSelectedIndexListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (!getTableModel().stateSelectionEmpty().isActive()) {
          scrollToCoordinate(getTableModel().getSelectedIndex(), getJTable().getSelectedColumn());
        }
      }
    });

    getTableModel().getSearchModel().stateSearchStateChanged().addStateListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        getJTable().getTableHeader().repaint();
        getJTable().repaint();
      }
    });

    if (getTableModel().getEditModel() != null) {
      getTableModel().getEditModel().addEntitiesChangedListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          getJTable().repaint();
        }
      });
    }
  }

  private void revalidateAndShowSearchPanel() {
    searchScrollPane.getViewport().setView(null);
    searchScrollPane.getViewport().setView((JPanel) searchPanel);
    revalidate();
  }

  private static void showDependenciesDialog(final Map<String, Collection<Entity>> dependencies, final EntityDbProvider dbProvider,
                                             final JComponent dialogParent) {
    JPanel dependenciesPanel;
    try {
      UiUtil.setWaitCursor(true, dialogParent);
      dependenciesPanel = createDependenciesPanel(dependencies, dbProvider);
    }
    finally {
      UiUtil.setWaitCursor(false, dialogParent);
    }
    UiUtil.showInDialog(UiUtil.getParentWindow(dialogParent), dependenciesPanel,
            true, FrameworkMessages.get(FrameworkMessages.DEPENDENT_RECORDS_FOUND), true, true, null);
  }

  private static JPanel createDependenciesPanel(final Map<String, Collection<Entity>> dependencies,
                                                final EntityDbProvider dbProvider) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    tabPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final Map.Entry<String, Collection<Entity>> entry : dependencies.entrySet()) {
      final Collection<Entity> dependantEntities = entry.getValue();
      if (!dependantEntities.isEmpty()) {
        tabPane.addTab(entry.getKey(), createStaticEntityTablePanel(dependantEntities, dbProvider));
      }
    }
    panel.add(tabPane, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Populates the given root menu with the property values of the given entity
   * @param rootMenu the menu to populate
   * @param entity the entity
   * @param dbProvider if provided then lazy loaded entity references are loaded so that the full object graph can be shown
   */
  private static void populateEntityMenu(final JComponent rootMenu, final Entity entity, final EntityDbProvider dbProvider) {
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<Property.PrimaryKeyProperty>(EntityRepository.getPrimaryKeyProperties(entity.getEntityID())));
    populateForeignKeyMenu(rootMenu, entity, dbProvider, new ArrayList<Property.ForeignKeyProperty>(EntityRepository.getForeignKeyProperties(entity.getEntityID())));
    populateValueMenu(rootMenu, entity, new ArrayList<Property>(EntityRepository.getProperties(entity.getEntityID(), false)));
  }

  private static void populatePrimaryKeyMenu(final JComponent rootMenu, final Entity entity, final List<Property.PrimaryKeyProperty> primaryKeyProperties) {
    Util.collate(primaryKeyProperties);
    for (final Property.PrimaryKeyProperty property : primaryKeyProperties) {
      rootMenu.add(new JMenuItem("[PK] " + property.getColumnName() + ": " + entity.getValueAsString(property.getPropertyID())));
    }
  }

  private static void populateForeignKeyMenu(final JComponent rootMenu, final Entity entity, final EntityDbProvider dbProvider,
                                             final List<Property.ForeignKeyProperty> fkProperties) {
    try {
      Util.collate(fkProperties);
      for (final Property.ForeignKeyProperty property : fkProperties) {
        final boolean fkValueNull = entity.isForeignKeyNull(property);
        if (!fkValueNull) {
          boolean queried = false;
          Entity referencedEntity = entity.getForeignKeyValue(property.getPropertyID());
          if (referencedEntity == null) {
            referencedEntity = dbProvider.getEntityDb().selectSingle(entity.getReferencedPrimaryKey(property));
            entity.removeValue(property.getPropertyID());
            entity.setValue(property, referencedEntity);
            queried = true;
          }
          final StringBuilder text = new StringBuilder("[FK").append(queried ? "+" : "")
                  .append("] ").append(property.getCaption()).append(": ");
          text.append(referencedEntity.toString());
          final JMenu foreignKeyMenu = new JMenu(text.toString());
          populateEntityMenu(foreignKeyMenu, entity.getForeignKeyValue(property.getPropertyID()), dbProvider);
          rootMenu.add(foreignKeyMenu);
        }
        else {
          final StringBuilder text = new StringBuilder("[FK] ").append(property.getCaption()).append(": <null>");
          rootMenu.add(new JMenuItem(text.toString()));
        }
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void populateValueMenu(final JComponent rootMenu, final Entity entity, final List<Property> properties) {
    Util.collate(properties);
    for (final Property property : properties) {
      if (!property.hasParentProperty() && !(property instanceof Property.ForeignKeyProperty)) {
        final String prefix = "[" + Util.getTypeClass(property.getType()).getSimpleName().substring(0, 1)
                + (property instanceof Property.DenormalizedViewProperty ? "*" : "")
                + (property instanceof Property.DenormalizedProperty ? "+" : "") + "] ";
        final String value = entity.isValueNull(property.getPropertyID()) ? "<null>" : entity.getValueAsString(property.getPropertyID());
        final boolean longValue = value != null && value.length() > TOOLBAR_BUTTON_SIZE;
        final JMenuItem menuItem = new JMenuItem(prefix + property + ": " + (longValue ? value.substring(0, TOOLBAR_BUTTON_SIZE) + "..." : value));
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

    public void actionPerformed(final ActionEvent e) {
      popupMenu.show(table, 100, table.getSelectedRow() * table.getRowHeight());
    }
  }
}
