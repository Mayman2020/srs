# Copy to run-backend.secrets.ps1 (gitignored) and adjust.
# run-backend.ps1 dot-sources run-backend.secrets.ps1 when AC_JWT_SECRET is not set.

$env:AC_JWT_SECRET = '0123456789abcdef0123456789abcdef0123456789abcdef01'
$env:SPRING_DATASOURCE_PASSWORD = 'postgres'
$env:CAMUNDA_BPM_ADMIN_PASSWORD = 'admin'
