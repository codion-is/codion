/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.JTable;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.Format;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

import static is.codion.swing.common.ui.Utilities.darker;
import static java.util.Objects.requireNonNull;

/**
 * The default table cell renderer for a {@link EntityTablePanel}
 * @param <T> the column value type
 * @see EntityTableCellRenderer#entityTableCellRenderer(SwingEntityTableModel, Property)
 * @see EntityTableCellRenderer#entityTableCellRenderer(SwingEntityTableModel, Property, Format, DateTimeFormatter, int)
 */
public class DefaultEntityTableCellRenderer<T> extends DefaultTableCellRenderer implements EntityTableCellRenderer {

  private static final double DARKENING_FACTOR = 0.9;
  private static final double DOUBLE_DARKENING_FACTOR = 0.8;

  private static Color foregroundColor;

  private static Color backgroundColor;
  private static Color backgroundColorSearch;
  private static Color backgroundColorDoubleSearch;

  private static Color alternateBackgroundColor;
  private static Color alternateBackgroundColorSearch;
  private static Color alternateBackgroundColorDoubleSearch;

  private static Border focusedCellBorder;

  static {
    configureColors();
  }

  private final SwingEntityTableModel tableModel;
  private final Property<T> property;
  private final Format format;
  private final DateTimeFormatter dateTimeFormatter;

  private boolean displayConditionStatus = true;
  private boolean tooltipData = false;

  protected DefaultEntityTableCellRenderer(final SwingEntityTableModel tableModel, final Property<T> property,
                                           final Format format, final DateTimeFormatter dateTimeFormatter,
                                           final int horizontalAlignment) {
    this.tableModel = requireNonNull(tableModel, "tableModel");
    this.property = requireNonNull(property, "property");
    this.format = format == null ? property.getFormat() : format;
    this.dateTimeFormatter = dateTimeFormatter;
    setHorizontalAlignment(horizontalAlignment);
  }

  @Override
  public final void updateUI() {
    super.updateUI();
    configureColors();
  }

  @Override
  public final boolean isDisplayConditionStatus() {
    return displayConditionStatus;
  }

  @Override
  public final void setDisplayConditionStatus(final boolean displayConditionStatus) {
    this.displayConditionStatus = displayConditionStatus;
  }

  @Override
  public final boolean isTooltipData() {
    return tooltipData;
  }

  @Override
  public final void setTooltipData(final boolean tooltipData) {
    this.tooltipData = tooltipData;
  }

  @Override
  public final void setHorizontalAlignment(final int alignment) {
    //called in constructor, make final
    super.setHorizontalAlignment(alignment);
  }

  @Override
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                 final boolean hasFocus, final int row, final int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    setForeground(getForeground(table, row, isSelected));
    setBackground(getBackground(table, row, isSelected));
    setBorder(hasFocus ? focusedCellBorder : null);
    if (isTooltipData()) {
      setToolTipText(value == null ? "" : value.toString());
    }

