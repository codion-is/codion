/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

final class DefaultInputDialogBuilder<T> implements InputDialogBuilder<T> {

  private final JPanel basePanel = new JPanel(Layouts.borderLayout());
  private final ComponentValue<T, ?> componentValue;
  private final OkCancelDialogBuilder okCancelDialogBuilder = new DefaultOkCancelDialogBuilder(basePanel);

  private String caption;

  DefaultInputDialogBuilder(ComponentValue<T, ?> componentValue) {
    this.componentValue = requireNonNull(componentValue);
    this.basePanel.add(componentValue.component(), BorderLayout.CENTER);
    this.basePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
  }

  @Override
  public InputDialogBuilder<T> owner(Window owner) {
    okCancelDialogBuilder.owner(owner);
    return this;
  }

  @Override
  public InputDialogBuilder<T> owner(Component owner) {
    okCancelDialogBuilder.owner(owner);
    return this;
  }

  @Override
  public InputDialogBuilder<T> locationRelativeTo(Component component) {
    okCancelDialogBuilder.locationRelativeTo(component);
    return this;
  }

  @Override
  public InputDialogBuilder<T> location(Point location) {
    okCancelDialogBuilder.location(location);
    return this;
  }

  @Override
  public InputDialogBuilder<T> titleProvider(ValueObserver<String> titleProvider) {
    okCancelDialogBuilder.titleProvider(titleProvider);
    return this;
  }

  @Override
  public InputDialogBuilder<T> icon(ImageIcon icon) {
    okCancelDialogBuilder.icon(icon);
    return this;
  }

  @Override
  public InputDialogBuilder<T> title(String title) {
    okCancelDialogBuilder.title(title);
    return this;
  }

  @Override
  public InputDialogBuilder<T> caption(String caption) {
    this.caption = caption;
    return this;
  }

  @Override
  public InputDialogBuilder<T> inputValidObserver(StateObserver inputValidObserver) {
    okCancelDialogBuilder.okEnabledObserver(inputValidObserver);
    return this;
  }

  @Override
  public InputDialogBuilder<T> inputValidator(Predicate<T> validInputPredicate) {
    return inputValidObserver(createInputValidObserver(requireNonNull(validInputPredicate)));
  }

  @Override
  public InputDialogBuilder<T> keyEvent(KeyEvents.Builder keyEventBuilder) {
    okCancelDialogBuilder.keyEvent(keyEventBuilder);
    return this;
  }

  @Override
  public T show() {
    State okPressed = State.state();
    if (caption != null) {
      basePanel.add(new JLabel(caption), BorderLayout.NORTH);
    }
    okCancelDialogBuilder.onOk(new OnOk(componentValue, okPressed)).show();
    if (okPressed.get()) {
      return componentValue.get();
    }

    throw new CancelException();
  }

  private StateObserver createInputValidObserver(Predicate<T> inputValidator) {
    State validInputState = State.state(inputValidator.test(componentValue.get()));
    componentValue.addListener(new InputValidStateListener<>(validInputState, inputValidator, componentValue));

    return validInputState;
  }

  private static final class OnOk implements Runnable {

    private final ComponentValue<?, ?> componentValue;
    private final State okPressed;

    private OnOk(ComponentValue<?, ?> componentValue, State okPressed) {
      this.componentValue = componentValue;
      this.okPressed = okPressed;
    }

    @Override
    public void run() {
      Utilities.parentDialog(componentValue.component()).dispose();
      okPressed.set(true);
    }
  }

  private static final class InputValidStateListener<T> implements Runnable {

    private final State validInputState;
    private final Predicate<T> validInputPredicate;
    private final Value<T> componentValue;

    private InputValidStateListener(State validInputState, Predicate<T> validInputPredicate, Value<T> componentValue) {
      this.validInputState = validInputState;
      this.validInputPredicate = validInputPredicate;
      this.componentValue = componentValue;
    }

    @Override
    public void run() {
      validInputState.set(validInputPredicate.test(componentValue.get()));
    }
  }
}
