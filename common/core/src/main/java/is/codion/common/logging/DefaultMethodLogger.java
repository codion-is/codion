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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.logging;

import is.codion.common.Text;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * TODO this class should be able to handle/recover from incorrect usage, not crash the application
 */
final class DefaultMethodLogger implements MethodLogger {

	private final Deque<DefaultEntry> callStack = new LinkedList<>();
	private final LinkedList<Entry> entries = new LinkedList<>();
	private final ArgumentToString argumentToString;
	private final int maxSize;

	private boolean enabled = false;

	DefaultMethodLogger(int maxSize, ArgumentToString argumentToString) {
		this.maxSize = maxSize;
		this.argumentToString = requireNonNull(argumentToString, "argumentStringProvider");
	}

	@Override
	public synchronized void enter(String method) {
		if (enabled) {
			callStack.push(new DefaultEntry(method, null));
		}
	}

	@Override
	public synchronized void enter(String method, Object argument) {
		if (enabled) {
			callStack.push(new DefaultEntry(method, argumentToString.argumentToString(method, argument)));
		}
	}

	@Override
	public Entry exit(String method) {
		return exit(method, null);
	}

	@Override
	public Entry exit(String method, Throwable exception) {
		return exit(method, exception, null);
	}

	@Override
	public synchronized Entry exit(String method, Throwable exception, String exitMessage) {
		if (!enabled) {
			return null;
		}
		if (callStack.isEmpty()) {
			throw new IllegalStateException("Call stack is empty when trying to log method exit: " + method);
		}
		DefaultEntry entry = callStack.pop();
		if (!entry.method().equals(method)) {//todo pop until found or empty?
			throw new IllegalStateException("Expecting method " + entry.method() + " but got " + method + " when trying to log method exit");
		}
		entry.setExitTime();
		entry.setException(exception);
		entry.setExitMessage(exitMessage);
		if (callStack.isEmpty()) {
			if (entries.size() == maxSize) {
				entries.removeFirst();
			}
			entries.addLast(entry);
		}
		else {
			callStack.peek().addChildEntry(entry);
		}

		return entry;
	}

	@Override
	public synchronized boolean isEnabled() {
		return enabled;
	}

	@Override
	public synchronized void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			if (!enabled) {
				entries.clear();
				callStack.clear();
			}
		}
	}

	@Override
	public synchronized List<Entry> entries() {
		return Collections.unmodifiableList(entries);
	}

	private static final class DefaultEntry implements Entry, Serializable {

		private static final long serialVersionUID = 1;

		private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		private static final NumberFormat MICROSECONDS_FORMAT = NumberFormat.getIntegerInstance();
		private static final String NEWLINE = "\n";
		private static final int INDENTATION_CHARACTERS = 11;

		private final LinkedList<Entry> childEntries = new LinkedList<>();
		private final String method;
		private final String enterMessage;
		private final long enterTime;
		private final long enterTimeNano;
		private String exitMessage;
		private long exitTime;
		private long exitTimeNano;
		private String stackTrace;

		/**
		 * Creates a new Entry, using the current time
		 * @param method the method being logged
		 * @param enterMessage the message associated with entering the method
		 */
		private DefaultEntry(String method, String enterMessage) {
			this(method, enterMessage, System.currentTimeMillis(), System.nanoTime());
		}

		/**
		 * Creates a new Entry
		 * @param method the method being logged
		 * @param enterMessage the message associated with entering the method
		 * @param enterTime the time to associate with entering the method
		 * @param enterTimeNano the nano time to associate with entering the method
		 */
		private DefaultEntry(String method, String enterMessage, long enterTime, long enterTimeNano) {
			this.method = method;
			this.enterTime = enterTime;
			this.enterTimeNano = enterTimeNano;
			this.enterMessage = enterMessage;
		}

		@Override
		public boolean hasChildEntries() {
			return !childEntries.isEmpty();
		}

		@Override
		public List<Entry> childEntries() {
			return Collections.unmodifiableList(childEntries);
		}

		@Override
		public String method() {
			return method;
		}

		@Override
		public String enterMessage() {
			return enterMessage;
		}

		@Override
		public long duration() {
			return exitTimeNano - enterTimeNano;
		}

		@Override
		public void appendTo(StringBuilder builder) {
			requireNonNull(builder).append(this).append(NEWLINE);
			appendLogEntries(builder, childEntries(), 1);
		}

		@Override
		public String toString() {
			return toString(0);
		}

		@Override
		public String toString(int indentation) {
			LocalDateTime enterDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(enterTime), TimeZone.getDefault().toZoneId());
			String indentString = indentation > 0 ? Text.rightPad("", indentation * INDENTATION_CHARACTERS, ' ') : "";
			StringBuilder stringBuilder = new StringBuilder(indentString).append(TIMESTAMP_FORMATTER.format(enterDateTime)).append(" @ ");
			int timestampLength = stringBuilder.length();
			stringBuilder.append(method);
			String padString = Text.rightPad("", timestampLength, ' ');
			if (!nullOrEmpty(enterMessage)) {
				if (multiLine(enterMessage)) {
					stringBuilder.append(NEWLINE).append(padString).append(enterMessage.replace(NEWLINE, NEWLINE + padString));
				}
				else {
					stringBuilder.append(": ").append(enterMessage);
				}
			}
			if (exitTime != 0) {
				LocalDateTime exitDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(exitTime), TimeZone.getDefault().toZoneId());
				stringBuilder.append(NEWLINE).append(indentString).append(TIMESTAMP_FORMATTER.format(exitDateTime)).append(" > ")
								.append(MICROSECONDS_FORMAT.format(TimeUnit.NANOSECONDS.toMicros(duration()))).append(" μs")
								.append(exitMessage == null ? "" : " (" + exitMessage + ")");
				if (stackTrace != null) {
					stringBuilder.append(NEWLINE).append(padString).append(stackTrace.replace(NEWLINE, NEWLINE + padString));
				}
			}

			return stringBuilder.toString();
		}

		/**
		 * Adds a child entry to this log entry
		 * @param childEntry the child entry to add
		 */
		private void addChildEntry(Entry childEntry) {
			childEntries.addLast(childEntry);
		}

		/**
		 * Sets the exit time as the current time
		 */
		private void setExitTime() {
			exitTime = System.currentTimeMillis();
			exitTimeNano = System.nanoTime();
		}

		/**
		 * @param exception the exception that occurred during the method call logged by this entry
		 */
		private void setException(Throwable exception) {
			if (exception != null) {
				stackTrace = stackTrace(exception);
			}
		}

		/**
		 * @param exitMessage the exit message
		 */
		private void setExitMessage(String exitMessage) {
			this.exitMessage = exitMessage;
		}

		/**
		 * Appends the given log entries to the log
		 * @param log the log
		 * @param entries the List containing the entries to append
		 * @param indentationLevel the indentation to use for the given log entries
		 */
		private static void appendLogEntries(StringBuilder log, List<Entry> entries, int indentationLevel) {
			for (Entry entry : entries) {
				log.append(entry.toString(indentationLevel)).append(NEWLINE);
				appendLogEntries(log, entry.childEntries(), indentationLevel + 1);
			}
		}

		private static String stackTrace(Throwable exception) {
			StringWriter writer = new StringWriter();
			exception.printStackTrace(new PrintWriter(writer));

			return writer.toString();
		}

		private static boolean multiLine(String enterMessage) {
			return enterMessage.contains(NEWLINE);
		}
	}
}
