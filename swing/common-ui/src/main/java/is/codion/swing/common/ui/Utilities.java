/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
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
        enabledState.addListener(() -> SwingUtilities.invokeLater(() ->
                component.setEnabled(enabledState.get())));
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

    return event.getObserver();
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
              onFocusAction.actionPerformed(new ActionEvent(component, 0, "onFocusAction"));
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
   * Makes the text component accept files during drag and drop operations and
   * insert the absolute path of the dropped file (the first file in a list if more
   * than one file is dropped)
   * @param textComponent the text component
   */
  public static void addAcceptSingleFileDragAndDrop(JTextComponent textComponent) {
    requireNonNull(textComponent, "textComponent");
    textComponent.setDragEnabled(true);
    textComponent.setTransferHandler(new FileTransferHandler(textComponent));
  }

  /**
   * @param transferSupport a drag'n drop transfer support instance
   * @return true if the given transfer support instance represents a file or a list of files
   */
  public static boolean isFileDataFlavor(TransferHandler.TransferSupport transferSupport) {
    requireNonNull(transferSupport, "transferSupport");
    try {
      DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");

      return stream(transferSupport.getDataFlavors())
              .anyMatch(flavor -> flavor.isFlavorJavaFileListType() || flavor.equals(nixFileDataFlavor));
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the files described by the given transfer support object.
   * An empty list is returned if no files are found.
   * @param transferSupport the drag'n drop transfer support
   * @return the files described by the given transfer support object
   * @throws RuntimeException in case of an exception
   */
  public static List<File> getTransferFiles(TransferHandler.TransferSupport transferSupport) {
    requireNonNull(transferSupport, "transferSupport");
    try {
      for (DataFlavor flavor : transferSupport.getDataFlavors()) {
        if (flavor.isFlavorJavaFileListType()) {
          List<File> files = (List<File>) transferSupport.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

          return files.isEmpty() ? emptyList() : files;
        }
      }
      //the code below is for handling unix/linux
      List<File> files = new ArrayList<>();
      DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
      String data = (String) transferSupport.getTransferable().getTransferData(nixFileDataFlavor);
      for (StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens(); ) {
        String token = st.nextToken().trim();
        if (token.startsWith("#") || token.isEmpty()) {// comment line, by RFC 2483
          continue;
        }

        files.add(new File(new URI(token)));
      }

      return files;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Searches the parent component hierarchy of the given component for
   * an ancestor of the given type
   * @param <T> the type of parent to find
   * @param component the component
   * @param clazz the class of the parent to find
   * @return the parent of the given component of the given type, an empty Optional if none is found
   */
  public static <T> Optional<T> getParentOfType(Component component, Class<T> clazz) {
    return Optional.ofNullable((T) SwingUtilities.getAncestorOfClass(clazz, component));
  }

  private static final class FileTransferHandler extends TransferHandler {

    private final JTextComponent textComponent;

    private FileTransferHandler(JTextComponent textComponent) {
      this.textComponent = textComponent;
    }

    @Override
    public boolean canImport(TransferSupport transferSupport) {
      return isFileDataFlavor(transferSupport);
    }

    @Override
    public boolean importData(TransferSupport transferSupport) {
      List<File> files = getTransferFiles(transferSupport);
      if (files.isEmpty()) {
        return false;
      }

      textComponent.setText(files.get(0).getAbsolutePath());
      textComponent.requestFocusInWindow();
      return true;
    }
  }
}