    return this;
  }

  @Override
  public Color getBackground(final JTable table, final int row, final boolean selected) {
    if (selected) {
      return table.getSelectionBackground();
    }

    return getBackgroundColor(tableModel, property.getAttribute(), row, displayConditionStatus);
  }

  @Override
  public Color getForeground(final JTable table, final int row, final boolean selected) {
    return getForegroundColor(tableModel, property.getAttribute(), row);
  }

  /**
   * @param value the value to set
   * @see SwingEntityTableModel#getValue(Entity, Attribute)
   */
  @Override
  protected void setValue(final Object value) {
    if (property instanceof ItemProperty) {
      setText((String) value);
    }
    else if (value instanceof Temporal) {
      setText(dateTimeFormatter.format((Temporal) value));
    }
    else if (format != null && value != null) {
      setText(format.format(value));
    }
    else {
      super.setValue(value);
    }
  }

  private static Color getBackgroundColor(final SwingEntityTableModel tableModel, final Attribute<?> attribute, final int row,
                                          final boolean indicateCondition) {
    final boolean conditionEnabled = tableModel.getTableConditionModel().isConditionEnabled(attribute);
    final boolean filterEnabled = tableModel.getTableConditionModel().isFilterEnabled(attribute);
    final boolean showCondition = indicateCondition && (conditionEnabled || filterEnabled);
    final Color cellColor = tableModel.getBackgroundColor(row, attribute);
    if (showCondition) {
      return getConditionEnabledColor(row, conditionEnabled, filterEnabled, cellColor);
    }
    else if (cellColor != null) {
      return cellColor;
    }
    else {
      return row % 2 == 0 ? backgroundColor : alternateBackgroundColor;
    }
  }

  private static Color getForegroundColor(final SwingEntityTableModel tableModel, final Attribute<?> attribute, final int row) {
    final Color cellColor = tableModel.getForegroundColor(row, attribute);

    return cellColor == null ? foregroundColor : cellColor;
  }

  private static Color getConditionEnabledColor(final int row, final boolean propertyConditionEnabled,
                                                final boolean propertyFilterEnabled, final Color cellColor) {
    final boolean doubleSearch = propertyConditionEnabled && propertyFilterEnabled;
    if (cellColor != null) {
      return darker(cellColor, DARKENING_FACTOR);
    }
    else {
      return row % 2 == 0 ?
              (doubleSearch ? backgroundColorDoubleSearch : backgroundColorSearch) :
              (doubleSearch ? alternateBackgroundColorDoubleSearch : alternateBackgroundColorSearch);
    }
  }

  private static void configureColors() {
    final LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
    foregroundColor = lookAndFeel.getDefaults().getColor("Table.foreground");
    backgroundColor = lookAndFeel.getDefaults().getColor("Table.background");
    backgroundColorSearch = darker(backgroundColor, DARKENING_FACTOR);
    backgroundColorDoubleSearch = darker(backgroundColor, DOUBLE_DARKENING_FACTOR);
    final Color alternate = darker(backgroundColor, DOUBLE_DARKENING_FACTOR);
    alternateBackgroundColor = alternate == null ? backgroundColor : alternate;
    alternateBackgroundColorSearch = darker(alternateBackgroundColor, DARKENING_FACTOR);
    alternateBackgroundColorDoubleSearch = darker(alternateBackgroundColor, DOUBLE_DARKENING_FACTOR);
    focusedCellBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
  }

  static final class BooleanRenderer extends NullableCheckBox
          implements TableCellRenderer, javax.swing.plaf.UIResource, EntityTableCellRenderer {

    private final SwingEntityTableModel tableModel;
    private final Property<Boolean> property;

    private boolean displayConditionStatus = true;

    BooleanRenderer(final SwingEntityTableModel tableModel, final Property<Boolean> property) {
      super(new NullableToggleButtonModel());
      this.tableModel = tableModel;
      this.property = property;
      setHorizontalAlignment(SwingConstants.CENTER);
      setBorderPainted(true);
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
      getNullableModel().setState((Boolean) value);
      setForeground(getForeground(table, row, isSelected));
      setBackground(getBackground(table, row, isSelected));
      setBorder(hasFocus ? focusedCellBorder : null);

      return this;
    }

    @Override
    public Color getBackground(final JTable table, final int row, final boolean selected) {
      if (selected) {
        return table.getSelectionBackground();
      }

      return getBackgroundColor(tableModel, property.getAttribute(), row, displayConditionStatus);
    }

    @Override
    public boolean isDisplayConditionStatus() {
      return displayConditionStatus;
    }

    @Override
    public void setDisplayConditionStatus(final boolean displayConditionStatus) {
      this.displayConditionStatus = displayConditionStatus;
    }

    /**
     * @return false
     */
    @Override
    public boolean isTooltipData() {
      return false;
    }

    /**
     * Disabled
     * @param tooltipData the value
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setTooltipData(final boolean tooltipData) {
      throw new UnsupportedOperationException("Tooltip data is not available for boolean properties");
    }
  }
}
