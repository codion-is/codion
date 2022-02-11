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

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.LookAndFeel;
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
 * @see EntityTableCellRenderer#builder(SwingEntityTableModel, Property)
 */
final class DefaultEntityTableCellRenderer extends DefaultTableCellRenderer implements EntityTableCellRenderer {

  private final UISettings settings = new UISettings();
  private final SwingEntityTableModel tableModel;
  private final Property<?> property;
  private final Format format;
  private final DateTimeFormatter dateTimeFormatter;
  private final boolean toolTipData;
  private final boolean displayConditionState;
  private final Border border;

  private DefaultEntityTableCellRenderer(final DefaultBuilder builder, final Border border) {
    this.tableModel = requireNonNull(builder.tableModel, "tableModel");
    this.property = requireNonNull(builder.property, "property");
    this.format = builder.format == null ? property.getFormat() : builder.format;
    this.dateTimeFormatter = builder.dateTimeFormatter;
    this.toolTipData = builder.toolTipData;
    this.displayConditionState = builder.displayConditionState;
    this.border = border;
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
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                 final boolean hasFocus, final int row, final int column) {
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
  public Color getBackground(final JTable table, final int row, final boolean selected) {
    if (selected) {
      return table.getSelectionBackground();
    }

    return settings.getBackgroundColor(tableModel, property.getAttribute(), row, displayConditionState);
  }

  @Override
  public Color getForeground(final JTable table, final int row, final boolean selected) {
    return settings.getForegroundColor(tableModel, property.getAttribute(), row);
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

  private static final class BooleanRenderer extends NullableCheckBox
          implements TableCellRenderer, javax.swing.plaf.UIResource, EntityTableCellRenderer {

    private final UISettings settings = new UISettings();
    private final SwingEntityTableModel tableModel;
    private final Property<?> property;
    private final boolean displayConditionState;
    private final Border border;

    private BooleanRenderer(final DefaultBuilder builder, final Border border) {
      super(new NullableToggleButtonModel());
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
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
      getNullableModel().setState((Boolean) value);
      setForeground(getForeground(table, row, isSelected));
      setBackground(getBackground(table, row, isSelected));
      setBorder(hasFocus ? settings.focusedCellBorder : border);

      return this;
    }

    @Override
    public Color getBackground(final JTable table, final int row, final boolean selected) {
      if (selected) {
        return table.getSelectionBackground();
      }

      return settings.getBackgroundColor(tableModel, property.getAttribute(), row, displayConditionState);
    }

    @Override
    public Color getForeground(final JTable table, final int row, final boolean selected) {
      return settings.getForegroundColor(tableModel, property.getAttribute(), row);
    }
  }

  private static final class UISettings {

    private static final double DARKENING_FACTOR = 0.9;
    private static final double DOUBLE_DARKENING_FACTOR = 0.8;

    private Color foregroundColor;
    private Color backgroundColor;
    private Color backgroundColorSearch;
    private Color backgroundColorDoubleSearch;
    private Color alternateBackgroundColor;
    private Color alternateBackgroundColorSearch;
    private Color alternateBackgroundColorDoubleSearch;
    private Border focusedCellBorder;

    private UISettings() {
      configure();
    }

    private void configure() {
      final LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
      foregroundColor = lookAndFeel.getDefaults().getColor("Table.foreground");
      backgroundColor = lookAndFeel.getDefaults().getColor("Table.background");
      backgroundColorSearch = darker(backgroundColor, DARKENING_FACTOR);
      backgroundColorDoubleSearch = darker(backgroundColor, DOUBLE_DARKENING_FACTOR);
      alternateBackgroundColor = darker(backgroundColor, DOUBLE_DARKENING_FACTOR);
      alternateBackgroundColorSearch = darker(alternateBackgroundColor, DARKENING_FACTOR);
      alternateBackgroundColorDoubleSearch = darker(alternateBackgroundColor, DOUBLE_DARKENING_FACTOR);
      focusedCellBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
    }

    private Color getBackgroundColor(final SwingEntityTableModel tableModel, final Attribute<?> attribute, final int row,
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

    private Color getForegroundColor(final SwingEntityTableModel tableModel, final Attribute<?> attribute, final int row) {
      final Color cellColor = tableModel.getForegroundColor(row, attribute);

      return cellColor == null ? foregroundColor : cellColor;
    }

    private Color getConditionEnabledColor(final int row, final boolean propertyConditionEnabled,
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

    DefaultBuilder(final SwingEntityTableModel tableModel, final Property<?> property) {
      this.tableModel = requireNonNull(tableModel);
      this.property = requireNonNull(property);
      this.tableModel.getEntityDefinition().getProperty(property.getAttribute());
      this.format = property.getFormat();
      this.dateTimeFormatter = property.getDateTimeFormatter();
      this.horizontalAlignment = getDefaultHorizontalAlignment(property);
    }

    @Override
    public Builder format(final Format format) {
      this.format = format;
      return this;
    }

    @Override
    public Builder dateTimeFormatter(final DateTimeFormatter dateTimeFormatter) {
      this.dateTimeFormatter = dateTimeFormatter;
      return this;
    }

    @Override
    public Builder horizontalAlignment(final int horizontalAlignment) {
      this.horizontalAlignment = horizontalAlignment;
      return this;
    }

    @Override
    public Builder toolTipData(final boolean toolTipData) {
      this.toolTipData = toolTipData;
      return this;
    }

    @Override
    public Builder displayConditionState(final boolean displayConditionState) {
      this.displayConditionState = displayConditionState;
      return this;
    }

    @Override
    public Builder leftPadding(final int leftPadding) {
      this.leftPadding = leftPadding;
      return null;
    }

    @Override
    public Builder rightPadding(final int rightPadding) {
      this.rightPadding = rightPadding;
      return this;
    }

    @Override
    public EntityTableCellRenderer build() {
      final Border border = leftPadding > 0 || rightPadding > 0 ? BorderFactory.createEmptyBorder(0, leftPadding, 0, rightPadding) : null;
      if (property.getAttribute().isBoolean() && !(property instanceof ItemProperty)) {
        return new DefaultEntityTableCellRenderer.BooleanRenderer(this, border);
      }

      return new DefaultEntityTableCellRenderer(this, border);
    }

    private static int getDefaultHorizontalAlignment(final Property<?> property) {
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
