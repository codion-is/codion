/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.model.component.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.component.checkbox.NullableCheckBox;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.text.Format;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.function.Function;

import static is.codion.swing.common.ui.Utilities.darker;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.*;

/**
 * The default table cell renderer for a {@link EntityTablePanel}
 * @see EntityTableCellRenderer#builder(SwingEntityTableModel, Property)
 */
final class DefaultEntityTableCellRenderer extends DefaultTableCellRenderer implements EntityTableCellRenderer {

  private final UISettings settings;
  private final SwingEntityTableModel tableModel;
  private final Property<?> property;
  private final Format format;
  private final DateTimeFormatter dateTimeFormatter;
  private final boolean toolTipData;
  private final boolean displayConditionState;
  private final Border border;
  private final Function<Object, Object> displayValueProvider;

  private DefaultEntityTableCellRenderer(DefaultBuilder builder, Border border) {
    this.settings = new UISettings(border);
    this.tableModel = builder.tableModel;
    this.property = builder.property;
    this.format = builder.format;
    this.dateTimeFormatter = builder.dateTimeFormatter;
    this.toolTipData = builder.toolTipData;
    this.displayConditionState = builder.displayConditionState;
    this.border = border;
    this.displayValueProvider = builder.displayValueProvider == null ? new DefaultDisplayValueProvider() : builder.displayValueProvider;
    setHorizontalAlignment(builder.horizontalAlignment);
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (settings != null) {
      settings.configure();
    }
  }

  @Override
  public boolean isDisplayConditionState() {
    return displayConditionState;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                 boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    setForeground(getForeground(table, row, isSelected));
    setBackground(getBackground(table, row, isSelected));
    setBorder(hasFocus ? settings.focusedCellBorder : border);
    if (toolTipData) {
      setToolTipText(value == null ? "" : value.toString());
    }

    return this;
  }

  @Override
  public Color getBackground(JTable table, int row, boolean selected) {
    if (selected) {
      return table.getSelectionBackground();
    }

    return settings.getBackgroundColor(tableModel, property.getAttribute(), row, displayConditionState);
  }

  @Override
  public Color getForeground(JTable table, int row, boolean selected) {
    return settings.getForegroundColor(tableModel, property.getAttribute(), row);
  }

  /**
   * @param value the value to set
   * @see SwingEntityTableModel#getValue(Entity, Attribute)
   */
  @Override
  protected void setValue(Object value) {
    super.setValue(displayValueProvider.apply(value));
  }

  private static String getItemCaption(Object value, ItemProperty<Object> itemProperty) {
    if (!itemProperty.isValid(value)) {
      //display empty string for invalid values
      return "";
    }

    return itemProperty.getItem(value).getCaption();
  }

  private final class DefaultDisplayValueProvider implements Function<Object, Object> {
    @Override
    public Object apply(Object value) {
      if (property instanceof ItemProperty) {
        return getItemCaption(value, (ItemProperty<Object>) property);
      }
      else if (value instanceof Temporal) {
        return dateTimeFormatter.format((Temporal) value);
      }
      else if (format != null && value != null) {
        return format.format(value);
      }

      return value;
    }
  }

  private static final class BooleanRenderer extends NullableCheckBox
          implements TableCellRenderer, javax.swing.plaf.UIResource, EntityTableCellRenderer {

    private final UISettings settings;
    private final SwingEntityTableModel tableModel;
    private final Property<?> property;
    private final boolean displayConditionState;
    private final Border border;

    private BooleanRenderer(DefaultBuilder builder, Border border) {
      super(new NullableToggleButtonModel());
      this.settings = new UISettings(border);
      this.tableModel = requireNonNull(builder.tableModel, "tableModel");
      this.property = requireNonNull(builder.property, "property");
      this.displayConditionState = builder.displayConditionState;
      this.border = border;
      setHorizontalAlignment(builder.horizontalAlignment);
      setBorderPainted(true);
    }

    @Override
    public void updateUI() {
      super.updateUI();
      if (settings != null) {
        settings.configure();
      }
    }

    @Override
    public boolean isDisplayConditionState() {
      return displayConditionState;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      getNullableModel().setState((Boolean) value);
      setForeground(getForeground(table, row, isSelected));
      setBackground(getBackground(table, row, isSelected));
      setBorder(hasFocus ? settings.focusedCellBorder : border);

      return this;
    }

    @Override
    public Color getBackground(JTable table, int row, boolean selected) {
      if (selected) {
        return table.getSelectionBackground();
      }

      return settings.getBackgroundColor(tableModel, property.getAttribute(), row, displayConditionState);
    }

    @Override
    public Color getForeground(JTable table, int row, boolean selected) {
      return settings.getForegroundColor(tableModel, property.getAttribute(), row);
    }
  }

  private static final class UISettings {

    private static final double DARKENING_FACTOR = 0.9;
    private static final double DOUBLE_DARKENING_FACTOR = 0.8;
    private static final int FOCUSED_CELL_BORDER_THICKNESS = 1;

    private final Border defaultCellBorder;

    private Color foregroundColor;
    private Color backgroundColor;
    private Color backgroundColorSearch;
    private Color backgroundColorDoubleSearch;
    private Color alternateBackgroundColor;
    private Color alternateBackgroundColorSearch;
    private Color alternateBackgroundColorDoubleSearch;
    private Border focusedCellBorder;

