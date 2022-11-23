/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnFilterModel;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
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
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.columnConditionPanel;
import static is.codion.swing.common.ui.control.Control.control;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A JTable implementation for {@link FilteredTableModel}.
 * Note that for the table header to display you must add this table to a JScrollPane.
 * For instances use the {@link #filteredTable(FilteredTableModel)} or
 * {@link #filteredTable(FilteredTableModel, ConditionPanelFactory)} factory methods.
 * @param <R> the type representing rows
 * @param <C> the type used to identify columns
 * @param <T> the table model type
 * @see #filteredTable(FilteredTableModel)
 * @see #filteredTable(FilteredTableModel, ConditionPanelFactory)
 */
public final class FilteredTable<R, C, T extends FilteredTableModel<R, C>> extends JTable {

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
  private static final List<Integer> RESIZE_KEYS = asList(KeyEvent.VK_PLUS, KeyEvent.VK_ADD, KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT);

  /**
   * The table model
   */
  private final T tableModel;

  /**
   * Creates the filter condition panels
   */
  private final ConditionPanelFactory conditionPanelFactory;

  /**
   * The column filter panels
   */
  private final Map<FilteredTableColumn<C>, ColumnConditionPanel<C, ?>> columnFilterPanels = new HashMap<>();

  /**
   * Active filter panel dialogs
   */
  private final Map<ColumnConditionPanel<C, ?>, JDialog> columnFilterPanelDialogs = new HashMap<>();

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

  private FilteredTable(T tableModel, ConditionPanelFactory conditionPanelFactory) {
    super(requireNonNull(tableModel, "tableModel"), tableModel.columnModel(), tableModel.selectionModel());
    this.tableModel = tableModel;
    this.conditionPanelFactory = requireNonNull(conditionPanelFactory, "conditionPanelFactory");
    this.searchField = createSearchField();
    initializeTableHeader();
    bindEvents();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(getTableHeader(), searchField);
    if (columnFilterPanels != null) {
      Utilities.updateUI(columnFilterPanels.values());
    }
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
   * Hides or shows the active filter panel dialogs for this table panel
   * @param filterPanelsVisible true if the active filter panel dialogs should be shown, false if they should be hidden
   */
  public void setFilterPanelsVisible(boolean filterPanelsVisible) {
    columnFilterPanelDialogs.values().forEach(dialog -> dialog.setVisible(filterPanelsVisible));
  }

  /**
   * Shows a dialog for selecting which columns to show/hide
   */
  public void selectColumns() {
    SelectColumnsPanel<C> selectColumnsPanel = new SelectColumnsPanel<>(tableModel.columnModel());
    Dialogs.okCancelDialog(selectColumnsPanel)
            .owner(getParent())
            .title(MESSAGES.getString(SELECT_COLUMNS))
            .onShown(dialog -> selectColumnsPanel.requestColumnPanelFocus())
            .onOk(selectColumnsPanel::applyChanges)
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
      Utilities.setClipboard(getModel().getStringAt(selectedRow, column.getIdentifier()));
    }
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
   * Instantiates a new {@link FilteredTable} using the given model
   * @param tableModel the table model
   * @param <R> the type representing rows
   * @param <C> the type used to identify columns
   * @param <T> the table model type
   * @return a new {@link FilteredTable}
   */
  public static <R, C, T extends FilteredTableModel<R, C>> FilteredTable<R, C, T> filteredTable(T tableModel) {
    return filteredTable(tableModel, new DefaultConditionPanelFactory<>(tableModel));
  }

  /**
   * Instantiates a new {@link FilteredTable} using the given model
   * @param tableModel the table model
   * @param conditionPanelFactory the column condition panel factory
   * @param <R> the type representing rows
   * @param <C> the type used to identify columns
   * @param <T> the table model type
   * @return a new {@link FilteredTable}
   */
  public static <R, C, T extends FilteredTableModel<R, C>> FilteredTable<R, C, T> filteredTable(T tableModel, ConditionPanelFactory conditionPanelFactory) {
    return new FilteredTable<>(tableModel, conditionPanelFactory);
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

    String hintText = Messages.searchFieldHint();
    return Components.textField(tableModel.searchModel().searchStringValue())
            .columns(SEARCH_FIELD_COLUMNS)
            .selectAllOnFocusGained(true)
            .keyEvent(KeyEvents.builder(KeyEvent.VK_ENTER)
                    .action(nextResult))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_ENTER)
                    .modifiers(InputEvent.SHIFT_DOWN_MASK)
                    .action(selectNextResult))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_DOWN)
                    .action(nextResult))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_DOWN)
                    .modifiers(InputEvent.SHIFT_DOWN_MASK)
                    .action(selectNextResult))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_UP)
                    .action(previousResult))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_UP)
                    .modifiers(InputEvent.SHIFT_DOWN_MASK)
                    .action(selectPreviousResult))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_ESCAPE)
                    .action(requestTableFocus))
            .popupMenuControls(searchFieldPopupMenuControls())
            .hintText(hintText)
            .onTextChanged(searchText -> {
              if (!searchText.isEmpty() && !Objects.equals(searchText, hintText)) {
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

  private void initializeTableHeader() {
    getTableHeader().addMouseListener(new ColumnFilterPanelMouseHandler());
    tableModel.addSortListener(getTableHeader()::repaint);
    getTableHeader().setReorderingAllowed(true);
    getTableHeader().setAutoscrolls(true);
    getTableHeader().addMouseMotionListener(new ColumnDragMouseHandler());
    getTableHeader().addMouseListener(new MouseSortHandler());
    tableModel.columnModel().columns().forEach(tableColumn ->
            tableColumn.setHeaderRenderer(new SortableHeaderRenderer(tableColumn.getHeaderRenderer())));
  }

  private void bindEvents() {
    addMouseListener(createTableMouseListener());
    tableModel.selectionModel().addSelectedIndexesListener(selectedRowIndexes -> {
      if (scrollToSelectedItem && !selectedRowIndexes.isEmpty() && noRowVisible(selectedRowIndexes)) {
        scrollToCoordinate(selectedRowIndexes.get(0), getSelectedColumn(), centerOnScroll);
      }
    });
    tableModel.columnModel().columns().forEach(this::bindFilterIndicatorEvents);
    tableModel.searchModel().addCurrentResultListener(rowColumn -> repaint());
    addKeyListener(new MoveResizeColumnKeyListener());
  }

  private boolean noRowVisible(List<Integer> rows) {
    JViewport viewport = Utilities.getParentOfType(JViewport.class, this);
    if (viewport != null) {
      return rows.stream()
              .noneMatch(row -> isRowVisible(viewport, row));
    }

    return false;
  }

  private void bindFilterIndicatorEvents(FilteredTableColumn<C> column) {
    ColumnFilterModel<R, C, Object> model = (ColumnFilterModel<R, C, Object>) getModel().columnFilterModels().get(column.getIdentifier());
    if (model != null) {
      model.addEnabledListener(() -> getTableHeader().repaint());
    }
  }

  /**
   * Creates the MouseListener for the table component handling double click.
   * Double-clicking invokes the action returned by {@link #getDoubleClickAction()}
   * with this table as the ActionEvent source
   * @return the MouseListener for the table
   * @see #getDoubleClickAction()
   */
  private MouseListener createTableMouseListener() {
    return new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          if (doubleClickAction != null) {
            doubleClickAction.actionPerformed(new ActionEvent(this, -1, "doubleClick"));
          }
          doubleClickedEvent.onEvent(e);
        }
      }
    };
  }

  private static final class DefaultConditionPanelFactory<C> implements ConditionPanelFactory {

    private final FilteredTableModel<?, C> tableModel;

    private DefaultConditionPanelFactory(FilteredTableModel<?, C> tableModel) {
      this.tableModel = tableModel;
    }

    @Override
    public <C, T> ColumnConditionPanel<C, T> createConditionPanel(FilteredTableColumn<C> column) {
      ColumnFilterModel<?, C, Object> filterModel = (ColumnFilterModel<?, C, Object>) tableModel.columnFilterModels().get(column.getIdentifier());
      if (filterModel == null) {
        return null;
      }

      return columnConditionPanel((ColumnConditionModel<C, T>) filterModel, ColumnConditionPanel.ToggleAdvancedButton.YES);
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
        ColumnFilterModel<R, C, ?> filterModel = tableModel.columnFilterModels().get(tableColumn.getIdentifier());
        label.setFont((filterModel != null && filterModel.isEnabled()) ? defaultFont.deriveFont(Font.ITALIC) : defaultFont);
        label.setHorizontalTextPosition(SwingConstants.LEFT);
        label.setIcon(headerRendererIcon(tableColumn.getIdentifier(), label.getFont().getSize() + SORT_ICON_SIZE));
      }

      return component;
    }

    private Icon headerRendererIcon(C columnIdentifier, int iconSizePixels) {
      SortOrder sortOrder = tableModel.sortModel().sortingState(columnIdentifier).sortOrder();
      if (sortOrder == SortOrder.UNSORTED) {
        return null;
      }

      return new Arrow(sortOrder == SortOrder.DESCENDING, iconSizePixels,
              tableModel.sortModel().sortingState(columnIdentifier).priority());
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
          SortOrder newSortOrder = newSortOrder(sortModel.sortingState(columnIdentifier).sortOrder(), e.isShiftDown());
          if (e.isControlDown()) {
            sortModel.addSortOrder(columnIdentifier, newSortOrder);
          }
          else {
            sortModel.setSortOrder(columnIdentifier, newSortOrder);
          }
        }
      }
    }

    private SortOrder newSortOrder(SortOrder currentSortOrder, boolean isShiftDown) {
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

  private final class ColumnFilterPanelMouseHandler extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      if (e.isAltDown() && e.isControlDown()) {
        toggleFilterPanel(e);
      }
    }

    private void toggleFilterPanel(MouseEvent event) {
      FilteredTableColumnModel<C> columnModel = getModel().columnModel();
      FilteredTableColumn<C> column = columnModel.getColumn(columnModel.getColumnIndexAtX(event.getX()));
      toggleFilterPanel(columnFilterPanels.computeIfAbsent(column, conditionPanelFactory::createConditionPanel),
              column.getHeaderValue().toString(), event.getLocationOnScreen());
    }

    private void toggleFilterPanel(ColumnConditionPanel<C, ?> filterPanel, String title, Point location) {
      if (filterPanel != null) {
        JDialog dialog = filterPanelDialog(filterPanel, title);
        if (dialog.isShowing()) {
          columnFilterPanelDialogs.remove(filterPanel).dispose();
        }
        else {
          showDialog(filterPanel, title, location);
        }
      }
    }

    private void showDialog(ColumnConditionPanel<C, ?> filterPanel, String title, Point location) {
      JDialog dialog = filterPanelDialog(filterPanel, title);
      //adjust the location to above the column header
      location.y = location.y - dialog.getHeight() - getTableHeader().getHeight();
      dialog.setLocation(location);
      dialog.setVisible(true);
      filterPanel.addAdvancedViewListener(advanced -> dialog.pack());
      filterPanel.requestInputFocus();
    }

    private JDialog filterPanelDialog(ColumnConditionPanel<C, ?> filterPanel, String title) {
      return columnFilterPanelDialogs.computeIfAbsent(filterPanel, k -> Dialogs.componentDialog(k)
              .owner(FilteredTable.this)
              .title(title)
              .modal(false)
              .disposeOnEscape(false)
              .onClosed(event -> columnFilterPanelDialogs.remove(filterPanel).dispose())
              .build());
    }
  }

  private final class ColumnDragMouseHandler extends MouseMotionAdapter {
    @Override
    public void mouseDragged(MouseEvent e) {
      scrollRectToVisible(new Rectangle(e.getX(), getVisibleRect().y, 1, 1));
    }
  }

  private final class MoveResizeColumnKeyListener extends KeyAdapter {

    @Override
    public void keyPressed(KeyEvent e) {
      if (e.isControlDown() && e.isShiftDown() && (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT)) {
        moveSelectedColumn(e.getKeyCode() == KeyEvent.VK_LEFT);
        e.consume();
      }
      else if (e.isControlDown() && (RESIZE_KEYS.contains(e.getKeyCode()))) {
        resizeSelectedColumn(e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_ADD);
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
}
