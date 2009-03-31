/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.model.Type;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * The default TableCellRenderer implementation used by EntityTablePanel
 */
public class EntityTableCellRenderer implements TableCellRenderer {

  private final EntityTableModel tableModel;
  private final boolean specialRendering;

  private final HashMap<Integer, TableCellRenderer> renderers = new HashMap<Integer, TableCellRenderer>();

  private static final Color SINGLE_FILTERED_BACKGROUND = new Color(235, 235, 235);
  private static final Color DOUBLE_FILTERED_BACKGROUND = new Color(215, 215, 215);

  public EntityTableCellRenderer(final EntityTableModel tableModel, final boolean specialRendering) {
    this.specialRendering = specialRendering;
    this.tableModel = tableModel;
  }

  /** {@inheritDoc} */
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                 final boolean hasFocus, final int row, final int column) {
    final Component component = getRenderer(column).getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column);

    if (isSelected)
      return component;

    final boolean propertySearchEnabled = tableModel.getSearchModel().isSearchEnabled(column);
    final boolean propertyFilterEnabled = tableModel.getSearchModel().isFilterEnabled(column);
    final Color rowColor = specialRendering ? tableModel.getRowBackgroundColor(row) : null;
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

  protected TableCellRenderer getRenderer(final int columnIndex) {
    TableCellRenderer renderer = renderers.get(columnIndex);
    if (renderer == null) {
      final Type propType = tableModel.getTableColumnProperties().get(columnIndex).getPropertyType();
      switch (propType) {
        case BOOLEAN:
          renderer = new BooleanRenderer();
          break;
        case DOUBLE:
        case INT:
          renderer = new NumberRenderer(true);
          break;
        case SHORT_DATE:
          renderer = new ShortDateRenderer();
          break;
        case LONG_DATE:
          renderer = new LongDateRenderer();
          break;
        default:
          renderer = new DefaultTableCellRenderer();
      }
      renderers.put(columnIndex, renderer);
    }

    return renderer;
  }

  /**
   * Default Renderers
   */
  public static class NumberRenderer extends DefaultTableCellRenderer {
    private final NumberFormat formatter;
    private final boolean formatValue;

    public NumberRenderer(final boolean formatValue) {
      super();
      formatter = NumberFormat.getInstance();
      this.formatValue = formatValue;
      setHorizontalAlignment(JLabel.RIGHT);
    }

    public void setValue(final Object value) {
      if (value instanceof String)
        setText((String) value);
      else
        setText((value == null) ? "" : (formatValue ? formatter.format(value) : value.toString()));
    }
  }

  public static class ShortDateRenderer extends DefaultTableCellRenderer {
    public ShortDateRenderer() {
      super();
      setHorizontalAlignment(JLabel.RIGHT);
    }

    /** {@inheritDoc} */
    public void setValue(final Object value) {
      String txt = "";
      if (value != null && value instanceof Date)
        txt = ShortDashDateFormat.get().format(value);
      else if (value instanceof String)
        txt = (String) value;

      setText(txt);
    }
  }

  public static class LongDateRenderer extends DefaultTableCellRenderer {
    /** Constructs a new LongDateRenderer. */
    public LongDateRenderer() {
      super();
      setHorizontalAlignment(JLabel.RIGHT);
    }

    /** {@inheritDoc} */
    public void setValue(final Object value) {
      String txt = "";
      if (value != null && value instanceof Date)
        txt = LongDateFormat.get().format(value);
      else if (value instanceof String)
        txt = (String) value;

      setText(txt);
    }
  }

  public static class BooleanRenderer extends JCheckBox implements TableCellRenderer {

    /** Constructs a new BooleanRenderer. */
    public BooleanRenderer() {
      super();
      setHorizontalAlignment(JLabel.CENTER);
    }

    /** {@inheritDoc} */
    public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                   final boolean isSelected, final boolean hasFocus,
                                                   final int row, final int column) {
      if (value != null && !(value instanceof Type.Boolean))
        throw new IllegalArgumentException("Non boolean value: " + value.getClass());

      setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
      setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

      setSelected(value == Type.Boolean.TRUE);
      setEnabled(value != null);

      return this;
    }
  }
}
