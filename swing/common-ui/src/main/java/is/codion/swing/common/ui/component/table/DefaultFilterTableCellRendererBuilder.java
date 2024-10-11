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
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer.ColorProvider;

import java.time.temporal.Temporal;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link FilterTableCellRenderer.Builder} implementation.
 * @param <C> the column identifier type
 */
public class DefaultFilterTableCellRendererBuilder<C> implements FilterTableCellRenderer.Builder {

	final C columnIdentifier;

	private final Class<?> columnClass;
	private final boolean useBooleanRenderer;

	ConditionModel<?> filter;
	int horizontalAlignment;
	boolean toolTipData;
	boolean columnShading = true;
	boolean alternateRowColoring = FilterTableCellRenderer.ALTERNATE_ROW_COLORING.get();
	int leftPadding = FilterTableCellRenderer.TABLE_CELL_LEFT_PADDING.get();
	int rightPadding = FilterTableCellRenderer.TABLE_CELL_RIGHT_PADDING.get();
	Function<Object, String> string = new DefaultString();
	ColorProvider backgroundColor;
	ColorProvider foregroundColor;

	/**
	 * Instantiates a new builder
	 * @param columnIdentifier the column identifier
	 * @param columnClass the column class
	 */
	protected DefaultFilterTableCellRendererBuilder(C columnIdentifier, Class<?> columnClass) {
		this(columnIdentifier, columnClass, Boolean.class.equals(requireNonNull(columnClass)));
	}

	/**
	 * Instantiates a new builder
	 * @param columnIdentifier the column identifier
	 * @param columnClass the column class
	 * @param useBooleanRenderer true if the boolean renderer should be used
	 */
	protected DefaultFilterTableCellRendererBuilder(C columnIdentifier, Class<?> columnClass, boolean useBooleanRenderer) {
		this.columnIdentifier = requireNonNull(columnIdentifier);
		this.columnClass = requireNonNull(columnClass);
		this.useBooleanRenderer = useBooleanRenderer;
		this.horizontalAlignment = defaultHorizontalAlignment();
	}

	@Override
	public FilterTableCellRenderer.Builder filter(ConditionModel<?> filter) {
		this.filter = filter;
		return this;
	}

	@Override
	public final FilterTableCellRenderer.Builder horizontalAlignment(int horizontalAlignment) {
		this.horizontalAlignment = horizontalAlignment;
		return this;
	}

	@Override
	public final FilterTableCellRenderer.Builder toolTipData(boolean toolTipData) {
		this.toolTipData = toolTipData;
		return this;
	}

	@Override
	public final FilterTableCellRenderer.Builder columnShading(boolean columnShading) {
		this.columnShading = columnShading;
		return this;
	}

	@Override
	public final FilterTableCellRenderer.Builder alternateRowColoring(boolean alternateRowColoring) {
		this.alternateRowColoring = alternateRowColoring;
		return this;
	}

	@Override
	public final FilterTableCellRenderer.Builder leftPadding(int leftPadding) {
		this.leftPadding = leftPadding;
		return this;
	}

	@Override
	public final FilterTableCellRenderer.Builder rightPadding(int rightPadding) {
		this.rightPadding = rightPadding;
		return this;
	}

	@Override
	public final FilterTableCellRenderer.Builder string(Function<Object, String> string) {
		this.string = requireNonNull(string);
		return this;
	}

	@Override
	public FilterTableCellRenderer.Builder background(ColorProvider background) {
		this.backgroundColor = requireNonNull(background);
		return this;
	}

	@Override
	public FilterTableCellRenderer.Builder foreground(ColorProvider foreground) {
		this.foregroundColor = requireNonNull(foreground);
		return this;
	}

	@Override
	public final FilterTableCellRenderer build() {
		return useBooleanRenderer ?
						new DefaultFilterTableCellRenderer.BooleanRenderer<>(this, settings(leftPadding, rightPadding, alternateRowColoring)) :
						new DefaultFilterTableCellRenderer<>(this, settings(leftPadding, rightPadding, alternateRowColoring));
	}

	/**
	 * @param leftPadding the left padding
	 * @param rightPadding the right padding
	 * @param alternateRowColoring true if alternate row coloring is enabled
	 * @return the {@link FilterTableCellRenderer.Settings} instance for this renderer
	 */
	protected FilterTableCellRenderer.Settings<C> settings(int leftPadding, int rightPadding, boolean alternateRowColoring) {
		return new FilterTableCellRenderer.Settings<>(leftPadding, rightPadding, alternateRowColoring);
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

	private static final class DefaultString implements Function<Object, String> {
		@Override
		public String apply(Object value) {
			return value == null ? "" : value.toString();
		}
	}
}
