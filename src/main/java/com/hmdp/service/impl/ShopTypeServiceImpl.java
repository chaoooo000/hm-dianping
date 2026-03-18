package com.hmdp.service.impl;

import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.Result;
import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final String SHOP_TYPE_KEY = "shop:type:list";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result queryTypeList() {
        // 1. 查询 Redis
        String json = stringRedisTemplate.opsForValue().get(SHOP_TYPE_KEY);
        if (json != null && !json.isEmpty()) {
            try {
                List<ShopType> typeList = objectMapper.readValue(json, new TypeReference<List<ShopType>>() {});
                return Result.ok(typeList);
            } catch (Exception e) {
                // 解析异常，降级查数据库
            }
        }
        // 2. 查数据库
        List<ShopType> typeList = this.query().orderByAsc("sort").list();
        if (typeList == null || typeList.isEmpty()) {
            return Result.ok();
        }
        try {
            // 3. 写入 Redis
            stringRedisTemplate.opsForValue().set(SHOP_TYPE_KEY, objectMapper.writeValueAsString(typeList));
        } catch (Exception e) {
            // 序列化异常，忽略缓存
        }
        return Result.ok(typeList);
    }
}
