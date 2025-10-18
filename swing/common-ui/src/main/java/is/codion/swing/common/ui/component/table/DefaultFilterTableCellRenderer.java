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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.state.ObservableState;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;

import org.jspecify.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.time.temporal.Temporal;
import java.util.function.Function;

import static is.codion.swing.common.model.component.button.NullableToggleButtonModel.nullableToggleButtonModel;
import static is.codion.swing.common.ui.color.Colors.darker;
import static is.codion.swing.common.ui.component.table.FilterTableCellRenderer.DefaultUISettings.DOUBLE_DARKENING_FACTOR;
import static is.codion.swing.common.ui.component.table.FilterTableCellRenderer.DefaultUISettings.blendColors;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableCellRenderer<R, C, T> extends DefaultTableCellRenderer implements FilterTableCellRenderer<T> {

	private final Settings<R, C, T> settings;
	private final Class<T> columnClass;
	private final @Nullable TableCellRenderer renderer;

	DefaultFilterTableCellRenderer(Settings<R, C, T> settings, Class<T> columnClass, @Nullable TableCellRenderer renderer) {
		this.settings = settings;
		this.columnClass = columnClass;
		this.renderer = renderer;
		this.settings.update();
		setHorizontalAlignment(settings.horizontalAlignment);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (renderer instanceof JComponent) {
			((JComponent) renderer).updateUI();
		}
		if (settings != null) {
			settings.update();
		}
	}

	@Override
	public Class<T> columnClass() {
		return columnClass;
	}

	@Override
	public boolean filterIndicator() {
		return settings.filterIndicator;
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
		JComponent component = (JComponent) component(table, value, isSelected, hasFocus, row, column);
		settings.configure((FilterTable<R, C>) table, component, (T) value, isSelected, hasFocus, row, column);
		if (settings.toolTipData) {
			setToolTipText(settings.formatter.apply((T) value));
		}

		return component;
	}

	@Override
	protected void setValue(Object value) {
		setText(settings.formatter.apply((T) value));
	}

	UISettings settings() {
		return settings.uiSettings;
	}

	private Component component(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		return renderer == null ?
						super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) :
						renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

	/**
	 * A default {@link FilterTableCellRenderer} implementation for Boolean values
	 */
	private static final class BooleanRenderer<R, C> extends NullableCheckBox
					implements TableCellRenderer, javax.swing.plaf.UIResource, FilterTableCellRenderer<Boolean> {

		private final Settings<R, C, Boolean> settings;

		BooleanRenderer(Settings<R, C, Boolean> settings) {
			super(nullableToggleButtonModel(), null, null);
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
		public Class<Boolean> columnClass() {
			return Boolean.class;
		}

		@Override
		public boolean filterIndicator() {
			return settings.filterIndicator;
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
			model().set((Boolean) value);
			settings.configure((FilterTable<R, C>) table, this, (Boolean) value, isSelected, hasFocus, row, column);
			if (settings.toolTipData) {
				setToolTipText(value == null ? "" : value.toString());
			}

			return this;
		}
	}

	private static final class Settings<R, C, T> {

		private final int leftPadding;
		private final int rightPadding;
		private final boolean alternateRowColoring;
		private final boolean filterIndicator;
		private final CellColor<R, C, T> backgroundColor;
		private final CellColor<R, C, T> foregroundColor;
		private final int horizontalAlignment;
		private final boolean toolTipData;
		private final Function<T, String> formatter;

		private final UISettings uiSettings;

		private @Nullable ObservableState filterEnabled;
		private boolean filterEnabledSet = false;

		private Settings(SettingsBuilder<R, C, T> builder) {
			this.uiSettings = builder.uiSettings;
			this.leftPadding = builder.leftPadding;
			this.rightPadding = builder.rightPadding;
			this.alternateRowColoring = builder.alternateRowColoring;
			this.filterIndicator = builder.filterIndicator;
			this.foregroundColor = builder.foregroundColor;
			this.backgroundColor = builder.backgroundColor;
			this.horizontalAlignment = builder.horizontalAlignment;
			this.toolTipData = builder.toolTipData;
			this.formatter = builder.formatter;
		}

		private void update() {
			uiSettings.update(leftPadding, rightPadding);
		}

		private void configure(FilterTable<R, C> filterTable, JComponent component, T value,
													 boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
			R row = filterTable.model().items().included().get(rowIndex);
			C identifier = filterTable.columnModel().getColumn(columnIndex).identifier();
			boolean alternateRow = alternateRow(rowIndex);
			Color foreground = foregroundColor(filterTable, row, identifier, value, isSelected);
			Color background = backgroundColor(filterTable, row, identifier, value, isSelected, alternateRow);
			Border border = border(filterTable, hasFocus, rowIndex, columnIndex);
			if (component instanceof JCheckBox) {
				((JCheckBox) component).setBorderPainted(true);
			}
			if (!(component instanceof NullableCheckBox)) {
				// Don't set the foreground color for NullableCheckBox,
				// since that is used as icon foreground when painted which
				// renders it invisible in case the background is the same
				component.setForeground(foreground);
			}
			component.setBackground(background);
			component.setBorder(border);
		}

		private Color foregroundColor(FilterTable<R, C> filterTable, R row, C identifier, T value, boolean selected) {
			Color foreground = foregroundColor.get(filterTable, row, identifier, value);
			if (foreground != null) {
				return foreground;
			}

			return selected ? uiSettings.selectionForeground() : uiSettings.foreground();
		}

		private Color backgroundColor(FilterTable<R, C> filterTable, R row, C identifier, T value, boolean selected, boolean alternateRow) {
			if (alternateRowColoring) {
				return backgroundAlternating(filterTable, row, identifier, value, selected, alternateRow);
			}

			return backgroundNonAlternating(filterTable, row, identifier, value, selected, alternateRow);
		}

		private Color backgroundAlternating(FilterTable<R, C> filterTable, R row, C identifier, T value, boolean selected, boolean alternateRow) {
			Color cellBackgroundColor = backgroundColor.get(filterTable, row, identifier, value);
			cellBackgroundColor = backgroundAlternating(cellBackgroundColor, alternateRow, selected);
			if (filterIndicator) {
				cellBackgroundColor = uiSettings.background(filterEnabled(filterTable, identifier), alternateRow, cellBackgroundColor);
			}
			if (cellBackgroundColor != null) {
				return cellBackgroundColor;
			}

			return alternateRow ? uiSettings.alternateBackground() : uiSettings.background();
		}

		private Color backgroundNonAlternating(FilterTable<R, C> filterTable, R row, C identifier, T value, boolean selected, boolean alternateRow) {
			Color cellBackgroundColor = backgroundColor.get(filterTable, row, identifier, value);
			cellBackgroundColor = backgroundNonAlternating(cellBackgroundColor, selected);
			if (filterIndicator) {
				cellBackgroundColor = uiSettings.background(filterEnabled(filterTable, identifier), false, cellBackgroundColor);
			}
			if (cellBackgroundColor != null) {
				return cellBackgroundColor;
			}
			if (uiSettings.alternateRowColor() == null) {
				return uiSettings.background();
			}
			// If alternate row coloring is enabled, respect it
			return alternateRow ? uiSettings.alternateRowColor() : uiSettings.background();
		}

		private Color backgroundAlternating(Color cellBackgroundColor, boolean alternateRow, boolean selected) {
			if (cellBackgroundColor != null && alternateRow) {
				cellBackgroundColor = darker(cellBackgroundColor, DOUBLE_DARKENING_FACTOR);
			}
			if (selected) {
				Color selectionBackground = alternateRow ? uiSettings.alternateSelectionBackground() : uiSettings.selectionBackground();
				if (cellBackgroundColor == null) {
					return selectionBackground;
				}

				return blendColors(cellBackgroundColor, selectionBackground);
			}

			return cellBackgroundColor;
		}

		private Color backgroundNonAlternating(Color cellBackgroundColor, boolean selected) {
			if (selected) {
				if (cellBackgroundColor == null) {
					return uiSettings.selectionBackground();
				}

				return blendColors(cellBackgroundColor, uiSettings.selectionBackground());
			}

			return cellBackgroundColor;
		}

		private Border border(FilterTable<?, ?> filterTable, boolean hasFocus, int rowIndex, int columnIndex) {
			return hasFocus || isSearchResult(filterTable.search(), rowIndex, columnIndex) ?
							uiSettings.focusedCellBorder() : uiSettings.defaultCellBorder();
		}

		private <C> boolean filterEnabled(FilterTable<?, C> filterTable, C identifier) {
			if (filterEnabledSet) {
				return filterEnabled != null && filterEnabled.is();
			}

			ConditionModel<?> filter = filterTable.model().filters().get().get(identifier);
			filterEnabled = filter == null ? null : filter.enabled();
			filterEnabledSet = true;

			return filterEnabled != null && filterEnabled.is();
		}

		private static boolean isSearchResult(FilterTableSearchModel searchModel, int rowIndex, int columnIndex) {
			return searchModel.results().current().getOrThrow().equals(rowIndex, columnIndex);
		}

		private static boolean alternateRow(int rowIndex) {
			return rowIndex % 2 != 0;
		}
	}

	static final class SettingsBuilder<R, C, T> {

		private static final CellColor<?, ?, ?> NULL_CELL_COLOR = (table, row, identifier, value) -> null;

		private UISettings uiSettings = new DefaultUISettings();
		private int leftPadding = TABLE_CELL_LEFT_PADDING.getOrThrow();
		private int rightPadding = TABLE_CELL_RIGHT_PADDING.getOrThrow();
		private boolean alternateRowColoring = ALTERNATE_ROW_COLORING.getOrThrow();
		private boolean filterIndicator = true;
		private CellColor<R, C, T> backgroundColor = (CellColor<R, C, T>) NULL_CELL_COLOR;
		private CellColor<R, C, T> foregroundColor = (CellColor<R, C, T>) NULL_CELL_COLOR;
		private boolean toolTipData = false;
		private Function<T, String> formatter = new DefaultFormatter<>();
		private int horizontalAlignment;

		private SettingsBuilder(int defaultHorizontalAlignment) {
			this.horizontalAlignment = defaultHorizontalAlignment;
		}

		SettingsBuilder<R, C, T> uiSettings(UISettings uiSettings) {
			this.uiSettings = requireNonNull(uiSettings);
			return this;
		}

		SettingsBuilder<R, C, T> leftPadding(int leftPadding) {
			this.leftPadding = leftPadding;
			return this;
		}

		SettingsBuilder<R, C, T> rightPadding(int rightPadding) {
			this.rightPadding = rightPadding;
			return this;
		}

		SettingsBuilder<R, C, T> alternateRowColoring(boolean alternateRowColoring) {
			this.alternateRowColoring = alternateRowColoring;
			return this;
		}

		SettingsBuilder<R, C, T> filterIndicator(boolean filterIndicator) {
			this.filterIndicator = filterIndicator;
			return this;
		}

		SettingsBuilder<R, C, T> backgroundColor(CellColor<R, C, T> backgroundColor) {
			this.backgroundColor = requireNonNull(backgroundColor);
			return this;
		}

		SettingsBuilder<R, C, T> foregroundColor(CellColor<R, C, T> foregroundColor) {
			this.foregroundColor = requireNonNull(foregroundColor);
			return this;
		}

		SettingsBuilder<R, C, T> horizontalAlignment(int horizontalAlignment) {
			this.horizontalAlignment = horizontalAlignment;
			return this;
		}

		SettingsBuilder<R, C, T> toolTipData(boolean toolTipData) {
			this.toolTipData = toolTipData;
			return this;
		}

		SettingsBuilder<R, C, T> formatter(Function<T, String> formatter) {
			this.formatter = requireNonNull(formatter);
			return this;
		}

		Settings<R, C, T> build() {
			return new Settings<>(this);
		}
	}

	private static final class DefaultFormatter<T> implements Function<T, String> {
		@Override
		public String apply(T value) {
			return value == null ? "" : value.toString();
		}
	}

	private static final class DefaultColumnClassStep implements Builder.ColumnClassStep {

		@Override
		public <R, C, T> Builder<R, C, T> columnClass(Class<T> columnClass) {
			return new DefaultBuilder<>(requireNonNull(columnClass));
		}
	}

	static final class DefaultBuilder<R, C, T> implements Builder<R, C, T> {

		static final Builder.ColumnClassStep COLUMN_CLASS = new DefaultColumnClassStep();

		private final SettingsBuilder<R, C, T> settings;
		private final Class<T> columnClass;

		private boolean useBooleanRenderer;
		private @Nullable TableCellRenderer renderer;

		private DefaultBuilder(Class<T> columnClass) {
			this.columnClass = requireNonNull(columnClass);
			this.useBooleanRenderer = Boolean.class.equals(columnClass);
			this.settings = new SettingsBuilder<>(defaultHorizontalAlignment(columnClass));
		}

		@Override
		public Builder<R, C, T> uiSettings(UISettings uiSettings) {
			this.settings.uiSettings(uiSettings);
			return this;
		}

		@Override
		public Builder<R, C, T> horizontalAlignment(int horizontalAlignment) {
			this.settings.horizontalAlignment(horizontalAlignment);
			return this;
		}

		@Override
		public Builder<R, C, T> toolTipData(boolean toolTipData) {
			this.settings.toolTipData(toolTipData);
			return this;
		}

		@Override
		public Builder<R, C, T> filterIndicator(boolean filterIndicator) {
			this.settings.filterIndicator(filterIndicator);
			return this;
		}

		@Override
		public Builder<R, C, T> alternateRowColoring(boolean alternateRowColoring) {
			this.settings.alternateRowColoring(alternateRowColoring);
			return this;
		}

		@Override
		public Builder<R, C, T> leftPadding(int leftPadding) {
			this.settings.leftPadding(leftPadding);
			return this;
		}

		@Override
		public Builder<R, C, T> rightPadding(int rightPadding) {
			this.settings.rightPadding(rightPadding);
			return this;
		}

		@Override
		public Builder<R, C, T> formatter(Function<T, String> formatter) {
			this.settings.formatter(formatter);
			return this;
		}

		@Override
		public Builder<R, C, T> background(CellColor<R, C, T> background) {
			this.settings.backgroundColor(background);
			return this;
		}

		@Override
		public Builder<R, C, T> foreground(CellColor<R, C, T> foreground) {
			this.settings.foregroundColor(foreground);
			return this;
		}

		@Override
		public Builder<R, C, T> renderer(TableCellRenderer renderer) {
			this.renderer = requireNonNull(renderer);
			this.useBooleanRenderer = false;
			return this;
		}

		@Override
		public FilterTableCellRenderer<T> build() {
			if (useBooleanRenderer) {
				return (FilterTableCellRenderer<T>) new BooleanRenderer<>((Settings<R, C, Boolean>) settings.build());
			}

			return new DefaultFilterTableCellRenderer<>(settings.build(), columnClass, renderer);
		}

		private int defaultHorizontalAlignment(Class<T> columnClass) {
			if (useBooleanRenderer) {
				return BOOLEAN_HORIZONTAL_ALIGNMENT.getOrThrow();
			}
			if (Number.class.isAssignableFrom(columnClass)) {
				return NUMERICAL_HORIZONTAL_ALIGNMENT.getOrThrow();
			}
			if (Temporal.class.isAssignableFrom(columnClass)) {
				return TEMPORAL_HORIZONTAL_ALIGNMENT.getOrThrow();
			}

			return HORIZONTAL_ALIGNMENT.getOrThrow();
		}
	}

	static final class DefaultFactory<R, C> implements Factory<R, C> {

		@Override
		public FilterTableCellRenderer<?> create(C identifier, FilterTableModel<R, C> tableModel) {
			return new DefaultBuilder<>(tableModel.getColumnClass(identifier)).build();
		}
	}
}
