/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
   * Finds the first component of type {@link Window} in the parent hierarchy of {@code component}.
   * Note that if {@code component} is of type {@link Window}, it is returned.
   * @param component the component
   * @return the parent Window of the given component, null if none exists
   */
  public static Window getParentWindow(final Component component) {
    if (component instanceof Window) {
      return (Window) component;
    }

    return Utilities.getParentOfType(component, Window.class);
  }

  /**
   * Finds the first component of type {@link JFrame} in the parent hierarchy of {@code component}.
   * Note that if {@code component} is of type {@link JFrame}, it is returned.
   * @param component the component
   * @return the parent JFrame of the given component, null if none exists
   */
  public static JFrame getParentFrame(final Component component) {
    if (component instanceof JFrame) {
      return (JFrame) component;
    }

    return Utilities.getParentOfType(component, JFrame.class);
  }

  /**
   * Finds the first component of type {@link JDialog} in the parent hierarchy of {@code component}.
   * Note that if {@code component} is of type {@link JDialog}, it is returned.
   * @param component the component
   * @return the parent JDialog of the given component, null if none exists
   */
  public static JDialog getParentDialog(final Component component) {
    if (component instanceof JDialog) {
      return (JDialog) component;
    }

    return Utilities.getParentOfType(component, JDialog.class);
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
   * @param component the component to display in the frame
   * @return a frame builder
   */
  public static FrameBuilder frameBuilder(final JComponent component) {
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
     * @param preferredSize the preferred size
     * @return this builder instance
     */
    FrameBuilder preferredSize(Dimension preferredSize);

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
     * @param onClosed called when the frame has been closed
     * @return this builder instance
     */
    FrameBuilder onClosed(Runnable onClosed);

    /**
     * @return a JFrame based on this builder
     */
    JFrame build();

    /**
     * Builds and shows a JFrame based on this builder
     */
    void show();
  }

  private static final class DefaultFrameBuilder implements FrameBuilder {

    private final JComponent component;

    private ImageIcon icon;
    private String title;
    private Runnable onClosed;
    private Dimension preferredSize;
    private boolean resizable = true;
    private JComponent relativeTo;

    private DefaultFrameBuilder(final JComponent component) {
      this.component = requireNonNull(component);
    }

    @Override
    public FrameBuilder title(final String title) {
      this.title = title;
      return this;
    }

    @Override
    public FrameBuilder icon(final ImageIcon icon) {
      this.icon = icon;
      return this;
    }

    @Override
    public FrameBuilder preferredSize(final Dimension preferredSize) {
      this.preferredSize = preferredSize;
      return this;
    }

    @Override
    public FrameBuilder resizable(final boolean resizable) {
      this.resizable = resizable;
      return this;
    }

    @Override
    public FrameBuilder relativeTo(final JComponent relativeTo) {
      this.relativeTo = relativeTo;
      return this;
    }

    @Override
    public FrameBuilder onClosed(final Runnable onClosed) {
      this.onClosed = onClosed;
      return this;
    }

    @Override
    public JFrame build() {
      final JFrame frame = new JFrame();
      frame.setLayout(Layouts.borderLayout());
      frame.add(component, BorderLayout.CENTER);
      if (title != null) {
        frame.setTitle(title);
      }
      if (icon != null) {
        frame.setIconImage(icon.getImage());
      }
      if (preferredSize != null) {
        frame.setPreferredSize(preferredSize);
      }
      else {
        frame.pack();
      }
      frame.setResizable(resizable);
      frame.setLocationRelativeTo(relativeTo);
      if (onClosed != null) {
        frame.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosed(final WindowEvent e) {
            onClosed.run();
          }
        });
      }

      return frame;
    }

    @Override
    public void show() {
      build().setVisible(true);
    }
  }
}
