/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.ValueListProperty;
import org.jminor.framework.model.EntityTableModel;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.Format;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Provides TableCellRenderer implementations for EntityTablePanels
 */
public final class EntityTableCellRenderers {

  private static final int SHADE_AMOUNT = 42;

  private static final Color DEFAULT_BACKGROUND;
  private static final Color SEARCH_BACKGROUND;
  private static final Color DOUBLE_SEARCH_BACKGROUND;

  private static final Color DEFAULT_ALTERNATE_BACKGROUND;
  private static final Color SEARCH_ALTERNATE_BACKGROUND;
  private static final Color DOUBLE_ALTERNATE_SEARCH_BACKGROUND;

  private static final int RGB_CENTER = 128;

  static {
    DEFAULT_BACKGROUND = UIManager.getColor("Table.background");
    SEARCH_BACKGROUND = shade(DEFAULT_BACKGROUND, SHADE_AMOUNT);
    DOUBLE_SEARCH_BACKGROUND = shade(DEFAULT_BACKGROUND, SHADE_AMOUNT * 2);
    final Color alternate = UIManager.getColor("Table.alternateRowColor");
    DEFAULT_ALTERNATE_BACKGROUND = alternate == null ? DEFAULT_BACKGROUND : alternate;
    SEARCH_ALTERNATE_BACKGROUND = shade(DEFAULT_ALTERNATE_BACKGROUND, SHADE_AMOUNT);
    DOUBLE_ALTERNATE_SEARCH_BACKGROUND = shade(DEFAULT_ALTERNATE_BACKGROUND, SHADE_AMOUNT * 2);
  }

  private EntityTableCellRenderers() {}

  /**
   * Instantiates a new EntityTableCellRenderer for the given property
   * @param tableModel the table model
   * @param property the property
   * @return the table cell renderer
   * @see Entity.ColorProvider
   * @see Entity.Definition.Builder#setColorProvider(Entity.ColorProvider)
   */
  public static EntityTableCellRenderer getTableCellRenderer(final EntityTableModel tableModel, final Property property) {
    if (!Objects.equals(tableModel.getEntityId(), property.getEntityId())) {
      throw new IllegalArgumentException("Property " + property + " not found in entity : " + tableModel.getEntityId());
    }
    if (property.isBoolean()) {
      return new BooleanRenderer(tableModel, property);
    }

    return new DefaultEntityTableCellRenderer(tableModel, property);
  }

  private static Color shade(final Color color, final int amount) {
    requireNonNull(color, "color");
    int r = color.getRed();
    int g = color.getGreen();
    int b = color.getBlue();

    r += r < RGB_CENTER ? amount : -amount;
    g += g < RGB_CENTER ? amount : -amount;
    b += b < RGB_CENTER ? amount : -amount;

    return new Color(r, g, b);
  }

  /**
   * The default table cell renderer for a {@link EntityTablePanel}
   */
  public static class DefaultEntityTableCellRenderer extends DefaultTableCellRenderer implements EntityTableCellRenderer {

    private final EntityTableModel tableModel;
    private final Property property;
    private final Format format;
    private final DateTimeFormatter dateTimeFormatter;

    private boolean indicateCondition = true;
    private boolean tooltipData = false;

    /**
     * Instantiates a new DefaultEntityTableCellRenderer based on the data provided by the given EntityTableModel
     * @param tableModel the table model providing the data to render
     * @param property the property
     */
    public DefaultEntityTableCellRenderer(final EntityTableModel tableModel, final Property property) {
      this(tableModel, property, property.getFormat(), property.getDateTimeFormatter(),
              property.isNumerical() || property.isTemporal() ? RIGHT : LEFT);
    }

    /**
     * Instantiates a new DefaultEntityTableCellRenderer based on the data provided by the given EntityTableModel
     * @param tableModel the table model providing the data to render
     * @param property the property
     * @param format the format, overrides the format associated with the property
     * @param dateTimeFormatter the date/time formatter
     */
    public DefaultEntityTableCellRenderer(final EntityTableModel tableModel, final Property property, final Format format,
                                          final DateTimeFormatter dateTimeFormatter) {
      this(tableModel, property, format, dateTimeFormatter, LEFT);
    }

