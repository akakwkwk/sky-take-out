package com.sky.mapper;

import com.sky.entity.AddressBook;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AddressBookMapper {

    @Insert("insert into address_book" +
            "        (user_id, consignee, phone, sex, province_code, province_name, city_code, city_name, district_code," +
            "         district_name, detail, label, is_default)" +
            "        values (#{userId}, #{consignee}, #{phone}, #{sex}, #{provinceCode}, #{provinceName}, #{cityCode}, #{cityName}," +
            "                #{districtCode}, #{districtName}, #{detail}, #{label}, #{isDefault})")
    void insert(AddressBook addressBook);

    @Select("select * from address_book where user_id=#{currentId}")
    List<AddressBook> list(Long currentId);

    @Select("select * from address_book where user_id=#{userId} and is_default=1")
    AddressBook getDefault(AddressBook addressBook);

    void update(AddressBook addressBook);

    @Delete("delete from address_book where id=#{id}")
    void delete(Long id);

    @Select("select * from address_book where id= #{id}")
    AddressBook getById(Long id);

    @Update("update address_book set is_default = #{isDefault} where user_id = #{userId}")
    void updateIsDefaultByUserId(AddressBook addressBook);
}
