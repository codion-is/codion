/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link SwingEntityModel.Builder} implementation.
 */
final class SwingEntityModelBuilder implements SwingEntityModel.Builder {

  private static final Logger LOG = LoggerFactory.getLogger(SwingEntityModelBuilder.class);

  private static final String CONNECTION_PROVIDER_PARAMETER = "connectionProvider";

  private final EntityType entityType;

  private final List<SwingEntityModel.Builder> detailModelBuilders = new ArrayList<>();

  private Class<? extends SwingEntityModel> modelClass;
  private Class<? extends SwingEntityEditModel> editModelClass;
  private Class<? extends SwingEntityTableModel> tableModelClass;

  private ModelBuilder modelBuilder;
  private EditModelBuilder editModelBuilder;
  private TableModelBuilder tableModelBuilder;

  private Consumer<SwingEntityModel> onBuildModel = new EmptyOnBuild<>();
  private Consumer<SwingEntityEditModel> onBuildEditModel = new EmptyOnBuild<>();
  private Consumer<SwingEntityTableModel> onBuildTableModel = new EmptyOnBuild<>();

  /**
   * Instantiates a new SwingeEntityModel.Builder based on the given entityType
   * @param entityType the entityType
   */
  SwingEntityModelBuilder(EntityType entityType) {
    this.entityType = requireNonNull(entityType, "entityType");
  }

  @Override
  public EntityType getEntityType() {
    return entityType;
  }

  @Override
  public SwingEntityModel.Builder modelClass(Class<? extends SwingEntityModel> modelClass) {
    if (editModelClass != null || tableModelClass != null) {
      throw new IllegalStateException("Edit or table model class has been set");
    }
    this.modelClass = requireNonNull(modelClass, "modelClass");
    return this;
  }

  @Override
  public SwingEntityModel.Builder editModelClass(Class<? extends SwingEntityEditModel> editModelClass) {
    if (modelClass != null) {
      throw new IllegalStateException("Model class has been set");
    }
    if (tableModelClass != null) {
      throw new IllegalStateException("TableModel class has been set");
    }
    this.editModelClass = requireNonNull(editModelClass, "editModelClass");
    return this;
  }

  @Override
  public SwingEntityModel.Builder tableModelClass(Class<? extends SwingEntityTableModel> tableModelClass) {
    if (modelClass != null) {
      throw new IllegalStateException("Model class has been set");
    }
    if (editModelClass != null) {
      throw new IllegalStateException("EditModel class has been set");
    }
    this.tableModelClass = requireNonNull(tableModelClass, "tableModelClass");
    return this;
  }

  @Override
  public SwingEntityModel.Builder modelBuilder(ModelBuilder modelBuilder) {
    this.modelBuilder = requireNonNull(modelBuilder);
    return this;
  }

  @Override
  public SwingEntityModel.Builder editModelBuilder(EditModelBuilder editModelBuilder) {
    this.editModelBuilder = requireNonNull(editModelBuilder);
    return this;
  }

  @Override
  public SwingEntityModel.Builder tableModelBuilder(TableModelBuilder tableModelBuilder) {
    this.tableModelBuilder = requireNonNull(tableModelBuilder);
    return this;
  }

  @Override
  public SwingEntityModel.Builder onBuildModel(Consumer<SwingEntityModel> onBuildModel) {
    this.onBuildModel = requireNonNull(onBuildModel);
    return this;
  }

  @Override
  public SwingEntityModel.Builder onBuildEditModel(Consumer<SwingEntityEditModel> onBuildEditModel) {
    this.onBuildEditModel = requireNonNull(onBuildEditModel);
    return this;
  }

  @Override
  public SwingEntityModel.Builder onBuildTableModel(Consumer<SwingEntityTableModel> onBuildTableModel) {
    this.onBuildTableModel = requireNonNull(onBuildTableModel);
    return this;
  }

  @Override
  public SwingEntityModel.Builder detailModelBuilder(SwingEntityModel.Builder detailModelBuilder) {
    requireNonNull(detailModelBuilder, "detailModelBuilder");
    if (!detailModelBuilders.contains(detailModelBuilder)) {
      detailModelBuilders.add(detailModelBuilder);
    }

    return this;
  }

  @Override
  public Class<? extends SwingEntityModel> getModelClass() {
    return modelClass == null ? SwingEntityModel.class : modelClass;
  }

  @Override
  public Class<? extends SwingEntityEditModel> getEditModelClass() {
    return editModelClass ==  null ? SwingEntityEditModel.class : editModelClass;
  }

  @Override
  public Class<? extends SwingEntityTableModel> getTableModelClass() {
    return tableModelClass == null ? SwingEntityTableModel.class : tableModelClass;
  }

  @Override
  public SwingEntityModel buildModel(EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      SwingEntityModel model;
      if (modelBuilder != null) {
        LOG.debug("{} modelBuilder initializing entity model", this);
        model = modelBuilder.build(connectionProvider);
      }
      else if (getModelClass().equals(SwingEntityModel.class)) {
        LOG.debug("{} initializing a default entity model", this);
        model = buildDefaultModel(connectionProvider);
      }
      else {
        LOG.debug("{} initializing a custom entity model: {}", this, getModelClass());
        model = getModelClass().getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      for (SwingEntityModel.Builder detailProvider : detailModelBuilders) {
        model.addDetailModel(detailProvider.buildModel(connectionProvider));
      }
      onBuildModel.accept(model);

      return model;
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SwingEntityEditModel buildEditModel(EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      SwingEntityEditModel editModel;
      if (editModelBuilder != null) {
        LOG.debug("{} editModelBuilder initializing edit model", this);
        editModel = editModelBuilder.build(connectionProvider);
      }
      else if (getEditModelClass().equals(SwingEntityEditModel.class)) {
        LOG.debug("{} initializing a default edit model", this);
        editModel = new SwingEntityEditModel(entityType, connectionProvider);
      }
      else {
        LOG.debug("{} initializing a custom edit model: {}", this, getEditModelClass());
        editModel = getEditModelClass().getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      onBuildEditModel.accept(editModel);

      return editModel;
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SwingEntityTableModel buildTableModel(EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, CONNECTION_PROVIDER_PARAMETER);
    try {
      SwingEntityTableModel tableModel;
      if (tableModelBuilder != null) {
        LOG.debug("{} tableModelBuilder initializing table model", this);
        tableModel = tableModelBuilder.build(connectionProvider);
      }
      else if (getTableModelClass().equals(SwingEntityTableModel.class)) {
        LOG.debug("{} initializing a default table model", this);
        tableModel = new SwingEntityTableModel(buildEditModel(connectionProvider));
      }
      else {
        LOG.debug("{} initializing a custom table model: {}", this, getTableModelClass());
        tableModel = getTableModelClass().getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      onBuildTableModel.accept(tableModel);

      return tableModel;
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SwingEntityModelBuilder) {
      SwingEntityModelBuilder that = (SwingEntityModelBuilder) obj;

      return Objects.equals(entityType, that.entityType) &&
              Objects.equals(modelClass, that.modelClass) &&
              Objects.equals(editModelClass, that.editModelClass) &&
              Objects.equals(tableModelClass, that.tableModelClass);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType, modelClass, editModelClass, tableModelClass);
  }

  private SwingEntityModel buildDefaultModel(EntityConnectionProvider connectionProvider) {
    return new SwingEntityModel(buildTableModel(connectionProvider));
  }

  private static final class EmptyOnBuild<T> implements Consumer<T> {
    @Override
    public void accept(T panel) {/*Do nothing*/}
  }
}