/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Configuration;
import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.properties.PropertyValue;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableSearchModel;
import is.codion.swing.common.model.component.table.FilteredTableSearchModel.RowColumn;
import is.codion.swing.common.model.component.table.FilteredTableSortModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.columnConditionPanel;
import static is.codion.swing.common.ui.control.Control.control;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A JTable implementation for {@link FilteredTableModel}.
 * Note that for the table header to display you must add this table to a JScrollPane.
 * For instances use the builder {@link #builder(FilteredTableModel)}
 * @param <T> the table model type
 * @param <R> the type representing rows
 * @param <C> the type used to identify columns
 * @see #builder(FilteredTableModel)
 */
public final class FilteredTable<T extends FilteredTableModel<R, C>, R, C> extends JTable {

  /**
   * Specifies the default table column resize mode for tables in the application<br>
   * Value type: Integer (JTable.AUTO_RESIZE_*)<br>
   * Default value: JTable.AUTO_RESIZE_OFF
   */
  public static final PropertyValue<Integer> AUTO_RESIZE_MODE =
          Configuration.integerValue("is.codion.swing.common.ui.component.table.FilteredTable.autoResizeMode", AUTO_RESIZE_OFF);

  /**
   * Specifies whether columns can be rearranged in tables<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> ALLOW_COLUMN_REORDERING =
          Configuration.booleanValue("is.codion.swing.common.ui.component.table.FilteredTable.allowColumnReordering", true);

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(FilteredTable.class.getName());

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
     * Centers neither the selected column nor row.
     */
    NEITHER
  }

  private static final String SELECT = "select";
  private static final String SELECT_COLUMNS = "select_columns";
  private static final String RESET = "reset";
  private static final String RESET_COLUMNS_DESCRIPTION = "reset_columns_description";
  private static final String SINGLE_SELECTION_MODE = "single_selection_mode";
  private static final int SEARCH_FIELD_COLUMNS = 8;
  private static final int SORT_ICON_SIZE = 5;
  private static final int COLUMN_RESIZE_AMOUNT = 10;
  private static final List<Integer> RESIZE_KEYS = asList(VK_PLUS, VK_ADD, VK_MINUS, VK_SUBTRACT);

  /**
   * The table model
   */
  private final T tableModel;

  /**
   * The condition panel factory
   */
  private final ColumnConditionPanel.Factory<C> conditionPanelFactory;

  /**
   * The text field used for entering the search condition
   */
  private final JTextField searchField;

  /**
   * Fired each time the table is double-clicked
   */
  private final Event<MouseEvent> doubleClickedEvent = Event.event();

  /**
   * Holds column identifiers of columns for which sorting should be disabled
   */
  private final Set<C> columnSortingDisabled = new HashSet<>();

  /**
   * The filter condition panel
   */
  private FilteredTableConditionPanel<T, C> conditionPanel;

  /**
   * the action performed when the table is double-clicked
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
   * Specifies the scrolling behaviour when scrolling to the selected row/column
   */
  private CenterOnScroll centerOnScroll = CenterOnScroll.NEITHER;

  private FilteredTable(T tableModel,
                        ColumnConditionPanel.Factory<C> conditionPanelFactory,
                        TableCellRendererFactory<C> cellRendererFactory) {
    super(requireNonNull(tableModel, "tableModel"), tableModel.columnModel(), tableModel.selectionModel());
    this.tableModel = tableModel;
    this.conditionPanelFactory = requireNonNull(conditionPanelFactory);
    this.tableModel.columnModel().columns().forEach(column -> configureColumn(column, requireNonNull(cellRendererFactory)));
    this.searchField = createSearchField();
    initializeTableHeader();
    bindEvents();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(getTableHeader(), searchField, conditionPanel);
  }

  @Override
  public T getModel() {
    return (T) super.getModel();
  }

  @Override
  public void setModel(TableModel dataModel) {
    if (this.tableModel != null) {
      throw new IllegalStateException("Table model has already been set");
    }
    if (!(dataModel instanceof FilteredTableModel)) {
      throw new IllegalArgumentException("FilteredTable model must be a FilteredTableModel instance");
    }
    List<R> selection = ((FilteredTableModel<R, C>) dataModel).selectionModel().getSelectedItems();
    super.setModel(dataModel);
    if (!selection.isEmpty()) {
      ((FilteredTableModel<R, C>) dataModel).selectionModel().setSelectedItems(selection);
    }
  }

  /**
   * @return the condition panel
   */
  public FilteredTableConditionPanel<T, C> conditionPanel() {
    if (conditionPanel == null) {
      conditionPanel = new FilteredTableConditionPanel<>(tableModel, conditionPanelFactory);
    }

    return conditionPanel;
  }

  /**
   * @return the search field
   */
  public JTextField searchField() {
    return searchField;
  }

  /**
   * @param doubleClickAction the action to perform when a double click is performed on the table,
   * null for no double click action
   */
  public void setDoubleClickAction(Action doubleClickAction) {
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
  public void setSortingEnabled(boolean sortingEnabled) {
    this.sortingEnabled = sortingEnabled;
  }

  /**
   * @param columnIdentifier the column identifier
   * @param sortingEnabled true if sorting via the table header should be enabled for the given column
   */
  public void setSortingEnabled(C columnIdentifier, boolean sortingEnabled) {
    requireNonNull(columnIdentifier);
    if (sortingEnabled) {
      columnSortingDisabled.remove(columnIdentifier);
    }
    else {
      columnSortingDisabled.add(columnIdentifier);
    }
  }

  /**
   * @param columnIdentifier the column identifier
   * @return true if sorting via the table header is enabled for the given column
   */
  public boolean isSortingEnabled(C columnIdentifier) {
    return !columnSortingDisabled.contains(requireNonNull(columnIdentifier)) && sortingEnabled;
  }

  /**
   * @return true if the JTable instance scrolls automatically to the coordinate
   * of the record selected in the underlying table model
   */
  public boolean isScrollToSelectedItem() {
    return scrollToSelectedItem;
  }

  /**
   * Specifies whether this table should automatically scroll to the topmost selected row.
   * Note that no scrolling is performed if any of the selected rows are already visible.
   * @param scrollToSelectedItem true if this table should automatically scroll to selected rows
   */
  public void setScrollToSelectedItem(boolean scrollToSelectedItem) {
    this.scrollToSelectedItem = scrollToSelectedItem;
  }

  /**
   * @return the scrolling behaviour when scrolling to the selected row/column
   */
  public CenterOnScroll getCenterOnScroll() {
    return centerOnScroll;
  }

  /**
   * Specifies the scrolling behaviour when scrolling to the selected row/column
   * @param centerOnScroll the scrolling behaviour
   */
  public void setCenterOnScroll(CenterOnScroll centerOnScroll) {
    this.centerOnScroll = requireNonNull(centerOnScroll);
  }

  @Override
  public void setSelectionMode(int selectionMode) {
    tableModel.selectionModel().setSelectionMode(selectionMode);
  }

  /**
   * Shows a dialog for selecting which columns to display
   */
  public void selectColumns() {
    ColumnSelectionPanel<C> columnSelectionPanel = new ColumnSelectionPanel<>(tableModel.columnModel());
    Dialogs.okCancelDialog(columnSelectionPanel)
            .owner(getParent())
            .title(MESSAGES.getString(SELECT_COLUMNS))
            .onShown(dialog -> columnSelectionPanel.requestColumnPanelFocus())
            .onOk(columnSelectionPanel::applyChanges)
            .show();
  }

  /**
   * Returns true if the given cell is visible.
   * @param row the row
   * @param column the column
   * @return true if this table is contained in a scrollpanel and the cell with the given coordinates is visible.
   */
  public boolean isCellVisible(int row, int column) {
    JViewport viewport = Utilities.getParentOfType(JViewport.class, this);

    return viewport != null && isCellVisible(viewport, row, column);
  }

  /**
   * Scrolls horizontally so that the column identified by the given identifier becomes visible.
   * Has no effect if this table is not contained in a scrollpanel.
   * @param columnIdentifier the column identifier
   */
  public void scrollToColumn(C columnIdentifier) {
    JViewport viewport = Utilities.getParentOfType(JViewport.class, this);
    if (viewport != null) {
      scrollToRowColumn(viewport, rowAtPoint(viewport.getViewPosition()),
              getModel().columnModel().getColumnIndex(columnIdentifier), CenterOnScroll.NEITHER);
    }
  }

  /**
   * Scrolls to the given coordinate. Has no effect if this table is not contained in a scrollpanel.
   * @param row the row
   * @param column the column
   * @param centerOnScroll specifies whether to center the selected row and or column
   */
  public void scrollToCoordinate(int row, int column, CenterOnScroll centerOnScroll) {
    requireNonNull(centerOnScroll);
    JViewport viewport = Utilities.getParentOfType(JViewport.class, this);
    if (viewport != null) {
      scrollToRowColumn(viewport, row, column, centerOnScroll);
    }
  }

  /**
   * Copies the contents of the selected cell to the clipboard.
   */
  public void copySelectedCell() {
    int selectedRow = getSelectedRow();
    int selectedColumn = getSelectedColumn();
    if (selectedRow >= 0 && selectedColumn >= 0) {
      FilteredTableColumn<C> column = getModel().columnModel().getColumn(selectedColumn);
      Utilities.setClipboard(getModel().getStringValueAt(selectedRow, column.getIdentifier()));
    }
  }

  /**
   * Copies the table data as a TAB delimited string, with header, to the clipboard.
   * Note that if the selection is empty all rows are copied, otherwise only selected rows.
   */
  public void copyRowsAsTabDelimitedString() {
    Utilities.setClipboard(tableModel.rowsAsDelimitedString('\t'));
  }

  /**
   * @return a control for showing the column selection dialog
   */
  public Control createSelectColumnsControl() {
    return Control.builder(this::selectColumns)
            .caption(MESSAGES.getString(SELECT) + "...")
            .enabledState(tableModel.columnModel().lockedState().reversedObserver())
            .description(MESSAGES.getString(SELECT_COLUMNS))
            .build();
  }

  /**
   * @return Controls containing {@link ToggleControl}s for showing/hiding columns.
   */
  public Controls createToggleColumnsControls() {
    return Controls.builder()
            .caption(MESSAGES.getString(SELECT))
            .enabledState(tableModel.columnModel().lockedState().reversedObserver())
            .controls(tableModel.columnModel().columns().stream()
                    .sorted(new ColumnComparator())
                    .map(this::createToggleColumnControl)
                    .toArray(ToggleControl[]::new))
            .build();
  }

  /**
   * @return a Control for resetting the columns to their original location and visibility
   */
  public Control createResetColumnsControl() {
    return Control.builder(getModel().columnModel()::resetColumns)
            .caption(MESSAGES.getString(RESET))
            .enabledState(tableModel.columnModel().lockedState().reversedObserver())
            .description(MESSAGES.getString(RESET_COLUMNS_DESCRIPTION))
            .build();
  }

  /**
   * @return a ToggleControl for toggling the table selection mode (single or multiple)
   */
  public ToggleControl createSingleSelectionModeControl() {
    return ToggleControl.builder(tableModel.selectionModel().singleSelectionModeState())
            .caption(MESSAGES.getString(SINGLE_SELECTION_MODE))
            .build();
  }

  /**
   * A convenience method for setting the client property 'JTable.autoStartsEdit'.
   * @param autoStartsEdit the value
   */
  public void setAutoStartsEdit(boolean autoStartsEdit) {
    putClientProperty("JTable.autoStartsEdit", autoStartsEdit);
  }

  /**
   * @param listener a listener notified each time the table is double-clicked
   */
  public void addDoubleClickListener(EventDataListener<MouseEvent> listener) {
    doubleClickedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeDoubleClickListener(EventDataListener<MouseEvent> listener) {
    doubleClickedEvent.removeDataListener(listener);
  }

  /**
   * Instantiates a new {@link FilteredTable.Builder} using the given model
   * @param tableModel the table model
   * @param <T> the table model type
   * @param <R> the type representing rows
   * @param <C> the type used to identify columns
   * @return a new {@link FilteredTable.Builder} instance
   */
  public static <T extends FilteredTableModel<R, C>, R, C> Builder<T, R, C> builder(T tableModel) {
    return new DefaultBuilder<>(tableModel);
  }

  /**
   * Creates a JTextField for searching through this table.
   * @return a search field
   */
  private JTextField createSearchField() {
    Control nextResult = control(() -> selectSearchResult(false, true));
    Control selectNextResult = control(() -> selectSearchResult(true, true));
    Control previousResult = control(() -> selectSearchResult(false, false));
    Control selectPreviousResult = control(() -> selectSearchResult(true, false));
    Control requestTableFocus = control(this::requestFocusInWindow);

    return Components.textField(tableModel.searchModel().searchStringValue())
            .columns(SEARCH_FIELD_COLUMNS)
            .selectAllOnFocusGained(true)
            .keyEvent(KeyEvents.builder(VK_ENTER)
                    .action(nextResult))
            .keyEvent(KeyEvents.builder(VK_ENTER)
                    .modifiers(SHIFT_DOWN_MASK)
                    .action(selectNextResult))
            .keyEvent(KeyEvents.builder(VK_DOWN)
                    .action(nextResult))
            .keyEvent(KeyEvents.builder(VK_DOWN)
                    .modifiers(SHIFT_DOWN_MASK)
                    .action(selectNextResult))
            .keyEvent(KeyEvents.builder(VK_UP)
                    .action(previousResult))
            .keyEvent(KeyEvents.builder(VK_UP)
                    .modifiers(SHIFT_DOWN_MASK)
                    .action(selectPreviousResult))
            .keyEvent(KeyEvents.builder(VK_ESCAPE)
                    .action(requestTableFocus))
            .popupMenuControls(searchFieldPopupMenuControls())
            .hintText(Messages.find() + "...")
            .onTextChanged(searchText -> {
              if (!searchText.isEmpty()) {
                tableModel.searchModel().nextResult();
              }
            })
            .build();
  }

  private void selectSearchResult(boolean addToSelection, boolean next) {
    searchResult(addToSelection, next).ifPresent(rowColumn -> {
      if (!addToSelection) {
        setColumnSelectionInterval(rowColumn.column(), rowColumn.column());
      }
      scrollToCoordinate(rowColumn.row(), rowColumn.column(), centerOnScroll);
    });
  }

  private Optional<RowColumn> searchResult(boolean addToSelection, boolean next) {
    FilteredTableSearchModel searchModel = tableModel.searchModel();
    if (next) {
      return addToSelection ? searchModel.selectNextResult() : searchModel.nextResult();
    }

    return addToSelection ? searchModel.selectPreviousResult() : searchModel.previousResult();
  }

  private Controls searchFieldPopupMenuControls() {
    return Controls.builder()
            .control(ToggleControl.builder(tableModel.searchModel().caseSensitiveSearchState())
                    .caption(MESSAGES.getString("case_sensitive_search")))
            .controls(ToggleControl.builder(tableModel.searchModel().regularExpressionSearchState())
                    .caption(MESSAGES.getString("regular_expression_search")))
            .build();
  }

  private boolean isCellVisible(JViewport viewport, int row, int column) {
    Rectangle cellRect = getCellRect(row, column, true);
    Point viewPosition = viewport.getViewPosition();
    cellRect.setLocation(cellRect.x - viewPosition.x, cellRect.y - viewPosition.y);

    return new Rectangle(viewport.getExtentSize()).contains(cellRect);
  }

  private boolean isRowVisible(JViewport viewport, int row) {
    int topRow = rowAtPoint(viewport.getViewPosition());
    int visibleRows = viewport.getExtentSize().height / getRowHeight();

    return row >= topRow && row <= topRow + visibleRows;
  }

  private void scrollToRowColumn(JViewport viewport, int row, int column, CenterOnScroll centerOnScroll) {
    Rectangle cellRectangle = getCellRect(row, column, true);
    Rectangle viewRectangle = viewport.getViewRect();
    cellRectangle.setLocation(cellRectangle.x - viewRectangle.x, cellRectangle.y - viewRectangle.y);
    int x = cellRectangle.x;
    int y = cellRectangle.y;
    if (centerOnScroll != CenterOnScroll.NEITHER) {
      if (centerOnScroll == CenterOnScroll.COLUMN || centerOnScroll == CenterOnScroll.BOTH) {
        x = (viewRectangle.width - cellRectangle.width) / 2;
        if (cellRectangle.x < x) {
          x = -x;
        }
      }
      if (centerOnScroll == CenterOnScroll.ROW || centerOnScroll == CenterOnScroll.BOTH) {
        y = (viewRectangle.height - cellRectangle.height) / 2;
        if (cellRectangle.y < y) {
          y = -y;
        }
      }
      cellRectangle.translate(x, y);
    }
    viewport.scrollRectToVisible(cellRectangle);
  }

  private ToggleControl createToggleColumnControl(FilteredTableColumn<C> column) {
    C identifier = column.getIdentifier();
    State visibleState = State.state(tableModel.columnModel().isColumnVisible(identifier));
    visibleState.addDataListener(visible -> tableModel.columnModel().setColumnVisible(identifier, visible));

    return ToggleControl.builder(visibleState)
            .caption(column.getHeaderValue().toString())
            .build();
  }

  private void configureColumn(FilteredTableColumn<C> column, TableCellRendererFactory<C> rendererFactory) {
    column.setHeaderRenderer(new SortableHeaderRenderer(column.getHeaderRenderer()));
    column.setCellRenderer(rendererFactory.tableCellRenderer(column));
  }

  private void initializeTableHeader() {
    JTableHeader header = getTableHeader();
    header.setReorderingAllowed(true);
    header.setAutoscrolls(true);
    header.addMouseMotionListener(new ColumnDragMouseHandler());
    header.addMouseListener(new MouseSortHandler());
  }

  private void bindEvents() {
    addMouseListener(new FilteredTableMouseListener());
    tableModel.selectionModel().addSelectedIndexesListener(selectedRowIndexes -> {
      if (scrollToSelectedItem && !selectedRowIndexes.isEmpty() && noRowVisible(selectedRowIndexes)) {
        scrollToCoordinate(selectedRowIndexes.get(0), getSelectedColumn(), centerOnScroll);
      }
    });
    tableModel.columnModel().columns().forEach(this::bindFilterIndicatorEvents);
    tableModel.searchModel().addCurrentResultListener(rowColumn -> repaint());
    tableModel.addSortListener(getTableHeader()::repaint);
    addKeyListener(new MoveResizeColumnKeyListener());
    KeyEvents.builder(VK_C)
            .action(Control.control(this::copySelectedCell))
            .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
            .enable(this);
    KeyEvents.builder(VK_F)
            .action(Control.control(searchField::requestFocusInWindow))
            .modifiers(CTRL_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);
  }

  private boolean noRowVisible(List<Integer> rows) {
    JViewport viewport = Utilities.getParentOfType(JViewport.class, this);
    if (viewport != null) {
      return rows.stream()
              .noneMatch(row -> isRowVisible(viewport, row));
    }

    return false;
  }

  private void bindFilterIndicatorEvents(FilteredTableColumn<?> column) {
    ColumnConditionModel<?, ?> model = getModel().filterModel().columnFilterModels().get(column.getIdentifier());
    if (model != null) {
      model.addEnabledListener(() -> getTableHeader().repaint());
    }
  }

  private final class SortableHeaderRenderer implements TableCellRenderer {

    private final TableCellRenderer wrappedRenderer;

    private SortableHeaderRenderer(TableCellRenderer wrappedRenderer) {
      this.wrappedRenderer = wrappedRenderer;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      Component component = wrappedRenderer == null ?
              table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) :
              wrappedRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      Font defaultFont = component.getFont();
      if (component instanceof JLabel) {
        JLabel label = (JLabel) component;
        FilteredTableColumn<C> tableColumn = ((FilteredTableColumnModel<C>) table.getColumnModel()).getColumn(column);
        ColumnConditionModel<?, ?> filterModel = tableModel.filterModel().columnFilterModels().get(tableColumn.getIdentifier());
        label.setFont((filterModel != null && filterModel.isEnabled()) ? defaultFont.deriveFont(Font.ITALIC) : defaultFont);
        label.setHorizontalTextPosition(SwingConstants.LEFT);
        label.setIcon(headerRendererIcon(tableColumn.getIdentifier(), label.getFont().getSize() + SORT_ICON_SIZE));
      }

      return component;
    }

    private Icon headerRendererIcon(C columnIdentifier, int iconSizePixels) {
      SortOrder sortOrder = tableModel.sortModel().sortOrder(columnIdentifier);
      if (sortOrder == SortOrder.UNSORTED) {
        return null;
      }

      return new Arrow(sortOrder == SortOrder.DESCENDING, iconSizePixels,
              tableModel.sortModel().sortPriority(columnIdentifier));
    }
  }

  /**
   * A MouseListener for handling double click, which invokes the action returned by {@link #getDoubleClickAction()}
   * with this table as the ActionEvent source as well as triggering the {@link #addDoubleClickListener(EventDataListener)} event.
   * @see #getDoubleClickAction()
   */
  private final class FilteredTableMouseListener extends MouseAdapter {

    @Override
    public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
        if (doubleClickAction != null) {
          doubleClickAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "doubleClick"));
        }
        doubleClickedEvent.onEvent(e);
      }
    }
  }

  private static final class Arrow implements Icon {

    private static final double PRIORITY_SIZE_RATIO = 0.8;
    private static final double PRIORITY_SIZE_CONST = 2.0;
    private static final int ALIGNMENT_CONSTANT = 6;

    private final boolean descending;
    private final int size;
    private final int priority;

    private Arrow(boolean descending, int size, int priority) {
      this.descending = descending;
      this.size = size;
      this.priority = priority;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Color color = c == null ? Color.GRAY : c.getBackground();
      // In a compound sort, make each successive triangle 20% smaller than the previous one.
      int dx = (int) (size / PRIORITY_SIZE_CONST * Math.pow(PRIORITY_SIZE_RATIO, priority));
      int dy = descending ? dx : -dx;
      // Align icon (roughly) with font baseline.
      int theY = y + SORT_ICON_SIZE * size / ALIGNMENT_CONSTANT + (descending ? -dy : 0);
      int shift = descending ? 1 : -1;
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
    public void mouseClicked(MouseEvent e) {
      if (!sortingEnabled || e.getButton() != MouseEvent.BUTTON1 || e.isAltDown()) {
        return;
      }

      JTableHeader tableHeader = (JTableHeader) e.getSource();
      FilteredTableColumnModel<C> columnModel = (FilteredTableColumnModel<C>) tableHeader.getColumnModel();
      int index = columnModel.getColumnIndexAtX(e.getX());
      if (index >= 0) {
        if (!getSelectionModel().isSelectionEmpty()) {
          setColumnSelectionInterval(index, index);//otherwise, the focus jumps to the selected column after sorting
        }
        C columnIdentifier = columnModel.getColumn(index).getIdentifier();
        if (isSortingEnabled(columnIdentifier)) {
          FilteredTableSortModel<R, C> sortModel = getModel().sortModel();
          SortOrder nextSortOrder = nextSortOrder(sortModel.sortOrder(columnIdentifier), e.isShiftDown());
          if (e.isControlDown()) {
            sortModel.addSortOrder(columnIdentifier, nextSortOrder);
          }
          else {
            sortModel.setSortOrder(columnIdentifier, nextSortOrder);
          }
        }
      }
    }

    private SortOrder nextSortOrder(SortOrder currentSortOrder, boolean isShiftDown) {
      switch (currentSortOrder) {
        case UNSORTED:
          return isShiftDown ? SortOrder.DESCENDING : SortOrder.ASCENDING;
        case ASCENDING:
          return SortOrder.DESCENDING;
        case DESCENDING:
          return SortOrder.ASCENDING;
        default:
          throw new IllegalStateException("Unknown sort order: " + currentSortOrder);
      }
    }
  }

  private final class ColumnDragMouseHandler extends MouseMotionAdapter {
    @Override
    public void mouseDragged(MouseEvent e) {
      scrollRectToVisible(new Rectangle(e.getX(), getVisibleRect().y, 1, 1));
    }
  }

  /**
   * A builder for a {@link FilteredTable}
   * @param <T> the table model type
   * @param <R> the type representing rows
   * @param <C> the type used to identify columns
   */
  public interface Builder<T extends FilteredTableModel<R, C>, R, C> {

    /**
     * @param conditionPanelFactory the column condition panel factory
     * @return this builder instance
     */
    Builder<T, R, C> conditionPanelFactory(ColumnConditionPanel.Factory<C> conditionPanelFactory);

    /**
     * @param cellRendererFactory the table cell renderer factory
     * @return this builder instance
     */
    Builder<T, R, C> cellRendererFactory(TableCellRendererFactory<C> cellRendererFactory);

    /**
     * @param autoStartsEdit true if editing should start automatically
     * @return this builder instance
     */
    Builder<T, R, C> autoStartsEdit(boolean autoStartsEdit);

    /**
     * @param centerOnScroll the center on scroll behavious
     * @return this builder instance
     */
    Builder<T, R, C> centerOnScroll(CenterOnScroll centerOnScroll);

    /**
     * @param doubleClickAction the double click action
     * @return this builder instance
     */
    Builder<T, R, C> doubleClickAction(Action doubleClickAction);

    /**
     * @param scrollToSelectedItem true if this table should scroll to the selected item
     * @return this builder instance
     */
    Builder<T, R, C> scrollToSelectedItem(boolean scrollToSelectedItem);

    /**
     * @param sortingEnabled true if sorting via clicking the header should be enbled
     * @return this builder instance
     */
    Builder<T, R, C> sortingEnabled(boolean sortingEnabled);

    /**
     * @param selectionMode the table selection mode
     * @return this builder instance
     */
    Builder<T, R, C> selectionMode(int selectionMode);

    /**
     * @param columnReorderingAllowed true if column reordering should be allowed
     * @return this builder instance
     */
    Builder<T, R, C> columnReorderingAllowed(boolean columnReorderingAllowed);

    /**
     * @param columnResizingAllowed true if column resizing should be allowed
     * @return this builder instance
     */
    Builder<T, R, C> columnResizingAllowed(boolean columnResizingAllowed);

    /**
     * @param autoResizeMode the table auto column resizing mode
     * @return this builder instance
     */
    Builder<T, R, C> autoResizeMode(int autoResizeMode);

    /**
     * @return a new {@link FilteredTable} base on this builder
     */
    FilteredTable<T, R, C> build();
  }

  private static final class DefaultBuilder<T extends FilteredTableModel<R, C>, R, C> implements Builder<T, R, C> {

    private final T tableModel;

    private ColumnConditionPanel.Factory<C> conditionPanelFactory;
    private TableCellRendererFactory<C> cellRendererFactory;
    private boolean autoStartsEdit = false;
    private CenterOnScroll centerOnScroll = CenterOnScroll.NEITHER;
    private Action doubleClickAction;
    private boolean scrollToSelectedItem = true;
    private boolean sortingEnabled = true;
    private int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
    private boolean columnReorderingAllowed = ALLOW_COLUMN_REORDERING.get();
    private boolean columnResizingAllowed = true;
    private int autoResizeMode = AUTO_RESIZE_MODE.get();

    private DefaultBuilder(T tableModel) {
      this.tableModel = requireNonNull(tableModel);
      this.conditionPanelFactory = new DefaultFilterPanelFactory<>();
      this.cellRendererFactory = new DefaultTableCellRendererFactory<>(tableModel);
    }

    @Override
    public Builder<T, R, C> conditionPanelFactory(ColumnConditionPanel.Factory<C> conditionPanelFactory) {
      this.conditionPanelFactory = requireNonNull(conditionPanelFactory);
      return this;
    }

    @Override
    public Builder<T, R, C> cellRendererFactory(TableCellRendererFactory<C> cellRendererFactory) {
      this.cellRendererFactory = requireNonNull(cellRendererFactory);
      return this;
    }

    @Override
    public Builder<T, R, C> autoStartsEdit(boolean autoStartsEdit) {
      this.autoStartsEdit = autoStartsEdit;
      return this;
    }

    @Override
    public Builder<T, R, C> centerOnScroll(CenterOnScroll centerOnScroll) {
      this.centerOnScroll = requireNonNull(centerOnScroll);
      return this;
    }

    @Override
    public Builder<T, R, C> doubleClickAction(Action doubleClickAction) {
      this.doubleClickAction = requireNonNull(doubleClickAction);
      return this;
    }

    @Override
    public Builder<T, R, C> scrollToSelectedItem(boolean scrollToSelectedItem) {
      this.scrollToSelectedItem = scrollToSelectedItem;
      return this;
    }

    @Override
    public Builder<T, R, C> sortingEnabled(boolean sortingEnabled) {
      this.sortingEnabled = sortingEnabled;
      return this;
    }

    @Override
    public Builder<T, R, C> selectionMode(int selectionMode) {
      this.selectionMode = selectionMode;
      return this;
    }

    @Override
    public Builder<T, R, C> columnReorderingAllowed(boolean columnReorderingAllowed) {
      this.columnReorderingAllowed = columnReorderingAllowed;
      return this;
    }

    @Override
    public Builder<T, R, C> columnResizingAllowed(boolean columnResizingAllowed) {
      this.columnResizingAllowed = columnResizingAllowed;
      return this;
    }

    @Override
    public Builder<T, R, C> autoResizeMode(int autoResizeMode) {
      this.autoResizeMode = autoResizeMode;
      return this;
    }

    @Override
    public FilteredTable<T, R, C> build() {
      FilteredTable<T, R, C> filteredTable = new FilteredTable<>(tableModel, conditionPanelFactory, cellRendererFactory);
      filteredTable.setAutoStartsEdit(autoStartsEdit);
      filteredTable.setCenterOnScroll(centerOnScroll);
      filteredTable.setDoubleClickAction(doubleClickAction);
      filteredTable.setScrollToSelectedItem(scrollToSelectedItem);
      filteredTable.setSortingEnabled(sortingEnabled);
      filteredTable.setSelectionMode(selectionMode);
      filteredTable.getTableHeader().setReorderingAllowed(columnReorderingAllowed);
      filteredTable.getTableHeader().setResizingAllowed(columnResizingAllowed);
      filteredTable.setAutoResizeMode(autoResizeMode);

      return filteredTable;
    }
  }

  private final class MoveResizeColumnKeyListener extends KeyAdapter {

    @Override
    public void keyPressed(KeyEvent e) {
      if (e.isControlDown() && e.isShiftDown() && (e.getKeyCode() == VK_LEFT || e.getKeyCode() == VK_RIGHT)) {
        moveSelectedColumn(e.getKeyCode() == VK_LEFT);
        e.consume();
      }
      else if (e.isControlDown() && (RESIZE_KEYS.contains(e.getKeyCode()))) {
        resizeSelectedColumn(e.getKeyCode() == VK_PLUS || e.getKeyCode() == VK_ADD);
        e.consume();
      }
    }

    private void moveSelectedColumn(boolean left) {
      int selectedColumnIndex = getSelectedColumn();
      if (selectedColumnIndex != -1) {
        int columnCount = getColumnModel().getColumnCount();
        int newIndex;
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

    private void resizeSelectedColumn(boolean enlarge) {
      int selectedColumnIndex = getSelectedColumn();
      if (selectedColumnIndex != -1) {
        TableColumn column = getColumnModel().getColumn(selectedColumnIndex);
        column.setPreferredWidth(column.getWidth() + (enlarge ? COLUMN_RESIZE_AMOUNT : -COLUMN_RESIZE_AMOUNT));
      }
    }
  }

  static final class ColumnComparator implements Comparator<TableColumn> {

    private final Collator columnCollator = Collator.getInstance();

    @Override
    public int compare(TableColumn col1, TableColumn col2) {
      return Text.collateSansSpaces(columnCollator, col1.getHeaderValue().toString(), col2.getHeaderValue().toString());
    }
  }

  private static final class DefaultFilterPanelFactory<C> implements ColumnConditionPanel.Factory<C> {

    @Override
    public <T> ColumnConditionPanel<C, T> createConditionPanel(ColumnConditionModel<? extends C, T> filterModel) {
      return (ColumnConditionPanel<C, T>) columnConditionPanel(filterModel);
    }
  }

  private static final class DefaultTableCellRendererFactory<T extends FilteredTableModel<R, C>, R, C> implements TableCellRendererFactory<C> {

    private final T tableModel;

    private DefaultTableCellRendererFactory(T tableModel) {
      this.tableModel = tableModel;
    }

    @Override
    public TableCellRenderer tableCellRenderer(FilteredTableColumn<C> column) {
      return FilteredTableCellRenderer.builder(tableModel, column.getIdentifier(), column.getClass()).build();
    }
  }
}
