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
  private Value.Validator<T> validator;
  private T initialValue;
  private Consumer<C> onSetVisible;

  protected AbstractComponentBuilder() {
    this(null);
  }

  /**
   * @param linkedValue the linked value, may be null
   */
  protected AbstractComponentBuilder(final Value<T> linkedValue) {
    this.linkedValue = linkedValue;
  }

  @Override
  public final B focusable(final boolean focusable) {
    this.focusable = focusable;
    return (B) this;
  }

  @Override
  public final B preferredHeight(final int preferredHeight) {
    this.preferredHeight = preferredHeight;
    return (B) this;
  }

  @Override
  public final B preferredWidth(final int preferredWidth) {
    this.preferredWidth = preferredWidth;
    return (B) this;
  }

  @Override
  public final B preferredSize(final Dimension preferredSize) {
    this.preferredHeight = preferredSize == null ? 0 : preferredSize.height;
    this.preferredWidth = preferredSize == null ? 0 : preferredSize.width;
    return (B) this;
  }

  @Override
  public final B maximumHeight(final int maximumHeight) {
    this.maximumHeight = maximumHeight;
    return (B) this;
  }

  @Override
  public final B maximumWidth(final int maximumWidth) {
    this.maximumWidth = maximumWidth;
    return (B) this;
  }

  @Override
  public final B maximumSize(final Dimension maximumSize) {
    this.maximumHeight = maximumSize == null ? 0 : maximumSize.height;
    this.maximumWidth = maximumSize == null ? 0 : maximumSize.width;
    return (B) this;
  }

  @Override
  public final B minimumHeight(final int minimumHeight) {
    this.minimumHeight = minimumHeight;
    return (B) this;
  }

  @Override
  public final B minimumWidth(final int minimumWidth) {
    this.minimumWidth = minimumWidth;
    return (B) this;
  }

  @Override
  public final B minimumSize(final Dimension minimumSize) {
    this.minimumHeight = minimumSize == null ? 0 : minimumSize.height;
    this.minimumWidth = minimumSize == null ? 0 : minimumSize.width;
    return (B) this;
  }

  @Override
  public final B border(final Border border) {
    this.border = border;
    return (B) this;
  }

  @Override
  public final B transferFocusOnEnter(final boolean transferFocusOnEnter) {
    this.transferFocusOnEnter = transferFocusOnEnter;
    return (B) this;
  }

  @Override
  public final B enabled(final boolean enabled) {
    this.enabled = enabled;
    return (B) this;
  }

  @Override
  public final B enabledState(final StateObserver enabledState) {
    this.enabledState = enabledState;
    return (B) this;
  }

  @Override
  public final B popupMenuControl(final Control popupMenuControl) {
    return popupMenuControls(Controls.controls(popupMenuControl));
  }

  @Override
  public final B popupMenuControls(final Controls popupMenuControls) {
    return popupMenu(requireNonNull(popupMenuControls).createPopupMenu());
  }

  @Override
  public final B popupMenu(final JPopupMenu popupMenu) {
    this.popupMenu = popupMenu;
    return (B) this;
  }

  @Override
  public final B toolTipText(final String toolTipText) {
    this.toolTipText = toolTipText;
    return (B) this;
  }

  @Override
  public final B font(final Font font) {
    this.font = font;
    return (B) this;
  }

  @Override
  public final B foreground(final Color foreground) {
    this.foreground = foreground;
    return (B) this;
  }

  @Override
  public final B background(final Color background) {
    this.background = background;
    return (B) this;
  }

  @Override
  public final B componentOrientation(final ComponentOrientation componentOrientation) {
    this.componentOrientation = requireNonNull(componentOrientation);
    return (B) this;
  }

  @Override
  public final B validator(final Value.Validator<T> validator) {
    this.validator = validator;
    return (B) this;
  }

  @Override
  public final B keyEvent(final KeyEvents.Builder keyEventBuilder) {
    this.keyEventBuilders.add(requireNonNull(keyEventBuilder));
    return (B) this;
  }

  @Override
  public final B clientProperty(final Object key, final Object value) {
    this.clientProperties.put(key, value);
    return (B) this;
  }

  @Override
  public final B focusListener(final FocusListener focusListener) {
    this.focusListeners.add(requireNonNull(focusListener));
    return (B) this;
  }

  @Override
  public final B mouseListener(final MouseListener mouseListener) {
    this.mouseListeners.add(requireNonNull(mouseListener));
    return (B) this;
  }

  @Override
  public final B mouseMotionListener(final MouseMotionListener mouseMotionListener) {
    this.mouseMotionListeners.add(requireNonNull(mouseMotionListener));
    return (B) this;
  }

  @Override
  public final B mouseWheelListener(final MouseWheelListener mouseWheelListener) {
    this.mouseWheelListeners.add(requireNonNull(mouseWheelListener));
    return (B) this;
  }

  @Override
  public final B keyListener(final KeyListener keyListener) {
    this.keyListeners.add(requireNonNull(keyListener));
    return (B) this;
  }

  @Override
  public final B componentListener(final ComponentListener componentListener) {
    this.componentListeners.add(requireNonNull(componentListener));
    return (B) this;
  }

  @Override
  public final B onSetVisible(final Consumer<C> onSetVisible) {
    this.onSetVisible = onSetVisible;
    return (B) this;
  }

  @Override
  public final B linkedValue(final Value<T> linkedValue) {
    if (linkedValueObserver != null) {
      throw new IllegalStateException("linkeValueObserver has already been set");
    }
    this.linkedValue = linkedValue;
    return (B) this;
  }

  @Override
  public final B linkedValueObserver(final ValueObserver<T> linkedValueObserver) {
    if (linkedValue != null) {
      throw new IllegalStateException("linkedValue has already been set");
    }
    this.linkedValueObserver = linkedValueObserver;
    return (B) this;
  }

  @Override
  public final B initialValue(final T initialValue) {
    this.initialValue = requireNonNull(initialValue);
    return (B) this;
  }

  @Override
  public final ScrollPaneBuilder scrollPane() {
    return new DefaultScrollPaneBuilder(build());
  }

  @Override
  public final B onBuild(final Consumer<C> onBuild) {
    buildEvent.addDataListener(onBuild::accept);
    return (B) this;
  }

  @Override
  public final C build() {
    return build(null);
  }

  @Override
  public final C build(final Consumer<C> onBuild) {
    if (component != null) {
      return component;
    }
    component = buildComponent();
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
    if (initialValue != null) {
      setInitialValue(component, initialValue);
    }
    if (validator != null) {
      buildComponentValue().addValidator(validator);
    }
    if (linkedValue != null) {
      buildComponentValue().link(linkedValue);
    }
    if (linkedValueObserver != null) {
      buildComponentValue().link(linkedValueObserver);
    }
    buildEvent.onEvent(component);
    if (onBuild != null) {
      onBuild.accept(component);
    }

    return component;
  }

  @Override
  public final ComponentValue<T, C> buildComponentValue() {
    if (componentValue != null) {
      return componentValue;
    }
    componentValue = buildComponentValue(build());

    return componentValue;
  }

  @Override
  public final B clear() {
    component = null;
    componentValue = null;

    return (B) this;
  }

  /**
   * Builds the component.
   * @return a new component instance
   */
  protected abstract C buildComponent();

  /**
   * @param component the component
   * @return a component value based on the component
   */
  protected abstract ComponentValue<T, C> buildComponentValue(final C component);

  /**
   * Sets the initial value in the component, only called for non-null values.
   * @param component the component
   * @param initialValue the initial value, not null
   */
  protected abstract void setInitialValue(final C component, final T initialValue);

  /**
   * Enables focus transfer on Enter, override for special handling
   * @param component the component
   */
  protected void setTransferFocusOnEnter(final C component) {
    TransferFocusOnEnter.enable(component);
  }

  private void setSizes(final C component) {
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

    private OnSetVisible(final C component, final Consumer<C> consumer) {
      this.component = component;
      this.consumer = consumer;
      this.component.addAncestorListener(this);
    }

    @Override
    public void ancestorAdded(final AncestorEvent event) {
      consumer.accept(component);
      component.removeAncestorListener(this);
    }

    @Override
    public void ancestorRemoved(final AncestorEvent event) {/*Not necessary*/}

    @Override
    public void ancestorMoved(final AncestorEvent event) {/*Not necessary*/}
  }
}
