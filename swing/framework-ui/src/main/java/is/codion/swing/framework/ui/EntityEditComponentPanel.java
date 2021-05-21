/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.i18n.Messages;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.PropertyValue;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityInputComponents.BooleanComboBoxBuilder;
import is.codion.swing.framework.ui.EntityInputComponents.CheckBoxBuilder;
import is.codion.swing.framework.ui.EntityInputComponents.ComboBoxBuilder;
import is.codion.swing.framework.ui.EntityInputComponents.ForeignKeyComboBoxBuilder;
import is.codion.swing.framework.ui.EntityInputComponents.ForeignKeySearchFieldBuilder;
import is.codion.swing.framework.ui.EntityInputComponents.FormattedTextFieldBuilder;
import is.codion.swing.framework.ui.EntityInputComponents.TemporalInputPanelBuilder;
import is.codion.swing.framework.ui.EntityInputComponents.TextAreaBuilder;
import is.codion.swing.framework.ui.EntityInputComponents.TextFieldBuilder;
import is.codion.swing.framework.ui.EntityInputComponents.TextInputPanelBuilder;
import is.codion.swing.framework.ui.EntityInputComponents.ValueListComboBoxBuilder;

import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A base class for entity edit panels, providing components for editing entities.
 */
public class EntityEditComponentPanel extends JPanel {

  /**
   * Specifies whether focus should be transferred from components on enter.
   * Note that this does not apply to text areas<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> TRANSFER_FOCUS_ON_ENTER = Configuration.booleanValue(
          "is.codion.swing.framework.ui.EntityEditComponentPanel.transferFocusOnEnter", true);

  /**
   * The edit model these edit components are associated with
   */
  private final SwingEntityEditModel editModel;

  /**
   * The input component creator
   */
  private final EntityInputComponents inputComponents;

  /**
   * Input components mapped to their respective attributes
   */
  private final Map<Attribute<?>, JComponent> components = new HashMap<>();

  /**
   * Attributes that should be excluded when presenting the component selection
   */
  private final Set<Attribute<?>> excludeFromSelection = new HashSet<>();

  /**
   * The component that should receive focus when the UI is initialized
   */
  private JComponent initialFocusComponent;

  /**
   * The attribute for which component should receive the focus when the UI is prepared
   */
  private Attribute<?> initialFocusAttribute;

  /**
   * The component that should receive focus when the UI is prepared after insert
   */
  private JComponent afterInsertFocusComponent;

  /**
   * The attribute for which component should receive the focus when the UI is prepared after insert
   */
  private Attribute<?> afterInsertFocusAttribute;

  /**
   * Specifies whether components created by this edit component panel should transfer focus on enter.
   */
  private boolean transferFocusOnEnter = TRANSFER_FOCUS_ON_ENTER.get();

  /**
   * Instantiates a new EntityEditComponentPanel
   * @param editModel the edit model
   */
  protected EntityEditComponentPanel(final SwingEntityEditModel editModel) {
    this.editModel = requireNonNull(editModel, "editModel");
    this.inputComponents = new EntityInputComponents(editModel.getEntityDefinition());
  }

  /**
   * @return the edit model this panel is based on
   */
  public final SwingEntityEditModel getEditModel() {
    return editModel;
  }

  /**
   * @return the attributes that have been associated with components.
   */
  public final List<Attribute<?>> getComponentAttributes() {
    return new ArrayList<>(components.keySet());
  }

  /**
   * @param attribute the attribute
   * @return the component associated with the given attribute, null if no component has been
   * associated with the given attribute
   */
  public final JComponent getComponent(final Attribute<?> attribute) {
    return components.get(attribute);
  }

  /**
   * @param component the component
   * @param <T> the attribute type
   * @return the attribute the given component is associated with, null if the component has not been
   * associated with a attribute
   */
  public final <T> Attribute<T> getAttribute(final JComponent component) {
    return (Attribute<T>) components.entrySet().stream().filter(entry -> entry.getValue() == component)
            .findFirst().map(Map.Entry::getKey).orElse(null);
  }

