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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityPanel.DetailLayout;

import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

final class EntityPanelBuilder implements EntityPanel.Builder {

	private final EntityType entityType;
	private final SwingEntityModel.Builder modelBuilder;
	private final SwingEntityModel model;
	private final List<EntityPanel.Builder> detailPanelBuilders = new ArrayList<>();

	private String caption;
	private String description;
	private ImageIcon icon;
	private boolean refreshWhenInitialized = true;
	private Dimension preferredSize;
	private ConditionView conditionView = EntityTablePanel.Config.CONDITION_VIEW.get();
	private ConditionView filterView = EntityTablePanel.Config.FILTER_VIEW.get();
	private Function<EntityPanel, DetailLayout> detailLayout = new DefaultDetailLayout();

	private Class<? extends EntityPanel> panelClass;
	private Class<? extends EntityTablePanel> tablePanelClass;
	private Class<? extends EntityEditPanel> editPanelClass;

	private Consumer<EntityPanel> onBuildPanel = new EmptyOnBuild<>();
	private Consumer<EntityEditPanel> onBuildEditPanel = new EmptyOnBuild<>();
	private Consumer<EntityTablePanel> onBuildTablePanel = new EmptyOnBuild<>();

	EntityPanelBuilder(SwingEntityModel.Builder modelBuilder) {
		this.modelBuilder = requireNonNull(modelBuilder, "modelBuilder");
		this.entityType = modelBuilder.entityType();
		this.model = null;
	}

	EntityPanelBuilder(SwingEntityModel model) {
		this.model = requireNonNull(model, "model");
		this.entityType = model.entityType();
		this.modelBuilder = null;
	}

	@Override
	public EntityType entityType() {
		return entityType;
	}

	@Override
	public EntityPanelBuilder caption(String caption) {
		this.caption = caption;
		return this;
	}

	@Override
	public Optional<String> description() {
		return Optional.ofNullable(description);
	}

	@Override
	public EntityPanelBuilder description(String description) {
		this.description = description;
		return this;
	}

	@Override
	public Optional<String> caption() {
		return Optional.ofNullable(caption);
	}

	@Override
	public EntityPanel.Builder icon(ImageIcon icon) {
		this.icon = icon;
		return this;
	}

	@Override
	public Optional<ImageIcon> icon() {
		return Optional.ofNullable(icon);
	}

	@Override
	public EntityPanel.Builder detailPanel(EntityPanel.Builder panelBuilder) {
		if (!detailPanelBuilders.contains(panelBuilder)) {
			detailPanelBuilders.add(panelBuilder);
		}

		return this;
	}

	@Override
	public EntityPanel.Builder refreshWhenInitialized(boolean refreshWhenInitialized) {
		this.refreshWhenInitialized = refreshWhenInitialized;
		return this;
	}

	@Override
	public EntityPanel.Builder conditionView(ConditionView conditionView) {
		this.conditionView = requireNonNull(conditionView);
		return this;
	}

	@Override
	public EntityPanel.Builder filterView(ConditionView filterView) {
		this.filterView = requireNonNull(filterView);
		return this;
	}

	@Override
	public EntityPanel.Builder detailLayout(Function<EntityPanel, DetailLayout> detailLayout) {
		this.detailLayout = requireNonNull(detailLayout);
		return this;
	}

	@Override
	public EntityPanel.Builder preferredSize(Dimension preferredSize) {
		this.preferredSize = requireNonNull(preferredSize);
		return this;
	}

	@Override
	public EntityPanel.Builder panel(Class<? extends EntityPanel> panelClass) {
		if (editPanelClass != null || tablePanelClass != null) {
			throw new IllegalStateException("Edit or table panel class has been set");
		}
		this.panelClass = requireNonNull(panelClass, "panelClass");
		return this;
	}

