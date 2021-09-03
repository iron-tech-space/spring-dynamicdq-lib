package com.irontechspace.dynamicdq.utils;

import java.util.*;

public class Auth {

    public final static UUID DEFAULT_USER_ID = UUID.fromString("0be7f31d-3320-43db-91a5-3c44c99329ab");
    public final static List<String> DEFAULT_USER_ROLE = Collections.singletonList("ROLE_ADMIN");

    public static UUID getUserId (Map<String, String> headers){
        return UUID.fromString(headers.get("userid"));
    }

    public static String getStringUserRoles (Map<String, String> headers){
        return headers.get("userRoles").replace("[", "\'").replace("]", "\'").replace(", ", "\', \'");
    }

    public static List<String> getListUserRoles (Map<String, String> headers){
        String[] roles = headers.get("userRoles").replace("[", "").replace("]", "").split(", ");
        return Arrays.asList(roles);
    }


}
