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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.i18n.Messages;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Colors.darker;
import static is.codion.swing.common.ui.Utilities.parentOfType;
import static is.codion.swing.common.ui.component.Components.menu;
import static is.codion.swing.common.ui.control.Control.commandControl;
import static java.awt.event.KeyEvent.*;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

/**
 * Highlights search results in a JTextComponent.<br>
 * Instantiate via {@link SearchHighlighter#builder(JTextComponent)}.
 */
public final class SearchHighlighter {

	private static final MessageBundle MESSAGES =
					messageBundle(SearchHighlighter.class, getBundle(SearchHighlighter.class.getName()));

	private static final String UI_PROPERTY_NAME = "UI";

	private final JTextComponent textComponent;
	private final Value<String> searchStringValue = Value.builder()
					.nonNull("")
					.listener(this::searchAndHighlightResults)
					.build();
	private final State caseSensitiveState;
	private final Highlighter highlighter = new DefaultHighlighter();
	private final List<MatchPosition> searchTextPositions = new ArrayList<>();
	private final Value<Integer> currentSearchTextPositionIndex = Value.value();
	private final Value<Integer> selectedSearchTextPosition = Value.value();
	private final ScrollToRatio scrollToRatio;
	private final boolean customHighlightColor;
	private final boolean customSelectedHighlightColor;

	private HighlightPainter highlightPainter;
	private SelectedHighlightPainter selectedHightlightPainter;

	private SearchHighlighter(DefaultBuilder builder) {
		this.textComponent = builder.textComponent;
		this.scrollToRatio = new ScrollToRatio(builder.scrollYRatio, builder.scrollXRatio);
		this.caseSensitiveState = State.builder(builder.caseSensitive)
						.listener(this::searchAndHighlightResults)
						.build();
		customHighlightColor = builder.customHighlightColor;
		customSelectedHighlightColor = builder.customSelectedHighlightColor;
		highlightPainter = new DefaultHighlightPainter(builder.selectedHighlightColor);
		selectedHightlightPainter = new SelectedHighlightPainter(builder.highlightColor);
		textComponent.setHighlighter(highlighter);
		bindEvents();
	}

	/**
	 * @return the search string value
	 */
	public Value<String> searchString() {
		return searchStringValue;
	}

	/**
	 * @return the {@link State} controlling whether the search is case-sensitive.
	 */
	public State caseSensitive() {
		return caseSensitiveState;
	}

	/**
	 * @param color the color to use when highlighting search results.
	 */
	public void highlightColor(Color color) {
		highlightPainter = new DefaultHighlightPainter(requireNonNull(color));
		updateHighligths();
	}

	/**
	 * @param color the color to use when highlighting the selected search result.
	 */
	public void selectedHighlightColor(Color color) {
		selectedHightlightPainter = new SelectedHighlightPainter(requireNonNull(color));
		updateHighligths();
	}

	/**
	 * @return a text field for entering the search text.
	 */
	public JTextField createSearchField() {
		return new DefaultTextFieldBuilder<>(String.class, searchStringValue)
						.selectAllOnFocusGained(true)
						.keyEvent(KeyEvents.builder(VK_DOWN)
										.action(commandControl(this::nextSearchPosition)))
						.keyEvent(KeyEvents.builder(VK_UP)
										.action(commandControl(this::previousSearchPosition)))
						.keyEvent(KeyEvents.builder(VK_ESCAPE)
										.action(commandControl(textComponent::requestFocusInWindow)))
						.popupMenu(textField -> menu()
										.control(Control.builder()
														.toggle(caseSensitiveState)
														.name(MESSAGES.getString("case_sensitive"))
														.build())
										.buildPopupMenu())
						.hint(Messages.find() + "...")
						.build();
	}

	/**
	 * @param textComponent the text component
	 * @return a new {@link Builder} instance
	 */
	public static Builder builder(JTextComponent textComponent) {
		return new DefaultBuilder(requireNonNull(textComponent));
	}

	/**
	 * Moves to the next search position, if available, with wrap-around.
	 */
	public void nextSearchPosition() {
		if (!searchTextPositions.isEmpty()) {
			deselectCurrentSearchPosition();
			if (currentSearchTextPositionIndex.isNull() || currentSearchTextPositionIndex.isEqualTo(searchTextPositions.size() - 1)) {
				currentSearchTextPositionIndex.set(0);
			}
			else {
				currentSearchTextPositionIndex.set(currentSearchTextPositionIndex.get() + 1);
			}
			selectCurrentSearchPosition();
		}
	}

