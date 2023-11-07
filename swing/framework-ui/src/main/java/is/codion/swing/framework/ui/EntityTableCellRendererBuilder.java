/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.ui.component.table.DefaultFilteredTableCellRendererBuilder;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.CellColorProvider;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer.Settings;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.awt.Color;
import java.util.function.Function;

import static is.codion.swing.common.ui.Colors.darker;
import static java.util.Objects.requireNonNull;

final class EntityTableCellRendererBuilder extends DefaultFilteredTableCellRendererBuilder<Entity, Attribute<?>> {

  EntityTableCellRendererBuilder(SwingEntityTableModel tableModel, Attribute<?> attribute) {
    this(requireNonNull(tableModel), tableModel.entityDefinition().attributes().definition(attribute));
  }

  private EntityTableCellRendererBuilder(SwingEntityTableModel tableModel, AttributeDefinition<?> attributeDefinition) {
    super(requireNonNull(tableModel), requireNonNull(attributeDefinition).attribute(), attributeDefinition.attribute().type().valueClass(),
            attributeDefinition.attribute().type().isBoolean() && attributeDefinition.items().isEmpty());
    tableModel.entityDefinition().attributes().definition(attributeDefinition.attribute());
    displayValueProvider(new DefaultDisplayValueProvider(attributeDefinition));
    cellColorProvider(new EntityCellColorProvider(tableModel));
  }

  @Override
  protected Settings<Attribute<?>> settings(int leftPadding, int rightPadding, boolean alternateRowColoring) {
    return new EntitySettings(leftPadding, rightPadding, alternateRowColoring);
  }

  private static final class EntitySettings extends Settings<Attribute<?>> {

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
    protected Color backgroundColorShaded(FilteredTableModel<?, Attribute<?>> tableModel, int row,
                                          Attribute<?> columnIdentifier, Color cellBackgroundColor) {
      boolean conditionEnabled = ((SwingEntityTableModel) tableModel).conditionModel().enabled(columnIdentifier);
      boolean filterEnabled = tableModel.filterModel().enabled(columnIdentifier);
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

    private final AttributeDefinition<Object> objectAttributeDefinition;

    private DefaultDisplayValueProvider(AttributeDefinition<?> attributeDefinition) {
      this.objectAttributeDefinition = (AttributeDefinition<Object>) attributeDefinition;
    }

    @Override
    public Object apply(Object value) {
      return objectAttributeDefinition.string(value);
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
