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
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.utilities.format.LocaleDateTimePattern;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import static is.codion.swing.common.ui.color.Colors.darker;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.*;

final class DefaultFilterTableCellRenderer<R, C, T> extends DefaultTableCellRenderer implements FilterTableCellRenderer<R, C, T> {

	private static final double DARKENING_FACTOR = 0.9;
	private static final double DOUBLE_DARKENING_FACTOR = 0.8;
	private static final float SELECTION_COLOR_BLEND_RATIO = 0.5f;

	private final Settings<R, C, T> settings;
	private final Class<T> columnClass;
	private final @Nullable TableCellRenderer renderer;
	private final @Nullable ComponentValue<? extends JComponent, T> componentValue;

	DefaultFilterTableCellRenderer(Settings<R, C, T> settings, Class<T> columnClass, @Nullable TableCellRenderer renderer,
																 @Nullable ComponentValue<? extends JComponent, T> componentValue) {
		this.settings = settings;
		this.columnClass = columnClass;
		this.renderer = renderer;
		this.componentValue = componentValue;
		this.settings.update();
		setHorizontalAlignment(settings.horizontalAlignment);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (renderer instanceof JComponent) {
			((JComponent) renderer).updateUI();
		}
		if (componentValue != null) {
			componentValue.component().updateUI();
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
	public int horizontalAlignment() {
		return getHorizontalAlignment();
	}

	@Override
	public Border cellBorder() {
		return settings.uiSettings.cellBorder();
	}

	@Override
	public Collection<Customizer<R, C>> customizers() {
		return settings.customizers;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		JComponent component = component(table, value, isSelected, hasFocus, row, column);
		settings.configure((FilterTable<R, C>) table, component, (T) value, isSelected, hasFocus, row, column);
		if (settings.toolTip != null) {
			setToolTipText(settings.toolTip.apply((T) value));
		}

		return component;
	}

	@Override
	protected void setValue(Object value) {
		setText(settings.formatter.apply((T) value));
	}

	private JComponent component(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		if (componentValue != null) {
			componentValue.set((T) value);

			return componentValue.component();
		}
		if (renderer != null) {
			return (JComponent) renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}

		return (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

	private void paintBackground(FilterTable<R, C> filterTable, int row, int column, Graphics graphics) {
		paintBackground(filterTable, row, column, graphics,
						settings.backgroundColor(filterTable, row, filterTable.columnModel().getColumn(column).identifier(),
										component(filterTable, null, false, false, row, column)));
	}

	static <R, C> void paintBackground(FilterTable<R, C> filterTable, int row, int column, Graphics graphics, TableCellRenderer renderer) {
		if (renderer instanceof DefaultFilterTableCellRenderer) {
			((DefaultFilterTableCellRenderer<R, C, ?>) renderer).paintBackground(filterTable, row, column, graphics);
		}
		else if (renderer instanceof DefaultFilterTableCellRenderer.BooleanRenderer) {
			((DefaultFilterTableCellRenderer.BooleanRenderer<R, C>) renderer).paintBackground(filterTable, row, column, graphics);
		}
		else {
			Color color = renderer.getTableCellRendererComponent(filterTable, null, false, false, row, column).getBackground();
			paintBackground(filterTable, row, column, graphics, color);
		}
	}

	static void paintBackground(FilterTable<?, ?> filterTable, int row, int column, Graphics graphics, Color color) {
		Rectangle cellRect = filterTable.getCellRect(row, column, false);
		// cellRect for non-existing rows doesnt have height
		Rectangle rectToPaint = new Rectangle(cellRect.x, row * filterTable.getRowHeight(), cellRect.width, filterTable.getRowHeight());
		graphics.setColor(color);
		graphics.fillRect(rectToPaint.x, rectToPaint.y, rectToPaint.width, rectToPaint.height);
	}

	/**
	 * A default {@link FilterTableCellRenderer} implementation for Boolean values
	 */
	static final class BooleanRenderer<R, C> extends NullableCheckBox
					implements TableCellRenderer, javax.swing.plaf.UIResource, FilterTableCellRenderer<R, C, Boolean> {

		private final Settings<R, C, Boolean> settings;

		BooleanRenderer(Settings<R, C, Boolean> settings) {
			super(null, null);
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
		public int horizontalAlignment() {
			return getHorizontalAlignment();
		}

		@Override
		public Border cellBorder() {
			return settings.uiSettings.cellBorder();
		}

		@Override
		public Collection<Customizer<R, C>> customizers() {
			return settings.customizers;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
																									 boolean hasFocus, int row, int column) {
			set((Boolean) value);
			settings.configure((FilterTable<R, C>) table, this, (Boolean) value, isSelected, hasFocus, row, column);
			if (settings.toolTip != null) {
				setToolTipText(settings.toolTip.apply((Boolean) value));
			}

			return this;
		}

		private void paintBackground(FilterTable<R, C> filterTable, int row, int column, Graphics graphics) {
			DefaultFilterTableCellRenderer.paintBackground(filterTable, row, column, graphics,
							settings.backgroundColor(filterTable, row, filterTable.columnModel().getColumn(column).identifier(), this));
		}
	}

	private static final class Settings<R, C, T> {

		private final boolean alternateRowColoring;
		private final boolean filterIndicator;
		private final boolean focusedCellIndicator;
		private final boolean setBorder;
		private final CellColor<R, C, T> backgroundColor;
		private final CellColor<R, C, T> foregroundColor;
		private final Collection<Customizer<R, C>> customizers = new ArrayList<>();
		private final int horizontalAlignment;
		private final @Nullable Function<T, String> toolTip;
		private final Function<T, String> formatter;

		private UISettings<C> uiSettings;

		private Settings(SettingsBuilder<R, C, T> builder) {
			this.uiSettings = new UISettings<>(builder.leftPadding, builder.rightPadding);
			this.alternateRowColoring = builder.alternateRowColoring;
			this.filterIndicator = builder.filterIndicator;
			if (filterIndicator) {
				customizers.add(new FilterIndicator<>());
			}
			this.customizers.addAll(builder.customizers);
			this.backgroundColor = builder.backgroundColor;
			this.focusedCellIndicator = builder.focusedCellIndicator;
			this.setBorder = builder.setBorder;
			this.foregroundColor = builder.foregroundColor;
			this.horizontalAlignment = builder.horizontalAlignment;
			this.toolTip = builder.toolTip;
			this.formatter = builder.formatter;
		}

		private void update() {
			uiSettings = uiSettings.update();
		}

		private void configure(FilterTable<R, C> filterTable, JComponent component, T value,
													 boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
			R row = filterTable.model().items().included().get(rowIndex);
			C identifier = filterTable.columnModel().getColumn(columnIndex).identifier();
			boolean alternateRow = alternateRow(rowIndex);
			Color foreground = foregroundColor(filterTable, row, identifier, value, isSelected);
			Color background = backgroundColor(filterTable, row, identifier, value, isSelected, alternateRow);
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
			setComponentBorder(component, isSearchResult(filterTable.search(), rowIndex, columnIndex), hasFocus);
			for (Customizer<R, C> customizer : customizers) {
				if (customizer.enabled()) {
					customizer.customize(filterTable, identifier, component);
				}
			}
		}

		private Color foregroundColor(FilterTable<R, C> filterTable, R row, C identifier, T value, boolean selected) {
			Color foreground = foregroundColor.get(filterTable, row, identifier, value);
			if (foreground != null) {
				return foreground;
			}

			return selected ? uiSettings.selectionForeground() : uiSettings.foreground();
		}

		private Color backgroundColor(FilterTable<R, C> filterTable, int row, C identifier, JComponent component) {
			component.setBackground(backgroundColor(row));
			for (Customizer<R, C> customizer : customizers) {
				if (customizer.enabled()) {
					customizer.customize(filterTable, identifier, component);
				}
			}

			return component.getBackground();
		}

		private Color backgroundColor(int row) {
			boolean alternateRow = alternateRow(row);
			if (alternateRowColoring) {
				return alternateRow ? uiSettings.alternateBackground() : uiSettings.background();
			}
			else {
				if (uiSettings.alternateRowColor() == null) {
					return uiSettings.background();
				}
				// If UIManager's Table.alternateRowColor is set, respect it
				return alternateRow ? uiSettings.alternateRowColor() : uiSettings.background();
			}
		}

		private Color backgroundColor(FilterTable<R, C> filterTable, R row, C identifier, T value, boolean selected, boolean alternateRow) {
			return alternateRowColoring ?
							backgroundAlternating(filterTable, row, identifier, value, selected, alternateRow) :
							backgroundNonAlternating(filterTable, row, identifier, value, selected, alternateRow);
		}

		private Color backgroundAlternating(FilterTable<R, C> filterTable, R row, C identifier, T value, boolean selected, boolean alternateRow) {
			Color cellBackgroundColor = backgroundColor.get(filterTable, row, identifier, value);
			cellBackgroundColor = backgroundAlternating(cellBackgroundColor, alternateRow, selected);
			if (cellBackgroundColor != null) {
				return cellBackgroundColor;
			}

			return alternateRow ? uiSettings.alternateBackground() : uiSettings.background();
		}

		private Color backgroundNonAlternating(FilterTable<R, C> filterTable, R row, C identifier, T value, boolean selected, boolean alternateRow) {
			Color cellBackgroundColor = backgroundColor.get(filterTable, row, identifier, value);
			cellBackgroundColor = backgroundNonAlternating(cellBackgroundColor, selected);
			if (cellBackgroundColor != null) {
				return cellBackgroundColor;
			}
			if (uiSettings.alternateRowColor() == null) {
				return uiSettings.background();
			}
			// If UIManager's Table.alternateRowColor is set, respect it
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

		private static boolean isSearchResult(FilterTableSearchModel searchModel, int rowIndex, int columnIndex) {
			return searchModel.results().current().getOrThrow().equals(rowIndex, columnIndex);
		}

		private static boolean alternateRow(int rowIndex) {
			return rowIndex % 2 != 0;
		}

		private void setComponentBorder(JComponent component, boolean searchResult, boolean hasFocus) {
			if (searchResult) {
				component.setBorder(uiSettings.focusedCellBorder());
			}
			else if (setBorder) {
				if ((focusedCellIndicator && hasFocus)) {
					component.setBorder(uiSettings.focusedCellBorder());
				}
				else {
					component.setBorder(uiSettings.cellBorder());
				}
			}
		}

		private static Color blendColors(Color color1, Color color2) {
			int r = (int) (color1.getRed() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getRed() * SELECTION_COLOR_BLEND_RATIO);
			int g = (int) (color1.getGreen() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getGreen() * SELECTION_COLOR_BLEND_RATIO);
			int b = (int) (color1.getBlue() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getBlue() * SELECTION_COLOR_BLEND_RATIO);

			return new Color(r, g, b, color1.getAlpha());
		}
	}

	private static final class FilterIndicator<R, C> implements Customizer<R, C> {

		private @Nullable ObservableState filterEnabled;
		private boolean filterEnabledSet = false;

		@Override
		public boolean enabled() {
			return true;
		}

		@Override
		public void customize(FilterTable<R, C> table, C identifier, JComponent component) {
			if (filterEnabled(identifier, table.model())) {
				component.setBackground(darker(component.getBackground(), DARKENING_FACTOR));
			}
		}

		private boolean filterEnabled(C identifier, FilterTableModel<R, C> tableModel) {
			if (filterEnabledSet) {
				return filterEnabled != null && filterEnabled.is();
			}

			ConditionModel<?> filter = tableModel.filters().get().get(identifier);
			filterEnabled = filter == null ? null : filter.enabled();
			filterEnabledSet = true;

			return filterEnabled != null && filterEnabled.is();
		}
	}

	private static final class SettingsBuilder<R, C, T> {

		private static final CellColor<?, ?, ?> NULL_CELL_COLOR = (table, row, identifier, value) -> null;

		private static final LocalTimeFormatter TIME_FORMATTER = new LocalTimeFormatter();
		private static final LocalDateFormatter DATE_FORMATTER = new LocalDateFormatter();
		private static final LocalDateTimeFormatter DATE_TIME_FORMATTER = new LocalDateTimeFormatter();
		private static final OffsetDateTimeFormatter OFFSET_DATE_TIME_FORMATTER = new OffsetDateTimeFormatter();
		private static final DefaultFormatter<Object> FORMATTER = new DefaultFormatter<>();

		private final Collection<Customizer<R, C>> customizers = new ArrayList<>();

		private int leftPadding = TABLE_CELL_LEFT_PADDING.getOrThrow();
		private int rightPadding = TABLE_CELL_RIGHT_PADDING.getOrThrow();
		private boolean alternateRowColoring = ALTERNATE_ROW_COLORING.getOrThrow();
		private boolean filterIndicator = true;
		private boolean focusedCellIndicator = FOCUSED_CELL_INDICATOR.getOrThrow();
		private boolean setBorder = SET_BORDER.getOrThrow();
		private CellColor<R, C, T> backgroundColor = (CellColor<R, C, T>) NULL_CELL_COLOR;
		private CellColor<R, C, T> foregroundColor = (CellColor<R, C, T>) NULL_CELL_COLOR;
		private @Nullable Function<T, String> toolTip;
		private Function<T, String> formatter;
		private int horizontalAlignment;

		private SettingsBuilder(Class<T> columnClass) {
			this.horizontalAlignment = defaultHorizontalAlignment(columnClass);
			this.formatter = defaultFormatter(columnClass);
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

		SettingsBuilder<R, C, T> focusedCellIndicator(boolean focusedCellIndicator) {
			this.focusedCellIndicator = focusedCellIndicator;
			return this;
		}

		SettingsBuilder<R, C, T> setBorder(boolean setBorder) {
			this.setBorder = setBorder;
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

		SettingsBuilder<R, C, T> customizer(Customizer<R, C> customizer) {
			customizers.add(requireNonNull(customizer));
			return this;
		}

		SettingsBuilder<R, C, T> horizontalAlignment(int horizontalAlignment) {
			this.horizontalAlignment = horizontalAlignment;
			return this;
		}

		SettingsBuilder<R, C, T> toolTip(Function<T, String> toolTip) {
			this.toolTip = toolTip;
			return this;
		}

		SettingsBuilder<R, C, T> formatter(Function<T, String> formatter) {
			this.formatter = requireNonNull(formatter);
			return this;
		}

		Settings<R, C, T> build() {
			return new Settings<>(this);
		}

		private int defaultHorizontalAlignment(Class<T> columnClass) {
			if (Boolean.class.equals(columnClass)) {
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

		private Function<T, String> defaultFormatter(Class<T> columnClass) {
			if (columnClass.equals(LocalTime.class)) {
				return (Function<T, String>) TIME_FORMATTER;
			}
			else if (columnClass.equals(LocalDate.class)) {
				return (Function<T, String>) DATE_FORMATTER;
			}
			else if (columnClass.equals(LocalDateTime.class)) {
				return (Function<T, String>) DATE_TIME_FORMATTER;
			}
			else if (columnClass.equals(OffsetDateTime.class)) {
				return (Function<T, String>) OFFSET_DATE_TIME_FORMATTER;
			}

			return (Function<T, String>) FORMATTER;
		}
	}

	private static final class LocalTimeFormatter implements Function<LocalTime, String> {

		private final DateTimeFormatter formatter;

		private LocalTimeFormatter() {
			this.formatter = ofPattern(LocaleDateTimePattern.TIME_PATTERN.getOrThrow());
		}

		@Override
		public String apply(LocalTime localDate) {
			return localDate == null ? "" : formatter.format(localDate);
		}
	}

	private static final class LocalDateFormatter implements Function<LocalDate, String> {

		private final DateTimeFormatter formatter;

		private LocalDateFormatter() {
			this.formatter = ofPattern(LocaleDateTimePattern.DATE_PATTERN.getOrThrow());
		}

		@Override
		public String apply(LocalDate localDate) {
			return localDate == null ? "" : formatter.format(localDate);
		}
	}

	private static final class LocalDateTimeFormatter implements Function<LocalDateTime, String> {

		private final DateTimeFormatter formatter;

		private LocalDateTimeFormatter() {
			this.formatter = ofPattern(LocaleDateTimePattern.DATE_TIME_PATTERN.getOrThrow());
		}

		@Override
		public String apply(LocalDateTime localDate) {
			return localDate == null ? "" : formatter.format(localDate);
		}
	}

	private static final class OffsetDateTimeFormatter implements Function<OffsetDateTime, String> {

		private final DateTimeFormatter formatter;

		private OffsetDateTimeFormatter() {
			this.formatter = ofPattern(LocaleDateTimePattern.DATE_TIME_PATTERN.getOrThrow());
		}

		@Override
		public String apply(OffsetDateTime localDate) {
			return localDate == null ? "" : formatter.format(localDate);
		}
	}

	private static final class DefaultFormatter<T> implements Function<T, String> {
		@Override
		public String apply(T value) {
			return value == null ? "" : value.toString();
		}
	}

	static final class DefaultColumnClassStep<R, C> implements Builder.ColumnClassStep<R, C> {

		@Override
		public <T> Builder<R, C, T> columnClass(Class<T> columnClass) {
			return new DefaultBuilder<>(requireNonNull(columnClass));
		}
	}

	private static final class DefaultBuilder<R, C, T> implements Builder<R, C, T> {

		private final SettingsBuilder<R, C, T> settings;
		private final Class<T> columnClass;

		private boolean useBooleanRenderer;
		private @Nullable TableCellRenderer renderer;
		private @Nullable ComponentValue<? extends JComponent, T> componentValue;

		private DefaultBuilder(Class<T> columnClass) {
			this.columnClass = requireNonNull(columnClass);
			this.useBooleanRenderer = Boolean.class.equals(columnClass);
			this.settings = new SettingsBuilder<>(columnClass);
		}

		@Override
		public Builder<R, C, T> horizontalAlignment(int horizontalAlignment) {
			this.settings.horizontalAlignment(horizontalAlignment);
			return this;
		}

		@Override
		public Builder<R, C, T> toolTip(Function<T, String> toolTip) {
			this.settings.toolTip(toolTip);
			return this;
		}

		@Override
		public Builder<R, C, T> filterIndicator(boolean filterIndicator) {
			this.settings.filterIndicator(filterIndicator);
			return this;
		}

		@Override
		public Builder<R, C, T> focusedCellIndicator(boolean focusedCellIndicator) {
			this.settings.focusedCellIndicator(focusedCellIndicator);
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
		public Builder<R, C, T> customizer(Customizer<R, C> customizer) {
			this.settings.customizer(customizer);
			return this;
		}

		@Override
		public Builder<R, C, T> renderer(TableCellRenderer renderer) {
			if (componentValue != null) {
				throw new IllegalStateException("Component has already been set");
			}
			this.renderer = requireNonNull(renderer);
			this.useBooleanRenderer = false;
			return this;
		}

		@Override
		public Builder<R, C, T> component(ComponentValue<? extends JComponent, T> component) {
			if (renderer != null) {
				throw new IllegalStateException("Renderer has already been set");
			}
			this.componentValue = requireNonNull(component);
			this.useBooleanRenderer = false;
			return this;
		}

		@Override
		public FilterTableCellRenderer<R, C, T> build() {
			if (useBooleanRenderer) {
				return (FilterTableCellRenderer<R, C, T>) new BooleanRenderer<>((Settings<R, C, Boolean>) settings.build());
			}

			return new DefaultFilterTableCellRenderer<>(settings.build(), columnClass, renderer, componentValue);
		}
	}

	static final class DefaultFactory<R, C> implements Factory<R, C> {

		@Override
		public FilterTableCellRenderer<R, C, ?> create(C identifier, FilterTable<R, C> table) {
			requireNonNull(identifier);
			requireNonNull(table);

			return (FilterTableCellRenderer<R, C, ?>) new DefaultBuilder<>(table.model().getColumnClass(identifier)).build();
		}
	}

	private static final class UISettings<C> {

		private static final int FOCUSED_CELL_BORDER_THICKNESS = 1;

		private final int leftPadding;
		private final int rightPadding;
		private final Color foreground;
		private final Color background;
		private final Color alternateRowColor;
		private final Color alternateBackground;
		private final Color selectionForeground;
		private final Color selectionBackground;
		private final Color alternateSelectionBackground;
		private final Border cellBorder;
		private final Border focusedCellBorder;

		private UISettings(int leftPadding, int rightPadding) {
			this.leftPadding = leftPadding;
			this.rightPadding = rightPadding;
			foreground = UIManager.getColor("Table.foreground");
			background = UIManager.getColor("Table.background");
			alternateRowColor = UIManager.getColor("Table.alternateRowColor");
			alternateBackground = alternateRowColor == null ? darker(background, DOUBLE_DARKENING_FACTOR) : alternateRowColor;
			selectionForeground = UIManager.getColor("Table.selectionForeground");
			selectionBackground = UIManager.getColor("Table.selectionBackground");
			alternateSelectionBackground = darker(selectionBackground, DARKENING_FACTOR);
			cellBorder = createEmptyBorder(0, leftPadding, 0, rightPadding);
			focusedCellBorder = createFocusedCellBorder();
		}

		private UISettings<C> update() {
			return new UISettings<>(leftPadding, rightPadding);
		}

		private Color foreground() {
			return foreground;
		}

		private Color background() {
			return background;
		}

		private Color alternateRowColor() {
			return alternateRowColor;
		}

		private Color selectionForeground() {
			return selectionForeground;
		}

		private Color selectionBackground() {
			return selectionBackground;
		}

		private Color alternateBackground() {
			return alternateBackground;
		}

		private Color alternateSelectionBackground() {
			return alternateSelectionBackground;
		}

		private Border cellBorder() {
			return cellBorder;
		}

		private Border focusedCellBorder() {
			return focusedCellBorder;
		}

		private CompoundBorder createFocusedCellBorder() {
			return createCompoundBorder(createLineBorder(darker(foreground, DOUBLE_DARKENING_FACTOR),
							FOCUSED_CELL_BORDER_THICKNESS), cellBorder);
		}
	}
}
