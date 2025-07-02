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
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityTableModel;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
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
import java.util.stream.Stream;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.button.ToggleButtonType.RADIO_BUTTON;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createTitledBorder;

final class EntityTableExport {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTableExport.class, getBundle(EntityTableExport.class.getName()));

	private static final String TAB = "\t";
	private static final String SPACE = " ";

	private final EntityConnectionProvider connectionProvider;
	private final FilterTableColumnModel<Attribute<?>> columnModel;
	private final EntityNode entityNode;
	private final State selectedRows;
	private final EntityTablePanel tablePanel;

	private ConfigurationPanel configurationPanel;

	EntityTableExport(EntityTablePanel tablePanel, FilterTableColumnModel<Attribute<?>> columnModel) {
		this.tablePanel = tablePanel;
		SwingEntityTableModel tableModel = tablePanel.tableModel();
		this.connectionProvider = tableModel.connectionProvider();
		this.entityNode = new EntityNode(tableModel.entityDefinition(), connectionProvider.entities());
		this.columnModel = columnModel;
		this.selectedRows = State.state(!tableModel.selection().empty().get());
		tableModel.selection().empty().addConsumer(selectionEmpty ->
						selectedRows.set(!selectionEmpty));
	}

	void exportToClipboard() {
		if (configurationPanel == null) {
			configurationPanel = new ConfigurationPanel();
		}

		Dialogs.okCancel()
						.component(configurationPanel)
						.owner(tablePanel)
						.title(MESSAGES.getString("export"))
						.onOk(this::export)
						.show();
	}

	private void export() {
		List<Entity> entities = entities(tablePanel.tableModel());
		Dialogs.<String, Void>progressWorker(progress -> export(progress, entities))
						.owner(tablePanel)
						.title(MESSAGES.getString("exporting_rows"))
						.maximum(entities.size())
						.onResult(Utilities::setClipboard)
						.execute();
	}

	private String export(ProgressReporter<Void> progressReporter, List<Entity> entities) {
		AtomicInteger counter = new AtomicInteger();

		return entities.stream()
						.map(entity -> {
							List<String> row = createRow(entity);
							progressReporter.report(counter.incrementAndGet());

							return row;
						})
						.map(line -> join(TAB, line))
						.collect(joining(lineSeparator(), createHeader().stream()
										.collect(joining(TAB, "", lineSeparator())), ""));
	}

	private List<Entity> entities(SwingEntityTableModel tableModel) {
		return selectedRows.get() ?
						tableModel.selection().items().get() :
						tableModel.items().visible().get();
	}

	private List<String> createHeader() {
		return addToHeader(entityNode.children(), new ArrayList<>(), "");
	}

	private List<String> createRow(Entity entity) {
		return addToRow(entityNode.children(), entity.primaryKey(),
						new ArrayList<>(), new HashMap<>(), connectionProvider.connection());
	}

	private static List<String> addToHeader(Enumeration<TreeNode> nodes, List<String> header, String prefix) {
		while (nodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) nodes.nextElement();
			String caption = node.definition.caption();
			String columnHeader = prefix.isEmpty() ? caption : (prefix + SPACE + caption);
			if (node.selected.get()) {
				header.add(columnHeader);
			}
			if (node.definition.attribute() instanceof ForeignKey) {
				addToHeader(node.children(), header, columnHeader);
			}
		}

		return header;
	}

	private static List<String> addToRow(Enumeration<TreeNode> attributeNodes, Entity.Key key, List<String> row,
																			 Map<Entity.Key, Entity> cache, EntityConnection connection) {
		Entity entity = cache.computeIfAbsent(key, k -> connection.select(key));
		while (attributeNodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) attributeNodes.nextElement();
			Attribute<?> attribute = node.definition.attribute();
			if (node.selected.get()) {
				row.add(replaceNewlinesAndTabs(entity.string(attribute)));
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

	private final class ConfigurationPanel extends JPanel {

		private final JTree exportTree = createTree();
		private final CommandControl selectDefaultsControl = Control.builder()
						.command(this::selectDefaults)
						.caption(MESSAGES.getString("default_columns"))
						.mnemonic(MESSAGES.getString("default_columns_mnemonic").charAt(0))
						.build();
		private final CommandControl selectAllControl = Control.builder()
						.command(() -> select(true))
						.caption(MESSAGES.getString("columns_all"))
						.mnemonic(MESSAGES.getString("columns_all_mnemonic").charAt(0))
						.build();
		private final CommandControl selectNoneControl = Control.builder()
						.command(() -> select(false))
						.caption(MESSAGES.getString("columns_none"))
						.mnemonic(MESSAGES.getString("columns_none_mnemonic").charAt(0))
						.build();
		private final ToggleControl allRowsControl = Control.builder()
						.toggle(State.state())
						.caption(MESSAGES.getString("rows_all"))
						.mnemonic(MESSAGES.getString("rows_all_mnemonic").charAt(0))
						.build();
		private final ToggleControl selectedRowsControl = Control.builder()
						.toggle(selectedRows)
						.caption(MESSAGES.getString("rows_selected"))
						.mnemonic(MESSAGES.getString("rows_selected_mnemonic").charAt(0))
						.enabled(tablePanel.tableModel().selection().empty().not())
						.build();

		private ConfigurationPanel() {
			super(borderLayout());
			selectDefaults();
			add(borderLayoutPanel()
							.border(emptyBorder())
							.centerComponent(borderLayoutPanel()
											.border(createTitledBorder(MESSAGES.getString("columns")))
											.centerComponent(scrollPane(exportTree).build())
											.southComponent(borderLayoutPanel()
															.eastComponent(buttonPanel(selectDefaultsControl, selectAllControl, selectNoneControl)
																			.transferFocusOnEnter(true)
																			.build())
															.build())
											.build())
							.southComponent(borderLayoutPanel()
											.border(createTitledBorder(MESSAGES.getString("rows")))
											.centerComponent(borderLayoutPanel()
															.eastComponent(buttonPanel(allRowsControl, selectedRowsControl)
																			.toggleButtonType(RADIO_BUTTON)
																			.buttonGroup(new ButtonGroup())
																			.fixedButtonSize(false)
																			.transferFocusOnEnter(true)
																			.build())
															.build())
											.build())
							.build(), BorderLayout.CENTER);
		}

		private JTree createTree() {
			JTree tree = new JTree(entityNode);
			tree.setShowsRootHandles(true);
			tree.setRootVisible(false);
			KeyEvents.builder(VK_SPACE)
							.action(command(this::toggleSelected))
							.enable(tree);

			return tree;
		}

		private void selectDefaults() {
			select(false);
			Enumeration<TreeNode> children = entityNode.children();
			while (children.hasMoreElements()) {
				TreeNode child = children.nextElement();
				if (child instanceof AttributeNode &&
								columnModel.visible(((AttributeNode) child).definition.attribute()).get()) {
					((AttributeNode) child).selected.set(true);
				}
			}
		}

		private void select(boolean select) {
			Enumeration<TreeNode> enumeration = entityNode.breadthFirstEnumeration();
			enumeration.nextElement();// root
			while (enumeration.hasMoreElements()) {
				((AttributeNode) enumeration.nextElement()).selected.set(select);
			}
			exportTree.repaint();
		}

		private void toggleSelected() {
			TreePath[] selectionPaths = exportTree.getSelectionPaths();
			if (selectionPaths != null) {
				Stream.of(selectionPaths)
								.filter(exportTree::isPathSelected)
								.map(TreePath::getLastPathComponent)
								.map(AttributeNode.class::cast)
								.forEach(node -> node.selected.set(!node.selected.get()));
				exportTree.repaint();
			}
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
				return collator.compare(definition1.toString().toLowerCase(), definition2.toString().toLowerCase());
			}
		}
	}

	private static class AttributeNode extends DefaultMutableTreeNode {

		private final AttributeDefinition<?> definition;
		private final State selected = State.state();

		AttributeNode(AttributeDefinition<?> definition) {
			this.definition = definition;
		}

		@Override
		public String toString() {
			return selected.get() ? "+" + definition.caption() : definition.caption() + "  ";
		}
	}
}
