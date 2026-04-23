package com.jnet.biz.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openslide.OpenSlide;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 病理切片瓦片生成工具类
 * <p>
 * 基于 OpenSlide 实现 WSI（Whole Slide Image）瓦片的动态生成和批量预生成
 * 支持 SVS、NDPI、TIFF 等病理切片格式
 *
 * <h3>金字塔层级说明</h3>
 * <ul>
 *   <li>Level 0: 最高分辨率（原始尺寸，downSample=1）</li>
 *   <li>Level N: 较低分辨率（downSample=2^N）</li>
 *   <li>每增加一级，分辨率减半，瓦片数量减少为原来的 1/4</li>
 * </ul>
 *
 * @author JNet Team
 * @since 2024-04-20
 */
@Slf4j
public class WsiTileGenerator {

    /**
     * 瓦片坐标信息
     * <p>
     * 用于标识瓦片在金字塔中的位置和层级
     */
    @Data
    public static class TileCoordinate {
        /**
         * 金字塔层级（从0开始）
         * <ul>
         *   <li>0: 最高分辨率（原始尺寸）</li>
         *   <li>N: 较低分辨率（downSample = 2^N）</li>
         * </ul>
         */
        int pyramidLevel;

        /**
         * 瓦片列索引（从0开始，从左到右）
         */
        int columnIndex;

        /**
         * 瓦片行索引（从0开始，从上到下）
         */
        int rowIndex;

        TileCoordinate(int pyramidLevel, int columnIndex, int rowIndex) {
            this.pyramidLevel = pyramidLevel;
            this.columnIndex = columnIndex;
            this.rowIndex = rowIndex;
        }

        @Override
        public String toString() {
            return String.format("Tile[level=%d, col=%d, row=%d]", pyramidLevel, columnIndex, rowIndex);
        }
    }

    /**
     * 解析和验证位置参数
     * <p>
     * 将字符串格式的位置参数解析为瓦片坐标对象
     *
     * @param location 位置字符串，格式: "level-x-y"
     * @return 瓦片坐标对象
     * @throws IllegalArgumentException 参数格式错误
     */
    public static TileCoordinate parseAndValidateLocation(String location) {
        if (StringUtils.isEmpty(location)) {
            throw new IllegalArgumentException("位置参数不能为空");
        }

        String[] parts = StringUtils.split(location, "-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("位置参数格式错误，应为: level-x-y");
        }

        try {
            int pyramidLevel = Integer.parseInt(parts[0]);
            int columnIndex = Integer.parseInt(parts[1]);
            int rowIndex = Integer.parseInt(parts[2]);

            log.debug("解析位置参数: location={}, level={}, x={}, y={}",
                     location, pyramidLevel, columnIndex, rowIndex);

            return new TileCoordinate(pyramidLevel, columnIndex, rowIndex);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("位置参数必须是整数，格式: level-x-y", e);
        }
    }

    /**
     * 验证切片文件存在性
     *
     * @param slidePath 切片文件绝对路径
     * @return 文件对象
     * @throws IOException 文件不存在
     */
    public static File validateSlideFile(String slidePath) throws IOException {
        File slideFile = new File(slidePath);
        if (!slideFile.exists()) {
            log.error("切片文件不存在: {}", slidePath);
            throw new IOException("切片文件不存在: " + slidePath);
        }

        if (!slideFile.canRead()) {
            log.error("切片文件不可读: {}", slidePath);
            throw new IOException("切片文件不可读: " + slidePath);
        }

        log.debug("验证切片文件成功: path={}, size={} bytes", slidePath, slideFile.length());
        return slideFile;
    }

