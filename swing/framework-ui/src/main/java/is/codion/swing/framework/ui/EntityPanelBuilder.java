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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;

import org.jspecify.annotations.Nullable;

import javax.swing.ImageIcon;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

final class EntityPanelBuilder implements EntityPanel.Builder {

	static final EntityTypeStep ENTITY_TYPE = new DefaultEntityTypeStep();

	private final EntityType entityType;
	private final Function<EntityConnectionProvider, EntityPanel> entityPanel;

	private @Nullable String caption;
	private @Nullable String description;
	private @Nullable ImageIcon icon;

	EntityPanelBuilder(EntityType entityType, Function<EntityConnectionProvider, EntityPanel> entityPanel) {
		this.entityType = requireNonNull(entityType);
		this.entityPanel = requireNonNull(entityPanel);
	}

	@Override
	public EntityType entityType() {
		return entityType;
	}

	@Override
	public EntityPanelBuilder caption(@Nullable String caption) {
		this.caption = caption;
		return this;
	}

	@Override
	public Optional<String> description() {
		return Optional.ofNullable(description);
	}

	@Override
	public EntityPanelBuilder description(@Nullable String description) {
		this.description = description;
		return this;
	}

	@Override
	public Optional<String> caption() {
		return Optional.ofNullable(caption);
	}

	@Override
	public EntityPanel.Builder icon(@Nullable ImageIcon icon) {
		this.icon = icon;
		return this;
	}

	@Override
	public Optional<ImageIcon> icon() {
		return Optional.ofNullable(icon);
	}

	@Override
	public EntityPanel build(EntityConnectionProvider connectionProvider) {
		return entityPanel.apply(requireNonNull(connectionProvider));
	}

	private static final class DefaultEntityTypeStep implements EntityTypeStep {

		@Override
		public EntityPanel.Builder.PanelBuilder entityType(EntityType entityType) {
			return new DefaultPanelBuilder(requireNonNull(entityType));
		}
	}

	private static final class DefaultPanelBuilder implements PanelBuilder {

		private final EntityType entityType;

		private DefaultPanelBuilder(EntityType entityType) {
			this.entityType = entityType;
		}

		@Override
		public EntityPanel.Builder panel(Function<EntityConnectionProvider, EntityPanel> entityPanel) {
			return new EntityPanelBuilder(entityType, entityPanel);
		}
	}
}
