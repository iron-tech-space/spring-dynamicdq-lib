package com.irontechspace.dynamicdq.utils;

import com.irontechspace.dynamicdq.model.TypeSql;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

public class SqlUtils {

    public static String toSearchString(String value) {
        if (value == null) return "";
        return '%' + value.replace(' ', '%') + '%';
    }

    public static Map<String, Object> getParams(Object object) {
        BeanPropertySqlParameterSource objParams = new BeanPropertySqlParameterSource(object);
        return Stream.of(Objects.requireNonNull(objParams.getParameterNames()))
                .collect(HashMap::new, (m, v) -> m.put(v, objParams.getValue(v)), HashMap::putAll);
    }

    public static String generateCodeSql(String tableName){
        return String.format("(select coalesce(max(code) + 1, 1) from %s)", tableName);
    }

    public static String generateDeleteSql(String tableName, String whereField){
        return String.format("DELETE FROM %s WHERE %s=:%s ", tableName, whereField.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase(), whereField);
    }

    public static String getSqlForRecursiveDelete(String tableName, String whereFields){
        return getSqlForRecursive("DELETE", tableName, whereFields);
    }

    public static String getSqlForRecursiveSelect(String tableName, String whereFields, String primaryKey){
        return getSqlForRecursive("SELECT " + primaryKey, tableName, whereFields);
    }

    private static String getSqlForRecursive(String type, String tableName, String whereFields) {
        return String.format("%s FROM %s WHERE %s",
                type,
                tableName,
                whereFields);
    }

    public static <T> String generateSql(TypeSql type, String tableName, Class<T> clazz){
        return generateSql(type, tableName, clazz, "id", new ArrayList<>());
    }

    public static <T> String generateSql(TypeSql type, String tableName, Class<T> clazz, List<String> excludeFieldsNames){
        return generateSql(type, tableName, clazz, "id", excludeFieldsNames);
    }

    public static <T> String generateSql(TypeSql type, String tableName, Class<T> clazz, String primaryKey){
        return generateSql(type, tableName, clazz, primaryKey, new ArrayList<>());
    }

    public static <T> String generateSql(TypeSql type, String tableName, Class<T> clazz, String primaryKey, List<String> excludeFieldsNames){
        return generateSql(type, tableName, inspect(clazz), primaryKey, excludeFieldsNames);
    }

    public static <T> String generateSql(TypeSql type, String tableName, List<String> fieldsNames, String primaryKey, Boolean excludePrimaryKey){
        return generateSql(type, tableName, fieldsNames, primaryKey,
                excludePrimaryKey ? Collections.singletonList(primaryKey) : new ArrayList<>());
    }

    public static <T> String generateSql(TypeSql type, String tableName, List<String> fieldsNames, String primaryKey, List<String> excludeFieldsNames){
        switch (type) {
            case SELECT_BY_ID:
                String select_sql = "SELECT %s \n FROM %s \n WHERE %s=:%s;";
                List<String> select_fields = new ArrayList<>();
                for (String field : fieldsNames) {
                    if(!excludeFieldsNames.contains(field)){
                        select_fields.add(field.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase());
                    }
                }
                return String.format(
                        select_sql, // Строка запроса
                        String.join(", ", select_fields), // Поля select-a
                        tableName,  // Имя таблицы
                        primaryKey.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase(), primaryKey); // Условие выборки
            case INSERT:
                String insert_sql = "INSERT INTO %s \n ( %s ) \n VALUES( %s ) returning %s;";
                List<String> insert_fields = new ArrayList<>();
                List<String> insert_values = new ArrayList<>();
                for (String field : fieldsNames) {
                    //if (!field.equals("id") && !field.equals("fields")) {
                    if(!excludeFieldsNames.contains(field)){
                        insert_fields.add(field.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase());
                        insert_values.add(":" + field);
                    }
                }
                return String.format(
                        insert_sql, // Строка запроса
                        tableName,  // Имя таблицы
                        String.join(", ", insert_fields), // Поля инсерта
                        String.join(", ", insert_values), // Параметры инсерта
                        primaryKey.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase()); // Ключ который вернуть после выполнения
            case UPDATE:
                String update_sql = "UPDATE %s \n SET %s \n WHERE %s=:%s;";
                List<String> update_fields = new ArrayList<>();
                for(String field : fieldsNames){
//                    if(!field.equals("id") && !field.equals("fields"))
                    if(!excludeFieldsNames.contains(field))
                        update_fields.add(field.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase() + " = :" + field);
                }
                return String.format(
                        update_sql, // Строка запроса
                        tableName,  // Имя таблицы
                        String.join(", ", update_fields), // Параметры обновления
                        primaryKey.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase(), primaryKey); // Условие обновления
            default:
                return null;
        }
    }

    public static <T> List<String> inspect(Class<T> klazz) {
        List<Field> fields = getInheritedPrivateFields(klazz);
        List<String> names = new ArrayList<>();
        for (Field field : fields) {
            names.add(field.getName());
        }
        return names;
    }

    private static List<Field> getInheritedPrivateFields(Class<?> type) {
        List<Field> result = new ArrayList<Field>();
        Class<?> i = type;
        while (i != null && i != Object.class) {
            Collections.addAll(result, i.getDeclaredFields());
            i = i.getSuperclass();
        }
        return result;
    }

    public <T> T merge(T local, T remote) throws IllegalAccessException, InstantiationException {
        Class<?> clazz = local.getClass();
        Object merged = clazz.newInstance();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Object localValue = field.get(local);
            Object remoteValue = field.get(remote);
            field.set(merged, (remoteValue != null) ? remoteValue : localValue);
//            if (localValue != null) {
//                switch (localValue.getClass().getSimpleName()) {
//                    case "Default":
//                    case "Detail":
//                        field.set(merged, merge(localValue, remoteValue));
//                        break;
//                    default:
//                        field.set(merged, (remoteValue != null) ? remoteValue : localValue);
//                }
//            }
        }
        return (T) merged;
    }
}
