/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.generator.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.swing.common.ui.LoginPanel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.table.FilteredTablePanel;
import org.jminor.swing.framework.generator.EntityGeneratorModel;

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
 * @see org.jminor.swing.framework.generator.EntityGeneratorModel
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
    final FilteredTablePanel<EntityGeneratorModel.Table, Integer> table =
            new FilteredTablePanel<>(generatorModel.getTableModel());
    final JScrollPane scroller = new JScrollPane(table.getJTable());

    final JSplitPane splitPane = new JSplitPane();
    splitPane.setLeftComponent(scroller);

    final JTextArea textArea = new JTextArea(generatorModel.getDocument(), "", 40, 60);
    final JScrollPane documentScroller = new JScrollPane(textArea);
    splitPane.setRightComponent(documentScroller);

    splitPane.setResizeWeight(RESIZE_WEIGHT);

    setLayout(UiUtil.createBorderLayout());
    add(splitPane, BorderLayout.CENTER);

    bindEvents();
  }

  private void bindEvents() {
    model.addRefreshStartedListener(new EventListener() {
      @Override
      public void eventOccurred() {
        UiUtil.setWaitCursor(true, EntityGeneratorPanel.this);
      }
    });
    model.addRefreshDoneListener(new EventListener() {
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
    @Override
    public void run() {
      try {
        Configuration.init();
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        final String schemaName = JOptionPane.showInputDialog("Schema name");
        if (Util.nullOrEmpty(schemaName)) {
          return;
        }

        final User user = new LoginPanel(new User(schemaName, null)).showLoginPanel(null);
        final EntityGeneratorModel generatorModel = new EntityGeneratorModel(user, schemaName);
        final EntityGeneratorPanel generatorPanel = new EntityGeneratorPanel(generatorModel);
        final ImageIcon icon = Images.loadImage("jminor_logo32.gif");
        final JFrame frame = new JFrame("JMinor Entity Generator");
        frame.setIconImage(icon.getImage());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(generatorPanel);

        frame.pack();
        UiUtil.centerWindow(frame);
        frame.setVisible(true);
      }
      catch (final CancelException ignored) {/*ignored*/}
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
