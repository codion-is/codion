/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.generator.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.LoginPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.table.FilteredTablePanel;
import org.jminor.framework.Configuration;
import org.jminor.framework.tools.generator.EntityGeneratorModel;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;

/**
 * A UI class based on the EntityGeneratorModel.
 * @see org.jminor.framework.tools.generator.EntityGeneratorModel
 */
public class EntityGeneratorPanel extends JPanel {

  private final EntityGeneratorModel model;

  /**
   * Instantiates a new EntityGeneratorPanel.
   * @param generatorModel the entity generator model
   */
  public EntityGeneratorPanel(final EntityGeneratorModel generatorModel) {
    this.model = generatorModel;
    final FilteredTablePanel<EntityGeneratorModel.Table, Integer> table =
            new FilteredTablePanel<EntityGeneratorModel.Table, Integer>(generatorModel.getTableModel());
    final JScrollPane scroller = new JScrollPane(table.getJTable());

    final JSplitPane splitPane = new JSplitPane();
    splitPane.setLeftComponent(scroller);

    final JTextArea textArea = new JTextArea(generatorModel.getDocument(), "", 40, 60);
    final JScrollPane documentScroller = new JScrollPane(textArea);
    splitPane.setRightComponent(documentScroller);

    splitPane.setResizeWeight(0.2);

    setLayout(UiUtil.createBorderLayout());
    add(splitPane, BorderLayout.CENTER);

    bindEvents();
  }

  private void bindEvents() {
    model.addRefreshStartedListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        UiUtil.setWaitCursor(true, EntityGeneratorPanel.this);
      }
    });
    model.addRefreshDoneListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        UiUtil.setWaitCursor(false, EntityGeneratorPanel.this);
      }
    });
  }

  /**
   * Runs a EntityGeneratorPanel instance in a frame
   * @param arguments no arguments required
   */
  public static void main(final String[] arguments) {
    SwingUtilities.invokeLater(new Starter());
  }

  private static final class Starter implements Runnable {
    /** {@inheritDoc} */
    @Override
    public void run() {
      try {
        Configuration.init();
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        final String schemaName = JOptionPane.showInputDialog("Schema name");
        if (Util.nullOrEmpty(schemaName)) {
          return;
        }

        final User user = LoginPanel.getUser(null, new User(schemaName, null));
        final EntityGeneratorModel model = new EntityGeneratorModel(user, schemaName);
        final EntityGeneratorPanel panel = new EntityGeneratorPanel(model);
        final ImageIcon icon = Images.loadImage("jminor_logo32.gif");
        final JFrame frame = new JFrame("JMinor Entity Generator");
        frame.setIconImage(icon.getImage());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(panel);

        frame.pack();
        UiUtil.centerWindow(frame);
        frame.setVisible(true);
      }
      catch (CancelException ignored) {}
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
