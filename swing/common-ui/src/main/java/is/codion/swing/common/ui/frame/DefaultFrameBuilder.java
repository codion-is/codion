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
import is.codion.common.value.Value;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.window.Windows;

import org.jspecify.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultFrameBuilder implements FrameBuilder {

	private final List<WindowListener> windowListeners = new ArrayList<>(0);

	private @Nullable JComponent component;
	private @Nullable ImageIcon icon;
	private @Nullable Observable<String> title;
	private @Nullable Consumer<WindowEvent> onClosing;
	private @Nullable Consumer<WindowEvent> onClosed;
	private @Nullable Consumer<WindowEvent> onOpened;
	private @Nullable Consumer<JFrame> onBuild;
	private @Nullable Dimension size;
	private boolean resizable = true;
	private @Nullable Point location;
	private @Nullable Component locationRelativeTo;
	private int defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE;
	private @Nullable JMenuBar menuBar;
	private int extendedState = Frame.NORMAL;
	private boolean centerFrame;

	@Override
	public FrameBuilder component(@Nullable JComponent component) {
		this.component = component;
		return this;
	}

	@Override
	public FrameBuilder title(@Nullable String title) {
		return title(Value.nullable(title));
	}

	@Override
	public FrameBuilder title(@Nullable Observable<String> title) {
		this.title = title;
		return this;
	}

	@Override
	public FrameBuilder icon(@Nullable ImageIcon icon) {
		this.icon = icon;
		return this;
	}

	@Override
	public FrameBuilder size(@Nullable Dimension size) {
		this.size = size;
		return this;
	}

	@Override
	public FrameBuilder resizable(boolean resizable) {
		this.resizable = resizable;
		return this;
	}

	@Override
	public FrameBuilder location(@Nullable Point location) {
		this.location = location;
		return this;
	}

	@Override
	public FrameBuilder locationRelativeTo(@Nullable Component locationRelativeTo) {
		this.locationRelativeTo = locationRelativeTo;
		return this;
	}

	@Override
	public FrameBuilder defaultCloseOperation(int defaultCloseOperation) {
		this.defaultCloseOperation = defaultCloseOperation;
		return this;
	}

	@Override
	public FrameBuilder onOpened(@Nullable Consumer<WindowEvent> onOpened) {
		this.onOpened = onOpened;
		return this;
	}

	@Override
	public FrameBuilder onClosed(@Nullable Consumer<WindowEvent> onClosed) {
		this.onClosed = onClosed;
		return this;
	}

	@Override
	public FrameBuilder onClosing(@Nullable Consumer<WindowEvent> onClosing) {
		this.onClosing = onClosing;
		return this;
	}

	@Override
	public FrameBuilder menuBar(@Nullable JMenuBar menuBar) {
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
	public FrameBuilder onBuild(@Nullable Consumer<JFrame> onBuild) {
		this.onBuild = onBuild;
		return this;
	}

	@Override
	public JFrame build() {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(defaultCloseOperation);
		if (component != null) {
			frame.setLayout(Layouts.borderLayout());
			frame.add(component, BorderLayout.CENTER);
		}
		if (title != null) {
			frame.setTitle(title.get());
			title.addConsumer(frame::setTitle);
		}
		if (icon != null) {
			frame.setIconImage(icon.getImage());
		}
		if (size != null) {
			frame.setSize(size);
		}
		else {
			frame.pack();
			Windows.sizeWithinScreenBounds(frame);
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
		if (onBuild != null) {
			onBuild.accept(frame);
		}

		return frame;
	}

	@Override
	public JFrame show() {
		JFrame frame = build();
		frame.setVisible(true);

		return frame;
	}

	private static final class FrameListener extends WindowAdapter {

		private final @Nullable Consumer<WindowEvent> onClosing;
		private final @Nullable Consumer<WindowEvent> onClosed;
		private final @Nullable Consumer<WindowEvent> onOpened;

		private FrameListener(@Nullable Consumer<WindowEvent> onClosing, @Nullable Consumer<WindowEvent> onClosed, @Nullable Consumer<WindowEvent> onOpened) {
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
