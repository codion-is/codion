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
import is.codion.common.state.StateObserver;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.time.temporal.Temporal;
import java.util.function.Function;

import static is.codion.swing.common.ui.component.table.FilterTableCellRenderer.alternateRow;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableCellRenderer<T> extends DefaultTableCellRenderer implements FilterTableCellRenderer {

	private final Settings<T> settings;

	DefaultFilterTableCellRenderer(Settings<T> settings) {
		this.settings = requireNonNull(settings);
		this.settings.update();
		setHorizontalAlignment(settings.horizontalAlignment);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (settings != null) {
			settings.update();
		}
	}

	@Override
	public boolean columnShading() {
		return settings.columnShading;
	}

	@Override
	public boolean alternateRowColoring() {
		return settings.alternateRowColoring;
	}

	@Override
	public int horizontalAlignment() {
		return getHorizontalAlignment();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		settings.configure(this, (FilterTable<?, ?>) table, (T) value, isSelected, hasFocus, row, column);
		if (settings.toolTipData) {
			setToolTipText(settings.string.apply((T) value));
		}

		return this;
	}

	@Override
	protected void setValue(Object value) {
		setText(settings.string.apply((T) value));
	}

	UISettings settings() {
		return settings.uiSettings;
	}

	/**
	 * A default {@link FilterTableCellRenderer} implementation for Boolean values
	 */
	public static final class BooleanRenderer extends NullableCheckBox
					implements TableCellRenderer, javax.swing.plaf.UIResource, FilterTableCellRenderer {

		private final Settings<Boolean> settings;

		BooleanRenderer(Settings<Boolean> settings) {
			super(new NullableToggleButtonModel());
			this.settings = requireNonNull(settings);
			this.settings.update();
			setHorizontalAlignment(settings.horizontalAlignment);
			setBorderPainted(true);
		}

		@Override
		public void updateUI() {
			super.updateUI();
			if (settings != null) {
				settings.update();
			}
		}

		@Override
		public boolean columnShading() {
			return settings.columnShading;
		}

		@Override
		public boolean alternateRowColoring() {
			return settings.alternateRowColoring;
		}

		@Override
		public int horizontalAlignment() {
			return getHorizontalAlignment();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
																									 boolean hasFocus, int row, int column) {
			model().toggleState().set((Boolean) value);
			settings.configure(this, (FilterTable<?, ?>) table, (Boolean) value, isSelected, hasFocus, row, column);
			if (settings.toolTipData) {
				setToolTipText(value == null ? "" : value.toString());
			}

			return this;
		}
	}

	private static final class Settings<T> {

		private final int leftPadding;
		private final int rightPadding;
		private final boolean alternateRowColoring;
		private final boolean columnShading;
		private final ColorProvider<T> backgroundColorProvider;
		private final ColorProvider<T> foregroundColorProvider;
		private final int horizontalAlignment;
		private final boolean toolTipData;
		private final Function<T, String> string;

		private final UISettings uiSettings;

		private StateObserver filterEnabled;
		private boolean filterEnabledSet = false;

		private Settings(SettingsBuilder<T> builder) {
			this.uiSettings = builder.uiSettings;
			this.leftPadding = builder.leftPadding;
			this.rightPadding = builder.rightPadding;
			this.alternateRowColoring = builder.alternateRowColoring;
			this.columnShading = builder.columnShading;
			this.foregroundColorProvider = builder.foregroundColor;
			this.backgroundColorProvider = builder.backgroundColor;
			this.horizontalAlignment = builder.horizontalAlignment;
			this.toolTipData = builder.toolTipData;
			this.string = builder.string;
		}

		private void update() {
			uiSettings.update(leftPadding, rightPadding);
		}

		private void configure(FilterTableCellRenderer cellRenderer, FilterTable<?, ?> filterTable, T value,
													 boolean isSelected, boolean hasFocus, int row, int column) {
			requireNonNull(cellRenderer);
			requireNonNull(filterTable);
			Color foreground = foregroundColor(filterTable, value, row);
			Color background = backgroundColor(filterTable, value, isSelected, row, column);
			Border border = border(filterTable, hasFocus, row, column);
			if (cellRenderer instanceof DefaultTableCellRenderer) {
				DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) cellRenderer;
				renderer.setForeground(foreground);
				renderer.setBackground(background);
				renderer.setBorder(border);
			}
			else if (cellRenderer instanceof JComponent) {
				JComponent component = (JComponent) cellRenderer;
				component.setForeground(foreground);
				component.setBackground(background);
				component.setBorder(border);
			}
		}

		private Color foregroundColor(FilterTable<?, ?> filterTable, T value, int row) {
			return foregroundColorProvider == null ? uiSettings.foregroundColor() : foregroundColorProvider.color(filterTable, row, value);
		}

		private Color backgroundColor(FilterTable<?, ?> filterTable, T value, boolean selected, int row, int column) {
			Color cellBackgroundColor = null;
			if (backgroundColorProvider != null) {
				cellBackgroundColor = backgroundColorProvider.color(filterTable, row, value);
			}
			cellBackgroundColor = backgroundColor(cellBackgroundColor, row, selected);
			if (columnShading) {
				cellBackgroundColor = uiSettings.shadedBackgroundColor(filterEnabled(filterTable, column), row, cellBackgroundColor);
			}
			if (cellBackgroundColor != null) {
				return cellBackgroundColor;
			}
			if (alternateRowColoring) {
				return alternateRow(row) ? uiSettings.alternateBackgroundColor() : uiSettings.backgroundColor();
			}
			if (uiSettings.alternateRowColor() == null) {
				return uiSettings.backgroundColor();
			}
			// If alternate row coloring is enabled outside of the framework, respect it
			return alternateRow(row) ? uiSettings.alternateRowColor() : uiSettings.backgroundColor();
		}

		private Border border(FilterTable<?, ?> filterTable, boolean hasFocus, int row, int column) {
			return hasFocus || isSearchResult(filterTable.searchModel(), row, column) ? uiSettings.focusedCellBorder() : uiSettings.defaultCellBorder();
		}

		private <C> boolean filterEnabled(FilterTable<?, C> filterTable, int columnIndex) {
			if (filterEnabledSet) {
				return filterEnabled != null && filterEnabled.get();
			}

			ConditionModel<?> filter = filterTable.model().filters().get()
							.get(filterTable.columnModel().getColumn(columnIndex).identifier());
			filterEnabled = filter == null ? null : filter.enabled();
			filterEnabledSet = true;

			return filterEnabled != null && filterEnabled.get();
		}

		private static boolean isSearchResult(FilterTableSearchModel searchModel, int row, int column) {
			return searchModel.currentResult().get().equals(row, column);
		}

		private Color backgroundColor(Color cellBackgroundColor, int row, boolean selected) {
			if (selected) {
				if (cellBackgroundColor == null) {
					return selectionBackgroundColor(row);
				}

				return DefaultUISettings.blendColors(cellBackgroundColor, selectionBackgroundColor(row));
			}

			return cellBackgroundColor;
		}

		private Color selectionBackgroundColor(int row) {
			return alternateRow(row) ? uiSettings.alternateSelectionBackground() : uiSettings.selectionBackground();
		}
	}

	static final class SettingsBuilder<T> {

		private UISettings uiSettings = new DefaultUISettings();
		private int leftPadding = TABLE_CELL_LEFT_PADDING.get();
		private int rightPadding = TABLE_CELL_RIGHT_PADDING.get();
		private boolean alternateRowColoring = ALTERNATE_ROW_COLORING.get();
		private boolean columnShading = true;
		private ColorProvider<T> backgroundColor;
		private ColorProvider<T> foregroundColor;
		private boolean toolTipData = false;
		private Function<T, String> string = new DefaultString<>();
		private int horizontalAlignment;

		private SettingsBuilder(int defaultHorizontalAlignment) {
			this.horizontalAlignment = defaultHorizontalAlignment;
		}

		SettingsBuilder<T> uiSettings(UISettings uiSettings) {
			this.uiSettings = requireNonNull(uiSettings);
			return this;
		}

		SettingsBuilder<T> leftPadding(int leftPadding) {
			this.leftPadding = leftPadding;
			return this;
		}

		SettingsBuilder<T> rightPadding(int rightPadding) {
			this.rightPadding = rightPadding;
			return this;
		}

		SettingsBuilder<T> alternateRowColoring(boolean alternateRowColoring) {
			this.alternateRowColoring = alternateRowColoring;
			return this;
		}

		SettingsBuilder<T> columnShading(boolean columnShading) {
			this.columnShading = columnShading;
			return this;
		}

		SettingsBuilder<T> backgroundColor(ColorProvider<T> backgroundColor) {
			this.backgroundColor = requireNonNull(backgroundColor);
			return this;
		}

		SettingsBuilder<T> foregroundColor(ColorProvider<T> foregroundColor) {
			this.foregroundColor = requireNonNull(foregroundColor);
			return this;
		}

		SettingsBuilder<T> horizontalAlignment(int horizontalAlignment) {
			this.horizontalAlignment = horizontalAlignment;
			return this;
		}

		SettingsBuilder<T> toolTipData(boolean toolTipData) {
			this.toolTipData = toolTipData;
			return this;
		}

		SettingsBuilder<T> string(Function<T, String> string) {
			this.string = requireNonNull(string);
			return this;
		}

		Settings<T> build() {
			return new Settings<>(this);
		}
	}

	private static final class DefaultString<T> implements Function<T, String> {
		@Override
		public String apply(T value) {
			return value == null ? "" : value.toString();
		}
	}

	/**
	 * A default {@link Builder} implementation.
	 */
	static class DefaultBuilder<T> implements Builder<T> {

		private final SettingsBuilder<T> settings;
		private final boolean useBooleanRenderer;

		DefaultBuilder(Class<T> columnClass, boolean useBooleanRenderer) {
			this.settings = new SettingsBuilder<>(defaultHorizontalAlignment(requireNonNull(columnClass)));
			this.useBooleanRenderer = useBooleanRenderer;
		}

		@Override
		public final Builder<T> uiSettings(UISettings uiSettings) {
			this.settings.uiSettings(uiSettings);
			return this;
		}

		@Override
		public final Builder<T> horizontalAlignment(int horizontalAlignment) {
			this.settings.horizontalAlignment(horizontalAlignment);
			return this;
		}

		@Override
		public final Builder<T> toolTipData(boolean toolTipData) {
			this.settings.toolTipData(toolTipData);
			return this;
		}

		@Override
		public final Builder<T> columnShading(boolean columnShading) {
			this.settings.columnShading(columnShading);
			return this;
		}

		@Override
		public final Builder<T> alternateRowColoring(boolean alternateRowColoring) {
			this.settings.alternateRowColoring(alternateRowColoring);
			return this;
		}

		@Override
		public final Builder<T> leftPadding(int leftPadding) {
			this.settings.leftPadding(leftPadding);
			return this;
		}

		@Override
		public final Builder<T> rightPadding(int rightPadding) {
			this.settings.rightPadding(rightPadding);
			return this;
		}

		@Override
		public final Builder<T> string(Function<T, String> string) {
			this.settings.string(string);
			return this;
		}

		@Override
		public Builder<T> background(ColorProvider<T> background) {
			this.settings.backgroundColor(background);
			return this;
		}

		@Override
		public Builder<T> foreground(ColorProvider<T> foreground) {
			this.settings.foregroundColor(foreground);
			return this;
		}

		@Override
		public final FilterTableCellRenderer build() {
			return useBooleanRenderer ?
							new BooleanRenderer((Settings<Boolean>) this.settings.build()) :
							new DefaultFilterTableCellRenderer<>(this.settings.build());
		}

		private int defaultHorizontalAlignment(Class<T> columnClass) {
			if (useBooleanRenderer) {
				return BOOLEAN_HORIZONTAL_ALIGNMENT.get();
			}
			if (Number.class.isAssignableFrom(columnClass)) {
				return NUMERICAL_HORIZONTAL_ALIGNMENT.get();
			}
			if (Temporal.class.isAssignableFrom(columnClass)) {
				return TEMPORAL_HORIZONTAL_ALIGNMENT.get();
			}

			return HORIZONTAL_ALIGNMENT.get();
		}
	}
}