	@Override
	public EntityPanel.Builder editPanel(Class<? extends EntityEditPanel> editPanelClass) {
		if (panelClass != null) {
			throw new IllegalStateException("Panel class has been set");
		}
		this.editPanelClass = requireNonNull(editPanelClass, "editPanelClass");
		return this;
	}

	@Override
	public EntityPanel.Builder tablePanel(Class<? extends EntityTablePanel> tablePanelClass) {
		if (panelClass != null) {
			throw new IllegalStateException("Panel class has been set");
		}
		this.tablePanelClass = requireNonNull(tablePanelClass, "tablePanelClass");
		return this;
	}

	@Override
	public EntityPanel.Builder onBuildPanel(Consumer<EntityPanel> onBuildPanel) {
		this.onBuildPanel = requireNonNull(onBuildPanel);
		return this;
	}

	@Override
	public EntityPanelBuilder onBuildEditPanel(Consumer<EntityEditPanel> onBuildEditPanel) {
		this.onBuildEditPanel = requireNonNull(onBuildEditPanel);
		return this;
	}

	@Override
	public EntityPanel.Builder onBuildTablePanel(Consumer<EntityTablePanel> onBuildTablePanel) {
		this.onBuildTablePanel = requireNonNull(onBuildTablePanel);
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof EntityPanelBuilder) {
			EntityPanelBuilder that = (EntityPanelBuilder) obj;

			return Objects.equals(modelBuilder, that.model) &&
							Objects.equals(model, that.model) &&
							Objects.equals(panelClass, that.panelClass) &&
							Objects.equals(editPanelClass, that.editPanelClass) &&
							Objects.equals(tablePanelClass, that.tablePanelClass);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(modelBuilder, model, panelClass, editPanelClass, tablePanelClass);
	}

	@Override
	public EntityPanel build(EntityConnectionProvider connectionProvider) {
		requireNonNull(connectionProvider, "connectionProvider");
		if (modelBuilder == null) {
			throw new IllegalStateException("A SwingEntityModel.Builder is not available in this panel builder: " + entityType);
		}

		return build(modelBuilder.build(connectionProvider));
	}

	@Override
	public EntityPanel build(SwingEntityModel model) {
		requireNonNull(model, "model");
		EntityPanel entityPanel = createPanel(model);
		if (entityPanel.containsTablePanel()) {
			entityPanel.tablePanel().conditions().view().set(conditionView);
			entityPanel.tablePanel().table().filters().view().set(filterView);
		}
		if (!detailPanelBuilders.isEmpty()) {
			for (EntityPanel.Builder detailPanelBuilder : detailPanelBuilders) {
				SwingEntityModel detailModel = model.detailModel(detailPanelBuilder.entityType());
				EntityPanel detailPanel = detailPanelBuilder.build(detailModel);
				entityPanel.addDetailPanel(detailPanel);
			}
		}
		onBuildPanel.accept(entityPanel);
		if (refreshWhenInitialized && model.containsTableModel()) {
			model.tableModel().refresh();
		}

		return entityPanel;
	}

