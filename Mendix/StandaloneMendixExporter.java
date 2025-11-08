import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Standalone tool to export database metadata to CaseTalk jcatalog format.
 *
 * This can connect to any JDBC-compatible database (including Mendix databases)
 * and export the metadata structure.
 *
 * Usage:
 *   java StandaloneMendixExporter <jdbcUrl> <username> <password> <outputFile> [includeSystemTables]
 *
 * Examples:
 *   PostgreSQL (Mendix default):
 *     java StandaloneMendixExporter "jdbc:postgresql://localhost:5432/mendix" postgres secret mendix.jcatalog false
 *
 *   SQL Server:
 *     java StandaloneMendixExporter "jdbc:sqlserver://localhost:1433;databaseName=mendix" sa secret mendix.jcatalog false
 *
 *   MySQL:
 *     java StandaloneMendixExporter "jdbc:mysql://localhost:3306/mendix" root secret mendix.jcatalog false
 *
 * @author CaseTalk Integration
 * @version 1.0
 */
public class StandaloneMendixExporter
{
    private boolean includeSystemTables = false;

    public static void main(String[] args)
    {
        if (args.length < 4)
        {
            System.err.println("Usage: java StandaloneMendixExporter <jdbcUrl> <username> <password> <outputFile> [includeSystemTables]");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  PostgreSQL: java StandaloneMendixExporter \"jdbc:postgresql://localhost:5432/mendix\" postgres secret mendix.jcatalog false");
            System.err.println("  SQL Server: java StandaloneMendixExporter \"jdbc:sqlserver://localhost:1433;databaseName=mendix\" sa secret mendix.jcatalog false");
            System.err.println("  MySQL:      java StandaloneMendixExporter \"jdbc:mysql://localhost:3306/mendix\" root secret mendix.jcatalog false");
            System.exit(1);
        }

        String jdbcUrl = args[0];
        String username = args[1];
        String password = args[2];
        String outputFile = args[3];
        boolean includeSystemTables = args.length > 4 ? Boolean.parseBoolean(args[4]) : false;

        StandaloneMendixExporter exporter = new StandaloneMendixExporter();
        exporter.includeSystemTables = includeSystemTables;

