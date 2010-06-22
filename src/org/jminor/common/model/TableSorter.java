/*
 * Copyright (c) 1995 - 2008 Sun Microsystems, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jminor.common.model;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * TableSorter is a decorator for TableModels; adding sorting
 * functionality to a supplied TableModel. TableSorter does
 * not store or copy the data in its TableModel; instead it maintains
 * a map from the row indexes of the view to the row indexes of the
 * model. As requests are made of the sorter (like getValueAt(row, col))
 * they are passed to the underlying model after the row numbers
 * have been translated via the internal mapping array. This way,
 * the TableSorter appears to hold another copy of the table
 * with the rows in a different order.
 * <p/>
 * TableSorter registers itself as a listener to the underlying model,
 * just as the JTable itself would. Events recieved from the model
 * are examined, sometimes manipulated (typically widened), and then
 * passed on to the TableSorter's listeners (typically the JTable).
 * If a change to the model has invalidated the order of TableSorter's
 * rows, a note of this is made and the sorter will resort the
 * rows the next time a value is requested.
 * <p/>
 * When the tableHeader property is set, either by using the
 * setTableHeader() method or the two argument constructor, the
 * table header may be used as a complete UI for TableSorter.
 * The default renderer of the tableHeader is decorated with a renderer
 * that indicates the sorting status of each column. In addition,
 * a mouse listener is installed with the following behavior:
 * <ul>
 * <li>
 * Mouse-click: Clears the sorting status of all other columns
 * and advances the sorting status of that column through three
 * values: {NOT_SORTED, ASCENDING, DESCENDING} (then back to
 * NOT_SORTED again).
 * <li>
 * SHIFT-mouse-click: Clears the sorting status of all other columns
 * and cycles the sorting status of the column through the same
 * three values, in the opposite order: {NOT_SORTED, DESCENDING, ASCENDING}.
 * <li>
 * CONTROL-mouse-click and CONTROL-SHIFT-mouse-click: as above except
 * that the changes to the column do not cancel the statuses of columns
 * that are already sorting - giving a way to initiate a compound
 * sort.
 * </ul>
 * <p/>
 * This is a long overdue rewrite of a class of the same name that
 * first appeared in the swing table demos in 1997.
 *
 * @author Philip Milne
 * @author Brendon McLean
 * @author Dan van Enckevort
 * @author Parwinder Sekhon
 * @version 2.0 02/27/04
 */

@SuppressWarnings({"unchecked"})
public class TableSorter extends AbstractTableModel {
  private TableModel tableModel;

  public static final int DESCENDING = -1;
  public static final int NOT_SORTED = 0;
  public static final int ASCENDING = 1;

  private final Event evtBeforeSort = new Event();
  private final Event evtAfterSort = new Event();

  public static final Comparator COMPARABLE_COMPARATOR = new Comparator() {
    public int compare(Object o1, Object o2) {
      return ((Comparable) o1).compareTo(o2);
    }
  };
  public static final Comparator LEXICAL_COMPARATOR = new Comparator() {
    private final Collator collator = Collator.getInstance();
    public int compare(Object o1, Object o2) {
      return collator.compare(o1.toString(), o2.toString());
    }
  };

  private Row[] viewToModel;
  private int[] modelToView;

  private JTableHeader tableHeader;

  private final MouseListener mouseListener;
  private final TableModelListener tableModelListener;
  private final List<Directive> sortingColumns = new ArrayList<Directive>();

  private static final Directive EMPTY_DIRECTIVE = new Directive(-1, NOT_SORTED);

  /** Constructs a new TableSorter. */
  public TableSorter() {
    this.mouseListener = new MouseHandler();
    this.tableModelListener = new TableModelHandler();
  }

  public TableSorter(TableModel tableModel) {
    this();
    setTableModel(tableModel);
  }

  public TableSorter(TableModel tableModel, JTableHeader tableHeader) {
    this();
    setTableHeader(tableHeader);
    setTableModel(tableModel);
  }

  /**
   * @return the table model
   */
  public TableModel getTableModel() {
    return tableModel;
  }

  /**
   * @param tableModel the TableModel instance this TableSorter sort
   */
  public void setTableModel(TableModel tableModel) {
    if (this.tableModel != null) {
      this.tableModel.removeTableModelListener(tableModelListener);
    }

    this.tableModel = tableModel;
    if (this.tableModel != null) {
      this.tableModel.addTableModelListener(tableModelListener);
    }

    clearSortingState();
    fireTableStructureChanged();
  }

  /**
   * @return the table header
   */
  public JTableHeader getTableHeader() {
    return tableHeader;
  }

  /**
   * @param tableHeader the JTableHeader this TableSorter instance should use
   */
  public void setTableHeader(JTableHeader tableHeader) {
    if (this.tableHeader != null) {
      this.tableHeader.removeMouseListener(mouseListener);
      TableCellRenderer defaultRenderer = this.tableHeader.getDefaultRenderer();
      if (defaultRenderer instanceof SortableHeaderRenderer) {
        this.tableHeader.setDefaultRenderer(((SortableHeaderRenderer) defaultRenderer).tableCellRenderer);
      }
    }
    this.tableHeader = tableHeader;
    if (this.tableHeader != null) {
      this.tableHeader.addMouseListener(mouseListener);
      this.tableHeader.setDefaultRenderer(new SortableHeaderRenderer(this.tableHeader.getDefaultRenderer()));
    }
  }

