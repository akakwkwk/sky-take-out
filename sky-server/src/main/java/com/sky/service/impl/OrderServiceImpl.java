package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.*;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 订单服务实现类
 * 提供订单提交、支付、查询、取消等核心业务功能
 */
@Service
@Slf4j

public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户提交订单
     * 该方法会验证收货地址和购物车数据，创建订单及订单详情，并清空购物车
     *
     * @param ordersSubmitDTO 订单提交数据传输对象，包含地址ID、支付方式等信息
     * @return OrderSubmitVO 订单提交结果视图对象，包含订单ID、订单号、下单时间和订单金额
     * @throws AddressBookBusinessException  当收货地址不存在时抛出
     * @throws ShoppingCartBusinessException 当购物车为空时抛出
     */
    @Transactional
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

        // 验证收货地址是否存在
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 获取当前登录用户ID并查询购物车列表
        Long uesrId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(uesrId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 构建订单对象并设置基本信息
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(BaseContext.getCurrentId());
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));//时间戳订单号
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(uesrId);

        String fullAddress = addressBook.getProvinceName()
                + addressBook.getCityName()
                + addressBook.getDistrictName()
                + addressBook.getDetail();
        orders.setAddress(fullAddress);//详细地址 管理端订单搜索时使用

        // 插入订单主表数据
        orderMapper.insert(orders);

        // 根据购物车列表批量创建订单详情
        List<OrderDetail> orderDetailList = new ArrayList<>();

        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//订单id
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);//批量插入订单详情数据

        // 清空当前用户的购物车
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());//清空购物车数据


        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    /**
     * 订单支付
     * 调用微信支付接口生成预支付交易单（当前为模拟实现）
     *
     * @param ordersPaymentDTO 订单支付数据传输对象，包含订单号等支付信息
     * @return OrderPaymentVO 订单支付视图对象，包含支付所需参数
     * @throws Exception 支付过程中的异常
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 获取当前登录用户信息
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        /*//调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );*/

        // 模拟微信支付返回结果
        JSONObject jsonObject = new JSONObject();

        // 检查订单是否已支付
        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功后修改订单状态
     * 将订单状态更新为待确认，支付状态更新为已支付，并记录结账时间
     *
     * @param outTradeNo 商户订单号，用于查询和更新订单
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单信息
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 构建订单更新对象，更新订单状态、支付状态和结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        Map map =new HashMap();
        map.put("type",1);
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号"+outTradeNo);

        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);//发送消息给客户端
    }

    /**
     * 用户端订单分页查询
     * 根据页码、每页大小和订单状态查询当前用户的订单列表，并关联查询订单详情
     *
     * @param page     页码，从1开始
     * @param pageSize 每页显示条数
     * @param status   订单状态筛选条件，可为null查询所有状态
     * @return PageResult 分页结果对象，包含总记录数和订单视图列表
     */
    @Override
    public PageResult pageQuery(Integer page, Integer pageSize, Integer status) {
        // 开启分页查询
        PageHelper.startPage(page, pageSize);//开启分页

        // 构建查询参数，设置当前用户ID和订单状态
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();//封装查询参数,封装当前登录用户的id,订单状态
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        // 执行分页查询
        Page<Orders> pagedQuery = orderMapper.pageQuery(ordersPageQueryDTO);//查询分页后的结果

        // 组装订单视图对象列表，并关联查询每个订单的详情信息
        List<OrderVO> list = new ArrayList<>();//创建OrderVO集合，用于存放查询后的结果,这里用VO没问题
        if (pagedQuery != null && !pagedQuery.isEmpty()) {
            for (Orders orders : pagedQuery) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                // 获取订单详情，并确保不为 null
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());//这里要用details!!不然会报空指针异常
                orderVO.setOrderDetailList(orderDetails != null ? orderDetails : new ArrayList<>());//判断订单详情是否为 null，为 null 则创建一个空的订单详情列表
                list.add(orderVO);
            }
        }
        return new PageResult(pagedQuery.getTotal(), list);
    }

    /**
     * 查询订单详情
     * 根据订单ID查询订单基本信息和订单详情列表
     *
     * @param id 订单ID
     * @return OrderVO 订单视图对象，包含订单基本信息和订单详情列表
     */
    @Override
    public OrderVO getOrderDetail(Long id) {
        // 查询订单基本信息
        Orders orders = orderMapper.getById(id);

        // 查询订单详情列表
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);//获取订单详情

        // 组装订单视图对象
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 用户取消订单
     * 只有待付款或待接单状态的订单可以取消，取消后更新订单状态、取消原因和取消时间
     *
     * @param id 订单ID
     * @throws OrderBusinessException 当订单不存在或订单状态不允许取消时抛出
     */
    @Override
    public void userCancelById(Long id) {
        // 查询订单信息
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 已取消则直接返回（幂等）
        if (Objects.equals(orders.getStatus(), Orders.CANCELLED)) {
            return;
        }

        // 只有待付款(1)或待接单(2)状态可以取消
        if (!Objects.equals(orders.getStatus(), Orders.PENDING_PAYMENT) && !Objects.equals(orders.getStatus(), Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        /*// 已支付的订单不允许取消
        if (Objects.equals(orders.getPayStatus(), Orders.PAID)) {
            throw new OrderBusinessException(MessageConstant.ORDER_PAID);
        }*/

        // 更新订单取消信息
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * 将指定订单的商品重新添加到购物车
     *
     * @param id 订单ID
     */
    @Override
    public void repetition(Long id) {

        // 查询订单详情列表
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);//获取订单详情
        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }


    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQuery) {
        PageHelper.startPage(ordersPageQuery.getPage(), ordersPageQuery.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQuery);//查询分页后的结果
        List<OrderVO> orderVOList = new ArrayList<>();//创建OrderVO集合，用于存放查询后的结果

        //前端期望的菜品字段是 orderDishes（字符串格式，例如 “宫保鸡丁2；鱼香肉丝1；”），而不是 orderDetailList（对象数组）!!
        if (page != null && !page.isEmpty()) {
            for (Orders orders : page) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDishes(getOrderDishes(orders));
                orderVOList.add(orderVO);
            }
        }


        return new PageResult(page.getTotal(), orderVOList);
    }


    private String getOrderDishes(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());//创建一个流，将每条订单菜品信息转换为字符串，并拼接在一起 直接用黑马的代码

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);

    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(orderMapper.countStatus(Orders.TO_BE_CONFIRMED));
        orderStatisticsVO.setConfirmed(orderMapper.countStatus(Orders.CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS));
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /*商家拒单其实就是将订单状态修改为“已取消”
      只有订单处于“待接单”状态时可以执行拒单操作
      商家拒单时需要指定拒单原因
      商家拒单时，如果用户已经完成了支付，需要为用户退款*/
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        if (!Objects.equals(ordersDB.getStatus(), Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }//订单状态不是待接单，则不能拒单

        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())//拒单原因
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);

        //如果用户已经完成支付，需要退款
        // 处理退款（模拟）
        if (Objects.equals(ordersDB.getPayStatus(), Orders.PAID)) {
            // 模拟退款成功，记录日志
            log.info("模拟退款成功：订单号={}，金额={}", ordersDB.getNumber(), ordersDB.getAmount());

            // 如果需要，可更新支付状态为“退款”
            Orders refundUpdate = Orders.builder()
                    .id(ordersDB.getId())
                    .payStatus(Orders.REFUND)
                    .build();
            orderMapper.update(refundUpdate);
        }
    }
        /** 取消订单其实就是将订单状态修改为“已取消”
         * 商家取消订单时需要指定取消原因
         * 商家取消订单时，如果用户已经完成了支付，需要为用户退款*/
    @Override
    public void orderCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        
        // 已取消或已完成的订单不允许再次取消
        if (Objects.equals(ordersDB.getStatus(), Orders.CANCELLED)) {
            log.warn("订单已取消，无需重复操作，订单ID：{}", ordersCancelDTO.getId());
            return;
        }
        if (Objects.equals(ordersDB.getStatus(), Orders.COMPLETED)) {
            throw new OrderBusinessException("已完成订单无法取消");
        }
        
        // 只有待付款、待接单、已接单状态可以取消
        if (!Objects.equals(ordersDB.getStatus(), Orders.PENDING_PAYMENT) 
                && !Objects.equals(ordersDB.getStatus(), Orders.TO_BE_CONFIRMED)
                && !Objects.equals(ordersDB.getStatus(), Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
                orderMapper.update(orders);
                //如果用户已经完成支付，需要退款
        if (Objects.equals(ordersDB.getPayStatus(), Orders.PAID)) {
            // 模拟退款成功，记录日志
            log.info("模拟退款成功：订单号={}，金额={}", ordersDB.getNumber(), ordersDB.getAmount());
            // 如果需要，可更新支付状态为"退款"
            Orders refundUpdate = Orders.builder()
                    .id(ordersDB.getId())
                    .payStatus(Orders.REFUND)
                    .build();
                    orderMapper.update(refundUpdate);
        }
    }

    /** 派送订单其实就是将订单状态修改为“派送中”
     * 只有状态为“待派送”的订单可以执行派送订单操作*/
    @Override
    public void delivery(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if (!Objects.equals(ordersDB.getStatus(), Orders.CONFIRMED)) {//订单状态不是待派送，则不能派送
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
                orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if (!Objects.equals(ordersDB.getStatus(), Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
                orderMapper.update(orders);
    }
}
