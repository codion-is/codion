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

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.ui.component.table.DefaultFilterTableCellRenderer.DefaultBuilder;

import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.util.function.Function;

import static is.codion.swing.common.ui.Colors.darker;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.*;

/**
 * A {@link TableCellRenderer} for {@link FilterTable}, instantiated via {@link #builder(Class)}.
 */
public interface FilterTableCellRenderer extends TableCellRenderer {

	/**
	 * The default left padding for table cells.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 0
	 * </ul>
	 */
	PropertyValue<Integer> TABLE_CELL_LEFT_PADDING =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".cellLeftPadding", 0);

	/**
	 * The default right padding for table cells.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 5
	 * </ul>
	 */
	PropertyValue<Integer> TABLE_CELL_RIGHT_PADDING =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".cellRightPadding", 5);

	/**
	 * The default horizontal alignment for numerical columns.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#RIGHT}
	 * </ul>
	 */
	PropertyValue<Integer> NUMERICAL_HORIZONTAL_ALIGNMENT =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".numericalHorizontalAlignment", SwingConstants.RIGHT);

	/**
	 * The default horizontal alignment for temporal columns.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#RIGHT}
	 * </ul>
	 */
	PropertyValue<Integer> TEMPORAL_HORIZONTAL_ALIGNMENT =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".temporalHorizontalAlignment", SwingConstants.RIGHT);

	/**
	 * The default horizontal alignment for boolean columns.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#CENTER}
	 * </ul>
	 */
	PropertyValue<Integer> BOOLEAN_HORIZONTAL_ALIGNMENT =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".booleanHorizontalAlignment", SwingConstants.CENTER);

	/**
	 * The default horizontal alignment.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#LEADING}
	 * </ul>
	 */
	PropertyValue<Integer> HORIZONTAL_ALIGNMENT =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".horizontalAlignment", SwingConstants.LEADING);

	/**
	 * Specifies whether alternate row coloring is enabled by default.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> ALTERNATE_ROW_COLORING =
					Configuration.booleanValue(FilterTableCellRenderer.class.getName() + ".alternateRowColoring", true);

	/**
	 * @return true if column shading is enabled
	 */
	boolean columnShading();

	/**
	 * @return true if alternate row coloring is enabled
	 */
	boolean alternateRowColoring();

	/**
	 * @return the horizontal alignment
	 */
	int horizontalAlignment();

	/**
	 * @param row the row index
	 * @return true if the row is an alternate row (odd number)
	 */
	static boolean alternateRow(int row) {
		return row % 2 != 0;
	}

	/**
	 * Instantiates a new {@link FilterTableCellRenderer.Builder}.
	 * @param <T> the cell value type
	 * @param columnClass the column class
	 * @return a new {@link FilterTableCellRenderer.Builder} instance
	 */
	static <T> Builder<T> builder(Class<T> columnClass) {
		return new DefaultBuilder<>(requireNonNull(columnClass), Boolean.class.equals(columnClass));
	}

	/**
	 * Provides a color to override the default color for table cells.
	 * @param <T> the cell value type
	 */
	interface ColorProvider<T> {

		/**
		 * @param row the row number
		 * @param value the cell value
		 * @return the Color for the given cell, null for the default color
		 */
		Color color(FilterTable<?, ?> table, int row, T value);
	}

	/**
	 * Builds a {@link FilterTableCellRenderer}
	 * @param <T> the cell value type
	 */
	interface Builder<T> {

		/**
		 * @param uiSettings the ui settings
		 * @return this builder instance
		 */
		Builder<T> uiSettings(UISettings uiSettings);

		/**
		 * @param horizontalAlignment the horizontal alignment
		 * @return this builder instance
		 */
		Builder<T> horizontalAlignment(int horizontalAlignment);

		/**
		 * @param toolTipData true if the cell should display its contents in a tool tip
		 * @return this builder instance
		 */
		Builder<T> toolTipData(boolean toolTipData);

		/**
		 * @param columnShading true if column specific shading should be enabled, for example to indicated that the column is involved in a search/filter
		 * @return this builder instance
		 */
		Builder<T> columnShading(boolean columnShading);

		/**
		 * @param alternateRowColoring true if alternate row coloring should be enabled
		 * @return this builder instance
		 */
		Builder<T> alternateRowColoring(boolean alternateRowColoring);

		/**
		 * @param leftPadding the left cell padding
		 * @return this builder instance
		 */
		Builder<T> leftPadding(int leftPadding);

		/**
		 * @param rightPadding the right cell padding
		 * @return this builder instance
		 */
		Builder<T> rightPadding(int rightPadding);

		/**
		 * @param string provides a String to display for a given cell value, formatted or otherwise
		 * @return this builder instance
		 */
		Builder<T> string(Function<T, String> string);

		/**
		 * @param background provides the background color
		 * @return this builder instance
		 */
		Builder<T> background(ColorProvider<T> background);

		/**
		 * @param foreground provides the foreground color
		 * @return this builder instance
		 */
		Builder<T> foreground(ColorProvider<T> foreground);

		/**
		 * @return a new {@link FilterTableCellRenderer} instance based on this builder
		 */
		FilterTableCellRenderer build();
	}

	/**
	 * A factory for {@link FilterTableCellRenderer} instances.
	 */
	interface Factory<C> {

		/**
		 * @param identifier the column identifier
		 * @return a {@link FilterTableCellRenderer} instance for the given column
		 */
		FilterTableCellRenderer create(C identifier);
	}

	/**
	 * Represents the UI cell colors according to the look and feel.
	 */
	interface UISettings {

		/**
		 * The foreground color as defined by the {@code Table.foreground} system property
		 * @return the foreground color
		 */
		Color foregroundColor();

		/**
		 * The background color as defined by the {@code Table.background} system property
		 * @return the background color
		 */
		Color backgroundColor();

		/**
		 * The alternate row color as defined by the {@code Table.alternateRowColor} system property
		 * @return the alternate row color, if any
		 */
		Color alternateRowColor();

		/**
		 * The selection background color as defined by the {@code Table.selectionBackground} system property
		 * @return the selection background color
		 */
		Color selectionBackground();

		/**
		 * @return the shaded background color
		 */
		Color shadedBackgroundColor();

		/**
		 * Returns the {@link #alternateRowColor()} if specified
		 * or double shaded {@link #backgroundColor()}.
		 * @return the alternate background color
		 */
		Color alternateBackgroundColor();

		/**
		 * @return the shaded alternate background color
		 */
		Color shadedAlternateBackgroundColor();

		/**
		 * @return the default cell border to use
		 */
		Border defaultCellBorder();

		/**
		 * @return the cell border to use for the focused cell
		 */
		Border focusedCellBorder();

		/**
		 * @param filterEnabled true if a filter is enabled
		 * @param row the row index
		 * @param cellBackgroundColor the cell background color, if any
		 * @return the shaded cell background color if specified, otherwise the default
		 */
		Color shadedBackgroundColor(boolean filterEnabled, int row, Color cellBackgroundColor);

		/**
		 * @param row the row
		 * @param cellBackgroundColor the cell background color, if any
		 * @return the shaded cell background color if specified, otherwise the default
		 */
		Color shadedBackgroundColor(int row, Color cellBackgroundColor);

		/**
		 * @return the alternate selection background color
		 */
		Color alternateSelectionBackground();

		/**
		 * Updates the colors and border according to the current Look and Feel.
		 * @param leftPadding the left padding to use for the border
		 * @param rightPadding the right padding to use for the border
		 */
		void update(int leftPadding, int rightPadding);
	}

	/**
	 * A default {@link UISettings} implementation.
	 */
	class DefaultUISettings implements UISettings {

		protected static final float SELECTION_COLOR_BLEND_RATIO = 0.5f;
		protected static final double DARKENING_FACTOR = 0.9;
		protected static final double DOUBLE_DARKENING_FACTOR = 0.8;
		protected static final int FOCUSED_CELL_BORDER_THICKNESS = 1;

		private Color foregroundColor;
		private Color backgroundColor;
		private Color alternateRowColor;
		private Color shadedBackgroundColor;
		private Color alternateBackgroundColor;
		private Color shadedAlternateBackgroundColor;
		private Color selectionBackground;
		private Color alternateSelectionBackground;
		private Border defaultCellBorder;
		private Border focusedCellBorder;

		protected DefaultUISettings() {}

		/**
		 * Updates the colors according the the selected look and feel
		 */
		@Override
		public void update(int leftPadding, int rightPadding) {
			foregroundColor = UIManager.getColor("Table.foreground");
			backgroundColor = UIManager.getColor("Table.background");
			alternateRowColor = UIManager.getColor("Table.alternateRowColor");
			alternateBackgroundColor = alternateRowColor;
			if (alternateBackgroundColor == null) {
				alternateBackgroundColor = darker(backgroundColor, DOUBLE_DARKENING_FACTOR);
			}
			selectionBackground = UIManager.getColor("Table.selectionBackground");
			shadedBackgroundColor = darker(backgroundColor, DARKENING_FACTOR);
			shadedAlternateBackgroundColor = darker(alternateBackgroundColor, DARKENING_FACTOR);
			alternateSelectionBackground = darker(selectionBackground, DARKENING_FACTOR);
			defaultCellBorder = leftPadding > 0 || rightPadding > 0 ? createEmptyBorder(0, leftPadding, 0, rightPadding) : null;
			focusedCellBorder = createFocusedCellBorder();
		}

		@Override
		public final Color foregroundColor() {
			return foregroundColor;
		}

		@Override
		public final Color backgroundColor() {
			return backgroundColor;
		}

		@Override
		public final Color alternateRowColor() {
			return alternateRowColor;
		}

		@Override
		public final Color selectionBackground() {
			return selectionBackground;
		}

		@Override
		public final Color shadedBackgroundColor() {
			return shadedBackgroundColor;
		}

		@Override
		public final Color alternateBackgroundColor() {
			return alternateBackgroundColor;
		}

		@Override
		public final Color shadedAlternateBackgroundColor() {
			return shadedAlternateBackgroundColor;
		}

		@Override
		public final Color alternateSelectionBackground() {
			return alternateSelectionBackground;
		}

		@Override
		public final Border defaultCellBorder() {
			return defaultCellBorder;
		}

		@Override
		public final Border focusedCellBorder() {
			return focusedCellBorder;
		}

		@Override
		public Color shadedBackgroundColor(boolean filterEnabled, int row, Color cellBackgroundColor) {
			if (filterEnabled) {
				return shadedBackgroundColor(row, cellBackgroundColor);
			}

			return cellBackgroundColor;
		}

		@Override
		public final Color shadedBackgroundColor(int row, Color cellBackgroundColor) {
			if (cellBackgroundColor != null) {
				return darker(cellBackgroundColor, DARKENING_FACTOR);
			}

			return alternateRow(row) ? shadedAlternateBackgroundColor : shadedBackgroundColor;
		}

		private CompoundBorder createFocusedCellBorder() {
			return createCompoundBorder(createLineBorder(darker(foregroundColor, DOUBLE_DARKENING_FACTOR),
							FOCUSED_CELL_BORDER_THICKNESS), defaultCellBorder);
		}

		static Color blendColors(Color color1, Color color2) {
			int r = (int) (color1.getRed() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getRed() * SELECTION_COLOR_BLEND_RATIO);
			int g = (int) (color1.getGreen() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getGreen() * SELECTION_COLOR_BLEND_RATIO);
			int b = (int) (color1.getBlue() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getBlue() * SELECTION_COLOR_BLEND_RATIO);

			return new Color(r, g, b, color1.getAlpha());
		}
	}
}
