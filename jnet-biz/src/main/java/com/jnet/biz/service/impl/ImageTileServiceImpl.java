package com.jnet.biz.service.impl;

import com.jnet.biz.entity.Image;
import com.jnet.biz.exception.BizErrorCode;
import com.jnet.biz.exception.BizException;
import com.jnet.biz.mapper.ImageMapper;
import com.jnet.biz.service.IImageTileService;
import com.jnet.biz.util.WsiTileGenerator;
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 图像瓦片 Service 实现类
 * <p>
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
    // OpenSlide 缓存（512MB）
    private static final OpenSlideCache slideCache;

    // OpenSlide 缓存：key=imageId, value=OpenSlide实例
    private final Map<Long, OpenSlide> openSlideCache = new ConcurrentHashMap<>();

    // 缓存最大数量限制
    private static final int MAX_CACHE_SIZE = 10;

    // 静态代码块：加载 OpenSlide 原生库
    static {
        try {
            System.load("D:\\tools\\openslide4\\bin\\libopenslide-1.dll");
            loadOpenSlideLibrary();
            // 库加载成功后再创建缓存
            slideCache = new OpenSlideCache(1024 * 1024 * 512);
        } catch (Exception e) {
            log.error("加载 OpenSlide 原生库失败", e);
            throw new RuntimeException("无法加载 OpenSlide 库，请检查 openslide.dll 是否存在", e);
        }
    }

    /**
     * 加载 OpenSlide 原生库
     * <p>
     * OpenSlide Java 0.13.x 使用 FFM API，需要：
     * 1. openslide.dll - 通过 System.loadLibrary 加载
     * 2. libopenslide-1.dll - 通过 SymbolLookup.libraryLookup 加载
     */
    public static void loadOpenSlideLibrary() {
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

    public static void main(String[] args) {
        File image = new File("E:\\doc\\jnet\\imageStore\\project_2\\dev-batch\\R25-0818-RD 25081806-18 1F.svs");
        try {
            OpenSlide openSlide = new OpenSlide(image);
            for (String location : WsiTileGenerator.getTileNamesByLevel(openSlide, 2)) {
                WsiTileGenerator.TileCoordinate coordinate = WsiTileGenerator.parseAndValidateLocation(location);
                String outputPath = String.format("E:\\wsi_tile_level\\%d-%d-%d.jpg", 2, coordinate.getRowIndex(), coordinate.getColumnIndex());
                WsiTileGenerator.generateSingleTileToFile(openSlide, location, 256, outputPath);
            }

            // 用系统默认查看器打开
            /*
            File outputFile = new File(outputPath);
            if (Desktop.isDesktopSupported() && outputFile.exists()) {
                Desktop.getDesktop().open(outputFile);
                log.info("已打开图片查看器");
            }*/

            openSlide.close();

        } catch (IOException e) {
            log.error("处理失败", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

            // 2. 检查图像文件是否存在
            File imageFile = new File(image.getFilePath());
            if (!imageFile.exists()) {
                log.warn("图像文件不存在，返回占位图: {}", image.getFilePath());
                return generatePlaceholderThumbnail(maxSize);
            }

            // 3. 根据格式选择处理方式
            String format = image.getFormat();
            BufferedImage thumbnailImage;

            if ("JPG".equals(format) || "JPEG".equals(format) || "PNG".equals(format)) {
                // 普通图片格式：直接使用ImageIO
                log.info("使用ImageIO生成缩略图: {}", image.getFilename());
                thumbnailImage = generateThumbnailForStandardImage(imageFile, maxSize);
            } else {
                // WSI 格式：尝试使用 OpenSlide
                try {
                    log.info("使用 OpenSlide 生成缩略图: {}", image.getFilename());
                    thumbnailImage = WsiTileGenerator.generateThumbnailWithOpenSlide(imageFile, maxSize);
                } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
                    log.warn("OpenSlide 未集成，返回占位图: {}", e.getMessage());
                    return generatePlaceholderThumbnail(maxSize);
                } catch (Exception e) {
                    log.error("OpenSlide 解析失败，返回占位图: {}", image.getFilename(), e);
                    return generatePlaceholderThumbnail(maxSize);
                }
            }

            // 4. 转换为JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 确保BufferedImage类型兼容JPEG编码
            BufferedImage jpegCompatibleImage = thumbnailImage;
            if (thumbnailImage.getType() != BufferedImage.TYPE_INT_RGB &&
                    thumbnailImage.getType() != BufferedImage.TYPE_INT_BGR &&
                    thumbnailImage.getType() != BufferedImage.TYPE_3BYTE_BGR) {
                log.info("转换BufferedImage类型: {} -> TYPE_INT_RGB", thumbnailImage.getType());
                jpegCompatibleImage = new BufferedImage(
                        thumbnailImage.getWidth(),
                        thumbnailImage.getHeight(),
                        BufferedImage.TYPE_INT_RGB
                );
                Graphics2D g = jpegCompatibleImage.createGraphics();
                g.drawImage(thumbnailImage, 0, 0, null);
                g.dispose();
            }

            boolean writeSuccess = ImageIO.write(jpegCompatibleImage, "jpeg", baos);

            if (!writeSuccess || jpegCompatibleImage == null) {
                log.error("ImageIO.write失败: imageId={}, imageNull={}, writeSuccess={}, imageType={}",
                        imageId, jpegCompatibleImage == null, writeSuccess,
                        jpegCompatibleImage != null ? jpegCompatibleImage.getType() : -1);
                return generatePlaceholderThumbnail(maxSize);
            }

            byte[] thumbnailData = baos.toByteArray();

            if (thumbnailData.length == 0) {
                log.error("缩略图数据为空: imageId={}", imageId);
                return generatePlaceholderThumbnail(maxSize);
            }

            // 5. 保存缩略图到磁盘
            String thumbnailPath = saveThumbnail(imageId, thumbnailData);
            image.setThumbnailUrl(thumbnailPath);
            imageMapper.updateById(image);

            log.info("缩略图生成成功: imageId={}, path={}", imageId, thumbnailPath);
            return new ByteArrayResource(thumbnailData);

        } catch (IOException e) {
            log.error("生成缩略图失败: imageId={}", imageId, e);
            // 返回占位图而不是抛出异常
            try {
                return generatePlaceholderThumbnail(maxSize);
            } catch (IOException ex) {
                throw new BizException(BizErrorCode.SYSTEM_ERROR, "生成缩略图失败: " + e.getMessage());
            }
        }
    }

    /**
     * 获取或创建 OpenSlide 实例（带缓存）
     *
     * @param imageId  图像ID
     * @param filePath 文件路径
     * @return OpenSlide 实例
     * @throws IOException 打开失败
     */
    private OpenSlide getOrCreateOpenSlide(Long imageId, String filePath) throws IOException {
        // 先从缓存获取
        OpenSlide cachedSlide = openSlideCache.get(imageId);
        if (cachedSlide != null) {
            log.debug("从缓存获取 OpenSlide: imageId={}", imageId);
            return cachedSlide;
        }

        // 缓存已满，移除最旧的条目
        if (openSlideCache.size() >= MAX_CACHE_SIZE) {
            Long oldestKey = openSlideCache.keySet().iterator().next();
            OpenSlide oldSlide = openSlideCache.remove(oldestKey);
            if (oldSlide != null) {
                try {
                    oldSlide.close();
                    log.info("移除缓存的 OpenSlide: imageId={}", oldestKey);
                } catch (Exception e) {
                    log.warn("关闭旧的 OpenSlide 失败", e);
                }
            }
        }

        // 创建新的 OpenSlide 实例
        File slideFile = WsiTileGenerator.validateSlideFile(filePath);
        OpenSlide newSlide = new OpenSlide(slideFile);

        // 设置缓存（使用 OpenSlide 内置缓存）
        OpenSlideCache slideCache = new OpenSlideCache(64 * 1024 * 1024); // 64MB
        newSlide.setCache(slideCache);

        // 放入我们的缓存
        openSlideCache.put(imageId, newSlide);
        log.info("创建并缓存 OpenSlide: imageId={}, file={}", imageId, filePath);

        return newSlide;
    }

    /**
     * 清理指定图像的 OpenSlide 缓存
     *
     * @param imageId 图像ID
     */
    public void evictOpenSlideCache(Long imageId) {
        OpenSlide slide = openSlideCache.remove(imageId);
        if (slide != null) {
            try {
                slide.close();
                log.info("清理 OpenSlide 缓存: imageId={}", imageId);
            } catch (Exception e) {
                log.warn("关闭 OpenSlide 失败: imageId={}", imageId, e);
            }
        }
    }

    /**
     * 清理所有 OpenSlide 缓存
     */
    public void clearAllOpenSlideCache() {
        for (Map.Entry<Long, OpenSlide> entry : openSlideCache.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                log.warn("关闭 OpenSlide 失败: imageId={}", entry.getKey(), e);
            }
        }
        openSlideCache.clear();
        log.info("已清理所有 OpenSlide 缓存");
    }


    /**
     * 获取图像瓦片
     *
     * @param imageId  图像ID
     * @param zoom     OpenLayers zoom级别
     * @param x        瓦片X坐标
     * @param y        瓦片Y坐标
     * @param tileSize 瓦片尺寸（像素），默认256
     * @return
     */
    @Override
    public Resource getTileByZoom(Long imageId, Integer zoom, Integer x, Integer y, Integer tileSize) {
        try {
            Image image = imageMapper.selectById(imageId);
            if (image == null) {
                throw new BizException(BizErrorCode.IMAGE_NOT_FOUND,
                        "图像不存在: " + imageId);
            }

            // 1. 检查Tile缓存
            /*String tileCacheKey = String.format("tile:%d:%d:%d:%d",
                    query.getImageId(), query.getLevel(), query.getCol(), query.getRow());
            byte[] cachedTile = (byte[]) redisTemplate.opsForValue().get(tileCacheKey);
            if (cachedTile != null) {
                log.debug("从缓存获取Tile: {}", tileCacheKey);
                return new ByteArrayResource(cachedTile);
            }*/
            OpenSlide openSlide = getOrCreateOpenSlide(image.getImageId(), image.getFilePath());
            BufferedImage tileImage = WsiTileGenerator.generateTile(openSlide, zoom, x, y, tileSize);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(tileImage, "jpeg", baos);
            byte[] tileData = baos.toByteArray();
            // 5. 缓存Tile（24小时）
//            redisTemplate.opsForValue().set(tileCacheKey, tileData, 24, TimeUnit.HOURS);
            return new ByteArrayResource(tileData);
        } catch (Exception e) {
            log.error("根据Zoom获取Tile失败: imageId={}, zoom={}, x={}, y={}",
                    imageId, zoom, x, y, e);
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
    private ImageMetadataVO readMetadataWithOpenSlide(Image image) {

            return ImageMetadataVO.builder()
                    .imageId(image.getImageId())
                    .filename(image.getFilename())
                    .width(image.getWidth())
                    .height(image.getHeight())
                    .levelCount(image.getLevels())
                    .mppX(image.getMppX())
                    .mppY(image.getMppY())
                    .magnification(image.getMagnification())
                    .tileWidth(256)
                    .tileHeight(256)
                    .format(image.getFormat())
                    .build();

    }

    /**
     * 构建元数据（简化版，降级方案）
     */
    private ImageMetadataVO buildMetadata(Image image) {
        // 实际应该解析SVS文件头获取真实元数据
        // 这里使用数据库中的信息进行估算

        int width = image.getWidth() != null ? image.getWidth() : 100000;
        int height = image.getHeight() != null ? image.getHeight() : 80000;
        int tileSize = 256;

        // 计算金字塔层级数
        // 公式：level_count = ceil(log2(max(width, height) / tile_size)) + 1
        int maxDim = Math.max(width, height);
        int levelCount = (int) Math.ceil(Math.log((double) maxDim / tileSize) / Math.log(2)) + 1;

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
                .tileWidth(tileSize)
                .tileHeight(tileSize)
                .format(image.getFormat())
                .build();
    }

    /**
     * 为标准图片格式生成缩略图（JPG/PNG）
     */
    private BufferedImage generateThumbnailForStandardImage(File imageFile, int maxSize) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);
        if (originalImage == null) {
            throw new IOException("无法读取图像文件: " + imageFile.getAbsolutePath());
        }

        // 使用Thumbnails库进行缩放
        return Thumbnails.of(originalImage)
                .size(maxSize, maxSize)
                .keepAspectRatio(true)
                .asBufferedImage();
    }

    /**
     * 生成占位缩略图（当OpenSlide不可用时）
     */
    private Resource generatePlaceholderThumbnail(int maxSize) throws IOException {
        log.info("生成占位缩略图: {}x{}", maxSize, maxSize);

        // 创建一个简单的彩色占位图
        BufferedImage placeholder = new BufferedImage(maxSize, maxSize, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = placeholder.createGraphics();

        // 设置背景色
        g2d.setColor(new java.awt.Color(240, 240, 240));
        g2d.fillRect(0, 0, maxSize, maxSize);

        // 绘制边框
        g2d.setColor(new java.awt.Color(200, 200, 200));
        g2d.drawRect(0, 0, maxSize - 1, maxSize - 1);

        // 绘制文字
        g2d.setColor(new java.awt.Color(150, 150, 150));
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, maxSize / 10));
        String text = "No Preview";
        java.awt.FontMetrics fm = g2d.getFontMetrics();
        int x = (maxSize - fm.stringWidth(text)) / 2;
        int y = (maxSize + fm.getAscent()) / 2;
        g2d.drawString(text, x, y);

        g2d.dispose();

        // 转换为JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(placeholder, "jpeg", baos);

        return new ByteArrayResource(baos.toByteArray());
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
