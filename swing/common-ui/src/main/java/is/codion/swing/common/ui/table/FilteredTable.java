/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnFilterModel;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.model.table.FilteredTableModel;
import is.codion.swing.common.model.table.FilteredTableModel.RowColumn;
import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;
import is.codion.swing.common.model.table.TableSortModel;
import is.codion.swing.common.model.textfield.DocumentAdapter;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.table.ColumnConditionPanel.ToggleAdvancedButton;
import is.codion.swing.common.ui.textfield.TextFieldHint;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
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
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.control.Control.control;
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

  private static final String SELECT_COLUMNS = "select_columns";
  private static final String RESET_COLUMNS = "reset_columns";
  private static final String RESET_COLUMNS_DESCRIPTION = "reset_columns_description";
  private static final String SINGLE_SELECTION_MODE = "single_selection_mode";
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
   * Creates the filter condition panels
   */
  private final ConditionPanelFactory conditionPanelFactory;

  /**
   * The column filter panels
   */
  private final Map<TableColumn, ColumnConditionPanel<C, ?>> columnFilterPanels = new HashMap<>();

  /**
   * The text field used for entering the search condition
   */
  private final JTextField searchField;
  private final TextFieldHint searchFieldHint;

  /**
   * Fired each time the table is double-clicked
   */
  private final Event<MouseEvent> doubleClickedEvent = Event.event();

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

  /**
   * The coordinate of the last search result
   */
  private RowColumn lastSearchResultCoordinate = NULL_COORDINATE;

  /**
   * Instantiates a new FilteredTable using the given model
   * @param tableModel the table model
   */
  public FilteredTable(final T tableModel) {
    this(tableModel, new DefaultConditionPanelFactory<>(tableModel));
  }

  /**
   * Instantiates a new FilteredTable using the given model
   * @param tableModel the table model
   * @param conditionPanelFactory the column condition panel factory
   */
  public FilteredTable(final T tableModel, final ConditionPanelFactory conditionPanelFactory) {
    super(requireNonNull(tableModel, "tableModel"), tableModel.getColumnModel(), tableModel.getSelectionModel());
    this.tableModel = tableModel;
    this.conditionPanelFactory = requireNonNull(conditionPanelFactory, "conditionPanelFactory");
    this.searchField = initializeSearchField();
    this.searchFieldHint = initializeSearchFieldHint();
    initializeTableHeader();
    bindEvents();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(getTableHeader(), searchField);
    if (searchFieldHint != null) {
      searchFieldHint.updateHint();
    }
    if (columnFilterPanels != null) {
      Utilities.updateUI(columnFilterPanels.values());
    }
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
  public void setCenterOnScroll(final CenterOnScroll centerOnScroll) {
    this.centerOnScroll = requireNonNull(centerOnScroll);
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
    columnFilterPanels.forEach((column, conditionPanel) -> SwingUtilities.invokeLater(() -> {
      if (filterPanelsVisible) {
        conditionPanel.showDialog(null);
      }
      else {
        conditionPanel.hideDialog();
      }
    }));
  }

  /**
   * Shows a dialog for selecting which columns to show/hide
   */
  public void selectColumns() {
    final SelectColumnsPanel<C> selectColumnsPanel = new SelectColumnsPanel<>(tableModel.getColumnModel());
    Dialogs.okCancelDialog(selectColumnsPanel)
            .owner(this)
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
  public boolean isCellVisible(final int row, final int column) {
    final JViewport viewport = Utilities.getParentOfType(this, JViewport.class);
    if (viewport == null) {
      return false;
    }
    final Rectangle cellRect = getCellRect(row, column, true);
    final Point viewPosition = viewport.getViewPosition();
    cellRect.setLocation(cellRect.x - viewPosition.x, cellRect.y - viewPosition.y);

    return new Rectangle(viewport.getExtentSize()).contains(cellRect);
  }

  /**
   * Scrolls horizontally so that the column identified by columnIdentifier becomes visible.
   * Has no effect if this table is not contained in a scrollpanel.
   * @param columnIdentifier the column identifier
   */
  public void scrollToColumn(final C columnIdentifier) {
    final JViewport viewport = Utilities.getParentOfType(this, JViewport.class);
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
    requireNonNull(centerOnScroll);
    final JViewport viewport = Utilities.getParentOfType(this, JViewport.class);
    if (viewport != null) {
      final Rectangle cellRectangle = getCellRect(row, column, true);
      final Rectangle viewRectangle = viewport.getViewRect();
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
  }

  /**
   * @return a control for showing the column selection dialog
   */
  public Control createSelectColumnsControl() {
    return Control.builder(this::selectColumns)
            .caption(MESSAGES.getString(SELECT_COLUMNS) + "...")
            .enabledState(tableModel.getColumnModel().getLockedState().getReversedObserver())
            .description(MESSAGES.getString(SELECT_COLUMNS))
            .build();
  }

  /**
   * @return Controls containing {@link ToggleControl}s for showing/hiding columns.
   */
  public Controls createToggleColumnsControls() {
    return Controls.builder()
            .caption(MESSAGES.getString(SELECT_COLUMNS))
            .enabledState(tableModel.getColumnModel().getLockedState().getReversedObserver())
            .description(MESSAGES.getString(SELECT_COLUMNS))
            .controls(tableModel.getColumnModel().getAllColumns().stream()
                    .sorted(new ColumnComparator())
                    .map(this::createToggleColumnControl)
                    .toArray(ToggleControl[]::new))
            .build();
  }

  /**
   * @return a Control for resetting the columns to their original location and visibility
   */
  public Control createResetColumnsControl() {
    return Control.builder(getModel().getColumnModel()::resetColumns)
            .caption(MESSAGES.getString(RESET_COLUMNS))
            .enabledState(tableModel.getColumnModel().getLockedState().getReversedObserver())
            .description(MESSAGES.getString(RESET_COLUMNS_DESCRIPTION))
            .build();
  }

  /**
   * @return a ToggleControl for toggling the table selection mode (single or multiple)
   */
  public ToggleControl createSingleSelectionModeControl() {
    return ToggleControl.builder(tableModel.getSelectionModel().getSingleSelectionModeState())
            .caption(MESSAGES.getString(SINGLE_SELECTION_MODE))
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
   * @param listener a listener notified each time the table is double-clicked
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
    final Value<String> searchString = Value.value();
    final Control findNext = control(() -> findNext(searchString.get()));
    final Control findAndSelectNext = control(() -> findAndSelectNext(searchString.get()));
    final Control findPrevious = control(() -> findPrevious(searchString.get()));
    final Control findAndSelectPrevious = control(() -> findAndSelectPrevious(searchString.get()));
    final Control cancel = control(this::requestFocusInWindow);

    return Components.textField(searchString)
            .columns(SEARCH_FIELD_COLUMNS)
            .selectAllOnFocusGained(true)
            .keyEvent(KeyEvents.builder(KeyEvent.VK_ENTER)
                    .onKeyPressed()
                    .action(findNext))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_ENTER)
                    .onKeyPressed()
                    .modifiers(InputEvent.SHIFT_DOWN_MASK)
                    .action(findAndSelectNext))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_DOWN)
                    .onKeyPressed()
                    .action(findNext))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_DOWN)
                    .onKeyPressed()
                    .modifiers(InputEvent.SHIFT_DOWN_MASK)
                    .action(findAndSelectNext))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_UP)
                    .onKeyPressed()
                    .action(findPrevious))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_UP)
                    .onKeyPressed()
                    .modifiers(InputEvent.SHIFT_DOWN_MASK)
                    .action(findAndSelectPrevious))
            .keyEvent(KeyEvents.builder(KeyEvent.VK_ESCAPE)
                    .onKeyPressed()
                    .action(cancel))
            .popupMenuControls(getSearchFieldPopupMenuControls())
            .build();
  }

  private TextFieldHint initializeSearchFieldHint() {
    final TextFieldHint textFieldHint = TextFieldHint.create(searchField, Messages.get(Messages.SEARCH_FIELD_HINT));
    searchField.getDocument().addDocumentListener((DocumentAdapter) e -> {
      if (!textFieldHint.isHintVisible()) {
        performSearch(false, lastSearchResultCoordinate.getRow() == -1 ? 0 :
                lastSearchResultCoordinate.getRow(), true, searchField.getText());
      }
    });

    return textFieldHint;
  }

  private void performSearch(final boolean addToSelection, final int fromIndex, final boolean forward, final String searchText) {
    if (!searchText.isEmpty()) {
      final RowColumn coordinate = (forward ?
              tableModel.findNext(fromIndex, searchText) :
              tableModel.findPrevious(fromIndex, searchText))
              .orElse(null);
      if (coordinate != null) {
        lastSearchResultCoordinate = coordinate;
        if (addToSelection) {
          tableModel.getSelectionModel().addSelectedIndex(coordinate.getRow());
        }
        else {
          tableModel.getSelectionModel().setSelectedIndex(coordinate.getRow());
          setColumnSelectionInterval(coordinate.getColumn(), coordinate.getColumn());
        }
        scrollToCoordinate(coordinate.getRow(), coordinate.getColumn(), centerOnScroll);
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

  private Controls getSearchFieldPopupMenuControls() {
    return Controls.builder()
            .control(ToggleControl.builder(tableModel.getCaseSensitiveSearchState())
                    .caption(MESSAGES.getString("case_sensitive_search")))
            .controls(ToggleControl.builder(tableModel.getRegularExpressionSearchState())
                    .caption(MESSAGES.getString("regular_expression_search")))
            .build();
  }

  private void toggleColumnFilterPanel(final MouseEvent event) {
    final SwingFilteredTableColumnModel<C> columnModel = getModel().getColumnModel();
    final TableColumn column = columnModel.getColumn(columnModel.getColumnIndexAtX(event.getX()));
    toggleFilterPanel(columnFilterPanels.computeIfAbsent(column, c ->
                    (ColumnConditionPanel<C, ?>) conditionPanelFactory.createConditionPanel(column)),
            this, column.getHeaderValue().toString(), event.getLocationOnScreen());
  }

  private static void toggleFilterPanel(final ColumnConditionPanel<?, ?> columnFilterPanel, final Container parent,
                                        final String title, final Point position) {
    if (columnFilterPanel != null) {
      if (!columnFilterPanel.isDialogEnabled()) {
        columnFilterPanel.enableDialog(parent, title);
      }
      if (columnFilterPanel.isDialogVisible()) {
        columnFilterPanel.hideDialog();
      }
      else {
        columnFilterPanel.showDialog(position);
      }
    }
  }

  private ToggleControl createToggleColumnControl(final TableColumn column) {
    final C identifier = (C) column.getIdentifier();
    final State visibleState = State.state(tableModel.getColumnModel().isColumnVisible(identifier));
    visibleState.addDataListener(visible -> tableModel.getColumnModel().setColumnVisible(identifier, visible));

    return ToggleControl.builder(visibleState)
            .caption(column.getHeaderValue().toString())
            .build();
  }

  private void initializeTableHeader() {
    getTableHeader().addMouseListener(new MouseColumnFilterPanelHandler());
    tableModel.addSortListener(getTableHeader()::repaint);
    getTableHeader().setReorderingAllowed(true);
    getTableHeader().setAutoscrolls(true);
    getTableHeader().addMouseMotionListener(new MouseColumnDragHandler());
    getTableHeader().addMouseListener(new MouseSortHandler());
    getTableHeader().setDefaultRenderer(new SortableHeaderRenderer(getTableHeader().getDefaultRenderer()));
  }

  private void bindEvents() {
    addMouseListener(initializeTableMouseListener());
    tableModel.getSelectionModel().addSelectedIndexListener(selected -> {
      if (scrollToSelectedItem && !tableModel.getSelectionModel().isSelectionEmpty()) {
        scrollToCoordinate(selected, getSelectedColumn(), centerOnScroll);
      }
    });
    tableModel.getColumnModel().getAllColumns().forEach(this::bindFilterIndicatorEvents);
    addKeyListener(new MoveResizeColumnKeyListener());
  }

  private void bindFilterIndicatorEvents(final TableColumn column) {
    final ColumnFilterModel<R, C, Object> model = (ColumnFilterModel<R, C, Object>) getModel().getColumnFilterModels().get(column.getIdentifier());
    if (model != null) {
      model.addEnabledListener(() -> getTableHeader().repaint());
    }
  }

  /**
   * Initialize the MouseListener for the table component handling double click.
   * Double-clicking invokes the action returned by {@link #getDoubleClickAction()}
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

  private static final class DefaultConditionPanelFactory<C> implements ConditionPanelFactory {

    private final FilteredTableModel<?, C> tableModel;

    private DefaultConditionPanelFactory(final FilteredTableModel<?, C> tableModel) {
      this.tableModel = tableModel;
    }

    @Override
    public <T> ColumnConditionPanel<C, T> createConditionPanel(final TableColumn column) {
      final ColumnFilterModel<?, C, Object> filterModel = (ColumnFilterModel<?, C, Object>) tableModel.getColumnFilterModels().get(column.getIdentifier());
      if (filterModel == null) {
        return null;
      }

      return new ColumnConditionPanel<>((ColumnConditionModel<C, T>) filterModel, ToggleAdvancedButton.YES);
    }
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
      final Font defaultFont = component.getFont();
      if (component instanceof JLabel) {
        final JLabel label = (JLabel) component;
        final TableColumn tableColumn = table.getColumnModel().getColumn(column);
        final ColumnFilterModel<R, C, ?> filterModel = tableModel.getColumnFilterModels().get(tableColumn.getIdentifier());
        label.setFont((filterModel != null && filterModel.isEnabled()) ? defaultFont.deriveFont(Font.ITALIC) : defaultFont);
        label.setHorizontalTextPosition(SwingConstants.LEFT);
        label.setIcon(getHeaderRendererIcon((C) tableColumn.getIdentifier(), label.getFont().getSize() + SORT_ICON_SIZE));
      }

      return component;
    }

    private Icon getHeaderRendererIcon(final C columnIdentifier, final int iconSizePixels) {
      final SortOrder sortOrder = tableModel.getSortModel().getSortingState(columnIdentifier).getSortOrder();
      if (sortOrder == SortOrder.UNSORTED) {
        return null;
      }

      return new Arrow(sortOrder == SortOrder.DESCENDING, iconSizePixels,
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
        if (!getSelectionModel().isSelectionEmpty()) {
          setColumnSelectionInterval(index, index);//otherwise, the focus jumps to the selected column after sorting
        }
        final C columnIdentifier = (C) columnModel.getColumn(index).getIdentifier();
        final TableSortModel<R, C> sortModel = getModel().getSortModel();
        final SortOrder sortOrder = getSortOrder(sortModel.getSortingState(columnIdentifier).getSortOrder(), e.isShiftDown());
        if (e.isControlDown()) {
          sortModel.addSortOrder(columnIdentifier, sortOrder);
        }
        else {
          sortModel.setSortOrder(columnIdentifier, sortOrder);
        }
      }
    }

    private SortOrder getSortOrder(final SortOrder currentSortOrder, final boolean isShiftDown) {
      switch (currentSortOrder) {
        case UNSORTED:
          return isShiftDown ? SortOrder.DESCENDING : SortOrder.ASCENDING;
        case ASCENDING:
          return SortOrder.DESCENDING;
        default://case DESCENDING:
          return SortOrder.ASCENDING;
      }
    }
  }

  private final class MouseColumnFilterPanelHandler extends MouseAdapter {
    @Override
    public void mouseClicked(final MouseEvent e) {
      if (e.isAltDown() && e.isControlDown()) {
        toggleColumnFilterPanel(e);
      }
    }
  }

  private final class MouseColumnDragHandler extends MouseMotionAdapter {
    @Override
    public void mouseDragged(final MouseEvent e) {
      scrollRectToVisible(new Rectangle(e.getX(), e.getY(), 1, 1));
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

  static final class ColumnComparator implements Comparator<TableColumn> {

    private final Collator columnCollator = Collator.getInstance();

    @Override
    public int compare(final TableColumn col1, final TableColumn col2) {
      return Text.collateSansSpaces(columnCollator, col1.getHeaderValue().toString(), col2.getHeaderValue().toString());
    }
  }
}
