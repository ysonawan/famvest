# FamVest Portfolio Management Application

FamVest is a portfolio management application designed for families to track, analyze, and manage their investments in one place. It provides tools for monitoring asset allocation, performance, and generating reports for better financial planning.

## Features
- Centralized portfolio tracking for multiple family members
- Asset allocation and performance analytics
- Transaction history and reporting
- Integration with Zerodha KiteConnect API
- Secure and customizable configuration

## Prerequisites
- Java 17 or higher
- Maven 3.6+
- **Python 3.8+**  
  Download and install from [python.org](https://www.python.org/downloads/).
  
  **Set up Python virtual environment:**
  ```bash
  # Create virtual environment
  python3 -m venv venv
  
  # Activate virtual environment
  # On macOS/Linux:
  source venv/bin/activate
  # On Windows:
  # venv\Scripts\activate
  
  # Install required Python packages
  pip install requests pyotp pandas numpy
  ```
  
  **Note:** The Python scripts in `scripts/python/` require these dependencies for Zerodha Kite API integration.
- Zerodha KiteConnect API (kiteconnect.jar)
- **Node.js (v22+) and npm**  
  Download and install from [nodejs.org](https://nodejs.org/).
- **Angular CLI**  
  Install globally using:
  ```bash
  npm install -g @angular/cli
  ```

## Installing Dependencies on Linux

This section provides detailed steps to install all required dependencies on a Linux system (Ubuntu/Debian-based).

### Step 1: Update System Package Manager
Update the package manager to ensure you have the latest package information:
```bash
sudo apt update
```

### Step 2: Install Network Tools
Install essential network utilities:
```bash
sudo apt install net-tools
```

### Step 3: Install Nginx (Web Server)
Install and configure Nginx as a reverse proxy for the application:
```bash
sudo apt install -y nginx
sudo systemctl restart nginx
sudo systemctl enable nginx
```

Verify Nginx is running:
```bash
sudo systemctl status nginx
```

### Step 4: Install Java Development Kit (JDK 17)
Install OpenJDK 17 required for running the FamVest application:
```bash
sudo apt install -y openjdk-17-jdk
```

Verify the installation:
```bash
java -version
```

### Step 5: Install Maven
Install Maven for building the Java application:
```bash
sudo apt install -y maven
```

Verify the installation:
```bash
mvn -version
```

### Step 6: Install Node.js and npm
Install Node.js and npm for the Angular frontend:
```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
```

Verify the installation:
```bash
node --version
npm --version
```

Install Angular CLI globally:
```bash
sudo npm install -g @angular/cli
```

### Step 7: Install Python and Virtual Environment
Install Python 3.8+ and pip:
```bash
sudo apt install -y python3 python3-pip python3-venv
```

Verify the installation:
```bash
python3 --version
```

Create and activate a Python virtual environment:
```bash
cd /path/to/fam-vest-app
python3 -m venv venv
source venv/bin/activate
```

Install required Python packages for Zerodha Kite API integration:
```bash
pip install requests pyotp pandas numpy
```

### Step 8: Install and Configure Redis
Install Redis for caching and session management:
```bash
sudo apt install -y redis-server
```

Enable and start Redis:
```bash
sudo systemctl enable redis-server
sudo systemctl start redis-server
```

Verify Redis is running:
```bash
redis-cli ping
```

**Configure Redis (Optional but Recommended):**

Open the Redis configuration file:
```bash
sudo vi /etc/redis/redis.conf
```

Update the following settings for security and systemd integration:
```
bind 127.0.0.1 ::1
protected-mode yes
supervised systemd
requirepass StrongRedisPasswordHere
```

Restart Redis to apply changes:
```bash
sudo systemctl restart redis-server
sudo systemctl enable redis-server
```

### Step 9: Install and Configure PostgreSQL
Install PostgreSQL database:
```bash
sudo apt update
sudo apt install -y postgresql postgresql-contrib
```

Verify the installation:
```bash
psql --version
```

Enable and start PostgreSQL:
```bash
sudo systemctl enable postgresql
sudo systemctl start postgresql
```

Verify PostgreSQL is running:
```bash
sudo systemctl status postgresql
```

### Step 10: Install AWS S3 CLI Tools (Optional)
Install s3cmd for AWS S3 integration if backup functionality is needed:
```bash
sudo apt install -y s3cmd
```

Configure S3 credentials:
```bash
s3cmd --configure
```

Follow the prompts to enter your AWS Access Key and Secret Key.

### Step 11: Verify All Installations
Run the following commands to verify all dependencies are properly installed:
```bash
java -version
mvn -version
node --version
npm --version
python3 --version
psql --version
redis-cli ping
sudo systemctl status nginx
sudo systemctl status postgresql
sudo systemctl status redis-server
```

All services should show as "active (running)" or similar status indicators.

## Project Structure
```
├── pom.xml
├── README.md
├── libs/                # External JARs (e.g., kiteconnect.jar)
├── config/              # Application configuration files
├── src/                 # Main source code (Java, resources)
├── scripts/             # Utility and deployment scripts
├── logs/                # Application logs
├── ui/                  # Angular frontend
```

## Building the Application
1. **Add KiteConnect Dependency**
   - Download `kiteconnect.jar` and place it in the `libs/` directory.
   - Install the JAR to your local Maven repository:
     ```bash
     mvn install:install-file \
       -Dfile=libs/kiteconnect.jar \
       -DgroupId=com.zerodhatech.kiteconnect \
       -DartifactId=kiteconnect \
       -Dversion=3.5.1 \
       -Dpackaging=jar
     ```
   - Add the following to your `pom.xml`:
     ```xml
     <dependency>
         <groupId>com.zerodhatech.kiteconnect</groupId>
         <artifactId>kiteconnect</artifactId>
         <version>3.5.1</version>
     </dependency>
     ```
2. **Build Application (Front end and Backend)**
   ```bash
   mvn clean install
   ```

## Running the Application
  ```bash
  java -jar target/fam-vest-app-2.0.0.jar
  ```

## Configuration
- Edit `config/application.properties` and `src/main/resources/application.properties` for environment-specific settings.
- Update scripts in `scripts/` for deployment and automation.

## FamVest Production Deployment Guide

This guide provides comprehensive step-by-step instructions for deploying the FamVest application to a production server.

### Prerequisites for Production Deployment

- Ubuntu/Debian-based server with sudo access
- Java 17+ installed
- Maven 3.6+ installed
- Nginx installed and configured
- Certbot for SSL certificates (Let's Encrypt)
- PostgreSQL database server configured
- Redis server configured
- Python 3.8+ with virtual environment
- All dependencies installed as per the "Installing Dependencies on Linux" section

### Step 1: Create Application Directory Structure

Navigate to the deployment directory and create the required folder structure:
```bash
cd /opt
sudo mkdir -p app/famvest/config
sudo mkdir -p app/famvest/logs
cd app/famvest
```

Set appropriate permissions:
```bash
sudo chown -R $(whoami):$(whoami) /opt/app/famvest
chmod -R 755 /opt/app/famvest
```

### Step 2: Clone or Copy Application Files

Clone the FamVest repository or copy the application source:
```bash
cd /opt/app/famvest
git clone <your-repo-url> source
# OR
cp -r /path/to/local/fam-vest-app source
```

### Step 3: Copy and Configure Production Properties

Copy the production configuration file to the config directory:
```bash
cp source/src/main/resources/application-prod.properties config/application-prod.properties
```

Edit the production properties file with production-specific settings:
```bash
vi config/application-prod.properties
```

Update the following settings:
- Database connection URL and credentials
- Redis configuration and password
- Zerodha KiteConnect API keys
- Email configuration for notifications
- AWS S3 credentials (if using S3 backups)
- Log file paths

Example configuration:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/famvest_prod
spring.datasource.username=famvest_user
spring.datasource.password=SecureDbPassword

spring.redis.host=127.0.0.1
spring.redis.port=6379
spring.redis.password=StrongRedisPasswordHere

server.port=8080
server.servlet.context-path=/api

logging.file.path=/opt/app/famvest/logs
logging.level.root=INFO
```

### Step 4: Build the Application

Build the FamVest application with Maven:
```bash
cd source
mvn clean install -DskipTests -Pprod
```

The compiled JAR will be located at `target/fam-vest-app-2.0.0.jar`.

Copy the JAR to the application directory:
```bash
cp target/fam-vest-app-2.0.0.jar /opt/app/famvest/
```

### Step 5: Execute Database Schema Scripts

Set up the PostgreSQL database:
```bash
sudo -u postgres psql
```

Inside the PostgreSQL prompt:
```sql
CREATE DATABASE famvest_prod;
CREATE USER famvest_user WITH PASSWORD 'SecureDbPassword';
ALTER ROLE famvest_user SET client_encoding TO 'utf8';
ALTER ROLE famvest_user SET default_transaction_isolation TO 'read committed';
ALTER ROLE famvest_user SET default_transaction_deferrable TO on;
ALTER ROLE famvest_user SET default_time_zone TO 'UTC';
GRANT ALL PRIVILEGES ON DATABASE famvest_prod TO famvest_user;
\q
```

Execute the database schema scripts:
```bash
psql -U famvest_user -d famvest_prod -f source/src/main/resources/ddl/ddl.sql
psql -U famvest_user -d famvest_prod -f source/src/main/resources/ddl/inserts.sql
```

### Step 6: Set Up Systemd Service File

Copy the systemd service file to the system directory:
```bash
sudo cp source/prod-deployment-scripts/systemd/famvest-app.service /etc/systemd/system/
```

Edit the service file if needed:
```bash
sudo vi /etc/systemd/system/famvest-app.service
```

Ensure the service file contains:
```ini
[Unit]
Description=FamVest Application
After=network.target postgresql.service redis-server.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/app/famvest
Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
Environment="SPRING_CONFIG_LOCATION=file:/opt/app/famvest/config/application-prod.properties"
ExecStart=/usr/bin/java -jar fam-vest-app-2.0.0.jar
Restart=on-failure
RestartSec=10
StandardOutput=append:/opt/app/famvest/logs/famvest-app.log
StandardError=append:/opt/app/famvest/logs/famvest-app-error.log

[Install]
WantedBy=multi-user.target
```

Enable and reload systemd:
```bash
sudo systemctl enable famvest-app
sudo systemctl daemon-reload
```

### Step 7: Configure Nginx Reverse Proxy

Copy the Nginx configuration file:
```bash
sudo cp source/prod-deployment-scripts/nginx/famvest.online /etc/nginx/sites-available/
sudo cp source/prod-deployment-scripts/nginx/trade.famvest.online /etc/nginx/sites-available/
```

Edit Nginx configuration for main domain:
```bash
sudo vi /etc/nginx/sites-available/famvest.online
```

Example Nginx configuration:
```nginx
upstream famvest_backend {
    server 127.0.0.1:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name famvest.online www.famvest.online;

    location / {
        proxy_pass http://famvest_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /api {
        proxy_pass http://famvest_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    error_page 502 /502.html;
    location = /502.html {
        root /etc/nginx/pages;
        internal;
    }
}
```

Enable the Nginx sites:
```bash
sudo ln -sf /etc/nginx/sites-available/famvest.online /etc/nginx/sites-enabled/famvest.online
sudo ln -sf /etc/nginx/sites-available/trade.famvest.online /etc/nginx/sites-enabled/trade.famvest.online
```

Test Nginx configuration:
```bash
sudo nginx -t
```

### Step 8: Obtain SSL Certificate with Let's Encrypt

Install and obtain SSL certificates for both domains:
```bash
sudo certbot --nginx -d famvest.online -d www.famvest.online
sudo certbot --nginx -d trade.famvest.online
```

Follow the Certbot prompts to complete the SSL setup.

Enable automatic certificate renewal:
```bash
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
```

### Step 9: Start Application and Verify Deployment

Start the FamVest application:
```bash
sudo systemctl start famvest-app
```

Verify the application is running:
```bash
sudo systemctl status famvest-app
```

Restart Nginx to apply all configurations:
```bash
sudo systemctl restart nginx
```

Verify Nginx is running:
```bash
sudo systemctl status nginx
```

### Step 10: Set Up Application Logging and Monitoring

Create log rotation configuration:
```bash
sudo tee /etc/logrotate.d/famvest > /dev/null <<EOF
/opt/app/famvest/logs/*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 ubuntu ubuntu
    sharedscripts
    postrotate
        sudo systemctl reload famvest-app > /dev/null 2>&1 || true
    endscript
}
EOF
```

Monitor application logs:
```bash
tail -f /opt/app/famvest/logs/famvest-app.log
tail -f /opt/app/famvest/logs/famvest-app-error.log
```

### Step 11: Set Up Automated Backups (Optional)

Copy the backup script:
```bash
sudo cp source/prod-deployment-scripts/backup/daily_backup.sh /usr/local/bin/
sudo chmod +x /usr/local/bin/daily_backup.sh
```

Configure cron for automated daily backups:
```bash
sudo cp source/prod-deployment-scripts/crontab/crontab /etc/cron.d/famvest-backup
```

Edit the cron configuration:
```bash
sudo vi /etc/cron.d/famvest-backup
```

Example cron configuration:
```cron
# FamVest Application Backups
0 2 * * * ubuntu /usr/local/bin/daily_backup.sh >> /opt/app/famvest/logs/backup.log 2>&1
```

### Step 12: Configure S3 Backup (Optional)

If using AWS S3 for backups, configure s3cmd:
```bash
s3cmd --configure
```

Copy your S3 configuration:
```bash
cp ~/.s3cfg /opt/app/famvest/config/
```

Update the backup script with S3 upload functionality.

### Step 13: Health Check and Verification

Verify all components are operational:
```bash
# Check application status
sudo systemctl status famvest-app

# Check Nginx status
sudo systemctl status nginx

# Check PostgreSQL connection
psql -U famvest_user -d famvest_prod -c "SELECT version();"

# Check Redis connection
redis-cli ping

# Test API endpoint
curl -I https://famvest.online/api/health

# Test frontend
curl -I https://famvest.online/
```

## Contributing
Pull requests and suggestions are welcome! Please open an issue for major changes.

## Contact
For support or inquiries, contact: [famvest@hotmail.com]

---
