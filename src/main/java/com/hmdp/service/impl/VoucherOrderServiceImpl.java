package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        //查询优惠券id
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);

        //判断优惠券秒杀是否开始，否，返回错误信息
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始");
        }
        //开始，判断库存是否充足，否，直接结束
        if(voucher.getStock() < 1){
            return Result.fail("库存不足");
        }
        //库存充足，扣减库存，创建订单，返回订单id
        // 1. 扣减库存（乐观锁，防止超卖）
        Long userId = UserHolder.getUser().getId();
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();

        if (count > 0) {
            return Result.fail("你已经购买过一次了");
        }
        // 2. 创建订单
        VoucherOrder order = new VoucherOrder();
        // 假设有 UserHolder 工具类获取当前用户id
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        // 订单id可分布式id生成器
        order.setId(redisIdWorker.nextId("order"));
        save(order);
        // 3. 返回订单id
        return Result.ok(order.getId());
    }
}
