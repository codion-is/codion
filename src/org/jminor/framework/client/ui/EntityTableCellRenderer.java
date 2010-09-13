/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityTableModel;
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

  private final Map<String, TableCellRenderer> renderers = new HashMap<String, TableCellRenderer>();

  private static final Color SINGLE_FILTERED_BACKGROUND = new Color(235, 235, 235);
  private static final Color DOUBLE_FILTERED_BACKGROUND = new Color(215, 215, 215);

  /**
   * Instantiates a new EntityTableCellRenderer
   * @param tableModel the table model
   * @see org.jminor.framework.domain.Entity.BackgroundColorProvider
   * @see org.jminor.framework.domain.EntityDefinition#setBackgroundColorProvider(org.jminor.framework.domain.Entity.BackgroundColorProvider)
   */
  public EntityTableCellRenderer(final EntityTableModel tableModel) {
    Util.rejectNullValue(tableModel, "tableModel");
    this.tableModel = tableModel;
  }

  /** {@inheritDoc} */
  public final Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                       final boolean hasFocus, final int row, final int column) {
    final Property property = (Property) tableModel.getColumnModel().getColumn(column).getIdentifier();
    final Component component =
            getRenderer(property).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    if (isSelected) {
      return component;
    }

    final boolean propertySearchEnabled = tableModel.getSearchModel().isSearchEnabled(property.getPropertyID());
    final boolean propertyFilterEnabled = tableModel.getSearchModel().isFilterEnabled(property.getPropertyID());
    final Color cellColor = tableModel.getPropertyBackgroundColor(row, property);
    if (cellColor == null && !(propertySearchEnabled || propertyFilterEnabled)) {
      component.setBackground(table.getBackground());
      component.setForeground(table.getForeground());
    }
    else {
      if (cellColor != null) {
        component.setBackground(cellColor);
      }
      else {
        component.setBackground((propertySearchEnabled && propertyFilterEnabled) ? DOUBLE_FILTERED_BACKGROUND : SINGLE_FILTERED_BACKGROUND);
      }
    }

    return component;
  }

  protected final TableCellRenderer getRenderer(final Property columnProperty) {
    TableCellRenderer renderer = renderers.get(columnProperty.getPropertyID());
    if (renderer == null) {
      renderer = initializeRenderer(columnProperty);
      renderers.put(columnProperty.getPropertyID(), renderer);
    }

    return renderer;
  }

  protected TableCellRenderer initializeRenderer(final Property property) {
    if (property.isBoolean()) {
      return new BooleanRenderer();
    }
    if (property.isDouble()) {
      return new DoubleRenderer(property);
    }
    if (property.isInteger()) {
      return new IntegerRenderer(property);
    }
    if (property.isDate()) {
      return new DateRenderer(property);
    }
    if (property.isTimestamp()) {
      return new TimestampRenderer(property);
    }
    else {
      return new DefaultTableCellRenderer();
    }
  }

  /**
   * A cell renderer for doubles.
   */
  public static final class DoubleRenderer extends DefaultTableCellRenderer {
    private final NumberFormat format;

    /**
     * Instantiates a new DoubleRenderer.
     * @param property the property to base this renderer on
     */
    public DoubleRenderer(final Property property) {
      this(initNumberFormat(property));
    }

    /**
     * Instantiates a new DoubleRenderer.
     * @param format the format
     */
    public DoubleRenderer(final NumberFormat format) {
      this.format = format;
      setHorizontalAlignment(JLabel.RIGHT);
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(final Object value) {
      if (value instanceof String) {
        setText((String) value);
      }
      else {
        setText((value == null) ? "" : format.format(value));
      }
    }

    private static NumberFormat initNumberFormat(final Property property) {
      return (NumberFormat) property.getFormat();
    }
  }

  /**
   * A cell renderer for integers.
   */
  public static final class IntegerRenderer extends DefaultTableCellRenderer {
    private final NumberFormat format;

    /**
     * Instantiates a new DoubleRenderer.
     * @param property the property to base this renderer on
     */
    public IntegerRenderer(final Property property) {
      this(initNumberFormat(property));
    }

    /**
     * Instantiates a new IntegerRenderer.
     * @param format the format
     */
    public IntegerRenderer(final NumberFormat format) {
      this.format = format;
      setHorizontalAlignment(JLabel.RIGHT);
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(final Object value) {
      if (value instanceof String) {
        setText((String) value);
      }
      else {
        setText((value == null) ? "" : format.format(value));
      }
    }

    private static NumberFormat initNumberFormat(final Property property) {
      return (NumberFormat) property.getFormat();
    }
  }

  /**
   * A cell renderer for dates.
   */
  public static final class DateRenderer extends DefaultTableCellRenderer {
    private final DateFormat format;

    /**
     * Instantiates a new DateRenderer.
     * @param property the property to base this renderer on
     */
    public DateRenderer(final Property property) {
      this(initDateFormat(property));
    }

    /**
     * Instantiates a new DateRenderer.
     * @param format the format
     */
    public DateRenderer(final DateFormat format) {
      this.format =  format;
      setHorizontalAlignment(JLabel.RIGHT);
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(final Object value) {
      String txt = "";
      if (value instanceof Date) {
        txt = format.format(value);
      }
      else if (value instanceof String) {
        txt = (String) value;
      }

      setText(txt);
    }

    private static DateFormat initDateFormat(final Property property) {
      return property.getFormat() == null ? Configuration.getDefaultDateFormat() : (DateFormat) property.getFormat();
    }
  }

  /**
   * A cell renderer for timestamps.
   */
  public static final class TimestampRenderer extends DefaultTableCellRenderer {
    private final DateFormat format;

    /**
     * Instantiates a new TimestampRenderer.
     * @param property the property to base this renderer on
     */
    public TimestampRenderer(final Property property) {
      this(initTimestampFormat(property));
    }

    /**
     * Instantiates a new TimestampRenderer.
     * @param format the format
     */
    public TimestampRenderer(final DateFormat format) {
      this.format = format;
      setHorizontalAlignment(JLabel.RIGHT);
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(final Object value) {
      String txt = "";
      if (value instanceof Date) {
        txt = format.format(value);
      }
      else if (value instanceof String) {
        txt = (String) value;
      }

      setText(txt);
    }

    private static DateFormat initTimestampFormat(final Property property) {
      return property.getFormat() == null ? Configuration.getDefaultTimestampFormat() : (DateFormat) property.getFormat();
    }
  }

  /**
   * A cell renderer for booleans.
   */
  public static final class BooleanRenderer extends JCheckBox implements TableCellRenderer {

    /** Constructs a new BooleanRenderer. */
    public BooleanRenderer() {
      setHorizontalAlignment(JLabel.CENTER);
    }

    /** {@inheritDoc} */
    public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                   final boolean isSelected, final boolean hasFocus,
                                                   final int row, final int column) {
      if (value != null && !(value instanceof Boolean)) {
        throw new IllegalArgumentException("Non boolean value: " + value.getClass());
      }

      setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
      setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

      setSelected(value != null && (Boolean) value);
      setEnabled(value != null);

      return this;
    }
  }
}
