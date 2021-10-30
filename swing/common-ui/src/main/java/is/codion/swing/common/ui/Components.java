/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.common.Memory;
import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A utility class for UI components.
 */
public final class Components {

  private static final Map<Window, Integer> WAIT_CURSOR_REQUESTS = new HashMap<>();
  private static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
  private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
  private static final Map<String, LookAndFeelProvider> LOOK_AND_FEEL_PROVIDERS = new HashMap<>();
  private static final String COMPONENT = "component";
  private static JScrollBar verticalScrollBar;

  static {
    final LookAndFeelProvider systemProvider = lookAndFeelProvider(getSystemLookAndFeelClassName());
    LOOK_AND_FEEL_PROVIDERS.put(systemProvider.getName(), systemProvider);
    final LookAndFeelProvider crossPlatformProvider = lookAndFeelProvider(UIManager.getCrossPlatformLookAndFeelClassName());
    if (!LOOK_AND_FEEL_PROVIDERS.containsKey(crossPlatformProvider.getName())) {
      LOOK_AND_FEEL_PROVIDERS.put(crossPlatformProvider.getName(), crossPlatformProvider);
    }
  }

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
   * Links the given components to the given StateObserver, so that each component is enabled only when the observed state is active
   * @param enabledState the StateObserver with which to link the components
   * @param components the components
   */
  public static void linkToEnabledState(final StateObserver enabledState, final JComponent... components) {
    requireNonNull(components, "components");
    requireNonNull(enabledState, "enabledState");
    for (final JComponent component : components) {
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
  public static <T> EventObserver<T> propertyChangeObserver(final JComponent component, final String property) {
    requireNonNull(component, COMPONENT);
    requireNonNull(property, "property");
    final Event<T> event = Event.event();
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
    requireNonNull(component, COMPONENT);
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
    requireNonNull(component, COMPONENT);
    component.setPreferredSize(new Dimension(component.getPreferredSize().width, preferredHeight));

    return component;
  }

  /**
   * Links the given BoundedRangeModels so that changes in {@code main} are reflected in {@code linked}
   * @param main the main model
   * @param linked the model to link with main
   */
  public static void linkBoundedRangeModels(final BoundedRangeModel main, final BoundedRangeModel linked) {
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
  public static void expandAll(final JTree tree, final TreePath parent) {
    requireNonNull(tree, "tree");
    requireNonNull(parent, "parent");
    final TreeNode node = (TreeNode) parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      final Enumeration<? extends TreeNode> e = node.children();
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
    requireNonNull(tree, "tree");
    requireNonNull(parent, "parent");
    final TreeNode node = (TreeNode) parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      final Enumeration<? extends TreeNode> e = node.children();
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
      verticalScrollBar = new JScrollBar(Adjustable.VERTICAL);
    }

    return verticalScrollBar.getPreferredSize().width;
  }

  /**
   * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5018574
   * @param component the component
   * @param onFocusAction the action to run when the focus has been requested
   */
  public static void addInitialFocusHack(final JComponent component, final Action onFocusAction) {
    requireNonNull(component, COMPONENT);
    requireNonNull(onFocusAction, "onFocusAction");
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
    requireNonNull(north, "north");
    requireNonNull(center, "center");
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
    requireNonNull(west, "west");
    requireNonNull(center, "center");
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
    requireNonNull(okAction, "okAction");
    requireNonNull(cancelAction, "cancelAction");
    final JButton okButton = new JButton(okAction);
    final JButton cancelButton = new JButton(cancelAction);
    okButton.setText(Messages.get(Messages.OK));
    okButton.setMnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0));
    cancelButton.setText(Messages.get(Messages.CANCEL));
    cancelButton.setMnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0));
    final JPanel buttonPanel = new JPanel(Layouts.gridLayout(1, 2));
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
    textField.setHorizontalAlignment(SwingConstants.CENTER);
    TaskScheduler.builder(() -> SwingUtilities.invokeLater(() -> textField.setText(Memory.getMemoryUsage())))
            .interval(updateIntervalMilliseconds)
            .timeUnit(TimeUnit.MILLISECONDS)
            .start();

    return textField;
  }

  /**
   * Loads an icon as a resource
   * @param resourceClass the class owning the resource
   * @param resourceName the resource name
   * @return an icon
   */
  public static ImageIcon loadIcon(final Class<?> resourceClass, final String resourceName) {
    final URL url = requireNonNull(resourceClass).getResource(resourceName);
    requireNonNull(url, "Resource: " + resourceName + " for " + resourceClass);

    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
  }

  /**
   * Sets a global font size multiplier.
   * @param multiplier the font size multiplier
   */
  public static void setFontSize(final float multiplier) {
    final UIDefaults defaults = UIManager.getDefaults();
    final Enumeration<Object> enumeration = defaults.keys();
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
   * Returns a darker version of the given color, using 0.8 as the mulitiplication factor.
   * @param color the color to darken
   * @return a darker version of the given color
   * @see Color#darker()
   */
  public static Color darker(final Color color) {
    return darker(color, 0.8);
  }

  /**
   * Returns a darker version of the given color, using the given factor.
   * @param color the color to darken
   * @param factor a number between 0 and 1, non-inclusive
   * @return a darker version of the given color
   * @see Color#darker()
   */
  public static Color darker(final Color color, final double factor) {
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
   * Adds the given look and feel provider.
   * Note that this overrides any existing look and feel provider with the same name.
   * @param lookAndFeelProvider the look and feel provider to add
   */
  public static void addLookAndFeelProvider(final LookAndFeelProvider lookAndFeelProvider) {
    LOOK_AND_FEEL_PROVIDERS.put(requireNonNull(lookAndFeelProvider).getName(), lookAndFeelProvider);
  }

  /**
   * Returns a look and feel provider with the given name, if available
   * @param name the look and feel name
   * @return a look and feel provider, null if not found
   */
  public static Optional<LookAndFeelProvider> getLookAndFeelProvider(final String name) {
    return name == null ? Optional.empty() : Optional.ofNullable(LOOK_AND_FEEL_PROVIDERS.get(name));
  }

  /**
   * Allows the user the select between all available Look and Feels.
   * @param dialogOwner the dialog owner
   * @param dialogTitle the dialog title
   * @return the selected look and feel provider, null if none was selected
   */
  public static LookAndFeelProvider selectLookAndFeel(final JComponent dialogOwner, final String dialogTitle) {
    final List<Item<LookAndFeelProvider>> items = new ArrayList<>();
    final Value<Item<LookAndFeelProvider>> currentLookAndFeel = Value.value();
    final String currentLookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();
    LOOK_AND_FEEL_PROVIDERS.values().stream()
            .sorted(Comparator.comparing(LookAndFeelProvider::getName))
            .map(provider -> Item.item(provider, provider.getName()))
            .forEach(item -> {
              items.add(item);
              if (currentLookAndFeelClassName.equals(item.getValue().getClassName())) {
                currentLookAndFeel.set(item);
              }
            });
    final ItemComboBoxModel<LookAndFeelProvider> comboBoxModel = ItemComboBoxModel.createModel(items);
    currentLookAndFeel.toOptional().ifPresent(comboBoxModel::setSelectedItem);

    final int option = JOptionPane.showOptionDialog(dialogOwner, new JComboBox<>(comboBoxModel),
            dialogTitle, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);

    return option == JOptionPane.OK_OPTION ? comboBoxModel.getSelectedValue().getValue() : null;
  }

  /**
   * Note that GTKLookAndFeel is overridden with MetalLookAndFeel, since JTabbedPane
   * does not respect the 'TabbedPane.contentBorderInsets' setting, making hierachical
   * tabbed panes look bad
   * @return the default look and feel for the platform we're running on
   */
  public static String getSystemLookAndFeelClassName() {
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
    requireNonNull(textComponent, "textComponent");
    textComponent.setDragEnabled(true);
    textComponent.setTransferHandler(new FileTransferHandler(textComponent));
  }

  /**
   * @param transferSupport a drag'n drop transfer support instance
   * @return true if the given transfer support instance represents a file or a list of files
   */
  public static boolean isFileDataFlavor(final TransferHandler.TransferSupport transferSupport) {
    requireNonNull(transferSupport, "transferSupport");
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
   * @param transferSupport the drag'n drop transfer support
   * @return the files described by the given transfer support object
   * @throws RuntimeException in case of an exception
   */
  public static List<File> getTransferFiles(final TransferHandler.TransferSupport transferSupport) {
    requireNonNull(transferSupport, "transferSupport");
    try {
      for (final DataFlavor flavor : transferSupport.getDataFlavors()) {
        if (flavor.isFlavorJavaFileListType()) {
          final List<File> files = (List<File>) transferSupport.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

          return files.isEmpty() ? emptyList() : files;
        }
      }
      //the code below is for handling unix/linux
      final List<File> files = new ArrayList<>();
      final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
      final String data = (String) transferSupport.getTransferable().getTransferData(nixFileDataFlavor);
      for (final StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens(); ) {
        final String token = st.nextToken().trim();
        if (token.startsWith("#") || token.isEmpty()) {// comment line, by RFC 2483
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
   * Adds a key event to the component which transfers focus
   * on enter, and backwards if shift is down
   * @param component the component
   * @param <T> the component type
   * @see #removeTransferFocusOnEnter(JComponent)
   * @return the component
   */
  public static <T extends JComponent> T transferFocusOnEnter(final T component) {
    transferFocusForwardBuilder(component).enable(component);
    transferFocusBackwardsBuilder(component).enable(component);

    return component;
  }

  /**
   * Removes the transfer focus action added via {@link #transferFocusOnEnter(JComponent)}
   * @param component the component
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JComponent> T removeTransferFocusOnEnter(final T component) {
    transferFocusForwardBuilder(component).disable(component);
    transferFocusBackwardsBuilder(component).disable(component);

    return component;
  }

  /**
   * Instantiates an Action for transferring keyboard focus forward.
   * @param component the component
   * @return an Action for transferring focus
   */
  public static Action transferFocusForwardAction(final JComponent component) {
    return new TransferFocusAction(component);
  }

  /**
   * Instantiates an Action for transferring keyboard focus backward.
   * @param component the component
   * @return an Action for transferring focus
   */
  public static Action transferFocusBackwardAction(final JComponent component) {
    return new TransferFocusAction(component, true);
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
    showWaitCursor(Windows.getParentWindow(component));
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
    hideWaitCursor(Windows.getParentWindow(component));
  }

  public static void showWaitCursor(final Window window) {
    setWaitCursor(true, window);
  }

  /**
   * Removes a wait cursor request for the given window
   * @param window the window
   */
  public static void hideWaitCursor(final Window window) {
    setWaitCursor(false, window);
  }

  /**
   * Adds a wait cursor request for the given window
   * @param window the window
   */
  private static void setWaitCursor(final boolean on, final Window window) {
    if (window == null) {
      return;
    }

    synchronized (WAIT_CURSOR_REQUESTS) {
      int requests = WAIT_CURSOR_REQUESTS.computeIfAbsent(window, win -> 0);
      if (on) {
        requests++;
      }
      else {
        requests--;
      }

      if ((requests == 1 && on) || (requests == 0 && !on)) {
        window.setCursor(on ? WAIT_CURSOR : DEFAULT_CURSOR);
      }
      if (requests == 0) {
        WAIT_CURSOR_REQUESTS.remove(window);
      }
      else {
        WAIT_CURSOR_REQUESTS.put(window, requests);
      }
    }
  }

  private static <T extends JComponent> KeyEvents.KeyEventBuilder transferFocusBackwardsBuilder(final T component) {
    return KeyEvents.builder(KeyEvent.VK_ENTER)
            .modifiers(InputEvent.SHIFT_DOWN_MASK)
            .condition(JComponent.WHEN_FOCUSED)
            .onKeyPressed()
            .action(transferFocusBackwardAction(component));
  }

  private static <T extends JComponent> KeyEvents.KeyEventBuilder transferFocusForwardBuilder(final T component) {
    return KeyEvents.builder(KeyEvent.VK_ENTER)
            .modifiers(component instanceof JTextArea ? InputEvent.CTRL_DOWN_MASK : 0)
            .condition(JComponent.WHEN_FOCUSED)
            .onKeyPressed()
            .action(transferFocusForwardAction(component));
  }

  /**
   * Provides a LookAndFeel implementation.
   */
  public interface LookAndFeelProvider {

    /**
     * The name of the underlying LookAndFeel class
     * @return the look and feel classname
     */
    String getClassName();

    /**
     * @return a unique name representing this look and feel, the classname by default
     */
    default String getName() {
      return getClassName();
    }

    /**
     * Configures and enables this LookAndFeel.
     */
    default void enable() {
      try {
        UIManager.setLookAndFeel(getClassName());
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Instantiates a new LookAndFeelProvider, using {@link UIManager#setLookAndFeel(String)} to enable.
   * @param classname the look and feel classname
   * @return a look and feel provider
   */
  public static LookAndFeelProvider lookAndFeelProvider(final String classname) {
    return lookAndFeelProvider(classname, () -> {
      try {
        UIManager.setLookAndFeel(classname);
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Instantiates a new LookAndFeelProvider.
   * @param classname the look and feel classname
   * @param enabler configures and enables this look and feel
   * @return a look and feel provider
   */
  public static LookAndFeelProvider lookAndFeelProvider(final String classname, final Runnable enabler) {
    return lookAndFeelProvider(classname, classname, enabler);
  }

  /**
   * Instantiates a new LookAndFeelProvider, using {@link UIManager#setLookAndFeel(String)} to enable.
   * @param classname the look and feel classname
   * @param name a unique name
   * @return a look and feel provider
   */
  public static LookAndFeelProvider lookAndFeelProvider(final String classname, final String name) {
    return lookAndFeelProvider(classname, name, () -> {
      try {
        UIManager.setLookAndFeel(classname);
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Instantiates a new LookAndFeelProvider.
   * @param classname the look and feel classname
   * @param name a unique name
   * @param enabler configures and enables this look and feel
   * @return a look and feel provider
   */
  public static LookAndFeelProvider lookAndFeelProvider(final String classname, final String name, final Runnable enabler) {
    return new DefaultLookAndFeelProvider(classname, name, enabler);
  }

  private static final class DefaultLookAndFeelProvider implements LookAndFeelProvider {

    private final String classname;
    private final String name;
    private final Runnable enabler;

    private DefaultLookAndFeelProvider(final String classname, final String name, final Runnable enabler) {
      this.classname = requireNonNull(classname);
      this.name = requireNonNull(name);
      this.enabler = requireNonNull(enabler);
    }

    @Override
    public String getClassName() {
      return classname;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void enable() {
      this.enabler.run();
    }

    @Override
    public String toString() {
      return getName();
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

  /**
   * An action which transfers focus either forward or backward for a given component
   */
  private static final class TransferFocusAction extends AbstractAction {

    private final JComponent component;
    private final boolean backward;

    /**
     * Instantiates an Action for transferring keyboard focus.
     * @param component the component
     */
    private TransferFocusAction(final JComponent component) {
      this(component, false);
    }

    /**
     * @param component the component
     * @param backward if true the focus is transferred backward
     */
    private TransferFocusAction(final JComponent component, final boolean backward) {
      super(backward ? "KeyEvents.transferFocusBackward" : "KeyEvents.transferFocusForward");
      this.component = requireNonNull(component, COMPONENT);
      this.backward = backward;
    }

    /**
     * Transfers focus according the value of {@code backward}
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
}
