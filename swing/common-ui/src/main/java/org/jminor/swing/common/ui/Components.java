/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;
import org.jminor.common.state.StateObserver;
import org.jminor.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTree;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for UI components.
 */
public final class Components {

  private static final Map<RootPaneContainer, Integer> WAIT_CURSOR_REQUESTS = new HashMap<>();
  private static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
  private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
  private static JScrollBar verticalScrollBar;

  private Components() {}

  /**
   * Links the given action to the given StateObserver, so that the action is enabled
   * only when the observed state is active
   * @param enabledState the StateObserver with which to link the action
   * @param action the action
   * @return the linked action
   */
  public static Action linkToEnabledState(final StateObserver enabledState, final Action action) {
    requireNonNull(enabledState, "enabledState");
    requireNonNull(action, "action");
    action.setEnabled(enabledState.get());
    enabledState.addListener(() -> action.setEnabled(enabledState.get()));

    return action;
  }

  /**
   * Links the given components to the given StateObserver, so that each component is enabled and focusable
   * only when the observed state is active
   * @param enabledState the StateObserver with which to link the components
   * @param components the components
   */
  public static void linkToEnabledState(final StateObserver enabledState, final JComponent... components) {
    linkToEnabledState(enabledState, true, components);
  }

  /**
   * Links the given components to the given StateObserver, so that each component is enabled only when the observed state is active.
   * @param enabledState the StateObserver with which to link the components
   * @param includeFocusable if true then the focusable attribute is set as well as the enabled attribute
   * @param components the components
   */
  public static void linkToEnabledState(final StateObserver enabledState, final boolean includeFocusable, final JComponent... components) {
    requireNonNull(components, "components");
    requireNonNull(enabledState, "enabledState");
    for (final JComponent component : components) {
      if (component != null) {
        component.setEnabled(enabledState.get());
        if (includeFocusable) {
          component.setFocusable(enabledState.get());
        }
        enabledState.addListener(() -> {
          component.setEnabled(enabledState.get());
          if (includeFocusable) {
            component.setFocusable(enabledState.get());
          }
        });
      }
    }
  }

  /**
   * Returns a {@link EventObserver} notified each time the value of the given property changes in the given component.
   * @param component the component
   * @param property the property to listen to changes for
   * @param <T> the property data type
   * @return a {@link EventObserver} notified each time the value of the given property changes
   */
  public static <T> EventObserver<T> propertyChangeObserver(final JComponent component, final String property) {
    requireNonNull(component, "component");
    requireNonNull(property, "property");
    final Event<T> event = Events.event();
    component.addPropertyChangeListener(property,
            changeEvent -> event.onEvent((T) changeEvent.getNewValue()));

    return event.getObserver();
  }

  /**
   * Sets the preferred size of the given component to its current height and the given {@code preferredWidth}
   * @param component the component
   * @param preferredWidth the preferred width
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JComponent> T setPreferredWidth(final T component, final int preferredWidth) {
    component.setPreferredSize(new Dimension(preferredWidth, component.getPreferredSize().height));

    return component;
  }

  /**
   * Sets the preferred size of the given component to its current width and the given {@code preferredHeight}
   * @param component the component
   * @param preferredHeight the preferred height
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JComponent> T setPreferredHeight(final T component, final int preferredHeight) {
    component.setPreferredSize(new Dimension(component.getPreferredSize().width, preferredHeight));

    return component;
  }

  /**
   * Links the given BoundedRangeModels so that changes in {@code master} are reflected in {@code slave}
   * @param master the master model
   * @param slave the model to link with master
   */
  public static void linkBoundedRangeModels(final BoundedRangeModel master, final BoundedRangeModel slave) {
    master.addChangeListener(e -> slave.setRangeProperties(master.getValue(), master.getExtent(),
            master.getMinimum(), master.getMaximum(), master.getValueIsAdjusting()));
  }

