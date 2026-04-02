package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理控制器（管理端）
 */
@RestController
@RequestMapping("/admin/category")
@Slf4j
public class CategotyController {
    @Autowired
    CategoryService categoryService;

    /**
     * 新增分类
     *
     * @param categoryDTO 分类数据传输对象，包含分类名称、类型等信息
     * @return Result 操作结果
     */
    @PostMapping
    public Result save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增分类：{}",categoryDTO);
        categoryService.save(categoryDTO);
        return Result.success();
    }

    /**
     * 分页查询分类
     *
     * @param categoryPageQueryDTO 分页查询参数，包含页码、每页记录数、分类名称等
     * @return Result<PageResult> 分页查询结果
     */
    @GetMapping("/page")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页查询：{}",categoryPageQueryDTO);
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据 ID 删除分类
     *
     * @param id 要删除的分类 ID
     * @return Result 操作结果
     */
    @DeleteMapping
    public Result deleteById(Long id){
        log.info("删除分类：{}",id);
        categoryService.deleteById(id);
        return Result.success();
    }

    /**
     * 修改分类信息
     *
     * @param categoryDTO 分类数据传输对象，包含分类 ID、名称、类型、排序等信息
     * @return Result 操作结果
     */
    @PutMapping
    public Result update(@RequestBody CategoryDTO categoryDTO){
        log.info("修改分类：{}",categoryDTO);
        categoryService.update(categoryDTO);
        return Result.success();
    }

    /**
     * 启用或停用分类
     *
     * @param status 状态码，1 表示启用，0 表示停用
     * @param id 要修改状态的分类 ID
     * @return Result 操作结果
     */
    @PostMapping("/status/{status}")
    public Result startOrStop(@PathVariable Integer status,Long id){
        log.info("{}分类：{}",status,id);
        categoryService.startOrStop(status,id);
        return Result.success();
    }

    /**
     * 根据类型查询分类列表
     *
     * @param type 分类类型，1 表示菜品分类，2 表示套餐分类
     * @return Result<List<Category>> 分类列表
     */
    @GetMapping("/list")
    public Result<List<Category>> list(Integer type){
        log.info("查询分类：{}",type);
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }


}
