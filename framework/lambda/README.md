# Codion AWS Lambda Module

Serverless Codion entity server implementation for AWS Lambda.

## Overview

This module provides AWS Lambda support for Codion applications, allowing existing `HttpEntityConnection` clients 
to work with Lambda deployments without modification. The implementation uses Codion's connection pooling framework
to efficiently manage database connections across Lambda invocations.

## Architecture

```
HttpEntityConnection → API Gateway → Lambda → LocalEntityConnection → Database
        ↓                    ↓           ↓              ↓                ↓
  Java Serialization   Base64 Encode  Decode    Domain + JDBC      PostgreSQL/H2
```

## Quick Start

### Option 1: Use the Default Handler

For simple single-domain applications, use the provided `DefaultLambdaEntityHandler`:

```bash
aws lambda create-function \
  --function-name your-entity-server \
  --runtime java21 \
  --handler is.codion.framework.lambda.DefaultLambdaEntityHandler::handleRequest \
  --role arn:aws:iam::YOUR_ACCOUNT:role/lambda-execution-role \
  --zip-file fileb://build/distributions/codion-server-aws-deployment.zip \
  --memory-size 1024 \
  --timeout 30 \
  --environment Variables="{DOMAIN_TYPE=YourDomain}"
```

### Option 2: Create a Custom Handler

For more control, extend `AbstractLambdaEntityHandler`:

```java
public class MyAppLambdaHandler extends AbstractLambdaEntityHandler {
    public MyAppLambdaHandler() {
        super(new MyDomainImpl());
    }
    
    @Override
    protected ConnectionPoolWrapper createConnectionPool(User user) {
        // Customize pool settings per user
        ConnectionPoolWrapper pool = super.createConnectionPool(user);
        pool.setMaximumPoolSize(20); // Larger pool for this app
        pool.setCleanupInterval(60); // Cleanup every minute
        return pool;
    }
}
```

## Features

- **Connection Pooling**: Uses Codion's ConnectionPoolWrapper for efficient connection management
- **Multi-User Support**: Separate connection pools per authenticated user
- **Warm Start Optimization**: Connection pools persist across Lambda invocations
- **Health Monitoring**: Connection pool statistics exposed via health endpoint
- **Direct Protocol Implementation**: No EntityServer required
- **Java Serialization**: Full compatibility with HttpEntityConnection clients
- **Transactional Support**: All standard entity operations supported

## Build

```bash
./gradlew :codion-framework-lambda:shadowJar
```

This creates a deployable JAR in `build/libs/`

## Configuration

### Environment Variables

#### Required
- `DOMAIN_TYPE` - Domain type identifier (required for DefaultLambdaEntityHandler)

#### Database Configuration
- `DATABASE_URL` - JDBC connection URL (default: `jdbc:h2:mem:codion`)
- `DATABASE_USER` - Database username (default: `sa`)
- `DATABASE_PASSWORD` - Database password (default: empty)
- `DATABASE_INIT_SCRIPTS` - Optional initialization scripts (e.g., `classpath:create_schema.sql`)

#### Connection Pool Configuration
- `CONNECTION_POOL_SIZE` - Maximum connections per pool (default: 5)
- `CONNECTION_POOL_TIMEOUT` - Idle timeout in seconds (default: 30)
- `DEFAULT_USER` - Default user for unauthenticated requests (default: `lambda:lambda`)

### Connection Pool Behavior

The Lambda handler implements intelligent connection pooling:

1. **Default User Pool**: A main connection pool is created on startup for the default user
2. **Per-User Pools**: Authenticated users get dedicated connection pools created on-demand
3. **Pool Persistence**: Pools persist across Lambda invocations during warm starts
4. **Automatic Cleanup**: Connections are validated and recycled as needed
5. **Statistics Tracking**: Pool usage statistics are collected for monitoring

### Health Check Endpoint

The `/health` endpoint provides connection pool statistics:

```json
{
  "status": "UP",
  "service": "codion-lambda",
  "domain": "YourDomain",
  "pool_size": 5,
  "pool_in_use": 2,
  "pool_available": 3
}
```

## Advanced Usage

### Custom Authentication

```java
public class SecureLambdaHandler extends AbstractLambdaEntityHandler {
    public SecureLambdaHandler() {
        super(new MyDomainImpl());
    }
    
    @Override
    public User extractUser(Map<String, String> headers) {
        // Implement JWT validation, API key checks, etc.
        String token = headers.get("Authorization");
        return validateToken(token);
    }
}
```

### Multi-Tenant Support

