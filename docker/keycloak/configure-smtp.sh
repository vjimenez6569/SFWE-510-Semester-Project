
#!/bin/sh
set -eu

KC="/opt/keycloak/bin/kcadm.sh"
SERVER="http://keycloak:8080"
REALM="wyntermarket"

MAX_ATTEMPTS=30
RETRY_DELAY=5

# Verify required environment variables.
: "${KEYCLOAK_ADMIN:?KEYCLOAK_ADMIN is missing}"
: "${KEYCLOAK_ADMIN_PASSWORD:?KEYCLOAK_ADMIN_PASSWORD is missing}"
: "${GMAIL_SMTP_PASSWORD:?GMAIL_SMTP_PASSWORD is missing}"

echo "Waiting for Keycloak authentication..."

attempt=1

until "$KC" config credentials \
    --server "$SERVER" \
    --realm master \
    --user "$KEYCLOAK_ADMIN" \
    --password "$KEYCLOAK_ADMIN_PASSWORD" \
    >/dev/null 2>&1
do
    if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
        echo "ERROR: Keycloak authentication failed after $MAX_ATTEMPTS attempts."
        exit 1
    fi

    echo "Keycloak not ready or authentication failed ($attempt/$MAX_ATTEMPTS)."
    sleep "$RETRY_DELAY"
    attempt=$((attempt + 1))
done

echo "Keycloak authentication successful."

echo "Waiting for realm '$REALM'..."

attempt=1

until "$KC" get "realms/$REALM" >/dev/null 2>&1
do
    if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
        echo "ERROR: Realm '$REALM' was not available."
        exit 1
    fi

    echo "Realm not available ($attempt/$MAX_ATTEMPTS)."
    sleep "$RETRY_DELAY"
    attempt=$((attempt + 1))
done

echo "Realm found. Configuring Gmail SMTP..."

"$KC" update "realms/$REALM" \
    -s "smtpServer.host=smtp.gmail.com" \
    -s "smtpServer.port=587" \
    -s "smtpServer.from=wynter.marketplace@gmail.com" \
    -s "smtpServer.fromDisplayName=Wynter Market" \
    -s "smtpServer.replyTo=wynter.marketplace@gmail.com" \
    -s "smtpServer.replyToDisplayName=Wynter Market Support" \
    -s "smtpServer.auth=true" \
    -s "smtpServer.ssl=false" \
    -s "smtpServer.starttls=true" \
    -s "smtpServer.user=wynter.marketplace@gmail.com" \
    -s "smtpServer.password=$GMAIL_SMTP_PASSWORD"

echo "SUCCESS: Wynter Market Gmail SMTP configured."

echo "Configuring Keycloak administrator email..."

ADMIN_ID=$("$KC" get users \
    -r master \
    -q "username=$KEYCLOAK_ADMIN" \
    --fields id \
    --format csv | tail -n 1 | tr -d '"' | tr -d '\r')

if [ -z "$ADMIN_ID" ]; then
    echo "ERROR: Keycloak administrator not found."
    exit 1
fi

"$KC" update "users/$ADMIN_ID" \
    -r master \
    -s "email=$KEYCLOAK_ADMIN_EMAIL" \
    -s "emailVerified=true"

echo "Keycloak administrator email configured."
