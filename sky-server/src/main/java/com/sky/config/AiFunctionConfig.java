package com.sky.config;

import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Configuration
public class AiFunctionConfig {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    // ----- 函数入参 -----
    public record OrderRequest(String orderNumber) {}
    public record DishRequest(String keyword) {}
    public record UserOrdersRequest(Long userId) {}
    public record CategoryTypeRequest(Integer type) {}
    public record ShopStatusRequest() {}
    public record HotDishesRequest() {}

    // ----- 工具 1：根据订单号查订单 -----
    @Bean
    @Description("根据订单号查询订单状态、金额和详情")
    public Function<OrderRequest, String> queryOrderFunction() {
        return request -> {
            log.info("【AI函数调用】queryOrderFunction - 订单号: {}", request.orderNumber());
            Orders order = orderMapper.getByNumber(request.orderNumber());
            if (order == null) {
                log.warn("【AI函数调用】queryOrderFunction - 未找到订单: {}", request.orderNumber());
                return "未查询到单号为 " + request.orderNumber() + " 的订单，请确认订单号是否正确。";
            }
            log.info("【AI函数调用】queryOrderFunction - 查询成功: status={}, amount={}", order.getStatus(), order.getAmount());
            String statusStr = switch (order.getStatus()) {
                case 1 -> "待付款"; case 2 -> "待接单"; case 3 -> "已接单";
                case 4 -> "派送中"; case 5 -> "已完成"; case 6 -> "已取消";
                default -> "未知状态";
            };
            return String.format("订单号：%s，状态：%s，总金额：%.2f元，下单时间：%s，收货人：%s，地址：%s",
                    order.getNumber(), statusStr, order.getAmount().doubleValue(),
                    order.getOrderTime(), order.getConsignee(), order.getAddress());
        };
    }

    // ----- 工具 2：搜索菜品 -----
    @Bean
    @Description("根据菜品名称或关键字搜索菜品信息及价格，可用于推荐菜品")
    public Function<DishRequest, String> searchDishFunction() {
        return request -> {
            log.info("【AI函数调用】searchDishFunction - 关键词: {}", request.keyword());
            Dish query = new Dish();
            query.setName(request.keyword());
            query.setStatus(1);
            List<Dish> list = dishMapper.list(query);
            if (list == null || list.isEmpty()) {
                log.warn("【AI函数调用】searchDishFunction - 未找到菜品: {}", request.keyword());
                return "没有找到与「" + request.keyword() + "」相关的菜品，建议用户换个关键词试试。";
            }
            log.info("【AI函数调用】searchDishFunction - 找到 {} 个菜品", list.size());
            StringBuilder sb = new StringBuilder("为您找到以下菜品：\n");
            for (int i = 0; i < Math.min(list.size(), 5); i++) {
                Dish dish = list.get(i);
                sb.append(String.format("%d. %s — 价格：%.2f元，描述：%s\n",
                        i + 1, dish.getName(), dish.getPrice().doubleValue(),
                        dish.getDescription() != null ? dish.getDescription() : "暂无描述"));
            }
            return sb.toString();

        };
    }

    // ----- 工具 3：查用户最近订单 -----
    @Bean
    @Description("根据用户ID查询该用户最近的订单列表，用于回答用户关于'我的订单'、'订单到哪了'等问题")
    public Function<UserOrdersRequest, String> queryUserOrdersFunction() {
        return request -> {
            Long userId = request.userId();
            log.info("【AI函数调用】queryUserOrdersFunction - userId: {}", userId);
            if (userId == null) {
                return "请先登录后再查询订单哦~";
            }
            OrdersPageQueryDTO dto = new OrdersPageQueryDTO();
            dto.setUserId(userId);
            dto.setPage(1);
            dto.setPageSize(5);
            com.github.pagehelper.Page<Orders> page = orderMapper.pageQuery(dto);
            List<Orders> orders = page.getResult();
            if (orders == null || orders.isEmpty()) {
                log.warn("【AI函数调用】queryUserOrdersFunction - userId={} 无订单记录", userId);
                return "您目前还没有订单记录，快去下单吧！";
            }
            log.info("【AI函数调用】queryUserOrdersFunction - 找到 {} 条订单", orders.size());
            StringBuilder sb = new StringBuilder("您最近的订单如下：\n");
            for (int i = 0; i < orders.size(); i++) {
                Orders o = orders.get(i);
                String statusStr = switch (o.getStatus()) {
                    case 1 -> "待付款"; case 2 -> "待接单"; case 3 -> "已接单";
                    case 4 -> "派送中"; case 5 -> "已完成"; case 6 -> "已取消";
                    default -> "未知";
                };
                sb.append(String.format("%d. 订单号：%s，状态：%s，金额：%.2f元，时间：%s\n",
                        i + 1, o.getNumber(), statusStr, o.getAmount().doubleValue(), o.getOrderTime()));
            }
            return sb.toString();
        };
    }

    // ----- 工具 4：查询菜品分类 -----
    @Bean
    @Description("查询菜品分类列表，type=1为菜品分类，type=2为套餐分类")
    public Function<CategoryTypeRequest, String> queryCategoryListFunction() {
        return request -> {
            List<Category> categories = categoryMapper.list(request.type() != null ? request.type() : 1);
            if (categories == null || categories.isEmpty()) {
                return "暂无分类信息。";
            }
            StringBuilder sb = new StringBuilder("当前菜品分类如下：\n");
            for (Category c : categories) {
                if (c.getStatus() == 1) {
                    sb.append(String.format("- %s\n", c.getName()));
                }
            }
            return sb.toString();
        };
    }

    // ----- 工具 5：查询店铺营业状态 -----
    @Bean
    @Description("查询店铺当前的营业状态，返回店铺是否正在营业")
    public Function<ShopStatusRequest, String> queryShopStatusFunction() {
        return request -> {
            log.info("【AI函数调用】queryShopStatusFunction");
            Integer status = (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");
            if (status == null || status == 0) {
                return "店铺当前已打烊休息，暂时无法下单。营业期间欢迎您随时光临！";
            }
            return "店铺正在营业中，可以正常点餐下单！配送时间约30-45分钟。";
        };
    }

    // ----- 工具 6：查询热销菜品 -----
    @Bean
    @Description("查询当前热销菜品排行榜，用于向用户推荐热门菜品")
    public Function<HotDishesRequest, String> queryHotDishesFunction() {
        return request -> {
            log.info("【AI函数调用】queryHotDishesFunction");
            LocalDateTime begin = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MAX);
            List<GoodsSalesDTO> topList = orderMapper.getSalesTop10(begin, end);
            String range = "近7天";
            if (topList == null || topList.isEmpty()) {
                // 近7天无销量时兜底：统计全部历史订单的销量，避免演示环境数据太旧导致榜单为空
                topList = orderMapper.getSalesTop10(LocalDateTime.of(2000, 1, 1, 0, 0), end);
                range = "全部时间";
            }
            if (topList == null || topList.isEmpty()) {
                return "暂无任何热销数据，您可以浏览菜单挑选喜欢的菜品。";
            }
            StringBuilder sb = new StringBuilder(range + "热销菜品排行榜：\n");
            for (int i = 0; i < Math.min(topList.size(), 5); i++) {
                GoodsSalesDTO item = topList.get(i);
                sb.append(String.format("%d. %s — 已售%d份\n", i + 1, item.getName(), item.getNumber()));
            }
            return sb.toString();
        };
    }
}