/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.Util;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.framework.model.EntityEditEvents;
import is.codion.swing.common.model.combobox.SwingFilteredComboBoxModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static is.codion.framework.db.condition.Conditions.condition;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.
 */
public class SwingEntityComboBoxModel extends SwingFilteredComboBoxModel<Entity> implements EntityComboBoxModel {

  /**
   * the id of the underlying entity
   */
  private final EntityType<?> entityType;

  /**
   * the EntityConnectionProvider instance used by this EntityComboBoxModel
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * The domain model entities
   */
  private final Entities entities;

  /**
   * true if the data should only be fetched once, unless {@code forceRefresh()} is called
   */
  private boolean staticData = false;

  /**
   * used to indicate that a refresh is being forced, as in, overriding the staticData directive
   */
  private boolean forceRefresh = false;

  /**
   * the Condition.Provider used when querying
   */
  private Condition.Provider selectConditionProvider;

  /**
   * A map of entities used to filter the contents of this model by foreign key value.
   * The key in the map is the attribute of the foreign key property.
   */
  private final Map<ForeignKey, Set<Entity>> foreignKeyFilterEntities = new HashMap<>();

  private boolean strictForeignKeyFiltering = true;

  private boolean listenToEditEvents = true;

  //we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
  private final EventDataListener<List<Entity>> insertListener = new InsertListener();
  private final EventDataListener<Map<Key, Entity>> updateListener = new UpdateListener();
  private final EventDataListener<List<Entity>> deleteListener = new DeleteListener();

  private final Predicate<Entity> foreignKeyIncludeCondition = item -> {
    for (final Map.Entry<ForeignKey, Set<Entity>> entry : foreignKeyFilterEntities.entrySet()) {
      final Entity foreignKeyValue = item.getForeignKey(entry.getKey());
      if (foreignKeyValue == null) {
        return !strictForeignKeyFiltering;
      }
      if (!entry.getValue().contains(foreignKeyValue)) {
        return false;
      }
    }

    return true;
  };

