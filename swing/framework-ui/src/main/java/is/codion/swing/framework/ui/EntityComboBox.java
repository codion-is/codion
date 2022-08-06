/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.combobox.FilteredComboBoxModel.Finder;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.combobox.DefaultComboBoxBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.util.Collection;
import java.util.ResourceBundle;

import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;
import static java.util.Objects.requireNonNull;

/**
 * A UI component based on the SwingEntityComboBoxModel.
 * @see EntityComboBoxModel
 */
public final class EntityComboBox extends JComboBox<Entity> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityComboBox.class.getName());

  /**
   * Instantiates a new EntityComboBox
   * @param model the SwingEntityComboBoxModel
   */
  EntityComboBox(SwingEntityComboBoxModel model) {
    super(model);
  }

  @Override
  public SwingEntityComboBoxModel getModel() {
    return (SwingEntityComboBoxModel) super.getModel();
  }

  /**
   * Overridden as a workaround for editable combo boxes as initial focus components on
   * detail panels stealing the focus from the parent panel on initialization
   */
  @Override
  public void requestFocus() {
    if (isEditable()) {
      getEditor().getEditorComponent().requestFocus();
    }
    else {
      super.requestFocus();
    }
  }

  /**
   * Creates an Action which displays a dialog for filtering this combo box via a foreign key
   * @param foreignKey the foreign key on which to filter
   * @return a Control for filtering this combo box
   */
  public Control createForeignKeyFilterControl(ForeignKey foreignKey) {
    return Control.builder(createForeignKeyFilterCommand(requireNonNull(foreignKey)))
            .smallIcon(frameworkIcons().filter())
            .build();
  }

  /**
   * Creates a {@link ComboBoxBuilder} returning a combo box for filtering this combo box via a foreign key
   * @param foreignKey the foreign key on which to filter
   * @param <B> the builder type
   * @return a {@link ComboBoxBuilder} for a foreign key filter combo box
   */
  public <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> createForeignKeyFilterComboBox(
          ForeignKey foreignKey) {
    return (B) builder(getModel().createForeignKeyFilterComboBoxModel(requireNonNull(foreignKey)))
            .completionMode(Completion.Mode.MAXIMUM_MATCH);
  }

  /**
   * Creates a {@link TextFieldBuilder} returning a {@link NumberField} which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a {@link NumberField} builder bound to the selected value
   */
  public <B extends TextFieldBuilder<Integer, NumberField<Integer>, B>> TextFieldBuilder<Integer, NumberField<Integer>, B> integerSelectorField(
          Attribute<Integer> attribute) {
    requireNonNull(attribute);
    return (B) Components.integerField(getModel().createSelectorValue(attribute))
            .columns(2)
            .selectAllOnFocusGained(true);
  }

  /**
   * Creates a {@link TextFieldBuilder} returning a {@link NumberField} which value is bound to the selected value in this combo box
   * @param finder responsible for finding the item to select by value
   * @param <B> the builder type
   * @return a {@link NumberField} builder bound to the selected value
   */
  public <B extends TextFieldBuilder<Integer, NumberField<Integer>, B>> TextFieldBuilder<Integer, NumberField<Integer>, B> integerSelectorField(
          Finder<Entity, Integer> finder) {
    requireNonNull(finder);
    return (B) Components.integerField(getModel().createSelectorValue(finder))
            .columns(2)
            .selectAllOnFocusGained(true);
  }

  /**
   * Creates a {@link TextFieldBuilder} returning a text field which value is bound to the selected value in this combo box
   * @param attribute the attribute
   * @param <B> the builder type
   * @return a {@link JTextField} builder bound to the selected value
   */
  public <B extends TextFieldBuilder<String, JTextField, B>> TextFieldBuilder<String, JTextField, B> stringSelectorField(
          Attribute<String> attribute) {
    requireNonNull(attribute);
    return (B) Components.textField(getModel().createSelectorValue(attribute))
            .columns(2)
            .selectAllOnFocusGained(true);
  }

  /**
   * Creates a {@link TextFieldBuilder} returning a text field which value is bound to the selected value in this combo box
   * @param finder responsible for finding the item to select by value
   * @param <B> the builder type
   * @return a {@link JTextField} builder bound to the selected value
   */
  public <B extends TextFieldBuilder<String, JTextField, B>> TextFieldBuilder<String, JTextField, B> stringSelectorField(
          Finder<Entity, String> finder) {
    requireNonNull(finder);
    return (B) Components.textField(getModel().createSelectorValue(finder))
            .columns(2)
            .selectAllOnFocusGained(true);
  }

  /**
   * Instantiates a new {@link EntityComboBox} builder
   * @param comboBoxModel the combo box model
   * @param <B> the builder type
   * @return a builder for a {@link EntityComboBox}
   */
  public static <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> builder(SwingEntityComboBoxModel comboBoxModel) {
    return builder(comboBoxModel, null);
  }

  /**
   * Instantiates a new {@link EntityComboBox} builder
   * @param comboBoxModel the combo box model
   * @param linkedValue the linked value
   * @param <B> the builder type
   * @return a builder for a {@link EntityComboBox}
   */
  public static <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> builder(SwingEntityComboBoxModel comboBoxModel,
                                                                                                                          Value<Entity> linkedValue) {
    return new DefaultBuilder<>(comboBoxModel, linkedValue);
  }

  private Control.Command createForeignKeyFilterCommand(ForeignKey foreignKey) {
    return () -> {
      Collection<Entity> current = getModel().getForeignKeyFilterEntities(foreignKey);
      Dialogs.okCancelDialog(createForeignKeyFilterComboBox(foreignKey).build())
              .owner(this)
              .title(MESSAGES.getString("filter_by"))
              .onOk(() -> getModel().setForeignKeyFilterEntities(foreignKey, current))
              .show();
    };
  }

  private static final class DefaultBuilder<B extends ComboBoxBuilder<Entity, EntityComboBox, B>> extends DefaultComboBoxBuilder<Entity, EntityComboBox, B> {

    private DefaultBuilder(SwingEntityComboBoxModel comboBoxModel, Value<Entity> linkedValue) {
      super(comboBoxModel, linkedValue);
      popupMenuControl(Control.builder(new ForceRefreshCommand(comboBoxModel))
              .caption(FrameworkMessages.refresh())
              .build());
    }

    @Override
    protected EntityComboBox createComboBox() {
      return new EntityComboBox((SwingEntityComboBoxModel) comboBoxModel);
    }
  }

  private static final class ForceRefreshCommand implements Control.Command {

    private final SwingEntityComboBoxModel comboBoxModel;

    private ForceRefreshCommand(SwingEntityComboBoxModel comboBoxModel) {
      this.comboBoxModel = comboBoxModel;
    }

    @Override
    public void perform() throws Exception {
      comboBoxModel.forceRefresh();
    }
  }
}
