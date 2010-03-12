/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Collection;

public class EntityComboBox extends SteppedComboBox {

  /**
   * Instantiates a new EntityComboBox
   * @param model the EntityComboBoxModel
   */
  public EntityComboBox(final EntityComboBoxModel model) {
    super(model);
    setComponentPopupMenu(initializePopupMenu());
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension dimension = super.getPreferredSize();
    dimension.setSize(new Dimension(dimension.width, UiUtil.getPreferredTextFieldHeight()));

    return dimension;
  }

  @Override
  public EntityComboBoxModel getModel() {
    return (EntityComboBoxModel) super.getModel();
  }

  public AbstractAction createForeignKeyFilterAction(final String foreignKeyPropertyID) {
    return new AbstractAction(null, Images.loadImage(Images.IMG_FILTER_16)) {
      public void actionPerformed(final ActionEvent e) {
        final Collection<Entity> current = getModel().getForeignKeyFilterEntities(foreignKeyPropertyID);
        final int result = JOptionPane.showOptionDialog(EntityComboBox.this, createForeignKeyFilterComboBox(foreignKeyPropertyID),
                FrameworkMessages.get(FrameworkMessages.FILTER_BY), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (result != JOptionPane.OK_OPTION) {
          getModel().setForeignKeyFilterEntities(foreignKeyPropertyID, current);
        }
      }
    };
  }

  public EntityComboBox createForeignKeyFilterComboBox(final String foreignKeyPropertyID) {
    return new EntityComboBox(getModel().createForeignKeyFilterComboBoxModel(foreignKeyPropertyID));
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new AbstractAction(FrameworkMessages.get(FrameworkMessages.REFRESH)) {
      public void actionPerformed(ActionEvent e) {
        getModel().forceRefresh();
      }
    });

    return popupMenu;
  }
}
