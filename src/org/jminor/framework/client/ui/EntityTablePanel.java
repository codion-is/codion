/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertyFilterModel;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableColumnModel;
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
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

public class EntityTablePanel extends JPanel {

  public final static char FILTER_INDICATOR = '*';

  /**
   * fired when the table is double clicked
   */
  public final Event evtTableDoubleClick = new Event("EntityTablePanel.evtTableDoubleClick");

  /**
   * fired when the search panel state is changed
   */
  public final Event evtSearchPanelVisibleChanged = new Event("EntityTablePanel.evtSearchPanelVisibleChanged");

  /**
   * fired when the summary panel state is changed
   */
  public final Event evtTableSummaryPanelVisibleChanged = new Event("EventTablePanel.evtTableSummaryPanelVisibleChanged");

  /**
   * the EntityTableModel instance used by this EntityTablePanel
   */
  private final EntityTableModel tableModel;

  /**
   * the JTable for showing the underlying entities
   */
  private final JTable entityTable;

  /**
   * the scroll pane used by the JTable instance
   */
  private final JScrollPane tableScrollPane;

  /**
   * the search panel
   */
  private final JPanel searchPanel;//todo should this implement a ITableSearchPanel perhaps?

  /**
   * a map mapping the summary panels to their respective properties
   */
  private final HashMap<String, PropertySummaryPanel> propertySummaryPanels = new HashMap<String, PropertySummaryPanel>();

  /**
   * the property filter panels
   */
  private final List<PropertyFilterPanel> propertyFilterPanels;

  /**
   * the scroll pane used for the search panel
   */
  private JScrollPane searchScrollPane;

  /**
   * the scroll pane used for the summary panel
   */
  private JScrollPane summaryScrollPane;

  /**
   * the panel used as a base panel for the summary panels, used for showing/hiding the summary panels
   */
  private JPanel summaryScrollPaneBase;

  /**
   * the horizontal table scroll bar
   */
  private JScrollBar horizontalTableScrollBar;

  /**
   * the south tool bar
   */
  private JToolBar southToolBar;

  /**
   * the label for showing the status of the table, that is, the number of rows, number of selected rows etc.
   */
  private JLabel lblStatusMessage;

  /**
   * the action performed when the table is double clicked
   */
  private Action doubleClickAction;

