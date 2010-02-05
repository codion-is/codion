/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.util.DateUtil;
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
 * The default TableCellRenderer implementation used by EntityTablePanel
 */
public class EntityTableCellRenderer implements TableCellRenderer {

  private final EntityTableModel tableModel;
  private final boolean rowColoring;

  private final Map<String, TableCellRenderer> renderers = new HashMap<String, TableCellRenderer>();

  private static final Color SINGLE_FILTERED_BACKGROUND = new Color(235, 235, 235);
  private static final Color DOUBLE_FILTERED_BACKGROUND = new Color(215, 215, 215);

  public EntityTableCellRenderer(final EntityTableModel tableModel, final boolean rowColoring) {
    if (tableModel == null)
      throw new IllegalArgumentException("EntityTableCellRenderer requires a EntityTableModel instance");
    this.rowColoring = rowColoring;
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
        case INT:
          return new NumberRenderer(property);
        case DATE:
          return new DateRenderer();
        case TIMESTAMP:
          return new TimestampRenderer();
        default:
          return new DefaultTableCellRenderer();
      }
  }

  /**
   * Default Renderers
   */
  public static class NumberRenderer extends DefaultTableCellRenderer {
    private final NumberFormat format;

    public NumberRenderer(final Property property) {
      this.format = NumberFormat.getInstance();
      if (property.getMaximumFractionDigits() != -1)
        this.format.setMaximumFractionDigits(property.getMaximumFractionDigits());
      this.format.setGroupingUsed(property.useNumberFormatGrouping());
      setHorizontalAlignment(JLabel.RIGHT);
    }

    @Override
    public void setValue(final Object value) {
      if (value instanceof String)
        setText((String) value);
      else
        setText((value == null) ? "" : format.format(value));
    }
  }

  public static class DateRenderer extends DefaultTableCellRenderer {
    private final DateFormat format;

    public DateRenderer() {
      this(DateUtil.getDefaultDateFormat());
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
  }

  public static class TimestampRenderer extends DefaultTableCellRenderer {
    private final DateFormat format;

    public TimestampRenderer() {
      this(DateUtil.getDefaultTimestampFormat());
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
  }

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
