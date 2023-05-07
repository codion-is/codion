/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableSearchModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.time.temporal.Temporal;
import java.util.Objects;
import java.util.function.Function;

import static is.codion.swing.common.ui.Colors.darker;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.*;

/**
 * A default {@link FilteredTableCellRenderer} implementation.
 * @param <T> the table model type
 * @param <R> the row type
 * @param <C> the column identifier type
 */
public class DefaultFilteredTableCellRenderer<T extends FilteredTableModel<R, C>, R, C> extends DefaultTableCellRenderer implements FilteredTableCellRenderer {

  private final Settings<T, C> settings;
  private final T tableModel;
  private final C columnIdentifier;
  private final boolean toolTipData;
  private final boolean displayCondition;
  private final Function<Object, Object> displayValueProvider;
  private final CellColorProvider<C> cellColorProvider;

  /**
   * @param builder the builder
   * @param settings the UI settings for the renderer
   */
  public DefaultFilteredTableCellRenderer(DefaultBuilder<T, R, C> builder, Settings<T, C> settings) {
    this.tableModel = requireNonNull(builder).tableModel;
    this.settings = requireNonNull(settings);
    this.settings.updateColors();
    this.columnIdentifier = builder.columnIdentifier;
    this.toolTipData = builder.toolTipData;
    this.displayCondition = builder.displayCondition;
    this.displayValueProvider = builder.displayValueProvider;
    this.cellColorProvider = builder.cellColorProvider;
    setHorizontalAlignment(builder.horizontalAlignment);
  }

  @Override
  public final void updateUI() {
    super.updateUI();
    if (settings != null) {
      settings.updateColors();
    }
  }

  @Override
  public final boolean isDisplayCondition() {
    return displayCondition;
  }

  @Override
  public final Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                 boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    setForeground(settings.foregroundColor(columnIdentifier, row, isSelected, cellColorProvider));
    setBackground(settings.backgroundColor(tableModel, row, columnIdentifier, isDisplayCondition(), isSelected, cellColorProvider));
    setBorder(hasFocus || isSearchResult(row, column, tableModel) ? settings.focusedCellBorder : settings.defaultCellBorder);
    if (toolTipData) {
      setToolTipText(value == null ? "" : value.toString());
    }

