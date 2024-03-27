/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2011 - 2024, Björn Darri Sigurðsson.
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
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link SwingEntityModel.Builder} implementation.
 */
final class SwingEntityModelBuilder implements SwingEntityModel.Builder {

	private static final Logger LOG = LoggerFactory.getLogger(SwingEntityModelBuilder.class);

	private final EntityType entityType;
	private final List<SwingEntityModel.Builder> detailModelBuilders = new ArrayList<>();

	private Class<? extends SwingEntityModel> modelClass;
	private Class<? extends SwingEntityEditModel> editModelClass;
	private Class<? extends SwingEntityTableModel> tableModelClass;

	private Function<EntityConnectionProvider, SwingEntityModel> modelFactory;
	private Function<EntityConnectionProvider, SwingEntityEditModel> editModelFactory;
	private Function<EntityConnectionProvider, SwingEntityTableModel> tableModelFactory;

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
	public EntityType entityType() {
		return entityType;
	}

	@Override
	public SwingEntityModel.Builder model(Class<? extends SwingEntityModel> modelClass) {
		if (editModelClass != null || tableModelClass != null) {
			throw new IllegalStateException("Edit or table model class has been set");
		}
		this.modelClass = requireNonNull(modelClass, "modelClass");
		return this;
	}

	@Override
	public SwingEntityModel.Builder editModel(Class<? extends SwingEntityEditModel> editModelClass) {
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
	public SwingEntityModel.Builder tableModel(Class<? extends SwingEntityTableModel> tableModelClass) {
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
	public SwingEntityModel.Builder model(Function<EntityConnectionProvider, SwingEntityModel> modelFactory) {
		this.modelFactory = requireNonNull(modelFactory);
		return this;
	}

	@Override
	public SwingEntityModel.Builder editModel(Function<EntityConnectionProvider, SwingEntityEditModel> editModelFactory) {
		this.editModelFactory = requireNonNull(editModelFactory);
		return this;
	}

	@Override
	public SwingEntityModel.Builder tableModel(Function<EntityConnectionProvider, SwingEntityTableModel> tableModelFactory) {
		this.tableModelFactory = requireNonNull(tableModelFactory);
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
	public SwingEntityModel.Builder detailModel(SwingEntityModel.Builder detailModelBuilder) {
		requireNonNull(detailModelBuilder, "detailModelBuilder");
		if (!detailModelBuilders.contains(detailModelBuilder)) {
			detailModelBuilders.add(detailModelBuilder);
		}

		return this;
	}

	@Override
	public SwingEntityModel build(EntityConnectionProvider connectionProvider) {
		requireNonNull(connectionProvider, "connectionProvider");
		try {
			SwingEntityModel model;
			if (modelFactory != null) {
				LOG.debug("{} modelBuilder initializing entity model", this);
				model = modelFactory.apply(connectionProvider);
			}
			else if (modelClass().equals(SwingEntityModel.class)) {
				LOG.debug("{} initializing a default entity model", this);
				model = new SwingEntityModel(buildTableModel(connectionProvider));
			}
			else {
				LOG.debug("{} initializing a custom entity model: {}", this, modelClass());
				model = modelClass().getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
			}
			for (SwingEntityModel.Builder detailProvider : detailModelBuilders) {
				model.addDetailModel(detailProvider.build(connectionProvider));
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

	private SwingEntityEditModel buildEditModel(EntityConnectionProvider connectionProvider) {
		try {
			SwingEntityEditModel editModel;
			if (editModelFactory != null) {
				LOG.debug("{} editModelBuilder initializing edit model", this);
				editModel = editModelFactory.apply(connectionProvider);
			}
			else if (editModelClass().equals(SwingEntityEditModel.class)) {
				LOG.debug("{} initializing a default edit model", this);
				editModel = new SwingEntityEditModel(entityType, connectionProvider);
			}
			else {
				LOG.debug("{} initializing a custom edit model: {}", this, editModelClass());
				editModel = editModelClass().getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
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

	private SwingEntityTableModel buildTableModel(EntityConnectionProvider connectionProvider) {
		try {
			SwingEntityTableModel tableModel;
			if (tableModelFactory != null) {
				LOG.debug("{} tableModelBuilder initializing table model", this);
				tableModel = tableModelFactory.apply(connectionProvider);
			}
			else if (tableModelClass().equals(SwingEntityTableModel.class)) {
				LOG.debug("{} initializing a default table model", this);
				tableModel = new SwingEntityTableModel(buildEditModel(connectionProvider));
			}
			else {
				LOG.debug("{} initializing a custom table model: {}", this, tableModelClass());
				tableModel = tableModelClass().getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
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

	private Class<? extends SwingEntityModel> modelClass() {
		return modelClass == null ? SwingEntityModel.class : modelClass;
	}

	private Class<? extends SwingEntityEditModel> editModelClass() {
		return editModelClass == null ? SwingEntityEditModel.class : editModelClass;
	}

	private Class<? extends SwingEntityTableModel> tableModelClass() {
		return tableModelClass == null ? SwingEntityTableModel.class : tableModelClass;
	}

	private static final class EmptyOnBuild<T> implements Consumer<T> {
		@Override
		public void accept(T panel) {/*Do nothing*/}
	}
}