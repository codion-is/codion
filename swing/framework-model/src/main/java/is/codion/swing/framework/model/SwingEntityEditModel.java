/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.proxy.ProxyBuilder;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.model.AbstractEntityEditModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A Swing implementation of {@link EntityEditModel}.
 */
public class SwingEntityEditModel extends AbstractEntityEditModel {

  private final State.Combination refreshingObserver = State.or();
  private final Map<Attribute<?>, FilteredComboBoxModel<?>> comboBoxModels = new HashMap<>();

  /**
   * Instantiates a new {@link SwingEntityEditModel} based on the given entity type.
   * @param entityType the type of the entity to base this {@link SwingEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public SwingEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this(entityType, requireNonNull(connectionProvider), connectionProvider.entities().definition(entityType).validator());
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
   * @see #createComboBoxModel(Column)
   * @see #createForeignKeyComboBoxModel(ForeignKey)
   */
  public final void initializeComboBoxModels(Attribute<?>... attributes) {
    requireNonNull(attributes);
    for (Attribute<?> attribute : attributes) {
      if (attribute instanceof ForeignKey) {
        foreignKeyComboBoxModel((ForeignKey) attribute).refresh();
      }
      else if (attribute instanceof Column<?>) {
        comboBoxModel((Column<?>) attribute).refresh();
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
   * Returns the {@link EntityComboBoxModel} for the given foreign key attribute. If one does not exist one is created.
   * @param foreignKey the foreign key attribute
   * @return a {@link EntityComboBoxModel} based on the entity referenced by the given foreign key attribute
   * @see #createForeignKeyComboBoxModel(ForeignKey)
   */
  public final EntityComboBoxModel foreignKeyComboBoxModel(ForeignKey foreignKey) {
    entityDefinition().foreignKeys().definition(foreignKey);
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
   * Returns the {@link FilteredComboBoxModel} for the given column. If one does not exist one is created.
   * @param column the column
   * @param <T> the value type
   * @return a {@link FilteredComboBoxModel} for the given column
   * @see #createComboBoxModel(Column)
   */
  public final <T> FilteredComboBoxModel<T> comboBoxModel(Column<T> column) {
    entityDefinition().columns().definition(column);
    synchronized (comboBoxModels) {
      // can't use computeIfAbsent here, see foreignKeyComboBoxModel() comment
      FilteredComboBoxModel<T> comboBoxModel = (FilteredComboBoxModel<T>) comboBoxModels.get(column);
      if (comboBoxModel == null) {
        comboBoxModel = createAndInitializeColumnComboBoxModel(column);
        comboBoxModels.put(column, comboBoxModel);
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
   * Creates a {@link EntityComboBoxModel} for the given foreign key, override to
   * provide a custom {@link EntityComboBoxModel} implementation.
   * This method is called when creating a foreign key {@link EntityComboBoxModel} for the edit
   * fields used when editing a single record.
   * This default implementation returns a sorted {@link EntityComboBoxModel} with the default
   * nullValueCaption if the underlying attribute is nullable.
   * If the foreign key has select attributes defined, those are set in the combo box model.
   * @param foreignKey the foreign key for which to create a {@link EntityComboBoxModel}
   * @return a {@link EntityComboBoxModel} for the given foreign key
   * @see FilteredComboBoxModel#COMBO_BOX_NULL_CAPTION
   * @see AttributeDefinition#nullable()
   * @see EntityComboBoxModel#attributes()
   * @see ForeignKeyDefinition#attributes()
   */
  public EntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey) {
    ForeignKeyDefinition foreignKeyDefinition = entityDefinition().foreignKeys().definition(foreignKey);
    EntityComboBoxModel model = new EntityComboBoxModel(foreignKey.referencedType(), connectionProvider());
    model.attributes().set(foreignKeyDefinition.attributes());
    if (nullable(foreignKey)) {
      model.setNullCaption(FilteredComboBoxModel.COMBO_BOX_NULL_CAPTION.get());
    }

    return model;
  }

  /**
   * Creates a combo box model containing the current values of the given column.
   * This default implementation returns a sorted {@link FilteredComboBoxModel} with the default nullValueItem
   * if the underlying column is nullable
   * @param column the column
   * @param <T> the value type
   * @return a combo box model based on the given column
   */
  public <T> FilteredComboBoxModel<T> createComboBoxModel(Column<T> column) {
    requireNonNull(column, "column");
    FilteredComboBoxModel<T> model = createColumnComboBoxModel(column);
    if (nullable(column)) {
      model.includeNull().set(true);
      if (column.type().valueClass().isInterface()) {
        model.nullItem().set(ProxyBuilder.builder(column.type().valueClass())
                .method("toString", parameters -> FilteredComboBoxModel.COMBO_BOX_NULL_CAPTION.get())
                .build());
      }
    }
    addInsertUpdateOrDeleteListener(model::refresh);

    return model;
  }

  @Override
  public final void add(ForeignKey foreignKey, Collection<Entity> entities) {
    requireNonNull(foreignKey);
    requireNonNull(entities);
    if (containsComboBoxModel(foreignKey)) {
      EntityComboBoxModel comboBoxModel = foreignKeyComboBoxModel(foreignKey);
      for (Entity inserted : entities) {
        comboBoxModel.add(inserted);
      }
    }
  }

  @Override
  public final void remove(ForeignKey foreignKey, Collection<Entity> entities) {
    requireNonNull(foreignKey);
    requireNonNull(entities);
    clearForeignKeyReferences(foreignKey, entities);
    if (containsComboBoxModel(foreignKey)) {
      EntityComboBoxModel comboBoxModel = foreignKeyComboBoxModel(foreignKey);
      Entity selectedEntity = comboBoxModel.selectedValue();
      entities.forEach(comboBoxModel::remove);
      if (comboBoxModel.visible(selectedEntity)) {
        comboBoxModel.setSelectedItem(selectedEntity);
      }//if the null value is selected we're fine, otherwise select topmost item
      else if (!comboBoxModel.nullSelected() && comboBoxModel.getSize() > 0) {
        comboBoxModel.setSelectedItem(comboBoxModel.getElementAt(0));
      }
      else {
        comboBoxModel.setSelectedItem(null);
      }
    }
  }

  @Override
  public final StateObserver refreshing() {
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
      entities.forEach(foreignKeyValue -> comboBoxModel.replace(foreignKeyValue, foreignKeyValue));
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
    refreshingObserver.add(comboBoxModel.refresher().observer());

    return comboBoxModel;
  }

  private <T> FilteredComboBoxModel<T> createAndInitializeColumnComboBoxModel(Column<T> column) {
    FilteredComboBoxModel<T> comboBoxModel = createComboBoxModel(column);
    refreshingObserver.add(comboBoxModel.refresher().observer());

    return comboBoxModel;
  }

  private <T> FilteredComboBoxModel<T> createColumnComboBoxModel(Column<T> column) {
    FilteredComboBoxModel<T> model = new FilteredComboBoxModel<>();
    model.refresher().itemSupplier().set(column.type().isEnum() ?
            new EnumAttributeItemSupplier<>(column) :
            new ColumnItemSupplier<>(connectionProvider(), column));

    return model;
  }

  private static final class EnumAttributeItemSupplier<T> implements Supplier<Collection<T>> {

    private final Collection<T> items;

    private EnumAttributeItemSupplier(Column<T> column) {
      items = asList(column.type().valueClass().getEnumConstants());
    }

    @Override
    public Collection<T> get() {
      return items;
    }
  }

  private static final class ColumnItemSupplier<T> implements Supplier<Collection<T>> {

    private final EntityConnectionProvider connectionProvider;
    private final Column<T> column;

    private ColumnItemSupplier(EntityConnectionProvider connectionProvider, Column<T> column) {
      this.connectionProvider = connectionProvider;
      this.column = column;
    }

    @Override
    public Collection<T> get() {
      try {
        return connectionProvider.connection().select(column);
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
