# Copy to run-backend.secrets.ps1 (gitignored) and adjust values for your machine.
# run-backend.ps1 loads this for profile local when this file exists (see script). Other profiles: loaded when JWT/db env is incomplete.

$env:AC_JWT_SECRET = '0123456789abcdef0123456789abcdef0123456789abcdef01'
# PostgreSQL user postgres — default matches hesabaty-backend ($env:DB_PASSWORD default admin). Use YOUR real password.
# $env:DB_PASSWORD = 'admin'   # optional: share same var as hesabaty-backend
$env:AC_LOCAL_DB_PASSWORD = 'admin'
$env:CAMUNDA_BPM_ADMIN_PASSWORD = 'admin'