    private UISettings(Border defaultCellBorder) {
      this.defaultCellBorder = defaultCellBorder;
      configure();
    }

    private void configure() {
      foregroundColor = UIManager.getColor("Table.foreground");
      backgroundColor = UIManager.getColor("Table.background");
      backgroundColorSearch = darker(backgroundColor, DARKENING_FACTOR);
      backgroundColorDoubleSearch = darker(backgroundColor, DOUBLE_DARKENING_FACTOR);
      alternateBackgroundColor = darker(backgroundColor, DOUBLE_DARKENING_FACTOR);
      alternateBackgroundColorSearch = darker(alternateBackgroundColor, DARKENING_FACTOR);
      alternateBackgroundColorDoubleSearch = darker(alternateBackgroundColor, DOUBLE_DARKENING_FACTOR);
      focusedCellBorder = createCompoundBorder(createLineBorder(foregroundColor, FOCUSED_CELL_BORDER_THICKNESS), defaultCellBorder);
    }

    private Color getBackgroundColor(SwingEntityTableModel tableModel, Attribute<?> attribute, int row,
                                     boolean indicateCondition) {
      boolean conditionEnabled = tableModel.getTableConditionModel().isConditionEnabled(attribute);
      boolean filterEnabled = tableModel.getTableConditionModel().isFilterEnabled(attribute);
      boolean showCondition = indicateCondition && (conditionEnabled || filterEnabled);
      Color cellColor = tableModel.getBackgroundColor(row, attribute);
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

    private Color getForegroundColor(SwingEntityTableModel tableModel, Attribute<?> attribute, int row) {
      Color cellColor = tableModel.getForegroundColor(row, attribute);

      return cellColor == null ? foregroundColor : cellColor;
    }

    private Color getConditionEnabledColor(int row, boolean conditionEnabled,
                                           boolean filterEnabled, Color cellColor) {
      boolean conditionAndFilterEnabled = conditionEnabled && filterEnabled;
      if (cellColor != null) {
        return darker(cellColor, DARKENING_FACTOR);
      }
      else {
        return row % 2 == 0 ?
                (conditionAndFilterEnabled ? backgroundColorDoubleSearch : backgroundColorSearch) :
                (conditionAndFilterEnabled ? alternateBackgroundColorDoubleSearch : alternateBackgroundColorSearch);
      }
    }
  }

  static final class DefaultBuilder implements Builder {

    private final SwingEntityTableModel tableModel;
    private final Property<?> property;

    private Format format;
    private DateTimeFormatter dateTimeFormatter;
    private int horizontalAlignment;
    private boolean toolTipData;
    private boolean displayConditionState = true;
    private int leftPadding = EntityTableCellRenderer.TABLE_CELL_LEFT_PADDING.get();
    private int rightPadding = EntityTableCellRenderer.TABLE_CELL_RIGHT_PADDING.get();
    private Function<Object, Object> displayValueProvider;

    DefaultBuilder(SwingEntityTableModel tableModel, Property<?> property) {
      this.tableModel = requireNonNull(tableModel);
      this.property = requireNonNull(property);
      this.tableModel.getEntityDefinition().getProperty(property.getAttribute());
      this.format = property.getFormat();
      this.dateTimeFormatter = property.getDateTimeFormatter();
      this.horizontalAlignment = getDefaultHorizontalAlignment(property);
    }

    @Override
    public Builder format(Format format) {
      this.format = requireNonNull(format);
      return this;
    }

    @Override
    public Builder dateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
      this.dateTimeFormatter = requireNonNull(dateTimeFormatter);
      return this;
    }

    @Override
    public Builder horizontalAlignment(int horizontalAlignment) {
      this.horizontalAlignment = horizontalAlignment;
      return this;
    }

    @Override
    public Builder toolTipData(boolean toolTipData) {
      this.toolTipData = toolTipData;
      return this;
    }

    @Override
    public Builder displayConditionState(boolean displayConditionState) {
      this.displayConditionState = displayConditionState;
      return this;
    }

    @Override
    public Builder leftPadding(int leftPadding) {
      this.leftPadding = leftPadding;
      return null;
    }

    @Override
    public Builder rightPadding(int rightPadding) {
      this.rightPadding = rightPadding;
      return this;
    }

    @Override
    public Builder displayValueProvider(Function<Object, Object> displayValueProvider) {
      this.displayValueProvider = requireNonNull(displayValueProvider);
      return this;
    }

    @Override
    public EntityTableCellRenderer build() {
      Border border = leftPadding > 0 || rightPadding > 0 ? createEmptyBorder(0, leftPadding, 0, rightPadding) : null;
      if (property.getAttribute().isBoolean() && !(property instanceof ItemProperty)) {
        return new DefaultEntityTableCellRenderer.BooleanRenderer(this, border);
      }

      return new DefaultEntityTableCellRenderer(this, border);
    }

    private static int getDefaultHorizontalAlignment(Property<?> property) {
      if (property.getAttribute().isBoolean() && !(property instanceof ItemProperty)) {
        return EntityTableCellRenderer.BOOLEAN_HORIZONTAL_ALIGNMENT.get();
      }
      if (property.getAttribute().isNumerical()) {
        return EntityTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.get();
      }
      if (property.getAttribute().isTemporal()) {
        return EntityTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.get();
      }

      return EntityTableCellRenderer.HORIZONTAL_ALIGNMENT.get();
    }
  }
}
