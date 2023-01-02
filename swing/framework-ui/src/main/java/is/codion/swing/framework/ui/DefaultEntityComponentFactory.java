/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.text.TemporalInputPanel;
import is.codion.swing.framework.model.EntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.component.EntityComponents;

import javax.swing.JComponent;
import java.time.temporal.Temporal;

import static is.codion.swing.common.ui.component.Components.fileInputPanel;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityComponentFactory} implementation.
 */
public class DefaultEntityComponentFactory<T, A extends Attribute<T>, C extends JComponent> implements EntityComponentFactory<T, A, C> {

  @Override
  public ComponentValue<T, C> createComponentValue(A attribute, SwingEntityEditModel editModel, T initialValue) {
    requireNonNull(attribute, "attribute");
    requireNonNull(editModel, "editModel");
    if (attribute instanceof ForeignKey) {
      return createForeignKeyComponentValue((ForeignKey) attribute, editModel, (Entity) initialValue);
    }

    EntityComponents inputComponents = new EntityComponents(editModel.entityDefinition());
    if (attribute.isTemporal()) {
      ComponentValue<Temporal, TemporalInputPanel<Temporal>> componentValue =
              inputComponents.temporalInputPanel((Attribute<Temporal>) attribute)
                      .buildComponentValue();
      componentValue.set((Temporal) initialValue);

      return (ComponentValue<T, C>) componentValue;
    }
    if (attribute.isByteArray()) {
      return (ComponentValue<T, C>) fileInputPanel()
              .buildComponentValue();
    }

    return (ComponentValue<T, C>) inputComponents.component(attribute)
            .initialValue(initialValue)
            .buildComponentValue();
  }

  private ComponentValue<T, C> createForeignKeyComponentValue(ForeignKey foreignKey, SwingEntityEditModel editModel,
                                                              Entity initialValue) {
    EntityComponents inputComponents = new EntityComponents(editModel.entityDefinition());
    if (editModel.connectionProvider().entities().definition(foreignKey.referencedType()).isSmallDataset()) {
      EntityComboBoxModel comboBoxModel = editModel.createForeignKeyComboBoxModel(foreignKey);
      comboBoxModel.setSelectedItem(initialValue);

      return (ComponentValue<T, C>) inputComponents.foreignKeyComboBox(foreignKey, comboBoxModel)
              .onSetVisible(comboBox -> comboBoxModel.refresh())
              .buildComponentValue();
    }

    EntitySearchModel searchModel = editModel.createForeignKeySearchModel(foreignKey);
    searchModel.setSelectedEntity(initialValue);

    return (ComponentValue<T, C>) inputComponents.foreignKeySearchField(foreignKey, searchModel).buildComponentValue();
  }
}
