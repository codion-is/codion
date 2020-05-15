/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.common.Memory;
import is.codion.common.TaskScheduler;
import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.event.Events;
import is.codion.common.i18n.Messages;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
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
    requireNonNull(components, "components");
    requireNonNull(enabledState, "enabledState");
    for (final JComponent component : components) {
      if (component != null) {
        component.setEnabled(enabledState.get());
        component.setFocusable(enabledState.get());
        enabledState.addListener(() -> SwingUtilities.invokeLater(() -> {
          component.setEnabled(enabledState.get());
          component.setFocusable(enabledState.get());
        }));
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
    component.addPropertyChangeListener(property, changeEvent -> event.onEvent((T) changeEvent.getNewValue()));

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
   * Expands all the paths from a parent in the given tree
   * @param tree the tree
   * @param parent the parent from which to exapand
   */
  public static void expandAll(final JTree tree, final TreePath parent) {
    // Traverse children
    final TreeNode node = (TreeNode) parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      final Enumeration e = node.children();
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
  public static void collapseAll(final JTree tree, final TreePath parent) {
    final TreeNode node = (TreeNode) parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      final Enumeration e = node.children();
      while (e.hasMoreElements()) {
        collapseAll(tree, parent.pathByAddingChild(e.nextElement()));
      }
    }
    // Expansion or collapse must be done bottom-up
    tree.collapsePath(parent);
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
    final JPanel panel = new JPanel(Layouts.borderLayout());
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
    final JPanel panel = new JPanel(Layouts.borderLayout());
    panel.add(west, BorderLayout.WEST);
    panel.add(center, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Creates a panel with {@code centerComponent} in the BorderLayout.CENTER position and a non-focusable button based on buttonAction
   * in the BorderLayout.EAST position, with the buttons preferred size based on the preferred height of {@code centerComponent}.
   * @param centerComponent the center component
   * @param buttonAction the button action
   * @return a panel
   * @see #createEastFocusableButtonPanel(JComponent, Action)
   */
  public static JPanel createEastButtonPanel(final JComponent centerComponent, final Action buttonAction) {
    requireNonNull(centerComponent, "centerComponent");
    requireNonNull(buttonAction, "buttonAction");
    final JPanel panel = new JPanel(new BorderLayout());
    final JButton button = new JButton(buttonAction);
    button.setPreferredSize(new Dimension(centerComponent.getPreferredSize().height, centerComponent.getPreferredSize().height));
    button.setFocusable(false);
    panel.add(centerComponent, BorderLayout.CENTER);
    panel.add(button, BorderLayout.EAST);

    return panel;
  }

  /**
   * Creates a panel with {@code centerComponent} in the BorderLayout.CENTER position and a focusable button based on buttonAction
   * in the BorderLayout.EAST position, with the buttons preferred size based on the preferred height of {@code centerComponent}.
   * @param centerComponent the center component
   * @param buttonAction the button action
   * @return a panel
   */
  public static JPanel createEastFocusableButtonPanel(final JComponent centerComponent, final Action buttonAction) {
    final JPanel panel = createEastButtonPanel(centerComponent, buttonAction);
    for (final Component component : panel.getComponents()) {
      if (component instanceof JButton && ((JButton) component).getAction() == buttonAction) {
        component.setFocusable(true);
      }
    }

    return panel;
  }

  /**
   * Creates a JPanel with two buttons, based on the given ok and cancel actions.
   * @param okAction the OK button action
   * @param cancelAction the cancel button action
   * @return a ok/cancel button panel
   */
  public static JPanel createOkCancelButtonPanel(final Action okAction, final Action cancelAction) {
    final JButton okButton = new JButton(okAction);
    final JButton cancelButton = new JButton(cancelAction);
    okButton.setText(Messages.get(Messages.OK));
    okButton.setMnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0));
    cancelButton.setText(Messages.get(Messages.CANCEL));
    cancelButton.setMnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0));
    final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);

    return buttonPanel;
  }

  /**
   * Creates a text field containing information about the memory usage in KB.
   * @param updateIntervalMilliseconds the interval between updating the memory usage info
   * @return a text field displaying the current VM memory usage
   */
  public static JTextField createMemoryUsageField(final int updateIntervalMilliseconds) {
    final JTextField textField = new JTextField(8);
    textField.setEditable(false);
    textField.setHorizontalAlignment(JTextField.CENTER);
    new TaskScheduler(() -> SwingUtilities.invokeLater(() ->
            textField.setText(Memory.getMemoryUsage())), updateIntervalMilliseconds, 0, TimeUnit.MILLISECONDS).start();

    return textField;
  }

  /**
   * Sets a global font size multiplier.
   * @param multiplier the font size multiplier
   */
  public static void setFontSize(final float multiplier) {
    final UIDefaults defaults = UIManager.getDefaults();
    final Enumeration enumeration = defaults.keys();
    while (enumeration.hasMoreElements()) {
      final Object key = enumeration.nextElement();
      final Object defaultValue = defaults.get(key);
      if (defaultValue instanceof Font) {
        final Font font = (Font) defaultValue;
        final int newSize = Math.round(font.getSize() * multiplier);
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
   * Note that GTKLookAndFeel is overridden with MetalLookAndFeel, since JTabbedPane
   * does not respect the 'TabbedPane.contentBorderInsets' setting, making hierachical
   * tabbed panes look bad
   * @return the default look and feel for the platform we're running on
   */
  public static String getDefaultLookAndFeelClassName() {
    String systemLookAndFeel = UIManager.getSystemLookAndFeelClassName();
    if (systemLookAndFeel.endsWith("GTKLookAndFeel")) {
      systemLookAndFeel = "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    return systemLookAndFeel;
  }

  /**
   * Sets the given string as clipboard contents
   * @param string the string to put on the clipboard
   */
  public static void setClipboard(final String string) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
  }

  /**
   * Makes the text component accept files during drag and drop operations and
   * insert the absolute path of the dropped file (the first file in a list if more
   * than one file is dropped)
   * @param textComponent the text component
   */
  public static void addAcceptSingleFileDragAndDrop(final JTextComponent textComponent) {
    textComponent.setDragEnabled(true);
    textComponent.setTransferHandler(new FileTransferHandler(textComponent));
  }

  /**
   * @param transferSupport a drag'n drop transfer support instance
   * @return true if the given transfer support instance represents a file or a list of files
   */
  public static boolean isFileDataFlavor(final TransferHandler.TransferSupport transferSupport) {
    try {
      final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");

      return stream(transferSupport.getDataFlavors())
              .anyMatch(flavor -> flavor.isFlavorJavaFileListType() || flavor.equals(nixFileDataFlavor));
    }
    catch (final ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the files described by the given transfer support object.
   * An empty list is returned if no files are found.
   * @param support the drag'n drop transfer support
   * @return the files described by the given transfer support object
   * @throws RuntimeException in case of an exception
   */
  public static List<File> getTransferFiles(final TransferHandler.TransferSupport support) {
    try {
      for (final DataFlavor flavor : support.getDataFlavors()) {
        if (flavor.isFlavorJavaFileListType()) {
          final List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

          return files.isEmpty() ? emptyList() : files;
        }
      }
      //the code below is for handling unix/linux
      final List<File> files = new ArrayList<>();
      final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
      final String data = (String) support.getTransferable().getTransferData(nixFileDataFlavor);
      for (final StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens(); ) {
        final String token = st.nextToken().trim();
        if (token.startsWith("#") || token.length() == 0) {// comment line, by RFC 2483
          continue;
        }

        files.add(new File(new URI(token)));
      }

      return files;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
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
     Components.showWaitCursor(component);
     doSomething();
   }
   finally {
     Components.hideWaitCursor(component);
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
     Components.showWaitCursor(component);
     doSomething();
   }
   finally {
     Components.hideWaitCursor(component);
   }
   * </pre>
   * @param component the component
   * @see #showWaitCursor(JComponent)
   */
  public static void hideWaitCursor(final JComponent component) {
    setWaitCursor(false, component);
  }

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

  private static final class FileTransferHandler extends TransferHandler {

    private final JTextComponent textComponent;

    private FileTransferHandler(final JTextComponent textComponent) {
      this.textComponent = textComponent;
    }

    @Override
    public boolean canImport(final TransferSupport transferSupport) {
      return isFileDataFlavor(transferSupport);
    }

    @Override
    public boolean importData(final TransferSupport transferSupport) {
      final List<File> files = getTransferFiles(transferSupport);
      if (files.isEmpty()) {
        return false;
      }

      textComponent.setText(files.get(0).getAbsolutePath());
      textComponent.requestFocusInWindow();
      return true;
    }
  }
}
