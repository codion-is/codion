/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.RowColumn;
import is.codion.common.model.table.SortingDirective;
import is.codion.common.model.table.TableSortModel;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;
import is.codion.swing.common.model.textfield.DocumentAdapter;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.textfield.TextFields;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.KeyEvents.KeyTrigger.ON_KEY_PRESSED;
import static is.codion.swing.common.ui.KeyEvents.addKeyEvent;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A JTable implementation for {@link AbstractFilteredTableModel}.
 * Note that for the table header to display you must add this table to a JScrollPane.
 * @param <R> the type representing rows
 * @param <C> the type used to identify columns
 * @param <T> the table model type
 */
public final class FilteredTable<R, C, T extends AbstractFilteredTableModel<R, C>> extends JTable {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(FilteredTable.class.getName());

  public static final char FILTER_INDICATOR = '*';

  /**
   * Specifies whether to center the scrolled to row and or column.
   */
  public enum CenterOnScroll {
    /**
     * Centers the selected column, if possible.
     */
    COLUMN,
    /**
     * Centers the selected row, if possible.
     */
    ROW,
    /**
     * Centers both the selected column and row, if possible.
     */
    BOTH,
    /**
     * Centers neither the selected column or row.
     */
    NEITHER
  }

  private static final String SELECT_COLUMNS = "select_columns";
  private static final String SINGLE_SELECTION_MODE = "single_selection_mode";
  private static final int SELECT_COLUMNS_GRID_ROWS = 15;
  private static final int SEARCH_FIELD_COLUMNS = 8;
  private static final int SORT_ICON_SIZE = 5;
  private static final int COLUMN_RESIZE_AMOUNT = 10;
  private static final List<Integer> RESIZE_KEYS = asList(KeyEvent.VK_PLUS, KeyEvent.VK_ADD, KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT);
  private static final RowColumn NULL_COORDINATE = RowColumn.rowColumn(-1, -1);

  /**
   * The table model
   */
  private final T tableModel;

  /**
   * Provides filter panels
   */
  private final ConditionPanelFactory<R, C, ?> conditionPanelFactory;

  /**
   * the property filter panels
   */
  private final Map<TableColumn, ColumnConditionPanel<R, C, ?>> columnFilterPanels = new HashMap<>();

  /**
   * The text field used for entering the search condition
   */
  private final JTextField searchField;

  /**
   * Fired each time the table is double clicked
   */
  private final Event<MouseEvent> doubleClickedEvent = Event.event();

  /**
   * the action performed when the table is double clicked
   */
  private Action doubleClickAction;

  /**
   * If true then sorting via the table header is enabled
   */
  private boolean sortingEnabled = true;

  /**
   * If true then this table scrolls to the item selected in the table model
   */
  private boolean scrollToSelectedItem = true;

  /**
   * The coordinate of the last search result
   */
  private RowColumn lastSearchResultCoordinate = NULL_COORDINATE;

  /**
   * Instantiates a new FilteredTable using the given model
   * @param tableModel the table model
   */
  public FilteredTable(final T tableModel) {
    this(tableModel, column -> new ColumnConditionPanel<>(tableModel.getColumnModel().getColumnFilterModel(
            (C) column.getIdentifier()), ColumnConditionPanel.ToggleAdvancedButton.YES));
  }

  /**
   * Instantiates a new FilteredTable using the given model
   * @param tableModel the table model
   * @param conditionPanelFactory the column condition panel factory
   */
  public FilteredTable(final T tableModel, final ConditionPanelFactory<R, C, ?> conditionPanelFactory) {
    super(requireNonNull(tableModel, "tableModel"), tableModel.getColumnModel(), tableModel.getSelectionModel());
    this.tableModel = tableModel;
    this.conditionPanelFactory = requireNonNull(conditionPanelFactory, "conditionPanelFactory");
    this.searchField = initializeSearchField();
    initializeTableHeader();
    bindEvents();
  }

  @Override
  public T getModel() {
    return (T) super.getModel();
  }

