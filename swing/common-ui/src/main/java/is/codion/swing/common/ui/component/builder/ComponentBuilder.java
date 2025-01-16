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

import is.codion.common.observable.Observable;
import is.codion.common.state.ObservableState;
import is.codion.common.value.Value;
import is.codion.common.value.Value.Validator;
import is.codion.swing.common.ui.component.scrollpane.ScrollPaneBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Use {@link #build} to build a JComponent instance or {@link #buildValue()} to build a {@link ComponentValue} instance.<br>
 * The component is available via {@link ComponentValue#component()} and the associated {@link ComponentValue} instance
 * is available via the {@link #COMPONENT_VALUE} client property.
 * @param <T> the type of the value the component represents
 * @param <C> the component type
 * @param <B> the builder type
 */
public interface ComponentBuilder<T, C extends JComponent, B extends ComponentBuilder<T, C, B>> {

	/**
	 * The client property key for the associated {@link ComponentValue}
	 * <pre>
	 * {@code
	 *   JTextField textField =
	 *            Components.stringField()
	 *                    .build();
	 *
	 *   ComponentValue<String, JTextField> componentValue =
	 *            (ComponentValue<String, JTextField>)
	 *                    textField.getClientProperty(COMPONENT_VALUE);
	 * }
	 * </pre>
	 * @see JComponent#getClientProperty(Object)
	 */
	String COMPONENT_VALUE = "componentValue";

	/**
	 * @param name the name to assign to the component
	 * @return this builder instance
	 * @see Component#setName(String)
	 */
	B name(String name);

	/**
	 * @param label the label for the component
	 * @return this builder instance
	 * @see JLabel#setLabelFor(Component)
	 */
	B label(JLabel label);

	/**
	 * @param focusable false if the component should not be focusable
	 * @return this builder instance
	 * @see JComponent#setFocusable(boolean)
	 */
	B focusable(boolean focusable);

	/**
	 * @param preferredHeight the preferred component height
	 * @return this builder instance
	 */
	B preferredHeight(int preferredHeight);

	/**
	 * @param preferredWidth the preferred component width
	 * @return this builder instance
	 */
	B preferredWidth(int preferredWidth);

	/**
	 * @param preferredSize the preferred component size
	 * @return this builder instance
	 * @see JComponent#setPreferredSize(Dimension)
	 */
	B preferredSize(Dimension preferredSize);

	/**
	 * @param maximumHeight the maximum component height
	 * @return this builder instance
	 */
	B maximumHeight(int maximumHeight);

	/**
	 * @param maximumWidth the maximum component width
	 * @return this builder instance
	 */
	B maximumWidth(int maximumWidth);

	/**
	 * @param maximumSize the maximum component size
	 * @return this builder instance
	 * @see JComponent#setMaximumSize(Dimension)
	 */
	B maximumSize(Dimension maximumSize);

	/**
	 * @param minimumHeight the minimum component height
	 * @return this builder instance
	 */
	B minimumHeight(int minimumHeight);

	/**
	 * @param minimumWidth the minimum component width
	 * @return this builder instance
	 */
	B minimumWidth(int minimumWidth);

	/**
	 * @param minimumSize the minimum component size
	 * @return this builder instance
	 * @see JComponent#setMinimumSize(Dimension)
	 */
	B minimumSize(Dimension minimumSize);

	/**
	 * @param border the component border
	 * @return this builder instance
	 * @see JComponent#setBorder(Border)
	 */
	B border(Border border);

	/**
	 * Note that for JTextArea CTRL is added to move focus forward and CTRL + SHIFT to move it backwards
	 * @param transferFocusOnEnter if true then the text field transfer focus on enter (shift-enter for backwards)
	 * @return this builder instance
	 */
	B transferFocusOnEnter(boolean transferFocusOnEnter);

	/**
	 * @param toolTipText the tool tip text
	 * @return this builder instance
	 * @see JComponent#setToolTipText(String)
	 */
	B toolTipText(String toolTipText);

	/**
	 * Sets the enabled state of the component, for a dynamic enabled state use {@link #enabled(ObservableState)}.
	 * Overridden by {@link #enabled(ObservableState)}.
	 * @param enabled the enabled state
	 * @return this builder instance
	 * @see JComponent#setEnabled(boolean)
	 */
	B enabled(boolean enabled);

	/**
	 * @param enabled the state observer controlling the component enabled status
	 * @return this builder instance
	 */
	B enabled(ObservableState enabled);

	/**
	 * @param popupMenuControl a function, receiving the component being built, providing the control to base a popup menu on
	 * @return this builder instance
	 */
	B popupMenuControl(Function<C, Control> popupMenuControl);

	/**
	 * @param popupMenuControls a function, receiving the component being built, providing the controls to base a popup menu on
	 * @return this builder instance
	 */
	B popupMenuControls(Function<C, Controls> popupMenuControls);

	/**
	 * @param popupMenu a function, receiving the component being built, providing the popup menu
	 * @return this builder instance
	 * @see JComponent#setComponentPopupMenu(JPopupMenu)
	 */
	B popupMenu(Function<C, JPopupMenu> popupMenu);

	/**
	 * @param font the component font
	 * @return this builder instance
	 * @see JComponent#setFont(Font)
	 */
	B font(Font font);

	/**
	 * @param foreground the foreground color
	 * @return this builder instance
	 * @see JComponent#setForeground(Color)
	 */
	B foreground(Color foreground);

	/**
	 * @param background the background color
	 * @return this builder instance
	 * @see JComponent#setBackground(Color)
	 */
	B background(Color background);

	/**
	 * @param opaque true if the component should be opaque
	 * @return this builder instance
	 * @see JComponent#setOpaque(boolean)
	 */
	B opaque(boolean opaque);

	/**
	 * @param visible the initial component visibility, default true
	 * @return this builder instance
	 * @see JComponent#setVisible(boolean)
	 */
	B visible(boolean visible);

	/**
	 * @param orientation the component orientation
	 * @return this builder instance
	 * @see JComponent#setComponentOrientation(ComponentOrientation)
	 */
	B componentOrientation(ComponentOrientation orientation);

	/**
	 * @param validator the validator to use
	 * @return this builder instance
	 */
	B validator(Validator<T> validator);

	/**
	 * Enables the key event defined by the given {@link KeyEvents.Builder} on the component.
	 * Note that setting {@link #transferFocusOnEnter(boolean)} to true overrides
	 * any conflicting key event based on {@link java.awt.event.KeyEvent#VK_ENTER} added via this method.
	 * @param keyEventBuilder a key event builder to enable on the component
	 * @return this builder instance
	 */
	B keyEvent(KeyEvents.Builder keyEventBuilder);

	/**
	 * Adds an arbitrary key/value "client property" to the component
	 * @param key the key
	 * @param value the value
	 * @return this builder instance
	 * @see JComponent#putClientProperty(Object, Object)
	 */
	B clientProperty(Object key, Object value);

	/**
	 * @param focusListener the focus listener
	 * @return this builder instance
	 * @see JComponent#addFocusListener(FocusListener)
	 */
	B focusListener(FocusListener focusListener);

	/**
	 * @param mouseListener the mouse listener
	 * @return this builder instance
	 * @see JComponent#addMouseListener(MouseListener)
	 */
	B mouseListener(MouseListener mouseListener);

	/**
	 * @param mouseMotionListener the mouse motion listener
	 * @return this builder instance
	 * @see JComponent#addMouseMotionListener(MouseMotionListener)
	 */
	B mouseMotionListener(MouseMotionListener mouseMotionListener);

	/**
	 * @param mouseWheelListener the mouse wheel listener
	 * @return this builder instance
	 * @see JComponent#addMouseWheelListener(MouseWheelListener)
	 */
	B mouseWheelListener(MouseWheelListener mouseWheelListener);

	/**
	 * @param keyListener the key listener
	 * @return this builder instance
	 * @see JComponent#addKeyListener(KeyListener)
	 */
	B keyListener(KeyListener keyListener);

	/**
	 * @param componentListener the component listener
	 * @return this builder instance
	 * @see JComponent#addComponentListener(ComponentListener)
	 */
	B componentListener(ComponentListener componentListener);

	/**
	 * @param propertyChangeListener the property change listener
	 * @return this builder instance
	 * @see JComponent#addPropertyChangeListener(PropertyChangeListener)
	 */
	B propertyChangeListener(PropertyChangeListener propertyChangeListener);

	/**
	 * @param propertyName the name of the property to listen for
	 * @param propertyChangeListener the property change listener
	 * @return this builder instance
	 * @see JComponent#addPropertyChangeListener(String, PropertyChangeListener)
	 */
	B propertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener);

	/**
	 * @param transferHandler the transfer handler
	 * @return this builder instance
	 * @see JComponent#setTransferHandler(TransferHandler)
	 */
	B transferHandler(TransferHandler transferHandler);

	/**
	 * @param focusCycleRoot true if the component should be the root of a focus traversal cycle
	 * @return this builder instance
	 * @see java.awt.Container#setFocusCycleRoot(boolean)
	 */
	B focusCycleRoot(boolean focusCycleRoot);

	/**
	 * @param onSetVisible called when the component is made visible for the first time
	 * @return this builder instance
	 */
	B onSetVisible(Consumer<C> onSetVisible);

	/**
	 * Creates a bidirectional link to the given value. Overrides any initial value set.
	 * @param linkedValue a value to link to the component value
	 * @return this builder instance
	 */
	B link(Value<T> linkedValue);

	/**
	 * Creates a read-only link to the given {@link Observable}.
	 * @param linkedValue a value to link to the component value
	 * @return this builder instance
	 */
	B link(Observable<T> linkedValue);

	/**
	 * @param listener a listener to add to the resulting component value
	 * @return this builder instance
	 */
	B listener(Runnable listener);

	/**
	 * @param consumer a consumer to add to the resulting component value
	 * @return this builder instance
	 */
	B consumer(Consumer<T> consumer);

	/**
	 * Sets the initial value for the component, unless value(s) have been linked via {@link #link(Value)}
	 * or {@link #link(Observable)}, which then control the inital value.
	 * The initial value is set before any listeners are added, so no events are triggered.
	 * @param value the initial value
	 * @return this builder instance
	 */
	B value(T value);

	/**
	 * @return a {@link ScrollPaneBuilder} using this component as the view
	 */
	ScrollPaneBuilder scrollPane();

	/**
	 * @param onBuild called when the component has been built.
	 * @return this builder instance
	 */
	B onBuild(Consumer<C> onBuild);

	/**
	 * @param onBuildValue called when the component value has been built.
	 * @return this builder instance
	 */
	B onBuildValue(Consumer<ComponentValue<T, C>> onBuildValue);

	/**
	 * Builds a new component instance.
	 * @return the component
	 */
	C build();

	/**
	 * Builds a new component instance.
	 * @param onBuild called after the component is built.
	 * @return the component
	 */
	C build(Consumer<C> onBuild);

	/**
	 * Builds and returns the component value.
	 * @return the component value
	 */
	ComponentValue<T, C> buildValue();

	/**
	 * Builds and returns the component value.
	 * @param onBuild called after the component value is built.
	 * @return the component value
	 */
	ComponentValue<T, C> buildValue(Consumer<ComponentValue<T, C>> onBuild);
}
