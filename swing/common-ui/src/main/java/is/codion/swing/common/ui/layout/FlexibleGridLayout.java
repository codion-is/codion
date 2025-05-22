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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.Arrays;
import java.util.function.Function;

/**
 * A layout manager similar to GridLayout, but allows components to maintain their preferred size.
 * Rows and columns can optionally be normalized to the largest size in their dimension.
 * Designed in Codion style with a fluent builder API.
 * <p>
 * Features:
 * - Optional fixed row heights or column widths.
 * - Custom horizontal and vertical gaps.
 * - Maintains preferred component sizes when not fixed.
 * - Safe for variable component counts and nested containers.
 * <p>
 * Author: Björn Darri Sigurðsson (with help)
 */
public final class FlexibleGridLayout implements LayoutManager2 {

	private final int rows;
	private final int columns;
	private final int horizontalGap;
	private final int verticalGap;
	private final boolean fixRowHeights;
	private final boolean fixColumnWidths;
	private final Integer fixedRowHeight;
	private final Integer fixedColumnWidth;

	private FlexibleGridLayout(DefaultBuilder defaultBuilder) {
		this.rows = defaultBuilder.rows;
		this.columns = defaultBuilder.columns;
		this.horizontalGap = defaultBuilder.horizontalGap;
		this.verticalGap = defaultBuilder.verticalGap;
		this.fixRowHeights = defaultBuilder.fixRowHeights;
		this.fixColumnWidths = defaultBuilder.fixColumnWidths;
		this.fixedRowHeight = defaultBuilder.fixedRowHeight;
		this.fixedColumnWidth = defaultBuilder.fixedColumnWidth;
	}

	/**
	 * Returns a new builder instance for FlexibleGridLayout.
	 * @return a builder
	 */
	public static Builder builder() {
		return new DefaultBuilder();
	}

	@Override
	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			int componentCount = parent.getComponentCount();
			if (componentCount == 0) {
				return;
			}

			Insets insets = parent.getInsets();
			int effectiveCols = columns > 0
							? columns
							: (rows > 0
							? Math.max(1, (componentCount + rows - 1) / rows)
							: 1);
			int effectiveRows = rows > 0
							? rows
							: Math.max(1, (componentCount + effectiveCols - 1) / effectiveCols);

			int[] rowHeights = new int[effectiveRows];
			int[] colWidths = new int[effectiveCols];
			Dimension[] sizes = new Dimension[componentCount];

			for (int i = 0; i < componentCount; i++) {
				Component c = parent.getComponent(i);
				sizes[i] = c.getPreferredSize();
				int row = i / effectiveCols;
				int col = i % effectiveCols;
				if (row < rowHeights.length && col < colWidths.length) {
					rowHeights[row] = Math.max(rowHeights[row], sizes[i].height);
					colWidths[col] = Math.max(colWidths[col], sizes[i].width);
				}
			}

			adjustFixedSizes(rowHeights, colWidths);

