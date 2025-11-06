# Mendix Database Connection Examples

This document provides connection string examples for different Mendix database configurations.

## PostgreSQL (Default for Mendix)

### Local Development

```
JDBC URL: jdbc:postgresql://localhost:5432/mendix
Username: postgres
Password: [your password]
Driver JAR: postgresql-42.7.1.jar
```

### Mendix Cloud

Mendix Cloud uses PostgreSQL. Get connection details from:
1. Mendix Developer Portal
2. Select your app → Environments
3. Details → Database → Show connection string

```
JDBC URL: jdbc:postgresql://[host]:[port]/[database]?sslmode=require
Username: [from portal]
Password: [from portal]
Driver JAR: postgresql-42.7.1.jar
```

Example:
```
jdbc:postgresql://db-123abc.postgres.database.azure.com:5432/mendix-prod?sslmode=require
```

### Docker Container

```
JDBC URL: jdbc:postgresql://localhost:5432/mendix
Username: mendix
Password: mendix
Driver JAR: postgresql-42.7.1.jar
```

## Microsoft SQL Server

### Local SQL Server

```
JDBC URL: jdbc:sqlserver://localhost:1433;databaseName=mendix;trustServerCertificate=true
Username: sa
Password: [your password]
Driver JAR: mssql-jdbc-12.4.2.jre11.jar
```

### Azure SQL Database

```
JDBC URL: jdbc:sqlserver://[server].database.windows.net:1433;databaseName=[database];encrypt=true;trustServerCertificate=false
Username: [username]@[server]
Password: [password]
Driver JAR: mssql-jdbc-12.4.2.jre11.jar
```

Example:
```
jdbc:sqlserver://mendix-server.database.windows.net:1433;databaseName=mendix-prod;encrypt=true
```

### Windows Authentication

```
JDBC URL: jdbc:sqlserver://localhost:1433;databaseName=mendix;integratedSecurity=true
Username: (not needed)
Password: (not needed)
Driver JAR: mssql-jdbc-12.4.2.jre11.jar + sqljdbc_auth.dll
```

## MySQL/MariaDB

### Local MySQL

```
JDBC URL: jdbc:mysql://localhost:3306/mendix?useSSL=false&allowPublicKeyRetrieval=true
Username: root
Password: [your password]
Driver JAR: mysql-connector-j-8.2.0.jar
```

### Cloud MySQL (AWS RDS, Azure Database)

```
JDBC URL: jdbc:mysql://[host]:3306/[database]?useSSL=true&requireSSL=true
Username: [username]
Password: [password]
Driver JAR: mysql-connector-j-8.2.0.jar
```

### MariaDB

```
JDBC URL: jdbc:mariadb://localhost:3306/mendix
Username: root
Password: [your password]
Driver JAR: mariadb-java-client-3.3.1.jar
```

## Oracle Database

### Local Oracle

```
JDBC URL: jdbc:oracle:thin:@localhost:1521:XE
Username: mendix
Password: [your password]
Driver JAR: ojdbc11.jar
```

### Oracle Service Name

```
JDBC URL: jdbc:oracle:thin:@//[host]:[port]/[service_name]
Username: [username]
Password: [password]
Driver JAR: ojdbc11.jar
```

### Oracle SID

```
JDBC URL: jdbc:oracle:thin:@[host]:[port]:[SID]
Username: [username]
Password: [password]
Driver JAR: ojdbc11.jar
```

## H2 Database (Mendix Development)

### Embedded H2

```
JDBC URL: jdbc:h2:~/mendix/data/database
Username: sa
Password: (empty)
Driver JAR: h2-2.2.224.jar
```

### H2 Server Mode

```
JDBC URL: jdbc:h2:tcp://localhost/~/mendix/data/database
Username: sa
Password: (empty)
Driver JAR: h2-2.2.224.jar
```

## SQLite (Not officially supported by Mendix, but possible)

```
JDBC URL: jdbc:sqlite:C:/mendix/data/database.db
Username: (not needed)
Password: (not needed)
Driver JAR: sqlite-jdbc-3.44.1.0.jar
```

## Finding Your Mendix Database Connection

### Method 1: Mendix Settings File

Look in your Mendix project's configuration:

**File**: `[project]/deployment/settings.yaml` or `settings.json`

Example `settings.yaml`:
```yaml
DatabaseType: PostgreSQL
DatabaseJdbcUrl: jdbc:postgresql://localhost:5432/mendix
DatabaseUserName: postgres
DatabasePassword: secret
```

### Method 2: Mendix Studio Pro

1. Open your project in Mendix Studio Pro
2. Go to **Project** → **Settings** → **Configurations** → **[Active Configuration]**
3. Look at the **Database** tab
4. Note the database type and connection details

### Method 3: Runtime Logs

Check Mendix runtime logs for database connection strings:

**Log file**: `[project]/deployment/log/mendix.log`

Look for lines containing:
```
INFO - Database: Successfully connected to database
```

### Method 4: Environment Variables

Mendix can use environment variables for database connection:

- `DATABASE_ENDPOINT` - Full JDBC URL
- `DATABASE_URL` - Alternative format
- `DB_HOST`, `DB_PORT`, `DB_NAME` - Individual components
- `DB_USERNAME`, `DB_PASSWORD` - Credentials

