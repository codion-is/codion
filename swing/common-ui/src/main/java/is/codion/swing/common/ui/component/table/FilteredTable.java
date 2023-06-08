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
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableSearchModel;
import is.codion.swing.common.model.component.table.FilteredTableSearchModel.RowColumn;
import is.codion.swing.common.model.component.table.FilteredTableSortModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.ComponentBuilder;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
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
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.swing.common.model.component.table.FilteredTableSortModel.nextSortOrder;
import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.columnConditionPanel;
import static is.codion.swing.common.ui.component.table.FilteredTableConditionPanel.filteredTableConditionPanel;
import static is.codion.swing.common.ui.control.Control.control;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A JTable implementation for {@link FilteredTableModel}.
 * Note that for the table header to display you must add this table to a JScrollPane.
 * For instances use the builder {@link #builder(FilteredTableModel)}
 * @param <R> the type representing rows
 * @param <C> the type used to identify columns
 * @see #builder(FilteredTableModel)
 */
public final class FilteredTable<R, C> extends JTable {

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
  private static final int COLUMN_RESIZE_AMOUNT = 10;
  private static final List<Integer> RESIZE_KEYS = asList(VK_PLUS, VK_ADD, VK_MINUS, VK_SUBTRACT);

  /**
   * The table model
   */
  private final FilteredTableModel<R, C> tableModel;

  /**
   * The condition panel factory
   */
  private final ColumnConditionPanel.Factory<C> conditionPanelFactory;

  /**
   * Fired each time the table is double-clicked
   */
  private final Event<MouseEvent> doubleClickEvent = Event.event();

  /**
   * The filter condition panel
   */
  private FilteredTableConditionPanel<C> filterPanel;

  /**
   * The text field used for entering the search condition
   */
  private JTextField searchField;

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

  private FilteredTable(DefaultBuilder<R, C> builder) {
    super(requireNonNull(builder.tableModel, "tableModel"), builder.tableModel.columnModel(), builder.tableModel.selectionModel());
    this.tableModel = builder.tableModel;
    this.conditionPanelFactory = requireNonNull(builder.conditionPanelFactory);
    this.tableModel.columnModel().columns().forEach(column -> configureColumn(column, requireNonNull(builder.cellRendererFactory)));
    this.centerOnScroll = builder.centerOnScroll;
    this.doubleClickAction = builder.doubleClickAction;
    this.scrollToSelectedItem = builder.scrollToSelectedItem;
    this.sortingEnabled = builder.sortingEnabled;
    setAutoStartsEdit(builder.autoStartsEdit);
    setSelectionMode(builder.selectionMode);
    setAutoResizeMode(builder.autoResizeMode);
    initializeTableHeader(builder.columnReorderingAllowed, builder.columnResizingAllowed);
    bindEvents();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(getTableHeader(), searchField, filterPanel);
  }

  @Override
  public FilteredTableModel<R, C> getModel() {
    return (FilteredTableModel<R, C>) super.getModel();
  }

  @Override
  public FilteredTableColumnModel<C> getColumnModel() {
    return (FilteredTableColumnModel<C>) super.getColumnModel();
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
   * @return the filter panel
   */
  public FilteredTableConditionPanel<C> filterPanel() {
    if (filterPanel == null) {
      filterPanel = filteredTableConditionPanel(tableModel.filterModel(), tableModel.columnModel(), conditionPanelFactory);
    }

    return filterPanel;
  }

  /**
   * @return the search field
   */
  public JTextField searchField() {
    if (searchField == null) {
      searchField = createSearchField();
    }

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
    JViewport viewport = Utilities.parentOfType(JViewport.class, this);

    return viewport != null && isCellVisible(viewport, row, column);
  }

  /**
   * Scrolls horizontally so that the column identified by the given identifier becomes visible.
   * Has no effect if this table is not contained in a scrollpanel.
   * @param columnIdentifier the column identifier
   */
  public void scrollToColumn(C columnIdentifier) {
    requireNonNull(columnIdentifier);
    JViewport viewport = Utilities.parentOfType(JViewport.class, this);
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
    JViewport viewport = Utilities.parentOfType(JViewport.class, this);
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
            .name(MESSAGES.getString(SELECT) + "...")
            .enabledState(tableModel.columnModel().lockedState().reversedObserver())
            .description(MESSAGES.getString(SELECT_COLUMNS))
            .build();
  }

  /**
   * @return Controls containing {@link ToggleControl}s for showing/hiding columns.
   */
  public Controls createToggleColumnsControls() {
    return Controls.builder()
            .name(MESSAGES.getString(SELECT))
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
            .name(MESSAGES.getString(RESET))
            .enabledState(tableModel.columnModel().lockedState().reversedObserver())
            .description(MESSAGES.getString(RESET_COLUMNS_DESCRIPTION))
            .build();
  }

  /**
   * @return a ToggleControl for toggling the table selection mode (single or multiple)
   */
  public ToggleControl createSingleSelectionModeControl() {
    return ToggleControl.builder(tableModel.selectionModel().singleSelectionModeState())
            .name(MESSAGES.getString(SINGLE_SELECTION_MODE))
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
    doubleClickEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeDoubleClickListener(EventDataListener<MouseEvent> listener) {
    doubleClickEvent.removeDataListener(listener);
  }

  /**
   * Instantiates a new {@link FilteredTable.Builder} using the given model
   * @param tableModel the table model
   * @param <R> the type representing rows
   * @param <C> the type used to identify columns
   * @return a new {@link FilteredTable.Builder} instance
   */
  public static <R, C> Builder<R, C> builder(FilteredTableModel<R, C> tableModel) {
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
            .onTextChanged(this::onSearchTextChanged)
            .onBuild(field -> KeyEvents.builder(VK_F)
                    .modifiers(CTRL_DOWN_MASK)
                    .action(control(field::requestFocusInWindow))
                    .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .enable(this))
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

  private void onSearchTextChanged(String searchText) {
    if (!searchText.isEmpty()) {
      tableModel.searchModel().nextResult();
    }
  }

  private void toggleColumnSorting(int selectedColumn, boolean add) {
    if (selectedColumn != -1) {
      C columnIdentifier = tableModel.columnModel().getColumn(selectedColumn).getIdentifier();
      FilteredTableSortModel<R, C> sortModel = tableModel.sortModel();
      if (sortModel.isSortingEnabled(columnIdentifier)) {
        if (add) {
          sortModel.addSortOrder(columnIdentifier, nextSortOrder(sortModel.sortOrder(columnIdentifier)));
        }
        else {
          sortModel.setSortOrder(columnIdentifier, nextSortOrder(sortModel.sortOrder(columnIdentifier)));
        }
      }
    }
  }

  private Controls searchFieldPopupMenuControls() {
    return Controls.builder()
            .control(ToggleControl.builder(tableModel.searchModel().caseSensitiveState())
                    .name(MESSAGES.getString("case_sensitive_search")))
            .controls(ToggleControl.builder(tableModel.searchModel().regularExpressionState())
                    .name(MESSAGES.getString("regular_expression_search")))
            .build();
  }

  private boolean isCellVisible(JViewport viewport, int row, int column) {
    Rectangle cellRect = getCellRect(row, column, true);
    Point viewPosition = viewport.getViewPosition();
    cellRect.setLocation(cellRect.x - viewPosition.x, cellRect.y - viewPosition.y);

    return new Rectangle(viewport.getExtentSize()).contains(cellRect);
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
    return ToggleControl.builder(tableModel.columnModel().visibleState(column.getIdentifier()))
            .name(column.getHeaderValue().toString())
            .build();
  }

  private void configureColumn(FilteredTableColumn<C> column, FilteredTableCellRendererFactory<C> rendererFactory) {
    column.setCellRenderer(rendererFactory.tableCellRenderer(column));
    column.setHeaderRenderer(new FilteredTableHeaderRenderer<>(this, column));
  }

  private void initializeTableHeader(boolean reorderingAllowed, boolean columnResizingAllowed) {
    JTableHeader header = getTableHeader();
    header.setFocusable(false);
    header.setReorderingAllowed(reorderingAllowed);
    header.setResizingAllowed(columnResizingAllowed);
    header.setAutoscrolls(true);
    header.addMouseMotionListener(new ColumnDragMouseHandler());
    header.addMouseListener(new MouseSortHandler());
  }

  private void bindEvents() {
    addMouseListener(new FilteredTableMouseListener());
    tableModel.selectionModel().addSelectedIndexesListener(new ScrollToSelectedListener());
    tableModel.filterModel().addChangeListener(getTableHeader()::repaint);
    tableModel.searchModel().addCurrentResultListener(rowColumn -> repaint());
    tableModel.sortModel().addSortingChangedListener(columnIdentifier -> getTableHeader().repaint());
    addKeyListener(new MoveResizeColumnKeyListener());
    KeyEvents.builder(VK_C)
            .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
            .action(control(this::copySelectedCell))
            .enable(this);
    KeyEvents.builder(VK_UP)
            .modifiers(ALT_DOWN_MASK)
            .action(control(() -> toggleColumnSorting(getSelectedColumn(), true)))
            .enable(this);
    KeyEvents.builder(VK_DOWN)
            .modifiers(ALT_DOWN_MASK)
            .action(control(() -> toggleColumnSorting(getSelectedColumn(), false)))
            .enable(this);
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
        doubleClickEvent.onEvent(e);
      }
    }
  }

  private final class ScrollToSelectedListener implements EventDataListener<List<Integer>> {

    @Override
    public void onEvent(List<Integer> selectedRowIndexes) {
      if (scrollToSelectedItem && !selectedRowIndexes.isEmpty() && noRowVisible(selectedRowIndexes)) {
        scrollToCoordinate(selectedRowIndexes.get(0), getSelectedColumn(), centerOnScroll);
      }
    }

    private boolean noRowVisible(List<Integer> rows) {
      JViewport viewport = Utilities.parentOfType(JViewport.class, FilteredTable.this);
      if (viewport != null) {
        return rows.stream().noneMatch(row -> isRowVisible(viewport, row));
      }

      return false;
    }

    private boolean isRowVisible(JViewport viewport, int row) {
      int topRow = rowAtPoint(viewport.getViewPosition());
      int visibleRows = viewport.getExtentSize().height / getRowHeight();

      return row >= topRow && row <= topRow + visibleRows;
    }
  }

  private final class MouseSortHandler extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      if (!sortingEnabled || e.getButton() != MouseEvent.BUTTON1 || e.isAltDown()) {
        return;
      }

      FilteredTableColumnModel<C> columnModel = tableModel.columnModel();
      int index = columnModel.getColumnIndexAtX(e.getX());
      if (index >= 0) {
        if (!getSelectionModel().isSelectionEmpty()) {
          setColumnSelectionInterval(index, index);//otherwise, the focus jumps to the selected column after sorting
        }
        C columnIdentifier = columnModel.getColumn(index).getIdentifier();
        if (tableModel.sortModel().isSortingEnabled(columnIdentifier)) {
          FilteredTableSortModel<R, C> sortModel = tableModel.sortModel();
          SortOrder nextSortOrder = nextSortOrder(sortModel.sortOrder(columnIdentifier));
          if (e.isControlDown()) {
            sortModel.addSortOrder(columnIdentifier, nextSortOrder);
          }
          else {
            sortModel.setSortOrder(columnIdentifier, nextSortOrder);
          }
        }
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
   * @param <R> the type representing rows
   * @param <C> the type used to identify columns
   */
  public interface Builder<R, C> extends ComponentBuilder<Void, FilteredTable<R, C>, Builder<R, C>> {

    /**
     * @param conditionPanelFactory the column condition panel factory
     * @return this builder instance
     */
    Builder<R, C> conditionPanelFactory(ColumnConditionPanel.Factory<C> conditionPanelFactory);

    /**
     * @param cellRendererFactory the table cell renderer factory
     * @return this builder instance
     */
    Builder<R, C> cellRendererFactory(FilteredTableCellRendererFactory<C> cellRendererFactory);

    /**
     * @param autoStartsEdit true if editing should start automatically
     * @return this builder instance
     */
    Builder<R, C> autoStartsEdit(boolean autoStartsEdit);

    /**
     * @param centerOnScroll the center on scroll behavious
     * @return this builder instance
     */
    Builder<R, C> centerOnScroll(CenterOnScroll centerOnScroll);

    /**
     * @param doubleClickAction the double click action
     * @return this builder instance
     */
    Builder<R, C> doubleClickAction(Action doubleClickAction);

    /**
     * @param scrollToSelectedItem true if this table should scroll to the selected item
     * @return this builder instance
     */
    Builder<R, C> scrollToSelectedItem(boolean scrollToSelectedItem);

    /**
     * @param sortingEnabled true if sorting via clicking the header should be enbled
     * @return this builder instance
     */
    Builder<R, C> sortingEnabled(boolean sortingEnabled);

    /**
     * @param selectionMode the table selection mode
     * @return this builder instance
     */
    Builder<R, C> selectionMode(int selectionMode);

    /**
     * @param columnReorderingAllowed true if column reordering should be allowed
     * @return this builder instance
     */
    Builder<R, C> columnReorderingAllowed(boolean columnReorderingAllowed);

    /**
     * @param columnResizingAllowed true if column resizing should be allowed
     * @return this builder instance
     */
    Builder<R, C> columnResizingAllowed(boolean columnResizingAllowed);

    /**
     * @param autoResizeMode the table auto column resizing mode
     * @return this builder instance
     */
    Builder<R, C> autoResizeMode(int autoResizeMode);
  }

  private static final class DefaultBuilder<R, C>
          extends AbstractComponentBuilder<Void, FilteredTable<R, C>, Builder<R, C>>
          implements Builder<R, C> {

    private final FilteredTableModel<R, C> tableModel;

    private ColumnConditionPanel.Factory<C> conditionPanelFactory;
    private FilteredTableCellRendererFactory<C> cellRendererFactory;
    private boolean autoStartsEdit = false;
    private CenterOnScroll centerOnScroll = CenterOnScroll.NEITHER;
    private Action doubleClickAction;
    private boolean scrollToSelectedItem = true;
    private boolean sortingEnabled = true;
    private int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
    private boolean columnReorderingAllowed = ALLOW_COLUMN_REORDERING.get();
    private boolean columnResizingAllowed = true;
    private int autoResizeMode = AUTO_RESIZE_MODE.get();

    private DefaultBuilder(FilteredTableModel<R, C> tableModel) {
      this.tableModel = requireNonNull(tableModel);
      this.conditionPanelFactory = new DefaultFilterPanelFactory<>();
      this.cellRendererFactory = new DefaultFilteredTableCellRendererFactory<>(tableModel);
    }

    @Override
    public Builder<R, C> conditionPanelFactory(ColumnConditionPanel.Factory<C> conditionPanelFactory) {
      this.conditionPanelFactory = requireNonNull(conditionPanelFactory);
      return this;
    }

    @Override
    public Builder<R, C> cellRendererFactory(FilteredTableCellRendererFactory<C> cellRendererFactory) {
      this.cellRendererFactory = requireNonNull(cellRendererFactory);
      return this;
    }

    @Override
    public Builder<R, C> autoStartsEdit(boolean autoStartsEdit) {
      this.autoStartsEdit = autoStartsEdit;
      return this;
    }

    @Override
    public Builder<R, C> centerOnScroll(CenterOnScroll centerOnScroll) {
      this.centerOnScroll = requireNonNull(centerOnScroll);
      return this;
    }

    @Override
    public Builder<R, C> doubleClickAction(Action doubleClickAction) {
      this.doubleClickAction = requireNonNull(doubleClickAction);
      return this;
    }

    @Override
    public Builder<R, C> scrollToSelectedItem(boolean scrollToSelectedItem) {
      this.scrollToSelectedItem = scrollToSelectedItem;
      return this;
    }

    @Override
    public Builder<R, C> sortingEnabled(boolean sortingEnabled) {
      this.sortingEnabled = sortingEnabled;
      return this;
    }

    @Override
    public Builder<R, C> selectionMode(int selectionMode) {
      this.selectionMode = selectionMode;
      return this;
    }

    @Override
    public Builder<R, C> columnReorderingAllowed(boolean columnReorderingAllowed) {
      this.columnReorderingAllowed = columnReorderingAllowed;
      return this;
    }

    @Override
    public Builder<R, C> columnResizingAllowed(boolean columnResizingAllowed) {
      this.columnResizingAllowed = columnResizingAllowed;
      return this;
    }

    @Override
    public Builder<R, C> autoResizeMode(int autoResizeMode) {
      this.autoResizeMode = autoResizeMode;
      return this;
    }

    @Override
    protected FilteredTable<R, C> createComponent() {
      return new FilteredTable<>(this);
    }

    @Override
    protected ComponentValue<Void, FilteredTable<R, C>> createComponentValue(FilteredTable<R, C> component) {
      throw new UnsupportedOperationException("A ComponentValue can not be based on a FilteredTable");
    }

    @Override
    protected void setInitialValue(FilteredTable<R, C> component, Void initialValue) {}
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
    public <T> Optional<ColumnConditionPanel<C, T>> createConditionPanel(ColumnConditionModel<? extends C, T> filterModel) {
      return columnConditionPanel(filterModel);
    }
  }

  private static final class DefaultFilteredTableCellRendererFactory<R, C> implements FilteredTableCellRendererFactory<C> {

    private final FilteredTableModel<R, C> tableModel;

    private DefaultFilteredTableCellRendererFactory(FilteredTableModel<R, C> tableModel) {
      this.tableModel = tableModel;
    }

    @Override
    public TableCellRenderer tableCellRenderer(FilteredTableColumn<C> column) {
      return FilteredTableCellRenderer.builder(tableModel, column.getIdentifier(), column.columnClass()).build();
    }
  }
}
