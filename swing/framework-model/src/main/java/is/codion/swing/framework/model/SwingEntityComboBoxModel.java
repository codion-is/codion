/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.Util;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.Events;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityId;
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

import static is.codion.framework.db.condition.Conditions.selectCondition;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.
 */
public class SwingEntityComboBoxModel extends SwingFilteredComboBoxModel<Entity> implements EntityComboBoxModel {

  private final Event<?> refreshDoneEvent = Events.event();

  /**
   * the id of the underlying entity
   */
  private final EntityId entityId;

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
  private final Map<Attribute<Entity>, Set<Entity>> foreignKeyFilterEntities = new HashMap<>();

  private boolean strictForeignKeyFiltering = true;

  private boolean listenToEditEvents = true;

  //we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
  private final EventDataListener<List<Entity>> insertListener = new InsertListener();
  private final EventDataListener<Map<Entity.Key, Entity>> updateListener = new UpdateListener();
  private final EventDataListener<List<Entity>> deleteListener = new DeleteListener();

  private final Predicate<Entity> foreignKeyIncludeCondition = item -> {
    for (final Map.Entry<Attribute<Entity>, Set<Entity>> entry : foreignKeyFilterEntities.entrySet()) {
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
   * @param entityId the id of the entity this combo box model should represent
   * @param connectionProvider a EntityConnectionProvider instance
   */
  public SwingEntityComboBoxModel(final EntityId entityId, final EntityConnectionProvider connectionProvider) {
    requireNonNull(entityId, "entityId");
    requireNonNull(connectionProvider, "connectionProvider");
    this.entityId = entityId;
    this.connectionProvider = connectionProvider;
    this.entities = connectionProvider.getEntities();
    setStaticData(this.entities.getDefinition(entityId).isStaticData());
    setIncludeCondition(foreignKeyIncludeCondition);
    addEditEventListeners();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + " [entityId: " + entityId + "]";
  }

  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public final EntityId getEntityId() {
    return entityId;
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
  public boolean isListenToEditEvents() {
    return listenToEditEvents;
  }

  @Override
  public void setListenToEditEvents(final boolean listenToEditEvents) {
    this.listenToEditEvents = listenToEditEvents;
    if (listenToEditEvents) {
      addEditEventListeners();
    }
    else {
      removeEditEventListeners();
    }
  }

  @Override
  public final Entity getEntity(final Entity.Key primaryKey) {
    return getItems().stream().filter(entity -> entity != null && entity.getKey().equals(primaryKey)).findFirst().orElse(null);
  }

  @Override
  public final void setSelectedEntityByKey(final Entity.Key key) {
    requireNonNull(key, "key");
    final int indexOfKey = getIndexOfKey(key);
    if (indexOfKey >= 0) {
      setSelectedItem(getElementAt(indexOfKey));
    }
    else {
      final int filteredIndexOfKey = getFilteredIndexOfKey(key);
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
  public final void setForeignKeyFilterEntities(final Attribute<Entity> foreignKeyAttribute, final Collection<Entity> entities) {
    if (Util.nullOrEmpty(entities)) {
      foreignKeyFilterEntities.remove(foreignKeyAttribute);
    }
    else {
      foreignKeyFilterEntities.put(foreignKeyAttribute, new HashSet<>(entities));
    }
    setIncludeCondition(foreignKeyIncludeCondition);
  }

  @Override
  public final Collection<Entity> getForeignKeyFilterEntities(final Attribute<Entity> foreignKeyAttribute) {
    final Collection<Entity> filterEntities = new ArrayList<>();
    if (foreignKeyFilterEntities.containsKey(foreignKeyAttribute)) {
      filterEntities.addAll(foreignKeyFilterEntities.get(foreignKeyAttribute));
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
  public final SwingEntityComboBoxModel createForeignKeyFilterComboBoxModel(final Attribute<Entity> foreignKeyAttribute) {
    final ForeignKeyProperty foreignKeyProperty = entities.getDefinition(entityId).getForeignKeyProperty(foreignKeyAttribute);
    final SwingEntityComboBoxModel foreignKeyModel =
            new SwingEntityComboBoxModel(foreignKeyProperty.getForeignEntityId(), connectionProvider);
    foreignKeyModel.setNullValue(entities.createToStringEntity(foreignKeyProperty.getForeignEntityId(), "-"));
    foreignKeyModel.refresh();
    linkForeignKeyComboBoxModel(foreignKeyAttribute, foreignKeyModel);

    return foreignKeyModel;
  }

  @Override
  public final void linkForeignKeyComboBoxModel(final Attribute<Entity> foreignKeyAttribute, final EntityComboBoxModel foreignKeyModel) {
    final ForeignKeyProperty foreignKeyProperty = entities.getDefinition(entityId).getForeignKeyProperty(foreignKeyAttribute);
    if (!foreignKeyProperty.getForeignEntityId().equals(foreignKeyModel.getEntityId())) {
      throw new IllegalArgumentException("Foreign key ComboBoxModel is of type: " + foreignKeyModel.getEntityId()
              + ", should be: " + foreignKeyProperty.getForeignEntityId());
    }
    final Collection<Entity> filterEntities = getForeignKeyFilterEntities(foreignKeyAttribute);
    if (!Util.nullOrEmpty(filterEntities)) {
      foreignKeyModel.setSelectedItem(filterEntities.iterator().next());
    }
    final Predicate<Entity> filterAllCondition = item -> false;
    if (isStrictForeignKeyFiltering()) {
      setIncludeCondition(filterAllCondition);
    }
    foreignKeyModel.addSelectionListener(selected -> {
      if (selected == null && isStrictForeignKeyFiltering()) {
        setIncludeCondition(filterAllCondition);
      }
      else {
        setForeignKeyFilterEntities(foreignKeyAttribute, selected == null ? emptyList() : singletonList(selected));
      }
    });
    addSelectionListener(selected -> {
      if (selected != null) {
        foreignKeyModel.setSelectedEntityByKey(selected.getReferencedKey(foreignKeyAttribute));
      }
    });
    addRefreshListener(new ForeignKeyModelRefreshListener(foreignKeyModel));
  }

  @Override
  public final Value<Integer> integerValueSelector(final Attribute<Integer> attribute) {
    return integerValueSelector(attribute, (theEntities, theAttribute, value) ->
            theEntities.stream().filter(entity ->
                    Objects.equals(value, entity.get(attribute))).findFirst().orElse(null));
  }

  @Override
  public final Value<Integer> integerValueSelector(final Attribute<Integer> attribute, final Finder<Integer> finder) {
    return new SelectorValue<>(attribute, finder);
  }

  @Override
  public final void addRefreshListener(final EventListener listener) {
    refreshDoneEvent.addListener(listener);
  }

  @Override
  public final void removeRefreshListener(final EventListener listener) {
    refreshDoneEvent.removeListener(listener);
  }

  @Override
  protected final Entity translateSelectionItem(final Object item) {
    if (item == null) {
      return null;
    }

    if (item instanceof Entity) {
      final int indexOfKey = getIndexOfKey(((Entity) item).getKey());
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
  protected final List<Entity> initializeContents() {
    try {
      if (staticData && !isCleared() && !forceRefresh) {
        return super.initializeContents();
      }

      return performQuery();
    }
    finally {
      refreshDoneEvent.onEvent();
    }
  }

  /**
   * Retrieves the entities to present in this EntityComboBoxModel, taking into account
   * the {@link #getSelectConditionProvider()} specified for this model
   * @return the entities to present in this EntityComboBoxModel
   * @see #getSelectConditionProvider()
   */
  protected List<Entity> performQuery() {
    try {
      return connectionProvider.getConnection().select(selectCondition(entityId,
              selectConditionProvider == null ? null : selectConditionProvider.getCondition()));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private int getIndexOfKey(final Entity.Key primaryKey) {
    final int size = getSize();
    for (int index = 0; index < size; index++) {
      final Entity item = getElementAt(index);
      if (item != null && item.getKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private int getFilteredIndexOfKey(final Entity.Key primaryKey) {
    final List<Entity> filteredItems = getFilteredItems();
    for (int index = 0; index < filteredItems.size(); index++) {
      final Entity item = filteredItems.get(index);
      if (item.getKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private void addEditEventListeners() {
    EntityEditEvents.addInsertListener(entityId, insertListener);
    EntityEditEvents.addUpdateListener(entityId, updateListener);
    EntityEditEvents.addDeleteListener(entityId, deleteListener);
  }

  private void removeEditEventListeners() {
    EntityEditEvents.removeInsertListener(entityId, insertListener);
    EntityEditEvents.removeUpdateListener(entityId, updateListener);
    EntityEditEvents.removeDeleteListener(entityId, deleteListener);
  }

  private final class InsertListener implements EventDataListener<List<Entity>> {

    @Override
    public void onEvent(final List<Entity> inserted) {
      inserted.forEach(SwingEntityComboBoxModel.this::addItem);
    }
  }

  private final class UpdateListener implements EventDataListener<Map<Entity.Key, Entity>> {

    @Override
    public void onEvent(final Map<Entity.Key, Entity> updated) {
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

  private static final class ForeignKeyModelRefreshListener implements EventListener {

    private final EntityComboBoxModel foreignKeyModel;

    private ForeignKeyModelRefreshListener(final EntityComboBoxModel foreignKeyModel) {
      this.foreignKeyModel = foreignKeyModel;
    }

    @Override
    public void onEvent() {
      foreignKeyModel.forceRefresh();
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
      this.attribute = requireNonNull(attribute);
      this.finder = requireNonNull(finder);
      addSelectionListener(selected -> notifyValueChange());
    }

    /**
     * Selects the first entity found in the underlying combo box model, which
     * has the given value associated with the underlying attribute.
     * @param value the value
     */
    @Override
    public void set(final T value) {
      setSelectedItem(value == null ? null : finder.findByValue(getVisibleItems(), attribute, value));
    }

    /**
     * @return the value of the underlying attribute in the selected Entity, null if the selection is empty
     */
    @Override
    public T get() {
      if (isSelectionEmpty()) {
        return null;
      }

      return (T) getSelectedValue().get(attribute);
    }

    /**
     * @return true
     */
    @Override
    public boolean isNullable() {
      return true;
    }
  }
}