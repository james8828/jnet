
export interface Slide {
    slideId?: number;
    slideName?: string;
    slidePath?: string;
    thumbUrl?: string;
    macroUrl?: string;
    labelUrl?: string;
    cacheUrl?: string;
    multiple?: string;
    format?: string;
    width?: string;
    height?: string;
    depth?: string;
    size?: string;
    globalSize?: string;
    resolvingPower?: string;
    tileCountList?: string;
    levelCount?: number;
    resolutionX?: string;
    resolutionY?: string;
    sourceLens?: number;
    status?: number;
    createBy?: number;
    createTime?: string;
    updateBy?: number;
    updateTime?: string;
    delFlag?: boolean;
}

export interface SlidePageQueryVo {
    currentPage: number;
    pageSize: number;
    slideName?: string;
    status?: number;
}

// @types/slide.ts 或 @/constants/imageStatus.ts
export enum ImageProcessStatus {
    Uploading = 1,
    Parsing = 2,
    ParseSuccess = 3,
    ParseFail = 4,
    UploadFail = 5
}

export const ImageProcessStatusMap: Record<ImageProcessStatus, string> = {
    [ImageProcessStatus.Uploading]: '上传中',
    [ImageProcessStatus.Parsing]: '解析中',
    [ImageProcessStatus.ParseSuccess]: '解析成功',
    [ImageProcessStatus.ParseFail]: '解析失败',
    [ImageProcessStatus.UploadFail]: '上传失败'
}


