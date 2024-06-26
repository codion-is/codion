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
import is.codion.swing.common.ui.component.button.CheckBoxMenuItemBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Colors.darker;
import static is.codion.swing.common.ui.control.Control.commandControl;
import static java.awt.event.KeyEvent.*;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

/**
 * Highlights search results in a JTextComponent.<br>
 * Instantiate via the {@link SearchHighlighter#searchHighlighter(JTextComponent)} factory method.
 */
public final class SearchHighlighter {

	private static final MessageBundle MESSAGES =
					messageBundle(SearchHighlighter.class, getBundle(SearchHighlighter.class.getName()));

	private final JTextComponent textComponent;
	private final Value<String> searchStringValue = Value.nonNull("")
					.listener(this::searchAndHighlightResults)
					.build();
	private final State caseSensitiveState = State.builder()
					.listener(this::searchAndHighlightResults)
					.build();
	private final Highlighter highlighter = new DefaultHighlighter();
	private final List<MatchPosition> searchTextPositions = new ArrayList<>();
	private final Value<Integer> currentSearchTextPositionIndex = Value.value();
	private final Value<Integer> selectedSearchTextPosition = Value.value();

	private HighlightPainter highlightPainter;
	private HighlightPainter highlightSelectedPainter;

	private SearchHighlighter(JTextComponent textComponent) {
		this.textComponent = requireNonNull(textComponent);
		highlightPainter = new DefaultHighlightPainter(darker(textComponent.getSelectionColor()));
		highlightSelectedPainter = new DefaultHighlightPainter(textComponent.getSelectionColor());
		textComponent.setHighlighter(highlighter);
		bindEvents(textComponent);
	}

	/**
	 * @return the search string value
	 */
	public Value<String> searchString() {
		return searchStringValue;
	}

	/**
	 * @return the state controlling whether the search is case-sensitive.
	 */
	public State caseSensitive() {
		return caseSensitiveState;
	}

	/**
	 * @param color the color to use when highlighting search results.
	 */
	public void highlightColor(Color color) {
		highlightPainter = new DefaultHighlightPainter(requireNonNull(color));
		searchAndHighlightResults();
	}

	/**
	 * @param color the color to use when highlighting the selected search result.
	 */
	public void highlightSelectedColor(Color color) {
		highlightSelectedPainter = new DefaultHighlightPainter(requireNonNull(color));
		searchAndHighlightResults();
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
						.popupMenu(textField -> createPopupMenu(Control.builder()
										.toggle(caseSensitiveState)
										.name(MESSAGES.getString("case_sensitive"))
										.build()))
						.hint(Messages.find() + "...")
						.build();
	}

	/**
	 * Instantiates a new search highlighter for the given text component.
	 * @param textComponent the text component to search
	 * @return a new {@link SearchHighlighter} for the given component
	 */
	public static SearchHighlighter searchHighlighter(JTextComponent textComponent) {
		return new SearchHighlighter(textComponent);
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
			matchPosition.highlightTag = highlighter.addHighlight(matchPosition.start, matchPosition.end, highlightSelectedPainter);
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

	private void bindEvents(JTextComponent textComponent) {
		textComponent.getDocument().addDocumentListener((DocumentAdapter) e -> searchAndHighlightResults());
		selectedSearchTextPosition.addConsumer(selectedSearchPosition -> {
			if (selectedSearchPosition != null) {
				try {
					Rectangle rect = textComponent.modelToView(selectedSearchPosition);
					if (rect != null) {
						textComponent.scrollRectToVisible(rect);
					}
				}
				catch (BadLocationException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	private static JPopupMenu createPopupMenu(ToggleControl caseSensitiveControl) {
		JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.add(CheckBoxMenuItemBuilder.builder()
						.toggleControl(caseSensitiveControl)
						.build());

		return popupMenu;
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
