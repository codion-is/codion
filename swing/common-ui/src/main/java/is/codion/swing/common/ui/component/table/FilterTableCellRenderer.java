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

import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.util.Collection;
import java.util.function.Function;

import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.Configuration.integerValue;

/**
 * A {@link TableCellRenderer} for {@link FilterTable}, instantiated via {@link #builder()}.
 * @param <R> the row type
 * @param <C> the column identifier type
 * @param <T> the column type
 */
public interface FilterTableCellRenderer<R, C, T> extends TableCellRenderer {

	/**
	 * The default left padding for table cells.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 0
	 * </ul>
	 */
	PropertyValue<Integer> TABLE_CELL_LEFT_PADDING =
					integerValue(FilterTableCellRenderer.class.getName() + ".cellLeftPadding", 0);

	/**
	 * The default right padding for table cells.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 5
	 * </ul>
	 */
	PropertyValue<Integer> TABLE_CELL_RIGHT_PADDING =
					integerValue(FilterTableCellRenderer.class.getName() + ".cellRightPadding", 5);

	/**
	 * The default horizontal alignment for numerical columns.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#TRAILING}
	 * </ul>
	 */
	PropertyValue<Integer> NUMERICAL_HORIZONTAL_ALIGNMENT =
					integerValue(FilterTableCellRenderer.class.getName() + ".numericalHorizontalAlignment", SwingConstants.TRAILING);

	/**
	 * The default horizontal alignment for temporal columns.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#TRAILING}
	 * </ul>
	 */
	PropertyValue<Integer> TEMPORAL_HORIZONTAL_ALIGNMENT =
					integerValue(FilterTableCellRenderer.class.getName() + ".temporalHorizontalAlignment", SwingConstants.TRAILING);

	/**
	 * The default horizontal alignment for boolean columns.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#CENTER}
	 * </ul>
	 */
	PropertyValue<Integer> BOOLEAN_HORIZONTAL_ALIGNMENT =
					integerValue(FilterTableCellRenderer.class.getName() + ".booleanHorizontalAlignment", SwingConstants.CENTER);

	/**
	 * The default horizontal alignment.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: {@link SwingConstants#LEADING}
	 * </ul>
	 */
	PropertyValue<Integer> HORIZONTAL_ALIGNMENT =
					integerValue(FilterTableCellRenderer.class.getName() + ".horizontalAlignment", SwingConstants.LEADING);

	/**
	 * Specifies whether alternate row coloring is enabled by default.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> ALTERNATE_ROW_COLORING =
					booleanValue(FilterTableCellRenderer.class.getName() + ".alternateRowColoring", true);

	/**
	 * Specifies whether if a focused cell should be indicated with a cell border.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> FOCUSED_CELL_INDICATOR =
					booleanValue(FilterTableCellRenderer.class.getName() + ".focusedCellIndicator", true);

	/**
	 * Specifies whether cell borders are set. Disable to use the table cell borders provided by the look and feel.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> SET_BORDER =
					booleanValue(FilterTableCellRenderer.class.getName() + ".setBorder", true);

	/**
	 * @return the column class
	 */
	Class<T> columnClass();

	/**
	 * @return the horizontal alignment
	 */
	int horizontalAlignment();

	/**
	 * @return the cell border
	 */
	Border cellBorder();

	/**
	 * @return the cell renderer customizers
	 */
	Collection<Customizer<R, C>> customizers();

	/**
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 * @return a {@link Builder.ColumnClassStep} instance
	 */
	static <R, C> Builder.ColumnClassStep<R, C> builder() {
		return new DefaultFilterTableCellRenderer.DefaultColumnClassStep<>();
	}

	/**
	 * Provides a color to override the default color for table cells.
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 * @param <T> the cell value type
	 */
	interface CellColor<R, C, T> {

		/**
		 * @param table the table
		 * @param row the row object
		 * @param identifier the column identifier
		 * @param value the cell value
		 * @return the Color for the given cell, null for the default color
		 */
		@Nullable Color get(FilterTable<R, C> table, R row, C identifier, T value);
	}

