package top.strelitzia.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.strelitzia.service.UpdateDataService;

/**
 * @author wangzy
 * @Date 2020/12/8 15:53
 **/

@Component
public class UpdateJob {
    private static final Logger log = LoggerFactory.getLogger(UpdateJob.class);
    @Autowired
    private UpdateDataService updateDataService;


    //每天凌晨四点重置抽卡次数
    @Scheduled(cron = "${scheduled.updateJob}")
    @Async
    public void cleanDayCountJob() {
        updateDataService.downloadDataFile(false);
    }
}
