/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.ColorProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
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
   * @param <T> the value type
   * @return the table cell renderer
   * @see ColorProvider
   * @see EntityDefinition.Builder#colorProvider(ColorProvider)
   */
  public static <T> EntityTableCellRenderer createTableCellRenderer(final SwingEntityTableModel tableModel, final Property<T> property) {
    if (!Objects.equals(tableModel.getEntityType(), property.getEntityType())) {
      throw new IllegalArgumentException("Property " + property + " not found in entity : " + tableModel.getEntityType());
    }
    if (property.getAttribute().isBoolean()) {
      return new BooleanRenderer(tableModel, (Property<Boolean>) property);
    }

    return new DefaultEntityTableCellRenderer<>(tableModel, property);
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

  private static Color getBackgroundColor(final SwingEntityTableModel tableModel, final Property<?> property, final int row,
                                          final boolean indicateCondition) {
    final boolean propertyConditionEnabled = tableModel.getConditionModel().isEnabled(property.getAttribute());
    final boolean propertyFilterEnabled = tableModel.getConditionModel().isFilterEnabled(property.getAttribute());
    final boolean showCondition = indicateCondition && (propertyConditionEnabled || propertyFilterEnabled);
    final Color cellColor = tableModel.getPropertyBackgroundColor(row, property);
    if (showCondition) {
      return getConditionEnabledColor(row, propertyConditionEnabled, propertyFilterEnabled, cellColor);
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
      return shade(cellColor, doubleShade ? SHADE_AMOUNT * 2 : SHADE_AMOUNT);
    }
    else {
      return row % 2 == 0 ?
              (doubleShade ? DOUBLE_SEARCH_BACKGROUND : SEARCH_BACKGROUND) :
              (doubleShade ? DOUBLE_ALTERNATE_SEARCH_BACKGROUND : SEARCH_ALTERNATE_BACKGROUND);
    }
  }

  /**
   * The default table cell renderer for a {@link EntityTablePanel}
   */
  public static class DefaultEntityTableCellRenderer<T> extends DefaultTableCellRenderer implements EntityTableCellRenderer {

    private final SwingEntityTableModel tableModel;
    private final Property<T> property;
    private final Format format;
    private final DateTimeFormatter dateTimeFormatter;

    private boolean indicateCondition = true;
    private boolean tooltipData = false;

    /**
     * Instantiates a new DefaultEntityTableCellRenderer based on the data provided by the given EntityTableModel
     * @param tableModel the table model providing the data to render
     * @param property the property
     */
    public DefaultEntityTableCellRenderer(final SwingEntityTableModel tableModel, final Property<T> property) {
      this(tableModel, property, property.getFormat(), property.getDateTimeFormatter(),
              property.getAttribute().isNumerical() || property.getAttribute().isTemporal() ? RIGHT : LEFT);
    }

    /**
     * Instantiates a new DefaultEntityTableCellRenderer based on the data provided by the given EntityTableModel
     * @param tableModel the table model providing the data to render
     * @param property the property
     * @param format overrides the format defined by the property
     * @param dateTimeFormatter the date/time formatter
     * @param horizontalAlignment the horizontal alignment
     */
    public DefaultEntityTableCellRenderer(final SwingEntityTableModel tableModel, final Property<T> property, final Format format,
                                          final DateTimeFormatter dateTimeFormatter, final int horizontalAlignment) {
      this.tableModel = requireNonNull(tableModel, "tableModel");
      this.property = requireNonNull(property, "property");
      this.format = format == null ? property.getFormat() : format;
      this.dateTimeFormatter = dateTimeFormatter;
      setHorizontalAlignment(horizontalAlignment);
    }

    @Override
    public final boolean isIndicateCondition() {
      return indicateCondition;
    }

    @Override
    public final void setIndicateCondition(final boolean indicateCondition) {
      this.indicateCondition = indicateCondition;
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

      return getBackgroundColor(tableModel, property, row, indicateCondition);
    }

    /**
     * @param value the value to set
     * @see is.codion.swing.framework.model.SwingEntityTableModel#getValue(Entity, Property)
     */
    @Override
    protected void setValue(final Object value) {
      if (property instanceof ValueListProperty) {
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
  }

  private static final class BooleanRenderer extends NullableCheckBox
          implements TableCellRenderer, UIResource, EntityTableCellRenderer {

    private static final Border nonFocusedBorder = new EmptyBorder(1, 1, 1, 1);
    private static final Border focusedBorder = UIManager.getBorder("Table.focusCellHighlightBorder");

    private final SwingEntityTableModel tableModel;
    private final Property<Boolean> property;

    private boolean indicateCondition = true;

    public BooleanRenderer(final SwingEntityTableModel tableModel, final Property<Boolean> property) {
      super(new NullableToggleButtonModel());
      this.tableModel = tableModel;
      this.property = property;
      setHorizontalAlignment(JLabel.CENTER);
      setBorderPainted(true);
    }

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

      return getBackgroundColor(tableModel, property, row, indicateCondition);
    }

    @Override
    public boolean isIndicateCondition() {
      return indicateCondition;
    }

    @Override
    public void setIndicateCondition(final boolean indicateCondition) {
      this.indicateCondition = indicateCondition;
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
