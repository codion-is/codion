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
import is.codion.framework.model.EntityExport.Builder.ConnectionProviderStep;
import is.codion.framework.model.EntityExport.Builder.EntitiesStep;
import is.codion.framework.model.EntityExport.Builder.OutputStep;
import is.codion.framework.model.EntityExport.Builder.SettingsStep;
import is.codion.framework.model.EntityExport.Settings.AttributeExport;
import is.codion.framework.model.EntityExport.Settings.ForeignKeyExport;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

final class DefaultEntityExport implements EntityExport {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityExport.class);

	static final EntitiesStep EXPORT_ENTITIES = new DefaultEntitiesStep();

	private static final String TAB = "\t";
	private static final String SPACE = " ";

	private final Iterator<Entity> iterator;
	private final EntityConnectionProvider connectionProvider;
	private final Entities entities;
	private final Consumer<String> output;
	private final Settings settings;
	private final @Nullable Consumer<Entity> handler;
	private final @Nullable ObservableState cancel;

	private DefaultEntityExport(DefaultBuilder builder) {
		this.iterator = builder.entities;
		this.connectionProvider = builder.connectionProvider;
		this.entities = connectionProvider.entities();
		this.output = builder.output;
		this.settings = builder.settings;
		this.handler = builder.handler;
		this.cancel = builder.cancel;
	}

	private void export() {
		EntityConnection connection = connectionProvider.connection();
		output.accept(createHeader());
		while (iterator.hasNext() && (cancel == null || !cancel.is())) {
			Entity entity = iterator.next();
			if (!entity.type().equals(settings.definition().type())) {
				throw new IllegalArgumentException("Invalid entity type: " + entity.type());
			}
			output.accept(createRow(entity, connection));
			if (handler != null) {
				handler.accept(entity);
			}
		}
	}

	private String createHeader() {
		return addToHeader(settings.attributes().get(), new ArrayList<>(), "")
						.stream().collect(joining(TAB, "", "\n"));
	}

	private String createRow(Entity entity, EntityConnection connection) {
		return addToRow(settings.attributes().get(), entity.primaryKey(), null,
						new ArrayList<>(), new HashMap<>(), connection)
						.stream().collect(joining(TAB, "", "\n"));
	}

	private List<String> addToHeader(List<AttributeExport> nodes, List<String> header, String prefix) {
		for (AttributeExport node : nodes) {
			String caption = entities.definition(node.attribute().entityType()).attributes().definition(node.attribute()).caption();
			String columnHeader = prefix.isEmpty() ? caption : (prefix + SPACE + caption);
			if (node.include().is()) {
				header.add(columnHeader);
			}
			if (node instanceof ForeignKeyExport) {
				addToHeader(((ForeignKeyExport) node).attributes().get(), header, columnHeader);
			}
		}

		return header;
	}

	private static List<String> addToRow(List<AttributeExport> nodes, Entity.@Nullable Key key, @Nullable ForeignKey foreignKey,
																			 List<String> row, Map<Entity.Key, Entity> cache, EntityConnection connection) {
		Entity entity = key == null ? null : selectEntity(key, foreignKey, cache, connection);
		for (AttributeExport node : nodes) {
			Attribute<?> attribute = node.attribute();
			if (node.include().is()) {
				row.add(entity == null ? "" : replaceNewlinesAndTabs(entity.formatted(attribute)));
			}
			if (node instanceof ForeignKeyExport) {
				Entity.Key referencedKey = entity == null ? null : entity.key((ForeignKey) attribute);
				addToRow(((ForeignKeyExport) node).attributes().get(), referencedKey, (ForeignKey) attribute, row, cache, connection);
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
			String message = "Record not found: " + key + (foreignKey == null ? "" : ", foreignKey: " + foreignKey);
			LOG.error(message, e);
			throw new RecordNotFoundException(message);
		}
	}

	private static String replaceNewlinesAndTabs(String string) {
		return string.replace("\r\n", SPACE)
						.replace("\n", SPACE)
						.replace("\r", SPACE)
						.replace(TAB, SPACE);
	}

	private static final class DefaultEntitiesStep implements EntitiesStep {

		@Override
		public ConnectionProviderStep entities(Iterator<Entity> entities) {
			return new DefaultConnectionProviderStep(entities);
		}
	}

	private static final class DefaultConnectionProviderStep implements ConnectionProviderStep {

		private final Iterator<Entity> entities;

		private DefaultConnectionProviderStep(Iterator<Entity> entities) {
			this.entities = entities;
		}

		@Override
		public OutputStep connectionProvider(EntityConnectionProvider connectionProvider) {
			return new DefaultOutputStep(entities, requireNonNull(connectionProvider));
		}
	}

	private static final class DefaultOutputStep implements OutputStep {

		private final EntityConnectionProvider connectionProvider;
		private final Iterator<Entity> entities;

		private DefaultOutputStep(Iterator<Entity> entities, EntityConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
			this.entities = entities;
		}

		@Override
		public SettingsStep output(Consumer<String> output) {
			return new DefaultSettingsStep(entities, connectionProvider, requireNonNull(output));
		}
	}

	private static final class DefaultSettingsStep implements SettingsStep {

		private final Iterator<Entity> entities;
		private final EntityConnectionProvider connectionProvider;
		private final Consumer<String> output;

		private DefaultSettingsStep(Iterator<Entity> entities, EntityConnectionProvider connectionProvider, Consumer<String> output) {
			this.entities = entities;
			this.connectionProvider = connectionProvider;
			this.output = output;
		}

		@Override
		public Builder settings(Settings settings) {
			return new DefaultBuilder(entities, connectionProvider, output, requireNonNull(settings));
		}
	}

	private static final class DefaultBuilder implements Builder {

		private final Iterator<Entity> entities;
		private final EntityConnectionProvider connectionProvider;
		private final Consumer<String> output;
		private final Settings settings;

		private @Nullable Consumer<Entity> handler;
		private @Nullable ObservableState cancel;

		private DefaultBuilder(Iterator<Entity> entities, EntityConnectionProvider connectionProvider,
													 Consumer<String> output, Settings settings) {
			this.entities = entities;
			this.connectionProvider = connectionProvider;
			this.output = output;
			this.settings = settings;
		}

		@Override
		public Builder handler(Consumer<Entity> handler) {
			this.handler = requireNonNull(handler);
			return this;
		}

		@Override
		public Builder cancel(ObservableState cancel) {
			this.cancel = requireNonNull(cancel);
			return this;
		}

		@Override
		public void export() {
			new DefaultEntityExport(this).export();
		}
	}

	static final class DefaultSettings implements Settings {

		private static final AttributeDefinitionComparator ATTRIBUTE_COMPARATOR = new AttributeDefinitionComparator();

		private final DefaultAttributes attributes = new DefaultAttributes();
		private final EntityDefinition definition;

		DefaultSettings(EntityType entityType, Entities entities) {
			this.definition = entities.definition(entityType);
			populate(attributes.attributes, entities.definition(entityType), entities, new HashSet<>(), entityType);
		}

		@Override
		public EntityDefinition definition() {
			return definition;
		}

		@Override
		public Attributes attributes() {
			return attributes;
		}

		private static void populate(List<AttributeExport> attributes, EntityDefinition definition, Entities entities,
																 Set<ForeignKey> visited, EntityType rootEntityType) {
			attributes.clear();
			for (AttributeDefinition<?> attributeDefinition : definition.attributes().definitions()
							.stream()
							.sorted(ATTRIBUTE_COMPARATOR)
							.collect(toList())) {
				if (attributeDefinition instanceof ForeignKeyDefinition) {
					ForeignKey foreignKey = (ForeignKey) attributeDefinition.attribute();
					boolean cyclical = visited.contains(foreignKey) || foreignKey.referencedType().equals(rootEntityType);
					DefaultForeignKeyExport foreignKeyNode = new DefaultForeignKeyExport(foreignKey, cyclical, entities, new HashSet<>(visited));
					attributes.add(foreignKeyNode);
					if (!cyclical) {
						visited.add(foreignKey);
						populate(foreignKeyNode.attributes.attributes, entities.definition(foreignKeyNode.attribute()
										.referencedType()), entities, visited, rootEntityType);
					}
				}
				else if (!attributeDefinition.hidden()) {
					attributes.add(new DefaultAttributeExport(attributeDefinition.attribute()));
				}
			}
		}

		private static class DefaultAttributes implements Attributes {

			private final List<AttributeExport> attributes = new ArrayList<>();

			@Override
			public List<AttributeExport> get() {
				return unmodifiableList(attributes);
			}

			@Override
			public void sort(Comparator<AttributeExport> comparator) {
				attributes.sort(requireNonNull(comparator));
			}
		}

		private static class DefaultAttributeExport implements AttributeExport {

			private final Attribute<?> attribute;
			private final State include = State.state();

			private DefaultAttributeExport(Attribute<?> attribute) {
				this.attribute = attribute;
			}

			@Override
			public Attribute<?> attribute() {
				return attribute;
			}

			@Override
			public final State include() {
				return include;
			}
		}

		private static final class DefaultForeignKeyExport extends DefaultAttributeExport implements ForeignKeyExport {

			private final DefaultAttributes attributes = new DefaultAttributes();
			private final EntityType entityType;
			private final Entities entities;
			private final boolean expandable;
			private final Set<ForeignKey> visitedPath;

			private DefaultForeignKeyExport(ForeignKey foreignKey, boolean expandable,
																			Entities entities, Set<ForeignKey> visitedPath) {
				super(foreignKey);
				this.entityType = foreignKey.entityType();
				this.entities = entities;
				this.expandable = expandable;
				this.visitedPath = visitedPath;
			}

			@Override
			public ForeignKey attribute() {
				return (ForeignKey) super.attribute();
			}

			@Override
			public Attributes attributes() {
				return attributes;
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
					populate(attributes.attributes, entities.definition(entityType), entities, newVisited, attribute().referencedType());
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
}
