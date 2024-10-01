/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.builder;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.component.button.MenuBuilder;
import is.codion.swing.common.ui.component.scrollpane.ScrollPaneBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;
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
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static is.codion.swing.common.ui.Sizes.*;
import static is.codion.swing.common.ui.Utilities.linkToEnabledState;
import static java.awt.ComponentOrientation.getOrientation;
import static java.util.Objects.requireNonNull;

public abstract class AbstractComponentBuilder<T, C extends JComponent, B extends ComponentBuilder<T, C, B>>
				implements ComponentBuilder<T, C, B> {

	private final List<Consumer<C>> buildConsumers = new ArrayList<>(1);
	private final List<Consumer<ComponentValue<T, C>>> buildValueConsumers = new ArrayList<>(1);
	private final List<Value<T>> linkedValues = new ArrayList<>(1);
	private final List<ValueObserver<T>> linkedValueObservers = new ArrayList<>(1);
	private final List<KeyEvents.Builder> keyEventBuilders = new ArrayList<>(1);
	private final Map<Object, Object> clientProperties = new HashMap<>();
	private final List<FocusListener> focusListeners = new ArrayList<>();
	private final List<MouseListener> mouseListeners = new ArrayList<>();
	private final List<MouseMotionListener> mouseMotionListeners = new ArrayList<>();
	private final List<MouseWheelListener> mouseWheelListeners = new ArrayList<>();
	private final List<KeyListener> keyListeners = new ArrayList<>();
	private final List<ComponentListener> componentListeners = new ArrayList<>();
	private final List<PropertyChangeListener> propertyChangeListeners = new ArrayList<>();
	private final Map<String, PropertyChangeListener> propertyChangeListenerMap = new HashMap<>();
	private final List<Value.Validator<T>> validators = new ArrayList<>();
	private final List<Runnable> listeners = new ArrayList<>();
	private final List<Consumer<T>> consumers = new ArrayList<>();

	private JLabel label;
	private boolean focusable = true;
	private int preferredHeight = -1;
	private int preferredWidth = -1;
	private int minimumHeight = -1;
	private int minimumWidth = -1;
	private int maximumHeight = -1;
	private int maximumWidth = -1;
	private boolean opaque = false;
	private boolean visible = true;
	private Border border;
	private boolean transferFocusOnEnter = TRANSFER_FOCUS_ON_ENTER.get();
	private String toolTipText;
	private Font font;
	private Color foreground;
	private Color background;
	private ComponentOrientation componentOrientation = getOrientation(Locale.getDefault());
	private StateObserver enabledObserver;
	private boolean enabled = true;
	private Function<C, JPopupMenu> popupMenu;
	private T value;
	private boolean valueSet = false;
	private Consumer<C> onSetVisible;
	private TransferHandler transferHandler;
	private boolean focusCycleRoot = false;

	protected AbstractComponentBuilder() {
		this(null);
	}

	/**
	 * Note that when a linked value is set via the constructor,
	 * it is considered locked and cannot be changed.
	 * @param linkedValue the linked value, may be null
	 */
	protected AbstractComponentBuilder(Value<T> linkedValue) {
		if (linkedValue != null) {
			link(linkedValue);
		}
	}

	@Override
	public final B label(JLabel label) {
		this.label = label;
		return self();
	}

	@Override
	public final B focusable(boolean focusable) {
		this.focusable = focusable;
		return self();
	}

	@Override
	public final B preferredHeight(int preferredHeight) {
		this.preferredHeight = validatePositiveInteger(preferredHeight);
		return self();
	}

	@Override
	public final B preferredWidth(int preferredWidth) {
		this.preferredWidth = validatePositiveInteger(preferredWidth);
		return self();
	}

	@Override
	public final B preferredSize(Dimension preferredSize) {
		this.preferredHeight = preferredSize == null ? -1 : preferredSize.height;
		this.preferredWidth = preferredSize == null ? -1 : preferredSize.width;
		return self();
	}

	@Override
	public final B maximumHeight(int maximumHeight) {
		this.maximumHeight = validatePositiveInteger(maximumHeight);
		return self();
	}

	@Override
	public final B maximumWidth(int maximumWidth) {
		this.maximumWidth = validatePositiveInteger(maximumWidth);
		return self();
	}

	@Override
	public final B maximumSize(Dimension maximumSize) {
		this.maximumHeight = maximumSize == null ? -1 : maximumSize.height;
		this.maximumWidth = maximumSize == null ? -1 : maximumSize.width;
		return self();
	}

	@Override
	public final B minimumHeight(int minimumHeight) {
		this.minimumHeight = validatePositiveInteger(minimumHeight);
		return self();
	}

	@Override
	public final B minimumWidth(int minimumWidth) {
		this.minimumWidth = validatePositiveInteger(minimumWidth);
		return self();
	}

	@Override
	public final B minimumSize(Dimension minimumSize) {
		this.minimumHeight = minimumSize == null ? -1 : minimumSize.height;
		this.minimumWidth = minimumSize == null ? -1 : minimumSize.width;
		return self();
	}

	@Override
	public final B border(Border border) {
		this.border = border;
		return self();
	}

	@Override
	public final B transferFocusOnEnter(boolean transferFocusOnEnter) {
		this.transferFocusOnEnter = transferFocusOnEnter;
		return self();
	}

	@Override
	public final B enabled(boolean enabled) {
		this.enabled = enabled;
		return self();
	}

	@Override
	public final B enabled(StateObserver enabled) {
		this.enabledObserver = enabled;
		return self();
	}

	@Override
	public final B popupMenuControl(Function<C, Control> popupMenuControl) {
		requireNonNull(popupMenuControl);

		return popupMenuControls(comp -> Controls.controls(popupMenuControl.apply(comp)));
	}

	@Override
	public final B popupMenuControls(Function<C, Controls> popupMenuControls) {
		requireNonNull(popupMenuControls);

		return popupMenu(comp -> MenuBuilder.builder(popupMenuControls.apply(comp)).buildPopupMenu());
	}

	@Override
	public final B popupMenu(Function<C, JPopupMenu> popupMenu) {
		this.popupMenu = popupMenu;
		return self();
	}

	@Override
	public final B toolTipText(String toolTipText) {
		this.toolTipText = toolTipText;
		return self();
	}

	@Override
	public final B font(Font font) {
		this.font = font;
		return self();
	}

	@Override
	public final B foreground(Color foreground) {
		this.foreground = foreground;
		return self();
	}

	@Override
	public final B background(Color background) {
		this.background = background;
		return self();
	}

	@Override
	public final B opaque(boolean opaque) {
		this.opaque = opaque;
		return self();
	}

	@Override
	public final B visible(boolean visible) {
		this.visible = visible;
		return self();
	}

	@Override
	public final B componentOrientation(ComponentOrientation componentOrientation) {
		this.componentOrientation = requireNonNull(componentOrientation);
		return self();
	}

	@Override
	public final B validator(Value.Validator<T> validator) {
		this.validators.add(requireNonNull(validator));
		return self();
	}

	@Override
	public final B keyEvent(KeyEvents.Builder keyEventBuilder) {
		this.keyEventBuilders.add(requireNonNull(keyEventBuilder));
		return self();
	}

	@Override
	public final B clientProperty(Object key, Object value) {
		this.clientProperties.put(requireNonNull(key), value);
		return self();
	}

	@Override
	public final B focusListener(FocusListener focusListener) {
		this.focusListeners.add(requireNonNull(focusListener));
		return self();
	}

	@Override
	public final B mouseListener(MouseListener mouseListener) {
		this.mouseListeners.add(requireNonNull(mouseListener));
		return self();
	}

	@Override
	public final B mouseMotionListener(MouseMotionListener mouseMotionListener) {
		this.mouseMotionListeners.add(requireNonNull(mouseMotionListener));
		return self();
	}

	@Override
	public final B mouseWheelListener(MouseWheelListener mouseWheelListener) {
		this.mouseWheelListeners.add(requireNonNull(mouseWheelListener));
		return self();
	}

	@Override
	public final B keyListener(KeyListener keyListener) {
		this.keyListeners.add(requireNonNull(keyListener));
		return self();
	}

	@Override
	public final B componentListener(ComponentListener componentListener) {
		this.componentListeners.add(requireNonNull(componentListener));
		return self();
	}

	@Override
	public final B propertyChangeListener(PropertyChangeListener propertyChangeListener) {
		this.propertyChangeListeners.add(requireNonNull(propertyChangeListener));
		return self();
	}

	@Override
	public final B propertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
		this.propertyChangeListenerMap.put(requireNonNull(propertyName), requireNonNull(propertyChangeListener));
		return self();
	}

	@Override
	public final B transferHandler(TransferHandler transferHandler) {
		this.transferHandler = requireNonNull(transferHandler);
		return self();
	}

	@Override
	public final B focusCycleRoot(boolean focusCycleRoot) {
		this.focusCycleRoot = focusCycleRoot;
		return self();
	}

	@Override
	public final B onSetVisible(Consumer<C> onSetVisible) {
		this.onSetVisible = requireNonNull(onSetVisible);
		return self();
	}

	@Override
	public final B link(Value<T> linkedValue) {
		if (requireNonNull(linkedValue).nullable() && !supportsNull()) {
			throw new IllegalArgumentException("Component does not support a nullable value");
		}
		this.linkedValues.add(linkedValue);
		return self();
	}

	@Override
	public final B link(ValueObserver<T> linkedValueObserver) {
		if (requireNonNull(linkedValueObserver).nullable() && !supportsNull()) {
			throw new IllegalArgumentException("Component does not support a nullable value");
		}
		this.linkedValueObservers.add(linkedValueObserver);
		return self();
	}

	@Override
	public final B listener(Runnable listener) {
		this.listeners.add(requireNonNull(listener));
		return self();
	}

	@Override
	public final B consumer(Consumer<T> consumer) {
		this.consumers.add(requireNonNull(consumer));
		return self();
	}

	@Override
	public final B value(T value) {
		this.valueSet = true;
		this.value = value;
		return self();
	}

	@Override
	public final ScrollPaneBuilder scrollPane() {
		return ScrollPaneBuilder.builder(build());
	}

	@Override
	public final B onBuild(Consumer<C> onBuild) {
		buildConsumers.add(requireNonNull(onBuild));
		return self();
	}

	@Override
	public final B onBuildValue(Consumer<ComponentValue<T, C>> onBuildValue) {
		buildValueConsumers.add(requireNonNull(onBuildValue));
		return self();
	}

	@Override
	public final C build() {
		return build(null);
	}

	@Override
	public final C build(Consumer<C> onBuild) {
		ComponentValue<T, C> componentValue = createComponentValue(createComponent());
		C component = configureComponent(componentValue);
		if (onBuild != null) {
			onBuild.accept(component);
		}

		return component;
	}

	@Override
	public final ComponentValue<T, C> buildValue() {
		return buildValue(null);
	}

	@Override
	public final ComponentValue<T, C> buildValue(Consumer<ComponentValue<T, C>> onBuild) {
		ComponentValue<T, C> componentValue = createComponentValue(createComponent());
		configureComponent(componentValue);
		if (onBuild != null) {
			onBuild.accept(componentValue);
		}

		return componentValue;
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
	 * @return true if this component can be linked with a nullable value
	 */
	protected boolean supportsNull() {
		return true;
	}

	/**
	 * Enables focus transfer on Enter, override for special handling
	 * @param component the component
	 */
	protected void enableTransferFocusOnEnter(C component) {
		TransferFocusOnEnter.enable(component);
	}

	protected final B self() {
		return (B) this;
	}

	private C configureComponent(ComponentValue<T, C> componentValue) {
		C component = componentValue.component();
		component.putClientProperty(COMPONENT_VALUE, componentValue);
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
		if (enabledObserver != null) {
			linkToEnabledState(enabledObserver, component);
		}
		if (popupMenu != null) {
			component.setComponentPopupMenu(popupMenu.apply(component));
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
		if (opaque) {
			component.setOpaque(true);
		}
		component.setVisible(visible);
		component.setComponentOrientation(componentOrientation);
		clientProperties.forEach(component::putClientProperty);
		if (onSetVisible != null) {
			new OnSetVisible<>(component, onSetVisible);
		}
		if (transferFocusOnEnter) {
			enableTransferFocusOnEnter(component);
		}
		if (transferHandler != null) {
			component.setTransferHandler(transferHandler);
		}
		if (focusCycleRoot) {
			component.setFocusCycleRoot(true);
		}
		validators.forEach(componentValue::addValidator);
		if (valueSet && linkedValues.isEmpty() && linkedValueObservers.isEmpty()) {
			componentValue.set(value);
		}
		linkedValues.forEach(componentValue::link);
		linkedValueObservers.forEach(componentValue::link);
		listeners.forEach(componentValue::addListener);
		consumers.forEach(componentValue::addConsumer);
		if (label != null) {
			label.setLabelFor(component);
		}
		keyEventBuilders.forEach(keyEventBuilder -> keyEventBuilder.enable(component));
		focusListeners.forEach(component::addFocusListener);
		mouseListeners.forEach(component::addMouseListener);
		mouseMotionListeners.forEach(component::addMouseMotionListener);
		mouseWheelListeners.forEach(component::addMouseWheelListener);
		keyListeners.forEach(component::addKeyListener);
		componentListeners.forEach(component::addComponentListener);
		propertyChangeListeners.forEach(component::addPropertyChangeListener);
		propertyChangeListenerMap.forEach(component::addPropertyChangeListener);
		buildConsumers.forEach(consumer -> consumer.accept(component));
		buildValueConsumers.forEach(consumer -> consumer.accept(componentValue));

		return component;
	}

	private void setSizes(C component) {
		if (minimumHeight != -1) {
			setMinimumHeight(component, minimumHeight);
		}
		if (minimumWidth != -1) {
			setMinimumWidth(component, minimumWidth);
		}
		if (maximumHeight != -1) {
			setMaximumHeight(component, maximumHeight);
		}
		if (maximumWidth != -1) {
			setMaximumWidth(component, maximumWidth);
		}
		if (preferredHeight != -1) {
			setPreferredHeight(component, preferredHeight);
		}
		if (preferredWidth != -1) {
			setPreferredWidth(component, preferredWidth);
		}
	}

	private static int validatePositiveInteger(int value) {
		if (value < 0) {
			throw new IllegalArgumentException("Value must be positive");
		}

		return value;
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
