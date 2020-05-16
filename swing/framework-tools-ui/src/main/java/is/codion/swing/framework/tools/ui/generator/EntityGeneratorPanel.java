/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.ui.generator;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.LoginPanel;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.table.FilteredTable;
import is.codion.swing.common.ui.value.TextValues;
import is.codion.swing.framework.tools.generator.EntityGeneratorModel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.icons.Icons.icons;

/**
 * A UI class based on the EntityGeneratorModel.
 * @see EntityGeneratorModel
 */
public class EntityGeneratorPanel extends JPanel {

  private static final double RESIZE_WEIGHT = 0.2;

  private final EntityGeneratorModel model;

  /**
   * Instantiates a new EntityGeneratorPanel.
   * @param generatorModel the entity generator model
   */
  public EntityGeneratorPanel(final EntityGeneratorModel generatorModel) {
    this.model = generatorModel;
    final FilteredTable<EntityGeneratorModel.Schema, Integer,
            AbstractFilteredTableModel<EntityGeneratorModel.Schema, Integer>> schema =
            new FilteredTable<>(generatorModel.getSchemaModel());
    final JScrollPane schemaScroller = new JScrollPane(schema);

    final FilteredTable<EntityGeneratorModel.Table, Integer,
            AbstractFilteredTableModel<EntityGeneratorModel.Table, Integer>> table =
            new FilteredTable<>(generatorModel.getTableModel());
    final JScrollPane tableScroller = new JScrollPane(table);

    final JSplitPane schemaTableSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    schemaTableSplitPane.setResizeWeight(RESIZE_WEIGHT);
    schemaTableSplitPane.setTopComponent(schemaScroller);
    schemaTableSplitPane.setBottomComponent(tableScroller);

    final JSplitPane splitPane = new JSplitPane();
    splitPane.setLeftComponent(schemaTableSplitPane);

    final JTextArea textArea = new JTextArea(40, 60);
    textArea.setEditable(false);
    generatorModel.getDefinitionTextValue().link(TextValues.textValue(textArea));
    final JScrollPane documentScroller = new JScrollPane(textArea);
    splitPane.setRightComponent(documentScroller);

    splitPane.setResizeWeight(RESIZE_WEIGHT);

    setLayout(Layouts.borderLayout());
    add(splitPane, BorderLayout.CENTER);

    bindEvents();
  }

  private void bindEvents() {
    model.addRefreshStartedListener(() -> Components.showWaitCursor(EntityGeneratorPanel.this));
    model.addRefreshDoneListener(() -> Components.hideWaitCursor(EntityGeneratorPanel.this));
  }

  /**
   * Runs a EntityGeneratorPanel instance in a frame
   * @param arguments no arguments required
   */
  public static void main(final String[] arguments) {
    try {
      final User user = new LoginPanel().showLoginPanel(null);
      final EntityGeneratorPanel generatorPanel = new EntityGeneratorPanel(new EntityGeneratorModel(user));
      final JFrame frame = new JFrame("Codion Entity Generator");
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
    };
  }
}
