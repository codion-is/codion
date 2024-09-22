/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.condition.ColumnConditionModel;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

import static javax.swing.BorderFactory.createCompoundBorder;

final class FilterTableHeaderRenderer<R, C> implements TableCellRenderer {

	private static final int SORT_ICON_SIZE = 5;

	private final FilterTable<R, C> filterTable;
	private final TableCellRenderer wrappedRenderer;
	private final TableCellRenderer columnCellRenderer;

	FilterTableHeaderRenderer(FilterTable<R, C> filterTable, FilterTableColumn<C> column) {
		this.filterTable = filterTable;
		this.wrappedRenderer = column.getHeaderRenderer();
		this.columnCellRenderer = column.getCellRenderer();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
																								 boolean hasFocus, int row, int column) {
		Component component = wrappedRenderer == null ?
						filterTable.getTableHeader().getDefaultRenderer()
										.getTableCellRendererComponent(filterTable, value, isSelected, hasFocus, row, column) :
						wrappedRenderer.getTableCellRendererComponent(filterTable, value, isSelected, hasFocus, row, column);
		Font defaultFont = component.getFont();
		if (component instanceof JLabel) {
			JLabel label = (JLabel) component;
			FilterTableColumn<C> tableColumn = filterTable.getColumnModel().getColumn(column);
			ColumnConditionModel<?, ?> filterModel = filterTable.model().conditionModel().conditionModels().get(tableColumn.identifier());
			label.setFont((filterModel != null && filterModel.enabled().get()) ? defaultFont.deriveFont(Font.ITALIC) : defaultFont);
			label.setIcon(sortArrowIcon(tableColumn.identifier(), label.getFont().getSize() + SORT_ICON_SIZE));
			label.setIconTextGap(0);
			if (columnCellRenderer instanceof JLabel) {
				label.setHorizontalAlignment(((JLabel) columnCellRenderer).getHorizontalAlignment());
			}
			else if (columnCellRenderer instanceof AbstractButton) {
				label.setHorizontalAlignment(((AbstractButton) columnCellRenderer).getHorizontalAlignment());
			}
			if (columnCellRenderer instanceof DefaultFilterTableCellRenderer) {
				Border tableCellBorder = ((DefaultFilterTableCellRenderer<?, ?>) columnCellRenderer).settings().defaultCellBorder();
				label.setBorder(label.getBorder() == null ? tableCellBorder : createCompoundBorder(label.getBorder(), tableCellBorder));
			}
		}

		return component;
	}

	private Icon sortArrowIcon(C identifier, int iconSizePixels) {
		SortOrder sortOrder = filterTable.sortModel().sortOrder(identifier);
		if (sortOrder == SortOrder.UNSORTED) {
			return null;
		}

		return new Arrow(sortOrder == SortOrder.DESCENDING, iconSizePixels,
						filterTable.sortModel().sortPriority(identifier));
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
