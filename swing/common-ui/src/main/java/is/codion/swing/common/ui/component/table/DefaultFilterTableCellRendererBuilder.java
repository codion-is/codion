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

import is.codion.common.model.condition.ConditionModel;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer.Builder;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer.ColorProvider;

import java.time.temporal.Temporal;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link Builder} implementation.
 */
public class DefaultFilterTableCellRendererBuilder<T> implements Builder<T> {

	private final Class<T> columnClass;
	private final boolean useBooleanRenderer;

	ConditionModel<?> filter;
	int horizontalAlignment;
	boolean toolTipData;
	boolean columnShading = true;
	boolean alternateRowColoring = FilterTableCellRenderer.ALTERNATE_ROW_COLORING.get();
	int leftPadding = FilterTableCellRenderer.TABLE_CELL_LEFT_PADDING.get();
	int rightPadding = FilterTableCellRenderer.TABLE_CELL_RIGHT_PADDING.get();
	Function<T, String> string = new DefaultString();
	ColorProvider<T> backgroundColor;
	ColorProvider<T> foregroundColor;

	/**
	 * Instantiates a new builder
	 * @param columnClass the column class
	 */
	protected DefaultFilterTableCellRendererBuilder(Class<T> columnClass) {
		this(columnClass, Boolean.class.equals(requireNonNull(columnClass)));
	}

	/**
	 * Instantiates a new builder
	 * @param columnClass the column class
	 * @param useBooleanRenderer true if the boolean renderer should be used
	 */
	protected DefaultFilterTableCellRendererBuilder(Class<T> columnClass, boolean useBooleanRenderer) {
		this.columnClass = requireNonNull(columnClass);
		this.useBooleanRenderer = useBooleanRenderer;
		this.horizontalAlignment = defaultHorizontalAlignment();
	}

	@Override
	public Builder<T> filter(ConditionModel<?> filter) {
		this.filter = filter;
		return this;
	}

	@Override
	public final Builder<T> horizontalAlignment(int horizontalAlignment) {
		this.horizontalAlignment = horizontalAlignment;
		return this;
	}

	@Override
	public final Builder<T> toolTipData(boolean toolTipData) {
		this.toolTipData = toolTipData;
		return this;
	}

	@Override
	public final Builder<T> columnShading(boolean columnShading) {
		this.columnShading = columnShading;
		return this;
	}

	@Override
	public final Builder<T> alternateRowColoring(boolean alternateRowColoring) {
		this.alternateRowColoring = alternateRowColoring;
		return this;
	}

	@Override
	public final Builder<T> leftPadding(int leftPadding) {
		this.leftPadding = leftPadding;
		return this;
	}

	@Override
	public final Builder<T> rightPadding(int rightPadding) {
		this.rightPadding = rightPadding;
		return this;
	}

	@Override
	public final Builder<T> string(Function<T, String> string) {
		this.string = requireNonNull(string);
		return this;
	}

	@Override
	public Builder<T> background(ColorProvider<T> background) {
		this.backgroundColor = requireNonNull(background);
		return this;
	}

	@Override
	public Builder<T> foreground(ColorProvider<T> foreground) {
		this.foregroundColor = requireNonNull(foreground);
		return this;
	}

	@Override
	public final FilterTableCellRenderer build() {
		return useBooleanRenderer ?
						new DefaultFilterTableCellRenderer.BooleanRenderer((DefaultFilterTableCellRendererBuilder<Boolean>) this, settings(leftPadding, rightPadding, alternateRowColoring)) :
						new DefaultFilterTableCellRenderer<>(this, settings(leftPadding, rightPadding, alternateRowColoring));
	}

	/**
	 * @param leftPadding the left padding
	 * @param rightPadding the right padding
	 * @param alternateRowColoring true if alternate row coloring is enabled
	 * @return the {@link FilterTableCellRenderer.Settings} instance for this renderer
	 */
	protected FilterTableCellRenderer.Settings settings(int leftPadding, int rightPadding, boolean alternateRowColoring) {
		return new FilterTableCellRenderer.Settings(leftPadding, rightPadding, alternateRowColoring);
	}

	private int defaultHorizontalAlignment() {
		if (useBooleanRenderer) {
			return FilterTableCellRenderer.BOOLEAN_HORIZONTAL_ALIGNMENT.get();
		}
		if (Number.class.isAssignableFrom(columnClass)) {
			return FilterTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.get();
		}
		if (Temporal.class.isAssignableFrom(columnClass)) {
			return FilterTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.get();
		}

		return FilterTableCellRenderer.HORIZONTAL_ALIGNMENT.get();
	}

	private final class DefaultString implements Function<T, String> {
		@Override
		public String apply(T value) {
			return value == null ? "" : value.toString();
		}
	}
}
