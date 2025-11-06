# Mendix to CaseTalk Integration

This folder contains tools and documentation for exporting Mendix database metadata to CaseTalk's jcatalog format.

## 📁 Files in This Folder

| File | Description |
|------|-------------|
| **README_MENDIX_EXPORT.md** | Main documentation - Start here! |
| **MENDIX_CONNECTION_EXAMPLES.md** | Database connection strings and troubleshooting |
| **ExportMendixMetadata.java** | Mendix Java Action for in-app export |
| **StandaloneMendixExporter.java** | Standalone command-line tool |
| **export_mendix.bat** | Windows batch script for easy execution |

## 🚀 Quick Start

### Option 1: Standalone Tool (Recommended)

1. Download required JARs:
   - `json-20231013.jar` (org.json library)
   - `postgresql-42.7.1.jar` (or your database's JDBC driver)

2. Compile the exporter:
   ```cmd
   javac -cp ".;json-20231013.jar" StandaloneMendixExporter.java
   ```

3. Run the batch script:
   ```cmd
   export_mendix.bat
   ```

   Or manually:
   ```cmd
   java -cp ".;json-20231013.jar;postgresql-42.7.1.jar" StandaloneMendixExporter ^
     "jdbc:postgresql://localhost:5432/mendix" postgres secret output.jcatalog false
   ```

4. Copy the generated `.jcatalog` file to:
   - Your CaseTalk project folder, OR
   - `%APPDATA%\CaseTalk12\` for global access

5. Open CaseTalk → Repository → SQL Browser (F12)
   - The jcatalog should appear in the tree
   - Right-click tables → "Save to Repository" to import

### Option 2: Mendix Java Action

1. Copy `ExportMendixMetadata.java` into your Mendix project
2. Add `org.json` library to `userlib` folder
3. Create a microflow that calls the Java Action
4. Add a button to trigger the export

See **README_MENDIX_EXPORT.md** for detailed instructions.

## 📊 What Gets Exported

The jcatalog format captures:

- ✅ **Catalogs** - Database containers
- ✅ **Schemas** - Logical groupings (e.g., `public`)
- ✅ **Tables** - All user tables
- ✅ **Columns** - With data types, sizes, nullable flags
- ✅ **Primary Keys** - Column(s) and their positions
- ✅ **Foreign Keys** - Relationships between tables
- ✅ **Annotations** - Table and column comments

## 🔧 Requirements

### Java
- **JDK/JRE**: 8 or higher
- **Libraries**:
  - `org.json` (json-20231013.jar or later)
  - JDBC driver for your database

### JDBC Drivers

| Database | Driver JAR | Download |
|----------|-----------|----------|
| PostgreSQL | postgresql-42.7.1.jar | https://jdbc.postgresql.org/download/ |
| SQL Server | mssql-jdbc-12.4.2.jre11.jar | https://learn.microsoft.com/sql/connect/jdbc/ |
| MySQL | mysql-connector-j-8.2.0.jar | https://dev.mysql.com/downloads/connector/j/ |
| Oracle | ojdbc11.jar | https://www.oracle.com/database/technologies/jdbc-drivers-12c-downloads.html |
| H2 | h2-2.2.224.jar | https://www.h2database.com/html/download.html |

### CaseTalk
- CaseTalk version 12 or later
- Repository SQL Browser feature

## 🗂️ jcatalog Format

Example structure:

```json
{
  "connection": {
    "catalogs": [{
      "name": "mendix",
      "schemas": [{
        "name": "public",
        "tables": [{
          "name": "mymodule$customer",
          "columns": [
            {"name": "id", "metadata": {"type": "BIGINT", "nullable": false}},
            {"name": "name", "metadata": {"type": "VARCHAR", "size": 255}}
          ],
          "primarykey": {
            "columns": [{"column": "id", "position": "1"}]
          },
          "foreignkeys": [...]
        }]
      }]
    }]
  }
}
```

## 🎯 Use Cases

### Reverse Engineering
1. Export Mendix database structure
2. Import into CaseTalk Repository
3. Analyze and refine conceptual model
4. Generate documentation

### Data Modeling
1. Compare Mendix implementation vs. conceptual model
2. Identify normalization opportunities
3. Document business rules
4. Generate UML/ORM diagrams

### Integration Analysis
1. Document existing database schema
2. Plan data migrations
3. Design API integrations
4. Create data dictionaries

### Governance
1. Maintain metadata catalog
2. Track schema changes over time
3. Enforce naming conventions
4. Document data lineage

## 📖 Documentation Links

- **[README_MENDIX_EXPORT.md](README_MENDIX_EXPORT.md)** - Complete usage guide
- **[MENDIX_CONNECTION_EXAMPLES.md](MENDIX_CONNECTION_EXAMPLES.md)** - Connection strings and troubleshooting
- [CaseTalk Documentation](https://www.casetalk.com)
- [Mendix Database Settings](https://docs.mendix.com/refguide/database-settings/)

## 🐛 Troubleshooting

### "No suitable driver found"
- Ensure JDBC driver JAR is in classpath
- Check driver class name matches your database

### "Connection refused"
- Verify database server is running
- Check host, port, and firewall settings
- Test with database client (pgAdmin, etc.)

### "Authentication failed"
- Verify username and password
- Check database user permissions
- Review host-based authentication (PostgreSQL: pg_hba.conf)

### Empty or missing tables
- Set `includeSystemTables = true` to see all tables
- Check schema name (might not be "public")
- Verify database has tables

See **MENDIX_CONNECTION_EXAMPLES.md** for more troubleshooting tips.

## 🤝 Support

For issues or questions:
- **CaseTalk**: https://www.casetalk.com/support
- **Mendix**: https://docs.mendix.com
- **This integration**: Check the documentation files in this folder

## 📝 Version History

- **v1.0** (2025-01-06)
  - Initial release
  - Support for PostgreSQL, SQL Server, MySQL, Oracle, H2
  - Standalone and Mendix Java Action implementations
  - Batch script for Windows
  - Comprehensive documentation

## 🔮 Future Enhancements

Potential improvements:
- GUI application for easier configuration
- Support for Mendix-specific metadata (entities, associations)
- Scheduled exports
- Version comparison
- Direct CaseTalk API integration
- Cross-platform shell scripts (Linux, macOS)

## 📄 License

This integration tool is provided as-is for use with CaseTalk.
Refer to CaseTalk license terms for usage rights.

---

**Author**: CaseTalk Integration Team
**Date**: 2025-01-06
**CaseTalk Version**: 12.x and later
