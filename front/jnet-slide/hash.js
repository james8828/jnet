// 引入spark-md5
// 将一个或多个脚本同步导入当前文件。
self.importScripts('/src/libs/spark-md5.min.js')
// 接收主线程信息
self.addEventListener('message', (e) => {
    const chunks = e.data.fileChunkList
    const spark = new self.SparkMD5.ArrayBuffer()

    let percentage = 0 // 进度信息
    let count = 0
    const loadNext = index => {
        const reader = new FileReader()
        reader.readAsArrayBuffer(chunks[index].file)
        reader.onload = e => {
            count++
            spark.append(e.target.result)

            if (count == chunks.length) {
                self.postMessage({
                    percentage: 100,
                    hash: spark.end()
                })
            } else {
                percentage += 100 / chunks.length
                self.postMessage({
                    percentage
                })
                loadNext(count)
            }
        }
    }
    loadNext(0)
})


/*self.onmessage = e => {
    // 接受主线程传递的数据
    const { chunks } = e.data
    const spark = new self.SparkMD5.ArrayBuffer()

    let percentage = 0 // 进度信息
    let count = 0

    const loadNext = index => {
        const reader = new FileReader()
        reader.readAsArrayBuffer(chunks[index].file)
        reader.onload = e => {
            count++
            spark.append(e.target.result)

            if (count == chunks.length) {
                self.postMessage({
                    percentage: 100,
                    hash: spark.end()
                })
            } else {
                percentage += 100 / chunks.length
                self.postMessage({
                    percentage
                })
                loadNext(count)
            }
        }
    }
    loadNext(0)
}*/
