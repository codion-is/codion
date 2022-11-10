/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.ProxyBuilder;
import is.codion.common.db.exception.DatabaseException;
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
import is.codion.framework.model.AbstractEntityEditModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link EntityEditModel}.
 */
public class SwingEntityEditModel extends AbstractEntityEditModel {

  private final State.Combination refreshingObserver = State.combination(Conjunction.OR);
  private final Map<Attribute<?>, FilteredComboBoxModel<?>> comboBoxModels = new HashMap<>();

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the given entity type.
   * @param entityType the type of the entity to base this {@link SwingEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public SwingEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this(entityType, connectionProvider, connectionProvider.entities().definition(entityType).validator());
  }

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the given entity type.
   * @param entityType the type of the entity to base this {@link SwingEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public SwingEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider, EntityValidator validator) {
    super(entityType, connectionProvider, validator);
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
        foreignKeyComboBoxModel((ForeignKey) attribute).refresh();
      }
      else {
        comboBoxModel(attribute).refresh();
      }
    }
  }

  /**
   * Refreshes all foreign key combobox models
   */
  public final void refreshForeignKeyComboBoxModels() {
    synchronized (comboBoxModels) {
      for (FilteredComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
        if (comboBoxModel instanceof EntityComboBoxModel) {
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
      for (FilteredComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
        comboBoxModel.refresh();
      }
    }
  }

  /**
   * Clears all combobox models
   */
  public final void clearComboBoxModels() {
    synchronized (comboBoxModels) {
      for (FilteredComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
        comboBoxModel.clear();
      }
    }
  }

  /**
   * Returns a {@link EntityComboBoxModel} for the given foreign key attribute. If one does not exist it is created.
   * @param foreignKey the foreign key attribute
   * @return a {@link EntityComboBoxModel} based on the entity referenced by the given foreign key attribute
   * @see #createForeignKeyComboBoxModel(ForeignKey)
   */
  public final EntityComboBoxModel foreignKeyComboBoxModel(ForeignKey foreignKey) {
    entityDefinition().foreignKeyProperty(foreignKey);
    synchronized (comboBoxModels) {
      // can't use computeIfAbsent() here, since that prevents recursive initialization of interdepending combo
      // box models, createForeignKeyComboBoxModel() may for example call this function
      // see javadoc: must not attempt to update any other mappings of this map
      EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) comboBoxModels.get(foreignKey);
      if (comboBoxModel == null) {
        comboBoxModel = createAndInitializeForeignKeyComboBoxModel(foreignKey);
        comboBoxModels.put(foreignKey, comboBoxModel);
      }

      return comboBoxModel;
    }
  }

  /**
   * Returns a {@link FilteredComboBoxModel} for the given attribute,
   * @param attribute the attribute
   * @param <T> the value type
   * @return a {@link FilteredComboBoxModel} for the given attribute
   * @see #createComboBoxModel(Attribute)
   */
  public final <T> FilteredComboBoxModel<T> comboBoxModel(Attribute<T> attribute) {
    entityDefinition().property(attribute);
    synchronized (comboBoxModels) {
      // can't use computeIfAbsent here, see foreignKeyComboBoxModel() comment
      FilteredComboBoxModel<T> comboBoxModel = (FilteredComboBoxModel<T>) comboBoxModels.get(attribute);
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
   * Creates a default {@link EntityComboBoxModel} for the given attribute, override to provide
   * a specific {@link EntityComboBoxModel} (filtered for example) for attributes.
   * This method is called when creating a {@link EntityComboBoxModel} for foreign keys, both
   * for the edit fields used when editing a single record and the edit field used when updating multiple records.
   * This default implementation returns a sorted {@link EntityComboBoxModel} with the default nullValueItem
   * if the underlying attribute is nullable.
   * If the foreign key property has select attributes defined, those are set in the combo box model.
   * @param foreignKey the foreign key for which to create a {@link EntityComboBoxModel}
   * @return a {@link EntityComboBoxModel} for the given foreign key
   * @see FilteredComboBoxModel#COMBO_BOX_NULL_CAPTION
   * @see Property#isNullable()
   * @see EntityComboBoxModel#setSelectAttributes(Collection)
   * @see ForeignKeyProperty#selectAttributes()
   */
  public EntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey) {
    ForeignKeyProperty foreignKeyProperty = entityDefinition().foreignKeyProperty(foreignKey);
    EntityComboBoxModel model = new EntityComboBoxModel(foreignKeyProperty.referencedType(), connectionProvider());
    model.setSelectAttributes(foreignKeyProperty.selectAttributes());
    if (isNullable(foreignKey)) {
      model.setNullCaption(FilteredComboBoxModel.COMBO_BOX_NULL_CAPTION.get());
    }

    return model;
  }

  /**
   * Creates a combo box model containing the current values of the given attribute.
   * This default implementation returns a sorted {@link FilteredComboBoxModel} with the default nullValueItem
   * if the underlying attribute is nullable
   * @param attribute the attribute
   * @param <T> the value type
   * @return a combo box model based on the given attribute
   */
  public <T> FilteredComboBoxModel<T> createComboBoxModel(Attribute<T> attribute) {
    requireNonNull(attribute, "attribute");
    AttributeComboBoxModel<T> model = new AttributeComboBoxModel<>(connectionProvider(), attribute);
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
  public final void addForeignKeyValues(ForeignKey foreignKey, Collection<Entity> entities) {
    requireNonNull(foreignKey);
    requireNonNull(entities);
    if (containsComboBoxModel(foreignKey)) {
      EntityComboBoxModel comboBoxModel = foreignKeyComboBoxModel(foreignKey);
      for (Entity inserted : entities) {
        comboBoxModel.addItem(inserted);
      }
    }
  }

  @Override
  public final void removeForeignKeyValues(ForeignKey foreignKey, Collection<Entity> entities) {
    requireNonNull(foreignKey);
    requireNonNull(entities);
    if (containsComboBoxModel(foreignKey)) {
      EntityComboBoxModel comboBoxModel = foreignKeyComboBoxModel(foreignKey);
      Entity selectedEntity = comboBoxModel.selectedValue();
      for (Entity deletedEntity : entities) {
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
      clearForeignKeyReferences(foreignKey, entities);
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
  protected void replaceForeignKey(ForeignKey foreignKey, Collection<Entity> entities) {
    super.replaceForeignKey(foreignKey, entities);
    if (containsComboBoxModel(foreignKey)) {
      EntityComboBoxModel comboBoxModel = foreignKeyComboBoxModel(foreignKey);
      entities.forEach(foreignKeyValue -> comboBoxModel.replaceItem(foreignKeyValue, foreignKeyValue));
    }
  }

  private void clearForeignKeyReferences(ForeignKey foreignKey, Collection<Entity> entities) {
    entities.forEach(entity -> {
      if (Objects.equals(entity, get(foreignKey))) {
        put(foreignKey, null);
      }
    });
  }

  private EntityComboBoxModel createAndInitializeForeignKeyComboBoxModel(ForeignKey foreignKey) {
    EntityComboBoxModel comboBoxModel = createForeignKeyComboBoxModel(foreignKey);
    refreshingObserver.addState(comboBoxModel.refreshingObserver());

    return comboBoxModel;
  }

  private <T> FilteredComboBoxModel<T> createAndInitializeAttributeComboBoxModel(Attribute<T> attribute) {
    FilteredComboBoxModel<T> comboBoxModel = createComboBoxModel(attribute);
    refreshingObserver.addState(comboBoxModel.refreshingObserver());

    return comboBoxModel;
  }

  private static final class AttributeComboBoxModel<T> extends FilteredComboBoxModel<T> {

    private final EntityConnectionProvider connectionProvider;
    private final Attribute<T> attribute;

    private AttributeComboBoxModel(EntityConnectionProvider connectionProvider, Attribute<T> attribute) {
      this.connectionProvider = connectionProvider;
      this.attribute = attribute;
    }

    @Override
    protected Collection<T> refreshItems() {
      try {
        return connectionProvider.connection().select(attribute);
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
