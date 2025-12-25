import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

// 假设这些组件都采用基于模块的懒加载方式
const HomePage = () => import('@comps/layout');
const LoginPage = () => import("@views/login");
const Viewer = () => import("@views/viewer");
const slide = () => import("@views/slide");
const user = () => import("@views/system/user");
const role = () => import("@views/system/role");
const menu = () => import("@views/system/menu");

// 定义路由表，采用嵌套路由的方式组织
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Login',
    component: LoginPage,
  },
  {
    path: '/Home',
    name: 'Home',
    component: HomePage,
    meta: { title: 'Home', requiresAuth: false },
    children: [
      {
        path: '/viewer',
        name: 'Viewer',
        component: Viewer,
        meta: { title: 'Viewer', requiresAuth: false }
      },{
        path: '/slide',
        // name: 'Viewer',
        component: slide,
        meta: { title: 'slide', requiresAuth: false }
      },{
        path: '/user',
        component: user,
        meta: { title: 'user', requiresAuth: false }
      },{
        path: '/role',
        component: role,
        meta: { title: 'role', requiresAuth: false }
      },{
        path: '/menu',
        component: menu,
        meta: { title: 'menu', requiresAuth: false }
      }
    ],
  },
];



// 创建并导出Vue Router实例
const router = createRouter({
  history: createWebHistory(),
  routes
});

// 假设在项目的某个地方有错误处理逻辑，这里仅做示意
router.onError((error) => {
  console.error('Router error:', error);
  // 这里可以添加更多的错误处理逻辑，比如跳转到错误页面等
});

export default router;

