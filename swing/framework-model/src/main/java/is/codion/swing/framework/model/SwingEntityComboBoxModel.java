/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.ProxyBuilder;
import is.codion.common.Util;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.framework.model.EntityEditEvents;
import is.codion.swing.common.model.component.combobox.SwingFilteredComboBoxModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.framework.db.condition.Conditions.condition;
import static is.codion.framework.db.condition.Conditions.where;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.
 */
public class SwingEntityComboBoxModel extends SwingFilteredComboBoxModel<Entity> implements EntityComboBoxModel {

  private final EntityType entityType;
  private final EntityConnectionProvider connectionProvider;
  /** The attributes to include when selecting the entities for this combo box model, an empty list indicates all attributes */
  private final Collection<Attribute<?>> selectAttributes = new ArrayList<>(0);
  private final Entities entities;
  private final OrderBy orderBy;
  /** A map of entities used to filter the contents of this model by foreign key value. */
  private final Map<ForeignKey, Set<Entity>> foreignKeyFilterEntities = new HashMap<>();
  private final Predicate<Entity> foreignKeyIncludeCondition = new ForeignKeyIncludeCondition();

  //we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
  private final EventDataListener<List<Entity>> insertListener = new InsertListener();
  private final EventDataListener<Map<Key, Entity>> updateListener = new UpdateListener();
  private final EventDataListener<List<Entity>> deleteListener = new DeleteListener();

  /** true if the data should only be fetched once, unless {@code forceRefresh()} is called */
  private boolean staticData = false;
  /** used to indicate that a refresh is being forced, as in, overriding the staticData directive */
  private boolean forceRefresh = false;
  private Supplier<Condition> selectConditionSupplier;
  private boolean strictForeignKeyFiltering = true;
  private boolean listenToEditEvents = true;