  /**
   * Expands or collapses all the paths from a parent in the given tree
   * @param tree the tree
   * @param parent the parent from which to exapand/collapse
   * @param expand if true then the tree is expanded, collapsed otherwise
   */
  public static void expandAll(final JTree tree, final TreePath parent, final boolean expand) {
    // Traverse children
    final TreeNode node = (TreeNode) parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      final Enumeration e = node.children();
      while (e.hasMoreElements()) {
        expandAll(tree, parent.pathByAddingChild(e.nextElement()), expand);
      }
    }
    // Expansion or collapse must be done bottom-up
    if (expand) {
      tree.expandPath(parent);
    }
    else {
      tree.collapsePath(parent);
    }
  }

  /**
   * @return the preferred width of a JScrollBar
   */
  public static synchronized int getPreferredScrollBarWidth() {
    if (verticalScrollBar == null) {
      verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL);
    }

    return verticalScrollBar.getPreferredSize().width;
  }

  /**
   * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5018574
   * @param component the component
   * @param onFocusAction the action to run when the focus has been requested
   */
  public static void addInitialFocusHack(final JComponent component, final Action onFocusAction) {
    component.addHierarchyListener(e -> {
      if (component.isShowing() && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
        SwingUtilities.getWindowAncestor(component).addWindowFocusListener(new WindowAdapter() {
          @Override
          public void windowGainedFocus(final WindowEvent windowEvent) {
            component.requestFocus();
            if (onFocusAction != null) {
              onFocusAction.actionPerformed(new ActionEvent(component, 0, "onFocusAction"));
            }
          }
        });
      }
    });
  }

  /**
   * Creates a JPanel, using a BorderLayout, adding the given components to their respective positions.
   * @param north the panel to display in the BorderLayout.NORTH position
   * @param center the panel to display in the BorderLayout.CENTER position
   * @return a panel displaying the given components in the NORTH an CENTER positions in a BorderLayout
   */
  public static JPanel createNorthCenterPanel(final JComponent north, final JComponent center) {
    final JPanel panel = new JPanel(Layouts.createBorderLayout());
    panel.add(north, BorderLayout.NORTH);
    panel.add(center, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Creates a JPanel, using a BorderLayout, adding the given components to their respective positions.
   * @param west the panel to display in the BorderLayout.WEST position
   * @param center the panel to display in the BorderLayout.CENTER position
   * @return a panel displaying the given components in the WEST an CENTER positions in a BorderLayout
   */
  public static JPanel createWestCenterPanel(final JComponent west, final JComponent center) {
    final JPanel panel = new JPanel(Layouts.createBorderLayout());
    panel.add(west, BorderLayout.WEST);
    panel.add(center, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Creates a panel with {@code centerComponent} in the BorderLayout.CENTER position and a button based on buttonAction
   * in the BorderLayout.EAST position, with the buttons preferred size based on the preferred height of {@code centerComponent}.
   * @param centerComponent the center component
   * @param buttonAction the button action
   * @param buttonFocusable if true then the button is focusable, otherwise not
   * @return a panel
   */
  public static JPanel createEastButtonPanel(final JComponent centerComponent, final Action buttonAction,
                                             final boolean buttonFocusable) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JButton button = new JButton(buttonAction);
    button.setPreferredSize(new Dimension(centerComponent.getPreferredSize().height, centerComponent.getPreferredSize().height));
    button.setFocusable(buttonFocusable);

    panel.add(centerComponent, BorderLayout.CENTER);
    panel.add(button, BorderLayout.EAST);

    return panel;
  }

  /**
   * Searches the parent component hierarchy of the given component for
   * an ancestor of the given type
   * @param <T> the type of parent to find
   * @param component the component
   * @param clazz the class of the parent to find
   * @return the parent of the given component of the given type, null if none is found
   */
  public static <T> T getParentOfType(final Component component, final Class<T> clazz) {
    return (T) SwingUtilities.getAncestorOfClass(clazz, component);
  }

  /**
   * Adds a wait cursor request for the parent root pane of the given component,
   * the wait cursor is activated once a request is made, but only deactivated once all such
   * requests have been retracted. Best used in try/finally block combinations.
   * <pre>
   try {
     UiUtil.showWaitCursor(dialogParent);
     doSomething();
   }
   finally {
     UiUtil.hideWaitCursor(dialogParent);
   }
   * </pre>
   * @param component the component
   * @see #hideWaitCursor(JComponent)
   */
  public static void showWaitCursor(final JComponent component) {
    setWaitCursor(true, component);
  }

  /**
   * Removes a wait cursor request for the parent root pane of the given component,
   * the wait cursor is activated once a request is made, but only deactivated once all such
   * requests have been retracted. Best used in try/finally block combinations.
   * <pre>
   try {
     UiUtil.showWaitCursor(dialogParent);
     doSomething();
   }
   finally {
     UiUtil.hideWaitCursor(dialogParent);
   }
   * </pre>
   * @param component the component
   * @see #showWaitCursor(JComponent)
   */
  public static void hideWaitCursor(final JComponent component) {
    setWaitCursor(false, component);
  }

  /**
   * Adds or subtracts a wait cursor request for the parent root pane of the given component,
   * the wait cursor is activated once a request is made, but only deactivated once all such
   * requests have been retracted. Best used in try/finally block combinations.
   * <pre>
   try {
     UiUtil.showWaitCursor(dialogParent);
     doSomething();
   }
   finally {
     UiUtil.hideWaitCursor(dialogParent);
   }
   * </pre>
   * @param on if on, then the wait cursor is activated, otherwise it is deactivated
   * @param component the component
   */
  private static void setWaitCursor(final boolean on, final JComponent component) {
    RootPaneContainer root = getParentOfType(component, JDialog.class);
    if (root == null) {
      root = getParentOfType(component, JFrame.class);
    }
    if (root == null) {
      return;
    }

    synchronized (WAIT_CURSOR_REQUESTS) {
      if (!WAIT_CURSOR_REQUESTS.containsKey(root)) {
        WAIT_CURSOR_REQUESTS.put(root, 0);
      }

      int requests = WAIT_CURSOR_REQUESTS.get(root);
      if (on) {
        requests++;
      }
      else {
        requests--;
      }

      if ((requests == 1 && on) || (requests == 0 && !on)) {
        root.getRootPane().setCursor(on ? WAIT_CURSOR : DEFAULT_CURSOR);
      }
      if (requests == 0) {
        WAIT_CURSOR_REQUESTS.remove(root);
      }
      else {
        WAIT_CURSOR_REQUESTS.put(root, requests);
      }
    }
  }
}
