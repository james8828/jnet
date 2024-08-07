package com.jnet.image.slide.service;

import com.jnet.image.event.UploadCompleteEvent;
import jakarta.servlet.http.HttpServletResponse;
import org.openslide.OpenSlide;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.List;
import com.jnet.image.slide.domain.Image;


public interface OpenSlideService {

    void processImage(UploadCompleteEvent event)throws Exception;
    void processImage2Tile(Integer imageType, Long imageId, Integer gamma, Integer firmUpNum, String titleGroup, String location, HttpServletResponse response)throws Exception;

}

