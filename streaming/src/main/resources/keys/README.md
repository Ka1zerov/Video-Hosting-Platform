# CloudFront Private Key Setup

## Generate CloudFront Key Pair for URL Signing

### 1. Generate Private Key
```bash
# Generate RSA private key (2048 bits)
openssl genrsa -out cloudfront-private-key.pem 2048

# Extract public key from private key
openssl rsa -pubout -in cloudfront-private-key.pem -out cloudfront-public-key.pem

# Convert private key to DER format if needed
openssl pkcs8 -topk8 -nocrypt -in cloudfront-private-key.pem -inform PEM -out cloudfront-private-key.der -outform DER
```

### 2. Upload Public Key to CloudFront
1. Go to AWS CloudFront Console
2. Navigate to "Key Management" → "Public Keys"
3. Click "Create public key"
4. Upload the `cloudfront-public-key.pem` file
5. Note the Key Pair ID (e.g., `K1234567890ABC`)

### 3. Create Key Group
1. In CloudFront Console, go to "Key Management" → "Key Groups"
2. Click "Create key group"
3. Add your public key to the key group
4. Note the Key Group ID

### 4. Configure CloudFront Distribution
1. Edit your CloudFront distribution
2. Go to "Behaviors" tab
3. Edit the behavior for your content
4. Set "Restrict Viewer Access" to "Yes"
5. Select "Trusted Key Groups" and add your key group

### 5. Update Application Configuration
```yaml
aws:
  cloudfront:
    domain: your-distribution-domain.cloudfront.net
    enabled: true
    signing:
      enabled: true
      key-pair-id: K1234567890ABC  # Your Key Pair ID
      private-key-path: classpath:keys/cloudfront-private-key.pem
      default-expiration-hours: 2
```

### 6. Place Private Key
- Copy `cloudfront-private-key.pem` to this directory (`src/main/resources/keys/`)
- **IMPORTANT**: Never commit the actual private key to version control
- Add `*.pem` and `*.der` to `.gitignore`

### Security Notes
- Private key should be stored securely in production (AWS Secrets Manager, etc.)
- Use environment variables or secure key management for production deployments
- Rotate keys regularly as per security best practices 