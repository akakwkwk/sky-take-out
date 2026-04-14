package com.sky.mapper;

import com.sky.entity.AddressBook;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 地址簿数据访问层接口
 * 提供收货地址的数据库操作方法
 */
@Mapper
public interface AddressBookMapper {

    /**
     * 插入新的收货地址记录
     *
     * @param addressBook 地址簿实体对象，包含用户ID、收货人、电话等完整信息
     */
    @Insert("insert into address_book" +
            "        (user_id, consignee, phone, sex, province_code, province_name, city_code, city_name, district_code," +
            "         district_name, detail, label, is_default)" +
            "        values (#{userId}, #{consignee}, #{phone}, #{sex}, #{provinceCode}, #{provinceName}, #{cityCode}, #{cityName}," +
            "                #{districtCode}, #{districtName}, #{detail}, #{label}, #{isDefault})")
    void insert(AddressBook addressBook);

    /**
     * 查询指定用户的所有收货地址列表
     *
     * @param currentId 用户ID
     * @return 地址列表
     */
    @Select("select * from address_book where user_id=#{currentId}")
    List<AddressBook> list(Long currentId);

    /**
     * 查询指定用户的默认收货地址
     *
     * @param addressBook 地址簿实体对象，需包含用户ID
     * @return 默认地址对象，若不存在则返回null
     */
    @Select("select * from address_book where user_id=#{userId} and is_default=1")
    AddressBook getDefault(AddressBook addressBook);

    /**
     * 更新收货地址信息
     * SQL定义在对应的XML映射文件中
     *
     * @param addressBook 地址簿实体对象，需包含要更新的地址ID及修改后的字段
     */
    void update(AddressBook addressBook);

    /**
     * 删除指定的收货地址记录
     *
     * @param id 地址记录的主键ID
     */
    @Delete("delete from address_book where id=#{id}")
    void delete(Long id);

    /**
     * 根据ID查询收货地址详情
     *
     * @param id 地址记录的主键ID
     * @return 地址详细信息
     */
    @Select("select * from address_book where id= #{id}")
    AddressBook getById(Long id);

    /**
     * 批量更新指定用户所有地址的默认状态
     * 用于设置默认地址时先将其他地址全部取消默认
     *
     * @param addressBook 地址簿实体对象，需包含用户ID和默认状态值
     */
    @Update("update address_book set is_default = #{isDefault} where user_id = #{userId}")
    void updateIsDefaultByUserId(AddressBook addressBook);
}
