/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.builder;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.Value.Validator;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.component.scrollpane.ScrollPaneBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;

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
import java.util.function.Consumer;

/**
 * Builds a JComponent.<br>
 * Note that once {@link #build} or {@link #buildValue()} have been called they will return the same instance
 * on subsequent calls until the builder has been cleared by calling {@link #clear()}.
 * @param <T> the type of the value the component represents
 * @param <C> the component type
 * @param <B> the builder type
 */
public interface ComponentBuilder<T, C extends JComponent, B extends ComponentBuilder<T, C, B>> {

  /**
   * Specifies whether focus should be transferred from components on enter.<br>
   * Note that for JTextArea CTRL is added to move focus forward<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> TRANSFER_FOCUS_ON_ENTER =
          Configuration.booleanValue("is.codion.swing.common.ui.component.ComponentBuilder.transferFocusOnEnter", false);

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
   * Sets the enabled state of the component, for a dynamic enabled state use {@link #enabledState(StateObserver)}.
   * Overridden by {@link #enabledState(StateObserver)}.
   * @param enabled the enabled state
   * @return this builder instance
   * @see JComponent#setEnabled(boolean)
   */
  B enabled(boolean enabled);

  /**
   * @param enabledState the state controlling the component enabled status
   * @return this builder instance
   */
  B enabledState(StateObserver enabledState);

  /**
   * @param popupMenuControl the control to base a popup menu on
   * @return this builder instance
   */
  B popupMenuControl(Control popupMenuControl);

  /**
   * @param popupMenuControls the controls to base a popup menu on
   * @return this builder instance
   */
  B popupMenuControls(Controls popupMenuControls);

  /**
   * @param popupMenu the popup menu
   * @return this builder instance
   * @see JComponent#setComponentPopupMenu(JPopupMenu)
   */
  B popupMenu(JPopupMenu popupMenu);

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
   * @param transferHandler the transfer handler
   * @return this builder instance
   * @see JComponent#setTransferHandler(TransferHandler)
   */
  B transferHandler(TransferHandler transferHandler);

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
  B linkedValue(Value<T> linkedValue);

  /**
   * Creates a read-only link to the given {@link ValueObserver}.
   * @param linkedValueObserver a value to link to the component value
   * @return this builder instance
   */
  B linkedValueObserver(ValueObserver<T> linkedValueObserver);

  /**
   * Sets the initial value for the component, overridden by {@link #linkedValue(Value)}.
   * Note that null is not explicitely set, the component is assumed to represent the null
   * value right after it's been created.
   * @param initialValue the initial value
   * @return this builder instance
   */
  B initialValue(T initialValue);

  /**
   * @return a {@link ScrollPaneBuilder} using this component as the view
   */
  ScrollPaneBuilder scrollPane();

  /**
   * @param onBuild called when the component has been built, before any values are linked, not called on subsequent
   * calls until {@link #clear()} has been called.
   * @return this builder instance
   */
  B onBuild(Consumer<C> onBuild);

  /**
   * Builds and returns the component, note that subsequent calls return the same component until {@link #clear()} has been called.
   * @return the component
   */
  C build();

  /**
   * Builds and returns the component, note that subsequent calls return the same component until {@link #clear()} has been called.
   * @param onBuild called after the first call when the component is built, before any values are linked, not called on subsequent calls.
   * @return the component
   */
  C build(Consumer<C> onBuild);

  /**
   * Clears this builder so that it builds a new instance on next call to {@link #build()} or {@link #buildValue()}.
   * @return this builder instance
   */
  B clear();

  /**
   * Builds and returns the component value, note that subsequent calls return the same component value until {@link #clear()} has been called.
   * @return the component value
   */
  ComponentValue<T, C> buildValue();
}
