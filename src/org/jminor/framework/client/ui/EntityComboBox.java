/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.UserException;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityKey;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * An EntityComboBox provides a button for creating a new entity
 * Use <code>createPanel()</code> to create a panel which includes the EntityComboBox and the button
 */
public class EntityComboBox extends SteppedComboBox {

  private final JButton btnNewRecord;
  private final EntityPanelProvider newRecordPanelProvider;

  /**
   * Instantiates a new EntityComboBox
   * @param model the EntityComboBoxModel
   * @param newRecordPanelProvider a EntityPanelProvider which provides a panel for creating
   * new records for the underlying entity/table
   */
  public EntityComboBox(final EntityComboBoxModel model, final EntityPanelProvider newRecordPanelProvider) {
    this(model, newRecordPanelProvider, true);
  }

  /**
   * Instantiates a new EntityComboBox
   * @param model the EntityComboBoxModel
   * @param newRecordPanelProvider a EntityPanelProvider which provides a panel for creating
   * new records for the underlying entity/table
   * @param newRecordButtonTakesFocus if true then the new record button is included in the focus traversal
   */
  public EntityComboBox(final EntityComboBoxModel model, final EntityPanelProvider newRecordPanelProvider,
                        final boolean newRecordButtonTakesFocus) {
    super(model);
    this.newRecordPanelProvider = newRecordPanelProvider;
    this.btnNewRecord = newRecordPanelProvider != null ? initializeNewRecordButton(newRecordButtonTakesFocus) : null;
    setComponentPopupMenu(initializePopupMenu());
  }

  public Dimension getPreferredSize() {
    final Dimension ret = super.getPreferredSize();
    ret.setSize(new Dimension(ret.width, UiUtil.getPreferredTextFieldHeight()));

    return ret;
  }

  public JPanel createPanel() {
    final JPanel ret = new JPanel(new BorderLayout());
    ret.add(this, BorderLayout.CENTER);
    if (newRecordPanelProvider != null)
      ret.add(btnNewRecord, BorderLayout.EAST);

    return ret;
  }

  public EntityComboBoxModel getModel() {
    return (EntityComboBoxModel) super.getModel();
  }

  private JButton initializeNewRecordButton(final boolean newRecordButtonFocusable) {
    final JButton ret = new JButton(initializeNewRecordAction());
    ret.setIcon(Images.loadImage(Images.IMG_ADD_16));
    ret.setPreferredSize(new Dimension(18, UiUtil.getPreferredTextFieldHeight()));
    ret.setFocusable(newRecordButtonFocusable);

    return ret;
  }

  private AbstractAction initializeNewRecordAction() {
    return new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        try {
          final EntityPanel entityPanel = newRecordPanelProvider.createInstance(getModel().getDbProvider());
          entityPanel.initialize();
          final List<EntityKey> lastInsertedPrimaryKeys = new ArrayList<EntityKey>();
          entityPanel.getModel().evtAfterInsert.addListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
              lastInsertedPrimaryKeys.clear();
              lastInsertedPrimaryKeys.addAll(((InsertEvent) e).getInsertedKeys());
            }
          });
          final Window parentWindow = UiUtil.getParentWindow(EntityComboBox.this);
          final String caption = newRecordPanelProvider.getCaption() == null || newRecordPanelProvider.getCaption().equals("") ?
                  entityPanel.getModel().getCaption() : newRecordPanelProvider.getCaption();
          final JDialog dialog = new JDialog(parentWindow, caption);
          dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
          dialog.setLayout(new BorderLayout());
          dialog.add(entityPanel, BorderLayout.CENTER);
          final JButton btnClose = initializeOkButton(entityPanel, dialog, lastInsertedPrimaryKeys);
          final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
          buttonPanel.add(btnClose);
          dialog.add(buttonPanel, BorderLayout.SOUTH);
          dialog.pack();
          dialog.setLocationRelativeTo(parentWindow);
          dialog.setModal(true);
          dialog.setResizable(true);
          dialog.setVisible(true);
        }
        catch (UserException ue) {
          throw ue.getRuntimeException();
        }
      }
    };
  }

  private JButton initializeOkButton(final EntityPanel entityPanel, final JDialog pane,
                                     final List<EntityKey> lastInsertedPrimaryKeys) {
    final JButton ret = new JButton(new AbstractAction(Messages.get(Messages.OK)) {
      public void actionPerformed(ActionEvent e) {
        try {
          getModel().refresh();
          if (lastInsertedPrimaryKeys != null && lastInsertedPrimaryKeys.size() > 0) {
            getModel().setSelectedEntityByPrimaryKey(lastInsertedPrimaryKeys.get(0));
          }
          else {
            final Entity selEntity = entityPanel.getModel().getTableModel().getSelectedEntity();
            if (selEntity != null)
              getModel().setSelectedItem(selEntity);
          }
          pane.dispose();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    });
    ret.setMnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0));

    return ret;
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu ret = new JPopupMenu();
    ret.add(new AbstractAction(FrameworkMessages.get(FrameworkMessages.REFRESH)) {
      public void actionPerformed(ActionEvent e) {
        try {
          getModel().forceRefresh();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    });

    return ret;
  }
}