        try
        {
            System.out.println("Connecting to database: " + jdbcUrl);
            String result = exporter.exportMetadata(jdbcUrl, username, password, outputFile);
            System.out.println(result);
        }
        catch (Exception e)
        {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Export database metadata to jcatalog file
     */
    public String exportMetadata(String jdbcUrl, String username, String password, String outputFile) throws Exception
    {
        Connection conn = null;

        try
        {
            // Load appropriate JDBC driver based on URL
            loadDriver(jdbcUrl);

            // Connect to database
            conn = DriverManager.getConnection(jdbcUrl, username, password);
            System.out.println("Connected successfully!");

            // Generate jcatalog JSON
            System.out.println("Extracting metadata...");
            JSONObject jcatalog = generateJCatalog(conn);

            // Write to file
            System.out.println("Writing to file: " + outputFile);
            try (FileWriter file = new FileWriter(outputFile))
            {
                file.write(jcatalog.toString(4)); // Pretty print with 4-space indent
                file.flush();
            }

            return "Successfully exported metadata to: " + outputFile;
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (SQLException e)
                {
                    // Log but don't throw
                    System.err.println("Warning: Failed to close connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Load appropriate JDBC driver based on connection URL
     */
    private void loadDriver(String jdbcUrl) throws ClassNotFoundException
    {
        if (jdbcUrl.startsWith("jdbc:postgresql:"))
        {
            Class.forName("org.postgresql.Driver");
        }
        else if (jdbcUrl.startsWith("jdbc:sqlserver:"))
        {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        }
        else if (jdbcUrl.startsWith("jdbc:mysql:"))
        {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        else if (jdbcUrl.startsWith("jdbc:oracle:"))
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        }
        else if (jdbcUrl.startsWith("jdbc:sqlite:"))
        {
            Class.forName("org.sqlite.JDBC");
        }
        else if (jdbcUrl.startsWith("jdbc:h2:"))
        {
            Class.forName("org.h2.Driver");
        }
        else
        {
            System.out.println("Warning: Unknown JDBC driver for URL: " + jdbcUrl);
            System.out.println("Attempting to continue anyway...");
        }
    }

    /**
     * Generate the jcatalog JSON structure from database metadata
     */
    private JSONObject generateJCatalog(Connection conn) throws SQLException
    {
        DatabaseMetaData metaData = conn.getMetaData();

        JSONObject root = new JSONObject();
        JSONObject connection = new JSONObject();
        JSONArray catalogs = new JSONArray();

        // Get catalog name
        String catalogName = conn.getCatalog();
        if (catalogName == null || catalogName.isEmpty())
        {
            catalogName = "default";
        }

        System.out.println("Catalog: " + catalogName);

        JSONObject catalog = new JSONObject();
        catalog.put("name", catalogName);

        JSONArray schemas = new JSONArray();

        // Get all schemas
        ResultSet schemaRs = metaData.getSchemas();
        Map<String, List<String>> schemaTableMap = new HashMap<>();

        try
        {
            while (schemaRs.next())
            {
                String schemaName = schemaRs.getString("TABLE_SCHEM");

                // Filter out system schemas unless explicitly requested
                if (!includeSystemTables && isSystemSchema(schemaName))
                {
                    continue;
                }

                schemaTableMap.put(schemaName, new ArrayList<String>());
            }
        }
        finally
        {
            schemaRs.close();
        }

        // If no schemas found, use a default schema
        if (schemaTableMap.isEmpty())
        {
            schemaTableMap.put("public", new ArrayList<String>());
        }

        System.out.println("Found " + schemaTableMap.size() + " schema(s)");

        // Track junction tables to convert to associations
        List<JSONObject> junctionTableAssociations = new ArrayList<>();

        // Get all tables for each schema
        int totalTables = 0;
        for (String schemaName : schemaTableMap.keySet())
        {
            System.out.println("Processing schema: " + schemaName);

            JSONObject schema = new JSONObject();
            schema.put("name", schemaName);

            JSONArray tables = new JSONArray();

            // Get tables in this schema
            ResultSet tableRs = metaData.getTables(catalogName, schemaName, "%", new String[] {"TABLE"});

            try
            {
                while (tableRs.next())
                {
                    String tableName = tableRs.getString("TABLE_NAME");

                    // Filter out system tables unless explicitly requested
                    if (!includeSystemTables && (isSystemTable(tableName) || isMendixSystemTable(tableName)))
                    {
                        continue;
                    }

                    // Check if this is a Mendix junction table (many-to-many)
                    if (isJunctionTable(tableName, metaData, catalogName, schemaName))
                    {
                        // Convert to association instead of table
                        System.out.println("  Detected junction table: " + tableName + " (converting to association)");
                        JSONObject association = junctionTableToAssociation(
                            tableName, metaData, catalogName, schemaName);
                        junctionTableAssociations.add(association);
                        continue; // Don't add as table
                    }

                    System.out.println("  Processing table: " + tableName);

                    JSONObject table = new JSONObject();
                    table.put("name", tableName);

                    // Get columns for this table
                    JSONArray columns = getTableColumns(metaData, catalogName, schemaName, tableName);
                    table.put("columns", columns);

                    // Get primary key for this table
                    JSONObject primaryKey = getTablePrimaryKey(metaData, catalogName, schemaName, tableName);
                    if (primaryKey != null && primaryKey.has("columns"))
                    {
                        table.put("primarykey", primaryKey);
                    }

                    // Get foreign keys (optional - as annotations)
                    JSONArray foreignKeys = getTableForeignKeys(metaData, catalogName, schemaName, tableName);
                    if (foreignKeys.length() > 0)
                    {
                        table.put("foreignkeys", foreignKeys);
                    }

                    // Get table comment/annotation if available
                    String remarks = tableRs.getString("REMARKS");
                    if (remarks != null && !remarks.isEmpty())
                    {
                        table.put("annotation", remarks);
                    }

                    tables.put(table);
                    totalTables++;
                }
            }
            finally
            {
                tableRs.close();
            }

            // Only add schema if it has tables
            if (tables.length() > 0)
            {
                schema.put("tables", tables);

                // Add associations (converted junction tables)
                if (!junctionTableAssociations.isEmpty())
                {
                    JSONArray associations = new JSONArray();
                    for (JSONObject assoc : junctionTableAssociations)
                    {
                        associations.put(assoc);
                    }
                    schema.put("associations", associations);
                    System.out.println("Added " + associations.length() + " associations from junction tables");
                }

                schemas.put(schema);
            }
        }

        System.out.println("Total tables processed: " + totalTables);

        catalog.put("schemas", schemas);
        catalogs.put(catalog);

        connection.put("catalogs", catalogs);
        root.put("connection", connection);

        return root;
    }

    /**
     * Get all columns for a table
     */
    private JSONArray getTableColumns(DatabaseMetaData metaData, String catalog,
                                      String schema, String tableName) throws SQLException
    {
        JSONArray columns = new JSONArray();

        ResultSet columnRs = metaData.getColumns(catalog, schema, tableName, "%");

        try
        {
            while (columnRs.next())
            {
                JSONObject column = new JSONObject();
                String columnName = columnRs.getString("COLUMN_NAME");
                column.put("name", columnName);

                // Optional: Add data type information as metadata
                String dataType = columnRs.getString("TYPE_NAME");
                int columnSize = columnRs.getInt("COLUMN_SIZE");
                String nullable = columnRs.getString("IS_NULLABLE");
                String remarks = columnRs.getString("REMARKS");

                JSONObject metadata = new JSONObject();
                metadata.put("type", dataType);
                metadata.put("size", columnSize);
                metadata.put("nullable", "YES".equalsIgnoreCase(nullable));

                if (remarks != null && !remarks.isEmpty())
                {
                    metadata.put("annotation", remarks);
                }

                // Add default value if exists
                String defaultValue = columnRs.getString("COLUMN_DEF");
                if (defaultValue != null && !defaultValue.isEmpty())
                {
                    metadata.put("default", defaultValue);
                }

                column.put("metadata", metadata);

                columns.put(column);
            }
        }
        finally
        {
            columnRs.close();
        }

        return columns;
    }

    /**
     * Get primary key for a table
     */
    private JSONObject getTablePrimaryKey(DatabaseMetaData metaData, String catalog,
                                          String schema, String tableName) throws SQLException
    {
        JSONObject primaryKey = null;
        JSONArray pkColumns = new JSONArray();

        ResultSet pkRs = metaData.getPrimaryKeys(catalog, schema, tableName);

        try
        {
            while (pkRs.next())
            {
                JSONObject pkColumn = new JSONObject();
                pkColumn.put("column", pkRs.getString("COLUMN_NAME"));
                pkColumn.put("position", String.valueOf(pkRs.getInt("KEY_SEQ")));
                pkColumns.put(pkColumn);
            }
        }
        finally
        {
            pkRs.close();
        }

        if (pkColumns.length() > 0)
        {
            primaryKey = new JSONObject();
            primaryKey.put("columns", pkColumns);
        }

        return primaryKey;
    }

    /**
     * Get foreign keys for a table
     */
    private JSONArray getTableForeignKeys(DatabaseMetaData metaData, String catalog,
                                          String schema, String tableName) throws SQLException
    {
        JSONArray foreignKeys = new JSONArray();

        ResultSet fkRs = metaData.getImportedKeys(catalog, schema, tableName);

        try
        {
            Map<String, JSONObject> fkMap = new HashMap<>();

            while (fkRs.next())
            {
                String fkName = fkRs.getString("FK_NAME");

                if (fkName == null || fkName.isEmpty())
                {
                    fkName = "FK_" + fkRs.getString("FKCOLUMN_NAME");
                }

                JSONObject fk = fkMap.get(fkName);
                if (fk == null)
                {
                    fk = new JSONObject();
                    fk.put("name", fkName);
                    fk.put("referencedTable", fkRs.getString("PKTABLE_NAME"));
                    fk.put("referencedSchema", fkRs.getString("PKTABLE_SCHEM"));
                    fk.put("columns", new JSONArray());
                    fkMap.put(fkName, fk);
                }

                JSONObject fkColumn = new JSONObject();
                fkColumn.put("column", fkRs.getString("FKCOLUMN_NAME"));
                fkColumn.put("referencedColumn", fkRs.getString("PKCOLUMN_NAME"));
                fkColumn.put("position", String.valueOf(fkRs.getInt("KEY_SEQ")));

                ((JSONArray) fk.get("columns")).put(fkColumn);
            }

            for (JSONObject fk : fkMap.values())
            {
                foreignKeys.put(fk);
            }
        }
        finally
        {
            fkRs.close();
        }

        return foreignKeys;
    }

    /**
     * Check if schema is a system schema
     */
    private boolean isSystemSchema(String schemaName)
    {
        if (schemaName == null) return false;

        String lower = schemaName.toLowerCase();
        return lower.startsWith("information_schema") ||
               lower.startsWith("pg_") ||
               lower.startsWith("sys") ||
               lower.equals("performance_schema") ||
               lower.equals("mysql");
    }

    /**
     * Check if table is a system table
     */
    private boolean isSystemTable(String tableName)
    {
        if (tableName == null) return false;

        String lower = tableName.toLowerCase();
        return lower.startsWith("sys") ||
               lower.startsWith("msrep") ||
               lower.startsWith("dt") ||
               lower.startsWith("$");
    }

    /**
     * Check if table is a Mendix-specific system table
     * Enhanced filtering for Mendix system and technical modules
     */
    private boolean isMendixSystemTable(String tableName)
    {
        if (tableName == null) return false;

        String lower = tableName.toLowerCase();

        // Mendix system module (authentication, sessions, users, etc.)
        if (lower.startsWith("system$")) return true;

        // Mendix administration module
        if (lower.startsWith("administration$")) return true;

        // Common technical modules from Mendix Marketplace
        String[] techModules = {
            "mx",                    // Mendix internal
            "deeplink$",            // Deep link module
            "encryption$",          // Encryption module
            "email$",               // Email connector
            "audittrail$",          // Audit trail module
            "modelreflection$",     // Model reflection module
            "communitycommons$"     // Community commons
        };

        for (String module : techModules)
        {
            if (lower.startsWith(module)) return true;
        }

        return false;
    }

    /**
     * Detect if a table is a Mendix junction table for many-to-many relationships
     *
     * Junction tables have:
     * 1. Name format: module$entity1_entity2
     * 2. Exactly two foreign keys
     * 3. Typically a composite primary key
     */
    private boolean isJunctionTable(String tableName, DatabaseMetaData metaData,
                                    String catalog, String schema) throws SQLException
    {
        // Must have Mendix naming pattern
        if (!tableName.contains("$") || !tableName.contains("_"))
        {
            return false;
        }

        // Count foreign keys
        ResultSet fkRs = metaData.getImportedKeys(catalog, schema, tableName);
        int fkCount = 0;

        try
        {
            while (fkRs.next())
            {
                fkCount++;
            }
        }
        finally
        {
            fkRs.close();
        }

        // Junction tables have exactly 2 foreign keys
        return fkCount == 2;
    }

    /**
     * Convert junction table to semantic association
     */
    private JSONObject junctionTableToAssociation(String junctionTable,
                                                   DatabaseMetaData metaData,
                                                   String catalog, String schema) throws SQLException
    {
        JSONObject association = new JSONObject();
        association.put("type", "many-to-many");
        association.put("junctionTable", junctionTable);

        // Extract semantic name from table name
        // e.g., "mymodule$customer_order" → "Customer_Order"
        String name = junctionTable.substring(junctionTable.indexOf("$") + 1);
        association.put("name", formatAssociationName(name));

        // Get the two entities being associated
        ResultSet fkRs = metaData.getImportedKeys(catalog, schema, junctionTable);
        List<String> targets = new ArrayList<>();

        try
        {
            while (fkRs.next())
            {
                targets.add(fkRs.getString("PKTABLE_NAME"));
            }

            if (targets.size() == 2)
            {
                association.put("entity1", targets.get(0));
                association.put("entity2", targets.get(1));
            }
        }
        finally
        {
            fkRs.close();
        }

        return association;
    }

    /**
     * Format association name from table name
     */
    private String formatAssociationName(String name)
    {
        // Convert "customer_order" to "Customer_Order"
        String[] parts = name.split("_");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++)
        {
            if (i > 0) result.append("_");
            if (parts[i].length() > 0)
            {
                result.append(Character.toUpperCase(parts[i].charAt(0)));
                result.append(parts[i].substring(1));
            }
        }

        return result.toString();
    }
}
