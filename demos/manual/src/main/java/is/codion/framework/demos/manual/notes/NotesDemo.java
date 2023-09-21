package is.codion.framework.demos.manual.notes;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer;
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

import javax.swing.JButton;
import javax.swing.JPanel;
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
  private static class Notes extends DefaultDomain {

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
              .caption("Notes"));
    }
  }

  private static final class NoteEditModel extends SwingEntityEditModel {

    private NoteEditModel(EntityConnectionProvider connectionProvider) {
      super(Note.TYPE, connectionProvider);
      // Set the Note.UPDATED value before we perform an update
      addBeforeUpdateListener(notes ->
              notes.values().forEach(note ->
                      note.put(Note.UPDATED, LocalDateTime.now())));
    }
  }

  private static final class NoteEditPanel extends EntityEditPanel  {

    private NoteEditPanel(NoteEditModel editModel) {
      // CLEAR is the only standard control we require, for clearing the UI
      super(editModel, ControlCode.CLEAR);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusAttribute(Note.NOTE);

      createTextField(Note.NOTE)
              .hintText("Take note...")
              // Use the Enter key for inserting, updating
              // and deleting, depending on the edit model state
              .action(Control.control(this::insertDeleteOrUpdate));

      setLayout(Layouts.borderLayout());
      add(component(Note.NOTE), BorderLayout.CENTER);
      // Add a button based on the CLEAR control, which clears the UI
      add(new JButton(control(ControlCode.CLEAR)), BorderLayout.EAST);
    }

    private void insertDeleteOrUpdate() {
      if (editModel().entityNew().get() && editModel().isNotNull(Note.NOTE).get()) {
        // A new note with a non-empty text: INSERT
        insert();
      }
      else if (editModel().modified().get()) {
        if (editModel().isNull(Note.NOTE).get()) {
          // An existing note with empty text: DELETE
          deleteWithConfirmation();
        }
        else {
          // An existing note with a modified text: UPDATE
          updateWithConfirmation();
        }
      }
    }
  }

  private static final class NoteTableModel extends SwingEntityTableModel {

    private NoteTableModel(EntityConnectionProvider connectionProvider) {
      super(new NoteEditModel(connectionProvider));
      //configure the table model and columns
      sortModel().setSortOrder(Note.CREATED, SortOrder.DESCENDING);
      setOnInsert(OnInsert.ADD_TOP_SORTED);
      columnModel().column(Note.NOTE).setPreferredWidth(280);
      columnModel().column(Note.CREATED).setPreferredWidth(130);
      columnModel().column(Note.UPDATED).setPreferredWidth(130);
    }
  }

  private static final class NoteTablePanel extends EntityTablePanel  {

    private NoteTablePanel(NoteTableModel tableModel) {
      super(tableModel);
      table().setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
      // Exclude the Note.UPDATED attribute from the Edit popup menu since
      // the value is set automatically and shouldn't be editable via the UI.
      // Note.CREATED is excluded by default since it is not updatable.
      excludeFromEditMenu(Note.UPDATED);
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
              new NoteTablePanel(noteModel.tableModel()));
      // No need to include the default control button panel since
      // we added the CLEAR control button to the edit panel
      setIncludeControlPanel(false);
    }

    @Override
    protected JPanel createEditBasePanel(EntityEditPanel editPanel) {
      // Override to replace the default panel which uses a FlowLayout, in order
      // to have the edit panel fill the horizontal width of the parent panel
      return Components.borderLayoutPanel()
              .centerComponent(editPanel)
              .build();
    }
  }

  public static final class NotesApplicationModel extends SwingEntityApplicationModel {

    public NotesApplicationModel(EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
      NoteModel noteModel = new NoteModel(connectionProvider);
      addEntityModel(noteModel);
      // Refresh the table model to populate it
      noteModel.tableModel().refresh();
    }
  }

  public static final class NotesApplicationPanel extends EntityApplicationPanel<NotesApplicationModel> {

    public NotesApplicationPanel(NotesApplicationModel applicationModel) {
      super(applicationModel, applicationPanel -> {
        // Override the default JTabbedPane based layout,
        // since we're only displaying a single panel
        NotePanel notePanel = applicationPanel.entityPanel(Note.TYPE);
        notePanel.initialize();// Lazy initialization of UI components
        applicationPanel.setLayout(Layouts.borderLayout());
        applicationPanel.add(notePanel, BorderLayout.CENTER);
      });
    }

    @Override
    protected List<EntityPanel> createEntityPanels() {
      NoteModel noteModel = applicationModel().entityModel(Note.TYPE);

      return singletonList(new NotePanel(noteModel));
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

      // We create the EntityConnectionProvider instance manually
      // so we can safely ignore the method parameters.
      return LocalEntityConnectionProvider.builder()
              // Create the schema
              .database(createSchema(database))
              // Supply the domain model implementation
              .domain(new Notes())
              // Connect with the H2Database super user
              .user(User.user("sa"))
              .build();
    }
  }

  private static void startApplication() throws Exception {
    // Change the default horizontal alignment for temporal table columns
    FilteredTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
    // Make all the IntelliJ themes from Flat Look and Feel available (View -> Select Look & Feel)
    Arrays.stream(FlatAllIJThemes.INFOS)
            .forEach(LookAndFeelProvider::addLookAndFeelProvider);

    EntityApplicationPanel.builder(NotesApplicationModel.class, NotesApplicationPanel.class)
            .frameTitle("Notes")
            .frameSize(new Dimension(600, 500))
            // No need for a startup dialog since startup is very quick
            .displayStartupDialog(false)
            // Supply our connection provider factory from above
            .connectionProviderFactory(new NotesConnectionProviderFactory())
            // User is hardcoded in the connection provider factory
            .loginRequired(false)
            .defaultLookAndFeelClassName(FlatMaterialDarkerIJTheme.class.getName())
            // Runs on the EventDispatchThread
            .start();
  }

  public static void main(String[] args) throws Exception {
    startApplication();
  }

  private static Database createSchema(Database database) {
    List<String> dataStatements = asList(
            "insert into notes.note(note, created) values " +
                    "('My first note', '2023-10-03 10:40')",
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