  /**
   * @param entityType the type of the entity this combo box model should represent
   * @param connectionProvider a EntityConnectionProvider instance
   */
  public SwingEntityComboBoxModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this.entityType = requireNonNull(entityType, "entityType");
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.entities = connectionProvider.entities();
    this.orderBy = this.entities.getDefinition(entityType).getOrderBy();
    setStaticData(this.entities.getDefinition(entityType).isStaticData());
    setIncludeCondition(foreignKeyIncludeCondition);
    addRefreshListener(() -> forceRefresh = false);
    addRefreshFailedListener(throwable -> forceRefresh = false);
    addEditEventListeners();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + " [entityType: " + entityType + "]";
  }

  @Override
  public final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  @Override
  public final EntityType entityType() {
    return entityType;
  }

  @Override
  public final void forceRefresh() {
    forceRefresh = true;
    refresh();
  }

  @Override
  public final boolean isStaticData() {
    return staticData;
  }

  @Override
  public final void setStaticData(boolean staticData) {
    this.staticData = staticData;
  }

  @Override
  public final void setNullCaption(String caption) {
    setIncludeNull(true);
    setNullItem(ProxyBuilder.builder(Entity.class)
            .delegate(entities.entity(entityType))
            .method("toString", arguments -> caption)
            .build());
  }

  @Override
  public final void setSelectAttributes(Collection<Attribute<?>> selectAttributes) {
    for (Attribute<?> attribute : requireNonNull(selectAttributes)) {
      if (!attribute.entityType().equals(entityType)) {
        throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity type: " + entityType);
      }
    }
    this.selectAttributes.clear();
    this.selectAttributes.addAll(selectAttributes);
  }

  @Override
  public final boolean isListenToEditEvents() {
    return listenToEditEvents;
  }

  @Override
  public final void setListenToEditEvents(boolean listenToEditEvents) {
    this.listenToEditEvents = listenToEditEvents;
    if (listenToEditEvents) {
      addEditEventListeners();
    }
    else {
      removeEditEventListeners();
    }
  }

  @Override
  public final Optional<Entity> getEntity(Key primaryKey) {
    requireNonNull(primaryKey);

    return items().stream()
            .filter(entity -> entity != null && entity.getPrimaryKey().equals(primaryKey))
            .findFirst();
  }

  @Override
  public final void setSelectedEntityByKey(Key primaryKey) {
    requireNonNull(primaryKey);
    int indexOfKey = getIndexOfKey(primaryKey);
    if (indexOfKey >= 0) {
      setSelectedItem(getElementAt(indexOfKey));
    }
    else {
      int filteredIndexOfKey = getFilteredIndexOfKey(primaryKey);
      if (filteredIndexOfKey >= 0) {
        setSelectedItem(filteredItems().get(filteredIndexOfKey));
      }
    }
  }

  @Override
  public final void setSelectConditionSupplier(Supplier<Condition> selectConditionSupplier) {
    this.selectConditionSupplier = selectConditionSupplier;
  }

  @Override
  public final Supplier<Condition> getSelectConditionSupplier() {
    return this.selectConditionSupplier;
  }

  @Override
  public final Predicate<Entity> foreignKeyIncludeCondition() {
    return foreignKeyIncludeCondition;
  }

  @Override
  public final void setForeignKeyFilterEntities(ForeignKey foreignKey, Collection<Entity> entities) {
    requireNonNull(foreignKey);
    if (Util.nullOrEmpty(entities)) {
      foreignKeyFilterEntities.remove(foreignKey);
    }
    else {
      foreignKeyFilterEntities.put(foreignKey, new HashSet<>(entities));
    }
    setIncludeCondition(foreignKeyIncludeCondition);
  }

  @Override
  public final Collection<Entity> getForeignKeyFilterEntities(ForeignKey foreignKey) {
    requireNonNull(foreignKey);
    if (foreignKeyFilterEntities.containsKey(foreignKey)) {
      return unmodifiableCollection(new ArrayList<>(foreignKeyFilterEntities.get(foreignKey)));
    }

    return emptyList();
  }

  @Override
  public final void setStrictForeignKeyFiltering(boolean strictForeignKeyFiltering) {
    this.strictForeignKeyFiltering = strictForeignKeyFiltering;
  }

  @Override
  public final boolean isStrictForeignKeyFiltering() {
    return strictForeignKeyFiltering;
  }

  @Override
  public final SwingEntityComboBoxModel createForeignKeyFilterComboBoxModel(ForeignKey foreignKey) {
    return createForeignKeyComboBoxModel(foreignKey, true);
  }

  @Override
  public final SwingEntityComboBoxModel createForeignKeyConditionComboBoxModel(ForeignKey foreignKey) {
    return createForeignKeyComboBoxModel(foreignKey, false);
  }

  @Override
  public final void linkForeignKeyFilterComboBoxModel(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
    linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, true);
  }

  @Override
  public final void linkForeignKeyConditionComboBoxModel(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
    linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, false);
  }

  @Override
  public final <T> Value<T> selectorValue(Attribute<T> attribute) {
    if (!entities.getDefinition(entityType()).containsAttribute(attribute)) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + entityType());
    }

    return selectorValue(new EntityFinder<>(attribute));
  }

  @Override
  protected final Entity translateSelectionItem(Object item) {
    if (item == null) {
      return null;
    }

    if (item instanceof Entity) {
      int indexOfKey = getIndexOfKey(((Entity) item).getPrimaryKey());
      if (indexOfKey >= 0) {
        return getElementAt(indexOfKey);
      }

      return (Entity) item;
    }
    String itemToString = item.toString();

    return visibleItems().stream()
            .filter(visibleItem -> visibleItem != null && itemToString.equals(visibleItem.toString()))
            .findFirst()
            //item not found, select null value
            .orElse(null);
  }

  @Override
  protected final Collection<Entity> refreshItems() {
    if (staticData && !isCleared() && !forceRefresh) {
      return super.refreshItems();
    }

    return performQuery();
  }

  /**
   * Retrieves the entities to present in this EntityComboBoxModel, taking into account
   * the {@link #getSelectConditionSupplier()} specified for this model
   * @return the entities to present in this EntityComboBoxModel
   * @see #getSelectConditionSupplier()
   */
  protected Collection<Entity> performQuery() {
    try {
      Condition condition = selectConditionSupplier == null ? condition(entityType) : selectConditionSupplier.get();

      return connectionProvider.connection().select(condition.selectBuilder()
              .selectAttributes(selectAttributes)
              .orderBy(orderBy)
              .build());
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private int getIndexOfKey(Key primaryKey) {
    int size = getSize();
    int startIndex = isIncludeNull() ? 1 : 0;
    for (int index = startIndex; index < size; index++) {
      Entity item = getElementAt(index);
      if (item != null && item.getPrimaryKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private int getFilteredIndexOfKey(Key primaryKey) {
    List<Entity> filteredItems = filteredItems();
    for (int index = 0; index < filteredItems.size(); index++) {
      Entity item = filteredItems.get(index);
      if (item.getPrimaryKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private SwingEntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey, boolean filter) {
    ForeignKeyProperty foreignKeyProperty = entities.getDefinition(entityType).getForeignKeyProperty(foreignKey);
    SwingEntityComboBoxModel foreignKeyModel =
            new SwingEntityComboBoxModel(foreignKeyProperty.referencedEntityType(), connectionProvider);
    foreignKeyModel.setNullCaption(FilteredComboBoxModel.COMBO_BOX_NULL_CAPTION.get());
    foreignKeyModel.refresh();
    linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, filter);

    return foreignKeyModel;

  }

  private void linkForeignKeyComboBoxModel(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel,
                                           boolean filter) {
    ForeignKeyProperty foreignKeyProperty = entities.getDefinition(entityType).getForeignKeyProperty(foreignKey);
    if (!foreignKeyProperty.referencedEntityType().equals(foreignKeyModel.entityType())) {
      throw new IllegalArgumentException("EntityComboBoxModel is of type: " + foreignKeyModel.entityType()
              + ", should be: " + foreignKeyProperty.referencedEntityType());
    }
    //if foreign key filter entities have been set previously, initialize with one of those
    Collection<Entity> filterEntities = getForeignKeyFilterEntities(foreignKey);
    if (!Util.nullOrEmpty(filterEntities)) {
      foreignKeyModel.setSelectedItem(filterEntities.iterator().next());
    }
    if (filter) {
      linkFilter(foreignKey, foreignKeyModel);
    }
    else {
      linkCondition(foreignKey, foreignKeyModel);
    }
    addSelectionListener(selected -> {
      if (selected != null) {
        foreignKeyModel.setSelectedEntityByKey(selected.getReferencedKey(foreignKey));
      }
    });
    addRefreshListener(foreignKeyModel::forceRefresh);
  }

  private void linkFilter(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
    Predicate<Entity> filterAllCondition = item -> false;
    if (strictForeignKeyFiltering) {
      setIncludeCondition(filterAllCondition);
    }
    foreignKeyModel.addSelectionListener(selected -> {
      if (selected == null && isStrictForeignKeyFiltering()) {
        setIncludeCondition(filterAllCondition);
      }
      else {
        setForeignKeyFilterEntities(foreignKey, selected == null ? emptyList() : singletonList(selected));
      }
    });
  }

  private void linkCondition(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
    EventDataListener<Entity> listener = selected -> {
      setSelectConditionSupplier(() -> where(foreignKey).equalTo(selected));
      refresh();
    };
    foreignKeyModel.addSelectionListener(listener);
    //initialize
    listener.onEvent(selectedValue());
  }

  private void addEditEventListeners() {
    EntityEditEvents.addInsertListener(entityType, insertListener);
    EntityEditEvents.addUpdateListener(entityType, updateListener);
    EntityEditEvents.addDeleteListener(entityType, deleteListener);
  }

  private void removeEditEventListeners() {
    EntityEditEvents.removeInsertListener(entityType, insertListener);
    EntityEditEvents.removeUpdateListener(entityType, updateListener);
    EntityEditEvents.removeDeleteListener(entityType, deleteListener);
  }

  private final class InsertListener implements EventDataListener<List<Entity>> {

    @Override
    public void onEvent(List<Entity> inserted) {
      inserted.forEach(SwingEntityComboBoxModel.this::addItem);
    }
  }

  private final class UpdateListener implements EventDataListener<Map<Key, Entity>> {

    @Override
    public void onEvent(Map<Key, Entity> updated) {
      updated.forEach((key, entity) -> replaceItem(Entity.entity(key), entity));
    }
  }

  private final class DeleteListener implements EventDataListener<List<Entity>> {

    @Override
    public void onEvent(List<Entity> deleted) {
      deleted.forEach(SwingEntityComboBoxModel.this::removeItem);
    }
  }

  private final class ForeignKeyIncludeCondition implements Predicate<Entity> {

    @Override
    public boolean test(Entity item) {
      for (Map.Entry<ForeignKey, Set<Entity>> entry : foreignKeyFilterEntities.entrySet()) {
        Entity foreignKeyValue = item.getForeignKey(entry.getKey());
        if (foreignKeyValue == null) {
          return !strictForeignKeyFiltering;
        }
        if (!entry.getValue().contains(foreignKeyValue)) {
          return false;
        }
      }

      return true;
    }
  }

  private static final class EntityFinder<T> implements Finder<Entity, T> {

    private final Attribute<T> attribute;

    private EntityFinder(Attribute<T> attribute) {
      this.attribute = attribute;
    }

    @Override
    public T getValue(Entity item) {
      return item.get(attribute);
    }

    @Override
    public Predicate<Entity> createPredicate(T value) {
      return entity -> Objects.equals(entity.get(attribute), value);
    }
  }
}