# Codion AWS Lambda Module

Serverless Codion entity server implementation for AWS Lambda using Lambda Function URLs.

## Overview

This module provides AWS Lambda support for Codion applications, allowing existing `HttpEntityConnection` clients 
to work with Lambda deployments without modification. The implementation creates an embedded EntityServer instance
that handles all authentication, connection pooling, transaction management, and other server-side logic.

**⚠️ SECURITY WARNING**: This module uses Java serialization for client-server communication and is designed for 
**internal enterprise applications behind a VPN**. Do NOT deploy this to the public internet without additional 
security measures.

## Architecture

```
HttpEntityConnection → Lambda Function URL → Lambda → EntityServer → RemoteEntityConnection → Database
        ↓                      ↓                ↓          ↓               ↓                    ↓
  Java Serialization    Base64 Encode       Decode   Auth/Pool/TX    Domain + JDBC      PostgreSQL/H2
```

The Lambda handler delegates all entity operations to an embedded EntityServer instance, which provides:
- User authentication and authorization
- Per-user connection pooling
- Transaction management across Lambda invocations
- Connection idle timeout management
- All standard EntityServer features

## Quick Start

### Option 1: Use LambdaEntityHandler Directly

The handler automatically creates an EntityServer with loaded domains:

```bash
aws lambda create-function \
  --function-name your-entity-server \
  --runtime java21 \
  --handler is.codion.framework.lambda.LambdaEntityHandler::handleRequest \
  --role arn:aws:iam::YOUR_ACCOUNT:role/lambda-execution-role \
  --zip-file fileb://build/distributions/codion-server-aws-deployment.zip \
  --memory-size 1024 \
  --timeout 30 \
  --environment 'Variables={JAVA_TOOL_OPTIONS="-Dcodion.db.url=jdbc:postgresql://your-db-host/dbname -Dcodion.server.serialization.filter.patterns=is.codion.*;java.*;com.mycompany.*"}'

# Create a Lambda Function URL for direct HTTP access
aws lambda create-function-url-config \
  --function-name your-entity-server \
  --auth-type NONE

# Get the function URL
aws lambda get-function-url-config --function-name your-entity-server
```

### Option 2: Extend for Custom Behavior

For custom server configuration or authentication logic, extend `LambdaEntityHandler`:

```java
public class MyAppLambdaHandler extends LambdaEntityHandler {
    public MyAppLambdaHandler() {
        super(); // EntityServer created with default configuration
    }
    
    @Override
    protected EntityServerConfiguration createServerConfiguration() {
        // Customize server configuration
        return super.createServerConfiguration()
            .toBuilder()
            .adminUser(User.parse("admin:secret"))
            .authenticator(new MyCustomAuthenticator())
            .connectionLimit(100)
            .build();
    }
}
```

## Features

- **Embedded EntityServer**: Full EntityServer functionality in a Lambda environment
- **Automatic Connection Management**: EntityServer handles all connection pooling and lifecycle
- **Transaction Support**: EntityServer maintains transaction state across Lambda invocations
- **Multi-User Support**: Built-in authentication and per-user connection management
- **Warm Start Optimization**: EntityServer persists across Lambda invocations
- **Health Monitoring**: Server availability exposed via health endpoint
- **Java Serialization**: Full compatibility with HttpEntityConnection clients
- **All EntityServer Features**: Authentication, authorization, connection limits, idle timeouts, etc.

## Build

```bash
./gradlew :codion-framework-lambda:shadowJar
```

This creates a deployable JAR in `build/libs/`

## Configuration

### Configuration

#### EntityServer Configuration via System Properties

All EntityServer configuration is done through standard Java system properties. Set these using the `JAVA_TOOL_OPTIONS` environment variable:

```bash
JAVA_TOOL_OPTIONS="-Dcodion.db.url=jdbc:postgresql://host/db -Dcodion.server.idleConnectionTimeout=10"
```

Common configuration properties:

**Database:**
- `codion.db.url` - JDBC connection URL (required)
- `codion.db.initScripts` - Initialization scripts (e.g., `classpath:create_schema.sql`)
- `codion.db.countQueries` - Enable query counting for monitoring

**EntityServer:**
- `codion.server.domain.classes` - Comma-separated domain class names
- `codion.server.idleConnectionTimeout` - Connection idle timeout in minutes (default: 10)
- `codion.server.connectionLimit` - Max connections per client (default: 10)
- `codion.server.pooling.poolFactoryClass` - Connection pool implementation (e.g., `is.codion.plugin.hikari.pool.HikariConnectionPoolProvider`)
- `codion.server.serialization.filter.patterns` - Serialization filter patterns (required)
- `codion.server.serialization.filter.patternFile` - Path to filter pattern file