    /**
     * Instantiates a new DefaultEntityTableCellRenderer based on the data provided by the given EntityTableModel
     * @param tableModel the table model providing the data to render
     * @param property the property
     * @param format overrides the format defined by the property
     * @param dateTimeFormatter the date/time formatter
     * @param horizontalAlignment the horizontal alignment
     */
    public DefaultEntityTableCellRenderer(final EntityTableModel tableModel, final Property property, final Format format,
                                          final DateTimeFormatter dateTimeFormatter, final int horizontalAlignment) {
      this.tableModel = requireNonNull(tableModel, "tableModel");
      this.property = requireNonNull(property, "property");
      this.format = format == null ? property.getFormat() : format;
      this.dateTimeFormatter = dateTimeFormatter;
      setHorizontalAlignment(horizontalAlignment);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isIndicateCondition() {
      return indicateCondition;
    }

    /** {@inheritDoc} */
    @Override
    public final void setIndicateCondition(final boolean indicateCondition) {
      this.indicateCondition = indicateCondition;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isTooltipData() {
      return tooltipData;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTooltipData(final boolean tooltipData) {
      this.tooltipData = tooltipData;
    }

    /** {@inheritDoc} */
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (isTooltipData()) {
        setToolTipText(value == null ? "" : value.toString());
      }
      if (isSelected) {
        return this;
      }

      final boolean propertyConditionEnabled = tableModel.getConditionModel().isEnabled(property.getPropertyId());
      final boolean propertyFilterEnabled = tableModel.getConditionModel().isFilterEnabled(property.getPropertyId());
      final boolean showCondition = indicateCondition && (propertyConditionEnabled || propertyFilterEnabled);
      final Color cellColor = (Color) tableModel.getPropertyBackgroundColor(row, property);
      if (showCondition) {
        setBackground(getConditionEnabledColor(row, propertyConditionEnabled, propertyFilterEnabled, cellColor));
      }
      else {
        if (cellColor != null) {
          setBackground(cellColor);
        }
        else {
          setBackground(row % 2 == 0 ? DEFAULT_BACKGROUND : DEFAULT_ALTERNATE_BACKGROUND);
        }
      }
      return this;
    }

    @Override
    protected void setValue(final Object value) {
      if (format == null) {
        super.setValue(value);
      }
      else {
        if (property instanceof ValueListProperty) {
          setText((String) value);
        }
        else if (value instanceof Temporal) {
          setText(dateTimeFormatter.format((Temporal) value));
        }
        else {
          setText(value == null ? "" : format.format(value));
        }
      }
    }

    private static Color getConditionEnabledColor(final int row, final boolean propertyConditionEnabled,
                                                  final boolean propertyFilterEnabled, final Color cellColor) {
      final boolean doubleShade = propertyConditionEnabled && propertyFilterEnabled;
      if (cellColor != null) {
        return shade(cellColor, doubleShade ? SHADE_AMOUNT * 2 : SHADE_AMOUNT);
      }
      else {
        return row % 2 == 0 ?
                (doubleShade ? DOUBLE_SEARCH_BACKGROUND : SEARCH_BACKGROUND) :
                (doubleShade ? DOUBLE_ALTERNATE_SEARCH_BACKGROUND : SEARCH_ALTERNATE_BACKGROUND);
      }
    }
  }

  private static final class BooleanRenderer extends DefaultEntityTableCellRenderer {

    private final JCheckBox checkBox = new JCheckBox();

    private BooleanRenderer(final EntityTableModel tableModel, final Property property) {
      super(tableModel, property, null, null, CENTER);
      checkBox.setHorizontalAlignment(CENTER);
      checkBox.setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
      final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      checkBox.setBackground(component.getBackground());
      checkBox.setForeground(component.getForeground());

      return checkBox;
    }

    @Override
    protected void setValue(final Object value) {
      checkBox.setSelected(Boolean.TRUE.equals(value));
      checkBox.setEnabled(value != null);
    }
  }
}