    /**
     * 生成瓦片（公开方法 - 使用坐标参数）
     * <p>
     * 根据指定的金字塔层级和瓦片坐标，从 WSI 图像中提取并生成瓦片
     *
     * @param openSlide    OpenSlide 对象（已打开的 WSI 文件）
     * @param pyramidLevel 金字塔层级（0=最高分辨率）
     * @param columnIndex  瓦片列索引（从0开始）
     * @param rowIndex     瓦片行索引（从0开始）
     * @param tileSize     瓦片尺寸（像素），推荐 256 或 512
     * @return 生成的瓦片图像（BufferedImage）
     * @throws Exception 生成失败
     */
    public static BufferedImage generateTile(OpenSlide openSlide, int pyramidLevel, int columnIndex, int rowIndex, int tileSize) throws Exception {
        TileCoordinate coordinate = new TileCoordinate(pyramidLevel, columnIndex, rowIndex);
        log.info("请求生成瓦片: {}", coordinate);
        return generateTile(openSlide, coordinate, tileSize);
    }

    /**
     * 生成瓦片（公开方法 - 使用字符串位置）
     *
     * @param openSlide OpenSlide 对象
     * @param location  位置字符串，格式: "level-x-y"
     * @param tileSize  瓦片尺寸（像素）
     * @return 生成的瓦片图像
     * @throws Exception 生成失败
     */
    public static BufferedImage generateTile(OpenSlide openSlide, String location, int tileSize) throws Exception {
        log.info("请求生成瓦片: location={}", location);
        TileCoordinate coordinate = parseAndValidateLocation(location);
        return generateTile(openSlide, coordinate, tileSize);
    }

