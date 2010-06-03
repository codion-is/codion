/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.AggregateState;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.ui.AbstractFilteredTablePanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.ToggleBeanValueLink;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.images.NavigableImagePanel;
import org.jminor.common.ui.input.BooleanInputProvider;
import org.jminor.common.ui.input.DateInputProvider;
import org.jminor.common.ui.input.DoubleInputProvider;
import org.jminor.common.ui.input.InputProvider;
import org.jminor.common.ui.input.InputProviderPanel;
import org.jminor.common.ui.input.IntInputProvider;
import org.jminor.common.ui.input.TextInputProvider;
import org.jminor.common.ui.input.ValueListInputProvider;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.EntityTableSearchModel;
import org.jminor.framework.client.model.PropertyFilterModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.json.JSONException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
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
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

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
public class EntityTablePanel extends AbstractFilteredTablePanel<Entity> {

  public static final char FILTER_INDICATOR = '*';

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

  private final Event evtTableDoubleClicked = new Event();
  private final Event evtSearchPanelVisibilityChanged = new Event();
  private final Event evtSummaryPanelVisibilityChanged = new Event();

  private final Map<String, Control> controlMap = new HashMap<String, Control>();

  /**
   * the horizontal table scroll bar
   */
  private JScrollBar horizontalTableScrollBar;

  /**
   * the search panel
   */
  private final JPanel searchPanel;