  @Override
  public void setModel(final TableModel dataModel) {
    if (this.tableModel != null) {
      throw new IllegalStateException("Table model has already been set");
    }
    if (!(dataModel instanceof AbstractFilteredTableModel)) {
      throw new IllegalArgumentException("FilteredTable model must be a AbstractFilteredTableModel instance");
    }
    final List<R> selection = ((AbstractFilteredTableModel<R, C>) dataModel).getSelectionModel().getSelectedItems();
    super.setModel(dataModel);
    if (!selection.isEmpty()) {
      ((AbstractFilteredTableModel<R, C>) dataModel).getSelectionModel().setSelectedItems(selection);
    }
  }

  /**
   * @return the search field
   */
  public JTextField getSearchField() {
    return searchField;
  }

  /**
   * @param doubleClickAction the action to perform when a double click is performed on the table,
   * null for no double click action
   */
  public void setDoubleClickAction(final Action doubleClickAction) {
    this.doubleClickAction = doubleClickAction;
  }

  /**
   * @return the Action performed when the table receives a double click
   */
  public Action getDoubleClickAction() {
    return doubleClickAction;
  }

  /**
   * @return true if sorting via the table header is enabled
   */
  public boolean isSortingEnabled() {
    return sortingEnabled;
  }

  /**
   * @param sortingEnabled true if sorting via the table header should be enabled
   */
  public void setSortingEnabled(final boolean sortingEnabled) {
    this.sortingEnabled = sortingEnabled;
  }

  /**
   * @return true if the JTable instance scrolls automatically to the coordinate
   * of the record selected in the underlying table model
   */
  public boolean isScrollToSelectedItem() {
    return scrollToSelectedItem;
  }

  /**
   * @param scrollToSelectedItem true if the JTable instance should scroll automatically
   * to the coordinate of the record selected in the underlying table model
   */
  public void setScrollToSelectedItem(final boolean scrollToSelectedItem) {
    this.scrollToSelectedItem = scrollToSelectedItem;
  }

  @Override
  public void setSelectionMode(final int selectionMode) {
    tableModel.getSelectionModel().setSelectionMode(selectionMode);
  }

  /**
   * Hides or shows the active filter panels for this table panel
   * @param filterPanelsVisible true if the active filter panels should be shown, false if they should be hidden
   */
  public void setFilterPanelsVisible(final boolean filterPanelsVisible) {
    columnFilterPanels.values().forEach(columnFilterPanel -> SwingUtilities.invokeLater(() -> {
      if (filterPanelsVisible) {
        columnFilterPanel.showDialog();
      }
      else {
        columnFilterPanel.hideDialog();
      }
    }));
  }

  /**
   * Shows a dialog for selecting which columns to show/hide
   */
  public void selectColumns() {
    final SwingFilteredTableColumnModel<R, C> columnModel = tableModel.getColumnModel();
    final List<TableColumn> allColumns = new ArrayList<>(columnModel.getAllColumns());
    allColumns.sort(new Comparator<TableColumn>() {
      private final Collator collator = Collator.getInstance();

      @Override
      public int compare(final TableColumn o1, final TableColumn o2) {
        return Text.collateSansSpaces(collator, o1.getIdentifier().toString(), o2.getIdentifier().toString());
      }
    });
    final List<JCheckBox> checkBoxes = new ArrayList<>();
    final int result = JOptionPane.showOptionDialog(this, initializeSelectColumnsPanel(allColumns, checkBoxes),
            MESSAGES.getString(SELECT_COLUMNS), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            new String[] {MESSAGES.getString("show_all_columns"), Messages.get(Messages.CANCEL),
                    Messages.get(Messages.OK)}, Messages.get(Messages.OK));
    if (result != 1) {
      if (result == 0) {
        setSelected(checkBoxes, true);
      }
      checkBoxes.forEach(checkBox -> SwingUtilities.invokeLater(() -> {
        final TableColumn column = allColumns.get(checkBoxes.indexOf(checkBox));
        if (checkBox.isSelected()) {
          columnModel.showColumn((C) column.getIdentifier());
        }
        else {
          columnModel.hideColumn((C) column.getIdentifier());
        }
      }));
    }
  }