    /**
     * 生成瓦片（核心方法）
     * <p>
     * 算法流程：
     * <ol>
     *   <li>计算下采样因子：downSample = maxDimension / (2^level * tileSize)</li>
     *   <li>计算在 Level 0 坐标系中的读取区域</li>
     *   <li>边界检查：确保不超出图像范围</li>
     *   <li>使用 OpenSlide.paintRegion() 提取并缩放区域</li>
     *   <li>返回指定尺寸的瓦片图像</li>
     * </ol>
     *
     * @param openSlide  OpenSlide 对象
     * @param coordinate 瓦片坐标
     * @param tileSize   瓦片尺寸（像素）
     * @return 生成的瓦片图像
     * @throws Exception 生成失败
     */
    private static BufferedImage generateTile(OpenSlide openSlide, TileCoordinate coordinate, int tileSize)
            throws Exception {

        // 1. 获取 Level 0（原始）图像尺寸
        long level0Width = openSlide.getLevel0Width();
        long level0Height = openSlide.getLevel0Height();
        log.debug("Level 0 图像尺寸: {}x{}", level0Width, level0Height);

        // 2. 计算最大维度
        long maxDimension = Math.max(level0Width, level0Height);

        // 3. 计算当前层级的瓦片数量系数
        //    公式：tileCount = 2^level
        //    Level 0: tileCount = 1 (整个图像分为 1x1 个瓦片区)
        //    Level 1: tileCount = 2 (整个图像分为 2x2 个瓦片区)
        //    Level 2: tileCount = 4 (整个图像分为 4x4 个瓦片区)
        double tileCount = Math.pow(2, coordinate.pyramidLevel);

        // 4. 计算下采样因子
        //    公式：downSample = maxDimension / (tileCount * tileSize)
        //    这确保了每个瓦片在下采样后正好是 tileSize x tileSize 像素
        double downSample = maxDimension / (tileCount * tileSize);

        log.info("瓦片计算: level={}, tileCount={}, downSample={:.4f}",
                coordinate.pyramidLevel, tileCount, downSample);
        log.info("瓦片尺寸: tileSize={} pixels", tileSize);

        // 5. 计算在 Level 0 坐标系中的读取区域
        //    注意：regionX 和 regionY 是在下采样后的坐标系中
        int x = coordinate.columnIndex;
        int y = coordinate.rowIndex;
        int regionX = x * tileSize;
        int regionY = y * tileSize;
        int regionWidth = tileSize;
        int regionHeight = tileSize;

        log.debug("初始读取区域: x={}, y={}, w={}, h={}", regionX, regionY, regionWidth, regionHeight);

        // 6. 边界检查：计算下采样后的可用区域
        //    确保不会读取超出图像范围的区域
        int maxRegionX = (int) (level0Width / downSample);
        int maxRegionY = (int) (level0Height / downSample);

        log.debug("下采样后可用区域: maxW={}, maxH={}", maxRegionX, maxRegionY);

        // 7. 调整右侧边界
        if (regionX + regionWidth > maxRegionX) {
            int originalWidth = regionWidth;
            regionWidth = maxRegionX - regionX;
            log.debug("调整宽度: {} -> {} (右边界)", originalWidth, regionWidth);
        }

        // 8. 调整底部边界
        if (regionY + regionHeight > maxRegionY) {
            int originalHeight = regionHeight;
            regionHeight = maxRegionY - regionY;
            log.debug("调整高度: {} -> {} (下边界)", originalHeight, regionHeight);
        }

        // 9. 检查是否为有效区域
        if (regionWidth <= 0 || regionHeight <= 0) {
            log.warn("瓦片区域无效: width={}, height={}, 返回空白瓦片", regionWidth, regionHeight);
            return createBlankTile(tileSize);
        }

        log.info("最终读取区域: x={}, y={}, w={}, h={}", regionX, regionY, regionWidth, regionHeight);

        // 10. 创建目标图像缓冲区
        BufferedImage result = new BufferedImage(regionWidth, regionHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();

        // 11. 设置白色背景（用于填充透明或空白区域）
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, regionWidth, regionHeight);

        // 12. 配置渲染质量
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        try {
            // 13. 使用 OpenSlide paintRegion 提取并绘制瓦片
            //     paintRegion 会自动处理：
            //     - 从 Level 0 读取 regionX, regionY 处的区域
            //     - 按 downSample 因子下采样
            //     - 绘制到目标 BufferedImage
            log.debug("调用 paintRegion: src=({}, {}), size={}x{}, downSample={:.4f}",
                     regionX, regionY, regionWidth, regionHeight, downSample);

            openSlide.paintRegion(g,
                    0, 0,                 // 目标位置 (dx, dy) - 绘制到 BufferedImage 的左上角
                    regionX, regionY,     // 源位置 (sx, sy) - 从 Level 0 的该位置读取
                    regionWidth, regionHeight, // 源尺寸 (w, h) - 读取的区域大小
                    downSample);          // 下采样因子 - 控制缩放比例

            log.info("瓦片生成完成: 尺寸={}x{}", result.getWidth(), result.getHeight());

        } catch (Exception e) {
            log.error("paintRegion 失败: coordinate={}, error={}", coordinate, e.getMessage(), e);
            throw e;
        } finally {
            // 14. 释放图形上下文资源
            g.dispose();
        }

        return result;
    }

    /**
     * 创建空白瓦片（白色背景）
     * <p>
     * 用于边界情况或错误处理
     *
     * @param size 瓦片尺寸（像素）
     * @return 空白瓦片图像
     */
    private static BufferedImage createBlankTile(int size) {
        log.debug("创建空白瓦片: size={}x{}", size, size);
        BufferedImage blankImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = blankImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.dispose();
        return blankImage;
    }