			int x = insets.left;
			for (int col = 0; col < effectiveCols; col++) {
				int y = insets.top;
				for (int row = 0; row < effectiveRows; row++) {
					int idx = row * effectiveCols + col;
					if (idx < componentCount) {
						parent.getComponent(idx).setBounds(x, y, colWidths[col], rowHeights[row]);
					}
					y += rowHeights[row] + verticalGap;
				}
				x += colWidths[col] + horizontalGap;
			}
		}
	}

	private void adjustFixedSizes(int[] rowHeights, int[] colWidths) {
		if (fixRowHeights || fixedRowHeight != null) {
			int height = (fixedRowHeight != null) ? fixedRowHeight : Arrays.stream(rowHeights).max().orElse(0);
			Arrays.fill(rowHeights, height);
		}
		if (fixColumnWidths || fixedColumnWidth != null) {
			int width = (fixedColumnWidth != null) ? fixedColumnWidth : Arrays.stream(colWidths).max().orElse(0);
			Arrays.fill(colWidths, width);
		}
	}

	private Dimension calculateLayoutSize(Container parent, Function<Component, Dimension> dimension) {
		int componentCount = parent.getComponentCount();
		if (componentCount == 0) {
			return new Dimension(0, 0);
		}

		int effectiveCols = columns > 0
						? columns
						: (rows > 0
						? Math.max(1, (componentCount + rows - 1) / rows)
						: 1);
		int effectiveRows = rows > 0
						? rows
						: Math.max(1, (componentCount + effectiveCols - 1) / effectiveCols);

		int[] rowHeights = new int[effectiveRows];
		int[] columnWidths = new int[effectiveCols];

		for (int i = 0; i < componentCount; i++) {
			Dimension d = dimension.apply(parent.getComponent(i));
			int row = i / effectiveCols;
			int column = i % effectiveCols;
			if (row < rowHeights.length && column < columnWidths.length) {
				rowHeights[row] = Math.max(rowHeights[row], d.height);
				columnWidths[column] = Math.max(columnWidths[column], d.width);
			}
		}

		adjustFixedSizes(rowHeights, columnWidths);

		Insets insets = parent.getInsets();
		int width = Arrays.stream(columnWidths).sum() + (effectiveCols - 1) * horizontalGap;
		int height = Arrays.stream(rowHeights).sum() + (effectiveRows - 1) * verticalGap;

		return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return calculateLayoutSize(parent, Component::getPreferredSize);
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return calculateLayoutSize(parent, Component::getMinimumSize);
	}

	@Override
	public void addLayoutComponent(Component comp, Object constraints) {}

	@Override
	public void addLayoutComponent(String name, Component comp) {}

	@Override
	public void removeLayoutComponent(Component comp) {}

	@Override
	public Dimension maximumLayoutSize(Container target) {
		return preferredLayoutSize(target);
	}

	@Override
	public float getLayoutAlignmentX(Container target) {
		return 0.5f;
	}

	@Override
	public float getLayoutAlignmentY(Container target) {
		return 0.5f;
	}

	@Override
	public void invalidateLayout(Container target) {}

	/**
	 * Builds a {@link FlexibleGridLayout} instances.
	 */
	public interface Builder {

		/**
		 * Sets the number of rows in the layout.
		 * If both rows and columns are zero, layout behavior is undefined.
		 * @param rows the number of rows
		 * @return this builder instance
		 */
		Builder rows(int rows);

		/**
		 * Sets the number of columns in the layout.
		 * If both rows and columns are zero, layout behavior is undefined.
		 * @param columns the number of columns
		 * @return this builder instance
		 */
		Builder columns(int columns);

		/**
		 * Sets both the number of rows and columns.
		 * @param rows the number of rows
		 * @param columns the number of columns
		 * @return this builder instance
		 */
		Builder rowsColumns(int rows, int columns);

		/**
		 * Sets both the horizontal and vertical gap between components.
		 * @param gap the number of pixels between components in both directions
		 * @return this builder instance
		 */
		Builder gap(int gap);

		/**
		 * Sets the horizontal gap between components.
		 * @param horizontalGap the number of pixels between columns
		 * @return this builder instance
		 */
		Builder horizontalGap(int horizontalGap);

		/**
		 * Sets the vertical gap between components.
		 * @param verticalGap the number of pixels between rows
		 * @return this builder instance
		 */
		Builder verticalGap(int verticalGap);

		/**
		 * Enables or disables uniform row heights based on the tallest component in each row.
		 * @param fixRowHeights true to use fixed row heights
		 * @return this builder instance
		 */
		Builder fixRowHeights(boolean fixRowHeights);

		/**
		 * Enables or disables uniform column widths based on the widest component in each column.
		 * @param fixColumnWidths true to use fixed column widths
		 * @return this builder instance
		 */
		Builder fixColumnWidths(boolean fixColumnWidths);

		/**
		 * Sets a specific fixed pixel height for all rows.
		 * Automatically implies {@link #fixRowHeights(boolean)} with true.
		 * @param fixedRowHeight the fixed row height in pixels
		 * @return this builder instance
		 */
		Builder fixedRowHeight(int fixedRowHeight);

		/**
		 * Sets a specific fixed pixel width for all columns.
		 * Automatically implies {@link #fixColumnWidths(boolean)} with true.
		 * @param fixedColumnWidth the fixed column width in pixels
		 * @return this builder instance
		 */
		Builder fixedColumnWidth(int fixedColumnWidth);

		/**
		 * Builds a new {@link FlexibleGridLayout} instance with the current configuration.
		 * @return a configured layout manager
		 */
		FlexibleGridLayout build();
	}

	private static final class DefaultBuilder implements Builder {

		private int rows = 0;
		private int columns = 0;
		private int horizontalGap = 0;
		private int verticalGap = 0;
		private boolean fixRowHeights = false;
		private boolean fixColumnWidths = false;
		private Integer fixedRowHeight = null;
		private Integer fixedColumnWidth = null;

		public Builder rows(int rows) {
			this.rows = rows;
			return this;
		}

		public Builder columns(int cols) {
			this.columns = cols;
			return this;
		}

		@Override
		public Builder rowsColumns(int rows, int columns) {
			this.rows = rows;
			this.columns = columns;
			return this;
		}

		public Builder gap(int gap) {
			this.horizontalGap = gap;
			this.verticalGap = gap;
			return this;
		}

		public Builder horizontalGap(int horizontalGap) {
			this.horizontalGap = horizontalGap;
			return this;
		}

		public Builder verticalGap(int verticalGap) {
			this.verticalGap = verticalGap;
			return this;
		}

		public Builder fixRowHeights(boolean fixRowHeights) {
			this.fixRowHeights = fixRowHeights;
			return this;
		}

		public Builder fixColumnWidths(boolean fixColumnWidths) {
			this.fixColumnWidths = fixColumnWidths;
			return this;
		}

		public Builder fixedRowHeight(int fixedRowHeight) {
			this.fixedRowHeight = fixedRowHeight;
			return this;
		}

		public Builder fixedColumnWidth(int fixedColumnWidth) {
			this.fixedColumnWidth = fixedColumnWidth;
			return this;
		}

		public FlexibleGridLayout build() {
			return new FlexibleGridLayout(this);
		}
	}
}