  /**
   * Sets the component that should receive the focus when the UI is cleared or activated.
   * Overrides the value set via {@link #setInitialFocusAttribute(Attribute)}
   * @param initialFocusComponent the component
   * @return the component
   */
  public final JComponent setInitialFocusComponent(final JComponent initialFocusComponent) {
    this.initialFocusComponent = initialFocusComponent;
    return initialFocusComponent;
  }

  /**
   * Sets the component associated with the given attribute as the component
   * that should receive the initial focus in this edit panel.
   * This is overridden by setInitialFocusComponent().
   * @param attribute the component attribute
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void setInitialFocusAttribute(final Attribute<?> attribute) {
    getEditModel().getEntityDefinition().getProperty(attribute);
    this.initialFocusAttribute = attribute;
  }

  /**
   * Sets the component that should receive the focus after an insert has been performed..
   * Overrides the value set via {@link #setAfterInsertFocusAttribute(Attribute)}
   * @param afterInsertFocusComponent the component
   * @return the component
   */
  public final JComponent setAfterInsertFocusComponent(final JComponent afterInsertFocusComponent) {
    this.afterInsertFocusComponent = afterInsertFocusComponent;
    return afterInsertFocusComponent;
  }

  /**
   * Sets the component associated with the given attribute as the component
   * that should receive the focus after an insert is performed in this edit panel.
   * This is overridden by setAfterInsertFocusComponent().
   * @param attribute the component attribute
   * @see #setAfterInsertFocusComponent(JComponent)
   */
  public final void setAfterInsertFocusAttribute(final Attribute<?> attribute) {
    getEditModel().getEntityDefinition().getProperty(attribute);
    this.afterInsertFocusAttribute = attribute;
  }

  /**
   * Sets the initial focus, if a initial focus component or component attribute
   * has been set that component receives the focus, if not, or if that component
   * is not focusable, this panel receives the focus.
   * Note that if this panel is not visible nothing happens.
   * @see #isVisible()
   * @see #setInitialFocusAttribute
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void requestInitialFocus() {
    if (isVisible()) {
      requestFocus(getInitialFocusComponent());
    }
  }

  /**
   * Request focus for the component associated with the given attribute
   * @param attribute the attribute of the component to select
   */
  public final void requestComponentFocus(final Attribute<?> attribute) {
    final JComponent component = getComponent(attribute);
    if (component != null) {
      component.requestFocus();
    }
  }

  /**
   * Displays a dialog allowing the user the select a input component which should receive the keyboard focus,
   * if only one input component is available then that component is selected automatically.
   * @see #excludeComponentFromSelection(Attribute)
   * @see #requestComponentFocus(Attribute)
   */
  public void selectInputComponent() {
    final List<Property<?>> properties =
            Properties.sort(getEditModel().getEntityDefinition().getProperties(getSelectComponentAttributes()));
    final Optional<Property<?>> optionalProperty = properties.size() == 1 ?  Optional.of(properties.get(0)) :
            Dialogs.selectionDialogBuilder(properties)
                    .owner(this)
                    .title(Messages.get(Messages.SELECT_INPUT_FIELD))
                    .selectSingle();
    optionalProperty.ifPresent(property -> requestComponentFocus(property.getAttribute()));
  }

  /**
   * @return a list of attributes to use when selecting a input component in this panel,
   * this returns all attributes that have an associated component in this panel
   * that are enabled, displayable, visible and focusable.
   * @see #excludeComponentFromSelection(Attribute)
   * @see #setComponent(Attribute, JComponent)
   */
  public final List<Attribute<?>> getSelectComponentAttributes() {
    final List<Attribute<?>> attributes = getComponentAttributes();
    attributes.removeIf(attribute ->
            excludeFromSelection.contains(attribute) ||
                    !isComponentSelectable(getComponent(attribute)));

    return attributes;
  }

