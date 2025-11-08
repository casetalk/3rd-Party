# Mendix to CaseTalk Metadata Export

This document explains how to export Mendix database metadata in the jcatalog format that CaseTalk can import.

## Overview

The `ExportMendixMetadata` Java Action extracts metadata from your Mendix application's database and exports it as a `.jcatalog` JSON file that CaseTalk can read.

## What is Exported

The jcatalog format includes:

- **Database Catalogs**: Top-level database containers
- **Schemas**: Logical groupings of tables
- **Tables**: All user tables (system tables can be optionally included)
- **Columns**: All columns with metadata (data type, size, nullable, annotations)
- **Primary Keys**: Column(s) that form the primary key with their position
- **Foreign Keys**: Relationships between tables (as annotations)
- **Comments/Annotations**: Any remarks on tables or columns

## Installation in Mendix

### Step 1: Add the Java Action

1. In Mendix Studio Pro, open your project
2. Navigate to your module (e.g., `MyModule`)
3. Right-click on **JavaActions** folder
4. Select **Add other** > **Java action**
5. Name it: `ExportMendixMetadata`
6. Copy the contents of `ExportMendixMetadata.java` into the Java action

### Step 2: Add Required Library

The Java action uses the `org.json` library. Add it to your project:

1. Download `json-20231013.jar` (or latest) from https://mvnrepository.com/artifact/org.json/json
2. Place it in your project's `userlib` folder
3. Or add via Maven dependency if your project uses Maven

### Step 3: Configure Parameters

In the Java Action settings, add these parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `outputPath` | String | Yes | Full path where the .jcatalog file will be saved (e.g., "C:/exports/mendix_metadata.jcatalog") |
| `includeSystemTables` | Boolean | No | Whether to include system tables (default: false) |

**Return type**: String (returns success message with file path)

### Step 4: Create a Microflow

Create a microflow to call the Java Action:

```
1. Create new microflow: ExportMetadata_Microflow
2. Add Java Action Call activity
3. Select: ExportMendixMetadata
4. Set parameters:
   - outputPath: 'C:/temp/mendix_export.jcatalog'
   - includeSystemTables: false
5. Add Show Message activity to display the result
```

### Step 5: Add a Button (Optional)

Add a button to a page to trigger the export:

1. Create a new button on a page
2. Set On click action: Call a microflow
3. Select: ExportMetadata_Microflow
4. Run your app and click the button

## Using the Export in CaseTalk

Once you have the `.jcatalog` file:

1. Open CaseTalk
2. Navigate to **Repository** > **SQL Browser** (or press F12)
3. The jcatalog file should automatically appear in the connection tree
4. Alternatively, copy the `.jcatalog` file to:
   - `%APPDATA%\CaseTalk12\` (for global access)
   - Or your project folder (for project-specific access)
5. Refresh the SQL Browser to see the catalog
6. Expand the tree to explore tables, columns, and primary keys
7. Use "Save to Repository" to import the metadata into your CaseTalk model

## jcatalog Format Specification

The exported JSON follows this structure:

```json
{
    "connection": {
        "catalogs": [
            {
                "name": "mendix",
                "schemas": [
                    {
                        "name": "public",
                        "tables": [
                            {
                                "name": "MyTable",
                                "columns": [
                                    {
                                        "name": "Id",
                                        "metadata": {
                                            "type": "BIGINT",
                                            "size": 19,
                                            "nullable": false
                                        }
                                    },
                                    {
                                        "name": "Name",
                                        "metadata": {
                                            "type": "VARCHAR",
                                            "size": 255,
                                            "nullable": true,
                                            "annotation": "Customer name"
                                        }
                                    }
                                ],
                                "primarykey": {
                                    "columns": [
                                        {
                                            "column": "Id",
                                            "position": "1"
                                        }
                                    ]
                                },
                                "foreignkeys": [
                                    {
                                        "name": "FK_MyTable_RelatedTable",
                                        "referencedTable": "RelatedTable",
                                        "referencedSchema": "public",
                                        "columns": [
                                            {
                                                "column": "RelatedId",
                                                "referencedColumn": "Id",
                                                "position": "1"
                                            }
                                        ]
                                    }
                                ],
                                "annotation": "Table description"
                            }
                        ]
                    }
                ]
            }
        ]
    }
}
```

## Simplified Alternative (SQL-Based Export)

If you prefer not to use a Java Action, you can use this SQL-based approach:

### For PostgreSQL (Mendix Default)

```sql
-- Save this query result and format it as JSON
SELECT
    t.table_catalog,
    t.table_schema,
    t.table_name,
    json_agg(
        json_build_object(
            'name', c.column_name,
            'type', c.data_type,
            'size', c.character_maximum_length,
            'nullable', c.is_nullable = 'YES'
        ) ORDER BY c.ordinal_position
    ) as columns
