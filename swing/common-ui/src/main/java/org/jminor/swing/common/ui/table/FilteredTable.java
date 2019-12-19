/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.TextUtil;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.RowColumn;
import org.jminor.common.model.table.SortingDirective;
import org.jminor.swing.common.model.DocumentAdapter;
import org.jminor.swing.common.model.table.AbstractFilteredTableModel;
import org.jminor.swing.common.model.table.SwingFilteredTableColumnModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.textfield.TextFieldHint;

import javax.swing.AbstractAction;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * A JTable implementation for {@link AbstractFilteredTableModel}.
 * @param <R> the type representing rows
 * @param <C> type type used to identify columns
 * @param <M> the table model type
 */
public final class FilteredTable<R, C, T extends AbstractFilteredTableModel<R, C>> extends JTable {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(FilteredTable.class.getName(), Locale.getDefault());

  public static final char FILTER_INDICATOR = '*';

  private static final String SELECT_COLUMNS = "select_columns";
  private static final int SELECT_COLUMNS_GRID_ROWS = 15;
  private static final int SEARCH_FIELD_COLUMNS = 8;
  private static final int SORT_ICON_SIZE = 5;
  private static final RowColumn NULL_COORDINATE = RowColumn.rowColumn(-1, -1);

  /**
   * The table model
   */
  private final T tableModel;

  /**
   * Provides filter panels
   */
  private final ColumnConditionPanelProvider<C> conditionPanelProvider;

  /**
   * the property filter panels
   */
  private final Map<TableColumn, ColumnConditionPanel<C>> columnFilterPanels = new HashMap<>();

  /**
   * The text field used for entering the search condition
   */
  private final JTextField searchField;

  /**
   * If true then sorting via the table header is enabled
   */
  private boolean sortingEnabled = true;

  /**
   * If true then the JTable scrolls to the item selected in the table model
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
            (C) column.getIdentifier()), true, true));
  }

  /**
   * Instantiates a new FilteredTable using the given model
   * @param tableModel the table model
   */
  public FilteredTable(final T tableModel, final ColumnConditionPanelProvider<C> conditionPanelProvider) {
    super(requireNonNull(tableModel, "tableModel"), tableModel.getColumnModel(),
            (ListSelectionModel) tableModel.getSelectionModel());
    this.tableModel = tableModel;
    this.conditionPanelProvider = requireNonNull(conditionPanelProvider, "conditionPanelProvider");
    this.searchField = initializeSearchField();
    initializeTableHeader();
    bindEvents();
  }

  /** {@inheritDoc} */
  @Override
  public T getModel() {
    return (T) super.getModel();
  }

