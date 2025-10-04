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
package is.codion.swing.framework.ui;

import is.codion.common.model.CancelException;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.ui.Utilities;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class EntityTableExport {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTableExportPanel.class, getBundle(EntityTableExportPanel.class.getName()));

	private static final String TAB = "\t";
	private static final String SPACE = " ";
	private static final String TSV = ".tsv";

	private final EntityTableModel<?> tableModel;
	private final EntityConnectionProvider connectionProvider;
	private final EntityNode entityNode;
	private final State selected;

	EntityTableExport(EntityTableModel<?> tableModel) {
		this.tableModel = tableModel;
		this.connectionProvider = tableModel.connectionProvider();
		this.entityNode = new EntityNode(tableModel.entityDefinition(), connectionProvider.entities());
		this.selected = State.state(!tableModel.selection().empty().is());
		tableModel.selection().empty().addConsumer(empty -> selected.set(!empty));
	}

	ExportTask exportToClipboard() {
		return new ExportToClipboard();
	}

	ExportTask exportToFile(Path file) {
		return new ExportToFileTask(file);
	}

	DefaultMutableTreeNode entityNode() {
		return entityNode;
	}

	State selected() {
		return selected;
	}

	String defaultFileName() {
		return tableModel.entityDefinition().caption() + TSV;
	}

	private List<String> createHeader() {
		return addToHeader(entityNode.children(), new ArrayList<>(), "");
	}

	private static List<String> addToHeader(Enumeration<TreeNode> nodes, List<String> header, String prefix) {
		while (nodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) nodes.nextElement();
			String caption = node.definition.caption();
			String columnHeader = prefix.isEmpty() ? caption : (prefix + SPACE + caption);
			if (node.selected.is()) {
				header.add(columnHeader);
			}
			if (node.definition.attribute() instanceof ForeignKey) {
				addToHeader(node.children(), header, columnHeader);
			}
		}

		return header;
	}

	private List<String> createRow(Entity entity) {
		return addToRow(entityNode.children(), entity.primaryKey(),
						new ArrayList<>(), new HashMap<>(), connectionProvider.connection());
	}

	private static List<String> addToRow(Enumeration<TreeNode> attributeNodes, Entity.Key key, List<String> row,
																			 Map<Entity.Key, Entity> cache, EntityConnection connection) {
		Entity entity = cache.computeIfAbsent(key, k -> connection.select(key));
		while (attributeNodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) attributeNodes.nextElement();
			Attribute<?> attribute = node.definition.attribute();
			if (node.selected.is()) {
				row.add(replaceNewlinesAndTabs(entity.formatted(attribute)));
			}
			if (attribute instanceof ForeignKey) {
				Entity.Key referencedKey = entity.key((ForeignKey) attribute);
				if (referencedKey != null) {
					addToRow(node.children(), referencedKey, row, cache, connection);
				}
			}
		}

		return row;
	}

	private static String replaceNewlinesAndTabs(String string) {
		return string.replace("\r\n", SPACE)
						.replace("\n", SPACE)
						.replace("\r", SPACE)
						.replace(TAB, SPACE);
	}

	abstract class ExportTask implements ProgressTask<Void> {

		protected final AtomicInteger counter = new AtomicInteger();
		protected final State cancelled = State.state(false);
		protected final List<Entity> entities;

		protected ExportTask() {
			entities = selected.is() ?
							tableModel.selection().items().get() :
							tableModel.items().included().get();
		}

		@Override
		public final int maximum() {
			return entities.size();
		}

		final State cancelled() {
			return cancelled;
		}

		abstract String successMessage();
	}

	private final class ExportToFileTask extends ExportTask {

		private final Path file;

		private ExportToFileTask(Path file) {
			this.file = file;
		}

		@Override
		public void execute(ProgressReporter<Void> progress) throws Exception {
			try (BufferedWriter output = Files.newBufferedWriter(file)) {
				output.write(createHeader().stream()
								.collect(joining(TAB, "", lineSeparator())));
				for (Entity entity : entities) {
					if (cancelled.is()) {
						throw new CancelException();
					}
					output.write(join(TAB, createRow(entity)));
					output.write(lineSeparator());
					progress.report(counter.incrementAndGet());
				}
			}
		}

		@Override
		String successMessage() {
			return MESSAGES.getString("copied_to_file") + ": " + file;
		}
	}

	private final class ExportToClipboard extends ExportTask {

		@Override
		public void execute(ProgressReporter<Void> progress) {
			String result = entities.stream()
							.map(entity -> createLine(entity, progress))
							.map(line -> join(TAB, line))
							.collect(joining(lineSeparator(), createHeader().stream()
											.collect(joining(TAB, "", lineSeparator())), ""));
			if (cancelled.is()) {
				throw new CancelException();
			}

			Utilities.setClipboard(result);
		}

		@Override
		String successMessage() {
			return MESSAGES.getString("copied_to_clipboard");
		}

		private List<String> createLine(Entity entity, ProgressReporter<Void> progress) {
			if (cancelled.is()) {
				throw new CancelException();
			}
			List<String> row = createRow(entity);
			progress.report(counter.incrementAndGet());

			return row;
		}
	}

	private static final class EntityNode extends DefaultMutableTreeNode {

		private static final AttributeDefinitionComparator ATTRIBUTE_COMPARATOR = new AttributeDefinitionComparator();

		private EntityNode(EntityDefinition definition, Entities entities) {
			populate(this, definition, entities, new HashSet<>());
		}

		private static void populate(DefaultMutableTreeNode parent, EntityDefinition definition,
																 Entities entities, Set<ForeignKey> visited) {
			for (AttributeDefinition<?> attributeDefinition : definition.attributes().definitions()
							.stream()
							.sorted(ATTRIBUTE_COMPARATOR)
							.collect(toList())) {
				if (attributeDefinition instanceof ForeignKeyDefinition) {
					ForeignKeyDefinition foreignKeyDefinition = (ForeignKeyDefinition) attributeDefinition;
					ForeignKey foreignKey = foreignKeyDefinition.attribute();
					AttributeNode foreignKeyNode = new AttributeNode(foreignKeyDefinition);
					parent.add(foreignKeyNode);
					if (!visited.contains(foreignKey)) {
						visited.add(foreignKey);
						populate(foreignKeyNode, entities.definition(foreignKey.referencedType()), entities, visited);
					}
				}
				else if (!attributeDefinition.hidden()) {
					parent.add(new AttributeNode(attributeDefinition));
				}
			}
		}

		private static final class AttributeDefinitionComparator implements Comparator<AttributeDefinition<?>> {

			private final Collator collator = Collator.getInstance();

			@Override
			public int compare(AttributeDefinition<?> definition1, AttributeDefinition<?> definition2) {
				return collator.compare(definition1.caption().toLowerCase(), definition2.caption().toLowerCase());
			}
		}
	}

	static class AttributeNode extends DefaultMutableTreeNode {

		private final AttributeDefinition<?> definition;
		private final State selected = State.state();

		private AttributeNode(AttributeDefinition<?> definition) {
			this.definition = definition;
		}

		AttributeDefinition<?> definition() {
			return definition;
		}

		State selected() {
			return selected;
		}

		@Override
		public String toString() {
			return selected.is() ? "+" + definition.caption() : definition.caption() + "  ";
		}
	}
}
