/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.swing.common.model.component.table.FilteredTableModel;

import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.util.function.Function;

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
   * @return true if column shading is enabled
   */
  boolean isColumnShadingEnabled();

  /**
   * Instantiates a new {@link FilteredTableCellRenderer.Builder}.
   * @param <T> the table model type
   * @param <R> the table row type
   * @param <C> the column identifier type
   * @param tableModel the table model providing the data to render
   * @param columnIdentifier the column identifier
   * @param columnClass the column class
   * @return a new {@link FilteredTableCellRenderer.Builder} instance
   */
  static <T extends FilteredTableModel<R, C>, R, C> Builder<T, R, C> builder(T tableModel, C columnIdentifier, Class<?> columnClass) {
    return new DefaultFilteredTableCellRenderer.DefaultBuilder<>(tableModel, columnIdentifier, columnClass);
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
  interface Builder<T extends FilteredTableModel<R, C>, R, C> {

    /**
     * @param horizontalAlignment the horizontal alignment
     * @return this builder instance
     */
    Builder<T, R, C> horizontalAlignment(int horizontalAlignment);

    /**
     * @param toolTipData true if the cell should display its contents in a tool tip
     * @return this builder instance
     */
    Builder<T, R, C> toolTipData(boolean toolTipData);

    /**
     * @param columnShadingEnabled true if column specific shading should be enabled, for example to indicated that the column is involved in a search/filter
     * @return this builder instance
     */
    Builder<T, R, C> columnShadingEnabled(boolean columnShadingEnabled);

    /**
     * @param leftPadding the left cell padding
     * @return this builder instance
     */
    Builder<T, R, C> leftPadding(int leftPadding);

    /**
     * @param rightPadding the right cell padding
     * @return this builder instance
     */
    Builder<T, R, C> rightPadding(int rightPadding);

    /**
     * @param displayValueProvider provides the value to display in the cell, formatted or otherwise
     * @return this builder instance
     */
    Builder<T, R, C> displayValueProvider(Function<Object, Object> displayValueProvider);

    /**
     * @param cellColorProvider provides cell/row background and foreground color
     * @return this builder instance
     */
    Builder<T, R, C> cellColorProvider(CellColorProvider<C> cellColorProvider);

    /**
     * @return a new {@link FilteredTableCellRenderer} instance based on this builder
     */
    FilteredTableCellRenderer build();
  }
}
