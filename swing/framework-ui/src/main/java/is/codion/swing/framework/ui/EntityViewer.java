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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.dialog.Dialogs;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.condition.Condition.key;
import static is.codion.swing.common.ui.component.Components.scrollPane;
import static java.util.Objects.requireNonNull;

final class EntityViewer {

	private EntityViewer() {}

	static void view(Entity.Key key, EntityConnectionProvider connectionProvider, JComponent dialogOwner) {
		requireNonNull(key);
		requireNonNull(connectionProvider);
		view(select(key, connectionProvider), connectionProvider, dialogOwner);
	}

	static void view(Entity entity, EntityConnectionProvider connectionProvider, JComponent dialogOwner) {
		requireNonNull(entity);
		requireNonNull(connectionProvider);
		EntityDefinition definition = connectionProvider.entities().definition(entity.type());
		Dialogs.builder()
						.component(scrollPane()
										.view(createTree(entity, connectionProvider)))
						.owner(dialogOwner)
						.title(definition.caption() + " - " + entity)
						.modal(false)
						.show();
	}

	static JTree createTree(Entity entity, EntityConnectionProvider connectionProvider) {
		return Components.tree()
						.model(new EntityTreeModel(entity, connectionProvider))
						.showsRootHandles(true)
						.rootVisible(false)
						.treeWillExpandListener(new EntityTreeWillExpandListener())
						.build();
	}

	private static Entity select(Entity.Key primaryKey, EntityConnectionProvider connectionProvider) {
		return connectionProvider.connection().selectSingle(where(key(primaryKey))
						.referenceDepth(1)
						.build());
	}

	private static final class EntityTreeModel extends DefaultTreeModel {

		private static final int MAXIMUM_VALUE_LENGTH = 42;

		private EntityTreeModel(Entity entity, EntityConnectionProvider connectionProvider) {
			super(new DefaultMutableTreeNode());
			populate(entity, connectionProvider, (DefaultMutableTreeNode) getRoot());
		}

		private static void populate(Entity entity, EntityConnectionProvider connectionProvider, DefaultMutableTreeNode node) {
			connectionProvider.entities().definition(entity.type()).attributes().definitions().forEach(attributeDefinition -> {
				if (attributeDefinition instanceof ForeignKeyDefinition) {
					ForeignKeyDefinition foreignKeyDefinition = (ForeignKeyDefinition) attributeDefinition;
					Entity reference = entity.get(foreignKeyDefinition.attribute());
					if (reference != null) {
						node.add(new ForeignKeyNode(foreignKeyDefinition, entity, connectionProvider));
					}
					else {
						node.add(new AttributeNode(attributeDefinition, entity));
					}
				}
				else {
					node.add(new AttributeNode(attributeDefinition, entity));
				}
			});
		}

		private static class AttributeNode extends DefaultMutableTreeNode {

			private final Entity entity;
			private final AttributeDefinition<?> attributeDefinition;

			private AttributeNode(AttributeDefinition<?> attributeDefinition, Entity entity) {
				this.entity = entity;
				this.attributeDefinition = attributeDefinition;
			}

			@Override
			public final String toString() {
				return new StringBuilder(attributeDefinition.toString())
								.append(" [").append(attributeDefinition.attribute().type().valueClass().getSimpleName())
								.append(attributeDefinition.derived() ? "*" : "").append("]: ")
								.append(createValueString(entity, attributeDefinition, true)).toString();
			}

			protected AttributeDefinition<?> attributeDefinition() {
				return attributeDefinition;
			}

			protected final Entity entity() {
				return entity;
			}

			private static String createValueString(Entity entity, AttributeDefinition<?> attributeDefinition, boolean trim) {
				StringBuilder builder = new StringBuilder();
				if (entity.modified(attributeDefinition.attribute())) {
					builder.append(createValueString(entity.original(attributeDefinition.attribute()), (AttributeDefinition<Object>) attributeDefinition, trim));
					builder.append(" → ");
				}
				builder.append(createValueString(entity.get(attributeDefinition.attribute()), (AttributeDefinition<Object>) attributeDefinition, trim));

				return builder.toString();
			}

			private static String createValueString(@Nullable Object value, AttributeDefinition<Object> attributeDefinition, boolean trim) {
				String formatted = value == null ? "<null>" : attributeDefinition.format(value);
				if (trim && formatted.length() > MAXIMUM_VALUE_LENGTH) {
					formatted = formatted.substring(0, MAXIMUM_VALUE_LENGTH) + "...";
				}
				if (attributeDefinition.attribute().type().isString() && value != null) {
					formatted = "\"" + formatted + "\"";
				}

				return formatted;
			}
		}

		private static final class ForeignKeyNode extends AttributeNode {

			private final EntityConnectionProvider connectionProvider;

			private boolean populated;

			private ForeignKeyNode(ForeignKeyDefinition foreignKeyDefinition, Entity entity, EntityConnectionProvider connectionProvider) {
				super(foreignKeyDefinition, entity);
				this.connectionProvider = connectionProvider;
			}

			@Override
			public boolean isLeaf() {
				return entity().isNull(attributeDefinition().attribute());
			}

			@Override
			protected ForeignKeyDefinition attributeDefinition() {
				return (ForeignKeyDefinition) super.attributeDefinition();
			}

			private void populate() {
				if (!populated) {
					Entity.Key referencedKey = entity().key(attributeDefinition().attribute());
					if (referencedKey != null) {
						EntityTreeModel.populate(select(referencedKey, connectionProvider), connectionProvider, this);
					}
					populated = true;
				}
			}
		}
	}

	private static final class EntityTreeWillExpandListener implements TreeWillExpandListener {

		@Override
		public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
			Object node = event.getPath().getLastPathComponent();
			if (node instanceof EntityTreeModel.ForeignKeyNode) {
				((EntityTreeModel.ForeignKeyNode) node).populate();
			}
		}

		@Override
		public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {}
	}
}
