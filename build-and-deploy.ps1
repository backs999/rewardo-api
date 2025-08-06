#!/usr/bin/env pwsh

# build-and-deploy.ps1
# Script to build the rewardo-api application and upload the artifact to S3

# Configuration (can be overridden with environment variables)
$AWS_PROFILE = if ($env:AWS_PROFILE) { $env:AWS_PROFILE } else { "deploy-user" }
$S3_BUCKET = if ($env:S3_BUCKET) { $env:S3_BUCKET } else { "rewardo-deploy-artefacts" }
$S3_PREFIX = if ($env:S3_PREFIX) { $env:S3_PREFIX } else { "rewardo-api" }
$VERSION = if ($env:VERSION) { $env:VERSION } else { "latest" }
$BUILD_DIR = if ($env:BUILD_DIR) { $env:BUILD_DIR } else { "target" }

# Display configuration
Write-Host "=== Build and Deploy Configuration ==="
Write-Host "AWS Profile: $AWS_PROFILE"
Write-Host "S3 Bucket: $S3_BUCKET"
Write-Host "S3 Prefix: $S3_PREFIX"
Write-Host "Version: $VERSION"
Write-Host "Build Directory: $BUILD_DIR"
Write-Host ""

# Build the application
Write-Host "Building application..."
try {
    & ./mvnw clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven build failed with exit code $LASTEXITCODE"
        exit 1
    }
    Write-Host "Build completed successfully."
} catch {
    Write-Error "Build failed: $_"
    exit 1
}

# Get the JAR file
$jarFile = Get-ChildItem -Path "$BUILD_DIR" -Filter "*.jar" | Where-Object { -not $_.Name.Contains("sources") -and -not $_.Name.Contains("javadoc") } | Select-Object -First 1

if (-not $jarFile) {
    Write-Error "Could not find JAR file in $BUILD_DIR directory"
    exit 1
}

$jarPath = $jarFile.FullName
$originalJarName = $jarFile.Name

# Rename the artifact to reward-api.jar
$renamedJarName = "rewardo-api.jar"
$renamedJarPath = Join-Path -Path $BUILD_DIR -ChildPath $renamedJarName
Write-Host "Renaming artifact from $originalJarName to $renamedJarName..."
Copy-Item -Path $jarPath -Destination $renamedJarPath -Force
Write-Host "Artifact renamed successfully."
$jarPath = $renamedJarPath

$s3Key = if ($VERSION -eq "latest") { "$S3_PREFIX/$renamedJarName" } else { "$S3_PREFIX/$VERSION/$renamedJarName" }

Write-Host "Found JAR file: $jarPath"
Write-Host "Will upload to S3 as: $s3Key"

# Upload to S3
Write-Host "Uploading renamed artifact ($renamedJarName) to S3..."
try {
    aws s3 cp $jarPath "s3://$S3_BUCKET/$s3Key" --profile $AWS_PROFILE
    if ($LASTEXITCODE -ne 0) {
        Write-Error "AWS S3 upload failed with exit code $LASTEXITCODE"
        exit 1
    }
    Write-Host "Upload completed successfully."
    Write-Host "Renamed artifact available at: s3://$S3_BUCKET/$s3Key"
} catch {
    Write-Error "Upload failed: $_"
    exit 1
}

Write-Host "Build and deploy process completed successfully!"