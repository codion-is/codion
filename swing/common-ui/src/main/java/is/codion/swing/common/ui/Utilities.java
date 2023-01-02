/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventObserver;
import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for UI related things.
 */
public final class Utilities {

  private static final String COMPONENT = "component";

  private Utilities() {}

  /**
   * Calls {@link JComponent#updateUI()} for the given components, ignores null components.
   * @param components the components to update the UI for
   */
  public static void updateUI(JComponent... components) {
    if (components != null) {
      updateUI(Arrays.asList(components));
    }
  }

  /**
   * Calls {@link JComponent#updateUI()} for the given components, ignores null components.
   * @param components the components to update the UI for
   */
  public static void updateUI(Collection<? extends JComponent> components) {
    if (components != null) {
      for (JComponent component : components) {
        if (component != null) {
          component.updateUI();
        }
      }
    }
  }

  /**
   * Links the given action to the given StateObserver, so that the action is enabled
   * only when the observed state is active
   * @param enabledState the StateObserver with which to link the action
   * @param action the action
   * @return the linked action
   */
  public static Action linkToEnabledState(StateObserver enabledState, Action action) {
    requireNonNull(enabledState, "enabledState");
    requireNonNull(action, "action");
    action.setEnabled(enabledState.get());
    enabledState.addListener(() -> action.setEnabled(enabledState.get()));

    return action;
  }

