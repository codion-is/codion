/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertyFilterModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
import java.awt.event.MouseListener;
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
  public final Event evtTableDoubleClick = new Event();

  /**
   * fired when the search panel state is changed
   */
  public final Event evtSearchPanelVisibleChanged = new Event();

  /**
   * fired when the summary panel state is changed
   */
  public final Event evtTableSummaryPanelVisibleChanged = new Event();

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
  private final JPanel searchPanel;

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
  private Action tableDoubleClickAction;

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
   * @param rowColoring true if each row should be colored according to the underlying entity
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet popupControls,
                          final boolean rowColoring) {
    this(tableModel, popupControls, rowColoring, true);
  }

  /**
   * Initializes a new EntityTablePanel instance
   * @param tableModel the EntityTableModel instance
   * @param popupControls a ControlSet on which the table popup menu is based
   * @param rowColoring true if each row should be colored according to the underlying entity
   * @param allowQueryConfiguration true if the underlying query should be configurable
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet popupControls,
                          final boolean rowColoring, final boolean allowQueryConfiguration) {
    if (tableModel == null)
      throw new IllegalArgumentException("EntityTablePanel can not be constructed without a EntityTableModel instance");
    this.tableModel = tableModel;
    this.entityTable = initializeJTable(rowColoring);
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
    getJTable().getColumn(EntityRepository.getProperty(getTableModel().getEntityID(), propertyID)).setCellRenderer(renderer);
  }

  /**
   * @param doubleClickAction the action to perform when a double click is performed on the table
   */
  public void setTableDoubleClickAction(final Action doubleClickAction) {
    this.tableDoubleClickAction = doubleClickAction;
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
    final AbstractAction action = new AbstractAction(FrameworkMessages.get(FrameworkMessages.APPLY)) {
      public void actionPerformed(ActionEvent event) {
        try {
          getTableModel().refresh();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    };
    action.putValue(Action.MNEMONIC_KEY, FrameworkMessages.get(FrameworkMessages.APPLY_MNEMONIC).charAt(0));
    UiUtil.showInDialog(UiUtil.getParentWindow(this), panel, false,
            FrameworkMessages.get(FrameworkMessages.CONFIGURE_QUERY), true, false, action);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the summary panel
   * @return the PropertySummaryPanel for the given property ID
   */
  public PropertySummaryPanel getSummaryPanel(final String propertyID) {
    return propertySummaryPanels.get(propertyID);
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
    if (visible && isSearchPanelVisible())
      return;

    if (searchScrollPane != null) {
      searchScrollPane.getViewport().setView(visible ? searchPanel : null);
      if (searchRefreshToolBar != null)
        searchRefreshToolBar.setVisible(visible);
      evtSearchPanelVisibleChanged.fire();
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
        Integer preferredColumnWidth = property.getPreferredColumnWidth();
        if (preferredColumnWidth == null || preferredColumnWidth < 0)
          preferredColumnWidth = 80;
        column.setPreferredWidth(preferredColumnWidth);
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
        @Override
        public void mouseReleased(MouseEvent event) {
          if (event.getClickCount() == 2) {
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
    getTableModel().evtRefreshStarted.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        UiUtil.setWaitCursor(true, EntityTablePanel.this);
      }
    });
    getTableModel().evtRefreshDone.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        UiUtil.setWaitCursor(false, EntityTablePanel.this);
      }
    });
    getJTable().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        if(event.getClickCount() == 2){
          evtTableDoubleClick.fire();
        }
      }
    });
    final ActionListener statusListener = new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        updateStatusMessage();
      }
    };
    getTableModel().evtSelectionChanged.addListener(statusListener);
    getTableModel().evtFilteringDone.addListener(statusListener);
    getTableModel().evtTableDataChanged.addListener(statusListener);

    getJTable().getTableHeader().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent event) {
        if (event.isShiftDown())
          toggleColumnFilterPanel(event);
      }
    });

    getTableModel().evtSelectedIndexChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        if (!getTableModel().stSelectionEmpty.isActive())
          getJTable().scrollRectToVisible(getJTable().getCellRect(
                  getTableModel().getSelectedIndex(), getJTable().getSelectedColumn(), true));
      }
    });

    getTableModel().getSearchModel().stSearchStateChanged.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        getJTable().getTableHeader().repaint();
        getJTable().repaint();
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
      searchScrollPane = new JScrollPane(searchPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      if (searchPanel instanceof EntityTableSearchPanel)
        searchScrollPane.getHorizontalScrollBar().setModel(tableScrollPane.getHorizontalScrollBar().getModel());
      base.add(searchScrollPane, BorderLayout.NORTH);
    }

    final ControlSet popupControls = tablePopupControls == null ? new ControlSet() : tablePopupControls;
    if (searchPanel instanceof EntityTableSearchPanel) {
      ((EntityTableSearchPanel)searchPanel).evtAdvancedChanged.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent event) {
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
    addJTablePopupMenu(getJTable(), popupControls);

    base.add(tableScrollPane, BorderLayout.CENTER);
    add(base, BorderLayout.CENTER);
    final JPanel tableSummaryPanel = initializeSummaryPanel();
    if (tableSummaryPanel != null) {
      summaryScrollPane = new JScrollPane(tableSummaryPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
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
    setSearchPanelVisible((Boolean) Configuration.getValue(Configuration.INITIAL_SEARCH_PANEL_STATE));
  }

  /**
   * Adds a popup menu to <code>table</code>
   * @param table the table
   * @param popupControls a ControlSet specifying the controls in the popup menu
   */
  protected void addJTablePopupMenu(final JTable table, final ControlSet popupControls) {
    final JPopupMenu popupMenu = ControlProvider.createPopupMenu(popupControls);
    table.setComponentPopupMenu(popupMenu);
    table.getTableHeader().setComponentPopupMenu(popupMenu);
    table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_G,
                KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, true), "showPopupMenu");
    table.getActionMap().put("showPopupMenu", new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        popupMenu.show(table, 100, table.getSelectedRow() * table.getRowHeight());
      }
    });
  }

  /**
   * Initializes the summary panel
   * @return the summary panel
   */
  protected JPanel initializeSummaryPanel() {
    final List<JPanel> panels = new ArrayList<JPanel>();
    final JPanel ret = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
    final Enumeration<TableColumn> columns = getJTable().getColumnModel().getColumns();
    while (columns.hasMoreElements()) {
      final Property property = (Property) columns.nextElement().getIdentifier();
      final PropertySummaryPanel panel = initializeSummaryPanel(property);
      propertySummaryPanels.put(property.propertyID, panel);
      panels.add(panel);
      ret.add(panel);
    }
    UiUtil.bindColumnAndPanelSizes(getJTable().getColumnModel(), panels);

    final JLabel scrollBarBuffer = new JLabel();
    scrollBarBuffer.setPreferredSize(new Dimension(15,20));
    ret.add(scrollBarBuffer);

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
   * @return an initialized search panel
   */
  protected JPanel initializeSearchPanel() {
    return getTableModel().getSearchModel().stSimpleSearch.isActive() ? initializeSimpleSearchPanel() : initializeAdvancedSearchPanel();
  }

  /**
   * Initializes a simple search panel, with a single search field, which performes a search based on the default
   * search properties or if none are defined all string based properties
   * @return a simple search panel
   * @see org.jminor.framework.domain.EntityRepository#setEntitySearchProperties(String, String[])
   */
  protected JPanel initializeSimpleSearchPanel() {
    final List<Property> searchableProperties = new ArrayList<Property>();
    final String[] defaultSearchProperties = EntityRepository.getEntitySearchPropertyIDs(getTableModel().getEntityID());
    if (defaultSearchProperties != null) {
      for (final String propertyID : defaultSearchProperties)
        searchableProperties.add(EntityRepository.getProperty(getTableModel().getEntityID(), propertyID));
    }
    else {
      for (final Property property : EntityRepository.getDatabaseProperties(getTableModel().getEntityID())) {
        if (property.propertyType == Type.STRING)
          searchableProperties.add(property);
      }
    }
    if (searchableProperties.size() == 0)
      throw new RuntimeException("Unable to create a simple search panel for entity: "
              + getTableModel().getEntityID() + ", no STRING based properties found");

    final JTextField txtField = new JTextField();
    final Action action = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(final ActionEvent event) {
        try {
          getTableModel().getSearchModel().clearPropertySearchModels();
          if (txtField.getText().length() > 0) {
            final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
            final String searchText = wildcard + txtField.getText() + wildcard;
            for (final Property searchProperty : searchableProperties) {
              final PropertySearchModel searchModel = getTableModel().getSearchModel().getPropertySearchModel(searchProperty.propertyID);
              searchModel.setCaseSensitive(false);
              searchModel.setUpperBound(searchText);
              searchModel.setSearchType(SearchType.LIKE);
              searchModel.setSearchEnabled(true);
            }
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
    final EntityTableSearchPanel searchPanel = new EntityTableSearchPanel(getTableModel().getSearchModel(),
            getTableModel().getTableColumnProperties());
    searchPanel.bindToColumnSizes(getJTable());

    final JLabel scrollBarBuffer = new JLabel();
    scrollBarBuffer.setPreferredSize(new Dimension(15,20));
    searchPanel.add(scrollBarBuffer);

    return searchPanel;
  }

  /**
   * @return the Action performed when the table receives a double click
   */
  protected Action getTableDoubleClickAction() {
    return tableDoubleClickAction;
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
      public void actionPerformed(final ActionEvent event) {
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
   * @param rowColoring if true then the underlying Entity has specific background coloring
   * @return the TableCellRenderer
   */
  protected TableCellRenderer initializeTableCellRenderer(final boolean rowColoring) {
    return new EntityTableCellRenderer(getTableModel(), rowColoring);
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
      }
    };
  }

  /**
   * Initializes the JTable instance
   * @param rowColoring if true then the JTable should paint each row according to the underlying entity
   * @return the JTable instance
   * @see org.jminor.framework.domain.EntityProxy#getBackgroundColor(org.jminor.framework.domain.Entity)
   */
  protected JTable initializeJTable(final boolean rowColoring) {
    final JTable table = new JTable(getTableModel().getTableSorter(), initializeTableColumnModel(rowColoring),
            getTableModel().getSelectionModel());
    table.addMouseListener(initializeTableMouseListener());

    final JTableHeader header = table.getTableHeader();
    final TableCellRenderer defaultHeaderRenderer = header.getDefaultRenderer();
    final Font defaultFont = table.getFont();
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

    table.setColumnSelectionAllowed(false);
    table.setAutoResizeMode((Integer) Configuration.getValue(Configuration.TABLE_AUTO_RESIZE_MODE));
    getTableModel().getTableSorter().setTableHeader(header);

    return table;
  }

  private Control getCopyCellControl() {
    return new Control(FrameworkMessages.get(FrameworkMessages.COPY_CELL),
            getTableModel().stSelectionEmpty.getReversedState()) {
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

  private TableColumnModel initializeTableColumnModel(final boolean rowColoring) {
    final TableColumnModel columnModel = new DefaultTableColumnModel();
    final List<TableColumn> columns = getTableColumns(getTableModel().getTableColumnProperties());
    for (final TableColumn column : columns) {
      column.setResizable(true);
      column.setCellRenderer(initializeTableCellRenderer(rowColoring));
      columnModel.addColumn(column);
    }

    return columnModel;
  }

  private void updateStatusMessage() {
    if (lblStatusMessage != null) {
      final String status = getTableModel().getStatusMessage();
      lblStatusMessage.setText(status);
      lblStatusMessage.setToolTipText(status);
    }
  }

  private List<PropertyFilterPanel> initializeFilterPanels() {
    final List<PropertyFilterPanel> columnFilterPanels =
            new ArrayList<PropertyFilterPanel>(getTableModel().getSearchModel().getPropertyFilterModels().size());
    for (final PropertyFilterModel searchModel : getTableModel().getSearchModel().getPropertyFilterModels()) {
      final PropertyFilterPanel ret = new PropertyFilterPanel(searchModel, true, true);
      final TableColumn tableColumn = getJTable().getColumnModel().getColumn(searchModel.getColumnIndex());
      ret.getModel().evtSearchStateChanged.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent event) {
          if (ret.getModel().isSearchEnabled())
            addFilterIndicator(tableColumn);
          else
            removeFilterIndicator(tableColumn);

          getJTable().getTableHeader().repaint();
        }
      });
      if (ret.getModel().isSearchEnabled())
        addFilterIndicator(tableColumn);

      columnFilterPanels.add(ret);
    }

    return columnFilterPanels;
  }

  private void toggleColumnFilterPanel(final MouseEvent event) {
    toggleFilterPanel(event.getLocationOnScreen(),
            propertyFilterPanels.get(getJTable().getColumnModel().getColumnIndexAtX(event.getX())), getJTable());
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
      final TableColumn column = new TableColumn(i++);
      column.setIdentifier(property);
      column.setHeaderValue(property.getCaption());
      final Integer preferredColumnWidth = property.getPreferredColumnWidth();
      if (preferredColumnWidth != null && preferredColumnWidth > 0)
        column.setPreferredWidth(preferredColumnWidth);
      ret.add(column);
    }

    return ret;
  }
}
