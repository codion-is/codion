/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.ui.component.text.TemporalFieldPanel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;

import javax.swing.JComponent;
import java.time.temporal.Temporal;

import static is.codion.swing.common.ui.component.Components.fileInputPanel;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityComponentFactory} implementation.
 */
public class DefaultEntityComponentFactory<T, A extends Attribute<T>, C extends JComponent> implements EntityComponentFactory<T, A, C> {

  @Override
  public ComponentValue<T, C> componentValue(A attribute, SwingEntityEditModel editModel, T initialValue) {
    requireNonNull(attribute, "attribute");
    requireNonNull(editModel, "editModel");
    EntityComponents inputComponents = new EntityComponents(editModel.entityDefinition());
    if (attribute instanceof ForeignKey) {
      return createForeignKeyComponentValue((ForeignKey) attribute, editModel, (Entity) initialValue, inputComponents);
    }
    if (attribute.type().isTemporal()) {
      return createTemporalComponentValue(attribute, (Temporal) initialValue, inputComponents);
    }
    if (attribute.type().isByteArray()) {
      return (ComponentValue<T, C>) fileInputPanel()
              .buildValue();
    }

    return (ComponentValue<T, C>) inputComponents.component(attribute)
            .initialValue(initialValue)
            .buildValue();
  }

  private ComponentValue<T, C> createForeignKeyComponentValue(ForeignKey foreignKey, SwingEntityEditModel editModel,
                                                              Entity initialValue, EntityComponents inputComponents) {
    if (editModel.entities().definition(foreignKey.referencedType()).smallDataset()) {
      return (ComponentValue<T, C>) inputComponents.foreignKeyComboBox(foreignKey, editModel.createForeignKeyComboBoxModel(foreignKey))
              .initialValue(initialValue)
              .onSetVisible(comboBox -> comboBox.getModel().refresh())
              .buildValue();
    }

    return (ComponentValue<T, C>) inputComponents.foreignKeySearchField(foreignKey, editModel.createForeignKeySearchModel(foreignKey))
            .initialValue(initialValue)
            .buildValue();
  }

  private static <T, A extends Attribute<T>, C extends JComponent> ComponentValue<T, C> createTemporalComponentValue(A attribute,
                                                                                                                     Temporal initialValue,
                                                                                                                     EntityComponents inputComponents) {
    if (TemporalFieldPanel.supports((Class<Temporal>) attribute.type().valueClass())) {
      return (ComponentValue<T, C>) inputComponents.temporalFieldPanel((Attribute<Temporal>) attribute)
              .initialValue(initialValue)
              .buildValue();
    }

    return (ComponentValue<T, C>) inputComponents.temporalField((Attribute<Temporal>) attribute)
            .initialValue(initialValue)
            .buildValue();
  }
}
