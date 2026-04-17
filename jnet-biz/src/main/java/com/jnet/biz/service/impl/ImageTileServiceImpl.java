package com.jnet.biz.service.impl;

import com.jnet.biz.dto.TileQueryDTO;
import com.jnet.biz.entity.Image;
import com.jnet.biz.exception.BizErrorCode;
import com.jnet.biz.exception.BizException;
import com.jnet.biz.mapper.ImageMapper;
import com.jnet.biz.service.IImageTileService;
import com.jnet.biz.vo.ImageMetadataVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.openslide.OpenSlide;
import org.openslide.OpenSlideCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 图像瓦片 Service 实现类
 * 
 * 使用 OpenSlide Java 官方绑定读取 WSI 图像
 * 支持 SVS、NDPI、TIFF 等格式
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageTileServiceImpl implements IImageTileService {

    // 静态代码块：加载 OpenSlide 原生库
    static {
        try {
            loadOpenSlideLibrary();
        } catch (Exception e) {
            log.error("加载 OpenSlide 原生库失败", e);
            throw new RuntimeException("无法加载 OpenSlide 库，请检查 openslide.dll 是否存在", e);
        }
    }

    /**
     * 加载 OpenSlide 原生库
     * 
     * OpenSlide Java 0.13.x 使用 FFM API，需要：
     * 1. openslide.dll - 通过 System.loadLibrary 加载
     * 2. libopenslide-1.dll - 通过 SymbolLookup.libraryLookup 加载
     */
    private static void loadOpenSlideLibrary() {
        // 确定 DLL 所在目录
        String libDir = null;

        String[] possiblePaths = {
                System.getProperty("user.dir") + "/src/main/resources/libs",
                System.getProperty("user.dir") + "/libs"
        };

        for (String path : possiblePaths) {
            File dllFile = new File(path, "libopenslide-1.dll");
            if (dllFile.exists()) {
                libDir = path;
                break;
            }
        }
        
        // 3. 如果找到了目录，显式加载 DLL
        if (libDir != null) {
            File openslideDll = new File(libDir, "openslide.dll");
            File libOpenslideDll = new File(libDir, "libopenslide-1.dll");
            
            try {
                // 先加载依赖（如果有）
                if (openslideDll.exists()) {
                    System.load(openslideDll.getAbsolutePath());
                    log.info("已加载: {}", openslideDll.getAbsolutePath());
                }
                
                // 再加载主库
                if (libOpenslideDll.exists()) {
                    System.load(libOpenslideDll.getAbsolutePath());
                    log.info("已加载: {}", libOpenslideDll.getAbsolutePath());
                }
                
                log.info("✅ OpenSlide 原生库加载成功");
                return;
                
            } catch (UnsatisfiedLinkError e) {
                log.error("加载 OpenSlide DLL 失败", e);
                throw e;
            }
        }
    }

    private final ImageMapper imageMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${data-pool.storage.root-path:E:/doc/jnet/imageStore}")
    private String rootPath;

    private static final String METADATA_CACHE_KEY = "image:metadata:";
    private static final long CACHE_EXPIRE = 3600; // 1小时
    
    // OpenSlide 缓存（512MB）
    private static final OpenSlideCache slideCache = new OpenSlideCache(1024 * 1024 * 512);

    @Override
    public ImageMetadataVO getImageMetadata(Long imageId) {
        // 1. 尝试从缓存获取
        String cacheKey = METADATA_CACHE_KEY + imageId;
        ImageMetadataVO cached = (ImageMetadataVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取元数据: imageId={}", imageId);
            return cached;
        }

        // 2. 查询数据库
        Image image = imageMapper.selectById(imageId);
        if (image == null) {
            throw new BizException(BizErrorCode.IMAGE_NOT_FOUND, "图像不存在: " + imageId);
        }

        // 3. 使用 OpenSlide 读取元数据
        try {
            ImageMetadataVO metadata = readMetadataWithOpenSlide(image);
            
            // 4. 存入缓存
            redisTemplate.opsForValue().set(cacheKey, metadata, CACHE_EXPIRE, TimeUnit.SECONDS);
            
            return metadata;
        } catch (Exception e) {
            log.error("使用 OpenSlide 读取元数据失败，使用估算值: imageId={}", imageId, e);
            // 降级：使用估算值
            return buildMetadata(image);
        }
    }

    @Override
    public Resource getThumbnail(Long imageId, Integer maxSize) {
        if (maxSize == null || maxSize <= 0) {
            maxSize = 512; // 默认512px
        }

        Image image = imageMapper.selectById(imageId);
        if (image == null) {
            throw new BizException(BizErrorCode.IMAGE_NOT_FOUND, "图像不存在: " + imageId);
        }

        try {
            // 1. 检查是否已有缩略图
            if (image.getThumbnailUrl() != null && !image.getThumbnailUrl().isEmpty()) {
                File thumbnailFile = new File(image.getThumbnailUrl());
                if (thumbnailFile.exists()) {
                    byte[] data = readFile(thumbnailFile);
                    return new ByteArrayResource(data);
                }
            }

            // 2. 使用 OpenSlide 生成缩略图
            File imageFile = new File(image.getFilePath());
            if (!imageFile.exists()) {
                throw new BizException(BizErrorCode.SYSTEM_ERROR, "图像文件不存在: " + image.getFilePath());
            }

            // 3. 使用 OpenSlide 读取最低分辨率层级作为缩略图
            BufferedImage thumbnailImage = generateThumbnailWithOpenSlide(imageFile, maxSize);

            // 4. 转换为JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnailImage, "jpeg", baos);
            byte[] thumbnailData = baos.toByteArray();

            // 5. 保存缩略图到磁盘
            String thumbnailPath = saveThumbnail(imageId, thumbnailData);
            image.setThumbnailUrl(thumbnailPath);
            imageMapper.updateById(image);

            return new ByteArrayResource(thumbnailData);

        } catch (IOException e) {
            log.error("生成缩略图失败: imageId={}", imageId, e);
            throw new BizException(BizErrorCode.SYSTEM_ERROR, "生成缩略图失败: " + e.getMessage());
        }
    }

    @Override
    public Resource getTile(TileQueryDTO query) {
        Image image = imageMapper.selectById(query.getImageId());
        if (image == null) {
            throw new BizException(BizErrorCode.IMAGE_NOT_FOUND, 
                    "图像不存在: " + query.getImageId());
        }

        try {
            // 1. 检查Tile缓存
            String tileCacheKey = String.format("tile:%d:%d:%d:%d",
                    query.getImageId(), query.getLevel(), query.getCol(), query.getRow());
            byte[] cachedTile = (byte[]) redisTemplate.opsForValue().get(tileCacheKey);
            if (cachedTile != null) {
                log.debug("从缓存获取Tile: {}", tileCacheKey);
                return new ByteArrayResource(cachedTile);
            }

            // 2. 使用 OpenSlide 读取 Tile
            File imageFile = new File(image.getFilePath());
            if (!imageFile.exists()) {
                throw new BizException(BizErrorCode.SYSTEM_ERROR, 
                        "图像文件不存在: " + image.getFilePath());
            }

            int tileSize = query.getTileSize() != null ? query.getTileSize() : 256;
            
            // 3. 使用 OpenSlide 读取指定层级的区域
            BufferedImage tileImage = readTileWithOpenSlide(imageFile, query.getLevel(), 
                    query.getCol(), query.getRow(), tileSize);

            // 4. 转换为JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(tileImage, "jpeg", baos);
            byte[] tileData = baos.toByteArray();

            // 5. 缓存Tile（24小时）
            redisTemplate.opsForValue().set(tileCacheKey, tileData, 24, TimeUnit.HOURS);

            return new ByteArrayResource(tileData);

        } catch (IOException e) {
            log.error("获取Tile失败: imageId={}, level={}, col={}, row={}",
                    query.getImageId(), query.getLevel(), query.getCol(), query.getRow(), e);
            throw new BizException(BizErrorCode.SYSTEM_ERROR, "获取Tile失败: " + e.getMessage());
        }
    }

    @Override
    public String getLevelInfo(Long imageId) {
        ImageMetadataVO metadata = getImageMetadata(imageId);
        
        // 构建层级信息JSON
        StringBuilder json = new StringBuilder("{");
        json.append("\"imageId\":").append(metadata.getImageId()).append(",");
        json.append("\"width\":").append(metadata.getWidth()).append(",");
        json.append("\"height\":").append(metadata.getHeight()).append(",");
        json.append("\"levelCount\":").append(metadata.getLevelCount()).append(",");
        json.append("\"tileWidth\":").append(metadata.getTileWidth()).append(",");
        json.append("\"tileHeight\":").append(metadata.getTileHeight()).append(",");
        json.append("\"levels\":[");
        
        if (metadata.getLevelDimensions() != null) {
            for (int i = 0; i < metadata.getLevelDimensions().size(); i++) {
                int[] dim = metadata.getLevelDimensions().get(i);
                if (i > 0) json.append(",");
                json.append("{\"level\":").append(i)
                    .append(",\"width\":").append(dim[0])
                    .append(",\"height\":").append(dim[1])
                    .append("}");
            }
        }
        
        json.append("]}");
        
        return json.toString();
    }

    /**
     * 使用 OpenSlide 读取元数据
     */
    private ImageMetadataVO readMetadataWithOpenSlide(Image image) throws IOException {
        File imageFile = new File(image.getFilePath());
        
        try (OpenSlide slide = new OpenSlide(imageFile)) {
            // 设置缓存
            slide.setCache(slideCache);
            
            // 获取基本属性
            long width = slide.getLevel0Width();
            long height = slide.getLevel0Height();
            int levelCount = slide.getLevelCount();
            
            // 获取每层级的尺寸
            List<int[]> levelDimensions = new ArrayList<>();
            for (int i = 0; i < levelCount; i++) {
                long levelWidth = slide.getLevelWidth(i);
                long levelHeight = slide.getLevelHeight(i);
                levelDimensions.add(new int[]{(int) levelWidth, (int) levelHeight});
            }
            
            // 获取 MPP（如果可用）
            Double mppX = null;
            Double mppY = null;
            try {
                String mppXStr = slide.getProperties().get("openslide.mpp-x");
                String mppYStr = slide.getProperties().get("openslide.mpp-y");
                if (mppXStr != null) mppX = Double.parseDouble(mppXStr);
                if (mppYStr != null) mppY = Double.parseDouble(mppYStr);
            } catch (Exception e) {
                log.warn("无法获取 MPP 信息");
            }
            
            // 获取放大倍数
            Integer magnification = null;
            try {
                String magStr = slide.getProperties().get("openslide.objective-power");
                if (magStr != null) magnification = Integer.parseInt(magStr);
            } catch (Exception e) {
                log.warn("无法获取放大倍数");
            }
            
            return ImageMetadataVO.builder()
                    .imageId(image.getImageId())
                    .filename(image.getFilename())
                    .width((int) width)
                    .height((int) height)
                    .levelCount(levelCount)
                    .levelDimensions(levelDimensions)
                    .mppX(mppX != null ? mppX : image.getMppX())
                    .mppY(mppY != null ? mppY : image.getMppY())
                    .magnification(magnification != null ? magnification : image.getMagnification())
                    .tileWidth(256)
                    .tileHeight(256)
                    .format(image.getFormat())
                    .build();
        }
    }

    /**
     * 使用 OpenSlide 读取 Tile
     */
    private BufferedImage readTileWithOpenSlide(File imageFile, int level, int col, int row, int tileSize) throws IOException {
        try (OpenSlide slide = new OpenSlide(imageFile)) {
            slide.setCache(slideCache);
            
            // 计算在 Level 0 中的坐标
            double downsample = slide.getLevelDownsample(level);
            int x = (int) (col * tileSize * downsample);
            int y = (int) (row * tileSize * downsample);
            
            // 直接读取区域（返回 BufferedImage）
            return slide.readRegion(level, x, y, tileSize, tileSize);
        }
    }

    /**
     * 使用 OpenSlide 生成缩略图
     */
    private BufferedImage generateThumbnailWithOpenSlide(File imageFile, int maxSize) throws IOException {
        try (OpenSlide slide = new OpenSlide(imageFile)) {
            slide.setCache(slideCache);
            
            // 获取最低分辨率层级（最高层级索引）
            int levelCount = slide.getLevelCount();
            int lowestResLevel = levelCount - 1;
            
            // 获取该层级的尺寸
            long width = slide.getLevelWidth(lowestResLevel);
            long height = slide.getLevelHeight(lowestResLevel);
            
            // 如果已经小于 maxSize，直接读取
            if (width <= maxSize && height <= maxSize) {
                return slide.readRegion(lowestResLevel, 0, 0, (int) width, (int) height);
            }
            
            // 否则读取后缩放
            BufferedImage lowResImage = slide.readRegion(lowestResLevel, 0, 0, (int) width, (int) height);
            return Thumbnails.of(lowResImage)
                    .size(maxSize, maxSize)
                    .keepAspectRatio(true)
                    .asBufferedImage();
        }
    }

    /**
     * 构建元数据（简化版，降级方案）
     */
    private ImageMetadataVO buildMetadata(Image image) {
        // 实际应该解析SVS文件头获取真实元数据
        // 这里使用数据库中的信息进行估算
        
        int width = image.getWidth() != null ? image.getWidth() : 100000;
        int height = image.getHeight() != null ? image.getHeight() : 80000;
        
        // 计算金字塔层级数
        int levelCount = 1;
        int maxDim = Math.max(width, height);
        while (maxDim > 256) {
            maxDim /= 2;
            levelCount++;
        }
        
        // 构建每层级的尺寸
        List<int[]> levelDimensions = new ArrayList<>();
        for (int i = 0; i < levelCount; i++) {
            double scale = Math.pow(0.5, i);
            int levelWidth = (int) (width * scale);
            int levelHeight = (int) (height * scale);
            levelDimensions.add(new int[]{levelWidth, levelHeight});
        }
        
        return ImageMetadataVO.builder()
                .imageId(image.getImageId())
                .filename(image.getFilename())
                .width(width)
                .height(height)
                .levelCount(levelCount)
                .levelDimensions(levelDimensions)
                .mppX(image.getMppX())
                .mppY(image.getMppY())
                .magnification(image.getMagnification())
                .tileWidth(256)
                .tileHeight(256)
                .format(image.getFormat())
                .build();
    }

    /**
     * 保存缩略图到磁盘
     */
    private String saveThumbnail(Long imageId, byte[] data) throws IOException {
        String thumbnailDir = String.format("%s/thumbnails", rootPath);
        File dir = new File(thumbnailDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        String thumbnailPath = String.format("%s/%d.jpg", thumbnailDir, imageId);
        File thumbnailFile = new File(thumbnailPath);
        
        try (FileOutputStream fos = new FileOutputStream(thumbnailFile)) {
            fos.write(data);
        }
        
        return thumbnailPath;
    }

    /**
     * 读取文件为字节数组
     */
    private byte[] readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            
            return baos.toByteArray();
        }
    }
}
