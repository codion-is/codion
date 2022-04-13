/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.Event;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.Sizes.*;
import static is.codion.swing.common.ui.Utilities.linkToEnabledState;
import static java.util.Objects.requireNonNull;

public abstract class AbstractComponentBuilder<T, C extends JComponent, B extends ComponentBuilder<T, C, B>> implements ComponentBuilder<T, C, B> {

  private final Event<C> buildEvent = Event.event();
  private final List<KeyEvents.Builder> keyEventBuilders = new ArrayList<>(1);
  private final Map<Object, Object> clientProperties = new HashMap<>();
  private final List<FocusListener> focusListeners = new ArrayList<>();
  private final List<MouseListener> mouseListeners = new ArrayList<>();
  private final List<MouseMotionListener> mouseMotionListeners = new ArrayList<>();
  private final List<MouseWheelListener> mouseWheelListeners = new ArrayList<>();
  private final List<KeyListener> keyListeners = new ArrayList<>();
  private final List<ComponentListener> componentListeners = new ArrayList<>();
  private final List<Value.Validator<T>> validators = new ArrayList<>();

  private C component;
  private ComponentValue<T, C> componentValue;

  private boolean focusable = true;
  private int preferredHeight;
  private int preferredWidth;
  private int minimumHeight;
  private int minimumWidth;
  private int maximumHeight;
  private int maximumWidth;
  private Border border;
  private boolean transferFocusOnEnter = TRANSFER_FOCUS_ON_ENTER.get();
  private String toolTipText;
  private Font font;
  private Color foreground;
  private Color background;
  private ComponentOrientation componentOrientation = ComponentOrientation.UNKNOWN;
  private StateObserver enabledState;
  private boolean enabled = true;
  private JPopupMenu popupMenu;
  private Value<T> linkedValue;
  private ValueObserver<T> linkedValueObserver;
  private T initialValue;
  private Consumer<C> onSetVisible;

  /**
   * When a linked value is set via the constructor, it is locked and cannot be changed.
   */
  private final boolean linkedValueLocked;

  protected AbstractComponentBuilder() {
    this(null);
  }

  /**
   * Note that when a linked value is set via the constructor,
   * it is considered locked and cannot be changed.
   * @param linkedValue the linked value, may be null
   */
  protected AbstractComponentBuilder(Value<T> linkedValue) {
    this.linkedValue = linkedValue;
    this.linkedValueLocked = linkedValue != null;
  }

  @Override
  public final B focusable(boolean focusable) {
    this.focusable = focusable;
    return (B) this;
  }

  @Override
  public final B preferredHeight(int preferredHeight) {
    this.preferredHeight = preferredHeight;
    return (B) this;
  }

  @Override
  public final B preferredWidth(int preferredWidth) {
    this.preferredWidth = preferredWidth;
    return (B) this;
  }

  @Override
  public final B preferredSize(Dimension preferredSize) {
    this.preferredHeight = preferredSize == null ? 0 : preferredSize.height;
    this.preferredWidth = preferredSize == null ? 0 : preferredSize.width;
    return (B) this;
  }

  @Override
  public final B maximumHeight(int maximumHeight) {
    this.maximumHeight = maximumHeight;
    return (B) this;
  }

  @Override
  public final B maximumWidth(int maximumWidth) {
    this.maximumWidth = maximumWidth;
    return (B) this;
  }

  @Override
  public final B maximumSize(Dimension maximumSize) {
    this.maximumHeight = maximumSize == null ? 0 : maximumSize.height;
    this.maximumWidth = maximumSize == null ? 0 : maximumSize.width;
    return (B) this;
  }

  @Override
  public final B minimumHeight(int minimumHeight) {
    this.minimumHeight = minimumHeight;
    return (B) this;
  }

  @Override
  public final B minimumWidth(int minimumWidth) {
    this.minimumWidth = minimumWidth;
    return (B) this;
  }

  @Override
  public final B minimumSize(Dimension minimumSize) {
    this.minimumHeight = minimumSize == null ? 0 : minimumSize.height;
    this.minimumWidth = minimumSize == null ? 0 : minimumSize.width;
    return (B) this;
  }

  @Override
  public final B border(Border border) {
    this.border = border;
    return (B) this;
  }

  @Override
  public final B transferFocusOnEnter(boolean transferFocusOnEnter) {
    this.transferFocusOnEnter = transferFocusOnEnter;
    return (B) this;
  }

