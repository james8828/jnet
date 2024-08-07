import { UploadStatusEnum } from "@/enums/uploadEnum";

export interface Upload {
    container: Container
    fileChunk: []
    data: Data
}

export interface Data {
    fileHash: string
    index: number
    hash: string
    chunk: any
    size: number
    // 如果已上传切片数组uploadedList中包含这个切片，则证明这个切片之前已经上传成功了，进度设为100。
    percentage: number
}

export interface  FileChunk {

}

export interface Container
{
    file: File
    hash: string
    worker: Worker
}

export interface File {
    name: string
    percentage: number
    status: UploadStatusEnum
    size: number
    url: number
    raw: any
    uid: number
}
