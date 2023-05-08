/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;
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
  private final boolean columnShadingEnabled;
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
    this.columnShadingEnabled = builder.columnShadingEnabled;
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
  public final boolean isColumnShadingEnabled() {
    return columnShadingEnabled;
  }

  @Override
  public final Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    setForeground(settings.foregroundColor(cellColorProvider.foregroundColor(row, columnIdentifier, value, isSelected)));
    setBackground(settings.backgroundColor(tableModel, row, columnIdentifier, columnShadingEnabled, isSelected,
            cellColorProvider.backgroundColor(row, columnIdentifier, value, isSelected)));
    setBorder(hasFocus || isSearchResult(tableModel, row, column) ? settings.focusedCellBorder : settings.defaultCellBorder);
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

  private static boolean isSearchResult(FilteredTableModel<?, ?> tableModel, int row, int column) {
    return tableModel.searchModel().currentResult().equals(row, column);
  }

  private static final class DefaultDisplayValueProvider implements Function<Object, Object> {
    @Override
    public Object apply(Object value) {
      return Objects.toString(value);
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
    private final boolean columnShadingEnabled;
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
      this.columnShadingEnabled = builder.columnShadingEnabled;
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
    public boolean isColumnShadingEnabled() {
      return columnShadingEnabled;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      getNullableModel().setState((Boolean) value);
      setForeground(settings.foregroundColor(cellColorProvider.foregroundColor(row, columnIdentifier, value, isSelected)));
      setBackground(settings.backgroundColor(tableModel, row, columnIdentifier, columnShadingEnabled, isSelected,
              cellColorProvider.backgroundColor(row, columnIdentifier, value, isSelected)));
      setBorder(hasFocus || isSearchResult(tableModel, row, column) ? settings.focusedCellBorder : settings.defaultCellBorder);

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
    private Color backgroundColorShaded;
    private Color backgroundColorAlternate;
    private Color backgroundColorAlternateShaded;
    private Color selectionBackground;
    private Color selectionBackgroundAlternate;
    private Border defaultCellBorder;
    private Border focusedCellBorder;

    protected Settings(int leftPadding, int rightPadding) {
      this.leftPadding = leftPadding;
      this.rightPadding = rightPadding;
    }

    protected void updateColors() {
      foregroundColor = UIManager.getColor("Table.foreground");
      backgroundColor = UIManager.getColor("Table.background");
      backgroundColorAlternate = UIManager.getColor("Table.alternateRowColor");
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

    protected final Color backgroundColor(T tableModel, int row, C columnIdentifier, boolean columnShadingEnabled,
                                          boolean selected, Color cellBackgroundColor) {
      cellBackgroundColor = backgroundColor(cellBackgroundColor, row, selected);
      if (columnShadingEnabled) {
        cellBackgroundColor = backgroundColorShaded(tableModel, row, columnIdentifier, cellBackgroundColor);
      }
      if (cellBackgroundColor != null) {
        return cellBackgroundColor;
      }

      return isEven(row) ? backgroundColor : backgroundColorAlternate;
    }

    /**
     * Adds shading to the given cell, if applicable
     * @param tableModel the table model
     * @param row the row
     * @param columnIdentifier the column identifier
     * @param cellBackgroundColor the cell specific background color, if any
     * @return a shaded background color
     */
    protected Color backgroundColorShaded(T tableModel, int row, C columnIdentifier, Color cellBackgroundColor) {
      ColumnConditionModel<?, ?> filterModel = tableModel.columnFilterModels().get(columnIdentifier);
      boolean filterEnabled = filterModel != null && filterModel.isEnabled();
      if (filterEnabled) {
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

    private Color foregroundColor(Color cellForegroundColor) {
      return cellForegroundColor == null ? foregroundColor : cellForegroundColor;
    }

    private Color selectionBackgroundColor(int row) {
      return isEven(row) ? selectionBackground : selectionBackgroundAlternate;
    }

    private Color backgroundShaded(int row, Color cellBackgroundColor) {
      if (cellBackgroundColor != null) {
        return darker(cellBackgroundColor, DARKENING_FACTOR);
      }

      return isEven(row) ? backgroundColorShaded : backgroundColorAlternateShaded;
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
    private final boolean isBoolean;

    private int horizontalAlignment;
    private boolean toolTipData;
    private boolean columnShadingEnabled = true;
    private int leftPadding = FilteredTableCellRenderer.TABLE_CELL_LEFT_PADDING.get();
    private int rightPadding = FilteredTableCellRenderer.TABLE_CELL_RIGHT_PADDING.get();
    private Function<Object, Object> displayValueProvider = new DefaultDisplayValueProvider();
    private CellColorProvider<C> cellColorProvider = new CellColorProvider<C>() {};

    protected DefaultBuilder(T tableModel, C columnIdentifier, Class<?> columnClass) {
      this(tableModel, columnIdentifier, columnClass, Boolean.class.equals(requireNonNull(columnClass)));
    }

    protected DefaultBuilder(T tableModel, C columnIdentifier, Class<?> columnClass, boolean isBoolean) {
      this.tableModel = requireNonNull(tableModel);
      this.columnIdentifier = requireNonNull(columnIdentifier);
      this.columnClass = requireNonNull(columnClass);
      this.isBoolean = isBoolean;
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
    public final Builder<T, R, C> columnShadingEnabled(boolean columnShadingEnabled) {
      this.columnShadingEnabled = columnShadingEnabled;
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
    public final FilteredTableCellRenderer build() {
      return isBoolean ?
              new BooleanRenderer<>(this, settings(leftPadding, rightPadding)) :
              new DefaultFilteredTableCellRenderer<>(this, settings(leftPadding, rightPadding));
    }

    /**
     * @param leftPadding the left padding
     * @param rightPadding the right padding
     * @return the {@link Settings} instance for this renderer
     */
    protected Settings<T, C> settings(int leftPadding, int rightPadding) {
      return new Settings<>(leftPadding, rightPadding);
    }

    private int defaultHorizontalAlignment() {
      if (isBoolean) {
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
  }
}
