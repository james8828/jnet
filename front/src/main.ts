import { createApp } from 'vue'
import App from './App.vue'
import "element-plus/theme-chalk/dark/css-vars.css"
import "element-plus/dist/index.css"
import "vxe-table/lib/style.css"
import "vxe-table-plugin-element/dist/style.css"
import {VxeUI,VxePager} from 'vxe-pc-ui'
import 'vxe-pc-ui/lib/style.css'
import router from "./router"
import {
    VxeTable,
    VxeColumn,
    // VxeColgroup,
    VxeGrid,
    // VxeToolbar
} from 'vxe-table'

import ElementPlus from 'element-plus'
function LazyVxeTable (app:any) {
    app.use(VxePager)
    app.use(VxeGrid)
    app.use(VxeTable)
    app.use(VxeColumn)
    app.use(VxeUI)
    // app.use(VxeColgroup)
    // app.use(VxeToolbar)
}

const app = createApp(App)
app.use(ElementPlus).use(router).use(LazyVxeTable)
router.isReady().then(() => {
    app.mount("#app")
})

