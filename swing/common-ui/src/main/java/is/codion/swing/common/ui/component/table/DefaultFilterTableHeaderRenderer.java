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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.component.table.FilterTableSort.ColumnSort;
import is.codion.common.model.component.table.FilterTableSort.ColumnSortOrder;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditions;
import is.codion.common.model.filter.SortOrder;
import is.codion.swing.common.ui.component.table.FilterTableSearchModel.RowColumn;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import static is.codion.swing.common.ui.color.Colors.shade;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createCompoundBorder;

final class DefaultFilterTableHeaderRenderer<R, C> implements FilterTableHeaderRenderer {

	private static final int SORT_ICON_SIZE = 5;
	private static final double FOCUSED_COLUMN_SHADE = 0.2;
	private static final double SORT_ARROW_SHADE = 0.6;

	static final Factory<?, ?> FACTORY = new DefaultFactory<>();

	private final TableConditions<C> filters;
	private final ColumnSort<C> columnSort;
	private final FilterTableColumn<C> tableColumn;
	private final TableCellRenderer columnCellRenderer;
	private final boolean columnToolTips;
	private final boolean focusedColumnIndicator = FOCUSED_COLUMN_INDICATOR.getOrThrow();

	private DefaultFilterTableHeaderRenderer(FilterTable<R, C> table, C identifier) {
		this.filters = table.model().filters();
		this.columnSort = table.model().sort().columns();
		this.tableColumn = table.columns().get(identifier);
		this.columnCellRenderer = tableColumn.getCellRenderer();
		this.columnToolTips = table.columnToolTips;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
																								 boolean hasFocus, int row, int column) {
		Component component = table.getTableHeader()
						.getDefaultRenderer()
						.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (component instanceof JComponent && columnToolTips) {
			((JComponent) component).setToolTipText(tableColumn.toolTipText().orElse(null));
		}
		if (component instanceof JLabel) {
			Font defaultFont = component.getFont();
			JLabel label = (JLabel) component;
			ConditionModel<?> filterModel = filters.get().get(tableColumn.identifier());
			label.setFont((filterModel != null && filterModel.enabled().is()) ? defaultFont.deriveFont(Font.ITALIC) : defaultFont);
			label.setIcon(sortArrowIcon(tableColumn.identifier(), label.getFont().getSize() + SORT_ICON_SIZE));
			label.setIconTextGap(0);
			if (columnCellRenderer instanceof JLabel) {
				label.setHorizontalAlignment(((JLabel) columnCellRenderer).getHorizontalAlignment());
			}
			else if (columnCellRenderer instanceof AbstractButton) {
				label.setHorizontalAlignment(((AbstractButton) columnCellRenderer).getHorizontalAlignment());
			}
			if (columnCellRenderer instanceof DefaultFilterTableCellRenderer) {
				Border tableCellBorder = ((DefaultFilterTableCellRenderer<?, ?, ?>) columnCellRenderer).cellBorder();
				label.setBorder(label.getBorder() == null ? tableCellBorder : createCompoundBorder(label.getBorder(), tableCellBorder));
			}
			if (focusedColumnIndicator) {
				RowColumn currentSearchResult = ((FilterTable<R, C>) table).search().results().current().get();
				if (currentSearchResult != null && column == currentSearchResult.column()) {
					label.setBackground(shade(label.getBackground(), FOCUSED_COLUMN_SHADE));
				}
				if (!table.getSelectionModel().isSelectionEmpty() && column == table.getColumnModel().getSelectionModel().getLeadSelectionIndex()) {
					label.setBackground(shade(label.getBackground(), FOCUSED_COLUMN_SHADE));
				}
			}
		}

		return component;
	}

	private @Nullable Icon sortArrowIcon(C identifier, int iconSizePixels) {
		ColumnSortOrder<C> columnSortOrder = columnSort.get(identifier);

		return columnSortOrder.sortOrder() == SortOrder.UNSORTED ?
						null : new Arrow(columnSortOrder.sortOrder() == SortOrder.DESCENDING, iconSizePixels, columnSortOrder.priority());
	}

	private static final class Arrow implements Icon {

		private static final double PRIORITY_SIZE_RATIO = 0.8;
		private static final double PRIORITY_SIZE_CONST = 2.0;

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
			// In a compound sort, make each successive triangle 20% smaller than the previous one.
			int arrowSize = (int) (size / PRIORITY_SIZE_CONST * Math.pow(PRIORITY_SIZE_RATIO, priority));
			// The primary arrow is centered vertically, the smaller ones aligned with its bottom
			int bottom = y + (size + (int) (size / PRIORITY_SIZE_CONST)) / 2;
			int top = bottom - arrowSize;
			int base = descending ? top : bottom;
			int apex = descending ? bottom : top;
			int[] xPoints = {x, x + arrowSize, x + arrowSize / 2};
			int[] yPoints = {base, base, apex};
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
				g2.setColor(c == null ? Color.GRAY : shade(c.getBackground(), SORT_ARROW_SHADE));
				g2.fillPolygon(xPoints, yPoints, xPoints.length);
			}
			finally {
				g2.dispose();
			}
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

	private static class DefaultFactory<R, C> implements Factory<R, C> {

		@Override
		public FilterTableHeaderRenderer create(C identifier, FilterTable<R, C> table) {
			return new DefaultFilterTableHeaderRenderer<>(requireNonNull(table), requireNonNull(identifier));
		}
	}
}
