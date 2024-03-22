package com.itheima.prize.api.action;

import com.alibaba.fastjson.JSON;
import com.itheima.prize.api.config.LuaScript;
import com.itheima.prize.commons.config.RabbitKeys;
import com.itheima.prize.commons.config.RedisKeys;
import com.itheima.prize.commons.db.entity.*;
import com.itheima.prize.commons.db.mapper.CardGameMapper;
import com.itheima.prize.commons.db.service.CardGameService;
import com.itheima.prize.commons.utils.ApiResult;
import com.itheima.prize.commons.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/act")
@Api(tags = {"抽奖模块"})
public class ActController {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private LuaScript luaScript;

    @GetMapping("/go/{gameid}")
    @ApiOperation(value = "抽奖")
    @ApiImplicitParams({
            @ApiImplicitParam(name="gameid",value = "活动id",example = "1",required = true)
    })
    public ApiResult<Object> act(@PathVariable int gameid, HttpServletRequest request){
        //TODO
        return null;
    }

    @GetMapping("/info/{gameid}")
    @ApiOperation(value = "缓存信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name="gameid",value = "活动id",example = "1",required = true)
    })
    public ApiResult info(@PathVariable int gameid){
        //TODO
        Map<String,Object> cacheInfo = new HashMap<>();
        //获取活动基本信息
        CardGame game =  (CardGame) redisUtil.get(RedisKeys.INFO + gameid);
        cacheInfo.put("gameInfo",game);
        //从缓存中获取奖品相关的令牌桶
        String tokensKey = RedisKeys.TOKENS + gameid;
        List<Object> tokenStrings = redisUtil.lrange(tokensKey,0,-1);
        List<Long> tokens = new ArrayList<>();
        for (Object tokenString : tokenStrings){
            tokens.add((Long) tokenString);
        }
        cacheInfo.put("tokens",tokens);
        //将时间戳转换为日期类型
        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<Date,CardProduct> productDataMap = new HashMap<>();
        for (Long token : tokens){
            CardProduct product = (CardProduct) redisUtil.get(RedisKeys.TOKEN + gameid + "_" + token);
            if (product != null){
                Date tokenData = new Date(token/1000);
                productDataMap.put(tokenData,product);
            }
        }
        cacheInfo.put("productDateMap",productDataMap);
        //获取活动策略信息
        Map<Object,Object> maxGoals = redisUtil.hmget(RedisKeys.MAXGOAL + gameid);
        Map<Object,Object> maxEnters = redisUtil.hmget(RedisKeys.MAXENTER + gameid);
        cacheInfo.put("maxGoals",maxGoals);
        cacheInfo.put("maxEnters",maxEnters);
        return new ApiResult(1,"成功",cacheInfo);
    }
}
