/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.textfield.TemporalInputPanel;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.component.EntityInputComponents;

import javax.swing.JComponent;
import java.time.temporal.Temporal;

import static is.codion.swing.common.ui.value.ComponentValues.fileInputPanel;
import static java.util.Objects.requireNonNull;

/**
 * Provides {@link ComponentValue} implementations.
 */
public class EntityComponentValues {

  /**
   * Provides value input components for multiple entity update, override to supply
   * specific {@link ComponentValue} implementations for attributes.
   * Remember to return with a call to super.createComponentValue() after handling your case.
   * @param attribute the attribute for which to get the ComponentValue
   * @param editModel the edit model used to create foreign key input models
   * @param initialValue the initial value to display
   * @param <T> the attribute type
   * @param <C> the component type
   * @return the ComponentValue handling input for {@code attribute}
   */
  public <T, C extends JComponent> ComponentValue<T, C> createComponentValue(final Attribute<T> attribute, final SwingEntityEditModel editModel,
                                                                             final T initialValue) {
    requireNonNull(attribute, "attribute");
    requireNonNull(editModel, "editModel");
    if (attribute instanceof ForeignKey) {
      return (ComponentValue<T, C>) createForeignKeyComponentValue((ForeignKey) attribute, editModel, (Entity) initialValue);
    }

    final EntityInputComponents inputComponents = new EntityInputComponents(editModel.getEntityDefinition());
    if (attribute.isTemporal()) {
      final ComponentValue<Temporal, TemporalInputPanel<Temporal>> componentValue =
              inputComponents.getComponentBuilders().temporalInputPanel((Attribute<Temporal>) attribute).buildComponentValue();
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

  /**
   * Creates a {@link ComponentValue} for the given foreign key
   * @param foreignKey the foreign key
   * @param editModel the edit model involved in the updating
   * @param initialValue the current value to initialize the ComponentValue with
   * @param <T> the component type
   * @return a {@link ComponentValue} for the given foreign key
   */
  protected <T extends JComponent> ComponentValue<Entity, T> createForeignKeyComponentValue(final ForeignKey foreignKey,
                                                                                            final SwingEntityEditModel editModel,
                                                                                            final Entity initialValue) {
    final EntityInputComponents inputComponents = new EntityInputComponents(editModel.getEntityDefinition());
    if (editModel.getConnectionProvider().getEntities().getDefinition(foreignKey.getReferencedEntityType()).isSmallDataset()) {
      final SwingEntityComboBoxModel comboBoxModel = editModel.createForeignKeyComboBoxModel(foreignKey);
      comboBoxModel.setSelectedItem(initialValue);

      return (ComponentValue<Entity, T>) inputComponents.getComponentBuilders().foreignKeyComboBox(foreignKey, comboBoxModel)
              .refreshOnSetVisible(true)
              .buildComponentValue();
    }

    final EntitySearchModel searchModel = editModel.createForeignKeySearchModel(foreignKey);
    searchModel.setSelectedEntity(initialValue);

    return (ComponentValue<Entity, T>) inputComponents.getComponentBuilders().foreignKeySearchField(foreignKey, searchModel).buildComponentValue();
  }
}