```java
public class MultiTenantLambdaHandler extends AbstractLambdaEntityHandler {
    public MultiTenantLambdaHandler() {
        super(new MyDomainImpl());
    }
    
    @Override
    protected ConnectionPoolWrapper createConnectionPool(User user) {
        // Create tenant-specific connection pools
        String tenant = extractTenantFromUser(user);
        Database tenantDb = createTenantDatabase(tenant);
        
        ConnectionPoolWrapper pool = tenantDb.createConnectionPool(
            ConnectionPoolFactory.instance(), user);
        pool.setMaximumPoolSize(3); // Smaller pools for multi-tenant
        return pool;
    }
}
```

### Request Hooks and Monitoring

```java
public class MonitoredLambdaHandler extends AbstractLambdaEntityHandler {
    public MonitoredLambdaHandler() {
        super(new MyDomainImpl());
    }
    
    @Override
    public void beforeRequest(String path, Map<String, String> headers, Context context) {
        // Log request with pool statistics
        var poolStats = getConnectionPool().statistics(System.currentTimeMillis());
        context.getLogger().log(String.format(
            "Processing: %s [Pool: %d/%d connections in use]", 
            path, poolStats.inUse(), poolStats.size()));
    }
    
    @Override
    public void afterRequest(String path, Map<String, String> headers, Context context) {
        // Log performance metrics
        var poolStats = getConnectionPool().statistics(System.currentTimeMillis());
        context.getLogger().log(String.format(
            "Completed: %s [Avg checkout time: %.2fms]", 
            path, poolStats.averageTime()));
    }
}
```

### Enabling Functions and Procedures

```java
public class AdvancedLambdaHandler extends AbstractLambdaEntityHandler {
    public AdvancedLambdaHandler() {
        super(new MyDomainImpl());
    }
    
    @Override
    public Object handleFunction(Object function, EntityConnection connection) throws Exception {
        // Implement function execution
        if (function instanceof FunctionType) {
            return connection.execute((FunctionType<?, ?>) function);
        }
        throw new UnsupportedOperationException("Unknown function type");
    }
    
    @Override
    public Object handleReport(Object reportType, EntityConnection connection) throws Exception {
        // Implement report generation
        if (reportType instanceof ReportType) {
            return connection.report((ReportType) reportType);
        }
        throw new UnsupportedOperationException("Unknown report type");
    }
}
```

## Client Usage

Simply point your `HttpEntityConnection` to the Lambda function URL:

```java
EntityConnectionProvider provider =
    HttpEntityConnectionProvider.builder()
        .baseUrl("https://api-id.execute-api.region.amazonaws.com/prod")
        .domain(YourDomain.DOMAIN)
        .user(User.user("username", "password"))
        .build();
```

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

### Deployment Scripts

See the scripts in `src/main/scripts/` for deployment examples:
- `simple-deploy.sh` - Basic Lambda deployment
- `deploy-with-rds.sh` - Deploy with RDS database
- `deploy-with-efs.sh` - Deploy with EFS for persistent storage
- `create-lambda-role.sh` - Create IAM role for Lambda

## Features

- Direct entity protocol implementation (no EntityServer required)
- Java serialization for client compatibility
- Connection pooling for Lambda warm starts
- Health check endpoint at `/health`
- Support for all standard entity operations:
  - Select, Count, Insert, Update, Delete
  - Batch operations
  - Transactional support
- Extensible architecture for custom requirements
- Request/response hooks for monitoring
- Custom authentication support

## Performance Considerations

### Connection Pool Optimization

1. **Cold Start Impact**: Initial pool creation adds ~100-200ms to cold start time
2. **Warm Start Benefits**: Subsequent requests reuse pooled connections (typical checkout time: <5ms)
3. **Memory Usage**: Each connection pool uses ~5-10MB depending on pool size
4. **Recommended Settings**:
   - Single-tenant apps: `CONNECTION_POOL_SIZE=10`
   - Multi-tenant apps: `CONNECTION_POOL_SIZE=3-5` per tenant
   - High-traffic apps: Consider HikariCP integration for advanced features

### Lambda Configuration

- **Memory**: Allocate at least 512MB (1024MB recommended for better CPU)
- **Timeout**: Set based on longest expected query + 10s buffer
- **Provisioned Concurrency**: Use for latency-sensitive applications
- **VPC Configuration**: Required for private RDS access, adds cold start latency

### Monitoring Best Practices

1. Enable CloudWatch Logs for connection pool metrics
2. Monitor `/health` endpoint for pool saturation
3. Set CloudWatch alarms on:
   - Pool usage > 80%
   - Average checkout time > 100ms
   - Connection failures

## Security

- Basic authentication via Authorization header
- Custom user header support (X-User)
- SSL/TLS encryption via API Gateway
- IAM role-based Lambda permissions
- Extensible authentication via `extractUser` override

## Domain Deployment

Your domain module must be included in the Lambda deployment package. When using `DefaultLambdaEntityHandler`,
the domain is loaded via ServiceLoader, so ensure your domain implementation is properly registered in 
`META-INF/services/is.codion.framework.domain.Domain`.