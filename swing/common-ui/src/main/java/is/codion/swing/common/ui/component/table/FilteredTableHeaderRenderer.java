/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

final class FilteredTableHeaderRenderer<T extends FilteredTableModel<R, C>, R, C> implements TableCellRenderer {

  private static final int SORT_ICON_SIZE = 5;

  private final FilteredTable<T, R, C> filteredTable;
  private final TableCellRenderer wrappedRenderer;

  FilteredTableHeaderRenderer(FilteredTable<T, R, C> filteredTable, TableCellRenderer wrappedRenderer) {
    this.filteredTable = filteredTable;
    this.wrappedRenderer = wrappedRenderer;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                 boolean hasFocus, int row, int column) {
    Component component = wrappedRenderer == null ?
            table.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) :
            wrappedRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    Font defaultFont = component.getFont();
    if (component instanceof JLabel) {
      JLabel label = (JLabel) component;
      FilteredTableColumn<C> tableColumn = ((FilteredTableColumnModel<C>) table.getColumnModel()).getColumn(column);
      ColumnConditionModel<?, ?> filterModel = filteredTable.getModel().filterModel().conditionModels()
              .get(tableColumn.getIdentifier());
      label.setFont((filterModel != null && filterModel.isEnabled()) ? defaultFont.deriveFont(Font.ITALIC) : defaultFont);
      label.setHorizontalTextPosition(SwingConstants.LEFT);
      label.setIcon(sortArrowIcon(tableColumn.getIdentifier(), label.getFont().getSize() + SORT_ICON_SIZE));
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
