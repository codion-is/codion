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
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityConditionModelFactory;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Optional;

import static is.codion.swing.framework.model.EntityComboBoxConditionModel.entityComboBoxConditionModel;

/**
 * A Swing {@link ColumnConditionModel.Factory} implementation using {@link EntityComboBoxModel} for foreign keys based on small datasets
 */
public class SwingEntityConditionModelFactory extends EntityConditionModelFactory {

  /**
   * Instantiates a new {@link SwingEntityConditionModelFactory}.
   * @param connectionProvider the connection provider
   */
  public SwingEntityConditionModelFactory(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
  }

  @Override
  public Optional<ColumnConditionModel<? extends Attribute<?>, ?>> createConditionModel(Attribute<?> attribute) {
    if (attribute instanceof ForeignKey) {
      ForeignKey foreignKey = (ForeignKey) attribute;
      if (definition(foreignKey.referencedType()).smallDataset()) {
        return Optional.of(entityComboBoxConditionModel(foreignKey, createComboBoxModel(foreignKey)));
      }
    }

    return super.createConditionModel(attribute);
  }

  /**
   * Creates a combo box model based on the given foreign key
   * @param foreignKey the foreign key
   * @return a combo box model based on the given foreign key
   */
  protected EntityComboBoxModel createComboBoxModel(ForeignKey foreignKey) {
    EntityComboBoxModel comboBoxModel = new EntityComboBoxModel(foreignKey.referencedType(), connectionProvider());
    comboBoxModel.setNullCaption(FilteredComboBoxModel.COMBO_BOX_NULL_CAPTION.get());

    return comboBoxModel;
  }
}
