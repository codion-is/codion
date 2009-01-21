/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
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
import javax.swing.Action;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * EntityComboBox provides a button for creating a new entity
 */
public class EntityComboBox extends SteppedComboBox {

  private final JButton btnNewRecord;
  private final EntityPanel.EntityPanelInfo applicationInfo;
  private final Action closeAction;

  private JPopupMenu popupMenu;

  public EntityComboBox(final EntityComboBoxModel model,
                        final EntityPanel.EntityPanelInfo newRecordPanelInfo) {
    this(model, newRecordPanelInfo, true);
  }

  public EntityComboBox(final EntityComboBoxModel model,
                        final EntityPanel.EntityPanelInfo newRecordPanelInfo,
                        final boolean newRecordButtonTakesFocus) {
    this(model, newRecordPanelInfo, newRecordButtonTakesFocus, null);
  }

  public EntityComboBox(final EntityComboBoxModel model,
                        final EntityPanel.EntityPanelInfo newRecordPanelInfo,
                        final boolean newRecordButtonTakesFocus,
                        final Action closeAction) {
    super(model);
    this.applicationInfo = newRecordPanelInfo;
    this.closeAction = closeAction;
    this.btnNewRecord = newRecordPanelInfo != null ? initializeNewRecordButton(newRecordButtonTakesFocus) : null;
    addRefreshPopupMenu();
  }

  public Dimension getPreferredSize() {
    final Dimension ret = super.getPreferredSize();
    ret.setSize(new Dimension(ret.width, UiUtil.getPreferredTextFieldHeight()));

    return ret;
  }

  public JPanel createPanel() {
    final JPanel ret = new JPanel(new BorderLayout());
    ret.add(this, BorderLayout.CENTER);
    if (applicationInfo != null)
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
          final EntityPanel entityPanel = applicationInfo.getInstance(getModel().getDbProvider());
          entityPanel.initialize();
//          entityPanel.getModel().getTableModel().setSelectedEntity(getModel().getSelectedEntity());
          final List<EntityKey> lastInsertedPrimaryKeys = new ArrayList<EntityKey>();
          entityPanel.getModel().evtAfterInsert.addListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
              lastInsertedPrimaryKeys.clear();
              lastInsertedPrimaryKeys.addAll(((InsertEvent) e).getInsertedKeys());
            }
          });
          final Window parentWindow = UiUtil.getParentWindow(EntityComboBox.this);
          final JDialog dialog = new JDialog(parentWindow, applicationInfo.getCaption());
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
          if (closeAction != null)
            closeAction.actionPerformed(new ActionEvent(entityPanel, 0, ""));
          //to prevent a memory leak, otherwise the KeyboardFocusManager keeps a live
          //reference to the dialog, preventing it from being garbage collected
//          KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener);
        }
        catch (UserException e1) {
          throw e1.getRuntimeException();
        }
      }
    });
    ret.setMnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0));

    return ret;
  }

  private void addRefreshPopupMenu() {
    getEditor().getEditorComponent().addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {//for linux :|
          if (popupMenu == null)
            popupMenu = initializePopupMenu();
          popupMenu.show(getEditor().getEditorComponent(), e.getX(), e.getY());
        }
        else {
          if (popupMenu != null && popupMenu.isShowing())
            popupMenu.setVisible(false);
        }
      }
    });
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu ret = new JPopupMenu();
    ret.add(new AbstractAction(FrameworkMessages.get(FrameworkMessages.REFRESH)) {
      public void actionPerformed(ActionEvent e) {
        try {
          getModel().forceRefresh();
        }
        catch (UserException e1) {
          throw e1.getRuntimeException();
        }
      }
    });

    return ret;
  }
}
