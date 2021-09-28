package com.irontechspace.dynamicdq.quartz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irontechspace.dynamicdq.configurator.save.model.SaveField;
import com.irontechspace.dynamicdq.configurator.save.model.SaveLogic;
import com.irontechspace.dynamicdq.executor.save.SaveService;
import com.irontechspace.dynamicdq.quartz.model.QuartzScheduleJob;
import lombok.extern.log4j.Log4j2;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.irontechspace.dynamicdq.exceptions.ExceptionUtils.logException;
import static com.irontechspace.dynamicdq.utils.Auth.DEFAULT_USER_ROLE;
import static org.quartz.CronScheduleBuilder.cronSchedule;

@Log4j2
@Service
public class QuartzService {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Scheduler scheduler;
    private final SaveService saveService;


    @Autowired
    public QuartzService(Scheduler scheduler, SaveService saveService){
        this.scheduler = scheduler;
        this.saveService = saveService;
//        try {
//            this.scheduler.getListenerManager().addSchedulerListener(new QuartzSchedulerListener(this));
//        } catch (SchedulerException e) {
//            logException(log, e);
//        }

    }

    public void getScheduleTasks(){

    }

    public void saveScheduleTask(QuartzScheduleJob job, UUID userId, List<String> userRoles){
        UUID id = (UUID) saveService.analysisLogic(null, job.getSaveLogic(), OBJECT_MAPPER.valueToTree(job), null, userId, userRoles, true);
    }

    public void deleteScheduleTask(){

    }

    private void createJob(QuartzScheduleJob job){
        try {
            job.setId(UUID.randomUUID().toString());
            JobDetail jobDetail = buildJobDetail(job);
            Trigger trigger = buildTrigger(job, jobDetail);
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            logException(log, e);
        }

    }

    // Формирование объекта задания
    private JobDetail buildJobDetail(QuartzScheduleJob job) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("task", job.getTask().toString());

        return JobBuilder.newJob(QuartzJob.class)
                .withIdentity(job.getId())
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    // Формирование объекта триггера
    private Trigger buildTrigger(QuartzScheduleJob job, JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(job.getId())
                .withSchedule(cronSchedule(job.getSchedule()))
                .build();
    }
}
