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
import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.ObservableState;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityExport.Builder.EntitiesStep;
import is.codion.framework.model.EntityExport.Builder.ExportAttributesStep;
import is.codion.framework.model.EntityExport.Builder.OutputStep;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class DefaultEntityExport implements EntityExport {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityExport.class);

	private static final String TAB = "\t";
	private static final String SPACE = " ";

	private final Iterator<Entity> iterator;
	private final EntityConnectionProvider connectionProvider;
	private final Entities entities;
	private final Consumer<String> output;
	private final ExportAttributes attributes;
	private final @Nullable Consumer<Entity> processed;
	private final @Nullable ObservableState cancel;

	private final Map<ExportAttributes, List<Attribute<?>>> ordered = new HashMap<>();

	private DefaultEntityExport(DefaultBuilder builder) {
		this.iterator = builder.iterator;
		this.connectionProvider = builder.connectionProvider;
		this.entities = connectionProvider.entities();
		this.output = builder.output;
		this.attributes = builder.attributes;
		this.processed = builder.processed;
		this.cancel = builder.cancel;
	}

	private void export() {
		EntityConnection connection = connectionProvider.connection();
		String header = createHeader();
		if (header.isEmpty()) {
			return;
		}
		output.accept(header);
		while (iterator.hasNext()) {
			if (cancel != null && cancel.is()) {
				throw new CancelException();
			}
			Entity entity = iterator.next();
			if (!entity.type().equals(attributes.entityType())) {
				throw new IllegalArgumentException("Only entities of type: " + attributes.entityType() + " are being exported, encountered: " + entity.type());
			}
			output.accept(createRow(entity, connection));
			if (processed != null) {
				processed.accept(entity);
			}
		}
	}

	private String createHeader() {
		List<String> columnHeaders = addToHeader(attributes, new ArrayList<>(), "");
		if (columnHeaders.isEmpty()) {
			return "";
		}
		return columnHeaders.stream().collect(joining(TAB, "", "\n"));
	}

	private String createRow(Entity entity, EntityConnection connection) {
		return addToRow(attributes, entity.primaryKey(), null,
						new ArrayList<>(), new HashMap<>(), connection)
						.stream().collect(joining(TAB, "", "\n"));
	}

	private List<String> addToHeader(ExportAttributes attributes, List<String> header, String prefix) {
		for (Attribute<?> node : orderered(attributes)) {
			String caption = entities.definition(node.entityType()).attributes().definition(node).caption();
			String columnHeader = prefix.isEmpty() ? caption : (prefix + SPACE + caption);
			if (attributes.include().contains(node)) {
				header.add(columnHeader);
			}
			if (node instanceof ForeignKey) {
				attributes.attributes((ForeignKey) node).ifPresent(foreignKeyAttributes ->
								addToHeader(foreignKeyAttributes, header, columnHeader));
			}
		}

		return header;
	}

	private List<String> addToRow(ExportAttributes attributes, Entity.@Nullable Key key, @Nullable ForeignKey foreignKey,
																List<String> row, Map<Entity.Key, Entity> cache, EntityConnection connection) {
		Entity entity = key == null ? null : selectEntity(key, foreignKey, cache, connection);
		for (Attribute<?> attribute : orderered(attributes)) {
			if (attributes.include().contains(attribute)) {
				row.add(entity == null ? "" : replaceNewlinesAndTabs(entity.formatted(attribute)));
			}
			if (attribute instanceof ForeignKey) {
				Entity.Key referencedKey = entity == null ? null : entity.key((ForeignKey) attribute);
				attributes.attributes((ForeignKey) attribute).ifPresent(foreignKeyAttributes ->
								addToRow(foreignKeyAttributes, referencedKey, (ForeignKey) attribute, row, cache, connection));
			}
		}

		return row;
	}

	private List<Attribute<?>> orderered(ExportAttributes attributes) {
		return ordered.computeIfAbsent(attributes, k ->
						entities.definition(attributes.entityType()).attributes().get().stream()
										.sorted(attributes.comparator())
										.collect(toList()));
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

	static final class DefaultEntityTypeStep implements Builder.EntityTypeStep {

		private final EntityConnectionProvider connectionProvider;

		DefaultEntityTypeStep(EntityConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public ExportAttributesStep entityType(EntityType entityType) {
			return new DefaultExportAttributesStep(connectionProvider, requireNonNull(entityType));
		}
	}

	private static final class DefaultExportAttributesStep implements ExportAttributesStep {

		private final EntityConnectionProvider connectionProvider;
		private final EntityType entityType;

		private DefaultExportAttributesStep(EntityConnectionProvider connectionProvider, EntityType entityType) {
			this.connectionProvider = connectionProvider;
			this.entityType = entityType;
		}

		@Override
		public EntitiesStep attributes(Consumer<ExportAttributes.Builder> attributes) {
			ExportAttributes.Builder builder = new DefaultExportAttributesBuilder(connectionProvider.entities(), entityType);
			requireNonNull(attributes).accept(builder);

			return new DefaultEntitiesStep(connectionProvider, builder.build());
		}
	}

	private static final class DefaultEntitiesStep implements EntitiesStep {

		private final EntityConnectionProvider connectionProvider;
		private final ExportAttributes attributes;

		private DefaultEntitiesStep(EntityConnectionProvider connectionProvider, ExportAttributes attributes) {
			this.connectionProvider = connectionProvider;
			this.attributes = attributes;
		}

		@Override
		public OutputStep entities(Iterator<Entity> iterator) {
			return new DefaultOutputStep(requireNonNull(iterator), attributes, connectionProvider);
		}
	}

	private static final class DefaultOutputStep implements OutputStep {

		private final Iterator<Entity> iterator;
		private final ExportAttributes attributes;
		private final EntityConnectionProvider connectionProvider;

		private DefaultOutputStep(Iterator<Entity> iterator, ExportAttributes attributes, EntityConnectionProvider connectionProvider) {
			this.iterator = iterator;
			this.attributes = attributes;
			this.connectionProvider = connectionProvider;
		}

		@Override
		public Builder output(Consumer<String> output) {
			return new DefaultBuilder(iterator, attributes, connectionProvider, requireNonNull(output));
		}
	}

	private static final class DefaultBuilder implements Builder {

		private final Iterator<Entity> iterator;
		private final ExportAttributes attributes;
		private final EntityConnectionProvider connectionProvider;
		private final Consumer<String> output;

		private @Nullable Consumer<Entity> processed;
		private @Nullable ObservableState cancel;

		private DefaultBuilder(Iterator<Entity> iterator, ExportAttributes attributes,
													 EntityConnectionProvider connectionProvider, Consumer<String> output) {
			this.iterator = iterator;
			this.attributes = attributes;
			this.connectionProvider = connectionProvider;
			this.output = output;
		}

		@Override
		public Builder processed(Consumer<Entity> processed) {
			this.processed = requireNonNull(processed);
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

	static final class DefaultExportAttributes implements ExportAttributes {

		private final Entities entities;
		private final EntityDefinition definition;
		private final Set<Attribute<?>> include;
		private final Comparator<Attribute<?>> comparator;
		private final Map<ForeignKey, ExportAttributes> foreignKeyAttributes = new HashMap<>();

		DefaultExportAttributes(DefaultExportAttributesBuilder builder) {
			this.entities = builder.entities;
			this.definition = entities.definition(builder.entityType);
			this.include = unmodifiableSet(new HashSet<>(builder.include));
			this.comparator = builder.comparator;
			this.foreignKeyAttributes.putAll(builder.foreignKeyAttributes);
		}

		@Override
		public EntityType entityType() {
			return definition.type();
		}

		@Override
		public Collection<Attribute<?>> include() {
			return include;
		}

		@Override
		public Comparator<Attribute<?>> comparator() {
			return comparator;
		}

		@Override
		public Optional<ExportAttributes> attributes(ForeignKey foreignKey) {
			definition.attributes().definition(foreignKey);
			return Optional.ofNullable(foreignKeyAttributes.get(foreignKey));
		}
	}

	private static final class DefaultExportAttributesBuilder implements ExportAttributes.Builder {

		private final Entities entities;
		private final EntityType entityType;
		private final Map<ForeignKey, ExportAttributes> foreignKeyAttributes = new HashMap<>();

		private Collection<Attribute<?>> include = Collections.emptyList();
		private Comparator<Attribute<?>> comparator;

		private DefaultExportAttributesBuilder(Entities entities, EntityType entityType) {
			this.entities = entities;
			this.entityType = entityType;
			this.comparator = new AttributeComparator(entities.definition(entityType));
		}

		@Override
		public ExportAttributes.Builder include(Attribute<?>... attributes) {
			return include(asList(attributes));
		}

		@Override
		public ExportAttributes.Builder include(Collection<Attribute<?>> attributes) {
			requireNonNull(attributes).forEach(this::validate);
			this.include = requireNonNull(attributes);
			return this;
		}

		@Override
		public ExportAttributes.Builder order(Attribute<?>... attributes) {
			return order(asList(attributes));
		}

		@Override
		public ExportAttributes.Builder order(List<Attribute<?>> attributes) {
			requireNonNull(attributes).forEach(this::validate);
			return order(new IndexedOrder(attributes));
		}

		@Override
		public ExportAttributes.Builder order(Comparator<Attribute<?>> comparator) {
			this.comparator = requireNonNull(comparator);
			return this;
		}

		@Override
		public ExportAttributes.Builder attributes(ForeignKey foreignKey, Consumer<ExportAttributes.Builder> attributes) {
			ExportAttributes.Builder builder = new DefaultExportAttributesBuilder(entities, foreignKey.referencedType());
			attributes.accept(builder);
			foreignKeyAttributes.put(foreignKey, builder.build());
			return this;
		}

		@Override
		public ExportAttributes build() {
			return new DefaultExportAttributes(this);
		}

		private void validate(Attribute<?> attribute) {
			entities.definition(entityType).attributes().definition(attribute);
		}
	}

	private static final class IndexedOrder implements Comparator<Attribute<?>> {

		private final List<Attribute<?>> order;

		private IndexedOrder(List<Attribute<?>> order) {
			this.order = order;
		}

		@Override
		public int compare(Attribute<?> attribute1, Attribute<?> attribute2) {
			int index1 = order.indexOf(attribute1);
			int index2 = order.indexOf(attribute2);
			if (index1 == -1 || index2 == -1) {
				return 0;
			}

			return Integer.compare(index1, index2);
		}
	}

	private static final class AttributeComparator implements Comparator<Attribute<?>> {

		private final EntityDefinition definition;

		private AttributeComparator(EntityDefinition definition) {
			this.definition = definition;
		}

		@Override
		public int compare(Attribute<?> a1, Attribute<?> a2) {
			return definition.attributes().definition(a1).caption().compareTo(definition.attributes().definition(a2).caption());
		}
	}
}
