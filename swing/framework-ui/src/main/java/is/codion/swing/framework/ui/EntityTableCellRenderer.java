/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;
import is.codion.framework.domain.property.Property;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.text.Format;
import java.time.format.DateTimeFormatter;

/**
 * A TableCellRenderer with the added options of visually displaying if a
 * cell (or column) is involved in a condition and showing its contents in a tooltip.
 *
 * Provides TableCellRenderer implementations for EntityTablePanels via {@link #builder(SwingEntityTableModel, Property)}.
 */
public interface EntityTableCellRenderer extends TableCellRenderer {

  /**
   * The default left padding for table cells.<br>
   * Value type: Integer<br>
   * Default value: 0
   */
  PropertyValue<Integer> TABLE_CELL_LEFT_PADDING = Configuration.integerValue("codion.client.tableCellLeftPadding", 0);

  /**
   * The default right padding for table cells.<br>
   * Value type: Integer<br>
   * Default value: 5
   */
  PropertyValue<Integer> TABLE_CELL_RIGHT_PADDING = Configuration.integerValue("codion.client.tableCellRightPadding", 5);

  /**
   * @return true if the column condition state should be represented visually
   */
  boolean isDisplayConditionState();

  /**
   * Provides the foreground to use for cells in the given table.
   * @param table the table
   * @param row the row
   * @param selected true if the cell is selected
   * @return the foreground color
   */
  default Color getForeground(final JTable table, final int row, final boolean selected) {
    if (selected) {
      return table.getSelectionForeground();
    }

    return table.getForeground();
  }

  /**
   * Provides the background color for cells in the given table.
   * @param table the table
   * @param row the row
   * @param selected true if the cell is selected
   * @return the background color
   */
  default Color getBackground(final JTable table, final int row, final boolean selected) {
    if (selected) {
      return table.getSelectionBackground();
    }

    return table.getBackground();
  }

  /**
   * Instantiates a new {@link EntityTableCellRenderer.Builder} with defaults based on the given property.
   * @param tableModel the table model providing the data to render
   * @param property the property
   * @return a new {@link EntityTableCellRenderer.Builder} instance
   */
  static Builder builder(final SwingEntityTableModel tableModel, final Property<?> property) {
    return new DefaultEntityTableCellRenderer.DefaultBuilder(tableModel, property);
  }

  /**
   * Builds a {@link EntityTableCellRenderer}
   */
  interface Builder {

    /**
     * @param format overrides the format defined by the property
     * @return this builder instance
     */
    Builder format(Format format);

    /**
     * @param dateTimeFormatter the date/time formatter
     * @return this builder instance
     */
    Builder dateTimeFormatter(DateTimeFormatter dateTimeFormatter);

    /**
     * @param horizontalAlignment the horizontal alignment
     * @return this builder instance
     */
    Builder horizontalAlignment(int horizontalAlignment);

    /**
     * @param toolTipData true if the cell should display its contents in a tool tip
     * @return this builder instance
     */
    Builder toolTipData(boolean toolTipData);

    /**
     * @param displayConditionState true if true then cells/columns involved in a condition have different background color
     * @return this builder instance
     */
    Builder displayConditionState(boolean displayConditionState);

    /**
     * @param leftPadding the left cell padding
     * @return this builder instance
     */
    Builder leftPadding(int leftPadding);

    /**
     * @param rightPadding the right cell padding
     * @return this builder instance
     */
    Builder rightPadding(int rightPadding);

    /**
     * @return a new {@link EntityTableCellRenderer} instance based on this builder
     */
    EntityTableCellRenderer build();
  }
}
