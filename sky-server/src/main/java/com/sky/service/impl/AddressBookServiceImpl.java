package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地址簿服务实现类
 * 提供收货地址的业务逻辑处理
 */
@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {
    @Autowired
    private AddressBookMapper addressBookMapper;


    /**
     * 新增收货地址
     * 自动关联当前登录用户ID，新地址默认不为默认地址
     *
     * @param addressBook 地址簿实体对象
     */
    @Override
    public void add(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    /**
     * 查询当前用户的所有收货地址列表
     *
     * @return 地址列表
     */
    @Override
    public List<AddressBook> list() {
        return addressBookMapper.list(BaseContext.getCurrentId());
    }

    /**
     * 查询当前用户的默认收货地址
     *
     * @return 默认地址对象，若不存在则返回null
     */
    @Override
    public AddressBook getDefault() {
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());

        return addressBookMapper.getDefault(addressBook);
    }

    /**
     * 修改收货地址信息
     * 自动关联当前登录用户ID
     *
     * @param addressBook 地址簿实体对象，需包含要修改的地址ID
     */
    @Override
    public void update(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.update(addressBook);
    }

    /**
     * 删除指定的收货地址
     *
     * @param id 要删除的地址记录ID
     */
    @Override
    public void delete(Long id) {
        addressBookMapper.delete(id);
    }

    /**
     * 根据ID查询收货地址详情
     *
     * @param id 地址记录ID
     * @return 地址详细信息
     */
    @Override
    public AddressBook getById(Long id) {
        return addressBookMapper.getById(id);
    }

    /**
     * 设置默认收货地址
     * 采用事务保证数据一致性：先将该用户所有地址设为非默认，再将指定地址设为默认
     *
     * @param addressBook 地址簿实体对象，需包含要设为默认的地址ID和用户ID
     */
    @Transactional
    public void setDefault(AddressBook addressBook) {
        //1、将当前用户的所有地址修改为非默认地址 update address_book set is_default = ? where user_id = ?
        addressBook.setIsDefault(0);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.updateIsDefaultByUserId(addressBook);

        //2、将当前地址改为默认地址 update address_book set is_default = ? where id = ?
        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook);
    }
}
