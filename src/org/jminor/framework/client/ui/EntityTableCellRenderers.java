/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Util;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.domain.Property;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.Format;
import java.util.Date;

/**
 * Provides TableCellRenderer implementations for EntityTablePanels
 */
public final class EntityTableCellRenderers {

  private static final int SHADE_AMOUNT = 42;

  private final static Color DEFAULT_BACKGROUND;
  private final static Color SEARCH_BACKGROUND;
  private final static Color DOUBLE_SEARCH_BACKGROUND;

  private final static Color DEFAULT_ALTERNATE_BACKGROUND;
  private final static Color SEARCH_ALTERNATE_BACKGROUND;
  private final static Color DOUBLE_ALTERNATE_SEARCH_BACKGROUND;

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
   * @see org.jminor.framework.domain.Entity.BackgroundColorProvider
   * @see org.jminor.framework.domain.Entity.Definition#setBackgroundColorProvider(org.jminor.framework.domain.Entity.BackgroundColorProvider)
   */
  public static EntityTableCellRenderer getTableCellRenderer(final EntityTableModel tableModel, final Property property) {
    if (property.isBoolean()) {
      return getBooleanRenderer(tableModel);
    }
    if (property.isInteger() || property.isDouble()) {
      return getNumberRenderer(tableModel, property.getFormat());
    }
    if (property.isDateOrTime()) {
      return getDateRenderer(tableModel, property.getFormat());
    }
    else {
      return new DefaultEntityTableCellRenderer(tableModel);
    }
  }

  private static Color shade(final Color color, final int amount) {
    Util.rejectNullValue(color, "color");
    int r = color.getRed();
    int g = color.getGreen();
    int b = color.getBlue();

    r += r < 128 ? amount : -amount;
    g += g < 128 ? amount : -amount;
    b += b < 128 ? amount : -amount;

    return new Color(r, g, b);
  }

  private static EntityTableCellRenderer getDateRenderer(final EntityTableModel tableModel, final Format format) {
    return new AlignedFormattedRenderer(tableModel, format, JLabel.RIGHT) {
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
    };
  }

  private static EntityTableCellRenderer getNumberRenderer(final EntityTableModel tableModel, final Format format) {
    return new AlignedFormattedRenderer(tableModel, format, JLabel.RIGHT) {
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
    };
  }

  private static EntityTableCellRenderer getBooleanRenderer(final EntityTableModel tableModel) {
    return new BooleanRenderer(tableModel);
  }

  public static class DefaultEntityTableCellRenderer extends DefaultTableCellRenderer implements EntityTableCellRenderer {

    private final EntityTableModel tableModel;

    private boolean indicateSearch = true;
    private boolean tooltipData = false;

    public DefaultEntityTableCellRenderer(final EntityTableModel tableModel) {
      this.tableModel = tableModel;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isIndicateSearch() {
      return indicateSearch;
    }

    /** {@inheritDoc} */
    @Override
    public final void setIndicateSearch(final boolean indicateSearch) {
      this.indicateSearch = indicateSearch;
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
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      final Property property = (Property) tableModel.getColumnModel().getColumn(column).getIdentifier();
      if (tooltipData) {
        setToolTipText(value == null ? "" : value.toString());
      }
      if (isSelected) {
        return this;
      }

      final boolean propertySearchEnabled = tableModel.getSearchModel().isSearchEnabled(property.getPropertyID());
      final boolean propertyFilterEnabled = tableModel.getSearchModel().isFilterEnabled(property.getPropertyID());
      final boolean showSearch = indicateSearch && (propertySearchEnabled || propertyFilterEnabled);
      final Color cellColor = tableModel.getPropertyBackgroundColor(row, property);
      if (showSearch) {
        final boolean doubleShade = propertySearchEnabled && propertyFilterEnabled;
        if (cellColor != null) {
          final int shadeAmount = doubleShade ? SHADE_AMOUNT * 2 : SHADE_AMOUNT;
          setBackground(shade(cellColor, shadeAmount));
        }
        else {
          final Color background = row % 2 == 0 ?
                  (doubleShade ? DOUBLE_SEARCH_BACKGROUND : SEARCH_BACKGROUND) :
                  (doubleShade ? DOUBLE_ALTERNATE_SEARCH_BACKGROUND : SEARCH_ALTERNATE_BACKGROUND);
          setBackground(background);
        }
      }
      else {
        if (cellColor != null) {
          setBackground(cellColor);
        }
        else {
          final Color background = row % 2 == 0 ? DEFAULT_BACKGROUND : DEFAULT_ALTERNATE_BACKGROUND;
          setBackground(background);
        }
      }
      return this;
    }
  }

  /**
   * An aligned and formatted TableCellRenderer
   */
  private static class AlignedFormattedRenderer extends DefaultEntityTableCellRenderer {
    private final Format format;

    private AlignedFormattedRenderer(final EntityTableModel tableModel, final Format format, final int horizontalAlignment) {
      super(tableModel);
      setHorizontalAlignment(horizontalAlignment);
      this.format = format;
    }

    protected Format getFormat() {
      return format;
    }
  }

  public static final class BooleanRenderer extends DefaultEntityTableCellRenderer {

    private final JCheckBox checkBox = new JCheckBox();

    public BooleanRenderer(final EntityTableModel tableModel) {
      super(tableModel);
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
