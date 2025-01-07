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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.model.component.table.FilterTableModel;
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
 * @param <T> the column type
 */
public interface FilterTableCellRenderer<T> extends TableCellRenderer {

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
	 * @return the column class
	 */
	Class<T> columnClass();

	/**
	 * @return true if an enabled filter should be indicated
	 */
	boolean filterIndicator();

	/**
	 * @return true if alternate row coloring is enabled
	 */
	boolean alternateRowColoring();

	/**
	 * @return the horizontal alignment
	 */
	int horizontalAlignment();

	/**
	 * Instantiates a new {@link FilterTableCellRenderer.Builder}.
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 * @param <T> the cell value type
	 * @param columnClass the column class
	 * @return a new {@link FilterTableCellRenderer.Builder} instance
	 */
	static <R, C, T> Builder<R, C, T> builder(Class<T> columnClass) {
		return new DefaultBuilder<>(requireNonNull(columnClass));
	}

	/**
	 * Provides a color to override the default color for table cells.
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 * @param <T> the cell value type
	 */
	interface ColorProvider<R, C, T> {

		/**
		 * @param table the table
		 * @param row the row object
		 * @param identifier the column identifier
		 * @param value the cell value
		 * @return the Color for the given cell, null for the default color
		 */
		Color color(FilterTable<R, C> table, R row, C identifier, T value);
	}

	/**
	 * Builds a {@link FilterTableCellRenderer}
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 * @param <T> the cell value type
	 */
	interface Builder<R, C, T> {

		/**
		 * @param uiSettings the ui settings
		 * @return this builder instance
		 */
		Builder<R, C, T> uiSettings(UISettings uiSettings);

		/**
		 * @param horizontalAlignment the horizontal alignment
		 * @return this builder instance
		 */
		Builder<R, C, T> horizontalAlignment(int horizontalAlignment);

		/**
		 * @param toolTipData true if the cell should display its contents in a tool tip
		 * @return this builder instance
		 */
		Builder<R, C, T> toolTipData(boolean toolTipData);

		/**
		 * @param columnShading true if column specific shading should be enabled, for example to indicated that the column is involved in a search/filter
		 * @return this builder instance
		 */
		Builder<R, C, T> filterIndicator(boolean columnShading);

		/**
		 * @param alternateRowColoring true if alternate row coloring should be enabled
		 * @return this builder instance
		 */
		Builder<R, C, T> alternateRowColoring(boolean alternateRowColoring);

		/**
		 * @param leftPadding the left cell padding
		 * @return this builder instance
		 */
		Builder<R, C, T> leftPadding(int leftPadding);

		/**
		 * @param rightPadding the right cell padding
		 * @return this builder instance
		 */
		Builder<R, C, T> rightPadding(int rightPadding);

		/**
		 * @param string provides a String to display for a given cell value, formatted or otherwise
		 * @return this builder instance
		 */
		Builder<R, C, T> string(Function<T, String> string);

		/**
		 * @param background provides the background color
		 * @return this builder instance
		 */
		Builder<R, C, T> background(ColorProvider<R, C, T> background);

		/**
		 * @param foreground provides the foreground color
		 * @return this builder instance
		 */
		Builder<R, C, T> foreground(ColorProvider<R, C, T> foreground);

		/**
		 * @return a new {@link FilterTableCellRenderer} instance based on this builder
		 */
		FilterTableCellRenderer<T> build();
	}

	/**
	 * @return a new default {@link Factory} instance
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 */
	static <R, C> Factory<R, C> factory() {
		return new DefaultFilterTableCellRenderer.DefaultFactory<>();
	}

	/**
	 * A factory for {@link FilterTableCellRenderer} instances.
	 */
	interface Factory<R, C> {

		/**
		 * @param identifier the column identifier
		 * @param tableModel the table model
		 * @return a {@link FilterTableCellRenderer} instance for the given column
		 */
		FilterTableCellRenderer<?> create(C identifier, FilterTableModel<R, C> tableModel);
	}

	/**
	 * Represents the UI cell colors according to the look and feel.
	 */
	interface UISettings {

		/**
		 * The table foreground color associated with the {@code Table.foreground} UI key
		 * @return the foreground color
		 * @see UIManager#getColor(Object)
		 * @see UIManager#put(Object, Object)
		 */
		Color foreground();

