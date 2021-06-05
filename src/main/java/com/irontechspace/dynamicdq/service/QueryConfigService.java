package com.irontechspace.dynamicdq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irontechspace.dynamicdq.model.Query.QueryConfig;
import com.irontechspace.dynamicdq.model.Query.QueryField;
import com.irontechspace.dynamicdq.repository.QueryConfigRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class QueryConfigService extends ConfigService<QueryConfig> {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public QueryConfigService(QueryConfigRepository queryConfigRepository) {
        super(queryConfigRepository);
    }

    @Scheduled(fixedRateString = "${dynamicdq.caching-configs.delay:5000}")
    private void loadConfigs(){
        if(cachingConfigs)
            loadConfigs("Query configs:");
        else
            log.info("#> NO load query configs");
    }

    public ObjectNode getShortByName(String configName, UUID userId, List<String> userRoles){
        return convertQueryConfigToUI(getByName(configName, userId, userRoles));
    }

    public ObjectNode getShortByName(DataSource dateSource, String configName, UUID userId, List<String> userRoles){
        return convertQueryConfigToUI(getByName(dateSource, configName, userId, userRoles));
    }

    private ObjectNode convertQueryConfigToUI(QueryConfig config){
        ObjectNode uiConfig = OBJECT_MAPPER.createObjectNode();

        uiConfig.put("hierarchical", config.getHierarchical());
        uiConfig.put("hierarchyField", config.getHierarchyField());
        uiConfig.put("hierarchyView", config.getHierarchyView());
        uiConfig.put("hierarchyLazyLoad", config.getHierarchyLazyLoad());

        ArrayNode fields = OBJECT_MAPPER.createArrayNode();
        for(QueryField queryField : config.getFields()){
            ObjectNode uiField = OBJECT_MAPPER.createObjectNode();
            uiField.put("name", queryField.getName());
            uiField.put("alias", queryField.getAlias());
            uiField.put("aliasOrName", queryField.getAliasOrName());
            uiField.put("position", queryField.getPosition());
            uiField.put("header", queryField.getHeader());
            uiField.put("visible", queryField.getVisible());
            uiField.put("resizable", queryField.getResizable());
            uiField.put("sortable", queryField.getSortable());
            uiField.put("align", queryField.getAlign());
            uiField.put("width", queryField.getWidth());
            uiField.put("defaultSort", queryField.getDefaultSort());
            uiField.put("defaultFilter", queryField.getDefaultFilter());
            fields.add(uiField);
        }
        uiConfig.set("fields", fields);
        return uiConfig;
    }
}
