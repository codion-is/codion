/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.TaskScheduler;
import org.jminor.common.Util;
import org.jminor.common.state.StateObserver;
import org.jminor.swing.common.ui.layout.Layouts;

import javax.swing.AbstractAction;
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
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
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
 * A static utility class.
 */
public final class UiUtil {

  static {
    //otherwise a hierarchy of tabbed panes looks crappy
    UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 0, 0, 0));
  }

  /**
   * A wait cursor
   */
  public static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
  /**
   * The default cursor
   */
  public static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

  private static final Map<RootPaneContainer, Integer> WAIT_CURSOR_REQUESTS = new HashMap<>();
  private static JScrollBar verticalScrollBar;

  private UiUtil() {}

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
   * Creates a text field containing information about the memory usage
   * @param updateIntervalMilliseconds the interval between updating the memory usage info
   * @return a text field displaying the current VM memory usage
   */
  public static JTextField createMemoryUsageField(final int updateIntervalMilliseconds) {
    final JTextField textField = new JTextField(8);
    textField.setEditable(false);
    textField.setHorizontalAlignment(JTextField.CENTER);
    new TaskScheduler(() -> SwingUtilities.invokeLater(() ->
            textField.setText(Util.getMemoryUsageString())), updateIntervalMilliseconds, 0, TimeUnit.MILLISECONDS).start();

    return textField;
  }

  /**
   * Links the given action to the given StateObserver, so that the action is enabled
   * only when the observed state is active
   * @param enabledState the StateObserver with which to link the action
   * @param action the action
   * @return the linked action
   */
  public static Action linkToEnabledState(final StateObserver enabledState, final Action action) {
    if (enabledState != null && action != null) {
      action.setEnabled(enabledState.get());
      enabledState.addListener(() -> action.setEnabled(enabledState.get()));
    }

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
   * @param ratio a ratio, 0.0 - 1.0
   * @return a Dimension which is the size of the available screen times ratio
   */
  public static Dimension getScreenSizeRatio(final double ratio) {
    final Dimension screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();

    return new Dimension((int) (screen.getWidth() * ratio), (int) (screen.getHeight() * ratio));
  }

  /**
   * Resizes the given window so that if fits within the current screen bounds,
   * if the window already fits then calling this method has no effect
   * @param window the window to resize
   */
  public static void setSizeWithinScreenBounds(final Window window) {
    final Dimension screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
    final Dimension frameSize = window.getSize();
    if (frameSize.getHeight() > screenSize.getHeight() || frameSize.getWidth() > screenSize.getWidth()) {
      final Dimension newFrameSize = new Dimension((int) Math.min(frameSize.getWidth(), screenSize.getWidth()),
              (int) Math.min(frameSize.getHeight(), screenSize.getHeight()));
      window.setSize(newFrameSize);
    }
  }

  /**
   * Resizes the given window so that it is {@code screenSizeRatio} percent of the current screen size
   * @param window the window to resize
   * @param screenSizeRatio the screen size ratio
   */
  public static void resizeWindow(final Window window, final double screenSizeRatio) {
    resizeWindow(window, screenSizeRatio, null);
  }

  /**
   * Resizes the given window so that it is {@code screenSizeRatio} percent of the current screen size,
   * within the given minimum size
   * @param window the window to resize
   * @param screenSizeRatio the screen size ratio
   * @param minimumSize a minimum size
   */
  public static void resizeWindow(final Window window, final double screenSizeRatio,
                                  final Dimension minimumSize) {
    final Dimension ratioSize = getScreenSizeRatio(screenSizeRatio);
    if (minimumSize != null) {
      ratioSize.setSize(Math.max(minimumSize.width, ratioSize.width), Math.max(minimumSize.height, ratioSize.height));
    }

    window.setSize(ratioSize);
  }

  /**
   * @param component the component
   * @return the parent Window of the given component, null if none exists
   */
  public static Window getParentWindow(final Component component) {
    final Window window = getParentDialog(component);

    return window == null ? getParentFrame(component) : window;
  }

  /**
   * @param component the component
   * @return the parent JFrame of the given component, null if none exists
   */
  public static JFrame getParentFrame(final Component component) {
    return getParentOfType(component, JFrame.class);
  }

  /**
   * @param component the component
   * @return the parent JDialog of the given component, null if none exists
   */
  public static JDialog getParentDialog(final Component component) {
    return getParentOfType(component, JDialog.class);
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
   * Centers the given window on the screen
   * @param window the window to center on screen
   */
  public static void centerWindow(final Window window) {
    final Dimension size = window.getSize();
    final Dimension screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
    window.setLocation((int) (screen.getWidth() - size.getWidth()) / 2,
            (int) (screen.getHeight() - size.getHeight()) / 2);
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
   * Adds or subtracts a wait cursor request for the parent root pane of the given component,
   * the wait cursor is activated once a request is made, but only deactivated once all such
   * requests have been retracted. Best used in try/finally block combinations.
   * <pre>
   try {
   UiUtil.setWaitCursor(true, dialogParent);
   doSomething();
   }
   finally {
   UiUtil.setWaitCursor(false, dialogParent);
   }
   * </pre>
   * @param on if on, then the wait cursor is activated, otherwise it is deactivated
   * @param component the component
   */
  public static void setWaitCursor(final boolean on, final JComponent component) {
    RootPaneContainer root = getParentDialog(component);
    if (root == null) {
      root = getParentFrame(component);
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
   * Creates a JPanel, using a BorderLayout with a five pixel hgap and vgap, adding
   * the given components to their respective positions.
   * @param north the panel to display in the BorderLayout.NORTH position
   * @param center the panel to display in the BorderLayout.CENTER position
   * @return a panel displaying the given components in the NORTH an CENTER positions in a BorderLayout
   */
  public static JPanel northCenterPanel(final JComponent north, final JComponent center) {
    final JPanel panel = new JPanel(Layouts.createBorderLayout());
    panel.add(north, BorderLayout.NORTH);
    panel.add(center, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Adds a key event to the component which transfers focus
   * on enter, and backwards if shift is down
   * @param component the component
   * @param <T> the component type
   * @see #removeTransferFocusOnEnter(JTextComponent)
   * @return the component
   */
  public static <T extends JComponent> T transferFocusOnEnter(final T component) {
    KeyEvents.addKeyEvent(component, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, false, new TransferFocusAction(component));
    KeyEvents.addKeyEvent(component, KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, JComponent.WHEN_FOCUSED, false, new TransferFocusAction(component, true));

    return component;
  }

  /**
   * Removes the transfer focus action added via {@link #transferFocusOnEnter(javax.swing.JComponent)}
   * @param component the component
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JTextComponent> T removeTransferFocusOnEnter(final T component) {
    KeyEvents.addKeyEvent(component, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, false, null);
    KeyEvents.addKeyEvent(component, KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, JComponent.WHEN_FOCUSED, false, null);

    return component;
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
   * Sets the given string as clipboard contents
   * @param string the string to put on the clipboard
   */
  public static void setClipboard(final String string) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
  }

  /**
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
   * Sets the preferred size of the given component to its current height and the given {@code preferredWidth}
   * @param component the component
   * @param preferredWidth the preferred width
   */
  public static void setPreferredWidth(final JComponent component, final int preferredWidth) {
    component.setPreferredSize(new Dimension(preferredWidth, component.getPreferredSize().height));
  }

  /**
   * Sets the preferred size of the given component to its current width and the given {@code preferredHeight}
   * @param component the component
   * @param preferredHeight the preferred height
   */
  public static void setPreferredHeight(final JComponent component, final int preferredHeight) {
    component.setPreferredSize(new Dimension(component.getPreferredSize().width, preferredHeight));
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
   * Links the given BoundedRangeModels so that changes in {@code master} are reflected in {@code slave}
   * @param master the master model
   * @param slave the model to link with master
   */
  public static void linkBoundedRangeModels(final BoundedRangeModel master, final BoundedRangeModel slave) {
    master.addChangeListener(e -> slave.setRangeProperties(master.getValue(), master.getExtent(),
            master.getMinimum(), master.getMaximum(), master.getValueIsAdjusting()));
  }

  /**
   * An action which transfers focus either forward or backward for a given component
   */
  public static final class TransferFocusAction extends AbstractAction {

    private final JComponent component;
    private final boolean backward;

    /**
     * @param component the component
     */
    public TransferFocusAction(final JComponent component) {
      this(component, false);
    }

    /**
     * @param component the component
     * @param backward if true the focus is transferred backward
     */
    public TransferFocusAction(final JComponent component, final boolean backward) {
      super(backward ? "UiUtil.transferFocusBackward" : "UiUtil.transferFocusForward");
      this.component = component;
      this.backward = backward;
    }

    /**
     * Transfers focus according the the value of {@code backward}
     * @param e the action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
      if (backward) {
        component.transferFocusBackward();
      }
      else {
        component.transferFocus();
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
      return UiUtil.isFileDataFlavor(transferSupport);
    }

    @Override
    public boolean importData(final TransferSupport transferSupport) {
      final List<File> files = UiUtil.getTransferFiles(transferSupport);
      if (files.isEmpty()) {
        return false;
      }

      textComponent.setText(files.get(0).getAbsolutePath());
      textComponent.requestFocusInWindow();
      return true;
    }
  }
}
