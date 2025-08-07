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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.builder;

import is.codion.common.observer.Observable;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Sizes;
import is.codion.swing.common.ui.component.button.MenuBuilder;
import is.codion.swing.common.ui.component.indicator.ModifiedIndicatorFactory;
import is.codion.swing.common.ui.component.indicator.UnderlineModifiedIndicatorFactory;
import is.codion.swing.common.ui.component.indicator.ValidIndicatorFactory;
import is.codion.swing.common.ui.component.scrollpane.ScrollPaneBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;

import org.jspecify.annotations.Nullable;

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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static is.codion.swing.common.ui.key.TransferFocusOnEnter.FORWARD_BACKWARD;
import static java.util.Objects.requireNonNull;

public abstract class AbstractComponentBuilder<T, C extends JComponent, B extends ComponentBuilder<T, C, B>>
				implements ComponentBuilder<T, C, B> {

	private final List<Consumer<C>> buildConsumers = new ArrayList<>(1);
	private final List<Consumer<ComponentValue<T, C>>> buildValueConsumers = new ArrayList<>(1);
	private final List<Value<T>> linkedValues = new ArrayList<>(1);
	private final List<Observable<T>> linkedObservables = new ArrayList<>(1);
	private final List<KeyEvents.Builder> keyEventBuilders = new ArrayList<>(1);
	private final Map<Object, @Nullable Object> clientProperties = new HashMap<>();
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

	private @Nullable String name;
	private @Nullable JLabel label;
	private boolean focusable = true;
	private int preferredHeight = -1;
	private int preferredWidth = -1;
	private int minimumHeight = -1;
	private int minimumWidth = -1;
	private int maximumHeight = -1;
	private int maximumWidth = -1;
	private boolean opaque = false;
	private boolean visible = true;
	private @Nullable Border border;
	private @Nullable TransferFocusOnEnter transferFocusOnEnter;
	private @Nullable String toolTipText;
	private @Nullable Observable<String> toolTipTextObservable;
	private @Nullable Font font;
	private @Nullable Color foreground;
	private @Nullable Color background;
	private @Nullable ComponentOrientation componentOrientation;
	private @Nullable ValidIndicatorFactory validIndicatorFactory =
					ValidIndicatorFactory.instance().orElse(null);
	private @Nullable ObservableState enabledObservable;
	private @Nullable ObservableState focusableObservable;
	private @Nullable ObservableState visibleObservable;
	private @Nullable ObservableState validObservable;
	private @Nullable ModifiedIndicatorFactory modifiedIndicatorFactory = new UnderlineModifiedIndicatorFactory();
	private @Nullable ObservableState modifiedObservable;
	private @Nullable Predicate<T> validator;
	private boolean enabled = true;
	private @Nullable Function<C, JPopupMenu> popupMenu;
	private @Nullable T value;
	private boolean valueSet = false;
	private @Nullable Consumer<C> onSetVisible;
	private @Nullable TransferHandler transferHandler;
	private boolean focusCycleRoot = false;

	protected AbstractComponentBuilder() {}

	@Override
	public final B name(@Nullable String name) {
		this.name = name;
		return self();
	}

	@Override
	public final B label(@Nullable JLabel label) {
		this.label = label;
		return self();
	}

	@Override
	public final B focusable(boolean focusable) {
		this.focusable = focusable;
		return self();
	}

	@Override
	public final B focusable(@Nullable ObservableState focusable) {
		this.focusableObservable = focusable;
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
	public final B preferredSize(@Nullable Dimension preferredSize) {
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
	public final B maximumSize(@Nullable Dimension maximumSize) {
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
	public final B minimumSize(@Nullable Dimension minimumSize) {
		this.minimumHeight = minimumSize == null ? -1 : minimumSize.height;
		this.minimumWidth = minimumSize == null ? -1 : minimumSize.width;
		return self();
	}

	@Override
	public final B border(@Nullable Border border) {
		this.border = border;
		return self();
	}

	@Override
	public final B transferFocusOnEnter(boolean transferFocusOnEnter) {
		if (transferFocusOnEnter) {
			return transferFocusOnEnter(FORWARD_BACKWARD);
		}
		this.transferFocusOnEnter = null;
		return self();
	}

	@Override
	public final B transferFocusOnEnter(TransferFocusOnEnter transferFocusOnEnter) {
		this.transferFocusOnEnter = requireNonNull(transferFocusOnEnter);
		return self();
	}

	@Override
	public final B enabled(boolean enabled) {
		this.enabled = enabled;
		return self();
	}

	@Override
	public final B enabled(@Nullable ObservableState enabled) {
		this.enabledObservable = enabled;
		return self();
	}

	@Override
	public final B validIndicatorFactory(@Nullable ValidIndicatorFactory validIndicatorFactory) {
		this.validIndicatorFactory = validIndicatorFactory;
		return self();
	}

	@Override
	public final B validIndicator(@Nullable ObservableState valid) {
		this.validObservable = valid;
		return self();
	}

	@Override
	public final B validIndicator(@Nullable Predicate<T> validator) {
		this.validator = validator;
		return self();
	}

	@Override
	public final B modifiedIndicatorFactory(@Nullable ModifiedIndicatorFactory modifiedIndicatorFactory) {
		this.modifiedIndicatorFactory = modifiedIndicatorFactory;
		return self();
	}

	@Override
	public final B modifiedIndicator(@Nullable ObservableState modified) {
		this.modifiedObservable = modified;
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

		return popupMenu(comp -> MenuBuilder.builder()
						.controls(popupMenuControls.apply(comp))
						.buildPopupMenu());
	}

	@Override
	public final B popupMenu(@Nullable Function<C, JPopupMenu> popupMenu) {
		this.popupMenu = popupMenu;
		return self();
	}

	@Override
	public final B toolTipText(@Nullable String toolTipText) {
		this.toolTipText = toolTipText;
		return self();
	}

	@Override
	public final B toolTipText(@Nullable Observable<String> toolTipText) {
		this.toolTipTextObservable = toolTipText;
		return self();
	}

	@Override
	public final B font(@Nullable Font font) {
		this.font = font;
		return self();
	}

	@Override
	public final B foreground(@Nullable Color foreground) {
		this.foreground = foreground;
		return self();
	}

	@Override
	public final B background(@Nullable Color background) {
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
	public final B visible(@Nullable ObservableState visible) {
		this.visibleObservable = visible;
		return self();
	}

	@Override
	public final B componentOrientation(@Nullable ComponentOrientation componentOrientation) {
		this.componentOrientation = componentOrientation;
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
	public final B clientProperty(Object key, @Nullable Object value) {
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
	public final B transferHandler(@Nullable TransferHandler transferHandler) {
		this.transferHandler = transferHandler;
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
		if (requireNonNull(linkedValue).isNullable() && !supportsNull()) {
			throw new IllegalArgumentException("Component does not support a nullable value");
		}
		this.linkedValues.add(linkedValue);
		return self();
	}

	@Override
	public final B link(Observable<T> linkedObservable) {
		if (requireNonNull(linkedObservable).isNullable() && !supportsNull()) {
			throw new IllegalArgumentException("Component does not support a nullable value");
		}
		this.linkedObservables.add(linkedObservable);
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
	public final B value(@Nullable T value) {
		this.valueSet = true;
		this.value = value;
		return self();
	}

	@Override
	public final ScrollPaneBuilder scrollPane() {
		return ScrollPaneBuilder.builder().view(build());
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
	public final C build(@Nullable Consumer<C> onBuild) {
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
	public final ComponentValue<T, C> buildValue(@Nullable Consumer<ComponentValue<T, C>> onBuild) {
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
	 * @param transferFocusOnEnter the transfer focus on enter to enable
	 */
	protected void enableTransferFocusOnEnter(C component, TransferFocusOnEnter transferFocusOnEnter) {
		transferFocusOnEnter.enable(component);
	}

	/**
	 * Enables a valid indicator on the given component, based on the given valid state instance
	 * using the given {@link ValidIndicatorFactory}, override for special handling.
	 * @param validIndicatorFactory the {@link ValidIndicatorFactory} to use
	 * @param component the component
	 * @param valid the valid state to indicate
	 */
	protected void enableValidIndicator(ValidIndicatorFactory validIndicatorFactory, C component, ObservableState valid) {
		validIndicatorFactory.enable(component, valid);
	}

	protected final B self() {
		return (B) this;
	}

	private C configureComponent(ComponentValue<T, C> componentValue) {
		C component = componentValue.component();
		component.putClientProperty(COMPONENT_VALUE, componentValue);
		if (label != null) {
			label.setLabelFor(component);
		}
		if (component.isFocusable() && !focusable) {
			component.setFocusable(false);
		}
		if (focusableObservable != null) {
			Utilities.focusable(focusableObservable, component);
		}
		if (name != null) {
			component.setName(name);
		}
		setSizes(component);
		if (border != null) {
			component.setBorder(border);
		}
		if (!enabled) {
			component.setEnabled(false);
		}
		if (enabledObservable != null) {
			Utilities.enabled(enabledObservable, component);
		}
		if (popupMenu != null) {
			component.setComponentPopupMenu(popupMenu.apply(component));
		}
		if (toolTipTextObservable != null) {
			component.setToolTipText(toolTipTextObservable.get());
			toolTipTextObservable.addConsumer(new SetToolTipText(component));
		}
		else if (toolTipText != null) {
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
		if (!visible) {
			component.setVisible(visible);
		}
		if (visibleObservable != null) {
			Utilities.visible(visibleObservable, component);
		}
		if (componentOrientation != null) {
			component.setComponentOrientation(componentOrientation);
		}
		clientProperties.forEach(component::putClientProperty);
		if (onSetVisible != null) {
			new OnSetVisible<>(component, onSetVisible);
		}
		if (transferFocusOnEnter != null) {
			enableTransferFocusOnEnter(component, transferFocusOnEnter);
		}
		if (transferHandler != null) {
			component.setTransferHandler(transferHandler);
		}
		if (focusCycleRoot) {
			component.setFocusCycleRoot(true);
		}
		validators.forEach(componentValue::addValidator);
		if (valueSet && linkedValues.isEmpty() && linkedObservables.isEmpty()) {
			componentValue.set(value);
		}
		linkedValues.forEach(componentValue::link);
		linkedObservables.forEach(componentValue::link);
		listeners.forEach(componentValue::addListener);
		consumers.forEach(componentValue::addConsumer);
		configureValidIndicator(componentValue);
		configureModifiedIndicator(component);
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

	private void configureValidIndicator(ComponentValue<T, C> componentValue) {
		if (validIndicatorFactory == null) {
			return;
		}
		if (validObservable != null) {
			enableValidIndicator(validIndicatorFactory, componentValue.component(), validObservable);
		}
		else if (validator != null) {
			enableValidIndicator(validIndicatorFactory, componentValue.component(), createValidState(componentValue, validator));
		}
	}

	private void configureModifiedIndicator(C component) {
		if (modifiedIndicatorFactory != null && modifiedObservable != null) {
			modifiedIndicatorFactory.enable(component, modifiedObservable);
		}
	}

	private void setSizes(C component) {
		if (minimumHeight != -1) {
			Sizes.minimumHeight(component, minimumHeight);
		}
		if (minimumWidth != -1) {
			Sizes.minimumWidth(component, minimumWidth);
		}
		if (maximumHeight != -1) {
			Sizes.maximumHeight(component, maximumHeight);
		}
		if (maximumWidth != -1) {
			Sizes.maximumWidth(component, maximumWidth);
		}
		if (preferredHeight != -1) {
			Sizes.preferredHeight(component, preferredHeight);
		}
		if (preferredWidth != -1) {
			Sizes.preferredWidth(component, preferredWidth);
		}
	}

	private static int validatePositiveInteger(int value) {
		if (value < 0) {
			throw new IllegalArgumentException("Value must be positive");
		}

		return value;
	}

	private static <T, C extends JComponent> ObservableState createValidState(ComponentValue<T, C> componentValue,
																																						Predicate<T> validator) {
		ValidationConsumer<T> validationConsumer = new ValidationConsumer<>(componentValue.get(), validator);
		componentValue.addConsumer(validationConsumer);

		return validationConsumer.valid.observable();
	}

	private static final class ValidationConsumer<T> implements Consumer<T> {

		private final Predicate<@Nullable T> validator;
		private final State valid;

		private ValidationConsumer(@Nullable T initialValue, Predicate<T> validator) {
			this.validator = validator;
			this.valid = State.state();
			accept(initialValue);
		}

		@Override
		public void accept(@Nullable T value) {
			valid.set(validator.test(value));
		}
	}

	private static final class SetToolTipText implements Consumer<String> {

		private final JComponent component;

		private SetToolTipText(JComponent component) {
			this.component = component;
		}

		@Override
		public void accept(String toolTipText) {
			component.setToolTipText(toolTipText);
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
