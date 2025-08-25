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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.logging.MethodTrace;
import is.codion.common.state.State;
import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.tracer.MethodTracer;
import is.codion.framework.db.local.tracer.MethodTracer.Traceable;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static is.codion.framework.db.local.tracer.MethodTracer.methodTracer;

/**
 * A class responsible for managing a local EntityConnection.
 * @see LocalEntityConnectionProvider#builder()
 */
final class DefaultLocalEntityConnectionProvider extends AbstractEntityConnectionProvider
				implements LocalEntityConnectionProvider {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultLocalEntityConnectionProvider.class);

	private final Domain domain;
	private final Database database;
	private final int queryTimeout;

	private final State tracing = State.builder()
					.value(TRACING.getOrThrow())
					.consumer(this::tracingChanged)
					.build();

	DefaultLocalEntityConnectionProvider(DefaultLocalEntityConnectionProviderBuilder builder) {
		super(builder);
		this.domain = builder.domain == null ? initializeDomain(domainType()) : builder.domain;
		this.database = builder.database == null ? Database.instance() : builder.database;
		this.queryTimeout = builder.queryTimeout;
	}

	@Override
	public String connectionType() {
		return CONNECTION_TYPE_LOCAL;
	}

	@Override
	public String description() {
		return database().name().toUpperCase();
	}

	@Override
	public Domain domain() {
		return domain;
	}

	@Override
	public Database database() {
		return database;
	}

	@Override
	public LocalEntityConnection connection() {
		return (LocalEntityConnection) validConnection();
	}

	@Override
	public State tracing() {
		return tracing;
	}

	@Override
	public List<MethodTrace> traces() {
		return ((Traceable) connection()).tracer().entries();
	}

	@Override
	protected LocalEntityConnection connect() {
		LOG.debug("Initializing connection for {}", user());
		LocalEntityConnection connection = localEntityConnection(database(), domain(), user());
		if (tracing.is()) {
			setMethodTracer(methodTracer(TRACES.getOrThrow()), (Traceable) connection);
		}
		connection.queryTimeout(queryTimeout);

		return connection;
	}

	@Override
	protected void close(EntityConnection connection) {
		connection.close();
	}

	private void tracingChanged(boolean trace) {
		setMethodTracer(trace ? methodTracer(TRACES.getOrThrow()) : MethodTracer.NO_OP, (Traceable) connection());
	}

	private static void setMethodTracer(MethodTracer methodTracer, Traceable traceable) {
		traceable.tracer(methodTracer);
	}

	private static Domain initializeDomain(DomainType domainType) {
		return Domain.domains().stream()
						.filter(domain -> domain.type().equals(domainType))
						.findAny()
						.orElseThrow(() -> new IllegalStateException("Domain model not found in ServiceLoader: " + domainType));
	}
}