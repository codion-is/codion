/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.component.table.DefaultFilteredTableCellRendererBuilder;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.CellColorProvider;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.Settings;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.awt.Color;
import java.util.function.Function;

import static is.codion.swing.common.ui.Colors.darker;
import static java.util.Objects.requireNonNull;

final class EntityTableCellRendererBuilder extends DefaultFilteredTableCellRendererBuilder<SwingEntityTableModel, Entity, Attribute<?>> {

  private final Property<?> property;

  EntityTableCellRendererBuilder(SwingEntityTableModel tableModel, Property<?> property) {
    super(requireNonNull(tableModel), requireNonNull(property).attribute(), property.attribute().valueClass(),
            property.attribute().isBoolean() && !(property instanceof ItemProperty));
    this.property = property;
    tableModel.entityDefinition().property(property.attribute());
    displayValueProvider(new DefaultDisplayValueProvider(this.property));
    cellColorProvider(new EntityCellColorProvider(tableModel));
  }

  @Override
  protected Settings<SwingEntityTableModel, Attribute<?>> settings(int leftPadding, int rightPadding, boolean alternateRowColoring) {
    return new EntitySettings(leftPadding, rightPadding, alternateRowColoring);
  }

  private static final class EntitySettings extends Settings<SwingEntityTableModel, Attribute<?>> {

    private Color backgroundColorDoubleShade;
    private Color backgroundColorAlternateDoubleShade;

    private EntitySettings(int leftPadding, int rightPadding, boolean alternateRoColoring) {
      super(leftPadding, rightPadding, alternateRoColoring);
    }

    @Override
    protected void updateColors() {
      super.updateColors();
      backgroundColorDoubleShade = darker(backgroundColor(), DOUBLE_DARKENING_FACTOR);
      backgroundColorAlternateDoubleShade = darker(backgroundColorAlternate(), DOUBLE_DARKENING_FACTOR);
    }

    @Override
    protected Color backgroundColorShaded(SwingEntityTableModel tableModel, int row, Attribute<?> columnIdentifier, Color cellBackgroundColor) {
      boolean conditionEnabled = tableModel.tableConditionModel().isConditionEnabled(columnIdentifier);
      boolean filterEnabled = tableModel.tableConditionModel().isFilterEnabled(columnIdentifier);
      boolean showCondition = conditionEnabled || filterEnabled;
      if (showCondition) {
        return backgroundColorShaded(row, conditionEnabled && filterEnabled, cellBackgroundColor);
      }

      return cellBackgroundColor;
    }

    private Color backgroundColorShaded(int row, boolean doubleShading, Color cellBackgroundColor) {
      if (cellBackgroundColor != null) {
        return darker(cellBackgroundColor, DARKENING_FACTOR);
      }

      return alternateRowColor(row) ?
              (doubleShading ? backgroundColorDoubleShade : backgroundColorShaded()) :
              (doubleShading ? backgroundColorAlternateDoubleShade : backgroundColorAlternateShaded());
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
