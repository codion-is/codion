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

import is.codion.common.db.database.Database;
import is.codion.common.reactive.value.Value;
import is.codion.framework.db.EntityQueries;
import is.codion.framework.model.EntityEditor.EditorEntity;
import is.codion.framework.model.EntityEditor.EditorValue;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.tabbedpane.TabbedPaneBuilder;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.framework.model.SwingEntityEditor;
import is.codion.swing.framework.ui.EditorComponents.DetailComponents;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentListener;
import java.awt.Font;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.BorderLayout.CENTER;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static javax.swing.SwingConstants.HORIZONTAL;
import static javax.swing.SwingUtilities.invokeLater;

final class EditorInspector extends JPanel {

	private static final Logger LOG = LoggerFactory.getLogger(EditorInspector.class);

	private final SwingEntityEditor editor;

	private final @Nullable EntityQueries queries;
	private final Value<String> insertQuery;
	private final Value<String> updateQuery;

	EditorInspector(EditorComponents components) {
		this.editor = components.editor();
		this.queries = createQueries();
		this.insertQuery = Value.nullable(createInsertQuery());
		this.updateQuery = Value.nullable(createUpdateQuery());
		editor.values().changed().addListener(this::refreshQuery);
		editor.entity().replaced().addListener(this::refreshQuery);
		editor.entity().addListener(this::refreshQuery);
		TabbedPaneBuilder tabbedPane = tabbedPane()
						.tab("Editor", new EditorComponentsPanel(components));
		if (queries == null) {
			LOG.debug("No EntityQueries.Factory available");
		}
		else {
			tabbedPane.tab("Queries", createQueriesPanel());
		}
		setLayout(borderLayout());
		add(tabbedPane.build(), CENTER);
	}

	private JPanel createQueriesPanel() {
		return gridLayoutPanel(2, 1)
						.border(emptyBorder())
						.add(scrollPane()
										.view(queryTextArea(insertQuery)))
						.add(scrollPane()
										.view(queryTextArea(updateQuery)))
						.build();
	}

	private void refreshQuery() {
		insertQuery.set(createInsertQuery());
		updateQuery.set(createUpdateQuery());
	}

	private String createInsertQuery() {
		return queries == null ? "" : queries.insert(editor.entity().get());
	}

	private String createUpdateQuery() {
		if (editor.entity().modified().is()) {
			return queries == null ? "" : queries.update(editor.entity().get());
		}

		return "<unmodified>";
	}

	private @Nullable EntityQueries createQueries() {
		try {
			return EntityQueries.factory()
							.map(factory -> factory.create(Database.instance(), editor.connectionProvider().connection().entities()))
							.orElse(null);
		}
		catch (Exception e) {
			LOG.debug("Unable to initialize EntityQueries", e);
			return null;
		}
	}

	private static JTextArea queryTextArea(Value<String> insertQuery) {
		return textArea()
						.link(insertQuery)
						.rowsColumns(5, 20)
						.editable(false)
						.documentListener(EditorInspector::resetCaretPosition)
						.font(EditorInspector::monospaced)
						.build();
	}

	private static DocumentListener resetCaretPosition(JTextArea textArea) {
		return (DocumentAdapter) e -> invokeLater(() -> textArea.setCaretPosition(0));
	}

	private static Font monospaced(Font font) {
		return new Font(Font.MONOSPACED, font.getStyle(), font.getSize());
	}

	private static final class EditorComponentsPanel extends JPanel {

		private EditorComponentsPanel(EditorComponents components) {
			super(borderLayout());
			FilterTableModel<AttributeRow, AttributeColumn> attributeModel =
							FilterTableModel.builder()
											.columns(new AttributeColumns())
											.items(new AttributeItems(components))
											.refresh(true)
											.build();
			components.editor().values().changed().addListener(attributeModel.items()::refresh);
			TabbedPaneBuilder detailPanelsBuilder = tabbedPane();
			for (DetailComponents detail : components.detail().components()) {
				String title = detail.components().editor().entityDefinition().caption() + " " + detail.caption();
				detailPanelsBuilder.tab(title, new EditorInspector(detail.components()));
			}
			EditorEntity entity = components.editor().entity();
			JPanel mainPanel = borderLayoutPanel()
							.border(emptyBorder())
							.north(gridLayoutPanel(1, 4)
											.add(checkBox()
															.link(entity.exists())
															.text("Exists")
															.enabled(false))
											.add(checkBox()
															.link(entity.modified())
															.text("Modified")
															.enabled(false))
											.add(checkBox()
															.link(entity.valid())
															.text("Valid")
															.enabled(false))
											.add(checkBox()
															.link(entity.present())
															.text("Present")
															.enabled(false))
											.build())
							.center(FilterTable.builder()
											.model(attributeModel)
											.visibleRows(new AttributeItems(components).components.editor().entityDefinition().attributes().definitions().size())
											.columns(EditorComponentsPanel::columns)
											.autoResizeMode(JTable.AUTO_RESIZE_OFF)
											.fitColumnHeaders(true)
											.scrollPane()
											.build())
							.build();
			JTabbedPane detailsPane = detailPanelsBuilder.build();
			if (detailsPane.getTabCount() > 0) {
				add(splitPane()
								.orientation(HORIZONTAL)
								.continuousLayout(true)
								.topComponent(mainPanel)
								.bottomComponent(detailsPane)
								.build(), CENTER);
			}
			else {
				add(mainPanel, CENTER);
			}
		}

