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