  /**
   * Specifies that the given attribute should be excluded when presenting a component selection list.
   * @param attribute the attribute to exclude from selection
   * @see #selectInputComponent()
   */
  public final void excludeComponentFromSelection(final Attribute<?> attribute) {
    getEditModel().getEntityDefinition().getProperty(attribute);//just validating that the attribute exists
    excludeFromSelection.add(attribute);
  }

  /**
   * If set to true then components created subsequently will transfer focus on enter, otherwise not.
   * Note that this has no effect on components that have already been created.
   * @param transferFocusOnEnter the new value
   * @see #TRANSFER_FOCUS_ON_ENTER
   */
  protected final void setTransferFocusOnEnter(final boolean transferFocusOnEnter) {
    this.transferFocusOnEnter = transferFocusOnEnter;
  }

  /**
   * Associates the given input component with the given attribute.
   * @param attribute the attribute
   * @param component the input component
   */
  protected final void setComponent(final Attribute<?> attribute, final JComponent component) {
    getEditModel().getEntityDefinition().getProperty(attribute);
    requireNonNull(component, "component");
    components.put(attribute, component);
  }

  /**
   * Adds a panel for the given attribute to this panel
   * @param attribute the attribute
   * @see #createInputPanel(Attribute)
   */
  protected final void addInputPanel(final Attribute<?> attribute) {
    add(createInputPanel(attribute));
  }

  /**
   * Adds a panel for the given attribute to this panel using the given layout constraints
   * @param attribute the attribute
   * @param constraints the layout constraints
   * @see #createInputPanel(Attribute)
   */
  protected final void addInputPanel(final Attribute<?> attribute, final Object constraints) {
    add(createInputPanel(attribute), constraints);
  }

  /**
   * Adds a panel for the given attribute to this panel
   * @param attribute the attribute
   * @param inputComponent a component bound to {@code attribute}
   * @see #createInputPanel(Attribute, JComponent)
   */
  protected final void addInputPanel(final Attribute<?> attribute, final JComponent inputComponent) {
    add(createInputPanel(attribute, inputComponent));
  }

  /**
   * Adds a panel for the given attribute to this panel using the given layout constraints
   * @param attribute the attribute
   * @param inputComponent a component bound to {@code attribute}
   * @param constraints the layout constraints
   * @see #createInputPanel(Attribute, JComponent)
   */
  protected final void addInputPanel(final Attribute<?> attribute, final JComponent inputComponent, final Object constraints) {
    add(createInputPanel(attribute, inputComponent), constraints);
  }

  /**
   * Creates a panel containing a label and the component associated with the given attribute.
   * The label text is the caption of the property based on {@code attribute}.
   * The default layout of the resulting panel is with the label on top and inputComponent below.
   * @param attribute the attribute from which property to retrieve the label caption
   * @return a panel containing a label and a component
   * @throws IllegalArgumentException in case no component has been associated with the given attribute
   */
  protected final JPanel createInputPanel(final Attribute<?> attribute) {
    final JComponent component = getComponent(attribute);
    if (component == null) {
      throw new IllegalArgumentException("No component associated with attribute: " + attribute);
    }

    return createInputPanel(attribute, component);
  }

  /**
   * Creates a panel containing a label and the component associated with the given attribute.
   * The label text is the caption of the property based on {@code attribute}.
   * The default layout of the resulting panel is with the label on top and {@code inputComponent} below.
   * @param attribute the attribute from which property to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code attribute}
   * @return a panel containing a label and a component
   */
  protected final JPanel createInputPanel(final Attribute<?> attribute, final JComponent inputComponent) {
    return createInputPanel(attribute, inputComponent, BorderLayout.NORTH);
  }

