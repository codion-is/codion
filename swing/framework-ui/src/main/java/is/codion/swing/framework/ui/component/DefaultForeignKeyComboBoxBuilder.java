/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.ui.EntityComboBox;

import javax.swing.JComponent;

final class DefaultForeignKeyComboBoxBuilder extends AbstractComponentBuilder<Entity, EntityComboBox, ForeignKeyComboBoxBuilder>
        implements ForeignKeyComboBoxBuilder {

  private final SwingEntityComboBoxModel comboBoxModel;
  private int popupWidth;

  DefaultForeignKeyComboBoxBuilder(final ForeignKeyProperty foreignKey, final Value<Entity> value,
                                   final SwingEntityComboBoxModel comboBoxModel) {
    super(foreignKey, value);
    this.comboBoxModel = comboBoxModel;
  }

  @Override
  public ForeignKeyComboBoxBuilder popupWidth(final int popupWidth) {
    this.popupWidth = popupWidth;
    return this;
  }

  @Override
  public EntityComboBox build() {
    final EntityComboBox comboBox = createForeignKeyComboBox();
    setPreferredSize(comboBox);
    onBuild(comboBox);
    comboBox.setTransferFocusOnEnter(transferFocusOnEnter);
    if (transferFocusOnEnter) {
      Components.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
    }
    setPreferredSize(comboBox);
    if (popupWidth > 0) {
      comboBox.setPopupWidth(popupWidth);
    }

    return comboBox;
  }

  /**
   * Creates EntityComboBox based on the given foreign key
   * @param foreignKey the foreign key on which entity to base the combobox
   * @param value the value to bind to the field
   * @param comboBoxModel the combo box model
   * @param enabledState the state controlling the enabled state of the combobox
   * @return a EntityComboBox based on the given foreign key
   */
  private EntityComboBox createForeignKeyComboBox() {
    final EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    ComponentValues.comboBox(comboBox).link(value);
    DefaultComboBoxBuilder.addComboBoxCompletion(comboBox);

    return setDescriptionAndEnabledState(comboBox, property.getDescription(), enabledState);
  }
}
