/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.ui.explorer;

import is.codion.common.db.database.Database;
import is.codion.common.event.EventDataListener;
import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icon.Logos;
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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseEvent;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.flowLayout;
import static java.util.Objects.requireNonNull;

public final class DatabaseExplorerPanel extends JPanel {

  private static final double RESIZE_WEIGHT = 0.2;

  private final DatabaseExplorerModel model;

  /**
   * Instantiates a new DatabaseExplorerPanel.
   * @param model the database explorer model to base this panel on
   */
  DatabaseExplorerPanel(DatabaseExplorerModel model) {
    this.model = requireNonNull(model);
    FilteredTable<Schema, Integer, FilteredTableModel<Schema, Integer>> schemaTable =
            FilteredTable.filteredTable(model.schemaModel());
    schemaTable.addDoubleClickListener(this::populateSchema);

    FilteredTable<DefinitionRow, Integer,
            FilteredTableModel<DefinitionRow, Integer>> domainTable =
            FilteredTable.filteredTable(model.definitionModel());

    JSplitPane schemaTableSplitPane = Components.splitPane()
            .orientation(JSplitPane.VERTICAL_SPLIT)
            .resizeWeight(RESIZE_WEIGHT)
            .topComponent(new JScrollPane(schemaTable))
            .bottomComponent(new JScrollPane(domainTable))
            .build();

    JTextArea textArea = Components.textArea()
            .rowsColumns(40, 60)
            .editable(false)
            .build();

    Font font = textArea.getFont();
    textArea.setFont(new Font(Font.MONOSPACED, font.getStyle(), font.getSize()));

    JPanel textAreaCopyPanel = Components.panel(borderLayout())
            .add(new JScrollPane(textArea), BorderLayout.CENTER)
            .add(Components.panel(flowLayout(FlowLayout.RIGHT))
                    .add(Control.builder(() -> Utilities.setClipboard(textArea.getText()))
                            .caption(Messages.copy())
                            .build()
                            .createButton())
                    .build(), BorderLayout.SOUTH)
            .build();

    JSplitPane splitPane = Components.splitPane()
            .resizeWeight(RESIZE_WEIGHT)
            .leftComponent(schemaTableSplitPane)
            .rightComponent(textAreaCopyPanel)
            .build();

    setLayout(borderLayout());
    add(splitPane, BorderLayout.CENTER);

    model.domainSourceObserver().addDataListener(textArea::setText);
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
    JLabel schemaLabel = new JLabel("Testing", SwingConstants.CENTER);
    JPanel northPanel = Components.panel(borderLayout())
            .add(schemaLabel, BorderLayout.CENTER)
            .build();
    EventDataListener<String> schemaNotifier = schema -> SwingUtilities.invokeLater(() -> schemaLabel.setText(schema));
    Dialogs.progressWorkerDialog(() -> model.populateSelected(schemaNotifier))
            .owner(this)
            .title("Populating")
            .northPanel(northPanel)
            .onResult(model.schemaModel()::refresh)
            .execute();
  }

  /**
   * Runs a DatabaseExplorerPanel instance in a frame
   * @param arguments no arguments required
   */
  public static void main(String[] arguments) {
    try {
      Database database = Database.instance();
      DatabaseExplorerModel explorerModel = DatabaseExplorerModel.databaseExplorerModel(database,
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
      Dialogs.showExceptionDialog(e, null);
      System.exit(0);
    }
  }
}
