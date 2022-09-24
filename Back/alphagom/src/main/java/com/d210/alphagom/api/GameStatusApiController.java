package com.d210.alphagom.api;

import com.d210.alphagom.api.dto.*;
import com.d210.alphagom.domain.condition.RankCondition;
import com.d210.alphagom.domain.entity.GameStatus;
import com.d210.alphagom.domain.entity.User;
import com.d210.alphagom.domain.service.GameStatusService;
import com.d210.alphagom.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/be")
@RequiredArgsConstructor
public class GameStatusApiController {

    private final GameStatusService gameStatusService;

    private final UserService userService;

    private final RedisTemplate redisTemplate;

    @PostMapping("score/user/{userId}")
    public ResponseEntity<?> saveGameStatus(@PathVariable Long userId, @RequestBody GameStatusRequest request) {
        log.info("{}유저가 {}게임에 {}점수 저장", userId, request.getGameTag(), request.getScore());
        User user = userService.findUser(userId);
        gameStatusService.saveScore(request.getScore(), request.getGameTag(), user);
        return ResponseEntity.ok(new ResponseDTO("점수가 저장되었습니다."));
    }

    @GetMapping("rank/{tag}")
    public ResponseEntity<?> getRank(@PathVariable String tag) {
        log.info("{} 게임의 순위 리스트", tag);
        String key = tag;
        ZSetOperations<String, String> stringStringZSetOperations = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringStringZSetOperations.reverseRangeWithScores(key, 0, Long.MAX_VALUE);
        List<RankResponse> collect = typedTuples.stream()
                .map(o -> new RankResponse(key, userService.findUserNickName(Long.parseLong(o.getValue())), o.getScore().intValue()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(collect);
    }

    @GetMapping("rank/user/{userId}/{tag}")
    public ResponseEntity<?> getUserRank(@PathVariable Long userId, @PathVariable String tag) {
        log.info("{} 게임의 {} 유저의 순위", tag, userId);

        String key = tag;
        Double score = redisTemplate.opsForZSet().score(key, userId.toString());
        Long rank = redisTemplate.opsForZSet().reverseRank(key, userId.toString());

        return ResponseEntity.ok(new RankResponse(key, userService.findUserNickName(userId), score.intValue(), rank + 1));
    }
}
