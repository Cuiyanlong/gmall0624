package com.atguigu.gmall0624.manage.controller;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
public class FileUploadController {

    //fileUrl = http://192.168.93.225
    @Value("${fileServer.url}")
    private String fileUrl;

    //fileUpload
    //springMVC 文件上传技术
    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {
        String imgUrl = fileUrl;

        if (file != null){
            //上传谁回显谁
            //表示读取配置文件中的trackerd.conf
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            //初始化
            ClientGlobal.init(configFile);
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getConnection();
            //存储数据
            StorageClient storageClient=new StorageClient(trackerServer,null);
            //String orginalFilename="d://dianshang//img//1000502.jpg";
            //获取上传的文件名称
            String originalFilename = file.getOriginalFilename();
            //originalFilename文件名称 和 originalFilename 文件路径
            //获取文件的后缀名
            String extName = org.apache.commons.lang3.StringUtils.substringAfterLast(originalFilename, ".");
            //保存数据
            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
                //System.out.println("s = " + s);
                imgUrl+= "/"+path;
            }
        }
        return imgUrl;
    }
}
