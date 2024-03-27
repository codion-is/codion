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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Arrays;

/**
 * Grid Layout which allows components of different sizes.
 * @author unknown
 */
public final class FlexibleGridLayout extends GridLayout {

	private static final double ONE_POINT_O = 1.0;

	private final boolean fixedRowHeights;
	private final boolean fixedColumnWidths;

	private int fixedColumnWidth;
	private int fixedRowHeight;

	private FlexibleGridLayout(DefaultBuilder builder) {
		super(builder.rows, builder.columns, builder.horizontalGap, builder.verticalGap);
		this.fixedRowHeights = builder.fixRowHeights;
		this.fixedColumnWidths = builder.fixColumnWidths;
	}

	/**
	 * @param height the fixed row height to use in this layout
	 * @return this layout instance
	 */
	public FlexibleGridLayout setFixedRowHeight(int height) {
		fixedRowHeight = height;
		return this;
	}

	/**
	 * @param width the fixed column width to use in this layout
	 * @return this layout instance
	 */
	public FlexibleGridLayout setFixedColumnWidth(int width) {
		fixedColumnWidth = width;
		return this;
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return layoutSize(parent, true);
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return layoutSize(parent, false);
	}

	@Override
	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			Insets insets = parent.getInsets();
			int numberOfComponents = parent.getComponentCount();
			int numberOfRows = getRows();
			int numberOfColumns = getColumns();
			if (numberOfComponents == 0) {
				return;
			}
			if (numberOfRows > 0) {
				numberOfColumns = (numberOfComponents + numberOfRows - 1) / numberOfRows;
			}
			else {
				numberOfRows = (numberOfComponents + numberOfColumns - 1) / numberOfColumns;
			}
			int horizontalGap = getHgap();
			int verticalGap = getVgap();
			// scaling factors
			Dimension pd = preferredLayoutSize(parent);
			double sw = (ONE_POINT_O * parent.getWidth()) / pd.getWidth();
			double sh = (ONE_POINT_O * parent.getHeight()) / pd.getHeight();
			// scale
			int[] columnWidths = new int[numberOfColumns];
			int[] rowHeights = new int[numberOfRows];
			for (int i = 0; i < numberOfComponents; i++) {
				int row = i / numberOfColumns;
				int column = i % numberOfColumns;
				Component currentComponent = parent.getComponent(i);
				Dimension currCompPrefSize = currentComponent.getPreferredSize();
				currCompPrefSize.width = (int) (sw * currCompPrefSize.getWidth());
				currCompPrefSize.height = (int) (sh * currCompPrefSize.getHeight());
				if (columnWidths[column] < currCompPrefSize.getWidth()) {
					columnWidths[column] = (int) currCompPrefSize.getWidth();
				}
				if (rowHeights[row] < currCompPrefSize.getHeight()) {
					rowHeights[row] = (int) currCompPrefSize.getHeight();
				}
			}

			arrangeFixedSizes(columnWidths, rowHeights);

