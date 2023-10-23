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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for showing a wait cursor.
 */
public final class WaitCursor {

  private static final Map<Window, Integer> WAIT_CURSOR_REQUESTS = new HashMap<>();
  private static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
  private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

  private WaitCursor() {}

  /**
   * Adds a wait cursor request for the parent root pane of the given component,
   * the wait cursor is activated once a request is made, but only deactivated once all such
   * requests have been retracted. Best used with a try/finally block.
   * <pre>
   * WaitCursor.show(component);
   * try {
   *   doSomething();
   * }
   * finally {
   *   WaitCursor.hide(component);
   * }
   * </pre>
   * @param component the component
   * @see #hide(JComponent)
   */
  public static void show(JComponent component) {
    show(Utilities.parentWindow(component));
  }

  /**
   * Removes a wait cursor request for the parent root pane of the given component,
   * the wait cursor is activated once a request is made, but only deactivated once all such
   * requests have been retracted. Best used with a try/finally block.
   * <pre>
   * WaitCursor.show(component);
   * try {
   *   doSomething();
   * }
   * finally {
   *   WaitCursor.hide(component);
   * }
   * </pre>
   * @param component the component
   * @see #show(JComponent)
   */
  public static void hide(JComponent component) {
    hide(Utilities.parentWindow(component));
  }

  /**
   * Adds a wait cursor request for the given window
   * @param window the window
   */
  public static void show(Window window) {
    setWaitCursor(true, window);
  }

  /**
   * Removes a wait cursor request for the given window
   * @param window the window
   */
  public static void hide(Window window) {
    setWaitCursor(false, window);
  }

  /**
   * Adds a wait cursor request for the given window
   * @param window the window
   */
  private static void setWaitCursor(boolean on, Window window) {
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
        setCursor(on, window);
      }
      if (requests == 0) {
        WAIT_CURSOR_REQUESTS.remove(window);
      }
      else {
        WAIT_CURSOR_REQUESTS.put(window, requests);
      }
    }
  }

  private static void setCursor(boolean on, Window window) {
    if (SwingUtilities.isEventDispatchThread()) {
      window.setCursor(on ? WAIT_CURSOR : DEFAULT_CURSOR);
    }
    else {
      try {
        SwingUtilities.invokeAndWait(() -> window.setCursor(on ? WAIT_CURSOR : DEFAULT_CURSOR));
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
