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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.Text;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class DefaultEntityExportModel implements EntityExportModel {

	private static final AttributeDefinitionComparator ATTRIBUTE_COMPARATOR = new AttributeDefinitionComparator();

	private static final String TAB = "\t";
	private static final String SPACE = " ";

	private final EntityType entityType;
	private final EntityConnectionProvider connectionProvider;

	private final DefaultEntityNode entityNode;

	DefaultEntityExportModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this.entityType = requireNonNull(entityType);
		this.connectionProvider = requireNonNull(connectionProvider);
		this.entityNode = new DefaultEntityNode(entityType, connectionProvider.entities());
		this.entityNode.refresh();
	}

	@Override
	public EntityType entityType() {
		return entityType;
	}

	@Override
	public EntityNode root() {
		return entityNode;
	}

	@Override
	public void selectAll() {
		select(entityNode.children(), true);
	}

	@Override
	public void selectNone() {
		select(entityNode.children(), false);
	}

	@Override
	public void selectDefaults() {
		entityNode.refresh();
		entityNode.children().forEach(node -> node.selected().set(true));
	}

	@Override
	public ExportEntities export() {
		return new DefaultExportEntities();
	}

	private final class DefaultExportEntities implements ExportEntities {

		@Override
		public ExportOutput entities(Iterator<Entity> entities) {
			return new DefaultExportOutput(requireNonNull(entities));
		}
	}

	private final class DefaultExportOutput implements ExportOutput {

		private final Iterator<Entity> entities;

		private DefaultExportOutput(Iterator<Entity> entities) {
			this.entities = entities;
		}

		@Override
		public Exporter output(Consumer<String> output) {
			return new DefaultExporter(entities, requireNonNull(output));
		}
	}

	private final class DefaultExporter implements Exporter {

		private final Iterator<Entity> entities;
		private final Consumer<String> output;

		private @Nullable Consumer<Entity> handler;
		private @Nullable ObservableState cancel;

		private DefaultExporter(Iterator<Entity> entities, Consumer<String> output) {
			this.entities = entities;
			this.output = output;
		}

		@Override
		public Exporter handler(Consumer<Entity> handler) {
			this.handler = requireNonNull(handler);
			return this;
		}

		@Override
		public Exporter cancel(ObservableState cancel) {
			this.cancel = requireNonNull(cancel);
			return this;
		}

		@Override
		public void export() {
			EntityConnection connection = connectionProvider.connection();
			output.accept(createHeader());
			while (entities.hasNext() && (cancel == null || !cancel.is())) {
				Entity entity = entities.next();
				output.accept(createRow(entity, connection));
				if (handler != null) {
					handler.accept(entity);
				}
			}
		}

		private String createHeader() {
			return addToHeader(entityNode.children(), new ArrayList<>(), "")
							.stream().collect(joining(TAB, "", "\n"));
		}

		private String createRow(Entity entity, EntityConnection connection) {
			return addToRow(entityNode.children(), entity.primaryKey(), null,
							new ArrayList<>(), new HashMap<>(), connection)
							.stream().collect(joining(TAB, "", "\n"));
		}

		private static List<String> addToHeader(List<AttributeNode> nodes, List<String> header, String prefix) {
			for (AttributeNode node : nodes) {
				String caption = node.caption();
				String columnHeader = prefix.isEmpty() ? caption : (prefix + SPACE + caption);
				if (node.selected().is()) {
					header.add(columnHeader);
				}
				if (node instanceof ForeignKeyNode) {
					addToHeader(((ForeignKeyNode) node).children(), header, columnHeader);
				}
			}

			return header;
		}

		private static List<String> addToRow(List<AttributeNode> nodes, Entity.Key key, @Nullable ForeignKey foreignKey,
																				 List<String> row, Map<Entity.Key, Entity> cache, EntityConnection connection) {
			Entity entity = selectEntity(key, foreignKey, cache, connection);
			for (AttributeNode node : nodes) {
				Attribute<?> attribute = node.attribute();
				if (node.selected().is()) {
					row.add(replaceNewlinesAndTabs(entity.formatted(attribute)));
				}
				if (node instanceof ForeignKeyNode) {
					Entity.Key referencedKey = entity.key((ForeignKey) attribute);
					if (referencedKey != null) {
						addToRow(((ForeignKeyNode) node).children(), referencedKey, (ForeignKey) attribute, row, cache, connection);
					}
				}
			}

			return row;
		}

		private static Entity selectEntity(Entity.Key key, @Nullable ForeignKey foreignKey,
																			 Map<Entity.Key, Entity> cache, EntityConnection connection) {
			try {
				return cache.computeIfAbsent(key, k -> connection.select(key));
			}
			catch (RecordNotFoundException e) {
				throw new RuntimeException("Record not found: " + key + (foreignKey == null ? "" : ", foreignKey: " + foreignKey), e);
			}
		}

		private static String replaceNewlinesAndTabs(String string) {
			return string.replace("\r\n", SPACE)
							.replace("\n", SPACE)
							.replace("\r", SPACE)
							.replace(TAB, SPACE);
		}
	}

	private static void select(List<AttributeNode> nodes, boolean select) {
		for (AttributeNode node : nodes) {
			node.selected().set(select);
			if (node instanceof ForeignKeyNode) {
				select(((ForeignKeyNode) node).children(), select);
			}
		}
	}

	private static void populate(DefaultEntityNode entityNode, Set<ForeignKey> visited, EntityType rootEntityType) {
		entityNode.children.clear();
		EntityDefinition definition = entityNode.entities.definition(entityNode.entityType());
		for (AttributeDefinition<?> attributeDefinition : definition.attributes().definitions()
						.stream()
						.sorted(ATTRIBUTE_COMPARATOR)
						.collect(toList())) {
			if (attributeDefinition instanceof ForeignKeyDefinition) {
				ForeignKeyDefinition foreignKeyDefinition = (ForeignKeyDefinition) attributeDefinition;
				ForeignKey foreignKey = foreignKeyDefinition.attribute();
				boolean cyclical = visited.contains(foreignKey) || foreignKey.referencedType().equals(rootEntityType);
				DefaultForeignKeyNode foreignKeyNode = new DefaultForeignKeyNode(foreignKeyDefinition, cyclical, entityNode.entities, new HashSet<>(visited));
				entityNode.children.add(foreignKeyNode);
				if (!cyclical) {
					visited.add(foreignKey);
					populate(foreignKeyNode.entityNode, visited, rootEntityType);
				}
			}
			else if (!attributeDefinition.hidden()) {
				entityNode.children.add(new DefaultAttributeNode(attributeDefinition));
			}
		}
	}

	private static final class DefaultEntityNode implements EntityNode {

		private final List<AttributeNode> children = new ArrayList<>();
		private final EntityType entityType;
		private final Entities entities;

		private DefaultEntityNode(EntityType entityType, Entities entities) {
			this.entityType = entityType;
			this.entities = entities;
		}

		@Override
		public EntityType entityType() {
			return entityType;
		}

		@Override
		public List<AttributeNode> children() {
			return unmodifiableList(children);
		}

		@Override
		public void sort(Comparator<AttributeNode> comparator) {
			children.sort(requireNonNull(comparator));
		}

		private void refresh() {
			populate(this, new HashSet<>(), entityType);
		}
	}

	private static class DefaultAttributeNode implements AttributeNode {

		private final AttributeDefinition<?> definition;
		private final State selected = State.state();

		private DefaultAttributeNode(AttributeDefinition<?> definition) {
			this.definition = definition;
		}

		@Override
		public String caption() {
			return definition.caption();
		}

		@Override
		public Attribute<?> attribute() {
			return definition.attribute();
		}

		@Override
		public final State selected() {
			return selected;
		}
	}

	private static final class DefaultForeignKeyNode extends DefaultAttributeNode implements ForeignKeyNode {

		private final DefaultEntityNode entityNode;
		private final boolean expandable;
		private final Set<ForeignKey> visitedPath;

		private DefaultForeignKeyNode(ForeignKeyDefinition definition, boolean expandable,
																	Entities entities, Set<ForeignKey> visitedPath) {
			super(definition);
			this.entityNode = new DefaultEntityNode(definition.attribute().referencedType(), entities);
			this.expandable = expandable;
			this.visitedPath = visitedPath;
		}

		@Override
		public ForeignKey attribute() {
			return (ForeignKey) super.attribute();
		}

		@Override
		public EntityType entityType() {
			return entityNode.entityType();
		}

		@Override
		public List<AttributeNode> children() {
			return entityNode.children();
		}

		@Override
		public void sort(Comparator<AttributeNode> comparator) {
			entityNode.children.sort(comparator);
		}

		@Override
		public boolean expandable() {
			return expandable;
		}

		@Override
		public void expand() {
			if (expandable) {
				Set<ForeignKey> newVisited = new HashSet<>(visitedPath);
				newVisited.add(attribute());
				populate(entityNode, newVisited, attribute().referencedType());
			}
		}
	}

	private static final class AttributeDefinitionComparator implements Comparator<AttributeDefinition<?>> {

		private final Comparator<String> collator = Text.collator();

		@Override
		public int compare(AttributeDefinition<?> definition1, AttributeDefinition<?> definition2) {
			return collator.compare(definition1.caption().toLowerCase(), definition2.caption().toLowerCase());
		}
	}
}
