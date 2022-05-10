/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
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
  private final boolean toolTipData;
  private final boolean displayConditionState;
  private final Function<Object, Object> displayValueProvider;

  private DefaultEntityTableCellRenderer(DefaultBuilder builder) {
    this.settings = new UISettings(builder.leftPadding, builder.rightPadding);
    this.tableModel = builder.tableModel;
    this.property = builder.property;
    this.toolTipData = builder.toolTipData;
    this.displayConditionState = builder.displayConditionState;
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
    setBorder(hasFocus ? settings.focusedCellBorder : settings.defaultCellBorder);
    if (toolTipData) {
      setToolTipText(value == null ? "" : value.toString());
    }

    return this;
  }

  @Override
  public Color getBackground(JTable table, int row, boolean selected) {
    if (selected) {
      return settings.getSelectionBackgroundColor(row);
    }

    return settings.getBackgroundColor(tableModel, property.getAttribute(), row, displayConditionState);
  }

  @Override
  public Color getForeground(JTable table, int row, boolean selected) {
    return settings.getForegroundColor(tableModel, property.getAttribute(), row);
  }

  /**
   * @param value the value to set
   */
  @Override
  protected void setValue(Object value) {
    super.setValue(displayValueProvider.apply(value));
  }

  private final class DefaultDisplayValueProvider implements Function<Object, Object> {
    @Override
    public Object apply(Object value) {
      return ((Property<Object>) property).toString(value);
    }
  }

  private static final class BooleanRenderer extends NullableCheckBox
          implements TableCellRenderer, javax.swing.plaf.UIResource, EntityTableCellRenderer {

    private final UISettings settings;
    private final SwingEntityTableModel tableModel;
    private final Property<?> property;
    private final boolean displayConditionState;

    private BooleanRenderer(DefaultBuilder builder) {
      super(new NullableToggleButtonModel());
      this.settings = new UISettings(builder.leftPadding, builder.rightPadding);
      this.tableModel = requireNonNull(builder.tableModel, "tableModel");
      this.property = requireNonNull(builder.property, "property");
      this.displayConditionState = builder.displayConditionState;
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
      setBorder(hasFocus ? settings.focusedCellBorder : settings.defaultCellBorder);

      return this;
    }

    @Override
    public Color getBackground(JTable table, int row, boolean selected) {
      if (selected) {
        return settings.getSelectionBackgroundColor(row);
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

    private final int leftPadding;
    private final int rightPadding;

    private Color foregroundColor;
    private Color backgroundColor;
    private Color backgroundColorSearch;
    private Color backgroundColorDoubleSearch;
    private Color alternateBackgroundColor;
    private Color alternateBackgroundColorSearch;
    private Color alternateBackgroundColorDoubleSearch;
    private Color selectionBackground;
    private Color alternateSelectionBackground;
    private Border defaultCellBorder;
    private Border focusedCellBorder;

    private UISettings(int leftPadding, int rightPadding) {
      this.leftPadding = leftPadding;
      this.rightPadding = rightPadding;
      configure();
    }

    private void configure() {
      foregroundColor = UIManager.getColor("Table.foreground");
      backgroundColor = UIManager.getColor("Table.background");
      alternateBackgroundColor = UIManager.getColor("Table.alternateRowColor");
      if (alternateBackgroundColor == null) {
        alternateBackgroundColor = darker(backgroundColor, DOUBLE_DARKENING_FACTOR);
      }
      selectionBackground = UIManager.getColor("Table.selectionBackground");
      backgroundColorSearch = darker(backgroundColor, DARKENING_FACTOR);
      backgroundColorDoubleSearch = darker(backgroundColor, DOUBLE_DARKENING_FACTOR);
      alternateBackgroundColorSearch = darker(alternateBackgroundColor, DARKENING_FACTOR);
      alternateBackgroundColorDoubleSearch = darker(alternateBackgroundColor, DOUBLE_DARKENING_FACTOR);
      alternateSelectionBackground = darker(selectionBackground, DARKENING_FACTOR);
      defaultCellBorder = leftPadding > 0 || rightPadding > 0 ? createEmptyBorder(0, leftPadding, 0, rightPadding) : null;
      focusedCellBorder = createFocusedCellBorder(foregroundColor, defaultCellBorder);
    }

    private Color getBackgroundColor(SwingEntityTableModel tableModel, Attribute<?> attribute,
                                     int row, boolean indicateCondition) {
      boolean conditionEnabled = tableModel.getTableConditionModel().isConditionEnabled(attribute);
      boolean filterEnabled = tableModel.getTableConditionModel().isFilterEnabled(attribute);
      boolean showCondition = indicateCondition && (conditionEnabled || filterEnabled);
      Color cellColor = tableModel.getBackgroundColor(row, attribute);
      if (showCondition) {
        return getConditionEnabledColor(row, conditionEnabled && filterEnabled, cellColor);
      }
      else if (cellColor != null) {
        return cellColor;
      }

      return isEven(row) ? backgroundColor : alternateBackgroundColor;
    }

    private Color getForegroundColor(SwingEntityTableModel tableModel, Attribute<?> attribute, int row) {
      Color cellColor = tableModel.getForegroundColor(row, attribute);

      return cellColor == null ? foregroundColor : cellColor;
    }

    private Color getSelectionBackgroundColor(int row) {
      return isEven(row) ? selectionBackground : alternateSelectionBackground;
    }

    private Color getConditionEnabledColor(int row, boolean conditionAndFilterEnabled, Color cellColor) {
      if (cellColor != null) {
        return darker(cellColor, DARKENING_FACTOR);
      }

      return isEven(row) ?
              (conditionAndFilterEnabled ? backgroundColorDoubleSearch : backgroundColorSearch) :
              (conditionAndFilterEnabled ? alternateBackgroundColorDoubleSearch : alternateBackgroundColorSearch);
    }

    private static boolean isEven(int row) {
      return row % 2 == 0;
    }

    private static CompoundBorder createFocusedCellBorder(Color foregroundColor, Border defaultCellBorder) {
      return createCompoundBorder(createLineBorder(darker(foregroundColor, DOUBLE_DARKENING_FACTOR),
              FOCUSED_CELL_BORDER_THICKNESS), defaultCellBorder);
    }
  }

  static final class DefaultBuilder implements Builder {

    private final SwingEntityTableModel tableModel;
    private final Property<?> property;

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
      this.horizontalAlignment = getDefaultHorizontalAlignment(property);
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
      if (property.getAttribute().isBoolean() && !(property instanceof ItemProperty)) {
        return new DefaultEntityTableCellRenderer.BooleanRenderer(this);
      }

      return new DefaultEntityTableCellRenderer(this);
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
