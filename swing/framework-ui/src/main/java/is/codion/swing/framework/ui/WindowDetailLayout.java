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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.resource.MessageBundle;
import is.codion.common.value.Value;
import is.codion.common.value.Value.Notify;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Control.Command;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel.DetailController;
import is.codion.swing.framework.ui.EntityPanel.DetailLayout;
import is.codion.swing.framework.ui.EntityPanel.PanelState;
import is.codion.swing.framework.ui.EntityPanel.WindowType;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.JComponent;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.*;
import static is.codion.swing.framework.ui.EntityPanel.WindowType.FRAME;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;

/**
 * A detail layout which displays detail panels in a window, opened via the table popup menu.
 */
public final class WindowDetailLayout implements DetailLayout {

	private static final MessageBundle MESSAGES =
					messageBundle(TabbedDetailLayout.class, getBundle(TabbedDetailLayout.class.getName()));

	private static final String DETAIL_TABLES = "detail_tables";

	private static final Value.Validator<PanelState> PANEL_STATE_VALIDATOR = new RejectEmbedded();

	private final EntityPanel entityPanel;
	private final Map<EntityPanel, DetailWindow> panelWindows = new HashMap<>();
	private final WindowType windowType;
	private final WindowDetailController detailController = new WindowDetailController();

	private WindowDetailLayout(DefaultBuilder builder) {
		this.entityPanel = builder.entityPanel;
		this.windowType = builder.windowType;
	}

	@Override
	public Optional<JComponent> layout() {
		if (entityPanel.detailPanels().isEmpty()) {
			throw new IllegalStateException("EntityPanel " + entityPanel + " has no detail panels");
		}
		if (!panelWindows.isEmpty()) {
			throw new IllegalStateException("EntityPanel " + entityPanel + " has already been laid out");
		}
		entityPanel.detailPanels().forEach(this::addDetailPanel);
		entityPanel.detailPanels().forEach(this::bindEvents);
		setupControls(entityPanel);

		return Optional.empty();
	}

	@Override
	public Optional<DetailController> controller() {
		return Optional.of(detailController);
	}

	/**
	 * @param entityPanel the entity panel
	 * @return a new {@link Builder} instance
	 */
	public static Builder builder(EntityPanel entityPanel) {
		return new DefaultBuilder(entityPanel);
	}

	private void addDetailPanel(EntityPanel detailPanel) {
		panelWindows.put(detailPanel, new DetailWindow(detailPanel));
	}

	private void bindEvents(EntityPanel detailPanel) {
		detailPanel.activateEvent().addConsumer(detailController::activated);
	}

	private void setupControls(EntityPanel entityPanel) {
		if (entityPanel.containsTablePanel()) {
			entityPanel.tablePanel().addPopupMenuControls(Controls.builder()
							.name(MESSAGES.getString(DETAIL_TABLES))
							.smallIcon(FrameworkIcons.instance().detail())
							.controls(entityPanel.detailPanels().stream()
											.map(detailPanel -> Control.builder()
															.command(windowCommand(detailPanel))
															.name(detailPanel.caption())
															.smallIcon(detailPanel.icon().orElse(null))
															.build())
											.collect(toList()))
							.build());
		}
	}

	private Command windowCommand(EntityPanel detailPanel) {
		return () -> panelWindows.get(detailPanel).panelState.set(WINDOW);
	}

	private final class WindowDetailController implements DetailController {

		@Override
		public Value<PanelState> panelState(EntityPanel detailPanel) {
			return detailWindow(detailPanel).panelState;
		}

		@Override
		public void activated(EntityPanel detailPanel) {
			Window panelWindow = detailWindow(detailPanel).window;
			if (panelWindow != null && panelWindow.isShowing()) {
				panelWindow.toFront();
			}
		}

		private DetailWindow detailWindow(EntityPanel detailPanel) {
			DetailWindow detailWindow = panelWindows.get(requireNonNull(detailPanel));
			if (detailWindow == null) {
				throw new IllegalArgumentException("Detail panel not found: " + detailPanel);
			}

			return detailWindow;
		}
	}

	/**
	 * Builds a {@link WindowDetailLayout} instance.
	 */
	public interface Builder {

		/**
		 * @param windowType specifies whether a JFrame or a JDialog should be used
		 * @return this Builder instance
		 */
		Builder windowType(WindowType windowType);

		/**
		 * @return a new {@link WindowDetailLayout} instance
		 */
		WindowDetailLayout build();
	}

	private final class DetailWindow {

		private final Value<PanelState> panelState = Value.nonNull(HIDDEN)
						.notify(Notify.WHEN_SET)
						.validator(PANEL_STATE_VALIDATOR)
						.consumer(this::updateDetailState)
						.build();
		private final EntityPanel detailPanel;

		private Window window;
		private boolean initialized = false;

		private DetailWindow(EntityPanel detailPanel) {
			this.detailPanel = detailPanel;
		}

		private void updateDetailState(PanelState panelState) {
			if (window == null) {
				window = createDetailWindow();
			}
			if (panelState == WINDOW) {
				detailPanel.initialize();
				if (!initialized) {
					window.pack();
					detailPanel.requestInitialFocus();
					initialized = true;
				}
				window.setVisible(true);
				window.toFront();
			}
			else {
				window.setVisible(false);
			}
			SwingEntityModel detailModel = detailPanel.model();
			if (entityPanel.model().containsDetailModel(detailModel)) {
				entityPanel.model().detailModelLink(detailModel).active().set(panelState == WINDOW);
			}
		}

		private Window createDetailWindow() {
			if (windowType == FRAME) {
				return Windows.frame(detailPanel)
								.locationRelativeTo(entityPanel)
								.title(detailPanel.caption())
								.icon(detailPanel.icon().orElse(null))
								.onClosing(windowEvent -> panelWindows.get(detailPanel).panelState.set(HIDDEN))
								.build();
			}

			return Dialogs.componentDialog(detailPanel)
							.owner(entityPanel)
							.locationRelativeTo(entityPanel)
							.title(detailPanel.caption())
							.icon(detailPanel.icon().orElse(null))
							.modal(false)
							.onClosed(windowEvent -> panelWindows.get(detailPanel).panelState.set(HIDDEN))
							.build();
		}
	}

	private static final class DefaultBuilder implements Builder {

		private final EntityPanel entityPanel;

		private WindowType windowType;

		private DefaultBuilder(EntityPanel entityPanel) {
			this.entityPanel = requireNonNull(entityPanel);
			this.windowType = entityPanel.windowType();
		}

		@Override
		public Builder windowType(WindowType windowType) {
			this.windowType = requireNonNull(windowType);
			return this;
		}

		@Override
		public WindowDetailLayout build() {
			return new WindowDetailLayout(this);
		}
	}

	private static final class RejectEmbedded implements Value.Validator<PanelState> {
		@Override
		public void validate(PanelState panelState) {
			if (panelState == EMBEDDED) {
				throw new IllegalArgumentException("WindowedDetailLayout does not support the EMBEDDED PanelState");
			}
		}
	}
}
