#!/bin/bash

# Test the deployed Lambda

# Get API endpoint
API_ID=$(aws apigatewayv2 get-apis --query "Items[?Name=='chinook-api'].ApiId" --output text)
ENDPOINT="https://$API_ID.execute-api.$(aws configure get region).amazonaws.com/prod"

echo "Testing Lambda deployment at: $ENDPOINT"

# 1. Health check
echo -e "\n1. Testing health endpoint..."
curl -X GET "$ENDPOINT/health"

# 2. Test with Codion client
echo -e "\n\n2. Creating test client..."
cat > TestLambda.java <<'EOF'
import is.codion.common.user.User;
import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.http.HttpEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.Condition;

public class TestLambda {
    public static void main(String[] args) throws Exception {
        String endpoint = args[0];
        
        EntityConnectionProvider provider = HttpEntityConnectionProvider.builder()
            .baseUrl(endpoint)
            .domain(Chinook.DOMAIN)
            .user(User.parse("scott:tiger"))
            .https(true)
            .build();
            
        try (EntityConnection connection = provider.connection()) {
            System.out.println("Connected to Lambda!");
            
            // Count artists
            int count = connection.count(Condition.all(Chinook.Artist.TYPE));
            System.out.println("Artist count: " + count);
            
            System.out.println("Success!");
        }
    }
}
EOF

echo -e "\n3. To test with Codion client, update your connection:"
echo "EntityConnectionProvider provider = HttpEntityConnectionProvider.builder()"
echo "    .baseUrl(\"$ENDPOINT\")"
echo "    .domain(Chinook.DOMAIN)"
echo "    .https(true)"
echo "    .user(User.parse(\"scott:tiger\"))"
echo "    .build();"