/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for windows, dialogs and frames.
 */
public final class Windows {

  private Windows() {}

  /**
   * @param ratio a ratio, 0.0 - 1.0
   * @return a Dimension which is the size of the available screen times ratio
   */
  public static Dimension getScreenSizeRatio(double ratio) {
    Dimension screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();

    return new Dimension((int) (screen.getWidth() * ratio), (int) (screen.getHeight() * ratio));
  }

  /**
   * Resizes the given window so that if fits within the current screen bounds,
   * if the window already fits then calling this method has no effect
   * @param window the window to resize
   */
  public static void setSizeWithinScreenBounds(Window window) {
    Dimension screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
    Dimension frameSize = window.getSize();
    if (frameSize.getHeight() > screenSize.getHeight() || frameSize.getWidth() > screenSize.getWidth()) {
      Dimension newFrameSize = new Dimension((int) Math.min(frameSize.getWidth(), screenSize.getWidth()),
              (int) Math.min(frameSize.getHeight(), screenSize.getHeight()));
      window.setSize(newFrameSize);
    }
  }

  /**
   * Resizes the given window so that it is {@code screenSizeRatio} percent of the current screen size
   * @param window the window to resize
   * @param screenSizeRatio the screen size ratio
   */
  public static void resizeWindow(Window window, double screenSizeRatio) {
    resizeWindow(window, screenSizeRatio, null);
  }

  /**
   * Resizes the given window so that it is {@code screenSizeRatio} percent of the current screen size,
   * within the given minimum size
   * @param window the window to resize
   * @param screenSizeRatio the screen size ratio
   * @param minimumSize a minimum size
   */
  public static void resizeWindow(Window window, double screenSizeRatio,
                                  Dimension minimumSize) {
    Dimension ratioSize = getScreenSizeRatio(screenSizeRatio);
    if (minimumSize != null) {
      ratioSize.setSize(Math.max(minimumSize.width, ratioSize.width), Math.max(minimumSize.height, ratioSize.height));
    }

    window.setSize(ratioSize);
  }

  /**
   * Finds the first component of type {@link Window} in the parent hierarchy of {@code component}.
   * Note that if {@code component} is of type {@link Window}, it is returned.
   * @param component the component
   * @return the parent Window of the given component, an empty Optional if none exists
   */
  public static Optional<Window> getParentWindow(Component component) {
    if (component instanceof Window) {
      return Optional.of((Window) component);
    }

    return Utilities.getParentOfType(component, Window.class);
  }

  /**
   * Finds the first component of type {@link JFrame} in the parent hierarchy of {@code component}.
   * Note that if {@code component} is of type {@link JFrame}, it is returned.
   * @param component the component
   * @return the parent JFrame of the given component, an empty Optional if none exists
   */
  public static Optional<JFrame> getParentFrame(Component component) {
    if (component instanceof JFrame) {
      return Optional.of((JFrame) component);
    }

    return Utilities.getParentOfType(component, JFrame.class);
  }

  /**
   * Finds the first component of type {@link JDialog} in the parent hierarchy of {@code component}.
   * Note that if {@code component} is of type {@link JDialog}, it is returned.
   * @param component the component
   * @return the parent JDialog of the given component, an empty Optional if none exists
   */
  public static Optional<JDialog> getParentDialog(Component component) {
    if (component instanceof JDialog) {
      return Optional.of((JDialog) component);
    }

    return Utilities.getParentOfType(component, JDialog.class);
  }

  /**
   * Centers the given window on the screen
   * @param window the window to center on screen
   */
  public static void centerWindow(Window window) {
    Dimension size = window.getSize();
    Dimension screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
    window.setLocation((int) (screen.getWidth() - size.getWidth()) / 2,
            (int) (screen.getHeight() - size.getHeight()) / 2);
  }

  /**
   * @param component the component to display in the frame
   * @return a frame builder
   */
  public static FrameBuilder frameBuilder(JComponent component) {
    return new DefaultFrameBuilder(component);
  }

  /**
   * A builder for a JFrame.
   */
  public interface FrameBuilder {

    /**
     * @param title the title
     * @return this builder instance
     */
    FrameBuilder title(String title);

    /**
     * @param icon the icon
     * @return this builder instance
     */
    FrameBuilder icon(ImageIcon icon);

    /**
     * @param size the size
     * @return this builder instance
     */
    FrameBuilder size(Dimension size);

    /**
     * @param resizable true if the frame should be resizable
     * @return this builder instance
     */
    FrameBuilder resizable(boolean resizable);

    /**
     * @param relativeTo the component to which the location should be relative
     * @return this builder instance
     */
    FrameBuilder relativeTo(JComponent relativeTo);

    /**
     * @param onOpened called when the frame has been opened
     * @return this builder instance
     */
    FrameBuilder onOpened(Consumer<WindowEvent> onOpened);

    /**
     * @param onClosed called when the frame has been closed
     * @return this builder instance
     */
    FrameBuilder onClosed(Consumer<WindowEvent> onClosed);

    /**
     * @param onClosing called when the frame is about to be closed
     * @return this builder instance
     */
    FrameBuilder onClosing(Consumer<WindowEvent> onClosing);

    /**
     * Default {@link WindowConstants#DISPOSE_ON_CLOSE}.
     * @param defaultCloseOperation the default frame close operation
     * @return this builder instance
     */
    FrameBuilder defaultCloseOperation(int defaultCloseOperation);