	/**
	 * Customizes a renderer component for a given cell
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 */
	interface Customizer<R, C> {

		/**
		 * @return true if this customizer is enabled
		 */
		boolean enabled();

		/**
		 * @param table the table
		 * @param identifier the column identifier
		 * @param component the renderer component
		 */
		void customize(FilterTable<R, C> table, C identifier, JComponent component);
	}

	/**
	 * Builds a {@link FilterTableCellRenderer}
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 * @param <T> the cell value type
	 */
	interface Builder<R, C, T> {

		/**
		 * Provides a {@link Builder}
		 * @param <R> the row type
		 * @param <C> the column identifier type
		 */
		interface ColumnClassStep<R, C> {

			/**
			 * @param <T> the cell value type
			 * @param columnClass the column class
			 * @return a new {@link Builder} instance
			 */
			<T> Builder<R, C, T> columnClass(Class<T> columnClass);
		}

		/**
		 * Note that this setting does not apply when using {@link #renderer(TableCellRenderer)} or {@link #component(ComponentValue)}.
		 * @param horizontalAlignment the horizontal alignment
		 * @return this builder instance
		 */
		Builder<R, C, T> horizontalAlignment(int horizontalAlignment);

		/**
		 * @param toolTip provides the tooltip for the cell, given the cell data
		 * @return this builder instance
		 */
		Builder<R, C, T> toolTip(Function<T, String> toolTip);

		/**
		 * @param filterIndicator true if column specific shading should be enabled, for example to indicated that the column is involved in a search/filter
		 * @return this builder instance
		 */
		Builder<R, C, T> filterIndicator(boolean filterIndicator);

		/**
		 * @param focusedCellIndicator true if the focused cell should be indicated with a cell border
		 * @return this builder instance
		 */
		Builder<R, C, T> focusedCellIndicator(boolean focusedCellIndicator);

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
		 * The default formatter returns {@code value.toString()} and an empty string in case of null.
		 * @param formatter provides a String to display for a given cell value, formatted or otherwise
		 * @return this builder instance
		 */
		Builder<R, C, T> formatter(Function<T, String> formatter);

		/**
		 * @param background provides the background color
		 * @return this builder instance
		 */
		Builder<R, C, T> background(CellColor<R, C, T> background);

		/**
		 * @param foreground provides the foreground color
		 * @return this builder instance
		 */
		Builder<R, C, T> foreground(CellColor<R, C, T> foreground);

		/**
		 * @param customizer customizes the renderer component for a given cell
		 * @return this builder instance
		 */
		Builder<R, C, T> customizer(Customizer<R, C> customizer);

		/**
		 * Wraps the given renderer, using it as the base renderer before
		 * applying the rendering settings of this renderer.
		 * @param renderer the renderer to wrap
		 * @return this builder instance
		 */
		Builder<R, C, T> renderer(TableCellRenderer renderer);

		/**
		 * Wraps the given component, using it as the base renderer before
		 * applying the rendering settings of this renderer.
		 * @param component the {@link ComponentValue} to use when rendering
		 * @return this builder instance
		 */
		Builder<R, C, T> component(ComponentValue<? extends JComponent, T> component);

		/**
		 * @return a new {@link FilterTableCellRenderer} instance based on this builder
		 */
		FilterTableCellRenderer<R, C, T> build();
	}

	/**
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 * @return a new default {@link Factory} instance
	 */
	static <R, C> Factory<R, C> factory() {
		return new DefaultFilterTableCellRenderer.DefaultFactory<>();
	}

	/**
	 * A factory for {@link FilterTableCellRenderer} instances.
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 */
	interface Factory<R, C> {

		/**
		 * @param identifier the column identifier
		 * @param table the table
		 * @return a {@link FilterTableCellRenderer} instance for the given column
		 */
		FilterTableCellRenderer<R, C, ?> create(C identifier, FilterTable<R, C> table);
	}
}
