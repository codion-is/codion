/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.CancelException;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ItemColumnDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.dialog.AbstractDialogBuilder;
import is.codion.swing.common.ui.dialog.DialogBuilder;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.component.DefaultEntityComponentFactory;
import is.codion.swing.framework.ui.component.EntityComponentFactory;
import is.codion.swing.framework.ui.component.EntityComponents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static is.codion.swing.common.ui.Utilities.parentWindow;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

/**
 * Provides a dialog for editing a single attribute for one or more entities.
 */
public final class EntityEditDialog {

  private static final Logger LOG = LoggerFactory.getLogger(EntityEditDialog.class);

  /**
   * @param editModel the edit model to use
   * @param attribute the attribute to edit
   * @return a new builder
   * @param <T> the attribute type
   */
  public static <T> Builder<T> builder(SwingEntityEditModel editModel, Attribute<T> attribute) {
    return new DefaultBuilder<>(editModel, attribute);
  }

  /**
   * Builds a dialog for editing single attributes for one or more entities
   * @param <T> the attribute type
   */
  public interface Builder<T> extends DialogBuilder<Builder<T>> {

    /**
     * @param componentFactory the component factory, if null then the default is used
     * @return this builder
     */
    Builder<T> componentFactory(EntityComponentFactory<T, Attribute<T>, ?> componentFactory);

    /**
     * @param onValidationException called on validation exception
     * @return this builder
     */
    Builder<T> onValidationException(Consumer<ValidationException> onValidationException);

    /**
     * @param onException called on exception
     * @return this builder
     */
    Builder<T> onException(Consumer<Exception> onException);

    /**
     * @param multipleEntityUpdateEnabled false if multiple entity update should not be enabled
     * @return this builder
     */
    Builder<T> multipleEntityUpdateEnabled(boolean multipleEntityUpdateEnabled);

    /**
     * Displays a dialog for editing the given entity
     * @param entity the entity to edit
     */
    void edit(Entity entity);

    /**
     * Displays a dialog for editing the given entities
     * @param entities the entities to edit
     */
    void edit(Collection<? extends Entity> entities);
  }

  private static final class DefaultBuilder<T> extends AbstractDialogBuilder<Builder<T>> implements Builder<T> {

    private final SwingEntityEditModel editModel;
    private final Attribute<T> attribute;

    private EntityComponentFactory<T, Attribute<T>, ?> componentFactory = new EditEntityComponentFactory<>();
    private Consumer<ValidationException> onValidationException = new DefaultOnValidationException();
    private Consumer<Exception> onException = new DefaultOnException();
    private boolean multipleEntityUpdateEnabled = true;

    private DefaultBuilder(SwingEntityEditModel editModel, Attribute<T> attribute) {
      this.editModel = requireNonNull(editModel);
      this.attribute = requireNonNull(attribute);
    }

    @Override
    public Builder<T> componentFactory(EntityComponentFactory<T, Attribute<T>, ?> componentFactory) {
      this.componentFactory = componentFactory == null ? new EditEntityComponentFactory<>() : componentFactory;
      return this;
    }

    @Override
    public Builder<T> onValidationException(Consumer<ValidationException> onValidationException) {
      this.onValidationException = requireNonNull(onValidationException);
      return this;
    }

    @Override
    public Builder<T> onException(Consumer<Exception> onException) {
      this.onException = requireNonNull(onException);
      return this;
    }

    @Override
    public Builder<T> multipleEntityUpdateEnabled(boolean multipleEntityUpdateEnabled) {
      this.multipleEntityUpdateEnabled = multipleEntityUpdateEnabled;
      return this;
    }

    @Override
    public void edit(Entity entity) {
      edit(Collections.singleton(requireNonNull(entity)));
    }

