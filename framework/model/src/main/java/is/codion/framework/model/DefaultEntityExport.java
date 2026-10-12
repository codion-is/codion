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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.ObservableState;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.exception.EntityNotFoundException;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.DerivedAttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityExport.Builder.AttributesStep;
import is.codion.framework.model.EntityExport.Builder.EntitiesStep;
import is.codion.framework.model.EntityExport.Builder.OutputStep;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class DefaultEntityExport implements EntityExport {

	private static final String TAB = "\t";
	private static final String SPACE = " ";

	private final @Nullable Iterator<Entity> entityIterator;
	private final @Nullable Iterator<Entity.Key> keyIterator;
	private final EntityConnection connection;
	private final Consumer<String> output;
	private final @Nullable Consumer<Entity> processed;
	private final @Nullable ObservableState cancel;
	private final Reference root;
	private final List<ExportColumn> columns;

	private DefaultEntityExport(DefaultBuilder builder) {
		this.entityIterator = builder.entities;
		this.keyIterator = builder.keys;
		this.connection = builder.connection;
		this.output = builder.output;
		this.processed = builder.processed;
		this.cancel = builder.cancel;
		this.root = new Reference(null, null, connection.entities().definition(builder.entityType));
		this.columns = builder.paths.stream()
						.map(this::column)
						.collect(toList());
	}

	private void export() {
		if (columns.isEmpty()) {
			throw new IllegalStateException("No attributes included in the export of " + root.definition.type());
		}
		Iterator<Entity> iterator = iterator();
		output.accept(columns.stream()
						.map(column -> column.header)
						.collect(joining(TAB, "", "\n")));
		while (iterator.hasNext()) {
			if (cancel != null && cancel.is()) {
				throw new CancelException();
			}
			Entity entity = iterator.next();
			if (!entity.type().equals(root.definition.type())) {
				throw new IllegalArgumentException("Only entities of type: " + root.definition.type() + " are being exported, encountered: " + entity.type());
			}
			output.accept(row(entity));
			if (processed != null) {
				processed.accept(entity);
			}
		}
	}

	private Iterator<Entity> iterator() {
		if (entityIterator != null) {
			return entityIterator;
		}
		if (keyIterator != null) {
			return new QueriedIterator(keyIterator);
		}

		throw new IllegalStateException("Entity or Key iterator must be provided");
	}

	/**
	 * Adds the references along the given path, requiring the attribute of the last one, the root for an attribute
	 * of the entity being exported.
	 */
	private ExportColumn column(List<Attribute<?>> path) {
		Reference reference = root;
		List<String> captions = new ArrayList<>(path.size());
		for (Attribute<?> attribute : path.subList(0, path.size() - 1)) {
			captions.add(reference.definition.attributes().definition(attribute).caption());
			reference = reference.reference((ForeignKey) attribute);
		}
		Attribute<?> attribute = path.get(path.size() - 1);
		captions.add(reference.definition.attributes().definition(attribute).caption());
		reference.attributes.add(attribute);

		return new ExportColumn(reference, attribute, String.join(SPACE, captions));
	}

	private String row(Entity entity) {
		//the entities referenced by this row, resolved once each, null for a null reference
		Map<Reference, @Nullable Entity> referenced = new HashMap<>();
		referenced.put(root, entity);

		return columns.stream()
						.map(column -> value(column, referenced))
						.collect(joining(TAB, "", "\n"));
	}

	private String value(ExportColumn column, Map<Reference, @Nullable Entity> referenced) {
		Entity entity = entity(column.reference, referenced);

		return entity == null ? "" : replaceNewlinesAndTabs(entity.formatted(column.attribute));
	}

	private @Nullable Entity entity(Reference reference, Map<Reference, @Nullable Entity> referenced) {
		if (referenced.containsKey(reference)) {
			return referenced.get(reference);
		}
		Entity entity = referenced(requireNonNull(reference.parent), reference, referenced);
		referenced.put(reference, entity);

		return entity;
	}

	/**
	 * Returns the entity the given reference refers to, preferring the one the referencing entity already carries.
	 * @return the referenced entity or null if the referencing entity or the foreign key value is null
	 */
	private @Nullable Entity referenced(Reference parent, Reference reference, Map<Reference, @Nullable Entity> referenced) {
		Entity entity = entity(parent, referenced);
		if (entity == null) {
			return null;
		}
		ForeignKey foreignKey = requireNonNull(reference.foreignKey);
		Entity referencedEntity = entity.get(foreignKey);
		if (referencedEntity != null && reference.carried(referencedEntity)) {
			return referencedEntity;
		}
		Entity.Key referencedKey = entity.key(foreignKey);

		return referencedKey == null ? null : reference.select(referencedKey, connection);
	}

	private static String replaceNewlinesAndTabs(String string) {
		return string.replace("\r\n", SPACE)
						.replace("\n", SPACE)
						.replace("\r", SPACE)
						.replace(TAB, SPACE);
	}

	/**
	 * The entity being exported, the root, or an entity referenced via a path of foreign keys from it,
	 * along with the attributes the export requires of it.
	 */
	private static final class Reference {

		private final @Nullable Reference parent;
		private final @Nullable ForeignKey foreignKey;
		private final EntityDefinition definition;
		private final Map<ForeignKey, Reference> references = new LinkedHashMap<>();
		private final Set<Attribute<?>> attributes = new HashSet<>();
		private final Map<Entity.Key, Entity> selected = new HashMap<>();

		private @Nullable Collection<Attribute<?>> selectAttributes;

		private Reference(@Nullable Reference parent, @Nullable ForeignKey foreignKey, EntityDefinition definition) {
			this.parent = parent;
			this.foreignKey = foreignKey;
			this.definition = definition;
		}

		/**
		 * @return the reference via the given foreign key, the foreign key being required in order to follow it
		 */
		private Reference reference(ForeignKey foreignKey) {
			attributes.add(foreignKey);

			return references.computeIfAbsent(foreignKey, k ->
							new Reference(this, foreignKey, definition.foreignKeys().referencedBy(foreignKey)));
		}

		/**
		 * Returns true if the given entity was selected with every attribute the export requires, derived
		 * attributes excluded, those being computed from source attributes which the closure includes.
		 */
		private boolean carried(Entity entity) {
			return selectAttributes().stream()
							.filter(attribute -> !definition.attributes().definition(attribute).derived())
							.allMatch(entity::contains);
		}

		private Entity select(Entity.Key key, EntityConnection connection) {
			try {
				return selected.computeIfAbsent(key, k -> connection.selectSingle(where(key(key))
								.attributes(selectAttributes())));
			}
			catch (EntityNotFoundException e) {
				EntityNotFoundException exception =
								new EntityNotFoundException("Entity not found: " + key + ", foreignKey: " + foreignKey);
				exception.initCause(e);

				throw exception;
			}
		}

		/**
		 * @return the attributes the export requires, along with the source attributes of the derived ones
		 */
		private Collection<Attribute<?>> selectAttributes() {
			if (selectAttributes == null) {
				Set<Attribute<?>> select = new HashSet<>(attributes);
				select.addAll(sourceAttributes(definition, select));
				selectAttributes = select;
			}

			return selectAttributes;
		}

		private static Collection<Attribute<?>> sourceAttributes(EntityDefinition entityDefinition, Collection<Attribute<?>> included) {
			Set<Attribute<?>> sourceAttributes = new HashSet<>();
			included.stream()
							.map(attribute -> entityDefinition.attributes().definition(attribute))
							.filter(AttributeDefinition::derived)
							.map(DerivedAttributeDefinition.class::cast)
							.forEach(definition -> sourceAttributes.addAll(definition.attributes()));
			if (!sourceAttributes.isEmpty() && !included.containsAll(sourceAttributes)) {
				sourceAttributes.addAll(sourceAttributes(entityDefinition, sourceAttributes));
			}

			return sourceAttributes;
		}
	}

	private static final class ExportColumn {

		private final Reference reference;
		private final Attribute<?> attribute;
		private final String header;

		private ExportColumn(Reference reference, Attribute<?> attribute, String header) {
			this.reference = reference;
			this.attribute = attribute;
			this.header = header;
		}
	}

	private final class QueriedIterator implements Iterator<Entity> {

		private final Iterator<Entity.Key> keys;

		private QueriedIterator(Iterator<Entity.Key> keys) {
			this.keys = keys;
		}

		@Override
		public boolean hasNext() {
			return keys.hasNext();
		}

		@Override
		public Entity next() {
			Entity.Key key = keys.next();
			try {
				return connection.selectSingle(where(key(key))
								.attributes(root.selectAttributes()));
			}
			catch (EntityNotFoundException e) {
				EntityNotFoundException exception = new EntityNotFoundException("Entity not found: " + key);
				exception.initCause(e);

				throw exception;
			}
		}
	}

	static final class DefaultEntityTypeStep implements Builder.EntityTypeStep {

		private final EntityConnection connection;

		DefaultEntityTypeStep(EntityConnection connection) {
			this.connection = connection;
		}

		@Override
		public AttributesStep entityType(EntityType entityType) {
			return new DefaultAttributesStep(connection, requireNonNull(entityType));
		}
	}

	private static final class DefaultAttributesStep implements AttributesStep {

		private final EntityConnection connection;
		private final EntityType entityType;

		private DefaultAttributesStep(EntityConnection connection, EntityType entityType) {
			this.connection = connection;
			this.entityType = entityType;
		}

		@Override
		public EntitiesStep attributes(Consumer<Attributes> attributes) {
			DefaultAttributes defaultAttributes = new DefaultAttributes(connection.entities(), entityType);
			requireNonNull(attributes).accept(defaultAttributes);

			return new DefaultEntitiesStep(connection, entityType, defaultAttributes.paths);
		}
	}

	private static final class DefaultEntitiesStep implements EntitiesStep {

		private final EntityConnection connection;
		private final EntityType entityType;
		private final List<List<Attribute<?>>> paths;

		private DefaultEntitiesStep(EntityConnection connection, EntityType entityType, List<List<Attribute<?>>> paths) {
			this.connection = connection;
			this.entityType = entityType;
			this.paths = paths;
		}

		@Override
		public OutputStep entities(Iterator<Entity> iterator) {
			return new DefaultOutputStep(this, requireNonNull(iterator), null);
		}

		@Override
		public OutputStep keys(Iterator<Entity.Key> iterator) {
			return new DefaultOutputStep(this, null, requireNonNull(iterator));
		}
	}

	private static final class DefaultOutputStep implements OutputStep {

		private final DefaultEntitiesStep entitiesStep;
		private final @Nullable Iterator<Entity> entities;
		private final @Nullable Iterator<Entity.Key> keys;

		private DefaultOutputStep(DefaultEntitiesStep entitiesStep, @Nullable Iterator<Entity> entities,
															@Nullable Iterator<Entity.Key> keys) {
			this.entitiesStep = entitiesStep;
			this.entities = entities;
			this.keys = keys;
		}

		@Override
		public Builder output(Consumer<String> output) {
			return new DefaultBuilder(this, requireNonNull(output));
		}
	}

	private static final class DefaultBuilder implements Builder {

		private final EntityConnection connection;
		private final EntityType entityType;
		private final List<List<Attribute<?>>> paths;
		private final @Nullable Iterator<Entity> entities;
		private final @Nullable Iterator<Entity.Key> keys;
		private final Consumer<String> output;

		private @Nullable Consumer<Entity> processed;
		private @Nullable ObservableState cancel;

		private DefaultBuilder(DefaultOutputStep outputStep, Consumer<String> output) {
			this.connection = outputStep.entitiesStep.connection;
			this.entityType = outputStep.entitiesStep.entityType;
			this.paths = outputStep.entitiesStep.paths;
			this.entities = outputStep.entities;
			this.keys = outputStep.keys;
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

	private static final class DefaultAttributes implements Attributes {

		private final Entities entities;
		private final EntityType entityType;
		private final List<List<Attribute<?>>> paths = new ArrayList<>();

		private DefaultAttributes(Entities entities, EntityType entityType) {
			this.entities = entities;
			this.entityType = entityType;
		}

		@Override
		public Attributes add(Attribute<?>... path) {
			return add(asList(requireNonNull(path)));
		}

		@Override
		public Attributes add(List<Attribute<?>> path) {
			paths.add(validate(new ArrayList<>(requireNonNull(path))));
			return this;
		}

		private List<Attribute<?>> validate(List<Attribute<?>> path) {
			if (path.isEmpty()) {
				throw new IllegalArgumentException("An attribute path must contain at least one attribute");
			}
			EntityDefinition definition = entities.definition(entityType);
			for (int i = 0; i < path.size(); i++) {
				Attribute<?> attribute = requireNonNull(path.get(i));
				//throws in case the attribute does not belong to the entity
				definition.attributes().definition(attribute);
				if (i < path.size() - 1) {
					if (!(attribute instanceof ForeignKey)) {
						throw new IllegalArgumentException("Only the last attribute of a path may be other than a foreign key: " + path);
					}
					definition = entities.definition(((ForeignKey) attribute).referencedType());
				}
			}

			return unmodifiableList(path);
		}
	}
}
