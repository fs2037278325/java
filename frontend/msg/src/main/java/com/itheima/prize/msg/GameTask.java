package com.itheima.prize.msg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itheima.prize.commons.config.RedisKeys;
import com.itheima.prize.commons.db.entity.*;
import com.itheima.prize.commons.db.service.CardGameProductService;
import com.itheima.prize.commons.db.service.CardGameRulesService;
import com.itheima.prize.commons.db.service.CardGameService;
import com.itheima.prize.commons.db.service.GameLoadService;
import com.itheima.prize.commons.utils.RedisUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 活动信息预热，每隔1分钟执行一次
 * 查找未来1分钟内（含），要开始的活动
 */
@Component
public class GameTask {
    private final static Logger log = LoggerFactory.getLogger(GameTask.class);
    @Autowired
    private CardGameService gameService;
    @Autowired
    private CardGameProductService gameProductService;
    @Autowired
    private CardGameRulesService gameRulesService;
    @Autowired
    private GameLoadService gameLoadService;
    @Autowired
    private RedisUtil redisUtil;

    @Scheduled(cron = "0 * * * * ?")
    public void execute() {
        System.out.printf("scheduled!"+new Date());
        //TODO
        //1.查询未来1分钟内要开始的活动那个
        //1.查询未来1分钟内要开始的活动那个
        Date now = new Date();
        List<CardGame> games = gameService.list(new QueryWrapper<CardGame>()
                .ge("startTime", now)
                .le("startTime", DateUtils.addMinutes(now, 1)));
        for (CardGame game : games){
            Date endtime = game.getEndtime();
            long validDuration = endtime.getTime()-now.getTime();//活动剩余有效时长
            //2.缓存活动基本信息
            redisUtil.set(RedisKeys.INFO + game.getId(),game,-1);
            //3.查询活动相关的奖品列表及数量
            List<CardGameProduct> products = gameProductService.list(new QueryWrapper<CardGameProduct>()
                    .eq("gameid", game.getId()));
            List<Long> tokenList = generateTokens(products.size());//生成令牌列表
            //4.缓存奖品相关的令牌桶
            redisUtil.rightPushAll(RedisKeys.TOKENS + game.getId(),tokenList);
            redisUtil.expire(RedisKeys.TOKENS + game.getId(),validDuration);
            //5.缓存奖品映射信息
            List<CardProductDto> cardProductDtos = gameLoadService.getByGameId(game.getId());
            for(int i=0;i<products.size();i++){
                Long token = tokenList.get(i);//获取唯一的令牌
                CardProductDto cardProductDto = cardProductDtos.get(i);
                redisUtil.set(RedisKeys.TOKENS + game.getId() + "_" + token,cardProductDto, validDuration);
            }
            //查询活动相关的活动策略并放进Redis
            List<CardGameRules> rulesList = gameRulesService.list(new QueryWrapper<CardGameRules>().eq("gameid",
                    game.getId()));
            for (CardGameRules rule : rulesList){
                redisUtil.hset(RedisKeys.MAXGOAL + game.getId(),String.valueOf(rule.getUserlevel()),rule.
                        getGoalTimes(),validDuration);
                redisUtil.hset(RedisKeys.MAXENTER + game.getId(),String.valueOf(rule.getUserlevel()),rule.
                        getEnterTimes(),validDuration);
            }
        }
        log.info("Scheduled task completed:"+new Date());
    }

    private List<Long> generateTokens(int size) {
        List<Long> tokenList = new ArrayList<>();
        long start = System.currentTimeMillis();
        long end = start+60000;//1分钟后的时间戳
        for (int i = 0;i<size;i++){
            long duration = end -start;
            long rnd = start + new Random().nextInt(999);
            long token = rnd * 1000 + new Random().nextInt(999);
            tokenList.add(token);
        }
        return tokenList;
    }
}
