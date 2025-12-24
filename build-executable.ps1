# Build and Extract Executable Script
# This script builds the game and extracts it to a ready-to-run folder

Write-Host "Building game distribution..." -ForegroundColor Green
.\gradlew.bat distZip

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful!" -ForegroundColor Green
    
    $zipPath = "build\distributions\celectLikeUsingGameEngine-1.0-SNAPSHOT.zip"
    $extractPath = "game-executable"
    
    if (Test-Path $extractPath) {
        Write-Host "Removing old extraction..." -ForegroundColor Yellow
        try {
            Remove-Item -Recurse -Force $extractPath -ErrorAction Stop
        } catch {
            Write-Host "Warning: Could not remove old extraction (files may be in use). Continuing anyway..." -ForegroundColor Yellow
            Write-Host "  If the game is running, close it and try again for a clean extraction." -ForegroundColor Yellow
        }
    }
    
    Write-Host "Extracting to $extractPath..." -ForegroundColor Green
    Expand-Archive -Path $zipPath -DestinationPath $extractPath -Force
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "Executable ready!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Location: $extractPath\celectLikeUsingGameEngine-1.0-SNAPSHOT" -ForegroundColor Yellow
    Write-Host "`nTo run the game:" -ForegroundColor White
    Write-Host "  cd $extractPath\celectLikeUsingGameEngine-1.0-SNAPSHOT\bin" -ForegroundColor Yellow
    Write-Host "  .\celectLikeUsingGameEngine.bat" -ForegroundColor Yellow
    Write-Host "`nOr simply double-click the .bat file in the bin folder!" -ForegroundColor White
} else {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

