/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.ColorProvider;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Property;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.text.Format;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.LEFT;
import static javax.swing.SwingConstants.RIGHT;

/**
 * A TableCellRenderer with the added options of visually displaying if a
 * cell (or column) is involved in a condition and showing its contents in a tooltip.
 *
 * Provides TableCellRenderer implementations for EntityTablePanels via {@link #entityTableCellRenderer(SwingEntityTableModel, Property)}.
 */
public interface EntityTableCellRenderer extends TableCellRenderer {

  /**
   * @return true if the condition state should be represented visually
   */
  boolean isIndicateCondition();

  /**
   * If true then columns involved in a condition have different background color
   * @param indicateCondition the value
   */
  void setIndicateCondition(boolean indicateCondition);

  /**
   * @return if true then the cell data is added as a tool tip for the cell
   */
  boolean isTooltipData();

  /**
   * @param tooltipData if true then the cell data is added as a tool tip for the cell
   */
  void setTooltipData(boolean tooltipData);

  /**
   * Provides the foreground to use for cells in the given table.
   * @param table the table
   * @param selected true if the cell is selected
   * @return the foreground color
   */
  default Color getForeground(final JTable table, final boolean selected) {
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
   * Instantiates a new EntityTableCellRenderer for the given property
   * @param tableModel the table model
   * @param property the property
   * @param <T> the value type
   * @return a new EntityTableCellRenderer
   * @see ColorProvider
   * @see EntityDefinition.Builder#colorProvider(ColorProvider)
   */
  static <T> EntityTableCellRenderer entityTableCellRenderer(final SwingEntityTableModel tableModel, final Property<T> property) {
    return entityTableCellRenderer(tableModel, requireNonNull(property), property.getFormat(), property.getDateTimeFormatter(),
            property.getAttribute().isNumerical() || property.getAttribute().isTemporal() ? RIGHT : LEFT);
  }

  /**
   * Instantiates a new EntityTableCellRenderer based on the data provided by the given EntityTableModel
   * @param tableModel the table model providing the data to render
   * @param property the property
   * @param format overrides the format defined by the property
   * @param dateTimeFormatter the date/time formatter
   * @param horizontalAlignment the horizontal alignment
   * @param <T> the value type
   * @return a new EntityTableCellRenderer
   */
  static <T> EntityTableCellRenderer entityTableCellRenderer(final SwingEntityTableModel tableModel, final Property<T> property,
                                                             final Format format, final DateTimeFormatter dateTimeFormatter,
                                                             final int horizontalAlignment) {
    if (!Objects.equals(requireNonNull(tableModel).getEntityType(), requireNonNull(property).getEntityType())) {
      throw new IllegalArgumentException("Property " + property + " not found in entity : " + tableModel.getEntityType());
    }
    if (property.getAttribute().isBoolean()) {
      return new DefaultEntityTableCellRenderer.BooleanRenderer(tableModel, (Property<Boolean>) property);
    }

    return new DefaultEntityTableCellRenderer<>(tableModel, property, format, dateTimeFormatter, horizontalAlignment);
  }
}