		/**
		 * The table background color associated with the {@code Table.background} UI key
		 * @return the background color
		 * @see UIManager#getColor(Object)
		 * @see UIManager#put(Object, Object)
		 */
		Color background();

		/**
		 * The table alternate row color associated with the {@code Table.alternateRowColor} UI key
		 * @return the alternate row color, if any
		 * @see UIManager#getColor(Object)
		 * @see UIManager#put(Object, Object)
		 */
		Color alternateRowColor();

		/**
		 * The table selection foreground color associated with the {@code Table.selectionForeground} UI key
		 * @return the selection foreground color
		 * @see UIManager#getColor(Object)
		 * @see UIManager#put(Object, Object)
		 */
		Color selectionForeground();

		/**
		 * The table selection background color associated with the {@code Table.selectionBackground} UI key
		 * @return the selection background color
		 * @see UIManager#getColor(Object)
		 * @see UIManager#put(Object, Object)
		 */
		Color selectionBackground();

		/**
		 * @return the background color to use for columns with a filter enabled
		 * @see #filterIndicator()
		 */
		Color filteredBackground();

		/**
		 * @return the alternate background color
		 */
		Color alternateBackground();

		/**
		 * @return the alternate background color to use for columns with a filter enabled
		 */
		Color alternateFilteredBackground();

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
		 * @param alternateRow true if this is an alternate row number
		 * @param cellBackgroundColor the cell specific background color, if any
		 * @return the background color
		 */
		Color background(boolean filterEnabled, boolean alternateRow, Color cellBackgroundColor);

		/**
		 * @param alternateRow true if this is an alternate row number
		 * @param cellBackgroundColor the cell specific background color, if any
		 * @return the filtered cell background
		 */
		Color filteredBackground(boolean alternateRow, Color cellBackgroundColor);

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

		private Color foreground;
		private Color background;
		private Color alternateRowColor;
		private Color filteredBackground;
		private Color alternateFilteredBackground;
		private Color alternateBackground;
		private Color selectionForeground;
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
			foreground = UIManager.getColor("Table.foreground");
			background = UIManager.getColor("Table.background");
			alternateRowColor = UIManager.getColor("Table.alternateRowColor");
			alternateBackground = alternateRowColor;
			if (alternateBackground == null) {
				alternateBackground = darker(background, DOUBLE_DARKENING_FACTOR);
			}
			selectionForeground = UIManager.getColor("Table.selectionForeground");
			selectionBackground = UIManager.getColor("Table.selectionBackground");
			filteredBackground = darker(background, DARKENING_FACTOR);
			alternateFilteredBackground = darker(alternateBackground, DARKENING_FACTOR);
			alternateSelectionBackground = darker(selectionBackground, DARKENING_FACTOR);
			defaultCellBorder = leftPadding > 0 || rightPadding > 0 ? createEmptyBorder(0, leftPadding, 0, rightPadding) : null;
			focusedCellBorder = createFocusedCellBorder();
		}

		@Override
		public final Color foreground() {
			return foreground;
		}

		@Override
		public final Color background() {
			return background;
		}

		@Override
		public final Color alternateRowColor() {
			return alternateRowColor;
		}

		@Override
		public final Color selectionForeground() {
			return selectionForeground;
		}

		@Override
		public final Color selectionBackground() {
			return selectionBackground;
		}

		@Override
		public final Color filteredBackground() {
			return filteredBackground;
		}

		@Override
		public final Color alternateBackground() {
			return alternateBackground;
		}

		@Override
		public final Color alternateFilteredBackground() {
			return alternateFilteredBackground;
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
		public Color background(boolean filterEnabled, boolean alternateRow, Color cellBackgroundColor) {
			if (filterEnabled) {
				return filteredBackground(alternateRow, cellBackgroundColor);
			}

			return cellBackgroundColor;
		}

		@Override
		public final Color filteredBackground(boolean alternateRow, Color cellBackgroundColor) {
			if (cellBackgroundColor != null) {
				return darker(cellBackgroundColor, DARKENING_FACTOR);
			}

			return alternateRow ? alternateFilteredBackground : filteredBackground;
		}

		private CompoundBorder createFocusedCellBorder() {
			return createCompoundBorder(createLineBorder(darker(foreground, DOUBLE_DARKENING_FACTOR),
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