See EntityServer documentation for all available configuration properties.

### EntityServer Behavior

The embedded EntityServer manages all connection and transaction state:

1. **Automatic Management**: EntityServer handles all connection pooling, authentication, and lifecycle
2. **Connection Persistence**: EntityServer maintains connections across Lambda invocations during warm starts
3. **Transaction Management**: EntityServer keeps connections with open transactions alive across invocations
4. **Idle Timeout**: Connections are automatically closed after the configured idle timeout (default: 10 minutes)
5. **Per-User Isolation**: Each authenticated user gets isolated connections through EntityServer
6. **Standard Server Features**: All EntityServer capabilities including connection limits, authentication, etc.

### Health Check Endpoint

The `/health` endpoint provides service status:

```json
{
  "status": "UP",
  "service": "codion-lambda"
}
```

Note: The health endpoint doesn't require authentication and simply reports whether the EntityServer is available.

Access via Lambda Function URL:
```bash
curl https://your-function-url.lambda-url.region.on.aws/health
```

## Advanced Usage

### Custom Authentication

Use EntityServer's built-in authenticator support:

```java
public class SecureLambdaHandler extends LambdaEntityHandler {
    
    @Override
    protected EntityServerConfiguration createServerConfiguration() {
        return super.createServerConfiguration()
            .toBuilder()
            .authenticator(new Authenticator() {
                @Override
                public RemoteClient login(RemoteClient remoteClient) throws LoginException {
                    // Custom authentication logic
                    validateUserCredentials(remoteClient.user());
                    return remoteClient;
                }
            })
            .build();
    }
}
```

### Multi-Domain Support

The EntityServer automatically supports multiple domains. Each request must include a `domainTypeName` header:

```java
// Client configuration
EntityConnectionProvider provider =
    HttpEntityConnectionProvider.builder()
        .hostName("your-function-url.lambda-url.region.on.aws")
        .https(true)
        .securePort(443)
        .domain(YourDomain.DOMAIN)
        .user(User.parse("username:password"))
        .build();
```

Note: The `domainTypeName` header is automatically set by HttpEntityConnection based on the domain type.

### Multi-Tenant Support

```java
public class MultiTenantLambdaHandler extends LambdaEntityHandler {
    
    @Override
    protected Database createDatabase() {
        // Return tenant-specific database based on request context
        String tenant = System.getenv("TENANT_ID");
        return Database.instance("jdbc:postgresql://host/" + tenant);
    }
}
```

### Monitoring and Admin Access

```java
public class MonitoredLambdaHandler extends LambdaEntityHandler {
    
    @Override
    protected EntityServerConfiguration createServerConfiguration() {
        return super.createServerConfiguration()
            .toBuilder()
            .adminUser(User.parse("admin:secret"))
            .build();
    }
    
    // Access server statistics via admin interface
    public ServerStatistics getStatistics() throws RemoteException {
        return entityServer.admin(adminUser).serverStatistics();
    }
}
```

### Transaction Management

EntityServer provides full transaction management that works seamlessly across Lambda invocations:

```java
EntityConnection connection = connectionProvider.connection();
connection.startTransaction();
try {    
    // Perform multiple operations
    connection.insert(entities1);
    connection.update(entities2);
    connection.delete(keys);
    
    connection.commitTransaction();
} catch (Exception e) {
    connection.rollbackTransaction();
    throw e;
}
```

**How it works:**
- EntityServer maintains connection state across Lambda invocations
- When a transaction is started, EntityServer keeps the connection active
- Each operation within a transaction uses the same underlying database connection
- EntityServer's idle timeout (default: 10 minutes) ensures connections stay alive during transactions
- Transaction state is preserved as long as the Lambda container remains warm

## Client Usage

### Lambda Function URL (Recommended)
Lambda Function URLs provide direct HTTP(S) endpoints without path prefixes:

```java
EntityConnectionProvider provider =
    HttpEntityConnectionProvider.builder()
        .hostName("your-function-url.lambda-url.region.on.aws")
        .https(true)
        .securePort(443)
        .domain(YourDomain.DOMAIN)
        .user(User.parse("username:password"))
        .build();
```

