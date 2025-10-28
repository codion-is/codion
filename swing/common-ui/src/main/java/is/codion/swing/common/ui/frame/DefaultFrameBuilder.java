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

import is.codion.common.observer.Observable;
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
import java.awt.Image;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultFrameBuilder implements FrameBuilder {

	private final List<WindowListener> windowListeners = new ArrayList<>(0);
	private final Collection<Consumer<WindowEvent>> onClosing = new ArrayList<>();
	private final Collection<Consumer<WindowEvent>> onClosed = new ArrayList<>();
	private final Collection<Consumer<WindowEvent>> onOpened = new ArrayList<>();
	private final Collection<Consumer<JFrame>> onBuild = new ArrayList<>();
	private final List<Image> iconImages = new ArrayList<>();

	private @Nullable JComponent component;
	private @Nullable Observable<String> title;
	private @Nullable Dimension size;
	private boolean resizable = true;
	private boolean alwaysOnTop = false;
	private boolean focusableWindowState = true;
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
		return iconImage(icon == null ? null : icon.getImage());
	}

	@Override
	public FrameBuilder iconImage(@Nullable Image iconImage) {
		iconImages.clear();
		if (iconImage != null) {
			iconImages.add(iconImage);
		}
		return this;
	}

	@Override
	public FrameBuilder iconImages(List<Image> iconImages) {
		requireNonNull(iconImages);
		this.iconImages.clear();
		this.iconImages.addAll(iconImages);
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
	public FrameBuilder alwaysOnTop(boolean alwaysOnTop) {
		this.alwaysOnTop = alwaysOnTop;
		return this;
	}

	@Override
	public FrameBuilder focusableWindowState(boolean focusableWindowState) {
		this.focusableWindowState = focusableWindowState;
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
	public FrameBuilder onOpened(Consumer<WindowEvent> onOpened) {
		this.onOpened.add(requireNonNull(onOpened));
		return this;
	}

	@Override
	public FrameBuilder onClosed(Consumer<WindowEvent> onClosed) {
		this.onClosed.add(requireNonNull(onClosed));
		return this;
	}

	@Override
	public FrameBuilder onClosing(Consumer<WindowEvent> onClosing) {
		this.onClosing.add(requireNonNull(onClosing));
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
	public FrameBuilder onBuild(Consumer<JFrame> onBuild) {
		this.onBuild.add(requireNonNull(onBuild));
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
		if (!iconImages.isEmpty()) {
			frame.setIconImages(iconImages);
		}
		if (size != null) {
			frame.setSize(size);
		}
		else {
			frame.pack();
			Windows.resizeToFitScreen(frame);
		}
		if (menuBar != null) {
			frame.setJMenuBar(menuBar);
		}
		frame.setResizable(resizable);
		frame.setAlwaysOnTop(alwaysOnTop);
		if (!focusableWindowState) {
			frame.setFocusableWindowState(false);
		}
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
		onBuild.forEach(consumer -> consumer.accept(frame));

		return frame;
	}

	@Override
	public JFrame show() {
		JFrame frame = build();
		frame.setVisible(true);

		return frame;
	}

	private static final class FrameListener extends WindowAdapter {

		private final Collection<Consumer<WindowEvent>> onClosing;
		private final Collection<Consumer<WindowEvent>> onClosed;
		private final Collection<Consumer<WindowEvent>> onOpened;

		private FrameListener(Collection<Consumer<WindowEvent>> onClosing, Collection<Consumer<WindowEvent>> onClosed, Collection<Consumer<WindowEvent>> onOpened) {
			this.onClosing = onClosing;
			this.onClosed = onClosed;
			this.onOpened = onOpened;
		}

		@Override
		public void windowOpened(WindowEvent e) {
			onOpened.forEach(consumer -> consumer.accept(e));
		}

		@Override
		public void windowClosing(WindowEvent e) {
			onClosing.forEach(consumer -> consumer.accept(e));
		}

		@Override
		public void windowClosed(WindowEvent e) {
			onClosed.forEach(consumer -> consumer.accept(e));
		}
	}
}
