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
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.property.PropertyValue;

import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.util.function.Function;

import static is.codion.swing.common.ui.Colors.darker;
import static javax.swing.BorderFactory.*;

/**
 * Provides TableCellRenderer implementations for FilterTable via {@link #builder(Object, Class)}.
 */
public interface FilterTableCellRenderer extends TableCellRenderer {

	/**
	 * The default left padding for table cells.
	 * <li>Value type: Integer
	 * <li>Default value: 0
	 */
	PropertyValue<Integer> TABLE_CELL_LEFT_PADDING =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".cellLeftPadding", 0);

	/**
	 * The default right padding for table cells.
	 * <li>Value type: Integer
	 * <li>Default value: 5
	 */
	PropertyValue<Integer> TABLE_CELL_RIGHT_PADDING =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".cellRightPadding", 5);

	/**
	 * The default horizontal alignment for numerical columns.
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#RIGHT}
	 */
	PropertyValue<Integer> NUMERICAL_HORIZONTAL_ALIGNMENT =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".numericalHorizontalAlignment", SwingConstants.RIGHT);

	/**
	 * The default horizontal alignment for temporal columns.
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#RIGHT}
	 */
	PropertyValue<Integer> TEMPORAL_HORIZONTAL_ALIGNMENT =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".temporalHorizontalAlignment", SwingConstants.RIGHT);

	/**
	 * The default horizontal alignment for boolean columns.
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#CENTER}
	 */
	PropertyValue<Integer> BOOLEAN_HORIZONTAL_ALIGNMENT =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".booleanHorizontalAlignment", SwingConstants.CENTER);

	/**
	 * The default horizontal alignment.
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#LEADING}
	 */
	PropertyValue<Integer> HORIZONTAL_ALIGNMENT =
					Configuration.integerValue(FilterTableCellRenderer.class.getName() + ".horizontalAlignment", SwingConstants.LEADING);

	/**
	 * Specifies whether alternate row coloring is enabled by default.
	 * <li>Value type: Boolean
	 * <li>Default value: true
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
	 * Instantiates a new {@link FilterTableCellRenderer.Builder}.
	 * @param <C> the column identifier type
	 * @param identifier the column identifier
	 * @param columnClass the column class
	 * @return a new {@link FilterTableCellRenderer.Builder} instance
	 */
	static <C> Builder<C> builder(C identifier, Class<?> columnClass) {
		return new DefaultFilterTableCellRendererBuilder<>(identifier, columnClass);
	}

	/**
	 * Provides cell specific colors.
	 * @param <C> the column identifier type
	 */
	interface CellColors<C> {

		/**
		 * @param row the row number
		 * @param identifier the column identifier
		 * @param cellValue the cell value
		 * @param selected true if the cell is selected
		 * @return a background Color for the given cell, null for none
		 */
		default Color backgroundColor(int row, C identifier, Object cellValue, boolean selected) {
			return null;
		}

		/**
		 * @param row the row number
		 * @param identifier the column identifier
		 * @param cellValue the cell value
		 * @param selected true if the cell is selected
		 * @return a foreground Color for the given cell, null for none
		 */
		default Color foregroundColor(int row, C identifier, Object cellValue, boolean selected) {
			return null;
		}
	}

	/**
	 * Builds a {@link FilterTableCellRenderer}
	 * @param <C> the column identifier type
	 */
	interface Builder<C> {

		/**
		 * Used to indicate whether a filter condition is enabled for the column by shading it.
		 * @param filter the filter condition model, may be null
		 * @return this builder instance
		 */
		Builder<C> filter(ConditionModel<?> filter);

		/**
		 * @param horizontalAlignment the horizontal alignment
		 * @return this builder instance
		 */
		Builder<C> horizontalAlignment(int horizontalAlignment);

		/**
		 * @param toolTipData true if the cell should display its contents in a tool tip
		 * @return this builder instance
		 */
		Builder<C> toolTipData(boolean toolTipData);

		/**
		 * @param columnShading true if column specific shading should be enabled, for example to indicated that the column is involved in a search/filter
		 * @return this builder instance
		 */
		Builder<C> columnShading(boolean columnShading);

		/**
		 * @param alternateRowColoring true if alternate row coloring should be enabled
		 * @return this builder instance
		 */
		Builder<C> alternateRowColoring(boolean alternateRowColoring);

		/**
		 * @param leftPadding the left cell padding
		 * @return this builder instance
		 */
		Builder<C> leftPadding(int leftPadding);

		/**
		 * @param rightPadding the right cell padding
		 * @return this builder instance
		 */
		Builder<C> rightPadding(int rightPadding);

		/**
		 * @param string provides a String to display for a given cell value, formatted or otherwise
		 * @return this builder instance
		 */
		Builder<C> string(Function<Object, String> string);

		/**
		 * @param cellColors provides cell/row background and foreground color
		 * @return this builder instance
		 */
		Builder<C> cellColors(CellColors<C> cellColors);

		/**
		 * @return a new {@link FilterTableCellRenderer} instance based on this builder
		 */
		FilterTableCellRenderer build();
	}

	/**
	 * Settings for a {@link FilterTableCellRenderer}
	 * @param <C> the column identifier type
	 */
	class Settings<C> {

		protected static final float SELECTION_COLOR_BLEND_RATIO = 0.5f;
		protected static final double DARKENING_FACTOR = 0.9;
		protected static final double DOUBLE_DARKENING_FACTOR = 0.8;
		protected static final int FOCUSED_CELL_BORDER_THICKNESS = 1;

		private final int leftPadding;
		private final int rightPadding;
		private final boolean alternateRowColoring;

		private Color foregroundColor;
		private Color backgroundColor;
		private Color alternateRowColor;
		private Color backgroundColorShaded;
		private Color backgroundColorAlternate;
		private Color backgroundColorAlternateShaded;
		private Color selectionBackground;
		private Color selectionBackgroundAlternate;
		private Border defaultCellBorder;
		private Border focusedCellBorder;

		/**
		 * @param leftPadding the left padding
		 * @param rightPadding the right padding
		 * @param alternateRowColoring true if alternate row coloring should be enabled
		 */
		protected Settings(int leftPadding, int rightPadding, boolean alternateRowColoring) {
			this.leftPadding = leftPadding;
			this.rightPadding = rightPadding;
			this.alternateRowColoring = alternateRowColoring;
		}

		/**
		 * @param cellForegroundColor the cell specific foreground color
		 * @return the cell foreground color or the default foreground if null
		 */
		final Color foregroundColor(Color cellForegroundColor) {
			return cellForegroundColor == null ? foregroundColor : cellForegroundColor;
		}

		/**
		 * @return the border to use for a focused cell
		 */
		final Border focusedCellBorder() {
			return focusedCellBorder;
		}

		/**
		 * @return the default cell border
		 */
		final Border defaultCellBorder() {
			return defaultCellBorder;
		}

		/**
		 * Updates the colors according the the selected look and feel
		 */
		protected void updateColors() {
			foregroundColor = UIManager.getColor("Table.foreground");
			backgroundColor = UIManager.getColor("Table.background");
			alternateRowColor = UIManager.getColor("Table.alternateRowColor");
			backgroundColorAlternate = alternateRowColor;
			if (backgroundColorAlternate == null) {
				backgroundColorAlternate = darker(backgroundColor, DOUBLE_DARKENING_FACTOR);
			}
			selectionBackground = UIManager.getColor("Table.selectionBackground");
			backgroundColorShaded = darker(backgroundColor, DARKENING_FACTOR);
			backgroundColorAlternateShaded = darker(backgroundColorAlternate, DARKENING_FACTOR);
			selectionBackgroundAlternate = darker(selectionBackground, DARKENING_FACTOR);
			defaultCellBorder = leftPadding > 0 || rightPadding > 0 ? createEmptyBorder(0, leftPadding, 0, rightPadding) : null;
			focusedCellBorder = createFocusedCellBorder(foregroundColor, defaultCellBorder);
		}

		protected final Color backgroundColor(ConditionModel<?> filter, int row, C identifier, boolean columnShading,
																					boolean selected, Color cellBackgroundColor) {
			cellBackgroundColor = backgroundColor(cellBackgroundColor, row, selected);
			if (columnShading) {
				cellBackgroundColor = backgroundColorShaded(filter, row, identifier, cellBackgroundColor);
			}
			if (cellBackgroundColor != null) {
				return cellBackgroundColor;
			}
			if (alternateRowColoring) {
				return alternateRow(row) ? backgroundColorAlternate : backgroundColor;
			}
			if (alternateRowColor == null) {
				return backgroundColor;
			}
			// If alternate row coloring is enabled outside of the framework, respect it
			return alternateRow(row) ? alternateRowColor : backgroundColor;
		}

		/**
		 * Adds shading to the given cell, if applicable
		 * @param filter the filter condition model for the given column, may be null
		 * @param row the row
		 * @param identifier the column identifier
		 * @param cellBackgroundColor the cell specific background color, if any
		 * @return a shaded background color
		 */
		protected Color backgroundColorShaded(ConditionModel<?> filter, int row, C identifier, Color cellBackgroundColor) {
			if (filter != null && filter.enabled().get()) {
				return backgroundShaded(row, cellBackgroundColor);
			}

			return cellBackgroundColor;
		}

		protected final Color backgroundColor() {
			return backgroundColor;
		}

		protected final Color backgroundColorShaded() {
			return backgroundColorShaded;
		}

		protected final Color backgroundColorAlternate() {
			return backgroundColorAlternate;
		}

		protected final Color backgroundColorAlternateShaded() {
			return backgroundColorAlternateShaded;
		}

		private Color backgroundColor(Color cellBackgroundColor, int row, boolean selected) {
			if (selected) {
				if (cellBackgroundColor == null) {
					return selectionBackgroundColor(row);
				}

				return blendColors(cellBackgroundColor, selectionBackgroundColor(row));
			}

			return cellBackgroundColor;
		}

		private Color selectionBackgroundColor(int row) {
			return alternateRow(row) ? selectionBackgroundAlternate : selectionBackground;
		}

		private Color backgroundShaded(int row, Color cellBackgroundColor) {
			if (cellBackgroundColor != null) {
				return darker(cellBackgroundColor, DARKENING_FACTOR);
			}

			return alternateRow(row) ? backgroundColorAlternateShaded : backgroundColorShaded;
		}

		/**
		 * @param row the row
		 * @return true if the given row should use the alternate row color
		 */
		protected static boolean alternateRow(int row) {
			return row % 2 != 0;
		}

		private static CompoundBorder createFocusedCellBorder(Color foregroundColor, Border defaultCellBorder) {
			return createCompoundBorder(createLineBorder(darker(foregroundColor, DOUBLE_DARKENING_FACTOR),
							FOCUSED_CELL_BORDER_THICKNESS), defaultCellBorder);
		}

		private static Color blendColors(Color color1, Color color2) {
			int r = (int) (color1.getRed() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getRed() * SELECTION_COLOR_BLEND_RATIO);
			int g = (int) (color1.getGreen() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getGreen() * SELECTION_COLOR_BLEND_RATIO);
			int b = (int) (color1.getBlue() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getBlue() * SELECTION_COLOR_BLEND_RATIO);

			return new Color(r, g, b, color1.getAlpha());
		}
	}
}
