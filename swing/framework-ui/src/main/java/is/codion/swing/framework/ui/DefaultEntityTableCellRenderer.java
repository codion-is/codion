/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.Format;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

import static is.codion.swing.common.ui.Components.darker;
import static java.util.Objects.requireNonNull;

/**
 * The default table cell renderer for a {@link EntityTablePanel}
 * @param <T> the column value type
 * @see EntityTableCellRenderer#entityTableCellRenderer(SwingEntityTableModel, Property)
 * @see EntityTableCellRenderer#entityTableCellRenderer(SwingEntityTableModel, Property, Format, DateTimeFormatter, int)
 */
public class DefaultEntityTableCellRenderer<T> extends DefaultTableCellRenderer implements EntityTableCellRenderer {

  private static final Color DEFAULT_BACKGROUND;
  private static final Color SEARCH_BACKGROUND;
  private static final Color DOUBLE_SEARCH_BACKGROUND;

  private static final Color DEFAULT_ALTERNATE_BACKGROUND;
  private static final Color SEARCH_ALTERNATE_BACKGROUND;
  private static final Color DOUBLE_ALTERNATE_SEARCH_BACKGROUND;

  private static final double DARKENING_FACTOR = 0.9;
  private static final double DOUBLE_DARKENING_FACTOR = 0.8;

  static {
    DEFAULT_BACKGROUND = UIManager.getColor("Table.background");
    SEARCH_BACKGROUND = darker(DEFAULT_BACKGROUND, DARKENING_FACTOR);
    DOUBLE_SEARCH_BACKGROUND = darker(DEFAULT_BACKGROUND, DOUBLE_DARKENING_FACTOR);
    final Color alternate = UIManager.getColor("Table.alternateRowColor");
    DEFAULT_ALTERNATE_BACKGROUND = alternate == null ? DEFAULT_BACKGROUND : alternate;
    SEARCH_ALTERNATE_BACKGROUND = darker(DEFAULT_ALTERNATE_BACKGROUND, DARKENING_FACTOR);
    DOUBLE_ALTERNATE_SEARCH_BACKGROUND = darker(DEFAULT_ALTERNATE_BACKGROUND, DOUBLE_DARKENING_FACTOR);
  }

  private final SwingEntityTableModel tableModel;
  private final Property<T> property;
  private final Format format;
  private final DateTimeFormatter dateTimeFormatter;

  private boolean indicateCondition = true;
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
  public final boolean isDisplayConditionStatus() {
    return indicateCondition;
  }

  @Override
  public final void setDisplayConditionStatus(final boolean displayConditionStatus) {
    this.indicateCondition = displayConditionStatus;
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
    setForeground(getForeground(table, isSelected));
    setBackground(getBackground(table, row, isSelected));
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

    return getBackgroundColor(tableModel, property.getAttribute(), row, indicateCondition);
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
      return row % 2 == 0 ? DEFAULT_BACKGROUND : DEFAULT_ALTERNATE_BACKGROUND;
    }
  }

  private static Color getConditionEnabledColor(final int row, final boolean propertyConditionEnabled,
                                                final boolean propertyFilterEnabled, final Color cellColor) {
    final boolean doubleShade = propertyConditionEnabled && propertyFilterEnabled;
    if (cellColor != null) {
      return darker(cellColor, DARKENING_FACTOR);
    }
    else {
      return row % 2 == 0 ?
              (doubleShade ? DOUBLE_SEARCH_BACKGROUND : SEARCH_BACKGROUND) :
              (doubleShade ? DOUBLE_ALTERNATE_SEARCH_BACKGROUND : SEARCH_ALTERNATE_BACKGROUND);
    }
  }

  static final class BooleanRenderer extends NullableCheckBox
          implements TableCellRenderer, javax.swing.plaf.UIResource, EntityTableCellRenderer {

    private static final Border nonFocusedBorder = new EmptyBorder(1, 1, 1, 1);
    private static final Border focusedBorder = UIManager.getBorder("Table.focusCellHighlightBorder");

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
      setForeground(getForeground(table, isSelected));
      setBackground(getBackground(table, row, isSelected));
      setBorder(hasFocus ? focusedBorder : nonFocusedBorder);

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