			int x = insets.left;
			for (int c = 0; c < numberOfColumns; c++) {
				int y = insets.top;
				for (int r = 0; r < numberOfRows; r++) {
					int i = r * numberOfColumns + c;
					if (i < numberOfComponents) {
						parent.getComponent(i).setBounds(x, y, columnWidths[c], rowHeights[r]);
					}
					y += rowHeights[r] + verticalGap;
				}
				x += columnWidths[c] + horizontalGap;
			}
		}
	}

	/**
	 * @return a builder for {@link FlexibleGridLayout}.
	 */
	public static Builder builder() {
		return new DefaultBuilder();
	}

	/**
	 * A builder for {@link FlexibleGridLayout}.
	 */
	public interface Builder {

		/**
		 * @param rows the number of rows
		 * @return this builder instance
		 */
		Builder rows(int rows);

		/**
		 * @param columns the number of columns
		 * @return this builder instance
		 */
		Builder columns(int columns);

		/**
		 * @param rows the rows
		 * @param columns the columns
		 * @return this builder instance
		 */
		Builder rowsColumns(int rows, int columns);

		/**
		 * @param gap the horizontal and vertical gap to use
		 * @return this builder instance
		 */
		Builder gap(int gap);

		/**
		 * @param horizontalGap the horizontal gap
		 * @return this builder instance
		 */
		Builder horizontalGap(int horizontalGap);

		/**
		 * @param verticalGap the vertical gap
		 * @return this builder instance
		 */
		Builder verticalGap(int verticalGap);

		/**
		 * @param fixRowHeights true if rows should have a
		 * fixed height according to the tallest component
		 * @return this builder instance
		 */
		Builder fixRowHeights(boolean fixRowHeights);

		/**
		 * @param fixColumnWidths true if columns should have a
		 * fixed width according to the widest component
		 * @return this builder instance
		 */
		Builder fixColumnWidths(boolean fixColumnWidths);

		/**
		 * Also enables the fixed row heights.
		 * @param fixedRowHeight the fixed row height
		 * @return this builder instance
		 * @see #fixRowHeights(boolean)
		 */
		Builder fixedRowHeight(int fixedRowHeight);

		/**
		 * Also enables the fixed column widths.
		 * @param fixedColumnWidth the fixed column width
		 * @return this builder instance
		 * @see #fixColumnWidths(boolean)
		 */
		Builder fixedColumnWidth(int fixedColumnWidth);

		/**
		 * @return a new layout instance
		 */
		FlexibleGridLayout build();
	}

	private void arrangeFixedSizes(int[] columnWidths, int[] rowHeights) {
		if (fixedColumnWidths) {
			int maxColumnWidth = 0;
			if (fixedColumnWidth <= 0) {
				for (int columnWidth : columnWidths) {
					maxColumnWidth = Math.max(columnWidth, maxColumnWidth);
				}
			}
			else {
				maxColumnWidth = fixedColumnWidth;
			}
			Arrays.fill(columnWidths, maxColumnWidth);
		}
		if (fixedRowHeights) {
			int maxRowHeight = 0;
			if (fixedRowHeight <= 0) {
				for (int rowHeight : rowHeights) {
					maxRowHeight = Math.max(rowHeight, maxRowHeight);
				}
			}
			else {
				maxRowHeight = fixedRowHeight;
			}
			Arrays.fill(rowHeights, maxRowHeight);
		}
	}

	private Dimension layoutSize(Container parent, boolean preferredSize) {
		synchronized (parent.getTreeLock()) {
			Insets insets = parent.getInsets();
			int numberOfComponents = parent.getComponentCount();
			int numberOfRows = getRows();
			int numberOfColumns = getColumns();
			if (numberOfRows > 0) {
				numberOfColumns = (numberOfComponents + numberOfRows - 1) / numberOfRows;
			}
			else {
				numberOfRows = (numberOfComponents + numberOfColumns - 1) / numberOfColumns;
			}
			int[] columnWidths = new int[numberOfColumns];
			int[] rowHeights = new int[numberOfRows];
			for (int i = 0; i < numberOfComponents; i++) {
				int row = i / numberOfColumns;
				int column = i % numberOfColumns;
				Component comp = parent.getComponent(i);
				Dimension d = preferredSize ? comp.getPreferredSize() : comp.getMinimumSize();
				if (columnWidths[column] < d.getWidth()) {
					columnWidths[column] = (int) d.getWidth();
				}
				if (rowHeights[row] < d.getHeight()) {
					rowHeights[row] = (int) d.getHeight();
				}
			}

			arrangeFixedSizes(columnWidths, rowHeights);

			int newWidth = Arrays.stream(columnWidths).sum();
			int newHeight = Arrays.stream(rowHeights).sum();

			return new Dimension(insets.left + insets.right + newWidth + (numberOfColumns - 1) * getHgap(),
							insets.top + insets.bottom + newHeight + (numberOfRows - 1) * getVgap());
		}
	}

	private static final class DefaultBuilder implements Builder {

		private int rows = 0;
		private int columns = 0;
		private int horizontalGap = 0;
		private int verticalGap = 0;
		private boolean fixRowHeights = false;
		private boolean fixColumnWidths = false;
		private int fixedRowHeight;
		private int fixedColumnWidth;

		@Override
		public Builder rows(int rows) {
			this.rows = rows;
			return this;
		}

		@Override
		public Builder columns(int columns) {
			this.columns = columns;
			return this;
		}

		@Override
		public Builder rowsColumns(int rows, int columns) {
			this.rows = rows;
			this.columns = columns;
			return this;
		}

		@Override
		public Builder gap(int gap) {
			this.horizontalGap = gap;
			this.verticalGap = gap;
			return this;
		}

		@Override
		public Builder horizontalGap(int horizontalGap) {
			this.horizontalGap = horizontalGap;
			return this;
		}

		@Override
		public Builder verticalGap(int verticalGap) {
			this.verticalGap = verticalGap;
			return this;
		}

		@Override
		public Builder fixRowHeights(boolean fixRowHeights) {
			this.fixRowHeights = fixRowHeights;
			return this;
		}

		@Override
		public Builder fixColumnWidths(boolean fixColumnWidths) {
			this.fixColumnWidths = fixColumnWidths;
			return this;
		}

		@Override
		public Builder fixedRowHeight(int fixedRowHeight) {
			fixRowHeights(true);
			this.fixedRowHeight = fixedRowHeight;
			return this;
		}

		@Override
		public Builder fixedColumnWidth(int fixedColumnWidth) {
			fixColumnWidths(true);
			this.fixedColumnWidth = fixedColumnWidth;
			return this;
		}

		@Override
		public FlexibleGridLayout build() {
			FlexibleGridLayout layout = new FlexibleGridLayout(this);
			if (fixedRowHeight > 0) {
				layout.setFixedRowHeight(fixedRowHeight);
			}
			if (fixedColumnWidth > 0) {
				layout.setFixedColumnWidth(fixedColumnWidth);
			}

			return layout;
		}
	}
}