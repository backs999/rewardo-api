# Rewardo API

Spring Boot application for the Rewardo API.

## Build and Deploy

The project includes a PowerShell script (`build-and-deploy.ps1`) that builds the application and uploads the artifact to an S3 bucket.

### Prerequisites

- PowerShell
- AWS CLI installed and configured
- Maven (or use the included Maven wrapper)
- AWS credentials with permissions to upload to the S3 bucket

### Usage

Run the script from the project root directory:

```powershell
./build-and-deploy.ps1
```

### Configuration

The script uses the following default configuration, which can be overridden with environment variables:

| Environment Variable | Default Value | Description |
|---------------------|---------------|-------------|
| AWS_PROFILE | deploy-user | AWS profile to use for S3 upload |
| S3_BUCKET | rewardo-deploy-artefacts | S3 bucket name |
| S3_PREFIX | rewardo-api | Prefix (folder) within the S3 bucket |
| VERSION | latest | Version identifier for the artifact |
| BUILD_DIR | target | Directory where build artifacts are located |

#### Examples

Upload with a specific version:

```powershell
$env:VERSION = "1.0.0"; ./build-and-deploy.ps1
```

Use a different AWS profile:

```powershell
$env:AWS_PROFILE = "production"; ./build-and-deploy.ps1
```

Use a different S3 bucket:

```powershell
$env:S3_BUCKET = "my-deployment-bucket"; ./build-and-deploy.ps1
```

## Development

### Running Locally

To run the application locally:

```bash
./mvnw spring-boot:run
```

### Running Tests

To run tests:

```bash
./mvnw test
```