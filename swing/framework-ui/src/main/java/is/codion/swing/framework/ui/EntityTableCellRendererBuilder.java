/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.component.table.DefaultFilteredTableCellRenderer.DefaultBuilder;
import is.codion.swing.common.ui.component.table.DefaultFilteredTableCellRenderer.Settings;
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
    super(tableModel, property.attribute(), property.attribute().valueClass(),
            property.attribute().isBoolean() && !(property instanceof ItemProperty));
    this.property = requireNonNull(property);
    this.tableModel = requireNonNull(tableModel);
    this.tableModel.entityDefinition().property(property.attribute());
    displayValueProvider(new DefaultDisplayValueProvider(this.property));
    cellColorProvider(new EntityCellColorProvider(tableModel));
  }

  @Override
  protected Settings<SwingEntityTableModel, Attribute<?>> settings(int leftPadding, int rightPadding) {
    return new EntitySettings(leftPadding, rightPadding);
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
    protected Color backgroundColor(SwingEntityTableModel tableModel, int row, Attribute<?> attribute, Object cellValue,
                                    boolean indicateCondition, boolean selected,
                                    CellColorProvider<Attribute<?>> cellColorProvider) {
      boolean conditionEnabled = tableModel.tableConditionModel().isConditionEnabled(attribute);
      boolean filterEnabled = tableModel.tableConditionModel().isFilterEnabled(attribute);
      boolean showCondition = indicateCondition && (conditionEnabled || filterEnabled);
      Color cellBackgroundColor = cellBackgroundColor(cellColorProvider.backgroundColor(row, attribute, cellValue, selected), row, selected);
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

  private static final class EntityCellColorProvider implements CellColorProvider<Attribute<?>> {

    private final SwingEntityTableModel tableModel;

    private EntityCellColorProvider(SwingEntityTableModel tableModel) {
      this.tableModel = tableModel;
    }

    @Override
    public Color backgroundColor(int row, Attribute<?> columnIdentifier, Object cellValue, boolean selected) {
      return tableModel.backgroundColor(row, columnIdentifier);
    }

    @Override
    public Color foregroundColor(int row, Attribute<?> columnIdentifier, Object cellValue, boolean selected) {
      return tableModel.foregroundColor(row, columnIdentifier);
    }
  }
}