  /**
   * Returns true if the given cell is visible.
   * @param row the row
   * @param column the column
   * @return true if this table is contained in a scrollpanel and the cell with the given coordinates is visible.
   */
  public boolean isCellVisible(final int row, final int column) {
    final JViewport viewport = Components.getParentOfType(this, JViewport.class);
    if (viewport == null) {
      return false;
    }
    final Rectangle cellRect = getCellRect(row, column, true);
    final Point viewPosition = viewport.getViewPosition();
    cellRect.setLocation(cellRect.x - viewPosition.x, cellRect.y - viewPosition.y);

    return new Rectangle(viewport.getExtentSize()).contains(cellRect);
  }

  /**
   * Scrolls horizontally so that the column identified by columnIdentifier becomes visible, centered if possible.
   * Has no effect if this table is not contained in a scrollpanel.
   * @param columnIdentifier the column identifier
   */
  public void scrollToColumn(final Object columnIdentifier) {
    final JViewport viewport = Components.getParentOfType(this, JViewport.class);
    if (viewport != null) {
      scrollToCoordinate(rowAtPoint(viewport.getViewPosition()),
              getModel().getColumnModel().getColumnIndex(columnIdentifier), CenterOnScroll.NEITHER);
    }
  }

  /**
   * Scrolls to the given coordinate. Has no effect if this table is not contained in a scrollpanel.
   * @param row the row
   * @param column the column
   * @param centerOnScroll specifies whether to center the selected row and or column
   */
  public void scrollToCoordinate(final int row, final int column, final CenterOnScroll centerOnScroll) {
    final JViewport viewport = Components.getParentOfType(this, JViewport.class);
    if (viewport != null) {
      final Rectangle cellRectangle = getCellRect(row, column, true);
      final Rectangle viewRectangle = viewport.getViewRect();
      cellRectangle.setLocation(cellRectangle.x - viewRectangle.x, cellRectangle.y - viewRectangle.y);
      if (centerOnScroll == CenterOnScroll.COLUMN || centerOnScroll == CenterOnScroll.BOTH) {
        int centerX = (viewRectangle.width - cellRectangle.width) / 2;
        if (cellRectangle.x < centerX) {
          centerX = -centerX;
        }
        cellRectangle.translate(centerX, cellRectangle.y);
      }
      if (centerOnScroll == CenterOnScroll.ROW || centerOnScroll == CenterOnScroll.BOTH) {
        int centerY = (viewRectangle.height - cellRectangle.height) / 2;
        if (cellRectangle.y < centerY) {
          centerY = -centerY;
        }
        cellRectangle.translate(cellRectangle.x, centerY);
      }
      viewport.scrollRectToVisible(cellRectangle);
    }
  }

  /**
   * @return a control for showing the column selection dialog
   */
  public Control createSelectColumnsControl() {
    return Control.builder()
            .command(this::selectColumns)
            .name(MESSAGES.getString(SELECT_COLUMNS) + "...")
            .enabledState(tableModel.getColumnModel().getLockedState().getReversedObserver())
            .description(MESSAGES.getString(SELECT_COLUMNS))
            .build();
  }

  /**
   * @return a ToggleControl for toggling the table selection mode (single or multiple)
   */
  public ToggleControl createSingleSelectionModeControl() {
    return ToggleControl.builder()
            .state(tableModel.getSelectionModel().getSingleSelectionModeState())
            .name(MESSAGES.getString(SINGLE_SELECTION_MODE))
            .build();
  }

  /**
   * Performs a text search in the underlying table model, forward relative to the last search result coordinate.
   * @param searchText the text to search for
   */
  public void findNext(final String searchText) {
    performSearch(false, lastSearchResultCoordinate.getRow() + 1, true, searchText);
  }

  /**
   * Performs a text search in the underlying table model, backwards relative to the last search result coordinate.
   * @param searchText the text to search for
   */
  public void findPrevious(final String searchText) {
    performSearch(false, lastSearchResultCoordinate.getRow() - 1, false, searchText);
  }

