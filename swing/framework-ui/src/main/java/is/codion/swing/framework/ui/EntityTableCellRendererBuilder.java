/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.component.table.DefaultFilteredTableCellRenderer;
import is.codion.swing.common.ui.component.table.DefaultFilteredTableCellRenderer.BooleanRenderer;
import is.codion.swing.common.ui.component.table.DefaultFilteredTableCellRenderer.DefaultBuilder;
import is.codion.swing.common.ui.component.table.DefaultFilteredTableCellRenderer.Settings;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.CellColorProvider;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.awt.Color;
import java.util.function.Function;

import static is.codion.swing.common.ui.Colors.darker;
import static java.util.Objects.requireNonNull;

final class EntityTableCellRendererBuilder extends DefaultBuilder<SwingEntityTableModel, Entity, Attribute<?>> {

  private final SwingEntityTableModel tableModel;
  private final Property<?> property;

  EntityTableCellRendererBuilder(SwingEntityTableModel tableModel, Property<?> property) {
    super(tableModel, property.attribute(), property.attribute().valueClass());
    this.property = requireNonNull(property);
    this.tableModel = requireNonNull(tableModel);
    this.tableModel.entityDefinition().property(property.attribute());
    displayValueProvider(new DefaultDisplayValueProvider(this.property));
    horizontalAlignment(horizontalAlignment());
    cellColorProvider(new EntityCellColorProvider(tableModel));
  }

  @Override
  public FilteredTableCellRenderer build() {
    if (property.attribute().isBoolean() && !(property instanceof ItemProperty)) {
      return new BooleanRenderer<>(this, new EntitySettings(leftPadding(), rightPadding()));
    }

    return new DefaultFilteredTableCellRenderer<>(this, new EntitySettings(leftPadding(), rightPadding()));
  }

  private int horizontalAlignment() {
    if (property.attribute().isBoolean() && !(property instanceof ItemProperty)) {
      return FilteredTableCellRenderer.BOOLEAN_HORIZONTAL_ALIGNMENT.get();
    }

    return super.defaultHorizontalAlignment();
  }

  private static final class EntitySettings extends Settings<SwingEntityTableModel, Attribute<?>> {

    private Color backgroundColorDoubleSearch;
    private Color alternateBackgroundColorDoubleSearch;

    private EntitySettings(int leftPadding, int rightPadding) {
      super(leftPadding, rightPadding);
    }

    @Override
    protected void updateColors() {
      super.updateColors();
      backgroundColorDoubleSearch = darker(backgroundColor(), DOUBLE_DARKENING_FACTOR);
      alternateBackgroundColorDoubleSearch = darker(alternateBackgroundColor(), DOUBLE_DARKENING_FACTOR);
    }

    @Override
    protected Color backgroundColor(SwingEntityTableModel tableModel, int row, Attribute<?> attribute,
                                    boolean indicateCondition, boolean selected,
                                    CellColorProvider<Attribute<?>> cellColorProvider) {
      boolean conditionEnabled = tableModel.tableConditionModel().isConditionEnabled(attribute);
      boolean filterEnabled = tableModel.tableConditionModel().isFilterEnabled(attribute);
      boolean showCondition = indicateCondition && (conditionEnabled || filterEnabled);
      Color cellBackgroundColor = cellBackgroundColor(cellColorProvider.backgroundColor(row, attribute, selected), row, selected);
      if (showCondition) {
        return conditionEnabledColor(row, conditionEnabled && filterEnabled, cellBackgroundColor);
      }
      if (cellBackgroundColor != null) {
        return cellBackgroundColor;
      }

      return isEven(row) ? backgroundColor() : alternateBackgroundColor();
    }

    private Color conditionEnabledColor(int row, boolean conditionAndFilterEnabled, Color cellColor) {
      if (cellColor != null) {
        return darker(cellColor, DARKENING_FACTOR);
      }

      return isEven(row) ?
              (conditionAndFilterEnabled ? backgroundColorDoubleSearch : backgroundColorSearch()) :
              (conditionAndFilterEnabled ? alternateBackgroundColorDoubleSearch : alternateBackgroundColorSearch());
    }
  }

  private static final class DefaultDisplayValueProvider implements Function<Object, Object> {

    private final Property<Object> objectProperty;

    private DefaultDisplayValueProvider(Property<?> property) {
      this.objectProperty = (Property<Object>) property;
    }

    @Override
    public Object apply(Object value) {
      return objectProperty.toString(value);
    }
  }

  private static final class EntityCellColorProvider implements FilteredTableCellRenderer.CellColorProvider<Attribute<?>> {

    private final SwingEntityTableModel tableModel;

    private EntityCellColorProvider(SwingEntityTableModel tableModel) {
      this.tableModel = tableModel;
    }

    @Override
    public Color backgroundColor(int row, Attribute<?> columnIdentifier, boolean selected) {
      return tableModel.backgroundColor(row, columnIdentifier);
    }

    @Override
    public Color foregroundColor(int row, Attribute<?> columnIdentifier, boolean selected) {
      return tableModel.foregroundColor(row, columnIdentifier);
    }
  }
}
