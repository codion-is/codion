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
/**
 * AWS Lambda integration for Codion entity services.<br>
 * <br>
 * This module provides serverless deployment capabilities for Codion applications
 * on AWS Lambda. It implements the Codion entity protocol directly, allowing
 * existing HTTP-based entity connection clients to connect to Lambda functions
 * without modification.<br>
 * <br>
 * Key features:
 * <ul>
 * <li>Connection pooling using Codion's {@link is.codion.common.db.pool.ConnectionPoolWrapper}</li>
 * <li>Multi-user support with separate pools per authenticated user</li>
 * <li>Health monitoring with connection pool statistics</li>
 * <li>Extensible architecture via {@link is.codion.framework.lambda.AbstractLambdaEntityHandler}</li>
 * </ul>
 */
module is.codion.framework.lambda {
	requires transitive is.codion.framework.domain;
	requires transitive is.codion.framework.db.local;
	requires transitive is.codion.framework.db.core;
	
	requires is.codion.common.core;
	requires is.codion.common.db;
	
	// AWS Lambda dependencies - static since they're provided at runtime
	requires static aws.lambda.java.core;
	requires static aws.lambda.java.events;
	
	exports is.codion.framework.lambda;
}