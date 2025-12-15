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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.notes;

import is.codion.common.db.database.Database;
import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;
import is.codion.dbms.h2.H2DatabaseFactory;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Generator;
import is.codion.framework.model.EntityEditModel.EntityEditor;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialDarker;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityApplication;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static is.codion.swing.framework.ui.EntityEditPanel.ControlKeys.CLEAR;
import static java.util.Collections.emptyList;

// tag::notes[]
public final class NotesDemo {

	private static final List<String> CREATE_SCHEMA_STATEMENTS = List.of(
					"create schema notes",
					"create table notes.note(" +
									"id identity not null, " +
									"note text not null, " +
									"created timestamp default now() not null, " +
									"updated timestamp)"
	);

	private static final DomainType DOMAIN = DomainType.domainType("notes");

	// The domain model API.
	interface Note {
		EntityType TYPE = DOMAIN.entityType("notes.note");

		Column<Long> ID = TYPE.longColumn("id");
		Column<String> NOTE = TYPE.stringColumn("note");
		Column<LocalDateTime> CREATED = TYPE.localDateTimeColumn("created");
		Column<LocalDateTime> UPDATED = TYPE.localDateTimeColumn("updated");
	}

	// The domain model implementation
	private static class Notes extends DomainModel {

		private Notes() {
			super(DOMAIN);
			add(Note.TYPE.as(
											Note.ID.as()
															.primaryKey()
															.generator(Generator.identity()),
											Note.NOTE.as()
															.column()
															.caption("Note")
															.nullable(false),
											Note.CREATED.as()
															.column()
															.caption("Created")
															.nullable(false)
															.updatable(false)
															.withDefault(true),
											Note.UPDATED.as()
															.column()
															.caption("Updated"))
							.orderBy(OrderBy.descending(Note.CREATED))
							.caption("Notes")
							.build());
		}
	}

	private static final class NoteEditModel extends SwingEntityEditModel {

		private NoteEditModel(EntityConnectionProvider connectionProvider) {
			super(Note.TYPE, connectionProvider);
			// Set the Note.UPDATED value before we perform an update
			beforeUpdate().addConsumer(notes ->
							notes.forEach(note -> note.set(Note.UPDATED, LocalDateTime.now())));
		}
	}

	private static final class NoteEditPanel extends EntityEditPanel {

		private NoteEditPanel(SwingEntityEditModel editModel) {
			super(editModel);
		}

		@Override
		protected void initializeUI() {
			focus().initial().set(Note.NOTE);

			JTextField noteField = createTextField(Note.NOTE)
							.hint("Take note...")
							// Only a single input field, no need for focus transfer
							.transferFocusOnEnter(false)
							// Use CTRL-Enter for inserting, updating and
							// deleting, depending on the edit model state
							.keyEvent(KeyEvents.builder()
											.keyCode(KeyEvent.VK_ENTER)
											.modifiers(KeyEvents.MENU_SHORTCUT_MASK)
											.action(Control.command(this::insertDeleteOrUpdate)))
							.build();

			setLayout(Layouts.borderLayout());
			add(noteField, BorderLayout.CENTER);
			// Add a button based on the CLEAR control
			add(new JButton(control(CLEAR).get()), BorderLayout.EAST);
		}

		private void insertDeleteOrUpdate() {
			EntityEditor editor = editModel().editor();
			if (editor.exists().not().is() && !editor.value(Note.NOTE).isNull()) {
				// A new note with a non-null text
				insertCommand()
								.execute();
			}
			else if (editor.modified().is()) {
				if (editor.value(Note.NOTE).isNull()) {
					// An existing note with no text
					deleteCommand()
									.confirm(false)
									.execute();
				}
				else {
					// An existing note with a modified text
					updateCommand()
									.confirm(false)
									.execute();
				}
			}
		}
	}

	private static final class NoteTableModel extends SwingEntityTableModel {

		private NoteTableModel(EntityConnectionProvider connectionProvider) {
			super(new NoteEditModel(connectionProvider));
		}
	}

	private static final class NoteTablePanel extends EntityTablePanel {

