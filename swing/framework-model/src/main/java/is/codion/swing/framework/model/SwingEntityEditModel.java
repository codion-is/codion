/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.event.Event;
import is.codion.common.event.EventListener;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link EntityEditModel}.
 */
public class SwingEntityEditModel extends DefaultEntityEditModel {

  private final State.Combination refreshingObserver = State.combination(Conjunction.OR);
  private final Event<?> afterRefreshEvent = Event.event();

  /**
   * Holds the ComboBoxModels used by this {@link EntityEditModel}
   */
  private final Map<Attribute<?>, FilteredComboBoxModel<?>> comboBoxModels = new ConcurrentHashMap<>();

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the entity identified by {@code entityType}.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public SwingEntityEditModel(final EntityType entityType, final EntityConnectionProvider connectionProvider) {
    this(entityType, connectionProvider, connectionProvider.getEntities().getDefinition(entityType).getValidator());
  }

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the entity identified by {@code entityType}.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public SwingEntityEditModel(final EntityType entityType, final EntityConnectionProvider connectionProvider, final EntityValidator validator) {
    super(entityType, connectionProvider, validator);
  }

  @Override
  public void clear() {
    clearComboBoxModels();
  }

  /**
   * Refreshes all foreign key combobox models
   */
  public final void refreshForeignKeyComboBoxModels() {
    for (final FilteredComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
      if (comboBoxModel instanceof SwingEntityComboBoxModel) {
        comboBoxModel.refresh();
      }
    }
  }

  /**
   * Refreshes all combobox models
   */
  public final void refreshComboBoxModels() {
    for (final FilteredComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
      comboBoxModel.refresh();
    }
  }

  /**
   * Clears all combobox models
   */
  public final void clearComboBoxModels() {
    for (final FilteredComboBoxModel<?> comboBoxModel : comboBoxModels.values()) {
      comboBoxModel.clear();
    }
  }