    /**
     * @param menuBar the main menu bar
     * @return this builder instance
     */
    FrameBuilder menuBar(JMenuBar menuBar);

    /**
     * @param extendedState the extends state
     * @return this builder instance
     * @see JFrame#setExtendedState(int)
     */
    FrameBuilder extendedState(int extendedState);

    /**
     * This is overridden by setting the {@link #relativeTo(JComponent)} component.
     * @param centerFrame true if the frame should be centered in on the screen
     * @return this builder instance
     */
    FrameBuilder centerFrame(boolean centerFrame);

    /**
     * @param windowListener a window listener
     * @return this builder instance
     */
    FrameBuilder windowListener(WindowListener windowListener);

    /**
     * @return a JFrame based on this builder
     */
    JFrame build();

    /**
     * Builds and shows a JFrame based on this builder
     * @return a JFrame based on this builder
     */
    JFrame show();
  }

  private static final class DefaultFrameBuilder implements FrameBuilder {

    private final JComponent component;
    private final List<WindowListener> windowListeners = new ArrayList<>(0);

    private ImageIcon icon;
    private String title;
    private Consumer<WindowEvent> onClosing;
    private Consumer<WindowEvent> onClosed;
    private Consumer<WindowEvent> onOpened;
    private Dimension size;
    private boolean resizable = true;
    private JComponent relativeTo;
    private int defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE;
    private JMenuBar menuBar;
    private int extendedState = Frame.NORMAL;
    private boolean centerFrame;

    private DefaultFrameBuilder(JComponent component) {
      this.component = requireNonNull(component);
    }

    @Override
    public FrameBuilder title(String title) {
      this.title = title;
      return this;
    }

    @Override
    public FrameBuilder icon(ImageIcon icon) {
      this.icon = icon;
      return this;
    }

    @Override
    public FrameBuilder size(Dimension size) {
      this.size = size;
      return this;
    }

    @Override
    public FrameBuilder resizable(boolean resizable) {
      this.resizable = resizable;
      return this;
    }

    @Override
    public FrameBuilder relativeTo(JComponent relativeTo) {
      this.relativeTo = relativeTo;
      return this;
    }

    @Override
    public FrameBuilder defaultCloseOperation(int defaultCloseOperation) {
      this.defaultCloseOperation = defaultCloseOperation;
      return this;
    }

    @Override
    public FrameBuilder onOpened(Consumer<WindowEvent> onOpened) {
      this.onOpened = onOpened;
      return this;
    }

    @Override
    public FrameBuilder onClosed(Consumer<WindowEvent> onClosed) {
      this.onClosed = onClosed;
      return this;
    }

    @Override
    public FrameBuilder onClosing(Consumer<WindowEvent> onClosing) {
      this.onClosing = onClosing;
      return this;
    }

    @Override
    public FrameBuilder menuBar(JMenuBar menuBar) {
      this.menuBar = menuBar;
      return this;
    }

    @Override
    public FrameBuilder extendedState(int extendedState) {
      this.extendedState = extendedState;
      return this;
    }

    @Override
    public FrameBuilder centerFrame(boolean centerFrame) {
      this.centerFrame = centerFrame;
      return this;
    }

    @Override
    public FrameBuilder windowListener(WindowListener windowListener) {
      this.windowListeners.add(requireNonNull(windowListener));
      return this;
    }

    @Override
    public JFrame build() {
      JFrame frame = new JFrame();
      frame.setDefaultCloseOperation(defaultCloseOperation);
      frame.setLayout(Layouts.borderLayout());
      frame.add(component, BorderLayout.CENTER);
      if (title != null) {
        frame.setTitle(title);
      }
      if (icon != null) {
        frame.setIconImage(icon.getImage());
      }
      if (size != null) {
        frame.setSize(size);
      }
      else {
        frame.pack();
        setSizeWithinScreenBounds(frame);
      }
      if (menuBar != null) {
        frame.setJMenuBar(menuBar);
      }
      frame.setResizable(resizable);
      if (relativeTo != null) {
        frame.setLocationRelativeTo(relativeTo);
      }
      else if (centerFrame) {
        centerWindow(frame);
      }
      frame.setExtendedState(extendedState);
      if (onClosing != null || onClosed != null || onOpened != null) {
        frame.addWindowListener(new FrameListener(onClosing, onClosed, onOpened));
      }
      windowListeners.forEach(frame::addWindowListener);

      return frame;
    }

    @Override
    public JFrame show() {
      JFrame frame = build();
      frame.setVisible(true);

      return frame;
    }
  }

  private static final class FrameListener extends WindowAdapter {

    private final Consumer<WindowEvent> onClosing;
    private final Consumer<WindowEvent> onClosed;
    private final Consumer<WindowEvent> onOpened;

    private FrameListener(Consumer<WindowEvent> onClosing, Consumer<WindowEvent> onClosed, Consumer<WindowEvent> onOpened) {
      this.onClosing = onClosing;
      this.onClosed = onClosed;
      this.onOpened = onOpened;
    }

    @Override
    public void windowOpened(WindowEvent e) {
      if (onOpened != null) {
        onOpened.accept(e);
      }
    }

    @Override
    public void windowClosing(WindowEvent e) {
      if (onClosing != null) {
        onClosing.accept(e);
      }
    }

    @Override
    public void windowClosed(WindowEvent e) {
      if (onClosed != null) {
        onClosed.accept(e);
      }
    }
  }
}