  /**
   * Creates a panel containing a label and the component associated with the given attribute.
   * The label text is the caption of the property based on {@code attribute}.
   * @param attribute the attribute from which property to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code attribute}
   * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
   * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
   * @return a panel containing a label and a component
   */
  protected final JPanel createInputPanel(final Attribute<?> attribute, final JComponent inputComponent,
                                          final String labelBorderLayoutConstraints) {
    return createInputPanel(attribute, inputComponent, labelBorderLayoutConstraints, JLabel.LEADING);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of {@code attribute}.
   * @param attribute the attribute from which property to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code attribute}
   * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
   * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
   * @param labelAlignment the label alignment
   * @return a panel containing a label and a component
   */
  protected final JPanel createInputPanel(final Attribute<?> attribute, final JComponent inputComponent,
                                          final String labelBorderLayoutConstraints, final int labelAlignment) {
    return createInputPanel(createLabel(attribute, labelAlignment), inputComponent, labelBorderLayoutConstraints);
  }

  /**
   * Creates a panel containing a label component and the {@code inputComponent} with the label
   * component positioned above the input component.
   * @param labelComponent the label component
   * @param inputComponent a input component
   * @return a panel containing a label and a component
   */
  protected final JPanel createInputPanel(final JComponent labelComponent, final JComponent inputComponent) {
    return createInputPanel(labelComponent, inputComponent, BorderLayout.NORTH);
  }

  /**
   * Creates a panel with a BorderLayout, with the {@code inputComponent} at BorderLayout.CENTER
   * and the {@code labelComponent} at a specified location.
   * @param labelComponent the label component
   * @param inputComponent a input component
   * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
   * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
   * @return a panel containing a label and a component
   */
  protected final JPanel createInputPanel(final JComponent labelComponent, final JComponent inputComponent,
                                          final String labelBorderLayoutConstraints) {
    requireNonNull(labelComponent, "labelComponent");
    requireNonNull(inputComponent, "inputComponent");
    if (labelComponent instanceof JLabel) {
      setLabelForComponent((JLabel) labelComponent, inputComponent);
    }
    final JPanel panel = new JPanel(Layouts.borderLayout());
    panel.add(inputComponent, BorderLayout.CENTER);
    panel.add(labelComponent, labelBorderLayoutConstraints);

    return panel;
  }

  /**
   * Creates a builder for text areas.
   * @param attribute the attribute for which to build a text area
   * @return a text area builder
   */
  protected final TextAreaBuilder textAreaBuilder(final Attribute<String> attribute) {
    return inputComponents.textAreaBuilder(attribute, getEditModel().value(attribute))
            .onBuild(textArea -> {
              EntityComponentValidators.addValidator(attribute, textArea, getEditModel());
              setComponent(attribute, textArea);
            });
  }

  /**
   * Creates a builder for text input panels.
   * @param attribute the attribute for which to build a text input panel
   * @return a text input panel builder
   */
  protected final TextInputPanelBuilder textInputPanelBuilder(final Attribute<String> attribute) {
    return inputComponents.textInputPanelBuilder(attribute, getEditModel().value(attribute))
            .transferFocusOnEnter(transferFocusOnEnter)
            .onBuild(inputPanel -> setComponent(attribute, inputPanel));
  }

  /**
   * Creates a builder for temporal input panels.
   * @param attribute the attribute for which to build a temporal input panel
   * @return a text area builder
   */
  protected final <T extends Temporal> TemporalInputPanelBuilder<T> temporalInputPanelBuilder(final Attribute<T> attribute) {
    return inputComponents.temporalInputPanelBuilder(attribute, getEditModel().value(attribute))
            .transferFocusOnEnter(transferFocusOnEnter)
            .onBuild(inputPanel -> {
              EntityComponentValidators.addFormattedValidator(attribute, inputPanel.getInputField(), getEditModel());
              setComponent(attribute, inputPanel);
            });
  }

  /**
   * Creates a builder for text fields.
   * @param attribute the attribute for which to build a text field
   * @return a text field builder
   */
  protected final <T> TextFieldBuilder<T> textFieldBuilder(final Attribute<T> attribute) {
    return inputComponents.textFieldBuilder(attribute, getEditModel().value(attribute))
            .transferFocusOnEnter(transferFocusOnEnter)
            .onBuild(textField -> setComponent(attribute, textField));
  }

