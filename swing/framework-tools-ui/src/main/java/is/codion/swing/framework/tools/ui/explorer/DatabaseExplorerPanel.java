/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.ui.explorer;

import is.codion.common.db.database.Databases;
import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.table.FilteredTable;
import is.codion.swing.framework.tools.explorer.DatabaseExplorerModel;
import is.codion.swing.framework.tools.metadata.Schema;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.icons.Icons.icons;
import static java.util.Objects.requireNonNull;

public final class DatabaseExplorerPanel extends JPanel {

  private static final double RESIZE_WEIGHT = 0.2;

  private final DatabaseExplorerModel model;
  private final JSplitPane splitPane = new JSplitPane();

  /**
   * Instantiates a new DatabaseExplorerPanel.
   * @param model the database explorer model to base this panel on
   */
  public DatabaseExplorerPanel(final DatabaseExplorerModel model) {
    this.model = requireNonNull(model);
    final FilteredTable<Schema, Integer, AbstractFilteredTableModel<Schema, Integer>> schema =
            new FilteredTable<>(model.getSchemaModel());
    final JScrollPane schemaScroller = new JScrollPane(schema);

    final FilteredTable<EntityDefinition, Integer, AbstractFilteredTableModel<EntityDefinition, Integer>> table =
            new FilteredTable<>(model.getDefinitionModel());
    final JScrollPane tableScroller = new JScrollPane(table);

    final JSplitPane schemaTableSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    schemaTableSplitPane.setResizeWeight(RESIZE_WEIGHT);
    schemaTableSplitPane.setTopComponent(schemaScroller);
    schemaTableSplitPane.setBottomComponent(tableScroller);

    splitPane.setLeftComponent(schemaTableSplitPane);

    final JTextArea textArea = new JTextArea(40, 60);
    textArea.setEditable(false);

    splitPane.setRightComponent(new JScrollPane(textArea));

    splitPane.setResizeWeight(RESIZE_WEIGHT);

    setLayout(Layouts.borderLayout());
    add(splitPane, BorderLayout.CENTER);
    this.model.getDomainCodeObserver().addDataListener(textArea::setText);
  }

  /**
   * Runs a EntityGeneratorPanel instance in a frame
   * @param arguments no arguments required
   */
  public static void main(final String[] arguments) {
    try {
      final User user = Users.parseUser("scott:tiger");
      final DatabaseExplorerPanel generatorPanel = new DatabaseExplorerPanel(new DatabaseExplorerModel(Databases.getInstance(), user));
      final JFrame frame = new JFrame("Codion Database Explorer");
      frame.setIconImage(icons().logoTransparent().getImage());
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.add(generatorPanel);

      frame.pack();
      Windows.centerWindow(frame);
      frame.setVisible(true);
    }
    catch (final CancelException ignored) {/*ignored*/}
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