	private EntityPanel createPanel(SwingEntityModel entityModel) {
		try {
			EntityPanel entityPanel;
			if (panelClass().equals(EntityPanel.class)) {
				EntityTablePanel tablePanel = entityModel.containsTableModel() ? createTablePanel(entityModel.tableModel()) : null;
				EntityEditPanel editPanel = editPanelClass() == null ? null : createEditPanel(entityModel.editModel());
				entityPanel = createPanel(entityModel, editPanel, tablePanel);
			}
			else {
				entityPanel = findModelConstructor(panelClass()).newInstance(entityModel);
			}
			if (preferredSize != null) {
				entityPanel.setPreferredSize(preferredSize);
			}

			return entityPanel;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private EntityPanel createPanel(SwingEntityModel entityModel, EntityEditPanel editPanel, EntityTablePanel tablePanel) throws Exception {
		Consumer<EntityPanel.Config> configure = config -> {
			config.detailLayout(detailLayout);
			if (caption != null) {
				config.caption(caption);
			}
			if (description != null) {
				config.description(description);
			}
			if (icon != null) {
				config.icon(icon);
			}
		};

		return panelClass().getConstructor(SwingEntityModel.class, EntityEditPanel.class, EntityTablePanel.class, Consumer.class)
						.newInstance(entityModel, editPanel, tablePanel, configure);
	}

	private EntityEditPanel createEditPanel(SwingEntityEditModel editModel) {
		if (editPanelClass == null) {
			throw new IllegalArgumentException("No edit panel class has been specified for entity panel builder: " + entityType);
		}
		if (!editModel.entityType().equals(entityType)) {
			throw new IllegalArgumentException("Entity type mismatch, editModel: " + editModel.entityType() + ", required: " + entityType);
		}
		try {
			EntityEditPanel editPanel = findEditModelConstructor(editPanelClass).newInstance(editModel);
			onBuildEditPanel.accept(editPanel);

			return editPanel;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private EntityTablePanel createTablePanel(SwingEntityTableModel tableModel) {
		try {
			if (!tableModel.entityType().equals(entityType)) {
				throw new IllegalArgumentException("Entity type mismatch, tableModel: " + tableModel.entityType() + ", required: " + entityType);
			}
			EntityTablePanel tablePanel = findTableModelConstructor(tablePanelClass()).newInstance(tableModel);
			onBuildTablePanel.accept(tablePanel);

			return tablePanel;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Class<? extends EntityPanel> panelClass() {
		return panelClass == null ? EntityPanel.class : panelClass;
	}

	private Class<? extends EntityEditPanel> editPanelClass() {
		return editPanelClass;
	}

	private Class<? extends EntityTablePanel> tablePanelClass() {
		return tablePanelClass == null ? EntityTablePanel.class : tablePanelClass;
	}

	private static Constructor<EntityPanel> findModelConstructor(Class<? extends EntityPanel> panelClass)
					throws NoSuchMethodException {
		for (Constructor<?> constructor : panelClass.getConstructors()) {
			if (constructor.getParameterCount() == 1 &&
							SwingEntityModel.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
				return (Constructor<EntityPanel>) constructor;
			}
		}

		throw new NoSuchMethodException("Constructor with a single parameter of type SwingEntityModel (or subclass) not found in class: " + panelClass);
	}

	private static Constructor<EntityEditPanel> findEditModelConstructor(Class<? extends EntityEditPanel> editPanelClass)
					throws NoSuchMethodException {
		for (Constructor<?> constructor : editPanelClass.getConstructors()) {
			if (constructor.getParameterCount() == 1 &&
							SwingEntityEditModel.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
				return (Constructor<EntityEditPanel>) constructor;
			}
		}

		throw new NoSuchMethodException("Constructor with a single parameter of type SwingEntityEditModel (or subclass) not found in class: " + editPanelClass);
	}

	private static Constructor<EntityTablePanel> findTableModelConstructor(Class<? extends EntityTablePanel> tablePanelClass)
					throws NoSuchMethodException {
		for (Constructor<?> constructor : tablePanelClass.getConstructors()) {
			if (constructor.getParameterCount() == 1 &&
							SwingEntityTableModel.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
				return (Constructor<EntityTablePanel>) constructor;
			}
		}

		throw new NoSuchMethodException("Constructor with a single parameter of type SwingEntityTableModel (or subclass) not found in class: " + tablePanelClass);
	}

	private static final class EmptyOnBuild<T> implements Consumer<T> {
		@Override
		public void accept(T panel) {/*Do nothing*/}
	}

	private static final class DefaultDetailLayout implements Function<EntityPanel, DetailLayout> {

		@Override
		public DetailLayout apply(EntityPanel entityPanel) {
			return TabbedDetailLayout.builder(entityPanel).build();
		}
	}
}