    /**
     * 生成单个瓦片并保存到文件
     * <p>
     * 用于离线预生成瓦片
     *
     * @param openSlide    OpenSlide 对象
     * @param pyramidLevel 金字塔层级
     * @param columnIndex  X 坐标（列索引）
     * @param rowIndex     Y 坐标（行索引）
     * @param tileSize     瓦片尺寸（像素）
     * @param outputPath   输出文件路径
     * @throws Exception IO 异常或生成失败
     */
    public static void generateSingleTileToFile(OpenSlide openSlide, int pyramidLevel, int columnIndex,
                                                 int rowIndex, int tileSize, String outputPath) throws Exception {
        log.info("生成瓦片到文件: level={}-{}-{}, output={}", pyramidLevel, columnIndex, rowIndex, outputPath);

        long startTime = System.currentTimeMillis();

        BufferedImage tileImage = generateTile(openSlide, pyramidLevel, columnIndex, rowIndex, tileSize);


        // 确保输出目录存在
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                log.info("创建输出目录: {}", parentDir.getAbsolutePath());
            } else {
                log.warn("创建输出目录失败: {}", parentDir.getAbsolutePath());
            }
        }
        ImageIO.write(tileImage, "jpg", outputFile);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("瓦片保存成功: path={}, size={} bytes, time={} ms",
                outputPath, outputFile.length(), elapsed);
    }

    /**
     * 生成单个瓦片并保存到文件（使用字符串位置）
     *
     * @param openSlide  OpenSlide 对象
     * @param location   位置字符串，格式: "level-x-y"
     * @param tileSize   瓦片尺寸（像素）
     * @param outputPath 输出文件路径
     * @throws Exception IO 异常或生成失败
     */
    public static void generateSingleTileToFile(OpenSlide openSlide, String location, int tileSize, String outputPath) throws Exception {
        log.info("生成瓦片到文件: location={}, output={}", location, outputPath);
        TileCoordinate coordinate = parseAndValidateLocation(location);
        generateSingleTileToFile(openSlide, coordinate.pyramidLevel, coordinate.columnIndex,
                                coordinate.rowIndex, tileSize, outputPath);
    }

    /**
     * 使用 OpenSlide 生成缩略图
     * <p>
     * 利用 OpenSlide 内置的 createThumbnailImage API，自动选择最佳层级并生成缩略图
     *
     * @param imageFile WSI 图像文件（SVS/NDPI 等）
     * @param maxSize   缩略图最大边长（像素）
     * @return RGB 格式的 BufferedImage
     * @throws IOException 读取失败时抛出
     */
    public static BufferedImage generateThumbnailWithOpenSlide(File imageFile, int maxSize) throws IOException {
        log.info("开始生成缩略图: file={}, maxSize={}", imageFile.getName(), maxSize);

        long startTime = System.currentTimeMillis();

        try (OpenSlide slide = new OpenSlide(imageFile)) {
            // 使用 OpenSlide 内置 API 生成缩略图（自动处理层级选择和缩放）
            BufferedImage thumbnail = slide.createThumbnailImage(maxSize);

            log.info("OpenSlide 生成缩略图: {}x{}, type={}",
                    thumbnail.getWidth(), thumbnail.getHeight(), thumbnail.getType());

            // 转换为标准 RGB 类型（确保 JPEG 编码兼容）
            if (thumbnail.getType() != BufferedImage.TYPE_INT_RGB) {
                log.debug("转换图像类型: {} -> TYPE_INT_RGB", thumbnail.getType());
                thumbnail = convertToRGB(thumbnail);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("缩略图生成成功: {}x{}, time={} ms", thumbnail.getWidth(), thumbnail.getHeight(), elapsed);
            return thumbnail;
        } catch (Exception e) {
            log.error("生成缩略图失败: file={}, error={}", imageFile.getAbsolutePath(), e.getMessage(), e);
            throw new IOException("生成缩略图失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将任意类型的 BufferedImage 转换为 TYPE_INT_RGB
     * <p>
     * 逐像素复制，自动处理颜色空间转换
     *
     * @param source 源图像
     * @return RGB 格式的图像
     */
    private static BufferedImage convertToRGB(BufferedImage source) {
        log.debug("转换图像类型: {}x{}, type={} -> TYPE_INT_RGB",
                source.getWidth(), source.getHeight(), source.getType());

        BufferedImage rgbImage = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        // 逐像素复制（自动处理颜色空间转换）
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                rgbImage.setRGB(x, y, source.getRGB(x, y));
            }
        }

        return rgbImage;
    }

    /**
     * 根据逻辑层级计算瓦片网格信息
     * <p>
     * 计算指定层级下图像的瓦片行列数
     *
     * @param openSlide  OpenSlide 对象
     * @param level      金字塔层级（0=最高分辨率）
     * @param tileSize   瓦片尺寸（像素）
     * @return 瓦片网格信息 [tilesPerRow, tilesPerColumn]
     */
    public static int[] calculateTileGrid(OpenSlide openSlide, int level, int tileSize) {
        long level0Width = openSlide.getLevel0Width();
        long level0Height = openSlide.getLevel0Height();
        long maxDimension = Math.max(level0Width, level0Height);

        // 计算当前层级的瓦片数量系数：tileCount = 2^level
        double tileCount = Math.pow(2, level);

        // 计算下采样因子
        double downSample = maxDimension / (tileCount * tileSize);

        // 计算下采样后的图像尺寸
        long levelWidth = (long) (level0Width / downSample);
        long levelHeight = (long) (level0Height / downSample);

        // 计算瓦片行列数（向上取整）
        int tilesPerRow = (int) Math.ceil((double) levelWidth / tileSize);
        int tilesPerColumn = (int) Math.ceil((double) levelHeight / tileSize);

        log.info("瓦片网格计算: level={}, downSample={:.4f}, 层级尺寸={}x{}, 瓦片数={}x{}",
                level, downSample, levelWidth, levelHeight, tilesPerRow, tilesPerColumn);

        return new int[]{tilesPerRow, tilesPerColumn};
    }

    /**
     * 获取指定逻辑层的所有瓦片名称集合
     * <p>
     * 生成格式为 "level-col-row" 的瓦片名称列表
     * 例如：["3-0-0", "3-0-1", "3-1-0", "3-1-1", ...]
     *
     * @param openSlide OpenSlide 对象
     * @param level     金字塔层级（0=最高分辨率）
     * @param tileSize  瓦片尺寸（像素），默认 256
     * @return 瓦片名称集合，格式为 "level-col-row"
     */
    public static java.util.List<String> getTileNamesByLevel(OpenSlide openSlide, int level, int tileSize) {
        log.info("获取瓦片名称集合: level={}, tileSize={}", level, tileSize);

        // 1. 计算瓦片网格
        int[] grid = calculateTileGrid(openSlide, level, tileSize);
        int tilesPerRow = grid[0];
        int tilesPerColumn = grid[1];
        int totalTiles = tilesPerRow * tilesPerColumn;

        log.info("瓦片总数: {} ({}行 x {}列)", totalTiles, tilesPerColumn, tilesPerRow);

        // 2. 生成瓦片名称列表
        java.util.List<String> tileNames = new java.util.ArrayList<>(totalTiles);

        for (int row = 0; row < tilesPerColumn; row++) {
            for (int col = 0; col < tilesPerRow; col++) {
                String tileName = String.format("%d-%d-%d", level, col, row);
                tileNames.add(tileName);
            }
        }

        log.info("瓦片名称集合生成完成: 共 {} 个瓦片", tileNames.size());

        // 打印前几个和后几个瓦片名称作为示例
        if (!tileNames.isEmpty()) {
            log.debug("前5个瓦片: {}", tileNames.subList(0, Math.min(5, tileNames.size())));
            if (tileNames.size() > 5) {
                log.debug("后5个瓦片: {}", tileNames.subList(Math.max(0, tileNames.size() - 5), tileNames.size()));
            }
        }

        return tileNames;
    }

    /**
     * 获取指定逻辑层的所有瓦片名称集合（使用默认瓦片尺寸 256）
     *
     * @param openSlide OpenSlide 对象
     * @param level     金字塔层级（0=最高分辨率）
     * @return 瓦片名称集合，格式为 "level-col-row"
     */
    public static java.util.List<String> getTileNamesByLevel(OpenSlide openSlide, int level) {
        return getTileNamesByLevel(openSlide, level, 256);
    }
}

