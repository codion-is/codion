/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.component.ComboBoxBuilder;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.DefaultComboBoxBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.JTextField;
import java.util.Collection;
import java.util.ResourceBundle;

import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;
import static java.util.Objects.requireNonNull;

/**
 * A UI component based on the SwingEntityComboBoxModel.
 * @see EntityComboBoxModel
 */
public final class EntityComboBox extends SteppedComboBox<Entity> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityComboBox.class.getName());

  /**
   * Instantiates a new EntityComboBox
   * @param model the SwingEntityComboBoxModel
   */
  EntityComboBox(final SwingEntityComboBoxModel model) {
    super(model);
  }

  @Override
  public SwingEntityComboBoxModel getModel() {
    return (SwingEntityComboBoxModel) super.getModel();
  }

  /**
   * Creates an Action which displays a dialog for filtering this combo box via a foreign key
   * @param foreignKey the foreign key on which to filter
   * @return a Control for filtering this combo box
   */
  public Control createForeignKeyFilterControl(final ForeignKey foreignKey) {
    return Control.builder(createForeignKeyFilterCommand(requireNonNull(foreignKey)))
            .smallIcon(frameworkIcons().filter())
            .build();
  }

  /**
   * Creates a EntityComboBox for filtering this combo box via a foreign key
   * @param foreignKey the foreign key on which to filter
   * @return an EntityComboBox for filtering this combo box
   */
  public EntityComboBox createForeignKeyFilterComboBox(final ForeignKey foreignKey) {
    return builder(getModel().createForeignKeyFilterComboBoxModel(requireNonNull(foreignKey)))
            .completionMode(Completion.Mode.MAXIMUM_MATCH)
            .build();
  }

  /**
   * Creates a {@link IntegerField} which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @return a {@link IntegerField} bound to the selected value
   */
  public IntegerField integerFieldSelector(final Attribute<Integer> attribute) {
    requireNonNull(attribute);
    return Components.integerField(getModel().selectorValue(attribute))
            .columns(2)
            .selectAllOnFocusGained(true)
            .build();
  }

  /**
   * Creates a {@link IntegerField} which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @param finder responsible for finding the item to select by value
   * @return a {@link IntegerField} bound to the selected value
   */
  public IntegerField integerFieldSelector(final Attribute<Integer> attribute, final EntityComboBoxModel.Finder<Integer> finder) {
    requireNonNull(attribute);
    requireNonNull(finder);
    return Components.integerField(getModel().selectorValue(attribute, finder))
            .columns(2)
            .selectAllOnFocusGained(true)
            .build();
  }

  /**
   * Creates a {@link JTextField} which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @return a {@link JTextField} bound to the selected value
   */
  public JTextField textFieldSelector(final Attribute<String> attribute) {
    requireNonNull(attribute);
    return Components.textField(getModel().selectorValue(attribute))
            .columns(2)
            .selectAllOnFocusGained(true)
            .build();
  }

  /**
   * Creates a {@link JTextField} which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @param finder responsible for finding the item to select by value
   * @return a {@link JTextField} bound to the selected value
   */
  public JTextField textFieldSelector(final Attribute<String> attribute, final EntityComboBoxModel.Finder<String> finder) {
    requireNonNull(attribute);
    requireNonNull(finder);
    return Components.textField(getModel().selectorValue(attribute, finder))
            .columns(2)
            .selectAllOnFocusGained(true)
            .build();
  }

  /**
   * Instantiates a new {@link EntityComboBox} builder
   * @param comboBoxModel the combo box model
   * @param <B> the builder type
   * @return a builder for a {@link EntityComboBox}
   */
  public static <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> builder(final SwingEntityComboBoxModel comboBoxModel) {
    return new DefaultBuilder<>(comboBoxModel);
  }

  private Control.Command createForeignKeyFilterCommand(final ForeignKey foreignKey) {
    return () -> {
      final Collection<Entity> current = getModel().getForeignKeyFilterEntities(foreignKey);
      Dialogs.okCancelDialog(createForeignKeyFilterComboBox(foreignKey))
              .owner(this)
              .title(MESSAGES.getString("filter_by"))
              .onOk(() -> getModel().setForeignKeyFilterEntities(foreignKey, current))
              .show();
    };
  }

  private static final class DefaultBuilder<B extends ComboBoxBuilder<Entity, EntityComboBox, B>> extends DefaultComboBoxBuilder<Entity, EntityComboBox, B> {

    private DefaultBuilder(final SwingEntityComboBoxModel comboBoxModel) {
      super(comboBoxModel, null);
      popupMenuControl(Control.builder(comboBoxModel::forceRefresh)
              .caption(FrameworkMessages.get(FrameworkMessages.REFRESH))
              .build());
    }

    @Override
    protected EntityComboBox createComboBox() {
      return new EntityComboBox((SwingEntityComboBoxModel) comboBoxModel);
    }
  }
}
