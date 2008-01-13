/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.UserException;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.client.model.FrameworkModelUtil;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityKey;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

public class EntityComboBox extends SteppedComboBox {

  private final JButton btnNewRecord;
  private final EntityPanelInfo applicationInfo;
  private final Action closeAction;

  private JPopupMenu popupMenu;

  public EntityComboBox(final EntityComboBoxModel model,
                        final EntityPanelInfo newRecordPanelInfo) {
    this(model, newRecordPanelInfo, true);
  }

  public EntityComboBox(final EntityComboBoxModel model,
                        final EntityPanelInfo newRecordPanelInfo,
                        final boolean newRecordButtonTakesFocus) {
    this(model, newRecordPanelInfo, newRecordButtonTakesFocus, null);
  }

  public EntityComboBox(final EntityComboBoxModel model,
                        final EntityPanelInfo newRecordPanelInfo,
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

  /**
   * @return Value for property 'newRecordButtonFocusable'.
   */
  public boolean isNewRecordButtonFocusable() {
    return btnNewRecord.isFocusable();
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
          final EntityPanel entityPanel =
                  applicationInfo.getInstance(((EntityComboBoxModel) getModel()).getDbProvider());
          entityPanel.initialize();
          entityPanel.getModel().getTableModel().setSelectedEntity(((EntityComboBoxModel) getModel()).getSelectedEntity());
          final DefaultTreeModel applicationTree = FrameworkModelUtil.createApplicationTree(Arrays.asList(entityPanel.getModel()));
          FrameworkUiUtil.initializeNavigation(entityPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW),
                  entityPanel.getActionMap(), applicationTree);
          FrameworkUiUtil.initializeResizing(entityPanel);
          final Window parentWindow = UiUtil.getParentWindow(EntityComboBox.this);
          final JDialog dialog = new JDialog(parentWindow, applicationInfo.getCaption());
          dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
          dialog.setLayout(new BorderLayout());
          dialog.add(entityPanel, BorderLayout.CENTER);
          final JButton btnClose = initializeOkButton(entityPanel, dialog, null);
          btnClose.setMnemonic('O');
          final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
          buttonPanel.add(btnClose);
          dialog.add(buttonPanel, BorderLayout.SOUTH);
          if (entityPanel.usePreferredSize())
            dialog.pack();
          else
            UiUtil.resizeWindow(dialog, 0.5, new Dimension(800, 400));
          dialog.setLocationRelativeTo(parentWindow);
          dialog.setModal(true);
          dialog.setResizable(true);
          dialog.setVisible(true);
        }
        catch (UserException ue) {
          throw ue.getRuntimeException();
        }
        catch (RuntimeException ex) {
          throw ex;
        }
        catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    };
  }

  private JButton initializeOkButton(final EntityPanel entityPanel, final JDialog pane,
                               final PropertyChangeListener focusListener) {
    return new JButton(new AbstractAction(Messages.get(Messages.OK)) {
      public void actionPerformed(ActionEvent e) {
        try {
          ((EntityComboBoxModel) getModel()).refresh();
          final List<EntityKey> inserted = entityPanel.getModel().getLastInsertedEntityPrimaryKeys();
          if (inserted != null && inserted.size() > 0) {
            ((EntityComboBoxModel) getModel()).setSelectedEntityByPrimaryKey(inserted.get(0));
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
          KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusListener);
        }
        catch (UserException e1) {
          throw e1.getRuntimeException();
        }
      }
    });
  }

  private void addRefreshPopupMenu() {
    getEditor().getEditorComponent().addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
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
          ((EntityComboBoxModel) getModel()).forceRefresh();
        }
        catch (UserException e1) {
          throw e1.getRuntimeException();
        }
      }
    });

    return ret;
  }
}
