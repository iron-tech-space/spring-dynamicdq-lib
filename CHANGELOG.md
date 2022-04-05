## 0.0.46

- [ExportExcel] Add def value cell

## 0.0.45

- [ExportExcel] Fix col offset
- [ExportExcel] Many cell styles
- [ExportExcel] Split align header and body for table
- [ExportExcel] Add set WrapText

## 0.0.43

- Fix border style for export to excel (RegionUtil.setBorder - very long time executing)

## 0.0.42

- Fix write SystemEvents to some DB

## 0.0.41

- Add exit by exception from task
- Add `resolveOutputData` for events in task
- Add `taskVars` with general task info
- Refactor exception handler
- Refactor file service log

## 0.0.40

- Add visible to ExcelCol
- Add cellFormula to ExcelCol
- Add merge configFields and manual field 

## 0.0.39

- Fix JS template

## 0.0.38

- Add log http request
- Fix def task body and Fix set output
- Add read write array
- Add execute JS code
- Add props to `addTableExcel` operation 
  - hiddenHeader 
  - headerHeight 
  - rowHeight 
  - dateFormat
- Remove ActiveMQ
- Fix log exception
- Add excel style

## 0.0.37

- Refactor struct project
- Add rabbit tasks
- Add multi operations tasks
- Add System Events

## 0.0.32

- Add `json` type data for SET data
- Fix GET timestamp type

## 0.0.31

- Add `json` type data for GET data

## 0.0.30

- Fix set params `userId` and `userRoles` in **saveService**

## 0.0.29

- Change login add fields to get sourceFields
- Change idConfig to configId for `ConfigField`
- Change type to typeData for `ConfigField`
- Add typeField for `ConfigField`

## 0.0.28

- Remove math from aggregateTypes

## 0.0.27

- Fix build.gradle

## 0.0.26

- Fix set **userId** and **userRoles** in params in **saveService**

## 0.0.25

- Add userId and userRoles in saveService

## 0.0.24

- Change **query** to **queryForList** in func **getTable** in `DataRepository`
- Add **having** in `getFlatData`
- Add **aggregate** and **math** type fields in `getFlatData`
- Add getTypePK by `Logic` and return type `Class` in `SaveDataService`
- Add **aggregate** and **math** in `TypeConverter`

For use **aggregate** or **math** need add aggregate function in `name`

Examples:

* Count for **root** table - `count({0}.id)`
* Count for **join** table - `count({1}.id)`
* The custom field for **join** table - `CASE WHEN count({1}.id) = 0 THEN 0 ELSE 1 END`
* Math custom field - `{0}.duration * 2 + {0}.position`

## 0.0.23

- Change userId to UUID

## 0.0.22

- Add userRoles to SaveConfigs

## 0.0.21

- Add PSQLException

## 0.0.20

- Fix array filter

## 0.0.19

- Add `userId` and `userRoles` in sql params
- Add `JsonConverter` for upload files
- Refactor code

## 0.0.18

- Add typeConverter
- Add field type `password`

## 0.0.17

- Add ForbiddenException and NotFoundException
- Add sharedForRoles to SaveConfig
- Add new throw if NotFound or Forbidden GetConfig and SaveConfig

## 0.0.16

- Add ControllerAdvice
- Add sharedForRoles for configs
- Fix parentResult bug

## 0.0.15

- Change position save fileInfo to DB. Set save after write to file system.

## 0.0.14

- Add `CHANGELOG.md`
- Change properties names
- Add FileService (upload and download)