  @Override
  public final B enabled(boolean enabled) {
    this.enabled = enabled;
    return (B) this;
  }

  @Override
  public final B enabledState(StateObserver enabledState) {
    this.enabledState = enabledState;
    return (B) this;
  }

  @Override
  public final B popupMenuControl(Control popupMenuControl) {
    return popupMenuControls(Controls.controls(popupMenuControl));
  }

  @Override
  public final B popupMenuControls(Controls popupMenuControls) {
    return popupMenu(requireNonNull(popupMenuControls).createPopupMenu());
  }

  @Override
  public final B popupMenu(JPopupMenu popupMenu) {
    this.popupMenu = popupMenu;
    return (B) this;
  }

  @Override
  public final B toolTipText(String toolTipText) {
    this.toolTipText = toolTipText;
    return (B) this;
  }

  @Override
  public final B font(Font font) {
    this.font = font;
    return (B) this;
  }

  @Override
  public final B foreground(Color foreground) {
    this.foreground = foreground;
    return (B) this;
  }

  @Override
  public final B background(Color background) {
    this.background = background;
    return (B) this;
  }

  @Override
  public final B componentOrientation(ComponentOrientation componentOrientation) {
    this.componentOrientation = requireNonNull(componentOrientation);
    return (B) this;
  }

  @Override
  public final B validator(Value.Validator<T> validator) {
    this.validators.add(requireNonNull(validator));
    return (B) this;
  }

  @Override
  public final B keyEvent(KeyEvents.Builder keyEventBuilder) {
    this.keyEventBuilders.add(requireNonNull(keyEventBuilder));
    return (B) this;
  }

  @Override
  public final B clientProperty(Object key, Object value) {
    this.clientProperties.put(key, value);
    return (B) this;
  }

  @Override
  public final B focusListener(FocusListener focusListener) {
    this.focusListeners.add(requireNonNull(focusListener));
    return (B) this;
  }

  @Override
  public final B mouseListener(MouseListener mouseListener) {
    this.mouseListeners.add(requireNonNull(mouseListener));
    return (B) this;
  }

  @Override
  public final B mouseMotionListener(MouseMotionListener mouseMotionListener) {
    this.mouseMotionListeners.add(requireNonNull(mouseMotionListener));
    return (B) this;
  }

  @Override
  public final B mouseWheelListener(MouseWheelListener mouseWheelListener) {
    this.mouseWheelListeners.add(requireNonNull(mouseWheelListener));
    return (B) this;
  }

  @Override
  public final B keyListener(KeyListener keyListener) {
    this.keyListeners.add(requireNonNull(keyListener));
    return (B) this;
  }

  @Override
  public final B componentListener(ComponentListener componentListener) {
    this.componentListeners.add(requireNonNull(componentListener));
    return (B) this;
  }

  @Override
  public final B onSetVisible(Consumer<C> onSetVisible) {
    this.onSetVisible = onSetVisible;
    return (B) this;
  }

  @Override
  public final B linkedValue(Value<T> linkedValue) {
    if (linkedValueLocked) {
      throw new IllegalStateException("The value for this component builder has already been set");
    }
    if (linkedValueObserver != null) {
      throw new IllegalStateException("linkeValueObserver has already been set");
    }
    this.linkedValue = linkedValue;
    return (B) this;
  }

  @Override
  public final B linkedValueObserver(ValueObserver<T> linkedValueObserver) {
    if (linkedValueLocked) {
      throw new IllegalStateException("The value for this component builder has already been set");
    }
    if (linkedValue != null) {
      throw new IllegalStateException("linkedValue has already been set");
    }
    this.linkedValueObserver = linkedValueObserver;
    return (B) this;
  }

  @Override
  public final B initialValue(T initialValue) {
    this.initialValue = initialValue;
    return (B) this;
  }

  @Override
  public final ScrollPaneBuilder scrollPane() {
    return new DefaultScrollPaneBuilder(build());
  }

  @Override
  public final B onBuild(Consumer<C> onBuild) {
    buildEvent.addDataListener(onBuild::accept);
    return (B) this;
  }

  @Override
  public final C build() {
    return build(null);
  }

