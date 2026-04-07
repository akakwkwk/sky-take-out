package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.impl.SetmealServiceImpl;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
public class SetmealConcroller {

    @Autowired
    private SetmealServiceImpl setmealService;

    @PostMapping
    public Result saveWithDish(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐：{}", setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }


    @GetMapping("/page")
     public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
         log.info("分页查询：{}", setmealPageQueryDTO);
         PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
         return Result.success(pageResult);
        }


     @DeleteMapping
     public Result delete(@RequestParam List<Long> ids){
         log.info("批量删除：{}", ids);
         setmealService.delete(ids);
         return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("编辑套餐：{}", setmealDTO);
        setmealService.updateWithDish(setmealDTO);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据id查询套餐：{}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    @PostMapping("/status/{status}")
    public Result startOrStop(@PathVariable Integer status, @RequestParam Long id){
        log.info("启用或禁用套餐：{}, 状态：{}", id, status);
        setmealService.startOrStop(status, id);
        return Result.success();
    }
}