  public boolean isSorting() {
    return sortingColumns.size() != 0;
  }

  public int getSortingStatus(int column) {
    return getDirective(column).direction;
  }

  public void setSortingStatus(int column, int status) {
    Directive directive = getDirective(column);
    if (!directive.equals(EMPTY_DIRECTIVE)) {
      sortingColumns.remove(directive);
    }
    if (status != NOT_SORTED) {
      sortingColumns.add(new Directive(column, status));
    }
    sortingStatusChanged();
  }

  public int modelIndex(int viewIndex) {
    final Row[] model = getViewToModel();
    if (model != null && model.length > 0 && viewIndex >= 0 && viewIndex < model.length) {
      return model[viewIndex].modelIndex;
    }

    return -1;
  }

  public int viewIndex(int modelIndex) {
    final int[] view = getModelToView();
    if (view != null && view.length > 0 && modelIndex >= 0 && modelIndex < view.length) {
      return view[modelIndex];
    }

    return -1;
  }

  // TableModel interface methods

  /** {@inheritDoc} */
  public int getRowCount() {
    return (tableModel == null) ? 0 : tableModel.getRowCount();
  }

  /** {@inheritDoc} */
  public int getColumnCount() {
    return (tableModel == null) ? 0 : tableModel.getColumnCount();
  }

  /** {@inheritDoc} */
  @Override
  public String getColumnName(int column) {
    return tableModel.getColumnName(column);
  }

  /** {@inheritDoc} */
  @Override
  public Class getColumnClass(int columnIndex) {
    return tableModel.getColumnClass(columnIndex);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return tableModel.isCellEditable(modelIndex(rowIndex), columnIndex);
  }

  /** {@inheritDoc} */
  public Object getValueAt(int rowIndex, int columnIndex) {
    return tableModel.getValueAt(modelIndex(rowIndex), columnIndex);
  }

