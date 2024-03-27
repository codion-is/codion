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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.RecordNotFoundException;
import is.codion.common.proxy.ProxyBuilder;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static is.codion.swing.common.ui.Utilities.setClipboard;
import static is.codion.swing.common.ui.control.Control.control;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A popup menu for inspecting the values of a single entity instance.
 */
final class EntityPopupMenu extends JPopupMenu {

	private static final int MAXIMUM_VALUE_LENGTH = 42;

	EntityPopupMenu(Entity entity, EntityConnection connection) {
		requireNonNull(entity);
		requireNonNull(connection);
		populateEntityMenu(this, populateEntityGraph(entity.copy(), connection, new HashSet<>()), connection);
	}

	private static void populateEntityMenu(JComponent rootMenu, Entity entity, EntityConnection connection) {
		populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<>(entity.definition().primaryKey().definitions()));
		populateForeignKeyMenu(rootMenu, entity, connection);
		populateValueMenu(rootMenu, entity);
	}

	private static void populatePrimaryKeyMenu(JComponent rootMenu, Entity entity, List<ColumnDefinition<?>> primaryKeyColumns) {
		Text.collate(primaryKeyColumns);
		for (ColumnDefinition<?> primaryKeyColumn : primaryKeyColumns) {
			JMenuItem menuItem = new JMenuItem(new StringBuilder("[PK] ")
							.append(primaryKeyColumn.attribute())
							.append(" [").append(primaryKeyColumn.attribute().type().valueClass().getSimpleName()).append("]: ")
							.append(createValueString(entity, primaryKeyColumn)).toString());
			menuItem.addActionListener(clipboardControl(entity, primaryKeyColumn.attribute()));
			configureMenuItem(menuItem, entity, primaryKeyColumn.attribute(), primaryKeyColumn.attribute().name());
			rootMenu.add(menuItem);
		}
	}

	private static void populateForeignKeyMenu(JComponent rootMenu, Entity entity, EntityConnection connection) {
		List<ForeignKeyDefinition> fkDefinitions = new ArrayList<>(entity.definition().foreignKeys().definitions());
		Text.collate(fkDefinitions);
		for (ForeignKeyDefinition fkDefinition : fkDefinitions) {
			StringBuilder captionBuilder = new StringBuilder("[FK] ").append(fkDefinition.caption()).append(": ");
			ForeignKey foreignKey = fkDefinition.attribute();
			String caption = captionBuilder.append(createValueString(entity, fkDefinition)).toString();
			JMenuItem menuItem = entity.isNull(foreignKey) ? new JMenuItem(caption) : new JMenu(caption);
			configureMenuItem(menuItem, entity, foreignKey, foreignKeyAttributeNames(foreignKey));
			rootMenu.add(menuItem);
			Entity referencedEntity = entity.get(foreignKey);
			if (referencedEntity != null) {
				populateEntityMenu(menuItem, referencedEntity, connection);
			}
		}
	}

	private static String foreignKeyAttributeNames(ForeignKey foreignKey) {
		return foreignKey.references().stream()
						.map(reference -> reference.column().toString())
						.collect(joining(", "));
	}

	private static void populateValueMenu(JComponent rootMenu, Entity entity) {
		List<AttributeDefinition<?>> attributeDefinitions = Text.collate(new ArrayList<>(entity.definition().attributes().definitions()));
		for (AttributeDefinition<?> attributeDefinition : attributeDefinitions) {
			boolean primaryKeyColumn = attributeDefinition instanceof ColumnDefinition && ((ColumnDefinition<?>) attributeDefinition).primaryKey();
			if (!primaryKeyColumn && !(attributeDefinition instanceof ForeignKeyDefinition)) {
				JMenuItem menuItem = new JMenuItem(new StringBuilder(attributeDefinition.toString())
								.append(" [").append(attributeDefinition.attribute().type().valueClass().getSimpleName())
								.append(attributeDefinition.derived() ? "*" : "").append("]: ")
								.append(createValueString(entity, attributeDefinition)).toString());
				menuItem.addActionListener(clipboardControl(entity, attributeDefinition.attribute()));
				configureMenuItem(menuItem, entity, attributeDefinition.attribute(), attributeDefinition.attribute().toString());
				rootMenu.add(menuItem);
			}
		}
	}

	private static String createValueString(Entity entity, AttributeDefinition<?> attributeDefinition) {
		StringBuilder builder = new StringBuilder();
		if (entity.modified(attributeDefinition.attribute())) {
			builder.append(createValueString(entity.original(attributeDefinition.attribute()), (AttributeDefinition<Object>) attributeDefinition));
			builder.append(" → ");
		}
		builder.append(createValueString(entity.get(attributeDefinition.attribute()), (AttributeDefinition<Object>) attributeDefinition));

		return builder.toString();
	}

	private static String createValueString(Object value, AttributeDefinition<Object> attributeDefinition) {
		String valueAsString = value == null ? "<null>" : attributeDefinition.string(value);
		if (valueAsString.length() > MAXIMUM_VALUE_LENGTH) {
			valueAsString = valueAsString.substring(0, MAXIMUM_VALUE_LENGTH) + "...";
		}

		return valueAsString;
	}

	private static void configureMenuItem(JMenuItem menuItem, Entity entity, Attribute<?> attribute, String toolTipText) {
		Font currentFont = menuItem.getFont();
		if (!valid(entity, attribute)) {
			menuItem.setForeground(Color.RED);
			menuItem.setFont(new Font(currentFont.getName(), Font.BOLD, currentFont.getSize()));
		}
		if (entity.modified(attribute)) {
			menuItem.setFont(new Font(currentFont.getName(), currentFont.getStyle() | Font.ITALIC, currentFont.getSize()));
		}
		menuItem.setToolTipText(toolTipText);
	}

	private static boolean valid(Entity entity, Attribute<?> attribute) {
		try {
			entity.definition().validator().validate(entity, attribute);
			return true;
		}
		catch (ValidationException e) {
			return false;
		}
	}

	private static Entity populateEntityGraph(Entity entity, EntityConnection connection, Set<ForeignKeyEntity> visited) {
		for (ForeignKey foreignKey : entity.definition().foreignKeys().get()) {
			Entity.Key referencedKey = entity.referencedKey(foreignKey);
			if (entity.isNotNull(foreignKey)) {
				ForeignKeyEntity foreignKeyEntity = new ForeignKeyEntity(foreignKey, selectEntity(referencedKey, connection));
				if (visited.contains(foreignKeyEntity)) {
					entity.put(foreignKey, duplicateEntity(foreignKeyEntity.referencedEntity));
				}
				else {
					visited.add(foreignKeyEntity);
					entity.put(foreignKey, foreignKeyEntity.referencedEntity);
					populateEntityGraph(foreignKeyEntity.referencedEntity, connection, visited);
				}
			}
		}

		return entity;
	}

	private static Entity selectEntity(Entity.Key primaryKey, EntityConnection connection) {
		try {
			return connection.selectSingle(where(key(primaryKey))
							.fetchDepth(0)
							.build());
		}
		catch (RecordNotFoundException e) {
			return ProxyBuilder.builder(Entity.class)
							.delegate(Entity.entity(primaryKey))
							.method("toString", parameters -> primaryKey.toString() + " <RECORD NOT FOUND>")
							.build();
		}
		catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
	}

	private static Entity duplicateEntity(Entity referencedEntity) {
		return ProxyBuilder.builder(Entity.class)
						.delegate(referencedEntity)
						.method("toString", parameters -> referencedEntity + " <DUPLICATE>")
						.build();
	}

	private static Control clipboardControl(Entity entity, Attribute<?> attribute) {
		return control(new ClipboardCommand(entity, attribute));
	}

	private static final class ClipboardCommand implements Control.Command {

		private final Entity entity;
		private final Attribute<?> attribute;

		private ClipboardCommand(Entity entity, Attribute<?> attribute) {
			this.entity = entity;
			this.attribute = attribute;
		}

		@Override
		public void execute() throws Exception {
			setClipboard(entity.string(attribute));
		}
	}

	private static final class ForeignKeyEntity {

		private final ForeignKey foreignKey;
		private final Entity referencedEntity;

		private ForeignKeyEntity(ForeignKey foreignKey, Entity referencedEntity) {
			this.foreignKey = foreignKey;
			this.referencedEntity = referencedEntity;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof ForeignKeyEntity)) {
				return false;
			}
			ForeignKeyEntity that = (ForeignKeyEntity) object;

			return Objects.equals(foreignKey, that.foreignKey) && Objects.equals(referencedEntity, that.referencedEntity);
		}

		@Override
		public int hashCode() {
			return Objects.hash(foreignKey, referencedEntity);
		}
	}
}
