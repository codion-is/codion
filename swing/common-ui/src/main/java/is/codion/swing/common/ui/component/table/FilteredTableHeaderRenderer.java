/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

import static javax.swing.BorderFactory.createCompoundBorder;

final class FilteredTableHeaderRenderer<R, C> implements TableCellRenderer {

  private static final int SORT_ICON_SIZE = 5;

  private final FilteredTable<R, C> filteredTable;
  private final TableCellRenderer wrappedRenderer;
  private final TableCellRenderer columnCellRenderer;

  FilteredTableHeaderRenderer(FilteredTable<R, C> filteredTable, FilteredTableColumn<C> column) {
    this.filteredTable = filteredTable;
    this.wrappedRenderer = column.getHeaderRenderer();
    this.columnCellRenderer = column.getCellRenderer();
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                 boolean hasFocus, int row, int column) {
    Component component = wrappedRenderer == null ?
            filteredTable.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(filteredTable, value, isSelected, hasFocus, row, column) :
            wrappedRenderer.getTableCellRendererComponent(filteredTable, value, isSelected, hasFocus, row, column);
    Font defaultFont = component.getFont();
    if (component instanceof JLabel) {
      JLabel label = (JLabel) component;
      FilteredTableColumn<C> tableColumn = filteredTable.getColumnModel().getColumn(column);
      ColumnConditionModel<?, ?> filterModel = filteredTable.getModel().filterModel().conditionModels().get(tableColumn.getIdentifier());
      label.setFont((filterModel != null && filterModel.isEnabled()) ? defaultFont.deriveFont(Font.ITALIC) : defaultFont);
      label.setIcon(sortArrowIcon(tableColumn.getIdentifier(), label.getFont().getSize() + SORT_ICON_SIZE));
      label.setIconTextGap(0);
      if (columnCellRenderer instanceof DefaultTableCellRenderer) {
        label.setHorizontalAlignment(((DefaultTableCellRenderer) columnCellRenderer).getHorizontalAlignment());
      }
      if (columnCellRenderer instanceof DefaultFilteredTableCellRenderer) {
        Border tableCellBorder = ((DefaultFilteredTableCellRenderer<?, ?>) columnCellRenderer).settings().defaultCellBorder();
        label.setBorder(label.getBorder() == null ? tableCellBorder : createCompoundBorder(label.getBorder(), tableCellBorder));
      }
    }

    return component;
  }

  private Icon sortArrowIcon(C columnIdentifier, int iconSizePixels) {
    SortOrder sortOrder = filteredTable.getModel().sortModel().sortOrder(columnIdentifier);
    if (sortOrder == SortOrder.UNSORTED) {
      return null;
    }

    return new Arrow(sortOrder == SortOrder.DESCENDING, iconSizePixels,
            filteredTable.getModel().sortModel().sortPriority(columnIdentifier));
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
}