  /** {@inheritDoc} */
  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    tableModel.setValueAt(aValue, modelIndex(rowIndex), columnIndex);
  }

  /**
   * @return an Event fired after a sort has been performed
   */
  public Event eventAfterSort() {
    return evtAfterSort;
  }

  /**
   * @return an Event fired before a sort is performed
   */
  public Event eventBeforeSort() {
    return evtBeforeSort;
  }

  protected Icon getHeaderRendererIcon(int column, int size) {
    Directive directive = getDirective(column);
    if (directive == EMPTY_DIRECTIVE) {
      return null;
    }
    return new Arrow(directive.direction == DESCENDING, size, sortingColumns.indexOf(directive));
  }

  protected Comparator getComparator(int column) {
    final Class columnClass = tableModel.getColumnClass(column);
    if (columnClass.equals(String.class)) {
      return LEXICAL_COMPARATOR;
    }
    if (Comparable.class.isAssignableFrom(columnClass)) {
      return COMPARABLE_COMPARATOR;
    }

    return LEXICAL_COMPARATOR;
  }

  private Directive getDirective(int column) {
    for (final Directive sortingColumn : sortingColumns) {
      if (sortingColumn.column == column) {
        return sortingColumn;
      }
    }
    return EMPTY_DIRECTIVE;
  }

  private void sortingStatusChanged() {
    evtBeforeSort.fire();
    clearSortingState();
    fireTableDataChanged();
    evtAfterSort.fire();
    if (tableHeader != null) {
      tableHeader.repaint();
    }
  }

  private Row[] getViewToModel() {
    if (viewToModel == null) {
      int tableModelRowCount = tableModel.getRowCount();
      viewToModel = new Row[tableModelRowCount];
      for (int row = 0; row < tableModelRowCount; row++) {
        viewToModel[row] = new Row(row);
      }

      if (isSorting()) {
        Arrays.sort(viewToModel);
      }
    }
    return viewToModel;
  }

  private void cancelSorting() {
    sortingColumns.clear();
    sortingStatusChanged();
  }

  private int[] getModelToView() {
    if (modelToView == null) {
      int n = getViewToModel().length;
      modelToView = new int[n];
      for (int i = 0; i < n; i++) {
        modelToView[modelIndex(i)] = i;
      }
    }
    return modelToView;
  }

  private void clearSortingState() {
    viewToModel = null;
    modelToView = null;
  }

  // Helper classes

  private class Row implements Comparable {
    private final int modelIndex;

    Row(int index) {
      this.modelIndex = index;
    }

    public int compareTo(Object o) {
      int rowOne = modelIndex;
      int rowTwo = ((Row) o).modelIndex;

      for (final Directive directive : sortingColumns) {
        int column = directive.column;
        Object objectOne = tableModel.getValueAt(rowOne, column);
        Object objectTwo = tableModel.getValueAt(rowTwo, column);

        int comparison;
        // Define null less than everything, except null.
        if (objectOne == null && objectTwo == null) {
          comparison = 0;
        }
        else if (objectOne == null) {
          comparison = -1;
        }
        else if (objectTwo == null) {
          comparison = 1;
        }
        else {
          comparison = getComparator(column).compare(objectOne, objectTwo);
        }
        if (comparison != 0) {
          return directive.direction == DESCENDING ? -comparison : comparison;
        }
      }
      return 0;
    }
  }

  private class TableModelHandler implements TableModelListener {
    public void tableChanged(TableModelEvent e) {
      // If we're not sorting by anything, just pass the event along.
      if (!isSorting()) {
        clearSortingState();
        fireTableChanged(e);
        return;
      }

      // If the table structure has changed, cancel the sorting; the
      // sorting columns may have been either moved or deleted from
      // the model.
      if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
        cancelSorting();
        fireTableChanged(e);
        return;
      }

      // We can map a cell event through to the view without widening
      // when the following conditions apply:
      //
      // a) all the changes are on one row (e.getFirstRow() == e.getLastRow()) and,
      // b) all the changes are in one column (column != TableModelEvent.ALL_COLUMNS) and,
      // c) we are not sorting on that column (getSortingStatus(column) == NOT_SORTED) and,
      // d) a reverse lookup will not trigger a sort (modelToView != null)
      //
      // Note: INSERT and DELETE events fail this test as they have column == ALL_COLUMNS.
      //
      // The last check, for (modelToView != null) is to see if modelToView
      // is already allocated. If we don't do this check; sorting can become
      // a performance bottleneck for applications where cells
      // change rapidly in different parts of the table. If cells
      // change alternately in the sorting column and then outside of
      // it this class can end up re-sorting on alternate cell updates -
      // which can be a performance problem for large tables. The last
      // clause avoids this problem.
      int column = e.getColumn();
      if (e.getFirstRow() == e.getLastRow()
              && column != TableModelEvent.ALL_COLUMNS
              && getSortingStatus(column) == NOT_SORTED
              && modelToView != null) {
        int viewIndex = getModelToView()[e.getFirstRow()];
        fireTableChanged(new TableModelEvent(TableSorter.this,
                viewIndex, viewIndex,
                column, e.getType()));
        return;
      }

      // Something has happened to the data that may have invalidated the row order.
      clearSortingState();
      fireTableDataChanged();
    }
  }

  private class MouseHandler extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      if (e.getButton() != MouseEvent.BUTTON1) {
        return;
      }

      JTableHeader h = (JTableHeader) e.getSource();
      TableColumnModel columnModel = h.getColumnModel();
      int viewColumn = columnModel.getColumnIndexAtX(e.getX());
      int column;
      try {
        column = columnModel.getColumn(viewColumn).getModelIndex();
      }
      catch (ArrayIndexOutOfBoundsException ex) {
        return;
      }
      if (column != -1) {
        int status = getSortingStatus(column);
        if (!e.isControlDown()) {
          cancelSorting();
        }
        // Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or
        // {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed.
        status = status + (e.isShiftDown() ? -1 : 1);
        status = (status + 4) % 3 - 1; // signed mod, returning {-1, 0, 1}
        setSortingStatus(column, status);
      }
    }
  }

  private static class Arrow implements Icon {
    private boolean descending;
    private int size;
    private int priority;

    private Arrow(boolean descending, int size, int priority) {
      this.descending = descending;
      this.size = size;
      this.priority = priority;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
      Color color = c == null ? Color.GRAY : c.getBackground();
      // In a compound sort, make each succesive triangle 20%
      // smaller than the previous one.
      int dx = (int)(size/2*Math.pow(0.8, priority));
      int dy = descending ? dx : -dx;
      // Align icon (roughly) with font baseline.
      y = y + 5*size/6 + (descending ? -dy : 0);
      int shift = descending ? 1 : -1;
      g.translate(x, y);

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
      } else {
        g.setColor(color.brighter().brighter());
      }
      g.drawLine(dx, 0, 0, 0);

      g.setColor(color);
      g.translate(-x, -y);
    }

    public int getIconWidth() {
      return size;
    }

    public int getIconHeight() {
      return size;
    }
  }

  private class SortableHeaderRenderer implements TableCellRenderer {
    private TableCellRenderer tableCellRenderer;

    private SortableHeaderRenderer(TableCellRenderer tableCellRenderer) {
      this.tableCellRenderer = tableCellRenderer;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
      Component c = tableCellRenderer.getTableCellRendererComponent(table,
              value, isSelected, hasFocus, row, column);
      if (c instanceof JLabel) {
        JLabel l = (JLabel) c;
        l.setHorizontalTextPosition(JLabel.LEFT);
        int modelColumn = table.convertColumnIndexToModel(column);
        l.setIcon(getHeaderRendererIcon(modelColumn, l.getFont().getSize()));
      }
      return c;
    }
  }

  private static class Directive {
    private int column;
    private int direction;

    private Directive(int column, int direction) {
      this.column = column;
      this.direction = direction;
    }
  }
}