  /**
   * the scroll pane used for the search panel
   */
  private JScrollPane searchScrollPane;

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
   * the property filter panels
   */
  private final Map<String, PropertyFilterPanel> propertyFilterPanels;

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
   * @param additionalPopupControls a ControlSet which will be added to the popup control set
   * @param additionalToolbarControls a ControlSet which will be added to the toolbar control set
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet additionalPopupControls,
                          final ControlSet additionalToolbarControls) {
    this(tableModel, additionalPopupControls, additionalToolbarControls, null);
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param additionalPopupControls a ControlSet which will be added to the popup control set
   * @param additionalToolbarControls a ControlSet which will be added to the toolbar control set
   * @param printControls a ControlSet on which to base the print popup submenu
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet additionalPopupControls,
                          final ControlSet additionalToolbarControls, final ControlSet printControls) {
    super(tableModel);
    this.searchPanel = initializeSearchPanel();
    this.summaryPanel = initializeSummaryPanel();
    this.propertyFilterPanels = initializeFilterPanels();
    setupControls(printControls);
    initializeUI(getPopupControls(additionalPopupControls), getToolbarControls(additionalToolbarControls));
    bindEventsInternal();
    bindEvents();
    updateStatusMessage();
  }

  public EntityEditModel getEditModel() {
    return getTableModel().getEditModel();
  }

  /**
   * @param doubleClickAction the action to perform when a double click is performed on the table
   */
  public void setTableDoubleClickAction(final Action doubleClickAction) {
    this.tableDoubleClickAction = doubleClickAction;
  }

  /**
   * @return the Action performed when the table receives a double click
   */
  public Action getTableDoubleClickAction() {
    return tableDoubleClickAction;
  }

  /**
   * @return the EntityTableModel used by this EntityTablePanel
   */
  @Override
  public EntityTableModel getTableModel() {
    return (EntityTableModel) super.getTableModel();
  }

  /**
   * Hides or shows the active filter panels for this table panel
   * @param value true if the active filter panels should be shown, false if they should be hidden
   */
  public void setFilterPanelsVisible(final boolean value) {
    for (final PropertyFilterPanel columnFilterPanel : propertyFilterPanels.values()) {
      if (value)
        columnFilterPanel.showDialog();
      else
        columnFilterPanel.hideDialog();
    }
  }

  /**
   * Shows a dialog for configuring the underlying EntityTableModel query.
   * If the underlying table model does not allow query configuration this
   * method returns silently
   * @see EntityTableModel#isQueryConfigurationAllowed()
   */
  public void configureQuery() {
    if (!getTableModel().isQueryConfigurationAllowed())
      return;

    final EntityCriteriaPanel panel;
    AbstractAction action;
    try {
      UiUtil.setWaitCursor(true, this);
      panel = new EntityCriteriaPanel(getTableModel());
      action = new AbstractAction(FrameworkMessages.get(FrameworkMessages.APPLY)) {
        public void actionPerformed(ActionEvent event) {
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
  public void setSummaryPanelVisible(final boolean visible) {
    if (visible && isSummaryPanelVisible())
      return;

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
  public boolean isSummaryPanelVisible() {
    return summaryScrollPane != null && summaryScrollPane.isVisible();
  }

  /**
   * Hides or shows the column search panel for this EntityTablePanel
   * @param visible if true the search panel is shown, if false it is hidden
   */
  public void setSearchPanelVisible(final boolean visible) {
    if (visible && isSearchPanelVisible())
      return;

    if (searchScrollPane != null) {
      searchScrollPane.getViewport().setView(visible ? searchPanel : null);
      if (refreshToolBar != null)
        refreshToolBar.setVisible(visible);
      evtSearchPanelVisibilityChanged.fire();
    }
  }

  /**
   * @return true if the search panel is visible, false if it is hidden
   */
  public boolean isSearchPanelVisible() {
    return searchScrollPane != null && searchScrollPane.getViewport().getView() == searchPanel;
  }

  /**
   * @return the column search panel being used by this EntityTablePanel
   */
  public JPanel getSearchPanel() {
    return searchPanel;
  }

  /**
   * Toggles the search panel through the states hidden, visible and
   * in case it is a EntityTableSearchPanel advanced
   */
  public void toggleSearchPanel() {
    final JPanel searchPanel = getSearchPanel();
    if (searchPanel instanceof EntityTableSearchPanel) {
      if (isSearchPanelVisible()) {
        if (((EntityTableSearchPanel) searchPanel).isAdvanced())
          setSearchPanelVisible(false);
        else
          ((EntityTableSearchPanel) searchPanel).setAdvanced(true);
      }
      else {
        ((EntityTableSearchPanel) searchPanel).setAdvanced(false);
        setSearchPanelVisible(true);
      }
    }
    else
      setSearchPanelVisible(!isSearchPanelVisible());
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "EntityTablePanel: " + getTableModel().getEntityID();
  }

  /**
   * @param controlCode the control code
   * @return the control associated with <code>controlCode</code>
   * @throws RuntimeException in case no control is associated with the given control code
   */
  public final Control getControl(final String controlCode) {
    if (!controlMap.containsKey(controlCode))
      throw new RuntimeException(controlCode + " control not available in panel: " + this);

    return controlMap.get(controlCode);
  }

  /**
   * @return a control set containing a set of controls, one for each updatable property in the
   * underlying entity, for performing an update on the selected entities
   */
  public ControlSet getUpdateSelectedControlSet() {
    final EntityEditModel editModel = getTableModel().getEditModel();
    final State enabled = new AggregateState(AggregateState.Type.AND,
            editModel.stateAllowUpdate(),
            getTableModel().stateSelectionEmpty().getReversedState());
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED),
            (char) 0, Images.loadImage("Modify16.gif"), enabled);
    controlSet.setDescription(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_TIP));
    for (final Property property : EntityUtil.getUpdateProperties(editModel.getEntityID())) {
      final String caption = property.getCaption() == null ? property.getPropertyID() : property.getCaption();
      controlSet.add(UiUtil.linkToEnabledState(enabled, new AbstractAction(caption) {
        public void actionPerformed(final ActionEvent event) {
          updateSelectedEntities(property, editModel);
        }
      }));
    }

    return controlSet;
  }

  /**
   * @return a control for showing the query configuration dialog
   */
  public Control getConfigureQueryControl() {
    return ControlFactory.methodControl(this, "configureQuery",
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY) + "...", null,
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY), 0,
            null, Images.loadImage(Images.IMG_PREFERENCES_16));
  }

  /**
   * @return a control for showing the dependencies dialog
   */
  public Control getViewDependenciesControl() {
    return ControlFactory.methodControl(this, "viewSelectionDependencies",
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES) + "...",
            getTableModel().stateSelectionEmpty().getReversedState(),
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES_TIP), 'W');
  }

  /**
   * @return a control for deleting the selected entities
   */
  public Control getDeleteSelectedControl() {
    final EntityEditModel editModel = getTableModel().getEditModel();
    return ControlFactory.methodControl(this, "delete", FrameworkMessages.get(FrameworkMessages.DELETE),
            new AggregateState(AggregateState.Type.AND,
                    editModel.stateAllowDelete(),
                    getTableModel().stateSelectionEmpty().getReversedState()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP), 0, null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for exporting the selected records to file
   */
  public Control getExportControl() {
    return ControlFactory.methodControl(this, "exportSelected",
            FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED) + "...",
            getTableModel().stateSelectionEmpty().getReversedState(),
            FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED_TIP), 0, null,
            Images.loadImage(Images.IMG_SAVE_16));
  }

  /**
   * @return a control for printing the table
   */
  public Control getPrintTableControl() {
    final String printCaption = FrameworkMessages.get(FrameworkMessages.PRINT_TABLE);
    return ControlFactory.methodControl(this, "printTable", printCaption, null,
            printCaption, printCaption.charAt(0), null, Images.loadImage("Print16.gif"));
  }

  /**
   * Queries the user on which property to update, after which it calls the
   * <code>updateSelectedEntities(property)</code> with that property
   * @param editModel the edit model for performing the actual update
   * @see #updateSelectedEntities(org.jminor.framework.domain.Property, org.jminor.framework.client.model.EntityEditModel)
   * @see #getInputProvider(org.jminor.framework.domain.Property, java.util.List, org.jminor.framework.client.model.EntityEditModel)
   */
  public void updateSelectedEntities(final EntityEditModel editModel) {
    try {
      updateSelectedEntities(getPropertyToUpdate(), editModel);
    }
    catch (CancelException e) {/**/}
  }

  /**
   * Retrieves a new property value via input dialog and performs an update on the selected entities
   * @param propertyToUpdate the property to update
   * @param editModel the edit model for performing the actual update
   * @see #getInputProvider(org.jminor.framework.domain.Property, java.util.List, org.jminor.framework.client.model.EntityEditModel)
   */
  public void updateSelectedEntities(final Property propertyToUpdate, final EntityEditModel editModel) {
    if (getTableModel().stateSelectionEmpty().isActive())
      return;

    final List<Entity> selectedEntities = EntityUtil.copyEntities(getTableModel().getSelectedItems());
    final InputProviderPanel inputPanel = new InputProviderPanel(propertyToUpdate.getCaption(),
            getInputProvider(propertyToUpdate, selectedEntities, editModel));
    UiUtil.showInDialog(this, inputPanel, true, FrameworkMessages.get(FrameworkMessages.SET_PROPERTY_VALUE),
            null, inputPanel.getOkButton(), inputPanel.eventButtonClicked());
    if (inputPanel.isEditAccepted()) {
      EntityUtil.setPropertyValue(propertyToUpdate.getPropertyID(), inputPanel.getValue(), selectedEntities);
      try {
        UiUtil.setWaitCursor(true, this);
        editModel.update(selectedEntities);
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
  public void viewSelectionDependencies() throws DbException {
    final Map<String, List<Entity>> dependencies;
    try {
      UiUtil.setWaitCursor(true, this);
      dependencies = getTableModel().getSelectionDependencies();
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
    if (dependencies.size() > 0) {
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
  public void delete() throws DbException, CancelException {
    if (confirmDelete()) {
      try {
        UiUtil.setWaitCursor(true, this);
        getEditModel().delete(getTableModel().getSelectedItems());
      }
      finally {
        UiUtil.setWaitCursor(false, this);
      }
    }
  }

  /**
   * Exports the selected records as a JSON file
   * @throws CancelException in case the action is cancelled
   * @throws org.json.JSONException in case of a JSON exception
   */
  public void exportSelected() throws CancelException, JSONException {
    final List<Entity> selected = getTableModel().getSelectedItems();
    Util.writeFile(EntityUtil.getJSONString(selected, false, 2), UiUtil.chooseFileToSave(this, null, null));
    JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.EXPORT_SELECTED_DONE));
  }

  /**
   * Prints the table if one is available
   */
  public void printTable() {
    try {
      getJTable().print();
    }
    catch (PrinterException pr) {
      throw new RuntimeException(pr);
    }
  }

  public Control getToggleSummaryPanelControl() {
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
  public Control getToggleSearchPanelControl() {
    if (!getTableModel().isQueryConfigurationAllowed())
      return null;

    final Control ret = new Control() {
      @Override
      public void actionPerformed(ActionEvent event) {
        toggleSearchPanel();
      }
    };
    ret.setIcon(Images.loadImage(Images.IMG_FILTER_16));
    ret.setDescription(FrameworkMessages.get(FrameworkMessages.SEARCH));

    return ret;
  }

  public Control getClearSelectionControl() {
    final Control clearSelection = ControlFactory.methodControl(getTableModel(), "clearSelection", null,
            getTableModel().stateSelectionEmpty().getReversedState(), null, -1, null,
            Images.loadImage("ClearSelection16.gif"));
    clearSelection.setDescription(FrameworkMessages.get(FrameworkMessages.CLEAR_SELECTION_TIP));

    return clearSelection;
  }

  public Control getMoveSelectionDownControl() {
    final Control selectionDown = ControlFactory.methodControl(getTableModel(), "moveSelectionDown",
            Images.loadImage(Images.IMG_DOWN_16));
    selectionDown.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_DOWN_TIP));

    return selectionDown;
  }

  public Control getMoveSelectionUpControl() {
    final Control selectionUp = ControlFactory.methodControl(getTableModel(), "moveSelectionUp",
            Images.loadImage(Images.IMG_UP_16));
    selectionUp.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_UP_TIP));

    return selectionUp;
  }

  /**
   * @return an Event fired when the search panel state is changed
   */
  public Event eventSearchPanelVisibilityChanged() {
    return evtSearchPanelVisibilityChanged;
  }

  /**
   * @return an Event fired when the summary panel state is changed
   */
  public Event eventSummaryPanelVisibilityChanged() {
    return evtSummaryPanelVisibilityChanged;
  }

  /**
   * @return an Event fired when the table is double clicked
   */
  public Event eventTableDoubleClicked() {
    return evtTableDoubleClicked;
  }

  /**
   * Creates a static entity table panel showing the given entities
   * @param entities the entities to show in the panel
   * @param dbProvider the EntityDbProvider, in case the returned panel should require one
   * @return a static EntityTablePanel showing the given entities
   */
  public static EntityTablePanel createStaticEntityTablePanel(final Collection<Entity> entities, final EntityDbProvider dbProvider) {
    if (entities == null || entities.size() == 0)
      throw new RuntimeException("Cannot create an EntityPanel without the entities");

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
    final EntityTablePanel tablePanel = new EntityTablePanel(new EntityTableModel(new EntityEditModel(entityID, dbProvider)) {
      @Override
      protected List<Entity> performQuery(final Criteria<Property> criteria) {
        return new ArrayList<Entity>(entities);
      }
      @Override
      public boolean isQueryConfigurationAllowed() {
        return false;
      }
    }, null, null) {
      @Override
      protected JPanel initializeSearchPanel() {
        return null;
      }
    };
    tablePanel.getTableModel().refresh();

    return tablePanel;
  }

  public AbstractAction initializeViewImageAction(final String imagePathPropertyID) {
    return new AbstractAction() {
      public void actionPerformed(final ActionEvent e) {
        try {
          final EntityTableModel tableModel = getTableModel();
          if (!tableModel.getSelectionModel().isSelectionEmpty()) {
            final Entity selected = tableModel.getSelectedItem();
            if (!selected.isValueNull(imagePathPropertyID))
              showImage(selected.getStringValue(imagePathPropertyID), EntityTablePanel.this);
          }
        }
        catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    };
  }

  private static void showImage(final String imagePath, final JComponent dialogParent) throws IOException {
    if (imagePath != null && imagePath.length() > 0) {
      if (!isImage(imagePath))
        return;
      final NavigableImagePanel imagePanel = new NavigableImagePanel();
      final File imageFile = new File(imagePath);
      if (imageFile.exists()) {
        final BufferedImage bufferedImage = ImageIO.read(imageFile);
        imagePanel.setImage(bufferedImage);
        final JDialog dialog = initializeDialog(dialogParent, imagePanel);

        dialog.setTitle(imageFile.getName());

        if (!dialog.isShowing())
          dialog.setVisible(true);
      }
      else {
        throw new RuntimeException("Image does not exist: " + imagePath);
      }
    }
  }

  private static boolean isImage(final String imagePath) {
    final String lowerCasePath = imagePath.toLowerCase();
    return lowerCasePath.endsWith(".gif")
            || lowerCasePath.endsWith(".tif")
            || lowerCasePath.endsWith(".jpg")
            || lowerCasePath.endsWith(".jpeg")
            || lowerCasePath.endsWith(".png")
            || lowerCasePath.endsWith(".bmp");
  }

  private static JDialog initializeDialog(final JComponent parent, final NavigableImagePanel panel) {
    final JDialog ret =  new JDialog(UiUtil.getParentWindow(parent));
    ret.setLayout(new BorderLayout());
    ret.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    UiUtil.addKeyEvent(ret.getRootPane(), KeyEvent.VK_ESCAPE, new AbstractAction("close") {
      public void actionPerformed(ActionEvent e) {
        ret.dispose();
      }
    });
    ret.add(panel, BorderLayout.CENTER);
    ret.setSize(UiUtil.getScreenSizeRatio(0.5));
    ret.setLocationRelativeTo(parent);
    ret.setModal(false);

    return ret;
  }

  /**
   * Override to add event bindings
   */
  protected void bindEvents() {}

  /**
   * Initializes the UI
   * @param tablePopupControls the ControlSet on which to base the table's popup menu
   * @param toolbarControlSet the ControlSet on which to base the south toolbar
   */
  protected void initializeUI(final ControlSet tablePopupControls, final ControlSet toolbarControlSet) {
    final JPanel tableSearchAndSummaryPanel = new JPanel(new BorderLayout());
    setLayout(new BorderLayout());
    if (searchPanel != null) {
      searchScrollPane = new JScrollPane(searchPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      if (searchPanel instanceof EntityTableSearchPanel)
        searchScrollPane.getHorizontalScrollBar().setModel(getTableScrollPane().getHorizontalScrollBar().getModel());
      tableSearchAndSummaryPanel.add(searchScrollPane, BorderLayout.NORTH);
    }

    if (searchPanel instanceof EntityTableSearchPanel) {
      ((EntityTableSearchPanel)searchPanel).eventAdvancedChanged().addListener(new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          if (isSearchPanelVisible()) {
            revalidateAndShowSearchPanel();
          }
        }
      });
    }
    setTablePopupMenu(getJTable(), tablePopupControls == null ? new ControlSet() : tablePopupControls);

    final JScrollPane tableScrollPane = getTableScrollPane();
    tableSearchAndSummaryPanel.add(tableScrollPane, BorderLayout.CENTER);
    add(tableSearchAndSummaryPanel, BorderLayout.CENTER);
    if (summaryPanel != null) {
      summaryScrollPane = new JScrollPane(summaryPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      horizontalTableScrollBar = tableScrollPane.getHorizontalScrollBar();
      tableScrollPane.getViewport().addChangeListener(new ChangeListener() {
        public void stateChanged(final ChangeEvent event) {
          horizontalTableScrollBar.setVisible(tableScrollPane.getViewport().getViewSize().width > tableScrollPane.getSize().width);
          revalidate();
        }
      });
      summaryScrollPane.getHorizontalScrollBar().setModel(horizontalTableScrollBar.getModel());
      summaryScrollPane.setVisible(false);
      summaryBasePanel = new JPanel(new BorderLayout());
      tableSearchAndSummaryPanel.add(summaryBasePanel, BorderLayout.SOUTH);
    }

    final JPanel southPanel = initializeSouthPanel(toolbarControlSet);
    if (southPanel != null)
      add(southPanel, BorderLayout.SOUTH);

    setSearchPanelVisible(Configuration.getBooleanValue(Configuration.DEFAULT_SEARCH_PANEL_STATE));
  }

  /**
   * Initializes the south panel, override and return null for no south panel.
   * @param toolbarControlSet the control set from which to create the toolbar
   * @return the south panel, or null if no south panel should be used
   */
  protected JPanel initializeSouthPanel(final ControlSet toolbarControlSet) {
    statusMessageLabel = new JLabel("", JLabel.CENTER);
    statusMessageLabel.setFont(new Font(statusMessageLabel.getFont().getName(), Font.PLAIN, 12));
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
    if (refreshToolBar != null)
      panel.add(refreshToolBar, BorderLayout.WEST);

    if (toolbarControlSet != null) {
      final JToolBar southToolBar = ControlProvider.createToolbar(toolbarControlSet, JToolBar.HORIZONTAL);
      for (final Component component : southToolBar.getComponents())
        component.setPreferredSize(new Dimension(20, 20));
      southToolBar.setFocusable(false);
      southToolBar.setFloatable(false);
      southToolBar.setRollover(true);
      panel.add(southToolBar, BorderLayout.EAST);
    }

    return panel;
  }

  @Override
  protected JTextField initializeSearchField() {
    final JTextField searchField = super.initializeSearchField();
    searchField.setPreferredSize(new Dimension(searchField.getWidth(), 16));
    searchField.setBorder(BorderFactory.createLineBorder(searchField.getForeground(), 1));
    searchField.setColumns(8);
    UiUtil.selectAllOnFocusGained(searchField);
    UiUtil.addKeyEvent(searchField, KeyEvent.VK_ESCAPE, new AbstractAction("selectTablePanel") {
      public void actionPerformed(ActionEvent event) {
        getJTable().requestFocusInWindow();
      }
    });

    return searchField;
  }

  /**
   * Adds a popup menu to <code>table</code>
   * @param table the table
   * @param popupControls a ControlSet specifying the controls in the popup menu
   */
  protected void setTablePopupMenu(final JTable table, final ControlSet popupControls) {
    if (popupControls.size() == 0)
      return;

    final JPopupMenu popupMenu = ControlProvider.createPopupMenu(popupControls);
    table.setComponentPopupMenu(popupMenu);
    table.getTableHeader().setComponentPopupMenu(popupMenu);
    if (table.getParent() != null)
      ((JComponent) table.getParent()).setComponentPopupMenu(popupMenu);
    UiUtil.addKeyEvent(table, KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_FOCUSED,
            new AbstractAction("showPopupMenu") {
              public void actionPerformed(ActionEvent event) {
                popupMenu.show(table, 100, table.getSelectedRow() * table.getRowHeight());
              }
            });
    UiUtil.addKeyEvent(table, KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK, JComponent.WHEN_FOCUSED,
            new AbstractAction("showEntityMenu") {
              public void actionPerformed(ActionEvent event) {
                showEntityMenu(new Point(100, table.getSelectedRow() * table.getRowHeight()));
              }
            });
  }

  /**
   * Initializes the controls available to this EntityTablePanel by mapping them to their respective
   * control codes (EntityTablePanel.UPDATE_SELECTED, DELETE_SELECTED etc) via the <code>setControl(String, Control) method,
   * these can then be retrieved via the <code>getControl(String)</code> method.
   * @param printControls the control set on which to base th print sub menu
   * @see org.jminor.common.ui.control.Control
   * @see #setControl(String, org.jminor.common.ui.control.Control)
   * @see #getControl(String)
   */
  protected void setupControls(final ControlSet printControls) {
    final EntityEditModel editModel = getEditModel();
    if (!editModel.isReadOnly() && editModel.isDeleteAllowed())
      setControl(DELETE_SELECTED, getDeleteSelectedControl());
    if (!editModel.isReadOnly() && editModel.isMultipleUpdateAllowed())
      setControl(UPDATE_SELECTED, getUpdateSelectedControlSet());
    if (getTableModel().isQueryConfigurationAllowed()) {
      setControl(CONFIGURE_QUERY, getConfigureQueryControl());
      setControl(SEARCH_PANEL_VISIBLE, getSearchPanelControl());
    }
    setControl(CLEAR, getClearControl());
    setControl(REFRESH, getRefreshControl());
    setControl(SELECT_COLUMNS, getSelectColumnsControl());
    setControl(EXPORT_JSON, getExportControl());
    setControl(VIEW_DEPENDENCIES, getViewDependenciesControl());
    if (summaryPanel != null)
      setControl(TOGGLE_SUMMARY_PANEL, getToggleSummaryPanelControl());
    if (searchPanel != null)
      setControl(TOGGLE_SEARCH_PANEL, getToggleSearchPanelControl());
    if (printControls != null) {
      printControls.add(getPrintTableControl());
      if (!printControls.hasIcon())
        printControls.setIcon(Images.loadImage("Print16.gif"));
    }
    setControl(PRINT_TABLE, printControls == null ? getPrintTableControl() : printControls);
    setControl(CLEAR_SELECTION, getClearSelectionControl());
    setControl(MOVE_SELECTION_UP, getMoveSelectionDownControl());
    setControl(MOVE_SELECTION_DOWN, getMoveSelectionUpControl());
    setControl(COPY_TABLE_DATA, getCopyControlSet());
  }

  /**
   * Associates <code>control</code> with <code>controlCode</code>
   * @param controlCode the control code
   * @param control the control to associate with <code>controlCode</code>
   */
  protected void setControl(final String controlCode, final Control control) {
    if (control == null)
      controlMap.remove(controlCode);
    else
      controlMap.put(controlCode, control);
  }

  protected ControlSet getToolbarControls(final ControlSet additionalToolbarControls) {
    final ControlSet toolbarControls = new ControlSet("");
    if (controlMap.containsKey(TOGGLE_SUMMARY_PANEL))
      toolbarControls.add(controlMap.get(TOGGLE_SUMMARY_PANEL));
    if (controlMap.containsKey(TOGGLE_SEARCH_PANEL))
      toolbarControls.add(controlMap.get(TOGGLE_SEARCH_PANEL));
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
    if (additionalToolbarControls != null) {
      toolbarControls.addSeparator();
      for (final Action action : additionalToolbarControls.getActions()) {
        if (action == null)
          toolbarControls.addSeparator();
        else
          toolbarControls.add(action);
      }
    }

    return toolbarControls;
  }

  protected ControlSet getPopupControls(final ControlSet additionalPopupControls) {
    final ControlSet popupControls = new ControlSet("");
    popupControls.add(controlMap.get(REFRESH));
    popupControls.add(controlMap.get(CLEAR));
    popupControls.addSeparator();
    if (additionalPopupControls != null && additionalPopupControls.getActions().size() > 0) {
      if (additionalPopupControls.hasName())
        popupControls.add(additionalPopupControls);
      else
        popupControls.addAll(additionalPopupControls);
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
    if (controlMap.containsKey(PRINT_TABLE)) {
      popupControls.add(controlMap.get(PRINT_TABLE));
      separatorRequired = true;
    }
    if (controlMap.containsKey(SELECT_COLUMNS)) {
      if (separatorRequired)
        popupControls.addSeparator();
      popupControls.add(controlMap.get(SELECT_COLUMNS));
    }
    if (controlMap.containsKey(CONFIGURE_QUERY)) {
      if (separatorRequired) {
        popupControls.addSeparator();
        separatorRequired = false;
      }
      popupControls.add(controlMap.get(CONFIGURE_QUERY));
      if (searchPanel != null) {
        final ControlSet searchControls = ((EntityTableSearchPanel) searchPanel).getControls();
        if (controlMap.containsKey(SEARCH_PANEL_VISIBLE))
          searchControls.add(getControl(SEARCH_PANEL_VISIBLE));
        popupControls.add(searchControls);
      }
    }
    if (separatorRequired)
      popupControls.addSeparator();
    popupControls.add(controlMap.get(COPY_TABLE_DATA));

    return popupControls;
  }

  protected ToggleBeanValueLink getSearchPanelControl() {
    return ControlFactory.toggleControl(this, "searchPanelVisible",
            FrameworkMessages.get(FrameworkMessages.SHOW), evtSearchPanelVisibilityChanged);
  }

  protected ControlSet getCopyControlSet() {
    return new ControlSet(Messages.get(Messages.COPY), getCopyCellControl(), getCopyTableWithHeaderControl());
  }

  /**
   * Called before a delete is performed, if true is returned the delete action is performed otherwise it is canceled
   * @return true if the delete action should be performed
   */
  protected boolean confirmDelete() {
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
  protected Control getRefreshControl() {
    final String refreshCaption = FrameworkMessages.get(FrameworkMessages.REFRESH);
    return ControlFactory.methodControl(getTableModel(), "refresh", refreshCaption,
            null, FrameworkMessages.get(FrameworkMessages.REFRESH_TIP), refreshCaption.charAt(0),
            null, Images.loadImage(Images.IMG_REFRESH_16));
  }

  /**
   * @return a Control for clearing the underlying table model, that is, removing all rows
   */
  protected Control getClearControl() {
    final String clearCaption = FrameworkMessages.get(FrameworkMessages.CLEAR);
    return ControlFactory.methodControl(getTableModel(), "clear", clearCaption,
            null, null, clearCaption.charAt(0), null, null);
  }

  /**
   * Initializes the panel containing the table column summary panels
   * @return the summary panel
   */
  protected EntityTableSummaryPanel initializeSummaryPanel() {
    final EntityTableSummaryPanel summaryPanel = new EntityTableSummaryPanel(getTableModel());
    summaryPanel.setVerticalFillerWidth(UiUtil.getPreferredScrollBarWidth());

    return summaryPanel;
  }

  /**
   * @return an initialized search panel
   */
  protected JPanel initializeSearchPanel() {
    return getTableModel().getSearchModel().isSimpleSearch() ? initializeSimpleSearchPanel() : initializeAdvancedSearchPanel();
  }

  /**
   * Initializes a simple search panel, with a single search field, which performs a search based on the default
   * search properties or if none are defined all string based properties
   * @return a simple search panel
   * @see org.jminor.framework.domain.EntityDefinition#setSearchPropertyIDs(String[])
   */
  protected JPanel initializeSimpleSearchPanel() {
    final List<Property> searchableProperties = getSearchProperties();
    if (searchableProperties.size() == 0)
      throw new RuntimeException("Unable to create a simple search panel for entity: "
              + getTableModel().getEntityID() + ", no STRING based properties found");

    final JTextField searchField = new JTextField();
    final Action action = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(final ActionEvent event) {
        performSimpleSearch(searchField.getText(), searchableProperties);
      }
    };

    searchField.addActionListener(action);
    final JPanel searchPanel = new JPanel(new BorderLayout(5,5));
    searchPanel.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.CONDITION)));
    searchPanel.add(searchField, BorderLayout.CENTER);
    searchPanel.add(new JButton(action), BorderLayout.EAST);

    return searchPanel;
  }

  /**
   * @return an initialized EntityTableSearchPanel
   */
  protected JPanel initializeAdvancedSearchPanel() {
    final EntityTableSearchPanel searchPanel = new EntityTableSearchPanel(getTableModel().getSearchModel());
    searchPanel.setVerticalFillerWidth(UiUtil.getPreferredScrollBarWidth());

    return searchPanel;
  }

  /**
   * @return the refresh toolbar
   */
  protected JToolBar initializeRefreshToolbar() {
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
    button.setPreferredSize(new Dimension(20,20));
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
   * @param editModel the edit model involved in the updating
   * @return the InputProvider handling input for <code>property</code>
   * @see #updateSelectedEntities
   */
  @SuppressWarnings({"UnusedDeclaration"})
  protected InputProvider getInputProvider(final Property property, final List<Entity> toUpdate,
                                           final EntityEditModel editModel) {
    final Collection<Object> values = EntityUtil.getDistinctPropertyValues(toUpdate, property.getPropertyID());
    final Object currentValue = values.size() == 1 ? values.iterator().next() : null;
    if (property instanceof Property.ValueListProperty)
      return new ValueListInputProvider(currentValue, ((Property.ValueListProperty) property).getValues());
    if (property.isTimestamp())
      return new DateInputProvider((Date) currentValue, Configuration.getDefaultTimestampFormat());
    if (property.isDate())
      return new DateInputProvider((Date) currentValue, Configuration.getDefaultDateFormat());
    if (property.isDouble())
      return new DoubleInputProvider((Double) currentValue);
    if (property.isInteger())
      return new IntInputProvider((Integer) currentValue);
    if (property.isBoolean())
      return new BooleanInputProvider((Boolean) currentValue);
    if (property.isString())
      return new TextInputProvider(property.getCaption(), editModel.getValueProvider(property), (String) currentValue);
    if (property.isReference())
      return createEntityInputProvider((Property.ForeignKeyProperty) property, (Entity) currentValue, editModel);

    throw new IllegalArgumentException("Unsupported property type: " + property.getType());
  }

  /**
   * Creates a InputProvider for the given foreign key property
   * @param foreignKeyProperty the property
   * @param currentValue the current value to initialize the InputProvider with
   * @param editModel the edit model involved in the updating
   * @return a Entity InputProvider
   */
  protected InputProvider createEntityInputProvider(final Property.ForeignKeyProperty foreignKeyProperty, final Entity currentValue,
                                                    final EntityEditModel editModel) {
    if (!EntityRepository.isLargeDataset(foreignKeyProperty.getReferencedEntityID())) {
      return new EntityComboProvider(editModel.createEntityComboBoxModel(foreignKeyProperty), currentValue);
    }
    else {
      List<Property> searchProperties = EntityRepository.getSearchProperties(foreignKeyProperty.getReferencedEntityID());
      if (searchProperties.size() == 0)
        throw new RuntimeException("No searchable properties found for entity: " + foreignKeyProperty.getReferencedEntityID());

      return new EntityLookupProvider(editModel.createEntityLookupModel(foreignKeyProperty.getReferencedEntityID(), null, searchProperties), currentValue);
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
  protected MouseListener initializeTableMouseListener() {
    return new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
          final Action doubleClickAction = getTableDoubleClickAction();
          if (doubleClickAction != null)
            doubleClickAction.actionPerformed(new ActionEvent(getJTable(), -1, "doubleClick"));
        }
        else if (event.isAltDown()) {
          showEntityMenu(event.getPoint());
        }
      }
    };
  }

  /**
   * Initializes the JTable instance
   * @return the JTable instance
   * @see org.jminor.framework.domain.EntityDefinition#setRowColoring(boolean)
   * @see org.jminor.framework.domain.Entity.Proxy#getBackgroundColor(org.jminor.framework.domain.Entity)
   */
  @Override
  protected JTable initializeJTable() {
    final TableColumnModel columnModel = getTableModel().getColumnModel();
    final JTable table = new JTable(getTableModel().getTableSorter(), columnModel, getTableModel().getSelectionModel());
    final TableCellRenderer tableCellRenderer = initializeTableCellRenderer();
    final Enumeration<TableColumn> columnEnumeration = columnModel.getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final TableColumn column = columnEnumeration.nextElement();
      column.setCellRenderer(tableCellRenderer);
      column.setResizable(true);
    }

    table.addMouseListener(initializeTableMouseListener());

    final JTableHeader header = table.getTableHeader();
    final TableCellRenderer defaultHeaderRenderer = header.getDefaultRenderer();
    final Font defaultFont = table.getFont();
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

    table.setAutoResizeMode(getAutoResizeMode());
    getTableModel().getTableSorter().setTableHeader(header);

    return table;
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

  private void performSimpleSearch(final String searchText, final List<Property> searchProperties) {
    final EntityTableSearchModel tableSearchModel = getTableModel().getSearchModel();
    final CriteriaSet.Conjunction conjunction = tableSearchModel.getSearchConjunction();
    try {
      tableSearchModel.clearPropertySearchModels();
      tableSearchModel.setSearchConjunction(CriteriaSet.Conjunction.OR);
      if (searchText.length() > 0) {
        final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
        final String searchTextWithWildcards = wildcard + searchText + wildcard;
        for (final Property searchProperty : searchProperties) {
          final PropertySearchModel propertySearchModel = tableSearchModel.getPropertySearchModel(searchProperty.getPropertyID());
          propertySearchModel.setCaseSensitive(false);
          propertySearchModel.setUpperBound(searchTextWithWildcards);
          propertySearchModel.setSearchType(SearchType.LIKE);
          propertySearchModel.setSearchEnabled(true);
        }
      }

      getTableModel().refresh();
    }
    finally {
      tableSearchModel.setSearchConjunction(conjunction);
    }
  }

  private List<Property> getSearchProperties() {
    final List<Property> searchableProperties = new ArrayList<Property>();
    final String[] defaultSearchPropertyIDs = EntityRepository.getEntitySearchPropertyIDs(getTableModel().getEntityID());
    if (defaultSearchPropertyIDs != null) {
      for (final String propertyID : defaultSearchPropertyIDs)
        searchableProperties.add(EntityRepository.getProperty(getTableModel().getEntityID(), propertyID));
    }
    else {
      for (final Property property : EntityRepository.getDatabaseProperties(getTableModel().getEntityID())) {
        if (property.isString() && !property.isHidden())
          searchableProperties.add(property);
      }
    }

    return searchableProperties;
  }

  private Control getCopyCellControl() {
    return new Control(FrameworkMessages.get(FrameworkMessages.COPY_CELL),
            getTableModel().stateSelectionEmpty().getReversedState()) {
      @Override
      public void actionPerformed(final ActionEvent event) {
        final JTable table = getJTable();
        final Object value = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn());
        Util.setClipboard(value == null ? "" : value.toString());
      }
    };
  }

  private Control getCopyTableWithHeaderControl() {
    return new Control(FrameworkMessages.get(FrameworkMessages.COPY_TABLE_WITH_HEADER)) {
      @Override
      public void actionPerformed(final ActionEvent event) {
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
    while (iterator.hasNext())
      if (!getTableModel().isColumnVisible(iterator.next()))
        iterator.remove();
    for (final Property property : properties)
      headerValues.add(property.getCaption());

    final String[][] header = new String[][] {headerValues.toArray(new String[headerValues.size()])};

    final List<Entity> entities = getTableModel().getSelectionModel().isSelectionEmpty()
            ? getTableModel().getAllItems(false) : getTableModel().getSelectedItems();

    final String[][] data = new String[entities.size()][];
    for (int i = 0; i < data.length; i++) {
      final List<String> line = new ArrayList<String>(15);
      for (final Property property : properties)
        line.add(entities.get(i).getValueAsString(property));

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
    final JComboBox box = new JComboBox(new Vector<Property>(EntityUtil.getUpdateProperties(getTableModel().getEntityID())));
    final int ret = JOptionPane.showOptionDialog(this, box,
            FrameworkMessages.get(FrameworkMessages.SELECT_PROPERTY_FOR_UPDATE),
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);

    if (ret == JOptionPane.OK_OPTION)
      return (Property) box.getSelectedItem();
    else
      throw new CancelException();
  }

  private void updateStatusMessage() {
    if (statusMessageLabel != null) {
      final String status = getTableModel().getStatusMessage();
      statusMessageLabel.setText(status);
      statusMessageLabel.setToolTipText(status);
    }
  }

  private Map<String, PropertyFilterPanel> initializeFilterPanels() {
    final Map<String, PropertyFilterPanel> propertyFilterPanels =
            new HashMap<String, PropertyFilterPanel>(getTableModel().getSearchModel().getPropertyFilterModels().size());
    final Enumeration<TableColumn> columns = getJTable().getColumnModel().getColumns();
    while (columns.hasMoreElements()) {
      final TableColumn column = columns.nextElement();
      final PropertyFilterModel model = getTableModel().getSearchModel().getPropertyFilterModel(((Property) column.getIdentifier()).getPropertyID());
      model.eventSearchStateChanged().addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent event) {
          if (model.isSearchEnabled())
            addFilterIndicator(column);
          else
            removeFilterIndicator(column);

          getJTable().getTableHeader().repaint();
        }
      });
      if (model.isSearchEnabled())
        addFilterIndicator(column);

      propertyFilterPanels.put(model.getSearchProperty().getPropertyID(), new PropertyFilterPanel(model, true, true));
    }

    return propertyFilterPanels;
  }

  private void bindEventsInternal() {
    if (!getEditModel().isReadOnly() && getEditModel().isDeleteAllowed()) {
      getJTable().addKeyListener(new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent event) {
          if (event.getKeyChar() == KeyEvent.VK_DELETE && !getTableModel().stateSelectionEmpty().isActive())
            try {
              delete();
            }
            catch (DbException e) {
              throw new RuntimeException(e);
            }
            catch (CancelException e) {/**/}
        }
      });
    }
    getTableModel().eventRefreshStarted().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        UiUtil.setWaitCursor(true, EntityTablePanel.this);
      }
    });
    getTableModel().eventRefreshDone().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        UiUtil.setWaitCursor(false, EntityTablePanel.this);
      }
    });
    getJTable().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        if(event.getClickCount() == 2) {
          evtTableDoubleClicked.fire();
        }
      }
    });
    final ActionListener statusListener = new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        updateStatusMessage();
      }
    };
    getTableModel().eventSelectionChanged().addListener(statusListener);
    getTableModel().eventFilteringDone().addListener(statusListener);
    getTableModel().eventTableDataChanged().addListener(statusListener);

    getJTable().getTableHeader().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent event) {
        if (event.isShiftDown())
          toggleColumnFilterPanel(event);
      }
    });

    getTableModel().eventSelectedIndexChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        if (!getTableModel().stateSelectionEmpty().isActive())
          scrollToCoordinate(getTableModel().getSelectedIndex(), getJTable().getSelectedColumn());
      }
    });

    getTableModel().getSearchModel().stateSearchStateChanged().eventStateChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        getJTable().getTableHeader().repaint();
        getJTable().repaint();
      }
    });
  }

  private void toggleColumnFilterPanel(final MouseEvent event) {
    final Property property = getTableModel().getColumnProperty(getTableModel().getColumnModel().getColumnIndexAtX(event.getX()));

    toggleFilterPanel(event.getLocationOnScreen(), propertyFilterPanels.get(property.getPropertyID()), getJTable());
  }

  private void revalidateAndShowSearchPanel() {
    searchScrollPane.getViewport().setView(null);
    searchScrollPane.getViewport().setView(searchPanel);
    revalidate();
  }

  private static void toggleFilterPanel(final Point position, final PropertyFilterPanel columnFilterPanel,
                                        final Container parent) {
    if (columnFilterPanel.isDialogActive())
      columnFilterPanel.inactivateDialog();
    else
      columnFilterPanel.activateDialog(parent, position);
  }

  private static void addFilterIndicator(final TableColumn column) {
    String val = (String) column.getHeaderValue();
    if (val.length() > 0)
      if (val.charAt(0) != FILTER_INDICATOR)
        val = FILTER_INDICATOR + val;

    column.setHeaderValue(val);
  }

  private static void removeFilterIndicator(final TableColumn column) {
    String val = (String) column.getHeaderValue();
    if (val.length() > 0 && val.charAt(0) == FILTER_INDICATOR)
      val = val.substring(1);

    column.setHeaderValue(val);
  }

  private static void showDependenciesDialog(final Map<String, List<Entity>> dependencies, final EntityDbProvider dbProvider,
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

  private static JPanel createDependenciesPanel(final Map<String, List<Entity>> dependencies,
                                                final EntityDbProvider dbProvider) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    tabPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    for (final Map.Entry<String, List<Entity>> entry : dependencies.entrySet()) {
      final List<Entity> dependantEntities = entry.getValue();
      if (dependantEntities.size() > 0)
        tabPane.addTab(entry.getKey(), createStaticEntityTablePanel(dependantEntities, dbProvider));
    }
    panel.add(tabPane, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Populates the given root menu with the property values of the given entity
   * @param rootMenu the menu to populate
   * @param entity the entity
   * @param dbProvider if provided then lazy loaded entity references are loaded so that the full object graph can be shown
   * @throws Exception in case of an exception
   */
  private static void populateEntityMenu(final JComponent rootMenu, final Entity entity, final EntityDbProvider dbProvider) throws Exception {
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<Property.PrimaryKeyProperty>(EntityRepository.getPrimaryKeyProperties(entity.getEntityID())));
    populateForeignKeyMenu(rootMenu, entity, dbProvider, new ArrayList<Property.ForeignKeyProperty>(EntityRepository.getForeignKeyProperties(entity.getEntityID())));
    populateValueMenu(rootMenu, entity, new ArrayList<Property>(EntityRepository.getProperties(entity.getEntityID(), false)));
  }

  private static void populatePrimaryKeyMenu(final JComponent rootMenu, final Entity entity, final List<Property.PrimaryKeyProperty> primaryKeyProperties) {
    Util.collate(primaryKeyProperties);
    for (final Property.PrimaryKeyProperty property : primaryKeyProperties)
      rootMenu.add(new JMenuItem("[PK] " + property.getColumnName() + ": " + entity.getValueAsString(property.getPropertyID())));
  }

  private static void populateForeignKeyMenu(final JComponent rootMenu, final Entity entity, final EntityDbProvider dbProvider,
                                             final List<Property.ForeignKeyProperty> fkProperties) throws Exception {
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

  private static void populateValueMenu(final JComponent rootMenu, final Entity entity, final List<Property> properties) {
    Util.collate(properties);
    for (final Property property : properties) {
      if (!property.hasParentProperty() && !(property instanceof Property.ForeignKeyProperty)) {
        final String prefix = "[" + Property.getTypeClass(property.getType()).getSimpleName().substring(0, 1)
                + (property instanceof Property.DenormalizedViewProperty ? "*" : "")
                + (property instanceof Property.DenormalizedProperty ? "+" : "") + "] ";
        final String value = entity.isValueNull(property.getPropertyID()) ? "<null>" : entity.getValueAsString(property.getPropertyID());
        final boolean longValue = value != null && value.length() > 20;
        final JMenuItem menuItem = new JMenuItem(prefix + property + ": " + (longValue ? value.substring(0, 20) + "..." : value));
        if (longValue)
          menuItem.setToolTipText(value.length() > 1000 ? value.substring(0, 1000) : value);
        rootMenu.add(menuItem);
      }
    }
  }
}
