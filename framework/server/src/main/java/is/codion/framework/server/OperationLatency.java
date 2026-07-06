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
package is.codion.framework.server;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A fixed-bucket latency histogram for a single server operation type, collected always-on
 * and cheap: a bucket increment and a running sum per observation, no retention.
 * <p>
 * The bucket layout ({@link #bucketBounds()} plus a final {@code +Inf} overflow bucket) and the
 * cumulative counts returned by {@link #bucketCounts()} match the shape a Prometheus histogram
 * expects, so no percentile math happens in the server.
 */
final class OperationLatency {

	/**
	 * The finite bucket upper bounds in milliseconds. Chosen once; changing them changes the exposed histogram.
	 */
	private static final long[] BOUNDS_MILLISECONDS = {1, 5, 10, 25, 50, 100, 250, 500, 1_000};

	private static final long[] BOUNDS_NANOSECONDS = boundsNanoseconds();

	private final AtomicLong[] counts = counts();
	private final AtomicLong count = new AtomicLong();
	private final AtomicLong totalNanoseconds = new AtomicLong();

	void record(long nanoseconds) {
		count.incrementAndGet();
		totalNanoseconds.addAndGet(nanoseconds);
		counts[bucket(nanoseconds)].incrementAndGet();
	}

	/**
	 * @return the total number of observations
	 */
	long count() {
		return count.get();
	}

	/**
	 * @return the sum of all observed durations, in nanoseconds
	 */
	long totalNanoseconds() {
		return totalNanoseconds.get();
	}

	/**
	 * @return cumulative observation counts, one per finite bound in {@link #bucketBounds()},
	 * followed by the total count (the {@code +Inf} bucket)
	 */
	long[] bucketCounts() {
		long[] cumulative = new long[counts.length];
		long running = 0;
		for (int i = 0; i < counts.length; i++) {
			running += counts[i].get();
			cumulative[i] = running;
		}

		return cumulative;
	}

	/**
	 * @return the finite bucket upper bounds, in milliseconds
	 */
	static long[] bucketBounds() {
		return BOUNDS_MILLISECONDS.clone();
	}

	private static int bucket(long nanoseconds) {
		for (int i = 0; i < BOUNDS_NANOSECONDS.length; i++) {
			if (nanoseconds <= BOUNDS_NANOSECONDS[i]) {
				return i;
			}
		}

		return BOUNDS_NANOSECONDS.length;
	}

	private static AtomicLong[] counts() {
		AtomicLong[] counts = new AtomicLong[BOUNDS_MILLISECONDS.length + 1];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = new AtomicLong();
		}

		return counts;
	}

	private static long[] boundsNanoseconds() {
		long[] nanoseconds = new long[BOUNDS_MILLISECONDS.length];
		for (int i = 0; i < nanoseconds.length; i++) {
			nanoseconds[i] = BOUNDS_MILLISECONDS[i] * 1_000_000L;
		}

		return nanoseconds;
	}
}
