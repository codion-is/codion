/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.state.State;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link ForeignKeyDetailModelLink} implementation.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 */
public class DefaultForeignKeyDetailModelLink<M extends DefaultEntityModel<M, E, T>, E extends AbstractEntityEditModel,
        T extends EntityTableModel<E>> extends DefaultDetailModelLink<M, E, T> implements ForeignKeyDetailModelLink<M, E, T> {

  private final ForeignKey foreignKey;
  private final State clearForeignKeyOnEmptySelection = State.state(ForeignKeyDetailModelLink.CLEAR_FOREIGN_KEY_ON_EMPTY_SELECTION.get());
  private final State searchByInsertedEntity = State.state(ForeignKeyDetailModelLink.SEARCH_BY_INSERTED_ENTITY.get());
  private final State refreshOnSelection = State.state(ForeignKeyDetailModelLink.REFRESH_ON_SELECTION.get());

  /**
   * @param detailModel the detail model
   * @param foreignKey the foreign key to base this link on
   */
  public DefaultForeignKeyDetailModelLink(M detailModel, ForeignKey foreignKey) {
    super(detailModel);
    this.foreignKey = requireNonNull(foreignKey, "foreignKey");
  }

  @Override
  public final ForeignKey foreignKey() {
    return foreignKey;
  }

  @Override
  public final State searchByInsertedEntity() {
    return searchByInsertedEntity;
  }

  @Override
  public final State refreshOnSelection() {
    return refreshOnSelection;
  }

  @Override
  public final State clearForeignKeyOnEmptySelection() {
    return clearForeignKeyOnEmptySelection;
  }

  @Override
  public void onSelection(Collection<Entity> selectedEntities) {
    if (detailModel().containsTableModel() && detailModel().tableModel()
            .conditionModel().setEqualConditionValues(foreignKey, selectedEntities) && refreshOnSelection.get()) {
      detailModel().tableModel().refresher().refreshThen(items -> setEditModelForeignKeyValue(selectedEntities));
    }
    else {
      setEditModelForeignKeyValue(selectedEntities);
    }
  }

  @Override
  public void onInsert(Collection<Entity> insertedEntities) {
    Collection<Entity> entities = ofReferencedType(insertedEntities);
    detailModel().editModel().add(foreignKey, entities);
    if (!entities.isEmpty()) {
      detailModel().editModel().put(foreignKey, entities.iterator().next());
    }
    if (detailModel().containsTableModel() && searchByInsertedEntity.get()
            && detailModel().tableModel().conditionModel().setEqualConditionValues(foreignKey, entities)) {
      detailModel().tableModel().refresh();
    }
  }

  @Override
  public void onUpdate(Map<Entity.Key, Entity> updatedEntities) {
    Collection<Entity> entities = ofReferencedType(updatedEntities.values());
    detailModel().editModel().replace(foreignKey, entities);
    if (detailModel().containsTableModel()) {
      detailModel().tableModel().replace(foreignKey, entities);
    }
  }

  @Override
  public void onDelete(Collection<Entity> deletedEntities) {
    detailModel().editModel().remove(foreignKey, ofReferencedType(deletedEntities));
  }

  private Collection<Entity> ofReferencedType(Collection<Entity> entities) {
    return entities.stream()
            .filter(entity -> entity.entityType().equals(foreignKey.referencedType()))
            .collect(toList());
  }

  private void setEditModelForeignKeyValue(Collection<Entity> selectedEntities) {
    Entity foreignKeyValue = selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
    if (detailModel().editModel().exists().not().get() && (foreignKeyValue != null || clearForeignKeyOnEmptySelection.get())) {
      detailModel().editModel().put(foreignKey, foreignKeyValue);
    }
  }
}
