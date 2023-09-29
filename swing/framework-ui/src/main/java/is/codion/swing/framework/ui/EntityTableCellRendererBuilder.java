/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ItemColumnDefinition;
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
            attributeDefinition.attribute().type().isBoolean() && !(attributeDefinition instanceof ItemColumnDefinition));
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
      boolean conditionEnabled = ((SwingEntityTableModel) tableModel).conditionModel().isEnabled(columnIdentifier);
      boolean filterEnabled = tableModel.filterModel().isEnabled(columnIdentifier);
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
      return objectAttributeDefinition.toString(value);
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