    @Override
    public void edit(Collection<? extends Entity> entities) {
      Set<EntityType> entityTypes = requireNonNull(entities).stream()
              .map(Entity::entityType)
              .collect(toSet());
      if (entityTypes.isEmpty()) {
        return;//no entities
      }
      if (entityTypes.size() > 1) {
        throw new IllegalArgumentException("All entities must be of the same type when editing");
      }
      if (entities.size() > 1 && !multipleEntityUpdateEnabled) {
        throw new IllegalStateException("Updating multiple entities is not allowed");
      }

      AttributeDefinition<T> attributeDefinition = editModel.entityDefinition().attributeDefinition(attribute);
      Collection<Entity> selectedEntities = Entity.copy(entities);
      Collection<T> values = Entity.distinct(attribute, selectedEntities);
      T initialValue = values.size() == 1 ? values.iterator().next() : null;
      ComponentValue<T, ?> componentValue = editSelectedComponentValue(attribute, initialValue);
      InputValidator<T> inputValidator = new InputValidator<>(attributeDefinition, componentValue);
      boolean updatePerformed = false;
      while (!updatePerformed) {
        T newValue = Dialogs.inputDialog(componentValue)
                .owner(owner)
                .title(FrameworkMessages.edit())
                .caption(attributeDefinition.caption())
                .inputValidator(inputValidator)
                .show();
        Entity.put(attribute, newValue, selectedEntities);
        updatePerformed = update(selectedEntities);
      }
    }

    private ComponentValue<T, ? extends JComponent> editSelectedComponentValue(Attribute<T> attribute, T initialValue) {
      if (componentFactory == null) {
        return ((EntityComponentFactory<T, Attribute<T>, ?>) new EditEntityComponentFactory<T, Attribute<T>, JComponent>())
                .createComponentValue(attribute, editModel, initialValue);
      }

      return componentFactory.createComponentValue(attribute, editModel, initialValue);
    }

    private boolean update(Collection<Entity> entities) {
      try {
        editModel.update(entities);

        return true;
      }
      catch (CancelException ignored) {}
      catch (ValidationException e) {
        LOG.debug(e.getMessage(), e);
        onValidationException.accept(e);
      }
      catch (Exception e) {
        LOG.error(e.getMessage(), e);
        onException.accept(e);
      }

      return false;
    }

    private void displayException(Throwable exception) {
      Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
      if (focusOwner == null) {
        focusOwner = owner;
      }
      Dialogs.displayExceptionDialog(exception, parentWindow(focusOwner));
    }

    private final class DefaultOnException implements Consumer<Exception> {
      @Override
      public void accept(Exception e) {
        displayException(e);
      }
    }

    private final class DefaultOnValidationException implements Consumer<ValidationException> {

      @Override
      public void accept(ValidationException exception) {
        requireNonNull(exception);
        String title = editModel.entityDefinition()
                .attributeDefinition(exception.attribute())
                .caption();
        JOptionPane.showMessageDialog(owner, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
      }
    }

    private static final class InputValidator<T> implements Predicate<T> {

      private final AttributeDefinition<T> attributeDefinition;
      private final ComponentValue<T, ?> componentValue;

      private InputValidator(AttributeDefinition<T> attributeDefinition, ComponentValue<T, ?> componentValue) {
        this.attributeDefinition = attributeDefinition;
        this.componentValue = componentValue;
      }

      @Override
      public boolean test(T value) {
        if (value == null && !attributeDefinition.isNullable()) {
          return false;
        }
        try {
          componentValue.validators().forEach(validator -> validator.validate(value));
        }
        catch (IllegalArgumentException e) {
          return false;
        }

        return true;
      }
    }
  }

  private static final class EditEntityComponentFactory<T, A extends Attribute<T>, C extends JComponent> extends DefaultEntityComponentFactory<T, A, C> {

    @Override
    public ComponentValue<T, C> createComponentValue(A attribute, SwingEntityEditModel editModel, T initialValue) {
      AttributeDefinition<T> attributeDefinition = editModel.entityDefinition().attributeDefinition(attribute);
      if (!(attributeDefinition instanceof ItemColumnDefinition) && attribute.type().isString()) {
        //special handling for non-item based String attributes, text input panel instead of a text field
        return (ComponentValue<T, C>) new EntityComponents(editModel.entityDefinition())
                .textInputPanel((Attribute<String>) attribute)
                .initialValue((String) initialValue)
                .buildValue();
      }

      return super.createComponentValue(attribute, editModel, initialValue);
    }
  }
}
