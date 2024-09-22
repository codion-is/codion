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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.notes;

import is.codion.common.db.database.Database;
import is.codion.common.model.condition.ColumnConditionModel;
import is.codion.common.model.condition.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.dbms.h2.H2DatabaseFactory;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityApplicationPanel.Builder.ConnectionProviderFactory;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static is.codion.swing.framework.ui.EntityEditPanel.ControlKeys.CLEAR;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

// tag::notes[]
public final class NotesDemo {

	private static final List<String> CREATE_SCHEMA_STATEMENTS = asList(
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
			add(Note.TYPE.define(
											Note.ID.define()
															.primaryKey(),
											Note.NOTE.define()
															.column()
															.caption("Note")
															.nullable(false),
											Note.CREATED.define()
															.column()
															.caption("Created")
															.nullable(false)
															.updatable(false)
															.columnHasDefaultValue(true),
											Note.UPDATED.define()
															.column()
															.caption("Updated"))
							.keyGenerator(KeyGenerator.identity())
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
							notes.values().forEach(note ->
											note.put(Note.UPDATED, LocalDateTime.now())));
		}
	}

	private static final class NoteEditPanel extends EntityEditPanel {

		private NoteEditPanel(NoteEditModel editModel) {
			super(editModel);
			// CLEAR is the only standard control we require, for clearing the UI
			configureControls(config -> config.clear()
							.control(CLEAR));
		}

		@Override
		protected void initializeUI() {
			initialFocusAttribute().set(Note.NOTE);

			createTextField(Note.NOTE)
							.hint("Take note...")
							// Use the Enter key for inserting, updating
							// and deleting, depending on the edit model state
							.action(Control.command(this::insertDeleteOrUpdate));

			setLayout(Layouts.borderLayout());
			add(component(Note.NOTE).get(), BorderLayout.CENTER);
			// Add a button based on the CLEAR control
			add(new JButton(control(CLEAR).get()), BorderLayout.EAST);
		}

		private void insertDeleteOrUpdate() {
			if (editModel().exists().not().get() && editModel().isNotNull(Note.NOTE).get()) {
				// A new note with a non-empty text
				insert();
			}
			else if (editModel().modified().get()) {
				if (editModel().isNull(Note.NOTE).get()) {
					// An existing note with empty text
					deleteWithConfirmation();
				}
				else {
					// An existing note with a modified text
					updateWithConfirmation();
				}
			}
		}
	}

	private static final class NoteTableModel extends SwingEntityTableModel {

		private NoteTableModel(EntityConnectionProvider connectionProvider) {
			super(new NoteEditModel(connectionProvider));
			onInsert().set(OnInsert.ADD_TOP_SORTED);
			// Case-insensitive note search with automatic wildcards
			ColumnConditionModel<?, ?> noteConditionModel = queryModel().conditionModel().conditionModel(Note.NOTE);
			noteConditionModel.caseSensitive().set(false);
			noteConditionModel.automaticWildcard().set(AutomaticWildcard.PREFIX_AND_POSTFIX);
		}
	}

	private static final class NoteTablePanel extends EntityTablePanel {

		private NoteTablePanel(NoteTableModel tableModel) {
			super(tableModel, config -> config
							// Exclude the Note.UPDATED attribute from the Edit popup menu since
							// the value is set automatically and shouldn't be editable via the UI.
							// Note.CREATED is excluded by default since it is not updatable.
							.editable(attributes -> attributes.remove(Note.UPDATED)));
			// Configure the table and columns
			table().sortModel().setSortOrder(Note.CREATED, SortOrder.DESCENDING);
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

		private NotePanel(NoteModel noteModel) {
			super(noteModel,
							new NoteEditPanel(noteModel.editModel()),
							new NoteTablePanel(noteModel.tableModel()), config -> config
											// No need to include the default control buttons since
											// we added the CLEAR control button to the edit panel
											.includeControls(false)
											// Replace the default edit base panel which uses a FlowLayout, in order
											// to have the edit panel fill the horizontal width of the parent panel
											.editBasePanel(editPanel -> Components.borderLayoutPanel()
															.centerComponent(editPanel)
															.build()));
		}
	}

	public static final class NotesApplicationModel extends SwingEntityApplicationModel {

		private static final Version VERSION = Version.builder().major(1).build();

		public NotesApplicationModel(EntityConnectionProvider connectionProvider) {
			super(connectionProvider, VERSION);
			NoteModel noteModel = new NoteModel(connectionProvider);
			addEntityModel(noteModel);
			// Refresh the table model to populate it
			noteModel.tableModel().refresh();
		}
	}

	public static final class NotesApplicationPanel extends EntityApplicationPanel<NotesApplicationModel> {

		public NotesApplicationPanel(NotesApplicationModel applicationModel) {
			super(applicationModel, NotesApplicationLayout::new);
		}

		@Override
		protected List<EntityPanel> createEntityPanels() {
			NoteModel noteModel = applicationModel().entityModel(Note.TYPE);

			return singletonList(new NotePanel(noteModel));
		}

		// Replace the default JTabbedPane based layout,
		// since we're only displaying a single panel
		private static final class NotesApplicationLayout implements ApplicationLayout {

			private final EntityApplicationPanel<?> applicationPanel;

			private NotesApplicationLayout(EntityApplicationPanel<?> applicationPanel) {
				this.applicationPanel = applicationPanel;
			}

			@Override
			public JComponent layout() {
				return Components.borderLayoutPanel()
								// initialize() must be called to initialize the UI components
								.centerComponent(applicationPanel.entityPanel(Note.TYPE).initialize())
								.border(BorderFactory.createEmptyBorder(5, 5, 0, 5))
								.build();
			}
		}
	}

	private static final class NotesConnectionProviderFactory implements ConnectionProviderFactory {

		@Override
		public EntityConnectionProvider createConnectionProvider(User user,
																														 DomainType domainType,
																														 String clientTypeId,
																														 Version clientVersion) {
			Database database = new H2DatabaseFactory()
							.createDatabase("jdbc:h2:mem:h2db;DB_CLOSE_DELAY=-1");

			// Here we create the EntityConnectionProvider instance
			// manually so we can safely ignore some method parameters
			return LocalEntityConnectionProvider.builder()
							// Create the schema
							.database(createSchema(database))
							// Supply a domain model instance, if we used the domainType we'd
							// need to register the domain model in the ServiceLoader in order for
							// the framework to instantiate one for us
							.domain(new Notes())
							.clientVersion(clientVersion)
							.user(user)
							.build();
		}
	}

	private static void startApplication() throws Exception {
		// Change the default horizontal alignment for temporal table columns
		FilterTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
		// Make all the IntelliJ themes from Flat Look and Feel available (View -> Select Look & Feel)
		Arrays.stream(FlatAllIJThemes.INFOS)
						.forEach(LookAndFeelProvider::addLookAndFeel);

		EntityApplicationPanel.builder(NotesApplicationModel.class, NotesApplicationPanel.class)
						.frameTitle("Notes")
						.frameSize(new Dimension(600, 500))
						.applicationVersion(NotesApplicationModel.VERSION)
						// No need for a startup dialog since startup is very quick
						.displayStartupDialog(false)
						// Supply our connection provider factory from above
						.connectionProviderFactory(new NotesConnectionProviderFactory())
						// Automatically login with the H2Database super user
						.automaticLoginUser(User.user("sa"))
						.defaultLookAndFeelClassName(FlatMaterialDarkerIJTheme.class.getName())
						// Runs on the EventDispatchThread
						.start();
	}

	public static void main(String[] args) throws Exception {
		startApplication();
	}

	private static Database createSchema(Database database) {
		List<String> dataStatements = asList(
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
			throw new RuntimeException(e);
		}
	}
}
// end::notes[]