    return this;
  }

  /**
   * @param value the value to set
   */
  @Override
  protected final void setValue(Object value) {
    super.setValue(displayValueProvider.apply(value));
  }

  private static boolean isSearchResult(int row, int column, FilteredTableModel<?, ?> tableModel) {
    FilteredTableSearchModel.RowColumn searchResult = tableModel.searchModel().currentResult();

    return searchResult.row() == row && searchResult.column() == column;
  }

  private static final class DefaultDisplayValueProvider implements Function<Object, Object> {
    @Override
    public Object apply(Object value) {
      return Objects.toString(value);
    }
  }

  private static final class DefaultCellColorProvider<C> implements CellColorProvider<C> {

    @Override
    public Color backgroundColor(int row, C columnIdentifier, boolean selected) {
      return null;
    }

    @Override
    public Color foregroundColor(int row, C columnIdentifier, boolean selected) {
      return null;
    }
  }

  /**
   * A default {@link FilteredTableCellRenderer} implementation for Boolean values
   * @param <T> the tabel model type
   * @param <R> the row type
   * @param <C> the column identifier type
   */
  public static final class BooleanRenderer<T extends FilteredTableModel<R, C>, R, C> extends NullableCheckBox
          implements TableCellRenderer, javax.swing.plaf.UIResource, FilteredTableCellRenderer {

    private final Settings<T, C> settings;
    private final T tableModel;
    private final C columnIdentifier;
    private final boolean displayCondition;
    private final CellColorProvider<C> cellColorProvider;

    /**
     * @param builder the builder
     * @param settings the UI settings for the renderer
     */
    public BooleanRenderer(DefaultBuilder<T, R, C> builder, Settings<T, C> settings) {
      super(new NullableToggleButtonModel());
      this.tableModel = requireNonNull(requireNonNull(builder).tableModel, "tableModel");
      this.settings = requireNonNull(settings);
      this.settings.updateColors();
      this.columnIdentifier = requireNonNull(builder.columnIdentifier, "property");
      this.displayCondition = builder.displayCondition;
      this.cellColorProvider = builder.cellColorProvider;
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
    public boolean isDisplayCondition() {
      return displayCondition;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      getNullableModel().setState((Boolean) value);
      setForeground(settings.foregroundColor(columnIdentifier, row, isSelected, cellColorProvider));
      setBackground(settings.backgroundColor(tableModel, row, columnIdentifier, isDisplayCondition(), isSelected, cellColorProvider));
      setBorder(hasFocus || isSearchResult(row, column, tableModel) ? settings.focusedCellBorder : settings.defaultCellBorder);

      return this;
    }
  }

  /**
   * Settings for a {@link FilteredTableCellRenderer}
   * @param <T> the table model type
   * @param <C> the column identifier type
   */
  public static class Settings<T extends FilteredTableModel<?, C>, C> {

    protected static final float SELECTION_COLOR_BLEND_RATIO = 0.5f;
    protected static final double DARKENING_FACTOR = 0.9;
    protected static final double DOUBLE_DARKENING_FACTOR = 0.8;
    protected static final int FOCUSED_CELL_BORDER_THICKNESS = 1;

    private final int leftPadding;
    private final int rightPadding;

    private Color foregroundColor;
    private Color backgroundColor;
    private Color backgroundColorSearch;
    private Color alternateBackgroundColor;
    private Color alternateBackgroundColorSearch;
    private Color selectionBackground;
    private Color alternateSelectionBackground;
    private Border defaultCellBorder;
    private Border focusedCellBorder;

    protected Settings(int leftPadding, int rightPadding) {
      this.leftPadding = leftPadding;
      this.rightPadding = rightPadding;
    }

    protected void updateColors() {
      foregroundColor = UIManager.getColor("Table.foreground");
      backgroundColor = UIManager.getColor("Table.background");
      alternateBackgroundColor = UIManager.getColor("Table.alternateRowColor");
      if (alternateBackgroundColor == null) {
        alternateBackgroundColor = darker(backgroundColor, DOUBLE_DARKENING_FACTOR);
      }
      selectionBackground = UIManager.getColor("Table.selectionBackground");
      backgroundColorSearch = darker(backgroundColor, DARKENING_FACTOR);
      alternateBackgroundColorSearch = darker(alternateBackgroundColor, DARKENING_FACTOR);
      alternateSelectionBackground = darker(selectionBackground, DARKENING_FACTOR);
      defaultCellBorder = leftPadding > 0 || rightPadding > 0 ? createEmptyBorder(0, leftPadding, 0, rightPadding) : null;
      focusedCellBorder = createFocusedCellBorder(foregroundColor, defaultCellBorder);
    }

    protected Color backgroundColor(T tableModel, int row, C columnIdentifier,
                                    boolean indicateCondition, boolean selected,
                                    CellColorProvider<C> cellColorProvider) {
      ColumnConditionModel<?, ?> filterModel = tableModel.columnFilterModels().get(columnIdentifier);
      boolean filterEnabled = filterModel != null && filterModel.isEnabled();
      boolean showCondition = indicateCondition && filterEnabled;
      Color cellBackgroundColor = cellBackgroundColor(cellColorProvider.backgroundColor(row, columnIdentifier, selected), row, selected);
      if (showCondition) {
        return conditionEnabledColor(row, cellBackgroundColor);
      }
      if (cellBackgroundColor != null) {
        return cellBackgroundColor;
      }

      return isEven(row) ? backgroundColor : alternateBackgroundColor;
    }

    protected final Color backgroundColor() {
      return backgroundColor;
    }

    protected final Color backgroundColorSearch() {
      return backgroundColorSearch;
    }

    protected final Color alternateBackgroundColor() {
      return alternateBackgroundColor;
    }

    protected final Color alternateBackgroundColorSearch() {
      return alternateBackgroundColorSearch;
    }

    protected final Color cellBackgroundColor(Color cellSpecificBackgroundColor, int row, boolean selected) {
      if (selected) {
        return cellSpecificBackgroundColor == null ?
                selectionBackgroundColor(row) :
                blendColors(cellSpecificBackgroundColor, selectionBackgroundColor(row));
      }

      return cellSpecificBackgroundColor;
    }

    private Color foregroundColor(C columnIdentifier, int row, boolean selected, CellColorProvider<C> cellColorProvider) {
      Color cellColor = cellColorProvider.foregroundColor(row, columnIdentifier, selected);

      return cellColor == null ? foregroundColor : cellColor;
    }

    private Color selectionBackgroundColor(int row) {
      return isEven(row) ? selectionBackground : alternateSelectionBackground;
    }

    private Color conditionEnabledColor(int row, Color cellColor) {
      if (cellColor != null) {
        return darker(cellColor, DARKENING_FACTOR);
      }

      return isEven(row) ? backgroundColorSearch : alternateBackgroundColorSearch;
    }

    protected static boolean isEven(int row) {
      return row % 2 == 0;
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

  public static class DefaultBuilder<T extends FilteredTableModel<R, C>, R, C> implements Builder<T, R, C> {

    private final T tableModel;
    private final C columnIdentifier;
    private final Class<?> columnClass;

    private int horizontalAlignment;
    private boolean toolTipData;
    private boolean displayCondition = true;
    private int leftPadding = FilteredTableCellRenderer.TABLE_CELL_LEFT_PADDING.get();
    private int rightPadding = FilteredTableCellRenderer.TABLE_CELL_RIGHT_PADDING.get();
    private Function<Object, Object> displayValueProvider = new DefaultDisplayValueProvider();
    private CellColorProvider<C> cellColorProvider = new DefaultCellColorProvider<>();

    protected DefaultBuilder(T tableModel, C columnIdentifier, Class<?> columnClass) {
      this.tableModel = requireNonNull(tableModel);
      this.columnIdentifier = requireNonNull(columnIdentifier);
      this.columnClass = requireNonNull(columnClass);
      this.horizontalAlignment = defaultHorizontalAlignment();
    }

    @Override
    public final Builder<T, R, C> horizontalAlignment(int horizontalAlignment) {
      this.horizontalAlignment = horizontalAlignment;
      return this;
    }

    @Override
    public final Builder<T, R, C> toolTipData(boolean toolTipData) {
      this.toolTipData = toolTipData;
      return this;
    }

    @Override
    public final Builder<T, R, C> displayCondition(boolean displayCondition) {
      this.displayCondition = displayCondition;
      return this;
    }

    @Override
    public final Builder<T, R, C> leftPadding(int leftPadding) {
      this.leftPadding = leftPadding;
      return null;
    }

    @Override
    public final Builder<T, R, C> rightPadding(int rightPadding) {
      this.rightPadding = rightPadding;
      return this;
    }

    @Override
    public final Builder<T, R, C> displayValueProvider(Function<Object, Object> displayValueProvider) {
      this.displayValueProvider = requireNonNull(displayValueProvider);
      return this;
    }

    @Override
    public final Builder<T, R, C> cellColorProvider(CellColorProvider<C> cellColorProvider) {
      this.cellColorProvider = requireNonNull(cellColorProvider);
      return this;
    }

    @Override
    public FilteredTableCellRenderer build() {
      if (Boolean.class.equals(columnClass)) {
        return new BooleanRenderer<>(this, new Settings<T, C>(leftPadding, rightPadding));
      }

      return new DefaultFilteredTableCellRenderer<>(this, new Settings<T, C>(leftPadding, rightPadding));
    }

    protected final int defaultHorizontalAlignment() {
      if (Boolean.class.equals(columnClass)) {
        return FilteredTableCellRenderer.BOOLEAN_HORIZONTAL_ALIGNMENT.get();
      }
      if (Number.class.isAssignableFrom(columnClass)) {
        return FilteredTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.get();
      }
      if (Temporal.class.isAssignableFrom(columnClass)) {
        return FilteredTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.get();
      }

      return FilteredTableCellRenderer.HORIZONTAL_ALIGNMENT.get();
    }

    protected int leftPadding() {
      return leftPadding;
    }

    protected int rightPadding() {
      return rightPadding;
    }
  }
}
