/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Configuration;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.model.component.table.FilteredTableModel;

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
 * Provides TableCellRenderer implementations for FilteredTable via {@link #builder(FilteredTableModel, Object, Class)}.
 */
public interface FilteredTableCellRenderer extends TableCellRenderer {

  /**
   * The default left padding for table cells.<br>
   * Value type: Integer<br>
   * Default value: 0
   */
  PropertyValue<Integer> TABLE_CELL_LEFT_PADDING =
          Configuration.integerValue("is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.tableCellLeftPadding", 0);

  /**
   * The default right padding for table cells.<br>
   * Value type: Integer<br>
   * Default value: 5
   */
  PropertyValue<Integer> TABLE_CELL_RIGHT_PADDING =
          Configuration.integerValue("is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.tableCellRightPadding", 5);

  /**
   * The default horizontal alignment for numerical columns.<br>
   * Value type: Integer<br>
   * Default value: {@link SwingConstants#RIGHT}
   */
  PropertyValue<Integer> NUMERICAL_HORIZONTAL_ALIGNMENT =
          Configuration.integerValue("is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.tableNumericalHorizontalAlignment", SwingConstants.RIGHT);

  /**
   * The default horizontal alignment for temporal columns.<br>
   * Value type: Integer<br>
   * Default value: {@link SwingConstants#RIGHT}
   */
  PropertyValue<Integer> TEMPORAL_HORIZONTAL_ALIGNMENT =
          Configuration.integerValue("is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.tableTemporalHorizontalAlignment", SwingConstants.RIGHT);

  /**
   * The default horizontal alignment for boolean columns.<br>
   * Value type: Integer<br>
   * Default value: {@link SwingConstants#CENTER}
   */
  PropertyValue<Integer> BOOLEAN_HORIZONTAL_ALIGNMENT =
          Configuration.integerValue("is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.tableBooleanHorizontalAlignment", SwingConstants.CENTER);

  /**
   * The default horizontal alignment.<br>
   * Value type: Integer<br>
   * Default value: {@link SwingConstants#LEADING}
   */
  PropertyValue<Integer> HORIZONTAL_ALIGNMENT =
          Configuration.integerValue("is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.tableHorizontalAlignment", SwingConstants.LEADING);

  /**
   * Specifies whether alternate row coloring is enabled by default.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> ALTERNATE_ROW_COLORING =
          Configuration.booleanValue("is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.alternateRowColoring", true);

  /**
   * @return true if column shading is enabled
   */
  boolean isColumnShadingEnabled();

  /**
   * @return true if alternate row coloring is enabled
   */
  boolean isAlternateRowColoring();

  /**
   * Instantiates a new {@link FilteredTableCellRenderer.Builder}.
   * @param <R> the table row type
   * @param <C> the column identifier type
   * @param tableModel the table model providing the data to render
   * @param columnIdentifier the column identifier
   * @param columnClass the column class
   * @return a new {@link FilteredTableCellRenderer.Builder} instance
   */
  static <R, C> Builder<R, C> builder(FilteredTableModel<R, C> tableModel, C columnIdentifier, Class<?> columnClass) {
    return new DefaultFilteredTableCellRendererBuilder<>(tableModel, columnIdentifier, columnClass);
  }

  /**
   * Provides cell specific color.
   * @param <C> the column identifier type
   */
  interface CellColorProvider<C> {

    /**
     * @param row the row number
     * @param columnIdentifier the column identifier
     * @param cellValue the cell value
     * @param selected true if the cell is selected
     * @return a background Color for the given cell, null for none
     */
    default Color backgroundColor(int row, C columnIdentifier, Object cellValue, boolean selected) {
      return null;
    }

    /**
     * @param row the row number
     * @param columnIdentifier the column identifier
     * @param cellValue the cell value
     * @param selected true if the cell is selected
     * @return a foreground Color for the given cell, null for none
     */
    default Color foregroundColor(int row, C columnIdentifier, Object cellValue, boolean selected) {
      return null;
    }
  }

  /**
   * Builds a {@link FilteredTableCellRenderer}
   */
  interface Builder<R, C> {

    /**
     * @param horizontalAlignment the horizontal alignment
     * @return this builder instance
     */
    Builder<R, C> horizontalAlignment(int horizontalAlignment);

    /**
     * @param toolTipData true if the cell should display its contents in a tool tip
     * @return this builder instance
     */
    Builder<R, C> toolTipData(boolean toolTipData);

    /**
     * @param columnShadingEnabled true if column specific shading should be enabled, for example to indicated that the column is involved in a search/filter
     * @return this builder instance
     */
    Builder<R, C> columnShadingEnabled(boolean columnShadingEnabled);

    /**
     * @param alternateRowColoring true if alternate row coloring should be enabled
     * @return this builder instance
     */
    Builder<R, C> alternateRowColoring(boolean alternateRowColoring);

    /**
     * @param leftPadding the left cell padding
     * @return this builder instance
     */
    Builder<R, C> leftPadding(int leftPadding);

    /**
     * @param rightPadding the right cell padding
     * @return this builder instance
     */
    Builder<R, C> rightPadding(int rightPadding);

    /**
     * @param displayValueProvider provides the value to display in the cell, formatted or otherwise
     * @return this builder instance
     */
    Builder<R, C> displayValueProvider(Function<Object, Object> displayValueProvider);

    /**
     * @param cellColorProvider provides cell/row background and foreground color
     * @return this builder instance
     */
    Builder<R, C> cellColorProvider(CellColorProvider<C> cellColorProvider);

    /**
     * @return a new {@link FilteredTableCellRenderer} instance based on this builder
     */
    FilteredTableCellRenderer build();
  }

  /**
   * Settings for a {@link FilteredTableCellRenderer}
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

    protected final Color backgroundColor(FilteredTableModel<?, C> tableModel, int row, C columnIdentifier, boolean columnShadingEnabled,
                                          boolean selected, Color cellBackgroundColor) {
      cellBackgroundColor = backgroundColor(cellBackgroundColor, row, selected);
      if (columnShadingEnabled) {
        cellBackgroundColor = backgroundColorShaded(tableModel, row, columnIdentifier, cellBackgroundColor);
      }
      if (cellBackgroundColor != null) {
        return cellBackgroundColor;
      }

      return alternateRowColor(row) ? backgroundColor : backgroundColorAlternate;
    }

    /**
     * Adds shading to the given cell, if applicable
     * @param tableModel the table model
     * @param row the row
     * @param columnIdentifier the column identifier
     * @param cellBackgroundColor the cell specific background color, if any
     * @return a shaded background color
     */
    protected Color backgroundColorShaded(FilteredTableModel<?, C> tableModel, int row, C columnIdentifier, Color cellBackgroundColor) {
      ColumnConditionModel<?, ?> filterModel = tableModel.filterModel().conditionModels().get(columnIdentifier);
      boolean filterEnabled = filterModel != null && filterModel.enabled().get();
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

    private Color selectionBackgroundColor(int row) {
      return alternateRowColor(row) ? selectionBackground : selectionBackgroundAlternate;
    }

    private Color backgroundShaded(int row, Color cellBackgroundColor) {
      if (cellBackgroundColor != null) {
        return darker(cellBackgroundColor, DARKENING_FACTOR);
      }

      return alternateRowColor(row) ? backgroundColorShaded : backgroundColorAlternateShaded;
    }

    /**
     * @param row the row
     * @return true if the given row should use the alternate row color
     */
    protected boolean alternateRowColor(int row) {
      return alternateRowColoring && row % 2 == 0;
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