		private NoteTablePanel(SwingEntityTableModel tableModel) {
			super(tableModel, config -> config
							// Exclude the Note.UPDATED attribute from the Edit popup menu since
							// the value is set automatically and shouldn't be editable via the UI.
							// Note.CREATED is excluded by default since it is not updatable.
							.editable(attributes -> attributes.remove(Note.UPDATED)));
			// Configure the table and columns
			table().model().sort().descending(Note.CREATED);
			table().setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			FilterTableColumnModel<Attribute<?>> columnModel = table().columnModel();
			columnModel.column(Note.NOTE).setPreferredWidth(280);
			columnModel.column(Note.CREATED).setPreferredWidth(130);
			columnModel.column(Note.UPDATED).setPreferredWidth(130);
		}
	}

	private static final class NoteModel extends SwingEntityModel {

		private NoteModel(EntityConnectionProvider connectionProvider) {
			super(new NoteTableModel(connectionProvider));
		}
	}

	private static final class NotePanel extends EntityPanel {

		private NotePanel(SwingEntityModel noteModel) {
			super(noteModel,
							new NoteEditPanel(noteModel.editModel()),
							new NoteTablePanel(noteModel.tableModel()), config -> config
											// No need to include the default control buttons since
											// we added the CLEAR control button to the edit panel
											.includeControls(false)
											// No need for a edit base panel, since we want the edit
											// panel to fill the horizontal width of the parent panel
											.editBasePanel(editPanel -> editPanel));
		}
	}

	public static final class NotesApplicationModel extends SwingEntityApplicationModel {

		private static final Version VERSION = Version.builder().major(1).build();

		public NotesApplicationModel(EntityConnectionProvider connectionProvider) {
			super(connectionProvider, List.of(new NoteModel(connectionProvider)), VERSION);
			// Refresh the table model to populate it
			entityModels().get(Note.TYPE).tableModel().items().refresh();
		}
	}

	public static final class NotesApplicationPanel extends EntityApplicationPanel<NotesApplicationModel> {

		public NotesApplicationPanel(NotesApplicationModel applicationModel) {
			super(applicationModel,
							// Supply an instance of our NotePanel, using the model from above
							List.of(new NotePanel(applicationModel.entityModels().get(Note.TYPE))),
							emptyList(), applicationPanel -> () ->
											// Replace the default JTabbedPane based layout
											// since we're only displaying a single panel,
											// simply return our main panel, initialized
											applicationPanel.entityPanel(Note.TYPE).initialize());
			setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		}
	}

	private static void startApplication() {
		// Change the default horizontal alignment for temporal table columns
		FilterTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);

		EntityApplication.builder(NotesApplicationModel.class, NotesApplicationPanel.class)
						.frameTitle("Notes")
						.frameSize(new Dimension(600, 500))
						.version(NotesApplicationModel.VERSION)
						// No need for a startup dialog since startup is very quick
						.startupDialog(false)
						.connectionProvider(LocalEntityConnectionProvider.builder()
										// Initialize the database schema
										.database(initializeDatabase())
										// Supply our domain model
										.domain(new Notes())
										// Use the H2Database super-user
										.user(User.user("sa"))
										.build())
						// IntelliJ theme based Flat Look and Feels are available
						.defaultLookAndFeel(MaterialDarker.class)
						// Runs on the EventDispatchThread
						.start();
	}

	public static void main(String[] args) {
		startApplication();
	}

	private static Database initializeDatabase() {
		List<String> dataStatements = List.of(
						"insert into notes.note(note, created) " +
										"values ('My first note', '2023-10-03 10:40')",
						"insert into notes.note(note, created) " +
										"values ('My second note', '2023-10-03 12:20')",
						"insert into notes.note(note, created, updated) " +
										"values ('My third note', '2023-10-04 08:50', '2023-10-04 08:52')",
						"insert into notes.note(note, created) " +
										"values ('My fourth note', '2023-10-05 09:03')",
						"insert into notes.note(note, created) " +
										"values ('My fifth note', '2023-10-05 18:30')",
						"commit"
		);
		Database database = new H2DatabaseFactory()
						.create("jdbc:h2:mem:h2db;DB_CLOSE_DELAY=-1");
		try (Connection connection = database.createConnection(User.user("sa"));
				 Statement stmt = connection.createStatement()) {
			for (String statement : CREATE_SCHEMA_STATEMENTS) {
				stmt.execute(statement);
			}
			for (String statement : dataStatements) {
				stmt.execute(statement);
			}

			return database;
		}
		catch (Exception e) {
			throw Exceptions.runtime(e);
		}
	}
}
// end::notes[]