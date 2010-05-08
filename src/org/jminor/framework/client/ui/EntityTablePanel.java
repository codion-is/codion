/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.common.ui.AbstractFilteredTablePanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.ToggleBeanValueLink;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.EntityTableSearchModel;
import org.jminor.framework.client.model.PropertyFilterModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.print.PrinterException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
public class EntityTablePanel extends AbstractFilteredTablePanel {

  public static final char FILTER_INDICATOR = '*';

  private final Event evtTableDoubleClicked = new Event();
  private final Event evtSearchPanelVisibilityChanged = new Event();
  private final Event evtSummaryPanelVisibilityChanged = new Event();

  /**
   * Contains columns that have been hidden
   */
  private final List<TableColumn> hiddenColumns = new ArrayList<TableColumn>();

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
   * The south panel
   */
  private JPanel southPanel;

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
   * @param popupControls a ControlSet on which the table popup menu is based
   */
  public EntityTablePanel(final EntityTableModel tableModel, final ControlSet popupControls) {
    super(tableModel);
    this.searchPanel = initializeSearchPanel();
    this.summaryPanel = initializeSummaryPanel();
    this.propertyFilterPanels = initializeFilterPanels();
    initializeUI(popupControls);
    bindEventsInternal();
    bindEvents();
    updateStatusMessage();
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

  public void initializeSouthPanelToolBar(final ControlSet controls) {
    if (southPanel == null)
      throw new RuntimeException("No south panel available for toolbar");

    final JToolBar southToolBar = ControlProvider.createToolbar(controls, JToolBar.HORIZONTAL);
    for (final Component component : southToolBar.getComponents())
      component.setPreferredSize(new Dimension(20, 20));
    southToolBar.setFocusable(false);
    southToolBar.setFloatable(false);
    southToolBar.setRollover(true);
    southPanel.add(southToolBar, BorderLayout.EAST);
  }

  /**
   * Shows a dialog for selecting which columns to show/hide
   */
  public void selectTableColumns() {
    final Enumeration<TableColumn> columns = getTableModel().getColumnModel().getColumns();
    final List<TableColumn> allColumns = new ArrayList<TableColumn>();
    while (columns.hasMoreElements())
      allColumns.add(columns.nextElement());
    allColumns.addAll(hiddenColumns);
    Collections.sort(allColumns, new Comparator<TableColumn>() {
      public int compare(final TableColumn colOne, final TableColumn colTwo) {
        return Collator.getInstance().compare(colOne.getIdentifier().toString(), colTwo.getIdentifier().toString());
      }
    });

    final JPanel togglePanel = new JPanel(new GridLayout(Math.min(15, allColumns.size()), 0));
    final List<JCheckBox> buttonList = new ArrayList<JCheckBox>();
    for (final TableColumn column : allColumns) {
      final JCheckBox chkColumn = new JCheckBox(column.getHeaderValue().toString(), !hiddenColumns.contains(column));
      buttonList.add(chkColumn);
      togglePanel.add(chkColumn);
    }
    final JScrollPane scroller = new JScrollPane(togglePanel);
    final int result = JOptionPane.showOptionDialog(this, scroller,
            FrameworkMessages.get(FrameworkMessages.SELECT_COLUMNS), JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, null, null);
    if (result == JOptionPane.OK_OPTION) {
      for (final JCheckBox chkButton : buttonList) {
        final TableColumn column = allColumns.get(buttonList.indexOf(chkButton));
        setPropertyColumnVisible((Property) column.getIdentifier(), chkButton.isSelected());
      }
    }
  }

  /**
   * Toggles the visibility of the column representing the given property
   * Hiding a column removes it from the query criteria, by disabling the underlying search model (PropertySearchModel)
   * @param property the property
   * @param visible if true the column is shown, otherwise it is hidden
   */
  public void setPropertyColumnVisible(final Property property, final boolean visible) {
    if (getJTable() == null)
      return;

    if (visible) {
      if (!isPropertyColumnVisible(property)) {
        showColumn(property);
      }
    }
    else {
      if (isPropertyColumnVisible(property)) {
        //disable the search model for the column to be hidden, to prevent confusion
        getTableModel().getSearchModel().setSearchEnabled(property.getPropertyID(), false);
        hideColumn(property);
      }
    }
  }

  /**
   * @return a control for printing the table
   */
  public Control getPrintControl() {
    final String printCaption = FrameworkMessages.get(FrameworkMessages.PRINT_TABLE);
    return ControlFactory.methodControl(this, "printTable", printCaption, null,
            printCaption, printCaption.charAt(0), null, Images.loadImage("Print16.gif"));
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
   * Override to add event bindings
   */
  protected void bindEvents() {}

  /**
   * Initializes the UI
   * @param tablePopupControls the ControlSet on which to base the table's popup menu
   */
  protected void initializeUI(final ControlSet tablePopupControls) {
    final JPanel base = new JPanel(new BorderLayout());
    setLayout(new BorderLayout());
    if (searchPanel != null) {
      searchScrollPane = new JScrollPane(searchPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      if (searchPanel instanceof EntityTableSearchPanel)
        searchScrollPane.getHorizontalScrollBar().setModel(getTableScrollPane().getHorizontalScrollBar().getModel());
      base.add(searchScrollPane, BorderLayout.NORTH);
    }

    final ControlSet popupControls = tablePopupControls == null ? new ControlSet() : tablePopupControls;
    if (searchPanel instanceof EntityTableSearchPanel) {
      ((EntityTableSearchPanel)searchPanel).eventAdvancedChanged().addListener(new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          if (isSearchPanelVisible()) {
            revalidateAndShowSearchPanel();
          }
        }
      });
      if (getTableModel().isQueryConfigurationAllowed()) {
        final ControlSet searchControls = ((EntityTableSearchPanel)searchPanel).getControls();
        searchControls.addAt(ControlFactory.toggleControl(this, "searchPanelVisible",
                FrameworkMessages.get(FrameworkMessages.SHOW), evtSearchPanelVisibilityChanged), 0);
        popupControls.addSeparatorAt(0);
        popupControls.addAt(getClearControl(), 0);
        popupControls.addAt(getRefreshControl(), 0);
        popupControls.add(searchControls);
      }
    }
    if (popupControls.size() > 0)
      popupControls.addSeparator();
    popupControls.add(new ControlSet(Messages.get(Messages.COPY), getCopyCellControl(), getCopyTableWithHeaderControl()));
    setTablePopupMenu(getJTable(), popupControls);

    final JScrollPane tableScrollPane = getTableScrollPane();
    base.add(tableScrollPane, BorderLayout.CENTER);
    add(base, BorderLayout.CENTER);
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
      base.add(summaryBasePanel, BorderLayout.SOUTH);
    }

    southPanel = initializeSouthPanel();
    if (southPanel != null)
      add(southPanel, BorderLayout.SOUTH);

    setSearchPanelVisible(Configuration.getBooleanValue(Configuration.DEFAULT_SEARCH_PANEL_STATE));
  }

