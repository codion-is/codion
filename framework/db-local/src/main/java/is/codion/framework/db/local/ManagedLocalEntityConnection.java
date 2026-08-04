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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.logging.MethodTrace;
import is.codion.framework.db.AbstractEntityConnection;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionTracer;
import is.codion.framework.db.local.tracer.MethodTracer;
import is.codion.framework.db.local.tracer.MethodTracer.Traceable;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Optional;

import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static is.codion.framework.db.local.tracer.MethodTracer.methodTracer;

/**
 * A self-managing {@link LocalEntityConnection}, connecting on demand and reconnecting
 * when the underlying connection has gone bad.
 * @see LocalEntityConnection#builder()
 */
final class ManagedLocalEntityConnection extends AbstractEntityConnection
				implements LocalEntityConnection, EntityConnectionTracer {

	private static final Logger LOG = LoggerFactory.getLogger(ManagedLocalEntityConnection.class);

	private final Domain domain;
	private final Database database;

	private final Event<MethodTrace> traceEvent = Event.event();
	private final State tracing = State.builder()
					.value(TRACING.getOrThrow())
					.consumer(this::tracingChanged)
					.build();

	//held here rather than on the underlying connection, which these settings outlive
	private volatile int queryTimeout;
	private volatile boolean optimisticLocking = OPTIMISTIC_LOCKING.getOrThrow();
	private volatile boolean limitReferenceDepth = LIMIT_REFERENCE_DEPTH.getOrThrow();
	private volatile int iteratorBufferSize = ITERATOR_BUFFER_SIZE.getOrThrow();

	ManagedLocalEntityConnection(DefaultLocalEntityConnectionBuilder builder) {
		super(builder);
		this.domain = builder.domain == null ? initializeDomain(domainType()) : builder.domain;
		this.database = builder.database == null ? Database.instance() : builder.database;
		this.queryTimeout = builder.queryTimeout;
	}

	@Override
	public Optional<String> description() {
		return Optional.of(DESCRIPTION.optional().orElse(database.name()));
	}

	@Override
	public Connection connection() {
		return local().connection();
	}

	@Override
	public Database database() {
		return database;
	}

	@Override
	public boolean optimisticLocking() {
		return optimisticLocking;
	}

	@Override
	public void optimisticLocking(boolean optimisticLocking) {
		this.optimisticLocking = optimisticLocking;
		local().optimisticLocking(optimisticLocking);
	}

	@Override
	public int iteratorBufferSize() {
		return iteratorBufferSize;
	}

	@Override
	public void iteratorBufferSize(int iteratorBufferSize) {
		this.iteratorBufferSize = iteratorBufferSize;
		local().iteratorBufferSize(iteratorBufferSize);
	}

	@Override
	public boolean limitReferenceDepth() {
		return limitReferenceDepth;
	}

	@Override
	public void limitReferenceDepth(boolean limitReferenceDepth) {
		this.limitReferenceDepth = limitReferenceDepth;
		local().limitReferenceDepth(limitReferenceDepth);
	}

	@Override
	public int queryTimeout() {
		return queryTimeout;
	}

	@Override
	public void queryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
		local().queryTimeout(queryTimeout);
	}

	@Override
	public State tracing() {
		return tracing;
	}

	@Override
	public Observer<MethodTrace> trace() {
		return traceEvent.observer();
	}

	@Override
	protected LocalEntityConnection connect() {
		LOG.debug("Initializing connection for {}", user());
		LocalEntityConnection connection = localEntityConnection(database, domain, user());
		if (tracing.is()) {
			setMethodTracer(createMethodTracer(), (Traceable) connection);
		}
		//re-apply the settings, the connection being replaceable and they not
		connection.queryTimeout(queryTimeout);
		connection.optimisticLocking(optimisticLocking);
		connection.limitReferenceDepth(limitReferenceDepth);
		connection.iteratorBufferSize(iteratorBufferSize);
		LOG.info("Connection established to {} for user {}", database.name(), user());

		return connection;
	}

	@Override
	protected void close(EntityConnection connection) {
		LOG.info("Connection closed for user {}", user());
		connection.close();
	}

	private LocalEntityConnection local() {
		return (LocalEntityConnection) delegate();
	}

	private void tracingChanged(boolean trace) {
		LOG.debug("Method tracing {} for {}", trace ? "enabled" : "disabled", user());
		//configures the established connection, should one exist - connect() installs
		//the tracer on the next one, no reason to establish a connection here
		established().ifPresent(connection ->
						setMethodTracer(trace ? createMethodTracer() : MethodTracer.NO_OP, (Traceable) connection));
	}

	private static void setMethodTracer(MethodTracer methodTracer, Traceable traceable) {
		traceable.tracer(methodTracer);
	}

	private MethodTracer createMethodTracer() {
		MethodTracer methodTracer = methodTracer(TRACES.getOrThrow());
		methodTracer.onTrace(traceEvent);

		return methodTracer;
	}

	private static Domain initializeDomain(DomainType domainType) {
		return Domain.domains().stream()
						.filter(domain -> domain.type().equals(domainType))
						.findAny()
						.orElseThrow(() -> new IllegalStateException("Domain model not found in ServiceLoader: " + domainType));
	}
}