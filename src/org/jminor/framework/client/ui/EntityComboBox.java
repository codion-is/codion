/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.model.FilterCriteria;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.EntityRepository;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

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
      private Entity filterByEntity;
      public void actionPerformed(final ActionEvent e) {
        final Property.ForeignKeyProperty foreignKeyProperty =
                EntityRepository.getForeignKeyProperty(getModel().getEntityID(), foreignKeyPropertyID);
        final EntityComboBoxModel foreignKeyModel = new EntityComboBoxModel(foreignKeyProperty.getReferencedEntityID(),
                getModel().getDbProvider(), true, "-", true);
        foreignKeyModel.refresh();
        foreignKeyModel.setSelectedItem(filterByEntity);
        final int result = JOptionPane.showOptionDialog(EntityComboBox.this, new JComboBox(foreignKeyModel),
                FrameworkMessages.get(FrameworkMessages.FILTER_BY), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (result == JOptionPane.OK_OPTION) {
          filterByEntity = foreignKeyModel.getSelectedEntity();
          if (filterByEntity != null) {
            getModel().setFilterCriteria(new FilterCriteria() {
              public boolean include(final Object item) {
                final Entity foreignKeyValue = ((Entity)item).getEntityValue(foreignKeyProperty.getPropertyID());
                return foreignKeyValue != null && foreignKeyValue.equals(filterByEntity);
              }
            });
          }
          else {
            getModel().setFilterCriteria(null);
          }
        }
      }
    };
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
