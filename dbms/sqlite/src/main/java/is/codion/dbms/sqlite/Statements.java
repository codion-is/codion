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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.dbms.sqlite;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits a script into its statements, the driver running only the first statement of several.
 */
final class Statements {

	private static final String CREATE = "CREATE";
	private static final String TRIGGER = "TRIGGER";
	private static final String TEMP = "TEMP";
	private static final String TEMPORARY = "TEMPORARY";
	private static final String BEGIN = "BEGIN";
	private static final String END = "END";
	private static final String COMMIT = "COMMIT";
	private static final String ROLLBACK = "ROLLBACK";
	private static final String TRANSACTION = "TRANSACTION";
	private static final String TO = "TO";

	private final String script;
	private final List<String> statements = new ArrayList<>();
	private final List<String> words = new ArrayList<>();
	private final StringBuilder statement = new StringBuilder();

	private int index = 0;
	private boolean content = false;
	private boolean body = false;
	private @Nullable String lastWord;

	private Statements(String script) {
		this.script = script;
	}

	/**
	 * Splits a script into its statements, at the semicolons ending them, those within quotes, comments or a trigger's
	 * body left alone, a trigger ending with {@code END} and a semicolon. A statement of comments alone is left out.
	 * A {@code CASE} expression in a trigger's body, its {@code END} followed by a semicolon, ends the trigger early.
	 * <p>
	 * A script runs in a transaction of its own, the one recording it, so a statement beginning or ending a transaction,
	 * {@code BEGIN}, {@code COMMIT}, {@code END} or {@code ROLLBACK}, is refused, a savepoint, and a rollback to one,
	 * being within the transaction.
	 * @param script the script
	 * @return the statements
	 * @throws IllegalArgumentException in case a statement begins or ends a transaction
	 */
	static List<String> split(String script) {
		return new Statements(script).split();
	}

	private List<String> split() {
		while (index < script.length()) {
			char character = script.charAt(index);
			if (character == '\'' || character == '"' || character == '`') {
				append(quoted(character), true);
			}
			else if (character == '[') {
				append(until("]", 1), true);
			}
			else if (character == '-' && next('-')) {
				append(until("\n", 2), false);
			}
			else if (character == '/' && next('*')) {
				append(until("*/", 2), false);
			}
			else if (Character.isLetter(character) || character == '_') {
				word();
			}
			else if (character == ';') {
				semicolon();
			}
			else {
				statement.append(character);
				if (!Character.isWhitespace(character)) {
					content = true;
					lastWord = null;
				}
				index++;
			}
		}
		addStatement();

		return statements;
	}

	private void word() {
		int start = index;
		while (index < script.length() && (Character.isLetterOrDigit(script.charAt(index)) ||
						script.charAt(index) == '_' || script.charAt(index) == '$')) {
			index++;
		}
		String word = script.substring(start, index);
		statement.append(word);
		content = true;
		lastWord = word.toUpperCase(Locale.ROOT);
		if (words.size() < 3) {
			words.add(lastWord);
		}
		if (trigger() && lastWord.equals(BEGIN)) {
			body = true;
		}
	}

	private void semicolon() {
		index++;
		if (body && !END.equals(lastWord)) {
			// within a trigger's body
			statement.append(';');
			lastWord = null;
		}
		else {
			addStatement();
		}
	}

	private void addStatement() {
		if (content) {
			refuseTransaction();
			statements.add(statement.toString().trim());
		}
		statement.setLength(0);
		words.clear();
		content = false;
		body = false;
		lastWord = null;
	}

	// By the statement's first word, the comments before it aside
	private void refuseTransaction() {
		if (!words.isEmpty()) {
			String first = words.get(0);
			if (first.equals(BEGIN) || first.equals(COMMIT) || first.equals(END) || first.equals(ROLLBACK) && !rollbackToSavepoint()) {
				throw new IllegalArgumentException(first + " begins or ends a transaction, which a statement of a script may not, " +
								"the script running in a transaction of its own");
			}
		}
	}

	// ROLLBACK [TRANSACTION] TO
	private boolean rollbackToSavepoint() {
		return words.size() > 1 && (words.get(1).equals(TO) ||
						words.size() > 2 && words.get(1).equals(TRANSACTION) && words.get(2).equals(TO));
	}

	private boolean trigger() {
		return words.size() > 1 && words.get(0).equals(CREATE) && (words.get(1).equals(TRIGGER) ||
						words.size() > 2 && (words.get(1).equals(TEMP) || words.get(1).equals(TEMPORARY)) && words.get(2).equals(TRIGGER));
	}

	private void append(String text, boolean isContent) {
		statement.append(text);
		index += text.length();
		if (isContent) {
			content = true;
			lastWord = null;
		}
	}

	// up to and including the closing quote, a doubled quote within escaping it
	private String quoted(char quote) {
		int end = index + 1;
		while (end < script.length()) {
			if (script.charAt(end) == quote) {
				if (end + 1 < script.length() && script.charAt(end + 1) == quote) {
					end += 2;
				}
				else {
					return script.substring(index, end + 1);
				}
			}
			else {
				end++;
			}
		}

		return script.substring(index);
	}

	// up to and including the terminator, looked for from the given offset, past what opens it
	private String until(String terminator, int offset) {
		int end = script.indexOf(terminator, index + offset);

		return end == -1 ? script.substring(index) : script.substring(index, end + terminator.length());
	}

	private boolean next(char character) {
		return index + 1 < script.length() && script.charAt(index + 1) == character;
	}
}