  /**
   * @param entityType the type of the entity this combo box model should represent
   * @param connectionProvider a EntityConnectionProvider instance
   */
  public SwingEntityComboBoxModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider) {
    requireNonNull(entityType, "entityType");
    requireNonNull(connectionProvider, "connectionProvider");
    this.entityType = entityType;
    this.connectionProvider = connectionProvider;
    this.entities = connectionProvider.getEntities();
    setStaticData(this.entities.getDefinition(entityType).isStaticData());
    setIncludeCondition(foreignKeyIncludeCondition);
    addEditEventListeners();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + " [entityType: " + entityType + "]";
  }

  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public final EntityType<?> getEntityType() {
    return entityType;
  }

  @Override
  public final void forceRefresh() {
    try {
      forceRefresh = true;
      refresh();
    }
    finally {
      forceRefresh = false;
    }
  }

  @Override
  public final boolean isStaticData() {
    return staticData;
  }

  @Override
  public final void setStaticData(final boolean staticData) {
    this.staticData = staticData;
  }

  @Override
  public final boolean isListenToEditEvents() {
    return listenToEditEvents;
  }

  @Override
  public final void setListenToEditEvents(final boolean listenToEditEvents) {
    this.listenToEditEvents = listenToEditEvents;
    if (listenToEditEvents) {
      addEditEventListeners();
    }
    else {
      removeEditEventListeners();
    }
  }

  @Override
  public final Entity getEntity(final Key primaryKey) {
    return getItems().stream().filter(entity -> entity != null && entity.getPrimaryKey().equals(primaryKey)).findFirst().orElse(null);
  }

  @Override
  public final void setSelectedEntityByKey(final Key primaryKey) {
    requireNonNull(primaryKey, "primaryKey");
    final int indexOfKey = getIndexOfKey(primaryKey);
    if (indexOfKey >= 0) {
      setSelectedItem(getElementAt(indexOfKey));
    }
    else {
      final int filteredIndexOfKey = getFilteredIndexOfKey(primaryKey);
      if (filteredIndexOfKey >= 0) {
        setSelectedItem(getFilteredItems().get(filteredIndexOfKey));
      }
    }
  }

  @Override
  public final void setSelectConditionProvider(final Condition.Provider selectConditionProvider) {
    this.selectConditionProvider = selectConditionProvider;
  }

  @Override
  public final Condition.Provider getSelectConditionProvider() {
    return this.selectConditionProvider;
  }

  @Override
  public final Predicate<Entity> getForeignKeyIncludeCondition() {
    return foreignKeyIncludeCondition;
  }

  @Override
  public final void setForeignKeyFilterEntities(final ForeignKey foreignKey, final Collection<Entity> entities) {
    if (Util.nullOrEmpty(entities)) {
      foreignKeyFilterEntities.remove(foreignKey);
    }
    else {
      foreignKeyFilterEntities.put(foreignKey, new HashSet<>(entities));
    }
    setIncludeCondition(foreignKeyIncludeCondition);
  }

  @Override
  public final Collection<Entity> getForeignKeyFilterEntities(final ForeignKey foreignKey) {
    final Collection<Entity> filterEntities = new ArrayList<>();
    if (foreignKeyFilterEntities.containsKey(foreignKey)) {
      filterEntities.addAll(foreignKeyFilterEntities.get(foreignKey));
    }

    return filterEntities;
  }

  @Override
  public final void setStrictForeignKeyFiltering(final boolean strictForeignKeyFiltering) {
    this.strictForeignKeyFiltering = strictForeignKeyFiltering;
  }

  @Override
  public final boolean isStrictForeignKeyFiltering() {
    return strictForeignKeyFiltering;
  }

  @Override
  public final SwingEntityComboBoxModel createForeignKeyFilterComboBoxModel(final ForeignKey foreignKey) {
    return createForeignKeyComboBoxModel(foreignKey, true);
  }

  @Override
  public final SwingEntityComboBoxModel createForeignKeyConditionComboBoxModel(final ForeignKey foreignKey) {
    return createForeignKeyComboBoxModel(foreignKey, false);
  }

  @Override
  public final void linkForeignKeyFilterComboBoxModel(final ForeignKey foreignKey, final EntityComboBoxModel foreignKeyModel) {
    linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, true);
  }

  @Override
  public final void linkForeignKeyConditionComboBoxModel(final ForeignKey foreignKey, final EntityComboBoxModel foreignKeyModel) {
    linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, false);
  }

  @Override
  public final <T> Value<T> selectorValue(final Attribute<T> attribute) {
    return selectorValue(attribute, (entities, theAttribute, value) -> entities.stream().filter(entity ->
            Objects.equals(value, entity.get(theAttribute))).findFirst().orElse(null));
  }

  @Override
  public final <T> Value<T> selectorValue(final Attribute<T> attribute, final Finder<T> finder) {
    return new SelectorValue<>(attribute, finder);
  }

  @Override
  protected final Entity translateSelectionItem(final Object item) {
    if (item == null) {
      return null;
    }

    if (item instanceof Entity) {
      final int indexOfKey = getIndexOfKey(((Entity) item).getPrimaryKey());
      if (indexOfKey >= 0) {
        return getElementAt(indexOfKey);
      }

      return (Entity) item;
    }
    final String itemToString = item.toString();

    //item not found, select null value
    return getVisibleItems().stream().filter(visibleItem ->
            visibleItem != null && itemToString.equals(visibleItem.toString())).findFirst().orElse(null);
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
   * the {@link #getSelectConditionProvider()} specified for this model
   * @return the entities to present in this EntityComboBoxModel
   * @see #getSelectConditionProvider()
   */
  protected Collection<Entity> performQuery() {
    try {
      final Condition condition;
      if (selectConditionProvider == null) {
        condition = condition(entityType);
      }
      else {
        condition = selectConditionProvider.getCondition();
      }

      return connectionProvider.getConnection().select(condition.select()
              .orderBy(connectionProvider.getEntities().getDefinition(entityType).getOrderBy()));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private int getIndexOfKey(final Key primaryKey) {
    final int size = getSize();
    final int startIndex = getNullString() != null ? 1 : 0;
    for (int index = startIndex; index < size; index++) {
      final Entity item = getElementAt(index);
      if (item != null && item.getPrimaryKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private int getFilteredIndexOfKey(final Key primaryKey) {
    final List<Entity> filteredItems = getFilteredItems();
    for (int index = 0; index < filteredItems.size(); index++) {
      final Entity item = filteredItems.get(index);
      if (item.getPrimaryKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private SwingEntityComboBoxModel createForeignKeyComboBoxModel(final ForeignKey foreignKey, final boolean filter) {
    final ForeignKeyProperty foreignKeyProperty = entities.getDefinition(entityType).getForeignKeyProperty(foreignKey);
    final SwingEntityComboBoxModel foreignKeyModel =
            new SwingEntityComboBoxModel(foreignKeyProperty.getReferencedEntityType(), connectionProvider);
    foreignKeyModel.setNullString(FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());
    foreignKeyModel.refresh();
    linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, filter);

    return foreignKeyModel;

  }

  private void linkForeignKeyComboBoxModel(final ForeignKey foreignKey, final EntityComboBoxModel foreignKeyModel,
                                           final boolean filter) {
    final ForeignKeyProperty foreignKeyProperty = entities.getDefinition(entityType).getForeignKeyProperty(foreignKey);
    if (!foreignKeyProperty.getReferencedEntityType().equals(foreignKeyModel.getEntityType())) {
      throw new IllegalArgumentException("EntityComboBoxModel is of type: " + foreignKeyModel.getEntityType()
              + ", should be: " + foreignKeyProperty.getReferencedEntityType());
    }
    //if foreign key filter entities have been set previously, initialize with one of those
    final Collection<Entity> filterEntities = getForeignKeyFilterEntities(foreignKey);
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

  private void linkFilter(final ForeignKey foreignKey, final EntityComboBoxModel foreignKeyModel) {
    final Predicate<Entity> filterAllCondition = item -> false;
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

  private void linkCondition(final ForeignKey foreignKey, final EntityComboBoxModel foreignKeyModel) {
    final EventDataListener<Entity> listener = selected -> {
      setSelectConditionProvider(() -> condition(foreignKey).equalTo(selected));
      refresh();
    };
    foreignKeyModel.addSelectionListener(listener);
    //initialize
    listener.onEvent(getSelectedValue());
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
    public void onEvent(final List<Entity> inserted) {
      inserted.forEach(SwingEntityComboBoxModel.this::addItem);
    }
  }

  private final class UpdateListener implements EventDataListener<Map<Key, Entity>> {

    @Override
    public void onEvent(final Map<Key, Entity> updated) {
      final Entities domainEntities = getConnectionProvider().getEntities();
      updated.forEach((key, entity) -> replaceItem(domainEntities.entity(key), entity));
    }
  }

  private final class DeleteListener implements EventDataListener<List<Entity>> {

    @Override
    public void onEvent(final List<Entity> deleted) {
      deleted.forEach(SwingEntityComboBoxModel.this::removeItem);
    }
  }

  private final class SelectorValue<T> extends AbstractValue<T> {

    private final Attribute<T> attribute;
    private final EntityComboBoxModel.Finder<T> finder;

    /**
     * @param attribute the attribute
     * @param finder the Finder instance responsible for finding the entity by value
     */
    private SelectorValue(final Attribute<T> attribute, final EntityComboBoxModel.Finder<T> finder) {
      if (!entities.getDefinition(getEntityType()).containsAttribute(attribute)) {
        throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + getEntityType());
      }
      this.attribute = attribute;
      this.finder = requireNonNull(finder);
      addSelectionListener(selected -> notifyValueChange());
    }

    /**
     * @return the value of the underlying attribute in the selected Entity, null if the selection is empty
     */
    @Override
    public T get() {
      if (isSelectionEmpty()) {
        return null;
      }

      return getSelectedValue().get(attribute);
    }

    /**
     * Selects the first entity found in the underlying combo box model, which
     * has the given value associated with the underlying attribute.
     * @param value the value
     */
    @Override
    protected void setValue(final T value) {
      setSelectedItem(value == null ? null : finder.findByValue(getVisibleItems(), attribute, value));
    }
  }
}