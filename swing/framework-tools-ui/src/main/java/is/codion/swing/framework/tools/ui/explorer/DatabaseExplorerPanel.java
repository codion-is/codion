/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.ui.explorer;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.CancelException;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.dialog.DialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.tools.explorer.DatabaseExplorerModel;
import is.codion.swing.framework.tools.explorer.DefinitionRow;
import is.codion.swing.framework.tools.metadata.Schema;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import static java.util.Objects.requireNonNull;

public final class DatabaseExplorerPanel extends JPanel {

  private static final double RESIZE_WEIGHT = 0.2;

  private final DatabaseExplorerModel model;
  private final JSplitPane splitPane = new JSplitPane();

  /**
   * Instantiates a new DatabaseExplorerPanel.
   * @param model the database explorer model to base this panel on
   */
  DatabaseExplorerPanel(DatabaseExplorerModel model) {
    this.model = requireNonNull(model);
    FilteredTable<Schema, Integer, FilteredTableModel<Schema, Integer>> schemaTable =
            new FilteredTable<>(model.getSchemaModel());
    JScrollPane schemaScroller = new JScrollPane(schemaTable);

    FilteredTable<DefinitionRow, Integer,
            FilteredTableModel<DefinitionRow, Integer>> domainTable =
            new FilteredTable<>(model.getDefinitionModel());
    JScrollPane tableScroller = new JScrollPane(domainTable);

    JSplitPane schemaTableSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    schemaTableSplitPane.setResizeWeight(RESIZE_WEIGHT);
    schemaTableSplitPane.setTopComponent(schemaScroller);
    schemaTableSplitPane.setBottomComponent(tableScroller);

    JTextArea textArea = new JTextArea(40, 60);
    textArea.setEditable(false);

    splitPane.setLeftComponent(schemaTableSplitPane);
    splitPane.setRightComponent(new JScrollPane(textArea));
    splitPane.setResizeWeight(RESIZE_WEIGHT);

    setLayout(Layouts.borderLayout());
    add(splitPane, BorderLayout.CENTER);

    model.getDomainSourceObserver().addDataListener(textArea::setText);
    schemaTable.addDoubleClickListener(this::populateSchema);
  }

  public void showFrame() {
    Windows.frame(this)
            .title("Codion Database Explorer")
            .icon(Logos.logoTransparent())
            .defaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
            .onClosing(windowEvent -> model.close())
            .centerFrame(true)
            .show();
  }

  private void populateSchema(MouseEvent event) {
    JPanel northPanel = new JPanel(Layouts.borderLayout());
    JLabel schemaLabel = new JLabel("Testing", SwingConstants.CENTER);
    northPanel.add(schemaLabel, BorderLayout.CENTER);
    EventDataListener<String> schemaNotifier = schema -> SwingUtilities.invokeLater(() -> schemaLabel.setText(schema));
    Dialogs.progressWorkerDialog(() -> model.populateSelected(schemaNotifier))
            .owner(this)
            .title("Populating")
            .northPanel(northPanel)
            .onResult(model.getSchemaModel()::refresh)
            .execute();
  }

  /**
   * Runs a DatabaseExplorerPanel instance in a frame
   * @param arguments no arguments required
   */
  public static void main(String[] arguments) {
    try {
      Database database = DatabaseFactory.getDatabase();
      DatabaseExplorerModel explorerModel = new DatabaseExplorerModel(database,
              Dialogs.loginDialog()
                      .icon(Logos.logoTransparent())
                      .validator(user -> database.createConnection(user).close())
                      .show());
      new DatabaseExplorerPanel(explorerModel).showFrame();
    }
    catch (CancelException ignored) {
      System.exit(0);
    }
    catch (Exception e) {
      DialogExceptionHandler.getInstance().displayException(e, null);
      System.exit(0);
    }
  }
}
