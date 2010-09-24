/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.generator.ui;

import org.jminor.common.db.Databases;
import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.User;
import org.jminor.common.ui.AbstractFilteredTablePanel;
import org.jminor.common.ui.ColumnSearchPanel;
import org.jminor.common.ui.LoginPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.tools.generator.EntityGeneratorModel;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    AbstractFilteredTablePanel<EntityGeneratorModel.Table, Integer> table =
            new AbstractFilteredTablePanel<EntityGeneratorModel.Table, Integer>(generatorModel.getTableListModel()) {
      @Override
      protected ColumnSearchPanel<Integer> initializeFilterPanel(final ColumnSearchModel<Integer> model) {
        return null;
      }
    };
    final JScrollPane scroller = new JScrollPane(table.getJTable());
    setLayout(new BorderLayout(5, 5));
    add(scroller, BorderLayout.WEST);

    final JTextArea textArea = new JTextArea(generatorModel.getDocument(), "", 40, 60);
    final JScrollPane documentScroller = new JScrollPane(textArea);
    add(documentScroller, BorderLayout.CENTER);

    bindEvents();
  }

  private void bindEvents() {
    model.addRefreshStartedListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        UiUtil.setWaitCursor(true, EntityGeneratorPanel.this);
      }
    });
    model.addRefreshEndedListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        UiUtil.setWaitCursor(false, EntityGeneratorPanel.this);
      }
    });
  }

  /**
   * Runs a EntityGeneratorPanel instance in a frame
   * @param arguments no arguments required
   * @throws Exception in case of an exception while initializing the panel
   */
  public static void main(final String[] arguments) throws Exception {
    String schemaName = JOptionPane.showInputDialog("Schema name");
    if (schemaName == null || schemaName.isEmpty()) {
      return;
    }

    schemaName = schemaName.toUpperCase();

    final String username = schemaName;
    final User user = LoginPanel.getUser(null, username != null ? new User(username, null) : null);

    final EntityGeneratorModel model = new EntityGeneratorModel(Databases.createInstance(), user, schemaName);
    final EntityGeneratorPanel panel = new EntityGeneratorPanel(model);

    final ImageIcon icon = Images.loadImage("jminor_logo32.gif");
    final JFrame frame = new JFrame("JMinor Entity Genarator");
    frame.setIconImage(icon.getImage());
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(panel);

    frame.pack();
    UiUtil.centerWindow(frame);
    frame.setVisible(true);
  }
}
