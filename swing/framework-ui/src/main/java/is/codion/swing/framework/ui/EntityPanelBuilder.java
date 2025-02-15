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

import javax.swing.ImageIcon;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

final class EntityPanelBuilder implements EntityPanel.Builder {

	private final EntityType entityType;
	private final Function<EntityConnectionProvider, EntityPanel> entityPanel;

	private String caption;
	private String description;
	private ImageIcon icon;

	EntityPanelBuilder(EntityType entityType, Function<EntityConnectionProvider, EntityPanel> entityPanel) {
		this.entityType = requireNonNull(entityType);
		this.entityPanel = requireNonNull(entityPanel);
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
	public boolean equals(Object obj) {
		if (obj instanceof EntityPanelBuilder) {
			EntityPanelBuilder that = (EntityPanelBuilder) obj;

			return Objects.equals(entityType, that.entityType);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(entityType);
	}

	@Override
	public EntityPanel build(EntityConnectionProvider connectionProvider) {
		return entityPanel.apply(requireNonNull(connectionProvider));
	}
}
