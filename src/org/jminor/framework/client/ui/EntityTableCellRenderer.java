/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Util;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.domain.Property;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.Format;
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

  private boolean indicateSearch = true;
  private boolean tooltipData = false;

  /**
   * Instantiates a new EntityTableCellRenderer
   * @param tableModel the table model
   * @see org.jminor.framework.domain.Entity.BackgroundColorProvider
   * @see org.jminor.framework.domain.Entity.Definition#setBackgroundColorProvider(org.jminor.framework.domain.Entity.BackgroundColorProvider)
   */
  public EntityTableCellRenderer(final EntityTableModel tableModel) {
    Util.rejectNullValue(tableModel, "tableModel");
    this.tableModel = tableModel;
  }

  /**
   * If true then columns being search by have different background color
   * @param indicateSearch the value
   */
  public final void setIndicateSearch(final boolean indicateSearch) {
    this.indicateSearch = indicateSearch;
  }

  /**
   * @return true if the search state should be represented visually
   */
  public boolean isIndicateSearch() {
    return indicateSearch;
  }

  /**
   * @param tooltipData if true then cells display their data in a tooltip
   */
  public void setTooltipData(final boolean tooltipData) {
    this.tooltipData = tooltipData;
  }

  /**
   * @return true if cells display their data in a tooltip
   */
  public boolean isTooltipData() {
    return tooltipData;
  }

  /** {@inheritDoc} */
  @Override
  public final Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                       final boolean hasFocus, final int row, final int column) {
    final Property property = (Property) tableModel.getColumnModel().getColumn(column).getIdentifier();
    final JComponent component =
            (JComponent) getRenderer(property).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    if (tooltipData) {
      component.setToolTipText(value == null ? "" : value.toString());
    }

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
      else if (indicateSearch) {
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
    if (property.isInteger() || property.isDouble()) {
      return new NumberRenderer(property);
    }
    if (property.isDateOrTime()) {
      return new DateRenderer(property);
    }
    else {
      return new DefaultTableCellRenderer();
    }
  }

  /**
   * A cell renderer for numbers.
   */
  public static final class NumberRenderer extends AlignedFormattedRenderer {
    /**
     * Instantiates a new NumberRenderer.
     * @param property the property to base this renderer on
     */
    public NumberRenderer(final Property property) {
      this(property.getFormat());
    }

    /**
     * Instantiates a new NumberRenderer.
     * @param format the format to use when rendering
     */
    public NumberRenderer(final Format format) {
      super(format, JLabel.RIGHT);
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(final Object value) {
      if (value instanceof String) {
        setText((String) value);
      }
      else {
        setText((value == null) ? "" : getFormat().format(value));
      }
    }
  }

  /**
   * A cell renderer for date and timestamps.
   */
  public static final class DateRenderer extends AlignedFormattedRenderer {
    /**
     * Instantiates a new DateRenderer.
     * @param property the property to base this renderer on
     */
    public DateRenderer(final Property property) {
      this(property.getFormat());
    }

    /**
     * Instantiates a new DateRenderer.
     * @param format the format to use when rendering
     */
    public DateRenderer(final Format format) {
      super(format, JLabel.RIGHT);
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(final Object value) {
      String txt = "";
      if (value instanceof Date) {
        txt = getFormat().format(value);
      }
      else if (value instanceof String) {
        txt = (String) value;
      }

      setText(txt);
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
    @Override
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

  /**
   * An aligned and formatted TableCellRenderer
   */
  private static class AlignedFormattedRenderer extends DefaultTableCellRenderer {
    private final Format format;

    private AlignedFormattedRenderer(final Format format, final int horizontalAlignment) {
      setHorizontalAlignment(horizontalAlignment);
      this.format = format;
    }

    protected Format getFormat() {
      return format;
    }
  }
}
