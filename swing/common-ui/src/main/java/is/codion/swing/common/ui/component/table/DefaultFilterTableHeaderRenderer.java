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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.swing.common.model.component.table.FilterTableSort.ColumnSort;
import is.codion.swing.common.model.component.table.FilterTableSort.ColumnSortOrder;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

import static is.codion.swing.common.ui.color.Colors.darker;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createCompoundBorder;

final class DefaultFilterTableHeaderRenderer<R, C> implements FilterTableHeaderRenderer {

	private static final int SORT_ICON_SIZE = 5;
	private static final double FOCUSED_COLUMN_DARKENING_FACTOR = 0.8;

	static final Factory<?, ?> FACTORY = new DefaultFactory<>();

	private final TableConditionModel<C> filters;
	private final ColumnSort<C> columnSort;
	private final FilterTableColumn<C> tableColumn;
	private final TableCellRenderer columnCellRenderer;
	private final boolean columnToolTips;
	private final boolean focusedColumnIndicator = FOCUSED_COLUMN_INDICATOR.getOrThrow();

	private DefaultFilterTableHeaderRenderer(FilterTable<R, C> table, C identifier) {
		this.filters = table.model().filters();
		this.columnSort = table.model().sort().columns();
		this.tableColumn = table.columnModel().column(identifier);
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
			if (focusedColumnIndicator && !table.getSelectionModel().isSelectionEmpty()) {
				int selectedColumn = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
				if (column == selectedColumn) {
					label.setBackground(darker(label.getBackground(), FOCUSED_COLUMN_DARKENING_FACTOR));
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

	private static class DefaultFactory<R, C> implements Factory<R, C> {

		@Override
		public FilterTableHeaderRenderer create(C identifier, FilterTable<R, C> table) {
			return new DefaultFilterTableHeaderRenderer<>(requireNonNull(table), requireNonNull(identifier));
		}
	}
}