  /**
   * Links the given components to the given StateObserver, so that each component is enabled only when the observed state is active
   * @param enabledState the StateObserver with which to link the components
   * @param components the components
   */
  public static void linkToEnabledState(StateObserver enabledState, JComponent... components) {
    requireNonNull(components, "components");
    requireNonNull(enabledState, "enabledState");
    for (JComponent component : components) {
      if (component != null) {
        component.setEnabled(enabledState.get());
        enabledState.addDataListener(new EnableComponentListener(component));
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
  public static <T> EventObserver<T> propertyChangeObserver(JComponent component, String property) {
    requireNonNull(component, COMPONENT);
    requireNonNull(property, "property");
    Event<T> event = Event.event();
    component.addPropertyChangeListener(property, changeEvent -> event.onEvent((T) changeEvent.getNewValue()));

    return event.observer();
  }

  /**
   * Links the given BoundedRangeModels so that changes in {@code main} are reflected in {@code linked}
   * @param main the main model
   * @param linked the model to link with main
   */
  public static void linkBoundedRangeModels(BoundedRangeModel main, BoundedRangeModel linked) {
    requireNonNull(main, "main");
    requireNonNull(linked, "linked");
    main.addChangeListener(e -> linked.setRangeProperties(main.getValue(), main.getExtent(),
            main.getMinimum(), main.getMaximum(), main.getValueIsAdjusting()));
  }

  /**
   * Expands all the paths from a parent in the given tree
   * @param tree the tree
   * @param parent the parent from which to exapand
   */
  public static void expandAll(JTree tree, TreePath parent) {
    requireNonNull(tree, "tree");
    requireNonNull(parent, "parent");
    TreeNode node = (TreeNode) parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      Enumeration<? extends TreeNode> e = node.children();
      while (e.hasMoreElements()) {
        expandAll(tree, parent.pathByAddingChild(e.nextElement()));
      }
    }
    // Expansion or collapse must be done bottom-up
    tree.expandPath(parent);
  }

  /**
   * Collapses all the paths from a parent in the given tree
   * @param tree the tree
   * @param parent the parent from which to collapse
   */
  public static void collapseAll(JTree tree, TreePath parent) {
    requireNonNull(tree, "tree");
    requireNonNull(parent, "parent");
    TreeNode node = (TreeNode) parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      Enumeration<? extends TreeNode> e = node.children();
      while (e.hasMoreElements()) {
        collapseAll(tree, parent.pathByAddingChild(e.nextElement()));
      }
    }
    // Expansion or collapse must be done bottom-up
    tree.collapsePath(parent);
  }

  /**
   * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5018574
   * @param component the component
   * @param onFocusAction the action to run when the focus has been requested
   */
  public static void addInitialFocusHack(JComponent component, Action onFocusAction) {
    requireNonNull(component, COMPONENT);
    requireNonNull(onFocusAction, "onFocusAction");
    component.addHierarchyListener(e -> {
      if (component.isShowing() && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
        SwingUtilities.getWindowAncestor(component).addWindowFocusListener(new WindowAdapter() {
          @Override
          public void windowGainedFocus(WindowEvent windowEvent) {
            component.requestFocus();
            if (onFocusAction != null) {
              onFocusAction.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, "onFocusAction"));
            }
          }
        });
      }
    });
  }

  /**
   * Loads an icon as a resource
   * @param resourceClass the class owning the resource
   * @param resourceName the resource name
   * @return an icon
   */
  public static ImageIcon loadIcon(Class<?> resourceClass, String resourceName) {
    URL url = requireNonNull(resourceClass).getResource(resourceName);
    requireNonNull(url, "Resource: " + resourceName + " for " + resourceClass);

    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
  }

  /**
   * Sets a global font size multiplier.
   * @param multiplier the font size multiplier
   */
  public static void setFontSize(float multiplier) {
    UIDefaults defaults = UIManager.getDefaults();
    Enumeration<Object> enumeration = defaults.keys();
    while (enumeration.hasMoreElements()) {
      Object key = enumeration.nextElement();
      Object defaultValue = defaults.get(key);
      if (defaultValue instanceof Font) {
        Font font = (Font) defaultValue;
        int newSize = Math.round(font.getSize() * multiplier);
        if (defaultValue instanceof FontUIResource) {
          defaults.put(key, new FontUIResource(font.getName(), font.getStyle(), newSize));
        }
        else {
          defaults.put(key, new Font(font.getName(), font.getStyle(), newSize));
        }
      }
    }
  }

  /**
   * Returns a darker version of the given color, using 0.8 as the mulitiplication factor.
   * @param color the color to darken
   * @return a darker version of the given color
   * @see Color#darker()
   */
  public static Color darker(Color color) {
    return darker(color, 0.8);
  }

  /**
   * Returns a darker version of the given color, using the given factor.
   * @param color the color to darken
   * @param factor a number between 0 and 1, non-inclusive
   * @return a darker version of the given color
   * @see Color#darker()
   */
  public static Color darker(Color color, double factor) {
    requireNonNull(color);
    if (factor <= 0 || factor >= 1) {
      throw new IllegalArgumentException("Factor must be between 0 and 1, non-inclusive");
    }

    return new Color(Math.max((int) (color.getRed() * factor), 0),
            Math.max((int) (color.getGreen() * factor), 0),
            Math.max((int) (color.getBlue() * factor), 0),
            color.getAlpha());
  }

  /**
   * Sets the given string as clipboard contents
   * @param string the string to put on the clipboard
   */
  public static void setClipboard(String string) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
  }

  /**
   * Searches the parent component hierarchy of the given component for
   * an ancestor of the given type
   * @param <T> the type of parent to find
   * @param clazz the class of the parent to find
   * @param component the component
   * @return the parent of the given component of the given type, null if none is found
   */
  public static <T> T getParentOfType(Class<T> clazz, Component component) {
    return (T) SwingUtilities.getAncestorOfClass(clazz, component);
  }

  /**
   * Finds the first component of type {@link Window} in the parent hierarchy of {@code component}.
   * Note that if {@code component} is of type {@link Window}, it is returned.
   * @param component the component
   * @return the parent Window of the given component, null if none is found
   */
  public static Window getParentWindow(Component component) {
    if (component instanceof Window) {
      return (Window) component;
    }

    return getParentOfType(Window.class, component);
  }

  /**
   * Finds the first component of type {@link JFrame} in the parent hierarchy of {@code component}.
   * Note that if {@code component} is of type {@link JFrame}, it is returned.
   * @param component the component
   * @return the parent JFrame of the given component, null if none is found
   */
  public static JFrame getParentFrame(Component component) {
    if (component instanceof JFrame) {
      return (JFrame) component;
    }

    return getParentOfType(JFrame.class, component);
  }

  /**
   * Finds the first component of type {@link JDialog} in the parent hierarchy of {@code component}.
   * Note that if {@code component} is of type {@link JDialog}, it is returned.
   * @param component the component
   * @return the parent JDialog of the given component, null if none is found
   */
  public static JDialog getParentDialog(Component component) {
    if (component instanceof JDialog) {
      return (JDialog) component;
    }

    return getParentOfType(JDialog.class, component);
  }

  /**
   * Finds the parent Window and disposes it if found. If no parent Window is found this method has no effect
   * @param component the component which parent Window should be disposed
   * @return true if a parent Window was found and disposed
   */
  public static boolean disposeParentWindow(Component component) {
    Window parentWindow = getParentWindow(component);
    if (parentWindow != null) {
      parentWindow.dispose();

      return true;
    }

    return false;
  }

  /**
   * Note that GTKLookAndFeel is overridden with MetalLookAndFeel, since JTabbedPane
   * does not respect the 'TabbedPane.contentBorderInsets' setting, making hierachical
   * tabbed panes look bad
   * @return the default look and feel for the platform we're running on
   */
  public static String systemLookAndFeelClassName() {
    String systemLookAndFeel = UIManager.getSystemLookAndFeelClassName();
    if (systemLookAndFeel.endsWith("GTKLookAndFeel")) {
      systemLookAndFeel = "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    return systemLookAndFeel;
  }

  /**
   * @return true if the system or cross-platform look and feel is enabled
   * @see #systemLookAndFeelClassName()
   */
  public static boolean isSystemOrCrossPlatformLookAndFeelEnabled() {
    String lookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();

    return lookAndFeelClassName.equals(systemLookAndFeelClassName()) ||
            lookAndFeelClassName.equals(UIManager.getCrossPlatformLookAndFeelClassName());
  }

  private static final class EnableComponentListener implements EventDataListener<Boolean> {

    private final JComponent component;

    private EnableComponentListener(JComponent component) {
      this.component = component;
    }

    @Override
    public void onEvent(Boolean enabled) {
      if (SwingUtilities.isEventDispatchThread()) {
        component.setEnabled(enabled);
      }
      else {
        SwingUtilities.invokeLater(() -> component.setEnabled(enabled));
      }
    }
  }
}