  /**
   * Creates a builder for formatted text fields.
   * @param attribute the attribute for which to build a formatted text field
   * @return a formatted text field builder
   */
  protected final FormattedTextFieldBuilder formattedTextFieldBuilder(final Attribute<String> attribute) {
    return inputComponents.formattedTextFieldBuilder(attribute, getEditModel().value(attribute))
            .transferFocusOnEnter(true)
            .onBuild(textField -> {
              EntityComponentValidators.addFormattedValidator(attribute, textField, getEditModel());
              setComponent(attribute, textField);
            });
  }

  /**
   * Creates a builder for check boxes.
   * @param attribute the attribute for which to build a check box
   * @return a check box builder
   */
  protected final CheckBoxBuilder checkBoxBuilder(final Attribute<Boolean> attribute) {
    return inputComponents.checkBoxBuilder(attribute, getEditModel().value(attribute))
            .transferFocusOnEnter(transferFocusOnEnter)
            .onBuild(box -> setComponent(attribute, box));
  }

  /**
   * Creates a builder for boolean combo boxes.
   * @param attribute the attribute for which to build boolean combo box
   * @return a boolean combo box builder
   */
  protected BooleanComboBoxBuilder booleanComboBoxBuilder(final Attribute<Boolean> attribute) {
    return inputComponents.booleanComboBoxBuilder(attribute, getEditModel().value(attribute))
            .transferFocusOnEnter(transferFocusOnEnter)
            .onBuild(box -> setComponent(attribute, box));
  }

  /**
   * Creates a builder for combo boxes.
   * @param attribute the attribute for which to build combo box
   * @return a combo box builder
   */
  protected final <T> ComboBoxBuilder<T> comboBoxBuilder(final Attribute<T> attribute,
                                                         final ComboBoxModel<T> comboBoxModel) {
    return inputComponents.comboBoxBuilder(attribute, getEditModel().value(attribute), comboBoxModel)
            .transferFocusOnEnter(transferFocusOnEnter)
            .onBuild(box -> setComponent(attribute, box));
  }

  /**
   * Creates a builder for value list combo boxes.
   * @param attribute the attribute for which to build a value list combo box
   * @return a value list combo box builder
   */
  protected final <T> ValueListComboBoxBuilder<T> valueListComboBoxBuilder(final Attribute<T> attribute) {
    return inputComponents.valueListComboBoxBuilder(attribute, getEditModel().value(attribute))
            .transferFocusOnEnter(transferFocusOnEnter)
            .onBuild(box -> setComponent(attribute, box));
  }

  /**
   * Creates a builder for combo boxes, containing the values of the given attribute.
   * @param attribute the attribute for which to build a combo box
   * @return a combo box builder
   */
  protected final <T> ComboBoxBuilder<T> attributeComboBoxBuilder(final Attribute<T> attribute) {
    return inputComponents.comboBoxBuilder(attribute, getEditModel().value(attribute),
            (ComboBoxModel<T>) getEditModel().getComboBoxModel(attribute))
            .transferFocusOnEnter(transferFocusOnEnter)
            .onBuild(box -> setComponent(attribute, box));
  }

  /**
   * Creates a builder for foreign key combo boxes.
   * @param attribute the attribute for which to build a foreign key combo box
   * @return a foreign key combo box builder
   */
  protected final ForeignKeyComboBoxBuilder foreignKeyComboBoxBuilder(final ForeignKey foreignKey) {
    return inputComponents.foreignKeyComboBoxBuilder(foreignKey, getEditModel().value(foreignKey),
            getEditModel().getForeignKeyComboBoxModel(foreignKey))
            .transferFocusOnEnter(transferFocusOnEnter)
            .onBuild(box -> setComponent(foreignKey, box));
  }

  /**
   * Creates a builder for foreign key search fields.
   * @param attribute the attribute for which to build a foreign key search field
   * @return a foreign key search field builder
   */
  protected final ForeignKeySearchFieldBuilder foreignKeySearchFieldBuilder(final ForeignKey foreignKey) {
    return inputComponents.foreignKeySearchFieldBuilder(foreignKey, getEditModel().value(foreignKey),
            getEditModel().getForeignKeySearchModel(foreignKey))
            .transferFocusOnEnter(transferFocusOnEnter)
            .onBuild(searchField -> setComponent(foreignKey, searchField));
  }

