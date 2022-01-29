/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.ui.explorer;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.CancelException;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icons.Logos;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.table.FilteredTable;
import is.codion.swing.framework.tools.explorer.DatabaseExplorerModel;
import is.codion.swing.framework.tools.explorer.DefinitionRow;
import is.codion.swing.framework.tools.metadata.Schema;

import javax.swing.JFrame;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static java.util.Objects.requireNonNull;

public final class DatabaseExplorerPanel extends JPanel {

  private static final double RESIZE_WEIGHT = 0.2;

  private final DatabaseExplorerModel model;
  private final JSplitPane splitPane = new JSplitPane();

  /**
   * Instantiates a new DatabaseExplorerPanel.
   * @param model the database explorer model to base this panel on
   */
  DatabaseExplorerPanel(final DatabaseExplorerModel model) {
    this.model = requireNonNull(model);
    final FilteredTable<Schema, Integer, AbstractFilteredTableModel<Schema, Integer>> schemaTable =
            new FilteredTable<>(model.getSchemaModel());
    final JScrollPane schemaScroller = new JScrollPane(schemaTable);

    final FilteredTable<DefinitionRow, Integer,
            AbstractFilteredTableModel<DefinitionRow, Integer>> domainTable =
            new FilteredTable<>(model.getDefinitionModel());
    final JScrollPane tableScroller = new JScrollPane(domainTable);

    final JSplitPane schemaTableSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    schemaTableSplitPane.setResizeWeight(RESIZE_WEIGHT);
    schemaTableSplitPane.setTopComponent(schemaScroller);
    schemaTableSplitPane.setBottomComponent(tableScroller);

    final JTextArea textArea = new JTextArea(40, 60);
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
    final JFrame frame = new JFrame("Codion Database Explorer");
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        model.close();
      }
    });
    frame.setIconImage(Logos.logoTransparent().getImage());
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.add(this);
    frame.pack();
    Windows.centerWindow(frame);
    frame.setVisible(true);
  }

  private void populateSchema(final MouseEvent event) {
    final JPanel northPanel = new JPanel(Layouts.borderLayout());
    final JLabel schemaLabel = new JLabel("Testing", SwingConstants.CENTER);
    northPanel.add(schemaLabel, BorderLayout.CENTER);
    final EventDataListener<String> schemaNotifier = schema -> SwingUtilities.invokeLater(() -> schemaLabel.setText(schema));
    Dialogs.progressWorkerDialog(() -> model.populateSelected(schemaNotifier))
            .owner(this)
            .title("Populating")
            .northPanel(northPanel)
            .onSuccess(model.getSchemaModel()::refresh)
            .execute();
  }

  /**
   * Runs a DatabaseExplorerPanel instance in a frame
   * @param arguments no arguments required
   */
  public static void main(final String[] arguments) {
    try {
      final Database database = DatabaseFactory.getDatabase();
      final DatabaseExplorerModel explorerModel = new DatabaseExplorerModel(database,
              Dialogs.loginDialog()
                      .icon(Logos.logoTransparent())
                      .validator(user -> database.createConnection(user).close())
                      .show());
      new DatabaseExplorerPanel(explorerModel).showFrame();
    }
    catch (final CancelException ignored) {
      System.exit(0);
    }
    catch (final Exception e) {
      DefaultDialogExceptionHandler.getInstance().displayException(e, null);
      System.exit(0);
    }
  }
}