## Download JDBC Drivers

### PostgreSQL
- **Maven**: https://mvnrepository.com/artifact/org.postgresql/postgresql
- **Direct**: https://jdbc.postgresql.org/download/

### SQL Server
- **Maven**: https://mvnrepository.com/artifact/com.microsoft.sqlserver/mssql-jdbc
- **Direct**: https://learn.microsoft.com/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server

### MySQL
- **Maven**: https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
- **Direct**: https://dev.mysql.com/downloads/connector/j/

### Oracle
- **Maven**: https://mvnrepository.com/artifact/com.oracle.database.jdbc/ojdbc11
- **Direct**: https://www.oracle.com/database/technologies/jdbc-drivers-12c-downloads.html

### H2
- **Maven**: https://mvnrepository.com/artifact/com.h2database/h2
- **Direct**: https://www.h2database.com/html/download.html

## Troubleshooting Connection Issues

### "No suitable driver found"

**Solution**: Ensure the JDBC driver JAR is in the classpath
```bash
java -cp ".;postgresql-42.7.1.jar;json-20231013.jar" StandaloneMendixExporter ...
```

### "Connection refused"

**Causes**:
1. Database server is not running
2. Wrong host/port
3. Firewall blocking connection

**Solution**: Verify with database client (pgAdmin, SQL Server Management Studio, etc.)

### "Authentication failed"

**Causes**:
1. Wrong username/password
2. User doesn't have permissions
3. Host-based authentication restrictions (PostgreSQL pg_hba.conf)

**Solution**:
- Test credentials with database client
- Grant permissions: `GRANT SELECT ON ALL TABLES IN SCHEMA public TO username;`
- Check PostgreSQL: `pg_hba.conf` for allowed connections

### SSL/TLS Issues

**PostgreSQL**:
```
jdbc:postgresql://host:5432/db?sslmode=require
jdbc:postgresql://host:5432/db?sslmode=disable  (for testing only)
```

**SQL Server**:
```
jdbc:sqlserver://host:1433;encrypt=true;trustServerCertificate=true
```

**MySQL**:
```
jdbc:mysql://host:3306/db?useSSL=true&requireSSL=true
jdbc:mysql://host:3306/db?useSSL=false  (for testing only)
```

### Mendix-Specific Table Prefixes

Mendix uses prefixes for entity tables:
- Format: `modulename$entityname`
- Example: Entity `MyModule.Customer` → Table `mymodule$customer`
- System tables: `system$*`
- File documents: `system$filedocument`

To see all Mendix tables:
```sql
-- PostgreSQL
SELECT tablename FROM pg_tables WHERE schemaname = 'public';

-- SQL Server
SELECT name FROM sys.tables ORDER BY name;

-- MySQL
SHOW TABLES;
```

## Command-Line Examples

### Using the Batch Script

```cmd
cd Y:\CaseTalk.XE\Doc\Mendix
export_mendix.bat
```

### Manual Java Execution

**PostgreSQL**:
```cmd
java -cp ".;postgresql-42.7.1.jar;json-20231013.jar" StandaloneMendixExporter ^
  "jdbc:postgresql://localhost:5432/mendix" ^
  postgres secret mendix.jcatalog false
```

**SQL Server**:
```cmd
java -cp ".;mssql-jdbc-12.4.2.jre11.jar;json-20231013.jar" StandaloneMendixExporter ^
  "jdbc:sqlserver://localhost:1433;databaseName=mendix;trustServerCertificate=true" ^
  sa secret mendix.jcatalog false
```

**MySQL**:
```cmd
java -cp ".;mysql-connector-j-8.2.0.jar;json-20231013.jar" StandaloneMendixExporter ^
  "jdbc:mysql://localhost:3306/mendix?useSSL=false" ^
  root secret mendix.jcatalog false
```

## Testing Your Connection

Before running the exporter, test your connection:

### Using Command-Line Tools

**PostgreSQL**:
```bash
psql -h localhost -p 5432 -U postgres -d mendix
```

**SQL Server**:
```bash
sqlcmd -S localhost -U sa -P secret -d mendix
```

**MySQL**:
```bash
mysql -h localhost -P 3306 -u root -p mendix
```

### Using Java

Create a test file `TestConnection.java`:
```java
import java.sql.*;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/mendix";
        String user = "postgres";
        String pass = "secret";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Connected successfully!");
            System.out.println("Database: " + conn.getCatalog());
            System.out.println("Driver: " + conn.getMetaData().getDriverName());
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }
}
```

Compile and run:
```bash
javac TestConnection.java
java -cp ".;postgresql-42.7.1.jar" TestConnection
```

## See Also

- [README_MENDIX_EXPORT.md](README_MENDIX_EXPORT.md) - Main documentation
- [CaseTalk Documentation](https://www.casetalk.com)
- [Mendix Documentation - Database](https://docs.mendix.com/refguide/database-settings/)
- [JDBC Tutorial](https://docs.oracle.com/javase/tutorial/jdbc/)

---

**Version**: 1.0
**Last Updated**: 2025-01-06
