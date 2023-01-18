package top.strelitzia.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.strelitzia.dao.IntegralMapper;
import top.strelitzia.dao.LookWorldMapper;
import top.strelitzia.dao.TarotMapper;
import top.strelitzia.dao.UserFoundMapper;
import top.strelitzia.service.APIService;
import top.strelitzia.service.LotteryService;
import top.strelitzia.service.RedPacketService;

import java.util.Date;

/**
 * @author wangzy
 * @Date 2020/12/8 15:53
 **/

@Component
@Slf4j
public class DayJob {
    //private static final Logger log = LoggerFactory.getLogger(DayCountJob.class);

    @Autowired
    private UserFoundMapper userFoundMapper;

    @Autowired
    private TarotMapper tarotMapper;

    @Autowired
    private IntegralMapper integralMapper;

    @Autowired
    private LookWorldMapper lookWorldMapper;

    @Autowired
    private APIService apiService;

    @Autowired
    private LotteryService lotteryService;

    @Autowired
    private RedPacketService redPacketService;

    public DayJob() {
    }

    //每天凌晨四点重置抽卡次数
    @Scheduled(cron = "${scheduled.cleanJob}")
    @Async
    public void cleanDayCountJob() {
        userFoundMapper.cleanTodayCount();
        log.info("{}每日抽卡数清空", new Date());
    }

    //每天零点执行的任务
    @Scheduled(cron = "${scheduled.dayJob}")
    @Async
    public void dayJob() {
        integralMapper.cleanSignCount();
        tarotMapper.cleanTarotCount();
        lookWorldMapper.updateTime();
        log.info("{}每日零点任务执行完成", new Date());
    }

    @Scheduled(cron = "${scheduled.picJob}")
    @Async
    public void picJob() {
        apiService.lookWorldWord();
        apiService.lookWorldPic();
        log.info("{}每日看世界获取完成", new Date());
    }

    //每月一号零点执行的任务
    @Scheduled(cron = "${scheduled.monthJob}")
    @Async
    public void monthJob() {
        redPacketService.clean();
        lotteryService.pullLottery();
        integralMapper.cleanThisMonth();
        log.info("{}每月一号零点任务清空", new Date());
    }


}