  /**
   * Creates a builder for read-only foreign key fields.
   * @param attribute the attribute for which to build a foreign key field
   * @return a foreign key field builder
   */
  protected final EntityInputComponents.ForeignKeyFieldBuilder foreignKeyFieldBuilder(final ForeignKey foreignKey) {
    return inputComponents.foreignKeyFieldBuilder(foreignKey, new ForeignKeyModelValue(getEditModel(), foreignKey))
            .transferFocusOnEnter(transferFocusOnEnter)
            .onBuild(field -> setComponent(foreignKey, field));
  }

  /**
   * Creates a JLabel with a caption from {@code attribute}, if a input component exists
   * for the given attribute this label is associated with it via {@link JLabel#setLabelFor(Component)}.
   * @param attribute the attribute from which to retrieve the caption
   * @param <T> the attribute type
   * @return a JLabel for the given attribute
   */
  protected final <T> JLabel createLabel(final Attribute<T> attribute) {
    return createLabel(attribute, JLabel.LEFT);
  }

  /**
   * Creates a JLabel with a caption from {@code attribute}, if an input component exists
   * for the given attribute this label is associated with it via {@link JLabel#setLabelFor(Component)}.
   * @param attribute the attribute from which to retrieve the caption
   * @param horizontalAlignment the horizontal text alignment
   * @param <T> the attribute type
   * @return a JLabel for the given attribute
   */
  protected final <T> JLabel createLabel(final Attribute<T> attribute, final int horizontalAlignment) {
    return setLabelForComponent(inputComponents.createLabel(attribute, horizontalAlignment), getComponent(attribute));
  }

  /**
   * @return the component that should get the initial focus when the UI is cleared
   */
  protected JComponent getInitialFocusComponent() {
    if (initialFocusComponent != null) {
      return initialFocusComponent;
    }

    if (initialFocusAttribute != null) {
      return getComponent(initialFocusAttribute);
    }

    return null;
  }

  /**
   * @return the component that should get the focus when the UI is prepared after insert
   */
  protected JComponent getAfterInsertFocusComponent() {
    if (afterInsertFocusComponent != null) {
      return afterInsertFocusComponent;
    }

    if (afterInsertFocusAttribute != null) {
      return getComponent(afterInsertFocusAttribute);
    }

    return getInitialFocusComponent();
  }

  protected final void requestAfterInsertFocus() {
    requestFocus(getAfterInsertFocusComponent());
  }

  private void requestFocus(final JComponent component) {
    if (component != null && component.isFocusable()) {
      component.requestFocus();
    }
    else {
      requestFocus();
    }
  }

  /**
   * Returns true if this component can be selected, that is,
   * if it is non-null, displayable, visible, focusable and enabled.
   * @param component the component
   * @return true if this component can be selected
   */
  private static boolean isComponentSelectable(final JComponent component) {
    return component != null && component.isDisplayable() &&
            component.isVisible() && component.isFocusable() && component.isEnabled();
  }

  private static JLabel setLabelForComponent(final JLabel label, final JComponent component) {
    if (component != null && label.getLabelFor() != component) {
      label.setLabelFor(component);
    }

    return label;
  }

  private static final class ForeignKeyModelValue extends AbstractValue<Entity> {

    private final EntityEditModel editModel;
    private final ForeignKey foreignKey;

    private ForeignKeyModelValue(final EntityEditModel editModel, final ForeignKey foreignKey) {
      this.editModel = editModel;
      this.foreignKey = foreignKey;
      editModel.addValueListener(foreignKey, valueChange -> notifyValueChange());
    }

    @Override
    public Entity get() {
      return editModel.getForeignKey(foreignKey);
    }

    @Override
    protected void setValue(final Entity value) {/*read only*/}
  }
}