  /**
   * Performs a text search in the underlying table model, forward relative to the last search result coordinate,
   * adding the result to the current row selection.
   * @param searchText the text to search for
   */
  public void findAndSelectNext(final String searchText) {
    performSearch(true, lastSearchResultCoordinate.getRow() + 1, true, searchText);
  }

  /**
   * Performs a text search in the underlying table model, backwards relative to the last search result coordinate,
   * adding the result to the current row selection.
   * @param searchText the text to search for
   */
  public void findAndSelectPrevious(final String searchText) {
    performSearch(true, lastSearchResultCoordinate.getRow() - 1, false, searchText);
  }

  /**
   * A convenience method for setting the client property 'JTable.autoStartsEdit'.
   * @param autoStartsEdit the value
   */
  public void setAutoStartsEdit(final boolean autoStartsEdit) {
    putClientProperty("JTable.autoStartsEdit", autoStartsEdit);
  }

  /**
   * @param listener a listener notified each time the table is double clicked
   */
  public void addDoubleClickListener(final EventDataListener<MouseEvent> listener) {
    doubleClickedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeDoubleClickListener(final EventDataListener<MouseEvent> listener) {
    doubleClickedEvent.removeDataListener(listener);
  }

  /**
   * Creates a JTextField for searching through this table.
   * @return a search field
   */
  private JTextField initializeSearchField() {
    final JTextField field = new JTextField();
    field.setBackground((Color) UIManager.getLookAndFeel().getDefaults().get("TextField.inactiveBackground"));
    field.setColumns(SEARCH_FIELD_COLUMNS);
    TextFields.selectAllOnFocusGained(field);
    final TextFields.Hint textFieldHint = TextFields.hint(field, Messages.get(Messages.SEARCH_FIELD_HINT));
    field.getDocument().addDocumentListener((DocumentAdapter) e -> {
      if (!textFieldHint.isHintVisible()) {
        performSearch(false, lastSearchResultCoordinate.getRow() == -1 ? 0 :
                lastSearchResultCoordinate.getRow(), true, field.getText());
      }
    });
    final Control findNext = Control.control(() -> findNext(field.getText()));
    final Control findAndSelectNext = Control.control(() -> findAndSelectNext(field.getText()));
    final Control findPrevious = Control.control(() -> findPrevious(field.getText()));
    final Control findAndSelectPrevious = Control.control(() -> findAndSelectPrevious(field.getText()));
    final Control cancel = Control.control(this::requestFocusInWindow);
    addKeyEvent(field, KeyEvent.VK_ENTER, 0, 0, ON_KEY_PRESSED, findNext);
    addKeyEvent(field, KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, 0, ON_KEY_PRESSED, findAndSelectNext);
    addKeyEvent(field, KeyEvent.VK_DOWN, 0, 0, ON_KEY_PRESSED, findNext);
    addKeyEvent(field, KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK, 0, ON_KEY_PRESSED, findAndSelectNext);
    addKeyEvent(field, KeyEvent.VK_UP, 0, 0, ON_KEY_PRESSED, findPrevious);
    addKeyEvent(field, KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK, 0, ON_KEY_PRESSED, findAndSelectPrevious);
    addKeyEvent(field, KeyEvent.VK_ESCAPE, 0, 0, ON_KEY_PRESSED, cancel);

    field.setComponentPopupMenu(initializeSearchFieldPopupMenu());

    return field;
  }

  private void performSearch(final boolean addToSelection, final int fromIndex, final boolean forward, final String searchText) {
    if (!searchText.isEmpty()) {
      final RowColumn coordinate = forward ? tableModel.findNext(fromIndex, searchText) :
              tableModel.findPrevious(fromIndex, searchText);
      if (coordinate != null) {
        lastSearchResultCoordinate = coordinate;
        if (addToSelection) {
          tableModel.getSelectionModel().addSelectedIndex(coordinate.getRow());
        }
        else {
          tableModel.getSelectionModel().setSelectedIndex(coordinate.getRow());
          setColumnSelectionInterval(coordinate.getColumn(), coordinate.getColumn());
        }
        scrollToCoordinate(coordinate.getRow(), coordinate.getColumn(), CenterOnScroll.NEITHER);
      }
      else {
        tableModel.getSelectionModel().clearSelection();
        lastSearchResultCoordinate = NULL_COORDINATE;
      }
    }
    else {
      lastSearchResultCoordinate = NULL_COORDINATE;
    }
  }

  private JPopupMenu initializeSearchFieldPopupMenu() {
    final JCheckBox boxRegexp = new JCheckBox(MESSAGES.getString("regular_expression_search"), tableModel.isRegularExpressionSearch());
    final JPanel panel = new JPanel(Layouts.gridLayout(1, 1));
    panel.add(boxRegexp);

    final Control control = Control.builder()
            .command(() -> tableModel.setRegularExpressionSearch(boxRegexp.isSelected()))
            .name(Messages.get(Messages.OK))
            .mnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0))
            .build();

