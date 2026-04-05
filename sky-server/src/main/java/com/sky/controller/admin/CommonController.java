package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/admin/common")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile  file) {
        log.info("文件上传：{}",file);
        try {

            String fileName = file.getOriginalFilename();
            String extension = fileName.substring(fileName.lastIndexOf("."));
            String objectName = "sky-take-out" + "/" + System.currentTimeMillis() + extension;

            String url = aliOssUtil.upload(file.getBytes(), file.getOriginalFilename());
            return Result.success(url);
        } catch (IOException e) {
            log.error("文件上传失败：{}",e);
        }
        return null;
    }
}
