/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.ProxyBuilder;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.DefaultEntityEditModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.model.component.combobox.SwingFilteredComboBoxModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link EntityEditModel}.
 */
public class SwingEntityEditModel extends DefaultEntityEditModel {

  private final State.Combination refreshingObserver = State.combination(Conjunction.OR);
  private final Map<Attribute<?>, SwingFilteredComboBoxModel<?>> comboBoxModels = new HashMap<>();

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the given entity type.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public SwingEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this(entityType, connectionProvider, connectionProvider.entities().definition(entityType).validator());
  }

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the given entity type.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public SwingEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider, EntityValidator validator) {
    super(entityType, connectionProvider, validator);
  }

  @Override
  public void clear() {
    clearComboBoxModels();
  }

  /**
   * Creates and refreshes combo box models for the given attributes. Doing this avoids refreshing the
   * data on the EDT when the actual combo boxes are initialized.
   * In case of {@link ForeignKey} a foreign key combo box model and in
   * case of a {@link Attribute} a attribute combo box model.
   * @param attributes the attributes for which to initialize combo box models
   * @see #createComboBoxModel(Attribute)
   * @see #createForeignKeyComboBoxModel(ForeignKey)
   */
  public final void initializeComboBoxModels(Attribute<?>... attributes) {
    requireNonNull(attributes);
    for (Attribute<?> attribute : attributes) {
      if (attribute instanceof ForeignKey) {
        getForeignKeyComboBoxModel((ForeignKey) attribute).refresh();
      }
      else {
        getComboBoxModel(attribute).refresh();
      }
    }
  }

  /**
   * Refreshes all foreign key combobox models
   */
  public final void refreshForeignKeyComboBoxModels() {
    synchronized (comboBoxModels) {
      for (SwingFilteredComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
        if (comboBoxModel instanceof SwingEntityComboBoxModel) {
          comboBoxModel.refresh();
        }
      }
    }
  }

  /**
   * Refreshes all combobox models
   */
  public final void refreshComboBoxModels() {
    synchronized (comboBoxModels) {
      for (SwingFilteredComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
        comboBoxModel.refresh();
      }
    }
  }

  /**
   * Clears all combobox models
   */
  public final void clearComboBoxModels() {
    synchronized (comboBoxModels) {
      for (SwingFilteredComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
        comboBoxModel.clear();
      }
    }
  }

  /**
   * Returns a {@link SwingEntityComboBoxModel} for the given foreign key attribute. If one does not exist it is created.
   * @param foreignKey the foreign key attribute
   * @return a {@link SwingEntityComboBoxModel} based on the entity referenced by the given foreign key attribute
   * @see #createForeignKeyComboBoxModel(ForeignKey)
   */
  public final SwingEntityComboBoxModel getForeignKeyComboBoxModel(ForeignKey foreignKey) {
    entityDefinition().foreignKeyProperty(foreignKey);
    synchronized (comboBoxModels) {
      // can't use computeIfAbsent() here, since that prevents recursive initialization of interdepending combo
      // box models, createForeignKeyComboBoxModel() may for example call this function
      // see javadoc: must not attempt to update any other mappings of this map
      SwingEntityComboBoxModel comboBoxModel = (SwingEntityComboBoxModel) comboBoxModels.get(foreignKey);
      if (comboBoxModel == null) {
        comboBoxModel = createAndInitializeForeignKeyComboBoxModel(foreignKey);
        comboBoxModels.put(foreignKey, comboBoxModel);
      }

      return comboBoxModel;
    }
  }

  /**
   * Returns a {@link SwingFilteredComboBoxModel} for the given attribute,
   * @param attribute the attribute
   * @param <T> the value type
   * @return a {@link SwingFilteredComboBoxModel} for the given attribute
   * @see #createComboBoxModel(Attribute)
   */
  public final <T> SwingFilteredComboBoxModel<T> getComboBoxModel(Attribute<T> attribute) {
    entityDefinition().property(attribute);
    synchronized (comboBoxModels) {
      // can't use computeIfAbsent here, see getForeignKeyComboBoxModel() comment
      SwingFilteredComboBoxModel<T> comboBoxModel = (SwingFilteredComboBoxModel<T>) comboBoxModels.get(attribute);
      if (comboBoxModel == null) {
        comboBoxModel = createAndInitializeAttributeComboBoxModel(attribute);
        comboBoxModels.put(attribute, comboBoxModel);
      }

      return comboBoxModel;
    }
  }

  /**
   * @param attribute the attribute
   * @return true if this edit model contains a combobox model for the attribute
   */
  public final boolean containsComboBoxModel(Attribute<?> attribute) {
    synchronized (comboBoxModels) {
      return comboBoxModels.containsKey(attribute);
    }
  }

  /**
   * Creates a default {@link SwingEntityComboBoxModel} for the given attribute, override to provide
   * a specific {@link SwingEntityComboBoxModel} (filtered for example) for attributes.
   * This method is called when creating a {@link SwingEntityComboBoxModel} for foreign keys, both
   * for the edit fields used when editing a single record and the edit field used when updating multiple records.
   * This default implementation returns a sorted {@link SwingEntityComboBoxModel} with the default nullValueItem
   * if the underlying attribute is nullable.
   * If the foreign key property has select attributes defined, those are set in the combo box model.
   * @param foreignKey the foreign key for which to create a {@link SwingEntityComboBoxModel}
   * @return a {@link SwingEntityComboBoxModel} for the given foreign key
   * @see FilteredComboBoxModel#COMBO_BOX_NULL_CAPTION
   * @see Property#nullable()
   * @see is.codion.framework.model.EntityComboBoxModel#setSelectAttributes(Collection)
   * @see ForeignKeyProperty#selectAttributes()
   */
  public SwingEntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition().foreignKeyProperty(foreignKey);
    SwingEntityComboBoxModel model = new SwingEntityComboBoxModel(foreignKeyProperty.referencedEntityType(), connectionProvider());
    model.setSelectAttributes(foreignKeyProperty.selectAttributes());
    if (isNullable(foreignKey)) {
      model.setNullCaption(FilteredComboBoxModel.COMBO_BOX_NULL_CAPTION.get());
    }

    return model;
  }

  /**
   * Creates a combo box model containing the current values of the given attribute.
   * This default implementation returns a sorted {@link SwingPropertyComboBoxModel} with the default nullValueItem
   * if the underlying attribute is nullable
   * @param attribute the attribute
   * @param <T> the value type
   * @return a combo box model based on the given attribute
   */
  public <T> SwingPropertyComboBoxModel<T> createComboBoxModel(Attribute<T> attribute) {
    requireNonNull(attribute, "attribute");
    SwingPropertyComboBoxModel<T> model = new SwingPropertyComboBoxModel<>(connectionProvider(), attribute);
    if (isNullable(attribute)) {
      model.setIncludeNull(true);
      if (attribute.valueClass().isInterface()) {
        model.setNullItem(ProxyBuilder.builder(attribute.valueClass())
                .method("toString", parameters -> FilteredComboBoxModel.COMBO_BOX_NULL_CAPTION.get())
                .build());
      }
    }
    addEntitiesEditedListener(model::refresh);

    return model;
  }

  @Override
  public final void addForeignKeyValues(List<Entity> entities) {
    Map<EntityType, List<Entity>> mapped = Entity.mapToType(entities);
    for (Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (ForeignKey foreignKey : entityDefinition().foreignKeys(entry.getKey())) {
        if (containsComboBoxModel(foreignKey)) {
          SwingEntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKey);
          for (Entity inserted : entry.getValue()) {
            comboBoxModel.addItem(inserted);
          }
        }
      }
    }
  }

  @Override
  public final void removeForeignKeyValues(List<Entity> entities) {
    Map<EntityType, List<Entity>> mapped = Entity.mapToType(entities);
    for (Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (ForeignKey foreignKey : entityDefinition().foreignKeys(entry.getKey())) {
        if (containsComboBoxModel(foreignKey)) {
          SwingEntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKey);
          Entity selectedEntity = comboBoxModel.selectedValue();
          for (Entity deletedEntity : entry.getValue()) {
            comboBoxModel.removeItem(deletedEntity);
          }
          if (comboBoxModel.isVisible(selectedEntity)) {
            comboBoxModel.setSelectedItem(selectedEntity);
          }//if the null value is selected we're fine, otherwise select topmost item
          else if (!comboBoxModel.isNullSelected() && comboBoxModel.getSize() > 0) {
            comboBoxModel.setSelectedItem(comboBoxModel.getElementAt(0));
          }
          else {
            comboBoxModel.setSelectedItem(null);
          }
        }
        clearForeignKeyReferences(foreignKey, entry.getValue());
      }
    }
  }

  @Override
  public final void addRefreshingObserver(StateObserver refreshingObserver) {
    this.refreshingObserver.addState(refreshingObserver);
  }

  @Override
  public final StateObserver refreshingObserver() {
    return refreshingObserver;
  }

  @Override
  protected void refreshDataModels() {
    refreshComboBoxModels();
  }

  @Override
  protected void replaceForeignKey(ForeignKey foreignKey, List<Entity> entities) {
    super.replaceForeignKey(foreignKey, entities);
    if (containsComboBoxModel(foreignKey)) {
      SwingEntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKey);
      entities.forEach(foreignKeyValue -> comboBoxModel.replaceItem(foreignKeyValue, foreignKeyValue));
    }
  }

  private void clearForeignKeyReferences(ForeignKey foreignKey, List<Entity> entities) {
    entities.forEach(entity -> {
      if (Objects.equals(entity, get(foreignKey))) {
        put(foreignKey, null);
      }
    });
  }

  private SwingEntityComboBoxModel createAndInitializeForeignKeyComboBoxModel(ForeignKey foreignKey) {
    SwingEntityComboBoxModel comboBoxModel = createForeignKeyComboBoxModel(foreignKey);
    refreshingObserver.addState(comboBoxModel.refreshingObserver());

    return comboBoxModel;
  }

  private <T> SwingFilteredComboBoxModel<T> createAndInitializeAttributeComboBoxModel(Attribute<T> attribute) {
    SwingFilteredComboBoxModel<T> comboBoxModel = createComboBoxModel(attribute);
    refreshingObserver.addState(comboBoxModel.refreshingObserver());

    return comboBoxModel;
  }
}