    final JPopupMenu popupMenu = new JPopupMenu();
    final String settingsMessage = MESSAGES.getString("settings");
    popupMenu.add(Control.builder()
            .command(() -> Dialogs.displayInDialog(FilteredTable.this, panel, settingsMessage, control))
            .name(settingsMessage)
            .build());

    return popupMenu;
  }

  private JPanel initializeSelectColumnsPanel(final List<TableColumn> allColumns, final List<JCheckBox> checkBoxes) {
    final JPanel togglePanel = new JPanel(new GridLayout(Math.min(SELECT_COLUMNS_GRID_ROWS, allColumns.size()), 0));
    allColumns.forEach(column -> {
      final JCheckBox columnCheckBox = new JCheckBox(column.getHeaderValue().toString(),
              tableModel.getColumnModel().isColumnVisible((C) column.getIdentifier()));
      checkBoxes.add(columnCheckBox);
      togglePanel.add(columnCheckBox);
    });
    final JPanel southPanel = new JPanel(Layouts.flowLayout(FlowLayout.RIGHT));
    southPanel.add(new JButton(Control.builder()
            .command(() -> setSelected(checkBoxes, true))
            .name(MESSAGES.getString("select_all"))
            .build()));
    southPanel.add(new JButton(Control.builder()
            .command(() -> setSelected(checkBoxes, false))
            .name(MESSAGES.getString("select_none"))
            .build()));

    final JPanel base = new JPanel(Layouts.borderLayout());
    base.add(new JScrollPane(togglePanel), BorderLayout.CENTER);
    base.add(southPanel, BorderLayout.SOUTH);

    return base;
  }

  private void bindFilterIndicatorEvents(final TableColumn column) {
    final ColumnConditionModel<R, C, ?> model = getModel().getColumnModel().getColumnFilterModel((C) column.getIdentifier());
    if (model != null) {
      model.addConditionChangedListener(() -> SwingUtilities.invokeLater(() -> {
        if (model.isEnabled()) {
          addFilterIndicator(column);
        }
        else {
          removeFilterIndicator(column);
        }

        getTableHeader().repaint();
      }));
      if (model.isEnabled()) {
        SwingUtilities.invokeLater(() -> addFilterIndicator(column));
      }
    }
  }

  private void toggleColumnFilterPanel(final MouseEvent event) {
    final SwingFilteredTableColumnModel<R, C> columnModel = getModel().getColumnModel();
    final int index = columnModel.getColumnIndexAtX(event.getX());
    final TableColumn column = columnModel.getColumn(index);
    if (!columnFilterPanels.containsKey(column)) {
      columnFilterPanels.put(column, conditionPanelFactory.createConditionPanel(column));
    }

    toggleFilterPanel(event.getLocationOnScreen(), columnFilterPanels.get(column), this);
  }

  private static void toggleFilterPanel(final Point position, final ColumnConditionPanel<?, ?, ?> columnFilterPanel,
                                        final Container parent) {
    if (columnFilterPanel.isDialogEnabled()) {
      columnFilterPanel.disableDialog();
    }
    else {
      columnFilterPanel.enableDialog(parent, position);
    }
  }

  private static void addFilterIndicator(final TableColumn column) {
    String val = (String) column.getHeaderValue();
    if (val.length() != 0 && val.charAt(0) != FILTER_INDICATOR) {
      val = FILTER_INDICATOR + val;
    }

    column.setHeaderValue(val);
  }

  private static void removeFilterIndicator(final TableColumn column) {
    String val = (String) column.getHeaderValue();
    if (val.length() != 0 && val.charAt(0) == FILTER_INDICATOR) {
      val = val.substring(1);
    }

    column.setHeaderValue(val);
  }

  private void initializeTableHeader() {
    getTableHeader().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (e.isAltDown() && e.isControlDown()) {
          toggleColumnFilterPanel(e);
        }
      }
    });
    getTableHeader().setReorderingAllowed(true);
    getTableHeader().addMouseListener(new MouseSortHandler());
    getTableHeader().setDefaultRenderer(new SortableHeaderRenderer(getTableHeader().getDefaultRenderer()));
  }

  private void bindEvents() {
    addMouseListener(initializeTableMouseListener());
    tableModel.getSelectionModel().addSelectedIndexListener(selected -> {
      if (scrollToSelectedItem && !tableModel.getSelectionModel().isSelectionEmpty()) {
        scrollToCoordinate(selected, getSelectedColumn(), CenterOnScroll.NEITHER);
      }
    });
    tableModel.getColumnModel().getAllColumns().forEach(this::bindFilterIndicatorEvents);
    addKeyListener(new MoveResizeColumnKeyListener());
  }

  /**
   * Initialize the MouseListener for the table component handling double click.
   * Double clicking invokes the action returned by {@link #getDoubleClickAction()}
   * with this table as the ActionEvent source
   * @return the MouseListener for the table
   * @see #getDoubleClickAction()
   */
  private MouseListener initializeTableMouseListener() {
    return new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (e.getClickCount() == 2) {
          if (doubleClickAction != null) {
            doubleClickAction.actionPerformed(new ActionEvent(this, -1, "doubleClick"));
          }
          doubleClickedEvent.onEvent(e);
        }
      }
    };
  }

  private static void setSelected(final List<JCheckBox> checkBoxes, final boolean selected) {
    checkBoxes.forEach(box -> SwingUtilities.invokeLater(() -> box.setSelected(selected)));
  }

  private final class SortableHeaderRenderer implements TableCellRenderer {

    private final TableCellRenderer tableCellRenderer;

    private SortableHeaderRenderer(final TableCellRenderer tableCellRenderer) {
      this.tableCellRenderer = tableCellRenderer;
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
      final Component component = tableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (component instanceof JLabel) {
        final JLabel label = (JLabel) component;
        final TableColumn tableColumn = table.getColumnModel().getColumn(column);
        label.setHorizontalTextPosition(JLabel.LEFT);
        label.setIcon(getHeaderRendererIcon((C) tableColumn.getIdentifier(), label.getFont().getSize() + SORT_ICON_SIZE));
      }

      return component;
    }

    private Icon getHeaderRendererIcon(final C columnIdentifier, final int iconSizePixels) {
      final SortingDirective directive = tableModel.getSortModel().getSortingState(columnIdentifier).getDirective();
      if (directive == SortingDirective.UNSORTED) {
        return null;
      }

      return new Arrow(directive == SortingDirective.DESCENDING, iconSizePixels,
              tableModel.getSortModel().getSortingState(columnIdentifier).getPriority());
    }
  }

  private static final class Arrow implements Icon {

    private static final double PRIORITY_SIZE_RATIO = 0.8;
    private static final double PRIORITY_SIZE_CONST = 2.0;
    private static final int ALIGNMENT_CONSTANT = 6;

    private final boolean descending;
    private final int size;
    private final int priority;

    private Arrow(final boolean descending, final int size, final int priority) {
      this.descending = descending;
      this.size = size;
      this.priority = priority;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      final Color color = c == null ? Color.GRAY : c.getBackground();
      // In a compound sort, make each successive triangle 20% smaller than the previous one.
      final int dx = (int) (size / PRIORITY_SIZE_CONST * Math.pow(PRIORITY_SIZE_RATIO, priority));
      final int dy = descending ? dx : -dx;
      // Align icon (roughly) with font baseline.
      final int theY = y + SORT_ICON_SIZE * size / ALIGNMENT_CONSTANT + (descending ? -dy : 0);
      final int shift = descending ? 1 : -1;
      g.translate(x, theY);

      // Right diagonal.
      g.setColor(color.darker());
      g.drawLine(dx / 2, dy, 0, 0);
      g.drawLine(dx / 2, dy + shift, 0, shift);

      // Left diagonal.
      g.setColor(color.brighter());
      g.drawLine(dx / 2, dy, dx, 0);
      g.drawLine(dx / 2, dy + shift, dx, shift);

      // Horizontal line.
      if (descending) {
        g.setColor(color.darker().darker());
      }
      else {
        g.setColor(color.brighter().brighter());
      }
      g.drawLine(dx, 0, 0, 0);

      g.setColor(color);
      g.translate(-x, -theY);
    }

    @Override
    public int getIconWidth() {
      return size;
    }

    @Override
    public int getIconHeight() {
      return size;
    }
  }

  private final class MouseSortHandler extends MouseAdapter {
    @Override
    public void mouseClicked(final MouseEvent e) {
      if (!sortingEnabled || e.getButton() != MouseEvent.BUTTON1 || e.isAltDown()) {
        return;
      }

      final JTableHeader tableHeader = (JTableHeader) e.getSource();
      final TableColumnModel columnModel = tableHeader.getColumnModel();
      final int index = columnModel.getColumnIndexAtX(e.getX());
      if (index >= 0) {
        final C columnIdentifier = (C) columnModel.getColumn(index).getIdentifier();
        final TableSortModel<R, C, TableColumn> sortModel = getModel().getSortModel();
        SortingDirective status = sortModel.getSortingState(columnIdentifier).getDirective();
        final boolean shiftDown = e.isShiftDown();
        switch (status) {
          case UNSORTED:
            if (shiftDown) {
              status = SortingDirective.DESCENDING;
            }
            else {
              status = SortingDirective.ASCENDING;
            }
            break;
          case ASCENDING:
            status = SortingDirective.DESCENDING;
            break;
          default://case DESCENDING:
            status = SortingDirective.ASCENDING;
            break;
        }

        if (e.isControlDown()) {
          sortModel.addSortingDirective(columnIdentifier, status);
        }
        else {
          sortModel.setSortingDirective(columnIdentifier, status);
        }
      }
    }
  }

  private final class MoveResizeColumnKeyListener extends KeyAdapter {

    @Override
    public void keyPressed(final KeyEvent e) {
      if (e.isControlDown() && e.isShiftDown() && (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT)) {
        moveSelectedColumn(e.getKeyCode() == KeyEvent.VK_LEFT);
        e.consume();
      }
      else if (e.isControlDown() && (RESIZE_KEYS.contains(e.getKeyCode()))) {
        resizeSelectedColumn(e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_ADD);
        e.consume();
      }
    }

    private void moveSelectedColumn(final boolean left) {
      final int selectedColumnIndex = getSelectedColumn();
      if (selectedColumnIndex != -1) {
        final int columnCount = getColumnModel().getColumnCount();
        final int newIndex;
        if (left) {
          if (selectedColumnIndex == 0) {
            newIndex = columnCount - 1;
          }
          else {
            newIndex = selectedColumnIndex - 1;
          }
        }
        else {
          if (selectedColumnIndex == columnCount - 1) {
            newIndex = 0;
          }
          else {
            newIndex = selectedColumnIndex + 1;
          }
        }
        moveColumn(selectedColumnIndex, newIndex);
        scrollRectToVisible(getCellRect(getSelectedRow(), newIndex, true));
      }
    }

    private void resizeSelectedColumn(final boolean enlarge) {
      final int selectedColumnIndex = getSelectedColumn();
      if (selectedColumnIndex != -1) {
        final TableColumn column = getColumnModel().getColumn(selectedColumnIndex);
        column.setPreferredWidth(column.getWidth() + (enlarge ? COLUMN_RESIZE_AMOUNT : -COLUMN_RESIZE_AMOUNT));
      }
    }
  }
}