  /**
   * the toolbar containing the refresh button
   */
  private JToolBar searchRefreshToolBar;

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param popupControls a ControlSet on which the table popup menu is based
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet popupControls) {
    this(tableModel, popupControls, false, true);
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param popupControls a ControlSet on which the table popup menu is based
   * @param specialRendering true if each row should be colored according to the underlying entity
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet popupControls,
                          final boolean specialRendering) {
    this(tableModel, popupControls, specialRendering, true);
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param popupControls a ControlSet on which the table popup menu is based
   * @param specialRendering true if each row should be colored according to the underlying entity
   * @param allowQueryConfiguration true if the underlying query should be configurable
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet popupControls,
                          final boolean specialRendering, final boolean allowQueryConfiguration) {
    this.tableModel = tableModel;
    this.entityTable = initializeJTable(tableModel, specialRendering);
    this.tableScrollPane = new JScrollPane(entityTable);
    this.searchPanel = initializeSearchPanel();
    this.propertyFilterPanels = initializeFilterPanels();
    initializeUI(allowQueryConfiguration, popupControls);
    bindEvents();
    updateStatusMessage();
  }

  /**
   * @return the JTable instance used by this EntityTablePanel
   */
  public JTable getJTable() {
    return entityTable;
  }

  /**
   * Sets the TableCellRenderer for the property with the given property ID
   * @param propertyID the ID of the property for which to set the renderer
   * @param renderer the renderer to use for presenting column values for the given property
   */
  public void setTableCellRenderer(final String propertyID, final TableCellRenderer renderer) {
    entityTable.getColumn(EntityRepository.get().getProperty(getTableModel().getEntityID(), propertyID)).setCellRenderer(renderer);
  }

  /**
   * @param doubleClickAction the action to perform when a double click is performed on the table
   */
  public void setDoubleClickAction(final Action doubleClickAction) {
    this.doubleClickAction = doubleClickAction;
  }

  /**
   * @return the EntityTableModel used by this EntityTablePanel
   */
  public EntityTableModel getTableModel() {
    return tableModel;
  }

  /**
   * Hides or shows the active filter panels for this table panel
   * @param value true if the active filter panels should be shown, false if they should be hidden
   */
  public void setFilterPanelsVisible(final boolean value) {
    for (final PropertyFilterPanel columnFilterPanel : propertyFilterPanels) {
      if (value)
        columnFilterPanel.showDialog();
      else
        columnFilterPanel.hideDialog();
    }
  }

  /**
   * Shows a dialog for configuring the underlying EntityTableModel query
   */
  public void configureQuery() {
    final EntityCriteriaPanel panel;
    try {
      UiUtil.setWaitCursor(true, this);
      panel = new EntityCriteriaPanel(getTableModel());
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
    UiUtil.showInDialog(UiUtil.getParentWindow(this), panel, false,
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY),
            true, false, new AbstractAction(FrameworkMessages.get(FrameworkMessages.APPLY)) {
      public void actionPerformed(ActionEvent e) {
        try {
          if (getTableModel().isQueryRangeEnabled()) {
            getTableModel().setQueryRangeFrom(panel.getQueryRangeFrom());
            getTableModel().setQueryRangeTo(panel.getQueryRangeTo());
          }
          getTableModel().refresh();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    });
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the summary provider panel
   * @return the PropertySummaryPanel for the given property ID
   */
  public PropertySummaryPanel getSummaryProvider(final String propertyID) {
    return propertySummaryPanels.get(propertyID);
  }

  /**
   * Hides or shows the column summary panel for this EntityTablePanel
   * @param visible if true then the summary panel is shown, if false it is hidden
   */
  public void setSummaryPanelVisible(final boolean visible) {
    if (summaryScrollPane != null) {
      summaryScrollPane.setVisible(visible);
      if (visible) {
        summaryScrollPaneBase.add(summaryScrollPane, BorderLayout.NORTH);
        summaryScrollPaneBase.add(horizontalTableScrollBar, BorderLayout.SOUTH);
      }
      else {
        summaryScrollPaneBase.remove(horizontalTableScrollBar);
        tableScrollPane.setHorizontalScrollBar(horizontalTableScrollBar);
      }

      revalidate();
      evtTableSummaryPanelVisibleChanged.fire();
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
    if (searchScrollPane != null) {
      searchScrollPane.getViewport().setView(visible ? searchPanel : null);
      if (searchRefreshToolBar != null)
        searchRefreshToolBar.setVisible(visible);
      evtSearchPanelVisibleChanged.fire();
      revalidate();
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
  public String toString() {
    return "EntityTablePanel: " + getTableModel().getEntityID();
  }

  /**
   * Adds the given buttons to the south toolbar, in the order they are recieved
   * @param buttons the buttons to add to the south toolbar
   */
  public void addSouthPanelButtons(final AbstractButton... buttons) {
    if (buttons == null || buttons.length == 0)
      return;
    if (southToolBar == null)
      throw new RuntimeException("No south panel");

    for (final AbstractButton button : buttons) {
      if (button != null)
        southToolBar.add(button);
      else
        southToolBar.addSeparator();
    }
  }

  /**
   * Shows a dialog for selecting which columns to show/hide
   */
  public void selectTableColumns() {
    final JPanel togglePanel = new JPanel(new GridLayout(getJTable().getColumnCount(), 1));
    final Enumeration<TableColumn> columns = getJTable().getColumnModel().getColumns();
    final List<JCheckBox> buttonList = new ArrayList<JCheckBox>();
    while (columns.hasMoreElements()) {
      final TableColumn column = columns.nextElement();
      final JCheckBox chkColumn = new JCheckBox(column.getHeaderValue().toString(), column.getPreferredWidth() > 0);
      buttonList.add(chkColumn);
      togglePanel.add(chkColumn);
    }
    final JScrollPane scroller = new JScrollPane(togglePanel);
    scroller.setPreferredSize(new Dimension(200, 400));
    final int result = JOptionPane.showOptionDialog(this, scroller,
            FrameworkMessages.get(FrameworkMessages.SELECT_COLUMNS), JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, null, null);
    if (result == JOptionPane.OK_OPTION) {
      final TableColumnModel columnModel = getJTable().getColumnModel();
      for (final JCheckBox chkButton : buttonList) {
        final TableColumn column = columnModel.getColumn(buttonList.indexOf(chkButton));
        setPropertyColumnVisible((Property) column.getIdentifier(), chkButton.isSelected());
      }
    }
  }

  /**
   * @param property the property for which to query if its column is visible or hidden
   * @return true if the column is visible, false if it is hidden
   */
  public boolean isPropertyColumnVisible(final Property property) {
    return getJTable() != null && getJTable().getColumn(property).getPreferredWidth() > 0;
  }

  /**
   * Toggles the visibility of the column representing the given property.
   * This is done by setting the column width to 0 when hiding and setting it to the default width when showing.
   * This method does not prevent the column from being selected while traversing the table grid
   * with the arrow keys, something we'll call a *known bug*.
   * Hiding a column removes it from the query criteria, by disabling the underlying search model (PropertySearchModel)
   * @param property the property
   * @param visible if true the column is shown, otherwise it is hidden
   */
  public void setPropertyColumnVisible(final Property property, final boolean visible) {
    if (getJTable() == null)
      return;

    if (visible) {
      if (!isPropertyColumnVisible(property)) {
        final TableColumn column = getJTable().getColumn(property);
        column.setMaxWidth(Integer.MAX_VALUE);
        column.setMinWidth(15);
        Integer prw = property.getPreferredColumnWidth();
        if (prw == null || prw < 0)
          prw = 80;
        column.setPreferredWidth(prw);
      }
    }
    else {
      if (isPropertyColumnVisible(property)) {
        //disable the search model for the column to be hidden, to prevent confusion
        getTableModel().getSearchModel().setSearchEnabled(property.propertyID, false);
        final TableColumn column = getJTable().getColumn(property);
        column.setMinWidth(0);
        column.setPreferredWidth(0);
        column.setMaxWidth(0);
      }
    }
  }

  /**
   * Initializes the south panel
   * @param allowQueryConfiguration true if the underlying query is configurable
   * @return the south panel
   */
  protected JPanel initializeSouthPanel(final boolean allowQueryConfiguration) {
    southToolBar = new JToolBar(JToolBar.HORIZONTAL);
    southToolBar.setFocusable(false);
    southToolBar.setFloatable(false);
    southToolBar.setRollover(true);
    final JPanel ret = new JPanel(new BorderLayout());
    lblStatusMessage = new JLabel("", JLabel.CENTER);
    if (allowQueryConfiguration) {
      lblStatusMessage.addMouseListener(new MouseAdapter() {
        public void mouseReleased(MouseEvent e) {
          if (e.getClickCount() == 2) {
            configureQuery();
          }
        }
      });
    }
    lblStatusMessage.setFont(new Font(lblStatusMessage.getFont().getName(), Font.PLAIN, 12));
    ret.add(lblStatusMessage, BorderLayout.CENTER);
    ret.add(southToolBar, BorderLayout.EAST);

    return ret;
  }

  /**
   * Override to provide specific event bindings, remember to call <code>super.bindEvents()</code>
   */
  protected void bindEvents() {
    tableModel.evtRefreshStarted.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        UiUtil.setWaitCursor(true, EntityTablePanel.this);
      }
    });
    tableModel.evtRefreshDone.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        UiUtil.setWaitCursor(false, EntityTablePanel.this);
      }
    });
    entityTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me){
        if(me.getClickCount() == 2){
          evtTableDoubleClick.fire();
        }
      }
    });
    final ActionListener statusListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateStatusMessage();
      }
    };
    tableModel.evtSelectionChanged.addListener(statusListener);
    tableModel.evtFilteringDone.addListener(statusListener);
    tableModel.evtTableDataChanged.addListener(statusListener);

    tableModel.getTableSorter().evtTableHeaderShiftClick.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        toggleColumnFilterPanel(e);
      }
    });

    tableModel.evtSelectedIndexChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (!tableModel.stSelectionEmpty.isActive())
          entityTable.scrollRectToVisible(entityTable.getCellRect(
                  tableModel.getSelectedIndex(), entityTable.getSelectedColumn(), true));
      }
    });

    tableModel.getSearchModel().stSearchStateChanged.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        entityTable.getTableHeader().repaint();
        entityTable.repaint();
      }
    });
  }

  /**
   * Initializes the UI
   * @param allowQueryConfiguration true if the underlying query should be configurable
   * @param tablePopupControls the ControlSet on which to base the table's popup menu
   */
  protected void initializeUI(final boolean allowQueryConfiguration, final ControlSet tablePopupControls) {
    final JPanel base = new JPanel(new BorderLayout());
    setLayout(new BorderLayout());
    if (searchPanel != null) {
      searchScrollPane = new JScrollPane(searchPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      if (searchPanel instanceof EntityTableSearchPanel)
        searchScrollPane.getHorizontalScrollBar().setModel(tableScrollPane.getHorizontalScrollBar().getModel());
      base.add(searchScrollPane, BorderLayout.NORTH);
    }

    final ControlSet popupControls = tablePopupControls == null ? new ControlSet() : tablePopupControls;
    if (searchPanel instanceof EntityTableSearchPanel) {
      ((EntityTableSearchPanel)searchPanel).evtAdvancedChanged.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (isSearchPanelVisible()) {
            revalidateAndShowSearchPanel();
          }
        }
      });
      if (allowQueryConfiguration) {
        final ControlSet searchControls = ((EntityTableSearchPanel)searchPanel).getControls();
        searchControls.addAt(ControlFactory.toggleControl(this, "searchPanelVisible",
                FrameworkMessages.get(FrameworkMessages.SHOW), evtSearchPanelVisibleChanged), 0);
        popupControls.addSeparatorAt(0);
        popupControls.addAt(getClearControl(), 0);
        popupControls.addAt(getRefreshControl(), 0);
        popupControls.add(searchControls);
      }
    }
    popupControls.addSeparator();
    popupControls.add(new ControlSet(Messages.get(Messages.COPY), getCopyCellControl(), getCopyTableWithHeaderControl()));
    UiUtil.setTablePopup(entityTable, ControlProvider.createPopupMenu(popupControls));

    base.add(tableScrollPane, BorderLayout.CENTER);
    add(base, BorderLayout.CENTER);
    final JPanel tableSummaryPanel = initializeSummaryPanel();
    if (tableSummaryPanel != null) {
      summaryScrollPane = new JScrollPane(tableSummaryPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
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
      summaryScrollPaneBase = new JPanel(new BorderLayout());
      base.add(summaryScrollPaneBase, BorderLayout.SOUTH);
    }

    final JPanel southPanel = initializeSouthPanel(allowQueryConfiguration);
    if (southPanel != null) {
      final JPanel southBase = new JPanel(new BorderLayout(5,5));
      southBase.setBorder(BorderFactory.createEtchedBorder());
      final JToolBar refreshButton = getRefreshToolbar();
      if (refreshButton != null)
        southBase.add(refreshButton, BorderLayout.WEST);
      southBase.add(southPanel, BorderLayout.CENTER);
      add(southBase, BorderLayout.SOUTH);
    }
    setSearchPanelVisible(false);

    entityTable.repaint();
  }

  /**
   * Initializes the summary panel
   * @return the summary panel
   */
  protected JPanel initializeSummaryPanel() {
    final List<JPanel> panels = new ArrayList<JPanel>();
    final JPanel ret = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
    for (final Property property : EntityRepository.get().getVisiblePropertyList(getTableModel().getEntityID())) {
      final PropertySummaryPanel panel = initializeSummaryPanel(property);
      propertySummaryPanels.put(property.propertyID, panel);
      panels.add(panel);
      ret.add(panel);
    }

    UiUtil.bindColumnAndPanelSizes(getJTable().getColumnModel(), panels);

    return ret;
  }

  /**
   * Initializes a PropertySummaryPanel for the given property
   * @param property the property for which to create a summary panel
   * @return a PropertySummaryPanel for the given property
   */
  protected PropertySummaryPanel initializeSummaryPanel(final Property property) {
    return new PropertySummaryPanel(property, getTableModel());
  }

  /**
   * @return a Control for refreshing the underlying data
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
   * @return an initialized search panel
   */
  protected JPanel initializeSearchPanel() {
    return getTableModel().getSearchModel().stSimpleSearch.isActive() ? initializeSimpleSearchPanel() : initializeAdvancedSearchPanel();
  }

  /**
   * Initializes a simple search panel, with a single search field, which performes a search based on the default
   * search properties or if none are defined all string based properties
   * @return a simple search panel
   * @see org.jminor.framework.model.EntityRepository#setEntitySearchProperties(String, String[])
   */
  protected JPanel initializeSimpleSearchPanel() {
    final List<Property> searchableProperties = new ArrayList<Property>();
    final String[] defaultSearchProperties = EntityRepository.get().getEntitySearchPropertyIDs(getTableModel().getEntityID());
    if (defaultSearchProperties != null) {
      for (final String propertyID : defaultSearchProperties)
        searchableProperties.add(EntityRepository.get().getProperty(getTableModel().getEntityID(), propertyID));
    }
    else {
      for (final Property property : EntityRepository.get().getDatabaseProperties(getTableModel().getEntityID())) {
        if (property.propertyType == Type.STRING)
          searchableProperties.add(property);
      }
    }
    if (searchableProperties.size() == 0)
      throw new RuntimeException("Unable to create a simple search panel for entity: "
              + getTableModel().getEntityID() + ", no STRING based properties found");

    final JTextField txtField = new JTextField();
    final Action action = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(final ActionEvent e) {
        try {
          getTableModel().getSearchModel().clearPropertySearchModels();
          if (txtField.getText().length() > 0) {
            final String wildcard = (String) FrameworkSettings.get().getProperty(FrameworkSettings.WILDCARD_CHARACTER);
            final String searchText = wildcard + txtField.getText() + wildcard;
            for (final Property searchProperty : searchableProperties)
              getTableModel().getSearchModel().setStringSearchValue(searchProperty.propertyID, searchText);
          }

          getTableModel().refresh();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    };

    txtField.addActionListener(action);
    final JPanel searchPanel = new JPanel(new BorderLayout(5,5));
    searchPanel.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.CONDITION)));
    searchPanel.add(txtField, BorderLayout.CENTER);
    searchPanel.add(new JButton(action), BorderLayout.EAST);

    return searchPanel;
  }

  /**
   * @return an initialized EntityTableSearchPanel
   */
  protected JPanel initializeAdvancedSearchPanel() {
    final EntityTableSearchPanel searchPanel =
            new EntityTableSearchPanel(getTableModel().getSearchModel(), getTableModel().getTableColumnProperties());
    searchPanel.bindToColumnSizes(getJTable());

    return searchPanel;
  }

  /**
   * @return the Action performed when the table receives a double click
   */
  protected Action getDoubleClickAction() {
    return this.doubleClickAction;
  }

  /**
   * @return the refresh toolbar
   */
  protected JToolBar getRefreshToolbar() {
    final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    final String keyName = stroke.toString().replace("pressed ", "");
    final Control refresh = ControlFactory.methodControl(getTableModel(), "refresh", null,
            getTableModel().getSearchModel().stSearchStateChanged, FrameworkMessages.get(FrameworkMessages.REFRESH_TIP)
            + " (" + keyName + ")", 0, null, Images.loadImage("Stop16.gif"));

    final InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
    final ActionMap actionMap = getActionMap();

    inputMap.put(stroke, "refreshControl");
    actionMap.put("refreshControl", refresh);

    final AbstractButton button = ControlProvider.createButton(refresh);
    button.setPreferredSize(new Dimension(20,20));
    button.setFocusable(false);
    getTableModel().getSearchModel().stSearchStateChanged.evtSetActive.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        UiUtil.showToolTip(button);
      }
    });

    searchRefreshToolBar = new JToolBar(JToolBar.HORIZONTAL);
    searchRefreshToolBar.setFocusable(false);
    searchRefreshToolBar.setFloatable(false);
    searchRefreshToolBar.setRollover(true);

    searchRefreshToolBar.add(button);

    return searchRefreshToolBar;
  }

  /**
   * Returns the TableCellRenderer used for this EntityTablePanel
   * @param specialRendering if true then the underlying Entity has specific background coloring
   * @return the TableCellRenderer
   */
  protected TableCellRenderer initializeTableCellRenderer(final boolean specialRendering) {
    return new EntityTableCellRenderer(this.tableModel, specialRendering);
  }

  private JTable initializeJTable(final EntityTableModel tableModel, final boolean specialRendering) {
    final JTable ret = new JTable(tableModel.getTableSorter(), initializeTableColumnModel(specialRendering),
            tableModel.getSelectionModel());
    ret.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          final Entity selected = getTableModel().getSelectedEntity();
          if (selected != null) {
            final Action doubleClickAction = getDoubleClickAction();
            if (doubleClickAction != null)
              doubleClickAction.actionPerformed(new ActionEvent(selected, -1, "doubleClick"));
          }
        }
      }
    });

    final JTableHeader header = ret.getTableHeader();
    final TableCellRenderer defaultHeaderRenderer = header.getDefaultRenderer();
    final Font defaultFont = ret.getFont();
    final Font searchFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
    header.setDefaultRenderer(new TableCellRenderer() {
      public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                     final boolean hasFocus, final int row, final int column) {
        final JLabel ret = (JLabel) defaultHeaderRenderer.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);
        ret.setFont(getTableModel().getSearchModel().isSearchEnabled(column) ? searchFont : defaultFont);

        return ret;
      }
    });
    header.setFocusable(false);
    header.setReorderingAllowed(false);

    ret.setColumnSelectionAllowed(false);
    ret.setAutoResizeMode((Integer) FrameworkSettings.get().getProperty(FrameworkSettings.TABLE_AUTO_RESIZE_MODE));
    tableModel.getTableSorter().setTableHeader(header);

    return ret;
  }

  private Control getCopyCellControl() {
    return new Control(FrameworkMessages.get(FrameworkMessages.COPY_CELL),
            getTableModel().stSelectionEmpty.getReversedState()) {
      public void actionPerformed(final ActionEvent e) {
        final JTable table = getJTable();
        final Object value = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn());
        Util.setClipboard(value == null ? "" : value.toString());
      }
    };
  }

  private Control getCopyTableWithHeaderControl() {
    return new Control(FrameworkMessages.get(FrameworkMessages.COPY_TABLE_WITH_HEADER)) {
      public void actionPerformed(final ActionEvent e) {
        try {
          copyTableAsDelimitedString();
        }
        catch (UserCancelException ex) {/**/}
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    };
  }

  private void copyTableAsDelimitedString() throws UserException, UserCancelException {
    final List<String> headerValues = new ArrayList<String>();
    final List<Property> properties = new ArrayList<Property>(getTableModel().getTableColumnProperties());
    final ListIterator<Property> iterator = properties.listIterator();
    //remove hidden columns
    while (iterator.hasNext())
      if (!isPropertyColumnVisible(iterator.next()))
        iterator.remove();
    for (final Property property : properties)
      headerValues.add(property.getCaption());

    final String[][] header = new String[][] {headerValues.toArray(new String[headerValues.size()])};

    final List<Entity> entities = getTableModel().getSelectionModel().isSelectionEmpty()
            ? getTableModel().getAllEntities(false) : getTableModel().getSelectedEntities();

    final String[][] data = new String[entities.size()][];
    for (int i = 0; i < data.length; i++) {
      final ArrayList<String> line = new ArrayList<String>(15);
      for (final Property property : properties)
        line.add(entities.get(i).getValueAsString(property));

      data[i] = line.toArray(new String[line.size()]);
    }
    Util.setClipboard(Util.getDelimitedString(header, data, "\t"));
  }

  private TableColumnModel initializeTableColumnModel(final boolean specialRendering) {
    final TableColumnModel columnModel = new DefaultTableColumnModel();
    final List<TableColumn> columns = getTableColumns(tableModel.getTableColumnProperties());
    for (final TableColumn column : columns) {
      column.setResizable(true);
      column.setCellRenderer(initializeTableCellRenderer(specialRendering));
      columnModel.addColumn(column);
    }

    return columnModel;
  }

  private void updateStatusMessage() {
    if (lblStatusMessage != null) {
      final String status = tableModel.getStatusMessage();
      lblStatusMessage.setText(status);
      lblStatusMessage.setToolTipText(status);
    }
  }

  private List<PropertyFilterPanel> initializeFilterPanels() {
    final List<PropertyFilterPanel> columnFilterPanels =
            new ArrayList<PropertyFilterPanel>(tableModel.getSearchModel().getPropertyFilterModels().size());
    for (final PropertyFilterModel searchModel : tableModel.getSearchModel().getPropertyFilterModels()) {
      final PropertyFilterPanel ret = new PropertyFilterPanel(searchModel, true, true);
      final TableColumn tableColumn = entityTable.getColumnModel().getColumn(searchModel.getColumnIndex());
      ret.getModel().evtSearchStateChanged.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          if (ret.getModel().isSearchEnabled())
            addFilterIndicator(tableColumn);
          else
            removeFilterIndicator(tableColumn);

          entityTable.getTableHeader().repaint();
        }
      });
      if (ret.getModel().isSearchEnabled())
        addFilterIndicator(tableColumn);

      columnFilterPanels.add(ret);
    }

    return columnFilterPanels;
  }

  private void toggleColumnFilterPanel(final ActionEvent e) {
    final Point lp = entityTable.getTableHeader().getLocationOnScreen();
    final Point p = (Point) e.getSource();
    final Point pos = new Point((int) (lp.getX() + p.getX()), (int) (lp.getY() - p.getY()));
    final int col = entityTable.getColumnModel().getColumnIndexAtX((int) p.getX());
    toggleFilterPanel(pos, propertyFilterPanels.get(col), entityTable);
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

  private static List<TableColumn> getTableColumns(final List<Property> columnProperties) {
    final ArrayList<TableColumn> ret = new ArrayList<TableColumn>(columnProperties.size());
    int i = 0;
    for (final Property property : columnProperties) {
      final TableColumn col = new TableColumn(i++);
      col.setIdentifier(property);
      col.setHeaderValue(property.getCaption());
      final Integer prw = property.getPreferredColumnWidth();
      if (prw != null && prw > 0)
        col.setPreferredWidth(prw);
      ret.add(col);
    }

    return ret;
  }
}
