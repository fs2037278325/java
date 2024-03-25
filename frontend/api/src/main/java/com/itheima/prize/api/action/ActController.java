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
        Map resMap = new LinkedHashMap();
        Map tokenMap = new LinkedHashMap();

        Object gameInfo = redisUtil.get(RedisKeys.INFO + gameid);
        Map<Object, Object> maxGoalMap = redisUtil.hmget(RedisKeys.MAXGOAL + gameid);
        Map<Object, Object> maxEnterMap = redisUtil.hmget(RedisKeys.MAXENTER + gameid);
        List<Object> tokenList = redisUtil.lrange(RedisKeys.TOKENS + gameid,0,-1);

        resMap.put(RedisKeys.INFO + gameid, gameInfo);
        resMap.put(RedisKeys.MAXGOAL + gameid, maxGoalMap);
        resMap.put(RedisKeys.MAXENTER + gameid, maxEnterMap);
        for (Object item:tokenList){
            Object o = redisUtil.get(RedisKeys.TOKEN + gameid + "_" + item.toString());
            Long key = Long.valueOf(item.toString());
            Date date = new Date(key/1000);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String formattedDate = dateFormat.format(date);
            tokenMap.put(formattedDate,o);
        }
        resMap.put(RedisKeys.TOKENS + gameid, tokenMap);
        return new ApiResult(200,"缓存信息",resMap);
    }
}
