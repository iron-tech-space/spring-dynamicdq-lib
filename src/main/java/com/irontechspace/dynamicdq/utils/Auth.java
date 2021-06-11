package com.irontechspace.dynamicdq.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


public class Auth {

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