	/**
	 * Moves to the previous search position, if available, with wrap-around.
	 */
	public void previousSearchPosition() {
		if (!searchTextPositions.isEmpty()) {
			deselectCurrentSearchPosition();
			if (currentSearchTextPositionIndex.isNull() || currentSearchTextPositionIndex.isEqualTo(0)) {
				currentSearchTextPositionIndex.set(searchTextPositions.size() - 1);
			}
			else {
				currentSearchTextPositionIndex.set(currentSearchTextPositionIndex.get() - 1);
			}
			selectCurrentSearchPosition();
		}
	}

	/**
	 * @return the index of the selected search position within the document, null if none is selected.
	 */
	Integer selectedHighlightPosition() {
		return selectedSearchTextPosition.get();
	}

	private void searchAndHighlightResults() {
		currentSearchTextPositionIndex.clear();
		selectedSearchTextPosition.clear();
		highlighter.removeAllHighlights();
		searchTextPositions.clear();
		if (!searchStringValue.get().isEmpty()) {
			Pattern pattern = Pattern.compile(searchStringValue.get(), caseSensitiveState.get() ? 0 : Pattern.CASE_INSENSITIVE);
			try {
				Matcher matcher = pattern.matcher(textComponent.getDocument().getText(0, textComponent.getDocument().getLength()));
				int searchFrom = 0;
				while (matcher.find(searchFrom)) {
					Object highlightTag = highlighter.addHighlight(matcher.start(), matcher.end(), highlightPainter);
					searchTextPositions.add(new MatchPosition(matcher.start(), matcher.end(), highlightTag));
					searchFrom = matcher.end();
				}
				nextSearchPosition();
			}
			catch (BadLocationException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void selectCurrentSearchPosition() {
		MatchPosition matchPosition = searchTextPositions.get(currentSearchTextPositionIndex.get());
		selectedSearchTextPosition.set(matchPosition.start);
		try {
			highlighter.removeHighlight(matchPosition.highlightTag);
			matchPosition.highlightTag = highlighter.addHighlight(matchPosition.start, matchPosition.end, selectedHightlightPainter);
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	private void deselectCurrentSearchPosition() {
		if (currentSearchTextPositionIndex.isNotNull()) {
			MatchPosition matchPosition = searchTextPositions.get(currentSearchTextPositionIndex.get());
			try {
				highlighter.removeHighlight(matchPosition.highlightTag);
				matchPosition.highlightTag = highlighter.addHighlight(matchPosition.start, matchPosition.end, highlightPainter);
			}
			catch (BadLocationException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void bindEvents() {
		textComponent.getDocument().addDocumentListener((DocumentAdapter) e -> searchAndHighlightResults());
		textComponent.addPropertyChangeListener(UI_PROPERTY_NAME, new UpdateHightlightColors());
		selectedSearchTextPosition.addConsumer(selectedSearchPosition -> {
			if (selectedSearchPosition != null) {
				scrollToPosition(selectedSearchPosition);
			}
		});
	}

	private void scrollToPosition(int position) {
		JViewport viewport = parentOfType(JViewport.class, textComponent);
		try {
			Rectangle view = textComponent.modelToView(position);
			if (viewport != null && view != null) {
				viewport.setViewPosition(viewPosition(viewport, view));
			}
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	private Point viewPosition(JViewport viewport, Rectangle view) {
		return new Point(locationX(viewport, view), locationY(viewport, view));
	}

	private int locationY(JViewport viewport, Rectangle view) {
		int extentHeight = viewport.getExtentSize().height;
		int viewHeight = viewport.getViewSize().height;
		int yPosition = (int) Math.max(0, view.y - ((extentHeight - view.height) * scrollToRatio.scrollYRatio));
		yPosition = Math.min(yPosition, viewHeight - extentHeight);

		return yPosition;
	}

	private int locationX(JViewport viewport, Rectangle view) {
		int extentWidth = viewport.getExtentSize().width;
		int viewWidth = viewport.getViewSize().width;
		int yPosition = (int) Math.max(0, view.x - ((extentWidth - view.width) * scrollToRatio.scrollXRatio));
		yPosition = Math.min(yPosition, viewWidth - extentWidth);

		return yPosition;
	}

	private void updateHighligths() {
		Highlight[] highlights = highlighter.getHighlights();
		highlighter.removeAllHighlights();
		for (Highlight highlight : highlights) {
			updateHighlight(highlight, matchPosition(highlight.getStartOffset()));
		}
	}

	private void updateHighlight(Highlight highlight, MatchPosition matchPosition) {
		try {
			matchPosition.highlightTag = highlighter.addHighlight(highlight.getStartOffset(), highlight.getEndOffset(),
							highlight.getPainter() instanceof SelectedHighlightPainter ? selectedHightlightPainter : highlightPainter);
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	private MatchPosition matchPosition(int startOffset) {
		return searchTextPositions.stream()
						.filter(matchPosition -> matchPosition.start == startOffset)
						.findFirst()
						.orElseThrow(IllegalStateException::new);
	}

	/**
	 * Builds a {@link SearchHighlighter}.
	 */
	public interface Builder {

		/**
		 * @param highlightColor the highlight color
		 * @return this builder
		 */
		Builder highlightColor(Color highlightColor);

		/**
		 * @param selectedHighlightColor the selected highlight color
		 * @return this builder
		 */
		Builder selectedHighlightColor(Color selectedHighlightColor);

		/**
		 * @param caseSensitive true if the search should be case sensitive, default tue
		 * @return this builder
		 */
		Builder caseSensitive(boolean caseSensitive);

		/**
		 * @param scrollYRatio specifies the Y axis scroll ratio when scrolling to a search result, 0 being at the top and 1 at the bottom, default 0.5
		 * @return this builder
		 * @throws IllegalArgumentException in case the value is not between 0 and 1
		 */
		Builder scrollYRatio(double scrollYRatio);

		/**
		 * @param scrollXRatio specifies the X axis scroll ratio when scrolling to a search result,
		 * 0 being all the way to the left and 1 all the way to the right, default 0.5
		 * @return this builder
		 * @throws IllegalArgumentException in case the value is not between 0 and 1
		 */
		Builder scrollXRatio(double scrollXRatio);

		/**
		 * @return a new {@link SearchHighlighter}
		 */
		SearchHighlighter build();
	}

	private static final class DefaultBuilder implements Builder {

		private final JTextComponent textComponent;

		private Color highlightColor;
		private Color selectedHighlightColor;
		private boolean customHighlightColor = false;
		private boolean customSelectedHighlightColor = false;
		private boolean caseSensitive = true;
		private double scrollYRatio = 0.5;
		private double scrollXRatio = 0.5;

		private DefaultBuilder(JTextComponent textComponent) {
			this.textComponent = textComponent;
			this.highlightColor = textComponent.getSelectionColor();
			this.selectedHighlightColor = darker(textComponent.getSelectionColor());
		}

		@Override
		public Builder highlightColor(Color highlightColor) {
			this.highlightColor = requireNonNull(highlightColor);
			this.customHighlightColor = true;
			return this;
		}

		@Override
		public Builder selectedHighlightColor(Color selectedHighlightColor) {
			this.selectedHighlightColor = requireNonNull(selectedHighlightColor);
			this.customSelectedHighlightColor = true;
			return this;
		}

		@Override
		public Builder caseSensitive(boolean caseSensitive) {
			this.caseSensitive = caseSensitive;
			return this;
		}

		@Override
		public Builder scrollYRatio(double scrollYRatio) {
			if (scrollYRatio < 0 || scrollYRatio > 1) {
				throw new IllegalArgumentException("scrollYRatio must be between 0 and 1");
			}
			this.scrollYRatio = scrollYRatio;
			return this;
		}

		@Override
		public Builder scrollXRatio(double scrollXRatio) {
			if (scrollXRatio < 0 || scrollXRatio > 1) {
				throw new IllegalArgumentException("scrollXRatio must be between 0 and 1");
			}
			this.scrollXRatio = scrollXRatio;
			return this;
		}

		@Override
		public SearchHighlighter build() {
			return new SearchHighlighter(this);
		}
	}

	private final class UpdateHightlightColors implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
			if (!customHighlightColor) {
				highlightColor(textComponent.getSelectionColor());
			}
			if (!customSelectedHighlightColor) {
				selectedHighlightColor(darker(textComponent.getSelectionColor()));
			}
		}
	}

	private static final class SelectedHighlightPainter extends DefaultHighlightPainter {

		private SelectedHighlightPainter(Color color) {
			super(color);
		}
	}

	private static final class ScrollToRatio {

		private final double scrollYRatio;
		private final double scrollXRatio;

		private ScrollToRatio(double scrollYRatio, double scrollXRatio) {
			this.scrollYRatio = scrollYRatio;
			this.scrollXRatio = scrollXRatio;
		}
	}

	private static final class MatchPosition {

		private final int start;
		private final int end;

		private Object highlightTag;

		private MatchPosition(int start, int end, Object highlightTag) {
			this.start = start;
			this.end = end;
			this.highlightTag = highlightTag;
		}
	}
}
