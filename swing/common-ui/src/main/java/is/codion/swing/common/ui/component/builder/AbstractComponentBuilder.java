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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.builder;

import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.state.ObservableState;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Sizes;
import is.codion.swing.common.ui.component.button.MenuBuilder;
import is.codion.swing.common.ui.component.indicator.ValidIndicatorFactory;
import is.codion.swing.common.ui.component.label.LabelBuilder;
import is.codion.swing.common.ui.component.scrollpane.ScrollPaneBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ControlsBuilder;
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
import java.awt.event.HierarchyListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static is.codion.swing.common.ui.key.TransferFocusOnEnter.FORWARD_BACKWARD;
import static java.util.Objects.requireNonNull;

public abstract class AbstractComponentBuilder<C extends JComponent, B extends ComponentBuilder<C, B>>
				implements ComponentBuilder<C, B> {

	private final List<Consumer<C>> buildConsumers = new ArrayList<>(1);
	private final List<KeyEvents.Builder> keyEventBuilders = new ArrayList<>(1);
	private final List<BiConsumer<C, ControlsBuilder>> popupControls = new ArrayList<>();
	private final Map<Object, @Nullable Object> clientProperties = new HashMap<>();
	private final List<FocusListener> focusListeners = new ArrayList<>();
	private final List<MouseListener> mouseListeners = new ArrayList<>();
	private final List<MouseMotionListener> mouseMotionListeners = new ArrayList<>();
	private final List<MouseWheelListener> mouseWheelListeners = new ArrayList<>();
	private final List<KeyListener> keyListeners = new ArrayList<>();
	private final List<ComponentListener> componentListeners = new ArrayList<>();
	private final List<AncestorListener> ancestorListeners = new ArrayList<>();
	private final List<HierarchyListener> hierarchyListeners = new ArrayList<>();
	private final List<PropertyChangeListener> propertyChangeListeners = new ArrayList<>();
	private final Map<String, PropertyChangeListener> propertyChangeListenerMap = new HashMap<>();

	private @Nullable String name;
	private @Nullable JLabel label;
	private @Nullable Boolean focusable;
	private @Nullable Integer preferredHeight;
	private @Nullable Integer preferredWidth;
	private @Nullable Integer minimumHeight;
	private @Nullable Integer minimumWidth;
	private @Nullable Integer maximumHeight;
	private @Nullable Integer maximumWidth;
	private @Nullable Boolean opaque;
	private @Nullable Boolean visible;
	private @Nullable Border border;
	private @Nullable TransferFocusOnEnter transferFocusOnEnter;
	private @Nullable String toolTipText;
	private @Nullable Observable<String> toolTipTextObservable;
	private @Nullable Font font;
	private @Nullable UnaryOperator<Font> fontOperator;
	private @Nullable Color foreground;
	private @Nullable Color background;
	private @Nullable ComponentOrientation componentOrientation;
	private @Nullable ObservableState enabledObservable;
	private @Nullable ObservableState focusableObservable;
	private @Nullable ObservableState visibleObservable;
	private @Nullable Boolean enabled;
	private @Nullable Function<C, JPopupMenu> popupMenu;
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
	public final B label(Consumer<LabelBuilder<String>> label) {
		LabelBuilder<String> labelBuilder = LabelBuilder.builder();
		requireNonNull(label).accept(labelBuilder);

		return label(labelBuilder.build());
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
		this.preferredHeight = preferredSize == null ? null : preferredSize.height;
		this.preferredWidth = preferredSize == null ? null : preferredSize.width;
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
		this.maximumHeight = maximumSize == null ? null : maximumSize.height;
		this.maximumWidth = maximumSize == null ? null : maximumSize.width;
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
	public final B popupControls(BiConsumer<C, ControlsBuilder> popupControls) {
		this.popupControls.add(requireNonNull(popupControls));
		return self();
	}

	@Override
	public final B popupControl(Function<C, Control> popupMenuControl) {
		return popupControls(new ProcessPopupMenuControl(requireNonNull(popupMenuControl)));
	}

	@Override
	public final B popupControls(Function<C, Controls> popupMenuControls) {
		return popupControls(new ProcessPopupMenuControls(requireNonNull(popupMenuControls)));
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
		this.fontOperator = null;
		return self();
	}

	@Override
	public final B font(UnaryOperator<Font> font) {
		this.fontOperator = requireNonNull(font);
		this.font = null;
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
	public final B ancestorListener(AncestorListener ancestorListener) {
		this.ancestorListeners.add(requireNonNull(ancestorListener));
		return self();
	}

	@Override
	public final B hierarchyListener(HierarchyListener hierarchyListener) {
		this.hierarchyListeners.add(requireNonNull(hierarchyListener));
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
	public final ScrollPaneBuilder scrollPane() {
		return ScrollPaneBuilder.builder().view(build());
	}

	@Override
	public final B onBuild(Consumer<C> onBuild) {
		buildConsumers.add(requireNonNull(onBuild));
		return self();
	}

	@Override
	public final C build() {
		return build(null);
	}

	@Override
	public final C build(@Nullable Consumer<C> onBuild) {
		C component = configureComponent(createComponent());
		if (onBuild != null) {
			onBuild.accept(component);
		}
		buildConsumers.forEach(consumer -> consumer.accept(component));

		return component;
	}

	/**
	 * Creates the component.
	 * @return a new component instance
	 */
	protected abstract C createComponent();

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

	/**
	 * @return the label set via {@link #label(JLabel)} or {@link #label(Consumer)}, or null if none has been set
	 */
	protected final @Nullable JLabel label() {
		return label;
	}

	protected C configureComponent(C component) {
		if (label != null) {
			label.setLabelFor(component);
		}
		if (focusable != null) {
			component.setFocusable(focusable);
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
		if (enabled != null) {
			component.setEnabled(enabled);
		}
		if (enabledObservable != null) {
			Utilities.enabled(enabledObservable, component);
		}
		if (popupMenu != null) {
			component.setComponentPopupMenu(popupMenu.apply(component));
		}
		else if (!popupControls.isEmpty()) {
			ControlsBuilder controlsBuilder = Controls.builder();
			popupControls.forEach(controls -> controls.accept(component, controlsBuilder));
			component.setComponentPopupMenu(MenuBuilder.builder()
							.controls(controlsBuilder.build())
							.buildPopupMenu());
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
		else if (fontOperator != null) {
			component.setFont(fontOperator.apply(component.getFont()));
		}
		if (foreground != null) {
			component.setForeground(foreground);
		}
		if (background != null) {
			component.setBackground(background);
		}
		if (opaque != null) {
			component.setOpaque(opaque);
		}
		if (visible != null) {
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
		keyEventBuilders.forEach(keyEventBuilder -> keyEventBuilder.enable(component));
		focusListeners.forEach(component::addFocusListener);
		mouseListeners.forEach(component::addMouseListener);
		mouseMotionListeners.forEach(component::addMouseMotionListener);
		mouseWheelListeners.forEach(component::addMouseWheelListener);
		keyListeners.forEach(component::addKeyListener);
		componentListeners.forEach(component::addComponentListener);
		ancestorListeners.forEach(component::addAncestorListener);
		hierarchyListeners.forEach(component::addHierarchyListener);
		propertyChangeListeners.forEach(component::addPropertyChangeListener);
		propertyChangeListenerMap.forEach(component::addPropertyChangeListener);

		return component;
	}

	private void setSizes(C component) {
		if (minimumHeight != null) {
			Sizes.minimumHeight(component, minimumHeight);
		}
		if (minimumWidth != null) {
			Sizes.minimumWidth(component, minimumWidth);
		}
		if (maximumHeight != null) {
			Sizes.maximumHeight(component, maximumHeight);
		}
		if (maximumWidth != null) {
			Sizes.maximumWidth(component, maximumWidth);
		}
		if (preferredHeight != null) {
			Sizes.preferredHeight(component, preferredHeight);
		}
		if (preferredWidth != null) {
			Sizes.preferredWidth(component, preferredWidth);
		}
	}

	private static int validatePositiveInteger(int value) {
		if (value < 0) {
			throw new IllegalArgumentException("Value must be positive");
		}

		return value;
	}

	private final class ProcessPopupMenuControl implements BiConsumer<C, ControlsBuilder> {

		private final Function<C, Control> controlFactory;

		private ProcessPopupMenuControl(Function<C, Control> controlFactory) {
			this.controlFactory = controlFactory;
		}

		@Override
		public void accept(C component, ControlsBuilder controls) {
			controls.control(controlFactory.apply(component));
		}
	}

	private final class ProcessPopupMenuControls implements BiConsumer<C, ControlsBuilder> {

		private final Function<C, Controls> controlsFactory;

		private ProcessPopupMenuControls(Function<C, Controls> controlsFactory) {
			this.controlsFactory = controlsFactory;
		}

		@Override
		public void accept(C component, ControlsBuilder controlsBuilder) {
			Controls controls = controlsFactory.apply(component);
			if (controls.caption().isPresent()) {
				controlsBuilder.controls(controls);
			}
			else {
				controlsBuilder.actions(controls.actions());
			}
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