### API Gateway (Not Supported)
⚠️ **Important**: This module only supports Lambda Function URLs, not API Gateway.

API Gateway has fundamental incompatibilities with HttpEntityConnection:
- API Gateway requires stage paths (e.g., `/prod`) in URLs
- HttpEntityConnection does not support base paths
- API Gateway uses different event formats (v1 and v2) than Lambda Function URLs

Lambda Function URLs provide a simpler, more direct solution without these issues.

## Deployment

### Dependencies

```groovy
dependencies {
    implementation(project(":codion-framework-lambda"))
    implementation(project(":your-domain-module"))
    
    // Database driver
    runtimeOnly(project(":codion-dbms-postgresql")) // or your database
    
    // Optional: Advanced connection pooling
    runtimeOnly(project(":codion-plugin-hikari-pool"))
    
    // AWS Lambda runtime
    compileOnly("com.amazonaws:aws-lambda-java-core:1.2.3")
    compileOnly("com.amazonaws:aws-lambda-java-events:3.11.4")
}
```

**Note:** Database authentication is handled per-user through the Lambda authentication headers. Each user connects with their own database credentials, so there's no need to configure database username/password at the Lambda level.

### Deployment Scripts

See the scripts in `src/main/scripts/` for deployment examples:
- `simple-deploy.sh` - Basic Lambda deployment
- `deploy-with-rds.sh` - Deploy with RDS database
- `deploy-with-efs.sh` - Deploy with EFS for persistent storage
- `create-lambda-role.sh` - Create IAM role for Lambda

## Features

- Embedded EntityServer with all standard server capabilities
- Java serialization for client compatibility
- Automatic connection management by EntityServer
- Health check endpoint at `/health`
- Support for all standard entity operations:
  - Select, Count, Insert, Update, Delete
  - Batch operations
  - Transaction management (start, commit, rollback)
  - Query cache control
  - Database procedures and functions
  - Report generation
- EntityServer features:
  - Authentication and authorization
  - Connection pooling and lifecycle management
  - Idle timeout management
  - Connection limits per client
  - Admin interface support

## Performance Considerations

### EntityServer Optimization

1. **Cold Start Impact**: EntityServer initialization adds ~200-300ms to cold start time
2. **Warm Start Benefits**: EntityServer persists across invocations, providing instant connection access
3. **Memory Usage**: EntityServer overhead is ~20-30MB plus connection pools
4. **Recommended Settings**:
   - `IDLE_CONNECTION_TIMEOUT=10` (minutes) - Matches typical Lambda warm container lifetime
   - `CLIENT_CONNECTION_LIMIT=10` - Sufficient for most internal applications
   - Consider HikariCP integration for advanced connection pool features

### Lambda Configuration

- **Memory**: Allocate at least 512MB (1024MB recommended for better CPU)
- **Timeout**: Set based on longest expected query + 10s buffer
- **Provisioned Concurrency**: Use for latency-sensitive applications
- **VPC Configuration**: Required for private RDS access and network security, adds cold start latency

### Network Security Setup

For secure internal deployment:

1. **VPC Deployment**: Deploy Lambda in a private VPC subnet
2. **Security Groups**: Restrict access to known IP ranges/VPNs
3. **RDS in Private Subnet**: Keep database in private subnet, accessible only from Lambda
4. **No Internet Gateway**: Lambda subnet should not have direct internet access
5. **NAT Gateway**: If Lambda needs internet access (for dependencies), use NAT Gateway

### Monitoring Best Practices

1. Enable CloudWatch Logs for connection pool metrics
2. Monitor `/health` endpoint for pool saturation
3. Set CloudWatch alarms on:
   - Pool usage > 80%
   - Average checkout time > 100ms
   - Connection failures

## Security

### ⚠️ Deployment Security Requirements

**This Lambda module is designed for internal enterprise use and should be deployed with appropriate network security:**

1. **VPN/Private Network**: Deploy behind a corporate VPN or in a private network
2. **No Public Internet Access**: Do not expose Lambda Function URLs to the public internet
3. **Network ACLs**: Use AWS security groups and NACLs to restrict access
4. **Internal Users Only**: Intended for internal business applications with known, authenticated users

**Why these restrictions?**
- Uses Java serialization (security filtered, but still a concern for public exposure)
- Basic Authentication over HTTPS (appropriate for internal use, not public APIs)
- Designed for trusted internal environments with 1-10 concurrent users

### Authentication

**Authentication is required for all requests** (except `/health` endpoint).

