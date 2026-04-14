package com.sky.controller.user;

import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端地址簿控制器
 * 提供收货地址的增删改查及默认地址设置功能
 */
@RestController
@RequestMapping("/user/addressBook")
@Slf4j

public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增收货地址
     *
     * @param addressBook 地址簿实体对象，包含收货人、电话、详细地址等信息
     * @return 操作结果
     */
    @PostMapping
    public Result add (@RequestBody AddressBook addressBook){
        log.info("新增地址：{}", addressBook);
        addressBookService.add(addressBook);
        return Result.success();
    }

    /**
     * 查询当前用户的所有收货地址
     *
     * @return 地址列表
     */
    @GetMapping("/list")
    public Result<List<AddressBook>> list(){
        log.info("查询地址");
        List<AddressBook> list = addressBookService.list();

        return Result.success(list);
    }

    /**
     * 查询当前用户的默认收货地址
     *
     * @return 默认地址信息，若不存在则返回错误提示
     */
    @GetMapping("/default")
    public Result<AddressBook> getDefault(){
        log.info("查询默认地址");
        AddressBook addressBook = addressBookService.getDefault();
        if(addressBook != null){
            return Result.success(addressBook);
        }
        return Result.error("没有默认地址");
    }

    /**
     * 修改收货地址信息
     *
     * @param addressBook 地址簿实体对象，需包含要修改的地址ID
     * @return 操作结果
     */
    @PutMapping
    public Result update(@RequestBody AddressBook addressBook){
        log.info("修改地址：{}",addressBook);
        addressBookService.update(addressBook);
        return Result.success();
    }

    /**
     * 删除指定的收货地址
     *
     * @param id 要删除的地址记录ID
     * @return 操作结果
     */
    @DeleteMapping
    public Result delete(@RequestParam Long id){
        log.info("删除地址{}",id);
        addressBookService.delete(id);
        return Result.success();
    }

    /**
     * 根据ID查询收货地址详情
     *
     * @param id 地址记录ID
     * @return 地址详细信息
     */
    @GetMapping("/{id}")
    public Result<AddressBook> get(@PathVariable Long id){
        log.info("查询地址{}",id);
        AddressBook addressBook = addressBookService.getById(id);
        return Result.success(addressBook);
    }

    /**
     * 设置默认收货地址
     * 会将原默认地址取消，并将指定地址设为默认
     *
     * @param addressBook 地址簿实体对象，需包含要设为默认的地址ID
     * @return 操作结果
     */
    @PutMapping("default")
    public Result setDefault(@RequestBody AddressBook addressBook){
        log.info("设置默认地址{}",addressBook);
        addressBookService.setDefault(addressBook);
        return Result.success();
    }

}
