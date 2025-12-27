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

## Contributing
Pull requests and suggestions are welcome! Please open an issue for major changes.

## Contact
For support or inquiries, contact: [famvest@hotmail.com]

---