		private static void columns(FilterTableColumn.Builder<AttributeColumn> column) {
			switch (column.identifier()) {
				case VALUE:
				case ORIGINAL:
					column.cellRenderer(FilterTableCellRenderer.builder()
									.columnClass(String.class)
									.horizontalAlignment(SwingConstants.CENTER)
									.toolTip(Objects::toString)
									.build());
					break;
				case MESSAGE:
					column.cellRenderer(FilterTableCellRenderer.builder()
									.columnClass(String.class)
									.toolTip(Objects::toString)
									.build());
					break;
				default:
					break;
			}
		}

		private enum AttributeColumn {
			ATTRIBUTE, PRESENT, VALID, MODIFIED, PERSISTS, COMPONENT, VALUE, ORIGINAL, DEFAULT, MESSAGE
		}

		private static final class AttributeItems implements Supplier<Collection<AttributeRow>> {

			private final EditorComponents components;

			private AttributeItems(EditorComponents components) {
				this.components = components;
			}

			@Override
			public Collection<AttributeRow> get() {
				return components.editor().entityDefinition().attributes().definitions().stream()
								.map(attribute -> {
									EditorValue<?> value = components.editor().value(attribute.attribute());

									return new AttributeRow(attribute.caption(), value.present().is(), value.valid().is(), value.modified().is(),
													value.persist().is(), components.component(attribute.attribute()).optional().isPresent(),
													String.valueOf(value.get()), value.modified().is() ? String.valueOf(value.original()) : "",
													String.valueOf(value.defaultValue().get().get()), value.message().get());
								})
								.collect(toList());
			}
		}

		private static final class AttributeColumns implements TableColumns<AttributeRow, AttributeColumn> {

			private static final List<AttributeColumn> IDENTIFIERS = asList(AttributeColumn.values());

			@Override
			public List<AttributeColumn> identifiers() {
				return IDENTIFIERS;
			}

			@Override
			public Class<?> columnClass(AttributeColumn identifier) {
				switch (identifier) {
					case ATTRIBUTE:
					case VALUE:
					case ORIGINAL:
					case DEFAULT:
					case MESSAGE:
						return String.class;
					default:
						return Boolean.class;
				}
			}

			@Override
			public String caption(AttributeColumn identifier) {
				switch (identifier) {
					case ATTRIBUTE:
						return "Attribute";
					case PRESENT:
						return "Present";
					case VALID:
						return "Valid";
					case MODIFIED:
						return "Modified";
					case PERSISTS:
						return "Persists";
					case VALUE:
						return "Value";
					case ORIGINAL:
						return "Original";
					case DEFAULT:
						return "Default";
					case MESSAGE:
						return "Message";
					case COMPONENT:
						return "Component";
					default:
						throw new IllegalArgumentException();
				}
			}

			@Override
			public @Nullable Object value(AttributeRow row, AttributeColumn identifier) {
				switch (identifier) {
					case ATTRIBUTE:
						return row.attribute;
					case PRESENT:
						return row.present;
					case VALID:
						return row.valid;
					case MODIFIED:
						return row.modified;
					case PERSISTS:
						return row.persists;
					case VALUE:
						return row.value;
					case ORIGINAL:
						return row.original;
					case DEFAULT:
						return row.defaultValue;
					case MESSAGE:
						return row.message;
					case COMPONENT:
						return row.component;
					default:
						throw new IllegalArgumentException();
				}
			}
		}

		private static final class AttributeRow {

			private final String attribute;
			private final boolean present;
			private final boolean valid;
			private final boolean modified;
			private final boolean persists;
			private final boolean component;
			private final @Nullable String value;
			private final @Nullable String original;
			private final @Nullable String defaultValue;
			private final @Nullable String message;

			private AttributeRow(String attribute, boolean present, boolean valid, boolean modified,
													 boolean persists, boolean component, @Nullable String value,
													 @Nullable String original, @Nullable String defaultValue, @Nullable String message) {
				this.attribute = attribute;
				this.present = present;
				this.valid = valid;
				this.modified = modified;
				this.persists = persists;
				this.component = component;
				this.value = value;
				this.original = original;
				this.defaultValue = defaultValue;
				this.message = message;
			}

			@Override
			public boolean equals(Object object) {
				if (object == null || getClass() != object.getClass()) {
					return false;
				}
				AttributeRow that = (AttributeRow) object;

				return Objects.equals(attribute, that.attribute);
			}

			@Override
			public int hashCode() {
				return Objects.hashCode(attribute);
			}
		}
	}
}