FROM information_schema.tables t
JOIN information_schema.columns c
    ON t.table_schema = c.table_schema
    AND t.table_name = c.table_name
WHERE t.table_schema NOT IN ('pg_catalog', 'information_schema')
    AND t.table_type = 'BASE TABLE'
GROUP BY t.table_catalog, t.table_schema, t.table_name;
```

Then manually format this into the jcatalog structure.

## Mendix-Specific Notes

### Entity vs Table Names

Mendix entity names are transformed to database table names with the module prefix:
- Entity: `MyModule.Customer`
- Database table: `mymodule$customer`

### Associations as Foreign Keys

Mendix associations are stored as foreign key columns:
- One-to-many: FK column in child table
- Many-to-many: Junction table created

### System Tables

Mendix creates several system tables (prefixed with `system$`). Set `includeSystemTables` to `true` to include these.

### Generalization

Mendix generalization (inheritance) creates separate tables. The jcatalog will show these as separate tables with their own PKs.

## Troubleshooting

### "Cannot get database connection"

**Solution**: Ensure your Mendix app is running and the database is accessible.

### "org.json not found"

**Solution**: Add the `org.json` library to your `userlib` folder.

### "Permission denied writing file"

**Solution**: Ensure the output path is writable. Try using a folder like `C:\temp\` or your user home directory.

### Empty jcatalog file

**Solution**: Check that `includeSystemTables` is set appropriately. Mendix may use non-standard schema names.

## Advanced: Programmatic Export

You can also call this from a scheduled event or REST endpoint:

```java
// In a microflow or Java action
String result = ExportMendixMetadata.execute(
    context,
    "C:/exports/mendix_" + System.currentTimeMillis() + ".jcatalog",
    false
);
```

## Example Microflow Logic

```
1. Show message: "Exporting metadata..."
2. Call Java Action: ExportMendixMetadata
   - outputPath: $OutputPath (from input parameter or constant)
   - includeSystemTables: $IncludeSystemTables
3. Retrieve result: $ExportResult
4. Show message: $ExportResult
5. (Optional) Send file via email or store in FileDocument
```

## Integration with CaseTalk Reverse Engineering

Once imported into CaseTalk:

1. Use the **Repository SQL Browser** to view the structure
2. Select tables and use **Save to Repository** to create OFTypes
3. CaseTalk will reverse-engineer:
   - Object types from tables
   - Fact types from columns and relationships
   - Constraints from primary and foreign keys
4. Use CaseTalk's fact-based modeling to refine the conceptual model
5. Generate documentation, UML, ORM, or other outputs

## See Also

- CaseTalk Documentation: https://www.casetalk.com
- Mendix Database Documentation
- JDBC Metadata API: https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html

## Support

For issues with:
- **Java Action**: Check Mendix logs for detailed error messages
- **CaseTalk import**: Validate JSON structure at https://jsonlint.com
- **Metadata extraction**: Check database permissions

---

**Version**: 1.0
**Author**: CaseTalk Integration Team
**Date**: 2025-01-06
