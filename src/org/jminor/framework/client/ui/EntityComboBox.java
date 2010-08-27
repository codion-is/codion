/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.MaximumMatch;
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

/**
 * A UI component based on the EntityComboBoxModel.
 * @see EntityComboBoxModel
 */
public final class EntityComboBox extends SteppedComboBox {

  /**
   * Instantiates a new EntityComboBox
   * @param model the EntityComboBoxModel
   */
  public EntityComboBox(final EntityComboBoxModel model) {
    super(model);
    setComponentPopupMenu(initializePopupMenu());
  }

  /** {@inheritDoc} */
  @Override
  public Dimension getPreferredSize() {
    final Dimension dimension = super.getPreferredSize();
    dimension.setSize(new Dimension(dimension.width, UiUtil.getPreferredTextFieldHeight()));

    return dimension;
  }

  /** {@inheritDoc} */
  @Override
  public EntityComboBoxModel getModel() {
    return (EntityComboBoxModel) super.getModel();
  }

  public AbstractAction createForeignKeyFilterAction(final String foreignKeyPropertyID) {
    return new AbstractAction(null, Images.loadImage(Images.IMG_FILTER_16)) {
      /** {@inheritDoc} */
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
    final EntityComboBox comboBox = new EntityComboBox(getModel().createForeignKeyFilterComboBoxModel(foreignKeyPropertyID));
    MaximumMatch.enable(comboBox);
    return comboBox;
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new AbstractAction(FrameworkMessages.get(FrameworkMessages.REFRESH)) {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        getModel().forceRefresh();
      }
    });

    return popupMenu;
  }
}
