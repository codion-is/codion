/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.textfield.TemporalInputPanel;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.component.EntityInputComponents;

import javax.swing.JComponent;
import java.time.temporal.Temporal;

import static is.codion.swing.common.ui.component.ComponentValues.fileInputPanel;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityComponentFactory} implementation.
 */
public class DefaultEntityComponentFactory<T, A extends Attribute<T>, C extends JComponent> implements EntityComponentFactory<T, A, C> {

  @Override
  public ComponentValue<T, C> createComponentValue(final A attribute, final SwingEntityEditModel editModel, final T initialValue) {
    requireNonNull(attribute, "attribute");
    requireNonNull(editModel, "editModel");
    if (attribute instanceof ForeignKey) {
      return createForeignKeyComponentValue((ForeignKey) attribute, editModel, (Entity) initialValue);
    }

    final EntityInputComponents inputComponents = new EntityInputComponents(editModel.getEntityDefinition());
    if (attribute.isTemporal()) {
      final ComponentValue<Temporal, TemporalInputPanel<Temporal>> componentValue =
              inputComponents.getComponents().temporalInputPanel((Attribute<Temporal>) attribute).buildComponentValue();
      componentValue.set((Temporal) initialValue);

      return (ComponentValue<T, C>) componentValue;
    }
    if (attribute.isByteArray()) {
      return (ComponentValue<T, C>) fileInputPanel();
    }

    final ComponentValue<T, C> componentValue = inputComponents.createInputComponent(attribute);
    componentValue.set(initialValue);

    return componentValue;
  }

  private ComponentValue<T, C> createForeignKeyComponentValue(final ForeignKey foreignKey, final SwingEntityEditModel editModel,
                                                              final Entity initialValue) {
    final EntityInputComponents inputComponents = new EntityInputComponents(editModel.getEntityDefinition());
    if (editModel.getConnectionProvider().getEntities().getDefinition(foreignKey.getReferencedEntityType()).isSmallDataset()) {
      final SwingEntityComboBoxModel comboBoxModel = editModel.createForeignKeyComboBoxModel(foreignKey);
      comboBoxModel.setSelectedItem(initialValue);

      return (ComponentValue<T, C>) inputComponents.getComponents().foreignKeyComboBox(foreignKey, comboBoxModel)
              .onSetVisible(comboBox -> comboBoxModel.refresh())
              .buildComponentValue();
    }

    final EntitySearchModel searchModel = editModel.createForeignKeySearchModel(foreignKey);
    searchModel.setSelectedEntity(initialValue);

    return (ComponentValue<T, C>) inputComponents.getComponents().foreignKeySearchField(foreignKey, searchModel).buildComponentValue();
  }
}
