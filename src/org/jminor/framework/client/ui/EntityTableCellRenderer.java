/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 *
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.formats.ExactDateFormat;
import org.jminor.common.model.formats.FullDateFormat;
import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;

public class EntityTableCellRenderer implements TableCellRenderer {

  private final EntityTableModel tableModel;
  private final boolean specialRendering;

  private final HashMap<Integer, TableCellRenderer> renderers = new HashMap<Integer, TableCellRenderer>();
  private static Color defaultBackground;
  private static Color defaultForeground;

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

    if (defaultBackground == null)
      defaultBackground = component.getBackground();
    if (defaultForeground == null)
      defaultForeground = component.getForeground();

    final boolean propertySearchEnabled = tableModel.isSearchEnabled(getProperty(column).propertyID);
    final boolean propertyFilterEnabled = tableModel.isFilterEnabled(column);
    final Color rowColor = specialRendering ? tableModel.getRowBackgroundColor(row) : null;
    if (rowColor == null && !(propertySearchEnabled || propertyFilterEnabled)) {
      component.setBackground(defaultBackground);
      component.setForeground(defaultForeground);
    }
    else {
      if (rowColor != null)
        component.setBackground(rowColor);
      else
        component.setBackground((propertySearchEnabled && propertyFilterEnabled) ? DOUBLE_FILTERED_BACKGROUND : SINGLE_FILTERED_BACKGROUND);
    }

    return component;
  }

  protected TableCellRenderer getRenderer(final int colIdx) {
    TableCellRenderer renderer = renderers.get(colIdx);
    if (renderer == null) {
      final Type propType = tableModel.getTableColumnProperties()[colIdx].getPropertyType();
      switch (propType) {
        case BOOLEAN:
          renderer = new BooleanRenderer();
          break;
        case DOUBLE:
        case INT:
          renderer = new NumberRenderer();
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
      renderers.put(colIdx, renderer);
    }

    return renderer;
  }

  private Property getProperty(final int column) {
    return Entity.repository.getPropertyAtViewIndex(tableModel.getEntityID(), column);
  }

  /**
   * Default Renderers
   */
  public static class NumberRenderer extends DefaultTableCellRenderer {
    private final NumberFormat formatter;

    public NumberRenderer() {
      super();
      formatter = NumberFormat.getInstance();
      setHorizontalAlignment(JLabel.RIGHT);
    }

    public void setValue(final Object value) {
      if (value instanceof String)
        setText((String) value);
      else
        setText((value == null) ? "" : formatter.format(value));
    }
  }

  public static class DoubleRenderer extends NumberRenderer {
    public DoubleRenderer() {
      super();
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

      setText(txt);
    }
  }

  public static class FullDateRenderer extends DefaultTableCellRenderer {
    /** Constructs a new FullDateRenderer. */
    public FullDateRenderer() {
      super();
    }

    /** {@inheritDoc} */
    public void setValue(final Object value) {
      String txt = "";
      if (value != null && value instanceof Date)
        txt = FullDateFormat.get().format(value);

      setText(txt);
    }
  }

  public static class ExactDateRenderer extends DefaultTableCellRenderer {
    /** Constructs a new ExactDateRenderer. */
    public ExactDateRenderer() {
      super();
    }

    /** {@inheritDoc} */
    public void setValue(final Object value) {
      String txt = "";
      if (value != null && value instanceof Date)
        txt = ExactDateFormat.get().format(value);

      setText(txt);
    }
  }

  public static class IconRenderer extends DefaultTableCellRenderer {
    /** Constructs a new IconRenderer. */
    public IconRenderer() {
      super();
      setHorizontalAlignment(JLabel.CENTER);
    }

    /** {@inheritDoc} */
    public void setValue(final Object value) {
      setIcon((value instanceof Icon) ? (Icon) value : null);
    }
  }

  public static class BooleanRenderer extends JCheckBox implements TableCellRenderer, UIResource {

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

      setSelected(value == Type.Boolean.TRUE);
      setEnabled(value != null);

      return this;
    }
  }
}