  /**
   * Initializes the south panel
   * @return the south panel
   */
  protected JPanel initializeSouthPanel() {
    statusMessageLabel = new JLabel("", JLabel.CENTER);
    if (getTableModel().isQueryConfigurationAllowed()) {
      statusMessageLabel.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent event) {
          if (event.getClickCount() == 2) {
            configureQuery();
          }
        }
      });
    }
    statusMessageLabel.setFont(new Font(statusMessageLabel.getFont().getName(), Font.PLAIN, 12));

    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(statusMessageLabel, BorderLayout.CENTER);
    panel.setBorder(BorderFactory.createEtchedBorder());
    refreshToolBar = initializeRefreshToolbar();
    if (refreshToolBar != null)
      panel.add(refreshToolBar, BorderLayout.WEST);

    return panel;
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
    UiUtil.addKeyEvent(table, KeyEvent.VK_G, JComponent.WHEN_FOCUSED,
            KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, new AbstractAction("showPopupMenu") {
      public void actionPerformed(ActionEvent event) {
        popupMenu.show(table, 100, table.getSelectedRow() * table.getRowHeight());
      }
    });
    UiUtil.addKeyEvent(table, KeyEvent.VK_V, JComponent.WHEN_FOCUSED,
            KeyEvent.CTRL_DOWN_MASK, new AbstractAction("showEntityMenu") {
      public void actionPerformed(ActionEvent event) {
        showEntityMenu(new Point(100, table.getSelectedRow() * table.getRowHeight()));
      }
    });
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
   * @return the Action performed when the table receives a double click
   */
  protected Action getTableDoubleClickAction() {
    return tableDoubleClickAction;
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

    table.setColumnSelectionAllowed(false);
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
      if (!isPropertyColumnVisible(iterator.next()))
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

      propertyFilterPanels.put(model.getPropertyID(), new PropertyFilterPanel(model, true, true));
    }

    return propertyFilterPanels;
  }

  private void bindEventsInternal() {
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
          getJTable().scrollRectToVisible(getJTable().getCellRect(
                  getTableModel().getSelectedIndex(), getJTable().getSelectedColumn(), true));
      }
    });

    getTableModel().getSearchModel().stateSearchStateChanged().eventStateChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        getJTable().getTableHeader().repaint();
        getJTable().repaint();
      }
    });
  }

  private void showColumn(final Property property) {
    final ListIterator<TableColumn> hiddenColumnIterator = hiddenColumns.listIterator();
    while (hiddenColumnIterator.hasNext()) {
      final TableColumn hiddenColumn = hiddenColumnIterator.next();
      if (hiddenColumn.getIdentifier().equals(property)) {
        hiddenColumnIterator.remove();
        getTableModel().getColumnModel().addColumn(hiddenColumn);
      }
    }
  }

  private void hideColumn(final Property property) {
    final TableColumn column = getTableModel().getTableColumn(property);
    getTableModel().getColumnModel().removeColumn(column);
    hiddenColumns.add(column);
  }

  /**
   * @param property the property for which to query if its column is visible or hidden
   * @return true if the column is visible, false if it is hidden
   */
  private boolean isPropertyColumnVisible(final Property property) {
    for (final TableColumn column : hiddenColumns) {
      if (column.getIdentifier().equals(property))
        return false;
    }

    return true;
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
        Entity referencedEntity = entity.getEntityValue(property.getPropertyID());
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
        populateEntityMenu(foreignKeyMenu, entity.getEntityValue(property.getPropertyID()), dbProvider);
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
