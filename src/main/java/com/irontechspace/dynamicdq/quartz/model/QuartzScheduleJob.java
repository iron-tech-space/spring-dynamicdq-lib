package com.irontechspace.dynamicdq.quartz.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.irontechspace.dynamicdq.configurator.query.model.QueryConfig;
import com.irontechspace.dynamicdq.configurator.save.model.SaveField;
import com.irontechspace.dynamicdq.configurator.save.model.SaveLogic;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class QuartzScheduleJob {

    private String id;
    private String schedule;
    private JsonNode task;

//    public QueryConfig getQueryConfig () {
//        // fields / getTableName / - getHierarchical / getLoggingQueries / getConfigName / - getCustomSql
//        //     "name": "id",
//        //    "alias": "id",
//        //    "typeData": "uuid",
//        //    "visible": false,
//        //    "typeField": "column"
//
//        //    5469 4000 5860 7206 Алиса Алексеевна
//    }

    public SaveLogic getSaveLogic() {
        List<SaveField> fields = new ArrayList<>();
        fields.add(SaveField.builder().name("id").type("uuid").build());
        fields.add(SaveField.builder().name("schedule").type("text").build());
        fields.add(SaveField.builder().name("task").type("json").build());
        return SaveLogic.builder()
                .fieldType("root")
                .primaryKey("id")
                .tableName("")
                .excludePrimaryKey(false)
                .autoGenerateCode(false)
                .fields(fields).build();
    }
}