  @Override
  public final C build(Consumer<C> onBuild) {
    if (component != null) {
      return component;
    }
    component = createComponent();
    if (component.isFocusable() && !focusable) {
      component.setFocusable(false);
    }
    setSizes(component);
    if (border != null) {
      component.setBorder(border);
    }
    if (!enabled) {
      component.setEnabled(false);
    }
    if (enabledState != null) {
      linkToEnabledState(enabledState, component);
    }
    if (popupMenu != null) {
      component.setComponentPopupMenu(popupMenu);
    }
    if (toolTipText != null) {
      component.setToolTipText(toolTipText);
    }
    if (font != null) {
      component.setFont(font);
    }
    if (foreground != null) {
      component.setForeground(foreground);
    }
    if (background != null) {
      component.setBackground(background);
    }
    component.setComponentOrientation(componentOrientation);
    clientProperties.forEach((key, value) -> component.putClientProperty(key, value));
    keyEventBuilders.forEach(keyEventBuilder -> keyEventBuilder.enable(component));
    focusListeners.forEach(focusListener -> component.addFocusListener(focusListener));
    mouseListeners.forEach(mouseListener -> component.addMouseListener(mouseListener));
    mouseMotionListeners.forEach(mouseMotionListener -> component.addMouseMotionListener(mouseMotionListener));
    mouseWheelListeners.forEach(mouseWheelListener -> component.addMouseWheelListener(mouseWheelListener));
    keyListeners.forEach(keyListener -> component.addKeyListener(keyListener));
    componentListeners.forEach(componentListener -> component.addComponentListener(componentListener));
    if (onSetVisible != null) {
      new OnSetVisible<>(component, onSetVisible);
    }
    if (transferFocusOnEnter) {
      setTransferFocusOnEnter(component);
    }
    buildEvent.onEvent(component);
    if (onBuild != null) {
      onBuild.accept(component);
    }
    validators.forEach(validator -> getComponentValue(component).addValidator(validator));
    if (initialValue != null) {
      setInitialValue(component, initialValue);
    }
    if (linkedValue != null) {
      getComponentValue(component).link(linkedValue);
    }
    if (linkedValueObserver != null) {
      getComponentValue(component).link(linkedValueObserver);
    }

    return component;
  }

  @Override
  public final ComponentValue<T, C> buildComponentValue() {
    if (componentValue != null) {
      return componentValue;
    }
    build();//initializes the component value if required
    if (componentValue == null) {
      //try to intialize the component value if build() did not
      componentValue = createComponentValue(component);
    }

    return componentValue;
  }

  @Override
  public final B clear() {
    component = null;
    componentValue = null;

    return (B) this;
  }

  /**
   * Creates the component.
   * @return a new component instance
   */
  protected abstract C createComponent();

  /**
   * Creates the component value
   * @param component the component
   * @return a component value based on the component
   */
  protected abstract ComponentValue<T, C> createComponentValue(C component);

  /**
   * Sets the initial value in the component, only called for non-null values.
   * @param component the component
   * @param initialValue the initial value, not null
   */
  protected abstract void setInitialValue(C component, T initialValue);

  /**
   * Enables focus transfer on Enter, override for special handling
   * @param component the component
   */
  protected void setTransferFocusOnEnter(C component) {
    TransferFocusOnEnter.enable(component);
  }

  private ComponentValue<T, C> getComponentValue(C component) {
    if (componentValue == null) {
      componentValue = createComponentValue(component);
    }

    return componentValue;
  }

  private void setSizes(C component) {
    if (minimumHeight > 0) {
      setMinimumHeight(component, minimumHeight);
    }
    if (minimumWidth > 0) {
      setMinimumWidth(component, minimumWidth);
    }
    if (maximumHeight > 0) {
      setMaximumHeight(component, maximumHeight);
    }
    if (maximumWidth > 0) {
      setMaximumWidth(component, maximumWidth);
    }
    if (preferredHeight > 0) {
      setPreferredHeight(component, preferredHeight);
    }
    if (preferredWidth > 0) {
      setPreferredWidth(component, preferredWidth);
    }
  }

  private static final class OnSetVisible<C extends JComponent> implements AncestorListener {

    private final C component;
    private final Consumer<C> consumer;

    private OnSetVisible(C component, Consumer<C> consumer) {
      this.component = component;
      this.consumer = consumer;
      this.component.addAncestorListener(this);
    }

    @Override
    public void ancestorAdded(AncestorEvent event) {
      consumer.accept(component);
      component.removeAncestorListener(this);
    }

    @Override
    public void ancestorRemoved(AncestorEvent event) {/*Not necessary*/}

    @Override
    public void ancestorMoved(AncestorEvent event) {/*Not necessary*/}
  }
}