Supported authentication methods:
- **Basic authentication** via `Authorization` header (Base64 encoded over HTTPS)
- **Custom user header** via `X-User` header
- **Extensible authentication** via `extractUser` override

The handler will return an error if no authentication is provided:
```
IllegalArgumentException: No authentication provided - missing Authorization or X-User header
```

### Serialization Filtering
The Lambda handler implements Java serialization filtering to prevent deserialization attacks. 
You **must** configure allowed classes via environment variables or the Lambda will refuse to start.

#### Example Pattern Configuration
```bash
# Allow specific classes
SERIALIZATION_FILTER_PATTERNS="is.codion.*;java.lang.*;java.util.*;java.time.*;java.sql.*"

# Or use a pattern file
SERIALIZATION_FILTER_PATTERN_FILE="/opt/lambda/serialization-filter.txt"
# Or from classpath
SERIALIZATION_FILTER_PATTERN_FILE="classpath:serialization-filter.txt"
```

#### Pattern File Format
```
# serialization-filter.txt
# Comments start with #
is.codion.*
java.lang.*
java.util.*
java.time.*
java.sql.*
com.mycompany.domain.*
```

The filter uses the standard Java serialization filter format. See [JEP 290](https://openjdk.org/jeps/290) for details.

## Domain Deployment

### Option 1: ServiceLoader (Recommended)

Register your domain in `META-INF/services/is.codion.framework.domain.Domain`:
```
com.mycompany.MyDomain
```

### Option 2: Environment Variable

For non-service-enabled domains, use the `DOMAIN_CLASS_NAMES` environment variable:
```bash
DOMAIN_CLASS_NAMES="com.mycompany.Domain1,com.mycompany.Domain2"
```

Both options can be used together. The handler will load all available domains and route requests based on the `domainTypeName` header.

## Why Lambda Function URLs?

Lambda Function URLs are the only supported deployment method because:

1. **Direct HTTP Access** - No intermediate layers or path transformations
2. **Simple Event Format** - Consistent `rawPath` field for all requests
3. **Lower Latency** - No API Gateway overhead
4. **Simpler Setup** - One command deployment
5. **Cost Effective** - No additional charges

### CORS Support

The Lambda handler includes built-in CORS support:
- All responses include appropriate CORS headers
- OPTIONS requests are handled automatically
- All necessary Codion headers are allowed (`domainTypeName`, `X-User`, etc.)

## Troubleshooting

### Common Issues

#### Path Not Found Errors

If you're getting "Not found: /" errors, ensure you're using Lambda Function URLs, not API Gateway:

**Verify your deployment**:
```bash
# Check if you have a function URL
aws lambda get-function-url-config --function-name your-function

# If not, create one
aws lambda create-function-url-config \
  --function-name your-function \
  --auth-type NONE
```

**Use the function URL in your client**:
```java
// Function URL format: https://xxxxx.lambda-url.region.on.aws/
EntityConnectionProvider provider =
    HttpEntityConnectionProvider.builder()
        .hostName("xxxxx.lambda-url.region.on.aws")
        // ... rest of configuration
        .build();
```

#### Missing domainTypeName Header

Lambda Function URLs convert all header names to lowercase. The handler automatically checks for both:
- `domaintypename` (lowercase version)
- `domainTypeName` (original case)

If you're still getting "Missing required header: domainTypeName" errors:
1. Check CloudWatch logs for "Received headers:" debug output
2. Ensure HttpEntityConnection is sending the header
3. Verify no proxy/gateway is stripping headers

### StreamCorruptedException: invalid stream header

This error occurs when the Lambda returns JSON error messages instead of serialized Java objects. Common causes:
- 403 Forbidden from API Gateway (see above)
- Missing `domainTypeName` header
- Domain not loaded in Lambda
- Serialization filter blocking required classes

### Transaction Issues

#### Transaction State After Cold Starts
- EntityServer and all connection state is lost during Lambda cold starts
- If a cold start occurs between `startTransaction()` and `commit/rollback`, the transaction will be lost
- Consider implementing transaction retry logic in your client for critical operations
- Monitor Lambda metrics to understand cold start frequency

#### Transaction Timeout
- EntityServer's idle timeout (default: 10 minutes) usually covers typical transaction durations
- Lambda's maximum execution time (15 minutes) is per invocation, not per transaction
- Each operation within a transaction is a separate Lambda invocation
- Set `IDLE_CONNECTION_TIMEOUT` based on your longest expected transaction duration