  /**
   * Returns a {@link SwingEntityComboBoxModel} for the given foreign key attribute. If one does not exist it is created.
   * @param foreignKey the foreign key attribute
   * @return a {@link SwingEntityComboBoxModel} based on the entity referenced by the given foreign key attribute
   * @see #createForeignKeyComboBoxModel(ForeignKey)
   */
  public final SwingEntityComboBoxModel getForeignKeyComboBoxModel(final ForeignKey foreignKey) {
    getEntityDefinition().getForeignKeyProperty(foreignKey);
    synchronized (comboBoxModels) {
      SwingEntityComboBoxModel comboBoxModel = (SwingEntityComboBoxModel) comboBoxModels.get(foreignKey);
      if (comboBoxModel == null) {
        comboBoxModel = createForeignKeyComboBoxModel(foreignKey);
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
  public final <T> FilteredComboBoxModel<T> getComboBoxModel(final Attribute<T> attribute) {
    getEntityDefinition().getProperty(attribute);
    synchronized (comboBoxModels) {
      FilteredComboBoxModel<?> comboBoxModel = comboBoxModels.get(attribute);
      if (comboBoxModel == null) {
        comboBoxModel = createComboBoxModel(attribute);
        comboBoxModels.put(attribute, comboBoxModel);
      }

      return (FilteredComboBoxModel<T>) comboBoxModel;
    }
  }

  /**
   * @param attribute the attribute
   * @return true if this edit model contains a combobox model for the attribute
   */
  public final boolean containsComboBoxModel(final Attribute<?> attribute) {
    return comboBoxModels.containsKey(attribute);
  }

  /**
   * Creates a default {@link SwingEntityComboBoxModel} for the given attribute, override to provide
   * a specific {@link SwingEntityComboBoxModel} (filtered for example) for attributes.
   * This method is called when creating a {@link SwingEntityComboBoxModel} for entity attributes, both
   * for the edit fields used when editing a single record and the edit field used
   * when updating multiple records.
   * This default implementation returns a sorted {@link SwingEntityComboBoxModel} with the default nullValueItem
   * if the underlying attribute is nullable.
   * If the foreign key property has select attributes defined, those are set in the combo box model.
   * @param foreignKey the foreign key for which to create a {@link SwingEntityComboBoxModel}
   * @return a {@link SwingEntityComboBoxModel} for the given foreign key
   * @see FilteredComboBoxModel#COMBO_BOX_NULL_VALUE_ITEM
   * @see Property#isNullable()
   * @see is.codion.framework.model.EntityComboBoxModel#setSelectAttributes(Collection)
   * @see ForeignKeyProperty#getSelectAttributes()
   */
  public SwingEntityComboBoxModel createForeignKeyComboBoxModel(final ForeignKey foreignKey) {
    final ForeignKeyProperty foreignKeyProperty = getEntityDefinition().getForeignKeyProperty(foreignKey);
    final SwingEntityComboBoxModel model =
            new SwingEntityComboBoxModel(foreignKeyProperty.getReferencedEntityType(), getConnectionProvider());
    model.setSelectAttributes(foreignKeyProperty.getSelectAttributes());
    if (getValidator().isNullable(getEntity(), foreignKeyProperty)) {
      model.setNullString(FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());
    }
    refreshingObserver.addState(model.getRefreshingObserver());

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
  public <T> SwingPropertyComboBoxModel<T> createComboBoxModel(final Attribute<T> attribute) {
    requireNonNull(attribute, "attribute");
    final SwingPropertyComboBoxModel<T> model = new SwingPropertyComboBoxModel<>(getConnectionProvider(), attribute, null);
    model.setNullString(getValidator().isNullable(getEntity(), getEntityDefinition().getProperty(attribute)) ?
            FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get() : null);
    model.refresh();
    addEntitiesEditedListener(model::refresh);
    refreshingObserver.addState(model.getRefreshingObserver());

    return model;
  }

  @Override
  public final void addForeignKeyValues(final List<Entity> entities) {
    final Map<EntityType, List<Entity>> mapped = Entity.mapToType(entities);
    for (final Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKey foreignKey : getEntityDefinition().getForeignKeys(entry.getKey())) {
        if (containsComboBoxModel(foreignKey)) {
          final SwingEntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKey);
          for (final Entity inserted : entry.getValue()) {
            comboBoxModel.addItem(inserted);
          }
        }
      }
    }
  }

  @Override
  public final void removeForeignKeyValues(final List<Entity> entities) {
    final Map<EntityType, List<Entity>> mapped = Entity.mapToType(entities);
    for (final Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKey foreignKey : getEntityDefinition().getForeignKeys(entry.getKey())) {
        if (containsComboBoxModel(foreignKey)) {
          final SwingEntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKey);
          final Entity selectedEntity = comboBoxModel.getSelectedValue();
          for (final Entity deletedEntity : entry.getValue()) {
            comboBoxModel.removeItem(deletedEntity);
          }
          if (comboBoxModel.isVisible(selectedEntity)) {
            comboBoxModel.setSelectedItem(selectedEntity);
          }//if the null value is selected we're fine, otherwise select topmost item
          else if (!comboBoxModel.isNullValueSelected() && comboBoxModel.getSize() > 0) {
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
  public final void addRefreshingObserver(final StateObserver refreshingObserver) {
    this.refreshingObserver.addState(refreshingObserver);
  }

  @Override
  public final StateObserver getRefreshingObserver() {
    return refreshingObserver;
  }

  @Override
  public final void addAfterRefreshListener(final EventListener listener) {
    afterRefreshEvent.addListener(listener);
  }

  @Override
  public final void removeAfterRefreshListener(final EventListener listener) {
    afterRefreshEvent.removeListener(listener);
  }

  @Override
  protected void refreshDataModels() {
    refreshComboBoxModels();
    afterRefreshEvent.onEvent();
  }

  @Override
  protected void replaceForeignKey(final ForeignKey foreignKey, final List<Entity> entities) {
    super.replaceForeignKey(foreignKey, entities);
    if (containsComboBoxModel(foreignKey)) {
      final SwingEntityComboBoxModel comboBoxModel = getForeignKeyComboBoxModel(foreignKey);
      entities.forEach(foreignKeyValue -> comboBoxModel.replaceItem(foreignKeyValue, foreignKeyValue));
    }
  }

  private void clearForeignKeyReferences(final ForeignKey foreignKey, final List<Entity> entities) {
    entities.forEach(entity -> {
      if (Objects.equals(entity, get(foreignKey))) {
        put(foreignKey, null);
      }
    });
  }
}
