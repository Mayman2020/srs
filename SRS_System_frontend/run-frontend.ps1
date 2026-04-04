if (-not (Test-Path "node_modules")) {
    Write-Host "node_modules not found. Installing packages..." -ForegroundColor Yellow
    npm install
}
npm start
