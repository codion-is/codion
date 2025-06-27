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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.frame;

import is.codion.common.observable.Observable;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.WindowConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.function.Consumer;

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
	 * @param title an observable for a dynamic dialog title
	 * @return this builder instance
	 */
	FrameBuilder title(Observable<String> title);

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
	 * @param onBuild called when the frame has been built.
	 * @return this builder instance
	 */
	FrameBuilder onBuild(Consumer<JFrame> onBuild);

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
