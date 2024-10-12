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
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link FilterTableCellRenderer} implementation.
 * @param <T> the column value type
 */
final class DefaultFilterTableCellRenderer<T> extends DefaultTableCellRenderer implements FilterTableCellRenderer {

	private final Settings settings;
	private final ConditionModel<?> filter;
	private final boolean toolTipData;
	private final boolean columnShading;
	private final boolean alternateRowColoring;
	private final Function<T, String> string;
	private final ColorProvider<T> backgroundColor;
	private final ColorProvider<T> foregroundColor;

	/**
	 * @param builder the builder
	 * @param settings the UI settings for the renderer
	 */
	DefaultFilterTableCellRenderer(DefaultFilterTableCellRendererBuilder<T> builder, Settings settings) {
		this.settings = requireNonNull(settings);
		this.settings.updateColors();
		this.filter = builder.filter;
		this.toolTipData = builder.toolTipData;
		this.columnShading = builder.columnShading;
		this.alternateRowColoring = builder.alternateRowColoring;
		this.string = builder.string;
		this.backgroundColor = builder.backgroundColor;
		this.foregroundColor = builder.foregroundColor;
		setHorizontalAlignment(builder.horizontalAlignment);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (settings != null) {
			settings.updateColors();
		}
	}

	@Override
	public boolean columnShading() {
		return columnShading;
	}

	@Override
	public boolean alternateRowColoring() {
		return alternateRowColoring;
	}

	@Override
	public int horizontalAlignment() {
		return getHorizontalAlignment();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
																								 boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		FilterTable<?, ?> filterTable = (FilterTable<?, ?>) table;
		setForeground(settings.foregroundColor(foregroundColor == null ? null : foregroundColor.color(filterTable, row, (T) value)));
		setBackground(settings.backgroundColor(filter, row, columnShading, isSelected,
						backgroundColor == null ? null : backgroundColor.color(filterTable, row, (T) value)));
		setBorder(hasFocus || isSearchResult(filterTable.searchModel(), row, column) ? settings.focusedCellBorder() : settings.defaultCellBorder());
		if (toolTipData) {
			setToolTipText(value == null ? "" : value.toString());
		}

		return this;
	}

	/**
	 * @param value the value to set
	 */
	@Override
	protected void setValue(Object value) {
		super.setValue(string.apply((T) value));
	}

	/**
	 * @return the Settings instance
	 */
	Settings settings() {
		return settings;
	}

	private static boolean isSearchResult(FilterTableSearchModel searchModel, int row, int column) {
		return searchModel.currentResult().get().equals(row, column);
	}

	/**
	 * A default {@link FilterTableCellRenderer} implementation for Boolean values
	 */
	public static final class BooleanRenderer extends NullableCheckBox
					implements TableCellRenderer, javax.swing.plaf.UIResource, FilterTableCellRenderer {

		private final Settings settings;
		private final ConditionModel<?> filter;
		private final boolean columnShading;
		private final boolean alternateRowColoring;
		private final ColorProvider<Boolean> backgroundColor;
		private final ColorProvider<Boolean> foregroundColor;

		/**
		 * @param builder the builder
		 * @param settings the UI settings for the renderer
		 */
		BooleanRenderer(DefaultFilterTableCellRendererBuilder<Boolean> builder, Settings settings) {
			super(new NullableToggleButtonModel());
			this.settings = requireNonNull(settings);
			this.settings.updateColors();
			this.filter = builder.filter;
			this.columnShading = builder.columnShading;
			this.alternateRowColoring = builder.alternateRowColoring;
			this.backgroundColor = builder.backgroundColor;
			this.foregroundColor = builder.foregroundColor;
			setHorizontalAlignment(builder.horizontalAlignment);
			setBorderPainted(true);
		}

		@Override
		public void updateUI() {
			super.updateUI();
			if (settings != null) {
				settings.updateColors();
			}
		}

		@Override
		public boolean columnShading() {
			return columnShading;
		}

		@Override
		public boolean alternateRowColoring() {
			return alternateRowColoring;
		}

		@Override
		public int horizontalAlignment() {
			return getHorizontalAlignment();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
																									 boolean hasFocus, int row, int column) {
			model().toggleState().set((Boolean) value);
			FilterTable<?, ?> filterTable = (FilterTable<?, ?>) table;
			setForeground(settings.foregroundColor(foregroundColor == null ? null : foregroundColor.color(filterTable, row, (Boolean) value)));
			setBackground(settings.backgroundColor(filter, row, columnShading, isSelected,
							backgroundColor == null ? null : backgroundColor.color(filterTable, row, (Boolean) value)));
			setBorder(hasFocus || isSearchResult(filterTable.searchModel(), row, column) ? settings.focusedCellBorder() : settings.defaultCellBorder());

			return this;
		}
	}
}
