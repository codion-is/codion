/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The default TableCellRenderer implementation used by EntityTablePanel.
 */
public class EntityTableCellRenderer implements TableCellRenderer {

  private final EntityTableModel tableModel;
  private final boolean rowColoring;

  private final Map<String, TableCellRenderer> renderers = new HashMap<String, TableCellRenderer>();

  private static final Color SINGLE_FILTERED_BACKGROUND = new Color(235, 235, 235);
  private static final Color DOUBLE_FILTERED_BACKGROUND = new Color(215, 215, 215);

  public EntityTableCellRenderer(final EntityTableModel tableModel) {
    if (tableModel == null)
      throw new IllegalArgumentException("EntityTableCellRenderer requires a EntityTableModel instance");
    this.rowColoring = EntityRepository.isRowColoring(tableModel.getEntityID());
    this.tableModel = tableModel;
  }

  /** {@inheritDoc} */
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                 final boolean hasFocus, final int row, final int column) {
    final Property property = tableModel.getColumnProperty(column);
    final Component component =
            getRenderer(property).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    if (isSelected)
      return component;

    final boolean propertySearchEnabled = tableModel.getSearchModel().isSearchEnabled(property.getPropertyID());
    final boolean propertyFilterEnabled = tableModel.getSearchModel().isFilterEnabled(property.getPropertyID());
    final Color rowColor = rowColoring ? tableModel.getRowBackgroundColor(row) : null;
    if (rowColor == null && !(propertySearchEnabled || propertyFilterEnabled)) {
      component.setBackground(table.getBackground());
      component.setForeground(table.getForeground());
    }
    else {
      if (rowColor != null)
        component.setBackground(rowColor);
      else
        component.setBackground((propertySearchEnabled && propertyFilterEnabled) ? DOUBLE_FILTERED_BACKGROUND : SINGLE_FILTERED_BACKGROUND);
    }

    return component;
  }

  protected TableCellRenderer getRenderer(final Property columnProperty) {
    TableCellRenderer renderer = renderers.get(columnProperty.getPropertyID());
    if (renderer == null)
      renderers.put(columnProperty.getPropertyID(), renderer = initializeRenderer(columnProperty));

    return renderer;
  }

  protected TableCellRenderer initializeRenderer(final Property property) {
    switch (property.getPropertyType()) {
      case BOOLEAN:
        return new BooleanRenderer();
      case DOUBLE:
        return new DoubleRenderer(property);
      case INT:
        return new IntegerRenderer(property);
      case DATE:
        return new DateRenderer(property);
      case TIMESTAMP:
        return new TimestampRenderer(property);
      default:
        return new DefaultTableCellRenderer();
    }
  }

  /**
   * A cell renderer for doubles.
   */
  public static class DoubleRenderer extends DefaultTableCellRenderer {
    private final NumberFormat format;

    public DoubleRenderer(final Property property) {
      this(initNumberFormat(property));
    }

    public DoubleRenderer(final NumberFormat format) {
      this.format = format;
      setHorizontalAlignment(JLabel.RIGHT);
    }

    @Override
    public void setValue(final Object value) {
      if (value instanceof String)
        setText((String) value);
      else
        setText((value == null) ? "" : format.format(value));
    }

    private static NumberFormat initNumberFormat(final Property property) {
      if (property.getFormat() != null)
        return (NumberFormat) property.getFormat();

      final NumberFormat format = NumberFormat.getInstance();
      if (property.getMaximumFractionDigits() != -1)
        format.setMaximumFractionDigits(property.getMaximumFractionDigits());
      format.setGroupingUsed(property.useNumberFormatGrouping());

      return format;
    }
  }

  /**
   * A cell renderer for integers.
   */
  public static class IntegerRenderer extends DefaultTableCellRenderer {
    private final NumberFormat format;

    public IntegerRenderer(final Property property) {
      this(initNumberFormat(property));
    }

    public IntegerRenderer(final NumberFormat format) {
      this.format = format;
      setHorizontalAlignment(JLabel.RIGHT);
    }

    @Override
    public void setValue(final Object value) {
      if (value instanceof String)
        setText((String) value);
      else
        setText((value == null) ? "" : format.format(value));
    }

    private static NumberFormat initNumberFormat(final Property property) {
      if (property.getFormat() != null)
        return (NumberFormat) property.getFormat();

      final NumberFormat format = NumberFormat.getIntegerInstance();
      format.setGroupingUsed(property.useNumberFormatGrouping());

      return format;
    }
  }

  /**
   * A cell renderer for dates.
   */
  public static class DateRenderer extends DefaultTableCellRenderer {
    private final DateFormat format;

    public DateRenderer(final Property property) {
      this(initDateFormat(property));
    }

    public DateRenderer(final DateFormat format) {
      this.format =  format;
      setHorizontalAlignment(JLabel.RIGHT);
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(final Object value) {
      String txt = "";
      if (value != null && value instanceof Date)
        txt = format.format(value);
      else if (value instanceof String)
        txt = (String) value;

      setText(txt);
    }

    private static DateFormat initDateFormat(final Property property) {
      return property.getFormat() == null ? Configuration.getDefaultDateFormat() : (DateFormat) property.getFormat();
    }
  }

  /**
   * A cell renderer for timestamps.
   */
  public static class TimestampRenderer extends DefaultTableCellRenderer {
    private final DateFormat format;

    public TimestampRenderer(final Property property) {
      this(initTimestampFormat(property));
    }

    public TimestampRenderer(final DateFormat format) {
      this.format = format;
      setHorizontalAlignment(JLabel.RIGHT);
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(final Object value) {
      String txt = "";
      if (value != null && value instanceof Date)
        txt = format.format(value);
      else if (value instanceof String)
        txt = (String) value;

      setText(txt);
    }

    private static DateFormat initTimestampFormat(final Property property) {
      return property.getFormat() == null ? Configuration.getDefaultTimestampFormat() : (DateFormat) property.getFormat();
    }
  }

  /**
   * A cell renderer for booleans.
   */
  public static class BooleanRenderer extends JCheckBox implements TableCellRenderer {

    /** Constructs a new BooleanRenderer. */
    public BooleanRenderer() {
      setHorizontalAlignment(JLabel.CENTER);
    }

    /** {@inheritDoc} */
    public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                   final boolean isSelected, final boolean hasFocus,
                                                   final int row, final int column) {
      if (value != null && !(value instanceof Boolean))
        throw new IllegalArgumentException("Non boolean value: " + value.getClass());

      setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
      setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

      setSelected(value != null && (Boolean) value);
      setEnabled(value != null);

      return this;
    }
  }
}
