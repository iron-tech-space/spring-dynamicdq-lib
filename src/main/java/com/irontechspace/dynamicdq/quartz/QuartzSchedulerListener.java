package com.irontechspace.dynamicdq.quartz;

import lombok.extern.log4j.Log4j2;
import org.quartz.*;

@Log4j2
public class QuartzSchedulerListener implements SchedulerListener {

    private QuartzService quartzService;

    public QuartzSchedulerListener(QuartzService quartzService){
        this.quartzService = quartzService;
    }

    @Override
    public void jobScheduled(Trigger trigger) {
        log.debug("jobScheduled");
    }

    @Override
    public void jobUnscheduled(TriggerKey triggerKey) {
        log.debug("jobUnscheduled");
    }

    @Override
    public void triggerFinalized(Trigger trigger) {
//        jobService.deleteJob(trigger.getJobKey());
        log.debug("triggerFinalized");
    }

    @Override
    public void triggerPaused(TriggerKey triggerKey) {
        log.debug("triggerPaused");
    }

    @Override
    public void triggersPaused(String triggerGroup) {
        log.debug("triggersPaused");
    }

    @Override
    public void triggerResumed(TriggerKey triggerKey) {
        log.debug("triggerResumed");
    }

    @Override
    public void triggersResumed(String triggerGroup) {
        log.debug("triggersResumed");
    }

    @Override
    public void jobAdded(JobDetail jobDetail) {
        log.debug("jobAdded");
    }

    @Override
    public void jobDeleted(JobKey jobKey) {
        log.debug("jobDeleted");
    }

    @Override
    public void jobPaused(JobKey jobKey) {
        log.debug("jobPaused");
    }

    @Override
    public void jobsPaused(String jobGroup) {
        log.debug("jobsPaused");
    }

    @Override
    public void jobResumed(JobKey jobKey) {
        log.debug("jobResumed");
    }

    @Override
    public void jobsResumed(String jobGroup) {
        log.debug("jobsResumed");
    }

    @Override
    public void schedulerError(String msg, SchedulerException cause) {
        log.debug("schedulerError");
    }

    @Override
    public void schedulerInStandbyMode() {
        log.debug("schedulerInStandbyMode");
    }

    @Override
    public void schedulerStarted() {
        log.debug("schedulerStarted");
    }

    @Override
    public void schedulerStarting() {
        log.debug("schedulerStarting");
    }

    @Override
    public void schedulerShutdown() {
        log.debug("schedulerShutdown");
    }

    @Override
    public void schedulerShuttingdown() {
        log.debug("schedulerShuttingdown");
    }

    @Override
    public void schedulingDataCleared() {
        log.debug("schedulingDataCleared");
    }
}
