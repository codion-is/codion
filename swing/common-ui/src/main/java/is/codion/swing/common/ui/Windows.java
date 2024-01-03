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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
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
   * @throws IllegalArgumentException in case ratio is not between 0 and 1
   */
  public static Dimension screenSizeRatio(double ratio) {
    if (ratio < 0 || ratio > 1.0) {
      throw new IllegalArgumentException("Ratio must be between 0 and 1");
    }
    Dimension screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();

    return new Dimension((int) (screen.getWidth() * ratio), (int) (screen.getHeight() * ratio));
  }

  /**
   * Resizes the given window so that if fits within the current screen bounds,
   * if the window already fits then calling this method has no effect
   * @param window the window to resize
   */
  public static void setSizeWithinScreenBounds(Window window) {
    Dimension screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
            .getDefaultConfiguration().getBounds().getSize();
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
   * @throws IllegalArgumentException in case ratio is not between 0 and 1
   */
  public static void resizeWindow(Window window, double screenSizeRatio) {
    resizeWindow(window, screenSizeRatio, null, null);
  }

  /**
   * Resizes the given window so that it is {@code screenSizeRatio} percent of the current screen size,
   * within the given minimum and maximum sizes
   * @param window the window to resize
   * @param screenSizeRatio the screen size ratio
   * @param minimumSize the minimum size, may be null
   * @param maximumSize the maximum size, may be null
   * @throws IllegalArgumentException in case ratio is not between 0 and 1
   */
  public static void resizeWindow(Window window, double screenSizeRatio, Dimension minimumSize, Dimension maximumSize) {
    Dimension ratioSize = screenSizeRatio(screenSizeRatio);
    if (minimumSize != null) {
      ratioSize.setSize(Math.max(minimumSize.width, ratioSize.width), Math.max(minimumSize.height, ratioSize.height));
    }
    if (maximumSize != null) {
      ratioSize.setSize(Math.min(maximumSize.width, ratioSize.width), Math.min(maximumSize.height, ratioSize.height));
    }

    window.setSize(ratioSize);
  }

  /**
   * @param component the component to display in the frame
   * @return a frame builder
   */
  public static FrameBuilder frame(JComponent component) {
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
     * @param titleProvider a value observer for a dynamic dialog title
     * @return this builder instance
     */
    FrameBuilder titleProvider(ValueObserver<String> titleProvider);

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
     * Overrides {@link #locationRelativeTo(Component)} and {@link #centerFrame(boolean)}.
     * @param location the frame location
     * @return this builder instance
     */
    FrameBuilder location(Point location);

    /**
     * @param locationRelativeTo the component to which the location should be relative
     * @return this builder instance
     */
    FrameBuilder locationRelativeTo(Component locationRelativeTo);

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
     * This is overridden by {@link #location(Point)} or by setting the {@link #locationRelativeTo(Component)} component.
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
    private ValueObserver<String> titleProvider;
    private Consumer<WindowEvent> onClosing;
    private Consumer<WindowEvent> onClosed;
    private Consumer<WindowEvent> onOpened;
    private Dimension size;
    private boolean resizable = true;
    private Point location;
    private Component locationRelativeTo;
    private int defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE;
    private JMenuBar menuBar;
    private int extendedState = Frame.NORMAL;
    private boolean centerFrame;

    private DefaultFrameBuilder(JComponent component) {
      this.component = requireNonNull(component);
    }

    @Override
    public FrameBuilder title(String title) {
      return titleProvider(Value.value(title));
    }

    @Override
    public FrameBuilder titleProvider(ValueObserver<String> titleProvider) {
      this.titleProvider = requireNonNull(titleProvider);
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
    public FrameBuilder location(Point location) {
      this.location = location;
      return this;
    }

    @Override
    public FrameBuilder locationRelativeTo(Component locationRelativeTo) {
      this.locationRelativeTo = locationRelativeTo;
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
      if (titleProvider != null) {
        frame.setTitle(titleProvider.get());
        titleProvider.addDataListener(frame::setTitle);
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
      if (location != null) {
        frame.setLocation(location);
      }
      else if (locationRelativeTo != null) {
        frame.setLocationRelativeTo(locationRelativeTo);
      }
      else if (centerFrame) {
        frame.setLocationRelativeTo(null);
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