  /**
   * @return the search field
   */
  public JTextField getSearchField() {
    return searchField;
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
   * Hides or shows the active filter panels for this table panel
   * @param value true if the active filter panels should be shown, false if they should be hidden
   */
  public void setFilterPanelsVisible(final boolean value) {
    columnFilterPanels.values().forEach(columnFilterPanel -> SwingUtilities.invokeLater(() -> {
      if (value) {
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
    final SwingFilteredTableColumnModel<C> columnModel = tableModel.getColumnModel();
    final List<TableColumn> allColumns = new ArrayList<>(columnModel.getAllColumns());
    allColumns.sort(new Comparator<TableColumn>() {
      private final Collator collator = Collator.getInstance();

      @Override
      public int compare(final TableColumn o1, final TableColumn o2) {
        return TextUtil.collateSansSpaces(collator, o1.getIdentifier().toString(), o2.getIdentifier().toString());
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
   * @return true if the cell with the given coordinates is visible
   * @throws IllegalStateException in case this table is not in a scrollable viewport
   */
  public boolean isCellVisible(final int row, final int column) {
    final JViewport viewport = getViewport();
    final Rectangle cellRect = getCellRect(row, column, true);
    final Point viewPosition = viewport.getViewPosition();
    cellRect.setLocation(cellRect.x - viewPosition.x, cellRect.y - viewPosition.y);

    return new Rectangle(viewport.getExtentSize()).contains(cellRect);
  }

  /**
   * Scrolls horizontally so that the column identified by columnIdentifier becomes visible, centered if possible
   * @param columnIdentifier the column identifier
   * @throws IllegalStateException in case this table is not in a scrollable viewport
   */
  public void scrollToColumn(final Object columnIdentifier) {
    final JViewport viewport = getViewport();
    scrollToCoordinate(rowAtPoint(viewport.getViewPosition()),
            getModel().getColumnModel().getColumnIndex(columnIdentifier), false, false);
  }

  /**
   * Scrolls to the given coordinate.
   * @param row the row
   * @param column the column
   * @param centerXPos if true then the selected column is positioned in the center of the table, if possible
   * @param centerYPos if true then the selected row is positioned in the center of the table, if possible
   * @throws IllegalStateException in case this table is not in a scrollable viewport
   */
  public void scrollToCoordinate(final int row, final int column, final boolean centerXPos, final boolean centerYPos) {
    final JViewport viewport = getViewport();
    final Rectangle cellRectangle = getCellRect(row, column, true);
    final Rectangle viewRectangle = viewport.getViewRect();
    cellRectangle.setLocation(cellRectangle.x - viewRectangle.x, cellRectangle.y - viewRectangle.y);
    if (centerXPos) {
      int centerX = (viewRectangle.width - cellRectangle.width) / 2;
      if (cellRectangle.x < centerX) {
        centerX = -centerX;
      }
      cellRectangle.translate(centerX, cellRectangle.y);
    }
    if (centerYPos) {
      int centerY = (viewRectangle.height - cellRectangle.height) / 2;
      if (cellRectangle.y < centerY) {
        centerY = -centerY;
      }
      cellRectangle.translate(cellRectangle.x, centerY);
    }
    viewport.scrollRectToVisible(cellRectangle);
  }

  /**
   * @return a control for showing the column selection dialog
   */
  public Control getSelectColumnsControl() {
    return Controls.control(this::selectColumns, MESSAGES.getString(SELECT_COLUMNS) + "...",
            null, MESSAGES.getString(SELECT_COLUMNS));
  }

  /**
   * Performs a text search in the underlying table model, forward relative to the last search result coordinate.
   * @param addToSelection if true then the items found are added to the selection
   * @param searchText the text to search for
   */
  public void findNext(final boolean addToSelection, final String searchText) {
    performSearch(addToSelection, lastSearchResultCoordinate.getRow() + 1, true, searchText);
  }

  /**
   * Performs a text search in the underlying table model, backwards relative to the last search result coordinate.
   * @param addToSelection if true then the items found are added to the selection
   * @param searchText the text to search for
   */
  public void findPrevious(final boolean addToSelection, final String searchText) {
    performSearch(addToSelection, lastSearchResultCoordinate.getRow() - 1, false, searchText);
  }

  private JViewport getViewport() {
    final JViewport viewport = UiUtil.getParentOfType(this, JViewport.class);
    if (viewport == null) {
      throw new IllegalStateException("Table is not contained in a JViewport");
    }

    return viewport;
  }

  /**
   * Creates a JTextField for searching through this table.
   * @return a search field
   */
  private JTextField initializeSearchField() {
    final JTextField field = new JTextField();
    field.setBackground((Color) UIManager.getLookAndFeel().getDefaults().get("TextField.inactiveBackground"));
    field.setColumns(SEARCH_FIELD_COLUMNS);
    final TextFieldHint textFieldHint = TextFieldHint.enable(field, Messages.get(Messages.SEARCH_FIELD_HINT));
    field.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void contentsChanged(final DocumentEvent e) {
        if (!textFieldHint.isHintTextVisible()) {
          performSearch(false, lastSearchResultCoordinate.getRow() == -1 ? 0 :
                  lastSearchResultCoordinate.getRow(), true, field.getText());
        }
      }
    });
    field.addKeyListener(new SearchFieldKeyListener(field));
    UiUtil.selectAllOnFocusGained(field);

    field.setComponentPopupMenu(initializeSearchFieldPopupMenu());

    return field;
  }

  private void performSearch(final boolean addToSelection, final int fromIndex, final boolean forward, final String searchText) {
    if (searchText.length() != 0) {
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
        scrollToCoordinate(coordinate.getRow(), coordinate.getColumn(), false, false);
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
    final JPanel panel = new JPanel(UiUtil.createGridLayout(1, 1));
    panel.add(boxRegexp);

    final Control control = Controls.control(() -> tableModel.setRegularExpressionSearch(boxRegexp.isSelected()),
            Messages.get(Messages.OK), null, null, Messages.get(Messages.OK_MNEMONIC).charAt(0));

    final JPopupMenu popupMenu = new JPopupMenu();
    final String settingsMessage = MESSAGES.getString("settings");
    popupMenu.add(Controls.control(() ->
            UiUtil.displayInDialog(FilteredTable.this, panel, settingsMessage, control), settingsMessage));

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
    final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    southPanel.add(new JButton(Controls.control(() -> setSelected(checkBoxes, true), MESSAGES.getString("select_all"))));
    southPanel.add(new JButton(Controls.control(() -> setSelected(checkBoxes, false), MESSAGES.getString("select_none"))));

    final JPanel base = new JPanel(UiUtil.createBorderLayout());
    base.add(new JScrollPane(togglePanel), BorderLayout.CENTER);
    base.add(southPanel, BorderLayout.SOUTH);

    return base;
  }

  private void bindFilterIndicatorEvents(final TableColumn column) {
    final ColumnConditionModel<C> model = getModel().getColumnModel().getColumnFilterModel((C) column.getIdentifier());
    if (model != null) {
      model.addConditionStateListener(() -> SwingUtilities.invokeLater(() -> {
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
    final AbstractFilteredTableModel<R, C> tableModel = getModel();
    final int index = tableModel.getColumnModel().getColumnIndexAtX(event.getX());
    final TableColumn column = tableModel.getColumnModel().getColumn(index);
    if (!columnFilterPanels.containsKey(column)) {
      columnFilterPanels.put(column, conditionPanelProvider.createColumnConditionPanel(column));
    }

    toggleFilterPanel(event.getLocationOnScreen(), columnFilterPanels.get(column), this);
  }

  private static void toggleFilterPanel(final Point position, final ColumnConditionPanel columnFilterPanel,
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
    tableModel.getSelectionModel().addSelectedIndexListener(selected -> {
      if (scrollToSelectedItem && !tableModel.getSelectionModel().isSelectionEmpty()) {
        scrollToCoordinate(selected, getSelectedColumn(), false, false);
      }
    });
    tableModel.getColumnModel().getAllColumns().forEach(this::bindFilterIndicatorEvents);
    UiUtil.addKeyEvent(this, KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK,
            new ResizeSelectedColumnAction(this, false));
    UiUtil.addKeyEvent(this, KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK,
            new ResizeSelectedColumnAction(this, true));
    UiUtil.addKeyEvent(this, KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK,
            new MoveSelectedColumnAction(this, true));
    UiUtil.addKeyEvent(this, KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK,
            new MoveSelectedColumnAction(this, false));
  }

  private static void setSelected(final List<JCheckBox> checkBoxes, final boolean selected) {
    checkBoxes.forEach(box -> SwingUtilities.invokeLater(() -> box.setSelected(selected)));
  }

  private final class SearchFieldKeyListener extends KeyAdapter {

    private final JTextField field;

    private SearchFieldKeyListener(final JTextField field) {
      this.field = field;
    }

    @Override
    public void keyReleased(final KeyEvent e) {
      if (e.getModifiersEx() != 0) {
        return;
      }
      if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_DOWN) {
        findNext(e.isShiftDown(), field.getText());
      }
      else if (e.getKeyCode() == KeyEvent.VK_UP) {
        findPrevious(e.isShiftDown(), field.getText());
      }
      else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        requestFocusInWindow();
      }
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
        SortingDirective status = getModel().getSortModel().getSortingState(columnIdentifier).getDirective();
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

        getModel().getSortModel().setSortingDirective(columnIdentifier, status, e.isControlDown());
      }
    }
  }

  /**
   * Resizes the selected table column by 10 pixels.
   */
  private static final class ResizeSelectedColumnAction extends AbstractAction {

    private static final int RESIZE_AMOUNT = 10;
    private final JTable table;
    private final boolean enlarge;

    private ResizeSelectedColumnAction(final JTable table, final boolean enlarge) {
      super("FilteredTablePanel.column" + (enlarge ? "Larger" : "Smaller"));
      this.table = table;
      this.enlarge = enlarge;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      if (table.getAutoResizeMode() == JTable.AUTO_RESIZE_OFF) {
        final int selectedColumnIndex = table.getSelectedColumn();
        if (selectedColumnIndex != -1) {
          final TableColumn column = table.getColumnModel().getColumn(selectedColumnIndex);
          column.setPreferredWidth(column.getWidth() + (enlarge ? RESIZE_AMOUNT : -RESIZE_AMOUNT));
        }
      }
    }
  }

  /**
   * Moves the selected table column by one either left or right, with wrap around
   */
  private static final class MoveSelectedColumnAction extends AbstractAction {

    private final JTable table;
    private final boolean left;

    private MoveSelectedColumnAction(final JTable table, final boolean left) {
      super("FilteredTablePanel.column" + (left ? "Left" : "Right"));
      this.table = table;
      this.left = left;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      final TableColumnModel columnModel = table.getColumnModel();
      final int selectedColumnIndex = table.getSelectedColumn();
      if (selectedColumnIndex != -1) {
        final int columnCount = columnModel.getColumnCount();
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
        table.moveColumn(selectedColumnIndex, newIndex);
        table.scrollRectToVisible(table.getCellRect(table.getSelectedRow(), newIndex, true));
      }
    }
  }
}
